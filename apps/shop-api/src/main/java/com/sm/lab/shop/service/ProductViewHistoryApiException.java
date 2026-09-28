package com.sm.lab.shop.service;

import org.springframework.http.HttpStatus;

/**
 * 최근 본 상품 기록/조회 API의 계약 위반을 code/message로 실어 나르는 예외. {@link
 * com.sm.lab.shop.web.ProductViewHistoryExceptionHandler}가 이 예외를 {@code {code, message}}
 * 봉투로 응답한다({@code ProductWishApiException}과 동일 house 패턴).
 */
public class ProductViewHistoryApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public ProductViewHistoryApiException(HttpStatus httpStatus, String code, String message) {
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
