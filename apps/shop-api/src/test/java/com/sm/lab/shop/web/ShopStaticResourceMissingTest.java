package com.sm.lab.shop.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-301(INF-ORD-017) — 빌드 산출물이 없는 상태(클래스패스에 해당 위치 자체가 없음)에서도 기동은
 * 성공하고, {@code /shop} 접근은 404가 되는지 검증한다.
 *
 * <p>{@code shop.static-location}을 실제로 아무 파일도 두지 않은 클래스패스 경로(absent)로
 * 오버라이드한다 — 이 클래스가 정상적으로 컨텍스트를 띄운다는 사실 자체가 "기동은 성공" 요건의
 * 첫 단언이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "shop.static-location=classpath:/shop-fixture/absent/")
class ShopStaticResourceMissingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shopRoot_returns404_whenBuildOutputMissing() throws Exception {
        mockMvc.perform(get("/shop"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shopIndexHtml_returns404_whenBuildOutputMissing() throws Exception {
        mockMvc.perform(get("/shop/index.html"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shopAsset_returns404_whenBuildOutputMissing() throws Exception {
        mockMvc.perform(get("/shop/assets/x.js"))
                .andExpect(status().isNotFound());
    }
}
