// linked_func: FUNC-order-013 — SR-204 R-5
// spec: docs/00_FUNC/stories/STORY-FUNC-order-013.md
package com.sm.lab.shop.support;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * SR-204 R-1 도입(전역 {@code /api/**} X-Api-Key 인증) 이후, 인증과 무관한 기존 웹 계층
 * 테스트(@WebMvcTest/@SpringBootTest+@AutoConfigureMockMvc의 MockMvc)가 전부 401로 깨지지
 * 않도록 admin 키를 모든 요청에 기본 주입한다.
 *
 * <p>{@code MockMvcBuilderCustomizer} 빈은 Spring Boot의 MockMvc 자동구성이 자동 수집해
 * {@code builder.defaultRequest(...)}로 적용한다({@code ConfigurableMockMvcBuilder}가 병합
 * 시 요청에 이미 존재하는 헤더는 덮어쓰지 않으므로, 개별 테스트가 {@code .header("X-Api-Key", ...)}
 * 를 명시하면 그 값이 우선한다).
 *
 * <p>인증 자체를 검증하는 테스트(무키/오키/member키/IDOR/413)는 이 설정을 쓰지 않고 별도
 * {@code TestRestTemplate} 기반 통합 테스트({@code ApiKeyAuthIntegrationTest})로 수행한다 —
 * 이 커스터마이저 아래서는 "무키" 상태를 만들 수 없기 때문이다.
 */
@TestConfiguration
public class AdminApiKeyTestConfig {

    public static final String HEADER = "X-Api-Key";
    public static final String ADMIN_KEY = "lab-admin-key";

    @Bean
    public MockMvcBuilderCustomizer adminApiKeyMockMvcCustomizer() {
        return this::customize;
    }

    private void customize(ConfigurableMockMvcBuilder<?> builder) {
        builder.defaultRequest(get("/").header(HEADER, ADMIN_KEY));
    }
}
