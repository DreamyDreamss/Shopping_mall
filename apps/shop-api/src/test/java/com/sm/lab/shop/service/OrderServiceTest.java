// linked_func: FUNC-order-013
// spec: docs/00_FUNC/stories/STORY-FUNC-order-013.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.OrderDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.domain.Order;
import com.sm.lab.shop.domain.OrderDelivery;
import com.sm.lab.shop.domain.OrderItem;
import com.sm.lab.shop.support.TestClocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// LAB-104 / D10: CSV 내보내기 — OrderDao.selectOrders 재사용, 새 조회 규칙 없음.
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderDao orderDao;
    @Mock
    private ProductDao productDao;

    private OrderService newService() {
        return new OrderService(orderDao, productDao);
    }

    private static Order order(String orderNo, String memberId, String state, long total, LocalDateTime at) {
        Order o = new Order();
        o.setOrderNo(orderNo);
        o.setMemberId(memberId);
        o.setOrderState(state);
        o.setTotalAmount(total);
        o.setOrderedAt(at);
        return o;
    }

    // linked_tc: TC-FUNC-order-013-01
    @Test
    void exportCsv_prependsUtf8BomAsFirstThreeBytes() {
        when(orderDao.selectOrders(null, null, null, null, 0, Integer.MAX_VALUE)).thenReturn(List.of());

        byte[] csv = newService().exportCsv(null, null);

        assertThat(csv[0]).isEqualTo((byte) 0xEF);
        assertThat(csv[1]).isEqualTo((byte) 0xBB);
        assertThat(csv[2]).isEqualTo((byte) 0xBF);
    }

    // linked_tc: TC-FUNC-order-013-01
    // r3(SR-223): 헤더를 영문 필드명 → 한글 표시 라벨로 변경(데이터 컬럼 순서는 종전과 동일:
    // orderNo,memberId,status,totalAmount,orderedAt → 주문번호,회원,상태,금액,주문일시).
    @Test
    void exportCsv_headerRow_usesKoreanLabels() {
        when(orderDao.selectOrders(null, null, null, null, 0, Integer.MAX_VALUE)).thenReturn(List.of());

        byte[] csv = newService().exportCsv(null, null);
        String text = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);

        assertThat(text).startsWith("주문번호,회원,상태,금액,주문일시\r\n");
    }

    // linked_tc: TC-FUNC-order-013-05
    // r3(SR-223): 헤더 한글화(데이터 없음 — BOM+헤더만).
    @Test
    void exportCsv_emptyResult_returnsOnlyBomAndHeader() {
        when(orderDao.selectOrders("M-9999", null, null, null, 0, Integer.MAX_VALUE)).thenReturn(List.of());

        byte[] csv = newService().exportCsv("M-9999", null);
        String text = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);

        assertThat(text).isEqualTo("주문번호,회원,상태,금액,주문일시\r\n");
    }

    // linked_tc: TC-FUNC-order-013-02
    // r2(D11, QA r1 FAIL 정정): 기대값을 at.toString()(자기참조 — 포맷이 무엇이든 항상 통과)이 아닌
    // 리터럴 문자열로 고정. orderedAt은 ISO_LOCAL_DATE_TIME이라 초가 0이어도 생략되지 않아야 한다.
    @Test
    void exportCsv_dataRow_containsAllFieldsInHeaderOrder() {
        LocalDateTime at = LocalDateTime.of(2026, 8, 17, 9, 0, 0);
        when(orderDao.selectOrders(null, null, null, null, 0, Integer.MAX_VALUE))
                .thenReturn(List.of(order("20260817-0001", "M-0003", "PLACED", 55000L, at)));

        byte[] csv = newService().exportCsv(null, null);
        String text = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
        String[] lines = text.split("\r\n");

        assertThat(lines).hasSize(2);
        assertThat(lines[1]).isEqualTo("20260817-0001,M-0003,PLACED,55000,2026-08-17T09:00:00");
    }

    // linked_tc: TC-FUNC-order-013-04 (RFC 4180 인용)
    @Test
    void exportCsv_fieldWithCommaQuoteAndNewline_isRfc4180Quoted() {
        Order o = order("20260817-0002", "M-\"weird\",name\n", "PLACED", 1000L, null);
        when(orderDao.selectOrders(null, null, null, null, 0, Integer.MAX_VALUE)).thenReturn(List.of(o));

        byte[] csv = newService().exportCsv(null, null);
        String text = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);

        assertThat(text).contains("\"M-\"\"weird\"\",name\n\"");
    }

    // linked_tc: TC-FUNC-order-013-03/04 — 목록 조회와 동일 필터 파라미터를 그대로 DAO에 전달(새 조회 규칙 없음)
    @Test
    void exportCsv_passesMemberIdAndStatusThroughToSameDaoMethodAsList() {
        when(orderDao.selectOrders("M-0001", "PLACED", null, null, 0, Integer.MAX_VALUE)).thenReturn(List.of());

        newService().exportCsv("M-0001", "PLACED");

        verify(orderDao).selectOrders("M-0001", "PLACED", null, null, 0, Integer.MAX_VALUE);
    }

    // linked_tc: TC-FUNC-order-013-07 (CSV 수식 인젝션 방어, D11/QA r1 권고2→필수 승격)
    @Test
    void exportCsv_fieldStartingWithFormulaChar_isPrefixedWithSingleQuote() {
        when(orderDao.selectOrders(null, null, null, null, 0, Integer.MAX_VALUE)).thenReturn(List.of(
                order("20260817-0010", "=cmd|calc", "PLACED", 1000L, null),
                order("20260817-0011", "+1+1", "PLACED", 1000L, null),
                order("20260817-0012", "-2+3", "PLACED", 1000L, null),
                order("20260817-0013", "@SUM(A1)", "PLACED", 1000L, null)
        ));

        byte[] csv = newService().exportCsv(null, null);
        String text = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);

        assertThat(text).contains("20260817-0010,'=cmd|calc,PLACED,1000,");
        assertThat(text).contains("20260817-0011,'+1+1,PLACED,1000,");
        assertThat(text).contains("20260817-0012,'-2+3,PLACED,1000,");
        assertThat(text).contains("20260817-0013,'@SUM(A1),PLACED,1000,");
    }

    // linked_tc: TC-FUNC-order-013-26 (SR-204 R-3)
    // 실 DB를 변경하지 않고 상한 초과를 재현하기 위해 3-arg 생성자로 상한을 낮춰 검증한다
    // (프로덕션 기본은 application.yml lab.export-max-rows=1000).
    @Test
    void exportCsv_resultExceedsConfiguredLimit_throwsBeforeAssemblingCsv() {
        when(orderDao.selectOrders(null, null, null, null, 0, Integer.MAX_VALUE)).thenReturn(List.of(
                order("20260817-0001", "M-0001", "PLACED", 1000L, null),
                order("20260817-0002", "M-0001", "PLACED", 1000L, null),
                order("20260817-0003", "M-0001", "PLACED", 1000L, null),
                order("20260817-0004", "M-0001", "PLACED", 1000L, null)
        ));
        OrderService service = new OrderService(orderDao, productDao, 3);

        assertThatThrownBy(() -> service.exportCsv(null, null))
                .isInstanceOf(ExportRowLimitExceededException.class)
                .extracting(ex -> ((ExportRowLimitExceededException) ex).getLimit())
                .isEqualTo(3);
    }

    // linked_tc: TC-FUNC-order-013-27
    // 상한 이하는 종전과 동일하게 정상 조립된다(회귀).
    @Test
    void exportCsv_resultWithinConfiguredLimit_assemblesNormally() {
        when(orderDao.selectOrders(null, null, null, null, 0, Integer.MAX_VALUE)).thenReturn(List.of(
                order("20260817-0001", "M-0001", "PLACED", 1000L, null)
        ));
        OrderService service = new OrderService(orderDao, productDao, 3);

        byte[] csv = service.exportCsv(null, null);

        assertThat(csv[0]).isEqualTo((byte) 0xEF);
    }

    // linked_func: FUNC-order-001
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
    // SR-205: 조회 기간 필터 — 파라미터 미제시 시 최근 30일(오늘-30일 ~ 오늘, 양끝 포함) 기본 적용.
    // linked_tc: TC-FUNC-order-001-01
    @Test
    void list_noDateParams_defaultsToLast30DaysInclusive() {
        when(orderDao.selectOrders(eq(null), eq(null), any(), any(), eq(0), eq(20)))
                .thenReturn(List.of());
        when(orderDao.countOrders(eq(null), eq(null), any(), any())).thenReturn(0);

        newService().list(null, null, null, null, 1, 20);

        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(orderDao).selectOrders(eq(null), eq(null), startCaptor.capture(), endCaptor.capture(), eq(0), eq(20));
        assertThat(startCaptor.getValue()).isEqualTo(LocalDate.now().minusDays(30));
        assertThat(endCaptor.getValue()).isEqualTo(LocalDate.now());
    }

    // SR-300: 기본 조회창이 시스템 시계가 아니라 주입된 Clock을 쓰는지 직접 확인한다(회귀:
    // 시스템 LocalDate.now()와 상대 비교하는 위 테스트와 달리, 시스템 날짜와 무관한 고정
    // 기대값과 정확히 비교해야 이 결함(SR-300 원인)을 재현/방지한다).
    @Test
    void list_noDateParams_usesInjectedClockNotSystemClock() {
        Clock fixedClock = TestClocks.SEED_TODAY;
        OrderService service = new OrderService(orderDao, productDao, fixedClock);
        when(orderDao.selectOrders(eq(null), eq(null), any(), any(), eq(0), eq(20)))
                .thenReturn(List.of());
        when(orderDao.countOrders(eq(null), eq(null), any(), any())).thenReturn(0);

        service.list(null, null, null, null, 1, 20);

        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(orderDao).selectOrders(eq(null), eq(null), startCaptor.capture(), endCaptor.capture(), eq(0), eq(20));
        assertThat(startCaptor.getValue()).isEqualTo(LocalDate.of(2026, 7, 21));
        assertThat(endCaptor.getValue()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    // linked_func: FUNC-order-001 — 하나만 미제시된 경우 그 값만 기본 적용(독립 보정, [미상] 최소가정)
    // linked_tc: TC-FUNC-order-001-02
    @Test
    void list_onlyStartDateProvided_defaultsEndDateToToday() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        when(orderDao.selectOrders(eq(null), eq(null), eq(start), any(), eq(0), eq(20)))
                .thenReturn(List.of());
        when(orderDao.countOrders(eq(null), eq(null), eq(start), any())).thenReturn(0);

        newService().list(null, null, start, null, 1, 20);

        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(orderDao).selectOrders(eq(null), eq(null), eq(start), endCaptor.capture(), eq(0), eq(20));
        assertThat(endCaptor.getValue()).isEqualTo(LocalDate.now());
    }

    // linked_func: FUNC-order-001 — 명시적 기간 파라미터는 보정 없이 그대로 DAO에 전달
    // linked_tc: TC-FUNC-order-001-03
    @Test
    void list_explicitDateParams_passThroughUnchanged() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        when(orderDao.selectOrders("M-0001", "PLACED", start, end, 0, 20)).thenReturn(List.of());
        when(orderDao.countOrders("M-0001", "PLACED", start, end)).thenReturn(0);

        newService().list("M-0001", "PLACED", start, end, 1, 20);

        verify(orderDao).selectOrders("M-0001", "PLACED", start, end, 0, 20);
        verify(orderDao).countOrders("M-0001", "PLACED", start, end);
    }

    // linked_func: FUNC-order-001 — 기간 필터는 기존 orderState 필터와 AND(둘 다 그대로 DAO에 전달)
    // linked_tc: TC-FUNC-order-001-10
    @Test
    void list_periodFilterCombinesWithOrderStateAsAnd() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        when(orderDao.selectOrders(null, "CANCELED", start, end, 0, 20)).thenReturn(List.of());
        when(orderDao.countOrders(null, "CANCELED", start, end)).thenReturn(0);

        newService().list(null, "CANCELED", start, end, 1, 20);

        verify(orderDao).selectOrders(null, "CANCELED", start, end, 0, 20);
    }

    // linked_func: FUNC-order-001 — 응답 스키마(totalCount/page/items)는 SR-205로 변경되지 않는다(회귀)
    // linked_tc: TC-FUNC-order-001-11
    @Test
    void list_responseSchema_unchangedTotalCountPageItems() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        Order o = order("20260817-0001", "M-0003", "PLACED", 55000L, LocalDateTime.of(2026, 8, 17, 9, 0));
        when(orderDao.selectOrders(null, null, start, end, 0, 20)).thenReturn(List.of(o));
        when(orderDao.countOrders(null, null, start, end)).thenReturn(1);

        Map<String, Object> result = newService().list(null, null, start, end, 1, 20);

        assertThat(result.keySet()).containsExactlyInAnyOrder("totalCount", "page", "items");
        assertThat(result.get("totalCount")).isEqualTo(1);
        assertThat(result.get("page")).isEqualTo(1);
        assertThat(result.get("items")).isEqualTo(List.of(o));
    }

    // linked_func: FUNC-order-001 — round2(QA r1 FAIL 필수1 재작업): endDate만 제시되면 startDate가
    // 오늘-30일로 독립 보정되는데, endDate가 그보다 더 과거면 유효 구간이 역전된다(round1은 이 경우
    // 조용히 0건을 반환했음 — 화면 폼에서 시작일을 비우고 과거의 종료일만 채우면 재현되던 결함).
    // 지금은 400으로 거부해야 한다(DAO 호출 없이 즉시 실패).
    // linked_tc: TC-FUNC-order-001-12
    @Test
    void list_endDateOnlyCausesInvertedEffectiveRange_throws400() {
        LocalDate farPastEnd = LocalDate.now().minusDays(60); // effectiveStart(오늘-30일)보다 더 과거

        assertThatThrownBy(() -> newService().list(null, null, null, farPastEnd, 1, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // linked_func: FUNC-order-001 — round3(SR-208): startDate 미입력으로 서버가 계산한 값(오늘-30일)을
    // "역전 사유"로 제시하지 않는다 — "기본값(최근 30일) 적용" 사실 + 취할 행동(시작일 지정) 안내로 교체.
    // 사용자가 입력한 적 없는 계산된 startDate 값 자체는 문구에 등장하지 않아야 한다.
    // linked_tc: TC-FUNC-order-001-14
    @Test
    void list_endDateOnly_400ReasonExplainsDefaultInsteadOfComputedStartDate() {
        LocalDate farPastEnd = LocalDate.now().minusDays(60);
        LocalDate computedStart = LocalDate.now().minusDays(30);

        assertThatThrownBy(() -> newService().list(null, null, null, farPastEnd, 1, 20))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getReason())
                .satisfies(reason -> {
                    assertThat((String) reason).contains("기본값(최근 30일)");
                    assertThat((String) reason).contains("시작일");
                    assertThat((String) reason).doesNotContain(computedStart.toString());
                });
    }

    // linked_func: FUNC-order-001 — round2(QA r1 권고4): 두 값 모두 명시적으로 제시됐어도 역전
    // (start>end)이면 동일하게 400으로 거부한다(round1은 이 케이스에 대한 테스트가 없었음).
    // round3(SR-208): 이 경로는 사용자가 두 값을 모두 직접 지정했으므로 기존 문구(회귀)를 유지한다.
    // linked_tc: TC-FUNC-order-001-13
    @Test
    void list_explicitStartAfterEnd_throws400() {
        LocalDate start = LocalDate.of(2026, 8, 31);
        LocalDate end = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> newService().list(null, null, start, end, 1, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(((ResponseStatusException) e).getReason())
                            .isEqualTo("조회 시작일(" + start + ")이 종료일(" + end + ")보다 늦습니다");
                });
    }

    // linked_func: FUNC-order-001 — SR-210: 주문 목록 화면(UIS-ORD-001) 배송상태 열 조합.
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
    // 확정 문답(scope_freeze, api_compat): 이 메서드는 화면 조회 경로 전용 — list()의 응답 스키마·
    // Order 도메인은 건드리지 않고 별도 맵으로만 결과를 낸다.
    private static OrderDelivery delivery(String orderNo, String state) {
        OrderDelivery d = new OrderDelivery();
        d.setOrderNo(orderNo);
        d.setDeliveryState(state);
        return d;
    }

    // linked_tc: TC-FUNC-order-001-16
    @Test
    void latestDeliveryStatesByOrderNo_emptyOrderList_returnsEmptyMapWithoutCallingDao() {
        Map<String, String> result = newService().latestDeliveryStatesByOrderNo(List.of());

        assertThat(result).isEmpty();
        verify(orderDao, never()).selectLatestDeliveryStates(any());
    }

    // linked_tc: TC-FUNC-order-001-17
    @Test
    void latestDeliveryStatesByOrderNo_ordersWithDeliveries_mapsOrderNoToLatestState() {
        Order o1 = order("20260816-0002", "M-0001", "SHIPPED", 10000L, LocalDateTime.now());
        Order o2 = order("20260815-0001", "M-0001", "DONE", 20000L, LocalDateTime.now());
        when(orderDao.selectLatestDeliveryStates(List.of("20260816-0002", "20260815-0001")))
                .thenReturn(List.of(delivery("20260816-0002", "SHIPPED"), delivery("20260815-0001", "DELIVERED")));

        Map<String, String> result = newService().latestDeliveryStatesByOrderNo(List.of(o1, o2));

        assertThat(result).containsEntry("20260816-0002", "SHIPPED");
        assertThat(result).containsEntry("20260815-0001", "DELIVERED");
    }

    // linked_tc: TC-FUNC-order-001-18 — 배송 이력이 없는 주문번호는 맵에 키 자체가 없다(호출측이 "-"로 보완)
    @Test
    void latestDeliveryStatesByOrderNo_orderWithoutDeliveryHistory_isAbsentFromMap() {
        Order noDelivery = order("20260817-0001", "M-0001", "PLACED", 10000L, LocalDateTime.now());
        when(orderDao.selectLatestDeliveryStates(List.of("20260817-0001"))).thenReturn(List.of());

        Map<String, String> result = newService().latestDeliveryStatesByOrderNo(List.of(noDelivery));

        assertThat(result).doesNotContainKey("20260817-0001");
    }

    // ===== FUNC-order-002 — 주문 상세 조회 / 취소 =====
    // linked_func: FUNC-order-002
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-002.md

    private static OrderItem item(String sku, int qty) {
        OrderItem i = new OrderItem();
        i.setSku(sku);
        i.setQty(qty);
        return i;
    }

    // INF-ORD-004: orderNo로 조회된 주문이 없으면(del_yn 상시필터로 걸러진 논리삭제 포함) 404
    @Test
    void detail_orderNotFound_throws404() {
        when(orderDao.selectByOrderNo("NO-SUCH")).thenReturn(null);

        assertThatThrownBy(() -> newService().detail("NO-SUCH"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // INF-ORD-004: items/deliveries는 DAO 조회 결과를 그대로 주문 객체에 채운다
    @Test
    void detail_found_populatesItemsAndDeliveries() {
        Order o = order("20260907-0101", "M-0001", "PLACED", 45000L, LocalDateTime.now());
        List<OrderItem> items = List.of(item("SKU-001", 2));
        List<OrderDelivery> deliveries = List.of(delivery("20260907-0101", "READY"));
        when(orderDao.selectByOrderNo("20260907-0101")).thenReturn(o);
        when(orderDao.selectItems("20260907-0101")).thenReturn(items);
        when(orderDao.selectDeliveries("20260907-0101")).thenReturn(deliveries);

        Order result = newService().detail("20260907-0101");

        assertThat(result.getItems()).isEqualTo(items);
        assertThat(result.getDeliveries()).isEqualTo(deliveries);
    }

    // INF-ORD-004: 배송이 없으면 deliveries는 빈 배열(null 아님)
    @Test
    void detail_noDeliveries_returnsEmptyListNotNull() {
        Order o = order("20260907-0102", "M-0001", "PLACED", 10000L, LocalDateTime.now());
        when(orderDao.selectByOrderNo("20260907-0102")).thenReturn(o);
        when(orderDao.selectItems("20260907-0102")).thenReturn(List.of());
        when(orderDao.selectDeliveries("20260907-0102")).thenReturn(List.of());

        Order result = newService().detail("20260907-0102");

        assertThat(result.getDeliveries()).isNotNull();
        assertThat(result.getDeliveries()).isEmpty();
    }

    // INF-ORD-006: 주문 상태가 이미 CANCELED면 409 거부(상태 전이 UPDATE 시도 없이)
    @Test
    void cancel_orderAlreadyCanceled_throws409() {
        Order o = order("20260907-0103", "M-0001", "CANCELED", 10000L, LocalDateTime.now());
        when(orderDao.selectByOrderNo("20260907-0103")).thenReturn(o);
        when(orderDao.selectItems("20260907-0103")).thenReturn(List.of());
        when(orderDao.selectDeliveries("20260907-0103")).thenReturn(List.of());

        assertThatThrownBy(() -> newService().cancel("20260907-0103"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(orderDao, never()).updateState(any(), any());
    }

    // INF-ORD-006: 주문 상태가 이미 DONE이어도 동일하게 409 거부
    @Test
    void cancel_orderAlreadyDone_throws409() {
        Order o = order("20260907-0104", "M-0001", "DONE", 10000L, LocalDateTime.now());
        when(orderDao.selectByOrderNo("20260907-0104")).thenReturn(o);
        when(orderDao.selectItems("20260907-0104")).thenReturn(List.of());
        when(orderDao.selectDeliveries("20260907-0104")).thenReturn(List.of());

        assertThatThrownBy(() -> newService().cancel("20260907-0104"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    // INF-ORD-006: 배송 중 하나라도 SHIPPED면 409(ORD-4001) — 상태 전이 UPDATE 시도 없이 거부
    @Test
    void cancel_hasShippedDelivery_throws409WithOrd4001Code() {
        Order o = order("20260907-0105", "M-0001", "PARTIAL_SHIPPED", 10000L, LocalDateTime.now());
        when(orderDao.selectByOrderNo("20260907-0105")).thenReturn(o);
        when(orderDao.selectItems("20260907-0105")).thenReturn(List.of());
        when(orderDao.selectDeliveries("20260907-0105")).thenReturn(List.of(delivery("20260907-0105", "SHIPPED")));

        assertThatThrownBy(() -> newService().cancel("20260907-0105"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(((ResponseStatusException) e).getReason()).contains("ORD-4001");
                });
        verify(orderDao, never()).updateState(any(), any());
    }

    // INF-ORD-006: 배송 중 하나라도 DELIVERED여도 동일하게 409(ORD-4001) 거부
    @Test
    void cancel_hasDeliveredDelivery_throws409() {
        Order o = order("20260907-0106", "M-0001", "PAID", 10000L, LocalDateTime.now());
        when(orderDao.selectByOrderNo("20260907-0106")).thenReturn(o);
        when(orderDao.selectItems("20260907-0106")).thenReturn(List.of());
        when(orderDao.selectDeliveries("20260907-0106")).thenReturn(List.of(delivery("20260907-0106", "DELIVERED")));

        assertThatThrownBy(() -> newService().cancel("20260907-0106"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    // INF-ORD-006: 조건부 UPDATE(order_state NOT IN ('CANCELED','DONE'))가 0행이면 동시 요청이
    // 선점한 것 — 409로 거부하고 재고는 원복하지 않는다(레이스 가드).
    @Test
    void cancel_raceLost_conditionalUpdateReturnsZero_throws409WithoutRestockingAnyLine() {
        Order o = order("20260907-0107", "M-0001", "PLACED", 10000L, LocalDateTime.now());
        when(orderDao.selectByOrderNo("20260907-0107")).thenReturn(o);
        when(orderDao.selectItems("20260907-0107")).thenReturn(List.of(item("SKU-001", 2)));
        when(orderDao.selectDeliveries("20260907-0107")).thenReturn(List.of());
        when(orderDao.updateState("20260907-0107", "CANCELED")).thenReturn(0);

        assertThatThrownBy(() -> newService().cancel("20260907-0107"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(productDao, never()).increaseStock(any(), anyInt());
    }

    // INF-ORD-006: 상태 전이가 성공한 뒤에만 라인별 재고를 원복한다 — 호출 순서까지 검증
    @Test
    void cancel_success_updatesStateThenRestoresStockForEachLine() {
        Order o = order("20260907-0108", "M-0001", "PLACED", 10000L, LocalDateTime.now());
        when(orderDao.selectByOrderNo("20260907-0108")).thenReturn(o);
        when(orderDao.selectItems("20260907-0108"))
                .thenReturn(List.of(item("SKU-001", 2), item("SKU-002", 1)));
        when(orderDao.selectDeliveries("20260907-0108")).thenReturn(List.of());
        when(orderDao.updateState("20260907-0108", "CANCELED")).thenReturn(1);

        Order result = newService().cancel("20260907-0108");

        InOrder inOrder = Mockito.inOrder(orderDao, productDao);
        inOrder.verify(orderDao).updateState("20260907-0108", "CANCELED");
        inOrder.verify(productDao).increaseStock("SKU-001", 2);
        inOrder.verify(productDao).increaseStock("SKU-002", 1);
        assertThat(result).isNotNull();
    }
}
