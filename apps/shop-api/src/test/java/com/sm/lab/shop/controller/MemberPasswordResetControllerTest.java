// linked_func: FUNC-member-008
// spec: docs/00_FUNC/stories/STORY-FUNC-member-008.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.MemberPasswordResetApiException;
import com.sm.lab.shop.service.MemberPasswordResetService;
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
 * SR-234(FUNC-member-008) — POST /api/members/password-resets/codes 웹 계층 단위 테스트.
 * 이 경로는 {@code ApiKeyAuthFilter} 화이트리스트라 {@code AdminApiKeyTestConfig} 없이도
 * X-Api-Key 헤더 없이 통과해야 한다(로그인 전 사용자가 호출하는 무인증 엔드포인트 — 사례집
 * SR-232 r3와 반대 조건: 이 컨트롤러는 인증이 필요 "없다"). {@code @WebMvcTest}는
 * {@code @RestControllerAdvice}도 함께 로드하므로 {@link
 * com.sm.lab.shop.web.MemberPasswordResetExceptionHandler}가 실제로 400/500을
 * {@code {code, message}} 봉투로 응답하는지 이 슬라이스에서 검증한다.
 */
@WebMvcTest(MemberPasswordResetController.class)
class MemberPasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberPasswordResetService memberPasswordResetService;

    @Test
    void requestCode_validEmail_returns202WithoutApiKey() throws Exception {
        when(memberPasswordResetService.requestPasswordResetCode("user@example.com"))
                .thenReturn(new MemberPasswordResetService.VerificationCodeResult("EMAIL", "user@example.com", 600));

        mockMvc.perform(post("/api/members/password-resets/codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"user@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.target").value("user@example.com"))
                .andExpect(jsonPath("$.expiresInSeconds").value(600));

        verify(memberPasswordResetService).requestPasswordResetCode("user@example.com");
    }

    @Test
    void requestCode_validPhone_returns202() throws Exception {
        when(memberPasswordResetService.requestPasswordResetCode("010-1234-5678"))
                .thenReturn(new MemberPasswordResetService.VerificationCodeResult("SMS", "01012345678", 600));

        mockMvc.perform(post("/api/members/password-resets/codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"010-1234-5678\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.channel").value("SMS"))
                .andExpect(jsonPath("$.target").value("01012345678"));
    }

    @Test
    void requestCode_invalidFormat_returns400WithCodeMessageEnvelope() throws Exception {
        when(memberPasswordResetService.requestPasswordResetCode("bogus"))
                .thenThrow(new MemberPasswordResetApiException(HttpStatus.BAD_REQUEST, "MBR-4100",
                        "이메일 또는 휴대폰번호 형식이 올바르지 않습니다: bogus"));

        mockMvc.perform(post("/api/members/password-resets/codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"bogus\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MBR-4100"))
                .andExpect(jsonPath("$.message").value("이메일 또는 휴대폰번호 형식이 올바르지 않습니다: bogus"));
    }

    // STORY "테스트" 절 — 쿨다운 위반에도 202(컨트롤러 레벨에서 429가 존재하지 않음을 양성 확인).
    // 서비스가 쿨다운 무시 시나리오를 그대로 반환하도록 스텁한다.
    @Test
    void requestCode_cooldownIgnoredByService_stillReturns202() throws Exception {
        when(memberPasswordResetService.requestPasswordResetCode("user@example.com"))
                .thenReturn(new MemberPasswordResetService.VerificationCodeResult("EMAIL", "user@example.com", 600));

        mockMvc.perform(post("/api/members/password-resets/codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"user@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    // 사람 수정 (1) — 목 서비스가 정규화된 값을 반환하도록 스텁한 뒤, 응답 바디 target이 그
    // 정규화 값 그대로 노출되는지 확인(컨트롤러는 서비스 출력을 가공하지 않음을 양성 확인).
    @Test
    void requestCode_normalizedTargetFromService_echoedAsIs() throws Exception {
        when(memberPasswordResetService.requestPasswordResetCode("Foo@Bar.COM"))
                .thenReturn(new MemberPasswordResetService.VerificationCodeResult("EMAIL", "foo@bar.com", 600));

        mockMvc.perform(post("/api/members/password-resets/codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"Foo@Bar.COM\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.target").value("foo@bar.com"));
    }

    @Test
    void requestCode_dataAccessException_returns500WithGenericEnvelopeOnly() throws Exception {
        when(memberPasswordResetService.requestPasswordResetCode("user@example.com"))
                .thenThrow(new DataAccessResourceFailureException(
                        "Deadlock found when trying to get lock; nested exception is ... "
                                + "D:\\lab\\target\\classes\\mapper\\memberPasswordReset.xml ..."));

        mockMvc.perform(post("/api/members/password-resets/codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"user@example.com\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("MBR-5000"))
                .andExpect(jsonPath("$.message").value("일시적인 오류입니다. 잠시 후 다시 시도해 주세요"));
    }
}
