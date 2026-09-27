package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.ProductWishApiException;
import com.sm.lab.shop.service.ProductWishService;
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
 * 상품 찜 등록/해제/목록 API(SR-356 #2, INF-ORD-040) — {@code /api/members/me/wishes}.
 *
 * <p><b>memberId 해석</b> — 이 경로는 URL에 memberId가 없다("me"). {@link
 * com.sm.lab.shop.web.ApiKeyAuthFilter}가 {@code MemberAddressController}(SR-235)와 동일한
 * SCOPE_TO_SELF 메커니즘을 재사용해(새 우회 경로 없음) member 스코프 키의 {@code memberId}
 * 파라미터를 호출자 자신으로 강제한다. admin 키로 이 경로를 호출하면 그 강제가 일어나지 않으므로,
 * {@code memberId}가 없으면 이 컨트롤러가 400 {@code WSH-4001}로 명시적으로 막는다(사람 확정 —
 * "관리자가 회원을 대신해 호출"하는 시나리오는 정의하지 않는다).
 */
@RestController
@RequestMapping("/api/members/me/wishes")
public class ProductWishController {

    private final ProductWishService productWishService;

    public ProductWishController(ProductWishService productWishService) {
        this.productWishService = productWishService;
    }

    /**
     * 등록 — 200 {@code {sku, wished:true}}(신규/재등록 구분 없이 항상 같은 응답 — 확정 답변
     * "이미 찜한 상품 다시 찜은 멱등 200"). 400(sku 공백)/404(상품 없음).
     */
    @PostMapping
    public Map<String, Object> register(@RequestParam(required = false) String memberId,
                                         @RequestBody WishRequest req) {
        requireMemberId(memberId);
        productWishService.register(memberId, req.sku());
        return Map.of("sku", req.sku(), "wished", true);
    }

    /** 해제 — 204(본문 없음, 존재 여부 무관 — "찜 안 한 상품 해제도 멱등 204"). 400(memberId 누락)만. */
    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> unregister(@RequestParam(required = false) String memberId,
                                            @PathVariable String sku) {
        requireMemberId(memberId);
        productWishService.unregister(memberId, sku);
        return ResponseEntity.noContent().build();
    }

    /** 목록 — 200 {@code {items, page, size}}(size 고정 20). */
    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String memberId,
                                     @RequestParam(required = false, defaultValue = "1") String page) {
        requireMemberId(memberId);
        return productWishService.list(memberId, page);
    }

    /**
     * admin 키로 이 경로를 호출하면 {@code ApiKeyAuthFilter}의 SCOPE_TO_SELF 강제가 일어나지
     * 않는다(그 스코프는 evaluateMemberScope 자체를 타지 않음). 이 SR은 "관리자가 회원을 대신해
     * 호출"하는 시나리오를 정의하지 않으므로, 정의되지 않은 동작을 조용히 통과시키지 않고
     * 명시적으로 400으로 막는다(사람 확정, {@code MemberAddressController.requireMemberId}와 동일 원칙).
     */
    private void requireMemberId(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new ProductWishApiException(HttpStatus.BAD_REQUEST, "WSH-4001", "memberId가 필요합니다");
        }
    }

    public record WishRequest(String sku) { }
}
