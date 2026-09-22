// linked_func: FUNC-member-008, FUNC-member-009
// spec: docs/00_FUNC/stories/STORY-FUNC-member-008.md, docs/00_FUNC/stories/STORY-FUNC-member-009.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.MemberPasswordReset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 코드 요청 전용 DAO(SR-234, FUNC-member-008). MEMBER_PASSWORD_RESETS
 * 단일 테이블만 다룬다 — 회원 테이블은 이 DAO도, 이 DAO를 쓰는 서비스도 조회하지 않는다
 * (STORY "순서·보안" 3 — 존재 오라클을 만들지 않기 위한 구조적 강제).
 */
@Mapper
public interface MemberPasswordResetDao {

    /**
     * 원자 판정 UPSERT — {@code INSERT ... ON DUPLICATE KEY UPDATE} 한 문장(autocommit, 이
     * 서비스는 {@code @Transactional}을 쓰지 않는다)으로 쿨다운({@code cooldownSeconds})을
     * 만족할 때만 {@code code_hash}/{@code expires_at}/{@code consumed_at}/{@code attempt_count}/
     * {@code created_at}을 갱신한다. 신규 target이면 그대로 삽입(쿨다운 판정 자체가 없음).
     *
     * <p>SET 목록에서 {@code created_at}을 반드시 맨 뒤에 둔다 — MySQL/MariaDB는 UPDATE의 SET을
     * 좌→우로 평가하므로(문서화된 동작), 앞선 IF 조건들의 {@code created_at} 참조가 전부
     * 갱신 전(원본) 값을 보게 하기 위함이다(MEMBER_SIGNUP_RATE_LIMITS round6과 동일 원리). 세션
     * 변수나 datasource {@code useAffectedRows} 반환값 판정에는 의존하지 않는다 — 이 서비스는
     * 애초에 UPSERT의 반환값을 쓰지 않는다(응답이 항상 202이므로 판정 자체가 불필요).
     */
    void touchRequest(@Param("target") String target, @Param("codeHash") String codeHash,
                       @Param("expiresAt") LocalDateTime expiresAt, @Param("now") LocalDateTime now,
                       @Param("cooldownSeconds") int cooldownSeconds);

    /** 단건 조회(테스트 + FUNC-member-009 확정 API 재사용) — 없으면 null. */
    MemberPasswordReset selectByTarget(@Param("target") String target);

    /** 정리용 삭제(테스트 데이터 원복 전용) — 없어도 0 반환. */
    int deleteByTarget(@Param("target") String target);

    /**
     * linked_func: FUNC-member-009 — 원자 확정 UPDATE(비트랜잭션, autocommit 단일 문장 — 이 서비스는
     * {@code @Transactional}을 쓰지 않는다, 사례집 SR-231 r2 재발 방지). 코드가 일치하고
     * ({@code code_hash = #{codeHash}}), 아직 만료되지 않았고({@code expires_at > #{now}}), 아직
     * 소비된 적 없고({@code consumed_at IS NULL}), 시도 횟수가 상한 미만
     * ({@code attempt_count < #{maxAttempts}})인 행에만 {@code consumed_at}을 세팅한다. 0행이면
     * 코드 불일치·만료·이미 소비·시도 초과·행 없음 중 하나다 — 호출부가 {@link #selectByTarget}로
     * 재조회해 사유를 구분한다(STORY "순서·보안" 5단계, 사람 수정 (1) — "행 없음"도 오답과 완전히
     * 동일한 코드로 수렴시킨다).
     */
    int confirmIfCodeMatches(@Param("target") String target, @Param("codeHash") String codeHash,
                              @Param("now") LocalDateTime now, @Param("maxAttempts") int maxAttempts);

    /**
     * linked_func: FUNC-member-009 — 위 {@link #confirmIfCodeMatches}가 0행이면 서비스가
     * 원인 판별보다 먼저 무조건 호출한다(행 없음 여부는 이 문장이 WHERE절로 판정한다). 이 문장 자체가
     * {@code attempt_count < #{maxAttempts}}를 조건으로 갖는 원자 증가다(SR-298 — read-modify-write
     * 제거, select→분기→update 금지 하우스룰 정합). 아직 만료·소비되지 않은
     * 행({@code consumed_at IS NULL AND expires_at > #{now}})만 대상이며, 반환값 0은 상한 도달·
     * 이미 소비·이미 만료·행 없음 중 하나를 뜻한다(호출부가 {@link #selectByTarget}로 원인만
     * 재조회해 구분 — 재증가는 하지 않는다). 각 증가는 그 자체로 원자 UPDATE 문이라 동시 여러
     * 오답 시도에도(InnoDB 행 잠금이 직렬화) 카운터가 유실되지도, 상한을 넘겨 늘어나지도 않는다.
     */
    int incrementAttemptCount(@Param("target") String target, @Param("now") LocalDateTime now,
                               @Param("maxAttempts") int maxAttempts);

    /**
     * SR-297 #1(BAT-MBR-001) — 요청 경로에서 완전히 제거되고
     * {@code MemberPasswordResetMaintenanceScheduler}(@Scheduled)로만 호출된다.
     * {@code expires_at < #{beforeExpiresAt}}인 행만 삭제한다(조건부 DELETE, 재실행해도
     * 결과가 같아 멱등) — 요청 경로 트랜잭션과 분리된 독립 autocommit 문장이다.
     *
     * <p>round1 재작업(사람 결정) — 파라미터는 "지금"이 아니라 호출부가 미리 계산한 보존
     * 기준 시각이다({@code now - member.password-reset.purge-retention}, 기본 7일). 만료
     * 직후 즉시 삭제하면 확정 API({@code MemberPasswordResetConfirmationService}
     * #handleConfirmFailure)가 "행 있음=410(만료)/행 없음=409(오답)"로 갈리는 오류 계약상
     * "어제 받은 코드로 오늘 확정 시도"가 곧바로 410→409로 수렴해 화면(FUNC-member-007)의
     * "재요청" 전이가 끊긴다(round1 QA CONCERNS 권고1). 보존 기간 안에는 만료된 행도 남겨
     * 410을 유지하고, 보존 기간을 지난 행만 이 배치가 지운다.
     */
    int purgeExpiredCodes(@Param("beforeExpiresAt") LocalDateTime beforeExpiresAt);
}
