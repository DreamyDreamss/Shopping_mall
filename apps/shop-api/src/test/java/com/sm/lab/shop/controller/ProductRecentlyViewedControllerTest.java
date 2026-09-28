package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.ProductViewHistoryApiException;
import com.sm.lab.shop.service.ProductViewHistoryService;
import com.sm.lab.shop.support.AdminApiKeyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST/GET {@code /api/members/me/recently-viewed} 웹 계층 단위 테스트({@code controller-has-test}
 * must 충족). 서비스는 mock — HTTP 계약(상태코드·코드·메시지·memberId 강제)만 단언한다.
 *
 * <p>모든 member 시나리오는 {@code X-Api-Key: lab-member-0001-key}로 개별 오버라이드한다 — 기본
 * admin 헤더({@link AdminApiKeyTestConfig})로는 {@code ApiKeyAuthFilter}의 새 {@code
 * MEMBER_ME_RECENTLY_VIEWED_PATH} SCOPE_TO_SELF 분기가 전혀 걸리지 않아 이 항목의 실제 경로
 * (필터 수정분)를 검증하지 못한다({@code ProductWishControllerTest}와 동일 경고). admin 키 검증
 * (memberId 누락 400)만 예외로 기본 admin 헤더를 그대로 쓴다.
 */
@WebMvcTest(ProductRecentlyViewedController.class)
@Import(AdminApiKeyTestConfig.class)
class ProductRecentlyViewedControllerTest {

    private static final String MEMBER_KEY = "lab-member-0001-key";
    private static final String MEMBER_ID = "M-0001";
    private static final String BASE_URL = "/api/members/me/recently-viewed";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductViewHistoryService productViewHistoryService;

    // linked_tc: 기록 성공 — 204(본문 없음). record는 void라 mock이 아무것도 던지지 않으면 통과.
    @Test
    void record_success_returns204() throws Exception {
        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1001\"}"))
                .andExpect(status().isNoContent());

        verify(productViewHistoryService).record(MEMBER_ID, "SKU-1001");
    }

    // 사람 확인 코멘트 — 남의 memberId를 담아 호출해도 ApiKeyAuthFilter의 새
    // MEMBER_ME_RECENTLY_VIEWED_PATH 분기(SCOPE_TO_SELF)가 그 값을 무시하고 자기 자신으로
    // 강제하는지 명시적으로 검증한다(ProductWishControllerTest와 동일 확인 — "거부(403)"가 아니라
    // "무시하고 강제 축소" 정책이다).
    @Test
    void record_memberKeyWithOtherMembersIdParam_isForcedToOwnMemberIdNotAttacker() throws Exception {
        mockMvc.perform(post(BASE_URL).param("memberId", "M-9999").header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1001\"}"))
                .andExpect(status().isNoContent());

        verify(productViewHistoryService).record(MEMBER_ID, "SKU-1001");
        verify(productViewHistoryService, never()).record(eq("M-9999"), any());
    }

    // linked_tc: 기록 — sku 공백/형식 오류는 서비스가 400으로 거부(컨트롤러는 그대로 전달만).
    @Test
    void record_serviceThrowsValidationError_returns400() throws Exception {
        doThrow(new ProductViewHistoryApiException(HttpStatus.BAD_REQUEST, "RCV-4001",
                "sku: sku 형식이 올바르지 않습니다"))
                .when(productViewHistoryService).record(MEMBER_ID, "bad-sku");

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"bad-sku\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RCV-4001"));
    }

    // linked_tc: 기록 — 없는 sku는 404 PRODUCT_NOT_FOUND(AC 확정 문답이 지정한 리터럴 코드).
    @Test
    void record_serviceThrowsProductNotFound_returns404() throws Exception {
        doThrow(new ProductViewHistoryApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다"))
                .when(productViewHistoryService).record(MEMBER_ID, "SKU-9999");

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-9999\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    // linked_tc: admin 키는 evaluateMemberScope를 타지 않아 memberId가 강제되지 않는다. 이
    // 컨트롤러가 명시적으로 400으로 막는다(사람 확정, ProductWishControllerTest#11과 동일 패턴).
    @Test
    void record_adminKeyWithoutMemberIdParam_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RCV-4001"));
    }

    // linked_tc: 목록 — items 봉투 파싱, 항목당 필드 존재.
    @Test
    void list_returnsItemsEnvelopeWithFields() throws Exception {
        Map<String, Object> body = Map.of(
                "items", List.of(Map.of("sku", "SKU-1001", "productName", "스탠딩 데스크", "price", 390000L,
                        "imageUrl", "/images/products/sku-1001.svg", "soldOut", false,
                        "viewedAt", LocalDateTime.now().toString())));
        when(productViewHistoryService.list(MEMBER_ID)).thenReturn(body);

        mockMvc.perform(get(BASE_URL).header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sku").value("SKU-1001"))
                .andExpect(jsonPath("$.items[0].productName").value("스탠딩 데스크"))
                .andExpect(jsonPath("$.items[0].price").value(390000))
                .andExpect(jsonPath("$.items[0].soldOut").value(false))
                .andExpect(jsonPath("$.items[0].viewedAt").exists());

        verify(productViewHistoryService).list(MEMBER_ID);
    }

    // linked_tc: 목록 0건이면 200 {items:[]}(404 아님).
    @Test
    void list_empty_returns200WithEmptyItems() throws Exception {
        when(productViewHistoryService.list(MEMBER_ID)).thenReturn(Map.of("items", List.of()));

        mockMvc.perform(get(BASE_URL).header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    // linked_tc: 목록 — memberId가 자기 자신으로 강제됨(쿼리에 타인 memberId를 섞어도 무시).
    @Test
    void list_memberKeyWithOtherMembersIdParam_isForcedToOwnMemberIdNotAttacker() throws Exception {
        when(productViewHistoryService.list(MEMBER_ID)).thenReturn(Map.of("items", List.of()));

        mockMvc.perform(get(BASE_URL).param("memberId", "M-9999").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk());

        verify(productViewHistoryService).list(MEMBER_ID);
        verify(productViewHistoryService, never()).list("M-9999");
    }

    // linked_tc: admin 키 + memberId 없음은 400.
    @Test
    void list_adminKeyWithoutMemberIdParam_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RCV-4001"));
    }
}
