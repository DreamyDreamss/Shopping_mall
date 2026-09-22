// linked_func: FUNC-member-012
// spec: docs/00_FUNC/stories/STORY-FUNC-member-012.md
package com.sm.lab.shop.domain;

/**
 * 우편번호(도로명) 검색 결과 1건(ZIPCODES, SR-235, INF-MBR-009). {@link
 * com.sm.lab.shop.dao.ZipcodeDao} 매퍼의 {@code resultType}으로 쓰인다({@code MemberAddress}
 * 관례와 동일하게 resultMap 없이 필드명 매칭, {@code map-underscore-to-camel-case: true}).
 *
 * <p>내부 PK({@code zipcode_id})는 API 응답에 노출할 이유가 없어(STORY "계약" 절 — {@code
 * {zipcode, roadAddress, sido, sigungu}} 4필드만) 이 클래스에 아예 두지 않는다 — select하지 않는
 * 필드를 클래스에 남기면 항상 null인 유령 필드가 응답에 섞인다.
 */
public class Zipcode {
    private String zipcode;
    private String roadAddress;
    private String sido;
    private String sigungu;

    public String getZipcode() { return zipcode; }
    public void setZipcode(String zipcode) { this.zipcode = zipcode; }
    public String getRoadAddress() { return roadAddress; }
    public void setRoadAddress(String roadAddress) { this.roadAddress = roadAddress; }
    public String getSido() { return sido; }
    public void setSido(String sido) { this.sido = sido; }
    public String getSigungu() { return sigungu; }
    public void setSigungu(String sigungu) { this.sigungu = sigungu; }
}
