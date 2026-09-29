package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/**
 * 내 재입고 알림 신청 목록 조회 전용 프로젝션(SR-364 #2, INF-ORD-049) — {@code
 * RestockAlertDao#selectAlertList} 결과(RESTOCK_ALERTS 단독, 상품 조인 없음). {@link
 * ProductWishView} 관례와 동일하게 응답 전용 뷰 객체로 둔다(도메인 엔티티 아님).
 *
 * <p>{@code map-underscore-to-camel-case: true}(application.yml)라 {@code created_at} 컬럼이
 * 자동으로 {@code createdAt}에 매핑된다 — 별도 resultMap 불필요.
 */
public class RestockAlertView {
    private String sku;
    private LocalDateTime createdAt;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
