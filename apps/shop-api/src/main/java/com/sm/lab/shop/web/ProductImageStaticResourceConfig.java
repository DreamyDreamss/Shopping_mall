package com.sm.lab.shop.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SR-306(#2) — 상품 대표 이미지(정적 SVG)를 {@code /images/**}로 서빙한다.
 *
 * <p>{@link ShopStaticResourceConfig}와 같은 계열이지만, SPA 딥링크 폴백이 필요 없는 단순 자산
 * 서빙이라 커스텀 {@code PathResourceResolver} 없이 Spring 기본 {@code ResourceHttpRequestHandler}
 * 만 쓴다 — 위치 밖 경로(예: {@code ../application.yml}) 요청을 거부하는 경로 이탈 방지 로직은
 * 그 기본 구현에 이미 있다(별도 구현 불필요).
 *
 * <p>Spring Boot의 기본 정적 리소스 매핑({@code classpath:/static/} → {@code /**})과 이 핸들러가
 * 같은 클래스패스 위치를 놓고 중복 매핑일 수 있다 — 실제로 어느 쪽이 응답하는지는
 * {@code ProductImageStaticResourceServingTest}(MockMvc)로 실측 확인한다.
 */
@Configuration
public class ProductImageStaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}
