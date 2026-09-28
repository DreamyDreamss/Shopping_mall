package com.sm.lab.shop;

import com.sm.lab.shop.dao.MemberDao;
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
 * 동시성 실측(SR-360 #2) — STORY "데이터" 절 "동시성 위험 인지": 상한(count&lt;4) 서브쿼리를 결합한
 * 단일 조건부 INSERT가 InnoDB REPEATABLE READ의 넥스트키 락으로 같은 member_id 범위 동시 INSERT를
 * 실제로 직렬화하는지 실측한다({@code MemberAddressConcurrencyTest} 관례 — {@link CyclicBarrier}로
 * 두 스레드가 실제로 동시에 출발하도록 강제, admin 키 + 명시 {@code ?memberId=}). 만약 이 테스트가
 * 실패하면(캡이 5개로 샌다면) {@code MemberAddressService}처럼 {@code SELECT ... FOR UPDATE}
 * 선락으로 전환하는 재작업이 필요함을 STORY가 이미 명시(사람 재확인 대상). house 관례상 이 클래스는
 * 플레이키 여부 확인을 위해 3회 단독 실행한다(사례집 SR-231 r3).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductCompareConcurrencyTest {

    private static final String ADMIN_KEY = "lab-admin-key";
    // MEMBERS.member_id는 VARCHAR(20)이라(V3__members_signup.sql) STORY 예시 ID
    // "M-9013-COMPARE-CONC-TEST"(24자)는 그대로 쓸 수 없다 — 20자 이내로 축약.
    private static final String MEMBER_ID = "M-9013-CMP-CONC-TEST";
    private static final String TEMP_SKU = "SKU-TEST-CMP-5TH";
    private static final int CONCURRENCY = 2;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 여러 스레드가 서로 다른 트랜잭션/커넥션을 쓰므로 @Transactional 자동 롤백에 기댈 수 없다 —
    // JdbcTemplate으로 직접 만들고 지운다(STORY "테스트" 절).
    //
    // round2(재작업 지시 2, QA CONCERNS medium) — @AfterEach만 있으면 실행 중단·@AfterEach 실패 시
    // MEMBER_ID/TEMP_SKU 행이 잔류해 이후 모든 실행이 이 setUp()의 INSERT 중복키로 영구 실패한다
    // (잔류 PRODUCTS 행은 SALE_YN='Y'라 /api/products 목록·값 대조 스냅샷까지 흔든다). 같은 STORY가
    // ProductCompareDaoTest에는 이미 적용한 규율(사례집 SR-356 #6 round3 — 같은 범위를 앞뒤로 두 번)을
    // 여기도 적용 — @AfterEach와 동일 범위 3문(PRODUCT_COMPARES → 임시 PRODUCTS → MEMBERS)을
    // setUp() 맨 앞에서 선행 실행한다.
    @BeforeEach
    void setUp() {
        cleanUp();

        LocalDateTime now = LocalDateTime.now();
        memberDao.insertMember(MEMBER_ID, "비교함동시성테스터", "BRONZE",
                "product-compare-conc-test@example.com", null, null, "unused-hash", false, now);
        jdbcTemplate.update(
                "INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, PRICE, STOCK_QTY, SALE_YN) VALUES (?, ?, ?, ?, ?)",
                TEMP_SKU, "Test 5th Product For Compare Concurrency", 10000, 5, "Y");
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM PRODUCT_COMPARES WHERE member_id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM PRODUCTS WHERE sku = ?", TEMP_SKU);
        memberDao.deleteById(MEMBER_ID);
    }

    // linked_tc: 카운트 3(SKU-1001~1003 선등록) 상태에서 남은 두 상품(SKU-1004, 임시 5번째)을
    // 동시 출발시켜 POST — 정확히 1건만 200, 나머지 1건은 409, 최종 행수는 4(5 아님).
    @Test
    void concurrentAdd_atThreeExisting_exactlyOneSucceedsFinalCountFour() throws InterruptedException {
        registerSequentially("SKU-1001");
        registerSequentially("SKU-1002");
        registerSequentially("SKU-1003");

        CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        futures.add(pool.submit(callAdd(barrier, "SKU-1004")));
        futures.add(pool.submit(callAdd(barrier, TEMP_SKU)));
        pool.shutdown();
        List<ResponseEntity<String>> results = collect(futures);

        long successCount = results.stream().filter(r -> r.getStatusCode().value() == 200).count();
        long conflictCount = results.stream().filter(r -> r.getStatusCode().value() == 409).count();

        assertThat(successCount)
                .as("동시 %d건 중 정확히 1건만 200이어야 함 — 응답들: %s", CONCURRENCY, results)
                .isEqualTo(1);
        assertThat(conflictCount)
                .as("나머지 %d건은 409(CMP-4091)여야 함 — 응답들: %s", CONCURRENCY - 1, results)
                .isEqualTo(CONCURRENCY - 1);

        Integer finalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCT_COMPARES WHERE member_id = ?",
                Integer.class, MEMBER_ID);
        assertThat(finalCount).as("최종 4건이어야 함(5건 아님 — 상한 직렬화 실측)").isEqualTo(4);
    }

    // round2(재작업 지시 1, QA CONCERNS medium 실측 확인) — 비교함이 비어 있는 회원이 같은 sku를
    // 더블클릭(동시 담기)하면 두 요청 다 200(멱등)이어야 하고 최종 1행이어야 한다. round1은
    // insertCompareIfUnderLimit(잠금읽기 카운트 서브쿼리) 0행 뒤 existsCompare가 비잠금
    // 일관읽기라 뒤 요청이 앞 요청의 방금 커밋된 행을 못 보고 409로 잘못 떨어졌다(QA 실측) —
    // existsCompare를 FOR UPDATE로 바꾼 뒤 이 케이스가 통과하는지로 수정을 검증한다.
    @Test
    void concurrentAdd_sameSku_bothSucceedFinalCountOne() throws InterruptedException {
        CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        futures.add(pool.submit(callAdd(barrier, "SKU-1001")));
        futures.add(pool.submit(callAdd(barrier, "SKU-1001")));
        pool.shutdown();
        List<ResponseEntity<String>> results = collect(futures);

        long successCount = results.stream().filter(r -> r.getStatusCode().value() == 200).count();

        assertThat(successCount)
                .as("같은 sku 동시 %d건은 둘 다 200(멱등)이어야 함 — 응답들: %s", CONCURRENCY, results)
                .isEqualTo(CONCURRENCY);

        Integer finalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCT_COMPARES WHERE member_id = ? AND sku = ?",
                Integer.class, MEMBER_ID, "SKU-1001");
        assertThat(finalCount).as("같은 sku가 중복 삽입되지 않고 최종 1행이어야 함").isEqualTo(1);
    }

    private void registerSequentially(String sku) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", ADMIN_KEY);
        String body = "{\"sku\":\"" + sku + "\"}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/members/me/compares?memberId=" + MEMBER_ID,
                new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode().value()).as("사전 등록 %s는 200이어야 함", sku).isEqualTo(200);
    }

    private Callable<ResponseEntity<String>> callAdd(CyclicBarrier barrier, String sku) {
        return () -> {
            awaitBarrier(barrier);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Api-Key", ADMIN_KEY);
            String body = "{\"sku\":\"" + sku + "\"}";
            return restTemplate.postForEntity(
                    "/api/members/me/compares?memberId=" + MEMBER_ID,
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
