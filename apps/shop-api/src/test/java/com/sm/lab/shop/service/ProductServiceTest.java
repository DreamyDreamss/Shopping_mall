// linked_func: FUNC-order-007
// spec: docs/05_설계서/order/INF/INF-ORD-008.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.dao.ProductReviewDao;
import com.sm.lab.shop.dao.ProductWishDao;
import com.sm.lab.shop.domain.Product;
import com.sm.lab.shop.domain.ProductReviewSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    // SR-351(#2) — ProductService 생성자가 (ProductDao, ProductReviewDao)로 확장됐다(getDetail()이
    // avgRating/reviewCount 집계에 이 DAO를 쓴다). 이 클래스의 다른 FUNC(정렬·목록·soldOut) 테스트는
    // getDetail을 호출하지 않으므로 이 mock을 스텁하지 않아도 영향 없다(추가 인자일 뿐, 회귀 없음).
    @Mock
    private ProductReviewDao productReviewDao;
    // SR-356(#6) — ProductService 생성자가 (ProductDao, ProductReviewDao, ProductWishDao)로
    // 확장됐다(getDetail()이 wishCount 집계에 이 DAO를 쓴다). getDetail을 호출하지 않는 이 클래스의
    // 다른 FUNC(정렬·목록) 테스트는 이 mock을 스텁하지 않아도 영향 없다(추가 인자일 뿐, 회귀 없음).
    @Mock
    private ProductWishDao productWishDao;

    private static ProductReviewSummary summary(BigDecimal avgRating, int reviewCount) {
        ProductReviewSummary s = new ProductReviewSummary();
        s.setAvgRating(avgRating);
        s.setReviewCount(reviewCount);
        return s;
    }

    @Test
    void list_withoutKeyword_delegatesToDaoWithNullKeyword() {
        // AS-IS 회귀: keyword 미지정 시 기존과 동일하게 dao에 null을 전달한다 (TC-FUNC-order-007-03)
        when(productDao.selectProducts(null, null)).thenReturn(Collections.emptyList());
        ProductService service = new ProductService(productDao, productReviewDao, productWishDao);

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
        ProductService service = new ProductService(productDao, productReviewDao, productWishDao);

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
        ProductService service = new ProductService(productDao, productReviewDao, productWishDao);

        List<Product> result = service.list(null, true);

        verify(productDao).selectProducts(null, true);
        assertThat(result).extracting(Product::getSku).containsExactly("SKU-1003");
    }

    @Test
    void list_withInStockFalse_delegatesToDaoWithInStockFalse() {
        // SR-220: inStock=false는 필터 미적용(기존과 동일) — dao에 그대로 전달
        when(productDao.selectProducts(null, false)).thenReturn(Collections.emptyList());
        ProductService service = new ProductService(productDao, productReviewDao, productWishDao);

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
        ProductService service = new ProductService(productDao, productReviewDao, productWishDao);

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
        ProductService service = new ProductService(productDao, productReviewDao, productWishDao);

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
        ProductService service = new ProductService(productDao, productReviewDao, productWishDao);

        service.listSorted("키보드", "bogus");

        verify(productDao).selectProducts("키보드", null);
        verify(productDao, never()).selectProductsForList(anyString(), anyString());
    }

    // SR-353 — 상품 상세(INF-ORD-009) soldOut 매핑. 재고 있음 → soldOut=false, stockQty는 조회값 그대로.
    // SR-351(#2) — avgRating/reviewCount 단언 추가(레코드 접근자 기준 — 위치 인덱스 비교 금지,
    // STORY "테스트" 절: SR-353이 soldOut 내부 구현을 바꿔도 이 단언은 영향받지 않는다).
    // SR-356(#6) — wishCount 스텁·단언 추가(레코드 접근자 기준, 동일 관례).
    @Test
    void getDetail_inStock_returnsSoldOutFalseAndSameStockQty() {
        Product p = new Product();
        p.setSku("SKU-2001");
        p.setProductName("무선 이어폰");
        p.setPrice(59000L);
        p.setStockQty(7);
        p.setSaleYn("Y");
        when(productDao.selectBySku("SKU-2001")).thenReturn(p);
        when(productReviewDao.selectRatingSummary("SKU-2001")).thenReturn(summary(new BigDecimal("4.7"), 3));
        when(productWishDao.countWishesBySku("SKU-2001")).thenReturn(5);
        ProductService service = new ProductService(productDao, productReviewDao, productWishDao);

        ProductService.ProductDetailResponse result = service.getDetail("SKU-2001");

        assertThat(result.sku()).isEqualTo("SKU-2001");
        assertThat(result.stockQty()).isEqualTo(7);
        assertThat(result.soldOut()).isFalse();
        assertThat(result.avgRating()).isEqualTo(new BigDecimal("4.7"));
        assertThat(result.reviewCount()).isEqualTo(3);
        assertThat(result.wishCount()).isEqualTo(5);
    }

    // SR-353 — 재고 0은 "못 읽는 경우"(null)와 다른 조건이다(STORY "테스트" 절, 혼동 금지) — stockQty는
    // 0 그대로 내려가고 soldOut만 true가 된다.
    // SR-356(#6) — wishCount 스텁·단언 추가.
    @Test
    void getDetail_zeroStock_returnsSoldOutTrue() {
        Product p = new Product();
        p.setSku("SKU-2002");
        p.setProductName("품절 상품");
        p.setPrice(15000L);
        p.setStockQty(0);
        p.setSaleYn("Y");
        when(productDao.selectBySku("SKU-2002")).thenReturn(p);
        when(productReviewDao.selectRatingSummary("SKU-2002")).thenReturn(summary(new BigDecimal("2.0"), 1));
        when(productWishDao.countWishesBySku("SKU-2002")).thenReturn(1);
        ProductService service = new ProductService(productDao, productReviewDao, productWishDao);

        ProductService.ProductDetailResponse result = service.getDetail("SKU-2002");

        assertThat(result.stockQty()).isEqualTo(0);
        assertThat(result.soldOut()).isTrue();
        assertThat(result.wishCount()).isEqualTo(1);
    }

    // SR-351(#2) — 리뷰 0건일 때 avgRating은 0.0이 아니라 null이어야 한다(확정 문답 — "0.0 아님").
    // Map.of NPE 회귀(SR-306 #2 계열 판별력 0 가드 회피)와 동일한 함정을 여기서도 직접 재현한다.
    // SR-356(#6) — wishCount 스텁·단언 추가.
    @Test
    void getDetail_noReviews_returnsAvgRatingNullAndReviewCountZero() {
        Product p = new Product();
        p.setSku("SKU-2003");
        p.setProductName("신상품");
        p.setPrice(9900L);
        p.setStockQty(5);
        p.setSaleYn("Y");
        when(productDao.selectBySku("SKU-2003")).thenReturn(p);
        when(productReviewDao.selectRatingSummary("SKU-2003")).thenReturn(summary(null, 0));
        when(productWishDao.countWishesBySku("SKU-2003")).thenReturn(0);
        ProductService service = new ProductService(productDao, productReviewDao, productWishDao);

        ProductService.ProductDetailResponse result = service.getDetail("SKU-2003");

        assertThat(result.avgRating()).isNull();
        assertThat(result.reviewCount()).isEqualTo(0);
        assertThat(result.wishCount()).isEqualTo(0);
    }

    // SR-356(#6) — 신규 케이스: 찜 0건 상품도 wishCount=0으로 공개 응답에 그대로 포함된다(확정 답변
    // — 비로그인 조회 시 wishCount 노출 제한 없음, avgRating/reviewCount와 동일하게 공개).
    @Test
    void getDetail_noWishes_returnsWishCountZero() {
        Product p = new Product();
        p.setSku("SKU-2004");
        p.setProductName("찜 없는 상품");
        p.setPrice(12000L);
        p.setStockQty(3);
        p.setSaleYn("Y");
        when(productDao.selectBySku("SKU-2004")).thenReturn(p);
        when(productReviewDao.selectRatingSummary("SKU-2004")).thenReturn(summary(new BigDecimal("3.5"), 2));
        when(productWishDao.countWishesBySku("SKU-2004")).thenReturn(0);
        ProductService service = new ProductService(productDao, productReviewDao, productWishDao);

        ProductService.ProductDetailResponse result = service.getDetail("SKU-2004");

        assertThat(result.wishCount()).isEqualTo(0);
    }
}
