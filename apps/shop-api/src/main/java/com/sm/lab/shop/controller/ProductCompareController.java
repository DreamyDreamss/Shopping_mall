package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.ProductCompareApiException;
import com.sm.lab.shop.service.ProductCompareService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 상품 비교함 담기/빼기/목록 API(SR-360 #2, INF-ORD-045/046/047) — {@code
 * /api/members/me/compares}.
 *
 * <p><b>memberId 해석</b> — 이 경로는 URL에 memberId가 없다("me"). {@link
 * com.sm.lab.shop.web.ApiKeyAuthFilter}가 {@code ProductWishController}(SR-356 #2)와 동일한
 * SCOPE_TO_SELF 메커니즘을 재사용해(새 우회 경로 없음) member 스코프 키의 {@code memberId}
 * 파라미터를 호출자 자신으로 강제한다. admin 키로 이 경로를 호출하면 그 강제가 일어나지 않으므로,
 * {@code memberId}가 없으면 이 컨트롤러가 400 {@code CMP-4001}로 명시적으로 막는다(사람 확정 —
 * {@code ProductWishController.requireMemberId}와 동일 원칙).
 */
@RestController
@RequestMapping("/api/members/me/compares")
public class ProductCompareController {

    private final ProductCompareService productCompareService;

    public ProductCompareController(ProductCompareService productCompareService) {
        this.productCompareService = productCompareService;
    }

    /**
     * 담기 — 200 {@code {sku, compared:true}}(신규/이미 담김 구분 없이 항상 같은 응답 — 확정 답변
     * "이미 담긴 상품 담기는 멱등 200"). 400(sku 공백)/404(상품 없음)/409(상한 초과).
     */
    @PostMapping
    public Map<String, Object> add(@RequestParam(required = false) String memberId,
                                    @RequestBody CompareRequest req) {
        requireMemberId(memberId);
        productCompareService.add(memberId, req.sku());
        return Map.of("sku", req.sku(), "compared", true);
    }

    /** 빼기 — 204(본문 없음, 존재 여부 무관 — "담기지 않은 상품 빼기도 멱등 204"). 400(memberId 누락)만. */
    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> remove(@RequestParam(required = false) String memberId,
                                        @PathVariable String sku) {
        requireMemberId(memberId);
        productCompareService.remove(memberId, sku);
        return ResponseEntity.noContent().build();
    }

    /** 목록 — 200 {@code {items}}(최대 4건, 페이지 없음 — page/size 필드를 넣지 않는다). */
    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String memberId) {
        requireMemberId(memberId);
        return productCompareService.list(memberId);
    }

    /**
     * admin 키로 이 경로를 호출하면 {@code ApiKeyAuthFilter}의 SCOPE_TO_SELF 강제가 일어나지
     * 않는다(그 스코프는 evaluateMemberScope 자체를 타지 않음). 이 SR은 "관리자가 회원을 대신해
     * 호출"하는 시나리오를 정의하지 않으므로, 정의되지 않은 동작을 조용히 통과시키지 않고
     * 명시적으로 400으로 막는다(사람 확정, {@code ProductWishController.requireMemberId}와 동일 원칙).
     */
    private void requireMemberId(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new ProductCompareApiException(HttpStatus.BAD_REQUEST, "CMP-4001", "memberId가 필요합니다");
        }
    }

    public record CompareRequest(String sku) { }
}
