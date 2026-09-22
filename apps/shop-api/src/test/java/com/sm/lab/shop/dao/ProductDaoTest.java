// linked_func: FUNC-order-007
// spec: docs/05_설계서/order/INF/INF-ORD-008.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.PRODUCTS) 대상 통합 테스트.
 * 랩 데이터: SKU-1001 스탠딩 데스크(Y), SKU-1002 기계식 키보드(Y),
 *            SKU-1003 4K 모니터(Y), SKU-1004 단종 마우스(N).
 */
@SpringBootTest
class ProductDaoTest {

    @Autowired
    private ProductDao productDao;

    @Test
    void selectProducts_withoutKeyword_returnsAllOnSaleSortedBySku() {
        // AS-IS 회귀: keyword 미지정 시 판매중 전체를 sku 오름차순으로 반환 (TC-FUNC-order-007-03)
        List<Product> result = productDao.selectProducts(null, null);

        assertThat(result).extracting(Product::getSku)
                .containsExactly("SKU-1001", "SKU-1002", "SKU-1003");
        assertThat(result).allMatch(p -> "Y".equals(p.getSaleYn()));
    }

    // SR-306(#2) — 목록 조회 각 행의 listPrice/imageUrl이 V10 시드값과 일치하는지 sku별 대조.
    @Test
    void selectProducts_withoutKeyword_returnsSeededListPriceAndImageUrlPerSku() {
        List<Product> result = productDao.selectProducts(null, null);

        Product sku1001 = result.stream().filter(p -> "SKU-1001".equals(p.getSku())).findFirst().orElseThrow();
        assertThat(sku1001.getListPrice()).isEqualTo(450000L);
        assertThat(sku1001.getImageUrl()).isEqualTo("/images/products/sku-1001.svg");

        Product sku1002 = result.stream().filter(p -> "SKU-1002".equals(p.getSku())).findFirst().orElseThrow();
        assertThat(sku1002.getListPrice()).isNull();
        assertThat(sku1002.getImageUrl()).isEqualTo("/images/products/sku-1002.svg");

        Product sku1003 = result.stream().filter(p -> "SKU-1003".equals(p.getSku())).findFirst().orElseThrow();
        assertThat(sku1003.getListPrice()).isEqualTo(450000L);
        assertThat(sku1003.getImageUrl()).isEqualTo("/images/products/sku-1003.svg");
    }

    // SR-306(#2) — 단건 조회는 sale_yn 필터가 없어 판매종료 상품(SKU-1004)도 반환한다(기존 회귀와
    // 동일 전제). listPrice는 있고 imageUrl은 NULL(이미지 없음 상태를 실데이터로 보존).
    @Test
    void selectBySku_offSaleProduct_returnsListPriceButNullImageUrl() {
        Product result = productDao.selectBySku("SKU-1004");

        assertThat(result).isNotNull();
        assertThat(result.getListPrice()).isEqualTo(42000L);
        assertThat(result.getImageUrl()).isNull();
    }

    // SR-306(#2) — 정가 없는 상품(SKU-1002)은 listPrice가 null.
    @Test
    void selectBySku_withNullListPrice_returnsNull() {
        Product result = productDao.selectBySku("SKU-1002");

        assertThat(result).isNotNull();
        assertThat(result.getListPrice()).isNull();
        assertThat(result.getImageUrl()).isEqualTo("/images/products/sku-1002.svg");
    }

    // SR-306(#2) — 할인 0% 상태(정가==판매가)도 값 그대로 반환(할인율 계산은 화면 책임, 여기선
    // 저장값 대조만).
    @Test
    void selectBySku_withListPriceEqualToPrice_returnsBothValues() {
        Product result = productDao.selectBySku("SKU-1003");

        assertThat(result).isNotNull();
        assertThat(result.getListPrice()).isEqualTo(450000L);
        assertThat(result.getPrice()).isEqualTo(450000L);
        assertThat(result.getImageUrl()).isEqualTo("/images/products/sku-1003.svg");
    }

    @Test
    void selectProducts_withKeyword_returnsPartialMatchOnly() {
        // SR-201 TO-BE: product_name 부분일치(LIKE) 검색 (TC-FUNC-order-007-01)
        List<Product> result = productDao.selectProducts("키보드", null);

        assertThat(result).extracting(Product::getSku).containsExactly("SKU-1002");
    }

    @Test
    void selectProducts_withKeywordNoMatch_returnsEmpty() {
        // TC-FUNC-order-007-02: 미매칭은 오류가 아니라 빈 배열
        List<Product> result = productDao.selectProducts("존재하지않는이름XYZ", null);

        assertThat(result).isEmpty();
    }

    @Test
    void selectProducts_withKeywordMatchingOffSaleProduct_returnsEmpty() {
        // sale_yn='Y' 상시필터는 keyword 검색과 AND — 품절/미판매(SKU-1004, sale_yn=N)는 매칭돼도 제외
        List<Product> result = productDao.selectProducts("마우스", null);

        assertThat(result).isEmpty();
    }

    // linked_tc: TC-FUNC-order-007-06
    @Test
    void selectProducts_withInStockTrue_returnsOnlyPositiveStock() {
        // SR-220 TO-BE: inStock=true는 stock_qty>=1만 반환. 랩 고정 데이터(SKU-1001~1003)는 전부 재고
        // 보유(stock_qty>0)라 결과 집합은 무필터와 동일하지만, 모든 행이 stock_qty>=1임을 단언해 회귀를 지킨다.
        List<Product> result = productDao.selectProducts(null, true);

        assertThat(result).extracting(Product::getSku)
                .containsExactly("SKU-1001", "SKU-1002", "SKU-1003");
        assertThat(result).allMatch(p -> p.getStockQty() >= 1);
    }

    // linked_tc: TC-FUNC-order-007-07
    @Test
    void selectProducts_withInStockFalseOrNull_returnsSameAsNoFilter() {
        // SR-220: inStock=false/null은 기존과 동일(하위호환) — 필터 미적용
        List<Product> withFalse = productDao.selectProducts(null, false);
        List<Product> withNull = productDao.selectProducts(null, null);

        assertThat(withFalse).extracting(Product::getSku)
                .containsExactlyElementsOf(withNull.stream().map(Product::getSku).toList());
    }

    // linked_func: FUNC-order-009 — 상품 목록 화면 정렬(SR-214)
    // linked_tc: TC-FUNC-order-009-15
    // 랩 판매중 가격: SKU-1002 129,000 < SKU-1001 390,000 < SKU-1003 450,000
    @Test
    void selectProductsForList_withSortPriceAsc_returnsAscendingByPrice() {
        List<Product> result = productDao.selectProductsForList(null, "priceAsc");

        assertThat(result).extracting(Product::getSku)
                .containsExactly("SKU-1002", "SKU-1001", "SKU-1003");
        assertThat(result).allMatch(p -> "Y".equals(p.getSaleYn()));
    }

    // linked_func: FUNC-order-009
    // linked_tc: TC-FUNC-order-009-16
    @Test
    void selectProductsForList_withSortPriceDesc_returnsDescendingByPrice() {
        List<Product> result = productDao.selectProductsForList(null, "priceDesc");

        assertThat(result).extracting(Product::getSku)
                .containsExactly("SKU-1003", "SKU-1001", "SKU-1002");
    }

    // linked_func: FUNC-order-009
    // linked_tc: TC-FUNC-order-009-17
    // 커버: keyword + priceAsc 결합 — sale_yn 상시필터·부분일치는 유지한 채 정렬만 가격 오름차순
    @Test
    void selectProductsForList_withKeywordAndSortPriceAsc_appliesBothFilters() {
        List<Product> result = productDao.selectProductsForList("모니터", "priceAsc");

        assertThat(result).extracting(Product::getSku).containsExactly("SKU-1003");
    }

    // linked_tc: TC-FUNC-order-007-08
    @Transactional
    @Sql("classpath:sql/insert-out-of-stock.sql")
    @Test
    void selectProducts_withInStockTrue_excludesOutOfStockButIncludesSaleYnTrue() {
        // AC3 회귀: inStock=true는 sale_yn='Y'인 품절(stock_qty=0) 상품도 제외해야 함.
        // 랩 고정 데이터(SKU-1001~1003)는 전부 재고 보유라, @Sql로 SKU-TEST-OOS(sale_yn='Y', stock_qty=0)를
        // 임시 삽입한 후 inStock=true 호출 결과에서 제외되는지 검증한다.
        // @Transactional이 자동으로 롤백하므로 test data cleanup 불필요.

        List<Product> result = productDao.selectProducts(null, true);

        // 판매중 상품 3개는 모두 포함 (SKU-1001, SKU-1002, SKU-1003)
        assertThat(result).extracting(Product::getSku)
                .containsExactly("SKU-1001", "SKU-1002", "SKU-1003")
                // SKU-TEST-OOS(stock_qty=0)는 제외돼야 함
                .doesNotContain("SKU-TEST-OOS");

        // 모든 행이 stock_qty >= 1임을 재확인
        assertThat(result).allMatch(p -> p.getStockQty() >= 1);
    }

    // linked_tc: TC-FUNC-order-007-09
    @Transactional
    @Sql("classpath:sql/insert-out-of-stock.sql")
    @Test
    void selectProducts_withInStockNullOrFalse_includesOutOfStockProduct() {
        // AC3 회귀: inStock=null/false는 품절 상품도 포함해야 함(기존 하위호환).
        // SKU-TEST-OOS(sale_yn='Y', stock_qty=0)가 포함되는지 검증한다.

        // inStock=null 호출
        List<Product> resultNull = productDao.selectProducts(null, null);
        assertThat(resultNull).extracting(Product::getSku)
                .contains("SKU-TEST-OOS", "SKU-1001", "SKU-1002", "SKU-1003");

        // inStock=false 호출 (기존과 동일 = 필터 미적용)
        List<Product> resultFalse = productDao.selectProducts(null, false);
        assertThat(resultFalse).extracting(Product::getSku)
                .containsExactlyElementsOf(resultNull.stream().map(Product::getSku).toList());

        // 둘 다 SKU-TEST-OOS를 포함
        assertThat(resultNull.stream().map(Product::getSku).anyMatch(sku -> sku.equals("SKU-TEST-OOS"))).isTrue();
        assertThat(resultFalse.stream().map(Product::getSku).anyMatch(sku -> sku.equals("SKU-TEST-OOS"))).isTrue();
    }
}
