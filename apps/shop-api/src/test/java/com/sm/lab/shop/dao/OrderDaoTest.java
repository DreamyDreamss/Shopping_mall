// linked_func: FUNC-order-006
// spec: docs/05_설계서/order/INF/INF-ORD-007.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.OrderDelivery;
import com.sm.lab.shop.domain.OrderShipment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.ORDER_DELIVERY) 대상 통합 테스트.
 * 랩 시드: 주문 20260816-0002 — D-0816-1(SHIPPED, shipped_at 2026-08-17 08:20:00),
 * D-0816-2(READY, shipped_at NULL). 읽기 전용 검증만 수행(시드 변경 없음).
 */
@SpringBootTest
class OrderDaoTest {

    private static final String ORDER_NO = "20260816-0002";

    @Autowired
    private OrderDao orderDao;

    // LAB-113: 최신순 정렬 — shipped_at DESC(MariaDB DESC에서 NULL은 마지막이라 미발송 READY 건이
    // 하단으로 밀림), tie는 delivery_no DESC.
    @Test
    void selectDeliveries_sortsByShippedAtDescWithNullsLast() {
        List<OrderDelivery> deliveries = orderDao.selectDeliveries(ORDER_NO);

        assertThat(deliveries).extracting(OrderDelivery::getDeliveryNo)
                .containsExactly("D-0816-1", "D-0816-2");
        assertThat(deliveries.get(0).getDeliveryState()).isEqualTo("SHIPPED");
        assertThat(deliveries.get(0).getShippedAt()).isNotNull();
        assertThat(deliveries.get(1).getDeliveryState()).isEqualTo("READY");
        assertThat(deliveries.get(1).getShippedAt()).isNull();
    }

    // linked_func: FUNC-order-001 — SR-210: 주문 목록 화면(UIS-ORD-001) 배송상태 열 전용 배치 조회.
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
    // round4(재작업지시 2 — QA r13 CONCERNS 권고2): "최신"의 정의를 배송 이력 생성 순서(delivery_no
    // DESC 단독)로 재정의 — FUNC-order-006(selectDeliveries, 주문 상세 화면)의 shipped_at DESC 기준과
    // 더 이상 동일하지 않다(이 화면에 맞는 별도 정의).
    // 랩 시드: 20260816-0002(2건 — D-0816-1 SHIPPED shipped_at 2026-08-17 08:20:00,
    // D-0816-2 READY shipped_at NULL → D-0816-2가 더 나중에 채번되어 생성 순서상 최신 → READY),
    // 20260815-0001(1건 — DELIVERED), 20260817-0001(배송 이력 없음).
    @Test
    void selectLatestDeliveryStates_returnsOneLatestRowPerOrderAndOmitsOrdersWithoutHistory() {
        List<OrderDelivery> latest = orderDao.selectLatestDeliveryStates(
                List.of("20260816-0002", "20260815-0001", "20260817-0001"));

        Map<String, String> byOrderNo = latest.stream()
                .collect(Collectors.toMap(OrderDelivery::getOrderNo, OrderDelivery::getDeliveryState));
        assertThat(byOrderNo).containsEntry("20260816-0002", "READY");
        assertThat(byOrderNo).containsEntry("20260815-0001", "DELIVERED");
        assertThat(byOrderNo).doesNotContainKey("20260817-0001");
        // 주문번호당 정확히 1건(최신)만 — 2건 이력이 있는 20260816-0002도 결과에 1행뿐이어야 한다
        assertThat(latest).filteredOn(d -> "20260816-0002".equals(d.getOrderNo())).hasSize(1);
    }

    // LAB-105.1 — 주문 상세(INF-ORD-004) 배송 정보. 실측(db-main describe): D-0815-1
    // (order_no=20260815-0001, carrier_code=CJ, carrier_name=CJ대한통운, invoice_no=INV-88010).
    @Test
    void selectShipments_returnsCarrierAndInvoiceForShippedRow() {
        List<OrderShipment> shipments = orderDao.selectShipments("20260815-0001");

        assertThat(shipments).hasSize(1);
        OrderShipment s = shipments.get(0);
        assertThat(s.getDeliveryNo()).isEqualTo("D-0815-1");
        assertThat(s.getCarrierCode()).isEqualTo("CJ");
        assertThat(s.getCarrierName()).isEqualTo("CJ대한통운");
        assertThat(s.getInvoiceNo()).isEqualTo("INV-88010");
    }

    // 부분 출고 2건 — D-0816-1(carrier·invoice 있음) + D-0816-2(READY, "송장 등록 대기" = invoice_no NULL).
    @Test
    void selectShipments_partialShipment_returnsBothRowsWithOneAwaitingInvoice() {
        List<OrderShipment> shipments = orderDao.selectShipments("20260816-0002");

        Map<String, OrderShipment> byDeliveryNo = shipments.stream()
                .collect(Collectors.toMap(OrderShipment::getDeliveryNo, s -> s));
        assertThat(byDeliveryNo).hasSize(2);
        assertThat(byDeliveryNo.get("D-0816-1").getCarrierCode()).isEqualTo("CJ");
        assertThat(byDeliveryNo.get("D-0816-1").getInvoiceNo()).isNotNull();
        assertThat(byDeliveryNo.get("D-0816-2").getCarrierCode()).isNull();
        assertThat(byDeliveryNo.get("D-0816-2").getInvoiceNo()).isNull();
    }

    // 출고 전(ORDER_DELIVERY 행 0건) — 오류가 아니라 빈 리스트.
    @Test
    void selectShipments_noDeliveryRows_returnsEmptyList() {
        List<OrderShipment> shipments = orderDao.selectShipments("20260817-0001");

        assertThat(shipments).isEmpty();
    }
}
