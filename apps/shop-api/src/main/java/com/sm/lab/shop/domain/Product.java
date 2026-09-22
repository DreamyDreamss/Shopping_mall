package com.sm.lab.shop.domain;

/**
 * 상품 마스터 (PRODUCTS).
 *
 * <p>SR-306(#2) — listPrice(정가)·imageUrl(대표 이미지 경로)는 표시 전용 필드다. 둘 다 null을
 * 허용하고(값 없으면 화면이 감춘다), 주문·장바구니 금액 계산·재고 판정에는 쓰이지 않는다(그 쪽은
 * price/stockQty만 읽는다) — 기존 5필드는 이름·타입 불변, 새 필드는 끝에 추가한다.
 */
public class Product {
    private String sku;
    private String productName;
    private long price;
    private int stockQty;
    private String saleYn;
    private Long listPrice;
    private String imageUrl;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public int getStockQty() { return stockQty; }
    public void setStockQty(int stockQty) { this.stockQty = stockQty; }
    public String getSaleYn() { return saleYn; }
    public void setSaleYn(String saleYn) { this.saleYn = saleYn; }
    public Long getListPrice() { return listPrice; }
    public void setListPrice(Long listPrice) { this.listPrice = listPrice; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
