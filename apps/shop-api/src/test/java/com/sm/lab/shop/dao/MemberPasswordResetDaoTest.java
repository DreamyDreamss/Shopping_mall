// linked_func: FUNC-member-008, FUNC-member-009
// spec: docs/00_FUNC/stories/STORY-FUNC-member-008.md, docs/00_FUNC/stories/STORY-FUNC-member-009.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.MemberPasswordReset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 실 DB(MariaDB, sl_lab.MEMBER_PASSWORD_RESETS) 대상 통합 테스트(SR-234, FUNC-member-008).
 * 서비스/컨트롤러 테스트는 DAO를 목으로 대체하므로, SQL 자체(원자 UPSERT의 쿨다운 판정·SET
 * 목록 순서)의 정확성은 이 레벨에서만 실증된다({@code MemberSignupRateLimitDaoTest}와 동일 이유).
 *
 * <p><b>사람 수정 (3) 핵심 증거</b> — {@code touchRequest} 매퍼 SQL은 {@code created_at}을 SET
 * 목록 맨 뒤에 둬 앞선 IF 조건들이 갱신 전 값을 참조하게 한다("SET은 좌→우로 평가한다"는 가정은
 * 코드 리뷰만으로는 못 믿는다 — 이 테스트가 {@code now} 파라미터를 직접 주입해 실시계 대기 없이
 * 결정적으로 재현한다). 59초/61초 두 케이스가 짝을 이뤄야 순서 고정이 증명된다.
 */
@SpringBootTest
class MemberPasswordResetDaoTest {

    private static final String TARGET = "pwreset-dao-test@example.com";
    private static final int COOLDOWN_SECONDS = 60;

    @Autowired
    private MemberPasswordResetDao dao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // round2(QA 권고, 재작업 지시 low(5)) — 계획대로 UUID 접미. 동시성 테스트가 실행마다 새로
    // 만들어 이 필드에 채운다(고정 접미는 surefire 병렬화 시 다른 실행과 충돌할 수 있다 —
    // 사례집 SR-232 r2와 동일 계열 위험).
    private String concurrencyTarget;

    @AfterEach
    void cleanUp() {
        dao.deleteByTarget(TARGET);
        if (concurrencyTarget != null) {
            dao.deleteByTarget(concurrencyTarget);
        }
    }

    @Test
    void touchRequest_newTarget_createsRowWithZeroAttemptsAndNoConsumedAt() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        dao.touchRequest(TARGET, "a".repeat(64), now.plusSeconds(600), now, COOLDOWN_SECONDS);

        MemberPasswordReset saved = dao.selectByTarget(TARGET);
        assertThat(saved).isNotNull();
        assertThat(saved.getAttemptCount()).isEqualTo(0);
        assertThat(saved.getConsumedAt()).isNull();
        assertThat(saved.getCodeHash()).isEqualTo("a".repeat(64));
        assertThat(saved.getExpiresAt()).isEqualTo(now.plusSeconds(600));
        assertThat(saved.getCreatedAt()).isEqualTo(now);
    }

    // 사람 수정 (3) 필수 케이스 ① — 60초 미만은 code_hash/expires_at/created_at 전부 첫 호출
    // 값 그대로(무시). now 파라미터를 created_at + 59초로 직접 주입해 실시계 대기 없이 재현한다.
    @Test
    void touchRequest_within59Seconds_keepsFirstCodeHashExpiresAtAndCreatedAt() {
        LocalDateTime first = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String firstHash = "1".repeat(64);
        dao.touchRequest(TARGET, firstHash, first.plusSeconds(600), first, COOLDOWN_SECONDS);

        LocalDateTime second = first.plusSeconds(59); // 쿨다운(60초) 미만
        String secondHash = "2".repeat(64);
        dao.touchRequest(TARGET, secondHash, second.plusSeconds(600), second, COOLDOWN_SECONDS);

        MemberPasswordReset saved = dao.selectByTarget(TARGET);
        assertThat(saved.getCodeHash()).as("59초 재요청은 무시되어야 함").isEqualTo(firstHash);
        assertThat(saved.getExpiresAt()).isEqualTo(first.plusSeconds(600));
        assertThat(saved.getCreatedAt()).isEqualTo(first);
    }

    // 사람 수정 (3) 필수 케이스 ② — 60초 이상은 code_hash/expires_at/created_at 셋 다 갱신,
    // attempt_count 0으로 리셋, consumed_at NULL로 리셋. 리셋을 실제로 관찰하려면 첫 호출 뒤
    // attempt_count/consumed_at을 비기본값으로 세팅해 둬야 한다(이 FUNC의 touchRequest 자체는
    // 그 값을 절대 비기본값으로 만들지 않으므로 JdbcTemplate으로 직접 시뮬레이션한다 —
    // FUNC-member-009가 실제로 갱신할 값을 흉내낸다).
    @Test
    void touchRequest_after61Seconds_refreshesCodeAndResetsAttemptCountAndConsumedAt() {
        LocalDateTime first = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String firstHash = "1".repeat(64);
        dao.touchRequest(TARGET, firstHash, first.plusSeconds(600), first, COOLDOWN_SECONDS);
        jdbcTemplate.update(
                "UPDATE MEMBER_PASSWORD_RESETS SET attempt_count = 3, consumed_at = ? WHERE target = ?",
                first, TARGET);

        LocalDateTime second = first.plusSeconds(61); // 쿨다운(60초) 이상
        String secondHash = "2".repeat(64);
        dao.touchRequest(TARGET, secondHash, second.plusSeconds(600), second, COOLDOWN_SECONDS);

        MemberPasswordReset saved = dao.selectByTarget(TARGET);
        assertThat(saved.getCodeHash()).as("61초 이후 재요청은 갱신되어야 함").isEqualTo(secondHash);
        assertThat(saved.getExpiresAt()).isEqualTo(second.plusSeconds(600));
        assertThat(saved.getCreatedAt()).isEqualTo(second);
        assertThat(saved.getAttemptCount()).as("갱신 시 시도 횟수는 0으로 리셋되어야 함").isEqualTo(0);
        assertThat(saved.getConsumedAt()).as("갱신 시 소비 시각은 NULL로 리셋되어야 함").isNull();
    }

    // (should) 동시 5스레드가 같은 신규 target에 동시 호출 → 예외 없이 종료, 최종 행 1개만 존재.
    @Test
    void touchRequest_fiveConcurrentCallsSameNewTarget_noExceptionAndExactlyOneRow() throws InterruptedException {
        concurrencyTarget = "pwreset-dao-concurrency-" + UUID.randomUUID() + "@example.com";
        int concurrency = 5;
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CyclicBarrier barrier = new CyclicBarrier(concurrency);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < concurrency; i++) {
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
            tasks.add(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                dao.touchRequest(concurrencyTarget, "c".repeat(64), now.plusSeconds(600), now, COOLDOWN_SECONDS);
                return null;
            });
        }

        assertThatCode(() -> {
            List<Future<Void>> futures = new ArrayList<>();
            for (Callable<Void> task : tasks) {
                futures.add(pool.submit(task));
            }
            pool.shutdown();
            for (Future<Void> f : futures) {
                f.get(15, TimeUnit.SECONDS);
            }
        }).doesNotThrowAnyException();

        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_PASSWORD_RESETS WHERE target = ?", Integer.class, concurrencyTarget);
        assertThat(rowCount).isEqualTo(1);
    }

    // ===== confirmIfCodeMatches(FUNC-member-009) — 서비스 목만으로는 증명 못 하는 SQL 조건 실증 =====

    @Test
    void confirmIfCodeMatches_correctCodeNotExpiredNotConsumedUnderLimit_returnsOneRowAndSetsConsumedAt() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String codeHash = "a".repeat(64);
        dao.touchRequest(TARGET, codeHash, now.plusSeconds(600), now, COOLDOWN_SECONDS);

        int affected = dao.confirmIfCodeMatches(TARGET, codeHash, now, 5);

        assertThat(affected).isEqualTo(1);
        assertThat(dao.selectByTarget(TARGET).getConsumedAt()).isEqualTo(now);
    }

    @Test
    void confirmIfCodeMatches_wrongCode_returnsZeroRowsAndConsumedAtUnchanged() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.touchRequest(TARGET, "a".repeat(64), now.plusSeconds(600), now, COOLDOWN_SECONDS);

        int affected = dao.confirmIfCodeMatches(TARGET, "b".repeat(64), now, 5);

        assertThat(affected).isEqualTo(0);
        assertThat(dao.selectByTarget(TARGET).getConsumedAt()).isNull();
    }

    @Test
    void confirmIfCodeMatches_expired_returnsZeroRows() {
        LocalDateTime past = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusSeconds(1);
        String codeHash = "a".repeat(64);
        dao.touchRequest(TARGET, codeHash, past, past.minusSeconds(600), COOLDOWN_SECONDS);

        int affected = dao.confirmIfCodeMatches(TARGET, codeHash, LocalDateTime.now(), 5);

        assertThat(affected).isEqualTo(0);
    }

    @Test
    void confirmIfCodeMatches_alreadyConsumed_returnsZeroRows() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String codeHash = "a".repeat(64);
        dao.touchRequest(TARGET, codeHash, now.plusSeconds(600), now, COOLDOWN_SECONDS);
        jdbcTemplate.update("UPDATE MEMBER_PASSWORD_RESETS SET consumed_at = ? WHERE target = ?", now, TARGET);

        int affected = dao.confirmIfCodeMatches(TARGET, codeHash, now, 5);

        assertThat(affected).isEqualTo(0);
    }

    // DAO 레벨 실증 — 서비스 목만으로는 이 SQL 조건(attempt_count 상한)이 실제로 동작하는지 증명 못 한다.
    @Test
    void confirmIfCodeMatches_attemptCountAtMax_returnsZeroRowsEvenWithCorrectCode() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String codeHash = "a".repeat(64);
        dao.touchRequest(TARGET, codeHash, now.plusSeconds(600), now, COOLDOWN_SECONDS);
        jdbcTemplate.update("UPDATE MEMBER_PASSWORD_RESETS SET attempt_count = 5 WHERE target = ?", TARGET);

        int affected = dao.confirmIfCodeMatches(TARGET, codeHash, now, 5);

        assertThat(affected).isEqualTo(0);
        assertThat(dao.selectByTarget(TARGET).getConsumedAt())
                .as("시도 상한 도달 시 정답 코드가 들어와도 소비 처리되면 안 된다")
                .isNull();
    }

    // ===== incrementAttemptCount(FUNC-member-009) =====

    @Test
    void incrementAttemptCount_activeRow_incrementsByOne() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.touchRequest(TARGET, "a".repeat(64), now.plusSeconds(600), now, COOLDOWN_SECONDS);

        int affected = dao.incrementAttemptCount(TARGET, now, 5);

        assertThat(affected).isEqualTo(1);
        assertThat(dao.selectByTarget(TARGET).getAttemptCount()).isEqualTo(1);
    }

    @Test
    void incrementAttemptCount_expiredRow_doesNotIncrement() {
        LocalDateTime past = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusSeconds(1);
        dao.touchRequest(TARGET, "a".repeat(64), past, past.minusSeconds(600), COOLDOWN_SECONDS);

        int affected = dao.incrementAttemptCount(TARGET, LocalDateTime.now(), 5);

        assertThat(affected).isEqualTo(0);
        assertThat(dao.selectByTarget(TARGET).getAttemptCount()).isEqualTo(0);
    }

    @Test
    void incrementAttemptCount_consumedRow_doesNotIncrement() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.touchRequest(TARGET, "a".repeat(64), now.plusSeconds(600), now, COOLDOWN_SECONDS);
        jdbcTemplate.update("UPDATE MEMBER_PASSWORD_RESETS SET consumed_at = ? WHERE target = ?", now, TARGET);

        int affected = dao.incrementAttemptCount(TARGET, now, 5);

        assertThat(affected).isEqualTo(0);
    }

    // SR-298 신규 — 이 원자 캡이 이 SR의 핵심 산출물. 신선한 행에 5회 연속 증가(매회 영향행수 1,
    // attempt_count 1→5)한 뒤 6회째는 영향행수 0·attempt_count는 5 그대로임을 단언한다.
    @Test
    void incrementAttemptCount_atCap_returnsZeroRowsAndDoesNotIncrementBeyondFive() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.touchRequest(TARGET, "a".repeat(64), now.plusSeconds(600), now, COOLDOWN_SECONDS);

        for (int i = 1; i <= 5; i++) {
            int affected = dao.incrementAttemptCount(TARGET, now, 5);
            assertThat(affected).as("%d번째 증가는 영향행수 1이어야 함", i).isEqualTo(1);
            assertThat(dao.selectByTarget(TARGET).getAttemptCount()).as("%d번째 증가 후 attempt_count", i)
                    .isEqualTo(i);
        }

        int sixthAffected = dao.incrementAttemptCount(TARGET, now, 5);

        assertThat(sixthAffected).as("상한 도달 후 6번째 호출은 영향행수 0이어야 함").isEqualTo(0);
        assertThat(dao.selectByTarget(TARGET).getAttemptCount())
                .as("상한 도달 후 attempt_count는 5에 머물러야 함").isEqualTo(5);
    }

    // (should) 동시 5스레드가 같은 target에 오답 코드로 incrementAttemptCount 동시 호출 →
    // 예외 없이 종료, 최종 attempt_count == 5(유실 없음 — PK 행 잠금이 직렬화함을 실증).
    @Test
    void incrementAttemptCount_fiveConcurrentCallsSameTarget_noLostUpdatesFinalCountFive() throws InterruptedException {
        concurrencyTarget = "pwreset-confirm-concurrency-" + UUID.randomUUID() + "@example.com";
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.touchRequest(concurrencyTarget, "a".repeat(64), now.plusSeconds(600), now, COOLDOWN_SECONDS);

        int concurrency = 5;
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CyclicBarrier barrier = new CyclicBarrier(concurrency);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < concurrency; i++) {
            tasks.add(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                dao.incrementAttemptCount(concurrencyTarget, LocalDateTime.now(), 5);
                return null;
            });
        }

        assertThatCode(() -> {
            List<Future<Void>> futures = new ArrayList<>();
            for (Callable<Void> task : tasks) {
                futures.add(pool.submit(task));
            }
            pool.shutdown();
            for (Future<Void> f : futures) {
                f.get(15, TimeUnit.SECONDS);
            }
        }).doesNotThrowAnyException();

        assertThat(dao.selectByTarget(concurrencyTarget).getAttemptCount()).isEqualTo(concurrency);
    }

    // ===== purgeExpiredCodes(SR-297 #1, BAT-MBR-001) — 정리 배치 전용 =====
    //
    // round1 재작업(QA CONCERNS 권고3) — purgeExpiredCodes는 TARGET 하나가 아니라 테이블
    // 전역(WHERE expires_at < 임계값)에서 삭제한다. 영향행수 절대값(assertThat(affected)
    // .isEqualTo(1) 등)은 다른 테스트·e2e가 남긴 잔여 만료 행에 취약하다(1회성 플레이크,
    // MemberSignupRateLimitDaoTest#purgeOldRows_* 선례와 동일 문제). 아래는 반환값을 단언하지
    // 않고 내 TARGET 행의 존재/부재를 재조회해서만 판정한다.

    // linked_tc: TC-FUNC-member-bat001-001
    @Test
    void purgeExpiredCodes_expiredRow_deletesRow() {
        LocalDateTime past = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusSeconds(1);
        dao.touchRequest(TARGET, "a".repeat(64), past, past.minusSeconds(600), COOLDOWN_SECONDS);

        dao.purgeExpiredCodes(LocalDateTime.now());

        assertThat(dao.selectByTarget(TARGET)).as("만료 행은 삭제되어야 함").isNull();
    }

    // linked_tc: TC-FUNC-member-bat001-002
    @Test
    void purgeExpiredCodes_notYetExpiredRow_keepsRow() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.touchRequest(TARGET, "a".repeat(64), now.plusSeconds(600), now, COOLDOWN_SECONDS);

        dao.purgeExpiredCodes(now);

        assertThat(dao.selectByTarget(TARGET)).as("미만료 행은 보존되어야 함").isNotNull();
    }

    // linked_tc: TC-FUNC-member-bat001-006
    @Test
    void purgeExpiredCodes_calledTwiceInARow_myRowStaysDeletedAfterSecondCall() {
        LocalDateTime past = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusSeconds(1);
        dao.touchRequest(TARGET, "a".repeat(64), past, past.minusSeconds(600), COOLDOWN_SECONDS);

        dao.purgeExpiredCodes(LocalDateTime.now());
        assertThat(dao.selectByTarget(TARGET)).as("첫 호출 후 내 만료 행은 삭제되어야 함").isNull();

        dao.purgeExpiredCodes(LocalDateTime.now());
        assertThat(dao.selectByTarget(TARGET))
                .as("두 번째 연속 호출 뒤에도 내 행 상태(삭제됨)는 동일해야 함(멱등)").isNull();
    }

    // ===== purgeExpiredCodes — 보존 기간 경계(round1 재작업 지시 1) =====
    //
    // 배치는 이제 "지금"이 아니라 호출부가 계산한 "지금 - 보존기간(기본 7일)"을 임계값으로
    // 받는다(MemberPasswordResetMaintenanceScheduler 참고). 이 DAO 테스트는 그 임계값을
    // 직접 흉내내(now.minusDays(7)) SQL 조건(expires_at < 임계값) 자체가 경계에서 올바른지
    // 실증한다. 추가 AC — 만료 1일 지난 행(보존기간 안)은 보존, 만료 8일 지난 행(보존기간
    // 밖)은 삭제.

    // linked_tc: TC-FUNC-member-bat001-003
    @Test
    void purgeExpiredCodes_expiredOneDayAgo_withinSevenDayRetention_keepsRow() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime expiresAt = now.minusDays(1); // 만료된 지 1일 — 7일 보존기간 안
        dao.touchRequest(TARGET, "a".repeat(64), expiresAt, expiresAt.minusMinutes(10), COOLDOWN_SECONDS);
        LocalDateTime thresholdLikeScheduler = now.minusDays(7);

        dao.purgeExpiredCodes(thresholdLikeScheduler);

        assertThat(dao.selectByTarget(TARGET))
                .as("보존기간(7일) 안의 만료 행은 보존되어야 함(확정 API 410 유지)").isNotNull();
    }

    // linked_tc: TC-FUNC-member-bat001-004
    @Test
    void purgeExpiredCodes_expiredEightDaysAgo_beyondSevenDayRetention_deletesRow() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime expiresAt = now.minusDays(8); // 만료된 지 8일 — 7일 보존기간 밖
        dao.touchRequest(TARGET, "a".repeat(64), expiresAt, expiresAt.minusMinutes(10), COOLDOWN_SECONDS);
        LocalDateTime thresholdLikeScheduler = now.minusDays(7);

        dao.purgeExpiredCodes(thresholdLikeScheduler);

        assertThat(dao.selectByTarget(TARGET)).as("보존기간(7일)을 지난 만료 행은 삭제되어야 함").isNull();
    }
}
