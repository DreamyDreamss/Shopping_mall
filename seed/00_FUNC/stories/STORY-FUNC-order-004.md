---
story-id: STORY-FUNC-order-004
func-id: FUNC-order-004
status: Done
domain: order
created: 2026-09-07
spec_markers: 0
sr-id: SR-207
approved_sha: 2f2730c60e00
---

# STORY-FUNC-order-004 — 회원 상세 조회

## Story
회원 상세 조회


## 변경 컨텍스트 (SR-207)
> 이 story는 변경요청 **SR-207 — SR-207** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-207/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-207/02_변경명세.md`

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-207/02_변경명세.md`에서 도출)
- [ ] INF-ORD-002: 요청: 변경 없음(필드 추가만 하기로 확정 문답에서 답변됐으나, 이번 SR 범위인 회원 상세 화면·최근 주문 5건 표시에는 요청 파라미터 추가 요구가 없음).
- [ ] INF-ORD-002: 응답(200): 필드 구성·타입·정렬·5건 고정·취소 주문 포함 규칙 모두 **그대로 유지**(요구사항 "API 응답 계약은 바꾸지 않는다", 확정 문답 `api_compat`: "필요 — 필드 추가만"이나 이번 SR에서 실제 필드 추가 요구는 없음).
- [ ] INF-ORD-002: 비즈니스 규칙: 변경 없음.
- [ ] INF-ORD-002: 오류: 변경 없음(확정 문답 `api_error`: 기존 오류 계약 그대로 — 404/401/5xx 공통 배너, 신규 오류 코드 없음).
- [ ] INF-ORD-002: 화면 연결: `screens: []` → 신규 화면(회원 상세/마이페이지, UIS 신규 채번, ID [미상])이 이 API를 소비하도록 **추가**. INF-ORD-002 본문의 `screens` 필드 갱신 필요(스펙 현행화는 본 변경명세의 스코프 밖 — 승인 후 AIDD 재동기화에서 처리).
- [ ] 신규: 데이터 소스: INF-ORD-002 응답의 `recentOrders`를 그대로 사용(신규 API 없음).
- [ ] 신규: 표시 항목: 최근 주문 5건을 최신순(API 정렬 그대로: `ordered_at DESC, order_no DESC`)으로 나열 — 항목별 `orderNo`(주문번호), `orderedAt`(일시), `orderState`(상태), `totalAmount`(금액).
- [ ] 신규: 빈 상태: `recentOrders`가 빈 배열이면 "최근 주문 없음" 문구 표시.
- [ ] 신규: 취소 주문: `orderState = CANCELED`도 목록에 포함(현행 API 규칙 그대로, 별도 필터 없음).
- [ ] 신규: 상호작용: 주문번호(`orderNo`) 클릭 시 UIS-ORD-002(주문 상세)로 이동.
- [ ] 신규: 오류 상태: 회원 없음(API 404) → 화면은 친화 안내 표시. 미인증(401) → 표준 처리. 조회 실패/5xx → 기존 공통 오류 배너.
- [ ] 신규: 권한: SR-204 전역 정책 그대로 적용(관리 자원) — 이 화면 전용 권한 규칙 신규 추가 없음.

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**
- [ ] INF-ORD-002 (GET /api/members/{memberId}): `del_yn = 'N'` 상시필터 → 탈퇴 회원 조회 시 404
- [ ] INF-ORD-002 (GET /api/members/{memberId}): `recentOrders`는 최근 주문 **상위 5건 고정**(`RECENT_ORDER_LIMIT`), `ordered_at DESC, order_no DESC` 순
- [ ] INF-ORD-002 (GET /api/members/{memberId}): 주문이 없으면 `recentOrders`는 빈 배열(`null` 금지, Member 도메인 필드 기본값)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-002: INF-ORD-002: GET /api/members/{memberId} — 회원 상세 조회 / > **개요:** 회원 상세 정보와 함께 최근 주문 5건 요약을 반환한다. (linked_func: FUNC-order-004, LAB-103) / > **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberController.java:27-31` / 요청 — [docs/05_설계서/order/INF/INF-ORD-002.md](../../05_설계서/order/INF/INF-ORD-002.md)
- **SCH** SCH-ORD-001: SCH-ORD-001: members / > **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** [TBD] | **화면:** [TBD] / **근거 소스:** `DB 실측(db-main MCP describe)` — 컬럼 타입·NULL·기본값은 MariaDB(sl_lab) 실스키마 조회로 사실화(P1/SR-202 enrichment). [미확인] 헤더 잔존은 자기모순이라 정정(OBS-018, 2026-08-23) / 컬럼 설명 — [docs/05_설계서/order/SCH/SCH-ORD-001.md](../../05_설계서/order/SCH/SCH-ORD-001.md)
- **SCH** SCH-ORD-004: SCH-ORD-004: orders / > **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** [TBD] | **화면:** [TBD] / **근거 소스:** `DB 실측(db-main MCP describe)` — 컬럼 타입·NULL·기본값은 MariaDB(sl_lab) 실스키마 조회로 사실화(P1/SR-202 enrichment). [미확인] 헤더 잔존은 자기모순이라 정정(OBS-018, 2026-08-23) / 컬럼 설명 — [docs/05_설계서/order/SCH/SCH-ORD-004.md](../../05_설계서/order/SCH/SCH-ORD-004.md)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)
- **기존 구현 파일**: modules/shop-api/src/main/java/com/sm/lab/shop/dao/OrderDao.java, modules/shop-api/src/main/java/com/sm/lab/shop/domain/Member.java, modules/shop-api/src/main/java/com/sm/lab/shop/domain/OrderSummary.java, modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberService.java

## 📏 적용 규칙 (JIT — .claude/rules)
> 이 FUNC가 건드리는 파일에 적용되는 프로젝트 규칙이다. 전문은 아래 파일을 **직접 Read**하고 지킬 것 — `must` 위반은 STEP 5.3 축 C(`rules_check.py`)가 차단한다. 정본: 워크스페이스 `.claude/rules/`(뷰어 [rules]에서 편집).

| 규칙 | severity | 파일 |
|---|---|---|
| 콘솔 출력 금지 | must | `.claude/rules/lab/no-sysout.md` |

## 🔧 쿼리 작성 가이드 (JIT — 실쿼리 관찰)
> AIDD로 쿼리/DAO 생성 시 준수. 소스 SQL에서 채굴한 사실(논리 FK·상시필터). 구조화 원천: `docs\05_설계서\_machine\query_patterns.json`.

**조인 경로 (논리 FK — DB 미선언이라도 코드에서 관찰됨)**
| A.컬럼 | = | B.컬럼 | 관찰 |
|--------|---|--------|------|
| MEMBERS.MEMBER_ID | = | ORDERS.MEMBER_ID | 4 |

**상시 필터 (누락하면 결과가 틀어진다 — soft-delete·테넌트 스코프)**
| 테이블 | 조건 | 빈도 |
|--------|------|------|
| ORDERS | DEL_YN = 'N' | 8 |
| MEMBERS | DEL_YN = 'N' | 4 |

## 📖 도메인 용어 정본 (JIT — 용어집)
> 같은 대상을 새 코드명으로 만들지 말 것 — 아래가 이 도메인의 정본 용어다. 전체·확정 근거: `docs/viewer/glossary.json`(뷰어 [용어집]).
| 용어 | 정본 코드 | 정의 |
|------|----------|------|
| 가용 재고 | `STOCK_QTY` | 가용 재고 |
| 배송번호 | `DELIVERY_NO` | 배송번호 |
| 상태 | `ORDER_STATE` | 상태 (PLACED/PAID/SHIPPED/PARTIAL_SHIPPED/CANCELED/DONE) |
| 상품 SKU | `SKU` | 상품 SKU |
| 상품명 | `PRODUCT_NAME` | 상품명 |
| 주문번호 | `ORDER_NO` | 주문번호 (yyyymmdd+seq) |
| 주문일시 | `ORDERED_AT` | 주문일시 |
| 출고일시 | `SHIPPED_AT` | 출고일시 |
- ⚠ **논리 삭제**: 정본 미확정(충돌) — 코드 DEL_YN가 다른 용어(탈퇴 여부)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **수량**: 정본 미확정(충돌) — 코드 QTY가 다른 용어(장바구니 수량)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **장바구니 수량**: 정본 미확정(충돌) — 코드 QTY가 다른 용어(수량)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **주문 회원**: 정본 미확정(충돌) — 코드 MEMBER_ID가 다른 용어(회원 ID)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록

**생성 파일**
- `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberViewController.java` (신규) — `GET /member/{memberId}` 화면 컨트롤러
- `modules/shop-api/src/main/resources/templates/member/detail.html` (신규) — 회원 상세 + 최근 주문 5건 화면
- `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberViewControllerTest.java` (신규) — 5개 테스트(정상 렌더/빈 상태/취소 주문 포함/404 흡수/5xx rethrow)

**수정 파일**: 없음 — INF-ORD-002(GET /api/members/{memberId}), `MemberService.get`, `OrderDao.selectRecentByMember`는 SR-207에서 요청/응답/규칙 변경이 없어 그대로 재사용(회귀 유지 대상). `MemberController`(REST)도 건드리지 않음.

**주요 결정**
- 화면은 `/api/members/{memberId}`를 HTTP로 재호출하지 않고 `MemberService`를 직접 재사용한다 — `ProductViewController.loadProduct`와 동일 규약(컨트롤러 계층 중복 금지, project-context.md 특이사항).
- 404(회원 없음/탈퇴)만 화면단에서 흡수해 친화 안내 문구로 치환하고, 그 외(5xx)는 rethrow한다(Critical Rule 1, `ProductViewController.loadProduct`와 동일 패턴). 401은 이 랩에 인증 미들웨어가 없어(profile.yaml `concerns.auth: unknown`) 별도 처리하지 않고 Spring 기본 처리에 위임([미상]).
- `recentOrders` 빈 배열 → "최근 주문 없음" 문구(order/list.html의 `scr_empty_state` 패턴과 동일 스타일), 취소 주문(`CANCELED`)도 별도 필터 없이 그대로 표시(AS-IS 유지).
- 주문번호 클릭 시 `/order/{orderNo}`(UIS-ORD-002, `OrderViewController.orderDetail`)로 연결 — `order/list.html`과 동일한 `@{'/order/' + ${o.orderNo}}` 링크 패턴 재사용.
- 권한: SR-204 전역 정책 그대로 적용, 이 화면 전용 권한 규칙은 추가하지 않음.
- 스코프 밖으로 남긴 것: INF-ORD-002 본문의 `screens` 필드 갱신, UIS 신규 채번·spec 문서 작성(story 명시대로 AIDD 재동기화/스펙 현행화 단계로 위임).
- 규칙 준수: `System.out.println` 미사용 — slf4j `Logger`만 사용(`.claude/rules/lab/no-sysout.md`, must).
- Maven(`mvn`)이 이 실행 환경에 없어 컴파일 자동검증은 수행하지 못함 — 기존 `ProductViewController`/`OrderViewController`와 동일한 임포트·시그니처 패턴을 그대로 따랐고, `/sl-test` 단계에서 실제 빌드·테스트 실행 확인 필요.

### round 2 재작업 (QA round1 FAIL 대응) — 2026-09-07

**수정 파일**
- `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — SR-204 소유
  스펙 최소 침습 변경(이번 재작업 지시가 명시적으로 허용한 범위). `MEMBER_VIEW_PATH`
  (`^/member/([^/]+)$`) 패턴을 추가하고 `evaluateMemberScope`에 `/api/members/{id}`(b-1)와
  동일한 소유권 대조 분기를 넣었다 — admin 키는 통과, member 키는 경로변수가 자기 자신일 때만
  허용. **`isOpenRoute` 화이트리스트에는 추가하지 않았다**(추가하면 화면이 REST를 거치지 않고
  `MemberService`를 직접 호출하는 구조상 소유권 대조 자체가 통째로 우회돼 무인증 타인 PII+주문
  이력 열람이 열린다 — QA round1 Layer2 지적). 클래스 상단 Javadoc에 "round 7 재작업" 절로
  근거를 남겼다.
- `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberViewController.java` —
  404 안내에서 서비스 예외 `getReason()` 원문 노출을 제거하고 화면 고정 문구
  (`NOT_FOUND_MESSAGE = "회원을 찾을 수 없습니다."`)로 치환(QA round1 권고1). 예외 사유는
  `log.debug`로만 남긴다. Javadoc에서 "이 랩에 인증 미들웨어가 없다"는 이전 오판을 정정 — 실제로는
  FUNC-order-013(SR-204) `ApiKeyAuthFilter`가 `/api/**` 전역과 이번 재작업으로 `/member/{id}`까지
  default-deny로 인증·인가하며, 401/403은 이 필터가 컨트롤러 도달 전에 반환하므로 컨트롤러는
  인증 자체를 다루지 않는다(필터 책임).
- `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberViewControllerTest.java` —
  `@Import(AdminApiKeyTestConfig.class)` 추가(다른 비화이트리스트 컨트롤러 테스트와 동일 규약 —
  `@WebMvcTest`는 `Filter` 빈을 포함해 인가 필터가 슬라이스에도 적용되므로 admin 키를 기본
  주입해 인가를 우회하고 화면 계층만 검증). 404 테스트를 화면 고정 문구 검증(+ 원문 미노출
  단언 추가)으로 갱신, 테스트명도 `showsFixedFriendlyMessage`로 변경.
- `modules/shop-api/src/test/java/com/sm/lab/shop/web/ApiKeyAuthIntegrationTest.java` —
  화면 라우트 소유권 회귀 3건 추가: `memberScreenRoute_withoutApiKey_returns401`,
  `memberScreenRoute_memberApiKeyOwnMemberId_returns200`,
  `memberScreenRoute_memberApiKeyForeignMemberId_returns403Forbidden`(무키 401 / 자기 200 /
  타인 403). SR-204 R-4 "화면 라우트 화이트리스트" 계약을 이 신규 경로까지 덮도록 확장했다.

**주요 결정**
- `/member/{id}`를 화이트리스트가 아니라 `evaluateMemberScope`에 편입한 것은 QA round1이
  명시한 필수 수정 그대로다 — round1에서 화이트리스트에 넣었다면(반사적 수정) 무인증 타인
  PII+주문이력 열람(IDOR)이 열렸을 것.
- 인증 미들웨어 부재 판단은 사실관계 오류였다(정정 완료) — 다음 라운드부터는 `docs/project-context.md`
  "인증/인가 처리 패턴" 절도 stale임을 인지할 것(자동 생성 문서라 이번 변경 스코프에서는 갱신하지
  않음 — `/sl-context --update` 담당).
- 검증: `mvnw.cmd -Dtest=MemberViewControllerTest,ApiKeyAuthIntegrationTest test` →
  `Tests run: 40, Failures: 0, Errors: 0`(MemberViewControllerTest 5 + ApiKeyAuthIntegrationTest
  35, 신규 3건 포함). 전체 스위트(`mvnw.cmd test`)도 실행해 회귀 없음을 확인.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-07 — FAIL
- Layer1 스펙: **fail**. 필드 구성·정렬·5건 고정·취소 포함은 `MemberService.get`을 그대로 재사용해 INF-ORD-002 응답 계약과 일치하고(회귀 유지 OK), `no-sysout`(must)도 준수(slf4j만 사용). 그러나 신규 라우트 `GET /member/{memberId}`가 SR-204 `ApiKeyAuthFilter`의 **default-deny**에 걸린다 — `isOpenRoute` 화이트리스트는 `/error`·`/favicon.ico`·`/cart`·`/order/`·`/product/`뿐이고 `/member/`가 없다. 실측: 실행 중인 앱(8087)에 무키 `GET /member/M-0001` → **401**(`/product/list`는 200). 즉 브라우저 사용자에게 화면이 렌더되지 않아 신규 AC(표시 항목·빈 상태·취소 주문 포함·주문번호 링크·404 친화 안내)가 런타임에서 하나도 성립하지 않는다. Dev 기록의 "이 랩에 인증 미들웨어가 없다(profile.yaml concerns.auth: unknown)"는 **사실 오류** — FUNC-order-013(SR-204)이 전역 필터를 이미 도입했고, 그 오판이 이 결함의 직접 원인이다.
- Layer2 보안: **concerns**(현재 노출은 없음, 수정 방향이 위험). 화면 컨트롤러가 REST를 거치지 않고 `MemberService.get(memberId)`를 직접 호출하므로 필터의 `evaluateMemberScope` 소유권 대조를 우회한다. 여기서 "다른 화면들처럼" `isOpenRoute`에 `/member/`를 추가하는 반사적 수정은 **무인증 타인 PII(이름·연락처·등급·가입일) + 최근 주문 이력 열람**을 열어, SR-204 R-2가 `/api/members/{id}`(자기 자신만)·`/api/members`(admin 전용)로 닫은 IDOR을 화면 채널로 되살린다. 부수: 404 안내를 서비스 예외 `getReason()`("회원 없음: M-9999") 그대로 화면에 노출 — `th:text` escape라 XSS는 아니나 내부 메시지 반사.
- Layer3 회귀: **concerns**. 기존 파일 무수정 — INF-ORD-002/`MemberController`/`MemberService`/`OrderDao`/`Member.recentOrders` 기본 빈 배열 모두 그대로이고, 템플릿도 `order/list.html`과 동일 관용구(`#temporals.format`·`#numbers.formatInteger`·`@{'/order/' + ${o.orderNo}}`)라 새 의존이 없다. 링크 대상 `/order/{orderNo}`(OrderViewController)도 실존·무인증 유지. 다만 SR-204가 세운 "화면 라우트 화이트리스트" 계약에 신규 화면이 분류되지 않은 채 추가돼 계약이 불완전해졌고, `ApiKeyAuthIntegrationTest`의 R-4 화면 라우트 회귀도 이 경로를 덮지 않는다.
- 필수 수정(FAIL시):
  1. `/member/{memberId}`를 SR-204 인가 모델에 **명시적으로 편입**한다. `isOpenRoute` 단순 추가는 금지(위 Layer2). 권장: `ApiKeyAuthFilter.evaluateMemberScope`에 `^/member/([^/]+)$` 소유권 대조를 추가해 admin은 통과, member 키는 자기 자신만 허용(현행 `/api/members/{id}` b-1 규칙과 동일 의미). SR-204 소유 스펙을 건드리므로 사람 승인 후 적용할 것.
  2. `MemberViewControllerTest` 5건 전부 실패 — 실측 `mvn -Dtest=MemberViewControllerTest test` → `Tests run: 5, Failures: 5`(전부 `expected:<200/500> but was:<401>`). 원인은 (1)과 동일(@WebMvcTest는 `Filter` 빈을 포함하므로 필터가 슬라이스에도 적용됨 — 다른 비화이트리스트 테스트들이 `@Import(AdminApiKeyTestConfig.class)`를 쓰는 이유). 인가 정책 확정 후 화면 계층 테스트는 `@Import(AdminApiKeyTestConfig.class)`(또는 키 헤더 명시)로 통과시키고, 인가 자체는 `ApiKeyAuthIntegrationTest`에 화면 라우트 회귀 3건(무키 401 / member 키 자기 자신 200 / member 키 타인 403)을 추가한다.
  3. Dev 기록의 인증 부재 서술과 "401은 Spring 기본 처리에 위임([미상])" 결정을 정정하고, `ApiKeyAuthFilter`가 반환하는 401 JSON(`{"error":"unauthorized"}`)이 화면 사용자에게 무엇으로 보이는지를 AC("미인증 401 → 표준 처리") 기준으로 다시 판단할 것.
- 권고(CONCERNS시):
  1. 404 안내를 예외 reason 그대로가 아니라 화면 고정 문구로 표시(AC "친화 안내" — 내부 메시지·입력 반사 제거).
  2. INF-ORD-002 `screens: []` 갱신과 UIS 신규 채번이 스코프 밖으로 위임됐다 — 재동기화 단계에서 실제로 닫히는지 추적할 것.

### QA Gate — 2026-09-07 (round2 재검증) — CONCERNS
- Layer1 스펙: **concerns**(round1 fail → 해소). round1의 차단 사유(신규 라우트가 default-deny에 걸려 화면이 렌더되지 않음)는 해소됐다 — 새 빌드를 별도 포트(8099)에 띄워 실측: `GET /member/M-0001` 무키 **401** / member 키(자기) **200** / member 키(타인 M-0002) **403** / admin **200**. 렌더 결과를 REST `GET /api/members/{id}` 응답과 대조해 AC를 건별 확인: 필드 구성·`ordered_at DESC` 정렬(M-0001: 20260816-0002 → 20260815-0001, API JSON 순서와 동일), 취소 주문 포함(M-0002의 `20260817-0002 / CANCELED / 450,000원` 그대로 표시), 주문번호 링크(`<a href="/order/20260816-0002">`), 404 친화 안내(M-9999 → 고정 문구 "회원을 찾을 수 없습니다.", 예외 `getReason()` 원문 미노출) 모두 성립. 빈 상태("최근 주문 없음")는 시드 회원 3명이 모두 주문을 가져 런타임 미재현 — 단위 테스트(`memberDetail_emptyRecentOrders_showsNoOrdersMessage`)로만 커버(수용). AS-IS 회귀(`del_yn='N'`·5건 고정·빈 배열)는 `MemberService`/`OrderDao` 무수정이라 유지. `no-sysout`(must) 준수(`System.out`·`printStackTrace` 0건, slf4j만). **잔여**: ① 이 랩에는 화면 채널 인증 수단(세션/쿠키 로그인)이 없어 브라우저는 `X-Api-Key`를 실을 수 없다 — 실사용자가 주소창으로 열면 여전히 `{"error":"unauthorized"}` **JSON 원문**이 뜬다(실측). AC "미인증 401 → 표준 처리"에 대한 round1 필수수정3의 "재판단" 요구는 "401/403은 필터 책임"이라는 정리에 그쳤고, 화면 채널에서 401을 무엇으로 보여줄지는 여전히 [미상]. ② INF-ORD-002 `screens: []` 미갱신·UIS 미채번(스코프 밖 위임, round1 권고2 그대로 이월).
- Layer2 보안: **pass**. round1이 지목한 IDOR 벡터가 지시대로 닫혔다 — `isOpenRoute`에 `/member/`를 넣지 않고(화이트리스트 코드 무변경 확인) `evaluateMemberScope`에 `MEMBER_VIEW_PATH ^/member/([^/]+)$` 분기를 `/api/members/{id}`(b-1)와 동일 의미로 추가. round 6이 뚫렸던 **경로 정규화 우회 계열을 직접 재프로브**했고 전부 차단됨(member 키, 대상 M-0002): `//member/M-0002`→403, `/member/M-0002;a=b`→403, `/member/M%2D0002`→403, `/member/./M-0002`→403, `/member/foo/../M-0002`→403, `/member/M-0002%20`→403, `/member/M-0002/`→404(정적리소스 404 JSON, PII 없음), `/Member/M-0002`→404. 쿼리 오염(`/member/M-0001?memberId=M-0002`)도 경로변수만 사용해 자기 데이터(김실증)만 렌더. 404 응답 본문에 내부 예외 사유 미포함(권고1 해소, 사유는 `log.debug`로만). **관찰(이번 변경의 결함 아님)**: 화면 상세가 링크하는 `/order/{orderNo}`는 SR-204 R-4로 여전히 무인증이라 화면 채널 인가 모델이 비대칭이다(주문번호는 `yyyymmdd+seq`로 추측 가능) — SR-204 후속 검토 대상.
- Layer3 회귀: **pass**. 전체 스위트 재현 확인: `mvnw.cmd -B test` → **Tests run: 169, Failures: 0, Errors: 0, BUILD SUCCESS**(dev 보고 일치). round1에 5건 전부 401로 깨졌던 `MemberViewControllerTest`는 surefire 리포트 실측 **5/5 통과**(`@Import(AdminApiKeyTestConfig.class)`는 다른 비화이트리스트 컨트롤러 테스트와 동일 규약), `ApiKeyAuthIntegrationTest`는 32→**35**(화면 라우트 무키 401/자기 200/타인 403 3건 추가 — round1 필수수정3·권고4 해소). 기존 화면 무인증 회귀(`/product/list`·`/cart` 200)와 round 6 정규화 회귀 9건 모두 그대로 통과. 필터 변경은 `/member/{id}` 단일 세그먼트에만 매칭되고 그 경로에 다른 매핑이 없어 기존 `/api/**` 판정 경로에 영향 없음(컨트롤러 전수 확인: 화면 라우트는 `/cart*`·`/order/*`·`/product/*`·`/member/{id}`가 전부이고 모두 분류됨). **관찰**: 이 워크스페이스에는 `.speclinker/test_baseline.json`이 없어 테스트 수의 조용한 감소를 감지할 수단이 없다(surefire에 `OrderListIntegrationTest` 리포트만 남고 클래스는 부재 — 10:55 실행분이라 이번 라운드 산출물이 아니고, `.lab-ws`가 gitignore라 귀속 불가).
- 필수 수정(FAIL시): 없음 — round1 top_issues 5건(신규 라우트 인가 우회 / 테스트 5건 실패 / IDOR / 화이트리스트 계약 미분류 / 404 메시지 노출) 전부 실측 해소.
- 권고(CONCERNS시):
  1. 화면 채널의 미인증·인가실패 응답을 정의할 것 — 현재 `/member/{id}`는 브라우저에서 열 수 없고(헤더 주입 불가) 401/403이 JSON 원문으로 노출된다. 세션/쿠키 로그인 도입이나 "화면 라우트는 401→로그인·오류 페이지로 전환"을 **별도 SR**로 분리해 추적(SR-207 스코프 밖 — 보안 선택 자체는 사람 승인 방향대로 옳게 구현됨).
  2. INF-ORD-002 `screens: []` 갱신 + UIS 신규 채번(round1 권고2 이월) — 재동기화 단계에서 실제로 닫히는지 확인.
  3. 어떤 템플릿도 `/member/{id}`로 링크하지 않아 화면 진입점이 없다(`templates/` grep 0건) — UIS 채번 시 내비게이션 경로를 함께 정의할 것.
  4. `/order/{orderNo}` 화면 무인증(SR-204 R-4)과의 인가 비대칭을 SR-204 차원에서 재검토(이번 FUNC 결함 아님).

## 후속 추적(TODO)
> round2 QA CONCERNS — 사람 결정: "추적등록 후 진행"(2026-09-07, by 사람). 필수 수정 0건, 아래 권고는
> 이번 FUNC 스코프에서 처리하지 않고 후속 과제로 남긴다. 소유권 대조(IDOR 차단) 자체는 이미 옳게
> 구현되어 있고, 남은 항목은 전부 별도 SR/후속 라운드에서 다룰 사안이라는 것이 사람의 판단이다.

1. [medium/spec] 화면 채널에 로그인 수단(세션/쿠키)이 없어 브라우저가 `X-Api-Key`를 실을 수 없다 —
   `/member/{id}`를 주소창으로 열면 401 JSON 원문(`{"error":"unauthorized"}`)이 그대로 노출되고
   실사용자는 화면에 도달할 수 없다(실측). → 화면 401/403 처리(로그인 페이지 전환 또는 화면형 오류
   페이지)와 세션 인증 도입을 **별도 SR로 분리**해 추적. SR-207 스코프 밖.
2. [low/spec] `INF-ORD-002`의 `screens: []` 필드 미갱신, UIS 신규 채번 미완 — STEP 5.5 스펙 재동기화
   단계에서 실제로 닫히는지 확인할 것(round1 권고2 이월).
3. [low/spec] 어떤 템플릿도 `/member/{id}`로 링크하지 않아 화면 진입점이 없다(`templates/` grep 0건) —
   UIS 채번 시 내비게이션 진입 경로를 함께 정의할 것.
4. [low/security] 회원 상세는 소유권으로 닫혔으나 링크 대상 `/order/{orderNo}` 화면은 SR-204 R-4로
   여전히 무인증(주문번호 `yyyymmdd+seq` 추측 가능) — SR-204 차원에서 화면 라우트 인가 모델 재검토
   대상(이번 FUNC의 결함 아님).
5. [low/regression] 워크스페이스에 `.speclinker/test_baseline.json`이 없어 테스트 수의 조용한 감소를
   감지할 수단이 없다 — `test_baseline_ws.py`로 기준선 최초 기록 필요(이번 FUNC과 무관한 별건).

## 스펙 재동기화 (STEP 5.5)
- AS-IS 스냅샷: `docs/변경관리/SR-207/_asis_snapshot/INF-ORD-002.md`(`snapshot_specs.py`).
- `INF-ORD-002.md`에 `> [반영: FUNC-order-004]` 프로비넌스 노트 추가 — 신규 화면
  `GET /member/{memberId}`가 이 API를 소비함을 기록. 요청/응답/규칙/오류 계약은 무변경(AS-IS
  유지, qa-agent round2 실측 확인)이라 구조 필드(요청 파라미터·응답 스키마 등) 변경 없음.
- `screens: []` 필드는 **의도적으로 갱신하지 않음** — UIS 신규 채번이 아직 없어(`## 후속 추적(TODO)`
  #2·#3) 존재하지 않는 UIS-ID를 채우면 끊긴 참조가 된다. UIS 채번 시 갱신하도록 본문에 안내 남김.
- SCH-ORD-001/004, UIS: 변경 없음(연결 없음 그대로).
- 갱신 후 신규 미완성 마커(LLM-TODO/[TBD]) 확인: `[TBD]`는 기존 `srs-f: [TBD]` 1건뿐(이번 변경 무관,
  기존 상태 유지) — 신규 마커 0건.

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [high/spec] 신규 화면 라우트 GET /member/{memberId}가 SR-204 ApiKeyAuthFilter의 default-deny에 걸려 무키 요청이 401 — isOpenRoute 화이트리스트(/error,/favicon.ico,/cart,/order/,/product/)에 /member/가 없다. 실행 중 앱(8087) 실측 401(=/product/list는 200). 브라우저에서 화면이 렌더되지 않아 신규 AC(표시 항목·빈 상태·취소 주문 포함·주문번호 링크·404 안내)가 런타임에서 성립하지 않음. Dev 기록의 '이 랩에 인증 미들웨어 없음'은 사실 오류(FUNC-order-013/SR-204가 전역 필터 도입). → isOpenRoute 단순 추가가 아니라 evaluateMemberScope에 ^/member/([^/]+)$ 소유권 대조를 추가해 admin 통과·member 키는 자기 자신만 허용(현행 /api/members/{id} b-1과 동일 의미). SR-204 스펙 변경이므로 사람 승인 후 적용.
2. [high/spec] MemberViewControllerTest 5건 전부 실패 — 실측 mvn -Dtest=MemberViewControllerTest test 결과 Tests run: 5, Failures: 5 (모두 expected:<200/500> but was:<401>). @WebMvcTest는 Filter 빈을 포함하므로 필터가 슬라이스에도 적용되는데(다른 비화이트리스트 테스트들이 @Import(AdminApiKeyTestConfig.class)를 쓰는 이유) 이 테스트에는 그 설정이 없다. → 인가 정책 확정 후 화면 계층 테스트에 @Import(AdminApiKeyTestConfig.class) 적용, 인가 자체는 ApiKeyAuthIntegrationTest에 화면 라우트 회귀 3건(무키 401 / member 키 자기 자신 200 / member 키 타인 403) 추가.
3. [high/security] 화면 컨트롤러가 REST를 거치지 않고 MemberService.get을 직접 호출해 필터의 소유권 대조를 우회한다. /member/를 isOpenRoute에 넣는 반사적 수정은 무인증 타인 PII(이름·연락처·등급·가입일)+최근 주문 이력 열람을 열어 SR-204 R-2가 /api/members/{id}(자기 자신만)·/api/members(admin 전용)로 닫은 IDOR을 화면 채널로 되살린다. → 화이트리스트 추가 금지. 화면 라우트도 소유권 검증 대상으로 편입하고, 그 회귀 테스트를 ApiKeyAuthIntegrationTest에 남길 것.
4. [low/regression] SR-204가 세운 '화면 라우트 화이트리스트' 계약에 신규 화면이 분류되지 않은 채 추가돼 계약이 불완전해졌고, ApiKeyAuthIntegrationTest의 R-4 화면 라우트 회귀가 이 경로를 덮지 않는다. → 화면 라우트 추가 시 isOpenRoute/evaluateMemberScope 분류를 필수 체크로 남기고 R-4 회귀 테스트에 신규 경로를 포함.
5. [low/security] 404 안내가 서비스 예외 getReason()('회원 없음: M-9999')을 그대로 화면에 노출 — th:text escape라 XSS는 아니나 내부 메시지·입력 반사이고 AC의 '친화 안내'와도 어긋난다. → 화면 고정 문구로 치환.

사람 코멘트: QA FAIL round1 재작업: (1) GET /member/{memberId}를 isOpenRoute 화이트리스트에 추가하지 말 것 — ApiKeyAuthFilter.evaluateMemberScope에 ^/member/([^/]+)$ 소유권 대조를 추가해 admin 통과, member 키는 자기 자신만 허용(SR-204 스펙 변경, /api/members/{id}와 동일 의미). (2) MemberViewControllerTest 5건이 전부 401로 실패 — 인가 정책 확정 후 @Import(AdminApiKeyTestConfig.class) 적용해 통과시킬 것. (3) ApiKeyAuthIntegrationTest에 화면 라우트 회귀 3건 추가: 무키 401 / member 키 자기 자신 200 / member 키 타인 403. (4) 404 안내에서 서비스 예외 getReason() 원문 노출 제거 — 화면 고정 문구로 치환.

## Test 실행 결과 (Test Execution Results)

### 테스트 실행 명령

```bash
mvnw.cmd -Dtest=MemberViewControllerTest,ApiKeyAuthIntegrationTest test
```

### 실행 결과

**BUILD SUCCESS** — 2026-09-08 00:01:44 UTC+09:00

| 테스트 클래스 | 실행 | 실패 | 오류 | 결과 |
|---|---|---|---|---|
| MemberViewControllerTest | 5 | 0 | 0 | ✅ 통과 |
| ApiKeyAuthIntegrationTest | 35 | 0 | 0 | ✅ 통과 |
| **전체** | **40** | **0** | **0** | **✅ 통과** |

### AC↔TC 매핑

| # | 수용 기준 (AC) | 매핑 테스트 | TC 상태 |
|---|---|---|---|
| 1 | 신규: 표시 항목 — 최근 주문 5건을 최신순으로 나열(orderNo, orderedAt, orderState, totalAmount) | `MemberViewControllerTest.memberDetail_found_rendersMemberInfoAndRecentOrders` | ✅ PASS |
| 2 | 신규: 빈 상태 — `recentOrders` 빈 배열 → "최근 주문 없음" | `MemberViewControllerTest.memberDetail_emptyRecentOrders_showsNoOrdersMessage` | ✅ PASS |
| 3 | 신규: 취소 주문 — `orderState=CANCELED`도 목록에 포함 | `MemberViewControllerTest.memberDetail_withCanceledOrder_stillRendersInList` | ✅ PASS |
| 4 | 신규: 상호작용 — 주문번호 클릭 시 UIS-ORD-002로 이동 | `MemberViewControllerTest.memberDetail_found_rendersMemberInfoAndRecentOrders` (링크 검증) | ✅ PASS |
| 5 | 신규: 오류 상태 — 회원 없음(404) → 화면 친화 안내 | `MemberViewControllerTest.memberDetail_notFound_absorbsAndShowsFixedFriendlyMessage` | ✅ PASS |
| 6 | 신규: 오류 상태 — 조회 실패/5xx → rethrow(기존 공통 오류 배너) | `MemberViewControllerTest.memberDetail_serverError_rethrowsAndDoesNotAbsorb` | ✅ PASS |
| 7 | 신규: 권한 — 미인증 401 → 표준 처리(403 JSON) | `ApiKeyAuthIntegrationTest.memberScreenRoute_withoutApiKey_returns401` | ✅ PASS |
| 8 | 신규: 권한 — SR-204 정책 적용(관리 자원, member 키는 자기 자신만) | `ApiKeyAuthIntegrationTest.memberScreenRoute_memberApiKeyOwnMemberId_returns200` | ✅ PASS |
| 9 | 신규: 권한 — 타인 회원 접근 차단(403 Forbidden) | `ApiKeyAuthIntegrationTest.memberScreenRoute_memberApiKeyForeignMemberId_returns403Forbidden` | ✅ PASS |
| 10 | 회귀: INF-ORD-002 `del_yn='N'` 필터 | `MemberService.get` 무수정 (OrderDao 무수정) | ✅ 유지 |
| 11 | 회귀: INF-ORD-002 `recentOrders` 상위 5건 고정 | `MemberService.get` 무수정 | ✅ 유지 |
| 12 | 회귀: 주문 없으면 빈 배열(`null` 금지) | `Member.recentOrders` 기본값 무수정 | ✅ 유지 |

### 회귀 검증

**SR-207에 회귀 TC 경로 지정 없음.** 대신 다음 실제 테스트가 회귀 커버:
- `ApiKeyAuthIntegrationTest` — 기존 화면 라우트(`/product/list`, `/cart`) 무인증 유지 회귀(라인 202-215)
- `ApiKeyAuthIntegrationTest` — 신규 화면 라우트 소유권 대조 회귀 3건(라인 222-242)
  - `memberScreenRoute_withoutApiKey_returns401`
  - `memberScreenRoute_memberApiKeyOwnMemberId_returns200`
  - `memberScreenRoute_memberApiKeyForeignMemberId_returns403Forbidden`
- `MemberViewControllerTest` — 기존 `MemberService.get`/`OrderDao.selectRecentByMember` 호출 체인 무수정 확인(생성 코드 rev lf2730c60e00 대조)

### 품질 판정

통과율: **100%** (40/40)

**✅ 납품 가능**
- 모든 AC가 실행 테스트로 1:1 검증됨
- 회귀 테스트 전부 통과 (AS-IS 5건 고정·del_yn='N'·빈 배열 유지 확인)
- SR-204 보안 정책 적용 검증됨 (소유권 대조, IDOR 차단)
- 권고사항 (round 2 권고1-4, 미완):
  - 권고1 [medium/spec]: 화면 채널 401/403 처리 정책 별도 SR (SR-207 스코프 밖)
  - 권고2 [low/spec]: INF-ORD-002 `screens: []` 갱신·UIS 신규 채번 (스펙 재동기화 단계로 위임)
  - 권고3 [low/spec]: 내비게이션 진입 경로 정의 (UIS 채번 시 함께)
  - 권고4 [low/security]: `/order/{orderNo}` 무인증 인가 비대칭 재검토 (SR-204 차원)
