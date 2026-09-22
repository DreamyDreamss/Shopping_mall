// linked_func: FUNC-order-013, FUNC-order-004, FUNC-member-003, FUNC-member-005, FUNC-member-006,
// FUNC-member-012 —
// SR-204 R-1/R-2/R-4 (round 6) + SR-207 화면 라우트 소유권 회귀(round 7, QA round1 FAIL 필수3) +
// SR-231 가입 요청 API 화이트리스트 회귀(FUNC-member-003) + SR-232 로그인 API 화이트리스트
// 회귀 + DB 발급 회원 API 키 폴백 회귀(round 8, FUNC-member-005) + SR-232 refresh 화이트리스트/
// logout 인증필요 회귀(FUNC-member-006) + SR-235 우편번호 검색 API 무인증 401 회귀(FUNC-member-012 —
// ZipcodeControllerTest는 AdminApiKeyTestConfig 슬라이스 제약상 "무키" 상태를 만들 수 없어 이
// 실 서버 통합 테스트가 대신 검증한다)
// spec: docs/00_FUNC/stories/STORY-FUNC-order-013.md, docs/00_FUNC/stories/STORY-FUNC-order-004.md,
// docs/00_FUNC/stories/STORY-FUNC-member-003.md, docs/00_FUNC/stories/STORY-FUNC-member-005.md,
// docs/00_FUNC/stories/STORY-FUNC-member-006.md, docs/00_FUNC/stories/STORY-FUNC-member-012.md
package com.sm.lab.shop.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sm.lab.shop.dao.MemberAddressDao;
import com.sm.lab.shop.dao.MemberApiKeyDao;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberSignupRateLimitDao;
import com.sm.lab.shop.dao.MemberSignupVerificationDao;
import com.sm.lab.shop.domain.MemberAddress;
import com.sm.lab.shop.support.TestClocks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SR-204 D13/03_TC — {@code /api/**} 전역 X-Api-Key 인증(R-1) + member 스코프 IDOR 차단(R-2,
 * 쿼리·JSON 본문·경로 3경로) + 화면 라우트 무인증 유지(R-4)를 실 서버(RANDOM_PORT)로 검증한다.
 *
 * <p>{@code AdminApiKeyTestConfig}(admin 키 기본 주입)를 쓰지 않는다 — "무키" 상태를 직접
 * 만들어야 하는 이 테스트의 목적과 상충한다. 대신 {@link TestRestTemplate}로 헤더를 매 요청마다
 * 명시적으로 제어한다.
 *
 * <p>랩 시드 데이터: M-0001·M-0002·M-0003 회원 실존(다른 SR 테스트와 공유 실측). 이 테스트는
 * 조회(GET)만 수행하거나 필터에서 403으로 거부되어 컨트롤러에 도달하지 않는 요청만 사용하므로
 * DB를 변경하지 않는다(별도 정리 불필요).
 *
 * <p>SR-300: {@link #memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders()}는
 * startDate/endDate 없이 GET /api/orders를 호출해 OrderService의 기본 조회창(오늘-30일 ~ 오늘)에
 * 의존한다 — 시스템 시계로는 날짜가 흐르며 시드 주문일(2026-08-15~17)이 창 밖으로 밀려 실패한다
 * (SR-300 원인). 아래 {@link FixedClockTestConfig}가 '오늘'을 2026-08-20으로 고정한다. 이
 * {@code @Primary Clock}을 @Autowired로 받는 컴포넌트는 OrderService뿐이라(다른 서비스는 전부
 * 내부에서 Clock.systemDefaultZone()을 직접 생성) 같은 클래스의 다른 테스트에는 영향이 없다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiKeyAuthIntegrationTest {

    // SR-300 재작업(round2, QA CONCERNS 권고1 — 함정 경고): 주문 생성은 아직 시스템 시계다
    // (OrderService.create()는 이 SR 범위 밖 — raw LocalDate.now() 그대로). 고정 시계는 이
    // list() 기본 조회창에만 적용된다 — 이 클래스(60여 건 공유 컨텍스트)에 "주문 생성 → 날짜
    // 없이 목록 조회"를 하는 테스트를 새로 추가하면 ordered_at이 실제 오늘(고정창 밖,
    // ~2026-08-20)이라 조용히 0건이 나와 증상이 인가/필터 결함처럼 보일 수 있다(후속 SR에서
    // 시계화 검토).
    @TestConfiguration
    static class FixedClockTestConfig {
        // 빈 이름을 ShopApiApplication의 "clock"과 다르게 둔다 — 같은 이름이면 스프링 부트 기본
        // 설정(빈 정의 오버라이딩 비허용)에서 BeanDefinitionOverrideException이 난다. @Primary로
        // 타입 기준 주입 시 이 빈이 선택되게 한다.
        @Bean
        @Primary
        Clock fixedTestClock() {
            return TestClocks.SEED_TODAY;
        }
    }

    private static final String HEADER = "X-Api-Key";
    private static final String ADMIN_KEY = "lab-admin-key";
    private static final String MEMBER_0001_KEY = "lab-member-0001-key";
    private static final String OWN_MEMBER_ID = "M-0001";
    private static final String OTHER_MEMBER_ID = "M-0002";
    // 랩 시드 실측(OrderExportIntegrationTest 주석 근거): 20260817-0002는 M-0002 소유(CANCELED —
    // 취소 회귀 프로브가 상태를 바꿀 위험이 없는 안전한 대상), 20260816-0002는 M-0001(자기) 소유.
    private static final String FOREIGN_ORDER_NO = "20260817-0002";
    private static final String OWN_ORDER_NO = "20260816-0002";
    // FUNC-member-005 round 8 — 정적 lab.api-keys 맵에는 없는 키. MemberApiKeyDao에 직접
    // 발급해 DB 폴백 경로만 단독으로 확인한다(로그인 API 자체를 거치지 않음 — MemberLoginConcurrencyTest
    // 가 로그인→발급까지의 전체 흐름을 이미 검증한다).
    private static final String DB_FALLBACK_KEY = "db-fallback-test-key";
    // round 9(재작업 지시 2, QA FAIL 필수2) — 탈퇴 회원(del_yn='Y')용 DB 발급 키. M-0004는 랩 시드에
    // 이미 del_yn='Y'로 존재한다(실측, DB MCP) — 별도 탈퇴 처리 없이 그대로 재사용한다.
    private static final String DB_FALLBACK_KEY_WITHDRAWN_MEMBER = "db-fallback-withdrawn-member-key";
    private static final String WITHDRAWN_MEMBER_ID = "M-0004";
    // round 9(재작업 지시 2) — 폐기된(revoked_at NOT NULL) 키. MemberApiKeyDao에는 폐기 메서드가
    // 없어(폐기는 FUNC-006 소관) 테스트가 JdbcTemplate으로 직접 revoked_at을 세팅해 006이 로그아웃
    // 시 할 일을 흉내 낸다.
    private static final String DB_FALLBACK_KEY_REVOKED = "db-fallback-revoked-key";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MemberSignupVerificationDao memberSignupVerificationDao;

    @Autowired
    private MemberSignupRateLimitDao memberSignupRateLimitDao;

    // FUNC-member-003 — 가입 요청 화이트리스트 회귀 테스트가 실제로 회원을 생성하므로 정리용.
    @Autowired
    private MemberDao memberDao;

    // FUNC-member-005 — round 8 DB 폴백 회귀(아래 fallback* 테스트)가 직접 발급하는 키 정리용.
    // MemberApiKeyDao는 삭제 메서드를 두지 않아(계획 — selectByMemberId/issueIfAbsent만)
    // JdbcTemplate으로 직접 정리한다(MemberApiKeyDaoTest와 동일 관례).
    @Autowired
    private MemberApiKeyDao memberApiKeyDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    // FUNC-member-011 round 2(재작업 지시 5) — ?memberId=타인 강제 축소 회귀 테스트가 직접 심는
    // 타 회원(M-0002) 소유 배송지 행 정리용.
    @Autowired
    private MemberAddressDao memberAddressDao;

    private final ObjectMapper objectMapper = new ObjectMapper();
    // signUpRoute_* 두 테스트가 만든 회원 행 정리용(memberId는 응답에서 읽는다 — round2부터
    // ID_SEQUENCES DB 채번이라 사전에 알 수 없음).
    private final List<String> createdMemberIds = new ArrayList<>();
    // round 9(재작업 지시 1, QA FAIL 필수1 — 테스트 플레이키) — loginRoute_* 두 테스트가 매번
    // 새로 생성하는 이메일(UUID 접미) 정리용. 종전에는 두 테스트가 같은 리터럴 이메일
    // (no-such-member@example.com)을 공유해 매 실행 2회씩 실패시키면서도 지우지 않아 카운터가
    // 실행을 가로질러 누적됐다(실측: fail_count=5, locked_until 세팅 — 2~3회 실행마다 429로
    // 오응답하는 영구 플레이키). 이제 테스트마다 고유 이메일을 쓰고 여기서 그 행을 지운다.
    private final List<String> loginProbeEmails = new ArrayList<>();

    private static String uniqueLoginProbeEmail() {
        return "no-such-member-" + java.util.UUID.randomUUID() + "@example.com";
    }

    @LocalServerPort
    private int port;

    // round2(SR-231 round1 QA FAIL 권고#4) — signupVerificationCodeRoute_* 두 테스트가 실 DB에
    // 남기는 행(newbie@example.com, 01099998888)을 정리한다(MemberSignupVerificationDaoTest의
    // @AfterEach 관례와 동일). 다른 테스트는 이 두 target을 쓰지 않으므로 매 테스트 후 호출해도
    // 부작용이 없다(없으면 0건 삭제). round4 — 레이트리밋 카운터가 전용 테이블로 옮겨져
    // 그쪽도 함께 정리한다(정리하지 않으면 반복 실행마다 daily_count가 쌓여 결국 이 테스트가
    // 200 대신 429를 받게 된다). FUNC-member-003 — signUpRoute_* 두 테스트가 쓰는
    // signup-whitelist-check@example.com/01099997777 행과 그 요청으로 생성된 MEMBERS 행도 함께
    // 정리한다.
    @AfterEach
    void cleanUpSignupVerificationRows() {
        memberSignupVerificationDao.deleteByChannelAndTarget("EMAIL", "newbie@example.com");
        memberSignupVerificationDao.deleteByChannelAndTarget("SMS", "01099998888");
        memberSignupRateLimitDao.deleteRateLimit("newbie@example.com", LocalDate.now());
        memberSignupRateLimitDao.deleteRateLimit("01099998888", LocalDate.now());
        memberSignupVerificationDao.deleteByChannelAndTarget("EMAIL", "signup-whitelist-check@example.com");
        memberSignupVerificationDao.deleteByChannelAndTarget("SMS", "01099997777");
        for (String memberId : createdMemberIds) {
            memberDao.deleteById(memberId);
        }
        createdMemberIds.clear();
        jdbcTemplate.update("DELETE FROM MEMBER_API_KEYS WHERE api_key IN (?, ?, ?)",
                DB_FALLBACK_KEY, DB_FALLBACK_KEY_WITHDRAWN_MEMBER, DB_FALLBACK_KEY_REVOKED);
        // round 9(재작업 지시 1) — loginRoute_* 프로브 이메일의 MEMBER_LOGIN_ATTEMPTS 행 정리.
        for (String email : loginProbeEmails) {
            jdbcTemplate.update("DELETE FROM MEMBER_LOGIN_ATTEMPTS WHERE email = ?", email);
        }
        loginProbeEmails.clear();
    }

    /**
     * round 6 회귀 — 경로 인코딩·matrix 파라미터 우회 검증용 raw HTTP 클라이언트. {@link TestRestTemplate}은
     * 문자열 URL을 내부적으로 재정규화·재인코딩할 수 있어 {@code %61pi}·{@code ;a=b} 같은 원문을 그대로
     * 와이어에 보낸다는 보장이 없다 — QA r5가 curl 등으로 실측한 것과 동일한 raw 요청을 재현하기 위해
     * JDK {@link HttpClient}로 이미 인코딩된 {@link URI}를 직접 만들어 보낸다(재인코딩 없음).
     */
    private HttpResponse<String> rawRequest(String method, String rawPathAndQuery) throws Exception {
        return rawRequest(method, rawPathAndQuery, null);
    }

    // TestRestTemplate의 기본 SimpleClientHttpRequestFactory(JDK HttpURLConnection 기반)는
    // PATCH를 지원하지 않는다("Invalid HTTP method: PATCH") — PATCH가 필요한 경우도 이 raw
    // HttpClient로 수행한다(키 헤더 부여 가능).
    // SR-307(SR-307.1) round 2(재작업 지시 5) — HEAD + 401 조합도 같은 회피책이 필요하다: JDK
    // HttpURLConnection(TestRestTemplate 기본 팩토리) 기반 head()는 HEAD 응답에 본문이 없어
    // getErrorStream()을 못 얻고 "Server returned HTTP response code: 401" IOException을 그대로
    // 던진다(실측) — PATCH가 postJson/head 경로에서 겪은 것과 동일한 JDK HttpURLConnection 한계.
    private HttpResponse<String> rawRequest(String method, String rawPathAndQuery, String apiKey) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + rawPathAndQuery));
        if ("PATCH".equals(method)) {
            builder = builder.method("PATCH", HttpRequest.BodyPublishers.noBody());
        } else if ("HEAD".equals(method)) {
            builder = builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
        } else {
            builder = builder.GET();
        }
        if (apiKey != null) {
            builder = builder.header(HEADER, apiKey);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * FUNC-member-005 round 8 — POST + 401 조합 전용. {@link TestRestTemplate}(postJson)의 기본
     * {@code SimpleClientHttpRequestFactory}(JDK {@code HttpURLConnection} 기반)는 이 조합에서
     * "cannot retry due to server authentication, in streaming mode"(JDK 인증 재시도 로직과
     * 스트리밍 요청 바디의 충돌, WWW-Authenticate 헤더 유무와 무관)를 던진다(실측) — 위
     * {@link #rawRequest}가 PATCH를 위해 JDK {@link HttpClient}를 쓰는 것과 동일한 이유의 회피책.
     */
    private HttpResponse<String> rawPostJson(String path, String apiKey, String jsonBody) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (apiKey != null) {
            builder = builder.header(HEADER, apiKey);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private ResponseEntity<String> get(String path, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null) {
            headers.set(HEADER, apiKey);
        }
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> postJson(String path, String apiKey, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null) {
            headers.set(HEADER, apiKey);
        }
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    // FUNC-member-011 round 2(재작업 지시 5) 전용 — PUT/DELETE도 TestRestTemplate 기본
    // SimpleClientHttpRequestFactory로 문제없이 보낼 수 있다(PATCH만 별도 rawRequest가 필요).
    private ResponseEntity<String> putJson(String path, String apiKey, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null) {
            headers.set(HEADER, apiKey);
        }
        return restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> delete(String path, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null) {
            headers.set(HEADER, apiKey);
        }
        return restTemplate.exchange(path, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
    }

    // linked_tc: TC-FUNC-order-013-20
    @Test
    void noApiKey_toApiEndpoint_returns401Unauthorized() {
        ResponseEntity<String> res = get("/api/orders", null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).contains("\"error\"").contains("unauthorized");
    }

    // linked_tc: TC-FUNC-order-013-21
    @Test
    void invalidApiKey_toApiEndpoint_returns401Unauthorized() {
        ResponseEntity<String> res = get("/api/orders", "totally-bogus-key");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).contains("unauthorized");
    }

    // linked_tc: TC-FUNC-order-013-22
    @Test
    void adminApiKey_toApiEndpoint_returns200() {
        ResponseEntity<String> res = get("/api/orders", ADMIN_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // linked_tc: TC-FUNC-order-013-22 (admin은 타 회원 자원도 통과 — IDOR 대상 아님)
    @Test
    void adminApiKey_foreignMemberQuery_passesThroughWithout403() {
        ResponseEntity<String> res = get("/api/cart?memberId=" + OTHER_MEMBER_ID, ADMIN_KEY);

        assertThat(res.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // linked_tc: TC-FUNC-order-013-23
    @Test
    void memberApiKey_ownMemberIdViaQuery_returns200() {
        ResponseEntity<String> res = get("/api/cart?memberId=" + OWN_MEMBER_ID, MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // linked_tc: TC-FUNC-order-013-23 (경로 3경로 중 하나 — 자기 자신 조회는 허용)
    @Test
    void memberApiKey_ownMemberIdViaPath_returns200() {
        ResponseEntity<String> res = get("/api/members/" + OWN_MEMBER_ID, MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // linked_tc: TC-FUNC-order-013-24 (경로 1/3 — 쿼리 파라미터)
    @Test
    void memberApiKey_foreignMemberIdViaQuery_returns403Forbidden() {
        ResponseEntity<String> res = get("/api/cart?memberId=" + OTHER_MEMBER_ID, MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody()).contains("\"error\"").contains("forbidden");
    }

    // linked_tc: TC-FUNC-order-013-24 (경로 2/3 — JSON 본문. 필터가 거부하므로 CartService.addItem은
    // 호출되지 않고 DB 변경도 없다 — 스트림 재소비 없이 요청이 컨트롤러에 도달조차 못 함을 실증)
    @Test
    void memberApiKey_foreignMemberIdViaJsonBody_returns403Forbidden() {
        String body = "{\"memberId\":\"" + OTHER_MEMBER_ID + "\",\"sku\":\"SKU-1001\",\"qty\":1}";
        ResponseEntity<String> res = postJson("/api/cart/items", MEMBER_0001_KEY, body);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody()).contains("forbidden");
    }

    // linked_tc: TC-FUNC-order-013-24 (경로 3/3 — 경로변수 /api/members/{id})
    @Test
    void memberApiKey_foreignMemberIdViaPathVariable_returns403Forbidden() {
        ResponseEntity<String> res = get("/api/members/" + OTHER_MEMBER_ID, MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody()).contains("forbidden");
    }

    // 본문 memberId가 자기 자신이면(스트림 재소비 문제 없이) 정상적으로 컨트롤러까지 도달해야 한다.
    // qty=0으로 보내 requireValidQty(DB 접근 이전 검증)에 걸리게 함으로써 부작용 없이 확인한다.
    // 응답 본문에 CartService의 실제 사유 문구가 담겨 있어야 한다 — 필터가 본문을 다 읽고 재생하지
    // 못했다면 Spring의 "본문 없음" 오류(다른 문구)로 400이 났을 것이므로, 이 문구 존재가 곧
    // 스트림 재소비 수리가 실효함을 증명한다(회귀: DB 변경 없음 — requireValidQty가 DB 접근보다 먼저).
    @Test
    void memberApiKey_ownMemberIdViaJsonBody_passesFilterReachesControllerWithBodyIntact() {
        String body = "{\"memberId\":\"" + OWN_MEMBER_ID + "\",\"sku\":\"SKU-1001\",\"qty\":0}";
        ResponseEntity<String> res = postJson("/api/cart/items", MEMBER_0001_KEY, body);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).contains("수량은 1 이상이어야 합니다");
    }

    // memberId를 아예 참조하지 않는 엔드포인트는 member 키로도 통과한다(IDOR 대조 대상 없음).
    @Test
    void memberApiKey_endpointWithoutMemberIdReference_passesThrough() {
        ResponseEntity<String> res = get("/api/products", MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // linked_tc: TC-FUNC-order-013-25 / R-4 — 화면 라우트는 무인증 유지(내부적으로 서비스 계층을
    // 직접 호출하는 경로도 이 필터의 영향을 받지 않는다 — /api/** 밖은 shouldNotFilter로 완전 스킵).
    @Test
    void screenRoute_withoutApiKey_returns200() {
        ResponseEntity<String> res = get("/product/list", null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // linked_tc: TC-FUNC-order-013-25 — memberId 쿼리를 쓰는 화면(장바구니)도 무인증 그대로.
    @Test
    void cartScreenRoute_withoutApiKey_returns200() {
        ResponseEntity<String> res = get("/cart?memberId=" + OWN_MEMBER_ID, null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ===== SR-217(FUNC-order-017) — GET /api/members/grades는 /api/** 안에 있지만 공개 정보라
    // 화이트리스트로 인증을 완전히 스킵한다(shouldNotFilter가 evaluateMemberScope보다 먼저 실행돼
    // MEMBERS_ITEM_PATH 정규식이 "grades"를 memberId로 오인해 거부하는 경로를 타지 않는다).
    @Test
    void membersGradesRoute_withoutApiKey_returns200() {
        ResponseEntity<String> res = get("/api/members/grades", null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("\"code\":\"GOLD\"").contains("\"discountRate\"");
    }

    // 회귀: 무인증뿐 아니라 member 키(비-admin)로도 그대로 통과해야 한다(공개 정보라 소유권
    // 판정 자체가 없음 — evaluateMemberScope에 도달하지 않으므로 403 오탐이 없어야 한다).
    @Test
    void membersGradesRoute_withMemberApiKey_returns200() {
        ResponseEntity<String> res = get("/api/members/grades", MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ===== SR-231(FUNC-member-002) — 회원가입 인증코드 발송은 아직 회원이 아닌 사용자가 호출한다
    // (API 키를 발급받을 대상 자체가 없음). MEMBER_GRADES_PATH와 동일한 이유로 화이트리스트에
    // 추가했다 — 무키로도 통과해야 한다.
    // linked_tc: TC-FUNC-member-002-06
    @Test
    void signupVerificationCodeRoute_withoutApiKey_returns200() {
        ResponseEntity<String> res = postJson("/api/members/signup/verification-codes", null,
                "{\"target\":\"newbie@example.com\"}");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("\"channel\":\"EMAIL\"");
    }

    // 이 필터가 default-deny라 화이트리스트에 없으면 무키 요청이 401이 되는지 회귀 확인한다
    // (등급 조회와 동일한 화이트리스트 의존성 — round1 QA FAIL 권고#3 정정: MEMBERS_ITEM_PATH
    // 정규식은 세그먼트 1개만 매치해 이 경로(3세그먼트)와는 애초에 무관하다).
    @Test
    void signupVerificationCodeRoute_withMemberApiKey_returns200() {
        ResponseEntity<String> res = postJson("/api/members/signup/verification-codes", MEMBER_0001_KEY,
                "{\"target\":\"01099998888\"}");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("\"channel\":\"SMS\"");
    }

    // ===== SR-231(FUNC-member-003) — 가입 요청 API(POST /api/members/signup)도 아직 회원이
    // 아닌 사용자가 호출한다 — MEMBER_SIGNUP_REQUEST_PATH 화이트리스트(정확 일치)에 무키로도
    // 통과해야 한다. 인증 판정을 거쳐야 하므로 먼저 MemberSignupVerificationDao로 유효한
    // 코드를 직접 적재한다(FUNC-member-002 소유 파일은 이 테스트에서 수정하지 않고 그 공개
    // API만 호출 — 기존 cleanUpSignupVerificationRows 관례와 동일).
    // linked_tc: TC-FUNC-member-003-08
    @Test
    void signUpRoute_withoutApiKey_returns201() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        memberSignupVerificationDao.writeCode("EMAIL", "signup-whitelist-check@example.com", "111111",
                now.plusMinutes(5), now);

        ResponseEntity<String> res = postJson("/api/members/signup", null,
                "{\"target\":\"signup-whitelist-check@example.com\",\"code\":\"111111\","
                        + "\"password\":\"abcd1234\",\"name\":\"화이트리스트\"}");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode node = objectMapper.readTree(res.getBody());
        createdMemberIds.add(node.get("memberId").asText());
    }

    @Test
    void signUpRoute_withMemberApiKey_returns201() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        memberSignupVerificationDao.writeCode("SMS", "01099997777", "222222", now.plusMinutes(5), now);

        ResponseEntity<String> res = postJson("/api/members/signup", MEMBER_0001_KEY,
                "{\"target\":\"01099997777\",\"code\":\"222222\","
                        + "\"password\":\"abcd1234\",\"name\":\"화이트리스트\"}");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode node = objectMapper.readTree(res.getBody());
        createdMemberIds.add(node.get("memberId").asText());
    }

    // ===== SR-231(FUNC-member-001) — 회원가입 화면(GET /member/signup)도 아직 회원이 아닌
    // 사용자가 연다. MEMBER_SIGNUP_SCREEN_PATH 화이트리스트(정확 일치)에 무키로도 통과해야
    // 한다. 이 경로 문자열은 MEMBER_VIEW_PATH 정규식(^/member/([^/]+)$, 마이페이지 소유권
    // 대조)과도 매치되지만, isOpenRoute가 shouldNotFilter에서 먼저 걸러 그 분기까지 가지
    // 않는다 — 아래 두 테스트가 그 사실을 대조로 실증한다(회원 마이페이지는 무키 401이지만
    // 회원가입 화면은 무키 200).
    // linked_tc: TC-FUNC-member-001-01
    @Test
    void signupScreenRoute_withoutApiKey_returns200() {
        ResponseEntity<String> res = get("/member/signup", null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("회원가입");
    }

    @Test
    void signupScreenRoute_withMemberApiKey_returns200() {
        ResponseEntity<String> res = get("/member/signup", MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ===== SR-207(FUNC-order-004) round 7 재작업 — QA round1 FAIL 필수2/3: 화면 라우트
    // /member/{memberId}도 evaluateMemberScope 소유권 대조 대상이다(isOpenRoute 추가 아님).
    // MemberViewController가 REST를 거치지 않고 MemberService를 직접 재사용하므로, 이 대조가
    // 없으면 member 키가 타인 PII+최근 주문 이력을 열람할 수 있다(IDOR) — round1은 이 3건이 없었다.
    @Test
    void memberScreenRoute_withoutApiKey_returns401() {
        ResponseEntity<String> res = get("/member/" + OWN_MEMBER_ID, null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).contains("\"error\"").contains("unauthorized");
    }

    @Test
    void memberScreenRoute_memberApiKeyOwnMemberId_returns200() {
        ResponseEntity<String> res = get("/member/" + OWN_MEMBER_ID, MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void memberScreenRoute_memberApiKeyForeignMemberId_returns403Forbidden() {
        ResponseEntity<String> res = get("/member/" + OTHER_MEMBER_ID, MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody()).contains("\"error\"").contains("forbidden");
    }

    // ===== round 6 재작업 — QA r5 FAIL 필수1: 경로 정규화 우회 회귀(무키, 전부 401 기대) =====
    // 종전 raw getRequestURI() 판정은 이 요청들을 "/api/ 아님"으로 오판해 필터를 스킵시켰고,
    // Tomcat은 디코딩·정규화된 경로로 컨트롤러까지 라우팅해 인증이 전면 우회됐다(QA r5 실측: 200).
    // linked_tc: TC-FUNC-order-013-27
    @Test
    void percentEncodedApiPrefix_lowerA_noApiKey_returns401() throws Exception {
        HttpResponse<String> res = rawRequest("GET", "/%61pi/orders");

        assertThat(res.statusCode()).isEqualTo(401);
    }

    // linked_tc: TC-FUNC-order-013-27
    @Test
    void percentEncodedApiPrefix_lowerP_noApiKey_returns401() throws Exception {
        HttpResponse<String> res = rawRequest("GET", "/a%70i/orders");

        assertThat(res.statusCode()).isEqualTo(401);
    }

    // linked_tc: TC-FUNC-order-013-27
    @Test
    void percentEncodedApiPrefix_bothChars_noApiKey_returns401() throws Exception {
        HttpResponse<String> res = rawRequest("GET", "/%61%70i/orders");

        assertThat(res.statusCode()).isEqualTo(401);
    }

    // linked_tc: TC-FUNC-order-013-27 — 서블릿 matrix 파라미터(;a=b)
    @Test
    void matrixParamOnApiSegment_noApiKey_returns401() throws Exception {
        HttpResponse<String> res = rawRequest("GET", "/api;a=b/orders");

        assertThat(res.statusCode()).isEqualTo(401);
    }

    // linked_tc: TC-FUNC-order-013-27 — 빈 matrix 파라미터(;)
    @Test
    void emptyMatrixParamOnApiSegment_noApiKey_returns401() throws Exception {
        HttpResponse<String> res = rawRequest("GET", "/api;/orders");

        assertThat(res.statusCode()).isEqualTo(401);
    }

    // linked_tc: TC-FUNC-order-013-27 — 중복 슬래시
    @Test
    void doubleLeadingSlash_noApiKey_returns401() throws Exception {
        HttpResponse<String> res = rawRequest("GET", "//api/orders");

        assertThat(res.statusCode()).isEqualTo(401);
    }

    // linked_tc: TC-FUNC-order-013-27 — QA r5가 "전 회원 주문원장 CSV 전량 반출"로 실측한 벡터
    @Test
    void percentEncodedApiPrefix_exportEndpoint_noApiKey_returns401() throws Exception {
        HttpResponse<String> res = rawRequest("GET", "/%61pi/orders/export");

        assertThat(res.statusCode()).isEqualTo(401);
    }

    // linked_tc: TC-FUNC-order-013-27 — QA r5가 "서비스 계층까지 도달(409)"로 실측한 취소 우회 벡터
    @Test
    void percentEncodedApiPrefix_cancelEndpoint_noApiKey_returns401() throws Exception {
        HttpResponse<String> res = rawRequest("PATCH", "/%61pi/orders/" + FOREIGN_ORDER_NO + "/cancel");

        assertThat(res.statusCode()).isEqualTo(401);
    }

    // ===== round 6 재작업 — QA r5 FAIL 필수2: IDOR을 자원 소유권 검증으로 =====
    // linked_tc: TC-FUNC-order-013-28
    @Test
    void memberApiKey_foreignOrderDetail_returns403Forbidden() {
        ResponseEntity<String> res = get("/api/orders/" + FOREIGN_ORDER_NO, MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody()).contains("forbidden");
    }

    // linked_tc: TC-FUNC-order-013-28
    @Test
    void memberApiKey_foreignOrderDeliveries_returns403Forbidden() {
        ResponseEntity<String> res = get("/api/orders/" + FOREIGN_ORDER_NO + "/deliveries", MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // linked_tc: TC-FUNC-order-013-28 — 필터가 403으로 선차단하므로 서비스 계층(취소 상태 규칙)에는
    // 도달조차 하지 않는다(DB 무변경 — FOREIGN_ORDER_NO는 이미 CANCELED라 만약 도달해도 409였겠지만,
    // 이 테스트는 "도달 자체를 막는다"를 검증한다).
    @Test
    void memberApiKey_foreignOrderCancel_returns403Forbidden() throws Exception {
        HttpResponse<String> res = rawRequest("PATCH", "/api/orders/" + FOREIGN_ORDER_NO + "/cancel", MEMBER_0001_KEY);

        assertThat(res.statusCode()).isEqualTo(403);
    }

    // linked_tc: TC-FUNC-order-013-28 — 자기 주문은 여전히 열람 가능(회귀: 소유권 검증이 과잉차단 아님)
    @Test
    void memberApiKey_ownOrderDetail_returns200() {
        ResponseEntity<String> res = get("/api/orders/" + OWN_ORDER_NO, MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // linked_tc: TC-FUNC-order-013-28 — /api/members(목록)는 admin 전용
    @Test
    void memberApiKey_membersList_returns403Forbidden() {
        ResponseEntity<String> res = get("/api/members", MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // linked_tc: TC-FUNC-order-013-28
    @Test
    void adminApiKey_membersList_returns200() {
        ResponseEntity<String> res = get("/api/members", ADMIN_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // linked_tc: TC-FUNC-order-013-28 — /api/orders/export는 admin 전용
    @Test
    void memberApiKey_ordersExport_returns403Forbidden() {
        ResponseEntity<String> res = get("/api/orders/export", MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // linked_tc: TC-FUNC-order-013-28
    @Test
    void adminApiKey_ordersExport_returns200() {
        ResponseEntity<String> res = get("/api/orders/export", ADMIN_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // linked_tc: TC-FUNC-order-013-28 — admin은 여전히 타 회원 주문도 통과(IDOR 대상 아님, 회귀)
    @Test
    void adminApiKey_foreignOrderDetail_returns200() {
        ResponseEntity<String> res = get("/api/orders/" + FOREIGN_ORDER_NO, ADMIN_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // linked_tc: TC-FUNC-order-013-28 — memberId 토큰이 어디에도 없어도 "전 회원 5건"이 아니라
    // 자기 자신으로 강제 축소된 결과만 반환한다(QA r5: 전 회원 5건 200이 문제였던 그 축).
    @Test
    void memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders() {
        ResponseEntity<String> res = get("/api/orders", MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = res.getBody();
        assertThat(body).contains("\"memberId\":\"" + OWN_MEMBER_ID + "\"");
        assertThat(body).doesNotContain("\"orderNo\":\"" + FOREIGN_ORDER_NO + "\"");
        assertThat(body).doesNotContain("\"memberId\":\"" + OTHER_MEMBER_ID + "\"");
    }

    // ===== round 6 재작업 — QA r5 FAIL 필수3: 필터·컨트롤러 파라미터 뷰 통일(다중값 전량 대조) =====
    // linked_tc: TC-FUNC-order-013-29
    @Test
    void memberApiKey_duplicateMemberIdQueryParams_returns403Forbidden() {
        ResponseEntity<String> res = get(
                "/api/cart?memberId=" + OWN_MEMBER_ID + "&memberId=" + OTHER_MEMBER_ID, MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody()).contains("forbidden");
    }

    // ===== SR-232(FUNC-member-005) — 로그인 API(POST /api/members/login)도 아직 인증되지 않은
    // 사용자가 호출한다. MEMBER_LOGIN_PATH 화이트리스트(정확 일치)에 무키로도 통과해야 한다.
    // 실제 계정 없이도 "필터를 통과해 컨트롤러/서비스까지 도달했는가"만 확인한다 — 응답 본문이
    // 필터의 {"error":"unauthorized"}가 아니라 서비스 계약({"code":"MBR-4011",...})이면 그
    // 증거다(둘 다 401이라 상태 코드만으로는 구분되지 않는다).
    @Test
    void loginRoute_withoutApiKey_reachesServiceReturns401WithMbrCode() throws Exception {
        String email = uniqueLoginProbeEmail();
        loginProbeEmails.add(email);
        HttpResponse<String> res = rawPostJson("/api/members/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"whatever\"}");

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(res.body()).as("필터가 아니라 서비스가 응답했다는 증거").contains("MBR-4011");
    }

    @Test
    void loginRoute_withMemberApiKey_reachesServiceReturns401WithMbrCode() throws Exception {
        String email = uniqueLoginProbeEmail();
        loginProbeEmails.add(email);
        HttpResponse<String> res = rawPostJson("/api/members/login", MEMBER_0001_KEY,
                "{\"email\":\"" + email + "\",\"password\":\"whatever\"}");

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(res.body()).contains("MBR-4011");
    }

    // ===== round 8(SR-232/FUNC-member-005, 사람 확인 3) — ApiKeyAuthFilter DB 폴백 =====
    // 정적 lab.api-keys 맵에 없는 키라도 MEMBER_API_KEYS에 발급돼 있으면 그 회원 스코프로
    // 통과해야 한다(자가가입 회원이 로그인 후 API를 쓸 수 있게 하는 것이 이 사람 확인의 목적).
    @Test
    void dbIssuedApiKey_ownResource_returns200() {
        memberApiKeyDao.issueIfAbsent(OWN_MEMBER_ID, DB_FALLBACK_KEY, LocalDateTime.now());

        ResponseEntity<String> res = get("/api/cart?memberId=" + OWN_MEMBER_ID, DB_FALLBACK_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // DB로 발급된 키도 IDOR 대조를 그대로 받는다 — admin이 아니라 일반 member 스코프이기 때문.
    @Test
    void dbIssuedApiKey_foreignResource_returns403Forbidden() {
        memberApiKeyDao.issueIfAbsent(OWN_MEMBER_ID, DB_FALLBACK_KEY, LocalDateTime.now());

        ResponseEntity<String> res = get("/api/cart?memberId=" + OTHER_MEMBER_ID, DB_FALLBACK_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // 회귀 — 정적 맵 미스이면서 DB에도 없는 키(기존 invalidApiKey_toApiEndpoint_returns401Unauthorized
    // 와 동일 사례)는 이 DB 폴백 추가 이후에도 여전히 401이어야 한다(폴백이 fail-open이 아님).
    @Test
    void unknownApiKey_notInStaticMapNorDb_returns401Unauthorized() {
        ResponseEntity<String> res = get("/api/orders", "totally-unknown-key-not-anywhere");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).contains("unauthorized");
    }

    // 회귀 — 사람 확인 3 조건: 기존 정적 맵 경로(admin·M-0001)는 이 DB 폴백 추가로 코드·동작이
    // 전혀 바뀌지 않는다. 이 클래스 상단의 adminApiKey_toApiEndpoint_returns200 /
    // memberApiKey_ownMemberIdViaQuery_returns200 이 이미 그 회귀를 담당하지만, DB 폴백 코드가
    // 정적 맵 히트 이후에는 전혀 실행되지 않는다는 것을 이 스위트 안에서 한 번 더 명시적으로
    // 확인한다(정적 맵 히트 시 DB 조회 자체가 없다 — MEMBER_API_KEYS에 아무 것도 넣지 않은
    // 상태에서 admin/M-0001 키가 여전히 통과해야 함).
    @Test
    void staticMapKeys_stillWorkUnaffectedByDbFallback_regressionCheck() {
        ResponseEntity<String> adminRes = get("/api/orders", ADMIN_KEY);
        ResponseEntity<String> memberRes = get("/api/cart?memberId=" + OWN_MEMBER_ID, MEMBER_0001_KEY);

        assertThat(adminRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(memberRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ===== round 9(재작업 지시 2, QA FAIL 필수2) — 탈퇴·폐기 회원의 DB 발급 키는 더 이상
    // 통과하지 않는다. 실측 근거: MEMBER_API_KEYS에 MEMBERS에 없는 회원의 키가 남아 있었고
    // (M-9003-RACE), 이 폴백이 그 키로 GET /api/orders를 통과시켰다. selectMemberIdByApiKey를
    // MEMBERS 조인 + del_yn='N'으로 좁혀 이 취약점을 닫는다.
    @Test
    void dbIssuedApiKey_withdrawnMember_returns401Unauthorized() {
        memberApiKeyDao.issueIfAbsent(WITHDRAWN_MEMBER_ID, DB_FALLBACK_KEY_WITHDRAWN_MEMBER, LocalDateTime.now());

        ResponseEntity<String> res = get("/api/cart?memberId=" + WITHDRAWN_MEMBER_ID, DB_FALLBACK_KEY_WITHDRAWN_MEMBER);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).contains("\"error\"").contains("unauthorized");
    }

    // round 9 — revoked_at이 세팅된 키(FUNC-006의 로그아웃 처리를 흉내낸 것)도 더 이상 통과하지
    // 않아야 한다. MemberApiKeyDao에는 폐기 메서드가 없어(006 소관) JdbcTemplate으로 직접
    // revoked_at을 세팅한다.
    @Test
    void dbIssuedApiKey_revoked_returns401Unauthorized() {
        memberApiKeyDao.issueIfAbsent(OWN_MEMBER_ID, DB_FALLBACK_KEY_REVOKED, LocalDateTime.now());
        jdbcTemplate.update("UPDATE MEMBER_API_KEYS SET revoked_at = ? WHERE api_key = ?",
                LocalDateTime.now(), DB_FALLBACK_KEY_REVOKED);

        ResponseEntity<String> res = get("/api/cart?memberId=" + OWN_MEMBER_ID, DB_FALLBACK_KEY_REVOKED);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).contains("\"error\"").contains("unauthorized");
    }

    // ===== SR-232(FUNC-member-006) — refresh(무인증 화이트리스트)/logout(인증 필요) 회귀 =====
    // 계획 "폴백·우회 경로의 자격 판정" 절 근거: /api/members/sessions/logout(두 세그먼트)은
    // MEMBERS_ITEM_PATH(^/api/members/([^/]+)$, 한 세그먼트 전용)와 매치되지 않아 정규식 충돌이
    // 없다 — 이 테스트가 그 경로 설계를 실측으로 확인한다.

    // linked_tc: TC-FUNC-member-006-21 — refresh는 화이트리스트라 무키로도 필터를 통과해
    // 서비스까지 도달한다(응답이 필터의 {"error":"unauthorized"}가 아니라 서비스 계약
    // {"code":"MBR-4012",...}이면 그 증거 — loginRoute_* 회귀와 동일한 판별 방식).
    @Test
    void sessionsRefreshRoute_withoutApiKey_reachesServiceReturns401WithMbrCode() throws Exception {
        HttpResponse<String> res = rawPostJson("/api/members/sessions/refresh", null,
                "{\"refreshToken\":\"no-such-refresh-token\"}");

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(res.body()).as("필터가 아니라 서비스가 응답했다는 증거").contains("MBR-4012");
    }

    @Test
    void sessionsRefreshRoute_withMemberApiKey_reachesServiceReturns401WithMbrCode() throws Exception {
        HttpResponse<String> res = rawPostJson("/api/members/sessions/refresh", MEMBER_0001_KEY,
                "{\"refreshToken\":\"no-such-refresh-token\"}");

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(res.body()).contains("MBR-4012");
    }

    // linked_tc: TC-FUNC-member-006-22 — logout은 화이트리스트에 없다 — 무키 요청은 필터가
    // 컨트롤러 도달 전에 401(unauthorized)로 거부한다(MEMBERS_ITEM_PATH 정규식과 무관하게
    // 인증 자체가 막는지 — 계획 "테스트" 절).
    @Test
    void sessionsLogoutRoute_withoutApiKey_returns401Unauthorized() throws Exception {
        HttpResponse<String> res = rawPostJson("/api/members/sessions/logout", null, "");

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(res.body()).contains("\"error\"").contains("unauthorized");
    }

    // 유효한 member 키로는 필터를 통과해 컨트롤러까지 도달한다(idempotent 204 — 실제 발급 여부와
    // 무관하게 로그아웃 자체는 항상 성공, MemberSessionIntegrationTest가 발급된 키로 실제 폐기
    // 확인까지 검증한다. 이 테스트는 필터 통과 자체만 확인).
    @Test
    void sessionsLogoutRoute_withMemberApiKey_returns204NoContent() throws Exception {
        HttpResponse<String> res = rawPostJson("/api/members/sessions/logout", MEMBER_0001_KEY, "");

        assertThat(res.statusCode()).isEqualTo(204);
    }

    // ===== SR-235(FUNC-member-011) round 2(재작업 지시 5, QA CONCERNS 권고#5) — /api/members/me/addresses
    // 는 URL에 memberId가 없어 SCOPE_TO_SELF 메커니즘(ForcedMemberIdRequest)이 ?memberId= 쿼리를
    // 호출자 자신으로 강제한다. 코드는 getParameter/getParameterValues/getParameterMap 3종을 모두
    // 오버라이드해 이미 안전하지만(계획 "순서·보안" 절), 그 사실을 고정하는 회귀 테스트가 없었다 —
    // 이후 오버라이드 하나가 지워져도 조용히 뚫리지 않도록 여기서 실증한다.

    // 목록(GET): ?memberId=타인을 실어도 응답은 호출자 자신의 목록으로 강제되어 타인 배송지가
    // 노출되지 않는다(403도 아니고, 타인 데이터도 아님 — SCOPE_TO_SELF는 차단이 아니라 축소).
    @Test
    void memberAddressesRoute_foreignMemberIdQueryParam_forcedToSelf_excludesForeignData() {
        Long foreignAddressId = insertOtherMemberAddress();
        try {
            ResponseEntity<String> res = get(
                    "/api/members/me/addresses?memberId=" + OTHER_MEMBER_ID, MEMBER_0001_KEY);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(res.getBody())
                    .as("SCOPE_TO_SELF가 memberId를 강제해 타인 배송지가 목록에 노출되면 안 된다")
                    .doesNotContain("\"addressId\":" + foreignAddressId);
        } finally {
            jdbcTemplate.update("DELETE FROM MEMBER_ADDRESSES WHERE address_id = ?", foreignAddressId);
        }
    }

    // 수정(PUT)·삭제(DELETE): ?memberId=타인을 실어도 addressId 소유자는 여전히 강제된 자기 자신
    // 기준으로 판정되므로, 실제로는 타인 소유인 addressId는 "없음"과 동일한 404 MBR-4041이어야
    // 한다(존재 노출 금지 — selectOwned가 강제된 memberId와 address_id를 함께 본다).
    @Test
    void memberAddressesRoute_foreignMemberIdParamWithForeignAddressId_updateAndDelete_return404() {
        Long foreignAddressId = insertOtherMemberAddress();
        try {
            String updateBody = "{\"recipient\":\"위조수정시도\",\"phone\":\"01099998888\",\"zipcode\":\"12345\","
                    + "\"roadAddress\":\"서울시 위조로 1\",\"detailAddress\":\"1동 1호\"}";
            ResponseEntity<String> putRes = putJson(
                    "/api/members/me/addresses/" + foreignAddressId + "?memberId=" + OTHER_MEMBER_ID,
                    MEMBER_0001_KEY, updateBody);
            ResponseEntity<String> deleteRes = delete(
                    "/api/members/me/addresses/" + foreignAddressId + "?memberId=" + OTHER_MEMBER_ID,
                    MEMBER_0001_KEY);

            assertThat(putRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(putRes.getBody()).contains("MBR-4041");
            assertThat(deleteRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(deleteRes.getBody()).contains("MBR-4041");
        } finally {
            jdbcTemplate.update("DELETE FROM MEMBER_ADDRESSES WHERE address_id = ?", foreignAddressId);
        }
    }

    // ===== SR-235(FUNC-member-012) — 우편번호(도로명) 검색 API도 /api/** 전역 필터 아래 있다.
    // ZipcodeControllerTest(@WebMvcTest + AdminApiKeyTestConfig)는 슬라이스 제약상 "무키" 상태를
    // 만들 수 없어(그 클래스 javadoc 참고) 이 실 서버 통합 테스트가 대신 검증한다.
    @Test
    void zipcodesRoute_withoutApiKey_returns401Unauthorized() {
        ResponseEntity<String> res = get("/api/zipcodes?q=강남", null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).contains("\"error\"").contains("unauthorized");
    }

    // 회귀 — 이 엔드포인트는 회원 스코프 강제(SCOPE_TO_SELF)가 없다(memberId 파라미터 자체가
    // 없음, STORY "순서·보안" 절) — member 키로도 그대로 통과해야 한다.
    @Test
    void zipcodesRoute_withMemberApiKey_returns200() {
        ResponseEntity<String> res = get("/api/zipcodes?q=강남", MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ===== SR-307(SR-307.1) — 상품 조회 GET 공개 경로 =====
    // 랩 시드 실측 SKU-1001(CartDaoTest 주석: 스탠딩데스크, 재고12) 재사용. 전부 순수 GET
    // 또는 무매핑 POST(컨트롤러 도달 전 필터 차단)라 DB 변경이 없어 별도 정리가 불필요하다.
    private static final String SEED_SKU = "SKU-1001";
    private static final String UNKNOWN_SKU = "SKU-NOPE-9999";

    // linked_tc: TC-FUNC-order-013-60
    @Test
    void productsListRoute_withoutApiKey_returns200() {
        ResponseEntity<String> res = get("/api/products", null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // linked_tc: TC-FUNC-order-013-61
    @Test
    void productsItemRoute_withoutApiKey_existingSku_returns200() {
        ResponseEntity<String> res = get("/api/products/" + SEED_SKU, null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("\"sku\":\"" + SEED_SKU + "\"");
    }

    // 회귀 — 공개돼도 오류 계약은 불변: 없는 SKU는 여전히 404다.
    // linked_tc: TC-FUNC-order-013-62
    @Test
    void productsItemRoute_withoutApiKey_unknownSku_returns404() {
        ResponseEntity<String> res = get("/api/products/" + UNKNOWN_SKU, null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // 회귀 — 쓰기는 공개가 아니다. ProductController에 POST 매핑이 없어 필터를 통과해도
    // 405가 나겠지만, 이 테스트는 "필터 단계에서 이미 401로 막힌다"를 확인한다(405로 새면
    // 필터가 경로만 보고 메서드를 무시했다는 뜻이므로 그 자체가 실패 신호).
    // linked_tc: TC-FUNC-order-013-63
    @Test
    void productsWritePlaceholder_withoutApiKey_returns401() throws Exception {
        HttpResponse<String> res = rawPostJson("/api/products", null, "{}");

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(res.body()).contains("\"error\"").contains("unauthorized");
    }

    // round 2(재작업 지시 4) — 목록 경로(POST)뿐 아니라 단건 경로의 비-GET도 공개가 아니다.
    // ProductController에 PATCH 매핑이 없어 통과해도 405가 나겠지만, 이 테스트도 위와 동일하게
    // "필터 단계에서 이미 401"을 확인한다.
    // linked_tc: TC-FUNC-order-013-64
    @Test
    void productsItemPlaceholder_patchWithoutApiKey_returns401() throws Exception {
        HttpResponse<String> res = rawRequest("PATCH", "/api/products/" + SEED_SKU);

        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(res.body()).contains("\"error\"").contains("unauthorized");
    }

    // round 2(재작업 지시 5, 이번 라운드 수정 대상 아님 — TODO만) — HEAD는 "GET".equalsIgnoreCase가
    // 거짓이라 여전히 401이다. AS-IS에서도 401이었으므로 회귀는 아니다(Spring MVC가 @GetMapping에
    // HEAD를 자동 매핑하는 것과는 별개로, 이 필터의 무키 예외 조건이 HEAD를 GET으로 보지 않는다).
    // linked_tc: TC-FUNC-order-013-65
    @Test
    void productsListRoute_headWithoutApiKey_returns401() throws Exception {
        HttpResponse<String> res = rawRequest("HEAD", "/api/products");

        assertThat(res.statusCode()).isEqualTo(401);
    }

    // round 2(재작업 지시 2, QA FAIL 필수2 권고1) — 필터를 통째로 스킵하지 않고 공개 경로에서도
    // 항상 실행되므로, 키를 보낸 요청의 기존 동작(evaluateMemberScope 일반 규칙 (a) — 쿼리 memberId가
    // 자기 자신이 아니면 거부)이 그대로 유지된다. round1 구현(isOpenRoute 화이트리스트)에서는 필터가
    // 완전히 스킵돼 이 요청이 200으로 새는 회귀가 있었다(QA 실측) — 그 회귀가 되돌아오지 않는지
    // 고정한다.
    // linked_tc: TC-FUNC-order-013-66
    @Test
    void productsListRoute_withMemberApiKeyAndOtherMemberIdParam_returns403Forbidden() {
        ResponseEntity<String> res = get("/api/products?memberId=" + OTHER_MEMBER_ID, MEMBER_0001_KEY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody()).contains("\"error\"").contains("forbidden");
    }

    // round 2(재작업 지시 3, QA FAIL 필수2 권고2) — PRODUCT_ITEM_PATH를 SKU- 접두사 형식으로 좁힌
    // 효과를 직접 확인한다. "export"는 오늘 존재하지 않는 가상의 서브리소스이며(ProductController에
    // 매핑 없음), SKU- 접두사가 없어 PRODUCT_ITEM_PATH에 매치되지 않으므로 공개 화이트리스트 밖 —
    // default-deny로 무키 401이어야 한다. 200이 나오면 정규식이 다시 "세그먼트 1개면 전부 공개"로
    // 되돌아갔다는 뜻이므로 그 자체가 실패 신호다.
    // linked_tc: TC-FUNC-order-013-67
    @Test
    void productsItemRoute_virtualSubresourceExport_withoutApiKey_returns401() {
        ResponseEntity<String> res = get("/api/products/export", null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).contains("\"error\"").contains("unauthorized");
    }

    private Long insertOtherMemberAddress() {
        MemberAddress address = new MemberAddress();
        address.setMemberId(OTHER_MEMBER_ID);
        address.setRecipient("강제축소회귀테스트-" + java.util.UUID.randomUUID());
        address.setPhone("01099998888");
        address.setPhoneNorm("01099998888");
        address.setZipcode("12345");
        address.setRoadAddress("서울시 테스트로 1");
        address.setDetailAddress("1동 1호");
        address.setIsDefault("N");
        LocalDateTime now = LocalDateTime.now();
        address.setLastUsedAt(now);
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        memberAddressDao.insertAddress(address);
        return address.getAddressId();
    }
}
