package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.MemberPasswordResetRateLimit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 코드 요청 일일 상한 카운터 전용 DAO(SR-297). 코드 테이블을 다루는
 * {@link MemberPasswordResetDao}와 완전히 분리된 테이블(MEMBER_PASSWORD_RESET_RATE_LIMITS)을
 * 다뤄 락이 섞이지 않는다.
 *
 * <p>#1(BAT-MBR-001)이 정리 전용 메서드 2개({@code purgeOldRows}/{@code deleteRateLimit})를
 * 먼저 만들었다. 일일 상한 원자 판정(UPSERT)·조회 메서드({@code touchDailyLimit}/
 * {@code selectRateLimit})는 #2가 만듦(INF-MBR-006, 이 항목이 처음으로
 * daily_count/last_requested_at/last_token 세 컬럼을 읽고 쓴다).
 */
@Mapper
public interface MemberPasswordResetRateLimitDao {

    /**
     * 원자 판정 UPSERT(#2, STORY "순서·보안" 결정1) — {@code INSERT ... ON DUPLICATE KEY
     * UPDATE} 한 문장(autocommit, 서비스 쪽에 트랜잭션 없음)으로 쿨다운({@code cooldownSeconds})과
     * 일일상한({@code dailyLimit})을 동시에 만족할 때만 {@code daily_count}/
     * {@code last_requested_at}/{@code last_token}을 갱신한다. 조건 미달이면 그 UPDATE는 IF의
     * else 분기로 기존 값을 그대로 대입하므로(last_token 포함) 실제 행 변경이 없다.
     *
     * <p>판정은 이 문장의 반환값(affected rows)이 아니라, 뒤이은 {@link #selectRateLimit}가 읽는
     * <b>요청 토큰</b>({@code last_token})으로 한다 — {@link MemberSignupRateLimitDao#touchRateLimit}
     * round5/round6과 동일한 이유·동일한 매퍼 SQL 모양(세션 변수 없이 SET 목록 좌→우 평가만으로
     * 결정적)을 그대로 이식했다.
     */
    void touchDailyLimit(@Param("target") String target, @Param("dayKey") LocalDate dayKey,
                          @Param("now") LocalDateTime now, @Param("cooldownSeconds") int cooldownSeconds,
                          @Param("dailyLimit") int dailyLimit, @Param("token") String token);

    /** 거부 사유(쿨다운 vs 일일상한) 판별 + 게이트 판정(재조회) + 테스트용 조회. 없으면 null. */
    MemberPasswordResetRateLimit selectRateLimit(@Param("target") String target, @Param("dayKey") LocalDate dayKey);

    /**
     * 배치 전용 — {@code MemberPasswordResetMaintenanceScheduler}(@Scheduled)만 호출한다.
     * 오늘보다 이전 날짜의 행만 삭제한다(오늘 카운터는 절대 건드리지 않음). 조건부 DELETE라
     * 재실행해도 결과가 같다(멱등).
     */
    int purgeOldRows(@Param("beforeDay") LocalDate beforeDay);

    /** 정리용 삭제(테스트 데이터 원복 전용) — 없어도 0 반환. */
    int deleteRateLimit(@Param("target") String target, @Param("dayKey") LocalDate dayKey);
}
