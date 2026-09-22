// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop.service;

import org.springframework.http.HttpStatus;

/**
 * 가입 요청 API(SR-231, INF-MBR-002)의 계약 위반을 code/message로 실어 나르는 예외.
 * {@link com.sm.lab.shop.web.MemberRegistrationExceptionHandler}가 이 예외를
 * {@code {code, message}} 봉투로 응답한다(house 패턴 — {@code MemberSignupApiException}
 * (FUNC-member-002)과 같은 형태이나, 파일 경계상 그 클래스를 재사용하지 않고 이 FUNC 전용으로
 * 별도로 둔다).
 */
public class MemberRegistrationApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;
    private String loginUrl; // STORY AC — 중복 이메일/휴대폰(MBR-4092)에만 채워지는 부가 필드

    public MemberRegistrationApiException(HttpStatus httpStatus, String code, String message) {
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

    public String getLoginUrl() {
        return loginUrl;
    }

    /** STORY AC "중복 이메일/휴대폰 거부 ... + login_url 필드" — 빌더 스타일로 체이닝. */
    public MemberRegistrationApiException withLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
        return this;
    }
}
