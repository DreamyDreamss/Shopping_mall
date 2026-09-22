package com.sm.lab.shop.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 회원 마스터 (MEMBERS). */
public class Member {
    private String memberId;
    private String memberName;
    private String grade;           // BRONZE/SILVER/GOLD/VIP
    private String phone;
    private LocalDateTime createdAt;
    // linked_func: FUNC-order-004 — 최근 주문 5건, 주문 없으면 빈 배열(null 금지) (LAB-103)
    private List<OrderSummary> recentOrders = new ArrayList<>();

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<OrderSummary> getRecentOrders() { return recentOrders; }
    public void setRecentOrders(List<OrderSummary> recentOrders) {
        this.recentOrders = recentOrders == null ? new ArrayList<>() : recentOrders;
    }
}
