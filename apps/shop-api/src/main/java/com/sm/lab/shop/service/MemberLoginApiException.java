// linked_func: FUNC-member-005
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md
package com.sm.lab.shop.service;

import org.springframework.http.HttpStatus;

/**
 * 로그인 API(SR-232, INF-MBR-003)의 계약 위반을 code/message로 실어 나르는 예외.
 * {@link com.sm.lab.shop.web.MemberLoginExceptionHandler}가 이 예외를 {@code {code, message}}
 * 봉투로 응답한다(house 패턴 — {@code MemberSignupApiException}/{@code MemberRegistrationApiException}
 * 과 같은 형태이나, 파일 경계상 그 클래스들을 재사용하지 않고 이 FUNC 전용으로 별도로 둔다).
 */
public class MemberLoginApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;
    private Long retryAfterSeconds; // 429(MBR-4291)에만 채워지는 부가 필드

    public MemberLoginApiException(HttpStatus httpStatus, String code, String message) {
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

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /** STORY 계약 — 429 MBR-4291 응답의 {@code retryAfterSeconds} 필드. */
    public MemberLoginApiException withRetryAfterSeconds(long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
        return this;
    }
}
