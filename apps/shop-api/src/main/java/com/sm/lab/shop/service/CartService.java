// linked_func: FUNC-order-011, FUNC-order-012
// spec: docs/00_FUNC/stories/STORY-FUNC-order-011.md, docs/00_FUNC/stories/STORY-FUNC-order-012.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.CartDao;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.domain.CartItem;
import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.domain.Order;
import com.sm.lab.shop.domain.OrderItem;
import com.sm.lab.shop.domain.Product;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 장바구니(CART_ITEMS) — 담기·조회·수량변경·삭제 (SR-202).
 * 담기는 재고를 차감하지 않는다(보관 전용 — 02_변경명세 "재고 의미"). 재고 초과·품절은
 * 담기·수량변경 시점에 거부(409)한다.
 *
 * round2(QA FAIL round1 재작업): 담기 합산은 애플리케이션 select→분기 read-modify-write가 아니라
 * {@link CartDao#upsertMergeQty}(DB 원자 UPSERT, INSERT..ON DUPLICATE KEY UPDATE)로 수행한다.
 * PK(member_id,sku) 충돌을 DB가 흡수하므로 동시 담기에서도 lost update·500(PK 중복)이 발생하지
 * 않는다. 재고 판정은 UPSERT 직후 같은 트랜잭션 안에서 "최종(합산) qty" 기준으로 수행하고, 초과 시
 * 방금 더한 만큼만 되돌린다({@link #revertMerge}) — UPSERT 문이 잡은 행 잠금이 트랜잭션 종료까지
 * 유지되므로(InnoDB 계열), 판정·원복 사이에 다른 트랜잭션이 끼어들 수 없다.
 */
@Service
public class CartService {
    private final CartDao cartDao;
    private final ProductDao productDao;
    private final MemberDao memberDao;
    private final OrderService orderService;

    public CartService(CartDao cartDao, ProductDao productDao, MemberDao memberDao, OrderService orderService) {
        this.cartDao = cartDao;
        this.productDao = productDao;
        this.memberDao = memberDao;
        this.orderService = orderService;
    }

    /** 담기 — 같은 상품 재담기는 DB 원자 UPSERT로 수량 합산(D2, PK(member_id,sku)). */
    @Transactional
    public CartItem addItem(String memberId, String sku, int qty) {
        requireValidQty(qty);
        requireMember(memberId);
        Product product = requireProduct(sku);
        requireOnSale(product);
        if (product.getStockQty() == 0) {
            // 품절은 어떤 수량도 담길 수 없으므로 UPSERT 전에 조기 거부한다 —
            // 병합·원복 사이클 자체가 불필요(잠금·왕복 절약, r2 검증에서 정리).
            throw stockConflict(product, qty);
        }

        cartDao.upsertMergeQty(memberId, sku, qty);
        CartItem merged = cartDao.selectItem(memberId, sku);

        if (exceedsStock(product, merged.getQty())) {
            revertMerge(memberId, sku, merged.getQty(), qty);
            throw stockConflict(product, merged.getQty());
        }
        return merged;
    }

    /** 조회 — items(lineTotal 포함)·totalAmount. */
    public Map<String, Object> get(String memberId) {
        requireMember(memberId);
        List<CartItem> items = cartDao.selectItems(memberId);
        long totalAmount = items.stream().mapToLong(CartItem::getLineTotal).sum();
        return Map.of("items", items, "totalAmount", totalAmount);
    }

    /** 수량 변경 — qty&lt;1은 400(D4). 삭제는 명시적 DELETE로만(실수 삭제 방지). */
    @Transactional
    public CartItem updateQty(String memberId, String sku, int qty) {
        requireValidQty(qty);
        requireCartItem(memberId, sku);
        Product product = requireProduct(sku);
        checkStock(product, qty);

        cartDao.updateQty(memberId, sku, qty);
        return cartDao.selectItem(memberId, sku);
    }

    /** 삭제. */
    @Transactional
    public void delete(String memberId, String sku) {
        requireCartItem(memberId, sku);
        cartDao.deleteItem(memberId, sku);
    }

    /**
     * 체크아웃 — 장바구니 전체 품목을 주문으로 전환한다(FUNC-order-012, SR-203 D6).
     * 주문 생성은 새 규칙을 만들지 않고 {@link OrderService#create}를 그대로 재사용한다
     * (채번·PLACED·재고 차감 — 요구사항 명시, 검증·차감 로직을 여기서 복제하지 않는다).
     * {@code OrderService.create}가 기본 전파(REQUIRED)의 {@code @Transactional}이므로 이
     * 메서드가 이미 연 트랜잭션에 그대로 참여한다 — 다품목 중 후순위 품목의 재고 차감이 실패하면
     * (409) 앞서 성공한 선순위 품목의 차감까지 전부 롤백된다(단일 트랜잭션 원자성, D6 "원자성",
     * TC-06). 장바구니는 주문 생성이 트랜잭션 내에서 확정된 뒤에만 비운다 — 실패 시 품목 정보를
     * 보존해야 하기 때문이다(요구사항 "장바구니 보존").
     *
     * <p><b>r2 재작업(QA FAIL round1)</b>:
     * <ol>
     *   <li><b>동시 체크아웃 차단(필수1)</b> — round1은 {@code cartDao.selectItems}(비잠금 일관읽기)
     *       로 조회했다. MariaDB REPEATABLE-READ에서 각 트랜잭션이 자기 스냅샷의 장바구니를 보므로
     *       서로를 막지 못해, 같은 회원 동시 체크아웃에서 중복 주문이 재현됐다(4건 동시요청 →
     *       주문 4건·재고 4 차감, 10/10 라운드 재현). 이제 {@link CartDao#selectItemsForUpdate}
     *       (SELECT...FOR UPDATE)로 장바구니 행을 먼저 잠근다 — InnoDB 락킹 리드는 스냅샷이 아닌
     *       최신 커밋 데이터를 읽고 잠그므로, 선행 트랜잭션이 이 행들을 삭제하고 커밋하면 잠금
     *       대기 중이던 후속 트랜잭션은 잠금 해제 후 빈 결과를 보고 400(빈 장바구니)으로 거부된다.
     *       CartConcurrencyTest 관례대로 {@code CheckoutConcurrencyTest}(N=8)로 회귀 검증한다.</li>
     *   <li><b>재고 부족 품목별 사유 목록(필수2)</b> — round1은 {@code OrderService.create} 루프의
     *       첫 부족 품목 하나만 fail-fast로 전파해 D6 "품목별 사유 목록"을 부분 충족했다. 이제 잠금
     *       직후 {@link #sweepStockIssues}로 전 품목을 사전 스윕해 부족·판매중지·미존재 품목 전체를
     *       모아 409 본문 하나에 담는다. 사전 스윕은 안내용이고, 스윕 통과 후에도
     *       {@code OrderService.create}의 실제 {@code decreaseStock} 실패가 최종 진실이다(스윕과
     *       차감 사이 시점 차 대비 — 그 경로는 그대로 유지).</li>
     *   <li><b>판매중지 상품 정합(필수3)</b> — round1은 {@code OrderService.create}가 판매중지
     *       상품에 대해 그대로 400을 던져 D6("400=빈 장바구니")를 벗어난 계약 밖 응답을 냈다.
     *       사전 스윕이 판매중지도 함께 판정해 409(품절·판매중지 계열)로 정규화한다.</li>
     * </ol>
     */
    @Transactional
    public Map<String, Object> checkout(String memberId) {
        requireMember(memberId);
        List<CartItem> cartItems = cartDao.selectItemsForUpdate(memberId);
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "장바구니가 비어 있습니다");
        }

        List<String> stockIssues = sweepStockIssues(cartItems);
        if (!stockIssues.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "재고 부족: " + String.join(", ", stockIssues));
        }

        List<OrderItem> orderItems = cartItems.stream().map(ci -> {
            OrderItem item = new OrderItem();
            item.setSku(ci.getSku());
            item.setQty(ci.getQty());
            return item;
        }).collect(Collectors.toList());

        Order order = orderService.create(memberId, orderItems);
        cartDao.deleteAllItems(memberId);

        return Map.of(
                "orderNo", order.getOrderNo(),
                "totalAmount", order.getTotalAmount(),
                "itemCount", order.getItems().size());
    }

    /**
     * 체크아웃 잠금 확보 직후 전 품목의 재고·판매상태를 사전 스윕해, 부족·판매중지·미존재 품목
     * 전체를 사유 목록으로 모은다(D6 "품목별 사유", r2 재작업 필수2·필수3). 안내용 사전 판정이며,
     * 여기를 통과해도 {@code OrderService.create}의 실제 차감 실패(시점 차로 인한 경합)가 최종
     * 진실로 그대로 남는다.
     */
    private List<String> sweepStockIssues(List<CartItem> cartItems) {
        List<String> issues = new ArrayList<>();
        for (CartItem ci : cartItems) {
            Product p = productDao.selectBySku(ci.getSku());
            if (p == null) {
                issues.add(ci.getSku() + "(상품 없음)");
            } else if ("N".equals(p.getSaleYn())) {
                issues.add(p.getSku() + "(판매중지)");
            } else if (exceedsStock(p, ci.getQty())) {
                issues.add(p.getSku() + "(가용" + p.getStockQty() + "/요청" + ci.getQty() + ")");
            }
        }
        return issues;
    }

    private void requireValidQty(int qty) {
        if (qty < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "수량은 1 이상이어야 합니다(삭제하려면 삭제 버튼을 사용하세요)");
        }
    }

    private void requireMember(String memberId) {
        Member m = memberDao.selectById(memberId);
        if (m == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음: " + memberId);
        }
    }

    private Product requireProduct(String sku) {
        Product p = productDao.selectBySku(sku);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 없음: " + sku);
        }
        return p;
    }

    /** 판매중지(sale_yn='N') 상품은 담기 거부 — 품절과 동일 계열 사유(409, 재작업 지시 #6). */
    private void requireOnSale(Product product) {
        if ("N".equals(product.getSaleYn())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "판매중지 상품: " + product.getSku());
        }
    }

    private CartItem requireCartItem(String memberId, String sku) {
        CartItem item = cartDao.selectItem(memberId, sku);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니 품목 없음: " + sku);
        }
        return item;
    }

    private void checkStock(Product product, int requestedQty) {
        if (exceedsStock(product, requestedQty)) {
            throw stockConflict(product, requestedQty);
        }
    }

    private boolean exceedsStock(Product product, int requestedQty) {
        return product.getStockQty() == 0 || requestedQty > product.getStockQty();
    }

    private ResponseStatusException stockConflict(Product product, int requestedQty) {
        if (product.getStockQty() == 0) {
            return new ResponseStatusException(HttpStatus.CONFLICT, "품절 상품: " + product.getSku());
        }
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "재고 초과: 가용 " + product.getStockQty() + ", 요청 " + requestedQty);
    }

    /**
     * UPSERT로 합산된 최종 qty가 재고를 초과하면, 이번 호출에서 더한 양(addedQty)만 되돌려
     * 합산 전 상태로 원복한다. 되돌린 결과가 0 이하이면(신규 삽입이었던 경우) 행 자체를 삭제한다.
     * UPSERT 문이 잡은 행 잠금이 같은 트랜잭션 안에서 유지되므로 원복까지 원자적으로 안전하다.
     */
    private void revertMerge(String memberId, String sku, int mergedQty, int addedQty) {
        int restored = mergedQty - addedQty;
        if (restored <= 0) {
            cartDao.deleteItem(memberId, sku);
        } else {
            cartDao.updateQty(memberId, sku, restored);
        }
    }
}
