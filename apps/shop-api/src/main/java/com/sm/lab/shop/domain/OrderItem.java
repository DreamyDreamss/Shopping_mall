package com.sm.lab.shop.domain;

/** 주문 상세 라인 (ORDER_ITEMS). */
public class OrderItem {
    private String orderNo;
    private int lineNo;
    private String sku;
    private String productName;     // 조회 조인 결과
    private int qty;
    private long unitPrice;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public int getLineNo() { return lineNo; }
    public void setLineNo(int lineNo) { this.lineNo = lineNo; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(long unitPrice) { this.unitPrice = unitPrice; }
}
