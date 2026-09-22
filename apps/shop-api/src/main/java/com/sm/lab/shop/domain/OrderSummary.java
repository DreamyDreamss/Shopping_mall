package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/** 회원 상세 recentOrders 요약 — 라인·배송 미포함(상세는 /api/orders/{orderNo}).
 *  linked_func: FUNC-order-004 (LAB-103) */
public class OrderSummary {
    private String orderNo;
    private String orderState;
    private long totalAmount;
    private LocalDateTime orderedAt;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getOrderState() { return orderState; }
    public void setOrderState(String orderState) { this.orderState = orderState; }
    public long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getOrderedAt() { return orderedAt; }
    public void setOrderedAt(LocalDateTime orderedAt) { this.orderedAt = orderedAt; }
}
