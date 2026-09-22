// linked_func: FUNC-order-012
// spec: docs/00_FUNC/stories/STORY-FUNC-order-012.md
package com.sm.lab.shop;

import com.sm.lab.shop.dao.CartDao;
import com.sm.lab.shop.dao.OrderDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.domain.Product;
import com.sm.lab.shop.service.CartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 체크아웃 원자성 회귀(TC-FUNC-order-012-06, SR-203 D6 "원자성") — 다품목 중 후순위 품목이
 * 재고 부족이면 선순위 품목의 차감·주문 반영이 전혀 없어야 한다(단일 트랜잭션 롤백).
 * CartService.checkout이 OrderService.create를 REQUIRED 전파로 같은 트랜잭션에 참여시키므로,
 * 후순위 품목에서 실패해도 선순위 품목의 decreaseStock까지 함께 롤백되는지가 검증 대상이다.
 * 실 서버 컨텍스트 + 실 DB(sl_lab) 대상 — 랩 데이터: M-0002 이도그, SKU-1002 기계식키보드(재고40),
 * SKU-1001 스탠딩데스크(재고12). 테스트 후 삽입한 CART_ITEMS를 정리한다.
 */
@SpringBootTest
class CheckoutAtomicityTest {

    private static final String MEMBER_ID = "M-0002";
    private static final String OK_SKU = "SKU-1002";    // 재고 40 — 소량 요청은 항상 성공권
    private static final String SHORT_SKU = "SKU-1001"; // 재고 12 — 대량 요청으로 부족 유도

    @Autowired
    private CartService cartService;
    @Autowired
    private CartDao cartDao;
    @Autowired
    private ProductDao productDao;
    @Autowired
    private OrderDao orderDao;

    @AfterEach
    void cleanUp() {
        cartDao.deleteItem(MEMBER_ID, OK_SKU);
        cartDao.deleteItem(MEMBER_ID, SHORT_SKU);
    }

    // linked_tc: TC-FUNC-order-012-06
    @Test
    void checkout_laterItemInsufficientStock_rollsBackEarlierDecreaseOrderAndCart() {
        int stockBefore = productDao.selectBySku(OK_SKU).getStockQty();
        int ordersBefore = orderDao.countOrders(MEMBER_ID, null, null, null);

        // selectItems는 added_at, sku 오름차순으로 정렬(CartDao.selectItems) — OK_SKU를 먼저 담아
        // OrderService.create가 그 순서대로 처리하도록 하고, SHORT_SKU(후순위)에서 실패를 유도한다.
        cartDao.insertItem(MEMBER_ID, OK_SKU, 1);
        cartDao.insertItem(MEMBER_ID, SHORT_SKU, 999);

        assertThatThrownBy(() -> cartService.checkout(MEMBER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        // 선순위(OK_SKU) 재고 차감까지 롤백돼 변화가 없어야 한다.
        Product okAfter = productDao.selectBySku(OK_SKU);
        assertThat(okAfter.getStockQty())
                .as("후순위 품목 실패 시 선순위 차감도 함께 롤백돼야 함(단일 트랜잭션)")
                .isEqualTo(stockBefore);

        // 장바구니는 그대로 보존(비워지지 않음).
        assertThat(cartDao.selectItem(MEMBER_ID, OK_SKU)).isNotNull();
        assertThat(cartDao.selectItem(MEMBER_ID, SHORT_SKU)).isNotNull();

        // 주문이 생성되지 않아야 한다(부분 주문 금지).
        assertThat(orderDao.countOrders(MEMBER_ID, null, null, null)).isEqualTo(ordersBefore);
    }
}
