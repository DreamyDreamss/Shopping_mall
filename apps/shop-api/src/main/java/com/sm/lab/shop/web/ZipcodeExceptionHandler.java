// linked_func: FUNC-member-012
// spec: docs/00_FUNC/stories/STORY-FUNC-member-012.md
package com.sm.lab.shop.web;

import com.sm.lab.shop.controller.ZipcodeController;
import com.sm.lab.shop.service.ZipcodeApiException;
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
 * SR-235(FUNC-member-012) — {@link ZipcodeController}의 오류를 {@code {code, message}} 봉투로
 * 통일한다. project-context.md Critical Rule 2(REST 예외 어드바이스는 컨트롤러 스코프로 한정)에
 * 따라 {@link ZipcodeController}로만 한정한다.
 */
@RestControllerAdvice(assignableTypes = ZipcodeController.class)
public class ZipcodeExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ZipcodeExceptionHandler.class);

    @ExceptionHandler(ZipcodeApiException.class)
    public ResponseEntity<Map<String, Object>> handleZipcodeApiException(ZipcodeApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    /**
     * 경로·SQL·커넥션 정보를 응답 본문에 절대 싣지 않는다. 원본은 이 로그 한 줄에만 남긴다.
     * {@code MBR-5000}은 {@code MemberAddressExceptionHandler}와 동일 코드·문구 재사용(신규
     * 코드 아님).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        log.error("우편번호 검색 API — DB 계층 예외, 500(정제 메시지)로 응답. 원본은 이 로그에만", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "MBR-5000");
        body.put("message", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
