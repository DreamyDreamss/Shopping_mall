// linked_func: FUNC-order-013
// spec: docs/00_FUNC/stories/STORY-FUNC-order-013.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Order;
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
}
