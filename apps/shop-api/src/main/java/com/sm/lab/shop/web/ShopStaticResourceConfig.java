package com.sm.lab.shop.web;

import com.sm.lab.shop.controller.ShopIndexController;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * SR-301(INF-ORD-017) — shop-web(SPA) 빌드 산출물을 {@code /shop/**}로 정적 서빙한다.
 *
 * <p>핸들러 두 개를 등록한다: (1) {@code /shop/assets/**}(Vite 기본 산출물 구조상 해시 붙은
 * JS/CSS가 모이는 위치) — 장기 캐시(1년, immutable). (2) {@code /shop/**}(그 외 전부) — no-cache +
 * SPA 폴백. 두 패턴은 등록 순서가 아니라 경로 특이도(더 구체적인 패턴이 우선)로 Spring MVC가
 * 자동 정렬하므로 별도 순서 지정이 없어도 자산 요청은 항상 핸들러(1)에 먼저 매칭된다.
 *
 * <p>SPA 폴백은 {@link PathResourceResolver}를 확장해, 요청 경로에 실제로 매칭되는 파일이 없을 때만
 * (딥링크·클라이언트 라우트) {@code index.html}로 대체한다 — {@code super.getResource()}(Spring
 * 내장 위치-이탈 방지 로직)를 먼저 거치므로 새 경로 순회 표면을 만들지 않는다. {@code index.html}
 * 자체도 없으면(빌드 산출물 없음) null을 반환해 Spring 기본 404로 떨어뜨린다 — 파일 부재를 200으로
 * 위장하지 않는다.
 */
@Configuration
public class ShopStaticResourceConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ShopStaticResourceConfig.class);

    private final String shopLocation;
    private final ResourceLoader resourceLoader;

    public ShopStaticResourceConfig(
            @Value("${shop.static-location:classpath:/static/shop/}") String shopLocation,
            ResourceLoader resourceLoader) {
        // round 2(QA CONCERNS 권고 5) — 사람이 후행 슬래시 없이 설정해도(classpath:/static/shop)
        // 아래 "assets/"·"index.html" 접미 결합(.../shopassets/ 같은 깨짐)이 나지 않도록 보정한다.
        // ShopIndexController도 동일한 프로퍼티를 받아 동일하게 보정한다(판정 기준 불일치 방지).
        this.shopLocation = shopLocation.endsWith("/") ? shopLocation : shopLocation + "/";
        this.resourceLoader = resourceLoader;
    }

    /**
     * 기동 시 1회만 확인·경고한다(요청마다 반복 로깅하지 않음 — 스팸 방지). "서버 로그에 안내"
     * 요건은 이 1회 WARN으로 충분히 만족한다(AC — 빌드 산출물이 없어도 기동은 성공).
     */
    @PostConstruct
    void warnIfBuildOutputMissing() {
        Resource index = resourceLoader.getResource(shopLocation + "index.html");
        if (!index.exists()) {
            // round 2(QA CONCERNS 권고 4) — 원인만이 아니라 조치까지 안내한다.
            log.warn("shop-web 빌드 산출물 없음: {}index.html — /shop 접근은 404. "
                    + "해결: modules/shop-web에서 npm run build 실행 후 shop-api 재빌드·재기동.",
                    shopLocation);
        }
    }

    /**
     * 실측(구현 후 MockMvc로 확인, STORY "프레임워크 실행 모델 함정"이 예견하지 못한 항목) —
     * {@code ResourceHttpRequestHandler}는 매핑 패턴을 벗겨낸 나머지 리소스 경로가 빈 문자열이면
     * (정확히 {@code /shop} 또는 {@code /shop/}만 요청됐을 때) 커스텀 {@link PathResourceResolver}
     * 체인을 타기도 전에 자체 가드에서 404로 떨어뜨린다. 이 두 정확 경로는 {@link ShopIndexController}가
     * 별도로 처리한다({@code /shop/products/123} 같은 하위 미매칭 경로는 리소스 경로가 비어있지
     * 않으므로 이 핸들러의 폴백 리졸버가 정상 처리 — 영향 없음).
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/shop/assets/**")
                .addResourceLocations(shopLocation + "assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        registry.addResourceHandler("/shop/**")
                .addResourceLocations(shopLocation)
                .setCacheControl(CacheControl.noCache())
                .resourceChain(false)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = super.getResource(resourcePath, location);
                        if (requested != null) {
                            return requested;
                        }
                        Resource index = location.createRelative("index.html");
                        return (index.exists() && index.isReadable()) ? index : null;
                    }
                });
    }
}
