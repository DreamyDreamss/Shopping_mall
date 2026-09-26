package com.sm.lab.shop;

import com.sm.lab.shop.dao.MemberApiKeyDao;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.OrderDao;
import com.sm.lab.shop.domain.Order;
import com.sm.lab.shop.domain.OrderItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 회귀 — SR-351(#1) STORY "테스트" 절: 같은 (memberId, orderNo, sku)로 동시 2회 POST →
 * 정확히 1건만 201, 나머지 1건은 409(active_key 생성열 UNIQUE 인덱스가 실제로 레이스를 막는지
 * 실 DB로 재현, 사례집 SR-231 r3 — 3회 단독 실행이 house 관례).
 *
 * <p>{@link CyclicBarrier}로 두 스레드가 실제로 동시에 출발하도록 강제한다
 * ({@code MemberAddressConcurrencyTest}와 동일 근거).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductReviewConcurrencyTest {

    private static final String MEMBER_ID = "M-9011-RVWCONC-TST";
    private static final String API_KEY = "db-issued-review-conc-test-key";
    private static final String ORDER_NO = "20260904-RVWCONCTST";
    private static final String SKU = "SKU-1001";
    private static final int CONCURRENCY = 2;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private OrderDao orderDao;
    @Autowired
    private MemberApiKeyDao memberApiKeyDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        memberDao.insertMember(MEMBER_ID, "동시성상품평테스터", "BRONZE",
                "product-review-conc-test@example.com", null, null, "unused-hash", false, now);
        memberApiKeyDao.issueIfAbsent(MEMBER_ID, API_KEY, now);

        Order order = new Order();
        order.setOrderNo(ORDER_NO);
        order.setMemberId(MEMBER_ID);
        order.setOrderState("DONE");
        order.setTotalAmount(10000);
        orderDao.insertOrder(order);

        OrderItem item = new OrderItem();
        item.setOrderNo(ORDER_NO);
        item.setLineNo(1);
        item.setSku(SKU);
        item.setQty(1);
        item.setUnitPrice(10000);
        orderDao.insertItem(item);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM PRODUCT_REVIEWS WHERE order_no = ?", ORDER_NO);
        jdbcTemplate.update("DELETE FROM ORDER_ITEMS WHERE order_no = ?", ORDER_NO);
        jdbcTemplate.update("DELETE FROM ORDERS WHERE order_no = ?", ORDER_NO);
        jdbcTemplate.update("DELETE FROM MEMBER_API_KEYS WHERE api_key = ?", API_KEY);
        memberDao.deleteById(MEMBER_ID);
    }

    @Test
    void concurrentCreate_sameOrderAndSku_exactlyOneSucceedsOtherConflicts() throws InterruptedException {
        CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY; i++) {
            futures.add(pool.submit(callCreate(barrier)));
        }
        pool.shutdown();
        List<ResponseEntity<String>> results = collect(futures);

        long successCount = results.stream().filter(r -> r.getStatusCode().value() == 201).count();
        long conflictCount = results.stream().filter(r -> r.getStatusCode().value() == 409).count();

        assertThat(successCount)
                .as("동시 %d건 중 정확히 1건만 201이어야 함 — 응답들: %s", CONCURRENCY, results)
                .isEqualTo(1);
        assertThat(conflictCount)
                .as("나머지 %d건은 409(RVW-4091)여야 함 — 응답들: %s", CONCURRENCY - 1, results)
                .isEqualTo(CONCURRENCY - 1);

        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCT_REVIEWS WHERE order_no = ? AND sku = ? AND del_yn = 'N'",
                Integer.class, ORDER_NO, SKU);
        assertThat(activeCount).as("최종 활성 리뷰는 정확히 1건이어야 함").isEqualTo(1);
    }

    private Callable<ResponseEntity<String>> callCreate(CyclicBarrier barrier) {
        return () -> {
            awaitBarrier(barrier);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Api-Key", API_KEY);
            String body = "{\"rating\":5,\"content\":\"동시성 테스트 상품평 본문입니다.\",\"orderNo\":\"" + ORDER_NO + "\"}";
            return restTemplate.postForEntity(
                    "/api/products/" + SKU + "/reviews",
                    new HttpEntity<>(body, headers), String.class);
        };
    }

    private static void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (BrokenBarrierException | TimeoutException | InterruptedException e) {
            throw new IllegalStateException("CyclicBarrier 대기 실패", e);
        }
    }

    private static List<ResponseEntity<String>> collect(List<Future<ResponseEntity<String>>> futures) {
        List<ResponseEntity<String>> results = new ArrayList<>();
        for (Future<ResponseEntity<String>> f : futures) {
            try {
                results.add(f.get(15, TimeUnit.SECONDS));
            } catch (ExecutionException | TimeoutException | InterruptedException e) {
                throw new AssertionError("동시 요청 실행 실패", e);
            }
        }
        return results;
    }
}
