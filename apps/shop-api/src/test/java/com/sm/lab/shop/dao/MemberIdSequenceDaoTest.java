// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.ID_SEQUENCES) 대상 통합 테스트(SR-231, FUNC-member-003) —
 * round1 QA FAIL 필수3(회원 ID 채번이 JVM 기동마다 리셋되는 in-memory {@code AtomicInteger}라
 * 재기동 후 PK 위반을 냈다)의 재작업 검증. {@link MemberDao#touchMemberIdSeq()}/
 * {@link MemberDao#selectLastMemberIdSeq()}는 값을 DB({@code ID_SEQUENCES} 테이블)에만
 * 두므로, 이 값을 만드는 JVM 프로세스가 재기동돼도(테스트를 재실행해도) 다음 채번은 항상
 * 이전 값보다 커야 한다.
 *
 * <p><b>round3(2026-09-12, round2 QA FAIL 재작업 지시 4번, 사람 결정) — "재기동 안전" 주장을
 * 실제로 커밋되는 두 트랜잭션으로 증명하도록 고쳤다.</b> round2까지의
 * {@code touchMemberIdSeq_calledTwiceSequentially_secondValueIsExactlyOneGreater}는 테스트
 * 메서드 전체를 {@code @Transactional}로 감싸 실행 후 자동 롤백했다 — 즉 이 테스트가 검증한
 * "두 번째 채번이 항상 첫 번째+1"이라는 성질은 **같은(아직 커밋되지 않은) 트랜잭션 안에서만**
 * 성립함을 보였을 뿐, "재기동 시뮬"(=프로세스가 몰랐던 이전 커밋 위에서도 이어서 증가하는가)은
 * 전혀 증명하지 못했다(round2 QA가 DB 실측 {@code ID_SEQUENCES.next_val=10}으로 대신 증명해야
 * 했던 이유). {@link #touchMemberIdSeq_calledInTwoSeparateCommittedTransactions_secondValueIsExactlyOneGreater}는
 * {@link TransactionTemplate}(PROPAGATION_REQUIRES_NEW)으로 각 호출 쌍을 별도의, 실제로
 * 커밋되는 트랜잭션에 담아 이 성질을 진짜로 증명한다 — 두 트랜잭션이 서로 다른 커넥션을 쓸 수
 * 있어도(트랜잭션마다 새 커넥션을 빌려도) 값은 DB에만 있으므로 항상 이어서 증가해야 한다.</p>
 */
@SpringBootTest
class MemberIdSequenceDaoTest {

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** 매 호출을 완전히 새로운, 실제로 커밋되는 트랜잭션으로 실행한다(REQUIRES_NEW). */
    private long touchAndReadInNewCommittedTransaction() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Long seq = tx.execute(status -> {
            memberDao.touchMemberIdSeq();
            return memberDao.selectLastMemberIdSeq();
        });
        return seq;
    }

    // round3 재작업(item 4) — 커밋되는 채번 2회를 비교해 "재기동 안전"을 실제로 증명한다.
    // 이 테스트는 (다른 @Transactional 테스트와 달리) 롤백되지 않는다 — ID_SEQUENCES.next_val이
    // 실행마다 영구히 증가하는 것 자체가 "DB에만 상태가 있다"는 증거이므로 정리(cleanup)하지
    // 않는다(다른 채번 테스트들과 마찬가지로 이 카운터를 공유 자원으로 취급).
    @Test
    void touchMemberIdSeq_calledInTwoSeparateCommittedTransactions_secondValueIsExactlyOneGreater() {
        long first = touchAndReadInNewCommittedTransaction();
        long second = touchAndReadInNewCommittedTransaction();

        assertThat(first).as("기존 시드 회원(M-0001~M-0004)보다 커야 함").isGreaterThan(0);
        assertThat(second)
                .as("서로 다른 커밋된 트랜잭션에서 호출해도 두 번째 채번은 항상 첫 번째+1이어야 함"
                        + "(round1의 AtomicInteger는 재기동 시 이 성질이 깨졌었다)")
                .isEqualTo(first + 1);
    }

    @Test
    @Transactional
    void touchMemberIdSeq_threeCallsInSameTransaction_areStrictlyIncreasing() {
        memberDao.touchMemberIdSeq();
        long v1 = memberDao.selectLastMemberIdSeq();
        memberDao.touchMemberIdSeq();
        long v2 = memberDao.selectLastMemberIdSeq();
        memberDao.touchMemberIdSeq();
        long v3 = memberDao.selectLastMemberIdSeq();

        assertThat(v2).isEqualTo(v1 + 1);
        assertThat(v3).isEqualTo(v2 + 1);
    }
}
