package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.ProductReview;
import com.sm.lab.shop.service.ProductReviewApiException;
import com.sm.lab.shop.service.ProductReviewService;
import com.sm.lab.shop.support.AdminApiKeyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-351(#1) — POST/DELETE {@code /api/products/{sku}/reviews} 웹 계층 단위 테스트
 * ({@code controller-has-test} must 충족). 서비스는 mock — HTTP 계약(상태코드·코드·메시지·
 * memberId 강제)만 단언한다.
 *
 * <p>모든 요청은 {@code X-Api-Key: lab-member-0001-key}로 개별 오버라이드해 member 스코프를
 * 태운다 — 기본 admin 헤더({@link AdminApiKeyTestConfig})로는 {@code ApiKeyAuthFilter}의
 * SCOPE_TO_SELF가 걸리지 않아 이 엔드포인트의 실제 경로(memberId 강제)를 검증하지 못한다
 * ({@code MemberAddressControllerTest}와 동일 관례). admin 키 검증(memberId 누락 400)만 예외로
 * 기본 admin 헤더를 그대로 쓴다.
 */
@WebMvcTest(ProductReviewController.class)
@Import(AdminApiKeyTestConfig.class)
class ProductReviewControllerTest {

    private static final String MEMBER_KEY = "lab-member-0001-key";
    private static final String MEMBER_ID = "M-0001";
    private static final String SKU = "SKU-1001";
    private static final String ORDER_NO = "20260815-0001";
    private static final String BASE_URL = "/api/products/" + SKU + "/reviews";
    private static final String VALID_REQUEST_BODY =
            "{\"rating\":5,\"content\":\"정말 좋은 상품이었습니다. 다음에도 또 구매하겠습니다.\",\"orderNo\":\"" + ORDER_NO + "\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductReviewService productReviewService;

    private static ProductReview sampleReview(Long reviewId) {
        ProductReview review = new ProductReview();
        review.setReviewId(reviewId);
        review.setOrderNo(ORDER_NO);
        review.setSku(SKU);
        review.setMemberId(MEMBER_ID);
        review.setRating(5);
        review.setContent("정말 좋은 상품이었습니다. 다음에도 또 구매하겠습니다.");
        LocalDateTime now = LocalDateTime.now();
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        return review;
    }

    @Test
    void create_success_returns201WithoutMemberId() throws Exception {
        when(productReviewService.create(eq(MEMBER_ID), eq(SKU), any(ProductReviewService.ReviewInput.class)))
                .thenReturn(sampleReview(1L));

        // round 1 QA CONCERNS 3 — jsonPath("$.memberId").doesNotExist()는 ReviewResponse에
        // memberId 필드가 애초에 없어 항상 통과하는 판별력 0 가드였다(사례집 SR-306 #2 계열).
        // 응답 키 집합을 정확히 고정한다: ReviewResponse의 6개 필드(reviewId·sku·orderNo·rating·
        // content·createdAt)를 전부 단언하고 length()==6으로 그 이상(예: memberId)이 없음을 확인한다
        // — 훗날 누군가 memberId를 추가하면 이 length 단언이 즉시 깨진다.
        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").value(1))
                .andExpect(jsonPath("$.sku").value(SKU))
                .andExpect(jsonPath("$.orderNo").value(ORDER_NO))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.length()").value(6));

        verify(productReviewService).create(eq(MEMBER_ID), eq(SKU), any(ProductReviewService.ReviewInput.class));
    }

    @Test
    void create_serviceThrowsValidationError_returns400() throws Exception {
        when(productReviewService.create(eq(MEMBER_ID), eq(SKU), any(ProductReviewService.ReviewInput.class)))
                .thenThrow(new ProductReviewApiException(HttpStatus.BAD_REQUEST, "RVW-4001",
                        "rating: 별점은 1~5 사이여야 합니다"));

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":9,\"content\":\"정말 좋은 상품이었습니다.\",\"orderNo\":\"" + ORDER_NO + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RVW-4001"));
    }

    @Test
    void create_serviceThrowsForbidden_returns403() throws Exception {
        when(productReviewService.create(eq(MEMBER_ID), eq(SKU), any(ProductReviewService.ReviewInput.class)))
                .thenThrow(new ProductReviewApiException(HttpStatus.FORBIDDEN, "RVW-4031",
                        "이 상품을 구매한 회원만 작성할 수 있습니다"));

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RVW-4031"));
    }

    @Test
    void create_serviceThrowsNotFound_returns404() throws Exception {
        when(productReviewService.create(eq(MEMBER_ID), eq(SKU), any(ProductReviewService.ReviewInput.class)))
                .thenThrow(new ProductReviewApiException(HttpStatus.NOT_FOUND, "RVW-4041",
                        "상품 또는 주문을 찾을 수 없습니다"));

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RVW-4041"));
    }

    @Test
    void create_serviceThrowsDuplicate_returns409() throws Exception {
        when(productReviewService.create(eq(MEMBER_ID), eq(SKU), any(ProductReviewService.ReviewInput.class)))
                .thenThrow(new ProductReviewApiException(HttpStatus.CONFLICT, "RVW-4091",
                        "이미 이 주문으로 작성한 상품평이 있습니다"));

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RVW-4091"));
    }

    @Test
    void delete_success_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isNoContent());

        verify(productReviewService).delete(MEMBER_ID, SKU, 1L);
    }

    @Test
    void delete_serviceThrowsNotFound_returns404() throws Exception {
        doThrow(new ProductReviewApiException(HttpStatus.NOT_FOUND, "RVW-4041", "상품평을 찾을 수 없습니다"))
                .when(productReviewService).delete(MEMBER_ID, SKU, 99L);

        mockMvc.perform(delete(BASE_URL + "/99").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RVW-4041"));
    }

    @Test
    void delete_serviceThrowsForbidden_returns403() throws Exception {
        doThrow(new ProductReviewApiException(HttpStatus.FORBIDDEN, "RVW-4031", "이 상품평을 삭제할 권한이 없습니다"))
                .when(productReviewService).delete(MEMBER_ID, SKU, 2L);

        mockMvc.perform(delete(BASE_URL + "/2").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RVW-4031"));
    }

    // admin 키는 evaluateMemberScope를 타지 않아 memberId가 강제되지 않는다. 이 컨트롤러가
    // 명시적으로 400으로 막는다(STORY "순서·보안" 절, 사람 확정). 기본 AdminApiKeyTestConfig
    // 헤더를 그대로 사용(오버라이드 없음).
    @Test
    void create_adminKeyWithoutMemberIdParam_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RVW-4001"));
    }

    @Test
    void delete_adminKeyWithoutMemberIdParam_returns400() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RVW-4001"));
    }

    // ==================== SR-351(#2) — GET /api/products/{sku}/reviews ====================
    // 실제 "무키" 상태는 이 슬라이스(AdminApiKeyTestConfig가 모든 요청에 기본 admin 키를 주입)에서는
    // 재현할 수 없다(그 설정 클래스 자체의 Javadoc — "이 커스터마이저 아래서는 무키 상태를 만들 수
    // 없다"). 무인증 공개 자체(진짜 무키 200/멤버 키 200/서브리소스 401)는 TestRestTemplate 기반
    // ApiKeyAuthIntegrationTest가 검증한다. 이 클래스는 JSON 계약(필드명·상태 코드·오류 코드)만 본다
    // — 기본 admin 헤더 그대로 사용(오버라이드 없음).

    private static Map<String, Object> sampleListBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalCount", 2);
        body.put("page", 1);
        body.put("size", 10);
        body.put("avgRating", new BigDecimal("4.5"));
        body.put("reviewCount", 2);
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("1", 0);
        distribution.put("2", 0);
        distribution.put("3", 0);
        distribution.put("4", 1);
        distribution.put("5", 1);
        body.put("distribution", distribution);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("reviewId", 3L);
        item.put("rating", 5);
        item.put("content", "정말 좋아요");
        item.put("authorName", "홍*동");
        item.put("createdAt", LocalDateTime.now().toString());
        body.put("items", List.of(item));
        return body;
    }

    @Test
    void list_valid_returns200WithExpectedFields() throws Exception {
        when(productReviewService.list(SKU, "latest", "1")).thenReturn(sampleListBody());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.avgRating").value(4.5))
                .andExpect(jsonPath("$.reviewCount").value(2))
                .andExpect(jsonPath("$.distribution.4").value(1))
                .andExpect(jsonPath("$.distribution.5").value(1))
                .andExpect(jsonPath("$.items[0].authorName").value("홍*동"));

        verify(productReviewService).list(SKU, "latest", "1");
    }

    // 리뷰 0건 — avgRating은 doesNotExist()가 아니라 nullValue()로 단언한다(사례집 SR-306 #2
    // 판별력 0 가드).
    @Test
    void list_noReviews_returnsAvgRatingNull() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalCount", 0);
        body.put("page", 1);
        body.put("size", 10);
        body.put("avgRating", null);
        body.put("reviewCount", 0);
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("1", 0);
        distribution.put("2", 0);
        distribution.put("3", 0);
        distribution.put("4", 0);
        distribution.put("5", 0);
        body.put("distribution", distribution);
        body.put("items", List.of());
        when(productReviewService.list(SKU, "latest", "1")).thenReturn(body);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgRating").value(nullValue()))
                .andExpect(jsonPath("$.reviewCount").value(0));
    }

    @Test
    void list_serviceThrowsValidationError_returns400() throws Exception {
        when(productReviewService.list(eq(SKU), any(), any()))
                .thenThrow(new ProductReviewApiException(HttpStatus.BAD_REQUEST, "RVW-4001",
                        "sort: 정렬 값은 latest/ratingDesc/ratingAsc 중 하나여야 합니다"));

        mockMvc.perform(get(BASE_URL).param("sort", "bogus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RVW-4001"));
    }

    @Test
    void list_serviceThrowsNotFound_returns404() throws Exception {
        // round 1 QA CONCERNS(재작업 지시 3) — 목록 404는 작성 API 전용 notFound()("상품 또는
        // 주문을 찾을 수 없습니다")와 분리된 전용 메시지("상품을 찾을 수 없습니다")를 쓴다(코드
        // RVW-4041은 계약대로 유지). message까지 단언해 실제 서비스 메시지 분리를 교차 확인한다.
        when(productReviewService.list(eq(SKU), any(), any()))
                .thenThrow(new ProductReviewApiException(HttpStatus.NOT_FOUND, "RVW-4041", "상품을 찾을 수 없습니다"));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RVW-4041"))
                .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다"));
    }

    // 무인증 목록 조회 컨트롤러 진입점 자체는 requireMemberId를 호출하지 않는다 — admin 기본 헤더로도
    // memberId 파라미터 없이 그대로 200(POST/DELETE의 400 RVW-4001 강제와 대비되는 회귀 확인).
    @Test
    void list_adminKeyWithoutMemberIdParam_returns200NotBlocked() throws Exception {
        when(productReviewService.list(SKU, "latest", "1")).thenReturn(sampleListBody());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }
}
