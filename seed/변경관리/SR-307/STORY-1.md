---
story-id: STORY-SR-307.1
item: SR-307.1
title: 상품 조회 GET 공개 경로 허용
status: Done
domain: order
created: 2026-09-17
spec_markers: 0
sr-id: SR-307
approved_sha: b87dcc1655b9
---

# STORY-SR-307.1 — 상품 조회를 공개 경로로 — 앱 서빙 쇼핑 화면에서 401 해소(GET만) — 상품 조회 GET 공개 경로 허용

## Story
상품 조회를 공개 경로로 — 앱 서빙 쇼핑 화면에서 401 해소(GET만) — 상품 조회 GET 공개 경로 허용


## 변경 컨텍스트 (SR-307)
> 이 story는 변경요청 **SR-307 — 상품 조회를 공개 경로로 — 앱 서빙 쇼핑 화면에서 401 해소(GET만)** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-307/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-307/02_변경명세.md`

### 확정된 요건 문답 9건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: ApiKeyAuthFilter의 공개 경로에 상품 조회 GET 2건(목록·단건)만 추가한다. 제외: 다른 경로의 인증 정책, 쓰기(POST/PATCH/DELETE) 공개, 레이트리밋·캐시·CORS 정책, 응답 필드 변경.
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 특정 화면/API만: 회원·주문·장바구니·CSV·관리자 경로는 지금처럼 키 없으면 401 · 상품 조회도 GET 외 메서드는 401 · 키를 보낸 요청의 동작 불변 · 기존 Thymeleaf 화면과 개발 서버 프록시 경로 불변.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필요 — 요청·응답 형식 변경 없음. 인증 요구만 GET 상품 조회에서 없어진다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 새 오류 코드 없음. 공개된 GET은 200 또는 기존 404(없는 상품) · 공개 아닌 메서드·경로는 기존 401 그대로.
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음 — 테이블·쿼리 변경 없음(인증 필터만 바뀐다).
- **기존 데이터 이관·백필이 필요한가?** — 불필요 — 스키마 변경 없음.
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 나열된 화면이 전부: 화면 변경 없음. 쇼핑 홈(UIS-ORD-008)이 앱 서빙 환경에서 상품을 받아오게 되는 효과만 있다.
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 해당 없음 — 화면 파일을 바꾸지 않는다. 쇼핑 홈의 빈 상태·오류 표기는 SR-302에서 이미 정의됨.
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 해당없음(스토리북 미사용) — 이 SR은 React 부품을 만들지 않는다.

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-307/02_변경명세.md`에서 도출)
- [x] INF-ORD-008: [미상] — 위 SR-307 절의 요지 중 이 스펙에 해당하는 변경을 사람이 적는다
- [x] INF-ORD-009: [미상] — 위 SR-307 절의 요지 중 이 스펙에 해당하는 변경을 사람이 적는다

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**
- [x] INF-ORD-008 (GET /api/products): `sale_yn = 'Y'` 상시필터 → 판매중 상품만 반환(단건 조회인 [[INF-ORD-009]]는 이 필터가 없음)
- [x] INF-ORD-008 (GET /api/products): [반영: FUNC-order-007] `keyword` 파라미터가 있으면 `product_name` 부분 일치(LIKE) 검색 —
- [x] INF-ORD-008 (GET /api/products): [반영: FUNC-order-007](SR-220) `inStock=true`면 `stock_qty >= 1` 조건을 `sale_yn = 'Y'` 상시필터와
- [x] INF-ORD-009 (GET /api/products/{sku}): 목록 조회([[INF-ORD-008]])와 달리 `sale_yn` 필터가 없다 — 판매종료 상품도 SKU를 알면 조회 가능

> 변경명세에 스펙 ID가 없는 절 — 이 항목 몫인지 확인해 AC로 옮긴다: SR-307 (요구사항 요지 — 전 스펙 공통)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-008: > [변경: SR-201] 2026-08-22 / INF-ORD-008: GET /api/products — 판매중 상품 목록 조회 / > **개요:** 판매중 상태인 상품 전체를 SKU 순으로 조회한다. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/ProductController.java:24-31` — [docs/05_설계서/order/INF/INF-ORD-008.md](../../05_설계서/order/INF/INF-ORD-008.md)
- **INF** INF-ORD-009: INF-ORD-009: GET /api/products/{sku} — 상품 단건 조회 / > **개요:** SKU로 상품 1건을 조회한다. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/ProductController.java:27-31` / 요청 — [docs/05_설계서/order/INF/INF-ORD-009.md](../../05_설계서/order/INF/INF-ORD-009.md)
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)


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

## 구현 계획
- **파일**:
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — 공개 경로 화이트리스트에 상품 조회 GET 2건만 추가(컨트롤러·서비스·DAO·DDL은 변경 없음 — 이 SR은 인증 필터 1개 파일만 고친다).
    - 상수 추가: `PRODUCTS_LIST_PATH = "/api/products"`(정확 일치, `ProductControllerTest` 주석 실측대로 트레일링 슬래시 없음), `PRODUCT_ITEM_PATH = Pattern.compile("^/api/products/([^/]+)$")`(단건, `MEMBERS_ITEM_PATH`와 동일한 관례의 세그먼트 1개 정규식). 두 패턴 모두 `MEMBERS_ITEM_PATH`·`MEMBER_VIEW_PATH`·`ORDER_RESOURCE_PATH`·`MEMBER_ME_ADDRESSES_PATH`와 세그먼트·접두사가 달라 매치 충돌 없음(각 기존 정규식은 `/api/members/`·`/member/`·`/api/orders/` 하위만 본다 — SR-217이 겪은 "grades를 memberId로 오인"류 충돌이 여기서는 애초에 성립하지 않는다는 것을 정규식 대조로 미리 확인).
    - `isOpenRoute(String path)` → `isOpenRoute(String path, String method)`로 시그니처 변경(호출부 `shouldNotFilter` 한 곳뿐이라 파급 없음). 기존 화이트리스트 항목은 전부 메서드 무관이므로 그대로 두고, 마지막에 `|| (isPublicProductReadPath(path) && "GET".equalsIgnoreCase(method))` 절만 추가. `isPublicProductReadPath`는 `PRODUCTS_LIST_PATH.equals(path) || PRODUCT_ITEM_PATH.matcher(path).matches()`.
    - 다른 화이트리스트 항목(화면 라우트 제외)이 전부 "정확 일치"이고 메서드 판별이 없는 이유는 그 경로들이 애초에 단일 메서드로만 매핑돼 있어서다(로그인=POST만, refresh=POST만…). 상품 조회는 같은 경로(`/api/products`, `/api/products/{sku}`)가 REST 컨벤션상 나중에 POST/PUT/DELETE도 받을 수 있는 리소스 경로라 **경로만으로 열면 안 된다** — 확정 문답 "공개 경로라도 쓰기는 반드시 인증"을 어기게 된다. 그래서 이 항목만 메서드 조건을 명시한다(`ProductController`는 현재 `@GetMapping` 2개뿐이라 오늘 당장 노출되는 쓰기 엔드포인트는 없지만, 방어적으로 지금 막아 둔다).
    - 클래스 상단 관례대로 이 변경 이유를 JavaDoc/주석 한 단락으로 남긴다(SR-307, 항목 SR-307.1 — 추적용 `linked_func`/`linked_tc` 아님, 다른 SR 블록과 동일한 설명 주석).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/web/ApiKeyAuthIntegrationTest.java` — 같은 클래스에 SR-307 블록 추가(신규 파일 아님, `AdminApiKeyTestConfig`를 안 쓰는 이 클래스가 "무키" 상태를 직접 만들 수 있는 유일한 스위트 — `ProductControllerTest`는 슬라이스+admin 키 고정이라 이 필터 변경을 검증할 수 없음).
- **데이터**: 없음 — 테이블·DDL·트랜잭션 변경 없음(순수 인증 필터 화이트리스트 수정).
- **순서·보안**:
  - `shouldNotFilter`가 `isOpenRoute`로 여전히 먼저 판정하고, 일치하면 `doFilterInternal` 자체가 스킵된다(기존 관례 그대로) — `evaluateMemberScope`에는 도달하지 않는다. 상품 조회는 memberId를 참조하지 않는 리소스라 스킵돼도 소유권 판정 로직에 영향 없음(회귀: `memberApiKey_endpointWithoutMemberIdReference_passesThrough`가 이미 "필터를 안 거쳐도 200"을 실측 중인 경로와 동일 성격).
  - GET 외 메서드(POST/PATCH/DELETE `/api/products`·`/api/products/{sku}`)는 `isPublicProductReadPath && GET` 조건이 거짓이라 화이트리스트 밖 → default-deny로 무키 401 유지(오늘은 그런 매핑이 컨트롤러에 없어 필터가 먼저 401을 내고 컨트롤러의 405 판정까지 가지 않지만, 그것이 곧 "쓰기는 인증 필요"를 지키는 것 — 나중에 쓰기 매핑이 추가돼도 필터가 스스로 막는다).
  - 부수효과(로그·발송·이벤트·감사) 없음 — 이 SR은 판정 조건 하나만 넓히고 그 뒤의 어떤 실행 경로도 추가하지 않는다.
  - 폴백·우회 경로의 자격 판정: 새로 여는 "우회 경로"가 아니라 **판매중 여부와 무관하게 공개하는 읽기 경로**다(단건 조회는 원래도 `sale_yn` 필터가 없어 판매종료 상품까지 SKU로 조회 가능 — 이 SR로 새로 생기는 노출이 아니라 기존 INF-ORD-009 비즈니스 규칙 그대로). 탈퇴·폐기·만료 같은 회원 자격 개념 자체가 상품 리소스에는 없으므로 이 절에서 판정할 대상이 없음.
  - 프레임워크 실행 모델 함정: 없음(서블릿 필터 `shouldNotFilter`는 요청당 1회, StrictMode 이중 실행이나 프록시 self-invocation 같은 재실행 함정과 무관).
- **계약**: 새 오류 코드 없음. 공개된 GET은 기존 그대로 200(목록·있는 sku)/404(없는 sku, `ResponseStatusException(NOT_FOUND, "상품 없음: "+sku)` 불변). 공개 아닌 메서드·경로는 기존 401(`{"error":"unauthorized"}`) 그대로. 응답 스키마·필드 변경 없음.
- **테스트** (`ApiKeyAuthIntegrationTest`에 추가, 전부 HTTP 레벨 단언 — 랩 시드 실측 `SKU-1001`(CartDaoTest 주석: 스탠딩데스크, 재고12) 재사용, DB 변경 없는 순수 GET이라 정리 불필요):
  - `productsListRoute_withoutApiKey_returns200` — `GET /api/products`(무키) → 200 (TO-BE 핵심)
  - `productsItemRoute_withoutApiKey_existingSku_returns200` — `GET /api/products/SKU-1001`(무키) → 200 + `sku` 필드 확인 (TO-BE 핵심)
  - `productsItemRoute_withoutApiKey_unknownSku_returns404` — `GET /api/products/SKU-NOPE-9999`(무키) → 404 (회귀: 오류 계약 불변 — 공개돼도 404는 그대로)
  - `productsListRoute_withMemberApiKey_returns200` — 기존 `memberApiKey_endpointWithoutMemberIdReference_passesThrough`가 이미 커버(키 보낸 요청 동작 불변) — 신규 추가 없이 그대로 유지, 삭제·수정 금지.
  - `productsWritePlaceholder_withoutApiKey_returns401` — `rawRequest`로 `POST /api/products`(무키, 본문 없음) → 401 (회귀: 쓰기는 공개 아님. `ProductController`에 매핑이 없어 필터를 통과해도 405가 나겠지만, 이 테스트는 "필터 단계에서 이미 401로 막힌다"를 확인 — 405로 새면 필터가 열렸다는 뜻이므로 그 자체가 실패 신호).
  - 기존 회귀 스위트(특히 `noApiKey_toApiEndpoint_returns401Unauthorized`·`memberScreenRoute_withoutApiKey_returns401`·`memberApiKey_membersList_returns403Forbidden` 등 보호 경로 전부)는 수정 없이 그대로 통과해야 한다 — `mvnw test`로 `ApiKeyAuthIntegrationTest` 전체 재실행해 확인.
- **테스트 격리**: 전부 조회(GET) 또는 무매핑 POST(컨트롤러 도달 전 필터가 차단)만 수행 — 신규 행 생성·카운터 증가 없음. `@AfterEach cleanUpSignupVerificationRows`가 이미 있는 다른 테스트용 정리이며 이 신규 테스트들은 그 대상에 해당하지 않아 그대로 둬도 간섭 없음. 별도 UUID 접미·정리 코드 불필요(상태를 만들지 않으므로 다음 테스트로 샐 상태 자체가 없음).
- **폴백·우회 경로의 자격 판정**: 위 "순서·보안" 절 참고 — 해당 없음(회원 자격 개념이 없는 리소스).
- **프레임워크 실행 모델 함정**: 없음.
- **범위 밖**: 다른 경로의 인증 정책, 쓰기(POST/PATCH/DELETE) 공개, 레이트리밋·캐시·CORS 정책, 응답 필드 변경(확정 문답 그대로) — 전부 이번 SR 제외. `ApiKeyAuthFilter.java`가 이번 수정 전 이미 ~490줄로 `file-size-cap`(should, 자바 450줄) 초과 상태이며 이번 추가로 소폭 더 늘어난다 — should 위반이라 게이트 차단 대상은 아니지만 후속 SR에서 화이트리스트 판정 로직 분리(예: `OpenRoutePolicy` 클래스 추출)를 후속 과제로 남긴다.
- **실패 사례집 대조** (`harness/antipatterns.all.md`):
  - SR-217/SR-231 계열의 "화이트리스트 정규식이 다른 정규식을 오인해 충돌"(예: grades를 memberId로 오인) 패턴이 여기서도 성립하는지 위 "파일" 절에서 정규식 세그먼트 수로 직접 대조 확인함 — 성립하지 않음(상품 경로는 `/api/members/`·`/member/`·`/api/orders/` 계열과 접두사가 겹치지 않음).
  - SR-232 r2 "API 키 DB 폴백이 탈퇴 회원을 무기한 통과"류 자격 판정 누락 패턴 — 이 SR은 자격(회원) 개념이 없는 리소스를 여는 것이라 조건 자체가 성립하지 않음(위 "폴백·우회 경로" 절).
  - SR-231 r1 "예약 FUNC을 ID만 보고 역할이 반대로 붙었다" — 이 항목은 FUNC 예약이 없는 순수 인증정책 변경이라 해당 없음. 대신 이 SR 고유의 위험은 "경로만 보고 메서드를 안 봐서 쓰기까지 열리는 것"이었고, 그 조건은 성립할 뻔했다(다른 화이트리스트 항목을 그대로 흉내 내 경로만 추가했다면 재현됐을 것) — 그래서 이 SR만 메서드 조건을 명시적으로 추가한다(위 "파일" 절).

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
계획 확인: 계획대로 진행 (2026-09-17)

구현 완료 (2026-09-17) — 계획 그대로 진행, 이탈 없음.

- 수정 파일:
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — `isOpenRoute(String)` → `isOpenRoute(String, String)`로 메서드 인자 추가(호출부 `shouldNotFilter` 한 곳). 상수 `PRODUCTS_LIST_PATH = "/api/products"`(정확 일치), `PRODUCT_ITEM_PATH = Pattern.compile("^/api/products/([^/]+)$")` 추가. 화이트리스트 마지막 절에 `isPublicProductReadPath(path) && "GET".equalsIgnoreCase(method)`만 추가(GET 외 메서드·다른 경로는 기존 default-deny 그대로). 클래스 Javadoc에 SR-307 단락 추가(`linked_func` 아닌 설명 주석, 기존 관례와 동일).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/web/ApiKeyAuthIntegrationTest.java` — SR-307 블록 4건 추가: `productsListRoute_withoutApiKey_returns200`, `productsItemRoute_withoutApiKey_existingSku_returns200`(SKU-1001), `productsItemRoute_withoutApiKey_unknownSku_returns404`, `productsWritePlaceholder_withoutApiKey_returns401`. 기존 `memberApiKey_endpointWithoutMemberIdReference_passesThrough`(GET /api/products, 무키 아님)는 수정·삭제하지 않고 그대로 유지.

- 주요 결정:
  - 계획은 쓰기 회귀 테스트에 `rawRequest`를 지목했으나, 그 헬퍼는 GET/PATCH만 지원하고 POST는 만들 수 없다(실측 — `"PATCH".equals(method) ? PATCH : GET()`). 대신 이미 있는 `rawPostJson(path, apiKey, body)`(TestRestTemplate이 POST+401 조합에서 던지는 JDK 스트리밍 인증 재시도 예외를 피하려 만들어진 헬퍼)로 `POST /api/products`(무키, `{}`)를 보내 동일한 단언(401 + `{"error":"unauthorized"}`)을 확인했다. 계획의 의도(필터 단계에서 이미 401로 막힌다)는 그대로 충족.

- 테스트 실행: `mvnw test -Dtest=ApiKeyAuthIntegrationTest,ProductControllerTest` — 신규 4건 포함 전체 통과. 유일한 실패 `memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`는 이 SR과 무관한 사전 존재 결함(CLAUDE.md 기재 SR-300 시간창 드리프트 — `git stash`로 이번 변경을 제거한 master 상태에서도 동일하게 실패함을 확인) — 이번 변경이 유발한 회귀 아님.

재작업 완료 (round 2, 2026-09-17) — QA round1 FAIL 필수 2건 + medium 권고 2건 반영. 사람 게이트 지시(구조 변경 3·4항 포함) 그대로 따름.

- 수정 파일:
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — 구조 변경: 공개 상품 GET을 `isOpenRoute`(=`shouldNotFilter`) 화이트리스트에서 **제거**하고, `doFilterInternal`에서 "무키(키 헤더 자체 없음)일 때만" `isPublicProductReadPath(path) && GET`을 인증 요구의 예외로 처리하도록 재구성 — 필터는 이 경로에서도 항상 실행되어 `evaluateMemberScope`까지 도달하므로 키를 보낸 요청(무효 키 포함)의 기존 판정(예: `memberId` 파라미터 소유권 403)이 그대로 유지된다. `PRODUCT_ITEM_PATH` 정규식을 `^/api/products/([^/]+)$`(세그먼트 1개면 전부 매치)에서 `^/api/products/(SKU-[A-Za-z0-9-]+)$`(시드·테스트 전역 실측 SKU 형식)로 좁혀 `GET /api/products/export` 같은 가상 서브리소스가 자동 공개되지 않게 함(매치 실패 시 default-deny 401로 fail-closed). 두 구조 변경의 이유를 클래스 Javadoc(round 2 재작업 단락 2개)과 각 상수/분기 옆 인라인 주석에 남김.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/web/ApiKeyAuthIntegrationTest.java` — 추가: `productsListRoute_withMemberApiKeyAndOtherMemberIdParam_returns403Forbidden`(필터가 스킵되지 않고 키를 보낸 요청의 소유권 판정이 그대로임을 고정), `productsItemPlaceholder_patchWithoutApiKey_returns401`(`rawRequest("PATCH", ...)`), `productsListRoute_headWithoutApiKey_returns401`, `productsItemRoute_virtualSubresourceExport_withoutApiKey_returns401`(`GET /api/products/export`류 가상 서브리소스 미공개 확인).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/web/ShopStaticResourceServingTest.java:69-77` — 화이트리스트 누출 감시 테스트의 대상 경로를 `GET /api/products`(SR-307로 의도적 공개)에서 `GET /api/orders`(여전히 보호)로 교체. 감시 의도(=/shop 화이트리스트가 /api/\*\*로 새지 않았다)는 보존, 테스트 삭제 없음.

- 이번 세션에서 추가로 발견·수정한 문제(재작업 지시에는 없었음): 위 4건 중 `productsListRoute_headWithoutApiKey_returns401`이 `mvn -o test` 전체 실행에서 **ERROR**(단언 실패가 아니라 예외)로 죽었다 — `TestRestTemplate` 기본 `SimpleClientHttpRequestFactory`(JDK `HttpURLConnection` 기반)는 HEAD 응답에 본문이 없어 4xx/5xx일 때 `getErrorStream()`을 얻지 못하고 `IOException("Server returned HTTP response code: 401")`을 그대로 던진다(JDK 한계 — PATCH가 겪던 것과 동일 계열 문제). 기존 `head()` TestRestTemplate 헬퍼를 삭제하고, `rawRequest`(JDK `HttpClient` 기반)에 HEAD 분기를 추가해 그 헬퍼로 교체 — 필터가 실제로 401을 반환하는 것은 원래도 맞았고(예외 메시지 자체가 "401"을 담고 있었음), 테스트 클라이언트 선택의 문제였다.

- 테스트 실행(round 2): `mvnw -o test` **전체 560건** — 실패 3건, 에러 0건. 실패 3건은 전부 사람 게이트가 명시한 허용 기준선과 정확히 일치: `OrderListEndToEndIntegrationTest` 2건(`orderList_realDbEndToEnd_rendersLatestDeliveryStateColumnForEachOrder`, `orderList_realDbEndToEnd_showsDashWhenOrderHasNoDeliveryHistory`) + `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`(전부 SR-300 시간창 이월, 이 SR 무관). 그 외 실패·에러 없음 — 신규 4건 포함 SR-307 관련 테스트 전부 통과.

- 독립 재검증(체크포인트, 2026-09-17): 현재 작업 트리 상태에서 `mvnw.cmd -o test`를 처음부터 다시 동기 실행해 위 수치를 재현 — **Tests run: 560, Failures: 3, Errors: 0, Skipped: 0**, 실패 테스트명도 동일(`OrderListEndToEndIntegrationTest.orderList_realDbEndToEnd_rendersLatestDeliveryStateColumnForEachOrder` · `…_showsDashWhenOrderHasNoDeliveryHistory` · `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`). SR-307 신규 테스트 8건(round1 4 + round2 4) 전부 통과, 그 외 회귀 없음. 첫 시도에서 백그라운드 프로세스가 `OrderListEndToEndIntegrationTest` 진입 직후 무응답으로 멈춰(호스트 프로세스 트리에서 확인 불가 — 환경상의 유실로 추정, 이 SR의 코드 변경과는 무관: 해당 테스트는 `/order/list`만 다루고 `ApiKeyAuthFilter`의 상품 조회 분기와 무관) 재실행으로 확인했다.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-17 — FAIL
- **Layer1 스펙**: pass — `isOpenRoute(path, method)`에 `isPublicProductReadPath(path) && GET` 분기만 추가했고, 컨트롤러·서비스·DAO·DDL·응답 스키마는 불변(`Product`는 sku/productName/price/stockQty/saleYn뿐 — 회원 식별자·원가 같은 내부 정보 신규 노출 없음). 확정 문답의 포함/제외 범위와 일치하고, 회귀 AC 4건(`sale_yn='Y'` 상시필터·keyword LIKE·inStock·단건은 `sale_yn` 필터 없음)은 쿼리를 건드리지 않아 유지된다(`ProductServiceTest` 9건·`ProductDaoTest` 11건·`ProductControllerTest` 전부 통과 실측).
- **Layer2 보안**: concerns — 메서드 조건을 명시해 쓰기(POST/PATCH/DELETE)는 default-deny로 남는 것을 `productsWritePlaceholder_withoutApiKey_returns401`로 실측 확인(필터 단계 401, 405로 새지 않음). 다만 아래 권고 1·2의 판정 범위 문제가 남는다.
- **Layer3 회귀**: **fail** — 기존 회귀 테스트 `ShopStaticResourceServingTest.apiWithoutApiKey_stillRejectedUnauthorized_shopWhitelistDidNotLeak`(SR-301이 심은 화이트리스트 누출 감시)가 바로 이 동작(`GET /api/products` 무키 → 401)을 고정하고 있어 **깨진다**. QA 실측: `mvn -o test` 전체 556건 중 실패 4건 — 3건은 기준선(`.speclinker/test_baseline.json`: 552 실행/3 실패, SR-300 시간창 드리프트)에 이미 있던 사전 결함이고, `ShopStaticResourceServingTest` 1건은 **이번 변경이 새로 만든 회귀**다(`Status expected:<401> but was:<200>`). dev 기록의 "전체 통과"는 `-Dtest=ApiKeyAuthIntegrationTest,ProductControllerTest`로 범위를 좁혀 실행한 결과라 이 실패를 보지 못했다.

- 필수 수정(FAIL시):
  1. **`modules/shop-api/src/test/java/com/sm/lab/shop/web/ShopStaticResourceServingTest.java:70-73`** — `apiWithoutApiKey_stillRejectedUnauthorized_shopWhitelistDidNotLeak`가 이번 TO-BE와 정면 충돌한다. **테스트를 지우지 말 것**(이 테스트의 의도는 "/shop 화이트리스트가 /api/\*\*로 새지 않았다"는 감시이고 그 의도는 여전히 유효하다). 감시 대상 경로를 **여전히 보호되는 API**(예: `GET /api/orders` 또는 `GET /api/members`)로 바꿔 401 단언을 유지하고, `/api/products` GET은 SR-307로 **의도적으로 공개**됐음을 주석으로 남긴다. 기존 `/api/products` 줄을 그냥 200으로 바꾸기만 하면 화이트리스트 누출 감시가 통째로 사라지므로 불가.
  2. 수정 후 **좁힌 범위가 아니라 `mvn -o test` 전체**로 재확인한다. 허용 기준은 기준선과 동일한 실패 3건(`OrderListEndToEndIntegrationTest` 2건 + `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`, 전부 SR-300 시간창 이월)뿐이다. 그 외 실패가 1건이라도 남으면 이번 변경이 만든 회귀다.

- 권고(이번 라운드에 함께 처리 — medium 이상 전부 열거):
  1. **[medium · Layer2/Layer1]** 화이트리스트는 `shouldNotFilter`라 필터가 **통째로 스킵**된다. 그래서 **키를 보낸 요청의 동작도 바뀐다**: `GET /api/products?memberId=M-0002` + member 키는 종전 `evaluateMemberScope`의 일반 규칙 (a)로 **403**이었는데 이제 **200**이다. 확정 문답의 회귀 범위에 "키를 보낸 요청의 동작 불변"이 명시돼 있으므로 이 이탈은 기록 없이 넘어가면 안 된다. 실제 데이터 영향은 없다(`ProductController`는 `memberId`를 전혀 참조하지 않는다) — **수용 결정을 클래스 Javadoc SR-307 단락에 한 줄로 명시하고**, `ApiKeyAuthIntegrationTest`에 이 경로의 새 기대(200)를 고정하는 테스트를 1건 추가해 다음 SR이 조용히 되돌리지 못하게 한다.
  2. **[medium · Layer2]** `PRODUCT_ITEM_PATH = ^/api/products/([^/]+)$`는 `/api/products` 아래 **세그먼트 1개짜리 모든 GET**을 무인증으로 연다. 오늘은 `@GetMapping("/{sku}")` 하나뿐이라 실제 노출은 없지만, 나중에 `GET /api/products/export`·`/api/products/inventory` 같은 관리용 서브리소스가 추가되면 **아무도 손대지 않아도 자동으로 공개**된다. 이 코드베이스는 같은 함정을 이미 인지하고 있다(`ORDERS_EXPORT_PATH`를 `ORDER_RESOURCE_PATH`보다 먼저 검사하는 주석). `PRODUCT_ITEM_PATH` 주석에 "이 정규식 아래로 서브리소스를 추가할 때는 공개 여부를 먼저 판정할 것"을 못 박거나, `/api/products/{sku}` 외 세그먼트를 배제하는 가드를 둔다.
  3. **[low · Layer3]** 쓰기 회귀 테스트가 **목록 경로(POST /api/products)에만** 있다. 단건 경로의 비-GET(`PATCH /api/products/SKU-1001` 등)이 401인지는 미검증이다 — `rawRequest("PATCH", ...)` 헬퍼가 이미 있으므로 1건 추가로 메워진다.
  4. **[low · Layer3]** `HEAD /api/products`는 여전히 401이다(`"GET".equalsIgnoreCase(method)`가 거짓). Spring MVC는 `@GetMapping`에 HEAD를 자동 매핑하므로 헬스체크·CDN·프리페치가 HEAD를 쓰면 막힌다. AS-IS에서도 401이었으므로 **회귀는 아니고** 이번 범위 밖 — 후속 TODO로만 남긴다.

- 재동기화 입력(STEP 5.5 몫 — 권고 아님, 이번 라운드 수정 대상 아님):
  - `docs/05_설계서/order/INF/INF-ORD-008.md:67` — 응답 표의 `| 401 | 인증 실패 | 토큰 없음/만료 |` 행이 TO-BE와 어긋난다(무키 GET은 이제 200). 공개 경로가 된 사실과 함께 갱신 필요.
  - `docs/05_설계서/order/INF/INF-ORD-009.md` — 인증 요구에 대한 서술이 없다. GET 무키 공개(단건은 `sale_yn` 필터가 없어 판매종료 상품도 SKU를 알면 무인증 조회 가능)를 명시해야 한다.
  - `docs/변경관리/SR-307/02_변경명세.md:30,34` — INF-ORD-008·009의 TO-BE가 `[미상]` 그대로라 STORY 수용 기준의 변경 AC 2건도 `[미상]`이다. 이번 게이트는 확정 문답 9건과 회귀 AC로 판정했고 그 기준으로는 판정 가능했으나, 반영 단계에서 두 절을 채워야 AC 체크박스가 닫힌다.

### QA Gate — 2026-09-17 — PASS (round 2)
> round 1 FAIL의 필수 2건 + 사람 게이트가 지시한 구조 변경 2건을 **코드로 직접 대조**하고, 회귀는 범위를 좁히지 않은 `mvn -o test` **전체 실행**으로 QA가 독립 재현했다.

**round 1 필수·지시 반영 확인 (4/4 실측)**
1. ✅ **감시 테스트 대상 교체** — `ShopStaticResourceServingTest.java:74-78`. `apiWithoutApiKey_stillRejectedUnauthorized_shopWhitelistDidNotLeak`가 `GET /api/orders`(여전히 보호)로 바뀌었고 401 단언은 그대로다. 테스트 삭제·200 완화 없음. :69-73에 "`GET /api/products`는 SR-307로 의도적 공개" 사유 주석이 남았다. `/shop` 화이트리스트가 `/api/**`로 새면 여전히 잡힌다(`isOpenRoute`의 `/shop`·`/shop/` 절은 `/api/orders`에 매치될 수 없고, `/api/orders`는 화이트리스트 밖).
2. ✅ **전체 스위트 재확인** — 아래 Layer3 참조(QA가 직접 실행).
3. ✅ **필터를 스킵하지 않고 무키에서만 인증 면제** — `ApiKeyAuthFilter.java`. `isOpenRoute`(=`shouldNotFilter`)에 상품 경로가 **없다**(:258-278 확인, round 1의 `isOpenRoute(path, method)` 시그니처 변경도 원복). 면제는 `doFilterInternal`:318-329의 `if (key == null && path != null && isPublicProductReadPath(path) && "GET".equalsIgnoreCase(...))` 한 곳뿐이다. 키를 보낸 요청(무효 키 포함)은 `key == null`이 거짓이라 이 분기를 타지 못하고 종전 401/`evaluateMemberScope` 경로를 그대로 거친다 — 확정 문답 "키를 보낸 요청의 동작 불변"이 코드 수준에서 복구됐고, `productsListRoute_withMemberApiKeyAndOtherMemberIdParam_returns403Forbidden`(member 키 + `memberId=M-0002` → 403)이 이를 고정한다.
4. ✅ **단건 정규식을 실제 SKU 형식으로 좁힘** — `:215` `PRODUCT_ITEM_PATH = ^/api/products/(SKU-[A-Za-z0-9-]+)$`. round 1의 `([^/]+)`(세그먼트 1개면 전부 공개)가 사라졌다. `productsItemRoute_virtualSubresourceExport_withoutApiKey_returns401`(`GET /api/products/export` → 401)로 실측 고정. 실패 모드가 fail-closed다. 두 구조 변경의 사유가 클래스 Javadoc :97-116(round 2 단락 2개) + 상수·분기 인라인 주석에 남았다.

- **Layer1 스펙**: **pass** — diff는 `ApiKeyAuthFilter.java`(+64) · `ApiKeyAuthIntegrationTest.java`(+101) · `ShopStaticResourceServingTest.java`(+7/-7) **3파일뿐**이고 주석을 걷어낸 실질 변경은 상수 2·헬퍼 1·`doFilterInternal` 4줄이다(QA가 `git diff`로 직접 대조). 컨트롤러·서비스·DAO·매퍼·DDL·응답 스키마 전부 불변 — 회귀 AC 4건(`sale_yn='Y'` 상시필터 · `keyword` LIKE · `inStock` · 단건은 `sale_yn` 필터 없음)은 쿼리를 건드리지 않아 유지된다(`ProductServiceTest` 9 · `ProductDaoTest` 11 · `ProductControllerTest` 6 전부 통과 실측). 확정 문답의 포함/제외 범위(쓰기 미공개·레이트리밋/캐시/CORS 무변경·응답 필드 무변경)와 일치한다. 무키 GET 목록·단건 200과 없는 SKU 404(오류 계약 불변)도 실측 통과. 앱 서빙 경로 검증: `shop-web/src/api.ts`의 목록 호출은 `X-Api-Key`를 세팅하지 않으므로(키 세팅은 `logout` 한 곳뿐) `/shop`에서 무키로 나가 이번 면제 분기를 정확히 탄다 — SR의 원래 목적(401 해소)이 달성된다.
- **Layer2 보안**: **pass** — ① 화이트리스트(`shouldNotFilter`) 우회가 아니라 `doFilterInternal` 내부 면제라, 이 경로에서도 인가(`evaluateMemberScope`) 판정은 항상 살아 있다(403 테스트로 고정). ② 면제 조건이 무키 **AND** 경로 **AND** GET 3중이라 쓰기는 열리지 않는다 — `POST /api/products` 401 · `PATCH /api/products/SKU-1001` 401 실측(필터 단계 401, 405로 새지 않음 = 메서드 판정이 실제로 작동). ③ 경로 판정은 `resolveNormalizedPath`(servletPath+pathInfo, 디코딩·정규화된 값)를 그대로 재사용하므로 `/%61pi/...`·`;matrix` 인코딩 우회가 성립하지 않고, `path == null`(정규화 실패)이면 면제를 타지 않고 401로 떨어진다(fail-closed). ④ 정규식이 `SKU-` 접두사 + 슬래시 불가라 `..`·서브리소스 세그먼트가 매치되지 않는다. ⑤ 노출 데이터는 `Product`(sku/productName/price/stockQty/saleYn)뿐 — PII·내부 식별자 신규 노출 없음. ⑥ must 규칙(`no-sysout`·`no-printstacktrace`·`no-select-star`·`ddl-idempotent`·`controller-has-test`) 위반 없음(변경 파일 grep 실측, 신규 컨트롤러·매퍼·DDL 없음).
- **Layer3 회귀**: **pass** — QA가 `mvnw.cmd -o test` **전체**를 직접 실행했다(범위 미축소). **Tests run: 560, Failures: 3, Errors: 0, Skipped: 0**. 실패 3건은 허용 기준선과 정확히 일치한다: `OrderListEndToEndIntegrationTest.orderList_realDbEndToEnd_rendersLatestDeliveryStateColumnForEachOrder` · `…_showsDashWhenOrderHasNoDeliveryHistory` · `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`(전부 SR-300 시간창 이월, 이 SR 무관). **round 1이 새로 만들었던 `ShopStaticResourceServingTest` 실패는 사라졌다(7/7 통과)**. `ApiKeyAuthIntegrationTest` 67건 중 실패는 위 기준선 1건뿐. 기준선(`.speclinker/test_baseline.json`: 552 실행/3 실패) 대비 신규 실패 **0건**, 실행 건수 +8(SR-307 신규 테스트) — 이번 변경이 만든 회귀 없음. 사례집(`harness/antipatterns.all.md` 35줄) 대조: SR-231 r1 역할 반전·SR-232 r2 자격 판정 누락·SR-301 #2 관례 위치 이탈·SR-302 #1 가드 테스트 거짓 보증 등 기재 패턴 중 재발 항목 없음(신규 테스트는 전부 HTTP 실측 단언이라 "가드를 지워도 통과"하는 구조가 아니다 — `export` 401·403 테스트가 각각 정규식·필터 구조를 직접 행사한다).

- 필수 수정(FAIL시): 없음.
- 권고: 없음(medium 이상 0건 — 이번 라운드 전수 확인).

- 후속 TODO(이번 라운드 대상 아님 — 게이트를 다시 세우지 않는다):
  1. **[low · Layer2]** 공개 판정이 `SKU-` 접두사라는 **데이터 명명 관례**에 묶였다. 이 관례는 어디에도 강제되지 않는다(`PRODUCTS.sku`는 `VARCHAR(20)` PK, CHECK 제약 없음 · 앱에 상품 생성 경로 자체가 없어 매퍼 `product.xml`은 `decreaseStock`/`increaseStock`만 가짐). 현 시드 4건(`SKU-1001`~`SKU-1004`, DB 실조회)은 전부 매치하고 `/api/products/{sku}` 소비자가 아직 shop-web·Thymeleaf 어디에도 없어 **오늘 영향은 0**이며, 어긋나도 fail-closed(401)다. 다만 이 관례가 인증 정책의 근거가 된 이상 `SCH-ORD-005`(또는 INF-ORD-009)에 "SKU는 `SKU-` 접두사"를 명문화하거나 DB 제약으로 올리는 것을 후속 SR에 남긴다.
  2. **[low · Layer3]** 무키 `HEAD /api/products`는 여전히 401이다(round 1 권고 4 · 사람 지시 5로 범위 밖 확정). 현재 `productsListRoute_headWithoutApiKey_returns401`이 이 동작을 **고정**하고 있으므로, 후속 SR이 HEAD를 열 때는 이 테스트도 함께 뒤집어야 한다(테스트 주석에 기재됨).
  3. **[low · 관례]** `ApiKeyAuthFilter.java`가 547줄로 `file-size-cap`(should, 자바 450줄)을 초과한다 — 이번 변경 이전부터 초과 상태였고 `should`라 차단 대상이 아니다. STORY "범위 밖" 절이 이미 `OpenRoutePolicy` 추출을 후속 과제로 적어 뒀다.
  4. **[정보]** `## Dev 기록` round 2의 "기존 `head()` TestRestTemplate 헬퍼를 삭제하고"는 서술이 부정확하다 — `git diff` 실측상 삭제된 줄은 `rawRequest`의 3항 연산자 한 블록뿐이고, 그 `head()` 헬퍼는 이번 라운드 안에서 추가됐다가 제거돼 순변화가 없다. 코드 영향 없음(기록 정확도 메모).

- 재동기화 입력(STEP 5.5 몫 — 권고 아님, round 1에서 이월 · 이번 라운드 수정 대상 아님):
  - `docs/05_설계서/order/INF/INF-ORD-008.md:67` — 응답 표의 `| 401 | 인증 실패 | 토큰 없음/만료 |` 행이 TO-BE와 어긋난다(무키 GET은 이제 200). 공개 경로가 된 사실과 **함께 "키를 보낸 요청은 종전 인증·인가 판정 그대로"**(round 2 구조)도 적어야 한다.
  - `docs/05_설계서/order/INF/INF-ORD-009.md` — 인증 요구 서술이 없다. 무키 GET 공개 + 단건은 `sale_yn` 필터가 없어 판매종료 상품도 SKU를 알면 무인증 조회 가능 + **공개 대상 SKU 형식이 `SKU-` 접두사로 한정**됨을 명시해야 한다.
  - `docs/변경관리/SR-307/02_변경명세.md:30,34` — INF-ORD-008·009의 TO-BE가 `[미상]` 그대로라 STORY 수용 기준의 변경 AC 2건도 `[미상]`이다. 이번 게이트는 확정 문답 9건 + 회귀 AC로 판정 가능했으나, 반영 단계에서 두 절을 채워야 AC 체크박스가 닫힌다.

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [high/regression] 기존 회귀 테스트 ShopStaticResourceServingTest.apiWithoutApiKey_stillRejectedUnauthorized_shopWhitelistDidNotLeak(:70-73)가 무키 GET /api/products=401을 고정하고 있어 깨진다. QA 실측 mvn -o test 전체 556건 중 실패 4건 — 기준선(552 실행/3 실패, SR-300 이월) 대비 신규 실패 1건(Status expected:<401> but was:<200>). dev는 -Dtest=ApiKeyAuthIntegrationTest,ProductControllerTest로 범위를 좁혀 실행해 못 봤다. 계획의 '테스트' 절이 회귀 확인 범위를 ApiKeyAuthIntegrationTest 한 클래스로 한정한 것이 원인(plan_gap). → 테스트를 삭제하지 말고 감시 대상 경로를 여전히 보호되는 API(GET /api/orders 등)로 교체해 401 단언 의도를 유지하고, /api/products GET은 SR-307로 의도적 공개임을 주석으로 남긴다. 이후 좁힌 범위가 아니라 mvn -o test 전체로 재확인하고, 허용 실패는 기준선 3건(OrderListEndToEndIntegrationTest 2건 + ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders)뿐임을 확인한다.
2. [medium/security] 화이트리스트가 shouldNotFilter라 필터가 통째로 스킵되어 '키를 보낸 요청의 동작 불변'(확정 문답 회귀 범위)이 깨진다 — GET /api/products?memberId=M-0002 + member 키가 종전 evaluateMemberScope 일반규칙(a)로 403이었는데 이제 200. ProductController가 memberId를 참조하지 않아 데이터 노출은 없으나 명시된 회귀 조항에서 벗어난 이탈이다. → 수용 결정을 ApiKeyAuthFilter 클래스 Javadoc의 SR-307 단락에 명시하고, ApiKeyAuthIntegrationTest에 이 경로의 새 기대(200)를 고정하는 테스트 1건을 추가한다.
3. [medium/security] PRODUCT_ITEM_PATH = ^/api/products/([^/]+)$ 가 /api/products 아래 세그먼트 1개짜리 모든 GET을 무인증으로 연다. 오늘은 @GetMapping("/{sku}") 하나뿐이라 실제 노출은 없지만, 향후 GET /api/products/export·/inventory 같은 관리용 서브리소스가 추가되면 아무도 손대지 않아도 자동 공개된다. 같은 함정을 이 코드베이스가 이미 인지하고 있다(ORDERS_EXPORT_PATH를 ORDER_RESOURCE_PATH보다 먼저 검사하는 주석). → PRODUCT_ITEM_PATH 주석에 '이 정규식 아래 서브리소스 추가 시 공개 여부를 먼저 판정할 것'을 명시하거나, /api/products/{sku} 외 세그먼트를 배제하는 가드를 둔다.
4. [low/regression] 쓰기 회귀 테스트가 목록 경로(POST /api/products)에만 있고 단건 경로의 비-GET(PATCH /api/products/SKU-1001 등)이 401인지 미검증이다. → 기존 rawRequest("PATCH", ...) 헬퍼로 단건 경로 비-GET 401 테스트 1건을 추가한다.
5. [low/regression] HEAD /api/products가 여전히 401이다("GET".equalsIgnoreCase(method) 거짓). Spring MVC는 @GetMapping에 HEAD를 자동 매핑하므로 헬스체크·CDN·프리페치가 HEAD를 쓰면 막힌다. AS-IS에서도 401이라 회귀는 아니고 이번 범위 밖. → 후속 TODO로만 남긴다(이번 라운드 수정 대상 아님).

사람 코멘트: [결정 요약] 재작업 — 필수 2건 + medium 2건까지 이번에 처리한다. 감시 테스트의 의도(화이트리스트 누출 감시)는 보존하고 대상만 여전히 보호되는 API로 바꾼다. 회귀 확인은 반드시 전체 실행으로. [구현 방식] 1) ShopStaticResourceServingTest의 감시 테스트는 대상 경로를 GET /api/orders(보호 유지)로 바꾸고 주석에 'GET /api/products는 SR-307로 의도적 공개'를 남긴다. 2) 회귀는 mvn -o test 전체로 재확인(허용 실패는 SR-300 이월 3건뿐). 3) 필터를 통째로 스킵하지 말고 공개 경로에서도 필터는 타되 인증만 면제해, 키를 보낸 요청의 기존 동작(예: memberId 파라미터 권한 판정 403)이 그대로이게 한다 — 이게 확정 문답의 '키 있는 요청 동작 불변'이다. 4) 단건 정규식은 서브리소스가 자동 공개되지 않도록 sku 문자 집합을 좁힌다(슬래시 없는 영숫자·하이픈 등 실제 sku 형식). 이 두 결정을 Javadoc에 남길 것. [테스트·완료 조건] 추가 단언: 회원 키로 GET /api/products?memberId=<남의 ID> 가 종전과 같은 판정(403) · PATCH/HEAD /api/products 401 · GET /api/products/export 같은 가상의 서브리소스가 공개되지 않음(401) · 무키 GET 목록·단건 200. 전체 스위트 재실행 결과를 Dev 기록에 붙일 것.

## 테스트 결과 (STEP 5)

AC↔TC 매핑 — 전부 `docs/07_테스트케이스/TC_v1.0.md` "SR-307.1" 절에 `linked_tc` 주석과 함께 등재(TC-FUNC-order-013-60~67).

| TC-ID | AC | 테스트 함수 | 결과 |
|-------|----|-----------|------|
| TC-FUNC-order-013-60 | TO-BE: 무키 GET /api/products 목록 → 200 | `productsListRoute_withoutApiKey_returns200()` | 통과 |
| TC-FUNC-order-013-61 | TO-BE: 무키 GET /api/products/{sku} 단건 → 200 | `productsItemRoute_withoutApiKey_existingSku_returns200()` | 통과 |
| TC-FUNC-order-013-62 | 회귀: 없는 SKU → 404 | `productsItemRoute_withoutApiKey_unknownSku_returns404()` | 통과 |
| TC-FUNC-order-013-63 | 회귀: 무키 POST /api/products → 401 | `productsWritePlaceholder_withoutApiKey_returns401()` | 통과 |
| TC-FUNC-order-013-64 | 회귀: 무키 PATCH /api/products/{sku} → 401 | `productsItemPlaceholder_patchWithoutApiKey_returns401()` | 통과 |
| TC-FUNC-order-013-65 | 회귀: 무키 HEAD /api/products → 401 | `productsListRoute_headWithoutApiKey_returns401()` | 통과 |
| TC-FUNC-order-013-66 | 회귀: 키 있는 요청 동작 불변(memberId=타인 → 403) | `productsListRoute_withMemberApiKeyAndOtherMemberIdParam_returns403Forbidden()` | 통과 |
| TC-FUNC-order-013-67 | 회귀: 가상 서브리소스 미공개(/export → 401) | `productsItemRoute_virtualSubresourceExport_withoutApiKey_returns401()` | 통과 |

회귀 AC(`sale_yn='Y'` 상시필터·keyword LIKE·inStock·단건 `sale_yn` 필터 없음)는 신규 TC 없이 기존 `ProductControllerTest`·`ProductServiceTest`·`ProductDaoTest`로 유지 검증(쿼리·DAO 무변경).

회귀 TC 경로(SR Epic): `docs/변경관리/SR-307/03_TC.md` 없음 — SR-307 회귀는 위 AC + 기존 `ApiKeyAuthIntegrationTest` 67건(조회 권한·admin/member 스코프·IDOR·경로 정규화 우회)으로 커버.

전체 스위트: `mvnw.cmd -o test`(범위 미축소) — **Tests run: 560, Failures: 3, Errors: 0**. 실패 3건은 기준선(SR-300 시간창 이월, 이 SR 무관)과 정확히 일치: `OrderListEndToEndIntegrationTest` 2건 + `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`. 신규 실패 0.

## 후속 추적(TODO)

- **[medium, rules/should]** `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java`가 `file-size-cap`(should, 자바 450줄) 초과 — 이 SR 이전부터의 상태이며 이번 SR은 소폭 추가만 했다. 사람 결정(STEP 5.3 CONCERNS 게이트, 2026-09-17): "인증 필터를 지금 쪼개는 것은 위험 대비 이득이 없다 — 후속 SR로 등록". **후속 SR 제목(제안): "인증 필터 분리로 file-size-cap 해소"** — `OpenRoutePolicy`/`MemberScopePolicy` 등으로 화이트리스트·소유권 판정 로직을 분리하는 리팩토링. 이 SR-307.1은 이 항목 때문에 막지 않는다(추적등록 후 진행).
- **[정보]** `modules/shop-api/src/test/resources/shop-fixture/present/assets/app.test123.js`(scope_verify가 이 항목으로 classified한 미추적 파일)는 SR-301 유래 테스트 픽스처로, 이 SR의 AC와 무관. 사람이 SR-301로 직접 커밋 처리하기로 함(이 항목의 몫 아님).
