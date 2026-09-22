// linked_func: FUNC-member-005
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.MemberLoginService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 API(SR-232, INF-MBR-003) — {@code POST /api/members/login}. 이메일/비밀번호 로그인,
 * 30일 자동 로그인(리프레시 토큰), 5회 실패 시 10분 잠금({@link MemberLoginService} 참고).
 *
 * <p>로그인 전에는 아직 API 키가 없다(회원이 아직 인증되지 않음) — 이 엔드포인트는
 * {@link com.sm.lab.shop.web.ApiKeyAuthFilter} 화이트리스트(정확 일치)에 등록해 무인증으로
 * 연다(FUNC-member-002/003의 가입 관련 엔드포인트와 동일한 이유의 예외).
 *
 * <p>로그아웃·리프레시 토큰 재발급(FUNC-member-006)은 이 FUNC 범위 밖 — 별도 컨트롤러에서
 * 구현한다.
 */
@RestController
@RequestMapping("/api/members")
public class MemberLoginController {
    private final MemberLoginService memberLoginService;

    public MemberLoginController(MemberLoginService memberLoginService) {
        this.memberLoginService = memberLoginService;
    }

    /** 로그인 — 200(세션 발급) / 401 MBR-4011(자격 불일치, n/5) / 429 MBR-4291(잠김). */
    @PostMapping("/login")
    public MemberLoginService.LoginResult login(@RequestBody LoginRequest req) {
        return memberLoginService.login(req.email(), req.password());
    }

    public record LoginRequest(String email, String password) { }
}
