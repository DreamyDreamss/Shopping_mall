package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberPasswordResetDao;
import com.sm.lab.shop.dao.MemberPasswordResetRateLimitDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * SR-297 #1(BAT-MBR-001) — {@link MemberPasswordResetMaintenanceScheduler} 단위 테스트
 * (Mockito, DAO 목). 실 SQL의 조건부 DELETE 시맨틱은
 * {@code MemberPasswordResetDaoTest}·{@code MemberPasswordResetRateLimitDaoTest}(실 DB)에서
 * 별도 검증한다 — 이 테스트는 배치 내부 호출 순서·횟수(STORY "순서·보안" 절)만 증명한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberPasswordResetMaintenanceSchedulerTest {

    @Mock
    private MemberPasswordResetDao passwordResetDao;
    @Mock
    private MemberPasswordResetRateLimitDao rateLimitDao;

    // round1 재작업 — 코드 행은 "지금"이 아니라 "지금 - 보존기간" 이전 만료 행만 대상이다
    // (STORY 재작업 지시 1, MemberPasswordResetMaintenanceScheduler 클래스 상단 설명 참고).
    // 카운터 행(purgeOldRows)은 보존기간 대상이 아니므로 여전히 오늘 날짜 그대로 호출된다.

    // linked_tc: TC-FUNC-member-bat001-008
    @Test
    void purgeExpiredPasswordResetData_callsCodeCleanupWithRetentionAdjustedThresholdBeforeRateLimitCleanup() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-09-16T03:00:00Z"), ZoneId.of("UTC"));
        Duration retention = Duration.ofDays(7);
        LocalDateTime expectedNow = LocalDateTime.now(fixedClock);
        LocalDateTime expectedThreshold = expectedNow.minus(retention);
        MemberPasswordResetMaintenanceScheduler scheduler =
                new MemberPasswordResetMaintenanceScheduler(passwordResetDao, rateLimitDao, fixedClock, retention);

        scheduler.purgeExpiredPasswordResetData();

        verify(passwordResetDao, times(1)).purgeExpiredCodes(eq(expectedThreshold));
        verify(rateLimitDao, times(1)).purgeOldRows(eq(expectedNow.toLocalDate()));
        InOrder order = inOrder(passwordResetDao, rateLimitDao);
        order.verify(passwordResetDao).purgeExpiredCodes(any());
        order.verify(rateLimitDao).purgeOldRows(any());
    }
}
