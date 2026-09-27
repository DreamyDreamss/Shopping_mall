package com.sm.lab.shop.service;

import org.springframework.http.HttpStatus;

/**
 * 찜 등록/해제/목록 API(SR-356 #2, INF-ORD-040)의 계약 위반을 code/message로 실어 나르는 예외.
 * {@link com.sm.lab.shop.web.ProductWishExceptionHandler}가 이 예외를 {@code {code, message}}
 * 봉투로 응답한다({@code MemberAddressApiException}과 동일 house 패턴이나, 컨트롤러 스코프 advice
 * 원칙상 그 클래스를 재사용하지 않고 이 항목 전용으로 별도로 둔다).
 */
public class ProductWishApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public ProductWishApiException(HttpStatus httpStatus, String code, String message) {
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
