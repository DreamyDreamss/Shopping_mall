// linked_func: FUNC-order-017 — SR-217
// spec: docs/00_FUNC/stories/STORY-FUNC-order-017.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.MemberGrade;
import com.sm.lab.shop.service.MemberService;
import com.sm.lab.shop.support.AdminApiKeyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-217(FUNC-order-017) — GET /api/members/grades 웹 계층 단위 테스트. 인증 자체
 * (무키 통과 여부)는 실 서버가 필요한 {@link com.sm.lab.shop.web.ApiKeyAuthIntegrationTest}에서
 * 검증하고, 이 클래스는 응답 계약(code/name/discountRate 필드 구성)만 확인한다
 * (AdminApiKeyTestConfig — 다른 슬라이스 테스트와 동일 관례).
 */
@WebMvcTest(MemberController.class)
@Import(AdminApiKeyTestConfig.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    // linked_func: FUNC-order-017 — 등급 코드·이름·할인율(%) 3필드가 그대로 노출된다(MemberGrade
    // enum @JsonFormat(shape=OBJECT) 직렬화 계약).
    @Test
    void grades_returnsCodeNameDiscountRateForEachGrade() throws Exception {
        when(memberService.listGrades()).thenReturn(List.of(MemberGrade.values()));

        mockMvc.perform(get("/api/members/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].code").value("BRONZE"))
                .andExpect(jsonPath("$[0].name").value("브론즈"))
                .andExpect(jsonPath("$[0].discountRate").value(0))
                .andExpect(jsonPath("$[3].code").value("VIP"))
                .andExpect(jsonPath("$[3].discountRate").value(10));

        verify(memberService).listGrades();
    }

    // linked_func: FUNC-order-017 — 등급 코드 중복 정의 금지(SR-217 확정 문답) 회귀: 컨트롤러는
    // MemberService.listGrades() 위임만 하고 자체 상수를 만들지 않는다(코드 리뷰 성격의 계약
    // 테스트 — 위임 자체를 검증해 새 하드코딩 목록이 슬쩍 추가되는 것을 막는다).
    @Test
    void grades_delegatesToServiceOnly_noControllerLocalConstants() throws Exception {
        when(memberService.listGrades()).thenReturn(List.of());

        mockMvc.perform(get("/api/members/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(memberService).listGrades();
    }
}
