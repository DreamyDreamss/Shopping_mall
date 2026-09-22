// linked_func: FUNC-member-009
// spec: docs/00_FUNC/stories/STORY-FUNC-member-009.md
package com.sm.lab.shop.service;

import org.springframework.http.HttpStatus;

/**
 * 비밀번호 재설정 확정 API(SR-234, INF-MBR-007)의 계약 위반을 code/message로 실어 나르는 예외.
 * {@link com.sm.lab.shop.web.MemberPasswordResetConfirmationExceptionHandler}가 이 예외를
 * {@code {code, message}} 봉투로 응답한다({@link MemberPasswordResetApiException}과 동일한 모양).
 *
 * <p>형제 FUNC(008)의 {@link MemberPasswordResetApiException}을 재사용하지 않고 별도 클래스를
 * 둔다 — 이 코드베이스는 밀접하게 연관된 FUNC끼리도 각자 예외 클래스를 만드는 관례다(파일 소유
 * 경계를 예외 타입에도 그대로 반영, STORY "파일" 절 참고).
 */
public class MemberPasswordResetConfirmationApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public MemberPasswordResetConfirmationApiException(HttpStatus httpStatus, String code, String message) {
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
