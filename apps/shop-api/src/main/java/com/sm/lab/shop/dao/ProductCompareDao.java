package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.ProductCompareView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 비교함 담기/빼기/목록(SR-360 #2, INF-ORD-045/046/047) 전용 매퍼.
 *
 * <p><b>상한(4개)까지 있는 존재 판정은 단일 조건부 INSERT로</b>(STORY AC 명시, 사례집 SR-231
 * r2/SR-356 #2와 동일 계열 — read-then-write 금지). {@link #insertCompareIfUnderLimit}는 카운트
 * 서브쿼리(호출 sku 자신 제외)와 결합한 {@code INSERT IGNORE} 단일 원자문이라, "이미 담긴 상품
 * 재담기"와 "5번째 초과" 둘 다 0행으로 관측된다 — 호출부가 {@link #existsCompare}로 원인을 가른다.
 */
@Mapper
public interface ProductCompareDao {

    /**
     * 담기 — 카운트 서브쿼리(호출 sku 제외) < 4일 때만 {@code INSERT IGNORE}. 0행이면 "이미 담긴
     * 상품"(재조회 시 존재) 또는 "상한 초과"(재조회 시 부재) 둘 중 하나 — 호출부가 {@link
     * #existsCompare}로 가른다.
     */
    int insertCompareIfUnderLimit(@Param("memberId") String memberId, @Param("sku") String sku,
                                   @Param("createdAt") LocalDateTime createdAt);

    /**
     * {@link #insertCompareIfUnderLimit}이 0행일 때만 호출하는 원인 재조회 — PK 그대로 일치 검사.
     *
     * <p>round2(재작업 지시 1, QA CONCERNS medium) — {@code FOR UPDATE} 잠금읽기. 비잠금
     * 일관읽기였던 이전 버전은 {@link #insertCompareIfUnderLimit}의 카운트 서브쿼리(잠금읽기=항상
     * 최신)와 읽기 시점이 어긋나, 같은 sku 동시 담기(더블클릭)의 뒤 요청이 방금 커밋된 앞 요청의
     * 행을 옛 REPEATABLE READ 스냅샷에서 못 보고 409로 잘못 떨어졌다(QA 실측). 같은 트랜잭션 안의
     * 잠금읽기로 통일해 두 읽기의 시점을 맞춘다.
     */
    boolean existsCompare(@Param("memberId") String memberId, @Param("sku") String sku);

    /** 빼기 — 단일 DML(영향행수 0/1 무관). 존재 여부를 먼저 조회하지 않는다. */
    int deleteCompare(@Param("memberId") String memberId, @Param("sku") String sku);

    /**
     * 목록 — PRODUCT_COMPARES와 PRODUCTS 조인, {@code created_at ASC, sku ASC}(AC 명시 — 담은
     * 시각 오름차순, 동률은 sku). 최대 4건이라 페이지네이션 없음.
     */
    List<ProductCompareView> selectCompareList(@Param("memberId") String memberId);
}
