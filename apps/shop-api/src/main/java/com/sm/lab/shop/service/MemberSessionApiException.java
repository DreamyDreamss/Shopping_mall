// linked_func: FUNC-member-006
// spec: docs/00_FUNC/stories/STORY-FUNC-member-006.md
package com.sm.lab.shop.service;

import org.springframework.http.HttpStatus;

/**
 * 로그아웃·리프레시 API(SR-232, INF-MBR-004)의 계약 위반을 code/message로 실어 나르는 예외.
 * {@link com.sm.lab.shop.web.MemberSessionExceptionHandler}가 이 예외를 {@code {code, message}}
 * 봉투로 응답한다({@code MemberLoginApiException}과 동일 house 패턴이나, 컨트롤러 스코프 advice
 * 원칙상 그 클래스를 재사용하지 않고 이 FUNC 전용으로 별도로 둔다).
 *
 * <p>이 FUNC에는 429(잠금)가 없어 {@code MemberLoginApiException}의
 * {@code retryAfterSeconds} 같은 부가 필드가 필요 없다(계획 "계약" 절 — logout 204/500,
 * refresh 200/401 MBR-4012/500뿐).
 */
public class MemberSessionApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public MemberSessionApiException(HttpStatus httpStatus, String code, String message) {
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
