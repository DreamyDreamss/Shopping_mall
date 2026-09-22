// linked_func: FUNC-member-011
// spec: docs/00_FUNC/stories/STORY-FUNC-member-011.md
package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/**
 * 회원 배송지(MEMBER_ADDRESSES) — SR-235, INF-MBR-008. {@link com.sm.lab.shop.dao.MemberAddressDao}
 * 매퍼의 {@code resultType}으로 쓰인다(MemberDao 관례와 동일하게 resultMap 없이 필드명 매칭,
 * {@code map-underscore-to-camel-case: true}).
 *
 * <p>{@code isDefault}는 이 프로젝트의 Y/N 플래그 컬럼 house 관례(예: {@code Product.saleYn},
 * {@code MemberCredential.delYn})를 그대로 따라 {@code String}("Y"/"N")으로 둔다 — boolean으로
 * 바꾸지 않는다(API 응답에도 "Y"/"N" 문자열 그대로 노출, {@code ProductControllerTest}의
 * {@code saleYn} 응답 관례와 동일).
 */
public class MemberAddress {
    private Long addressId;
    private String memberId;
    private String recipient;
    private String phone;
    private String phoneNorm;
    private String zipcode;
    private String roadAddress;
    private String detailAddress;
    private String entranceMethod;
    private String deliveryMemo;
    private String isDefault;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPhoneNorm() { return phoneNorm; }
    public void setPhoneNorm(String phoneNorm) { this.phoneNorm = phoneNorm; }
    public String getZipcode() { return zipcode; }
    public void setZipcode(String zipcode) { this.zipcode = zipcode; }
    public String getRoadAddress() { return roadAddress; }
    public void setRoadAddress(String roadAddress) { this.roadAddress = roadAddress; }
    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }
    public String getEntranceMethod() { return entranceMethod; }
    public void setEntranceMethod(String entranceMethod) { this.entranceMethod = entranceMethod; }
    public String getDeliveryMemo() { return deliveryMemo; }
    public void setDeliveryMemo(String deliveryMemo) { this.deliveryMemo = deliveryMemo; }
    public String getIsDefault() { return isDefault; }
    public void setIsDefault(String isDefault) { this.isDefault = isDefault; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
