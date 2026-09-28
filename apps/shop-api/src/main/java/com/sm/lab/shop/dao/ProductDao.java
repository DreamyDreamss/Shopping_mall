// linked_func: FUNC-order-007
// spec: docs/05_설계서/order/INF/INF-ORD-008.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductDao {
    /**
     * keyword가 null/공백이면 기존과 동일(전체 판매중). 있으면 product_name 부분일치 AND sale_yn='Y' (SR-201).
     * inStock=true면 stock_qty >= 1 조건을 추가로 AND(품절 제외). null/false면 기존과 동일(하위호환, SR-220).
     */
    List<Product> selectProducts(@Param("keyword") String keyword, @Param("inStock") Boolean inStock);
    Product selectBySku(@Param("sku") String sku);
    int decreaseStock(@Param("sku") String sku, @Param("qty") int qty);
    // linked_func: FUNC-order-002 — 주문 취소 시 재고 원복 (LAB-102)
    int increaseStock(@Param("sku") String sku, @Param("qty") int qty);

    // linked_func: FUNC-order-009 — 상품 목록 화면 정렬(SR-214). sort는 서비스(ProductService.normalizeSort)가
    // 화이트리스트("priceAsc"/"priceDesc")로 검증을 마친 값만 전달한다("latest"는 기존 selectProducts를 그대로
    // 재사용해 데이터 계약을 불변 유지 — 이 메서드로 오지 않는다).
    List<Product> selectProductsForList(@Param("keyword") String keyword, @Param("sort") String sort);

    // SR-358(#2) — 상품 상세 조회수(INF-ORD-009). 상한 없는 순수 누적 카운터라 조건부 WHERE가
    // 필요 없다(단일 UPDATE). Product 도메인 클래스·selectBySku/selectProducts의 SELECT 목록에는
    // view_count를 넣지 않는다 — 목록 API(INF-ORD-008)로 원치 않게 유출되는 것을 막기 위한
    // 전용 스칼라 메서드 분리(STORY "실패 사례집 대조" 절).
    int incrementViewCount(@Param("sku") String sku);

    // 존재하지 않는 sku는 null을 반환한다(0이 아니다 — 컬럼 자체가 없는 행과 "0인 행"을 구분).
    Long selectViewCount(@Param("sku") String sku);
}
