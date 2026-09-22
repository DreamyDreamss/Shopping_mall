// linked_func: FUNC-member-009
// spec: docs/00_FUNC/stories/STORY-FUNC-member-009.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.MemberPasswordResetConfirmationApiException;
import com.sm.lab.shop.service.MemberPasswordResetConfirmationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-234(FUNC-member-009) — POST /api/members/password-resets/confirmations 웹 계층 단위
 * 테스트. 이 경로는 {@code ApiKeyAuthFilter} 화이트리스트라 {@code AdminApiKeyTestConfig} 없이도
 * X-Api-Key 헤더 없이 통과해야 한다(로그인 전 사용자가 호출하는 무인증 엔드포인트 — 사례집
 * SR-232 r3와 반대 조건, 형제 컨트롤러 {@link MemberPasswordResetControllerTest}와 동일 근거).
 * {@code @WebMvcTest}는 {@code @RestControllerAdvice}도 함께 로드하므로 {@link
 * com.sm.lab.shop.web.MemberPasswordResetConfirmationExceptionHandler}가 실제로 400/410/409/500을
 * {@code {code, message}} 봉투로 응답하는지 이 슬라이스에서 검증한다.
 */
@WebMvcTest(MemberPasswordResetConfirmationController.class)
class MemberPasswordResetConfirmationControllerTest {

    private static final String VALID_BODY = "{\"target\":\"user@example.com\",\"code\":\"123456\","
            + "\"newPassword\":\"abcd1234\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberPasswordResetConfirmationService confirmationService;

    @Test
    void confirm_valid_returns204NoContent() throws Exception {
        doNothing().when(confirmationService)
                .confirmPasswordReset("user@example.com", "123456", "abcd1234");

        mockMvc.perform(post("/api/members/password-resets/confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNoContent());

        verify(confirmationService).confirmPasswordReset("user@example.com", "123456", "abcd1234");
    }

    @Test
    void confirm_targetFormatError_returns400WithMbr4100() throws Exception {
        doThrow(new MemberPasswordResetConfirmationApiException(HttpStatus.BAD_REQUEST, "MBR-4100",
                "이메일 또는 휴대폰번호 형식이 올바르지 않습니다: bogus"))
                .when(confirmationService).confirmPasswordReset("bogus", "123456", "abcd1234");

        mockMvc.perform(post("/api/members/password-resets/confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"bogus\",\"code\":\"123456\",\"newPassword\":\"abcd1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MBR-4100"));
    }

    @Test
    void confirm_passwordFormatError_returns400WithMbr4001() throws Exception {
        doThrow(new MemberPasswordResetConfirmationApiException(HttpStatus.BAD_REQUEST, "MBR-4001",
                "비밀번호는 8~64자, 영문과 숫자를 포함해야 합니다"))
                .when(confirmationService).confirmPasswordReset("user@example.com", "123456", "short");

        mockMvc.perform(post("/api/members/password-resets/confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"user@example.com\",\"code\":\"123456\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MBR-4001"));
    }

    @Test
    void confirm_expiredCode_returns410WithMbr4101() throws Exception {
        doThrow(new MemberPasswordResetConfirmationApiException(HttpStatus.GONE, "MBR-4101",
                "인증코드가 만료되었습니다. 다시 요청해 주세요"))
                .when(confirmationService).confirmPasswordReset("user@example.com", "123456", "abcd1234");

        mockMvc.perform(post("/api/members/password-resets/confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("MBR-4101"));
    }

    @Test
    void confirm_attemptsExceeded_returns409WithMbr4103() throws Exception {
        doThrow(new MemberPasswordResetConfirmationApiException(HttpStatus.CONFLICT, "MBR-4103",
                "코드 확인 시도 횟수를 초과했습니다. 다시 요청해 주세요"))
                .when(confirmationService).confirmPasswordReset("user@example.com", "123456", "abcd1234");

        mockMvc.perform(post("/api/members/password-resets/confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MBR-4103"));
    }

    @Test
    void confirm_codeMismatch_returns409WithMbr4102() throws Exception {
        doThrow(new MemberPasswordResetConfirmationApiException(HttpStatus.CONFLICT, "MBR-4102",
                "코드가 올바르지 않습니다"))
                .when(confirmationService).confirmPasswordReset("user@example.com", "123456", "abcd1234");

        mockMvc.perform(post("/api/members/password-resets/confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MBR-4102"));
    }

    @Test
    void confirm_dataAccessException_returns500WithGenericEnvelopeOnly() throws Exception {
        doThrow(new DataAccessResourceFailureException(
                "Deadlock found when trying to get lock; nested exception is ... "
                        + "D:\\lab\\target\\classes\\mapper\\memberPasswordReset.xml ..."))
                .when(confirmationService).confirmPasswordReset("user@example.com", "123456", "abcd1234");

        mockMvc.perform(post("/api/members/password-resets/confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("MBR-5000"))
                .andExpect(jsonPath("$.message").value("일시적인 오류입니다. 잠시 후 다시 시도해 주세요"));
    }
}
