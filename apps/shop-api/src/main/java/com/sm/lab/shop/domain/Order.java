package com.sm.lab.shop.domain;

import java.time.LocalDateTime;
import java.util.List;

/** 주문 마스터 (ORDERS). */
public class Order {
    private String orderNo;
    private String memberId;
    private String memberName;      // 조회 조인 결과
    private String orderState;      // PLACED/PAID/SHIPPED/PARTIAL_SHIPPED/CANCELED/DONE
    private long totalAmount;
    private LocalDateTime orderedAt;
    private List<OrderItem> items;
    private List<OrderDelivery> deliveries;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getOrderState() { return orderState; }
    public void setOrderState(String orderState) { this.orderState = orderState; }
    public long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getOrderedAt() { return orderedAt; }
    public void setOrderedAt(LocalDateTime orderedAt) { this.orderedAt = orderedAt; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public List<OrderDelivery> getDeliveries() { return deliveries; }
    public void setDeliveries(List<OrderDelivery> deliveries) { this.deliveries = deliveries; }
}
