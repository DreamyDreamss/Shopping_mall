package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberPasswordResetDao;
import com.sm.lab.shop.dao.MemberPasswordResetRateLimitDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * SR-297 #1(BAT-MBR-001, 사람 게이트 결정 — 정리 배치 전용) — 만료된 비밀번호 재설정 코드
 * 행과 지난 날짜의 일일 상한 카운터 행을 요청 경로가 아니라 배치로 정리한다.
 * {@code MemberSignupMaintenanceScheduler}(SR-231 round4)와 동일한 구조를 미러링한다 —
 * 정리를 요청 처리와 완전히 분리해 요청 경로 트랜잭션과 락이 섞이지 않게 한다(사례집 SR-231
 * round3 QA FAIL 필수1 재발 방지).
 *
 * <p>새 {@code @Configuration}/{@code @EnableScheduling}을 만들지 않는다 — 기존
 * {@link MemberSignupSchedulingConfig}의 전역 {@code @EnableScheduling}(앱 컨텍스트 전체에
 * 대한 스위치, 빈 단위 아님)을 그대로 재사용한다. 그 스위치가 전역이라 이 빈의
 * {@code @Scheduled}도 코드 변경 없이 자동 포착된다.
 *
 * <p><b>round1 재작업(사람 결정) — 만료 즉시 삭제하지 않는다.</b> 만료 코드 행을 곧바로 지우면
 * 확정 API({@link MemberPasswordResetConfirmationService}#handleConfirmFailure)의 오류 계약이
 * 사실상 바뀐다 — 그 서비스는 행이 남아 있으면 410({@code MBR-4101}, 만료), 행이 없으면
 * 409({@code MBR-4102}, 오답)로 가르는데, 배치가 만료 행을 지우면 "어제 받은 코드로 오늘 확정
 * 시도"가 410→409로 수렴하고 화면(FUNC-member-007)은 410에서만 "재요청" 전이를 하므로 사용자가
 * 2단계에서 재시도를 반복한다(round1 QA CONCERNS 권고1). 이 배치는 만료 후 보존 기간
 * ({@code member.password-reset.purge-retention}, 기본 {@code P7D} = 7일)이 지난 행만
 * 지운다 — 그 창 안에서는 만료된 코드로 확정을 시도해도 여전히 410을 받는다.
 */
@Component
public class MemberPasswordResetMaintenanceScheduler {
    private static final Logger log = LoggerFactory.getLogger(MemberPasswordResetMaintenanceScheduler.class);

    private final MemberPasswordResetDao passwordResetDao;
    private final MemberPasswordResetRateLimitDao rateLimitDao;
    private final Clock clock;
    private final Duration purgeRetention;

    @Autowired
    public MemberPasswordResetMaintenanceScheduler(MemberPasswordResetDao passwordResetDao,
                                                     MemberPasswordResetRateLimitDao rateLimitDao,
                                                     @Value("${member.password-reset.purge-retention:P7D}")
                                                     Duration purgeRetention) {
        this(passwordResetDao, rateLimitDao, Clock.systemDefaultZone(), purgeRetention);
    }

    /** 시계·보존기간 주입 테스트 시임(package-private, 테스트 전용). */
    MemberPasswordResetMaintenanceScheduler(MemberPasswordResetDao passwordResetDao,
                                             MemberPasswordResetRateLimitDao rateLimitDao, Clock clock,
                                             Duration purgeRetention) {
        this.passwordResetDao = passwordResetDao;
        this.rateLimitDao = rateLimitDao;
        this.clock = clock;
        this.purgeRetention = purgeRetention;
    }

    /**
     * 일 1회(새벽, 주기는 {@code lab.batch.password-reset-cleanup.cron} 설정값). 코드 행
     * 정리 → 카운터 행 정리 순서로 실행하고, 부수효과(로그)는 두 DELETE가 모두 끝난 뒤에만
     * 낸다(STORY "순서·보안" 절 — 판정/작업 뒤에 부수효과 원칙, RUN9 008 r1 재발 방지). 삭제
     * 건수만 로그로 남기고 target/이메일 원문은 남기지 않는다.
     *
     * <p>코드 행은 {@code expires_at < now - purgeRetention}(보존 기간을 지난 행)만 대상이다
     * (round1 재작업 — 클래스 상단 설명 참고). 카운터 행({@code day_key < 오늘})은 보존 기간
     * 대상이 아니다 — 그대로 즉시 삭제를 유지한다(사람 결정, round1 재작업 지시 범위 한정).
     */
    @Scheduled(cron = "${lab.batch.password-reset-cleanup.cron:0 0 3 * * *}")
    public void purgeExpiredPasswordResetData() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime beforeExpiresAt = now.minus(purgeRetention);
        int purgedCodes = passwordResetDao.purgeExpiredCodes(beforeExpiresAt);
        int purgedRateLimits = rateLimitDao.purgeOldRows(now.toLocalDate());
        if (purgedCodes > 0 || purgedRateLimits > 0) {
            log.info("비밀번호 재설정 데이터 정리(배치) — 만료 후 보존기간 지난 코드 {}건, 지난 날짜 일일상한 카운터 {}건 삭제",
                    purgedCodes, purgedRateLimits);
        }
    }
}
