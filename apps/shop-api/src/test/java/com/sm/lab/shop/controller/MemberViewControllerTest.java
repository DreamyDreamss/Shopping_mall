// linked_func: FUNC-order-004
// spec: docs/00_FUNC/stories/STORY-FUNC-order-004.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.domain.OrderSummary;
import com.sm.lab.shop.service.MemberService;
import com.sm.lab.shop.support.AdminApiKeyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// SR-207 (FUNC-order-004): GET /member/{memberId} — 회원 상세 + 최근 주문 5건 요약 화면.
// INF-ORD-002(GET /api/members/{memberId}) 응답 계약은 바꾸지 않으므로 MemberService를 목킹해
// 화면 계층(빈 상태 문구·주문 상세 링크·404 흡수·5xx rethrow)만 검증한다.
// round2 재작업(QA round1 FAIL 필수2): SR-204 ApiKeyAuthFilter가 @WebMvcTest 슬라이스에도 Filter
// 빈으로 포함되어 /member/{id}도 default-deny 대상이 됐다(round2부터 evaluateMemberScope가 소유권을
// 대조) — 이 클래스는 화면 계층만 보므로 다른 비화이트리스트 컨트롤러 테스트(CartControllerTest 등)와
// 동일하게 admin 키를 기본 주입해 인가를 우회한다. 인가 자체(무키 401/자기 200/타인 403) 회귀는
// ApiKeyAuthIntegrationTest에서 별도 검증한다.
@WebMvcTest(MemberViewController.class)
@Import(AdminApiKeyTestConfig.class)
class MemberViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    // linked_func: FUNC-order-004 — 회원 정보·최근 주문 목록(주문번호 링크 포함)이 그대로 렌더된다
    @Test
    void memberDetail_found_rendersMemberInfoAndRecentOrders() throws Exception {
        Member member = new Member();
        member.setMemberId("M-0001");
        member.setMemberName("김실증");
        member.setGrade("GOLD");
        member.setPhone("010-0000-0000");
        member.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        OrderSummary o = new OrderSummary();
        o.setOrderNo("20260817-0001");
        o.setOrderState("PLACED");
        o.setTotalAmount(129000);
        o.setOrderedAt(LocalDateTime.of(2026, 8, 17, 9, 0));
        member.setRecentOrders(List.of(o));
        when(memberService.get("M-0001")).thenReturn(member);

        mockMvc.perform(get("/member/M-0001"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/detail"))
                .andExpect(content().string(containsString("김실증")))
                .andExpect(content().string(containsString("20260817-0001")))
                .andExpect(content().string(containsString("/order/20260817-0001")));
    }

    // linked_func: FUNC-order-004 — SR-207 신규: recentOrders 빈 배열 → "최근 주문 없음" 문구
    @Test
    void memberDetail_emptyRecentOrders_showsNoOrdersMessage() throws Exception {
        Member member = new Member();
        member.setMemberId("M-0002");
        member.setMemberName("신규회원");
        member.setGrade("BRONZE");
        member.setPhone("010-1111-1111");
        member.setCreatedAt(LocalDateTime.of(2026, 9, 1, 0, 0));
        when(memberService.get("M-0002")).thenReturn(member);

        mockMvc.perform(get("/member/M-0002"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("최근 주문 없음")));
    }

    // linked_func: FUNC-order-004 — 취소 주문도 회귀 유지(현행 API 규칙 그대로, 별도 필터 없음)
    @Test
    void memberDetail_withCanceledOrder_stillRendersInList() throws Exception {
        Member member = new Member();
        member.setMemberId("M-0003");
        member.setMemberName("취소회원");
        member.setGrade("SILVER");
        member.setPhone("010-2222-2222");
        member.setCreatedAt(LocalDateTime.of(2026, 2, 1, 0, 0));
        OrderSummary canceled = new OrderSummary();
        canceled.setOrderNo("20260820-0002");
        canceled.setOrderState("CANCELED");
        canceled.setTotalAmount(50000);
        canceled.setOrderedAt(LocalDateTime.of(2026, 8, 20, 10, 0));
        member.setRecentOrders(List.of(canceled));
        when(memberService.get("M-0003")).thenReturn(member);

        mockMvc.perform(get("/member/M-0003"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CANCELED")));
    }

    // linked_func: FUNC-order-004 — 회원 없음/탈퇴 회원(404)은 화면단에서 흡수해 화면 고정 문구로
    // 치환한다(round2 재작업 QA round1 FAIL 권고: 서비스 예외 getReason() 원문 노출 금지 — 내부
    // 메시지가 그대로 반사되지 않는지 검증).
    @Test
    void memberDetail_notFound_absorbsAndShowsFixedFriendlyMessage() throws Exception {
        when(memberService.get("M-9999"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음: M-9999"));

        mockMvc.perform(get("/member/M-9999"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/detail"))
                .andExpect(content().string(containsString("회원을 찾을 수 없습니다.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("회원 없음: M-9999"))));
    }

    // linked_func: FUNC-order-004 — 5xx는 장애 은폐 없이 rethrow(project-context.md Critical Rule 1)
    @Test
    void memberDetail_serverError_rethrowsAndDoesNotAbsorb() throws Exception {
        when(memberService.get("M-0001"))
                .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DB 오류"));

        mockMvc.perform(get("/member/M-0001"))
                .andExpect(status().isInternalServerError());
    }
}
