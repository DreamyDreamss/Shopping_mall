package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/**
 * 최근 본 상품 목록 조회 전용 프로젝션 — {@code ProductViewHistoryDao#selectRecentlyViewed}
 * 결과(PRODUCT_VIEW_HISTORY와 PRODUCTS 조인). {@link ProductWishView}와 동일하게 응답 전용 뷰
 * 객체로 둔다.
 *
 * <p>{@code soldOut}은 {@code p.stock_qty <= 0}로 SQL에서 계산한다({@code
 * ProductService.getDetail}/{@code ProductWishView}와 동일 정의, 새 정의를 만들지 않는다).
 */
public class ProductViewHistoryView {
    private String sku;
    private String productName;
    private long price;
    private String imageUrl;
    private boolean soldOut;
    private LocalDateTime viewedAt;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isSoldOut() { return soldOut; }
    public void setSoldOut(boolean soldOut) { this.soldOut = soldOut; }
    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }
}
