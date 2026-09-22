// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberSignupCompletionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SR-231(FUNC-member-003) round3 재작업(2026-09-12, round2 QA FAIL 재작업 지시, 사람 결정 —
 * 마지막 라운드·3단계 흐름과 정확한 SQL·트랜잭션 경계를 지정) — "가입" 단계(ID 채번 → MEMBERS
 * INSERT → verification {@code consumed_at} UPDATE, 이 세 문장만)의 트랜잭션 경계 전담 협력자.
 *
 * <p><b>왜 별도 스프링 빈으로 떼어냈는가</b> — round2 QA FAIL 필수1(재발)의 근본 원인은
 * {@code incrementAttemptCount}가 {@code signUp()}의 {@code @Transactional} 메서드 "안"에서
 * 호출돼, 뒤이어 던진 런타임 예외가 그 증가분까지 함께 롤백시킨 것이었다 — 즉 "비트랜잭션이어야
 * 할 코드"가 우연히 트랜잭션 안에 갇혔다. round3는 이 문제를 구조적으로 재발할 수 없게 만든다:
 * "사전 판정"·"코드 검증"(비트랜잭션)과 "가입"(트랜잭션)을 애초에 서로 다른 스프링 빈으로
 * 분리했다. {@link MemberRegistrationService#signUp}은 어떤 {@code @Transactional}도 갖지
 * 않는 순수 오케스트레이션 메서드이고, 이 클래스의 {@link #completeSignup} 메서드만
 * {@code @Transactional}이다 — 자기호출(self-invocation, 같은 클래스 안에서 프록시를 거치지
 * 않는 메서드 호출이라 트랜잭션 어드바이스가 적용되지 않는 Spring AOP의 잘 알려진 함정)이
 * 원천적으로 발생할 수 없는 구조다. 클래스 단위 {@code @Transactional}은 여전히 쓰지 않는다
 * (사람 결정 "클래스 단위 @Transactional 금지, 메서드마다 명시") — 이 클래스에 메서드가
 * 하나뿐이라도 애노테이션은 클래스가 아니라 이 메서드 위에 붙인다.
 */
@Service
public class MemberSignupCompletionWriter {

    private final MemberDao memberDao;
    private final MemberSignupCompletionDao completionDao;

    @Autowired
    public MemberSignupCompletionWriter(MemberDao memberDao, MemberSignupCompletionDao completionDao) {
        this.memberDao = memberDao;
        this.completionDao = completionDao;
    }

    /**
     * "가입" 세 문장(사람 결정 그대로) — ① ID 채번({@link MemberDao#touchMemberIdSeq()} +
     * {@link MemberDao#selectLastMemberIdSeq()} — {@code LAST_INSERT_ID()}가 세션 스코프라
     * 반드시 같은 커넥션, 즉 이 트랜잭션 안에서 짝을 이뤄야 한다. 회원 ID 채번 자체는 SQL
     * 호출이 둘이지만 하나의 논리적 "단계"다 — round2와 동일 해석) ② {@link MemberDao#insertMember}
     * ③ {@link MemberSignupCompletionDao#consumeVerifiedCode}. 그 외 문장(예: 이벤트 발행)은
     * 이 메서드에 넣지 않는다 — 호출부({@link MemberRegistrationService})가 이 메서드가 정상
     * 반환한 뒤(커밋 후)에 처리한다.
     *
     * <p>{@link org.springframework.dao.DuplicateKeyException}은 이 메서드 안에서 잡지 않고
     * 그대로 던진다 — 업무 코드(이메일/휴대폰 중복 vs 미분류 PK 충돌 재시도) 변환은 호출부의
     * 책임이다(호출부가 제약 이름으로 분기해야 하므로, 그 판단 로직까지 이 협력자에 넣으면
     * "이 세 문장만"이라는 트랜잭션 범위를 벗어난 책임을 이 클래스가 떠안게 된다).
     */
    @Transactional
    public String completeSignup(String channel, String target, String memberName, String grade,
                                  String email, String phone, String phoneNorm, String passwordHash,
                                  boolean marketingOptIn, LocalDateTime now) {
        memberDao.touchMemberIdSeq();
        long seq = memberDao.selectLastMemberIdSeq();
        String memberId = String.format("M-%04d", seq);

        memberDao.insertMember(memberId, memberName, grade, email, phone, phoneNorm, passwordHash,
                marketingOptIn, now);
        completionDao.consumeVerifiedCode(channel, target);
        return memberId;
    }
}
