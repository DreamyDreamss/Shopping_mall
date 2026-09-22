// linked_func: FUNC-member-005
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.MemberLoginAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.MEMBER_LOGIN_ATTEMPTS) 대상 통합 테스트 — {@code MemberSignupRateLimitDaoTest}
 * 와 동일 house 패턴(원자 UPSERT의 실제 SQL 동작을 실 DB로 검증).
 */
@SpringBootTest
class MemberLoginAttemptDaoTest {

    private static final String EMAIL = "login-dao-test@example.com";
    private static final int LOCK_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private MemberLoginAttemptDao dao;

    @AfterEach
    void cleanUp() {
        dao.reset(EMAIL);
    }

    @Test
    void touchFailure_firstFailure_setsFailCountOneNoLock() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        dao.touchFailure(EMAIL, now, LOCK_MINUTES, MAX_ATTEMPTS);

        MemberLoginAttempt saved = dao.selectAttempt(EMAIL);
        assertThat(saved.getFailCount()).isEqualTo(1);
        assertThat(saved.getLockedUntil()).isNull();
    }

    @Test
    void touchFailure_fifthFailure_setsLockedUntilTenMinutesLater() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            dao.touchFailure(EMAIL, now.plusSeconds(i), LOCK_MINUTES, MAX_ATTEMPTS);
        }

        MemberLoginAttempt saved = dao.selectAttempt(EMAIL);
        assertThat(saved.getFailCount()).isEqualTo(MAX_ATTEMPTS);
        assertThat(saved.getLockedUntil())
                .as("5번째 실패 자체가 잠금을 걸어야 한다(사람 확인 2)")
                .isEqualTo(now.plusSeconds(MAX_ATTEMPTS).plusMinutes(LOCK_MINUTES));
    }

    @Test
    void touchFailure_beforeFifth_doesNotLock() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        for (int i = 1; i < MAX_ATTEMPTS; i++) {
            dao.touchFailure(EMAIL, now.plusSeconds(i), LOCK_MINUTES, MAX_ATTEMPTS);
        }

        MemberLoginAttempt saved = dao.selectAttempt(EMAIL);
        assertThat(saved.getFailCount()).isEqualTo(MAX_ATTEMPTS - 1);
        assertThat(saved.getLockedUntil()).isNull();
    }

    @Test
    void touchFailure_afterLockExpired_resetsToOne() {
        LocalDateTime past = LocalDateTime.now().minusMinutes(30).truncatedTo(ChronoUnit.MILLIS);
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            dao.touchFailure(EMAIL, past.plusSeconds(i), LOCK_MINUTES, MAX_ATTEMPTS);
        }
        assertThat(dao.selectAttempt(EMAIL).getLockedUntil()).isBefore(LocalDateTime.now());

        LocalDateTime afterExpiry = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.touchFailure(EMAIL, afterExpiry, LOCK_MINUTES, MAX_ATTEMPTS);

        MemberLoginAttempt saved = dao.selectAttempt(EMAIL);
        assertThat(saved.getFailCount()).as("잠금 만료 후 재실패는 1로 리셋(무한 누적 방지)").isEqualTo(1);
        assertThat(saved.getLockedUntil()).isNull();
    }

    @Test
    void reset_deletesRow() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.touchFailure(EMAIL, now, LOCK_MINUTES, MAX_ATTEMPTS);

        dao.reset(EMAIL);

        assertThat(dao.selectAttempt(EMAIL)).isNull();
    }

    @Test
    void selectAttempt_noRow_returnsNull() {
        assertThat(dao.selectAttempt("never-failed@example.com")).isNull();
    }
}
