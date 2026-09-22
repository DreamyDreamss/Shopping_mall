// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.MemberSignupApiException;
import com.sm.lab.shop.service.MemberSignupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-231(FUNC-member-002) — POST /api/members/signup/verification-codes 웹 계층 단위 테스트.
 * 이 경로는 {@code ApiKeyAuthFilter} 화이트리스트라 {@code AdminApiKeyTestConfig}(admin 키 기본
 * 주입) 없이도 X-Api-Key 헤더 없이 통과해야 한다 — 무인증 자체를 이 슬라이스에서 실증하고, 실
 * 서버 라우팅까지 포함한 회귀는 {@code ApiKeyAuthIntegrationTest}에서 별도 확인한다.
 *
 * <p>round2(round1 QA FAIL 필수3) — {@code @WebMvcTest}는 {@code @RestControllerAdvice}도 함께
 * 로드하므로, {@link com.sm.lab.shop.web.MemberSignupExceptionHandler}가 실제로 400/429를
 * {@code {code, message}} 봉투로 응답하는지 이 슬라이스에서 검증한다.
 */
@WebMvcTest(MemberSignupController.class)
class MemberSignupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberSignupService memberSignupService;

    // linked_tc: TC-FUNC-member-002-04
    @Test
    void sendVerificationCode_validEmail_returns200WithoutApiKey() throws Exception {
        when(memberSignupService.requestVerificationCode("user@example.com"))
                .thenReturn(new MemberSignupService.VerificationCodeResult("EMAIL", "user@example.com", 300));

        mockMvc.perform(post("/api/members/signup/verification-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"user@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.target").value("user@example.com"))
                .andExpect(jsonPath("$.expiresInSeconds").value(300));

        verify(memberSignupService).requestVerificationCode("user@example.com");
    }

    // linked_tc: TC-FUNC-member-002-05
    @Test
    void sendVerificationCode_invalidFormat_returns400WithCodeMessageEnvelope() throws Exception {
        when(memberSignupService.requestVerificationCode("bogus"))
                .thenThrow(new MemberSignupApiException(HttpStatus.BAD_REQUEST, "MEMBER_TARGET_INVALID",
                        "이메일 또는 휴대폰번호 형식이 올바르지 않습니다: bogus"));

        mockMvc.perform(post("/api/members/signup/verification-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"bogus\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER_TARGET_INVALID"))
                .andExpect(jsonPath("$.message").value("이메일 또는 휴대폰번호 형식이 올바르지 않습니다: bogus"));
    }

    // round2(round1 QA FAIL 필수4) — 쿨다운 위반은 429 + {code, message} 봉투로 응답한다.
    // linked_tc: TC-FUNC-member-002-09
    @Test
    void sendVerificationCode_cooldownViolation_returns429WithCodeMessageEnvelope() throws Exception {
        when(memberSignupService.requestVerificationCode("user@example.com"))
                .thenThrow(new MemberSignupApiException(HttpStatus.TOO_MANY_REQUESTS, "MEMBER_VERIFY_COOLDOWN",
                        "잠시 후 다시 시도해주세요(재발송은 60초 후 가능합니다)"));

        mockMvc.perform(post("/api/members/signup/verification-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"user@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("MEMBER_VERIFY_COOLDOWN"));
    }

    // round4(STORY 재작업 지시(C)) — DB 계층 예외는 500 {code: MBR-5000, message: 정제 문구}로만
    // 응답해야 한다. 원본 예외 메시지(여기서는 매퍼 경로를 흉내낸 문자열)가 그대로 노출되면
    // round3 QA FAIL Layer2(서버 절대경로·SQL 노출)의 재발이다.
    // linked_tc: TC-FUNC-member-002-13
    @Test
    void sendVerificationCode_dataAccessException_returns500WithGenericEnvelopeOnly() throws Exception {
        when(memberSignupService.requestVerificationCode("user@example.com"))
                .thenThrow(new DataAccessResourceFailureException(
                        "Deadlock found when trying to get lock; nested exception is ... "
                                + "D:\\lab\\target\\classes\\mapper\\memberSignupVerification.xml ..."));

        mockMvc.perform(post("/api/members/signup/verification-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"user@example.com\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("MBR-5000"))
                .andExpect(jsonPath("$.message").value("일시적인 오류입니다. 잠시 후 다시 시도해 주세요"));
    }
}
