// linked_func: FUNC-member-008
// spec: docs/00_FUNC/stories/STORY-FUNC-member-008.md
package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 코드 요청 행(MEMBER_PASSWORD_RESETS) — SR-234, FUNC-member-008.
 * (target) 단위로 마지막 발급 코드 해시·만료 시각·쿨다운 판정 기준({@code createdAt})을 유지한다.
 *
 * <p>{@code consumedAt}/{@code attemptCount}는 이 FUNC(008, 요청 API)이 조회 목적으로만 매핑하고
 * 직접 세팅하지 않는다 — 코드 확인 + 새 비밀번호 반영(FUNC-member-009, 확정 API)이 갱신한다.
 */
public class MemberPasswordReset {
    private String target;
    private String codeHash;
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    private int attemptCount;
    private LocalDateTime createdAt;

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
