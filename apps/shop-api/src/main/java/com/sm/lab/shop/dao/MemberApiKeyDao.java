// linked_func: FUNC-member-005, FUNC-member-006
// SR-232 로그인(005, 최초 발급) + 로그아웃/리프레시(006 — issueIfAbsent를 조건부 rotate로 확장 +
// revokeByMemberId 신규, GATE-005 round2 인터페이스 경계표 근거)
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md, docs/00_FUNC/stories/STORY-FUNC-member-006.md
package com.sm.lab.shop.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 회원 API 키 전용 DAO(SR-232, {@code MEMBER_API_KEYS}) — 계획 상단 "전제": 정적
 * {@code lab.api-keys}(application.yml) 맵에는 admin·M-0001 두 항목뿐이라, 자가가입 회원
 * (FUNC-member-003)은 로그인에 성공해도 이후 {@code /api/**} 요청을 인증할 방법이 없었다.
 * 이 DAO가 그 간극을 메운다 — {@link com.sm.lab.shop.web.ApiKeyAuthFilter}가 정적 맵 조회에
 * 실패했을 때만(사람 확인 3) {@link #selectMemberIdByApiKey}로 폴백 조회한다.
 */
@Mapper
public interface MemberApiKeyDao {

    /** 이미 발급된 키 조회. 없으면 null(아직 로그인한 적 없음). */
    String selectByMemberId(@Param("memberId") String memberId);

    /**
     * linked_func: FUNC-member-006 — GATE-005 round2 인터페이스 경계표(사람 확정)에 따라
     * no-op UPSERT에서 <b>조건부 rotate</b>로 확장했다(시그니처는 005 그대로 불변). {@code member_id}가
     * PK인 단일 원자 UPSERT 문장 하나로 3가지 경우를 모두 처리한다:
     * <ol>
     *   <li>행이 없으면(최초 로그인) 그대로 INSERT.</li>
     *   <li>행이 있고 {@code revoked_at IS NOT NULL}(006이 로그아웃으로 폐기한 뒤의 refresh)이면
     *       이 메서드가 넘긴 새 {@code apiKey}/{@code issuedAt}으로 UPDATE하고 {@code revoked_at}을
     *       다시 NULL로 되돌린다 — 폐기된 키 문자열 자체는 절대 재사용하지 않는다(새 후보로 교체).</li>
     *   <li>행이 있고 {@code revoked_at IS NULL}(이미 유효한 키가 있음)이면 완전 no-op(005의 기존
     *       계약 그대로 유지 — 동시 최초 로그인/동시 refresh 레이스에서도 read-modify-write 없이
     *       DB가 흡수한다).</li>
     * </ol>
     * 호출부는 이 메서드 직후 반드시 {@link #selectByMemberId}로 재조회해 "누가 먼저 썼든" 실제
     * 저장된 값을 써야 한다(이 메서드가 넘긴 {@code apiKey} 후보가 그대로 쓰였다는 보장이 없다).
     */
    void issueIfAbsent(@Param("memberId") String memberId, @Param("apiKey") String apiKey,
                        @Param("issuedAt") LocalDateTime issuedAt);

    /**
     * linked_func: FUNC-member-006 — 로그아웃 시 이 회원의 API 키를 폐기한다(DELETE 아님, 계획
     * "005 인터페이스 경계표" 근거). {@code WHERE ... AND revoked_at IS NULL}로 이미 폐기된
     * 키에는 재실행해도 안전하다(idempotent — 중복 로그아웃 호출에도 부작용 없음).
     */
    int revokeByMemberId(@Param("memberId") String memberId, @Param("now") LocalDateTime now);

    /**
     * linked_func: FUNC-member-005 — 사람 확인 3(ApiKeyAuthFilter DB 폴백) 전용 역방향 조회.
     * {@code api_key}는 UNIQUE라 키→memberId 조회에 안전하게 쓸 수 있다. 정적 맵(application.yml
     * {@code lab.api-keys}) 조회가 실패했을 때만 호출된다(요청당 최대 1회).
     *
     * <p>round 9(재작업 지시 2, QA FAIL 필수2) — {@code MEMBERS} 조인 + {@code del_yn='N'} +
     * {@code revoked_at IS NULL}로 좁힌다. 탈퇴 회원(로그인이 delYn='Y'를 401로 막는 것과 동일
     * 규칙)이거나 폐기된 키(폐기는 FUNC-006 소관 — 006이 {@code revoked_at}을 세팅)는 결과
     * 없음(null)으로 반환돼 호출부가 종전과 동일한 401 unauthorized로 처리한다.
     */
    String selectMemberIdByApiKey(@Param("apiKey") String apiKey);
}
