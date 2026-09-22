// linked_func: FUNC-member-002 — SR-231 round5(사람 결정, round4 QA FAIL 재작업 지시(1)) 회귀 가드.
// FUNC-order-002(OrderService.create / ProductDao.decreaseStock)는 이 FUNC이 소유하지 않으며
// round5는 그 소스를 건드리지 않는다. round4가 이 FUNC의 필요로 datasource 전역 설정
// (useAffectedRows=true)을 켜면서 ProductDao.decreaseStock의 UPDATE 반환값 시맨틱까지 바뀌어,
// qty=0 주문 라인을 포함한 POST /api/orders 응답이 종전 200(주문 생성)에서 409("재고 부족")로
// 회귀했다(QA 실측). round5는 그 전역 설정을 제거했으므로 이 테스트는 FUNC-order-002의 계약이
// 다시 원래대로(200) 동작함을 고정한다 — 이 FUNC이 앞으로 다시 datasource 전역 설정을 건드리면
// 이 테스트가 잡아야 한다.
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop;

import com.sm.lab.shop.dao.ProductDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderCreateQtyZeroRegressionTest {

    private static final String MEMBER_ID = "M-0001";
    private static final String SKU = "SKU-1002"; // CheckoutConcurrencyTest와 동일 SKU(재고 40, 여유 충분)

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ProductDao productDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String createdOrderNo;

    @AfterEach
    void cleanUp() {
        if (createdOrderNo != null) {
            jdbcTemplate.update("DELETE FROM ORDER_ITEMS WHERE order_no = ?", createdOrderNo);
            jdbcTemplate.update("DELETE FROM ORDERS WHERE order_no = ?", createdOrderNo);
            createdOrderNo = null;
        }
    }

    // linked_tc: TC-FUNC-member-002-13 — round4 QA FAIL 필수1 회귀 고정: qty=0 주문 라인은
    // FUNC-order-002의 기존 계약대로 200(주문 생성)이어야 한다. 409(재고 부족)로 새면 이 FUNC의
    // datasource 전역 설정이 다시 다른 FUNC의 계약을 깨뜨렸다는 신호다.
    @Test
    void createOrder_withQtyZeroLine_returns200LikeBeforeThisFuncsDatasourceChange() {
        int stockBefore = productDao.selectBySku(SKU).getStockQty();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", "lab-admin-key"); // SR-204 R-1: 인증 필터 도입 후 필요
        String body = "{\"memberId\":\"" + MEMBER_ID + "\",\"items\":["
                + "{\"sku\":\"" + SKU + "\",\"qty\":0}]}";

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/orders", new HttpEntity<>(body, headers), String.class);

        String responseBody = response.getBody() == null ? "" : response.getBody();
        String orderNo = responseBody.replaceAll(".*\"orderNo\":\"([^\"]+)\".*", "$1");
        if (!orderNo.isBlank() && !orderNo.equals(responseBody)) {
            createdOrderNo = orderNo; // 실패해도 원복되도록 단언보다 먼저 등록
        }

        assertThat(response.getStatusCode())
                .as("qty=0 라인은 재고를 실질적으로 바꾸지 않으므로 409가 아니라 200이어야 함(FUNC-order-002 "
                        + "계약, round4 QA FAIL 실측 회귀) — 실제 응답: %s", responseBody)
                .isEqualTo(HttpStatus.OK);
        assertThat(createdOrderNo).as("성공 응답에 orderNo가 포함돼야 함 — 실제 본문: %s", responseBody).isNotNull();

        int stockAfter = productDao.selectBySku(SKU).getStockQty();
        assertThat(stockAfter).as("qty=0이므로 재고는 변하지 않아야 함").isEqualTo(stockBefore);
    }
}
