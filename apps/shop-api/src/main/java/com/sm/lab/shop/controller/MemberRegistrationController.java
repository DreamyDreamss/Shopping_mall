// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.MemberRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가입 요청 API(SR-231, INF-MBR-002) — {@code POST /api/members/signup}. 인증코드 확인 +
 * 비밀번호·이름·마케팅 수신 동의를 받아 회원을 생성한다({@link MemberRegistrationService} 참고).
 * 인증코드 발송(INF-MBR-001)은 별도 FUNC(FUNC-member-002, {@code MemberSignupController})의
 * 책임이며 이 컨트롤러는 그 클래스를 건드리지 않는다 — 기본 경로(베이스 {@code /api/members})가
 * 겹치지 않아 두 컨트롤러가 각자 독립적으로 라우팅된다
 * ({@code /api/members/signup/verification-codes} vs {@code /api/members/signup}).
 *
 * <p>신규 가입자는 아직 API 키가 없다 — {@link com.sm.lab.shop.web.ApiKeyAuthFilter}
 * 화이트리스트(정확 일치, {@code MEMBER_SIGNUP_REQUEST_PATH})에 등록해 무인증으로 연다
 * (FUNC-member-002의 인증코드 발송 API와 동일한 이유의 예외).
 */
@RestController
@RequestMapping("/api/members")
public class MemberRegistrationController {
    private final MemberRegistrationService memberRegistrationService;

    public MemberRegistrationController(MemberRegistrationService memberRegistrationService) {
        this.memberRegistrationService = memberRegistrationService;
    }

    /** 가입 요청 — 인증 완료(최근 30분 이내) + 중복 없음 + 비밀번호 규칙 충족 시 201. */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberRegistrationService.SignupResult signUp(@RequestBody SignupRequest req) {
        return memberRegistrationService.signUp(req.target(), req.code(), req.password(), req.name(),
                req.marketingOptIn() != null && req.marketingOptIn());
    }

    public record SignupRequest(String target, String code, String password, String name, Boolean marketingOptIn) { }
}
