package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.RestockAlertApiException;
import com.sm.lab.shop.service.RestockAlertService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 재입고 알림 신청 API(SR-364 #1, INF-ORD-048) — {@code POST /api/members/me/restock-alerts}.
 * 내 재입고 알림 신청 목록 API(SR-364 #2, INF-ORD-049) — {@code GET
 * /api/members/me/restock-alerts}.
 *
 * <p><b>memberId 해석</b> — 이 경로는 URL에 memberId가 없다("me"). {@link
 * com.sm.lab.shop.web.ApiKeyAuthFilter}가 {@code ProductWishController}(SR-356 #2)와 동일한
 * SCOPE_TO_SELF 메커니즘을 재사용해(새 우회 경로 없음) member 스코프 키의 {@code memberId}
 * 파라미터를 호출자 자신으로 강제한다. admin 키로 이 경로를 호출하면 그 강제가 일어나지 않으므로,
 * {@code memberId}가 없으면 이 컨트롤러가 400 {@code RSA-4001}로 명시적으로 막는다(사람 확정 —
 * "관리자가 회원을 대신해 호출"하는 시나리오는 정의하지 않는다).
 */
@RestController
@RequestMapping("/api/members/me/restock-alerts")
public class RestockAlertController {

    private final RestockAlertService restockAlertService;

    public RestockAlertController(RestockAlertService restockAlertService) {
        this.restockAlertService = restockAlertService;
    }

    /**
     * 신청 — 200 {@code {sku, requested:true}}(신규/재신청 구분 없이 항상 같은 응답 — 확정 답변
     * "이미 신청한 상품 다시 신청은 멱등 200"). 400(sku 공백)/404(상품 없음, {@code
     * PRODUCT_NOT_FOUND})/409(품절 아님, {@code NOT_SOLD_OUT}).
     */
    @PostMapping
    public Map<String, Object> register(@RequestParam(required = false) String memberId,
                                         @RequestBody RestockAlertRequest req) {
        requireMemberId(memberId);
        restockAlertService.register(memberId, req.sku());
        return Map.of("sku", req.sku(), "requested", true);
    }

    /**
     * 목록 — 200 {@code {items:[{sku, createdAt}]}}(최근 신청 순, 0건이면 빈 배열 — {@code
     * ProductRecentlyViewedController.list}와 동일 관례). {@code MEMBER_ME_RESTOCK_ALERTS_PATH}가
     * 메서드 무관으로 이미 SCOPE_TO_SELF에 등록돼 있어 GET도 자동으로 자기 memberId로 강제된다
     * (ApiKeyAuthFilter 변경 없음 — SR-364 #1이 이미 그렇게 만들어 둠).
     */
    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String memberId) {
        requireMemberId(memberId);
        return restockAlertService.list(memberId);
    }

    /**
     * admin 키로 이 경로를 호출하면 {@code ApiKeyAuthFilter}의 SCOPE_TO_SELF 강제가 일어나지
     * 않는다(그 스코프는 evaluateMemberScope 자체를 타지 않음). 이 SR은 "관리자가 회원을 대신해
     * 호출"하는 시나리오를 정의하지 않으므로, 정의되지 않은 동작을 조용히 통과시키지 않고
     * 명시적으로 400으로 막는다(사람 확정, {@code ProductWishController.requireMemberId}와 동일 원칙).
     */
    private void requireMemberId(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new RestockAlertApiException(HttpStatus.BAD_REQUEST, "RSA-4001", "memberId가 필요합니다");
        }
    }

    public record RestockAlertRequest(String sku) { }
}
