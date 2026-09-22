// linked_func: FUNC-member-005
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md
package com.sm.lab.shop.domain;

import java.time.LocalDateTime;

/**
 * 로그인 실패 카운터(SR-232, {@code MEMBER_LOGIN_ATTEMPTS}) — email 문자열 자체가 PK다(회원
 * 존재 여부와 무관하게 카운트된다). {@code lockedUntil}이 채워져 있고 현재 시각보다 미래이면
 * 잠긴 상태(429 응답 대상)다.
 */
public class MemberLoginAttempt {
    private String email;
    private int failCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastFailedAt;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public LocalDateTime getLastFailedAt() { return lastFailedAt; }
    public void setLastFailedAt(LocalDateTime lastFailedAt) { this.lastFailedAt = lastFailedAt; }
}
