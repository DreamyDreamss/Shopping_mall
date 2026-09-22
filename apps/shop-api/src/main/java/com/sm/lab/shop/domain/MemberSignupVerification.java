// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/**
 * 회원가입 인증코드(MEMBER_SIGNUP_VERIFICATIONS) — SR-231. (channel,target) 단위로 최신
 * 코드 1건을 유지한다.
 *
 * <p>round4(사람 결정 — "설계를 단순화해서 데드락 원인을 없앤다") — 레이트리밋 카운터는
 * 전용 테이블({@link MemberSignupRateLimit}, {@link com.sm.lab.shop.dao.MemberSignupRateLimitDao})
 * 로 완전히 옮겼다. 이 클래스는 더 이상 {@code dailyCount}/{@code previousRequestedAt}을
 * 갖지 않는다(round3에는 있었음) — {@code lastRequestedAt}은 "코드가 마지막으로 실제
 * 발송된 시각"(정보용)으로 의미가 바뀌었다.
 */
public class MemberSignupVerification {
    private String channel;
    private String target;
    private String code;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime lastRequestedAt;
    // FUNC-member-003(가입완료 API)이 코드 대입 시도 제한에 쓸 컬럼(이 FUNC은 컬럼만 소유).
    private int attemptCount;

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public LocalDateTime getLastRequestedAt() { return lastRequestedAt; }
    public void setLastRequestedAt(LocalDateTime lastRequestedAt) { this.lastRequestedAt = lastRequestedAt; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
}
