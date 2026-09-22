// linked_func: FUNC-member-011
// spec: docs/00_FUNC/stories/STORY-FUNC-member-011.md
package com.sm.lab.shop.web;

import com.sm.lab.shop.controller.MemberAddressController;
import com.sm.lab.shop.service.MemberAddressApiException;
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
 * SR-235(FUNC-member-011) — {@link MemberAddressController}의 오류를 {@code {code, message}}
 * 봉투로 통일한다. project-context.md Critical Rule 2(REST 예외 어드바이스는 컨트롤러 스코프로
 * 한정)에 따라 {@link MemberAddressController}로만 한정한다.
 */
@RestControllerAdvice(assignableTypes = MemberAddressController.class)
public class MemberAddressExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(MemberAddressExceptionHandler.class);

    @ExceptionHandler(MemberAddressApiException.class)
    public ResponseEntity<Map<String, Object>> handleMemberAddressApiException(MemberAddressApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    /**
     * 경로·SQL·커넥션 정보를 응답 본문에 절대 싣지 않는다({@code MemberSessionExceptionHandler}
     * #handleDataAccessException과 동일 패턴, 동일 코드·문구 재사용 — 신규 코드 아님). 원본은
     * 이 로그 한 줄에만 남긴다. 로그에는 주소 본문·연락처를 남기지 않는다(사람 확정 — addressId·
     * memberId만).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        log.error("배송지 API — DB 계층 예외, 500(정제 메시지)로 응답. 원본은 이 로그에만", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "MBR-5000");
        body.put("message", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
