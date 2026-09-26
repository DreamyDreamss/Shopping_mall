package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.ProductReview;
import com.sm.lab.shop.domain.ProductReviewSummary;
import com.sm.lab.shop.domain.ProductReviewView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품평 작성/삭제(SR-351(#1), INF-ORD-037/INF-ORD-039) 전용 매퍼. 구매 이력 판정 쿼리를
 * {@code ORDERS}+{@code ORDER_ITEMS}와 직접 조인해 이 매퍼 안에서 완결한다 — {@code OrderDao}/
 * {@code order.xml}은 건드리지 않는다(STORY "구현 계획" 절).
 */
@Mapper
public interface ProductReviewDao {

    /** 주문 존재(논리 삭제 제외) — 없으면 404 RVW-4041(STORY "순서·보안" ⑤). */
    int existsOrder(@Param("orderNo") String orderNo);

    /**
     * 구매 자격 단일 판정(STORY "순서·보안" ⑥) — 이 주문이 이 회원 소유 + CANCELED 아님 + 이
     * SKU 라인 포함을 한 SQL로 판정한다. 0이면 403 RVW-4031.
     */
    int countEligiblePurchase(@Param("orderNo") String orderNo, @Param("memberId") String memberId,
                              @Param("sku") String sku);

    /**
     * 작성 — 선조회 없이 INSERT하고, {@code active_key} 생성열 UNIQUE 인덱스 위반을
     * {@code DuplicateKeyException}으로 원자적으로 캐치해 409로 매핑한다(호출부 책임,
     * STORY "데이터" 절 — 사례집 SR-231 r2/r3와 동일 계열 함정을 이 방식으로 회피).
     */
    int insertReview(ProductReview review);

    /**
     * 삭제 — 단일 조건부 UPDATE(선조회 후 UPDATE 금지, 사례집 SR-298 #1·#2). 영향행수 1이면 성공,
     * 0이면 호출부가 {@link #selectById}로 원인(없음/이미 삭제됨/sku 불일치/소유 아님)을 가른다.
     */
    int softDelete(@Param("reviewId") Long reviewId, @Param("sku") String sku,
                   @Param("memberId") String memberId, @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * {@link #softDelete}가 0행일 때만 호출하는 원인 재조회 — {@code del_yn} 상시필터 없이
     * 전체 상태를 그대로 반환한다(삭제 여부·소유자·sku를 서비스가 직접 비교해 403/404를 가른다).
     */
    ProductReview selectById(@Param("reviewId") Long reviewId);

    /**
     * 공개 목록 조회(SR-351(#2), INF-ORD-038) — {@code del_yn='N'} 상시필터 + {@code MEMBERS}
     * 조인(이름은 마스킹 전 원본, 서비스가 마스킹). {@code sort}는 서비스가 화이트리스트
     * (latest/ratingDesc/ratingAsc) 검증을 마친 값만 전달한다. {@code offset}은 {@code long} —
     * {@code page}가 커 {@code int} 곱셈 오버플로가 나지 않도록 호출부가 캐스팅 후 넘긴다.
     */
    List<ProductReviewView> selectReviews(@Param("sku") String sku, @Param("sort") String sort,
                                           @Param("offset") long offset, @Param("limit") int limit);

    /**
     * 평균 별점·리뷰 수·별점 분포(1~5점 각각 건수) 단일 집계(SR-351(#2)) — {@code ProductService
     * .getDetail()}(INF-ORD-009)과 {@link #selectReviews}를 쓰는 목록 API(INF-ORD-038) 양쪽이
     * 이 메서드 하나를 공유 호출한다(STORY "데이터" 절 — 집계 쿼리 중복 금지). {@code del_yn='N'}
     * 상시필터, 리뷰 0건이어도 항상 1행(avgRating=null, reviewCount=0, rating*Count=0)을 반환한다.
     */
    ProductReviewSummary selectRatingSummary(@Param("sku") String sku);
}
