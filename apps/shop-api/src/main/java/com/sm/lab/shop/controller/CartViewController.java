// linked_func: FUNC-order-011, FUNC-order-012
// spec: docs/00_FUNC/stories/STORY-FUNC-order-011.md, docs/00_FUNC/stories/STORY-FUNC-order-012.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.service.CartService;
import com.sm.lab.shop.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * 장바구니 조회 화면(Thymeleaf) — UIS-ORD-005(SR-202 신규, SR-203 체크아웃 폼 추가).
 * 로그인이 없어 회원을 셀렉트로 선택한다(요구사항 "담기 시점 회원ID 선택" 방식과 동일 원칙).
 * 수량변경·삭제·체크아웃은 서버렌더 폼 POST → 서비스 호출 → redirect(주문/상품 화면 관례).
 */
@Controller
public class CartViewController {
    private static final Logger log = LoggerFactory.getLogger(CartViewController.class);

    private final CartService cartService;
    private final MemberService memberService;

    public CartViewController(CartService cartService, MemberService memberService) {
        this.cartService = cartService;
        this.memberService = memberService;
    }

    /** 장바구니 조회 — memberId 미지정 시 첫 회원을 기본 선택. */
    @GetMapping("/cart")
    public String cart(@RequestParam(required = false) String memberId, Model model) {
        List<Member> members = memberService.list();
        String selected = (memberId == null || memberId.isBlank())
                ? (members.isEmpty() ? null : members.get(0).getMemberId())
                : memberId;

        model.addAttribute("members", members);
        model.addAttribute("selectedMemberId", selected == null ? "" : selected);
        model.addAttribute("cart", selected == null
                ? Map.of("items", List.of(), "totalAmount", 0L)
                : cartService.get(selected));
        return "cart/list";
    }

    /** 수량 변경 폼 제출. */
    @PostMapping("/cart/items/{sku}/update")
    public String updateQty(@PathVariable String sku, @RequestParam String memberId, @RequestParam int qty) {
        cartService.updateQty(memberId, sku, qty);
        return "redirect:/cart?memberId=" + memberId;
    }

    /** 삭제 폼 제출 — 명시적 삭제만(D4, qty=0 유도 금지). */
    @PostMapping("/cart/items/{sku}/delete")
    public String delete(@PathVariable String sku, @RequestParam String memberId) {
        cartService.delete(memberId, sku);
        return "redirect:/cart?memberId=" + memberId;
    }

    /**
     * 체크아웃 폼 제출 — {@link CartService#checkout}을 그대로 호출한다(주문 규칙 재검증·복제
     * 금지, SR-203 D6). PRG(Post-Redirect-Get) 적용: 성공은 {@code redirect:/order/{orderNo}}
     * (기존 주문 상세 화면 재사용), 실패는 {@code redirect:/cart}로 돌아가고 사유는
     * {@link RedirectAttributes} 플래시로 전달해 장바구니를 그대로 보여준다(요구사항 "장바구니
     * 보존"). 흡수 대상은 CartService가 던지는 4xx(400 빈 장바구니/404 회원 없음/409 재고 부족)로
     * 한정하고, 5xx는 rethrow한다(ProductViewController.addToCart와 동일 규약 — 장애 은폐 금지).
     * linked_func: FUNC-order-012 (SR-203)
     */
    @PostMapping("/cart/checkout")
    public String checkout(@RequestParam String memberId, RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> result = cartService.checkout(memberId);
            return "redirect:/order/" + result.get("orderNo");
        } catch (ResponseStatusException e) {
            if (!e.getStatusCode().is4xxClientError()) {
                throw e;
            }
            log.debug("체크아웃 거부(4xx) 흡수 — memberId={}, reason={}", memberId, e.getReason());
            redirectAttributes.addFlashAttribute("checkoutError", e.getReason());
            return "redirect:/cart?memberId=" + memberId;
        }
    }
}
