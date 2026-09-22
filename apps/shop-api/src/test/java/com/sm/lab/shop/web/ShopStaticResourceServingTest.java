package com.sm.lab.shop.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-301(INF-ORD-017) — {@code /shop/**} 정적 서빙(빌드 산출물 있음)을 실 서버 컨텍스트로 검증한다.
 *
 * <p>{@code shop.static-location}을 픽스처 전용 클래스패스 경로(present)로 오버라이드해, 로컬에
 * 개발자가 {@code npm run build}로 실제 {@code shop-web/dist}를 만들어 뒀는지 여부와 무관하게
 * 항상 같은 결과가 나오게 격리한다(고정 픽스처만 본다).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "shop.static-location=classpath:/shop-fixture/present/")
class ShopStaticResourceServingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void indexHtml_isServed_withNoCacheHeader() throws Exception {
        mockMvc.perform(get("/shop/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SHOP-FIXTURE-INDEX-MARKER")))
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    void hashedAsset_isServed_withLongTermImmutableCache() throws Exception {
        MvcResult result = mockMvc.perform(get("/shop/assets/app.test123.js"))
                .andExpect(status().isOk())
                .andReturn();

        String cacheControl = result.getResponse().getHeader("Cache-Control");
        assertThat(cacheControl).contains("max-age=");
        assertThat(cacheControl).contains("immutable");
    }

    @Test
    void unmatchedDeepClientRoute_fallsBackToIndexHtml_withNoCache() throws Exception {
        mockMvc.perform(get("/shop/some/deep/client/route"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SHOP-FIXTURE-INDEX-MARKER")))
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    void shopRoot_withoutTrailingSlash_fallsBackToIndexHtml() throws Exception {
        mockMvc.perform(get("/shop"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SHOP-FIXTURE-INDEX-MARKER")));
    }

    // SR-307(SR-307.1) round 2 재작업(QA round1 FAIL 필수1) — 이 테스트는 원래 GET /api/products로
    // "/shop 화이트리스트가 /api/**로 새지 않았다"를 감시했으나, SR-307이 바로 그 경로의 무키 GET을
    // 의도적으로 공개(200)로 바꿔 정면 충돌했다. GET /api/products는 삭제하지 말고 SR-307로 의도적
    // 공개된 사실만 기록한 뒤, 감시 대상을 여전히 보호되는 다른 API(GET /api/orders — member 스코프
    // 소유권 판정이 걸려 있어 공개될 일이 없다)로 옮겨 화이트리스트 누출 감시 의도를 그대로 보존한다.
    @Test
    void apiWithoutApiKey_stillRejectedUnauthorized_shopWhitelistDidNotLeak() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void similarButDifferentPath_shopkeeper_isNotWhitelisted() throws Exception {
        mockMvc.perform(get("/shopkeeper"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pathTraversalAttempt_doesNotExposeArbitraryFile() throws Exception {
        MvcResult result = mockMvc.perform(get(URI.create("/shop/%2e%2e/application.yml")))
                .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        // 임의 파일(application.yml 실제 내용) 노출은 금지 — 404 또는 index.html 폴백(200)만 허용.
        assertThat(body).doesNotContain("spring:");
        if (status == 200) {
            assertThat(body).contains("SHOP-FIXTURE-INDEX-MARKER");
        } else {
            assertThat(status).isEqualTo(404);
        }
    }
}
