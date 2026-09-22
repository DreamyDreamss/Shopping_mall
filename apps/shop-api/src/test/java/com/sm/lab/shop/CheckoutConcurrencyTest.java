// linked_func: FUNC-order-012
// spec: docs/00_FUNC/stories/STORY-FUNC-order-012.md
package com.sm.lab.shop;

import com.sm.lab.shop.dao.CartDao;
import com.sm.lab.shop.dao.OrderDao;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시 체크아웃 중복 주문 차단 회귀(SR-203 QA FAIL round1 필수1·필수2, 재작업 지시 #1/#2).
 *
 * round1은 {@code CartService.checkout}이 {@code cartDao.selectItems}(비잠금 일관읽기) →
 * {@code OrderService.create} → {@code cartDao.deleteAllItems}의 애플리케이션 read-modify-write였다.
 * MariaDB 기본 REPEATABLE-READ에서 각 트랜잭션이 자기 스냅샷의 장바구니를 보므로 서로를 막지 못해,
 * 담긴 수량 1개짜리 장바구니에 동시요청 4건을 넣었더니 [200 200 200 200](주문 4건·재고 4 차감)이
 * 10/10 라운드 재현됐다(docs/project-context.md Critical Rule #3 위반, SR-202 round1 QA FAIL 재발).
 *
 * r2는 {@link CartDao#selectItemsForUpdate}(SELECT...FOR UPDATE)로 장바구니 행을 먼저 잠근 뒤
 * 진행한다 — InnoDB 락킹 리드는 트랜잭션 스냅샷이 아니라 최신 커밋 데이터를 읽고 잠그므로, 선행
 * 트랜잭션이 이 행을 삭제·커밋하면 잠금 대기 중이던 후속 트랜잭션은 잠금 해제 후 빈 결과를 보고
 * 400(빈 장바구니)으로 거부된다. N=8 동시 체크아웃 → 성공(2xx) 정확히 1건·나머지 400, 주문 정확히
 * 1건, 재고 정확히 1개 차감만 발생해야 한다(CartConcurrencyTest 관례 준수).
 *
 * 실 서버(RANDOM_PORT)+실 DB(sl_lab) 대상 — 테스트 후 생성된 주문·차감된 재고·삽입분 장바구니를
 * 전부 원복한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CheckoutConcurrencyTest {

    private static final String MEMBER_ID = "M-0001";
    private static final String SKU = "SKU-1002"; // 기계식 키보드, 재고 40 — 1개 차감에 여유
    private static final int CONCURRENCY = 8;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private CartDao cartDao;
    @Autowired
    private ProductDao productDao;
    @Autowired
    private OrderDao orderDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> createdOrderNos = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        cartDao.deleteItem(MEMBER_ID, SKU);
        for (String orderNo : createdOrderNos) {
            jdbcTemplate.update("DELETE FROM ORDER_ITEMS WHERE order_no = ?", orderNo);
            jdbcTemplate.update("DELETE FROM ORDERS WHERE order_no = ?", orderNo);
        }
        if (!createdOrderNos.isEmpty()) {
            productDao.increaseStock(SKU, createdOrderNos.size());
        }
        createdOrderNos.clear();
    }

    @Test
    void concurrentCheckout_eightParallel_onlyOneSucceedsExactlyOneOrderAndStockAccurate() throws InterruptedException {
        cartDao.insertItem(MEMBER_ID, SKU, 1);
        int stockBefore = productDao.selectBySku(SKU).getStockQty();
        int ordersBefore = orderDao.countOrders(MEMBER_ID, null, null, null);

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch ready = new CountDownLatch(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);

        // ⚠ CartConcurrencyTest 선례(사고): invokeAll은 전 태스크 완료까지 블록해 start 래치를 풀
        //   기회가 없다 — submit으로 흩뿌린 뒤 ready 정족 → start.countDown()으로 동시 출발시킨다.
        //   start.countDown() 누락 금지.
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("start 래치가 풀리지 않음");
                }
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Api-Key", "lab-admin-key"); // SR-204 R-1: 인증 필터 도입 후 필요
                String body = "{\"memberId\":\"" + MEMBER_ID + "\"}";
                return restTemplate.postForEntity("/api/cart/checkout", new HttpEntity<>(body, headers), String.class);
            }));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).as("워커 준비 정족").isTrue();
        start.countDown();
        pool.shutdown();

        List<ResponseEntity<String>> results = new ArrayList<>();
        for (Future<ResponseEntity<String>> f : futures) {
            try {
                results.add(f.get(20, TimeUnit.SECONDS));
            } catch (ExecutionException | TimeoutException e) {
                throw new AssertionError("동시 체크아웃 요청 실행 실패", e);
            }
        }
        assertThat(results).hasSize(CONCURRENCY);

        List<ResponseEntity<String>> succeeded = results.stream()
                .filter(r -> r.getStatusCode().is2xxSuccessful())
                .collect(Collectors.toList());
        List<ResponseEntity<String>> rejected = results.stream()
                .filter(r -> r.getStatusCode() == HttpStatus.BAD_REQUEST)
                .collect(Collectors.toList());

        // 정리 대상 등록은 단언보다 먼저 — 회귀(2xx 다건)가 재발한 그 순간에 단언이 던지면
        // 등록이 건너뛰어져 공용 DB에 주문·차감이 잔류한다(QA r2 권고 #1, 변이 검증에서
        // 주문 8건·재고 8 차감 잔류 실측). 성공 응답 전부를 즉시 등록해 실패해도 원복되게 한다.
        for (ResponseEntity<String> ok : succeeded) {
            String no = ok.getBody() == null ? ""
                    : ok.getBody().replaceAll(".*\"orderNo\":\"([^\"]+)\".*", "$1");
            if (!no.isBlank() && !no.equals(ok.getBody())) {
                createdOrderNos.add(no);
            }
        }

        assertThat(succeeded)
                .as("동시 체크아웃 %d건 중 정확히 1건만 성공해야 함(round1 재현: 4건 동시요청 전부 200) "
                                + "— 실제 상태코드 분포: %s",
                        CONCURRENCY,
                        results.stream().map(r -> r.getStatusCode().toString()).collect(Collectors.toList()))
                .hasSize(1);
        assertThat(rejected)
                .as("나머지는 잠금 해제 후 빈 장바구니를 보고 400으로 거부돼야 함")
                .hasSize(CONCURRENCY - 1);

        String successBody = succeeded.get(0).getBody();
        assertThat(createdOrderNos)
                .as("성공 응답에 orderNo가 포함돼야 함 — 실제 본문: %s", successBody)
                .hasSize(1);

        assertThat(orderDao.countOrders(MEMBER_ID, null, null, null))
                .as("주문이 정확히 1건만 생성돼야 함(round1 재현: 4건 중복 생성)")
                .isEqualTo(ordersBefore + 1);

        int stockAfter = productDao.selectBySku(SKU).getStockQty();
        assertThat(stockAfter)
                .as("재고는 성공 1건분(qty=1)만 차감돼야 함(round1 재현: 4 차감)")
                .isEqualTo(stockBefore - 1);

        assertThat(cartDao.selectItem(MEMBER_ID, SKU))
                .as("체크아웃 성공 후 장바구니는 비어 있어야 함")
                .isNull();
    }
}
