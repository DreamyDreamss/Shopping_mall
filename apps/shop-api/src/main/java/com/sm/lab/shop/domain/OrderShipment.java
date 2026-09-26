package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/**
 * 주문 상세(INF-ORD-004) 응답의 배송 정보 배열 원소. ORDER_DELIVERY와 같은 행에서 파생되지만
 * (LAB-105.1) 택배사 코드·명을 담는 별도 도메인 클래스다 — {@link OrderDelivery}(배송 이력,
 * deliveries[])와 분리해 두어야 기존 배송 이력 응답에 신규 필드가 조용히 얹히지 않는다.
 */
public class OrderShipment {
    private String deliveryNo;
    private String carrierCode;   // 표시전용, NULL=미배정(READY 등)
    private String carrierName;   // 표시전용
    private String invoiceNo;
    private LocalDateTime shippedAt;

    public String getDeliveryNo() { return deliveryNo; }
    public void setDeliveryNo(String deliveryNo) { this.deliveryNo = deliveryNo; }
    public String getCarrierCode() { return carrierCode; }
    public void setCarrierCode(String carrierCode) { this.carrierCode = carrierCode; }
    public String getCarrierName() { return carrierName; }
    public void setCarrierName(String carrierName) { this.carrierName = carrierName; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
}
