// linked_func: FUNC-order-013
// spec: docs/00_FUNC/stories/STORY-FUNC-order-013.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Order;
import com.sm.lab.shop.domain.OrderDelivery;
import com.sm.lab.shop.domain.OrderShipment;
import com.sm.lab.shop.service.ExportRowLimitExceededException;
import com.sm.lab.shop.service.OrderService;
import com.sm.lab.shop.support.AdminApiKeyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// LAB-104 (FUNC-order-013): GET /api/orders/export — HTTP 계약(응답 헤더/본문 위임)만 검증,
// CSV 조립 규칙 자체는 OrderServiceTest에서 검증.
// SR-204 R-5: 인증 필터 도입 후 admin 키 기본 주입(AdminApiKeyTestConfig) — 인증 자체 검증은
// ApiKeyAuthIntegrationTest에서 별도 수행.
@WebMvcTest(OrderController.class)
@Import(AdminApiKeyTestConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    // linked_tc: TC-FUNC-order-013-02
    @Test
    void export_noFilter_returns200WithCsvContentType() throws Exception {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = "주문번호,회원,상태,금액,주문일시\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] csv = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, csv, 0, bom.length);
        System.arraycopy(body, 0, csv, bom.length, body.length);
        when(orderService.exportCsv(null, null)).thenReturn(csv);

        mockMvc.perform(get("/api/orders/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().bytes(csv));

        verify(orderService).exportCsv(null, null);
    }

    // linked_tc: TC-FUNC-order-013-06
    @Test
    void export_returnsAttachmentContentDispositionWithTimestampedFilename() throws Exception {
        when(orderService.exportCsv(null, null)).thenReturn(new byte[0]);

        mockMvc.perform(get("/api/orders/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.matchesPattern("attachment; filename=\"orders_\\d{8}_\\d{6}\\.csv\"")));
    }

    // linked_tc: TC-FUNC-order-013-03
    // r2(D11, QA r1 FAIL 필수1): orderState가 정본 파라미터 — 목록 API([[INF-ORD-003]])와 동일 축.
    @Test
    void export_withOrderStateFilter_passesThroughToService() throws Exception {
        when(orderService.exportCsv(null, "PLACED")).thenReturn(new byte[0]);

        mockMvc.perform(get("/api/orders/export").param("orderState", "PLACED"))
                .andExpect(status().isOk());

        verify(orderService).exportCsv(null, "PLACED");
    }

    // linked_tc: TC-FUNC-order-013-03 (별칭 — D10 URL 예시 보존)
    @Test
    void export_withStatusAliasOnly_passesThroughToService() throws Exception {
        when(orderService.exportCsv(null, "PLACED")).thenReturn(new byte[0]);

        mockMvc.perform(get("/api/orders/export").param("status", "PLACED"))
                .andExpect(status().isOk());

        verify(orderService).exportCsv(null, "PLACED");
    }

    // linked_tc: TC-FUNC-order-013-03 (둘 다 오면 orderState 우선, D11)
    @Test
    void export_withBothOrderStateAndStatus_orderStateTakesPrecedence() throws Exception {
        when(orderService.exportCsv(null, "PLACED")).thenReturn(new byte[0]);

        mockMvc.perform(get("/api/orders/export")
                        .param("orderState", "PLACED")
                        .param("status", "CANCELED"))
                .andExpect(status().isOk());

        verify(orderService).exportCsv(null, "PLACED");
    }

    // linked_tc: TC-FUNC-order-013-04
    @Test
    void export_withMemberIdFilter_passesThroughToService() throws Exception {
        when(orderService.exportCsv("M-0001", null)).thenReturn(new byte[0]);

        mockMvc.perform(get("/api/orders/export").param("memberId", "M-0001"))
                .andExpect(status().isOk());

        verify(orderService).exportCsv("M-0001", null);
    }

    // linked_tc: TC-FUNC-order-013-26
    // SR-204 R-3: export 결과가 행 상한 초과 시 413 + {"error":"payload_too_large","limit":N}.
    // 실제 상한 판정·조립 순서는 OrderServiceTest에서 검증 — 여기는 예외→HTTP 매핑만 확인.
    @Test
    void export_rowLimitExceeded_returns413WithPayloadTooLargeBody() throws Exception {
        when(orderService.exportCsv(null, null)).thenThrow(new ExportRowLimitExceededException(1000));

        mockMvc.perform(get("/api/orders/export"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("payload_too_large"))
                .andExpect(jsonPath("$.limit").value(1000));
    }

    // linked_func: FUNC-order-001
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
    // SR-205: GET /api/orders/ 조회 기간 파라미터가 서비스로 그대로 전달되는지(HTTP 계약만 — 기본값
    // 계산·기간 필터 SQL 자체는 OrderServiceTest에서 검증).
    @Test
    void list_withDateRangeParams_passesThroughToService() throws Exception {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        when(orderService.list(null, null, start, end, 1, 20))
                .thenReturn(Map.of("totalCount", 0, "page", 1, "items", List.of()));

        mockMvc.perform(get("/api/orders").param("startDate", "2026-08-01").param("endDate", "2026-08-31"))
                .andExpect(status().isOk());

        verify(orderService).list(null, null, start, end, 1, 20);
    }

    // linked_func: FUNC-order-001 — 기간 파라미터 미제시 시 null을 그대로 서비스에 위임(기본값 계산은 서비스 책임)
    @Test
    void list_withoutDateRangeParams_passesNullToService() throws Exception {
        when(orderService.list(null, null, null, null, 1, 20))
                .thenReturn(Map.of("totalCount", 0, "page", 1, "items", List.of()));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk());

        verify(orderService).list(null, null, null, null, 1, 20);
    }

    // linked_func: FUNC-order-001 — 날짜 형식 오류는 Spring 기본 타입변환 실패로 기존 400 계약에 편입(회귀)
    @Test
    void list_invalidDateFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/orders").param("startDate", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    // linked_func: FUNC-order-001 — 기간 필터는 기존 memberId·orderState 필터와 함께 AND 결합 전달
    @Test
    void list_dateRangeWithMemberIdAndOrderState_passesAllFiltersToService() throws Exception {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        when(orderService.list("M-0001", "PLACED", start, end, 1, 20))
                .thenReturn(Map.of("totalCount", 0, "page", 1, "items", List.of()));

        mockMvc.perform(get("/api/orders")
                        .param("memberId", "M-0001")
                        .param("orderState", "PLACED")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isOk());

        verify(orderService).list("M-0001", "PLACED", start, end, 1, 20);
    }

    // linked_func: FUNC-order-001 — round2(QA r1 FAIL 필수1 재작업): 서비스가 유효 구간 역전을 400으로
    // 거부하면 그 계약이 REST 응답에 그대로 반영된다(HTTP 레벨 — 판정 로직 자체는 OrderServiceTest에서 검증).
    @Test
    void list_startAfterEnd_returns400() throws Exception {
        LocalDate start = LocalDate.of(2026, 8, 31);
        LocalDate end = LocalDate.of(2026, 8, 1);
        when(orderService.list(null, null, start, end, 1, 20))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 시작일이 종료일보다 늦습니다"));

        mockMvc.perform(get("/api/orders").param("startDate", "2026-08-31").param("endDate", "2026-08-01"))
                .andExpect(status().isBadRequest());
    }

    // linked_func: FUNC-order-001 — SR-210 회귀: 배송상태 열은 화면 조회 경로 전용이며(scope_freeze,
    // api_compat) INF-ORD-003(GET /api/orders) 응답에는 절대 포함되지 않는다. 필드가 조용히
    // 추가되는 회귀를 잡기 위해 응답 항목에 deliveryState 키가 없는지 직접 확인한다.
    // linked_tc: TC-FUNC-order-001-15
    @Test
    void list_responseNeverIncludesDeliveryStateField() throws Exception {
        Order o = new Order();
        o.setOrderNo("20260817-0001");
        o.setMemberId("M-0001");
        o.setMemberName("김실증");
        o.setOrderState("PLACED");
        o.setTotalAmount(55000L);
        o.setOrderedAt(LocalDateTime.of(2026, 8, 17, 9, 0));
        when(orderService.list(null, null, null, null, 1, 20))
                .thenReturn(Map.of("totalCount", 1, "page", 1, "items", List.of(o)));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderNo").value("20260817-0001"))
                .andExpect(jsonPath("$.items[0].deliveryState").doesNotExist());
    }

    // LAB-105.1 — GET /api/orders/{orderNo} 신설(이 파일에 detail() 테스트가 아직 없었다).
    private static Order orderWithoutOptionalLists(String orderNo) {
        Order o = new Order();
        o.setOrderNo(orderNo);
        o.setMemberId("M-0001");
        o.setMemberName("김실증");
        o.setOrderState("SHIPPED");
        o.setTotalAmount(55000L);
        o.setOrderedAt(LocalDateTime.of(2026, 8, 17, 9, 0));
        o.setItems(List.of());
        o.setDeliveries(List.of());
        return o;
    }

    private static OrderShipment shipment(String deliveryNo, String carrierCode, String carrierName, String invoiceNo) {
        OrderShipment s = new OrderShipment();
        s.setDeliveryNo(deliveryNo);
        s.setCarrierCode(carrierCode);
        s.setCarrierName(carrierName);
        s.setInvoiceNo(invoiceNo);
        return s;
    }

    // linked_tc: LAB-105.1
    @Test
    void detail_returnsShipmentsArrayWithCarrierFields() throws Exception {
        Order o = orderWithoutOptionalLists("20260815-0001");
        o.setShipments(List.of(shipment("D-0815-1", "CJ", "CJ대한통운", "INV-88010")));
        when(orderService.detail("20260815-0001")).thenReturn(o);

        mockMvc.perform(get("/api/orders/20260815-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipments[0].deliveryNo").value("D-0815-1"))
                .andExpect(jsonPath("$.shipments[0].carrierCode").value("CJ"))
                .andExpect(jsonPath("$.shipments[0].carrierName").value("CJ대한통운"))
                .andExpect(jsonPath("$.shipments[0].invoiceNo").value("INV-88010"));
    }

    // linked_tc: LAB-105.1 — 출고 전은 오류가 아니라 빈 배열(AC 확정)
    @Test
    void detail_noDeliveries_returnsEmptyShipmentsArray() throws Exception {
        Order o = orderWithoutOptionalLists("20260817-0001");
        o.setShipments(List.of());
        when(orderService.detail("20260817-0001")).thenReturn(o);

        mockMvc.perform(get("/api/orders/20260817-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipments").isArray())
                .andExpect(jsonPath("$.shipments").isEmpty());
    }

    // linked_tc: LAB-105.1 — 회귀 가드(SR-210 list_responseNeverIncludesDeliveryStateField와 동일 기법):
    // OrderDelivery에는 carrierCode getter 자체가 없어 이 단언은 "필드 부재" 검증이다(값이 null인
    // 경우를 놓치는 거짓보증이 아니다 — SR-306#2 doesNotExist() 오탐과 다른 조건, STORY "실패 사례집" 참조).
    @Test
    void detail_deliveriesArrayNeverGainsCarrierFields() throws Exception {
        Order o = orderWithoutOptionalLists("20260816-0002");
        OrderDelivery delivery = new OrderDelivery();
        delivery.setDeliveryNo("D-0816-1");
        delivery.setDeliveryState("SHIPPED");
        delivery.setInvoiceNo("INV-88031");
        o.setDeliveries(List.of(delivery));
        o.setShipments(List.of(shipment("D-0816-1", "CJ", "CJ대한통운", "INV-88031")));
        when(orderService.detail("20260816-0002")).thenReturn(o);

        mockMvc.perform(get("/api/orders/20260816-0002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveries[0].carrierCode").doesNotExist());
    }

    // linked_tc: LAB-105.1 — 회귀 확인용: 이 엔드포인트 자체가 이번에 신설이라 기존 404 계약도 같이 고정.
    @Test
    void detail_notFound_returns404() throws Exception {
        when(orderService.detail("NO-SUCH")).thenThrow(new ResponseStatusException(NOT_FOUND, "주문 없음: NO-SUCH"));

        mockMvc.perform(get("/api/orders/NO-SUCH"))
                .andExpect(status().isNotFound());
    }
}
