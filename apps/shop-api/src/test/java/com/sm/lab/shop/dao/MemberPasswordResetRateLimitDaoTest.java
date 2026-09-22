package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.MemberPasswordResetRateLimit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.MEMBER_PASSWORD_RESET_RATE_LIMITS) 대상 통합 테스트. SR-297 #1
 * (BAT-MBR-001)은 {@code purgeOldRows}만 검증했다 — {@code touchDailyLimit}/
 * {@code selectRateLimit}(일일 상한 원자 판정, INF-MBR-006)은 이 항목(#2)이 추가한다.
 * {@link MemberSignupRateLimitDaoTest}의 5개 시나리오(신규 target 허용, 쿨다운 경과 후
 * 허용+토큰 갱신, 쿨다운 이내 거부, 일일상한 도달 거부, 날짜 바뀜=새 행=카운트 리셋)를
 * {@code touchDailyLimit}/{@code MEMBER_PASSWORD_RESET_RATE_LIMITS}로 이식했다 — 단, 이
 * 파일 고유 관례(리터럴 TARGET 상수가 아니라 테스트별 UUID 접미 + {@code @AfterEach} 정리)는
 * 유지한다(가입 DAO 테스트의 리터럴 TARGET 패턴을 그대로 베끼지 않는다).
 */
@SpringBootTest
class MemberPasswordResetRateLimitDaoTest {

    private static final int COOLDOWN_SECONDS = 60;
    private static final int DAILY_LIMIT = 5;

    @Autowired
    private MemberPasswordResetRateLimitDao dao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 테스트마다 새 UUID 접미 target — 고정 리터럴이면 다른 테스트/재실행과 카운터 행이 섞여
    // 플레이키해질 수 있다(사례집 SR-232 r2 재발 방지).
    private String target;

    private static String newToken() {
        return UUID.randomUUID().toString();
    }

    @AfterEach
    void cleanUp() {
        if (target != null) {
            jdbcTemplate.update("DELETE FROM MEMBER_PASSWORD_RESET_RATE_LIMITS WHERE target = ?", target);
        }
    }

    private void seedRow(LocalDate dayKey) {
        jdbcTemplate.update(
                "INSERT INTO MEMBER_PASSWORD_RESET_RATE_LIMITS "
                        + "(target, day_key, daily_count, last_requested_at, last_token) VALUES (?, ?, ?, ?, ?)",
                target, dayKey, 1, LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS), UUID.randomUUID().toString());
    }

    // SR-297 #2 — 신규 target 허용(MemberSignupRateLimitDaoTest#touchRateLimit_newTargetToday_...
    // 이식).
    @Test
    void touchDailyLimit_newTargetToday_admitsAndStoresMyToken() {
        target = "pwreset-dl-new-" + UUID.randomUUID() + "@example.com";
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String token = newToken();

        dao.touchDailyLimit(target, now.toLocalDate(), now, COOLDOWN_SECONDS, DAILY_LIMIT, token);

        MemberPasswordResetRateLimit saved = dao.selectRateLimit(target, now.toLocalDate());
        assertThat(saved).isNotNull();
        assertThat(saved.getDailyCount()).isEqualTo(1);
        assertThat(saved.getLastRequestedAt()).isEqualTo(now);
        assertThat(saved.getLastToken()).as("신규 삽입은 내 토큰이 그대로 저장돼야 함").isEqualTo(token);
    }

    // SR-297 #2 — 쿨다운 경과 후 허용+토큰 갱신 이식.
    @Test
    void touchDailyLimit_afterCooldownPassed_admitsAndOverwritesToken() {
        target = "pwreset-dl-cooldown-ok-" + UUID.randomUUID() + "@example.com";
        LocalDateTime first = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String firstToken = newToken();
        dao.touchDailyLimit(target, first.toLocalDate(), first, COOLDOWN_SECONDS, DAILY_LIMIT, firstToken);

        LocalDateTime second = first.plusSeconds(COOLDOWN_SECONDS + 1);
        String secondToken = newToken();
        dao.touchDailyLimit(target, second.toLocalDate(), second, COOLDOWN_SECONDS, DAILY_LIMIT, secondToken);

        MemberPasswordResetRateLimit saved = dao.selectRateLimit(target, second.toLocalDate());
        assertThat(saved.getDailyCount()).isEqualTo(2);
        assertThat(saved.getLastRequestedAt()).isEqualTo(second);
        assertThat(saved.getLastToken()).as("쿨다운이 지난 두 번째 요청의 토큰으로 갱신돼야 함")
                .isEqualTo(secondToken);
    }

    // SR-297 #2 — 쿨다운 이내 거부 이식.
    @Test
    void touchDailyLimit_withinCooldown_rejectsAndKeepsFirstToken() {
        target = "pwreset-dl-cooldown-block-" + UUID.randomUUID() + "@example.com";
        LocalDateTime first = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String firstToken = newToken();
        dao.touchDailyLimit(target, first.toLocalDate(), first, COOLDOWN_SECONDS, DAILY_LIMIT, firstToken);

        LocalDateTime second = first.plusSeconds(10); // 쿨다운(60초) 이내
        String secondToken = newToken();
        dao.touchDailyLimit(target, second.toLocalDate(), second, COOLDOWN_SECONDS, DAILY_LIMIT, secondToken);

        MemberPasswordResetRateLimit saved = dao.selectRateLimit(target, first.toLocalDate());
        assertThat(saved.getDailyCount()).as("거부된 요청은 카운터를 소비하지 않아야 함").isEqualTo(1);
        assertThat(saved.getLastRequestedAt()).isEqualTo(first);
        assertThat(saved.getLastToken())
                .as("쿨다운 위반 요청의 토큰은 저장되지 않고 이전 토큰이 남아 있어야 함(거부 판정의 핵심)")
                .isEqualTo(firstToken)
                .isNotEqualTo(secondToken);
    }

    // SR-297 #2 — 일일상한 도달 거부 이식.
    @Test
    void touchDailyLimit_atDailyLimit_rejectsEvenAfterCooldownAndKeepsLastAdmittedToken() {
        target = "pwreset-dl-daily-limit-" + UUID.randomUUID() + "@example.com";
        LocalDateTime cursor = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String lastAdmittedToken = newToken();
        dao.touchDailyLimit(target, cursor.toLocalDate(), cursor, COOLDOWN_SECONDS, DAILY_LIMIT, lastAdmittedToken);
        for (int i = 2; i <= DAILY_LIMIT; i++) {
            cursor = cursor.plusSeconds(COOLDOWN_SECONDS + 1);
            lastAdmittedToken = newToken();
            dao.touchDailyLimit(target, cursor.toLocalDate(), cursor, COOLDOWN_SECONDS, DAILY_LIMIT,
                    lastAdmittedToken);
        }
        assertThat(dao.selectRateLimit(target, cursor.toLocalDate()).getLastToken())
                .as("call #%d(마지막 허용분)의 토큰이 저장돼 있어야 함", DAILY_LIMIT)
                .isEqualTo(lastAdmittedToken);

        LocalDateTime sixthCall = cursor.plusSeconds(COOLDOWN_SECONDS + 1); // 쿨다운은 지났지만 상한 도달
        String sixthToken = newToken();
        dao.touchDailyLimit(target, sixthCall.toLocalDate(), sixthCall, COOLDOWN_SECONDS, DAILY_LIMIT, sixthToken);

        MemberPasswordResetRateLimit saved = dao.selectRateLimit(target, sixthCall.toLocalDate());
        assertThat(saved.getDailyCount()).as("거부된 6번째는 카운터를 늘리지 않아야 함").isEqualTo(DAILY_LIMIT);
        assertThat(saved.getLastToken())
                .as("일일상한 도달 후 6번째 토큰은 저장되지 않아야 함(거부)")
                .isEqualTo(lastAdmittedToken)
                .isNotEqualTo(sixthToken);
    }

    // SR-297 #2 — 날짜 바뀜=새 행=카운트 리셋 이식.
    @Test
    void touchDailyLimit_newDay_startsFreshRowAndAdmitsWithNewToken() {
        target = "pwreset-dl-newday-" + UUID.randomUUID() + "@example.com";
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime yesterdayInstant = yesterday.atTime(12, 0);
        dao.touchDailyLimit(target, yesterday, yesterdayInstant, COOLDOWN_SECONDS, DAILY_LIMIT, newToken());
        // 어제 상한까지 채워둔다 — 오늘 행에는 영향이 없어야 함(별도 PK).
        LocalDateTime cursor = yesterdayInstant;
        for (int i = 2; i <= DAILY_LIMIT; i++) {
            cursor = cursor.plusSeconds(COOLDOWN_SECONDS + 1);
            dao.touchDailyLimit(target, yesterday, cursor, COOLDOWN_SECONDS, DAILY_LIMIT, newToken());
        }
        assertThat(dao.selectRateLimit(target, yesterday).getDailyCount()).isEqualTo(DAILY_LIMIT);

        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String todayToken = newToken();
        dao.touchDailyLimit(target, today.toLocalDate(), today, COOLDOWN_SECONDS, DAILY_LIMIT, todayToken);

        MemberPasswordResetRateLimit saved = dao.selectRateLimit(target, today.toLocalDate());
        assertThat(saved.getDailyCount())
                .as("날짜가 바뀌면 (target, day_key) PK가 달라 새 행이 INSERT되어야 함").isEqualTo(1);
        assertThat(saved.getLastToken()).isEqualTo(todayToken);
    }

    // round1 재작업(QA CONCERNS 권고3) — purgeOldRows는 UUID target과 무관하게 테이블
    // 전역(WHERE day_key < 임계값)에서 삭제한다. 영향행수 절대값 단언은 다른 테스트가 남긴
    // 잔여 행에 취약해 플레이키하다(MemberSignupRateLimitDaoTest#purgeOldRows_* 선례와 동일
    // 문제). 아래는 반환값을 단언하지 않고 내 target의 행 존재/부재만 재조회로 판정한다.

    // linked_tc: TC-FUNC-member-bat001-005
    @Test
    void purgeOldRows_deletesOnlyRowsBeforeGivenDayAndKeepsToday() {
        target = "pwreset-rl-cleanup-" + UUID.randomUUID() + "@example.com";
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        seedRow(yesterday);
        seedRow(today);

        dao.purgeOldRows(today);

        Integer yesterdayCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_PASSWORD_RESET_RATE_LIMITS WHERE target = ? AND day_key = ?",
                Integer.class, target, yesterday);
        Integer todayCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_PASSWORD_RESET_RATE_LIMITS WHERE target = ? AND day_key = ?",
                Integer.class, target, today);
        assertThat(yesterdayCount).as("어제 행은 삭제되어야 함").isEqualTo(0);
        assertThat(todayCount).as("오늘 행은 보존되어야 함").isEqualTo(1);
    }

    // linked_tc: TC-FUNC-member-bat001-007
    @Test
    void purgeOldRows_calledTwiceInARow_myRowStaysDeletedAfterSecondCall() {
        target = "pwreset-rl-idempotent-" + UUID.randomUUID() + "@example.com";
        LocalDate yesterday = LocalDate.now().minusDays(1);
        seedRow(yesterday);

        dao.purgeOldRows(LocalDate.now());
        Integer afterFirst = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_PASSWORD_RESET_RATE_LIMITS WHERE target = ? AND day_key = ?",
                Integer.class, target, yesterday);
        assertThat(afterFirst).as("첫 호출 후 내 어제 행은 삭제되어야 함").isEqualTo(0);

        dao.purgeOldRows(LocalDate.now());
        Integer afterSecond = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_PASSWORD_RESET_RATE_LIMITS WHERE target = ? AND day_key = ?",
                Integer.class, target, yesterday);
        assertThat(afterSecond)
                .as("두 번째 연속 호출 뒤에도 내 행 상태(삭제됨)는 동일해야 함(멱등)").isEqualTo(0);
    }
}
