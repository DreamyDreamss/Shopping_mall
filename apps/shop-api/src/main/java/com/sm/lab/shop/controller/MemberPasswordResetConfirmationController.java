// linked_func: FUNC-member-009
// spec: docs/00_FUNC/stories/STORY-FUNC-member-009.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.service.MemberPasswordResetConfirmationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비밀번호 재설정 — 확정 API(SR-234, INF-MBR-007). 코드 확인 + 새 비밀번호 반영 + 전 기기
 * 로그아웃({@link MemberPasswordResetConfirmationService} 참고). {@link
 * com.sm.lab.shop.controller.MemberPasswordResetController}(008, 코드 요청 API)와 별도
 * 클래스로 둔다 — 1 INF = 1 엔드포인트 = 1 컨트롤러 관례 유지(INF-MBR-007이 이 컨트롤러 하나에
 * 대응).
 *
 * <p>어느 경로든(회원 있음/없음) 204를 반환한다 — 성공 시 노출할 정보가 없다(형제
 * {@code MemberSessionController.logout()}과 동일 형태). 예외는 400(형식 오류)·410(만료)·
 * 409(불일치/시도초과)·500(DB 계층)뿐이다(봉투는 {@link
 * com.sm.lab.shop.web.MemberPasswordResetConfirmationExceptionHandler} 참고).
 *
 * <p>로그인 전 사용자(비밀번호를 잊은 사람)가 호출하므로 아직 API 키가 없다 —
 * {@link com.sm.lab.shop.web.ApiKeyAuthFilter}의 화이트리스트(정확 일치,
 * {@code MEMBER_PASSWORD_RESET_CONFIRM_PATH})에 등록해 무인증으로 연다(FUNC-008과 동일 이유의
 * 예외).
 */
@RestController
@RequestMapping("/api/members/password-resets")
public class MemberPasswordResetConfirmationController {
    private final MemberPasswordResetConfirmationService confirmationService;

    public MemberPasswordResetConfirmationController(
            MemberPasswordResetConfirmationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    /** 확정 — target/code/newPassword. 성공(회원 있음/없음 무관)은 항상 204, 본문 없음. */
    @PostMapping("/confirmations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@RequestBody ConfirmPasswordResetRequest req) {
        confirmationService.confirmPasswordReset(req.target(), req.code(), req.newPassword());
    }

    public record ConfirmPasswordResetRequest(String target, String code, String newPassword) { }
}
