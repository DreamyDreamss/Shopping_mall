package com.sm.lab.shop.controller;

import com.sm.lab.shop.web.ShopStaticResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SR-301(INF-ORD-017) — {@code GET /shop}·{@code GET /shop/}(경로 그대로, 하위 세그먼트 없음)
 * 전용 진입점.
 *
 * <p>실측(구현 후 MockMvc로 확인) — {@link ShopStaticResourceConfig}가 등록하는
 * {@code ResourceHttpRequestHandler}는 매핑 패턴({@code /shop/**})을 벗겨낸 나머지 리소스 경로가
 * 빈 문자열이면(=정확히 이 두 경로) 커스텀 {@code PathResourceResolver}의 SPA 폴백 로직을 타기도
 * 전에 자체 가드에서 404로 떨어뜨린다. "forward:" 뷰 컨트롤러로 우회를 먼저 시도했으나 MockMvc의
 * mock 서블릿 환경은 실제 재-디스패치를 수행하지 않아(forward 대상 URL만 기록하고 본문은 비어
 * 있음) 사람이 요구한 MockMvc 레벨 검증을 통과할 수 없었다 — 그래서 index.html을 이 컨트롤러가
 * 직접 읽어 반환한다(실 서버·MockMvc 양쪽에서 동일하게 동작, 존재 판정도 리소스 로더 그대로 재사용
 * 해 {@link ShopStaticResourceConfig}의 나머지 경로와 판정 기준이 갈리지 않는다).
 *
 * <p><b>round 2 재작업(QA CONCERNS 권고 2)</b>: 최초 구현은 이 클래스를 {@code web} 패키지(필터·
 * 예외핸들러 전용 위치)에 뒀는데, 기존 컨트롤러 17개가 전부 {@code controller} 패키지에 있는
 * 관례를 깼고 {@code controller-has-test}(must) 규칙의 파일 경로 대상(`**​/controller/*.java`)을
 * 우회하는 형태였다. 이번에 {@code controller} 패키지로 옮기고 {@link ShopStaticResourceConfig}는
 * 여전히 설정 클래스라 {@code web} 패키지에 남긴다(컨트롤러가 아닌 것까지 옮기지 않음).
 */
@Controller
public class ShopIndexController {

    private final ResourceLoader resourceLoader;
    private final String shopLocation;

    public ShopIndexController(ResourceLoader resourceLoader,
                                @Value("${shop.static-location:classpath:/static/shop/}") String shopLocation) {
        this.resourceLoader = resourceLoader;
        // round 2(QA CONCERNS 권고 5) — 사람이 후행 슬래시 없이 설정해도(classpath:/static/shop)
        // 아래 "index.html" 접미 결합이 깨지지 않도록 보정한다.
        this.shopLocation = shopLocation.endsWith("/") ? shopLocation : shopLocation + "/";
    }

    /** 루트 접속 = 쇼핑몰 첫 화면. 종전엔 매핑이 없어 인증 필터가 401을 돌려줬다. */
    @GetMapping("/")
    String root() {
        return "redirect:/shop/";
    }

    @GetMapping({"/shop", "/shop/"})
    ResponseEntity<Resource> index() {
        Resource index = resourceLoader.getResource(shopLocation + "index.html");
        if (!index.exists() || !index.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.TEXT_HTML)
                .body(index);
    }
}
