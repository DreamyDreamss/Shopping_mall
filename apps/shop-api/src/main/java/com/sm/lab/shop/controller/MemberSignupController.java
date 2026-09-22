// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.MemberSignupService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입 — 인증코드 발송 API(SR-231, INF-MBR-001). 이메일 또는 휴대폰번호를 받아 6자리
 * 인증코드를 생성·저장(5분 유효)하고 "발송"한다({@link MemberSignupService} 참고 — 이 랩은
 * 실제 발송 게이트웨이가 없어 로그로 대체한다). 형식 오류(빈 값·100자 초과·이메일/휴대폰 형식
 * 불일치)는 400 {@code MEMBER_TARGET_INVALID}, 레이트리밋 위반은 429
 * {@code MEMBER_VERIFY_COOLDOWN}/{@code MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED}로 응답한다
 * (봉투는 {@link com.sm.lab.shop.web.MemberSignupExceptionHandler} 참고). round4부터
 * 예상치 못한 DB 계층 예외는 500 {@code MBR-5000}(정제 메시지, 경로·SQL·커넥션 정보
 * 미노출 — 원본은 서버 로그로만)으로 응답한다.
 *
 * <p>인증코드 확인 + 비밀번호/이름/마케팅 동의 입력으로 회원을 실제 생성하는 가입완료
 * (INF-MBR-002)는 이 FUNC(FUNC-member-002) 범위 밖이다 — 별도 FUNC-member-003에서 구현한다.
 *
 * <p>신규 가입자는 아직 API 키가 없다(회원이 아니므로 발급받을 수 없음) — 이 엔드포인트는
 * {@link com.sm.lab.shop.web.ApiKeyAuthFilter}의 화이트리스트(MEMBER_SIGNUP_VERIFICATION_CODE_PATH)
 * 에 등록해 무인증으로 연다. SR-217 등급 조회(MEMBER_GRADES_PATH)와 동일한 이유의 예외다.
 */
@RestController
@RequestMapping("/api/members/signup")
public class MemberSignupController {
    private final MemberSignupService memberSignupService;

    public MemberSignupController(MemberSignupService memberSignupService) {
        this.memberSignupService = memberSignupService;
    }

    /** 인증코드 발송 — target 형식(이메일/휴대폰번호)으로 채널 자동 판별. 400(형식 오류). */
    @PostMapping("/verification-codes")
    public MemberSignupService.VerificationCodeResult sendVerificationCode(
            @RequestBody SendVerificationCodeRequest req) {
        return memberSignupService.requestVerificationCode(req.target());
    }

    public record SendVerificationCodeRequest(String target) { }
}
