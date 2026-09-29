package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.RestockAlertApiException;
import com.sm.lab.shop.service.RestockAlertService;
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
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-364(#1) — POST {@code /api/members/me/restock-alerts} 웹 계층 단위 테스트({@code
 * controller-has-test} must 충족). 서비스는 mock — HTTP 계약(상태코드·코드·메시지·memberId 강제)만
 * 단언한다.
 *
 * <p>모든 member 시나리오는 {@code X-Api-Key: lab-member-0001-key}로 개별 오버라이드한다 — 기본
 * admin 헤더({@link AdminApiKeyTestConfig})로는 {@code ApiKeyAuthFilter}의 새 {@code
 * MEMBER_ME_RESTOCK_ALERTS_PATH} SCOPE_TO_SELF 분기가 전혀 걸리지 않아 이 항목의 실제 경로(필터
 * 수정분)를 검증하지 못한다({@code ProductWishControllerTest} 클래스 javadoc 경고 재확인). admin
 * 키 검증(memberId 누락 400)만 예외로 기본 admin 헤더를 그대로 쓴다.
 *
 * <p>무인증(키 없음) 401은 이 슬라이스에서 검증하지 않는다 — {@link AdminApiKeyTestConfig}의
 * {@code MockMvcBuilderCustomizer}가 모든 요청에 admin 키를 기본 주입해 이 컨텍스트에서는 "무키"
 * 상태를 만들 수 없다({@code AdminApiKeyTestConfig} 클래스 javadoc 명시, {@code
 * ProductWishControllerTest}도 동일한 이유로 이 케이스가 없다). 그 검증은 전용 통합 테스트
 * {@code ApiKeyAuthIntegrationTest}(TestRestTemplate, 헤더 완전 제어)의 책임이다 — default-deny
 * 자체는 이 항목이 추가한 로직이 아니라 기존 필터 동작이므로 그 스위트가 이미 커버한다.
 */
@WebMvcTest(RestockAlertController.class)
@Import(AdminApiKeyTestConfig.class)
class RestockAlertControllerTest {

    private static final String MEMBER_KEY = "lab-member-0001-key";
    private static final String MEMBER_ID = "M-0001";
    private static final String BASE_URL = "/api/members/me/restock-alerts";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestockAlertService restockAlertService;

    // linked_tc: admin 키는 evaluateMemberScope를 타지 않아 memberId가 강제되지 않는다. 이
    // 컨트롤러가 명시적으로 400으로 막는다(STORY "순서·보안" 절, 사람 확정). 기본
    // AdminApiKeyTestConfig 헤더를 그대로 사용(오버라이드 없음) — ProductWishControllerTest와
    // 동일 패턴.
    @Test
    void register_adminKeyWithoutMemberIdParam_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RSA-4001"));
    }

    // linked_tc: 등록 — 존재하지 않는 sku는 404 PRODUCT_NOT_FOUND(서비스 스텁이 예외를 던짐).
    @Test
    void register_serviceThrowsProductNotFound_returns404() throws Exception {
        doThrow(new RestockAlertApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다"))
                .when(restockAlertService).register(MEMBER_ID, "SKU-9999");

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-9999\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    // linked_tc: 등록 — 품절이 아닌 상품은 409 NOT_SOLD_OUT.
    @Test
    void register_serviceThrowsNotSoldOut_returns409() throws Exception {
        doThrow(new RestockAlertApiException(HttpStatus.CONFLICT, "NOT_SOLD_OUT", "품절 상품이 아닙니다"))
                .when(restockAlertService).register(MEMBER_ID, "SKU-1001");

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1001\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_SOLD_OUT"));
    }

    // linked_tc: 등록 성공(최초 신청) — 200 {sku, requested:true}. register는 void라 성공 시
    // mock이 아무것도 던지지 않으면 그대로 통과한다.
    @Test
    void register_success_returns200WithRequestedTrue() throws Exception {
        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-1001"))
                .andExpect(jsonPath("$.requested").value(true));

        verify(restockAlertService).register(MEMBER_ID, "SKU-1001");
    }

    // linked_tc: 이미 신청한 상품에 대한 재신청(중복 클릭 재현) — round1에서는 이 케이스가
    // register_success_returns200WithRequestedTrue와 스텁·요청·단언이 완전히 동일해 판별력이
    // 없었다(재작업 지시 2, QA CONCERNS low, 사례집 SR-306 #2·SR-302 #1 "거짓 보증 가드"와
    // 동일 계열 — 멱등 분기가 있든 없든 똑같이 통과). register가 void라 서비스 반환값으로는
    // 구분할 수 없으므로, 이 테스트는 "같은 회원·같은 sku로 두 번 연속 호출해도 컨트롤러가
    // 예외 없이 매번 동일한 200을 반환하고 서비스에 매번 위임한다"는 반복호출 성질을
    // verify(times(2))로 실측해 단발 호출(success 케이스, times(1) 암묵)과 구분한다. 신규/기존
    // 신청의 실제 값 분기(insertAlert 1행 vs 0행)는 RestockAlertServiceTest가 담당한다(AC
    // 커버리지는 그쪽에서 충족, 여기는 컨트롤러가 반복 호출에 안전한지만 본다).
    @Test
    void register_alreadyRequested_returnsSame200IdempotentlyOnRepeatedCalls() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sku\":\"SKU-1001\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sku").value("SKU-1001"))
                    .andExpect(jsonPath("$.requested").value(true));
        }

        verify(restockAlertService, times(2)).register(MEMBER_ID, "SKU-1001");
    }

    // 사람 확인 코멘트 — 남의 memberId를 담아 호출해도 ApiKeyAuthFilter의 새
    // MEMBER_ME_RESTOCK_ALERTS_PATH 분기(SCOPE_TO_SELF)가 그 값을 무시하고 자기 자신으로 강제하는지
    // 명시적으로 검증한다(사례집 SR-307 #1/SR-232 r3/SR-356 #2 재발 방지 확인 — 이 경로는
    // "거부(403)"가 아니라 "무시하고 강제 축소" 정책이다, MEMBER_ME_WISHES_PATH와 동일 — STORY
    // "순서·보안" 절).
    @Test
    void register_memberKeyWithOtherMembersIdParam_isForcedToOwnMemberIdNotAttacker() throws Exception {
        mockMvc.perform(post(BASE_URL).param("memberId", "M-9999").header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1001\"}"))
                .andExpect(status().isOk());

        verify(restockAlertService).register(MEMBER_ID, "SKU-1001");
        verify(restockAlertService, never()).register("M-9999", "SKU-1001");
    }

    // linked_tc: SR-364(#2) — 목록. admin 키는 evaluateMemberScope를 타지 않아 memberId가 강제되지
    // 않는다. register와 동일 원칙으로 400으로 막는다(STORY "순서·보안" 절, 사람 확정). 기본
    // AdminApiKeyTestConfig 헤더를 그대로 사용(오버라이드 없음).
    @Test
    void list_adminKeyWithoutMemberIdParam_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RSA-4001"));
    }

    // linked_tc: 목록 성공 — 서비스가 반환한 items를 그대로 200 본문에 실어 보내는지 구체값
    // 단언(jsonPath doesNotExist/isIn 류 금지, 사례집 SR-306 #2).
    @Test
    void list_success_returnsItemsFromServiceAsIs() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 29, 10, 0, 0);
        when(restockAlertService.list(MEMBER_ID))
                .thenReturn(Map.of("items", java.util.List.of(Map.of("sku", "SKU-1001", "createdAt", createdAt))));

        mockMvc.perform(get(BASE_URL).header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sku").value("SKU-1001"))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-09-29T10:00:00"));
    }

    // linked_tc: 남의 memberId를 담아 호출해도 SCOPE_TO_SELF가 자기 자신으로 강제하는지 확인
    // (register_memberKeyWithOtherMembersIdParam_isForcedToOwnMemberIdNotAttacker와 동일 패턴).
    @Test
    void list_memberKeyWithOtherMembersIdParam_isForcedToOwnMemberId() throws Exception {
        when(restockAlertService.list(MEMBER_ID)).thenReturn(Map.of("items", java.util.List.of()));

        mockMvc.perform(get(BASE_URL).param("memberId", "M-9999").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk());

        verify(restockAlertService).list(MEMBER_ID);
        verify(restockAlertService, never()).list("M-9999");
    }
}
