package com.sm.lab.shop.support;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * SR-300 재작업(round2, QA CONCERNS 권고2): 고정 시계 리터럴 {@code 2026-08-20 09:00}이
 * {@code OrderServiceTest}·{@code OrderListEndToEndIntegrationTest}·{@code ApiKeyAuthIntegrationTest}
 * 3곳에 각각 독립 기술돼 있으면 한 곳만 바뀔 때 값이 어긋난다 — 이 상수 하나로 통합한다.
 *
 * <p>{@link #SEED_TODAY}를 "오늘"로 고정하면 기본 조회창(오늘-30일~오늘 = 2026-07-21~2026-08-20)이
 * 시드 주문일(2026-08-15~17)을 날짜와 무관하게 포함한다. {@code ZoneId.systemDefault()}를 쓰는 이유:
 * AS-IS의 {@code LocalDate.now()}도 시스템 기본 zone 기준이었으므로, zone을 바꾸면 그 자체가 새로운
 * 변수가 된다(다른 랩 시간 의존 테스트와 동일 관례).
 */
public final class TestClocks {

    /** 시드 주문일(2026-08-15~17)을 포함하는 고정 "오늘". */
    public static final Clock SEED_TODAY = Clock.fixed(
            LocalDateTime.of(2026, 8, 20, 9, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private TestClocks() {
    }
}
