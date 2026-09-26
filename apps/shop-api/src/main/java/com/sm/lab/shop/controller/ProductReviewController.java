package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.ProductReview;
import com.sm.lab.shop.service.ProductReviewApiException;
import com.sm.lab.shop.service.ProductReviewService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 상품평 작성/삭제 API(SR-351(#1), INF-ORD-037/INF-ORD-039) — {@code /api/products/{sku}/reviews}.
 *
 * <p><b>memberId 해석</b> — 이 경로는 URL에 memberId가 없는 쓰기 자원이다.
 * {@link com.sm.lab.shop.web.ApiKeyAuthFilter}가 {@code MemberAddressController}(SR-235)와 동일한
 * SCOPE_TO_SELF 메커니즘을 재사용해(POST·DELETE에만, GET은 SR-351 #2 몫) member 스코프 키의
 * {@code memberId} 파라미터를 호출자 자신으로 강제한다. admin 키로 이 경로를 호출하면 그 강제가
 * 일어나지 않으므로, {@code memberId}가 없으면 이 컨트롤러가 400 RVW-4001으로 명시적으로 막는다
 * (사람 확정 — "관리자가 특정 회원 대신 작성/삭제"하는 시나리오는 정의하지 않는다).
 */
@RestController
@RequestMapping("/api/products/{sku}/reviews")
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    public ProductReviewController(ProductReviewService productReviewService) {
        this.productReviewService = productReviewService;
    }

    /** 작성 — 201 + 생성된 상품평 본문(memberId는 응답에 넣지 않는다, STORY "계약" 절). */
    @PostMapping
    public ResponseEntity<ReviewResponse> create(@RequestParam(required = false) String memberId,
                                                  @PathVariable String sku,
                                                  @RequestBody ReviewRequest req) {
        requireMemberId(memberId);
        ProductReview created = productReviewService.create(memberId, sku, toInput(req));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    /** 삭제(소프트) — 204(본문 없음). 404(없음·이미 삭제됨·sku 불일치)/403(남의 것). */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(@RequestParam(required = false) String memberId,
                                        @PathVariable String sku, @PathVariable Long reviewId) {
        requireMemberId(memberId);
        productReviewService.delete(memberId, sku, reviewId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 공개 목록(SR-351(#2), INF-ORD-038) — 무인증(비로그인 포함 전원 조회 가능,
     * {@link #requireMemberId} 호출 없음). {@code page}는 String으로 받아 서비스가 직접
     * 파싱한다(STORY "순서·보안" 절 — Spring 기본 바인딩 실패가 이 컨트롤러의 {code,message}
     * 계약과 다른 바디로 떨어지는 것을 피하기 위함, page=abc 같은 비숫자 입력에서 실측 가능).
     */
    @GetMapping
    public Map<String, Object> list(@PathVariable String sku,
                                     @RequestParam(required = false, defaultValue = "latest") String sort,
                                     @RequestParam(required = false, defaultValue = "1") String page) {
        return productReviewService.list(sku, sort, page);
    }

    /**
     * admin 키로 이 경로를 호출하면 {@code ApiKeyAuthFilter}의 SCOPE_TO_SELF 강제가 일어나지
     * 않는다(그 스코프는 evaluateMemberScope 자체를 타지 않음). 이 SR은 "관리자가 회원을 대신해
     * 작성/삭제"하는 시나리오를 정의하지 않으므로, 정의되지 않은 동작을 조용히 통과시키지 않고
     * 명시적으로 400으로 막는다(사람 확정, {@code MemberAddressController.requireMemberId}와 동일 원칙).
     */
    private void requireMemberId(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new ProductReviewApiException(HttpStatus.BAD_REQUEST, "RVW-4001", "memberId가 필요합니다");
        }
    }

    /**
     * {@code CartService.addItem} 관례와 동일한 이유로 이 record를 서비스에 그대로 넘기지 않는다
     * (서비스가 controller 패키지를 import하지 않게 — {@code MemberAddressService} javadoc 참고).
     */
    private static ProductReviewService.ReviewInput toInput(ReviewRequest req) {
        return new ProductReviewService.ReviewInput(req.rating(), req.content(), req.orderNo());
    }

    private static ReviewResponse toResponse(ProductReview review) {
        return new ReviewResponse(review.getReviewId(), review.getSku(), review.getOrderNo(),
                review.getRating(), review.getContent(), review.getCreatedAt());
    }

    /** rating은 {@link BigDecimal}로 받는다 — 서비스가 "정수인지"를 직접 판정해 400을 던지기 위함. */
    public record ReviewRequest(BigDecimal rating, String content, String orderNo) { }

    public record ReviewResponse(Long reviewId, String sku, String orderNo, int rating, String content,
                                  LocalDateTime createdAt) { }
}
