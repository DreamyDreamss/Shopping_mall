// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.MemberSignupRateLimit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.MEMBER_SIGNUP_RATE_LIMITS) 대상 통합 테스트. round4는
 * {@link MemberSignupRateLimitDao#touchRateLimit}의 affected-rows 반환값(신규 삽입=1/실제 값
 * 변경=2/무변경=0)을 판정 신호로 썼으나, 그 의미는 datasource 전역 설정
 * ({@code useAffectedRows=true})에 의존했고 그 설정이 FUNC-order-002의 UPDATE 반환값
 * 시맨틱까지 바꿔 qty=0 주문 라인 회귀를 냈다(round4 QA FAIL). round5(사람 결정)는 그 전역
 * 설정을 제거하고 {@code last_token}(요청마다 만든 UUID)을 재조회해 "내 토큰이 저장돼
 * 있으면 허용"으로 판정하도록 바꿨다 — 이 테스트는 그 last_token 시맨틱을 실 DB로 직접
 * 검증한다.
 *
 * <p>⚠ 이 테스트가 깨지면 레이트리밋 전체가 무력화됐다는 뜻이다(round4 QA 권고4 —
 * fail-open 위험이 이 어서션 하나에 사실상 의존한다. STORY 후속 추적(TODO) 참고).
 */
@SpringBootTest
class MemberSignupRateLimitDaoTest {

    private static final String TARGET = "rl-dao-test@example.com";
    private static final int COOLDOWN_SECONDS = 60;
    private static final int DAILY_LIMIT = 5;

    @Autowired
    private MemberSignupRateLimitDao dao;

    @AfterEach
    void cleanUp() {
        dao.deleteRateLimit(TARGET, LocalDate.now());
        dao.deleteRateLimit(TARGET, LocalDate.now().minusDays(1));
    }

    private static String newToken() {
        return UUID.randomUUID().toString();
    }

    @Test
    void touchRateLimit_newTargetToday_admitsAndStoresMyToken() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String token = newToken();

        dao.touchRateLimit(TARGET, now.toLocalDate(), now, COOLDOWN_SECONDS, DAILY_LIMIT, token);

        MemberSignupRateLimit saved = dao.selectRateLimit(TARGET, now.toLocalDate());
        assertThat(saved).isNotNull();
        assertThat(saved.getDailyCount()).isEqualTo(1);
        assertThat(saved.getLastRequestedAt()).isEqualTo(now);
        assertThat(saved.getLastToken()).as("신규 삽입은 내 토큰이 그대로 저장돼야 함").isEqualTo(token);
    }

    @Test
    void touchRateLimit_afterCooldownPassed_admitsAndOverwritesToken() {
        LocalDateTime first = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String firstToken = newToken();
        dao.touchRateLimit(TARGET, first.toLocalDate(), first, COOLDOWN_SECONDS, DAILY_LIMIT, firstToken);

        LocalDateTime second = first.plusSeconds(COOLDOWN_SECONDS + 1);
        String secondToken = newToken();
        dao.touchRateLimit(TARGET, second.toLocalDate(), second, COOLDOWN_SECONDS, DAILY_LIMIT, secondToken);

        MemberSignupRateLimit saved = dao.selectRateLimit(TARGET, second.toLocalDate());
        assertThat(saved.getDailyCount()).isEqualTo(2);
        assertThat(saved.getLastRequestedAt()).isEqualTo(second);
        assertThat(saved.getLastToken()).as("쿨다운이 지난 두 번째 요청의 토큰으로 갱신돼야 함")
                .isEqualTo(secondToken);
    }

    @Test
    void touchRateLimit_withinCooldown_rejectsAndKeepsFirstToken() {
        LocalDateTime first = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String firstToken = newToken();
        dao.touchRateLimit(TARGET, first.toLocalDate(), first, COOLDOWN_SECONDS, DAILY_LIMIT, firstToken);

        LocalDateTime second = first.plusSeconds(10); // 쿨다운(60초) 이내
        String secondToken = newToken();
        dao.touchRateLimit(TARGET, second.toLocalDate(), second, COOLDOWN_SECONDS, DAILY_LIMIT, secondToken);

        MemberSignupRateLimit saved = dao.selectRateLimit(TARGET, first.toLocalDate());
        assertThat(saved.getDailyCount()).as("거부된 요청은 카운터를 소비하지 않아야 함").isEqualTo(1);
        assertThat(saved.getLastRequestedAt()).isEqualTo(first);
        assertThat(saved.getLastToken())
                .as("쿨다운 위반 요청의 토큰은 저장되지 않고 이전 토큰이 남아 있어야 함(거부 판정의 핵심)")
                .isEqualTo(firstToken)
                .isNotEqualTo(secondToken);
    }

    @Test
    void touchRateLimit_atDailyLimit_rejectsEvenAfterCooldownAndKeepsLastAdmittedToken() {
        LocalDateTime cursor = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String lastAdmittedToken = newToken();
        dao.touchRateLimit(TARGET, cursor.toLocalDate(), cursor, COOLDOWN_SECONDS, DAILY_LIMIT, lastAdmittedToken);
        for (int i = 2; i <= DAILY_LIMIT; i++) {
            cursor = cursor.plusSeconds(COOLDOWN_SECONDS + 1);
            lastAdmittedToken = newToken();
            dao.touchRateLimit(TARGET, cursor.toLocalDate(), cursor, COOLDOWN_SECONDS, DAILY_LIMIT, lastAdmittedToken);
        }
        assertThat(dao.selectRateLimit(TARGET, cursor.toLocalDate()).getLastToken())
                .as("call #%d(마지막 허용분)의 토큰이 저장돼 있어야 함", DAILY_LIMIT)
                .isEqualTo(lastAdmittedToken);

        LocalDateTime sixthCall = cursor.plusSeconds(COOLDOWN_SECONDS + 1); // 쿨다운은 지났지만 상한 도달
        String sixthToken = newToken();
        dao.touchRateLimit(TARGET, sixthCall.toLocalDate(), sixthCall, COOLDOWN_SECONDS, DAILY_LIMIT, sixthToken);

        MemberSignupRateLimit saved = dao.selectRateLimit(TARGET, sixthCall.toLocalDate());
        assertThat(saved.getDailyCount()).as("거부된 6번째는 카운터를 늘리지 않아야 함").isEqualTo(DAILY_LIMIT);
        assertThat(saved.getLastToken())
                .as("일일상한 도달 후 6번째 토큰은 저장되지 않아야 함(거부)")
                .isEqualTo(lastAdmittedToken)
                .isNotEqualTo(sixthToken);
    }

    @Test
    void touchRateLimit_newDay_startsFreshRowAndAdmitsWithNewToken() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime yesterdayInstant = yesterday.atTime(12, 0);
        dao.touchRateLimit(TARGET, yesterday, yesterdayInstant, COOLDOWN_SECONDS, DAILY_LIMIT, newToken());
        // 어제 상한까지 채워둔다 — 오늘 행에는 영향이 없어야 함(별도 PK).
        LocalDateTime cursor = yesterdayInstant;
        for (int i = 2; i <= DAILY_LIMIT; i++) {
            cursor = cursor.plusSeconds(COOLDOWN_SECONDS + 1);
            dao.touchRateLimit(TARGET, yesterday, cursor, COOLDOWN_SECONDS, DAILY_LIMIT, newToken());
        }
        assertThat(dao.selectRateLimit(TARGET, yesterday).getDailyCount()).isEqualTo(DAILY_LIMIT);

        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String todayToken = newToken();
        dao.touchRateLimit(TARGET, today.toLocalDate(), today, COOLDOWN_SECONDS, DAILY_LIMIT, todayToken);

        MemberSignupRateLimit saved = dao.selectRateLimit(TARGET, today.toLocalDate());
        assertThat(saved.getDailyCount())
                .as("날짜가 바뀌면 (target, day_key) PK가 달라 새 행이 INSERT되어야 함").isEqualTo(1);
        assertThat(saved.getLastToken()).isEqualTo(todayToken);
    }

    @Test
    void purgeOldRows_deletesOnlyRowsBeforeGivenDayAndKeepsToday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        dao.touchRateLimit(TARGET, yesterday, yesterday.atTime(12, 0), COOLDOWN_SECONDS, DAILY_LIMIT, newToken());
        dao.touchRateLimit(TARGET, today, LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
                COOLDOWN_SECONDS, DAILY_LIMIT, newToken());

        dao.purgeOldRows(today);

        assertThat(dao.selectRateLimit(TARGET, yesterday)).isNull();
        assertThat(dao.selectRateLimit(TARGET, today)).isNotNull();
    }
}
