package com.sm.lab.shop.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 코드 요청 일일 상한 카운터(MEMBER_PASSWORD_RESET_RATE_LIMITS) — SR-297 #2.
 * (target, dayKey) 단위로 오늘 허용된 요청 횟수와 마지막으로 허용된 요청 시각을 유지한다.
 *
 * <p>{@link com.sm.lab.shop.domain.MemberSignupRateLimit}(가입 도메인, SR-231 round4/round5)과
 * 완전히 같은 모양이다 — 이 항목(#2)의 STORY 사람 회신이 "가입 MEMBER_SIGNUP_RATE_LIMITS와 같은
 * 모양"을 명시했다. 테이블 DDL 자체는 SR-297 #1이 이미 만들었다(SCH-MBR-009) — 이 클래스는 그
 * 컬럼을 처음으로 읽고 쓰는 이 항목이 추가한다.
 */
public class MemberPasswordResetRateLimit {
    private String target;
    private LocalDate dayKey;
    private int dailyCount;
    private LocalDateTime lastRequestedAt;
    private String lastToken;

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public LocalDate getDayKey() { return dayKey; }
    public void setDayKey(LocalDate dayKey) { this.dayKey = dayKey; }
    public int getDailyCount() { return dailyCount; }
    public void setDailyCount(int dailyCount) { this.dailyCount = dailyCount; }
    public LocalDateTime getLastRequestedAt() { return lastRequestedAt; }
    public void setLastRequestedAt(LocalDateTime lastRequestedAt) { this.lastRequestedAt = lastRequestedAt; }
    public String getLastToken() { return lastToken; }
    public void setLastToken(String lastToken) { this.lastToken = lastToken; }
}
