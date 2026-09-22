// linked_func: FUNC-member-006
// spec: docs/00_FUNC/stories/STORY-FUNC-member-006.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.MemberSessionApiException;
import com.sm.lab.shop.service.MemberSessionService;
import com.sm.lab.shop.support.AdminApiKeyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-232(FUNC-member-006) — POST /api/members/sessions/{logout,refresh} 웹 계층 단위 테스트.
 * {@code controller-has-test}(must) 충족. 서비스는 mock — HTTP 계약(상태코드·코드·메시지)만
 * 단언한다. 실 필터 체인·왕복 시나리오는 {@code MemberSessionIntegrationTest}에서 확인한다.
 *
 * <p>{@code ApiKeyAuthFilter}는 {@code @WebMvcTest} 슬라이스에도 Filter 빈으로 함께 포함되고
 * logout 경로는 화이트리스트가 아니므로(인증 필요), 다른 비화이트리스트 컨트롤러 테스트
 * ({@code CartControllerTest}·{@code MemberViewControllerTest}와 동일)와 마찬가지로
 * {@link AdminApiKeyTestConfig}로 admin 키를 기본 주입해 필터를 통과시킨다 — 이 클래스는 서비스
 * mock 기반 HTTP 계약만 보므로 실제 회원 스코프 키 값은 중요하지 않다(IDOR·필터 자체 회귀는
 * {@code ApiKeyAuthIntegrationTest}/{@code MemberSessionIntegrationTest}에서 검증).
 */
@WebMvcTest(MemberSessionController.class)
@Import(AdminApiKeyTestConfig.class)
class MemberSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberSessionService memberSessionService;

    // linked_tc: TC-FUNC-member-006-01 — 헤더를 명시하지 않으면 AdminApiKeyTestConfig가 기본
    // 주입한 admin 키(lab-admin-key)가 필터를 통과해 컨트롤러까지 도달한다(그 값 그대로
    // memberSessionService.logout에 전달됨).
    @Test
    void logout_returns204NoContent() throws Exception {
        doNothing().when(memberSessionService).logout(AdminApiKeyTestConfig.ADMIN_KEY);

        mockMvc.perform(post("/api/members/sessions/logout"))
                .andExpect(status().isNoContent());

        verify(memberSessionService).logout(AdminApiKeyTestConfig.ADMIN_KEY);
    }

    // linked_tc: TC-FUNC-member-006-02
    @Test
    void refresh_validToken_returns200WithLoginResultShape() throws Exception {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        when(memberSessionService.refresh("refresh-token-plain"))
                .thenReturn(new MemberSessionService.SessionResult("M-0005", "테스터", "BRONZE",
                        "mk_rotated", "new-refresh-token-plain", expiresAt));

        mockMvc.perform(post("/api/members/sessions/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token-plain\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value("M-0005"))
                .andExpect(jsonPath("$.memberName").value("테스터"))
                .andExpect(jsonPath("$.grade").value("BRONZE"))
                .andExpect(jsonPath("$.apiKey").value("mk_rotated"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token-plain"));
    }

    // linked_tc: TC-FUNC-member-006-03 — 4갈래(미존재/만료/폐기/탈퇴) 모두 이 동일한 401로 온다.
    @Test
    void refresh_invalidToken_returns401WithGenericMessage() throws Exception {
        when(memberSessionService.refresh("bad-token"))
                .thenThrow(new MemberSessionApiException(HttpStatus.UNAUTHORIZED, "MBR-4012",
                        "유효하지 않거나 만료된 로그인 정보입니다"));

        mockMvc.perform(post("/api/members/sessions/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"bad-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MBR-4012"))
                .andExpect(jsonPath("$.message").value("유효하지 않거나 만료된 로그인 정보입니다"));
    }

    // linked_tc: TC-FUNC-member-006-04 — DB 계층 예외는 500 정제 봉투만(logout 쪽).
    @Test
    void logout_dataAccessException_returns500WithGenericEnvelopeOnly() throws Exception {
        doThrow(new DataAccessResourceFailureException(
                "Deadlock found; nested exception ... D:\\lab\\target\\classes\\mapper\\memberApiKey.xml ..."))
                .when(memberSessionService).logout(AdminApiKeyTestConfig.ADMIN_KEY);

        mockMvc.perform(post("/api/members/sessions/logout"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("MBR-5000"))
                .andExpect(jsonPath("$.message").value("일시적인 오류입니다. 잠시 후 다시 시도해 주세요"));
    }


    // linked_tc: TC-FUNC-member-006-05 — DB 계층 예외는 500 정제 봉투만(refresh 쪽).
    @Test
    void refresh_dataAccessException_returns500WithGenericEnvelopeOnly() throws Exception {
        when(memberSessionService.refresh("token-causing-db-error"))
                .thenThrow(new DataAccessResourceFailureException(
                        "Deadlock found; nested exception ... D:\\lab\\target\\classes\\mapper\\memberRefreshToken.xml ..."));

        mockMvc.perform(post("/api/members/sessions/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"token-causing-db-error\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("MBR-5000"))
                .andExpect(jsonPath("$.message").value("일시적인 오류입니다. 잠시 후 다시 시도해 주세요"));
    }
}
