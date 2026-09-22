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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartDao cartDao;
    @Mock
    private ProductDao productDao;
    @Mock
    private MemberDao memberDao;
    @Mock
    private OrderService orderService;

    private CartService newService() {
        return new CartService(cartDao, productDao, memberDao, orderService);
    }

    private static Member member(String id) {
        Member m = new Member();
        m.setMemberId(id);
        m.setMemberName("김실증");
        return m;
    }

    private static Product product(String sku, long price, int stock) {
        Product p = new Product();
        p.setSku(sku);
        p.setProductName("스탠딩 데스크");
        p.setPrice(price);
        p.setStockQty(stock);
        p.setSaleYn("Y");
        return p;
    }

    private static CartItem cartItem(String memberId, String sku, int qty, long price) {
        CartItem c = new CartItem();
        c.setMemberId(memberId);
        c.setSku(sku);
        c.setQty(qty);
        c.setPrice(price);
        c.setProductName("스탠딩 데스크");
        return c;
    }

    // linked_tc: TC-FUNC-order-011-01
    @Test
    void addItem_newItem_upsertsAndReturnsSavedItem() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 390000, 12));
        when(cartDao.selectItem("M-0001", "SKU-1001"))
                .thenReturn(cartItem("M-0001", "SKU-1001", 2, 390000));

        CartItem result = newService().addItem("M-0001", "SKU-1001", 2);

        verify(cartDao).upsertMergeQty("M-0001", "SKU-1001", 2);
        assertThat(result.getQty()).isEqualTo(2);
    }

    // linked_tc: TC-FUNC-order-011-02
    // round2: 합산은 CartDao.upsertMergeQty(DB 원자 UPSERT)로 수행 — 애플리케이션은 더 이상
    // select→분기 후 update를 별도 호출하지 않는다(QA FAIL round1 필수1 재작업).
    @Test
    void addItem_existingItem_mergesQtyViaAtomicUpsert() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 390000, 12));
        when(cartDao.selectItem("M-0001", "SKU-1001"))
                .thenReturn(cartItem("M-0001", "SKU-1001", 5, 390000));

        CartItem result = newService().addItem("M-0001", "SKU-1001", 3);

        verify(cartDao).upsertMergeQty("M-0001", "SKU-1001", 3);
        assertThat(result.getQty()).isEqualTo(5);
    }

    // linked_tc: TC-FUNC-order-011-05
    @Test
    void addItem_qtyLessThanOne_throws400() {
        CartService service = newService();

        assertThatThrownBy(() -> service.addItem("M-0001", "SKU-1001", 0))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void addItem_memberNotFound_throws404() {
        when(memberDao.selectById("M-9999")).thenReturn(null);
        CartService service = newService();

        assertThatThrownBy(() -> service.addItem("M-9999", "SKU-1001", 1))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void addItem_productNotFound_throws404() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        when(productDao.selectBySku("SKU-9999")).thenReturn(null);
        CartService service = newService();

        assertThatThrownBy(() -> service.addItem("M-0001", "SKU-9999", 1))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // linked_tc: TC-FUNC-order-011-04
    @Test
    void addItem_soldOutProduct_throws409() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        when(productDao.selectBySku("SKU-1004")).thenReturn(product("SKU-1004", 35000, 0));
        CartService service = newService();

        assertThatThrownBy(() -> service.addItem("M-0001", "SKU-1004", 1))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    // linked_tc: TC-FUNC-order-011-03
    // round2: 신규 삽입인데 재고 초과 — UPSERT 후 merged.qty(=addedQty)가 재고 초과이므로
    // revertMerge가 restored<=0 판정으로 방금 삽입한 행을 삭제한다(QA FAIL round1 필수1 재작업).
    @Test
    void addItem_exceedsStock_upsertsThenRevertsByDelete_throws409() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 390000, 12));
        when(cartDao.selectItem("M-0001", "SKU-1001"))
                .thenReturn(cartItem("M-0001", "SKU-1001", 13, 390000));
        CartService service = newService();

        assertThatThrownBy(() -> service.addItem("M-0001", "SKU-1001", 13))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(cartDao).upsertMergeQty("M-0001", "SKU-1001", 13);
        verify(cartDao).deleteItem("M-0001", "SKU-1001");
    }

    // linked_tc: TC-FUNC-order-011-03 (합산 후 초과)
    // round2: 재담기 합산(10+5=15)이 재고(12)를 초과 — revertMerge가 방금 더한 5만 되돌려
    // 합산 전 수량(10)으로 복원한다(신규 select→분기 대신 UPSERT 후 사후 보정, QA FAIL round1 필수1).
    @Test
    void addItem_mergedQtyExceedsStock_revertsToPreMergeQty_throws409() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 390000, 12));
        when(cartDao.selectItem("M-0001", "SKU-1001"))
                .thenReturn(cartItem("M-0001", "SKU-1001", 15, 390000));
        CartService service = newService();

        assertThatThrownBy(() -> service.addItem("M-0001", "SKU-1001", 5))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT))
                .satisfies(e -> assertThat(e.getMessage()).contains("가용 12", "요청 15"));
        verify(cartDao).upsertMergeQty("M-0001", "SKU-1001", 5);
        verify(cartDao).updateQty("M-0001", "SKU-1001", 10);
        verify(cartDao, never()).deleteItem(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    // round2 신규: 판매중지(sale_yn='N') 상품 담기 거부 — 품절과 동일 계열 사유(409, 재작업 지시 #6).
    // linked_tc: TC-FUNC-order-011-02-saleStopped
    @Test
    void addItem_saleStopped_throws409_beforeUpsert() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        Product stopped = product("SKU-1005", 50000, 20);
        stopped.setSaleYn("N");
        when(productDao.selectBySku("SKU-1005")).thenReturn(stopped);
        CartService service = newService();

        assertThatThrownBy(() -> service.addItem("M-0001", "SKU-1005", 1))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(cartDao, never()).upsertMergeQty(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    // linked_tc: TC-FUNC-order-011-06
    @Test
    void get_returnsItemsAndTotalAmount() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        CartItem c1 = cartItem("M-0001", "SKU-1001", 2, 390000);
        CartItem c2 = cartItem("M-0001", "SKU-1002", 1, 129000);
        when(cartDao.selectItems("M-0001")).thenReturn(List.of(c1, c2));

        Map<String, Object> result = newService().get("M-0001");

        assertThat(result.get("items")).isEqualTo(List.of(c1, c2));
        assertThat(result.get("totalAmount")).isEqualTo(390000L * 2 + 129000L);
    }

    // linked_tc: TC-FUNC-order-011-07
    @Test
    void get_emptyCart_returnsEmptyItemsAndZeroTotal() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        when(cartDao.selectItems("M-0001")).thenReturn(List.of());

        Map<String, Object> result = newService().get("M-0001");

        assertThat((List<?>) result.get("items")).isEmpty();
        assertThat(result.get("totalAmount")).isEqualTo(0L);
    }

    @Test
    void get_memberNotFound_throws404() {
        when(memberDao.selectById("M-9999")).thenReturn(null);
        CartService service = newService();

        assertThatThrownBy(() -> service.get("M-9999"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // linked_tc: TC-FUNC-order-011-08
    @Test
    void updateQty_valid_updatesAndReturnsItem() {
        when(cartDao.selectItem("M-0001", "SKU-1001"))
                .thenReturn(cartItem("M-0001", "SKU-1001", 2, 390000))
                .thenReturn(cartItem("M-0001", "SKU-1001", 4, 390000));
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 390000, 12));

        CartItem result = newService().updateQty("M-0001", "SKU-1001", 4);

        verify(cartDao).updateQty("M-0001", "SKU-1001", 4);
        assertThat(result.getQty()).isEqualTo(4);
    }

    // linked_tc: TC-FUNC-order-011-09
    @Test
    void updateQty_qtyLessThanOne_throws400() {
        CartService service = newService();

        assertThatThrownBy(() -> service.updateQty("M-0001", "SKU-1001", 0))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
        verify(cartDao, never()).selectItem(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void updateQty_itemNotFound_throws404() {
        when(cartDao.selectItem("M-0001", "SKU-1001")).thenReturn(null);
        CartService service = newService();

        assertThatThrownBy(() -> service.updateQty("M-0001", "SKU-1001", 3))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // linked_tc: TC-FUNC-order-011-10
    @Test
    void updateQty_exceedsStock_throws409() {
        when(cartDao.selectItem("M-0001", "SKU-1001"))
                .thenReturn(cartItem("M-0001", "SKU-1001", 2, 390000));
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 390000, 12));
        CartService service = newService();

        assertThatThrownBy(() -> service.updateQty("M-0001", "SKU-1001", 13))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    // linked_tc: TC-FUNC-order-011-11
    @Test
    void delete_existingItem_deletesRow() {
        when(cartDao.selectItem("M-0001", "SKU-1001"))
                .thenReturn(cartItem("M-0001", "SKU-1001", 2, 390000));

        newService().delete("M-0001", "SKU-1001");

        verify(cartDao).deleteItem("M-0001", "SKU-1001");
    }

    @Test
    void delete_itemNotFound_throws404() {
        when(cartDao.selectItem("M-0001", "SKU-1001")).thenReturn(null);
        CartService service = newService();

        assertThatThrownBy(() -> service.delete("M-0001", "SKU-1001"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(cartDao, never()).deleteItem(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    // ── 체크아웃 (FUNC-order-012, SR-203) ──────────────────────────────────

    private static Order order(String orderNo, long totalAmount, int itemCount) {
        Order o = new Order();
        o.setOrderNo(orderNo);
        o.setMemberId("M-0001");
        o.setOrderState("PLACED");
        o.setTotalAmount(totalAmount);
        List<OrderItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(new OrderItem());
        }
        o.setItems(items);
        return o;
    }

    // linked_tc: TC-FUNC-order-012-01, TC-FUNC-order-012-02
    // 주문 생성은 OrderService.create를 그대로 재사용(재고 차감·채번 로직 복제 금지) —
    // 이 테스트는 CartService가 재고 판정을 스스로 하지 않고 위임한다는 것을 검증한다.
    // r2: 조회는 selectItemsForUpdate(잠금)로 변경(QA FAIL round1 필수1 재작업), 사전 스윕
    // 통과를 위해 productDao 모킹 추가.
    // AC-12 (SELECT ... FOR UPDATE 잠금) 검증
    @Test
    void checkout_validCart_delegatesToOrderServiceThenEmptiesCart() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        CartItem c1 = cartItem("M-0001", "SKU-1001", 2, 390000);
        CartItem c2 = cartItem("M-0001", "SKU-1002", 1, 129000);
        when(cartDao.selectItemsForUpdate("M-0001")).thenReturn(List.of(c1, c2));
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 390000, 12));
        when(productDao.selectBySku("SKU-1002")).thenReturn(product("SKU-1002", 129000, 40));
        when(orderService.create(eq("M-0001"), anyList())).thenReturn(order("20260823-0101", 909000, 2));

        Map<String, Object> result = newService().checkout("M-0001");

        assertThat(result.get("orderNo")).isEqualTo("20260823-0101");
        assertThat(result.get("totalAmount")).isEqualTo(909000L);
        assertThat(result.get("itemCount")).isEqualTo(2);
        verify(orderService).create(eq("M-0001"), anyList());
        verify(cartDao).deleteAllItems("M-0001");
    }

    // linked_tc: TC-FUNC-order-012-03
    // r2: selectItemsForUpdate로 변경 — 잠금 조회가 빈 결과면 잠금 해제 후에도 여전히 빈 장바구니.
    @Test
    void checkout_emptyCart_throws400_withoutCallingOrderService() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        when(cartDao.selectItemsForUpdate("M-0001")).thenReturn(List.of());
        CartService service = newService();

        assertThatThrownBy(() -> service.checkout("M-0001"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
        verify(orderService, never()).create(org.mockito.ArgumentMatchers.anyString(), anyList());
        verify(cartDao, never()).deleteAllItems(org.mockito.ArgumentMatchers.anyString());
    }

    // linked_tc: TC-FUNC-order-012-04
    @Test
    void checkout_memberNotFound_throws404_withoutTouchingCart() {
        when(memberDao.selectById("M-9999")).thenReturn(null);
        CartService service = newService();

        assertThatThrownBy(() -> service.checkout("M-9999"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(cartDao, never()).selectItemsForUpdate(org.mockito.ArgumentMatchers.anyString());
    }

    // linked_tc: TC-FUNC-order-012-05
    // r2 재작업: round1은 OrderService.create가 던지는 409를 그대로 전파하는 경로만 있었다(첫 부족
    // 품목 하나만 fail-fast). 이제 CartService 자체가 잠금 직후 사전 스윕으로 409를 던지므로,
    // 여기서는 "사전 스윕 이후에도 남는 최종 진실 경로"(시점 차로 스윕은 통과했으나 실제 차감이
    // 실패하는 경합)만 검증한다 — productDao 모킹은 재고가 충분한 것으로 응답해 스윕을 통과시키고,
    // orderService.create가 그 뒤에도 409를 던지는 것을 그대로 전파하는지 확인한다.
    @Test
    void checkout_orderServiceConflictAfterSweepPasses_propagatesAndCartPreserved() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        CartItem c1 = cartItem("M-0001", "SKU-1001", 5, 390000);
        when(cartDao.selectItemsForUpdate("M-0001")).thenReturn(List.of(c1));
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 390000, 12));
        when(orderService.create(eq("M-0001"), anyList()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "재고 부족: SKU-1001"));
        CartService service = newService();

        assertThatThrownBy(() -> service.checkout("M-0001"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT))
                .satisfies(e -> assertThat(e.getMessage()).contains("SKU-1001"));
        verify(cartDao, never()).deleteAllItems(org.mockito.ArgumentMatchers.anyString());
    }

    // r2 신규(QA FAIL round1 필수2): 다품목이 동시에 부족하면 사전 스윕이 전 품목을 모아 하나의
    // 409에 목록으로 담는다(D6 "품목별 사유 목록") — round1은 첫 품목만 fail-fast로 통지했다.
    // linked_tc: TC-FUNC-order-012-06
    @Test
    void checkout_multipleItemsInsufficientStock_aggregatesAllShortagesInOne409() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        CartItem c1 = cartItem("M-0001", "SKU-1001", 5, 390000);
        CartItem c2 = cartItem("M-0001", "SKU-1002", 999, 129000);
        when(cartDao.selectItemsForUpdate("M-0001")).thenReturn(List.of(c1, c2));
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 390000, 3));
        when(productDao.selectBySku("SKU-1002")).thenReturn(product("SKU-1002", 129000, 40));
        CartService service = newService();

        assertThatThrownBy(() -> service.checkout("M-0001"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT))
                .satisfies(e -> assertThat(e.getMessage())
                        .contains("SKU-1001").contains("가용3").contains("요청5")
                        .contains("SKU-1002").contains("가용40").contains("요청999"));
        verify(orderService, never()).create(org.mockito.ArgumentMatchers.anyString(), anyList());
        verify(cartDao, never()).deleteAllItems(org.mockito.ArgumentMatchers.anyString());
    }

    // r2 신규(QA FAIL round1 필수3): 판매중지(sale_yn='N') 상품이 담긴 장바구니의 체크아웃은
    // 409(품절·판매중지 계열)로 정규화한다 — round1은 OrderService.create가 계약 밖 400을 던졌다.
    @Test
    void checkout_saleStoppedItem_throws409_notBadRequest() {
        when(memberDao.selectById("M-0001")).thenReturn(member("M-0001"));
        Product stopped = product("SKU-1005", 50000, 20);
        stopped.setSaleYn("N");
        CartItem c1 = cartItem("M-0001", "SKU-1005", 1, 50000);
        when(cartDao.selectItemsForUpdate("M-0001")).thenReturn(List.of(c1));
        when(productDao.selectBySku("SKU-1005")).thenReturn(stopped);
        CartService service = newService();

        assertThatThrownBy(() -> service.checkout("M-0001"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT))
                .satisfies(e -> assertThat(e.getMessage()).contains("SKU-1005").contains("판매중지"));
        verify(orderService, never()).create(org.mockito.ArgumentMatchers.anyString(), anyList());
        verify(cartDao, never()).deleteAllItems(org.mockito.ArgumentMatchers.anyString());
    }
}
