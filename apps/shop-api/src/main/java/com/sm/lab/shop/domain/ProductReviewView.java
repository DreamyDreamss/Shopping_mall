package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/**
 * 상품평 공개 목록 조회 전용 프로젝션(SR-351(#2), INF-ORD-038) —
 * {@code ProductReviewDao#selectReviews(sku, sort, offset, limit)} 결과. 기존 {@link ProductReview}
 * 도메인에는 필드를 추가하지 않는다 — 그 클래스 Javadoc이 "이름 필드를 두지 않는다"고 명시적으로
 * 확정했다(SR-351 #1 결정).
 *
 * <p>{@code memberName}은 {@code MEMBERS} 조인으로 얻은 <b>원본</b>(마스킹 전) 이름이다 — 서비스
 * 메서드 스코프 안에서만 잠깐 존재하다 마스킹 후 버려지고, 마스킹된 값만 응답 DTO로 나간다(원본은
 * 로그에도 남기지 않는다, 확정 문답 pii_format).
 */
public class ProductReviewView {
    private Long reviewId;
    private int rating;
    private String content;
    private String memberName;
    private LocalDateTime createdAt;

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
