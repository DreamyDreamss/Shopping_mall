package com.sm.lab.shop.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-306(#2) — {@code /images/**}(상품 대표 이미지 정적 SVG) 서빙을 실 서버 컨텍스트로 검증한다.
 *
 * <p>{@link ShopStaticResourceServingTest}와 동형(형제 관례) — 다만 {@link ProductImageStaticResourceConfig}는
 * 위치를 고정({@code classpath:/static/images/})으로 두므로 이 테스트는 실제 시드 자산
 * (SKU-1001~1003, SR-306.2가 생성)을 그대로 검증 대상으로 삼는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductImageStaticResourceServingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void existingImage_isServedWithoutApiKey() throws Exception {
        // MockHttpServletResponse#getContentAsString()은 응답 Content-Type에 charset 파라미터가
        // 없으면(image/svg+xml은 기본적으로 없음) 서블릿 기본값(ISO-8859-1)으로 디코딩한다 — 파일은
        // UTF-8로 저장돼 있으므로 이 테스트는 명시적으로 UTF-8로 읽는다(실제 서빙 바이트는 그대로).
        MvcResult result = mockMvc.perform(get("/images/products/sku-1001.svg"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).isEqualTo("image/svg+xml");
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("스탠딩 데스크").contains("SKU-1001");
    }

    @Test
    void missingImage_returns404() throws Exception {
        // SKU-1004는 의도적으로 이미지 파일을 만들지 않았다("이미지 없음" 실데이터 상태 보존).
        mockMvc.perform(get("/images/products/sku-1004.svg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pathTraversalAttempt_doesNotExposeArbitraryFile() throws Exception {
        MvcResult result = mockMvc.perform(get(URI.create("/images/%2e%2e/application.yml")))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // 임의 파일(application.yml 실제 내용) 비노출 — 기본 ResourceHttpRequestHandler의 경로
        // 이탈 방지가 차단하며(200 폴백이 없는 단순 자산 핸들러라 200은 절대 안전한 상태가 아님),
        // 컨테이너 레벨에서 먼저 막히면 404가 아닌 다른 상태코드일 수 있어 상태코드는 "200 아님"만
        // 확인한다(ShopStaticResourceServingTest와 동일한 보수적 판정 방식).
        assertThat(body).doesNotContain("spring:");
        assertThat(result.getResponse().getStatus()).isNotEqualTo(200);
    }

    @Test
    void apiWithoutApiKey_stillRejectedUnauthorized_imagesWhitelistDidNotLeak() throws Exception {
        // /images/** 화이트리스트가 /api/**로 새지 않았는지 감시(ShopStaticResourceServingTest와
        // 동일 의도 — SR-307이 GET /api/products를 의도적으로 공개했으므로 그 경로는 재사용하지
        // 않고 여전히 보호되는 /api/orders로 확인한다).
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void imagesPathOutsideProductsPrefix_isNotWhitelisted() throws Exception {
        // 재작업(SR-306.2 round 2, QA CONCERNS 권고2) — 화이트리스트를 "/images/"에서
        // "/images/products/"로 좁힌 뒤에는, 그 형제 경로(예: 향후 자격 판정이 필요한 업로드 API가
        // "/images/uploads/..."에 생기는 경우)가 더 이상 필터를 자동으로 스킵하지 않아야 한다.
        // 정적 리소스 매핑(ProductImageStaticResourceConfig)은 여전히 "/images/**" 전체를
        // classpath:/static/images/로 서빙 등록돼 있다("/images/products/**"로 고정돼 있지 않음) —
        // 그래서 화이트리스트가 좁혀져 있지 않으면 필터가 스킵되고 리소스 핸들러가 파일 없음으로
        // 404를 낼 뿐이라 "401 또는 404"로는 좁혀져 있음을 증명하지 못한다(round 2 QA 지적,
        // 재작업 round 3). 무키 요청은 반드시 인증 필터의 default-deny(401)로 막혀야 한다 — 401을
        // 고정해, 화이트리스트가 다시 "/images/"로 넓어지면 이 테스트가 404로 깨지도록 한다.
        mockMvc.perform(get("/images/secret.txt"))
                .andExpect(status().isUnauthorized());
    }
}
