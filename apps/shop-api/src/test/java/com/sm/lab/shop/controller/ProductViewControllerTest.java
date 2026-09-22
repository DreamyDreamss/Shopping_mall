// linked_func: FUNC-order-009, FUNC-order-010
// spec: docs/00_FUNC/stories/STORY-FUNC-order-009.md, docs/00_FUNC/stories/STORY-FUNC-order-010.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.CartItem;
import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.domain.Product;
import com.sm.lab.shop.service.CartService;
import com.sm.lab.shop.service.MemberService;
import com.sm.lab.shop.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ProductViewController.class)
class ProductViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;
    @MockBean
    private MemberService memberService;
    @MockBean
    private CartService cartService;

    private static Member member(String id, String name) {
        Member m = new Member();
        m.setMemberId(id);
        m.setMemberName(name);
        return m;
    }

    // linked_tc: TC-FUNC-order-009-01
    // 커버: AC1, AC2, AC5 — 목록 렌더 + 상태 뱃지(판매중/품절) + 상세 링크
    @Test
    void list_default_rendersTableWithStateBadgesAndDetailLink() throws Exception {
        Product inStock = new Product();
        inStock.setSku("SKU-1001");
        inStock.setProductName("마우스");
        inStock.setPrice(25000L);
        inStock.setStockQty(100);
        inStock.setSaleYn("Y");

        Product soldOut = new Product();
        soldOut.setSku("SKU-1002");
        soldOut.setProductName("키보드");
        soldOut.setPrice(59000L);
        soldOut.setStockQty(0);
        soldOut.setSaleYn("Y");

        when(productService.listSorted(null, "latest")).thenReturn(List.of(inStock, soldOut));

        mockMvc.perform(get("/product/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/list"))
                .andExpect(model().attribute("keyword", ""))
                .andExpect(content().string(containsString("SKU-1001")))
                .andExpect(content().string(containsString("판매중")))
                .andExpect(content().string(containsString("품절")))
                .andExpect(content().string(containsString("/product/SKU-1001")));

        verify(productService).listSorted(null, "latest");
    }

    // linked_tc: TC-FUNC-order-009-03
    // 커버: AC3 — 키워드 검색은 부분일치를 서비스에 위임
    @Test
    void list_withKeyword_passesKeywordToServiceAndRendersResult() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1002");
        p.setProductName("기계식 키보드");
        p.setPrice(59000L);
        p.setStockQty(5);
        p.setSaleYn("Y");
        when(productService.listSorted("키보드", "latest")).thenReturn(List.of(p));

        mockMvc.perform(get("/product/list").param("keyword", "키보드"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("keyword", "키보드"))
                .andExpect(content().string(containsString("기계식 키보드")));

        verify(productService).listSorted("키보드", "latest");
    }

    // linked_tc: TC-FUNC-order-009-04
    // 커버: AC4, SR-224 확정요건 — 검색 조건(keyword)이 걸린 0건은 '조건에 맞는 결과가 없습니다'
    @Test
    void list_noResultsWithKeyword_showsConditionSpecificEmptyMessage() throws Exception {
        when(productService.listSorted("존재하지않는상품XYZ", "latest")).thenReturn(List.of());

        mockMvc.perform(get("/product/list").param("keyword", "존재하지않는상품XYZ"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("조건에 맞는 결과가 없습니다")))
                .andExpect(content().string(not(containsString("표시할 상품이 없습니다"))));
    }

    // linked_tc: TC-FUNC-order-009-05
    // 커버: SR-224 확정요건 — 검색 조건 없이 0건이면 '조회 결과가 없습니다'로 통일(조건부 문구와 구분)
    @Test
    void list_noResultsWithoutKeyword_showsUnifiedEmptyMessage() throws Exception {
        when(productService.listSorted(null, "latest")).thenReturn(List.of());

        mockMvc.perform(get("/product/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("조회 결과가 없습니다")))
                .andExpect(content().string(not(containsString("조건에 맞는 결과가 없습니다"))))
                .andExpect(content().string(not(containsString("표시할 상품이 없습니다"))));
    }

    // linked_tc: TC-FUNC-order-009-06
    // 커버: SR-214 AC — 기본 진입(sort 미지정)은 "기본순"이 선택되고 서비스에 "latest"로 위임된다
    // (라벨 정정, 재작업 지시 SR-214 QA r5 — 실제 동작은 ORDER BY p.sku ASC이므로 "최신순"이 아님)
    // round 10 재작업: value/selected 속성만으로는 라벨이 "최신순"으로 되돌아가도 통과했다(QA r6 권고1) —
    // 셀렉트 3개 선택지의 실제 렌더 텍스트를 직접 단언해 라벨 회귀를 잠근다.
    @Test
    void list_default_sortDefaultsToLatestAndRendersSelected() throws Exception {
        when(productService.listSorted(null, "latest")).thenReturn(List.of());

        mockMvc.perform(get("/product/list"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "latest"))
                .andExpect(content().string(containsString("value=\"latest\" selected=\"selected\"")))
                .andExpect(content().string(containsString(">기본순</option>")))
                .andExpect(content().string(containsString(">가격 낮은순</option>")))
                .andExpect(content().string(containsString(">가격 높은순</option>")))
                .andExpect(content().string(not(containsString("최신순"))));

        verify(productService).listSorted(null, "latest");
    }

    // linked_tc: TC-FUNC-order-009-07
    // 커버: SR-214 AC — sort=priceAsc는 서비스에 그대로 위임되고 셀렉트는 "가격 낮은순"을 선택 표시한다
    // round 10 재작업: 3개 선택지 표시 텍스트 전부를 단언(QA r6 권고1 — 라벨 회귀 잠금)
    @Test
    void list_withSortPriceAsc_delegatesToServiceAndRendersSelected() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1002");
        p.setProductName("기계식 키보드");
        p.setPrice(129000L);
        p.setStockQty(40);
        p.setSaleYn("Y");
        when(productService.listSorted(null, "priceAsc")).thenReturn(List.of(p));

        mockMvc.perform(get("/product/list").param("sort", "priceAsc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "priceAsc"))
                .andExpect(content().string(containsString("value=\"priceAsc\" selected=\"selected\"")))
                .andExpect(content().string(containsString(">기본순</option>")))
                .andExpect(content().string(containsString(">가격 낮은순</option>")))
                .andExpect(content().string(containsString(">가격 높은순</option>")))
                .andExpect(content().string(not(containsString("최신순"))));

        verify(productService).listSorted(null, "priceAsc");
    }

    // linked_tc: TC-FUNC-order-009-08
    // 커버: SR-214 AC — sort=priceDesc는 서비스에 그대로 위임되고 셀렉트는 "가격 높은순"을 선택 표시한다
    // round 10 재작업: 3개 선택지 표시 텍스트 전부를 단언(QA r6 권고1 — 라벨 회귀 잠금)
    @Test
    void list_withSortPriceDesc_delegatesToServiceAndRendersSelected() throws Exception {
        when(productService.listSorted(null, "priceDesc")).thenReturn(List.of());

        mockMvc.perform(get("/product/list").param("sort", "priceDesc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "priceDesc"))
                .andExpect(content().string(containsString("value=\"priceDesc\" selected=\"selected\"")))
                .andExpect(content().string(containsString(">기본순</option>")))
                .andExpect(content().string(containsString(">가격 낮은순</option>")))
                .andExpect(content().string(containsString(">가격 높은순</option>")))
                .andExpect(content().string(not(containsString("최신순"))));

        verify(productService).listSorted(null, "priceDesc");
    }

    // linked_tc: TC-FUNC-order-009-09
    // 커버: SR-214 확정요건 — 정렬 값이 화이트리스트 밖(조작된 URL)이면 오류 화면 대신 조용히
    // "latest"로 되돌리고, 서비스에도 "latest"로 위임한다(원본 잘못된 값은 전달하지 않음)
    @Test
    void list_withInvalidSort_silentlyFallsBackToLatestWithoutErrorPage() throws Exception {
        when(productService.listSorted(null, "latest")).thenReturn(List.of());

        mockMvc.perform(get("/product/list").param("sort", "bogus"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/list"))
                .andExpect(model().attribute("sort", "latest"))
                .andExpect(content().string(containsString("value=\"latest\" selected=\"selected\"")));

        verify(productService).listSorted(null, "latest");
    }

    // linked_tc: TC-FUNC-order-010-01
    // 커버: AC1, AC2, AC3(판매중) — 상세 렌더(SKU·상품명·가격·재고·상태), 등록일 미표시(D1)
    @Test
    void detail_found_rendersProductInfoWithoutRegistrationDate() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1001");
        p.setProductName("마우스");
        p.setPrice(25000L);
        p.setStockQty(100);
        p.setSaleYn("Y");
        when(productService.get("SKU-1001")).thenReturn(p);

        mockMvc.perform(get("/product/SKU-1001"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/detail"))
                .andExpect(content().string(containsString("SKU-1001")))
                .andExpect(content().string(containsString("마우스")))
                .andExpect(content().string(containsString("25,000원")))
                .andExpect(content().string(containsString("판매중")))
                .andExpect(content().string(not(containsString("등록일"))));

        verify(productService).get("SKU-1001");
    }

    // linked_tc: TC-FUNC-order-010-02
    // 커버: AC3(품절) — stockQty==0 → "품절" 표시
    @Test
    void detail_soldOut_showsSoldOutState() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1002");
        p.setProductName("키보드");
        p.setPrice(59000L);
        p.setStockQty(0);
        p.setSaleYn("Y");
        when(productService.get("SKU-1002")).thenReturn(p);

        mockMvc.perform(get("/product/SKU-1002"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("품절")));
    }

    // linked_tc: TC-FUNC-order-010-03
    // 커버: AC4 — "목록으로" 링크가 /product/list로 이동
    @Test
    void detail_found_showsBackToListLink() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1001");
        p.setProductName("마우스");
        p.setPrice(25000L);
        p.setStockQty(100);
        p.setSaleYn("Y");
        when(productService.get("SKU-1001")).thenReturn(p);

        mockMvc.perform(get("/product/SKU-1001"))
                .andExpect(content().string(containsString("목록으로")))
                .andExpect(content().string(containsString("/product/list")));
    }

    // linked_tc: TC-FUNC-order-010-04
    // 커버: AC5 — 미존재 SKU는 예외 페이지 대신 안내 문구 + 목록 링크 (스택 트레이스 노출 금지)
    @Test
    void detail_notFound_showsFriendlyMessageInsteadOfErrorPage() throws Exception {
        when(productService.get("SKU-9999"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 없음: SKU-9999"));

        mockMvc.perform(get("/product/SKU-9999"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/detail"))
                .andExpect(content().string(containsString("상품을 찾을 수 없습니다")))
                .andExpect(content().string(containsString("목록으로")))
                .andExpect(content().string(not(containsString("ResponseStatusException"))))
                .andExpect(content().string(not(containsString("java.lang"))));
    }

    // linked_tc: TC-FUNC-order-010-05
    // 커버: AC7 — /product/list는 상세 라우트({sku})에 가려지지 않는다(리터럴 우선 매칭, 009 QA 권고 7)
    @Test
    void productList_isNotShadowedByDetailRoute() throws Exception {
        when(productService.listSorted(null, "latest")).thenReturn(List.of());

        mockMvc.perform(get("/product/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/list"));

        verify(productService).listSorted(null, "latest");
    }

    // linked_tc: TC-FUNC-order-010-06
    // 커버: r1 QA CONCERNS 권고1 — 404가 아닌 상태(503)는 흡수하지 않고 그대로 전파(장애 은폐 방지)
    @Test
    void detail_serviceThrowsNon404Status_isNotAbsorbed() throws Exception {
        when(productService.get("SKU-1001"))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "DB 점검중"));

        mockMvc.perform(get("/product/SKU-1001"))
                .andExpect(status().isServiceUnavailable());
    }

    // linked_tc: TC-FUNC-order-010-07
    // 커버: r1 QA CONCERNS 권고2 — 렌더 본문에 linked_func 추적 주석이 노출되지 않는다(파서레벨 주석 통일)
    @Test
    void detail_found_doesNotExposeLinkedFuncCommentInRenderedBody() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1001");
        p.setProductName("마우스");
        p.setPrice(25000L);
        p.setStockQty(100);
        p.setSaleYn("Y");
        when(productService.get("SKU-1001")).thenReturn(p);

        mockMvc.perform(get("/product/SKU-1001"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("linked_func"))))
                .andExpect(content().string(not(containsString("docs/00_FUNC/stories"))));
    }

    // linked_tc: TC-FUNC-order-010-08
    // 커버: SR-202 AC1, SR-216 AC3(회귀) — 재고 있는 상품 상세는 담기 폼(회원 셀렉트 + 수량 기본 1 +
    // [담기])을 보여주고, 품절 배지·비활성 사유·disabled 속성이 전혀 렌더되지 않는다(재고 0일 때만
    // 나타나야 하는 SR-216 신규 표기가 재고>0에는 나타나지 않음을 확인)
    @Test
    void detail_inStock_rendersAddToCartFormWithMemberSelect() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1001");
        p.setProductName("마우스");
        p.setPrice(25000L);
        p.setStockQty(100);
        p.setSaleYn("Y");
        when(productService.get("SKU-1001")).thenReturn(p);
        when(memberService.list()).thenReturn(List.of(member("M-0001", "김실증")));

        mockMvc.perform(get("/product/SKU-1001"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("김실증")))
                .andExpect(content().string(containsString("담기")))
                .andExpect(content().string(containsString("name=\"qty\"")))
                .andExpect(content().string(not(containsString("품절 상품은 담을 수 없습니다"))))
                .andExpect(content().string(not(containsString("class=\"badge-soldout\""))))
                .andExpect(content().string(not(containsString("disabled=\"disabled\""))));
    }

    // linked_tc: TC-FUNC-order-010-09
    // 커버: SR-216 AC2 — 품절 상품도 담기 폼 자체는 숨기지 않는다(폼은 계속 렌더). 회원 셀렉트·수량
    // 입력·담기 버튼이 모두 disabled로 표시되고, 버튼 옆에 비활성 사유가 붙는다.
    // (구 TC-FUNC-order-010-09 "폼 미노출" 기대는 SR-216으로 대체 — 폼을 숨기던 종전 구조 폐기)
    @Test
    void detail_soldOut_showsDisabledAddToCartFormWithReason() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1002");
        p.setProductName("키보드");
        p.setPrice(59000L);
        p.setStockQty(0);
        p.setSaleYn("Y");
        when(productService.get("SKU-1002")).thenReturn(p);
        when(memberService.list()).thenReturn(List.of(member("M-0001", "김실증")));

        mockMvc.perform(get("/product/SKU-1002"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("품절 상품은 담을 수 없습니다")))
                .andExpect(content().string(containsString("name=\"qty\"")))
                .andExpect(content().string(containsString("name=\"memberId\"")))
                .andExpect(content().string(containsString("disabled=\"disabled\"")));
    }

    // linked_tc: TC-FUNC-order-010-14
    // 커버: SR-216 AC1 — 재고 0이면 상품명 옆(제목 라인)에 '품절' 배지(badge-soldout)가 뜬다
    @Test
    void detail_soldOut_showsSoldOutBadgeNextToTitle() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1002");
        p.setProductName("키보드");
        p.setPrice(59000L);
        p.setStockQty(0);
        p.setSaleYn("Y");
        when(productService.get("SKU-1002")).thenReturn(p);
        when(memberService.list()).thenReturn(List.of(member("M-0001", "김실증")));

        mockMvc.perform(get("/product/SKU-1002"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"badge-soldout\"")));
    }

    // linked_tc: TC-FUNC-order-010-08
    // 커버: 담기 성공 — PRG 리다이렉트 + 플래시로 확인 문구·회원ID 전달, CartService.addItem 직접 호출(중복 검증 없음)
    // (r7: r6 QA 권고2 — PRG 적용)
    @Test
    void addToCart_success_redirectsWithFlashConfirmation() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1001");
        p.setProductName("마우스");
        p.setPrice(25000L);
        p.setStockQty(100);
        p.setSaleYn("Y");
        when(productService.get("SKU-1001")).thenReturn(p);
        CartItem item = new CartItem();
        item.setMemberId("M-0001");
        item.setSku("SKU-1001");
        item.setQty(2);
        when(cartService.addItem("M-0001", "SKU-1001", 2)).thenReturn(item);

        mockMvc.perform(post("/product/SKU-1001/cart")
                        .param("memberId", "M-0001")
                        .param("qty", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/product/SKU-1001"))
                .andExpect(flash().attribute("addToCartSuccess", true))
                .andExpect(flash().attribute("addedMemberId", "M-0001"));

        verify(cartService).addItem("M-0001", "SKU-1001", 2);
    }

    // linked_tc: TC-FUNC-order-010-09
    // 커버: 품절 상품 담기 거부 — PRG 리다이렉트 + 플래시로 CartService 409 사유 전달(재검증 로직 중복 금지)
    // (r7: r6 QA 권고2 — PRG 적용)
    @Test
    void addToCart_soldOutRejectedByCartService_redirectsWithFlashReason() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1002");
        p.setProductName("키보드");
        p.setPrice(59000L);
        p.setStockQty(0);
        p.setSaleYn("Y");
        when(productService.get("SKU-1002")).thenReturn(p);
        when(cartService.addItem("M-0001", "SKU-1002", 1))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "품절 상품: SKU-1002"));

        mockMvc.perform(post("/product/SKU-1002/cart")
                        .param("memberId", "M-0001")
                        .param("qty", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/product/SKU-1002"))
                .andExpect(flash().attribute("addToCartError", "품절 상품: SKU-1002"));
    }

    // linked_tc: TC-FUNC-order-010-10
    // 커버: 재고 초과 담기 거부 — PRG 리다이렉트 + 플래시로 CartService 409 사유 전달
    @Test
    void addToCart_exceedsStock_redirectsWithFlashReason() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1001");
        p.setProductName("마우스");
        p.setPrice(25000L);
        p.setStockQty(3);
        p.setSaleYn("Y");
        when(productService.get("SKU-1001")).thenReturn(p);
        when(cartService.addItem("M-0001", "SKU-1001", 10))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "재고 초과: 가용 3, 요청 10"));

        mockMvc.perform(post("/product/SKU-1001/cart")
                        .param("memberId", "M-0001")
                        .param("qty", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/product/SKU-1001"))
                .andExpect(flash().attribute("addToCartError", "재고 초과: 가용 3, 요청 10"));
    }

    // linked_tc: TC-FUNC-order-010-11
    // 커버: 수량<1 거부(D4) — PRG 리다이렉트 + 플래시로 CartService 400 사유 전달
    @Test
    void addToCart_qtyLessThanOne_redirectsWithFlashReason() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1001");
        p.setProductName("마우스");
        p.setPrice(25000L);
        p.setStockQty(100);
        p.setSaleYn("Y");
        when(productService.get("SKU-1001")).thenReturn(p);
        when(cartService.addItem("M-0001", "SKU-1001", 0))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "수량은 1 이상이어야 합니다(삭제하려면 삭제 버튼을 사용하세요)"));

        mockMvc.perform(post("/product/SKU-1001/cart")
                        .param("memberId", "M-0001")
                        .param("qty", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/product/SKU-1001"))
                .andExpect(flash().attribute("addToCartError",
                        "수량은 1 이상이어야 합니다(삭제하려면 삭제 버튼을 사용하세요)"));
    }

    // linked_tc: TC-FUNC-order-010-12
    // 커버: r6 QA 권고1 — CartService가 5xx를 던지면 흡수하지 않고 그대로 전파(장애 은폐 방지,
    // loadProduct와 동일 규약을 addToCart에도 적용)
    @Test
    void addToCart_serviceThrowsNon4xxStatus_isNotAbsorbed() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1001");
        p.setProductName("마우스");
        p.setPrice(25000L);
        p.setStockQty(100);
        p.setSaleYn("Y");
        when(productService.get("SKU-1001")).thenReturn(p);
        when(cartService.addItem("M-0001", "SKU-1001", 1))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "DB 점검중"));

        mockMvc.perform(post("/product/SKU-1001/cart")
                        .param("memberId", "M-0001")
                        .param("qty", "1"))
                .andExpect(status().isServiceUnavailable());
    }

    // linked_tc: TC-FUNC-order-010-13
    // 커버: r6 QA 권고3 — 미존재 SKU로 담기 POST 시 addItem 호출 없이 기존 미존재 안내 화면을 조기 반환
    @Test
    void addToCart_productNotFound_showsNotFoundNoticeWithoutCallingCartService() throws Exception {
        when(productService.get("SKU-9999"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 없음: SKU-9999"));

        mockMvc.perform(post("/product/SKU-9999/cart")
                        .param("memberId", "M-0001")
                        .param("qty", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/detail"))
                .andExpect(content().string(containsString("상품을 찾을 수 없습니다")));

        verify(cartService, never()).addItem(anyString(), anyString(), anyInt());
    }
}
