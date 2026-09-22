// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop.web;

import com.sm.lab.shop.controller.MemberRegistrationController;
import com.sm.lab.shop.service.MemberRegistrationApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SR-231(FUNC-member-003) — {@link MemberRegistrationController}의 오류를
 * {@code {code, message}} 봉투로 통일한다. project-context.md Critical Rule 2(REST 예외
 * 어드바이스는 컨트롤러 스코프로 한정)에 따라 {@link MemberRegistrationController}로만
 * 한정한다 — {@code MemberSignupExceptionHandler}(FUNC-member-002 소유, 그 컨트롤러 한정)와
 * 동일 패턴이며 서로 간섭하지 않는다.
 *
 * <p>{@link org.springframework.dao.DuplicateKeyException}은 {@link DataAccessException}의
 * 하위타입이지만 여기서는 별도 핸들러를 두지 않는다 — 이메일/휴대폰 중복은
 * {@code MemberRegistrationService}가 INSERT 시점에 직접 캐치해 409 {@code MBR-4092}(+
 * {@code login_url})로 변환한 뒤 {@link MemberRegistrationApiException}으로 다시 던지므로,
 * 이 핸들러까지 원본 DuplicateKeyException이 도달하는 경로는 정상 흐름에 없다(방어적으로만
 * 아래 DataAccessException 핸들러가 500으로 흡수한다).
 */
@RestControllerAdvice(assignableTypes = MemberRegistrationController.class)
public class MemberRegistrationExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(MemberRegistrationExceptionHandler.class);

    @ExceptionHandler(MemberRegistrationApiException.class)
    public ResponseEntity<Map<String, Object>> handleMemberRegistrationApiException(MemberRegistrationApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        // STORY AC — 중복 이메일/휴대폰(MBR-4092)에만 login_url이 채워져 있다.
        if (ex.getLoginUrl() != null) {
            body.put("login_url", ex.getLoginUrl());
        }
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    /**
     * 경로·SQL·커넥션 정보를 응답 본문에 절대 싣지 않는다(FUNC-member-002
     * MemberSignupExceptionHandler#handleDataAccessException과 동일 패턴). 원본은 이 로그
     * 한 줄에만 남긴다.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        log.error("가입 요청 API — DB 계층 예외, 500(정제 메시지)로 응답. 원본은 이 로그에만", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "MBR-5000");
        body.put("message", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
