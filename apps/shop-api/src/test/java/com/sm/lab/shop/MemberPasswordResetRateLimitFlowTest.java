package com.sm.lab.shop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sm.lab.shop.dao.MemberPasswordResetDao;
import com.sm.lab.shop.dao.MemberPasswordResetRateLimitDao;
import com.sm.lab.shop.domain.MemberPasswordReset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SR-297 #2(INF-MBR-006) 실 서버(RANDOM_PORT)+실 DB 왕복 검증 — STEP 3-0 사람 확인 추가 요구
 * ("상한 초과 테스트 보강: HTTP 단언(202+동일 바디, 컨트롤러 통합) 및 DB 단언(코드 테이블
 * created_at/code_hash 불변) 추가"). {@link com.sm.lab.shop.service.MemberPasswordResetServiceTest}
 * 의 Mockito 단위 테스트는 dao.touchRequest가 "호출되지 않았다"까지만 증명하고, 실제 DB 행이
 * 바뀌지 않았음은 증명하지 못한다 — 이 파일은 실제 컨트롤러→서비스→DAO→DB 왕복으로 그 공백을 메운다.
 *
 * <p>컨트롤러·매퍼·DDL은 전혀 건드리지 않는다(STORY "범위 밖") — 이 파일은 테스트 전용 추가다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberPasswordResetRateLimitFlowTest {

    private static final String CODE_PATH = "/api/members/password-resets/codes";
    private static final int COOLDOWN_SECONDS = 60;
    private static final int DAILY_LIMIT = 5;

    @Autowired
    private MemberPasswordResetDao passwordResetDao;
    @Autowired
    private MemberPasswordResetRateLimitDao rateLimitDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 테스트마다 새 UUID 접미 target — 고정 리터럴이면 다른 테스트/재실행과 행이 섞여 플레이키해질
    // 수 있다(사례집 SR-232 r2 재발 방지, 이 도메인 DAO 테스트와 동일 관례).
    private String target;

    @AfterEach
    void cleanUp() {
        if (target != null) {
            passwordResetDao.deleteByTarget(target);
            jdbcTemplate.update("DELETE FROM MEMBER_PASSWORD_RESET_RATE_LIMITS WHERE target = ?", target);
        }
    }

    private HttpResponse<String> postCodeRequest(String requestTarget) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + CODE_PATH))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"target\":\"" + requestTarget + "\"}"))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // STEP 3-0 사람 확인 — 상한 초과 테스트 보강. 오늘 카운터를 상한(5)까지 직접 시드해 둔 뒤
    // (쿨다운은 이미 지난 시각), 코드 테이블에도 이전 요청이 남긴 행을 시드한다. 상한 초과 요청은
    // 결정1 4단계에 따라 dao.touchRequest 자체를 호출하지 않아야 하므로, 코드 테이블 행은 요청
    // 전후로 바이트 하나도 바뀌면 안 된다 — 그러면서도 HTTP 응답은 정상 케이스와 동일한 202·바디다
    // (상한 오라클 없음, 확정 문답 regression_keep).
    @Test
    void dailyLimitExceeded_returns202WithSameBody_andCodeTableUnchanged() throws Exception {
        target = "pwreset-flow-dailylimit-" + UUID.randomUUID() + "@example.com";
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        // 오늘 카운터를 상한까지 직접 시드(쿨다운은 이미 지난 시각이라 daily_count만이 거부 사유).
        jdbcTemplate.update(
                "INSERT INTO MEMBER_PASSWORD_RESET_RATE_LIMITS "
                        + "(target, day_key, daily_count, last_requested_at, last_token) VALUES (?, ?, ?, ?, ?)",
                target, now.toLocalDate(), DAILY_LIMIT, now.minusSeconds(COOLDOWN_SECONDS + 1),
                UUID.randomUUID().toString());
        assertThat(rateLimitDao.selectRateLimit(target, now.toLocalDate()).getDailyCount())
                .as("사전 조건: 오늘 카운터가 상한에 도달해 있어야 함").isEqualTo(DAILY_LIMIT);

        // 코드 테이블에도 이전 요청이 남긴 행을 시드 — 상한 초과 요청이 이 행을 절대 건드리지
        // 않아야 함(touchRequest 자체가 호출되지 않으므로).
        String previousHash = "a".repeat(64);
        LocalDateTime previousCreatedAt = now.minusMinutes(5);
        passwordResetDao.touchRequest(target, previousHash, previousCreatedAt.plusMinutes(10),
                previousCreatedAt, COOLDOWN_SECONDS);
        MemberPasswordReset storedBefore = passwordResetDao.selectByTarget(target);
        assertThat(storedBefore.getCodeHash()).isEqualTo(previousHash);

        HttpResponse<String> response = postCodeRequest(target);

        assertThat(response.statusCode())
                .as("상한 초과도 202(존재·쿨다운·상한 오라클 방지): %s", response.body())
                .isEqualTo(202);
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.get("channel").asText()).isEqualTo("EMAIL");
        assertThat(body.get("target").asText()).isEqualTo(target);
        assertThat(body.get("expiresInSeconds").asInt()).isEqualTo(600);

        MemberPasswordReset storedAfter = passwordResetDao.selectByTarget(target);
        assertThat(storedAfter.getCodeHash())
                .as("상한 초과 시 코드 테이블 code_hash는 절대 갱신되면 안 됨(touchRequest 미호출)")
                .isEqualTo(storedBefore.getCodeHash());
        assertThat(storedAfter.getCreatedAt())
                .as("상한 초과 시 코드 테이블 created_at도 불변이어야 함")
                .isEqualTo(storedBefore.getCreatedAt());
    }
}
