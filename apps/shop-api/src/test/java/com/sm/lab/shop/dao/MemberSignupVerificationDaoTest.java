// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.MemberSignupVerification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.MEMBER_SIGNUP_VERIFICATIONS) 대상 통합 테스트(SR-231, FUNC-member-002).
 * 순수 신규 테이블이라 각 테스트 후 삽입분을 정리한다(CartDaoTest 관례와 동일).
 *
 * <p>round4(사람 결정 — "설계를 단순화해서 데드락 원인을 없앤다") — 레이트리밋 카운터가
 * 전용 테이블(MEMBER_SIGNUP_RATE_LIMITS, {@link MemberSignupRateLimitDaoTest})로 완전히
 * 옮겨져, 이 DAO는 이제 코드(code/expires_at/verified_at) 기록만 책임진다. {@code writeCode}는
 * 더 이상 "레이트리밋 판정을 통과한 뒤 카운터가 만들어 둔 행에 덧쓰는" 것이 아니라, 그 자체로
 * 완결된 원자 UPSERT다(선행 호출 불필요).
 */
@SpringBootTest
class MemberSignupVerificationDaoTest {

    private static final String CHANNEL = "EMAIL";
    private static final String TARGET = "dao-test@example.com";
    private static final String EXPIRED_TARGET = "dao-test-expired@example.com";

    @Autowired
    private MemberSignupVerificationDao dao;

    @Autowired
    private MemberSignupCompletionDao completionDao;

    @AfterEach
    void cleanUp() {
        dao.deleteByChannelAndTarget(CHANNEL, TARGET);
        dao.deleteByChannelAndTarget(CHANNEL, EXPIRED_TARGET);
    }

    @Test
    void writeCode_newTarget_insertsCodeAndClearsVerifiedAt() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        int updated = dao.writeCode(CHANNEL, TARGET, "123456", now.plusMinutes(5), now);

        assertThat(updated).isGreaterThan(0);
        MemberSignupVerification saved = dao.selectByChannelAndTarget(CHANNEL, TARGET);
        assertThat(saved).isNotNull();
        assertThat(saved.getCode()).isEqualTo("123456");
        assertThat(saved.getExpiresAt()).isEqualTo(now.plusMinutes(5));
        assertThat(saved.getVerifiedAt()).isNull();
        assertThat(saved.getLastRequestedAt()).isEqualTo(now);
        assertThat(saved.getAttemptCount()).isEqualTo(0);
    }

    // round4 — 재요청은 writeCode를 다시 호출하는 것만으로 처리된다(별도 선행 카운터 호출
    // 불필요). 이전 코드를 완전히 덮어쓰고 verified_at을 초기화한다(재발송 = 이전 코드 폐기).
    @Test
    void writeCode_reissue_overwritesCodeAndExpiryAndResetsVerifiedAt() {
        LocalDateTime first = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.writeCode(CHANNEL, TARGET, "111111", first.plusMinutes(5), first);

        LocalDateTime second = first.plusMinutes(2);
        dao.writeCode(CHANNEL, TARGET, "222222", second.plusMinutes(5), second);

        MemberSignupVerification saved = dao.selectByChannelAndTarget(CHANNEL, TARGET);
        assertThat(saved.getCode()).isEqualTo("222222");
        assertThat(saved.getExpiresAt()).isEqualTo(second.plusMinutes(5));
        assertThat(saved.getLastRequestedAt()).isEqualTo(second);
        assertThat(saved.getVerifiedAt()).isNull();
    }

    // SR-295 — 재발송(writeCode 재호출)은 잠금(MBR-4093) 회복 경로다: 시도 횟수를 이미
    // 상한 근처까지 올리고 검증·소비까지 마친 행이라도, 재발송이 attempt_count/consumed_at을
    // 0/NULL로 리셋해 새 코드로 다시 정상 검증할 수 있게 한다. 상태는 원시 SQL이 아니라
    // MemberSignupCompletionDao(FUNC-member-003)의 실제 프로덕션 경로로 만든다.
    @Test
    void writeCode_reissue_resetsAttemptCountAndConsumedAt() {
        LocalDateTime first = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.writeCode(CHANNEL, TARGET, "111111", first.plusMinutes(5), first);

        for (int i = 0; i < 3; i++) {
            completionDao.incrementAttemptCount(CHANNEL, TARGET, 5);
        }
        completionDao.markVerifiedIfCodeMatches(CHANNEL, TARGET, "111111", 5);
        completionDao.consumeVerifiedCode(CHANNEL, TARGET);

        LocalDateTime second = first.plusMinutes(2);
        dao.writeCode(CHANNEL, TARGET, "222222", second.plusMinutes(5), second);

        MemberSignupVerification reissued = dao.selectByChannelAndTarget(CHANNEL, TARGET);
        assertThat(reissued.getCode()).isEqualTo("222222");
        assertThat(reissued.getAttemptCount()).isEqualTo(0);
        assertThat(reissued.getVerifiedAt()).isNull();
        assertThat(completionDao.selectConsumedAt(CHANNEL, TARGET)).isNull();
    }

    @Test
    void selectByChannelAndTarget_notExisting_returnsNull() {
        MemberSignupVerification result = dao.selectByChannelAndTarget(CHANNEL, "no-such-target@example.com");

        assertThat(result).isNull();
    }

    // round4(STORY 재작업 지시(B)) — purgeExpiredCodes는 요청 경로 밖(배치)에서만 호출된다.
    // 카운터가 이 테이블에 더 이상 없으므로 코드 만료 삭제가 카운터에 영향을 줄 수 없다
    // (round2/round3가 겪은 결합 버그가 구조적으로 재발할 수 없음을 실증).
    @Test
    void purgeExpiredCodes_deletesOnlyExpiredCodeRows() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.writeCode(CHANNEL, EXPIRED_TARGET, "999999", now.minusMinutes(1), now.minusMinutes(6)); // 이미 만료
        dao.writeCode(CHANNEL, TARGET, "123456", now.plusMinutes(5), now); // 아직 유효

        dao.purgeExpiredCodes(now);

        assertThat(dao.selectByChannelAndTarget(CHANNEL, EXPIRED_TARGET)).isNull();
        MemberSignupVerification kept = dao.selectByChannelAndTarget(CHANNEL, TARGET);
        assertThat(kept).isNotNull();
        assertThat(kept.getCode()).isEqualTo("123456");
    }
}
