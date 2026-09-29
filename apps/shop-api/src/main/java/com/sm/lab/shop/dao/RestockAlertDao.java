package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.RestockAlertView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재입고 알림 신청(SR-364 #1, INF-ORD-048) 등록 + 목록 조회(SR-364 #2, INF-ORD-049) 매퍼.
 *
 * <p><b>존재 판정은 read-then-write가 아니라 단일 원자문</b>(사례집 SR-231 r2/SR-298 #1·#2와 동일
 * 계열 — 카운터가 아니라 존재 판정이지만 같은 이유로 read-then-write 경쟁을 피한다). {@link
 * #insertAlert}는 PK(member_id, sku) 충돌을 DB가 흡수해 이미 신청한 상품 재요청도 예외 없이 0행
 * 영향으로 끝난다({@code ProductWishDao#insertWish}와 동일 원리).
 */
@Mapper
public interface RestockAlertDao {

    /**
     * 등록 — {@code INSERT IGNORE} 단일 원자문. 신규/재신청 구분 없이 항상 성공(예외 없음) —
     * 동시에 두 번 신청 요청이 와도 PK 충돌을 DB가 흡수하므로 500이 나지 않는다.
     */
    int insertAlert(@Param("memberId") String memberId, @Param("sku") String sku,
                     @Param("createdAt") LocalDateTime createdAt);

    /**
     * 목록 — 이 회원이 신청한 재입고 알림(sku, createdAt)을 {@code created_at DESC}(최근 신청 순)로
     * 반환한다. 상품 조인 없음(AC 응답이 sku·createdAt 두 필드뿐 — {@code ProductWishView}처럼
     * 상품명·가격·이미지까지 조인하지 않는다, STORY "파일" 절 AC 초과 구현 금지).
     */
    List<RestockAlertView> selectAlertList(@Param("memberId") String memberId);
}
