// linked_func: FUNC-order-007
// spec: docs/05_설계서/order/INF/INF-ORD-008.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.domain.Product;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {
    // linked_func: FUNC-order-009 — 상품 목록 화면 정렬 셀렉트 옵션 화이트리스트(SR-214).
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-009.md
    private static final String SORT_LATEST = "latest";
    private static final List<String> VALID_SORTS = List.of(SORT_LATEST, "priceAsc", "priceDesc");

    private final ProductDao productDao;

    public ProductService(ProductDao productDao) {
        this.productDao = productDao;
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
}
