package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.ProductWishView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 찜 등록/해제/목록(SR-356 #2, INF-ORD-040) 전용 매퍼.
 *
 * <p><b>존재 판정은 read-then-write가 아니라 단일 원자문</b>(사례집 SR-231 r2/SR-298 #1·#2와 동일
 * 계열 — 카운터가 아니라 존재 판정이지만 같은 이유로 read-then-write 경쟁을 피한다). {@link
 * #insertWish}는 PK(member_id, sku) 충돌을 DB가 흡수해 이미 찜한 상품 재요청도 예외 없이 0행
 * 영향으로 끝난다({@code CartDao#upsertMergeQty}와 동일 원리).
 */
@Mapper
public interface ProductWishDao {

    /**
     * 등록 — {@code INSERT IGNORE} 단일 원자문. 신규/재등록 구분 없이 항상 성공(예외 없음) —
     * 동시에 두 번 찜 요청이 와도 PK 충돌을 DB가 흡수하므로 500이 나지 않는다.
     */
    int insertWish(@Param("memberId") String memberId, @Param("sku") String sku,
                   @Param("createdAt") LocalDateTime createdAt);

    /** 해제 — 단일 DML(영향행수 0/1 무관). 존재 여부를 먼저 조회하지 않는다. */
    int deleteWish(@Param("memberId") String memberId, @Param("sku") String sku);

    /**
     * 목록 — PRODUCT_WISHES와 PRODUCTS 조인, {@code created_at DESC}, offset/limit. {@code
     * offset}은 {@code long} — {@code page}가 커 {@code int} 곱셈 오버플로가 나지 않도록 호출부가
     * 캐스팅 후 넘긴다(사례집 SR-351 #1 round1→round2).
     */
    List<ProductWishView> selectWishList(@Param("memberId") String memberId,
                                          @Param("offset") long offset, @Param("limit") int limit);

    /**
     * 상품 단건 조회(INF-ORD-009, SR-356 #6) 전용 집계 — 이 sku를 찜한 회원 수. {@code
     * ix_product_wishes_sku}(V14 DDL에서 이 목적으로 이미 예약된 단독 보조 인덱스)로 커버된다.
     * 존재하지 않는 sku도 예외 없이 0을 반환한다(COUNT(*) 특성).
     */
    int countWishesBySku(@Param("sku") String sku);
}
