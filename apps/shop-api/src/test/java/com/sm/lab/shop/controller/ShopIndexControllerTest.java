package com.sm.lab.shop.controller;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-301(INF-ORD-017) round 2 재작업(QA CONCERNS 권고 2) — {@link ShopIndexController}가
 * {@code controller} 패키지로 옮겨지며 {@code controller-has-test}(must) 규칙 대상이 됐다.
 *
 * <p>여기서는 이 컨트롤러가 담당하는 정확히 두 경로({@code /shop}·{@code /shop/})와, 빌드 산출물이
 * 없을 때의 404만 다룬다. {@code /shop/products/1} 같은 딥링크는 실제로는
 * {@link com.sm.lab.shop.web.ShopStaticResourceConfig}의 SPA 폴백 리졸버가 처리하지만(이 컨트롤러의
 * 매핑은 {@code /shop}·{@code /shop/} 정확 일치뿐), "/shop 하위 어떤 경로로 들어와도 화면이 뜬다"는
 * 사용자 관점 계약을 이 클래스명으로 함께 단언한다(완료 조건 — 사람 코멘트). 캐시 헤더·경로순회·
 * 화이트리스트 오매칭 등 상세 시나리오는 기존 {@code ShopStaticResourceServingTest}/
 * {@code ShopStaticResourceMissingTest}가 이미 담당하므로 여기서 중복 확장하지 않는다.
 */
class ShopIndexControllerTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestPropertySource(properties = "shop.static-location=classpath:/shop-fixture/present/")
    class WhenBuildOutputPresent {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void shopRoot_returns200_withHtmlContentType() throws Exception {
            mockMvc.perform(get("/shop"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(content().string(containsString("SHOP-FIXTURE-INDEX-MARKER")));
        }

        @Test
        void root_redirectsToShop_withoutApiKey() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string("Location", "/shop/"));
        }

        @Test
        void deepLink_returns200_withIndexHtmlBody() throws Exception {
            mockMvc.perform(get("/shop/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("SHOP-FIXTURE-INDEX-MARKER")));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestPropertySource(properties = "shop.static-location=classpath:/shop-fixture/absent/")
    class WhenBuildOutputMissing {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void shopRoot_returns404_whenNoStaticResource() throws Exception {
            mockMvc.perform(get("/shop"))
                    .andExpect(status().isNotFound());
        }
    }
}
