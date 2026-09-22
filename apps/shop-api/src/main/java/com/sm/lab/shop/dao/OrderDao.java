// linked_func: FUNC-order-001
// spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.Order;
import com.sm.lab.shop.domain.OrderDelivery;
import com.sm.lab.shop.domain.OrderItem;
import com.sm.lab.shop.domain.OrderSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface OrderDao {
    // linked_func: FUNC-order-001 — 주문상태 필터(LAB-101) + 조회 기간 필터(SR-205).
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
    // round2(QA r1 권고1 반영): 별도 selectOrdersInPeriod/countOrdersInPeriod로 쪼갰던 round1을
    // 되돌려 한 벌로 통합했다 — del_yn='N' 상시필터·MEMBERS 조인·정렬이 두 벌로 중복돼 드리프트
    // 위험이 있었고("MyBatis는 동일 메서드명 오버로드 미지원"은 오버로드가 필요한 상황이 아니었어
    // 성립하지 않는 근거였다). startDate/endDate는 선택적(null이면 무필터) — 목록 화면/API는
    // OrderService.list가 항상 유효 기간을 채워 넘기고(미제시 시 최근 30일 기본), export
    // (FUNC-order-013, OrderService.exportCsv)는 null,null을 넘겨 기간 무필터로 기존 동작 그대로
    // 유지한다. 시그니처는 이번에 바뀌었지만 export 쪽 의미(필터 없음)는 유지되므로 "이 FUNC만"
    // 원칙 위반이 아니다(QA r1 권고1이 명시적으로 이 통합을 승인).
    List<Order> selectOrders(@Param("memberId") String memberId, @Param("state") String state,
                             @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                             @Param("offset") int offset, @Param("size") int size);
    int countOrders(@Param("memberId") String memberId, @Param("state") String state,
                     @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    Order selectByOrderNo(@Param("orderNo") String orderNo);
    // linked_func: FUNC-order-004 — 회원 최근 주문 요약 (LAB-103)
    List<OrderSummary> selectRecentByMember(@Param("memberId") String memberId, @Param("limit") int limit);
    List<OrderItem> selectItems(@Param("orderNo") String orderNo);
    // linked_func: FUNC-order-006 — 배송 이력 최신순 정렬 (LAB-113)
    List<OrderDelivery> selectDeliveries(@Param("orderNo") String orderNo);
    // linked_func: FUNC-order-001 — SR-210: 주문 목록 화면(UIS-ORD-001) 배송상태 열 전용 배치 조회.
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
    // 확정 문답(scope_freeze, api_compat): INF-ORD-003(GET /api/orders) 응답 계약은 바꾸지 않고,
    // 화면 조회 경로(OrderService.latestDeliveryStatesByOrderNo → OrderViewController)에서만
    // 별도로 조합한다. round4(재작업지시 2 — QA r13 CONCERNS 권고2): 최신 판정 기준은
    // FUNC-order-006(selectDeliveries, 주문 상세 화면)과 더 이상 동일하지 않다 — 이 화면(주문 목록)의
    // "최신"은 배송 이력 생성 순서(delivery_no DESC 단독 기준)로 재정의했다. shipped_at DESC를
    // 1순위로 쓰면 미출고(shipped_at NULL) 건이 더 나중에 생성돼도 항상 밀리는 결함이 있었다
    // (실데이터 20260816-0002로 재현). 주문번호별 여러 건이 아니라 1건(최신 생성)만 반환한다.
    // 배송 이력이 없는 주문번호는 결과 목록에 아예 나타나지 않는다(서비스 계층이 "-"로 보완).
    // orderNos가 비어 있으면 호출하지 않는다(호출측 책임 — IN () SQL 오류 방지).
    List<OrderDelivery> selectLatestDeliveryStates(@Param("orderNos") List<String> orderNos);
    int insertOrder(Order order);
    int insertItem(OrderItem item);
    int updateState(@Param("orderNo") String orderNo, @Param("state") String state);
}
