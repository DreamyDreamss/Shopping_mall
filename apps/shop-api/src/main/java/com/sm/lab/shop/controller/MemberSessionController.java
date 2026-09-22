// linked_func: FUNC-member-006
// spec: docs/00_FUNC/stories/STORY-FUNC-member-006.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.MemberSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그아웃·자동 로그인(리프레시) API(SR-232, INF-MBR-004) — {@code POST /api/members/sessions/logout}
 * (인증 필요) · {@code POST /api/members/sessions/refresh}(무인증).
 *
 * <p>경로를 두 세그먼트({@code /sessions/logout})로 설계해 {@code ApiKeyAuthFilter.MEMBERS_ITEM_PATH}
 * (한 세그먼트 {@code /api/members/{id}} 전용 정규식)와 애초에 매치되지 않게 했다 — 계획
 * "폴백·우회 경로의 자격 판정" 절 근거(한 세그먼트로 만들면 "logout"을 memberId로 오인해 매번
 * 403이 났을 것).
 *
 * <p>logout은 {@link com.sm.lab.shop.web.ApiKeyAuthFilter} 화이트리스트에 없다(인증 필요) —
 * 필터가 먼저 {@code X-Api-Key} 존재·유효성을 검증하고, 이 컨트롤러는 그 헤더 값을 다시 읽어
 * 어느 회원의 세션인지 서비스가 해석하게 한다(필터는 요청에 memberId를 별도로 실어주지 않는다).
 * refresh는 화이트리스트(정확 일치)로 필터를 완전히 스킵한다 — 자격 판정은
 * {@link MemberSessionService#refresh}가 전담한다.
 */
@RestController
@RequestMapping("/api/members/sessions")
public class MemberSessionController {

    private final MemberSessionService memberSessionService;

    public MemberSessionController(MemberSessionService memberSessionService) {
        this.memberSessionService = memberSessionService;
    }

    /** 로그아웃 — 204(항상, idempotent). 오류는 DB 예외만 500 MBR-5000. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("X-Api-Key") String apiKey) {
        memberSessionService.logout(apiKey);
        return ResponseEntity.noContent().build();
    }

    /** refresh — 200(LoginResult와 동일 셰이프) / 401 MBR-4012(4갈래 동일 취급) / 500 MBR-5000. */
    @PostMapping("/refresh")
    public MemberSessionService.SessionResult refresh(@RequestBody RefreshRequest req) {
        return memberSessionService.refresh(req.refreshToken());
    }

    public record RefreshRequest(String refreshToken) { }
}
