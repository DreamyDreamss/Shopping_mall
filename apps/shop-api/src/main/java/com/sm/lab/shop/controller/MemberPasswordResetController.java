// linked_func: FUNC-member-008
// spec: docs/00_FUNC/stories/STORY-FUNC-member-008.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.MemberPasswordResetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비밀번호 재설정 — 코드 요청 API(SR-234, INF-MBR-006). 이메일 또는 휴대폰번호를 받아 6자리
 * 코드를 생성·저장(10분 유효)하고 "발송"한다({@link MemberPasswordResetService} 참고 — 이 랩은
 * 실제 발송 게이트웨이가 없어 로그로 대체한다). 형식 오류(빈 값·100자 초과·이메일/휴대폰 형식
 * 불일치)만 400 {@code MBR-4100}으로 응답하고, 그 외(회원 존재 여부·쿨다운 위반 포함)는 항상
 * 202를 반환한다(봉투는 {@link com.sm.lab.shop.web.MemberPasswordResetExceptionHandler} 참고).
 * 예상치 못한 DB 계층 예외는 500 {@code MBR-5000}(정제 메시지)으로 응답한다.
 *
 * <p>코드 확인 + 새 비밀번호 반영 + 전 기기 로그아웃(INF-MBR-007)은 이 FUNC(FUNC-member-008)
 * 범위 밖이다 — 별도 FUNC-member-009에서 구현한다.
 *
 * <p>로그인 전 사용자(비밀번호를 잊은 사람)가 호출하므로 아직 API 키가 없다 —
 * {@link com.sm.lab.shop.web.ApiKeyAuthFilter}의 화이트리스트(정확 일치,
 * {@code MEMBER_PASSWORD_RESET_CODE_PATH})에 등록해 무인증으로 연다(회원가입 인증코드 발송
 * API와 동일한 이유의 예외).
 */
@RestController
@RequestMapping("/api/members/password-resets")
public class MemberPasswordResetController {
    private final MemberPasswordResetService memberPasswordResetService;

    public MemberPasswordResetController(MemberPasswordResetService memberPasswordResetService) {
        this.memberPasswordResetService = memberPasswordResetService;
    }

    /** 코드 요청 — target 형식(이메일/휴대폰번호)으로 채널 자동 판별. 형식 오류만 400, 그 외 항상 202. */
    @PostMapping("/codes")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MemberPasswordResetService.VerificationCodeResult requestCode(
            @RequestBody RequestPasswordResetCodeRequest req) {
        return memberPasswordResetService.requestPasswordResetCode(req.target());
    }

    public record RequestPasswordResetCodeRequest(String target) { }
}
