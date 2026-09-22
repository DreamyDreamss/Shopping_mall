// linked_func: FUNC-member-009
// spec: docs/00_FUNC/stories/STORY-FUNC-member-009.md
package com.sm.lab.shop.web;

import com.sm.lab.shop.controller.MemberPasswordResetConfirmationController;
import com.sm.lab.shop.service.MemberPasswordResetConfirmationApiException;
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
 * SR-234(FUNC-member-009) — {@link MemberPasswordResetConfirmationController}의 오류를
 * {@code {code, message}} 봉투로 통일한다. Critical Rule(REST 예외 어드바이스는 컨트롤러 단위로
 * 스코프 한정)에 따라 이 컨트롤러로만 한정한다({@link
 * com.sm.lab.shop.web.MemberPasswordResetExceptionHandler}(008)와 동일 패턴이며 서로 간섭하지
 * 않는다 — 이 코드베이스는 밀접하게 연관된 FUNC끼리도 각자 핸들러를 만드는 관례다).
 *
 * <p>DB 계층 예외는 경로·SQL·커넥션 정보를 응답 본문에 절대 싣지 않는다 — 원본은 이 로그 한
 * 줄에만 남긴다(무인증 엔드포인트라 정보 노출 위험이 특히 크다).
 */
@RestControllerAdvice(assignableTypes = MemberPasswordResetConfirmationController.class)
public class MemberPasswordResetConfirmationExceptionHandler {
    private static final Logger log =
            LoggerFactory.getLogger(MemberPasswordResetConfirmationExceptionHandler.class);

    @ExceptionHandler(MemberPasswordResetConfirmationApiException.class)
    public ResponseEntity<Map<String, Object>> handleMemberPasswordResetConfirmationApiException(
            MemberPasswordResetConfirmationApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        log.error("비밀번호 재설정 확정 API — DB 계층 예외, 500(정제 메시지)로 응답. 원본은 이 로그에만", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "MBR-5000");
        body.put("message", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
