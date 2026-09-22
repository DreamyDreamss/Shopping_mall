// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberSignupRateLimitDao;
import com.sm.lab.shop.dao.MemberSignupVerificationDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 회귀(SR-231 round2 QA CONCERNS 권고1, round3/round4 재작업) — 동일 target에 5분 1초 이상
 * 간격을 두고 6회 요청하면 6번째가 429({@code MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED})여야 한다.
 *
 * <p>round4(사람 결정, "설계를 단순화해서 데드락 원인을 없앤다") — 레이트리밋 카운터가 전용
 * 테이블({@link MemberSignupRateLimitDao}, MEMBER_SIGNUP_RATE_LIMITS)로 옮겨졌다 — 이 테스트는
 * 그 실 DB DAO를 그대로 쓰되, {@link Clock}을 주입한 별도 {@link MemberSignupService} 인스턴스로
 * "5분 1초씩 흐르는 시각"을 결정적으로 시뮬레이션한다(실제로 25분을 기다리지 않는다). 단일
 * 스레드 순차 호출이라 동시성 자체의 원자성 검증은 별도({@code MemberSignupRateLimitConcurrencyTest},
 * 실 서버·실 동시요청)가 맡는다 — STORY 재작업 지시 "기존 5분1초 간격 테스트는 유지"에 따라
 * 이 파일은 그대로 남기고 새 저장소 구조에만 맞춰 갱신한다.
 */
@SpringBootTest
class MemberSignupRateLimitTest {

    private static final String CHANNEL = "EMAIL";
    private static final String TARGET = "rate-limit-test@example.com";
    private static final long INTERVAL_SECONDS = 301L; // 5분 1초 — 코드 TTL(300초)·쿨다운(60초) 모두 지남

    @Autowired
    private MemberSignupVerificationDao verificationDao;
    @Autowired
    private MemberSignupRateLimitDao rateLimitDao;

    @AfterEach
    void cleanUp() {
        verificationDao.deleteByChannelAndTarget(CHANNEL, TARGET);
        rateLimitDao.deleteRateLimit(TARGET, LocalDate.now());
    }

    // linked_tc: TC-FUNC-member-002-11
    @Test
    void requestVerificationCode_sixRequestsFiveMinuteOneSecondApart_sixthHitsDailyLimit() {
        ZonedDateTime start = ZonedDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);

        for (int i = 1; i <= MemberSignupService.DAILY_REQUEST_LIMIT; i++) {
            LocalDateTime callTime = start.plusSeconds((i - 1) * INTERVAL_SECONDS).toLocalDateTime();
            MemberSignupService.VerificationCodeResult result =
                    serviceAt(callTime).requestVerificationCode(TARGET);
            assertThat(result.channel()).as("call #%d는 통과해야 함", i).isEqualTo(CHANNEL);
        }

        LocalDateTime sixthCallTime =
                start.plusSeconds(MemberSignupService.DAILY_REQUEST_LIMIT * INTERVAL_SECONDS).toLocalDateTime();

        assertThatThrownBy(() -> serviceAt(sixthCallTime).requestVerificationCode(TARGET))
                .isInstanceOf(MemberSignupApiException.class)
                .extracting(ex -> ((MemberSignupApiException) ex).getCode())
                .isEqualTo("MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED");

        assertThat(rateLimitDao.selectRateLimit(TARGET, start.toLocalDate()).getDailyCount())
                .as("상한 초과 요청은 거부되므로 카운터는 5(허용된 요청 수)에서 더 늘지 않아야 함")
                .isEqualTo(MemberSignupService.DAILY_REQUEST_LIMIT);
    }

    private MemberSignupService serviceAt(LocalDateTime instant) {
        Clock fixed = Clock.fixed(instant.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        return new MemberSignupService(verificationDao, rateLimitDao, fixed);
    }
}
