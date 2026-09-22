package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Order;
import com.sm.lab.shop.domain.OrderDelivery;
import com.sm.lab.shop.domain.OrderItem;
import com.sm.lab.shop.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 주문 목록 조회 — 회원·주문상태·조회기간 필터·페이징. linked_func: FUNC-order-001 (LAB-101, SR-205)
     * spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
     * SR-205: startDate/endDate(기간) 파라미터 추가 — orderState와 AND 결합, 응답 스키마는 불변.
     * 미제시 시 기본값(최근 30일)은 서비스 계층에서 적용한다(OrderService.list).
     * 날짜 형식 오류(예: 2026-13-99)는 Spring의 기본 타입 변환 실패 처리로 기존 400 계약에 편입된다
     * (MethodArgumentTypeMismatchException → 400, 별도 처리 불필요).
     */
    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String memberId,
                                    @RequestParam(required = false) String orderState,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return orderService.list(memberId, orderState, startDate, endDate, page, size);
    }

    /** 주문 상세 조회 — 라인·배송 포함. */
    @GetMapping("/{orderNo}")
    public Order detail(@PathVariable String orderNo) {
        return orderService.detail(orderNo);
    }

    /** 주문 생성 — 재고 차감 포함. */
    @PostMapping
    public Order create(@RequestBody CreateOrderRequest req) {
        return orderService.create(req.memberId(), req.items());
    }

    /** 주문 취소. */
    @PatchMapping("/{orderNo}/cancel")
    public Order cancel(@PathVariable String orderNo) {
        return orderService.cancel(orderNo);
    }

    /** 주문 배송 목록. */
    @GetMapping("/{orderNo}/deliveries")
    public List<OrderDelivery> deliveries(@PathVariable String orderNo) {
        return orderService.detail(orderNo).getDeliveries();
    }

    // linked_func: FUNC-order-013 — 주문 목록 CSV 내보내기 (LAB-104, INF-ORD-015)
    // spec: docs/00_FUNC/stories/STORY-FUNC-order-013.md
    /**
     * 주문 목록 CSV 내보내기 — [[INF-ORD-003]]과 동일 필터·정렬·상시필터(OrderDao.selectOrders
     * 재사용). 새 조회 규칙 없음. 경로 우선순위: 리터럴 세그먼트(/export)가 /{orderNo}보다 우선 매칭됨.
     * 필터 파라미터: {@code orderState}가 정본(목록 API [[INF-ORD-003]]과 동일 축), {@code status}는
     * 별칭 — 둘 다 오면 {@code orderState} 우선(D11, QA r1 FAIL 정정 — 종전 status-only는
     * orderState를 조용히 무시해 무필터 전량 CSV를 유발했음).
     */
    @GetMapping(value = "/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String memberId,
                                         @RequestParam(required = false) String orderState,
                                         @RequestParam(required = false) String status) {
        String stateFilter = (orderState != null) ? orderState : status;
        byte[] csv = orderService.exportCsv(memberId, stateFilter);
        String filename = "orders_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }

    public record CreateOrderRequest(String memberId, List<OrderItem> items) { }
}
