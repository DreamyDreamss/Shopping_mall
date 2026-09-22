// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.web;

import com.sm.lab.shop.controller.MemberSignupController;
import com.sm.lab.shop.service.MemberSignupApiException;
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
 * SR-231(FUNC-member-002) — {@link MemberSignupController}의 오류를 {@code {code, message}}
 * 봉투로 통일한다. Critical Rule(REST 예외 어드바이스는 컨트롤러 단위로 스코프 한정)에 따라
 * {@link MemberSignupController}로만 한정한다 — {@link ApiExceptionHandler}(CartController
 * 한정)·{@link OrderApiExceptionHandler}(OrderController 한정)와 동일 패턴이며 서로 간섭하지
 * 않는다.
 *
 * <p>round4(SR-231 round3 QA FAIL 필수3, STORY 재작업 지시(C)) — {@link DataAccessException}
 * 핸들러를 추가한다. round3까지는 이 컨트롤러에 DB 계층 예외 핸들러가 없어 기본 오류 경로로
 * 새어나갔고, {@code server.error.include-message: always}(application.yml)와 결합돼 서버
 * 절대경로·매퍼 파일명·SQL 전문·JDBC 커넥션 번호가 응답 본문에 그대로 노출됐다(round3 QA FAIL
 * Layer2 차단). 이제 500 응답은 항상 정제된 {@code {code: "MBR-5000", message: "..."}}
 * 뿐이고, 원본 예외는 서버 로그에만 남는다({@code docs/project-context.md} Critical Rule 2
 * 그대로 — {@link ApiExceptionHandler#handleDataAccess}와 동일 패턴).
 */
@RestControllerAdvice(assignableTypes = MemberSignupController.class)
public class MemberSignupExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(MemberSignupExceptionHandler.class);

    @ExceptionHandler(MemberSignupApiException.class)
    public ResponseEntity<Map<String, Object>> handleMemberSignupApiException(MemberSignupApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    /**
     * round4(STORY 재작업 지시(C)) — 경로·SQL·커넥션 정보를 응답 본문에 절대 싣지 않는다.
     * 원본은 이 로그 한 줄에만 남긴다(무인증 엔드포인트라 정보 노출 위험이 특히 크다).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        log.error("회원가입 인증코드 API — DB 계층 예외, 500(정제 메시지)로 응답. 원본은 이 로그에만", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "MBR-5000");
        body.put("message", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
