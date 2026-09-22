// linked_func: FUNC-order-013 — SR-204 R-3: export 결과 1000행 초과 → 413(D13, 부분 반출 위장 금지)
// spec: docs/00_FUNC/stories/STORY-FUNC-order-013.md
package com.sm.lab.shop.service;

/**
 * OrderService#exportCsv가 CSV 조립 전에 조회 결과 건수를 판정해 상한을 초과하면 던진다.
 * {@link com.sm.lab.shop.web.OrderApiExceptionHandler}가 413 + {"error":"payload_too_large",
 * "limit":N} 로 매핑한다(정직 거부 — 부분 반출로 위장하지 않음).
 */
public class ExportRowLimitExceededException extends RuntimeException {
    private final int limit;

    public ExportRowLimitExceededException(int limit) {
        super("export 결과가 상한(" + limit + "행)을 초과했습니다");
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }
}
