// linked_func: FUNC-order-001
// spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Order;
import com.sm.lab.shop.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// SR-205 (FUNC-order-001): GET /order/list — 조회 기간(startDate/endDate) 입력이 memberId·orderState와
// 함께 서비스로 전달되는지, 빈 결과 안내 문구가 렌더되는지 검증. 화면 라우트는 무인증(INF-ORD-003
// 인증 정책 문서 — /order/** 무인증 유지)이라 별도 인증 설정을 import하지 않는다.
@WebMvcTest(OrderViewController.class)
class OrderViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    // linked_func: FUNC-order-001 — 기간 파라미터 미제시 시 null을 그대로 서비스에 위임(기본값은 서비스 책임),
    // 폼 입력창은 계산된 기본값으로 자동 채우지 않는다([미상] 최소가정)
    // linked_tc: TC-FUNC-order-001-04
    @Test
    void orderList_noDateParams_passesNullToServiceAndRendersEmptyDateInputs() throws Exception {
        when(orderService.list(null, null, null, null, 1, 20))
                .thenReturn(Map.of("totalCount", 0, "page", 1, "items", List.of()));

        mockMvc.perform(get("/order/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/list"))
                .andExpect(model().attribute("startDate", ""))
                .andExpect(model().attribute("endDate", ""));

        verify(orderService).list(null, null, null, null, 1, 20);
    }

    // linked_func: FUNC-order-001 — SR-205 scr_empty_state: 빈 결과 안내 문구
    // round3(SR-208 재작업 필수3): 정상 조회 0건(오류 아님)은 회귀 범위 — 빈 결과 안내 문구뿐
    // 아니라 "전체 0건" 총건수 라벨도 함께 유지되는지 검증한다. searchError==null 조건을 이번에
    // "전체 N건" 라벨에 새로 걸었으므로, 라벨이 조용히 사라지는 회귀를 이 테스트가 잡아야 한다.
    // SR-224 확정요건: 검색 조건(memberId/orderState/startDate/endDate) 없이 0건이면
    // "조회 결과가 없습니다"로 통일(product/list.html·cart/list.html과 동일 컨벤션).
    // linked_tc: TC-FUNC-order-001-05
    @Test
    void orderList_emptyResult_showsNoResultsMessage() throws Exception {
        when(orderService.list(null, null, null, null, 1, 20))
                .thenReturn(Map.of("totalCount", 0, "page", 1, "items", List.of()));

        mockMvc.perform(get("/order/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("조회 결과가 없습니다")))
                .andExpect(content().string(not(containsString("조건에 맞는 결과가 없습니다"))))
                // "전체 0건" 라벨(list.html:70)이 정상 0건 경로에서도 그대로 렌더되는지 대상 요소를
                // 특정해 확인 — <p>...전체... 태그 자체가 아니라 렌더된 문구를 정확히 매칭한다.
                .andExpect(content().string(containsString("<p>전체 <span>0</span>건</p>")));
    }

    // linked_func: FUNC-order-001 — SR-224 확정요건: 검색 조건(memberId/orderState/startDate/endDate
    // 중 하나라도)이 걸린 상태에서 0건이면 "조건에 맞는 결과가 없습니다"로 구분(product/list.html의
    // keyword 조건부 문구와 동일 컨벤션). 조건 하나만(memberId) 걸어도 구분되는지 확인한다.
    // linked_tc: TC-FUNC-order-001-24
    @Test
    void orderList_emptyResultWithMemberIdFilter_showsConditionSpecificMessage() throws Exception {
        when(orderService.list("M-9999", null, null, null, 1, 20))
                .thenReturn(Map.of("totalCount", 0, "page", 1, "items", List.of()));

        mockMvc.perform(get("/order/list").param("memberId", "M-9999"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("조건에 맞는 결과가 없습니다")))
                .andExpect(content().string(not(containsString("조회 결과가 없습니다"))));
    }

    // linked_func: FUNC-order-001 — 기간·회원·상태 필터가 함께 GET /order/list로 전송되며(AND 결합)
    // 서비스로 그대로 위임된다
    // linked_tc: TC-FUNC-order-001-06
    @Test
    void orderList_withDateRangeMemberIdAndOrderState_passesAllFiltersToService() throws Exception {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        Order o = new Order();
        o.setOrderNo("20260817-0001");
        o.setMemberId("M-0001");
        o.setMemberName("김실증");
        o.setOrderState("PLACED");
        o.setOrderedAt(LocalDateTime.of(2026, 8, 17, 9, 0));
        when(orderService.list("M-0001", "PLACED", start, end, 1, 20))
                .thenReturn(Map.of("totalCount", 1, "page", 1, "items", List.of(o)));

        mockMvc.perform(get("/order/list")
                        .param("memberId", "M-0001")
                        .param("orderState", "PLACED")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("startDate", "2026-08-01"))
                .andExpect(model().attribute("endDate", "2026-08-31"));

        verify(orderService).list("M-0001", "PLACED", start, end, 1, 20);
    }

    // linked_func: FUNC-order-001 — 그 외 화면 구성(주문목록 테이블·총건수)은 변경되지 않는다(회귀)
    // linked_tc: TC-FUNC-order-001-07
    @Test
    void orderList_withResults_stillRendersTableAndTotalCount() throws Exception {
        Order o = new Order();
        o.setOrderNo("20260817-0001");
        o.setMemberId("M-0001");
        o.setMemberName("김실증");
        o.setOrderState("PLACED");
        o.setOrderedAt(LocalDateTime.of(2026, 8, 17, 9, 0));
        when(orderService.list(null, null, null, null, 1, 20))
                .thenReturn(Map.of("totalCount", 1, "page", 1, "items", List.of(o)));

        mockMvc.perform(get("/order/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("20260817-0001")))
                .andExpect(content().string(containsString("전체")));
    }

    // linked_func: FUNC-order-001 — round2(QA r1 FAIL 필수2 재작업): 서비스가 유효 구간 역전을 400으로
    // 거부하면 화면은 기존 공통 오류 배너 패턴(cart/list.html의 checkoutError, product/detail.html의
    // addToCartError)을 따라 4xx를 흡수해 안내 문구로 치환한다(장애 은폐가 아니라 사용자 원인 안내).
    // round3(SR-208): 4xx 시 오류 배너만 남기고 빈 결과 안내(SR-224: "조회 결과가 없습니다"/"조건에
    // 맞는 결과가 없습니다")·"전체 N건" 라벨·결과 그리드는 모두 숨긴다(회귀였던 동시노출을 해소).
    // linked_tc: TC-FUNC-order-001-08
    @Test
    void orderList_invalidDateRange_absorbsErrorAndShowsBannerOnlyHidesGridAndCounts() throws Exception {
        LocalDate start = LocalDate.of(2026, 8, 31);
        LocalDate end = LocalDate.of(2026, 8, 1);
        when(orderService.list(null, null, start, end, 1, 20))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 시작일이 종료일보다 늦습니다"));

        mockMvc.perform(get("/order/list").param("startDate", "2026-08-31").param("endDate", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/list"))
                .andExpect(content().string(containsString("조회 시작일이 종료일보다 늦습니다")))
                .andExpect(content().string(not(containsString("조회 결과가 없습니다"))))
                .andExpect(content().string(not(containsString("조건에 맞는 결과가 없습니다"))))
                // "전체 N건" 라벨(list.html:70, <p>전체 ...) 부재 확인 — 페이지 전체에서 한 음절
                // "건"의 부재를 요구하던 과대 단정 대신, 그 라벨의 실제 렌더 형태(<p>전체 ...)만 특정한다.
                .andExpect(content().string(not(containsString("<p>전체"))))
                .andExpect(content().string(not(containsString("<table"))));
    }

    // linked_func: FUNC-order-001 — SR-209: 조회기간 프리셋 버튼(최근 7/30/90일)이 검색조건 블록에
    // 렌더되고, 클릭 시 #startDate·#endDate를 채운 뒤 #searchForm(GET /order/list)을 즉시 제출하는
    // JS(applyOrderDatePreset)를 호출하도록 마크업이 연결돼 있는지 검증한다. 실제 날짜 계산·재조회는
    // 브라우저 JS 실행이 필요해 MockMvc 렌더 검증 범위 밖(E2E 대상) — 여기서는 계약(버튼 3개 존재,
    // 각각 올바른 인자로 onclick 연결, 기존 #btnSearch·필터 입력은 그대로 유지)만 확인한다.
    // linked_tc: TC-FUNC-order-001-18
    @Test
    void orderList_rendersDatePresetButtonsWiredToFormResubmit() throws Exception {
        when(orderService.list(null, null, null, null, 1, 20))
                .thenReturn(Map.of("totalCount", 0, "page", 1, "items", List.of()));

        mockMvc.perform(get("/order/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnPreset7\"")))
                .andExpect(content().string(containsString("onclick=\"applyOrderDatePreset(7)\"")))
                .andExpect(content().string(containsString("id=\"btnPreset30\"")))
                .andExpect(content().string(containsString("onclick=\"applyOrderDatePreset(30)\"")))
                .andExpect(content().string(containsString("id=\"btnPreset90\"")))
                .andExpect(content().string(containsString("onclick=\"applyOrderDatePreset(90)\"")))
                // 기존 검색 버튼·필터 입력은 그대로 유지(회귀)
                .andExpect(content().string(containsString("id=\"btnSearch\"")))
                .andExpect(content().string(containsString("id=\"selState\"")));
    }
    // linked_func: FUNC-order-001 — SR-210: ③ 그리드 6번째 열 "배송상태" — 값은 컨트롤러가 화면 조회
    // 경로에서 별도 조합한 deliveryStatusByOrderNo(orderService.latestDeliveryStatesByOrderNo)에서
    // 채워진다. INF-ORD-003 응답 계약과 무관(scope_freeze, api_compat) — orderService.list() 스텁은
    // 기존과 동일하게 유지하고 이 메서드만 별도로 스텁한다.
    // linked_tc: TC-FUNC-order-001-19
    @Test
    void orderList_orderHasDeliveryHistory_showsLatestDeliveryStateColumn() throws Exception {
        Order o = new Order();
        o.setOrderNo("20260816-0002");
        o.setMemberId("M-0001");
        o.setMemberName("김실증");
        o.setOrderState("PLACED");
        o.setOrderedAt(LocalDateTime.of(2026, 8, 17, 9, 0));
        when(orderService.list(null, null, null, null, 1, 20))
                .thenReturn(Map.of("totalCount", 1, "page", 1, "items", List.of(o)));
        when(orderService.latestDeliveryStatesByOrderNo(List.of(o)))
                .thenReturn(Map.of("20260816-0002", "SHIPPED"));

        mockMvc.perform(get("/order/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<th>배송상태</th>")))
                .andExpect(content().string(containsString("SHIPPED")));
    }

    // linked_func: FUNC-order-001 — SR-210: 배송 이력이 없는 주문은 "-"로 표시한다(요구 요지)
    // linked_tc: TC-FUNC-order-001-20
    @Test
    void orderList_orderHasNoDeliveryHistory_showsDashInDeliveryStateColumn() throws Exception {
        Order o = new Order();
        o.setOrderNo("20260817-0001");
        o.setMemberId("M-0001");
        o.setMemberName("김실증");
        o.setOrderState("PLACED");
        o.setOrderedAt(LocalDateTime.of(2026, 8, 17, 9, 0));
        when(orderService.list(null, null, null, null, 1, 20))
                .thenReturn(Map.of("totalCount", 1, "page", 1, "items", List.of(o)));
        when(orderService.latestDeliveryStatesByOrderNo(List.of(o))).thenReturn(Map.of());

        mockMvc.perform(get("/order/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<td>-</td>")));
    }

    // linked_func: FUNC-order-001 — SR-225: 화면의 금액은 천 단위 구분 기호(콤마) + '원'으로 표시한다
    // (요구 요지: "금액이 15000처럼 붙어 나와 자릿수를 잘못 읽는다"). list.html:67의
    // #numbers.formatInteger(o.totalAmount, 3, 'COMMA') 렌더를 잠근다 — API 응답의 숫자 타입 자체는
    // 이 SR의 범위가 아니다(변경 없음, Order.totalAmount는 계속 long — OrderControllerTest 참조).
    // round(GATE round14~16 carryover 정정): 부정 단언을 ">129000<"에서 ">129000원<"로 교정 —
    // 템플릿은 콤마 포맷팅과 '원' 접미사를 별도로 이어붙이므로(list.html:67), 포맷팅만 제거되는
    // 뮤테이션이면 렌더는 "129000원"이 된다. 종전 ">129000<" 부정 단언은 이 경우에도 매칭되지 않아
    // (실제 렌더가 ">129000원<"이라 하위문자열 ">129000<"는 없음) 항상 통과하는 발화 불가 단언이었다.
    // linked_tc: TC-FUNC-order-001-23
    @Test
    void orderList_totalAmount_rendersWithThousandsSeparator() throws Exception {
        Order o = new Order();
        o.setOrderNo("20260817-0001");
        o.setMemberId("M-0001");
        o.setMemberName("김실증");
        o.setOrderState("PLACED");
        o.setTotalAmount(129000L);
        o.setOrderedAt(LocalDateTime.of(2026, 8, 17, 9, 0));
        when(orderService.list(null, null, null, null, 1, 20))
                .thenReturn(Map.of("totalCount", 1, "page", 1, "items", List.of(o)));

        mockMvc.perform(get("/order/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("129,000원")))
                .andExpect(content().string(not(containsString(">129000원<"))));
    }

    // linked_func: FUNC-order-001 — SR-208: startDate 미입력(endDate만 입력)으로 인한 역전은 서버가
    // 계산한 startDate 값을 "역전 사유"로 노출하지 않고 기본값 적용 사실 + 시작일 지정 안내로 대체한다.
    // linked_tc: TC-FUNC-order-001-09
    @Test
    void orderList_endDateOnlyInvertedRange_bannerExplainsDefaultInsteadOfComputedStartDate() throws Exception {
        LocalDate farPastEnd = LocalDate.now().minusDays(60);
        when(orderService.list(null, null, null, farPastEnd, 1, 20))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "시작일을 지정하지 않아 기본값(최근 30일)이 적용되었습니다. 입력한 종료일(" + farPastEnd
                                + ")이 이 기본 조회 기간보다 앞서 있어 조회할 수 없습니다. 시작일을 함께 지정해 주세요."));

        mockMvc.perform(get("/order/list").param("endDate", farPastEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("기본값(최근 30일)")))
                .andExpect(content().string(containsString("시작일을 함께 지정")))
                .andExpect(content().string(not(containsString("조회 결과가 없습니다"))))
                .andExpect(content().string(not(containsString("조건에 맞는 결과가 없습니다"))))
                // "전체 N건" 라벨(list.html:70, <p>전체 ...) 부재 확인 — 대상 요소를 특정한 단정
                .andExpect(content().string(not(containsString("<p>전체"))));
    }

    // linked_func: FUNC-order-001 — SR-224 확정요건: startDate만 조건 있을 때 0건이면
    // "조건에 맞는 결과가 없습니다"로 구분된다 (QA 권고: startDate/endDate는 SR-209 프리셋 버튼으로
    // 주 사용 경로인데 테스트 커버리지 부재 — 추가 권장)
    // linked_tc: TC-FUNC-order-001-25
    @Test
    void orderList_emptyResultWithStartDateFilter_showsConditionSpecificMessage() throws Exception {
        LocalDate start = LocalDate.of(2026, 8, 1);
        when(orderService.list(null, null, start, null, 1, 20))
                .thenReturn(Map.of("totalCount", 0, "page", 1, "items", List.of()));

        mockMvc.perform(get("/order/list").param("startDate", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("조건에 맞는 결과가 없습니다")))
                .andExpect(content().string(not(containsString("조회 결과가 없습니다"))));
    }

    // linked_func: FUNC-order-001 — SR-224 확정요건: endDate만 조건 있을 때 0건이면
    // "조건에 맞는 결과가 없습니다"로 구분된다 (QA 권고: startDate/endDate 단독 케이스 추가)
    // linked_tc: TC-FUNC-order-001-26
    @Test
    void orderList_emptyResultWithEndDateFilter_showsConditionSpecificMessage() throws Exception {
        LocalDate end = LocalDate.of(2026, 8, 31);
        when(orderService.list(null, null, null, end, 1, 20))
                .thenReturn(Map.of("totalCount", 0, "page", 1, "items", List.of()));

        mockMvc.perform(get("/order/list").param("endDate", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("조건에 맞는 결과가 없습니다")))
                .andExpect(content().string(not(containsString("조회 결과가 없습니다"))));
    }

    // linked_func: FUNC-order-001 — SR-224 확정요건: orderState만 조건 있을 때 0건이면
    // "조건에 맞는 결과가 없습니다"로 구분된다 (AC2 조건 4개 중 orderState 단독 커버)
    // linked_tc: TC-FUNC-order-001-27
    @Test
    void orderList_emptyResultWithOrderStateFilter_showsConditionSpecificMessage() throws Exception {
        when(orderService.list(null, "CANCELED", null, null, 1, 20))
                .thenReturn(Map.of("totalCount", 0, "page", 1, "items", List.of()));

        mockMvc.perform(get("/order/list").param("orderState", "CANCELED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("조건에 맞는 결과가 없습니다")))
                .andExpect(content().string(not(containsString("조회 결과가 없습니다"))));
    }
}
