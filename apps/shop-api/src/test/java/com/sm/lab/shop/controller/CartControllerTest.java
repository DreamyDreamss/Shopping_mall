// linked_func: FUNC-order-011, FUNC-order-012
// spec: docs/00_FUNC/stories/STORY-FUNC-order-011.md, docs/00_FUNC/stories/STORY-FUNC-order-012.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.CartItem;
import com.sm.lab.shop.service.CartService;
import com.sm.lab.shop.support.AdminApiKeyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SR-204 R-5: 인증 필터 도입 후 admin 키 기본 주입(AdminApiKeyTestConfig) — 인증/IDOR 자체
// 검증은 ApiKeyAuthIntegrationTest에서 별도 수행.
@WebMvcTest(CartController.class)
@Import(AdminApiKeyTestConfig.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    private static CartItem cartItem(String sku, String name, long price, int qty) {
        CartItem c = new CartItem();
        c.setMemberId("M-0001");
        c.setSku(sku);
        c.setProductName(name);
        c.setPrice(price);
        c.setQty(qty);
        return c;
    }

    // linked_tc: TC-FUNC-order-011-01
    @Test
    void addItem_valid_returns200WithSavedItem() throws Exception {
        when(cartService.addItem("M-0001", "SKU-1001", 2))
                .thenReturn(cartItem("SKU-1001", "스탠딩 데스크", 390000, 2));

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"M-0001\",\"sku\":\"SKU-1001\",\"qty\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-1001"))
                .andExpect(jsonPath("$.qty").value(2))
                .andExpect(jsonPath("$.lineTotal").value(780000));

        verify(cartService).addItem("M-0001", "SKU-1001", 2);
    }

    // linked_tc: TC-FUNC-order-011-05
    @Test
    void addItem_qtyZero_returns400() throws Exception {
        when(cartService.addItem("M-0001", "SKU-1001", 0))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "수량은 1 이상이어야 합니다"));

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"M-0001\",\"sku\":\"SKU-1001\",\"qty\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addItem_memberNotFound_returns404() throws Exception {
        when(cartService.addItem("M-9999", "SKU-1001", 1))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음: M-9999"));

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"M-9999\",\"sku\":\"SKU-1001\",\"qty\":1}"))
                .andExpect(status().isNotFound());
    }

    // linked_tc: TC-FUNC-order-011-04
    @Test
    void addItem_soldOut_returns409() throws Exception {
        when(cartService.addItem("M-0001", "SKU-1004", 1))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "품절 상품: SKU-1004"));

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"M-0001\",\"sku\":\"SKU-1004\",\"qty\":1}"))
                .andExpect(status().isConflict());
    }

    // linked_tc: TC-FUNC-order-011-03
    @Test
    void addItem_exceedsStock_returns409() throws Exception {
        when(cartService.addItem("M-0001", "SKU-1001", 999))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "재고 초과: 가용 12, 요청 999"));

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"M-0001\",\"sku\":\"SKU-1001\",\"qty\":999}"))
                .andExpect(status().isConflict());
    }

    // linked_tc: TC-FUNC-order-011-06
    @Test
    void get_returnsItemsAndTotalAmount() throws Exception {
        CartItem c1 = cartItem("SKU-1001", "스탠딩 데스크", 390000, 1);
        CartItem c2 = cartItem("SKU-1002", "기계식 키보드", 129000, 2);
        when(cartService.get("M-0001")).thenReturn(Map.of("items", List.of(c1, c2), "totalAmount", 648000L));

        mockMvc.perform(get("/api/cart").param("memberId", "M-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(648000))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    // linked_tc: TC-FUNC-order-011-07
    @Test
    void get_emptyCart_returnsEmptyItemsAndZeroTotal() throws Exception {
        when(cartService.get("M-0003")).thenReturn(Map.of("items", List.of(), "totalAmount", 0L));

        mockMvc.perform(get("/api/cart").param("memberId", "M-0003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    void get_memberNotFound_returns404() throws Exception {
        when(cartService.get("M-9999"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음: M-9999"));

        mockMvc.perform(get("/api/cart").param("memberId", "M-9999"))
                .andExpect(status().isNotFound());
    }

    // linked_tc: TC-FUNC-order-011-08
    @Test
    void updateQty_valid_returns200WithUpdatedItem() throws Exception {
        when(cartService.updateQty("M-0001", "SKU-1001", 5))
                .thenReturn(cartItem("SKU-1001", "스탠딩 데스크", 390000, 5));

        mockMvc.perform(patch("/api/cart/items/SKU-1001")
                        .param("memberId", "M-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qty").value(5));

        verify(cartService).updateQty("M-0001", "SKU-1001", 5);
    }

    // linked_tc: TC-FUNC-order-011-09
    @Test
    void updateQty_qtyZero_returns400() throws Exception {
        when(cartService.updateQty("M-0001", "SKU-1001", 0))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "수량은 1 이상이어야 합니다"));

        mockMvc.perform(patch("/api/cart/items/SKU-1001")
                        .param("memberId", "M-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\":0}"))
                .andExpect(status().isBadRequest());
    }

    // linked_tc: TC-FUNC-order-011-10
    @Test
    void updateQty_exceedsStock_returns409() throws Exception {
        when(cartService.updateQty("M-0001", "SKU-1001", 999))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "재고 초과: 가용 12, 요청 999"));

        mockMvc.perform(patch("/api/cart/items/SKU-1001")
                        .param("memberId", "M-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\":999}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateQty_itemNotFound_returns404() throws Exception {
        when(cartService.updateQty("M-0001", "SKU-9999", 2))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니 품목 없음: SKU-9999"));

        mockMvc.perform(patch("/api/cart/items/SKU-9999")
                        .param("memberId", "M-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\":2}"))
                .andExpect(status().isNotFound());
    }

    // linked_tc: TC-FUNC-order-011-11
    @Test
    void delete_valid_returns204() throws Exception {
        mockMvc.perform(delete("/api/cart/items/SKU-1001").param("memberId", "M-0001"))
                .andExpect(status().isNoContent());

        verify(cartService).delete("M-0001", "SKU-1001");
    }

    @Test
    void delete_itemNotFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니 품목 없음: SKU-9999"))
                .when(cartService).delete("M-0001", "SKU-9999");

        mockMvc.perform(delete("/api/cart/items/SKU-9999").param("memberId", "M-0001"))
                .andExpect(status().isNotFound());
    }

    // ── 체크아웃 (FUNC-order-012, SR-203 D6) ───────────────────────────────

    // linked_tc: TC-FUNC-order-012-01
    @Test
    void checkout_valid_returns200WithOrderSummary() throws Exception {
        when(cartService.checkout("M-0001"))
                .thenReturn(Map.of("orderNo", "20260823-0101", "totalAmount", 909000L, "itemCount", 2));

        mockMvc.perform(post("/api/cart/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"M-0001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNo").value("20260823-0101"))
                .andExpect(jsonPath("$.totalAmount").value(909000))
                .andExpect(jsonPath("$.itemCount").value(2));

        verify(cartService).checkout("M-0001");
    }

    // linked_tc: TC-FUNC-order-012-03
    @Test
    void checkout_emptyCart_returns400() throws Exception {
        when(cartService.checkout("M-0001"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "장바구니가 비어 있습니다"));

        mockMvc.perform(post("/api/cart/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"M-0001\"}"))
                .andExpect(status().isBadRequest());
    }

    // linked_tc: TC-FUNC-order-012-04
    @Test
    void checkout_memberNotFound_returns404() throws Exception {
        when(cartService.checkout("M-9999"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음: M-9999"));

        mockMvc.perform(post("/api/cart/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"M-9999\"}"))
                .andExpect(status().isNotFound());
    }

    // linked_tc: TC-FUNC-order-012-05
    @Test
    void checkout_insufficientStock_returns409() throws Exception {
        when(cartService.checkout("M-0001"))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "재고 부족: SKU-1001"));

        mockMvc.perform(post("/api/cart/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"M-0001\"}"))
                .andExpect(status().isConflict());
    }
}
