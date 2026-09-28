package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.ProductViewHistoryView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 최근 본 상품 기록/조회 전용 매퍼.
 *
 * <p>기록은 read-then-write가 아니라 단일 원자 UPSERT다(카운터·존재판정을 읽고 비교한 뒤 쓰지
 * 않는다는 이 코드베이스 관례와 동일 계열). {@link #recordView}는 같은 회원·sku 조합이 이미
 * 있으면 {@code viewed_at}만 갱신하고, 없으면 신규 행을 삽입한다({@code CartDao#upsertMergeQty}와
 * 동일 원리) — {@code ProductWishDao#insertWish}의 {@code INSERT IGNORE}와 달리 갱신이 필요해
 * {@code ON DUPLICATE KEY UPDATE}를 쓴다.
 */
@Mapper
public interface ProductViewHistoryDao {

    /**
     * 기록 — 단일 원자 UPSERT. 이미 있으면 {@code viewed_at}만 갱신(맨 앞으로), 없으면 신규 삽입.
     */
    int recordView(@Param("memberId") String memberId, @Param("sku") String sku,
                    @Param("viewedAt") LocalDateTime viewedAt);

    /**
     * 회원별 보관 상한 초과분(viewed_at 오래된 순, 동률은 sku ASC)을 삭제한다({@code
     * MemberRefreshTokenDao#enforceActiveCap}과 동일 모양 — 삭제 대상 테이블을 서브쿼리 FROM에
     * 직접 쓸 수 없는 MariaDB 제약(Error 1093)을 파생 테이블로 회피).
     */
    int enforceRecentCap(@Param("memberId") String memberId, @Param("limit") int limit);

    /**
     * 목록 — 최근 본 순({@code viewed_at DESC}, 동률은 {@code sku ASC}) 최대 {@code limit}개,
     * PRODUCTS 조인.
     */
    List<ProductViewHistoryView> selectRecentlyViewed(@Param("memberId") String memberId,
                                                        @Param("limit") int limit);
}
