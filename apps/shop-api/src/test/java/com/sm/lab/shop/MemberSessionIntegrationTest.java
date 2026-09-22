// linked_func: FUNC-member-006
// spec: docs/00_FUNC/stories/STORY-FUNC-member-006.md
package com.sm.lab.shop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sm.lab.shop.dao.CartDao;
import com.sm.lab.shop.dao.MemberDao;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SR-232(FUNC-member-006) 실 서버(RANDOM_PORT) 왕복 검증 — 계획 "테스트" 절의 통합 시나리오
 * (로그아웃 폐기 확인, apiKey rotate, 회전 후 구토큰 재사용 차단, 탈퇴 회원 refresh 차단,
 * admin 키 idempotent no-op, 화이트리스트/인증필요 회귀) + Dev 기록 추가 확정사항(로그아웃 전후
 * CART 테이블 행 불변)을 실측한다.
 *
 * <p><b>계획 대비 정정</b> — 계획 "테스트" 절 원문은 "로그아웃 뒤 같은 refreshToken으로 refresh
 * → 200(rotate)"을 기대했으나, 이는 같은 계획의 "005 인터페이스 경계표"(GATE-005 round2 사람
 * 확정, 재해석 금지) "로그아웃 = 해당 회원의 활성 리프레시 토큰 전체 폐기"와 모순된다 — 실 코드는
 * 후자(사람 확정 결정표)를 따르므로, 로그아웃 뒤에는 같은 refreshToken도 이미 폐기돼 refresh가
 * 401이어야 한다(자동 로그인 백도어를 막는 더 안전한 동작이기도 하다). apiKey rotate 자체는
 * {@link #refresh_afterApiKeyIndividuallyRevoked_rotatesToNewApiKey}가 로그아웃 없이(직접 DB로
 * apiKey만 폐기해) 별도로 검증한다.
 *
 * <p>POST + 401 조합에서 {@code TestRestTemplate}의 기본 요청 팩토리가 "cannot retry due to
 * server authentication, in streaming mode" 오류를 던지는 house 실측(《MemberLoginConcurrencyTest》·
 * 《ApiKeyAuthIntegrationTest》 주석 참고)이 있어, 이 스위트도 JDK {@link HttpClient}로 전 요청을
 * 통일한다.
 *
 * <p><b>테스트 정리 설계</b> — {@code MEMBER_API_KEYS}는 {@code member_id}가 PK이고
 * {@code MEMBER_REFRESH_TOKENS}의 신규(회전) 행도 항상 같은 {@code member_id} 컬럼을 갖는다.
 * 그래서 이 스위트는 (계획이 언급한) "마지막으로 관측된 apiKey/refreshToken 값"을 추적하는 대신
 * {@code member_id} 기준으로 정리한다 — 로그인 최초 발급이든 refresh 회전 뒤의 새 값이든 전부
 * 같은 {@code member_id}에 걸리므로, 고정 리터럴로 지울 때 회전 후 값이 새는 문제(계획이 지적한
 * 위험)가 애초에 발생하지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberSessionIntegrationTest {

    private static final String PASSWORD = "abcd1234";
    private static final String CART_SKU = "SKU-1001"; // ApiKeyAuthIntegrationTest와 동일 시드 SKU
    private static final int CART_QTY = 2;

    @Autowired
    private MemberDao memberDao;
    @Autowired
    private CartDao cartDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 이번 실행이 만든 회원 행 — @AfterEach가 member_id 기준으로 세 테이블 전부 정리한다.
    private final List<String> createdMemberIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String memberId : createdMemberIds) {
            jdbcTemplate.update("DELETE FROM MEMBER_API_KEYS WHERE member_id = ?", memberId);
            jdbcTemplate.update("DELETE FROM MEMBER_REFRESH_TOKENS WHERE member_id = ?", memberId);
            jdbcTemplate.update("DELETE FROM CART_ITEMS WHERE member_id = ?", memberId);
            memberDao.deleteById(memberId);
        }
        createdMemberIds.clear();
    }

    private HttpResponse<String> postJson(String path, String apiKey, String jsonBody) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (apiKey != null) {
            builder = builder.header("X-Api-Key", apiKey);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithKey(String path, String apiKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (apiKey != null) {
            builder = builder.header("X-Api-Key", apiKey);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    // linked_tc: TC-FUNC-member-006-23 — 왕복: 로그인→(장바구니 확인)→GET 인증 확인→logout→
    // 폐기된 apiKey 401→같은 refreshToken도 이미 죽어 refresh 401(계획 "MEMBER_REFRESH_TOKENS
    // 폐기 범위" 결정표 — 로그아웃은 apiKey뿐 아니라 이 회원의 활성 리프레시 토큰 전체도 폐기한다,
    // 재해석 금지). 카트 테이블 불변. apiKey rotate 자체는
    // {@link #refresh_afterApiKeyIndividuallyRevoked_rotatesToNewApiKey}가 별도로 검증한다(로그아웃
    // API에는 "apiKey만 폐기하고 refreshToken은 살려두는" 경로가 없어 이 시나리오에서는 재현 불가).
    @Test
    void roundTrip_login_logout_refresh_bothCredentialsDieTogether() throws Exception {
        String memberId = memberIdOnly("M-9006-RT");
        String email = "session-test-" + UUID.randomUUID() + "@example.com";
        String passwordHash = new BCryptPasswordEncoder().encode(PASSWORD);
        memberDao.insertMember(memberId, "왕복테스터", "BRONZE", email, null, null, passwordHash,
                false, LocalDateTime.now());
        createdMemberIds.add(memberId);
        cartDao.insertItem(memberId, CART_SKU, CART_QTY);

        Map<String, Object> cartBefore = jdbcTemplate.queryForMap(
                "SELECT qty FROM CART_ITEMS WHERE member_id = ? AND sku = ?", memberId, CART_SKU);

        // 1) 로그인 — 실제 apiKey/refreshToken 발급
        HttpResponse<String> loginRes = postJson("/api/members/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(loginRes.statusCode()).as("로그인 응답: %s", loginRes.body()).isEqualTo(200);
        JsonNode loginBody = objectMapper.readTree(loginRes.body());
        String apiKey1 = loginBody.get("apiKey").asText();
        String refreshToken1 = loginBody.get("refreshToken").asText();

        // 2) 발급된 apiKey로 인증 필요 엔드포인트 200 확인
        HttpResponse<String> authedRes = getWithKey("/api/orders", apiKey1);
        assertThat(authedRes.statusCode()).isEqualTo(200);

        // 3) 로그아웃(그 apiKey로, 인증 필요 — 필터가 통과시킨다)
        HttpResponse<String> logoutRes = postJson("/api/members/sessions/logout", apiKey1, "");
        assertThat(logoutRes.statusCode()).isEqualTo(204);

        // 4) 같은 apiKey 재사용 — 폐기됐으므로 401(추가 확정: CART 테이블은 이 사이 변하지 않는다)
        HttpResponse<String> reuseRes = getWithKey("/api/orders", apiKey1);
        assertThat(reuseRes.statusCode()).isEqualTo(401);

        Map<String, Object> cartAfterLogout = jdbcTemplate.queryForMap(
                "SELECT qty FROM CART_ITEMS WHERE member_id = ? AND sku = ?", memberId, CART_SKU);
        assertThat(cartAfterLogout.get("qty"))
                .as("SR '세션 만료 시 장바구니 유지' — 로그아웃 전후 장바구니 행이 변하지 않는다")
                .isEqualTo(cartBefore.get("qty"));

        // 5) 같은 refreshToken으로 refresh — 로그아웃이 apiKey와 함께 이 회원의 활성 리프레시
        // 토큰 전체를 이미 폐기했으므로(revokeAllForMember), 이 refreshToken도 되살아나지 않고
        // 401이어야 한다(무인증 화이트리스트라 필터는 통과하지만 서비스가 거부한다).
        HttpResponse<String> refreshAfterLogoutRes = postJson("/api/members/sessions/refresh", null,
                "{\"refreshToken\":\"" + refreshToken1 + "\"}");
        assertThat(refreshAfterLogoutRes.statusCode())
                .as("로그아웃이 리프레시 토큰까지 전체 폐기했으므로 로그아웃 전 refreshToken은 부활하면 안 된다: %s",
                        refreshAfterLogoutRes.body())
                .isEqualTo(401);
        assertThat(refreshAfterLogoutRes.body()).contains("MBR-4012");

        Map<String, Object> cartAfterAll = jdbcTemplate.queryForMap(
                "SELECT qty FROM CART_ITEMS WHERE member_id = ? AND sku = ?", memberId, CART_SKU);
        assertThat(cartAfterAll.get("qty")).isEqualTo(cartBefore.get("qty"));
    }

    // linked_tc: TC-FUNC-member-006-27 — apiKey rotate(issueIfAbsent 조건부 rotate)를 HTTP
    // 레벨로 검증한다. 로그아웃 API에는 "apiKey만 폐기하고 refreshToken은 살려두는" 공개 경로가
    // 없으므로(로그아웃은 항상 둘 다 같이 폐기), jdbcTemplate으로 apiKey만 직접 폐기해 그 상태를
    // 재현한다 — refresh가 이 경우에도 refreshToken은 여전히 유효하다고 판단해 통과시키고, 폐기된
    // apiKey 문자열은 재사용하지 않고 새 후보로 교체하는지 확인한다.
    @Test
    void refresh_afterApiKeyIndividuallyRevoked_rotatesToNewApiKey() throws Exception {
        String memberId = memberIdOnly("M-9006-ROT");
        String email = "session-test-" + UUID.randomUUID() + "@example.com";
        String passwordHash = new BCryptPasswordEncoder().encode(PASSWORD);
        memberDao.insertMember(memberId, "회전테스터", "BRONZE", email, null, null, passwordHash,
                false, LocalDateTime.now());
        createdMemberIds.add(memberId);

        HttpResponse<String> loginRes = postJson("/api/members/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(loginRes.statusCode()).isEqualTo(200);
        JsonNode loginBody = objectMapper.readTree(loginRes.body());
        String apiKey1 = loginBody.get("apiKey").asText();
        String refreshToken = loginBody.get("refreshToken").asText();

        // 로그아웃 API를 거치지 않고 apiKey만 직접 폐기 — refreshToken은 살아있는 상태를 재현.
        jdbcTemplate.update("UPDATE MEMBER_API_KEYS SET revoked_at = ? WHERE member_id = ?",
                LocalDateTime.now(), memberId);

        HttpResponse<String> refreshRes = postJson("/api/members/sessions/refresh", null,
                "{\"refreshToken\":\"" + refreshToken + "\"}");
        assertThat(refreshRes.statusCode()).as("refresh 응답: %s", refreshRes.body()).isEqualTo(200);
        JsonNode refreshBody = objectMapper.readTree(refreshRes.body());
        String apiKey2 = refreshBody.get("apiKey").asText();
        assertThat(apiKey2).as("폐기된 apiKey 문자열 자체는 재사용하지 않고 새 후보로 교체한다")
                .isNotEqualTo(apiKey1);

        HttpResponse<String> reAuthedRes = getWithKey("/api/orders", apiKey2);
        assertThat(reAuthedRes.statusCode()).isEqualTo(200);

        // 회전(rotation) — 방금 쓴 구 refreshToken은 재사용 시 401(회전 후 구토큰 폐기 확인).
        HttpResponse<String> reuseRefreshRes = postJson("/api/members/sessions/refresh", null,
                "{\"refreshToken\":\"" + refreshToken + "\"}");
        assertThat(reuseRefreshRes.statusCode()).isEqualTo(401);
        assertThat(reuseRefreshRes.body()).contains("MBR-4012");
    }

    // linked_tc: TC-FUNC-member-006-24 — 탈퇴 회원 refresh 차단(사례집 SR-232 r2 계열 재발 방지
    // 실측). 200이 아님을 반드시 단언한다.
    @Test
    void refresh_afterMemberWithdrawn_returns401NotOk() throws Exception {
        String memberId = memberIdOnly("M-9006-WD");
        String email = "session-test-" + UUID.randomUUID() + "@example.com";
        String passwordHash = new BCryptPasswordEncoder().encode(PASSWORD);
        memberDao.insertMember(memberId, "탈퇴예정테스터", "BRONZE", email, null, null, passwordHash,
                false, LocalDateTime.now());
        createdMemberIds.add(memberId);

        HttpResponse<String> loginRes = postJson("/api/members/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(loginRes.statusCode()).isEqualTo(200);
        String refreshToken = objectMapper.readTree(loginRes.body()).get("refreshToken").asText();

        jdbcTemplate.update("UPDATE MEMBERS SET del_yn = 'Y' WHERE member_id = ?", memberId);

        HttpResponse<String> refreshRes = postJson("/api/members/sessions/refresh", null,
                "{\"refreshToken\":\"" + refreshToken + "\"}");

        assertThat(refreshRes.statusCode())
                .as("탈퇴 회원의 refreshToken은 여전히 유효 기간 안이라도 통과시키면 안 된다")
                .isNotEqualTo(200)
                .isEqualTo(401);
        assertThat(refreshRes.body()).contains("MBR-4012");
    }

    // linked_tc: TC-FUNC-member-006-25 — admin 키(MEMBER_API_KEYS에 행이 없음)로 logout 호출은
    // idempotent 204(계획 "폴백·우회 경로의 자격 판정" 절 — 의도된 동작, 결함 아님).
    @Test
    void logout_withAdminKey_returns204Idempotent() throws Exception {
        HttpResponse<String> res = postJson("/api/members/sessions/logout", "lab-admin-key", "");

        assertThat(res.statusCode()).isEqualTo(204);
    }

    // linked_tc: TC-FUNC-member-006-26 — 회귀: logout은 X-Api-Key 없이 401(필터가 컨트롤러
    // 도달 전에 거부 — MEMBERS_ITEM_PATH 정규식과 무관하게 인증 자체가 막는지).
    @Test
    void logout_withoutApiKey_returns401() throws Exception {
        HttpResponse<String> res = postJson("/api/members/sessions/logout", null, "");

        assertThat(res.statusCode()).isEqualTo(401);
    }

    // linked_tc: TC-FUNC-member-006-28 — QA round1 재작업 지시 5(사람 확정): 결정표 (a) "폐기된
    // apiKey 재발급 = 재로그인 시 새 키로 교체"의 1차 시나리오를 HTTP 레벨로 검증한다(GATE-005
    // 핵심 우려였던 "영구 락아웃"의 직접 확인). 로그아웃 → 같은 자격증명으로 재로그인 → 새
    // apiKey로 200, 옛(폐기된) apiKey로는 401.
    @Test
    void logout_then_reLogin_issuesNewApiKey_oldApiKeyRejected() throws Exception {
        String memberId = memberIdOnly("M-9006-RLG"); // member_id VARCHAR(20) 상한(prefix+"-"+8자 UUID)
        String email = "session-test-" + UUID.randomUUID() + "@example.com";
        String passwordHash = new BCryptPasswordEncoder().encode(PASSWORD);
        memberDao.insertMember(memberId, "재로그인테스터", "BRONZE", email, null, null, passwordHash,
                false, LocalDateTime.now());
        createdMemberIds.add(memberId);

        HttpResponse<String> loginRes1 = postJson("/api/members/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(loginRes1.statusCode()).as("1차 로그인 응답: %s", loginRes1.body()).isEqualTo(200);
        String apiKey1 = objectMapper.readTree(loginRes1.body()).get("apiKey").asText();

        HttpResponse<String> logoutRes = postJson("/api/members/sessions/logout", apiKey1, "");
        assertThat(logoutRes.statusCode()).isEqualTo(204);

        HttpResponse<String> loginRes2 = postJson("/api/members/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(loginRes2.statusCode()).as("재로그인 응답: %s", loginRes2.body()).isEqualTo(200);
        String apiKey2 = objectMapper.readTree(loginRes2.body()).get("apiKey").asText();
        assertThat(apiKey2)
                .as("로그아웃으로 폐기된 apiKey 문자열은 재사용하지 않고 새 후보로 교체한다")
                .isNotEqualTo(apiKey1);

        HttpResponse<String> newKeyRes = getWithKey("/api/orders", apiKey2);
        assertThat(newKeyRes.statusCode()).as("재로그인으로 발급된 새 apiKey는 즉시 인증 가능해야 한다")
                .isEqualTo(200);

        HttpResponse<String> oldKeyRes = getWithKey("/api/orders", apiKey1);
        assertThat(oldKeyRes.statusCode())
                .as("로그아웃으로 폐기된 옛 apiKey는 재로그인 뒤에도 되살아나면 안 된다(영구 락아웃 반대 극단 회귀)")
                .isEqualTo(401);
    }

    private static String memberIdOnly(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
