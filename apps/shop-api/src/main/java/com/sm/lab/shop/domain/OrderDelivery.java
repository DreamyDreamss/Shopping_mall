package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/** 주문 배송 (ORDER_DELIVERY). */
public class OrderDelivery {
    private String deliveryNo;
    private String orderNo;
    private String deliveryState;   // READY/SHIPPED/DELIVERED/CANCELED
    private String invoiceNo;
    private LocalDateTime shippedAt;

    public String getDeliveryNo() { return deliveryNo; }
    public void setDeliveryNo(String deliveryNo) { this.deliveryNo = deliveryNo; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getDeliveryState() { return deliveryState; }
    public void setDeliveryState(String deliveryState) { this.deliveryState = deliveryState; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
}
