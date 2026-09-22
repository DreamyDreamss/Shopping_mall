// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * round6(SR-231 round5 QA CONCERNS 재작업 지시 2, 사람 결정) — {@code @EnableScheduling}을
 * 애플리케이션 전역({@code ShopApiApplication})이 아니라 이 회원가입 레이트리밋/인증코드
 * 정리 스케줄러({@link MemberSignupMaintenanceScheduler}) 전용 설정 클래스로 옮겼다.
 * round4~5는 {@code ShopApiApplication}에 직접 {@code @EnableScheduling}을 둬 이 FUNC과
 * 무관한 모든 {@code @SpringBootTest} 컨텍스트(주문/장바구니/결제 등)가 예외 없이 유지보수
 * 스케줄러의 {@code ScheduledAnnotationBeanPostProcessor}를 함께 띄웠다 — {@code initialDelay
 * =10분}이 정상적인 테스트 실행 시간(수십 초)을 우연히 보호했을 뿐, 그 불변식은 주석으로만
 * 보장됐다(round4 QA 권고7).
 *
 * <p>{@code spring.task.scheduling.enabled}는 Spring Boot의 {@code TaskSchedulingProperties}가
 * 인식하는 공식 필드가 아니다 — 이 클래스의 {@link ConditionalOnProperty}가 정확히 이 키를
 * 읽어 이 {@code @Configuration} 자체(그리고 그 안의 {@code @EnableScheduling} 임포트)를
 * 통째로 걸러내는 방식으로 그 이름에 실제 효과를 부여한다. 테스트 실행 시에는 이 값을
 * {@code false}로 시스템 프로퍼티로 주입해(shop-api 모듈 {@code pom.xml}의 surefire
 * 설정) 이 설정 클래스가 통째로 스킵되므로 {@code MemberSignupMaintenanceScheduler}의
 * {@code @Scheduled} 메서드가 어떤 컨텍스트에서도 스케줄되지 않는다({@code @Component}
 * 자체는 계속 빈으로 등록된다 — DI만 되고 예약 실행만 빠진다). 운영 기동 시에는 이 프로퍼티가
 * 없으므로({@code matchIfMissing=true}) 그대로 켜진다.
 *
 * <p>SR-297 #1(round1 QA CONCERNS 권고5) — {@link MemberPasswordResetMaintenanceScheduler}
 * (비밀번호 재설정 만료 행 정리 배치)도 이 전역 스위치에 의존한다. 이 클래스를 가입 도메인
 * 전용으로 여기고 지우거나 이름을 바꾸면 그 배치도 함께 조용히 멈춘다 — 테스트는
 * {@code spring.task.scheduling.enabled=false}로 스케줄링 자체를 끄므로 이 회귀를 잡지 못한다.
 */
@Configuration
@ConditionalOnProperty(name = "spring.task.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class MemberSignupSchedulingConfig {
}
