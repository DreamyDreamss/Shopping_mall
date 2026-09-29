package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.ProductCompareApiException;
import com.sm.lab.shop.service.ProductCompareService;
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

import static org.hamcrest.Matchers.hasSize;
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
 * SR-360(#2) — POST/GET/DELETE {@code /api/members/me/compares} 웹 계층 단위 테스트({@code
 * controller-has-test} must 충족). 서비스는 mock — HTTP 계약(상태코드·코드·메시지·memberId 강제)만
 * 단언한다.
 *
 * <p>모든 member 시나리오는 {@code X-Api-Key: lab-member-0001-key}로 개별 오버라이드한다 — 기본
 * admin 헤더({@link AdminApiKeyTestConfig})로는 {@code ApiKeyAuthFilter}의 새 {@code
 * MEMBER_ME_COMPARES_PATH} SCOPE_TO_SELF 분기가 전혀 걸리지 않아 이 항목의 실제 경로(필터 수정분)를
 * 검증하지 못한다({@code ProductWishControllerTest} 관례 재확인). admin 키 검증(memberId 누락 400)만
 * 예외로 기본 admin 헤더를 그대로 쓴다.
 */
@WebMvcTest(ProductCompareController.class)
@Import(AdminApiKeyTestConfig.class)
class ProductCompareControllerTest {

    private static final String MEMBER_KEY = "lab-member-0001-key";
    private static final String MEMBER_ID = "M-0001";
    private static final String BASE_URL = "/api/members/me/compares";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductCompareService productCompareService;

    // linked_tc: 담기 성공 — 신규/재담기 구분 없이 항상 같은 응답 모양(멱등 200). add는 void라
    // 성공 시 mock이 아무것도 던지지 않으면 그대로 통과한다.
    @Test
    void add_success_returns200WithComparedTrue() throws Exception {
        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-1001"))
                .andExpect(jsonPath("$.compared").value(true));

        verify(productCompareService).add(MEMBER_ID, "SKU-1001");
    }

    // linked_tc: 담기 — sku 공백은 서비스가 400으로 거부(컨트롤러는 그대로 전달만).
    @Test
    void add_serviceThrowsValidationError_returns400() throws Exception {
        doThrow(new ProductCompareApiException(HttpStatus.BAD_REQUEST, "CMP-4001", "sku: sku를 입력하세요"))
                .when(productCompareService).add(MEMBER_ID, "");

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CMP-4001"));
    }

    // linked_tc: 담기 — 상품 없음은 404.
    @Test
    void add_serviceThrowsProductNotFound_returns404() throws Exception {
        doThrow(new ProductCompareApiException(HttpStatus.NOT_FOUND, "CMP-4041", "상품을 찾을 수 없습니다"))
                .when(productCompareService).add(MEMBER_ID, "SKU-9999");

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-9999\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CMP-4041"));
    }

    // linked_tc: 담기 — 5번째(상한 초과)는 409.
    @Test
    void add_serviceThrowsLimitExceeded_returns409() throws Exception {
        doThrow(new ProductCompareApiException(HttpStatus.CONFLICT, "CMP-4091", "비교함은 최대 4개까지 담을 수 있습니다"))
                .when(productCompareService).add(MEMBER_ID, "SKU-1005");

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1005\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CMP-4091"));
    }

    // linked_tc: 빼기 — 존재 여부 무관 항상 204(서비스 mock 호출 검증만).
    @Test
    void remove_success_returns204RegardlessOfExistence() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/SKU-1001").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isNoContent());

        verify(productCompareService).remove(MEMBER_ID, "SKU-1001");
    }

    // linked_tc: 목록 — items 봉투 파싱, page/size 필드가 없음을 확인(AC 명시 "페이지 없음").
    @Test
    void list_returnsItemsEnvelopeWithoutPageFields() throws Exception {
        Map<String, Object> body = Map.of(
                "items", List.of(Map.of("sku", "SKU-1001", "productName", "스탠딩 데스크",
                        "imageUrl", "/images/products/sku-1001.svg", "price", 390000L, "listPrice", 450000L,
                        "stockQty", 12, "soldOut", false,
                        "comparedAt", LocalDateTime.now().toString())));
        when(productCompareService.list(MEMBER_ID)).thenReturn(body);

        mockMvc.perform(get(BASE_URL).header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sku").value("SKU-1001"))
                .andExpect(jsonPath("$.page").doesNotExist())
                .andExpect(jsonPath("$.size").doesNotExist());

        verify(productCompareService).list(MEMBER_ID);
    }

    // SR-363.1 — 담은 상품이 하나도 없을 때 200 + items가 실제 빈 배열임을 명시 단언(사례집
    // SR-306 #2 — doesNotExist() 같은 판별력 없는 매처를 items 자체에는 쓰지 않는다, isArray()+
    // hasSize(0)로 빈 배열 그 자체를 증명). page/size 부재는 원래도 "없음"만 확인하면 충분해
    // doesNotExist()를 그대로 쓴다.
    @Test
    void list_emptyCompareBox_returns200WithEmptyItemsArray() throws Exception {
        when(productCompareService.list(MEMBER_ID)).thenReturn(Map.of("items", List.of()));

        mockMvc.perform(get(BASE_URL).header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.page").doesNotExist())
                .andExpect(jsonPath("$.size").doesNotExist());

        verify(productCompareService).list(MEMBER_ID);
    }

    // 사례집 SR-307 #1/SR-232 r3/SR-356 #2 재발 방지 확인 — 남의 memberId를 담아 호출해도
    // ApiKeyAuthFilter의 새 MEMBER_ME_COMPARES_PATH 분기(SCOPE_TO_SELF)가 그 값을 무시하고 자기
    // 자신으로 강제하는지 명시적으로 검증한다(ProductWishControllerTest #6과 동일 케이스 — 이 경로는
    // "거부(403)"가 아니라 "무시하고 강제 축소" 정책이다).
    @Test
    void list_memberKeyWithOtherMembersIdParam_isForcedToOwnMemberIdNotAttacker() throws Exception {
        when(productCompareService.list(MEMBER_ID)).thenReturn(Map.of("items", List.of()));

        mockMvc.perform(get(BASE_URL).param("memberId", "M-9999").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk());

        verify(productCompareService).list(MEMBER_ID);
        verify(productCompareService, never()).list("M-9999");
    }

    // linked_tc: admin 키는 evaluateMemberScope를 타지 않아 memberId가 강제되지 않는다. 이
    // 컨트롤러가 명시적으로 400으로 막는다(STORY "순서·보안" 절, 사람 확정). 기본
    // AdminApiKeyTestConfig 헤더를 그대로 사용(오버라이드 없음) — ProductWishControllerTest와
    // 동일 패턴.
    @Test
    void add_adminKeyWithoutMemberIdParam_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CMP-4001"));
    }
}
