package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/**
 * 상품 비교함 목록 조회 전용 프로젝션(SR-360 #2, INF-ORD-047) — {@code
 * ProductCompareDao#selectCompareList} 결과(PRODUCT_COMPARES와 PRODUCTS 조인). {@link
 * ProductWishView} 관례와 동일하게 응답 전용 뷰 객체로 둔다(도메인 엔티티와 분리). {@code
 * avgRating}/{@code reviewCount}는 이 뷰에 넣지 않는다 — 서비스가 {@code ProductReviewDao}로
 * 별도 병합한다(STORY "구현 계획" 절 — 집계 쿼리 중복 금지).
 *
 * <p>{@code soldOut}은 {@code p.stock_qty <= 0}로 SQL에서 계산한다({@code
 * ProductService.getDetail}/{@code ProductWishView}와 동일 정의, 새 정의를 만들지 않는다).
 */
public class ProductCompareView {
    private String sku;
    private String productName;
    private long price;
    private Long listPrice;
    private String imageUrl;
    private int stockQty;
    private boolean soldOut;
    private LocalDateTime comparedAt;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public Long getListPrice() { return listPrice; }
    public void setListPrice(Long listPrice) { this.listPrice = listPrice; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getStockQty() { return stockQty; }
    public void setStockQty(int stockQty) { this.stockQty = stockQty; }
    public boolean isSoldOut() { return soldOut; }
    public void setSoldOut(boolean soldOut) { this.soldOut = soldOut; }
    public LocalDateTime getComparedAt() { return comparedAt; }
    public void setComparedAt(LocalDateTime comparedAt) { this.comparedAt = comparedAt; }
}
