// linked_func: FUNC-order-001
// spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.OrderDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.domain.Order;
import com.sm.lab.shop.domain.OrderDelivery;
import com.sm.lab.shop.domain.OrderItem;
import com.sm.lab.shop.domain.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderService {
    private final OrderDao orderDao;
    private final ProductDao productDao;
    private final int exportMaxRows;
    private final Clock clock;
    private final AtomicInteger seq = new AtomicInteger(100);

    // SR-300: '오늘'을 읽는 방법만 주입 가능한 Clock으로 바꾼다(MemberAddressService와 동일
    // 패턴). 스프링 빈 생성 경로는 이 4-arg 생성자만 사용한다(유일한 @Autowired 생성자).
    @Autowired
    public OrderService(OrderDao orderDao, ProductDao productDao,
                        @Value("${lab.export-max-rows:1000}") int exportMaxRows, Clock clock) {
        this.orderDao = orderDao;
        this.productDao = productDao;
        this.exportMaxRows = exportMaxRows;
        this.clock = clock;
    }

    // 기존 단위 테스트 호환(기본 상한 1000, 시스템 시계) — 시그니처 불변.
    public OrderService(OrderDao orderDao, ProductDao productDao) {
        this(orderDao, productDao, 1000, Clock.systemDefaultZone());
    }

    // linked_func: FUNC-order-013 — SR-204 R-3: export 행 상한(application.yml lab.export-max-rows,
    // 기본 1000 — 코드 하드코딩 금지 D13). 테스트에서 상한을 낮춰 413을 실데이터 변경 없이 재현할 수
    // 있도록 이 시그니처를 유지한다(시스템 시계로 위임, @Autowired는 4-arg 생성자로 옮김 — SR-300).
    public OrderService(OrderDao orderDao, ProductDao productDao, int exportMaxRows) {
        this(orderDao, productDao, exportMaxRows, Clock.systemDefaultZone());
    }

    /** SR-300: 고정 Clock 단위 테스트 전용(MemberAddressService의 (dao, clock) 시임 패턴과 동일). */
    OrderService(OrderDao orderDao, ProductDao productDao, Clock clock) {
        this(orderDao, productDao, 1000, clock);
    }

    // linked_func: FUNC-order-001 — 주문상태 필터(LAB-101) + 조회 기간 필터(SR-205).
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
    // SR-205: startDate/endDate 미제시 시 기본값 = 최근 30일(오늘 기준 -30일 ~ 오늘, 양끝 포함) —
    // 두 파라미터를 독립적으로 기본 처리한다(하나만 비었을 때도 그 값만 보정, D-미상 최소가정).
    // round2(QA r1 FAIL 필수1 재작업): 독립 보정 자체는 유지하되, 그 결과로 유효 구간(effectiveStart/
    // effectiveEnd)이 역전(start>end)되면 조용히 0건을 반환하지 않고 기존 400 유효성 실패 계약으로
    // 거부한다 — endDate만 제시된 요청이 effectiveStart(오늘-30일)보다 더 과거면 이 경로로 재현된다.
    // 기간 필터는 기존 orderState 필터와 AND로 결합(scope_freeze). 응답 스키마(totalCount/page/items)는
    // 불변 — round1이 별도로 신설했던 selectOrdersInPeriod/countOrdersInPeriod는 QA r1 권고1에 따라
    // selectOrders/countOrders로 되돌려 통합했다(OrderDao 참조, export 경로는 null,null로 무영향).
    // round3(SR-208): startDate 미입력(=서버가 기본값을 계산) 상태에서 역전이 나면, 사용자가 입력한
    // 적 없는 계산값을 "역전 사유"로 제시하지 않는다 — "기본값(최근 30일) 적용" 사실과 취할 행동
    // (시작일을 함께 지정)만 안내한다. 두 값이 모두 명시적으로 주어진 경우(회귀)는 기존 문구 유지.
    public Map<String, Object> list(String memberId, String orderState, LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now(clock).minusDays(30);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now(clock);
        if (effectiveStart.isAfter(effectiveEnd)) {
            String reason = (startDate == null)
                    ? "시작일을 지정하지 않아 기본값(최근 30일)이 적용되었습니다. 입력한 종료일(" + effectiveEnd
                            + ")이 이 기본 조회 기간보다 앞서 있어 조회할 수 없습니다. 시작일을 함께 지정해 주세요."
                    : "조회 시작일(" + effectiveStart + ")이 종료일(" + effectiveEnd + ")보다 늦습니다";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
        }
        int offset = Math.max(0, page - 1) * size;
        List<Order> rows = orderDao.selectOrders(memberId, orderState, effectiveStart, effectiveEnd, offset, size);
        int total = orderDao.countOrders(memberId, orderState, effectiveStart, effectiveEnd);
        return Map.of("totalCount", total, "page", page, "items", rows);
    }

    // linked_func: FUNC-order-001 — SR-210: 주문 목록 화면(UIS-ORD-001) 배송상태 열 전용 조합.
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
    // 확정 문답(scope_freeze, api_compat): 배송상태는 INF-ORD-003(GET /api/orders) 응답에 포함하지
    // 않는다 — 이 메서드는 OrderViewController 전용이며 OrderController(REST API)는 호출하지 않는다.
    // list()의 리턴 스키마(totalCount/page/items)와 Order 도메인 자체는 건드리지 않고, 화면에서만
    // 별도 맵으로 조합해 붙인다. 값은 주문별 "최신 배송 이력 상태" — round4(재작업지시 2 — QA r13
    // CONCERNS 권고2): FUNC-order-006(주문 상세 화면)의 shipped_at DESC 기준과 더 이상 맞추지 않고,
    // 이 화면(주문 목록)에 맞게 "배송 이력 생성 순서"(delivery_no DESC 단독)로 재정의했다 — 미출고
    // (shipped_at NULL) 건이 더 나중에 생성됐어도 항상 밀리던 결함을 정정(OrderDao 참조). 배송
    // 이력이 없는 주문번호는 이 맵에 키 자체가 없다 — 호출측(화면 템플릿)이 "-"로 보완한다.
    public Map<String, String> latestDeliveryStatesByOrderNo(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return Map.of();
        }
        List<String> orderNos = orders.stream().map(Order::getOrderNo).toList();
        List<OrderDelivery> latest = orderDao.selectLatestDeliveryStates(orderNos);
        Map<String, String> byOrderNo = new HashMap<>();
        for (OrderDelivery d : latest) {
            byOrderNo.put(d.getOrderNo(), d.getDeliveryState());
        }
        return byOrderNo;
    }

    // linked_func: FUNC-order-013 — 주문 목록 CSV 내보내기: OrderDao.selectOrders 재사용, 새 조회
    // 규칙 없음(LAB-104, D10). 페이징 미적용(랩 데이터 소규모, offset=0/size=무제한).
    // r2(D11, QA r1 FAIL 정정): orderedAt은 ISO_LOCAL_DATE_TIME 고정 패턴 직렬화(가변폭 금지),
    // csvField에 CSV 수식 인젝션 방어(선두 =,+,-,@ 는 작은따옴표 프리픽스) 추가.
    // r3(SR-223): 헤더를 영문 필드명 → 한글 표시 라벨로 변경(주문번호·회원·상태·금액·주문일시).
    // 데이터 컬럼 순서·필드명(orderNo/memberId/status/totalAmount/orderedAt)·타입·의미는 그대로다
    // (SR-223 확정 문답 api_compat: "기존 필드명·타입·의미는 그대로 둔다" — 표시(헤더) 전용 변경).
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-013.md
    private static final String CSV_HEADER = "주문번호,회원,상태,금액,주문일시";
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter ORDERED_AT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public byte[] exportCsv(String memberId, String orderState) {
        // round2: selectOrders가 FUNC-order-001 통합으로 startDate/endDate 파라미터를 얻었다 —
        // export는 기간 필터를 쓰지 않으므로 null,null(무필터)을 넘겨 기존 동작을 그대로 유지한다.
        List<Order> rows = orderDao.selectOrders(memberId, orderState, null, null, 0, Integer.MAX_VALUE);

        // SR-204 R-3: CSV 조립(StringBuilder 구축) 전에 건수를 판정한다 — 부분 반출로 위장하지
        // 않고 413으로 정직하게 거부(D13).
        if (rows.size() > exportMaxRows) {
            throw new ExportRowLimitExceededException(exportMaxRows);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\r\n");
        for (Order o : rows) {
            sb.append(csvField(o.getOrderNo())).append(',')
              .append(csvField(o.getMemberId())).append(',')
              .append(csvField(o.getOrderState())).append(',')
              .append(o.getTotalAmount()).append(',')
              .append(csvField(o.getOrderedAt() == null ? "" : o.getOrderedAt().format(ORDERED_AT_FORMAT)))
              .append("\r\n");
        }

        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, out, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, out, UTF8_BOM.length, body.length);
        return out;
    }

    // RFC 4180 인용(콤마·큰따옴표·개행 포함 시 큰따옴표로 감싸고 내부 큰따옴표는 이중화) +
    // CSV 수식 인젝션 방어(D11): 선두가 =,+,-,@ 이면 작은따옴표를 앞에 붙여 Excel이 수식으로
    // 해석하지 않게 만든다(인용 여부 판단은 프리픽스 부착 이후 값 기준).
    private static String csvField(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        if (!v.isEmpty() && isFormulaLeadingChar(v.charAt(0))) {
            v = "'" + v;
        }
        if (v.indexOf(',') < 0 && v.indexOf('"') < 0 && v.indexOf('\n') < 0 && v.indexOf('\r') < 0) {
            return v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static boolean isFormulaLeadingChar(char c) {
        return c == '=' || c == '+' || c == '-' || c == '@';
    }

    public Order detail(String orderNo) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "주문 없음: " + orderNo);
        }
        order.setItems(orderDao.selectItems(orderNo));
        order.setDeliveries(orderDao.selectDeliveries(orderNo));
        return order;
    }

    @Transactional
    public Order create(String memberId, List<OrderItem> items) {
        // SR-300 재작업(round2, QA CONCERNS 권고1 — 함정 경고): 주문번호의 "오늘"은 SR-300 범위
        // 밖 — 시스템 시계 유지(list()만 주입 Clock을 쓴다). 통합테스트가 고정 Clock으로 "오늘"을
        // 과거로 고정한 상태에서 이 메서드로 주문을 생성하면 ordered_at은 실제 오늘이 되어, 그
        // 직후 날짜 없이 목록을 조회하는 테스트가 고정창 밖으로 조용히 빠질 수 있다(후속 SR에서
        // 시계화 검토).
        String orderNo = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%04d", seq.incrementAndGet());
        long total = 0;
        int line = 0;
        for (OrderItem it : items) {
            Product p = productDao.selectBySku(it.getSku());
            if (p == null || !"Y".equals(p.getSaleYn())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "판매 중인 상품이 아님: " + it.getSku());
            }
            if (productDao.decreaseStock(it.getSku(), it.getQty()) == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "재고 부족: " + it.getSku());
            }
            it.setOrderNo(orderNo);
            it.setLineNo(++line);
            it.setUnitPrice(p.getPrice());
            total += p.getPrice() * it.getQty();
        }
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setMemberId(memberId);
        order.setOrderState("PLACED");
        order.setTotalAmount(total);
        orderDao.insertOrder(order);
        for (OrderItem it : items) {
            orderDao.insertItem(it);
        }
        return detail(orderNo);
    }

    // linked_func: FUNC-order-002 — 주문 취소: 재고 원복 + 출고 주문 취소 거부 (LAB-102)
    @Transactional
    public Order cancel(String orderNo) {
        Order order = detail(orderNo);
        if ("CANCELED".equals(order.getOrderState()) || "DONE".equals(order.getOrderState())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "취소 불가 상태: " + order.getOrderState());
        }
        boolean shipped = order.getDeliveries().stream()
                .anyMatch(d -> "SHIPPED".equals(d.getDeliveryState()) || "DELIVERED".equals(d.getDeliveryState()));
        if (shipped) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ORD-4001: 출고 완료 배송은 취소할 수 없습니다");
        }
        // 상태 전이를 먼저 조건부로 확정(레이스 가드) — 0행이면 다른 요청이 선점한 것, 원복 없이 409
        if (orderDao.updateState(orderNo, "CANCELED") == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "취소 불가 상태(동시 처리됨): " + orderNo);
        }
        for (OrderItem it : order.getItems()) {
            productDao.increaseStock(it.getSku(), it.getQty());   // 라인별 재고 원복 — 실패 시 전체 롤백
        }
        return detail(orderNo);
    }
}
