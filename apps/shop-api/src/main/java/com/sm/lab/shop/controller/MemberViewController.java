// linked_func: FUNC-order-004
// spec: docs/00_FUNC/stories/STORY-FUNC-order-004.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * 회원 상세 화면(Thymeleaf) — 회원 정보 + 최근 주문 5건 요약(SR-207, 마이페이지).
 * INF-ORD-002(GET /api/members/{memberId})를 REST로 재호출하지 않고 {@link MemberService}를
 * 직접 재사용한다(ProductViewController.loadProduct와 동일 규약 — 컨트롤러 계층 중복 금지).
 * 응답 계약(필드 구성·정렬·5건 고정·취소 주문 포함 규칙)은 SR-207에서 변경하지 않는다(AS-IS 유지,
 * MemberService.get·OrderDao.selectRecentByMember 그대로 재사용).
 * linked_func: FUNC-order-004 (SR-207)
 */
@Controller
public class MemberViewController {
    private static final Logger log = LoggerFactory.getLogger(MemberViewController.class);

    private static final String NOT_FOUND_MESSAGE = "회원을 찾을 수 없습니다.";

    private final MemberService memberService;

    public MemberViewController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 회원 상세 화면 — 최근 주문 5건(취소 주문 포함) 요약 표시. 미존재/탈퇴 회원(404)만 화면단에서
     * 흡수해 화면 고정 안내 문구로 치환하고(서비스 예외 {@code getReason()} 원문은 노출하지 않는다 —
     * QA round1 FAIL 권고, 내부 메시지 반사 제거), 그 외(5xx)는 rethrow한다(ProductViewController.
     * loadProduct와 동일 규약 — 장애 은폐 금지, project-context.md Critical Rule 1).
     *
     * <p>round1 QA FAIL 정정: 이 랩에 인증 미들웨어가 "없다"는 이전 Dev 기록은 사실 오류였다 —
     * FUNC-order-013(SR-204) {@code ApiKeyAuthFilter}가 {@code /api/**} 전역과(이번 SR-207 round2
     * 재작업으로) 이 화면 라우트({@code /member/{memberId}})까지 default-deny로 인증·인가한다.
     * 무키 401 / member 키 타인 403은 그 필터가 서블릿 체인에서 이 컨트롤러에 도달하기 **전**에
     * 이미 반환하므로, 이 컨트롤러는 인증 자체를 다루지 않는다(필터의 책임). 권한은 SR-204 전역
     * 정책(evaluateMemberScope의 {@code /member/{id}} 소유권 대조)을 그대로 따르며, 이 화면 전용
     * 권한 규칙을 컨트롤러 레벨에 별도로 추가하지 않는다.
     * linked_func: FUNC-order-004 (SR-207)
     */
    @GetMapping("/member/{memberId}")
    public String memberDetail(@PathVariable String memberId, Model model) {
        Member member = null;
        try {
            member = memberService.get(memberId);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw e;
            }
            // 미존재/탈퇴 회원(404)만 화면단 흡수 — 서비스 예외 사유는 로그로만 남기고 화면에는
            // 고정 안내 문구만 노출한다(QA round1 FAIL: getReason() 원문 반사 제거).
            log.debug("회원 상세 조회 404 흡수 — memberId={}, reason={}", memberId, e.getReason());
        }
        model.addAttribute("member", member);
        model.addAttribute("notFoundMessage", NOT_FOUND_MESSAGE);
        return "member/detail";
    }
}
