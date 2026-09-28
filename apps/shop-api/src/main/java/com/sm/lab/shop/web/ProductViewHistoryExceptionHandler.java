package com.sm.lab.shop.web;

import com.sm.lab.shop.controller.ProductRecentlyViewedController;
import com.sm.lab.shop.service.ProductViewHistoryApiException;
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
 * {@link ProductRecentlyViewedController}의 오류를 {@code {code, message}} 봉투로 통일한다
 * (project-context Critical Rule — REST 예외 어드바이스는 컨트롤러 스코프로 한정, {@code
 * ProductWishExceptionHandler}와 동일 골격).
 */
@RestControllerAdvice(assignableTypes = ProductRecentlyViewedController.class)
public class ProductViewHistoryExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ProductViewHistoryExceptionHandler.class);

    @ExceptionHandler(ProductViewHistoryApiException.class)
    public ResponseEntity<Map<String, Object>> handleProductViewHistoryApiException(ProductViewHistoryApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    /**
     * 경로·SQL·커넥션 정보를 응답 본문에 절대 싣지 않는다({@code ProductWishExceptionHandler}와
     * 동일 패턴). 원본은 이 로그 한 줄에만 남긴다.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        log.error("최근 본 상품 API — DB 계층 예외, 500(정제 메시지)로 응답. 원본은 이 로그에만", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "RCV-5000");
        body.put("message", "일시적인 오류입니다. 잠시 후 다시 시도해 주세요");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
