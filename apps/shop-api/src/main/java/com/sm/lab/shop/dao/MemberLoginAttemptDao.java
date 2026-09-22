// linked_func: FUNC-member-005
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.MemberLoginAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 로그인 실패 카운터 + 잠금 전용 DAO(SR-232, {@code MEMBER_LOGIN_ATTEMPTS}). email 문자열
 * 자체가 PK — 회원 존재 여부와 무관하게 카운트한다(존재 판정 오라클 방지, STORY "데이터" 절).
 * {@code MemberSignupRateLimitDao}와 동일한 house 관례(원자 UPSERT + 재조회, 세션 변수 금지,
 * SET 목록 좌→우 평가만 사용)를 따른다.
 */
@Mapper
public interface MemberLoginAttemptDao {

    /**
     * 원자 UPSERT(사람 결정 — 클래스 단위 트랜잭션 없이 단일 문장, 세션변수 금지). 잠금이
     * 만료된 상태({@code locked_until <= now})에서 실패하면 {@code fail_count}를 1로 리셋하고
     * (무한 누적 방지), 그 외에는 1 증가시킨다. 증가 후 값이 {@code maxAttempts}에 도달하면
     * 같은 문장이 {@code locked_until = now + lockMinutes}를 함께 세팅한다(사람 확인 2 — N번째
     * 실패 자체가 잠금 트리거).
     *
     * <p>서비스는 이 메서드를 호출하기 전에 이미 {@link #selectAttempt}로 잠금 여부를 확인해
     * 잠긴 상태에서는 호출 자체를 하지 않는다(순서·보안 1) — 그래도 이 SQL은 방어적으로 잠금
     * 만료 판정을 스스로 다시 계산한다(만료 전 상태로 호출되면 카운터를 계속 늘리고 잠금
     * 윈도우를 갱신한다 — 정상 흐름에서는 발생하지 않는 경로).
     */
    void touchFailure(@Param("email") String email, @Param("now") LocalDateTime now,
                       @Param("lockMinutes") int lockMinutes, @Param("maxAttempts") int maxAttempts);

    /** 잠금·카운터 조회. 행이 없으면 null(=한 번도 실패한 적 없음). */
    MemberLoginAttempt selectAttempt(@Param("email") String email);

    /** 성공 시 실패카운터 reset(계획 "순서·보안" 6) — 단일 DELETE, 행이 없어도 0 반환. */
    int reset(@Param("email") String email);
}
