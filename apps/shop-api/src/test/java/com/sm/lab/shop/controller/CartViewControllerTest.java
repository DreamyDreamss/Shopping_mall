// linked_func: FUNC-order-011, FUNC-order-012
// spec: docs/00_FUNC/stories/STORY-FUNC-order-011.md, docs/00_FUNC/stories/STORY-FUNC-order-012.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.CartItem;
import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.service.CartService;
import com.sm.lab.shop.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CartViewController.class)
class CartViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;
    @MockBean
    private MemberService memberService;

    private static Member member(String id, String name) {
        Member m = new Member();
        m.setMemberId(id);
        m.setMemberName(name);
        return m;
    }

    private static CartItem cartItem(String sku, String name, long price, int qty) {
        CartItem c = new CartItem();
        c.setMemberId("M-0001");
        c.setSku(sku);
        c.setProductName(name);
        c.setPrice(price);
        c.setQty(qty);
        return c;
    }

    // SR-224 scr_empty_state: 빈 목록 안내 문구를 "조회 결과가 없습니다"로 통일(검색조건 없음 —
    // 회원 셀렉트는 조회 대상 식별용이지 필터가 아니므로 "조건에 맞는" 변형은 대상 아님).
    private static final String EMPTY_RESULT_MESSAGE = "조회 결과가 없습니다";

    // linked_tc: TC-FUNC-order-011-12, TC-FUNC-order-012-07
    @Test
    void cart_withItems_rendersTableWithLineTotalsAndGrandTotal() throws Exception {
        when(memberService.list()).thenReturn(List.of(member("M-0001", "김실증")));
        CartItem c1 = cartItem("SKU-1001", "스탠딩 데스크", 390000, 1);
        when(cartService.get("M-0001")).thenReturn(Map.of("items", List.of(c1), "totalAmount", 390000L));

        mockMvc.perform(get("/cart").param("memberId", "M-0001"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/list"))
                .andExpect(content().string(containsString("스탠딩 데스크")))
                .andExpect(content().string(containsString("390,000")))
                // TC-FUNC-order-012-07: 품목이 있으면 [주문하기] 폼(체크아웃) 노출
                .andExpect(content().string(containsString("/cart/checkout")));

        verify(cartService).get("M-0001");
    }

    // linked_tc: TC-FUNC-order-011-13, TC-FUNC-order-012-08
    @Test
    void cart_empty_showsEmptyMessageAndProductListLink() throws Exception {
        when(memberService.list()).thenReturn(List.of(member("M-0003", "박푸딩")));
        when(cartService.get("M-0003")).thenReturn(Map.of("items", List.of(), "totalAmount", 0L));

        mockMvc.perform(get("/cart").param("memberId", "M-0003"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(EMPTY_RESULT_MESSAGE)))
                .andExpect(content().string(containsString("/product/list")))
                // TC-FUNC-order-012-08: 빈 장바구니는 [주문하기] 폼(체크아웃) 미노출
                .andExpect(content().string(not(containsString("/cart/checkout"))));
    }

    // linked_tc: TC-FUNC-order-011-13 (round7 재작업 — QA r7 권고4/재작업지시4,
    // checkoutError == null 가드 회귀 테스트). 이미 빈 장바구니에서 주문하기를 재제출(중복 submit·
    // 뒤로가기·동시 체크아웃 패자)하면 checkout()이 4xx를 흡수해 checkoutError 플래시와 함께
    // redirect:/cart로 돌아온다 — 그 다음 GET에서 목록도 실제로 비어 있으므로, 가드가 없으면
    // "체크아웃 실패: ..." 배너와 "조회 결과가 없습니다" 안내가 동시 노출된다(형제 order/list.html의
    // SR-208 searchError == null 규약 위반). 가드 적용 후에는 오류 배너만 보이고 빈 목록 안내는 숨는다.
    // round8 재작업(QA r8 CONCERNS 권고1) — 가드는 안내 문구에만 걸리고 "상품 목록으로" 이동 링크는
    // checkoutError 유무와 무관하게 항상 남아 있어야 한다(UIS-ORD-005/spec.md의 "문구 + 이동 링크"
    // 규정). 아래 마지막 assertion이 그 회귀를 막는다 — 가드를 다시 div 전체로 넓히면 이 assertion이 실패한다.
    @Test
    void cart_emptyWithCheckoutError_hidesEmptyMessageShowsErrorBannerOnly() throws Exception {
        when(memberService.list()).thenReturn(List.of(member("M-0001", "김실증")));
        when(cartService.get("M-0001")).thenReturn(Map.of("items", List.of(), "totalAmount", 0L));

        mockMvc.perform(get("/cart").param("memberId", "M-0001")
                        .flashAttr("checkoutError", "장바구니가 비어 있습니다"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("체크아웃 실패: 장바구니가 비어 있습니다")))
                .andExpect(content().string(not(containsString(EMPTY_RESULT_MESSAGE))))
                // round8: 빈 장바구니 + 체크아웃 오류 상태에서도 "상품 목록으로" 이동 링크는 남아 있어야 한다.
                .andExpect(content().string(containsString("/product/list")));
    }

    @Test
    void cart_noMemberIdParam_defaultsToFirstMember() throws Exception {
        when(memberService.list()).thenReturn(List.of(member("M-0001", "김실증"), member("M-0002", "이도그")));
        when(cartService.get("M-0001")).thenReturn(Map.of("items", List.of(), "totalAmount", 0L));

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk());

        verify(cartService).get("M-0001");
    }

    // linked_tc: TC-FUNC-order-011-14
    @Test
    void updateQty_redirectsBackToCart() throws Exception {
        mockMvc.perform(post("/cart/items/SKU-1001/update")
                        .param("memberId", "M-0001")
                        .param("qty", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart?memberId=M-0001"));

        verify(cartService).updateQty("M-0001", "SKU-1001", 5);
    }

    // linked_tc: TC-FUNC-order-011-14
    @Test
    void delete_redirectsBackToCart() throws Exception {
        mockMvc.perform(post("/cart/items/SKU-1001/delete")
                        .param("memberId", "M-0001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart?memberId=M-0001"));

        verify(cartService).delete("M-0001", "SKU-1001");
    }

    // ── 체크아웃 폼 제출 (FUNC-order-012, SR-203 D6) ───────────────────────

    // linked_tc: TC-FUNC-order-012-09
    @Test
    void checkout_success_redirectsToOrderDetail() throws Exception {
        when(cartService.checkout("M-0001"))
                .thenReturn(Map.of("orderNo", "20260823-0101", "totalAmount", 909000L, "itemCount", 2));

        mockMvc.perform(post("/cart/checkout").param("memberId", "M-0001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/20260823-0101"));

        verify(cartService).checkout("M-0001");
    }

    // linked_tc: TC-FUNC-order-012-10
    @Test
    void checkout_insufficientStock_redirectsBackToCartWithFlashReason() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "재고 부족: SKU-1001"))
                .when(cartService).checkout("M-0001");

        mockMvc.perform(post("/cart/checkout").param("memberId", "M-0001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart?memberId=M-0001"))
                .andExpect(flash().attribute("checkoutError", "재고 부족: SKU-1001"));
    }

    // linked_tc: TC-FUNC-order-012-10 (빈 장바구니 400도 동일 규약으로 흡수)
    @Test
    void checkout_emptyCart_redirectsBackToCartWithFlashReason() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "장바구니가 비어 있습니다"))
                .when(cartService).checkout("M-0001");

        mockMvc.perform(post("/cart/checkout").param("memberId", "M-0001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart?memberId=M-0001"))
                .andExpect(flash().attribute("checkoutError", "장바구니가 비어 있습니다"));
    }

    // 5xx는 화면단에서 흡수하지 않고 rethrow해야 한다(Critical Rule #1, ProductViewController.addToCart와
    // 동일 규약 — r6 QA 권고1 커버 테스트와 동일 형태).
    @Test
    void checkout_serviceThrowsNon4xxStatus_isNotAbsorbed() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "DB 점검중"))
                .when(cartService).checkout("M-0001");

        mockMvc.perform(post("/cart/checkout").param("memberId", "M-0001"))
                .andExpect(status().isServiceUnavailable());
    }

    // r1 QA 규약(v4.19.29) — 템플릿 주석은 파서레벨만 사용, 렌더 본문에 linked_func 노출 금지
    @Test
    void cart_doesNotExposeLinkedFuncCommentInRenderedBody() throws Exception {
        when(memberService.list()).thenReturn(List.of(member("M-0001", "김실증")));
        when(cartService.get("M-0001")).thenReturn(Map.of("items", List.of(), "totalAmount", 0L));

        mockMvc.perform(get("/cart").param("memberId", "M-0001"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("linked_func"))))
                .andExpect(content().string(not(containsString("docs/00_FUNC/stories"))));
    }
}
