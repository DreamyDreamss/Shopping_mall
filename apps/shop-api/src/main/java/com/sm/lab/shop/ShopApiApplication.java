package com.sm.lab.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

// linked_func: FUNC-member-002 — round4(SR-231 재작업 지시(B))가 여기 뒀던 @EnableScheduling은
// round6(round5 QA CONCERNS 재작업 지시 2, 사람 결정)에서 com.sm.lab.shop.service.
// MemberSignupSchedulingConfig(레이트리밋/인증코드 정리 스케줄러 전용 @Configuration)로
// 옮겼다 — 이 FUNC과 무관한 모든 @SpringBootTest 컨텍스트가 예외 없이 스케줄러를 띄우던
// 문제(round4 QA 권고7)를 해소한다. 애플리케이션 전역에는 더 이상 스케줄링을 강제하지 않는다.
@SpringBootApplication
public class ShopApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopApiApplication.class, args);
    }

    // SR-300: OrderService의 '오늘' 계산을 주입 가능한 Clock으로 바꾸기 위한 빈. 운영 기본값은
    // 시스템 시계(AS-IS와 동일 결과) — 새 @Configuration을 만들지 않고 기존 @SpringBootApplication에
    // 최소 추가한다. 다른 서비스는 이 빈을 @Autowired하지 않는다(전부 내부에서 Clock.systemDefaultZone()
    // 직접 생성) — "다른 서비스의 시계 사용 불변" 회귀 요건과 충돌 없음.
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
