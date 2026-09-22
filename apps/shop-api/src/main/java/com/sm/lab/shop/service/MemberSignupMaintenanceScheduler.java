// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberSignupRateLimitDao;
import com.sm.lab.shop.dao.MemberSignupVerificationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * round4(SR-231, 사람 결정 — STORY-FUNC-member-002 재작업 지시(B)) — 만료된 인증코드 행과
 * 지난 날짜의 레이트리밋 카운터 행을 요청 경로가 아니라 배치로 정리한다.
 *
 * <p>round1~3는 무인증 엔드포인트의 매 요청마다 정리 쿼리(purgeExpired)를 실행했다 — 요청
 * 경로 안에서 도는 정리 쿼리가 (a) 락 범위를 넓혀 InnoDB 데드락의 한 원인이 됐고(round3 QA
 * FAIL 필수1), (b) 요청이 몰리면 그만큼 정리 쿼리도 몰리는 부하 증폭 구조였다. round4는 이
 * 정리를 요청 처리와 완전히 분리해, 10분마다 한 번만(응답 지연·락 경합과 무관하게) 실행한다.
 *
 * <p>round6 — 이 클래스의 {@code @Scheduled}가 실제로 예약 실행되려면
 * {@link MemberSignupSchedulingConfig}의 {@code @EnableScheduling}이 활성화돼 있어야 한다
 * (테스트 컨텍스트에서는 조건부로 비활성화됨 — 그 클래스 javadoc 참고). 이 빈 자체는
 * {@code @Component}라 스케줄링 활성화 여부와 무관하게 항상 생성된다.
 */
@Component
public class MemberSignupMaintenanceScheduler {
    private static final Logger log = LoggerFactory.getLogger(MemberSignupMaintenanceScheduler.class);

    private final MemberSignupVerificationDao verificationDao;
    private final MemberSignupRateLimitDao rateLimitDao;
    private final Clock clock;

    // round3에서 겪은 "생성자 2개(운영용/테스트용) 사이 Spring 기동 모호성"과 같은 문제를
    // 피하기 위해 @Autowired로 명시 지정한다(MemberSignupService와 동일 패턴).
    @Autowired
    public MemberSignupMaintenanceScheduler(MemberSignupVerificationDao verificationDao,
                                             MemberSignupRateLimitDao rateLimitDao) {
        this(verificationDao, rateLimitDao, Clock.systemDefaultZone());
    }

    /** 시계 주입 테스트 시임(package-private, 테스트 전용). */
    MemberSignupMaintenanceScheduler(MemberSignupVerificationDao verificationDao,
                                      MemberSignupRateLimitDao rateLimitDao, Clock clock) {
        this.verificationDao = verificationDao;
        this.rateLimitDao = rateLimitDao;
        this.clock = clock;
    }

    /**
     * 10분마다 1회 실행(요청 처리와 무관). {@code initialDelay}를 둬 애플리케이션 기동 직후
     * (테스트의 {@code @SpringBootTest} 컨텍스트 기동 포함) 곧바로 실행되지 않게 한다 —
     * 테스트가 스스로 심어 둔 행을 이 배치가 우연히 먼저 정리해버리는 간섭을 피한다.
     */
    @Scheduled(initialDelay = 600_000, fixedDelay = 600_000)
    public void purgeExpiredSignupData() {
        LocalDateTime now = LocalDateTime.now(clock);
        int purgedCodes = verificationDao.purgeExpiredCodes(now);
        int purgedRateLimits = rateLimitDao.purgeOldRows(now.toLocalDate());
        if (purgedCodes > 0 || purgedRateLimits > 0) {
            log.info("회원가입 인증 데이터 정리(배치) — 만료 코드 {}건, 지난 날짜 레이트리밋 {}건 삭제",
                    purgedCodes, purgedRateLimits);
        }
    }
}
