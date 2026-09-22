// linked_func: FUNC-member-008
// spec: docs/00_FUNC/stories/STORY-FUNC-member-008.md
package com.sm.lab.shop.service;

import org.springframework.http.HttpStatus;

/**
 * 비밀번호 재설정 코드 요청 API(SR-234, INF-MBR-006)의 계약 위반을 code/message로 실어 나르는
 * 예외. {@link com.sm.lab.shop.web.MemberPasswordResetExceptionHandler}가 이 예외를
 * {@code {code, message}} 봉투로 응답한다({@link MemberSignupApiException}과 동일한 모양).
 */
public class MemberPasswordResetApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public MemberPasswordResetApiException(HttpStatus httpStatus, String code, String message) {
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
