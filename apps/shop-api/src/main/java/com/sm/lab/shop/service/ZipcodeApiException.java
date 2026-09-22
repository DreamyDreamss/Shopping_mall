// linked_func: FUNC-member-012
// spec: docs/00_FUNC/stories/STORY-FUNC-member-012.md
package com.sm.lab.shop.service;

import org.springframework.http.HttpStatus;

/**
 * 우편번호(도로명) 검색 API(SR-235, INF-MBR-009)의 계약 위반을 code/message로 실어 나르는 예외.
 * {@link com.sm.lab.shop.web.ZipcodeExceptionHandler}가 이 예외를 {@code {code, message}} 봉투로
 * 응답한다({@code MemberAddressApiException}과 동일 house 패턴이나, 컨트롤러 스코프 advice 원칙상
 * (project-context.md Critical Rule 2) 재사용하지 않고 이 FUNC 전용으로 별도로 둔다).
 */
public class ZipcodeApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public ZipcodeApiException(HttpStatus httpStatus, String code, String message) {
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
