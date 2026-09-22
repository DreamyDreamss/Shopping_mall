// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회원가입 인증코드 레이트리밋 카운터(MEMBER_SIGNUP_RATE_LIMITS) — SR-231 round4.
 * (target, dayKey) 단위로 오늘 허용된 요청 횟수와 마지막으로 허용된 요청 시각을 유지한다.
 *
 * <p>코드 테이블({@link MemberSignupVerification})과는 완전히 분리된 테이블·락 범위를 갖는다
 * (사람 결정 — STORY-FUNC-member-002 재작업 지시(A): "별도 테이블이라 코드 테이블과 락이
 * 섞이지 않는다"). round1~3까지는 이 카운터가 코드 테이블 안에 함께 있었다.
 *
 * <p>round5(사람 결정) — {@code lastToken} 추가. round4는 판정을 JDBC affected-rows(0/1/2)로
 * 했는데, 그 의미는 datasource 전역 설정({@code useAffectedRows=true})에 의존했고 그 설정이
 * FUNC-order-002({@code ProductDao.decreaseStock})의 UPDATE 반환값 시맨틱까지 바꿔 qty=0 주문
 * 라인의 응답을 200→409로 회귀시켰다(QA 실측). round5는 그 전역 설정을 제거하고, 요청마다 만든
 * UUID 토큰을 원자 UPSERT의 조건부 갱신 절에 함께 기록해(허용될 때만 갱신) 그 값을 다시 읽어
 * "내 토큰이 저장돼 있으면 허용"으로 판정한다({@link com.sm.lab.shop.service.MemberSignupService}
 * 참고) — affected-rows 의미에 더 이상 의존하지 않는다.
 */
public class MemberSignupRateLimit {
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
