// linked_func: FUNC-order-013, FUNC-order-004, FUNC-order-017, FUNC-member-002, FUNC-member-003,
// FUNC-member-001, FUNC-member-005, FUNC-member-006, FUNC-member-008 —
// SR-204 R-1/R-2 (round 6) + SR-207 화면 소유권 편입(round 7) + SR-217 등급 조회 무인증
// 화이트리스트 추가 + SR-231 회원가입 인증코드 발송 무인증 화이트리스트 추가 + SR-231 가입 요청
// API 무인증 화이트리스트 추가(FUNC-member-003) + SR-231 회원가입 화면 무인증 화이트리스트
// 추가(FUNC-member-001) + SR-232 로그인 API 무인증 화이트리스트 추가 + DB 발급 회원 API 키
// 폴백 조회 추가(round 8, FUNC-member-005, 사람 확인 3) + SR-232 refresh API 무인증 화이트리스트
// 추가(FUNC-member-006 — logout은 인증 필요라 화이트리스트에 넣지 않는다. evaluateMemberScope/
// MEMBERS_ITEM_PATH는 건드리지 않음 — 경로 설계로 충돌 회피, STORY "폴백·우회 경로" 절 참고) +
// SR-234 비밀번호 재설정 코드 요청 API 무인증 화이트리스트 추가(FUNC-member-008, 정확 일치 경로만) +
// SR-234 비밀번호 재설정 확정 API 무인증 화이트리스트 추가(FUNC-member-009, 정확 일치 경로만) +
// SR-235 배송지 CRUD API — 기존 SCOPE_TO_SELF 메커니즘 재사용(FUNC-member-011, 인증 필요라
// 화이트리스트는 아님 — evaluateMemberScope에 패턴 1개만 추가)
// spec: docs/00_FUNC/stories/STORY-FUNC-order-013.md, docs/00_FUNC/stories/STORY-FUNC-order-004.md,
// docs/00_FUNC/stories/STORY-FUNC-order-017.md, docs/00_FUNC/stories/STORY-FUNC-member-002.md,
// docs/00_FUNC/stories/STORY-FUNC-member-003.md, docs/00_FUNC/stories/STORY-FUNC-member-001.md,
// docs/00_FUNC/stories/STORY-FUNC-member-005.md, docs/00_FUNC/stories/STORY-FUNC-member-006.md,
// docs/00_FUNC/stories/STORY-FUNC-member-008.md, docs/00_FUNC/stories/STORY-FUNC-member-009.md,
// docs/00_FUNC/stories/STORY-FUNC-member-011.md
package com.sm.lab.shop.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sm.lab.shop.dao.MemberApiKeyDao;
import com.sm.lab.shop.dao.OrderDao;
import com.sm.lab.shop.domain.Order;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SR-204 R-1/R-2 — {@code /api/**} 전역 {@code X-Api-Key} 인증 + member 스코프 키의 IDOR 차단.
 *
 * <p>키→회원 매핑은 코드에 하드코딩하지 않고 application.yml {@code lab.api-keys}에서 읽는다(D13).
 * {@code "*"}는 admin(전 회원 자원 통과), 그 외 값은 그 memberId 자신의 자원만 허용한다.
 *
 * <p><b>round 6 재작업(QA r5 FAIL) — 경로 판정 정규화 + default-deny</b>: 종전 {@link #shouldNotFilter}는
 * 원문(raw) {@code getRequestURI()}로 {@code "/api/"} 시작 여부를 판정했는데, 서블릿 컨테이너(Tomcat)는
 * 디코딩·정규화(percent-encoding 해제, {@code ;matrix=param} 제거)된 경로로 실제 라우팅을 결정한다.
 * 그 결과 {@code /%61pi/orders}·{@code /api;a=b/orders} 같은 요청은 필터에서는 "/api/ 아님"으로 보여
 * 스킵되지만 컨트롤러에는 정상 도달해 인증이 전면 우회됐다(무키 200 실측). 이번 수정은
 * {@link HttpServletRequest#getServletPath()}/{@link HttpServletRequest#getPathInfo()}(둘 다 서블릿
 * 스펙상 디코딩·정규화된 값 — Tomcat이 실제 매핑에 쓰는 것과 동일한 경로)로 판정 기준을 교체하고,
 * 판정 모델을 "{@code /api/}로 시작하면 검사"(default-allow)에서 "화면 화이트리스트({@link #isOpenRoute})
 * 외 전부 검사"(default-deny)로 뒤집었다 — 경로 정규화에 실패(모호)하면 스킵하지 않고 인증을 요구한다.
 *
 * <p><b>round 6 재작업 — IDOR을 토큰 대조에서 자원 소유권 검증으로</b>: 종전에는 요청 안의
 * {@code memberId} 토큰 문자열만 대조했으므로, 토큰이 없는 자원(주문 상세·배송·취소·목록·CSV
 * 내보내기·회원목록)은 member 키로 전부 통과했다(QA r5 실측). 이번 수정은 {@link #evaluateMemberScope}로
 * 자원별 소유권을 판정한다: {@code /api/members}(목록)·{@code /api/orders/export}는 admin 전용,
 * {@code /api/orders/{orderNo}}(및 그 하위 경로)는 {@link OrderDao#selectByOrderNo}로 조회한
 * {@code member_id}와 대조, {@code /api/orders}(목록, GET)는 memberId 파라미터가 없으면 자기 자신으로
 * 강제 축소(scope-to-self)한다. {@code OrderDao}는 새 조회 규칙을 추가하지 않고 기존 메서드를
 * 재사용한다.
 *
 * <p><b>round 6 재작업 — 파라미터 뷰 통일</b>: 필터는 {@code getParameterValues(memberId)} 전량을
 * 대조한다(종전 첫 값만 봐서 {@code ?memberId=자기&memberId=타인}이 통과할 수 있었음).
 *
 * <p><b>round 8(SR-232/FUNC-member-005, 사람 확인 3) — 정적 맵 미스 시 DB 폴백 조회</b>: 정적
 * {@code lab.api-keys} 맵(application.yml)에는 admin·M-0001 두 항목뿐이라, 로그인 API(FUNC-005)로
 * 자가가입 회원이 로그인에 성공해도 이후 어떤 {@code /api/**} 요청도 인증할 방법이 없었다.
 * {@link #doFilterInternal}은 이제 정적 맵 조회가 실패했을 때만(요청당 최대 1회)
 * {@link MemberApiKeyDao#selectMemberIdByApiKey}로 폴백 조회한다 — 기존 정적 맵 경로(admin·
 * M-0001)는 이 추가로 코드·동작이 전혀 바뀌지 않는다(정적 맵이 먼저 확인되고, 맞으면 DB
 * 조회 자체가 일어나지 않는다). 그 폴백마저 실패하면(키가 없거나 DAO를 쓸 수 없는 슬라이스
 * 테스트 컨텍스트) 종전과 동일한 401 unauthorized로 응답한다.
 *
 * <p><b>SR-307(항목 SR-307.1) — 상품 조회 GET 공개 경로 허용</b>: 앱 서빙 쇼핑 화면(/shop)이 인증
 * 없이 상품 목록·단건을 그려야 하는데, 이 필터는 default-deny라 무키 GET {@code /api/products}·
 * {@code /api/products/{sku}}도 지금까지 401이었다. 컨트롤러·서비스·DAO·DDL은 변경하지 않는다 —
 * 인증 정책 1개 파일만 고친다.
 *
 * <p><b>round 2 재작업(QA round1 FAIL)</b> — 처음에는 이 두 경로를 {@link #isOpenRoute}(=
 * {@link #shouldNotFilter}) 화이트리스트에 넣었으나, 그러면 필터 전체가 스킵돼
 * {@link #evaluateMemberScope}까지 도달하지 못한다 — 확정 문답 "키를 보낸 요청의 동작 불변"을
 * 어기고 member 키로 {@code GET /api/products?memberId=타인}을 보내도 종전 403이 200으로 바뀌는
 * 회귀가 났다(QA 실측). 이번 구조에서는 공개 상품 GET 경로를 {@link #isOpenRoute}에 넣지 않는다 —
 * 필터는 이 경로에서도 항상 실행되고, {@link #doFilterInternal}에서 "키 자체가 없을 때만"(무키)
 * {@link #isPublicProductReadPath} &amp;&amp; GET 조합을 인증 요구의 예외로 취급해 그대로 통과시킨다.
 * 키를 보낸 요청(무효 키 포함)은 이 예외를 타지 않고 종전과 동일한 401/스코프 판정 경로를 그대로
 * 거친다 — 즉 인증이 "요구되지 않을" 뿐, 인가(소유권) 판정 자체는 이 경로에도 항상 적용된다.
 *
 * <p><b>round 2 재작업 — 가상 서브리소스 자동 공개 방지</b>: {@code PRODUCT_ITEM_PATH}를 처음에는
 * {@code ^/api/products/([^/]+)$}(세그먼트 1개면 전부 매치)로 뒀는데, 이러면 오늘 없는
 * {@code GET /api/products/export}·{@code /inventory} 같은 관리용 서브리소스가 나중에 추가돼도
 * 아무도 이 필터를 손대지 않아 자동으로 무인증 공개된다(QA 권고 — 이 코드베이스가
 * {@code ORDERS_EXPORT_PATH}를 {@code ORDER_RESOURCE_PATH}보다 먼저 검사하는 것과 동일한 함정).
 * 이 SR은 가상의 미래 서브리소스 이름을 전부 열거할 수 없으므로, 대신 이 시스템에서 실제로
 * 관찰되는 SKU 형식(시드·테스트 전역 {@code SKU-}로 시작하는 영숫자·하이픈 — {@code SCH-ORD-005}의
 * {@code sku VARCHAR(20) PK}와도 상충하지 않음)으로 정규식을 좁혔다 — {@code export}처럼 이 접두사가
 * 없는 세그먼트는 매치되지 않고 default-deny(401)로 떨어진다. 실패 모드가 fail-open(잘못 공개)이
 * 아니라 fail-closed(잘못 인증 요구)라 이 SR의 보안 목표와 부합한다.
 *
 * <p><b>round 7 재작업(SR-207/FUNC-order-004, QA round1 FAIL)</b>: 신규 화면 라우트
 * {@code GET /member/{memberId}}(MemberViewController)는 REST {@code /api/members/{id}}를 거치지
 * 않고 {@code MemberService}를 직접 재사용한다. round1은 이 라우트를 {@link #isOpenRoute}
 * 화이트리스트에 추가하려 했으나, 그러면 소유권 대조 자체가 통째로 우회되어 무인증 타인
 * PII(이름·연락처·등급·가입일)+최근 주문 이력 열람(IDOR)이 열린다(QA 실측 지적). 화이트리스트는
 * 그대로 두고(무키 요청은 여전히 default-deny로 401), {@link #evaluateMemberScope}에
 * {@code /member/{id}} 경로 패턴을 추가해 {@code /api/members/{id}}(b-1)와 동일한 의미로 판정한다:
 * admin 키는 통과, member 키는 경로변수가 자기 자신일 때만 허용한다.
 *
 * <p><b>스트림 재소비 문제</b>: 서블릿 {@code InputStream}은 1회용이라, 필터가 JSON 본문을 먼저
 * 읽어 memberId를 검사하면 이후 컨트롤러의 {@code @RequestBody} 파싱은 빈 스트림을 보게 된다.
 * 이를 피하기 위해 본문이 있는 요청만 {@link CachedBodyRequest}로 감싸 전체를 한 번만 읽어
 * 캐싱하고, 그 래핑된 요청을 체인에 그대로 전달해 다운스트림이 같은 캐시본을 다시 읽게 한다
 * (하우스 스타일 최소침습 — 별도 프레임워크·Security 도입 없이 서블릿 필터 단독으로 해결).
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Api-Key";
    private static final String ADMIN_SCOPE = "*";
    private static final String MEMBER_ID_FIELD = "memberId";

    private static final String MEMBERS_LIST_PATH = "/api/members";
    private static final String ORDERS_LIST_PATH = "/api/orders";
    private static final String ORDERS_EXPORT_PATH = "/api/orders/export";
    // SR-217(FUNC-order-017) — 등급 코드·이름·할인율(%) 조회는 공개 정보(인증 불필요, SR 확정
    // 문답). isOpenRoute 화이트리스트에 넣어 필터를 완전히 스킵한다(evaluateMemberScope의
    // MEMBERS_ITEM_PATH 정규식이 "grades"를 memberId 경로변수로 오인해 admin 아니면 거부하는
    // 것을 사전에 피한다 — shouldNotFilter가 먼저 검사되므로 그 정규식 분기까지 가지 않는다).
    private static final String MEMBER_GRADES_PATH = "/api/members/grades";
    // SR-231(FUNC-member-002) — 회원가입 인증코드 발송은 아직 회원이 아닌 사용자가 호출한다(API
    // 키를 발급받을 대상 자체가 없음). MEMBER_GRADES_PATH와 동일한 이유의 예외로 화이트리스트에
    // 추가한다. 이 필터는 default-deny라 화이트리스트에 없으면 무키 요청이 401이 되므로
    // shouldNotFilter에서 먼저 걸러 무인증으로 열어야 한다(round1 QA FAIL 권고#3 정정 — 종전
    // "MEMBERS_ITEM_PATH 정규식이 signup을 memberId로 오인한다"는 서술은 틀렸다: 그 정규식
    // ^/api/members/([^/]+)$ 은 세그먼트 1개만 매치하므로 이 3세그먼트 경로에는 애초에 매치되지
    // 않는다).
    private static final String MEMBER_SIGNUP_VERIFICATION_CODE_PATH = "/api/members/signup/verification-codes";
    // SR-231(FUNC-member-003) — 가입 요청 API(POST /api/members/signup)도 아직 회원이 아닌
    // 사용자가 호출한다(MEMBER_SIGNUP_VERIFICATION_CODE_PATH와 동일한 이유의 예외). 정확 일치
    // 한 경로만 화이트리스트에 추가한다 — /api/members/signup/verification-codes(위 경로)는
    // 이 문자열과 다르므로 서로 영향이 없고, /api/members/{id}(MEMBERS_ITEM_PATH, 조회) 쪽
    // evaluateMemberScope 판정도 이 필드 추가로 전혀 바뀌지 않는다(shouldNotFilter가 먼저
    // 걸러 그 분기까지 가지 않음 — MEMBER_SIGNUP_VERIFICATION_CODE_PATH 주석과 동일 근거).
    private static final String MEMBER_SIGNUP_REQUEST_PATH = "/api/members/signup";
    // SR-231(FUNC-member-001) — 회원가입 화면(GET /member/signup)도 아직 회원이 아닌 사용자가
    // 연다. MEMBER_SIGNUP_VERIFICATION_CODE_PATH/MEMBER_SIGNUP_REQUEST_PATH와 동일한 이유의
    // 예외 — 화이트리스트가 없으면 default-deny로 401이 되어 신규 가입자가 화면 자체를 열 수
    // 없다. 이 경로 문자열은 아래 MEMBER_VIEW_PATH 정규식(^/member/([^/]+)$)과도 매치되지만,
    // isOpenRoute는 shouldNotFilter에서 evaluateMemberScope보다 먼저 검사되므로 그 정규식
    // (마이페이지 소유권 대조) 분기까지 가지 않는다(MEMBER_GRADES_PATH가 MEMBERS_ITEM_PATH를
    // 피하는 것과 동일한 원리 — round1 QA FAIL 권고#3 정정 코멘트 참고).
    private static final String MEMBER_SIGNUP_SCREEN_PATH = "/member/signup";
    // SR-232(FUNC-member-005) — 로그인 전에는 아직 API 키가 없다(회원가입 관련 화이트리스트
    // 3건과 동일한 이유의 예외). 정확 일치 경로만 화이트리스트에 추가한다 — 다른 /api/members/*
    // 화이트리스트 항목들과 마찬가지로 evaluateMemberScope의 MEMBERS_ITEM_PATH 정규식
    // (^/api/members/([^/]+)$, 세그먼트 1개만 매치)과는 무관하다.
    private static final String MEMBER_LOGIN_PATH = "/api/members/login";
    // SR-232(FUNC-member-006) — refresh 전에는 아직(또는 더 이상) 유효한 API 키가 없을 수 있다
    // (로그아웃 직후 자동 재로그인 시나리오 포함) — 로그인·가입 화이트리스트와 동일한 이유의
    // 예외다. 자격 판정은 필터가 아니라 MemberSessionService(토큰 해시 조회 + 회원 탈퇴 필터)가
    // 한다(STORY "폴백·우회 경로의 자격 판정" 절). 로그아웃(logout)은 반대로 인증이 필요하므로
    // 이 화이트리스트에 넣지 않는다 — 정확 일치 경로만 추가(다른 항목과 동일 관례).
    private static final String MEMBER_SESSIONS_REFRESH_PATH = "/api/members/sessions/refresh";
    // SR-234(FUNC-member-008) — 비밀번호를 잊은 사용자가 로그인 전에 호출한다(회원가입·로그인
    // 화이트리스트와 동일한 이유의 예외). 정확 일치 경로만 추가 — evaluateMemberScope의
    // MEMBERS_ITEM_PATH 정규식(^/api/members/([^/]+)$, 세그먼트 1개만 매치)과는 무관하다.
    private static final String MEMBER_PASSWORD_RESET_CODE_PATH = "/api/members/password-resets/codes";
    // SR-234(FUNC-member-009) — 확정 API도 로그인 전 사용자가 호출한다(MEMBER_PASSWORD_RESET_CODE_PATH와
    // 동일한 이유의 예외). 정확 일치 경로만 추가 — evaluateMemberScope의 MEMBERS_ITEM_PATH 정규식
    // (^/api/members/([^/]+)$, 세그먼트 1개만 매치)과는 무관하다. 자격 판정은 필터가 아니라
    // MemberPasswordResetConfirmationService(원자 확정 UPDATE + del_yn 필터 회원 조회)가 한다.
    private static final String MEMBER_PASSWORD_RESET_CONFIRM_PATH = "/api/members/password-resets/confirmations";
    private static final Pattern MEMBERS_ITEM_PATH = Pattern.compile("^/api/members/([^/]+)$");
    // SR-207(FUNC-order-004) round 7 — 화면 GET /member/{memberId}(MemberViewController, REST 미경유)도
    // /api/members/{id}와 동일한 소유권 대조를 적용한다. isOpenRoute에 추가하지 않는 이유는 클래스
    // 상단 "round 7 재작업" 노트 참고.
    private static final Pattern MEMBER_VIEW_PATH = Pattern.compile("^/member/([^/]+)$");
    // /api/orders/{orderNo} 및 그 하위(/cancel, /deliveries 등) — export는 위 ORDERS_EXPORT_PATH로
    // 먼저 걸러지므로 이 패턴이 "export"를 orderNo로 오인해 DB 조회를 시도하는 일은 없다.
    private static final Pattern ORDER_RESOURCE_PATH = Pattern.compile("^/api/orders/([^/]+)(?:/.*)?$");
    // SR-235(FUNC-member-011) — /api/members/me/addresses(및 하위 /{id}, /{id}/default)는 URL에
    // memberId가 없는 "me" 자원이다. 기존 GET /api/orders(memberId 토큰이 없는 목록 조회)의
    // SCOPE_TO_SELF 메커니즘을 그대로 재사용한다(STORY "순서·보안" 절 — 새 클래스·새 우회 경로를
    // 만들지 않는다). 이 정규식은 MEMBERS_ITEM_PATH(세그먼트 1개 전용, ^/api/members/([^/]+)$)와
    // 애초에 매치되지 않는다 — /api/members/me/addresses는 세그먼트가 3개 이상이다.
    private static final Pattern MEMBER_ME_ADDRESSES_PATH =
            Pattern.compile("^/api/members/me/addresses(?:/.*)?$");
    // SR-307(SR-307.1) — 상품 조회 GET만 공개(isOpenRoute 화이트리스트가 아니라 doFilterInternal의
    // "무키 예외" 판정에서만 쓰인다 — 클래스 상단 "round 2 재작업" 노트 참고). 목록은 정확 일치 —
    // "/api/members/"·"/member/"·"/api/orders/" 계열과 접두사가 겹치지 않아 다른 화이트리스트·
    // evaluateMemberScope 패턴과 매치 충돌이 없다.
    private static final String PRODUCTS_LIST_PATH = "/api/products";
    // round 2(QA round1 FAIL 권고) — 세그먼트 1개면 전부 열던 ^/api/products/([^/]+)$ 대신, 이
    // 시스템에서 실제 관찰되는 SKU 형식(SKU- 접두사 + 영숫자·하이픈, 시드·테스트 전역 SKU-1001·
    // SKU-TEST 등)으로 좁혔다 — "export"·"inventory"처럼 이 접두사가 없는 미래 서브리소스 세그먼트는
    // 매치되지 않아 default-deny(401)로 남는다. 클래스 상단 "가상 서브리소스 자동 공개 방지" 노트 참고.
    private static final Pattern PRODUCT_ITEM_PATH = Pattern.compile("^/api/products/(SKU-[A-Za-z0-9-]+)$");

    private enum Decision { ALLOW, DENY, SCOPE_TO_SELF }

    private final Map<String, String> apiKeys;
    private final ObjectMapper objectMapper;
    private final OrderDao orderDao;
    private final MemberApiKeyDao memberApiKeyDao;

    public ApiKeyAuthFilter(Environment environment, ObjectMapper objectMapper,
                            ObjectProvider<OrderDao> orderDaoProvider,
                            ObjectProvider<MemberApiKeyDao> memberApiKeyDaoProvider) {
        this.apiKeys = Binder.get(environment)
                .bind("lab.api-keys", Bindable.mapOf(String.class, String.class))
                .orElseGet(Collections::emptyMap);
        this.objectMapper = objectMapper;
        // ObjectProvider로 선택적 주입: OrderDao(MyBatis @Mapper)가 없는 슬라이스 테스트 컨텍스트
        // (@WebMvcTest)에서도 필터 빈 생성이 깨지지 않게 한다. 실제로 주문 소유권 판정이 필요한데
        // orderDao가 없으면(getIfAvailable()==null) default-deny로 거부한다 — evaluateMemberScope 참조.
        this.orderDao = orderDaoProvider.getIfAvailable();
        // round 8(FUNC-member-005) — 동일한 이유로 선택적 주입. 슬라이스 테스트에서 이 DAO가
        // 없으면(null) DB 폴백 조회를 건너뛰고 정적 맵 결과(미스=401)를 그대로 쓴다.
        this.memberApiKeyDao = memberApiKeyDaoProvider.getIfAvailable();
    }

    /**
     * round 6: 원문 URI가 아니라 서블릿이 실제 라우팅에 쓰는 디코딩·정규화 경로로 화이트리스트를
     * 판정한다(default-deny — 화이트리스트 밖은 전부 인증 대상). 경로를 얻지 못하면(모호) 스킵하지
     * 않는다(= 인증 요구).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = resolveNormalizedPath(request);
        return path != null && isOpenRoute(path);
    }

    /**
     * 화면 라우트(R-4, 무인증 유지) + SR-217 공개 API(등급 조회) 화이트리스트. 그 외 전부(오탐 포함)
     * 인증 대상(default-deny) — SR-307 공개 상품 조회 GET은 <b>여기 포함되지 않는다</b>(round 2
     * 재작업 — 클래스 상단 노트 참고): 이 화이트리스트에 넣으면 필터 자체가 스킵되어 키를 보낸
     * 요청의 소유권 판정까지 함께 우회된다. 그 예외는 {@link #doFilterInternal}에서 "무키일 때만"
     * 별도로 처리한다.
     */
    private static boolean isOpenRoute(String path) {
        return path.equals("/error")
                // 쇼핑몰 첫 화면 — 루트 접속은 /shop/으로 보낸다(ShopIndexController.root). 리다이렉트만 한다.
                || path.equals("/")
                || path.equals("/favicon.ico")
                || path.equals("/cart") || path.startsWith("/cart/")
                || path.startsWith("/order/")
                || path.startsWith("/product/")
                // SR-301 — 쇼핑 SPA 정적 서빙(/shop): 화면 진입점이라 /cart·/order·/product와 동일한
                // 이유로 무인증. "/shopkeeper"처럼 "/shop"으로 시작만 하고 "/shop/"은 아닌 경로가
                // 오매칭되지 않게 정확 일치 + "/shop/" 접두 조합만 허용한다(기존 /cart 관례와 동일).
                // 실제 데이터 접근은 여전히 /api/**(불변)에서만 일어나므로 이 화이트리스트가
                // 자격 판정을 우회하지 않는다.
                || path.equals("/shop") || path.startsWith("/shop/")
                // SR-306(#2) — 상품 대표 이미지(정적 SVG)는 회원 소유권 판정 대상이 아닌 순수 정적
                // 자산이다(소유자가 없는 리소스) — /shop과 동일한 이유로 화이트리스트. SR-307 #1이
                // 경고한 "판정이 필요한 /api/**를 화이트리스트로 스킵"과는 조건이 다르다(자격 판정
                // 자체가 없는 자원). /api/products·/api/products/{sku}의 무키 GET 공개 판정(SR-307)은
                // 이 화이트리스트가 아니라 doFilterInternal의 별도 예외 처리이므로 이 추가로 바뀌지
                // 않는다. round 2 재작업(QA r1 CONCERNS 권고2) — 접두사를 "/images/"에서 실제 서빙
                // 위치인 "/images/products/"로 좁혔다: 훗날 /images/ 아래(형제 경로)에 자격 판정이
                // 필요한 매핑(업로드 API 등)이 생겨도 이 화이트리스트가 그 경로까지 자동으로 삼키지
                // 않게 하기 위함 — 현재 정적 자산은 전부 /images/products/ 아래에만 있어 동작 변화 없음.
                || path.startsWith("/images/products/")
                || path.equals(MEMBER_GRADES_PATH)
                || path.equals(MEMBER_SIGNUP_VERIFICATION_CODE_PATH)
                || path.equals(MEMBER_SIGNUP_REQUEST_PATH)
                || path.equals(MEMBER_SIGNUP_SCREEN_PATH)
                || path.equals(MEMBER_LOGIN_PATH)
                || path.equals(MEMBER_SESSIONS_REFRESH_PATH)
                || path.equals(MEMBER_PASSWORD_RESET_CODE_PATH)
                || path.equals(MEMBER_PASSWORD_RESET_CONFIRM_PATH);
    }

    /**
     * SR-307(SR-307.1) round 2 — {@link #isOpenRoute}가 아니라 {@link #doFilterInternal}의 "무키
     * 예외" 판정 1곳에서만 쓰인다(클래스 상단 Javadoc "round 2 재작업" 노트 참고). 경로만으로 열지
     * 않는다 — 호출부가 메서드(GET)와 "키 자체가 없음"을 함께 확인해야만 이 결과를 쓴다.
     */
    private static boolean isPublicProductReadPath(String path) {
        return PRODUCTS_LIST_PATH.equals(path) || PRODUCT_ITEM_PATH.matcher(path).matches();
    }

    /**
     * {@link HttpServletRequest#getServletPath()}/{@link HttpServletRequest#getPathInfo()}는 서블릿
     * 스펙상 디코딩·정규화된 값이다(원문 {@code getRequestURI()}와 달리 percent-encoding·
     * {@code ;matrix} 파라미터가 해소된 상태) — Tomcat이 실제 디스패치에 쓰는 것과 동일한 경로이므로
     * 이 값으로 판정하면 인코딩 우회가 성립하지 않는다.
     */
    private static String resolveNormalizedPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath == null) {
            return null;
        }
        String pathInfo = request.getPathInfo();
        String path = (pathInfo != null) ? servletPath + pathInfo : servletPath;
        return path.isEmpty() ? "/" : path;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // SR-307(SR-307.1) round 2 — 화이트리스트(isOpenRoute)가 아니라 이 메서드 안에서 판정하므로
        // 경로를 맨 앞에서 한 번만 구한다(클래스 상단 "round 2 재작업" 노트 참고).
        String path = resolveNormalizedPath(request);
        String key = request.getHeader(HEADER);
        String scope = (key == null) ? null : apiKeys.get(key);
        if (scope == null && key != null) {
            // round 8(FUNC-member-005, 사람 확인 3) — 정적 맵 미스일 때만(요청당 최대 1회) DB
            // 폴백 조회. key==null(헤더 자체가 없음)이면 조회할 값이 없으므로 시도하지 않는다.
            scope = resolveMemberApiKeyFromDb(key);
        }
        if (scope == null) {
            // SR-307(SR-307.1) round 2 — "무키" 요청만 공개 상품 조회 GET의 인증 요구를 면제한다.
            // 키를 보낸 요청(무효 키 포함)은 이 예외를 타지 않고 아래 401로 그대로 떨어진다 —
            // 확정 문답 "키를 보낸 요청의 동작 불변"을 지키기 위한 구분(클래스 상단 Javadoc 참고).
            if (key == null && path != null && isPublicProductReadPath(path)
                    && "GET".equalsIgnoreCase(request.getMethod())) {
                chain.doFilter(request, response);
                return;
            }
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized");
            return;
        }
        if (ADMIN_SCOPE.equals(scope)) {
            chain.doFilter(request, response);
            return;
        }

        if (path == null) {
            // 경로 정규화 실패 — 인가 판정 근거가 없다. default-deny.
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "forbidden");
            return;
        }

        HttpServletRequest toUse = hasJsonBody(request) ? new CachedBodyRequest(request) : request;

        Decision decision = evaluateMemberScope(toUse, path, scope);
        if (decision == Decision.DENY) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "forbidden");
            return;
        }
        if (decision == Decision.SCOPE_TO_SELF) {
            toUse = new ForcedMemberIdRequest(toUse, scope);
        }
        chain.doFilter(toUse, response);
    }

    /**
     * round 6 — 토큰 문자열 대조가 아니라 자원별 소유권을 판정한다. 자원 유형을 특정 못 하는 그 외
     * {@code /api/**}(cart·products 등)는 종전과 동일하게 요청 안의 memberId 토큰(쿼리 전량·JSON
     * 본문)을 대조하는 일반 규칙으로 처리한다(회귀 없음).
     */
    private Decision evaluateMemberScope(HttpServletRequest request, String path, String ownMemberId) {
        // (c) 회원 목록·주문 CSV 내보내기는 admin 전용 — memberId 토큰 유무와 무관하게 거부.
        if (MEMBERS_LIST_PATH.equals(path) || ORDERS_EXPORT_PATH.equals(path)) {
            return Decision.DENY;
        }

        // (b-0) /api/members/me/addresses 및 하위(SR-235, FUNC-member-011) — memberId 토큰이
        // 없는 "me" 자원이라 GET /api/orders와 동일하게 무조건 자기 자신으로 강제 축소한다(메서드
        // 무관 — CRUD 전 경로가 동일 정책). MEMBERS_ITEM_PATH보다 먼저 검사하지만 두 정규식은
        // 세그먼트 수가 달라 애초에 겹치지 않는다(클래스 상단 주석 참고).
        if (MEMBER_ME_ADDRESSES_PATH.matcher(path).matches()) {
            return Decision.SCOPE_TO_SELF;
        }

        // (b-1) /api/members/{id} — 경로변수 대 소유 memberId 대조(기존 채널 유지).
        Matcher membersItem = MEMBERS_ITEM_PATH.matcher(path);
        if (membersItem.matches()) {
            return membersItem.group(1).equals(ownMemberId) ? Decision.ALLOW : Decision.DENY;
        }

        // (b-1') 화면 GET /member/{id}(SR-207 round 7) — 위 (b-1)과 동일 의미. REST를 경유하지
        // 않는 화면 컨트롤러라 여기서 검증하지 않으면 member 키가 타인 자원도 통과한다(IDOR).
        Matcher memberView = MEMBER_VIEW_PATH.matcher(path);
        if (memberView.matches()) {
            return memberView.group(1).equals(ownMemberId) ? Decision.ALLOW : Decision.DENY;
        }

        // (b-2) /api/orders/{orderNo} 및 하위(cancel/deliveries) — 토큰이 아니라 DB의 실제 주문
        // 소유자(member_id)와 대조한다(D13이 "orders 계열"을 대조 지점으로 명시).
        Matcher orderResource = ORDER_RESOURCE_PATH.matcher(path);
        if (orderResource.matches()) {
            if (orderDao == null) {
                return Decision.DENY; // 소유권을 확인할 방법이 없음 — default-deny
            }
            Order order = orderDao.selectByOrderNo(orderResource.group(1));
            return (order != null && ownMemberId.equals(order.getMemberId())) ? Decision.ALLOW : Decision.DENY;
        }

        // (a) 일반 규칙: 쿼리 memberId 전량(getParameterValues) + JSON 본문 memberId 대조.
        String[] queryMemberIds = request.getParameterValues(MEMBER_ID_FIELD);
        if (queryMemberIds != null) {
            for (String v : queryMemberIds) {
                if (!ownMemberId.equals(v)) {
                    return Decision.DENY;
                }
            }
        }
        String bodyMemberId = extractBodyMemberId(request);
        if (bodyMemberId != null && !ownMemberId.equals(bodyMemberId)) {
            return Decision.DENY;
        }

        // (a) memberId 토큰이 어디에도 없는 주문 목록 조회(GET /api/orders)는 기본 거부 대신
        // 자기 자신으로 강제 축소한다 — 회귀: "GET /api/orders가 자기 주문만 반환".
        boolean noMemberIdToken = (queryMemberIds == null || queryMemberIds.length == 0) && bodyMemberId == null;
        if (ORDERS_LIST_PATH.equals(path) && "GET".equalsIgnoreCase(request.getMethod()) && noMemberIdToken) {
            return Decision.SCOPE_TO_SELF;
        }

        return Decision.ALLOW;
    }

    /**
     * round 8(FUNC-member-005, 사람 확인 3) — 정적 맵 미스일 때만 호출되는 DB 폴백. DB로 발급된
     * 키는 항상 member 스코프(admin이 아님)다 — {@link MemberApiKeyDao#selectMemberIdByApiKey}가
     * 반환하는 값은 그대로 {@code memberId}이므로 그 뒤 {@link #evaluateMemberScope} 판정에
     * lab-member-0001-key와 동일하게 들어간다. DAO를 쓸 수 없으면(슬라이스 테스트 등) null —
     * 호출부가 종전과 동일한 401로 응답한다.
     */
    private String resolveMemberApiKeyFromDb(String key) {
        if (memberApiKeyDao == null) {
            return null;
        }
        return memberApiKeyDao.selectMemberIdByApiKey(key);
    }

    private String extractBodyMemberId(HttpServletRequest request) {
        if (!(request instanceof CachedBodyRequest cached)) {
            return null;
        }
        byte[] body = cached.getBody();
        if (body.length == 0) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode memberId = (node == null) ? null : node.get(MEMBER_ID_FIELD);
            return (memberId != null && !memberId.isNull()) ? memberId.asText() : null;
        } catch (IOException ex) {
            // 파싱 불가 본문은 필터가 판단하지 않는다 — 다운스트림 @RequestBody 바인딩이
            // 정직하게 400으로 거부한다(필터가 검증 실패를 흡수·은폐하지 않음).
            return null;
        }
    }

    private boolean hasJsonBody(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT).contains("json")
                && request.getContentLengthLong() != 0;
    }

    private void writeError(HttpServletResponse response, int status, String error) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("error", error)));
    }

    /**
     * 서블릿 입력 스트림 재소비 문제 해소용 캐싱 래퍼 — 생성 시점에 본문 전체를 한 번만 읽어
     * 바이트 배열로 보관하고, {@link #getInputStream()}/{@link #getReader()} 호출마다 그 캐시로부터
     * 새 스트림을 만들어 반환한다(몇 번을 읽어도 항상 같은 내용).
     */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = StreamUtils.copyToByteArray(request.getInputStream());
        }

        byte[] getBody() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream buffer = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return buffer.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // 동기 처리만 사용 — 비동기 리스너 불필요
                }

                @Override
                public int read() {
                    return buffer.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
        }
    }

    /**
     * round 6 — memberId 토큰이 없는 회원-스코프 목록 조회(GET /api/orders)를 자기 자신으로
     * 강제 축소하기 위한 래퍼. {@code memberId} 파라미터만 소유 회원 값으로 덮어쓰고 나머지는
     * 원본 요청에 위임한다(JSON 본문·기타 파라미터는 영향 없음).
     */
    private static final class ForcedMemberIdRequest extends HttpServletRequestWrapper {
        private final String memberId;

        ForcedMemberIdRequest(HttpServletRequest request, String memberId) {
            super(request);
            this.memberId = memberId;
        }

        @Override
        public String getParameter(String name) {
            return MEMBER_ID_FIELD.equals(name) ? memberId : super.getParameter(name);
        }

        @Override
        public String[] getParameterValues(String name) {
            return MEMBER_ID_FIELD.equals(name) ? new String[] {memberId} : super.getParameterValues(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> merged = new LinkedHashMap<>(super.getParameterMap());
            merged.put(MEMBER_ID_FIELD, new String[] {memberId});
            return Collections.unmodifiableMap(merged);
        }
    }
}
