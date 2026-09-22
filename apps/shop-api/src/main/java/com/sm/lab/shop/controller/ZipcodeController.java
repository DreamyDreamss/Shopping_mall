// linked_func: FUNC-member-012
// spec: docs/00_FUNC/stories/STORY-FUNC-member-012.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Zipcode;
import com.sm.lab.shop.service.ZipcodeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 우편번호(도로명) 검색 API(SR-235, INF-MBR-009) — {@code GET /api/zipcodes?q=}.
 *
 * <p>인증은 {@link com.sm.lab.shop.web.ApiKeyAuthFilter}가 컨트롤러 도달 전에 이미 끝낸다.
 * 이 경로는 회원 스코프 강제 대상 정규식 어디와도 매치하지 않고 {@code memberId} 파라미터도
 * JSON 본문도 없어 default-ALLOW 분기로 통과한다 — 유효한 {@code X-Api-Key}(admin/member 스코프
 * 아무거나)만 있으면 통과한다(코드 추적 완료, STORY "순서·보안" 절). 새 우회 경로를 추가하지
 * 않는다.
 *
 * <p>이 엔드포인트에는 존재/소유 판정이 없다(회원 소유 자원이 아닌 전역 참조데이터 검색) —
 * 결과 0건도 200 {@code {items: []}}로 응답한다.
 */
@RestController
@RequestMapping("/api/zipcodes")
public class ZipcodeController {

    private final ZipcodeService zipcodeService;

    public ZipcodeController(ZipcodeService zipcodeService) {
        this.zipcodeService = zipcodeService;
    }

    /** 검색 — 200 + {@code {items:[...]}}(0건 포함, 404 아님). 검색어 미달/초과 400 {@code MBR-4202}. */
    @GetMapping
    public Map<String, Object> search(@RequestParam(required = false) String q) {
        List<Zipcode> items = zipcodeService.search(q);
        return Map.of("items", items);
    }
}
