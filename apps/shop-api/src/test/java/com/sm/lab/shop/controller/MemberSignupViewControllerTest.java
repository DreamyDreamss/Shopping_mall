// linked_func: FUNC-member-001
// spec: docs/00_FUNC/stories/STORY-FUNC-member-001.md
package com.sm.lab.shop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// SR-231(FUNC-member-001) — GET /member/signup은 정적 뼈대만 렌더한다(모델 데이터 없음, 전부
// 클라이언트 스크립트가 API로 채운다). /member/signup은 ApiKeyAuthFilter 화이트리스트에 있어
// 이 슬라이스 테스트도 admin 키 주입(AdminApiKeyTestConfig) 없이 그대로 통과한다 — 인가 자체
// (무키 200) 회귀는 ApiKeyAuthIntegrationTest#signupScreenRoute_withoutApiKey_returns200에서
// 별도 검증한다(이 클래스와 동일한 원리로 ProductViewControllerTest도 화이트리스트 경로라
// admin 키를 주입하지 않는다).
@WebMvcTest(MemberSignupViewController.class)
class MemberSignupViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // linked_tc: TC-FUNC-member-001-01
    // 커버: 화면 진입 — 3단계 마법사 뼈대(STEP 표시·가입수단 입력·약관 동의 영역)가 렌더된다
    @Test
    void signupForm_rendersWizardSkeleton() throws Exception {
        mockMvc.perform(get("/member/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/signup"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("STEP 1/3")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("인증코드 받기")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("이용약관")));
    }

    // linked_tc: TC-FUNC-member-001-02
    // 커버: r?QA — 렌더 본문에 linked_func 추적 주석이 노출되지 않는다(파서레벨 주석 통일,
    // ProductViewControllerTest.detail_found_doesNotExposeLinkedFuncCommentInRenderedBody와 동일 규약)
    @Test
    void signupForm_doesNotExposeLinkedFuncCommentInRenderedBody() throws Exception {
        mockMvc.perform(get("/member/signup"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("linked_func"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("docs/00_FUNC/stories"))));
    }
}
