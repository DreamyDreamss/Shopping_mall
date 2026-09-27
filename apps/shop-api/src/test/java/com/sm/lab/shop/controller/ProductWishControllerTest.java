package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.ProductWishApiException;
import com.sm.lab.shop.service.ProductWishService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-356(#2) — POST/GET/DELETE {@code /api/members/me/wishes} 웹 계층 단위 테스트({@code
 * controller-has-test} must 충족). 서비스는 mock — HTTP 계약(상태코드·코드·메시지·memberId 강제)만
 * 단언한다.
 *
 * <p>모든 member 시나리오는 {@code X-Api-Key: lab-member-0001-key}로 개별 오버라이드한다 — 기본
 * admin 헤더({@link AdminApiKeyTestConfig})로는 {@code ApiKeyAuthFilter}의 새 {@code
 * MEMBER_ME_WISHES_PATH} SCOPE_TO_SELF 분기가 전혀 걸리지 않아 이 항목의 실제 경로(필터 수정분)를
 * 검증하지 못한다({@code MemberAddressControllerTest} 클래스 javadoc 경고 재확인). admin 키 검증
 * (memberId 누락 400)만 예외로 기본 admin 헤더를 그대로 쓴다.
 */
@WebMvcTest(ProductWishController.class)
@Import(AdminApiKeyTestConfig.class)
class ProductWishControllerTest {

    private static final String MEMBER_KEY = "lab-member-0001-key";
    private static final String MEMBER_ID = "M-0001";
    private static final String BASE_URL = "/api/members/me/wishes";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductWishService productWishService;

    // linked_tc: 등록 성공 — 신규/재등록 구분 없이 항상 같은 응답 모양(멱등 200). register는
    // void라 성공 시 mock이 아무것도 던지지 않으면 그대로 통과한다.
    @Test
    void register_success_returns200WithWishedTrue() throws Exception {
        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-1001"))
                .andExpect(jsonPath("$.wished").value(true));

        verify(productWishService).register(MEMBER_ID, "SKU-1001");
    }

    // linked_tc: 등록 — sku 공백은 서비스가 400으로 거부(컨트롤러는 그대로 전달만).
    @Test
    void register_serviceThrowsValidationError_returns400() throws Exception {
        doThrow(new ProductWishApiException(HttpStatus.BAD_REQUEST, "WSH-4001", "sku: sku를 입력하세요"))
                .when(productWishService).register(MEMBER_ID, "");

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WSH-4001"));
    }

    // linked_tc: 등록 — 상품 없음은 404.
    @Test
    void register_serviceThrowsProductNotFound_returns404() throws Exception {
        doThrow(new ProductWishApiException(HttpStatus.NOT_FOUND, "WSH-4041", "상품을 찾을 수 없습니다"))
                .when(productWishService).register(MEMBER_ID, "SKU-9999");

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-9999\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WSH-4041"));
    }

    // linked_tc: 해제 — 존재 여부 무관 항상 204(서비스 mock 호출 검증만).
    @Test
    void unregister_success_returns204RegardlessOfExistence() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/SKU-1001").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isNoContent());

        verify(productWishService).unregister(MEMBER_ID, "SKU-1001");
    }

    // linked_tc: 목록 — items 봉투 파싱.
    @Test
    void list_returnsItemsEnvelope() throws Exception {
        Map<String, Object> body = Map.of(
                "items", List.of(Map.of("sku", "SKU-1001", "productName", "스탠딩 데스크", "price", 390000L,
                        "imageUrl", "/images/products/sku-1001.svg", "soldOut", false,
                        "wishedAt", LocalDateTime.now().toString())),
                "page", 1, "size", 20);
        when(productWishService.list(eq(MEMBER_ID), any())).thenReturn(body);

        mockMvc.perform(get(BASE_URL).header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sku").value("SKU-1001"))
                .andExpect(jsonPath("$.size").value(20));

        verify(productWishService).list(eq(MEMBER_ID), any());
    }

    // 사람 확인 코멘트(2) — 남의 memberId를 담아 호출해도 ApiKeyAuthFilter의 새
    // MEMBER_ME_WISHES_PATH 분기(SCOPE_TO_SELF)가 그 값을 무시하고 자기 자신으로 강제하는지
    // 명시적으로 검증한다(사례집 SR-307 #1/SR-232 r3 재발 방지 확인 — 이 경로는 "거부(403)"가
    // 아니라 "무시하고 강제 축소" 정책이다, MEMBER_ME_ADDRESSES_PATH와 동일 — STORY "순서·보안" 절).
    @Test
    void list_memberKeyWithOtherMembersIdParam_isForcedToOwnMemberIdNotAttacker() throws Exception {
        when(productWishService.list(eq(MEMBER_ID), any()))
                .thenReturn(Map.of("items", List.of(), "page", 1, "size", 20));

        mockMvc.perform(get(BASE_URL).param("memberId", "M-9999").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk());

        verify(productWishService).list(eq(MEMBER_ID), any());
        verify(productWishService, never()).list(eq("M-9999"), any());
    }

    // linked_tc: admin 키는 evaluateMemberScope를 타지 않아 memberId가 강제되지 않는다. 이
    // 컨트롤러가 명시적으로 400으로 막는다(STORY "순서·보안" 절, 사람 확정). 기본
    // AdminApiKeyTestConfig 헤더를 그대로 사용(오버라이드 없음) — MemberAddressControllerTest
    // #11과 동일 패턴.
    @Test
    void list_adminKeyWithoutMemberIdParam_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WSH-4001"));
    }
}
