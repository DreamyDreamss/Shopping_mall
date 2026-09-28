package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.ProductViewHistoryApiException;
import com.sm.lab.shop.service.ProductViewHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 최근 본 상품 기록/조회 API — {@code /api/members/me/recently-viewed}.
 *
 * <p><b>memberId 해석</b> — 이 경로는 URL에 memberId가 없다("me"). {@link
 * com.sm.lab.shop.web.ApiKeyAuthFilter}가 {@code ProductWishController}와 동일한 SCOPE_TO_SELF
 * 메커니즘을 재사용해(새 우회 경로 없음) member 스코프 키의 {@code memberId} 파라미터를 호출자
 * 자신으로 강제한다. admin 키로 이 경로를 호출하면 그 강제가 일어나지 않으므로, {@code memberId}가
 * 없으면 이 컨트롤러가 400 {@code RCV-4001}로 명시적으로 막는다("관리자가 회원을 대신해 호출"하는
 * 시나리오는 정의하지 않는다, {@code ProductWishController}와 동일 원칙).
 */
@RestController
@RequestMapping("/api/members/me/recently-viewed")
public class ProductRecentlyViewedController {

    private final ProductViewHistoryService productViewHistoryService;

    public ProductRecentlyViewedController(ProductViewHistoryService productViewHistoryService) {
        this.productViewHistoryService = productViewHistoryService;
    }

    /** 기록 — 204(본문 없음, 부수효과성 기록 API — {@code ProductWishController.unregister}와 동일 관례). */
    @PostMapping
    public ResponseEntity<Void> record(@RequestParam(required = false) String memberId,
                                        @RequestBody RecentlyViewedRequest req) {
        requireMemberId(memberId);
        productViewHistoryService.record(memberId, req.sku());
        return ResponseEntity.noContent().build();
    }

    /** 목록 — 200 {@code {items:[...]}}(0건이면 빈 배열). */
    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String memberId) {
        requireMemberId(memberId);
        return productViewHistoryService.list(memberId);
    }

    /**
     * admin 키로 이 경로를 호출하면 {@code ApiKeyAuthFilter}의 SCOPE_TO_SELF 강제가 일어나지
     * 않는다. 정의되지 않은 대리 호출을 조용히 통과시키지 않고 명시적으로 400으로 막는다
     * ({@code ProductWishController.requireMemberId}와 동일 원칙).
     */
    private void requireMemberId(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new ProductViewHistoryApiException(HttpStatus.BAD_REQUEST, "RCV-4001", "memberId가 필요합니다");
        }
    }

    public record RecentlyViewedRequest(String sku) { }
}
