package com.sm.lab.shop;

import com.sm.lab.shop.dao.MemberPasswordResetDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
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
 * SR-298(SR-298.2) 동시성 회귀 — 이 SR이 메우는 핵심 공백. {@link MemberRegistrationConcurrencyTest}
 * #concurrentWrongCode_burstBeyondCap_doesNotExceedMaxAttempts와 동일 패턴(실 서버 RANDOM_PORT,
 * {@link CyclicBarrier}로 동시 도착 강제)을 {@code POST /api/members/password-resets/confirmations}에
 * 적용한다.
 *
 * <p>AS-IS 결함 — {@code handleConfirmFailure}가 {@code selectByTarget}로 먼저 읽어 상한
 * (attempt_count &gt;= 5)을 판정한 뒤에야 {@code incrementAttemptCount}를 호출했다(read-modify-write).
 * 그 사이 간극에서 동시에 도착한 오답 요청들이 모두 "아직 상한 미만"으로 읽어 attempt_count가 5를
 * 초과해 늘어날 수 있었다. TO-BE는 {@code incrementAttemptCount} 자체가
 * {@code attempt_count < maxAttempts}를 조건으로 갖는 원자 UPDATE라서 이 레이스가 불가능해야
 * 한다 — 정확히 5건만 증가에 성공(MBR-4102), 나머지는 상한 도달(MBR-4103), 500은 0건, 완료 후
 * DB attempt_count는 정확히 5(그 이상도 이하도 아님)임을 실측으로 증명한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberPasswordResetConfirmationConcurrencyTest {

    private static final String CONFIRM_PATH = "/api/members/password-resets/confirmations";
    private static final int COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5; // MemberPasswordResetConfirmationService.MAX_CONFIRM_ATTEMPTS와 동일

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MemberPasswordResetDao passwordResetDao;

    // 매 테스트가 새 UUID 접미 target을 만들어 채운다(SR-232 r2 재발 방지 — 리터럴 재사용 시
    // attempt_count가 실행을 가로질러 누적되는 것을 피한다).
    private String concurrencyTarget;

    @AfterEach
    void cleanUp() {
        if (concurrencyTarget != null) {
            passwordResetDao.deleteByTarget(concurrencyTarget);
        }
    }

    // linked_tc 없음(v5, story 도시에가 기준) — 완료 조건: 동시 10건 중 정확히 5건만 409 MBR-4102
    // (증가 성공), 나머지 5건은 409 MBR-4103(상한 도달, 미증가), 500 0건, 최종 attempt_count는
    // 정확히 5.
    @Test
    void concurrentWrongCode_burstBeyondCap_doesNotExceedMaxAttempts() throws InterruptedException {
        int burstSize = 10;
        concurrencyTarget = "pwreset-confirm-burst-" + UUID.randomUUID() + "@example.com";
        LocalDateTime now = LocalDateTime.now();
        passwordResetDao.touchRequest(concurrencyTarget, sha256Hex("246810"), now.plusMinutes(10), now,
                COOLDOWN_SECONDS);

        ExecutorService pool = Executors.newFixedThreadPool(burstSize);
        CyclicBarrier barrier = new CyclicBarrier(burstSize);

        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        for (int i = 0; i < burstSize; i++) {
            futures.add(pool.submit(callConfirmWrongCode(barrier)));
        }
        pool.shutdown();

        List<ResponseEntity<String>> results = new ArrayList<>();
        for (Future<ResponseEntity<String>> f : futures) {
            try {
                results.add(f.get(15, TimeUnit.SECONDS));
            } catch (ExecutionException | TimeoutException e) {
                throw new AssertionError("동시 오답 버스트 실행 실패", e);
            }
        }

        assertThat(results).hasSize(burstSize);

        long mismatchCount = results.stream()
                .filter(r -> r.getStatusCode().value() == 409 && r.getBody() != null
                        && r.getBody().contains("\"code\":\"MBR-4102\""))
                .count();
        long lockedCount = results.stream()
                .filter(r -> r.getStatusCode().value() == 409 && r.getBody() != null
                        && r.getBody().contains("\"code\":\"MBR-4103\""))
                .count();
        long serverErrorCount = results.stream().filter(r -> r.getStatusCode().is5xxServerError()).count();

        assertThat(mismatchCount)
                .as("정확히 상한(%d)건만 증가에 성공해 MBR-4102여야 함 — 응답들: %s", MAX_ATTEMPTS, results)
                .isEqualTo(MAX_ATTEMPTS);
        assertThat(lockedCount)
                .as("나머지 %d건은 상한 도달로 MBR-4103이어야 함 — 응답들: %s", burstSize - MAX_ATTEMPTS, results)
                .isEqualTo(burstSize - MAX_ATTEMPTS);
        assertThat(serverErrorCount).as("500이 0건이어야 함 — 응답들: %s", results).isEqualTo(0);
        assertThat(passwordResetDao.selectByTarget(concurrencyTarget).getAttemptCount())
                .as("동시 버스트 후에도 attempt_count는 상한(%d)을 넘지 않아야 함", MAX_ATTEMPTS)
                .isEqualTo(MAX_ATTEMPTS);
    }

    private Callable<ResponseEntity<String>> callConfirmWrongCode(CyclicBarrier barrier) {
        return () -> {
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (BrokenBarrierException | TimeoutException e) {
                throw new IllegalStateException("CyclicBarrier 대기 실패", e);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = String.format(
                    "{\"target\":\"%s\",\"code\":\"999999\",\"newPassword\":\"newpass456\"}", concurrencyTarget);
            return restTemplate.postForEntity(CONFIRM_PATH, new HttpEntity<>(body, headers), String.class);
        };
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
