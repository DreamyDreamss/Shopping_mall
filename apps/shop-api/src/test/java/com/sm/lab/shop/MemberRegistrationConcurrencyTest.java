// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberSignupCompletionDao;
import com.sm.lab.shop.dao.MemberSignupVerificationDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
 * 동시성 회귀 — SR-231(FUNC-member-003) STORY "테스트" 항목: "중복 409(동시 2건 중 1건만 성공
 * — CyclicBarrier로 동시성 재현)".
 *
 * <p><b>round2(2026-09-12, SR-231 round1 QA FAIL 재작업, 사람 결정) — 승자/패자 판정 지점이
 * 바뀌었다.</b> round1은 {@code markVerifiedIfCodeMatches}가 {@code verified_at IS NULL}을
 * 조건에 넣지 않아 같은 코드를 든 두 요청이 모두 "인증됨"까지 도달했고, 실제 승부는 MEMBERS의
 * {@code uq_members_email} UNIQUE 인덱스(INSERT 시점)에서 갈렸다(패자는 409 MBR-4092). round2는
 * 코드 재사용 방지(QA FAIL 필수4)를 위해 {@code markVerifiedIfCodeMatches}에
 * {@code consumed_at IS NULL AND verified_at IS NULL}을 추가했다 — 이 UPDATE는
 * {@code (channel, target)} PK 행을 잠그므로, 같은 코드를 공유하는 두 동시 요청은 이제 이
 * "코드 검증" 단계 자체에서 InnoDB 행 락으로 직렬화된다: 먼저 락을 잡은 트랜잭션만
 * {@code verified_at}을 세팅하고(그 트랜잭션이 끝날 때까지 락 보유 — {@code @Transactional}
 * 전체가 한 트랜잭션이므로 INSERT까지 마칠 때까지 잠긴다), 뒤이은 트랜잭션의 같은 UPDATE는
 * 그동안 블록됐다가 승자가 커밋한 뒤 {@code verified_at IS NOT NULL}을 보고 0행에 그친다 —
 * 즉 패자는 이제 MEMBERS INSERT 단계에 도달조차 못 하고 409 {@code MBR-4091}("인증이
 * 필요합니다")로 거부된다. 이 테스트는 "정확히 1건만 성공"이라는 STORY 요구는 그대로 지키되,
 * 그 직렬화 지점이 인증 테이블(패자=MBR-4091)로 옮겨졌다는 사실을 반영해 갱신했다 — 두 보호막
 * (코드 1회 소비 + 이메일 UNIQUE 인덱스)이 방어 심층화로 함께 남아 있다.
 *
 * <p><b>round3(2026-09-12, round2 QA FAIL 재작업 지시, 사람 결정) 갱신</b> — 코드 검증
 * (STEP1)이 더 이상 "가입"(STEP2, MEMBERS INSERT 포함)과 같은 트랜잭션에 묶이지 않는다(비트랜잭션
 * autocommit 단일 문장 — {@code MemberRegistrationService} javadoc 참고). 그래도 승부가 갈리는
 * 지점과 관찰 가능한 결과(정확히 1건만 201, 나머지 409 MBR-4091)는 바뀌지 않는다 — InnoDB는
 * 단일 UPDATE 문장이라도 그 문장이 커밋될 때까지는 여전히 행 락을 쥐므로, 동시에 도착한 두
 * 요청 중 하나는 그 짧은 락 구간 동안 블록됐다가 승자가 커밋한 뒤에야 {@code verified_at IS
 * NOT NULL}을 보고 0행에 그친다. 다만 이제 그 락은 INSERT까지 포함한 긴 트랜잭션이 아니라
 * 이 UPDATE 문장 하나의 수명 동안만 유지된다(락 보유 구간이 round2보다 더 짧아졌다).
 *
 * <p>{@link MemberSignupRateLimitConcurrencyTest}(FUNC-member-002)와 동일하게
 * {@link CyclicBarrier}로 두 스레드가 실제로 동시에 요청을 던지도록 강제한다(CountDownLatch로
 * "거의 동시"를 흉내내면 JVM 워밍업 때문에 순차 처리로 겹치지 않을 위험이 있다는 것이 그 클래스의
 * 실측 교훈).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberRegistrationConcurrencyTest {

    private static final String CHANNEL = "EMAIL";
    private static final String TARGET = "concurrent-signup@example.com";
    private static final String CODE = "654321";
    private static final int CONCURRENCY = 2;

    // SR-298(신규) — 오답 버스트 동시성 테스트 전용 target. 위 TARGET(정답 코드 동시성)과
    // 겹치지 않는 별도 리터럴을 써서 두 테스트의 verification 행이 서로 간섭하지 않는다.
    private static final String WRONGCODE_TARGET = "concurrent-wrongcode@example.com";
    private static final int MAX_ATTEMPTS = 5; // MemberRegistrationService.MAX_VERIFY_ATTEMPTS와 동일

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MemberSignupVerificationDao verificationDao;

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private MemberSignupCompletionDao completionDao;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<String> createdMemberIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        verificationDao.deleteByChannelAndTarget(CHANNEL, TARGET);
        // 다른 테스트가 WRONGCODE_TARGET을 안 써도 삭제 0행이라 무해(SR-298 — 테스트 격리).
        verificationDao.deleteByChannelAndTarget(CHANNEL, WRONGCODE_TARGET);
        // 성공한 요청이 만든 회원 행을 정리한다 — memberId는 실행마다 채번이 달라 응답에서 얻는다.
        for (String memberId : createdMemberIds) {
            memberDao.deleteById(memberId);
        }
        createdMemberIds.clear();
    }

    // linked_tc: TC-FUNC-member-003-06 — 완료 조건(round2 갱신): 동시 2건 중 정확히 1건만 201,
    // 나머지는 409 MBR-4091(코드 재사용 방지 — 클래스 javadoc round2 절 참고), 500은 0건.
    @Test
    void concurrentSignUp_sameEmail_exactlyOneSucceeds() throws InterruptedException {
        LocalDateTime now = LocalDateTime.now();
        verificationDao.writeCode(CHANNEL, TARGET, CODE, now.plusMinutes(5), now);

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY);

        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY; i++) {
            futures.add(pool.submit(callSignUp(barrier)));
        }
        pool.shutdown();

        List<ResponseEntity<String>> results = new ArrayList<>();
        for (Future<ResponseEntity<String>> f : futures) {
            try {
                results.add(f.get(15, TimeUnit.SECONDS));
            } catch (ExecutionException | TimeoutException e) {
                throw new AssertionError("동시 가입 요청 실행 실패", e);
            }
        }

        assertThat(results).hasSize(CONCURRENCY);

        long successCount = results.stream().filter(r -> r.getStatusCode().value() == 201).count();
        long conflictCount = results.stream().filter(r -> r.getStatusCode().value() == 409).count();
        long serverErrorCount = results.stream().filter(r -> r.getStatusCode().is5xxServerError()).count();

        assertThat(successCount)
                .as("동시 %d건 중 정확히 1건만 201이어야 함 — 응답들: %s", CONCURRENCY, results)
                .isEqualTo(1);
        assertThat(conflictCount)
                .as("나머지 %d건은 409여야 함 — 응답들: %s", CONCURRENCY - 1, results)
                .isEqualTo(CONCURRENCY - 1);
        assertThat(serverErrorCount).as("500이 0건이어야 함 — 응답들: %s", results).isEqualTo(0);

        for (ResponseEntity<String> res : results) {
            if (res.getStatusCode().value() == 201) {
                createdMemberIds.add(readMemberId(res.getBody()));
            } else {
                // round2 — 패자는 이제 코드 검증 단계(consumed_at/verified_at IS NULL 게이트)에서
                // 걸린다(클래스 javadoc round2 절 참고) — MEMBERS INSERT에 도달하지 못하므로
                // login_url이 있는 MBR-4092가 아니라 MBR-4091이어야 한다.
                assertThat(res.getBody()).contains("\"code\":\"MBR-4091\"");
            }
        }
    }

    private String readMemberId(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            return node.get("memberId").asText();
        } catch (Exception e) {
            throw new AssertionError("응답에서 memberId를 읽을 수 없음: " + body, e);
        }
    }

    private Callable<ResponseEntity<String>> callSignUp(CyclicBarrier barrier) {
        return () -> {
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (BrokenBarrierException | TimeoutException e) {
                throw new IllegalStateException("CyclicBarrier 대기 실패", e);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"target\":\"" + TARGET + "\",\"code\":\"" + CODE + "\","
                    + "\"password\":\"abcd1234\",\"name\":\"동시가입\"}";
            return restTemplate.postForEntity("/api/members/signup", new HttpEntity<>(body, headers), String.class);
        };
    }

    // SR-298(신규, 이 SR의 회귀 대상 그 자체) — 상한(5)보다 많은 동시 스레드가 같은 (channel,
    // target)에 오답 코드를 동시에 제출한다. AS-IS는 incrementAttemptCount 호출 전에 별도로
    // selectAttemptCount를 읽어 상한 여부를 판정했는데(select→분기→update), 그 두 문장 사이
    // 간극에서 동시에 도착한 요청들이 모두 "아직 상한 미만"으로 읽어 attempt_count가 5를
    // 초과해 늘어날 수 있었다 — TO-BE는 incrementAttemptCount 자체가 원자 조건부 UPDATE라서
    // 이 레이스가 불가능해야 한다: 정확히 5건만 증가에 성공(MBR-4091), 나머지는 상한 도달
    // (MBR-4093), 500은 0건, 완료 후 attempt_count는 정확히 5(그 이상도 이하도 아님).
    @Test
    void concurrentWrongCode_burstBeyondCap_doesNotExceedMaxAttempts() throws InterruptedException {
        int burstSize = 10;
        LocalDateTime now = LocalDateTime.now();
        verificationDao.writeCode(CHANNEL, WRONGCODE_TARGET, "111111", now.plusMinutes(5), now);

        ExecutorService pool = Executors.newFixedThreadPool(burstSize);
        CyclicBarrier barrier = new CyclicBarrier(burstSize);

        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        for (int i = 0; i < burstSize; i++) {
            futures.add(pool.submit(callSignUpWrongCode(barrier)));
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

        long verifyRequiredCount = results.stream()
                .filter(r -> r.getStatusCode().value() == 409 && r.getBody() != null
                        && r.getBody().contains("\"code\":\"MBR-4091\""))
                .count();
        long verifyLockedCount = results.stream()
                .filter(r -> r.getStatusCode().value() == 409 && r.getBody() != null
                        && r.getBody().contains("\"code\":\"MBR-4093\""))
                .count();
        long serverErrorCount = results.stream().filter(r -> r.getStatusCode().is5xxServerError()).count();

        assertThat(verifyRequiredCount)
                .as("정확히 상한(%d)건만 증가에 성공해 MBR-4091이어야 함 — 응답들: %s", MAX_ATTEMPTS, results)
                .isEqualTo(MAX_ATTEMPTS);
        assertThat(verifyLockedCount)
                .as("나머지 %d건은 상한 도달로 MBR-4093이어야 함 — 응답들: %s", burstSize - MAX_ATTEMPTS, results)
                .isEqualTo(burstSize - MAX_ATTEMPTS);
        assertThat(serverErrorCount).as("500이 0건이어야 함 — 응답들: %s", results).isEqualTo(0);
        assertThat(completionDao.selectAttemptCount(CHANNEL, WRONGCODE_TARGET))
                .as("동시 버스트 후에도 attempt_count는 상한(%d)을 넘지 않아야 함", MAX_ATTEMPTS)
                .isEqualTo(MAX_ATTEMPTS);
    }

    private Callable<ResponseEntity<String>> callSignUpWrongCode(CyclicBarrier barrier) {
        return () -> {
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (BrokenBarrierException | TimeoutException e) {
                throw new IllegalStateException("CyclicBarrier 대기 실패", e);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"target\":\"" + WRONGCODE_TARGET + "\",\"code\":\"999999\","
                    + "\"password\":\"abcd1234\",\"name\":\"오답버스트\"}";
            return restTemplate.postForEntity("/api/members/signup", new HttpEntity<>(body, headers), String.class);
        };
    }
}
