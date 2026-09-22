// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * round6(SR-231 round5 QA CONCERNS 재작업 지시 2, 사람 결정) — {@link MemberSignupSchedulingConfig}의
 * {@code @ConditionalOnProperty}가 실제로 스케줄러 빈 등록 여부를 가르는지 순수 Spring 컨텍스트로
 * 직접 검증한다. {@code @SpringBootTest}로는 이 모듈 surefire 설정이 이미
 * {@code spring.task.scheduling.enabled=false}를 시스템 프로퍼티로 고정해 두어(모든
 * {@code @SpringBootTest}가 스케줄러를 안 띄우는 "정상 상태"만 반복 확인하게 되고,
 * {@code true}/미설정 분기(=운영 기동 시 실제로 스케줄러가 켜지는 경로)는 별도로 확인할 방법이
 * 없다 — {@code @Scheduled(initialDelay=600_000)}라 실행을 몇 분씩 기다릴 수도 없다. 이 테스트는
 * {@code AnnotationConfigApplicationContext}에 프로퍼티만 주입해 세 분기(false/true/미설정)를
 * 전부 결정적·즉시 검증한다 — {@code @ConditionalOnProperty}는 Boot 오토컨피그 전용이 아니라
 * 표준 {@code @Conditional} 메커니즘이라 Boot 없는 순수 컨텍스트에서도 그대로 평가된다.
 */
class MemberSignupSchedulingConfigTest {

    @Test
    void schedulerBeanPostProcessorAbsent_whenPropertyFalse() {
        try (AnnotationConfigApplicationContext ctx = contextWithProperty("false")) {
            assertThat(ctx.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class))
                    .as("spring.task.scheduling.enabled=false면 @EnableScheduling 자체가 적용되지 않아야 함"
                            + "(=MemberSignupMaintenanceScheduler의 @Scheduled가 예약되지 않음)")
                    .isEmpty();
        }
    }

    @Test
    void schedulerBeanPostProcessorPresent_whenPropertyTrue() {
        try (AnnotationConfigApplicationContext ctx = contextWithProperty("true")) {
            assertThat(ctx.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class))
                    .as("spring.task.scheduling.enabled=true면 스케줄링이 정상 활성화돼야 함")
                    .hasSize(1);
        }
    }

    @Test
    void schedulerBeanPostProcessorPresent_byDefault_whenPropertyAbsent() {
        // 운영 기동 경로 — application.yml에 이 프로퍼티가 없다. 다만 이 모듈의 surefire
        // 설정이 "테스트 컨텍스트는 스케줄링을 끈다"를 위해 이 JVM 전체에 시스템 프로퍼티로
        // spring.task.scheduling.enabled=false를 주입해 뒀으므로(위 두 테스트 포함),
        // AnnotationConfigApplicationContext의 기본 StandardEnvironment는 systemProperties를
        // 자동 포함해 그 값을 그대로 물려받는다 — "프로퍼티가 아예 없는" 운영 기동 상태를
        // 이 JVM 안에서 재현하려면 그 시스템 프로퍼티 소스를 이 컨텍스트에서만 제거해야 한다.
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources()
                .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        ctx.register(MemberSignupSchedulingConfig.class);
        ctx.refresh();
        try {
            assertThat(ctx.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class))
                    .as("프로퍼티 미설정(운영 기동 기본값)이면 matchIfMissing=true로 스케줄링이 켜져야 함")
                    .hasSize(1);
        } finally {
            ctx.close();
        }
    }

    private AnnotationConfigApplicationContext contextWithProperty(String value) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test-scheduling-flag",
                        Map.of("spring.task.scheduling.enabled", value)));
        ctx.register(MemberSignupSchedulingConfig.class);
        ctx.refresh();
        return ctx;
    }
}
