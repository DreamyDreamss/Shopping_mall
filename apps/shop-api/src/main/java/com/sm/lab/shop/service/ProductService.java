// linked_func: FUNC-order-007
// spec: docs/05_설계서/order/INF/INF-ORD-008.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.dao.ProductReviewDao;
import com.sm.lab.shop.dao.ProductWishDao;
import com.sm.lab.shop.domain.Product;
import com.sm.lab.shop.domain.ProductReviewSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    // linked_func: FUNC-order-009 — 상품 목록 화면 정렬 셀렉트 옵션 화이트리스트(SR-214).
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-009.md
    private static final String SORT_LATEST = "latest";
    private static final List<String> VALID_SORTS = List.of(SORT_LATEST, "priceAsc", "priceDesc");

    private final ProductDao productDao;
    // SR-351(#2) — INF-ORD-009 응답에 avgRating/reviewCount를 얹기 위한 집계 조회 전용 의존성.
    // ProductReviewDao#selectRatingSummary(sku)를 목록 API(INF-ORD-038)와 공유 호출한다(STORY
    // "데이터" 절 — 집계 쿼리 중복 금지).
    private final ProductReviewDao productReviewDao;
    // SR-356(#6) — INF-ORD-009 응답에 wishCount를 얹기 위한 집계 조회 전용 의존성.
    // ProductWishDao#countWishesBySku(sku)를 호출한다(ix_product_wishes_sku, V14에서 이미 예약).
    private final ProductWishDao productWishDao;

    public ProductService(ProductDao productDao, ProductReviewDao productReviewDao,
                           ProductWishDao productWishDao) {
        this.productDao = productDao;
        this.productReviewDao = productReviewDao;
        this.productWishDao = productWishDao;
    }

    /**
     * sort 값을 화이트리스트로 검증한다. 목록에 없는 값(오탈자·조작된 URL 등)은 조용히 기본값
     * "latest"로 되돌린다(오류 화면 금지 — SR-214 확정요건). 컨트롤러가 화면 모델(선택된 옵션
     * 표시)과 서비스 호출 양쪽에 동일한 정규화 값을 쓰도록 공개 정적 메서드로 둔다.
     * linked_func: FUNC-order-009
     */
    public static String normalizeSort(String sort) {
        return sort != null && VALID_SORTS.contains(sort) ? sort : SORT_LATEST;
    }

    /**
     * 상품 목록 화면 전용(SR-214, FUNC-order-009) — 정렬 옵션 포함 조회. 이름을 {@code list}와
     * 겹치지 않게 한 이유: {@code list(String, Boolean)}(FUNC-order-007, inStock)과 오버로드하면
     * 그 메서드를 호출하는 기존 코드의 리터럴 {@code null} 인자가 두 오버로드 사이에서 컴파일
     * 중의성(ambiguous) 오류를 일으킨다(FUNC-order-007 소유 파일을 건드리지 않기 위해 별도 이름 사용).
     * sort는 {@link #normalizeSort(String)}로 정규화된 값만 받는다는 전제다(컨트롤러가 정규화 후 전달).
     * "latest"는 기존 {@link #list(String)}과 동일하게 selectProducts(ORDER BY sku)를 그대로 재사용해
     * 데이터 계약(회귀 범위: 기존 조회 결과 전부)을 불변 유지한다 — 이 값만 새 쿼리로 보내지 않는다.
     */
    public List<Product> listSorted(String keyword, String sort) {
        String normalized = normalizeSort(sort);
        if (SORT_LATEST.equals(normalized)) {
            return productDao.selectProducts(keyword, null);
        }
        return productDao.selectProductsForList(keyword, normalized);
    }

    /**
     * 하위호환 오버로드 — inStock 미지정 호출부(예: FUNC-order-009 화면 컨트롤러)는 변경 없이 그대로 사용.
     * FUNC-order-007 SR-220 범위 밖이라 그 호출부 파일은 건드리지 않는다.
     */
    public List<Product> list(String keyword) {
        return list(keyword, null);
    }

    /**
     * keyword가 null/미지정이면 기존과 동일한 전체 판매중 목록. 있으면 부분일치 검색(SR-201).
     * inStock=true면 재고 1개 이상인 상품만 반환(품절 제외). null/false면 기존과 동일(하위호환, SR-220).
     */
    public List<Product> list(String keyword, Boolean inStock) {
        return productDao.selectProducts(keyword, inStock);
    }

    public Product get(String sku) {
        Product p = productDao.selectBySku(sku);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 없음: " + sku);
        }
        return p;
    }

    /**
     * 상품 상세(SR-353) — 단건 조회에 {@code soldOut}을 얹어 반환한다. 인증·존재 판정({@link #get})은
     * 그대로 재사용하고 매핑만 얇게 더한다. {@code Product.stockQty}(int) 자체는 바꾸지 않는다 —
     * 목록(FUNC-order-007)·{@link CartService}(stockQty==0 비교, 언박싱 위험)처럼 이 SR과 무관한
     * 전역 소비처가 있어(SR-231 r4 "전역 계약 변경 금지" 사례) 컨트롤러 경계에서만 새 record로 감싼다.
     * "재고 수량을 읽지 못하는 경우"(stockQty를 null로 돌려야 하는 조건)는 현재 스키마·경로 어디에도
     * 트리거가 없어(요구사항 근거 없음, STORY "계약" 절) 이 메서드는 항상 실제 값을 채워 넣는다 —
     * 존재하지 않는 실패를 흉내 내는 분기를 만들지 않는다(SR-297 #2 사례 대조).
     */
    public ProductDetailResponse getDetail(String sku) {
        Product p = get(sku);
        // SR-351(#2) — avgRating/reviewCount는 이 단건 조회 뒤(존재 판정 통과 후)에만 집계한다.
        // distribution(별점 분포 원시값)은 이 응답에 넣지 않는다 — INF-ORD-009 "추가만" 계약
        // 범위 밖(목록 API INF-ORD-038 전용, STORY "계약" 절).
        ProductReviewSummary summary = productReviewDao.selectRatingSummary(sku);
        // SR-356(#6) — wishCount도 존재 판정 통과 후에만 집계한다(avgRating/reviewCount와 동일
        // 순서 — 두 집계 호출은 서로 독립이라 순서 무관, STORY "순서·보안" 절).
        int wishCount = productWishDao.countWishesBySku(sku);
        long viewCount = incrementAndReadViewCount(sku);
        return new ProductDetailResponse(
                p.getSku(), p.getProductName(), p.getPrice(), p.getStockQty(),
                p.getSaleYn(), p.getListPrice(), p.getImageUrl(), p.getStockQty() <= 0,
                /* avgRating */ summary.getAvgRating(), /* reviewCount */ summary.getReviewCount(),
                /* wishCount */ wishCount, /* viewCount */ viewCount
        );
    }

    /**
     * SR-358(#2) — 조회수 +1 시도 후 최신값을 읽어 "이번 조회 포함" 값을 만든다. 증가·조회 각각
     * 독자적인 실패 허용 구간이다(둘 다 실패해도 상세 조회 자체는 200을 유지 — AC "증가 실패는
     * 로그만 남기고 200"). 증가가 성공했으면 조회가 +1된 값을 읽고, 증가만 실패했어도 조회가
     * 성공하면 최소한 최신 저장값을 반환한다. 둘 다 실패하면 0으로 폴백한다(이 폴백값은 AC에
     * 명시되지 않은 판단 — STORY "데이터" 절, QA 확인 대상으로 남는다).
     */
    private long incrementAndReadViewCount(String sku) {
        try {
            productDao.incrementViewCount(sku);
        } catch (RuntimeException e) {
            log.warn("상품 조회수 증가 실패: sku={}", sku, e);
        }
        try {
            Long viewCount = productDao.selectViewCount(sku);
            return viewCount != null ? viewCount : 0L;
        } catch (RuntimeException e) {
            log.warn("상품 조회수 조회 실패: sku={}", sku, e);
            return 0L;
        }
    }

    /**
     * INF-ORD-009 응답 전용(SR-353 + SR-351(#2) + SR-356(#6) + SR-358(#2)). 기존 8필드(이름·타입·
     * 순서 불변, 확정 답변 api_compat) + {@code avgRating}(BigDecimal, nullable — 리뷰 0건이면
     * null, 소수 1자리)·{@code reviewCount}(int, 0 이상, SR-351(#2))·{@code wishCount}(int, 0
     * 이상, 찜하지 않은 상품은 0, SR-356(#6)) 뒤에 {@code viewCount}(long, 이번 조회 포함)를 끝에
     * 추가만 한다(SR-358(#2), 확정 답변 api_compat — 기존 11필드 이름·순서·타입 불변). stockQty는
     * nullable로 열어 두지만(계약상 가능성), 이 메서드가 실제로 null을 채우는 경로는 없다(위
     * {@link #getDetail} 문서 참조).
     */
    public record ProductDetailResponse(
            String sku, String productName, long price, Integer stockQty,
            String saleYn, Long listPrice, String imageUrl, boolean soldOut,
            BigDecimal avgRating, int reviewCount, int wishCount, long viewCount
    ) {}
}
