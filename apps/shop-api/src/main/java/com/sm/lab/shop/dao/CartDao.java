// linked_func: FUNC-order-011, FUNC-order-012
// spec: docs/00_FUNC/stories/STORY-FUNC-order-011.md, docs/00_FUNC/stories/STORY-FUNC-order-012.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartDao {
    /** 품목 단건(상품명·단가 조인). 없으면 null. */
    CartItem selectItem(@Param("memberId") String memberId, @Param("sku") String sku);

    /** 회원의 장바구니 품목 전체(상품명·단가 조인, 담은 순). */
    List<CartItem> selectItems(@Param("memberId") String memberId);

    int insertItem(@Param("memberId") String memberId, @Param("sku") String sku, @Param("qty") int qty);

    /**
     * 담기 합산 원자 UPSERT(round2) — 없으면 삽입, 있으면 qty를 더한다.
     * PK(member_id,sku) 충돌을 DB가 원자적으로 흡수하므로 동시 담기에서도 lost update·500이 없다.
     * 재고 판정은 이 호출 이후 최종(합산) qty 기준으로 수행하고, 초과 시 CartService가 되돌린다.
     */
    int upsertMergeQty(@Param("memberId") String memberId, @Param("sku") String sku, @Param("qty") int qty);

    int updateQty(@Param("memberId") String memberId, @Param("sku") String sku, @Param("qty") int qty);

    int deleteItem(@Param("memberId") String memberId, @Param("sku") String sku);

    // linked_func: FUNC-order-012 — 체크아웃 성공 시 장바구니 전체 비움(SR-203 D6)
    int deleteAllItems(@Param("memberId") String memberId);

    /**
     * linked_func: FUNC-order-012 — 체크아웃 동시성 차단용 잠금 조회(SELECT...FOR UPDATE,
     * QA FAIL round1 필수1 재작업). 상품 조인은 하지 않는다(memberId, sku, qty, addedAt만 채움) —
     * 재고·판매상태는 CartService의 사전 스윕에서 productDao로 별도 조회한다.
     */
    List<CartItem> selectItemsForUpdate(@Param("memberId") String memberId);
}
