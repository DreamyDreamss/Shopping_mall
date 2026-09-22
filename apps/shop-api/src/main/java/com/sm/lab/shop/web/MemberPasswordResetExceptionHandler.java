// linked_func: FUNC-member-008
// spec: docs/00_FUNC/stories/STORY-FUNC-member-008.md
package com.sm.lab.shop.web;

import com.sm.lab.shop.controller.MemberPasswordResetController;
import com.sm.lab.shop.service.MemberPasswordResetApiException;
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
 * SR-234(FUNC-member-008) — {@link MemberPasswordResetController}의 오류를
 * {@code {code, message}} 봉투로 통일한다. Critical Rule(REST 예외 어드바이스는 컨트롤러 단위로
 * 스코프 한정)에 따라 {@link MemberPasswordResetController}로만 한정한다({@link
 * MemberSignupExceptionHandler}와 동일 패턴이며 서로 간섭하지 않는다).
 *
 * <p>DB 계층 예외는 경로·SQL·커넥션 정보를 응답 본문에 절대 싣지 않는다 — 원본은 이 로그 한
 * 줄에만 남긴다(무인증 엔드포인트라 정보 노출 위험이 특히 크다, {@link MemberSignupExceptionHandler}
 * 패턴 그대로).
 */
@RestControllerAdvice(assignableTypes = MemberPasswordResetController.class)
public class MemberPasswordResetExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(MemberPasswordResetExceptionHandler.class);

    @ExceptionHandler(MemberPasswordResetApiException.class)
    public ResponseEntity<Map<String, Object>> handleMemberPasswordResetApiException(
            MemberPasswordResetApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        log.error("비밀번호 재설정 코드 요청 API — DB 계층 예외, 500(정제 메시지)로 응답. 원본은 이 로그에만", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "MBR-5000");
        body.put("message", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
