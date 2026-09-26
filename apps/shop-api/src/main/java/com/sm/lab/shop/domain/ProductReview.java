package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/**
 * 상품평(PRODUCT_REVIEWS) — SR-351(#1). 작성자 이름은 이 테이블에 없다(확정 문답 pii_format —
 * member_id만 저장하고, 마스킹된 이름은 조회 시 서버가 MEMBERS 조인 후 계산한다. 그 조회 경로는
 * 이 항목 범위 밖(SR-351 #2)이라 이 도메인에도 이름 필드를 두지 않는다).
 *
 * <p>{@code delYn}은 API 응답에는 절대 노출하지 않는다 — {@link com.sm.lab.shop.dao.ProductReviewDao#selectById}
 * 가 삭제 판정(0행일 때의 원인 재조회) 전용으로만 채운다.
 */
public class ProductReview {
    private Long reviewId;
    private String orderNo;
    private String sku;
    private String memberId;
    private int rating;
    private String content;
    private String delYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDelYn() { return delYn; }
    public void setDelYn(String delYn) { this.delYn = delYn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
