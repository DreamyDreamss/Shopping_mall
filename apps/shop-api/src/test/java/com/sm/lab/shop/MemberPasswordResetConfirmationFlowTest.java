// linked_func: FUNC-member-009
// spec: docs/00_FUNC/stories/STORY-FUNC-member-009.md
package com.sm.lab.shop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberPasswordResetDao;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SR-234(FUNC-member-009) 실 서버(RANDOM_PORT)+실 DB 왕복 검증 — Mockito로 증명 못 하는 트랜잭션
 * 경계(비밀번호 반영+전 기기 로그아웃의 원자성)·실제 비밀번호 변경·세션 폐기의 HTTP 레벨 회귀를
 * 실측한다({@link MemberRegistrationCompletionFlowTest}와 동일 이유). POST에 다양한 상태 코드
 * (204/410/409/401)가 섞여 {@code TestRestTemplate}의 스트리밍 모드 재시도 이슈(house 실측,
 * {@link MemberSessionIntegrationTest} 주석 참고)를 피하려 JDK {@link HttpClient}로 통일한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberPasswordResetConfirmationFlowTest {

    private static final String CONFIRM_PATH = "/api/members/password-resets/confirmations";
    private static final int COOLDOWN_SECONDS = 60;

    @Autowired
    private MemberDao memberDao;
    @Autowired
    private MemberPasswordResetDao passwordResetDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<String> createdMemberIds = new ArrayList<>();
    private final List<String> seededResetTargets = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String memberId : createdMemberIds) {
            jdbcTemplate.update("DELETE FROM MEMBER_API_KEYS WHERE member_id = ?", memberId);
            jdbcTemplate.update("DELETE FROM MEMBER_REFRESH_TOKENS WHERE member_id = ?", memberId);
            memberDao.deleteById(memberId);
        }
        createdMemberIds.clear();
        for (String target : seededResetTargets) {
            passwordResetDao.deleteByTarget(target);
        }
        seededResetTargets.clear();
    }

    private static String memberIdOnly(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String registerMember(String email, String password) {
        String memberId = memberIdOnly("M-9009");
        String passwordHash = new BCryptPasswordEncoder().encode(password);
        memberDao.insertMember(memberId, "확정흐름테스터", "BRONZE", email, null, null, passwordHash,
                false, LocalDateTime.now());
        createdMemberIds.add(memberId);
        return memberId;
    }

    /** target(정규화된 값)에 알려진 평문 code로 재설정 코드 행을 직접 시드한다(FUNC-008 API 왕복 생략, 실 발송 없음). */
    private void seedResetCode(String normalizedTarget, String plainCode) {
        LocalDateTime now = LocalDateTime.now();
        passwordResetDao.touchRequest(normalizedTarget, sha256Hex(plainCode), now.plusMinutes(10), now,
                COOLDOWN_SECONDS);
        seededResetTargets.add(normalizedTarget);
    }

    /** SR-297 #1 round1 재작업 — 이미 만료된 코드 행을 직접 시드한다(주어진 expiresAt이 과거). */
    private void seedExpiredResetCode(String normalizedTarget, String plainCode, LocalDateTime expiresAt) {
        passwordResetDao.touchRequest(normalizedTarget, sha256Hex(plainCode), expiresAt,
                expiresAt.minusMinutes(10), COOLDOWN_SECONDS);
        seededResetTargets.add(normalizedTarget);
    }

    private HttpResponse<String> postJson(String path, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** round2 재작업(QA CONCERNS 권고 1) — 회원 API 키 폐기 실측용, {@code MemberSessionIntegrationTest}
     * {@code getWithKey}와 동일 패턴. */
    private HttpResponse<String> getWithKey(String path, String apiKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (apiKey != null) {
            builder = builder.header("X-Api-Key", apiKey);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> confirm(String target, String code, String newPassword) throws Exception {
        String body = String.format("{\"target\":\"%s\",\"code\":\"%s\",\"newPassword\":\"%s\"}",
                target, code, newPassword);
        return postJson(CONFIRM_PATH, body);
    }

    private HttpResponse<String> login(String email, String password) throws Exception {
        return postJson("/api/members/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
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

    // (1) 실존 회원(대소문자 섞인 이메일로 가입) → 확정 → 새 비밀번호로 로그인 200(비밀번호가
    // 실제로 반영됐고 LOWER(email) 매칭이 원문 대소문자와 맞아떨어짐을 실측) → 재설정 이전 발급
    // 리프레시 토큰으로 refresh → 401(전 기기 로그아웃 실측, SR 수용 기준 핵심 증거) →
    // 재설정 이전 발급 회원 API 키로 보호 API 호출 → 401(round2 재작업, QA CONCERNS 권고 1 —
    // 전 기기 로그아웃의 나머지 절반인 회원 API 키 폐기도 함께 실측).
    @Test
    void confirmSucceeds_thenNewPasswordLogsIn_andPreResetRefreshTokenIsRejected() throws Exception {
        String email = "Flow009-" + UUID.randomUUID() + "@Example.com";
        String normalizedTarget = email.toLowerCase();
        String oldPassword = "oldpass123";
        String newPassword = "newpass456";
        registerMember(email, oldPassword);

        HttpResponse<String> loginBeforeReset = login(email, oldPassword);
        assertThat(loginBeforeReset.statusCode()).as("사전 로그인: %s", loginBeforeReset.body()).isEqualTo(200);
        String refreshTokenBeforeReset = objectMapper.readTree(loginBeforeReset.body())
                .get("refreshToken").asText();
        String apiKeyBeforeReset = objectMapper.readTree(loginBeforeReset.body()).get("apiKey").asText();
        String memberId = objectMapper.readTree(loginBeforeReset.body()).get("memberId").asText();

        HttpResponse<String> protectedCallBeforeReset = getWithKey("/api/members/" + memberId, apiKeyBeforeReset);
        assertThat(protectedCallBeforeReset.statusCode())
                .as("재설정 전 발급된 회원 API 키는 보호 API를 통과해야 한다: %s", protectedCallBeforeReset.body())
                .isEqualTo(200);

        seedResetCode(normalizedTarget, "654321");

        HttpResponse<String> confirmRes = confirm(normalizedTarget, "654321", newPassword);
        assertThat(confirmRes.statusCode()).as("확정 응답: %s", confirmRes.body()).isEqualTo(204);

        HttpResponse<String> loginWithNewPassword = login(email, newPassword);
        assertThat(loginWithNewPassword.statusCode())
                .as("새 비밀번호 로그인: %s", loginWithNewPassword.body())
                .isEqualTo(200);

        HttpResponse<String> refreshWithOldToken = postJson("/api/members/sessions/refresh",
                "{\"refreshToken\":\"" + refreshTokenBeforeReset + "\"}");
        assertThat(refreshWithOldToken.statusCode())
                .as("재설정 이전 리프레시 토큰은 전 기기 로그아웃으로 폐기돼야 한다: %s", refreshWithOldToken.body())
                .isEqualTo(401);
        assertThat(refreshWithOldToken.body()).contains("MBR-4012");

        HttpResponse<String> protectedCallAfterReset = getWithKey("/api/members/" + memberId, apiKeyBeforeReset);
        assertThat(protectedCallAfterReset.statusCode())
                .as("재설정 이전 발급된 회원 API 키는 전 기기 로그아웃으로 폐기돼야 한다: %s",
                        protectedCallAfterReset.body())
                .isEqualTo(401);
    }

    // (2) 탈퇴 회원 — 204(오라클 없음)이지만 password_hash는 변경되지 않는다(del_yn 필터 실측).
    @Test
    void withdrawnMember_confirmReturns204_butPasswordHashUnchanged() throws Exception {
        String email = "withdrawn-flow009-" + UUID.randomUUID() + "@example.com";
        String memberId = registerMember(email, "oldpass123");
        String passwordHashBefore = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM MEMBERS WHERE member_id = ?", String.class, memberId);
        jdbcTemplate.update("UPDATE MEMBERS SET del_yn = 'Y' WHERE member_id = ?", memberId);

        seedResetCode(email, "111222");

        HttpResponse<String> confirmRes = confirm(email, "111222", "newpass456");
        assertThat(confirmRes.statusCode()).as("탈퇴 회원도 오라클 없이 204: %s", confirmRes.body()).isEqualTo(204);

        String passwordHashAfter = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM MEMBERS WHERE member_id = ?", String.class, memberId);
        assertThat(passwordHashAfter).as("탈퇴 회원의 비밀번호는 변경되면 안 된다").isEqualTo(passwordHashBefore);
    }

    // (3) 한 번도 가입된 적 없는 이메일 — 회원 없음이 절대 다른 응답을 만들지 않는다(204).
    @Test
    void neverRegisteredEmail_confirmReturns204() throws Exception {
        String normalizedTarget = "never-registered-" + UUID.randomUUID() + "@example.com";
        seedResetCode(normalizedTarget, "333444");

        HttpResponse<String> confirmRes = confirm(normalizedTarget, "333444", "newpass456");

        assertThat(confirmRes.statusCode()).as("확정 응답: %s", confirmRes.body()).isEqualTo(204);
    }

    // (4) 동일 코드로 확정 두 번(재전송/중복 클릭 시나리오) — 1회용 실측.
    @Test
    void confirmTwiceWithSameCode_firstSucceeds_secondReturns410Expired() throws Exception {
        String email = "reuse-flow009-" + UUID.randomUUID() + "@example.com";
        registerMember(email, "oldpass123");
        seedResetCode(email, "555666");

        HttpResponse<String> first = confirm(email, "555666", "newpass456");
        assertThat(first.statusCode()).as("첫 확정: %s", first.body()).isEqualTo(204);

        HttpResponse<String> second = confirm(email, "555666", "newpass456");
        assertThat(second.statusCode()).as("재사용은 만료(1회용): %s", second.body()).isEqualTo(410);
        assertThat(second.body()).contains("MBR-4101");
    }

    // (5, 사람 수정 (1)의 통합 레벨 증거) "요청한 적 없는" target(MEMBER_PASSWORD_RESETS에 행
    // 자체가 없음)과 "단순 오답"(행은 있으나 코드가 틀림)이 HTTP 레벨에서 바이트 단위로 동일한
    // 응답(상태코드+본문)을 낸다 — 존재 오라클이 없음을 실측하는 이 STORY의 핵심 완료 조건.
    @Test
    void rowNeverRequested_and_simpleWrongCode_produceByteIdenticalResponses() throws Exception {
        String neverRequestedTarget = "never-requested-" + UUID.randomUUID() + "@example.com";

        String wrongCodeTarget = "wrongcode-flow009-" + UUID.randomUUID() + "@example.com";
        seedResetCode(wrongCodeTarget, "777888");

        HttpResponse<String> rowMissingRes = confirm(neverRequestedTarget, "999999", "newpass456");
        HttpResponse<String> wrongCodeRes = confirm(wrongCodeTarget, "000000", "newpass456");

        assertThat(rowMissingRes.statusCode()).isEqualTo(409);
        assertThat(wrongCodeRes.statusCode()).isEqualTo(409);
        assertThat(rowMissingRes.statusCode()).isEqualTo(wrongCodeRes.statusCode());
        assertThat(rowMissingRes.body())
                .as("행 없음과 단순 오답은 바이트 단위로 동일한 응답이어야 한다(존재 오라클 방지)")
                .isEqualTo(wrongCodeRes.body());
        assertThat(rowMissingRes.body()).contains("MBR-4102");
    }

    // (6, SR-298 신규 — 이번에 메우는 회귀 공백) 순차 오답 5회 각각 409/MBR-4102, DB attempt_count=5
    // 실측, 6회째(오답이든 정답이든)는 409/MBR-4103이며 attempt_count는 5에 머문다.
    @Test
    void wrongCodeFiveTimesThenSixth_locksOutAtFiveWithMbr4103() throws Exception {
        String target = "wrongcode-fivetimes-" + UUID.randomUUID() + "@example.com";
        String correctCode = "246810";
        seedResetCode(target, correctCode);

        for (int i = 1; i <= 5; i++) {
            HttpResponse<String> res = confirm(target, "000000", "newpass456");
            assertThat(res.statusCode()).as("%d번째 오답 응답: %s", i, res.body()).isEqualTo(409);
            assertThat(res.body()).as("%d번째 오답 응답 본문: %s", i, res.body()).contains("MBR-4102");
            assertThat(passwordResetDao.selectByTarget(target).getAttemptCount())
                    .as("%d번째 오답 이후 attempt_count", i).isEqualTo(i);
        }

        HttpResponse<String> sixth = confirm(target, correctCode, "newpass456");
        assertThat(sixth.statusCode()).as("6번째(정답이어도) 응답: %s", sixth.body()).isEqualTo(409);
        assertThat(sixth.body()).contains("MBR-4103");
        assertThat(passwordResetDao.selectByTarget(target).getAttemptCount())
                .as("상한 도달 후 attempt_count는 5에 머물러야 함").isEqualTo(5);
    }

    // (7, SR-297 #1 round1 재작업 AC) 만료 1일 지난 코드는 정리 배치(기본 보존기간 7일)가
    // 아직 지우지 않았을 시점이므로 행이 그대로 남아 있다 — 확정 API는 여전히 410/MBR-4101을
    // 내야 한다(즉시 삭제였다면 행이 없어 409/MBR-4102로 바뀌었을 것 — round1 QA CONCERNS
    // 권고1이 지적한 회귀를 이 통합 레벨에서 직접 실측한다).

    // linked_tc: TC-FUNC-member-bat001-009
    @Test
    void expiredOneDayAgo_stillWithinPurgeRetentionWindow_confirmStillReturns410Expired() throws Exception {
        String target = "expired-retention-flow009-" + UUID.randomUUID() + "@example.com";
        seedExpiredResetCode(target, "135790", LocalDateTime.now().minusDays(1));

        HttpResponse<String> confirmRes = confirm(target, "135790", "newpass456");

        assertThat(confirmRes.statusCode())
                .as("만료 1일 지난 코드는 보존기간(7일) 안이라 여전히 410이어야 함: %s", confirmRes.body())
                .isEqualTo(410);
        assertThat(confirmRes.body()).contains("MBR-4101");
    }
}
