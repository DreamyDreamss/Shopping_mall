package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/**
 * 찜 목록 조회 전용 프로젝션(SR-356 #2, INF-ORD-040) — {@code ProductWishDao#selectWishList}
 * 결과(PRODUCT_WISHES와 PRODUCTS 조인). {@link ProductReviewView} 관례와 동일하게 응답 전용 뷰
 * 객체로 둔다(도메인 엔티티와 분리) — 찜 자체는 별도 도메인 엔티티가 필요 없다(등록/해제가 단일
 * DML로 끝나 조회 반환값도 없음).
 *
 * <p>{@code soldOut}은 {@code p.stock_qty <= 0}로 SQL에서 계산한다({@code
 * ProductService.getDetail}과 동일 정의, 새 정의를 만들지 않는다).
 */
public class ProductWishView {
    private String sku;
    private String productName;
    private long price;
    private String imageUrl;
    private boolean soldOut;
    private LocalDateTime wishedAt;

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
    public LocalDateTime getWishedAt() { return wishedAt; }
    public void setWishedAt(LocalDateTime wishedAt) { this.wishedAt = wishedAt; }
}
