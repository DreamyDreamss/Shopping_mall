// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.MEMBER_SIGNUP_VERIFICATIONS) 대상 통합 테스트(SR-231 round2,
 * FUNC-member-003) — round1 QA FAIL 필수1(코드 대입 시도 무제한)과 필수4(코드 재사용)를
 * {@link MemberSignupCompletionDao}의 조건부 UPDATE가 실제로 막는지 실 DB로 증명한다.
 *
 * <p>코드 자체는 {@link MemberSignupVerificationDao#writeCode}(FUNC-member-002 소유, 공개
 * API만 재사용 — 그 파일은 수정하지 않는다)로 시딩한다. {@code NOW(3)}(DB 서버 시각)에
 * 의존하는 문장이라 애플리케이션 Clock으로 결과를 제어할 수 없다 — 만료·미만료를 가르는
 * 테스트는 실제 벽시계 오프셋(예: -6분/-1분)으로 구성한다.
 */
@SpringBootTest
class MemberSignupCompletionDaoTest {

    private static final String CHANNEL = "EMAIL";
    private static final String TARGET = "completion-dao-test@example.com";
    private static final String CODE = "123456";
    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private MemberSignupCompletionDao dao;

    @Autowired
    private MemberSignupVerificationDao verificationDao;

    @AfterEach
    void cleanUp() {
        verificationDao.deleteByChannelAndTarget(CHANNEL, TARGET);
    }

    private void writeFreshCode(String code) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        verificationDao.writeCode(CHANNEL, TARGET, code, now.plusMinutes(5), now);
    }

    @Test
    void markVerifiedIfCodeMatches_correctCodeUnexpired_setsVerifiedAtAndReturnsOne() {
        writeFreshCode(CODE);

        int affected = dao.markVerifiedIfCodeMatches(CHANNEL, TARGET, CODE, MAX_ATTEMPTS);

        assertThat(affected).isEqualTo(1);
        assertThat(dao.selectVerifiedAt(CHANNEL, TARGET)).isNotNull();
    }

    @Test
    void markVerifiedIfCodeMatches_wrongCode_returnsZeroAndLeavesVerifiedAtNull() {
        writeFreshCode(CODE);

        int affected = dao.markVerifiedIfCodeMatches(CHANNEL, TARGET, "000000", MAX_ATTEMPTS);

        assertThat(affected).isEqualTo(0);
        assertThat(dao.selectVerifiedAt(CHANNEL, TARGET)).isNull();
    }

    @Test
    void markVerifiedIfCodeMatches_expiredCode_returnsZero() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        verificationDao.writeCode(CHANNEL, TARGET, CODE, now.minusMinutes(1), now.minusMinutes(6));

        int affected = dao.markVerifiedIfCodeMatches(CHANNEL, TARGET, CODE, MAX_ATTEMPTS);

        assertThat(affected).isEqualTo(0);
    }

    // round2 QA FAIL 필수1(사람 결정) — 대입 시도 상한(5회)에 도달하면 정답 코드도 더 이상
    // 매치되지 않는다(그 코드는 사실상 폐기 — 재발송만 가능).
    @Test
    void markVerifiedIfCodeMatches_afterMaxAttempts_rejectsEvenCorrectCode() {
        writeFreshCode(CODE);

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            int wrongAttempt = dao.markVerifiedIfCodeMatches(CHANNEL, TARGET, "999999", MAX_ATTEMPTS);
            assertThat(wrongAttempt).as("오답 %d회차는 0행이어야 함", i + 1).isEqualTo(0);
            dao.incrementAttemptCount(CHANNEL, TARGET, MAX_ATTEMPTS);
        }
        assertThat(dao.selectAttemptCount(CHANNEL, TARGET)).isEqualTo(MAX_ATTEMPTS);

        int affected = dao.markVerifiedIfCodeMatches(CHANNEL, TARGET, CODE, MAX_ATTEMPTS);

        assertThat(affected)
                .as("시도 상한(%d회) 도달 후에는 정답 코드도 거부돼야 함", MAX_ATTEMPTS)
                .isEqualTo(0);
        assertThat(dao.selectVerifiedAt(CHANNEL, TARGET)).isNull();
    }

    @Test
    void incrementAttemptCount_pendingRow_incrementsByOneEachCall() {
        writeFreshCode(CODE);

        dao.incrementAttemptCount(CHANNEL, TARGET, MAX_ATTEMPTS);
        dao.incrementAttemptCount(CHANNEL, TARGET, MAX_ATTEMPTS);

        assertThat(dao.selectAttemptCount(CHANNEL, TARGET)).isEqualTo(2);
    }

    @Test
    void incrementAttemptCount_alreadyVerifiedRow_doesNotIncrement() {
        writeFreshCode(CODE);
        dao.markVerifiedIfCodeMatches(CHANNEL, TARGET, CODE, MAX_ATTEMPTS);

        int affected = dao.incrementAttemptCount(CHANNEL, TARGET, MAX_ATTEMPTS);

        assertThat(affected).as("이미 인증된 행은 시도 횟수를 늘리지 않아야 함").isEqualTo(0);
        assertThat(dao.selectAttemptCount(CHANNEL, TARGET)).isEqualTo(0);
    }

    // SR-298(신규, 이 SR의 핵심 산출물) — incrementAttemptCount 자체가 attempt_count <
    // maxAttempts를 조건으로 갖는 원자 UPDATE라서, 상한(5)까지는 매 호출이 영향행수 1로 정확히
    // 1씩 늘리고 그 이후 호출은 영향행수 0이며 attempt_count는 상한에서 더 늘지 않는다 — 이
    // 캡이 read-modify-write 없이 SQL 조건절 하나로 지켜진다는 것을 실 DB로 증명한다.
    @Test
    void incrementAttemptCount_atCap_returnsZeroAndDoesNotExceedCap() {
        writeFreshCode(CODE);

        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            int affected = dao.incrementAttemptCount(CHANNEL, TARGET, MAX_ATTEMPTS);
            assertThat(affected).as("%d번째 증가는 영향행수 1이어야 함", i).isEqualTo(1);
            assertThat(dao.selectAttemptCount(CHANNEL, TARGET)).as("%d번째 증가 후 값", i).isEqualTo(i);
        }

        int overCap = dao.incrementAttemptCount(CHANNEL, TARGET, MAX_ATTEMPTS);

        assertThat(overCap).as("상한 도달 후 추가 호출은 영향행수 0이어야 함").isEqualTo(0);
        assertThat(dao.selectAttemptCount(CHANNEL, TARGET))
                .as("상한 도달 후에는 attempt_count가 %d를 넘지 않아야 함", MAX_ATTEMPTS)
                .isEqualTo(MAX_ATTEMPTS);
    }

    @Test
    void consumeVerifiedCode_afterVerified_setsConsumedAt() {
        writeFreshCode(CODE);
        dao.markVerifiedIfCodeMatches(CHANNEL, TARGET, CODE, MAX_ATTEMPTS);

        int affected = dao.consumeVerifiedCode(CHANNEL, TARGET);

        assertThat(affected).isEqualTo(1);
        assertThat(dao.selectConsumedAt(CHANNEL, TARGET)).isNotNull();
    }

    @Test
    void consumeVerifiedCode_calledTwice_secondCallIsNoOp() {
        writeFreshCode(CODE);
        dao.markVerifiedIfCodeMatches(CHANNEL, TARGET, CODE, MAX_ATTEMPTS);
        dao.consumeVerifiedCode(CHANNEL, TARGET);

        int secondAttempt = dao.consumeVerifiedCode(CHANNEL, TARGET);

        assertThat(secondAttempt).as("이미 소비된 행은 다시 소비되지 않아야 함").isEqualTo(0);
    }

    // round2 QA FAIL 필수4(사람 결정) — 가입 성공 후 소비된 코드는 같은 값으로 다시 제출해도
    // 검증되지 않는다(재사용 방지 — 이 케이스가 이 재작업의 핵심 회귀).
    @Test
    void markVerifiedIfCodeMatches_afterConsumed_rejectsSameCodeAgain() {
        writeFreshCode(CODE);
        dao.markVerifiedIfCodeMatches(CHANNEL, TARGET, CODE, MAX_ATTEMPTS);
        dao.consumeVerifiedCode(CHANNEL, TARGET);

        int affected = dao.markVerifiedIfCodeMatches(CHANNEL, TARGET, CODE, MAX_ATTEMPTS);

        assertThat(affected).as("이미 소비된 코드는 재사용될 수 없어야 함").isEqualTo(0);
    }
}
