// linked_func: FUNC-member-005
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.MemberLoginApiException;
import com.sm.lab.shop.service.MemberLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-232(FUNC-member-005) — POST /api/members/login 웹 계층 단위 테스트. 이 경로는
 * {@code ApiKeyAuthFilter} 화이트리스트(정확 일치)라 {@code X-Api-Key} 헤더 없이도 통과해야
 * 한다 — {@code AdminApiKeyTestConfig} 없이 이 슬라이스에서 직접 실증한다({@code MemberSignupControllerTest}
 * 와 동일 관례). 실 서버 라우팅·API 키 발급/리프레시 토큰·기존 키 회귀는
 * {@code ApiKeyAuthIntegrationTest}·{@code MemberLoginConcurrencyTest}에서 별도 확인한다.
 */
@WebMvcTest(MemberLoginController.class)
class MemberLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberLoginService memberLoginService;

    // linked_tc: TC-FUNC-member-005-01
    @Test
    void login_validCredentials_returns200WithoutApiKey() throws Exception {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        when(memberLoginService.login("user@example.com", "abcd1234"))
                .thenReturn(new MemberLoginService.LoginResult("M-0005", "테스터", "BRONZE",
                        "mk_abc123", "refresh-token-plain", expiresAt));

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"abcd1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value("M-0005"))
                .andExpect(jsonPath("$.memberName").value("테스터"))
                .andExpect(jsonPath("$.grade").value("BRONZE"))
                .andExpect(jsonPath("$.apiKey").value("mk_abc123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-plain"));

        verify(memberLoginService).login("user@example.com", "abcd1234");
    }

    // linked_tc: TC-FUNC-member-005-02 — 사람 확인 1 문구(사유 비노출) 그대로.
    @Test
    void login_invalidCredentials_returns401WithGenericMessage() throws Exception {
        when(memberLoginService.login("nobody@example.com", "wrongpass"))
                .thenThrow(new MemberLoginApiException(HttpStatus.UNAUTHORIZED, "MBR-4011",
                        "이메일 또는 비밀번호가 올바르지 않습니다 (1/5)"));

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MBR-4011"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다 (1/5)"));
    }

    // linked_tc: TC-FUNC-member-005-03 — 사람 확인 2(5번째 실패 자체가 429) + retryAfterSeconds 필드.
    @Test
    void login_locked_returns429WithRetryAfterSeconds() throws Exception {
        when(memberLoginService.login("locked@example.com", "whatever"))
                .thenThrow(new MemberLoginApiException(HttpStatus.TOO_MANY_REQUESTS, "MBR-4291",
                        "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요").withRetryAfterSeconds(600L));

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"locked@example.com\",\"password\":\"whatever\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("MBR-4291"))
                .andExpect(jsonPath("$.message").value("로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(600));
    }

    // linked_tc: TC-FUNC-member-005-04 — DB 계층 예외는 500 정제 봉투만(경로·SQL 미노출).
    @Test
    void login_dataAccessException_returns500WithGenericEnvelopeOnly() throws Exception {
        when(memberLoginService.login("user@example.com", "abcd1234"))
                .thenThrow(new DataAccessResourceFailureException(
                        "Deadlock found; nested exception ... D:\\lab\\target\\classes\\mapper\\member.xml ..."));

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"abcd1234\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("MBR-5000"))
                .andExpect(jsonPath("$.message").value("일시적인 오류입니다. 잠시 후 다시 시도해 주세요"));
    }
}
