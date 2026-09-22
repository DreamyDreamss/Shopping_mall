// linked_func: FUNC-order-011, FUNC-order-012
// spec: docs/00_FUNC/stories/STORY-FUNC-order-011.md, docs/00_FUNC/stories/STORY-FUNC-order-012.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.CartItem;
import com.sm.lab.shop.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 장바구니 REST API — 담기/조회/수량변경/삭제 4종(SR-202 D3) + 체크아웃(SR-203 D6). */
@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** 담기 — 같은 상품 재담기는 수량 합산. 400(qty&lt;1)/404(회원·상품)/409(재고 초과·품절). */
    @PostMapping("/items")
    public CartItem addItem(@RequestBody AddCartItemRequest req) {
        return cartService.addItem(req.memberId(), req.sku(), req.qty());
    }

    /** 조회 — items[lineTotal 포함]·totalAmount. 404(회원). */
    @GetMapping
    public Map<String, Object> get(@RequestParam String memberId) {
        return cartService.get(memberId);
    }

    /** 수량 변경. 400(qty&lt;1, D4)/404(품목)/409(재고 초과). */
    @PatchMapping("/items/{sku}")
    public CartItem updateQty(@PathVariable String sku, @RequestParam String memberId,
                              @RequestBody UpdateQtyRequest req) {
        return cartService.updateQty(memberId, sku, req.qty());
    }

    /** 삭제. 404(품목). */
    @DeleteMapping("/items/{sku}")
    public ResponseEntity<Void> delete(@PathVariable String sku, @RequestParam String memberId) {
        cartService.delete(memberId, sku);
        return ResponseEntity.noContent().build();
    }

    /**
     * 체크아웃 — 장바구니 전체를 기존 주문 규칙(OrderService 재사용)으로 전환한다(FUNC-order-012,
     * SR-203 D6). 200 {orderNo,totalAmount,itemCount}. 400(빈 장바구니)/404(회원 없음)/
     * 409(재고 부족 — 품목별 사유, 전량 거부·부분 주문 금지).
     */
    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestBody CheckoutRequest req) {
        return cartService.checkout(req.memberId());
    }

    public record AddCartItemRequest(String memberId, String sku, int qty) { }
    public record UpdateQtyRequest(int qty) { }
    public record CheckoutRequest(String memberId) { }
}
