// linked_func: FUNC-member-005
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md
package com.sm.lab.shop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sm.lab.shop.dao.MemberApiKeyDao;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberLoginAttemptDao;
import com.sm.lab.shop.domain.MemberLoginAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * SR-232(FUNC-member-005) 동시성 회귀 — STORY "데이터" 절이 요구한 두 가지를 실 DB(RANDOM_PORT)로
 * 검증한다: (1) 동일 email 동시 5회 실패 → fail_count=5·locked_until 세팅(lost update 없음),
 * (2) 동시 최초 로그인에서 api_key가 정확히 1개만 발급되는지. {@code MemberSignupRateLimitConcurrencyTest}
 * 와 동일 house 패턴(CyclicBarrier로 실제 동시 진입 강제, 완료 조건 — 단독 3회 반복 실행 모두 통과).
 *
 * <p><b>{@code TestRestTemplate} 대신 {@link HttpClient}를 쓰는 이유</b> — 이 FUNC은 최초 실패에서
 * 401을 응답하는데, {@code TestRestTemplate}의 기본 {@code SimpleClientHttpRequestFactory}(JDK
 * {@code HttpURLConnection} 기반)는 POST + 401 조합에서 "cannot retry due to server
 * authentication, in streaming mode"(JDK 인증 재시도 로직과 스트리밍 요청 바디의 충돌, WWW-Authenticate
 * 헤더 유무와 무관)를 던진다(실측) — {@code ApiKeyAuthIntegrationTest}가 PATCH에 JDK
 * {@link HttpClient}(rawRequest)를 쓰는 것과 동일한 이유의 회피책이다.
 *
 * <p>응답 코드(401 vs 429) 자체는 이 테스트에서 단언하지 않는다 — {@link MemberLoginService}가
 * "원자 UPSERT(쓰기) 이후 별도 SELECT(읽기)"로 표시용 실패 횟수를 다시 읽으므로, 동시 요청
 * 사이에서 어느 요청이 몇 번째로 잠금을 관측하는지는 타이밍에 따라 갈릴 수 있다(쓰기 자체는
 * 원자적이라 데이터 정합성은 항상 보장된다). 그래서 이 테스트는 DB 최종 상태(fail_count/
 * locked_until, apiKey 유일성)만 단언한다 — 정확히 이것이 STORY가 요구한 것이다.
 *
 * <p>round 9(재작업 지시 3, QA FAIL medium 1) — 종전 {@code resetSuccessMember()}는
 * {@code MEMBERS} 행만 지우고 {@code MEMBER_API_KEYS} 행은 남겨, 2·3회차 반복은
 * {@code issueIfAbsent}가 매번 no-op이 되는 가짜 레이스였다(실측: DB의 발급 시각이 최초 실행
 * 시각에 고정). 이제 매 iteration 시작 전에 {@code MEMBER_API_KEYS}·{@code MEMBER_REFRESH_TOKENS}의
 * 이 회원 행도 함께 지워 매회 실제 최초발급 레이스가 되게 한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberLoginConcurrencyTest {

    private static final int CONCURRENCY = 5;
    private static final int REPEAT = 3;
    private static final String FAILURE_EMAIL = "login-race-failure@example.com";
    private static final String SUCCESS_EMAIL = "login-race-success@example.com";
    private static final String SUCCESS_PASSWORD = "abcd1234";
    private static final String SUCCESS_MEMBER_ID = "M-9003-RACE";

    @Autowired
    private MemberLoginAttemptDao loginAttemptDao;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private MemberApiKeyDao apiKeyDao;
    // round 9(재작업 지시 3) — MemberApiKeyDao/MemberRefreshTokenDao에는 회원 단위 삭제 메서드가
    // 없어(계획 — 조회/발급/하우스키핑만) 테스트 정리는 JdbcTemplate으로 직접 한다
    // (ApiKeyAuthIntegrationTest·MemberApiKeyDaoTest와 동일 관례).
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void cleanUp() {
        loginAttemptDao.reset(FAILURE_EMAIL);
        loginAttemptDao.reset(SUCCESS_EMAIL);
        deleteSuccessMemberArtifacts();
        memberDao.deleteById(SUCCESS_MEMBER_ID);
    }

    // round 9(재작업 지시 3) — MEMBER_API_KEYS/MEMBER_REFRESH_TOKENS 둘 다 정리해야 다음
    // iteration(또는 다음 실행)이 진짜 "최초 로그인"이 된다.
    private void deleteSuccessMemberArtifacts() {
        jdbcTemplate.update("DELETE FROM MEMBER_API_KEYS WHERE member_id = ?", SUCCESS_MEMBER_ID);
        jdbcTemplate.update("DELETE FROM MEMBER_REFRESH_TOKENS WHERE member_id = ?", SUCCESS_MEMBER_ID);
    }

    // linked_tc: TC-FUNC-member-005-11 — 동시 5회 실패 → fail_count=5·locked_until 세팅(단독 3회).
    @Test
    void concurrentFailedLogins_fiveParallel_failCountReachesFiveAndLocks_repeated3Times() throws Exception {
        for (int iteration = 1; iteration <= REPEAT; iteration++) {
            loginAttemptDao.reset(FAILURE_EMAIL);
            runFailureRound(iteration);
        }
    }

    private void runFailureRound(int iteration) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY);

        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY; i++) {
            futures.add(pool.submit(callLogin(barrier, FAILURE_EMAIL, "wrong-password")));
        }
        pool.shutdown();

        List<HttpResponse<String>> results = collect(futures, iteration);

        long serverErrorCount = results.stream().filter(r -> r.statusCode() >= 500).count();
        assertThat(serverErrorCount)
                .as("iteration #%d — 500(데드락 등)이 0건이어야 함 — 응답들: %s", iteration, results)
                .isEqualTo(0);

        MemberLoginAttempt saved = loginAttemptDao.selectAttempt(FAILURE_EMAIL);
        assertThat(saved).as("iteration #%d", iteration).isNotNull();
        assertThat(saved.getFailCount())
                .as("iteration #%d — 동시 실패 %d건이 정확히 반영돼야 함(lost update 없음)",
                        iteration, CONCURRENCY)
                .isEqualTo(CONCURRENCY);
        assertThat(saved.getLockedUntil())
                .as("iteration #%d — 5회 도달 시 잠금이 걸려야 함(사람 확인 2)", iteration)
                .isNotNull()
                .isAfter(LocalDateTime.now());
    }

    // linked_tc: TC-FUNC-member-005-12 — 동시 최초 로그인 5건 → api_key 정확히 1개만 발급(단독 3회).
    @Test
    void concurrentFirstLogins_fiveParallel_issuesExactlyOneApiKey_repeated3Times() throws Exception {
        for (int iteration = 1; iteration <= REPEAT; iteration++) {
            resetSuccessMember();
            runSuccessRound(iteration);
        }
    }

    private void resetSuccessMember() {
        memberDao.deleteById(SUCCESS_MEMBER_ID);
        deleteSuccessMemberArtifacts();
        loginAttemptDao.reset(SUCCESS_EMAIL);
        String passwordHash = new BCryptPasswordEncoder().encode(SUCCESS_PASSWORD);
        memberDao.insertMember(SUCCESS_MEMBER_ID, "동시로그인테스터", "BRONZE", SUCCESS_EMAIL, null, null,
                passwordHash, false, LocalDateTime.now());
    }

    private void runSuccessRound(int iteration) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY);

        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY; i++) {
            futures.add(pool.submit(callLogin(barrier, SUCCESS_EMAIL, SUCCESS_PASSWORD)));
        }
        pool.shutdown();

        List<HttpResponse<String>> results = collect(futures, iteration);

        Set<String> apiKeys = new HashSet<>();
        for (HttpResponse<String> res : results) {
            assertThat(res.statusCode() / 100)
                    .as("iteration #%d — 모두 200이어야 함 — 응답: %s", iteration, res.body()).isEqualTo(2);
            JsonNode node = objectMapper.readTree(res.body());
            apiKeys.add(node.get("apiKey").asText());
        }

        assertThat(apiKeys)
                .as("iteration #%d — 동시 최초 로그인 %d건 모두 같은(=1개) apiKey를 받아야 함",
                        iteration, CONCURRENCY)
                .hasSize(1);
        assertThat(apiKeyDao.selectByMemberId(SUCCESS_MEMBER_ID)).isEqualTo(apiKeys.iterator().next());
    }

    private List<HttpResponse<String>> collect(List<Future<HttpResponse<String>>> futures, int iteration)
            throws InterruptedException {
        List<HttpResponse<String>> results = new ArrayList<>();
        for (Future<HttpResponse<String>> f : futures) {
            try {
                results.add(f.get(15, TimeUnit.SECONDS));
            } catch (ExecutionException | TimeoutException e) {
                throw new AssertionError("iteration #" + iteration + " — 동시 로그인 요청 실행 실패", e);
            }
        }
        assertThat(results).as("iteration #%d 응답 개수", iteration).hasSize(CONCURRENCY);
        return results;
    }

    private Callable<HttpResponse<String>> callLogin(CyclicBarrier barrier, String email, String password) {
        return () -> {
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (BrokenBarrierException | TimeoutException e) {
                throw new IllegalStateException("CyclicBarrier 대기 실패", e);
            }
            String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/members/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        };
    }
}
