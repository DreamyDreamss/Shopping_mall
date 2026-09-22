// linked_func: FUNC-order-013 — SR-204 R-3
// spec: docs/00_FUNC/stories/STORY-FUNC-order-013.md
package com.sm.lab.shop.web;

import com.sm.lab.shop.controller.OrderController;
import com.sm.lab.shop.service.ExportRowLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SR-204 R-3 — export 결과가 행 상한을 초과하면 413 + 사유 JSON으로 응답한다.
 * Critical Rule #2(REST 예외 어드바이스는 컨트롤러 스코프로 한정)를 따라 {@link OrderController}
 * 로만 한정한다 — 기존 CartController 전용 {@link ApiExceptionHandler}(DuplicateKeyException/
 * DataAccessException)는 건드리지 않는다(스코프 분리 유지).
 */
@RestControllerAdvice(assignableTypes = OrderController.class)
public class OrderApiExceptionHandler {

    @ExceptionHandler(ExportRowLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleExportRowLimitExceeded(ExportRowLimitExceededException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "payload_too_large");
        body.put("limit", ex.getLimit());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }
}
