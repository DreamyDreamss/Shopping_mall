package com.sm.lab.shop.domain;

import java.math.BigDecimal;

/**
 * 상품평 집계 단일 쿼리 결과(SR-351(#2)) — {@code ProductReviewDao#selectRatingSummary(sku)}
 * 전용 프로젝션. {@code ProductService.getDetail()}(INF-ORD-009)과 {@code ProductReviewService.list()}
 * (INF-ORD-038) 양쪽이 이 하나의 조회를 공유한다(STORY "데이터" 절 — 집계 쿼리 중복 금지).
 *
 * <p>{@code rating1Count}~{@code rating5Count}(별점 분포 원시값)의 {@code distribution}
 * {@code Map} 조립은 이 도메인이 아니라 서비스 계층 책임이다(전송 형태를 도메인이 모르게
 * 관심사를 분리 — STORY "파일" 절). 리뷰 0건이어도 이 레코드는 {@code avgRating=null}·
 * {@code reviewCount=0}·{@code rating*Count=0}(전부 0 이상, null 아님)인 1행으로 항상 반환된다
 * (매퍼의 {@code COALESCE} 가드 — STORY "데이터" 절 "SUM(CASE...)의 공허집합 NULL" 함정 참고).
 */
public class ProductReviewSummary {
    private BigDecimal avgRating;
    private int reviewCount;
    private int rating1Count;
    private int rating2Count;
    private int rating3Count;
    private int rating4Count;
    private int rating5Count;

    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public int getRating1Count() { return rating1Count; }
    public void setRating1Count(int rating1Count) { this.rating1Count = rating1Count; }
    public int getRating2Count() { return rating2Count; }
    public void setRating2Count(int rating2Count) { this.rating2Count = rating2Count; }
    public int getRating3Count() { return rating3Count; }
    public void setRating3Count(int rating3Count) { this.rating3Count = rating3Count; }
    public int getRating4Count() { return rating4Count; }
    public void setRating4Count(int rating4Count) { this.rating4Count = rating4Count; }
    public int getRating5Count() { return rating5Count; }
    public void setRating5Count(int rating5Count) { this.rating5Count = rating5Count; }
}
