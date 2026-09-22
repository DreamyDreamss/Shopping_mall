// linked_func: FUNC-order-002
// spec: docs/00_FUNC/stories/STORY-FUNC-order-002.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.OrderDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.domain.Order;
import com.sm.lab.shop.domain.OrderDelivery;
import com.sm.lab.shop.domain.OrderItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 실 DB(MariaDB, sl_lab) 대상 통합 테스트 — FUNC-order-002(주문 상세/취소, INF-ORD-004/INF-ORD-006).
 * 랩 시드(실측): 20260817-0002(M-0002,CANCELED) · 20260816-0002(M-0001,PARTIAL_SHIPPED, 배송
 * D-0816-1 SHIPPED + D-0816-2 READY) · 20260817-0001(M-0003,PLACED, 배송 이력 없음) ·
 * 20260815-0001(M-0001,DONE, 아이템 SKU-1001/SKU-1002) — 전부 del_yn='N'.
 * SQL 레벨 규칙(del_yn 상시필터·조건부 UPDATE 레이스 가드)은 Mockito로 흉내낼 수 없어 실 DB로 검증한다.
 */
@SpringBootTest
class OrderCancelIntegrationTest {

    private static final String TEST_ORDER_NO = "20260823-9910";
    private static final String MEMBER_ID = "M-0001";
    private static final String TEST_SKU = "SKU-1002";
    private static final int TEST_QTY = 3;

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderDao orderDao;
    @Autowired
    private ProductDao productDao;
    @Autowired
    private DataSource dataSource;

    @AfterEach
    void cleanUp() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM ORDER_ITEMS WHERE order_no = '" + TEST_ORDER_NO + "'");
            st.executeUpdate("DELETE FROM ORDER_DELIVERY WHERE order_no = '" + TEST_ORDER_NO + "'");
            st.executeUpdate("DELETE FROM ORDERS WHERE order_no = '" + TEST_ORDER_NO + "'");
        }
    }

    // INF-ORD-004: del_yn='N' 상시필터 → 논리삭제된 주문 조회 시 404 (DAO 레벨 + 서비스 매핑 둘 다 실측)
    @Test
    void detail_logicallyDeletedOrder_returns404ViaDelYnFilter() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT INTO ORDERS (order_no, member_id, order_state, total_amount, del_yn) VALUES ('"
                    + TEST_ORDER_NO + "', '" + MEMBER_ID + "', 'DONE', 1000, 'Y')");
        }

        assertThat(orderDao.selectByOrderNo(TEST_ORDER_NO)).isNull();
        assertThatThrownBy(() -> orderService.detail(TEST_ORDER_NO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // INF-ORD-004: items는 ORDER_ITEMS와 PRODUCTS를 조인해 productName을 함께 반환
    @Test
    void selectItems_joinsProductsForProductName() {
        List<OrderItem> items = orderDao.selectItems("20260815-0001");

        assertThat(items).extracting(OrderItem::getSku, OrderItem::getProductName)
                .containsExactlyInAnyOrder(
                        tuple("SKU-1002", "기계식 키보드"),
                        tuple("SKU-1001", "스탠딩 데스크"));
    }

    // INF-ORD-004: deliveries는 배송이 없으면 빈 배열(null 아님)
    @Test
    void selectDeliveries_orderWithoutDeliveryHistory_returnsEmptyListNotNull() {
        List<OrderDelivery> deliveries = orderDao.selectDeliveries("20260817-0001");

        assertThat(deliveries).isNotNull();
        assertThat(deliveries).isEmpty();
    }

    // INF-ORD-006: 상태 전이(UPDATE)는 order_state NOT IN ('CANCELED','DONE') 조건부 쿼리 —
    // 이미 종결(CANCELED) 상태인 시드 주문은 조건에 안 걸려 0행(레이스 가드가 이미 종결된 상태도 막는다).
    @Test
    void updateState_orderAlreadyTerminal_conditionalUpdateAffectsZeroRows() {
        int affected = orderDao.updateState("20260817-0002", "CANCELED");

        assertThat(affected).isZero();
    }

    // INF-ORD-006: 배송 중 하나라도 SHIPPED/DELIVERED면 409(ORD-4001) — 상태 변경 없이 거부됨을 실측
    @Test
    void cancel_shippedDeliveryExists_rejectsWithoutStateChange() {
        assertThatThrownBy(() -> orderService.cancel("20260816-0002"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(((ResponseStatusException) e).getReason()).contains("ORD-4001");
                });
        assertThat(orderDao.selectByOrderNo("20260816-0002").getOrderState()).isEqualTo("PARTIAL_SHIPPED");
    }

    // INF-ORD-006: 성공 경로 — 상태 전이 성공 후에만 라인별 재고가 원복된다(실 DB 종단 검증)
    @Test
    void cancel_success_transitionsToCanceledAndRestoresLineStock() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT INTO ORDERS (order_no, member_id, order_state, total_amount, del_yn) VALUES ('"
                    + TEST_ORDER_NO + "', '" + MEMBER_ID + "', 'PLACED', 387000, 'N')");
            st.executeUpdate("INSERT INTO ORDER_ITEMS (order_no, line_no, sku, qty, unit_price) VALUES ('"
                    + TEST_ORDER_NO + "', 1, '" + TEST_SKU + "', " + TEST_QTY + ", 129000)");
        }
        int stockBefore = productDao.selectBySku(TEST_SKU).getStockQty();

        Order result = orderService.cancel(TEST_ORDER_NO);

        assertThat(result.getOrderState()).isEqualTo("CANCELED");
        assertThat(orderDao.selectByOrderNo(TEST_ORDER_NO).getOrderState()).isEqualTo("CANCELED");
        assertThat(productDao.selectBySku(TEST_SKU).getStockQty()).isEqualTo(stockBefore + TEST_QTY);

        // 다른 테스트 오염 방지 — 이 테스트가 늘린 재고만 원복(행 자체는 @AfterEach가 정리)
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE PRODUCTS SET stock_qty = stock_qty - " + TEST_QTY + " WHERE sku = '" + TEST_SKU + "'");
        }
    }
}
