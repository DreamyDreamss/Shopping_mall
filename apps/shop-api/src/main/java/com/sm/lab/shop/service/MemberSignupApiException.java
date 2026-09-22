// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.service;

import org.springframework.http.HttpStatus;

/**
 * 회원가입 인증코드 API(SR-231, INF-MBR-001)의 계약 위반을 code/message로 실어 나르는 예외.
 * {@link com.sm.lab.shop.web.MemberSignupExceptionHandler}가 이 예외를
 * {@code {code, message}} 봉투로 응답한다(round1 QA FAIL 필수3/필수4 재작업 — 02_변경명세.md
 * §오류 응답 계약 표와 같은 형태로 통일. 기존 {@code ResponseStatusException}은 code 필드가
 * 없어 그 계약을 충족하지 못했다).
 */
public class MemberSignupApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public MemberSignupApiException(HttpStatus httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }
}
