// linked_func: FUNC-member-011
// spec: docs/00_FUNC/stories/STORY-FUNC-member-011.md
package com.sm.lab.shop.service;

import org.springframework.http.HttpStatus;

/**
 * 배송지 CRUD API(SR-235, INF-MBR-008)의 계약 위반을 code/message로 실어 나르는 예외.
 * {@link com.sm.lab.shop.web.MemberAddressExceptionHandler}가 이 예외를 {@code {code, message}}
 * 봉투로 응답한다({@code MemberSessionApiException}과 동일 house 패턴이나, 컨트롤러 스코프 advice
 * 원칙상 그 클래스를 재사용하지 않고 이 FUNC 전용으로 별도로 둔다).
 */
public class MemberAddressApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public MemberAddressApiException(HttpStatus httpStatus, String code, String message) {
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
