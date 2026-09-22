// linked_func: FUNC-order-017 (grades 엔드포인트) — SR-217
// spec: docs/00_FUNC/stories/STORY-FUNC-order-017.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.domain.MemberGrade;
import com.sm.lab.shop.service.MemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /** 회원 목록(탈퇴 제외). */
    @GetMapping
    public List<Member> list() {
        return memberService.list();
    }

    /**
     * 등급 코드·이름·할인율(%) 목록(SR-217) — 인증 불필요(공개 정보,
     * {@link com.sm.lab.shop.web.ApiKeyAuthFilter} 화이트리스트). 등급 정의는
     * {@link MemberGrade} 한 곳뿐이며 이 API는 그 값을 그대로 노출한다(중복 정의 금지).
     *
     * <p>Spring MVC는 리터럴 세그먼트("/grades")를 경로변수("/{memberId}")보다 등록 순서와
     * 무관하게 우선 매칭한다(project-context.md API 구조 패턴 실측) — {@code memberId="grades"}로
     * 오인해 {@link #get}으로 라우팅될 위험이 없다.
     */
    @GetMapping("/grades")
    public List<MemberGrade> grades() {
        return memberService.listGrades();
    }

    /** 회원 상세 조회. */
    @GetMapping("/{memberId}")
    public Member get(@PathVariable String memberId) {
        return memberService.get(memberId);
    }
}
