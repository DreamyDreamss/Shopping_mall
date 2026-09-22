// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop;

import com.sm.lab.shop.dao.MemberSignupRateLimitDao;
import com.sm.lab.shop.dao.MemberSignupVerificationDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
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
 * 동시성 회귀 — SR-231(FUNC-member-002) round4 재작업(STORY 재작업 지시 (D)·(E)). 같은 target으로
 * N=5 동시 인증코드 요청 시 정확히 1건만 200(실제 코드 발송), 나머지 4건은 429, 500은 0건이어야
 * 한다.
 *
 * <p>round3는 이 시나리오에서 4건이 429가 아니라 HTTP 500(InnoDB Deadlock)으로 떨어졌다(QA가
 * 단독 재실행 3회 모두 재현). 원인은 (a) 카운터·코드가 같은 테이블 같은 행에 있어 락이 섞였고,
 * (b) 요청 트랜잭션 안에서 도는 purgeExpired(무인덱스 풀스캔)가 락 범위를 테이블 전체로
 * 확대했다. round4(사람 결정 — "설계를 단순화해서 데드락 원인을 없앤다")는 카운터를 전용
 * 테이블로 분리하고, 요청 경로의 {@code @Transactional}과 purge를 모두 제거했다.
 *
 * <p>round3의 회귀 테스트는 {@code CountDownLatch}(ready/start)로 "거의 동시"를 흉내 냈지만
 * 전체 스위트 안에서는 JVM·JDBC·MyBatis 워밍업 때문에 요청이 사실상 순차 처리돼 겹치지
 * 않았다(타이밍 의존 위양성 — QA FAIL round3 필수2). 이 재작업은 {@code CyclicBarrier}로
 * 5개 스레드가 실제로 동시에 요청을 던지도록 강제하고, 완료 조건("단독 실행 3회 연속 통과")을
 * 충족하기 위해 같은 테스트 메서드 안에서 3회 반복한다(반복마다 관련 행을 먼저 삭제).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberSignupRateLimitConcurrencyTest {

    private static final String CHANNEL = "EMAIL";
    private static final String TARGET = "signup-race@example.com";
    private static final int CONCURRENCY = 5;
    private static final int REPEAT = 3;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MemberSignupVerificationDao verificationDao;

    @Autowired
    private MemberSignupRateLimitDao rateLimitDao;

    @AfterEach
    void cleanUp() {
        cleanRelatedRows();
    }

    private void cleanRelatedRows() {
        verificationDao.deleteByChannelAndTarget(CHANNEL, TARGET);
        rateLimitDao.deleteRateLimit(TARGET, LocalDate.now());
    }

    // linked_tc: TC-FUNC-member-002-12 — 완료 조건(STORY 재작업 지시(E)): mvnw test 전체
    // 통과 + 이 테스트를 단독 실행(-Dtest=MemberSignupRateLimitConcurrencyTest)해도 매번
    // 통과해야 한다(위양성 아님을 스스로 증명하도록 같은 메서드 안에서 3회 반복).
    @Test
    void concurrentRequests_fiveParallel_exactlyOneSucceedsRestAreTooManyRequests_repeated3Times()
            throws InterruptedException {
        for (int iteration = 1; iteration <= REPEAT; iteration++) {
            cleanRelatedRows(); // 사람 지시(D) — 각 반복 전 관련 행 삭제
            runOneRound(iteration);
        }
    }

    private void runOneRound(int iteration) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY);

        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY; i++) {
            futures.add(pool.submit(callSendVerificationCode(barrier)));
        }
        pool.shutdown();

        List<ResponseEntity<String>> results = new ArrayList<>();
        for (Future<ResponseEntity<String>> f : futures) {
            try {
                results.add(f.get(15, TimeUnit.SECONDS));
            } catch (ExecutionException | TimeoutException e) {
                throw new AssertionError("iteration #" + iteration + " — 동시 인증코드 요청 실행 실패", e);
            }
        }

        assertThat(results).as("iteration #%d 응답 개수", iteration).hasSize(CONCURRENCY);

        long successCount = results.stream().filter(r -> r.getStatusCode().is2xxSuccessful()).count();
        long tooManyRequestsCount = results.stream().filter(r -> r.getStatusCode().value() == 429).count();
        long serverErrorCount = results.stream().filter(r -> r.getStatusCode().is5xxServerError()).count();

        assertThat(successCount)
                .as("iteration #%d — 동시 N=%d건 중 정확히 1건만 200이어야 함(나머지는 429) — 응답들: %s",
                        iteration, CONCURRENCY, results)
                .isEqualTo(1);
        assertThat(tooManyRequestsCount)
                .as("iteration #%d — 나머지 %d건은 429여야 함 — 응답들: %s",
                        iteration, CONCURRENCY - 1, results)
                .isEqualTo(CONCURRENCY - 1);
        assertThat(serverErrorCount)
                .as("iteration #%d — 500(데드락 등)이 0건이어야 함 — 응답들: %s", iteration, results)
                .isEqualTo(0);
    }

    private Callable<ResponseEntity<String>> callSendVerificationCode(CyclicBarrier barrier) {
        return () -> {
            try {
                barrier.await(10, TimeUnit.SECONDS); // 사람 지시(D) — 실제 동시 진입 강제
            } catch (BrokenBarrierException | TimeoutException e) {
                throw new IllegalStateException("CyclicBarrier 대기 실패", e);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"target\":\"" + TARGET + "\"}";
            return restTemplate.postForEntity("/api/members/signup/verification-codes",
                    new HttpEntity<>(body, headers), String.class);
        };
    }
}
