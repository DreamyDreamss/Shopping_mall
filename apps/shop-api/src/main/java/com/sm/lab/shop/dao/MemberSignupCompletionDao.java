// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * SR-231 가입 요청 API(INF-MBR-002)의 인증 판정 + 소비 전용 DAO — {@code MEMBER_SIGNUP_VERIFICATIONS}
 * 테이블을 대상으로 하지만, 그 테이블을 소유한 {@link MemberSignupVerificationDao}(FUNC-member-002
 * 소유 파일)는 이 FUNC이 수정하지 않는다(파일 경계 — FUNC-member-002 관련 파일은 읽기 전용).
 * 대신 같은 테이블을 다루는 별도 매퍼(memberSignupCompletion.xml)를 둔다 — 두 FUNC이 각자
 * 소유한 쿼리·컬럼만 갖고, 서로의 매퍼 파일을 공유·수정하지 않는다.
 *
 * <p><b>round2(2026-09-12, SR-231 round1 QA FAIL 재작업 지시, 사람 결정 — 방식까지 지정)</b> —
 * round1은 "코드 일치+미만료"만 판정하고 무제한으로 재시도할 수 있었다(QA FAIL 필수1 — 무인증
 * 엔드포인트에서 6자리 코드 대입 공격 가능). round2는 다음 3가지를 이 DAO에 추가했다:
 * <ol>
 *   <li>{@link #markVerifiedIfCodeMatches} — {@code attempt_count < maxAttempts} 조건을
 *       더해 시도 횟수 상한을 넘으면 정답이어도 더 이상 매치되지 않게 한다.</li>
 *   <li>{@link #incrementAttemptCount} — 위 UPDATE가 0행(코드 불일치·만료·상한 초과)일 때만
 *       호출해 시도 횟수를 1 증가시킨다(별도 문장, 조건부 — 이미 소비·인증된 행은 건드리지 않음).</li>
 *   <li>{@link #consumeVerifiedCode} — 가입 성공 트랜잭션 안에서 코드를 소비 처리(
 *       {@code consumed_at} 기록)해 같은 코드의 재사용을 막는다(QA FAIL 필수4 — round1은
 *       가입 성공 후에도 코드가 만료까지 그대로 유효했다).</li>
 * </ol>
 * 판정 방식은 여전히 select→분기→update가 아니라 "조건부 UPDATE의 affected-rows"다(project-context.md
 * Critical Rule 3, house rule) — 이 DAO의 모든 쓰기가 그 원칙을 따른다.
 *
 * <p><b>SR-298(2026-09-16, 병렬 버스트 우회 차단, 사람 결정)</b> — round2의
 * {@link #incrementAttemptCount}는 "select→분기→update"였다(호출부가 먼저
 * {@link #selectAttemptCount}로 현재 값을 읽어 상한 도달 여부를 판정한 뒤에만 호출) — 두
 * 호출 사이의 간극에서 동시에 도착한 다수 요청이 모두 "아직 상한 미만"으로 읽어 상한(5)을
 * 넘겨 증가시킬 수 있었다(레이스). 이제 이 메서드 자체가 {@code attempt_count < maxAttempts}를
 * 조건에 포함한 단일 원자 UPDATE다 — 호출부는 이 한 문장의 영향행수만으로 성공(1)/상한
 * 도달-또는-매치없음(0)을 판별하며, 0일 때만 원인 구분용으로 {@link #selectAttemptCount}를
 * 다시 읽는다(그 값으로 다시 증가시키지 않음 — house rule 위반 아님, 오류 코드 분기 전용).
 *
 * <p><b>round1 대비 제거됨 — {@code selectVerifiedAt} 기반 30분 창 판정</b>: round1은 이 API
 * 자체가 코드를 검증하는데도 "최근 30분 이내 verified_at"이라는 별도 신선도 창을 뒀다(원래
 * STORY 문면이 "먼저 인증된 뒤 이 API가 나중에 호출되는" 2단계 흐름을 전제했던 흔적). round2
 * 설계에서는 검증(verified_at 기록)과 소비(consumed_at 기록)가 같은 트랜잭션 안에서 곧바로
 * 이어지므로, 방금 세팅한 verified_at은 정의상 항상 "신선"하다 — 별도 창 판정은 불필요한
 * 복잡도만 남겨 제거했다({@link #selectVerifiedAt}는 테스트/조회 전용으로만 남긴다).
 */
@Mapper
public interface MemberSignupCompletionDao {

    /**
     * {@code channel}+{@code target}이 일치하고, 제출된 {@code code}가 저장된 코드와 같고,
     * 아직 만료되지 않았고(expires_at > now), 아직 소비·인증된 적 없고(consumed_at/verified_at
     * IS NULL), 시도 횟수가 상한 미만({@code attempt_count < maxAttempts})인 행에만
     * {@code verified_at = NOW(3)}을 기록한다. 조건에 맞지 않으면(코드 불일치·만료·이미
     * 소비/인증됨·시도 상한 초과·행 없음) 0을 반환하고 아무 것도 바뀌지 않는다 — 호출부는 0이면
     * {@link #incrementAttemptCount}를 호출한 뒤 409로 거부해야 한다.
     */
    int markVerifiedIfCodeMatches(@Param("channel") String channel, @Param("target") String target,
                                   @Param("code") String code, @Param("maxAttempts") int maxAttempts);

    /**
     * 위 {@link #markVerifiedIfCodeMatches}가 0행일 때만 호출 — 아직 소비·인증되지 않고
     * {@code attempt_count}가 {@code maxAttempts} 미만인 행에 한해서만 1 증가시킨다(SR-298 —
     * 이 조건 자체가 증가와 상한 판정을 한 문장으로 원자화한다, read-modify-write 아님. 이
     * UPDATE가 잡는 행 락이 동시 도착 요청들을 이 한 문장의 수명 동안만 직렬화하므로 상한을
     * 넘겨 증가하는 레이스가 불가능하다). 0을 반환하면 이미 소비·인증된 행이거나, 매치되는
     * 행이 없거나, 이미 상한에 도달한 행이라는 뜻이다 — 호출부는 그 중 어느 사유인지
     * {@link #selectAttemptCount}로 다시 읽어 오류 코드만 분기해야 한다(그 값으로 다시
     * 증가시키지 않는다 — 증가는 이미 이 UPDATE에서 원자적으로 끝났다).
     */
    int incrementAttemptCount(@Param("channel") String channel, @Param("target") String target,
                              @Param("maxAttempts") int maxAttempts);

    /**
     * 가입 성공 트랜잭션 안에서 코드를 소비 처리한다({@code consumed_at = NOW(3)}) — 인증된
     * (verified_at이 있는) 적 있고 아직 소비되지 않은 행에만 적용된다. 이후 같은 코드로 다시
     * {@link #markVerifiedIfCodeMatches}를 호출하면 {@code consumed_at IS NOT NULL}이라 항상
     * 0행이 되어 재사용이 막힌다.
     */
    int consumeVerifiedCode(@Param("channel") String channel, @Param("target") String target);

    /** 테스트/조회 전용 — 인증 시각. 행이 없거나 인증된 적 없으면 null. */
    LocalDateTime selectVerifiedAt(@Param("channel") String channel, @Param("target") String target);

    /** 테스트/조회 전용 — 소비 시각. 행이 없거나 아직 소비되지 않았으면 null. */
    LocalDateTime selectConsumedAt(@Param("channel") String channel, @Param("target") String target);

    /** 테스트/조회 전용 — 현재까지 누적된 대입 시도 횟수. 행이 없으면 null. */
    Integer selectAttemptCount(@Param("channel") String channel, @Param("target") String target);
}
