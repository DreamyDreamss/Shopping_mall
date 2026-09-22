// linked_func: FUNC-order-001
// spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Order;
import com.sm.lab.shop.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 운영 화면(Thymeleaf) — sl-recon-uis 라이브 캡처 실증 대상. */
@Controller
public class OrderViewController {
    private static final Logger log = LoggerFactory.getLogger(OrderViewController.class);

    private final OrderService orderService;

    public OrderViewController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 주문 목록 화면 — 상태·조회기간 필터 포함. linked_func: FUNC-order-001 (LAB-101, SR-205)
     * spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
     * SR-205: startDate/endDate 입력이 GET /order/list 재요청에 memberId·orderState와 함께
     * 전송되며 AND로 결합된다. 미입력 시 기본값(최근 30일)은 OrderService.list가 적용 — 이 화면은
     * 입력창에 계산된 기본값을 자동으로 채우지 않는다(AS-IS spec에 자동채움 근거가 없어 최소가정, [미상]).
     * round2(QA r1 FAIL 필수2 재작업): OrderService.list가 유효 구간 역전 시 400(ResponseStatusException)을
     * 던질 수 있다 — cart/list.html의 checkoutError, product/detail.html의 addToCartError와 동일한
     * "기존 공통 오류 배너" 패턴을 따라 4xx만 흡수해 안내 문구로 치환한다(Critical Rule 1: 5xx는 rethrow).
     * 이 GET은 PRG 대상 POST 액션이 없어(단순 검색) redirect 없이 같은 요청 안에서 바로 렌더한다
     * (ProductViewController.loadProduct와 동일 규약).
     * SR-210(FUNC-order-001 재사용): ③ 그리드 6번째 열 "배송상태" — INF-ORD-003 응답 계약은
     * 바꾸지 않고(확정 문답 scope_freeze, api_compat), 조회된 주문 목록의 주문번호로
     * orderService.latestDeliveryStatesByOrderNo를 추가 호출해 화면 전용 맵으로 별도 조합한다.
     */
    @GetMapping("/order/list")
    public String orderList(@RequestParam(required = false) String memberId,
                            @RequestParam(required = false) String orderState,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                            @RequestParam(defaultValue = "1") int page, Model model) {
        Map<String, Object> result;
        String searchError = null;
        try {
            result = orderService.list(memberId, orderState, startDate, endDate, page, 20);
        } catch (ResponseStatusException e) {
            if (!e.getStatusCode().is4xxClientError()) {
                throw e;
            }
            log.debug("주문 목록 조회 거부(4xx) 흡수 — reason={}", e.getReason());
            searchError = e.getReason();
            result = Map.of("totalCount", 0, "page", page, "items", List.of());
        }
        @SuppressWarnings("unchecked")
        List<Order> items = (List<Order>) result.get("items");
        model.addAttribute("result", result);
        model.addAttribute("searchError", searchError);
        model.addAttribute("memberId", memberId == null ? "" : memberId);
        model.addAttribute("orderState", orderState == null ? "" : orderState);
        model.addAttribute("startDate", startDate == null ? "" : startDate.toString());
        model.addAttribute("endDate", endDate == null ? "" : endDate.toString());
        // linked_func: FUNC-order-001 — SR-210: 배송상태 열 값(화면 조회 경로 전용 조합)
        model.addAttribute("deliveryStatusByOrderNo", orderService.latestDeliveryStatesByOrderNo(items));
        return "order/list";
    }

    /** 주문 상세 화면. */
    @GetMapping("/order/{orderNo}")
    public String orderDetail(@PathVariable String orderNo, Model model) {
        model.addAttribute("order", orderService.detail(orderNo));
        return "order/detail";
    }
}
