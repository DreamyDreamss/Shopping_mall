// linked_func: FUNC-member-005
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md
package com.sm.lab.shop.domain;

/**
 * 로그인 API(SR-232, INF-MBR-003) 인증 전용 DTO — {@code MEMBERS} 테이블의 인증에 필요한
 * 컬럼만 담는다. 공개 API 응답에 쓰이는 {@link Member}에는 {@code passwordHash}를 절대
 * 추가하지 않는다(직렬화 유출 위험) — 이 DTO는 서비스 계층 안에서만 쓰이고 컨트롤러 응답으로
 * 직접 반환되지 않는다.
 *
 * <p>{@code delYn}을 포함하는 이유 — {@link com.sm.lab.shop.dao.MemberDao#selectAuthByEmail}은
 * 탈퇴 회원도 조회한다(WHERE에 del_yn 필터를 걸지 않음). 서비스가 "회원 없음"과 "탈퇴 회원"을
 * 완전히 동일한 401 응답으로 처리하기 위해서는, 탈퇴 여부를 판정할 값 자체는 있어야 한다(STORY
 * "순서·보안" 2 — 존재 판정 오라클 방지).
 */
public class MemberCredential {
    private String memberId;
    private String memberName;
    private String grade;
    private String passwordHash;
    private String delYn;

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDelYn() { return delYn; }
    public void setDelYn(String delYn) { this.delYn = delYn; }
}
