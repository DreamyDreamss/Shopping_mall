// linked_func: FUNC-member-005
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md
package com.sm.lab.shop.web;

import com.sm.lab.shop.controller.MemberLoginController;
import com.sm.lab.shop.service.MemberLoginApiException;
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
 * SR-232(FUNC-member-005) — {@link MemberLoginController}의 오류를 {@code {code, message}}
 * 봉투로 통일한다. project-context.md Critical Rule 2(REST 예외 어드바이스는 컨트롤러 스코프로
 * 한정)에 따라 {@link MemberLoginController}로만 한정한다 — {@code MemberRegistrationExceptionHandler}
 * (FUNC-member-003 소유)·{@code MemberSignupExceptionHandler}(FUNC-member-002 소유)와 동일
 * 패턴이며 서로 간섭하지 않는다.
 */
@RestControllerAdvice(assignableTypes = MemberLoginController.class)
public class MemberLoginExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(MemberLoginExceptionHandler.class);

    @ExceptionHandler(MemberLoginApiException.class)
    public ResponseEntity<Map<String, Object>> handleMemberLoginApiException(MemberLoginApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        // STORY 계약 — 429(MBR-4291)에만 retryAfterSeconds가 채워져 있다.
        if (ex.getRetryAfterSeconds() != null) {
            body.put("retryAfterSeconds", ex.getRetryAfterSeconds());
        }
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    /**
     * 경로·SQL·커넥션 정보를 응답 본문에 절대 싣지 않는다({@code MemberRegistrationExceptionHandler}
     * #handleDataAccessException과 동일 패턴). 원본은 이 로그 한 줄에만 남긴다(무인증 엔드포인트라
     * 정보 노출 위험이 특히 크다).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        log.error("로그인 API — DB 계층 예외, 500(정제 메시지)로 응답. 원본은 이 로그에만", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "MBR-5000");
        body.put("message", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
