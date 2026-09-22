// linked_func: FUNC-order-011
// spec: docs/00_FUNC/stories/STORY-FUNC-order-011.md
package com.sm.lab.shop.web;

import com.sm.lab.shop.controller.CartController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * D3 오류 계약(400/404/409) 밖으로 내부 예외·DB 스키마가 새는 것을 차단한다
 * (SR-202 QA FAIL r1 필수2 → r2 재작업 지시 #2, r3에서 스코프·의미 정정).
 *
 * r2 최초본은 selector 없는 전역 advice로 {@link DataAccessException} 전건을 409로 매핑했다 —
 * 서버 장애까지 클라이언트 귀책(409)으로 표기하고 기존 order/product 컨트롤러에도 파급됐다
 * (QA r2 지적 — r1이 잡은 "장애 위장"의 재판). r3 정정:
 * <ul>
 *   <li>스코프를 {@link CartController}로 한정(assignableTypes) — 타 컨트롤러 동작 불변.</li>
 *   <li>{@link DuplicateKeyException}(동시 담기 PK 경합 — 클라이언트 재시도로 해소 가능)만 409.</li>
 *   <li>그 외 DB 계층 예외는 정직하게 500 — 단 본문은 정제 메시지만(SQL·제약명·스택 미노출,
 *       원본은 서버 로그).</li>
 * </ul>
 * {@code ResponseStatusException}(400/404/409 사유 전달)은 이 클래스가 건드리지 않는다.
 */
@RestControllerAdvice(assignableTypes = CartController.class)
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateKey(DuplicateKeyException ex) {
        log.warn("동시 담기 PK 경합 — 409로 응답(본문에 내부 정보 미노출)", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "동시 요청이 겹쳤습니다. 다시 시도하세요"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex) {
        log.error("DB 계층 예외 — 500(정제 메시지)로 응답, 원본은 로그로만", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "요청을 처리할 수 없습니다(잠시 후 다시 시도하세요)"));
    }
}
