// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.MemberRegistrationApiException;
import com.sm.lab.shop.service.MemberRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-231(FUNC-member-003) — POST /api/members/signup 웹 계층 단위 테스트. 이 경로는
 * {@code ApiKeyAuthFilter} 화이트리스트(정확 일치, {@code MEMBER_SIGNUP_REQUEST_PATH})라
 * {@code AdminApiKeyTestConfig} 없이도 통과해야 한다 — 무인증 자체 실증은 실 서버 라우팅까지
 * 포함하는 {@code ApiKeyAuthIntegrationTest}에서 별도 확인하고, 이 슬라이스는 응답 계약
 * (201/{code,message} 봉투)만 검증한다({@code MemberSignupControllerTest}와 동일 관례).
 */
@WebMvcTest(MemberRegistrationController.class)
class MemberRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberRegistrationService memberRegistrationService;

    // linked_tc: TC-FUNC-member-003-05
    @Test
    void signUp_valid_returns201() throws Exception {
        when(memberRegistrationService.signUp(eq("user@example.com"), eq("123456"), eq("abcd1234"),
                eq("홍길동"), eq(true)))
                .thenReturn(new MemberRegistrationService.SignupResult("M-1001", "EMAIL", "user@example.com"));

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"user@example.com\",\"code\":\"123456\","
                                + "\"password\":\"abcd1234\",\"name\":\"홍길동\",\"marketingOptIn\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value("M-1001"))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.target").value("user@example.com"));

        verify(memberRegistrationService).signUp("user@example.com", "123456", "abcd1234", "홍길동", true);
    }

    // marketingOptIn 생략 시 false로 처리(요청 record가 Boolean 박싱 타입 — null 방어).
    @Test
    void signUp_marketingOptInOmitted_defaultsToFalse() throws Exception {
        when(memberRegistrationService.signUp(eq("01012345678"), eq("123456"), eq("abcd1234"),
                eq("홍길동"), eq(false)))
                .thenReturn(new MemberRegistrationService.SignupResult("M-1002", "SMS", "01012345678"));

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"01012345678\",\"code\":\"123456\","
                                + "\"password\":\"abcd1234\",\"name\":\"홍길동\"}"))
                .andExpect(status().isCreated());

        verify(memberRegistrationService).signUp("01012345678", "123456", "abcd1234", "홍길동", false);
    }

    // linked_tc: TC-FUNC-member-003-02 — 미인증 거부는 409 {code, message} 봉투.
    @Test
    void signUp_notVerified_returns409WithCodeMessageEnvelope() throws Exception {
        when(memberRegistrationService.signUp(eq("user@example.com"), eq("000000"), eq("abcd1234"),
                eq("홍길동"), eq(false)))
                .thenThrow(new MemberRegistrationApiException(HttpStatus.CONFLICT, "MBR-4091", "인증이 필요합니다"));

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"user@example.com\",\"code\":\"000000\","
                                + "\"password\":\"abcd1234\",\"name\":\"홍길동\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MBR-4091"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다"));
    }

    // round3(SR-231 round2 QA FAIL 재작업 지시, 사람 결정) — 시도 상한(5회) 도달 후에는 409
    // MBR-4093("재발송 필요")로 구분해서 응답해야 한다(단순 오답 MBR-4091과 다른 코드).
    @Test
    void signUp_verifyAttemptsLocked_returns409WithVerifyLockedEnvelope() throws Exception {
        when(memberRegistrationService.signUp(eq("user@example.com"), eq("123456"), eq("abcd1234"),
                eq("홍길동"), eq(false)))
                .thenThrow(new MemberRegistrationApiException(HttpStatus.CONFLICT, "MBR-4093", "재발송 필요"));

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"user@example.com\",\"code\":\"123456\","
                                + "\"password\":\"abcd1234\",\"name\":\"홍길동\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MBR-4093"));
    }

    // linked_tc: TC-FUNC-member-003-03 — 중복 이메일 거부는 409 + login_url 필드 포함.
    @Test
    void signUp_duplicateEmail_returns409WithLoginUrl() throws Exception {
        when(memberRegistrationService.signUp(eq("dup@example.com"), eq("123456"), eq("abcd1234"),
                eq("홍길동"), eq(false)))
                .thenThrow(new MemberRegistrationApiException(HttpStatus.CONFLICT, "MBR-4092",
                        "이미 가입된 이메일입니다").withLoginUrl("/login"));

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"dup@example.com\",\"code\":\"123456\","
                                + "\"password\":\"abcd1234\",\"name\":\"홍길동\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MBR-4092"))
                .andExpect(jsonPath("$.login_url").value("/login"));
    }

    // round2(SR-231 round1 QA FAIL 필수2 재작업) — 휴대폰 중복은 409 + MBR-4094(login_url 없음,
    // 이메일 중복 MBR-4092와 다른 코드).
    @Test
    void signUp_duplicatePhone_returns409WithoutLoginUrl() throws Exception {
        when(memberRegistrationService.signUp(eq("01011112222"), eq("123456"), eq("abcd1234"),
                eq("홍길동"), eq(false)))
                .thenThrow(new MemberRegistrationApiException(HttpStatus.CONFLICT, "MBR-4094",
                        "이미 가입된 휴대폰번호입니다"));

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"01011112222\",\"code\":\"123456\","
                                + "\"password\":\"abcd1234\",\"name\":\"홍길동\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MBR-4094"))
                .andExpect(jsonPath("$.login_url").doesNotExist());
    }

    // linked_tc: TC-FUNC-member-003-04 — 비밀번호 규칙 위반은 400 {code, message} 봉투.
    @Test
    void signUp_invalidPassword_returns400WithCodeMessageEnvelope() throws Exception {
        when(memberRegistrationService.signUp(eq("user@example.com"), eq("123456"), eq("short"),
                eq("홍길동"), eq(false)))
                .thenThrow(new MemberRegistrationApiException(HttpStatus.BAD_REQUEST, "MBR-4001",
                        "비밀번호는 8~64자, 영문과 숫자를 포함해야 합니다"));

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"user@example.com\",\"code\":\"123456\","
                                + "\"password\":\"short\",\"name\":\"홍길동\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MBR-4001"));
    }

    // DB 계층 예외는 500 {code: MBR-5000, message: 정제 문구}로만 응답해야 한다(MemberSignupController
    // 동일 house 관례 — 경로·SQL 등 내부 정보 미노출).
    @Test
    void signUp_dataAccessException_returns500WithGenericEnvelopeOnly() throws Exception {
        when(memberRegistrationService.signUp(eq("user@example.com"), eq("123456"), eq("abcd1234"),
                eq("홍길동"), eq(false)))
                .thenThrow(new DataAccessResourceFailureException(
                        "Deadlock found; nested exception ... D:\\lab\\target\\classes\\mapper\\member.xml ..."));

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"user@example.com\",\"code\":\"123456\","
                                + "\"password\":\"abcd1234\",\"name\":\"홍길동\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("MBR-5000"))
                .andExpect(jsonPath("$.message").value("일시적인 오류입니다. 잠시 후 다시 시도해 주세요"));
    }
}
