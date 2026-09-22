// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.MemberSignupVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface MemberSignupVerificationDao {

    /**
     * round4(SR-231 재작업 지시, 사람 결정) — 레이트리밋 판정({@link MemberSignupRateLimitDao}
     * #touchRateLimit)을 통과한 요청에만 실제 인증코드를 원자 UPSERT로 기록한다({@code INSERT
     * ... ON DUPLICATE KEY UPDATE} — 이 채널·타깃이 처음이면 신규 삽입, 재발송이면 이전
     * 코드를 완전히 덮어쓰고 {@code verified_at}을 초기화한다). {@code last_requested_at}은
     * 정보용(이 코드가 마지막으로 실제 발송된 시각)으로만 갱신한다 — round3까지와 달리
     * 더 이상 레이트리밋 판정에 쓰지 않는다(그 역할은 {@link MemberSignupRateLimitDao}로
     * 완전히 이전됨).
     * <p>SR-295 — 재발송(재발송 분기)은 {@code attempt_count=0}, {@code consumed_at=NULL}도
     * 함께 리셋한다. 잠긴({@code attempt_count>=5}) target이 재발송을 받으면 새 코드 행은
     * 시도횟수가 0부터 다시 시작해 MBR-4093 잠금에서 회복된다.
     */
    int writeCode(@Param("channel") String channel, @Param("target") String target,
                  @Param("code") String code, @Param("expiresAt") LocalDateTime expiresAt,
                  @Param("now") LocalDateTime now);

    /** 단건 조회(테스트 + 향후 인증확인 단계 재사용 목적) — 없으면 null. */
    MemberSignupVerification selectByChannelAndTarget(@Param("channel") String channel,
                                                        @Param("target") String target);

    /** 정리용 삭제(테스트 데이터 원복 등) — 없어도 0 반환. */
    int deleteByChannelAndTarget(@Param("channel") String channel, @Param("target") String target);

    /**
     * round4(STORY 재작업 지시(B)) — 요청 경로에서 완전히 제거되고
     * {@code MemberSignupMaintenanceScheduler}(@Scheduled fixedDelay=10분)로만 호출된다.
     * 코드가 실제로 만료된 행({@code expires_at < now})만 삭제한다 — 레이트리밋 카운터가
     * 이 테이블에 더 이상 없으므로(별도 테이블로 이전) round2/round3가 겪었던 "코드 만료
     * 삭제가 카운터도 함께 지운다" 결합 버그가 구조적으로 재발할 수 없다.
     */
    int purgeExpiredCodes(@Param("now") LocalDateTime now);
}
