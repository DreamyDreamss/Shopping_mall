// linked_func: FUNC-order-011
// spec: docs/00_FUNC/stories/STORY-FUNC-order-011.md
package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/**
 * 장바구니 품목 (CART_ITEMS). PK(member_id, sku) — 같은 상품 재담기는 수량 합산(UPSERT, SR-202 D2).
 * 재고는 차감하지 않는다(담기는 보관 전용 — 02_변경명세 재고 의미).
 */
public class CartItem {
    private String memberId;
    private String sku;
    private String productName;     // 조회 조인 결과
    private long price;             // 조회 조인 결과(현재 단가)
    private int qty;
    private LocalDateTime addedAt;

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    /** 품목 합계(단가×수량) — 저장 컬럼이 아니라 조회 시점 계산값. */
    public long getLineTotal() { return price * qty; }
}
