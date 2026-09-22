// linked_func: FUNC-order-011
// spec: docs/00_FUNC/stories/STORY-FUNC-order-011.md
package com.sm.lab.shop;

import com.sm.lab.shop.dao.CartDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 회귀 — SR-202 QA FAIL round1(#1, 재작업 지시 #1): 같은 (member,sku)로 N=8 병렬
 * 담기 시 500이 0건이고 최종 qty가 Σqty와 정확히 일치해야 한다(D2 UPSERT 합산 원자성).
 * round1에서는 select→분기 read-modify-write로 200 5건/500 3건, 최종 qty=2(기대 8)로 재현됐다.
 * 실 서버(RANDOM_PORT)+실 DB(sl_lab) 대상 — 테스트 후 삽입분을 정리한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CartConcurrencyTest {

    private static final String MEMBER_ID = "M-0001";
    private static final String SKU = "SKU-1002"; // 기계식 키보드, 재고 40 — 8×qty1 담기에 여유
    private static final int CONCURRENCY = 8;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CartDao cartDao;

    @AfterEach
    void cleanUp() {
        cartDao.deleteItem(MEMBER_ID, SKU);
    }

    @Test
    void concurrentAddItem_eightParallel_noServerErrorsAndQtySummed() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch ready = new CountDownLatch(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);

        // ⚠ invokeAll은 전 태스크 완료까지 블록하므로 start 래치를 풀 기회가 없다 —
        //   r2 최초본이 정확히 그 조합(submit 없이 invokeAll + 아무도 start.countDown() 안 함)으로
        //   테스트 스위트를 영구 행에 빠뜨렸다(오케스트레이터 실측: DB trx 0·전 스레드 await 대기).
        //   submit으로 흩뿌린 뒤 ready 정족 → start 해제 순서로 동시 출발시킨다.
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
                String body = "{\"memberId\":\"" + MEMBER_ID + "\",\"sku\":\"" + SKU + "\",\"qty\":1}";
                return restTemplate.postForEntity("/api/cart/items", new HttpEntity<>(body, headers), String.class);
            }));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).as("워커 준비 정족").isTrue();
        start.countDown();
        pool.shutdown();

        List<ResponseEntity<String>> results = new ArrayList<>();
        for (Future<ResponseEntity<String>> f : futures) {
            try {
                results.add(f.get(15, TimeUnit.SECONDS));
            } catch (ExecutionException | TimeoutException e) {
                throw new AssertionError("동시 담기 요청 실행 실패", e);
            }
        }

        assertThat(results).hasSize(CONCURRENCY);
        for (ResponseEntity<String> r : results) {
            assertThat(r.getStatusCode().is2xxSuccessful())
                    .as("500/기타 오류 유출 없이 전부 2xx여야 함(QA FAIL round1 재현: 200 5건/500 3건) — 실제 응답: %s %s",
                            r.getStatusCode(), r.getBody())
                    .isTrue();
        }

        int finalQty = cartDao.selectItem(MEMBER_ID, SKU).getQty();
        assertThat(finalQty)
                .as("동시 담기 N=%d건의 합산 결과가 정확해야 함(round1 재현: 기대 8, 실제 2 — lost update)", CONCURRENCY)
                .isEqualTo(CONCURRENCY);
    }
}
