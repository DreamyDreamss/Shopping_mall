// linked_func: FUNC-order-007
// spec: docs/05_설계서/order/INF/INF-ORD-008.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Product;
import com.sm.lab.shop.service.ProductService;
import com.sm.lab.shop.support.AdminApiKeyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SR-204 R-5: 인증 필터 도입 후 admin 키 기본 주입(AdminApiKeyTestConfig).
@WebMvcTest(ProductController.class)
@Import(AdminApiKeyTestConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    // linked_tc: TC-FUNC-order-007-04
    @Test
    void list_withoutKeyword_callsServiceWithNull() throws Exception {
        // AS-IS 회귀: keyword 미지정 시 기존과 동일 호출 (TC-FUNC-order-007-03, 하위호환)
        // TC-FUNC-order-007-04: 응답 필드 집합 불변 단언 (sku·productName·price·stockQty·saleYn)
        Product p1 = new Product();
        p1.setSku("SKU-1001");
        p1.setProductName("마우스");
        p1.setPrice(25000L);
        p1.setStockQty(100);
        p1.setSaleYn("Y");

        when(productService.list(null, null)).thenReturn(List.of(p1));

        // 실측: 실제 라우트는 트레일링 슬래시 없음(AS-IS) — 문서상 "GET /api/products/" 표기는 관례 표기일 뿐
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                // 필드 집합 불변 검증
                .andExpect(jsonPath("$[0].sku").value("SKU-1001"))
                .andExpect(jsonPath("$[0].productName").value("마우스"))
                .andExpect(jsonPath("$[0].price").value(25000))
                .andExpect(jsonPath("$[0].stockQty").value(100))
                .andExpect(jsonPath("$[0].saleYn").value("Y"));

        verify(productService).list(null, null);
    }

    // linked_tc: TC-FUNC-order-007-01
    @Test
    void list_withKeyword_callsServiceWithKeywordAndReturnsBody() throws Exception {
        // SR-201 TO-BE: keyword 쿼리 파라미터를 그대로 서비스에 전달
        Product p = new Product();
        p.setSku("SKU-1002");
        p.setProductName("기계식 키보드");
        when(productService.list("키보드", null)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/products").param("keyword", "키보드"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-1002"));

        verify(productService).list("키보드", null);
    }

    // linked_tc: TC-FUNC-order-007-02
    @Test
    void list_withKeywordNoMatch_returns200WithEmptyArray() throws Exception {
        // 미매칭은 200 + 빈 배열 (오류 아님)
        when(productService.list("존재하지않는이름XYZ", null)).thenReturn(List.of());

        mockMvc.perform(get("/api/products").param("keyword", "존재하지않는이름XYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // linked_tc: TC-FUNC-order-007-06
    @Test
    void list_withInStockTrue_callsServiceWithInStockTrue() throws Exception {
        // SR-220 TO-BE: inStock=true 쿼리 파라미터를 그대로 서비스에 전달(품절 제외)
        Product p = new Product();
        p.setSku("SKU-1003");
        p.setProductName("4K 모니터");
        p.setStockQty(7);
        when(productService.list(null, true)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/products").param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-1003"));

        verify(productService).list(null, true);
    }

    // linked_tc: TC-FUNC-order-007-07
    @Test
    void list_withInStockFalse_callsServiceWithInStockFalse() throws Exception {
        // SR-220: inStock=false를 명시해도 서비스에 그대로 전달(필터 미적용은 서비스/DAO 책임)
        when(productService.list(null, false)).thenReturn(List.of());

        mockMvc.perform(get("/api/products").param("inStock", "false"))
                .andExpect(status().isOk());

        verify(productService).list(null, false);
    }

    // linked_tc: TC-FUNC-order-007-05
    // SR-353 — 컨트롤러가 이제 getDetail을 호출한다(get은 더 이상 호출되지 않음). soldOut/stockQty 단언 추가.
    // SR-351(#2) — avgRating/reviewCount 필드 단언 추가(jsonPath 필드명 기준, 위치 인덱스 비교 금지).
    // SR-356(#6) — wishCount 필드 단언 추가(동일 관례).
    // SR-358(#2) — AC1: viewCount 필드 단언 추가(43처럼 다른 값과 구분되는 값으로 신규 단언과 짝을 맺음).
    @Test
    void get_existingSku_returns200WithProduct() throws Exception {
        // 단건 조회 정상 케이스
        ProductService.ProductDetailResponse resp = new ProductService.ProductDetailResponse(
                "SKU-1001", "마우스", 25000L, 100, "Y", null, null, false,
                /* avgRating */ new BigDecimal("4.5"), /* reviewCount */ 3, /* wishCount */ 2,
                /* viewCount */ 43L);
        when(productService.getDetail("SKU-1001")).thenReturn(resp);

        mockMvc.perform(get("/api/products/SKU-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-1001"))
                .andExpect(jsonPath("$.productName").value("마우스"))
                .andExpect(jsonPath("$.stockQty").value(100))
                .andExpect(jsonPath("$.soldOut").value(false))
                .andExpect(jsonPath("$.avgRating").value(4.5))
                .andExpect(jsonPath("$.reviewCount").value(3))
                .andExpect(jsonPath("$.wishCount").value(2))
                .andExpect(jsonPath("$.viewCount").value(43));
    }

    // SR-353 — 품절 상품 단건 조회는 soldOut=true를 그대로 직렬화한다.
    @Test
    void get_soldOutProduct_returnsSoldOutTrueInBody() throws Exception {
        ProductService.ProductDetailResponse resp = new ProductService.ProductDetailResponse(
                "SKU-2002", "품절 상품", 15000L, 0, "Y", null, null, true,
                /* avgRating */ null, /* reviewCount */ 0, /* wishCount */ 0, /* viewCount */ 0L);
        when(productService.getDetail("SKU-2002")).thenReturn(resp);

        mockMvc.perform(get("/api/products/SKU-2002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soldOut").value(true))
                .andExpect(jsonPath("$.stockQty").value(0));
    }

    // SR-353 — stockQty가 null인 계약(서비스가 "읽지 못하는 경우"를 재현했다고 가정)도 컨트롤러가 그대로
    // 직렬화하는지만 확인한다 — 이 null을 실제로 만드는 서비스 조건은 재현하지 않는다(SR-297 #2 사례 대조,
    // STORY "테스트" 절).
    // SR-351(#2) — 리뷰 0건(avgRating null)도 이 계약대로 nullValue()로 단언한다(doesNotExist() 금지,
    // 사례집 SR-306 #2 판별력 0 가드).
    @Test
    void get_nullStockQty_returnsNullField() throws Exception {
        ProductService.ProductDetailResponse resp = new ProductService.ProductDetailResponse(
                "SKU-2003", "재고미상품", 20000L, null, "Y", null, null, false,
                /* avgRating */ null, /* reviewCount */ 0, /* wishCount */ 0, /* viewCount */ 0L);
        when(productService.getDetail("SKU-2003")).thenReturn(resp);

        mockMvc.perform(get("/api/products/SKU-2003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQty").value(nullValue()))
                .andExpect(jsonPath("$.avgRating").value(nullValue()))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.wishCount").value(0));
    }

    // SR-353 재작업(round 2, QA CONCERNS #2) — AC "오류 응답 코드·사유(401/404) 변경 없음"을 컨트롤러가
    // get() → getDetail() 호출로 바꾼 뒤에도 404 경로가 그대로 전파되는지 고정한다(이전 라운드까지
    // isNotFound() 단언이 하나도 없었다).
    @Test
    void get_missingSku_returns404() throws Exception {
        when(productService.getDetail("SKU-9999"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 없음"));

        mockMvc.perform(get("/api/products/SKU-9999"))
                .andExpect(status().isNotFound());
    }

    // SR-306(#2) — 목록 응답에 listPrice/imageUrl 필드가 추가되고 기존 필드는 불변인지 확인.
    @Test
    void list_returnsListPriceAndImageUrl_alongsideExistingFields() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1001");
        p.setProductName("스탠딩 데스크");
        p.setPrice(390000L);
        p.setStockQty(10);
        p.setSaleYn("Y");
        p.setListPrice(450000L);
        p.setImageUrl("/images/products/sku-1001.svg");
        when(productService.list(null, null)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-1001"))
                .andExpect(jsonPath("$[0].productName").value("스탠딩 데스크"))
                .andExpect(jsonPath("$[0].price").value(390000))
                .andExpect(jsonPath("$[0].stockQty").value(10))
                .andExpect(jsonPath("$[0].saleYn").value("Y"))
                .andExpect(jsonPath("$[0].listPrice").value(450000))
                .andExpect(jsonPath("$[0].imageUrl").value("/images/products/sku-1001.svg"));
    }

    // SR-306(#2) — 정가가 없는 상품(SKU-1002 상태)은 listPrice가 null로 내려가야 한다.
    @Test
    void list_withNullListPrice_returnsNullField() throws Exception {
        Product p = new Product();
        p.setSku("SKU-1002");
        p.setProductName("기계식 키보드");
        p.setListPrice(null);
        p.setImageUrl("/images/products/sku-1002.svg");
        when(productService.list(null, null)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                // doesNotExist()는 "필드 없음"과 "필드 있고 값 null" 둘 다 통과시켜 계약을 고정하지
                // 못한다(spring-test JsonPathExpectationsHelper.doesNotExist() == assertTrue(value==null)).
                // nullValue()는 경로가 아예 없으면 실패하고, 값이 null일 때만 통과한다(SR-306.2 재작업).
                .andExpect(jsonPath("$[0].listPrice").value(nullValue()));
    }

    // SR-306(#2) — 단건 조회 응답에도 listPrice/imageUrl이 추가된다(판매종료 상품, SKU-1004 상태
    // — sale_yn 필터가 없는 이 엔드포인트는 이미지가 없는 상품도 그대로 반환한다).
    // SR-353 — getDetail 스텁으로 교체(컨트롤러가 더 이상 get을 호출하지 않음).
    @Test
    void get_returnsListPriceAndImageUrl_withImageUrlNullWhenMissing() throws Exception {
        ProductService.ProductDetailResponse resp = new ProductService.ProductDetailResponse(
                "SKU-1004", "단종 마우스", 35000L, 0, "N", 42000L, null, true,
                /* avgRating */ null, /* reviewCount */ 0, /* wishCount */ 0, /* viewCount */ 0L);
        when(productService.getDetail("SKU-1004")).thenReturn(resp);

        mockMvc.perform(get("/api/products/SKU-1004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-1004"))
                .andExpect(jsonPath("$.listPrice").value(42000))
                // doesNotExist() 대신 nullValue() — 위 list_withNullListPrice_returnsNullField와 동일 근거.
                .andExpect(jsonPath("$.imageUrl").value(nullValue()));
    }

    // SR-353 회귀(AC-7) — GET /api/products/{sku}은 목록 조회(GET /api/products)와 달리 sale_yn 필터가
    // 없다 — 판매종료 상품(sale_yn='N')도 SKU를 알면 조회 가능해야 한다. 이는 사용자가 상품 외부 링크나
    // 이전 주문 히스토리에서 판매종료 상품을 다시 보고 싶어 하는 시나리오를 고려한 것이다(회귀 범위).
    @Test
    void get_discontinuedProduct_returns200WithSaleYnN() throws Exception {
        ProductService.ProductDetailResponse resp = new ProductService.ProductDetailResponse(
                "SKU-9001", "단종된 마우스", 28000L, 0, "N", null, null, true,
                /* avgRating */ null, /* reviewCount */ 0, /* wishCount */ 0, /* viewCount */ 0L);
        when(productService.getDetail("SKU-9001")).thenReturn(resp);

        mockMvc.perform(get("/api/products/SKU-9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-9001"))
                .andExpect(jsonPath("$.saleYn").value("N"))
                .andExpect(jsonPath("$.soldOut").value(true));
    }

}
