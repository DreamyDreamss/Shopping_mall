// linked_func: FUNC-order-007
// spec: docs/05_설계서/order/INF/INF-ORD-008.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.domain.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductDao productDao;

    @Test
    void list_withoutKeyword_delegatesToDaoWithNullKeyword() {
        // AS-IS 회귀: keyword 미지정 시 기존과 동일하게 dao에 null을 전달한다 (TC-FUNC-order-007-03)
        when(productDao.selectProducts(null, null)).thenReturn(Collections.emptyList());
        ProductService service = new ProductService(productDao);

        List<Product> result = service.list(null, null);

        verify(productDao).selectProducts(null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void list_withKeyword_delegatesToDaoWithKeyword() {
        // SR-201 TO-BE: keyword가 있으면 그대로 dao에 전달해 부분일치 검색을 수행한다 (TC-FUNC-order-007-01)
        Product p = new Product();
        p.setSku("SKU-1002");
        p.setProductName("기계식 키보드");
        when(productDao.selectProducts("키보드", null)).thenReturn(List.of(p));
        ProductService service = new ProductService(productDao);

        List<Product> result = service.list("키보드", null);

        verify(productDao).selectProducts("키보드", null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("SKU-1002");
    }

    @Test
    void list_withInStockTrue_delegatesToDaoWithInStockTrue() {
        // SR-220 TO-BE: inStock=true가 있으면 그대로 dao에 전달해 품절 상품을 제외한다
        Product p = new Product();
        p.setSku("SKU-1003");
        p.setProductName("4K 모니터");
        p.setStockQty(7);
        when(productDao.selectProducts(null, true)).thenReturn(List.of(p));
        ProductService service = new ProductService(productDao);

        List<Product> result = service.list(null, true);

        verify(productDao).selectProducts(null, true);
        assertThat(result).extracting(Product::getSku).containsExactly("SKU-1003");
    }

    @Test
    void list_withInStockFalse_delegatesToDaoWithInStockFalse() {
        // SR-220: inStock=false는 필터 미적용(기존과 동일) — dao에 그대로 전달
        when(productDao.selectProducts(null, false)).thenReturn(Collections.emptyList());
        ProductService service = new ProductService(productDao);

        List<Product> result = service.list(null, false);

        verify(productDao).selectProducts(null, false);
        assertThat(result).isEmpty();
    }

    // linked_func: FUNC-order-009 — 상품 목록 화면 정렬(SR-214)
    // linked_tc: TC-FUNC-order-009-10
    @Test
    void normalizeSort_validValues_returnedAsIs() {
        assertThat(ProductService.normalizeSort("latest")).isEqualTo("latest");
        assertThat(ProductService.normalizeSort("priceAsc")).isEqualTo("priceAsc");
        assertThat(ProductService.normalizeSort("priceDesc")).isEqualTo("priceDesc");
    }

    // linked_func: FUNC-order-009
    // linked_tc: TC-FUNC-order-009-11
    // 커버: SR-214 확정요건 — 화이트리스트 밖 값(null·오탈자·조작값)은 조용히 "latest"로 되돌린다
    @Test
    void normalizeSort_invalidOrNullValues_fallBackToLatest() {
        assertThat(ProductService.normalizeSort(null)).isEqualTo("latest");
        assertThat(ProductService.normalizeSort("")).isEqualTo("latest");
        assertThat(ProductService.normalizeSort("bogus")).isEqualTo("latest");
        assertThat(ProductService.normalizeSort("PRICEASC")).isEqualTo("latest");
    }

    // linked_func: FUNC-order-009
    // linked_tc: TC-FUNC-order-009-12
    // 커버: sort="latest"는 기존 selectProducts(ORDER BY sku)를 그대로 재사용한다(회귀 범위:
    // 기존 조회 결과 전부 데이터 계약 불변) — 새 정렬 쿼리로 보내지 않는다
    @Test
    void list_withSortLatest_delegatesToExistingSelectProductsForRegressionSafety() {
        Product p = new Product();
        p.setSku("SKU-1001");
        when(productDao.selectProducts(null, null)).thenReturn(List.of(p));
        ProductService service = new ProductService(productDao);

        List<Product> result = service.listSorted(null, "latest");

        verify(productDao).selectProducts(null, null);
        verify(productDao, never()).selectProductsForList(anyString(), anyString());
        assertThat(result).extracting(Product::getSku).containsExactly("SKU-1001");
    }

    // linked_func: FUNC-order-009
    // linked_tc: TC-FUNC-order-009-13
    @Test
    void list_withSortPriceAsc_delegatesToSelectProductsForList() {
        Product p = new Product();
        p.setSku("SKU-1002");
        when(productDao.selectProductsForList(null, "priceAsc")).thenReturn(List.of(p));
        ProductService service = new ProductService(productDao);

        List<Product> result = service.listSorted(null, "priceAsc");

        verify(productDao).selectProductsForList(null, "priceAsc");
        assertThat(result).extracting(Product::getSku).containsExactly("SKU-1002");
    }

    // linked_func: FUNC-order-009
    // linked_tc: TC-FUNC-order-009-14
    // 커버: 화이트리스트 밖 값은 selectProductsForList가 아니라 기존 latest 경로로 위임된다
    @Test
    void list_withInvalidSort_fallsBackToLatestDaoCall() {
        when(productDao.selectProducts("키보드", null)).thenReturn(Collections.emptyList());
        ProductService service = new ProductService(productDao);

        service.listSorted("키보드", "bogus");

        verify(productDao).selectProducts("키보드", null);
        verify(productDao, never()).selectProductsForList(anyString(), anyString());
    }
}
