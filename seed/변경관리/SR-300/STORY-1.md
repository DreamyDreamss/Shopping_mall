---
story-id: STORY-SR-300.1
item: SR-300.1
title: GET /api/orders — 주문 목록 조회
status: Done
domain: order
created: 2026-09-19
spec_markers: 0
sr-id: SR-300
approved_sha: aa699ac6826e
---

# STORY-SR-300.1 — 주문 목록 기본 조회창(최근 30일) 시간 드리프트로 통합테스트 날짜 의존 실패 — GET /api/orders — 주문 목록 조회

## Story
주문 목록 기본 조회창(최근 30일) 시간 드리프트로 통합테스트 날짜 의존 실패 — GET /api/orders — 주문 목록 조회


## 변경 컨텍스트 (SR-300)
> 이 story는 변경요청 **SR-300 — 주문 목록 기본 조회창(최근 30일) 시간 드리프트로 통합테스트 날짜 의존 실패** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-300/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-300/02_변경명세.md`

### 확정된 요건 문답 4건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: OrderService.list의 기본 조회창(startDate/endDate 미제시 시 오늘-30일~오늘) 계산을 주입 가능한 Clock으로 바꾼다(기존 MemberAddressService의 Clock 주입 패턴 재사용, 운영 기본값 = 시스템 시계). 날짜에 따라 실패하던 테스트 3건(OrderListEndToEndIntegrationTest 2건 · ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders)이 시드 주문일(2026-08-15~17) 기준 고정 시계로 날짜와 무관하게 통과한다. 제외: 기본 조회창 30일 정책 변경 · 시드 데이터 날짜 변경 · 다른 날짜/플레이키 이슈(SR-299 레이트리밋 행 정리) · 프런트 변경
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 운영 동작 불변: 기본 조회창(최근 30일, 양끝 포함) · 두 파라미터 독립 보정 · 역전 시 400과 기존 안내 문구(기본값 적용/명시 역전 두 갈래) · 응답 스키마(totalCount/page/items) · 명시 startDate/endDate 경로 · 주문 엑셀 export 경로(null,null) · 다른 서비스의 시계 사용
- **기존 클라이언트와의 하위호환이 필요한가?** — 계약 불변 — GET /api/orders의 요청 파라미터·기본값 의미(최근 30일)·응답 스키마(totalCount/page/items)를 바꾸지 않는다. 바뀌는 것은 '오늘'을 읽는 방법(주입된 Clock)뿐이며 운영에서는 시스템 시계라 결과가 동일하다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 오류 계약 불변 — 역전 시 400과 기존 안내 문구 두 갈래(기본값 적용/명시 역전)를 그대로 유지한다. 새 오류 코드 없음.

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-300/02_변경명세.md`에서 도출)
- [x] INF-ORD-003: '오늘'을 읽는 방법만 주입 가능한 `Clock`으로 바꾼다(기존 `MemberAddressService`의 Clock 주입 패턴 재사용).
- [x] INF-ORD-003: 운영 기본값은 시스템 시계이므로 운영 결과는 AS-IS와 동일 — 요청 파라미터·기본값 의미(최근 30일)·응답 스키마(`totalCount`/`page`/`items`)는 변경하지 않는다.
- [x] INF-ORD-003: 테스트는 고정 `Clock`으로 시드 주문일(2026-08-15~17) 기준 창을 재현해 날짜와 무관하게 통과한다.

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**
- [x] INF-ORD-003 (GET /api/orders): `del_yn = 'N'` 상시필터 → 논리삭제된 주문 제외
- [x] INF-ORD-003 (GET /api/orders): `memberId` 파라미터가 있으면 해당 회원 주문만, `orderState` 파라미터가 있으면 해당 상태만 필터(LAB-101 추가 요구사항)
- [x] INF-ORD-003 (GET /api/orders): `offset = max(0, page - 1) * size`
- [x] INF-ORD-003 (GET /api/orders): `ORDERS`와 `MEMBERS`를 조인해 `memberName`을 함께 반환(회원 미탈퇴 여부와 무관하게 조인만 수행)
- [x] INF-ORD-003 (GET /api/orders): 목록 응답의 각 항목은 `items`/`deliveries`를 채우지 않음(상세는 [[INF-ORD-004]])
- [x] INF-ORD-003 (GET /api/orders): **[반영: FUNC-order-001]** 기간 필터는 `orderState`/`memberId`와 **AND**로 결합되며, `ordered_at`(DATETIME) 기준
- [x] INF-ORD-003 (GET /api/orders): **[반영: FUNC-order-001]** `startDate`/`endDate`는 각각 독립적으로 기본값이 적용된 뒤 유효 구간(`effectiveStart`~`effectiveEnd`)이

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-003: INF-ORD-003: GET /api/orders — 주문 목록 조회 / > **개요:** 회원·주문상태·조회기간으로 필터링한 주문 목록을 페이징 조회한다. (linked_func: FUNC-order-001, LAB-101) / > [반영: FUNC-order-001] SR-205 — 조회 기간(startDate/endDate) 필터 추가. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/OrderController.java:28-44` — [docs/05_설계서/order/INF/INF-ORD-003.md](../../05_설계서/order/INF/INF-ORD-003.md)
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
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java` — `list()`의 `LocalDate.now()` 2곳(effectiveStart/effectiveEnd)을 `LocalDate.now(clock)`으로 교체. `Clock clock` 필드 추가 + 생성자 4벌 구성(아래 "데이터" 참조). `exportCsv`는 날짜를 쓰지 않으므로(필터 null,null) 무영향 — 손대지 않는다.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/ShopApiApplication.java` — `@Bean Clock clock() { return Clock.systemDefaultZone(); }` 추가(새 `@Configuration` 클래스를 만들지 않고 기존 `@SpringBootApplication`(=`@Configuration`)에 최소 추가 — CLAUDE.md "새 Configuration 남설 금지" 취지와 일치). 이 빈은 OrderService의 `@Autowired` 생성자가 Clock을 스프링 컨텍스트에서 받도록 하기 위함(그래야 SpringBootTest가 `@TestConfiguration`으로 override 가능) — 다른 서비스는 이 빈을 `@Autowired`하지 않으므로(전부 내부에서 `Clock.systemDefaultZone()` 직접 생성) 영향 없음(regression_keep "다른 서비스의 시계 사용 불변" 충족, 실측: grep으로 Clock 주입 지점이 OrderService 신설분 외 없음을 확인함).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/service/OrderServiceTest.java` — 신규 단위 테스트 1건 추가(고정 Clock 생성자로 기본 조회창이 시스템 시계가 아니라 주입된 Clock을 쓰는지 직접 확인). 기존 `new OrderService(orderDao, productDao)` / `new OrderService(orderDao, productDao, 3)` 두 호출부는 그대로 컴파일되게 시그니처를 보존한다(아래 "데이터").
  - `modules/shop-api/src/test/java/com/sm/lab/shop/controller/OrderListEndToEndIntegrationTest.java` — 중첩 정적 `@TestConfiguration` 클래스로 `@Primary Clock` 빈(고정, 시드일 포함)을 얹어 실 스프링 컨텍스트의 `OrderService` 빈이 고정 시계를 쓰게 한다. 기존 두 테스트 본문은 바꾸지 않는다(원인이 날짜 드리프트였으므로 Clock 고정만으로 통과해야 한다 — 통과 안 되면 테스트가 아니라 원인 진단을 다시 본다).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/web/ApiKeyAuthIntegrationTest.java` — 동일하게 중첩 `@TestConfiguration`(`@Primary Clock`)을 추가. 이 클래스는 60여 개 테스트를 공유하는 단일 `@SpringBootTest` 컨텍스트이므로, Clock 빈을 override해도 다른 테스트에 영향이 없는지가 이 계획의 핵심 리스크(아래 "순서·보안"에서 확인 근거 명시).
- **데이터**: DDL/테이블 변경 없음. 트랜잭션 경계 변경 없음(`list()`는 원래도 `@Transactional` 아님, 단순 조회). 락 불필요.
  - `OrderService` 생성자 4벌(순서 중요 — 기존 2벌의 시그니처는 절대 변경하지 않는다, `OrderServiceTest`가 이미 그 두 시그니처로 직접 생성 중):
    1. `@Autowired public OrderService(OrderDao, ProductDao, @Value("${lab.export-max-rows:1000}") int exportMaxRows, Clock clock)` — 신설, 유일한 `@Autowired` 생성자(스프링 빈 생성 경로).
    2. `public OrderService(OrderDao, ProductDao)` — 기존 시그니처 그대로 유지, `this(orderDao, productDao, 1000, Clock.systemDefaultZone())`로 위임(내부 변경만, 호출부 무영향).
    3. `public OrderService(OrderDao, ProductDao, int exportMaxRows)` — 기존 시그니처 그대로 유지(현재 `@Autowired`가 붙어 있던 자리) → `@Autowired` 제거하고 `this(orderDao, productDao, exportMaxRows, Clock.systemDefaultZone())`로 위임. `OrderServiceTest`의 두 호출부(`new OrderService(orderDao, productDao, 3)`)는 그대로 컴파일된다.
    4. `OrderService(OrderDao, ProductDao, Clock clock)`(package-private, `MemberAddressService`의 `(dao, clock)` 시임 생성자와 동일한 패턴) — 신설, 고정 Clock 단위 테스트 전용. `this(orderDao, productDao, 1000, clock)`로 위임.
  - `list()` 본문: `LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now(clock).minusDays(30);` / `LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now(clock);` — 이 2줄 외 로직(역전 판정·오프셋·DAO 호출)은 그대로.
  - 테스트용 고정 시계 값: `Clock.fixed(LocalDateTime.of(2026, 8, 20, 9, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())` — "오늘"을 2026-08-20으로 고정하면 기본창(오늘-30일~오늘=2026-07-21~2026-08-20)이 시드 주문일(08-15~17)을 전부 포함한다. `ZoneId.systemDefault()`를 쓰는 이유: `LocalDate.now()`(AS-IS)도 시스템 기본 zone 기준이었으므로 zone을 바꾸면 그 자체가 새로운 변수가 된다(`MemberSignupRateLimitTest`가 쓰는 것과 동일 관례).
- **순서·보안**: 인증·인가 순서 변경 없음(이 SR은 인증 경로를 건드리지 않는다). 부수효과 없음(로그·발송·이벤트 없는 순수 조회). `ApiKeyAuthIntegrationTest`의 `@Primary Clock` override가 같은 클래스의 다른 60여 테스트에 영향 없는 근거: (1) 이 코드베이스에서 `Clock`을 `@Autowired`로 받는 컴포넌트는 이번에 신설하는 `OrderService` 4-arg 생성자뿐이다(grep 확인 — 다른 서비스는 전부 내부 `Clock.systemDefaultZone()` 직접 생성, 스프링 빈 아님) — 즉 이 override는 `OrderService`의 "오늘" 계산에만 도달한다. (2) 같은 클래스 안에서 `/api/orders` 목록(날짜 미지정)을 조회하는 테스트는 대상 테스트(`memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`) 하나뿐(같은 파일의 다른 `/api/orders/**` 호출은 상세조회·취소·export — 전부 date 파라미터 미사용 경로이거나 export처럼 날짜 필터 자체가 없는 경로)이다. (3) export 경로(`exportCsv`)는 `selectOrders(memberId, orderState, null, null, ...)`로 애초에 `LocalDate.now()`를 호출하지 않아(코드 118행 확인) Clock 변경과 무관.
- **계약**: 새 오류 코드·응답 봉투·상태 코드 없음(확정 문답 api_error·api_compat대로 전부 불변). `GET /api/orders`의 요청 파라미터·기본값 의미·응답 스키마(`totalCount`/`page`/`items`) 그대로.
- **테스트**:
  - (신규, 단위) `OrderServiceTest#list_noDateParams_usesInjectedClockNotSystemClock` — `new OrderService(orderDao, productDao, fixedClock)`으로 생성 후 `list(null,null,null,null,1,20)` 호출, DAO에 넘어간 `effectiveStart`/`effectiveEnd`를 `ArgumentCaptor`로 캡처해 `LocalDate.of(2026,7,21)`/`LocalDate.of(2026,8,20)`(시스템 날짜와 무관한 고정값)와 정확히 일치하는지 단언 — 시스템 시계를 참조하지 않는다는 것 자체가 이 테스트의 요지이므로 기존 `list_noDateParams_defaultsToLast30DaysInclusive`(시스템 `LocalDate.now()`와 비교, 2-arg 생성자 사용)처럼 상대값과 비교하지 않는다.
  - (기존, 회귀— 코드 안 바꿈) `OrderListEndToEndIntegrationTest`의 2건, `ApiKeyAuthIntegrationTest#memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders` — Clock 고정만으로 그대로 통과해야 한다.
  - (기존, 회귀 확인) `OrderServiceTest`의 날짜 관련 기존 7건(`list_*`) — 전부 2-arg `newService()`(시스템 시계 위임)를 쓰므로 동작·기대값 변경 없음, 그대로 통과해야 한다.
  - 최종 확인은 사례집(SR-307 #1) 교훈대로 좁힌 클래스 지정이 아니라 `mvn -o test` 전체 스위트로 한다(계획에 특정 클래스를 적어도 전체 실행이 최종 판정).
- **테스트 격리**: 신규 단위 테스트(Mockito)는 DB를 건드리지 않는다 — 격리 이슈 없음. `OrderListEndToEndIntegrationTest`·`ApiKeyAuthIntegrationTest`의 대상 테스트는 이미 GET 전용(쓰기 없음, 기존 상태 그대로) — 이번 변경으로 새로 쓰는 행이 생기지 않는다.
- **폴백·우회 경로의 자격 판정**: 해당 없음 — 이 SR은 인증·조회 경로를 새로 열지 않는다(날짜 계산 방식만 바뀐다).
- **프레임워크 실행 모델 함정**: Spring Test 컨텍스트 캐싱 — `OrderListEndToEndIntegrationTest`·`ApiKeyAuthIntegrationTest` 각각의 중첩 `@TestConfiguration`이 서로 다른 설정 조합이라 스프링이 클래스별로 별도 캐시 컨텍스트를 띄운다(같은 프로젝트의 다른 `@SpringBootTest`(Clock override 없음)와도 별개 컨텍스트) — 상태 공유·오염 없음, 다만 전체 스위트 기동 시간이 컨텍스트 재사용 실패로 소폭 늘 수 있다(기능 영향 없음). 그 외 해당 없음(비동기·재시도·프록시 self-invocation 없음).
- **범위 밖**: 기본 조회창 30일 정책 변경, 시드 데이터 날짜 변경, SR-299(레이트리밋 행 정리), 프런트 변경, `OrderExportIntegrationTest` 등 이 SR이 지정한 3건 밖의 다른 날짜 의존 테스트 정리(다른 `@SpringBootTest` 클래스는 별도 컨텍스트라 이번 Clock override의 영향도 받지 않고 고쳐지지도 않는다 — 필요하면 별도 SR).
- **실패 사례집 대조**: 이 antipatterns.all.md 자체에 이미 이 결함의 1차 기록("주문 조회 기본 창이 LocalDate.now().minusDays(30)인데... (SR-300 이월)")이 있다 — 이번이 그 이월 항목의 해결분. 그 외 닿는 항목: "자정 경계 테스트가 Clock.fixed로 시각만 주입하고 기본 허용 스텁을 그대로 둬 무조건 통과"(SR-297 #2) — 이 계획의 신규 단위 테스트는 스텁을 관대하게 두지 않고 DAO에 캡처된 실제 `LocalDate` 값을 고정 기대값과 정확히 비교해 같은 함정(허수아비 통과)을 피한다. "회귀 확인은 좁힌 클래스가 아니라 전체 스위트로"(SR-307 #1)를 위 "테스트" 절 마지막 줄에 그대로 반영했다.

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
계획 확인: 계획대로 진행 (2026-09-19)

구현 완료 (2026-09-19) — 계획대로 진행, 이탈 없음.

- 수정 파일:
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java` — `list()`의
    `LocalDate.now()` 2곳을 `LocalDate.now(clock)`으로 교체. `Clock clock` 필드 추가, 생성자 4벌
    구성(계획대로 — 4-arg `@Autowired`(스프링 빈 경로), 2-arg/3-arg 기존 시그니처 보존(내부 위임),
    신규 package-private `(dao, productDao, Clock)` 시임 생성자). `exportCsv`는 무변경.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/ShopApiApplication.java` — `@Bean Clock clock()`
    추가(기존 `@SpringBootApplication`에 최소 추가, 새 `@Configuration` 없음).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/service/OrderServiceTest.java` — 신규 단위
    테스트 `list_noDateParams_usesInjectedClockNotSystemClock` 추가(고정 Clock, DAO에 캡처된
    `effectiveStart`/`effectiveEnd`를 고정 기대값 `2026-07-21`/`2026-08-20`과 정확히 비교). 기존
    호출부(2-arg/3-arg) 전부 그대로 컴파일·통과.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/controller/OrderListEndToEndIntegrationTest.java` —
    중첩 `@TestConfiguration` `FixedClockTestConfig`(`@Primary Clock`, 고정 2026-08-20) 추가. 기존
    두 테스트 본문은 무변경.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/web/ApiKeyAuthIntegrationTest.java` — 동일하게
    중첩 `FixedClockTestConfig` 추가.

- 계획과 다르게 간 지점(코드 작성 전 파악, 계획 골격은 그대로): 두 통합테스트의 `@TestConfiguration`
  안 `@Bean` 메서드명을 계획대로 `clock()`으로 하면 `ShopApiApplication`의 `clock` 빈과 이름이 같아
  `BeanDefinitionOverrideException`(스프링 부트 기본값 = 빈 정의 오버라이딩 비허용)으로 컨텍스트
  기동 자체가 실패한다(최초 실행에서 실측 — `ApiKeyAuthIntegrationTest` 67건 전부 컨텍스트 로드
  실패로 에러). `@Primary`는 "같은 타입 중 우선순위"이지 "같은 이름 재정의 허용"이 아니어서 이름
  충돌은 별개 문제였다. 메서드명을 `fixedTestClock()`으로 바꿔 빈 이름을 분리하고 `@Primary`로 타입
  기준 주입만 우선시키는 방식으로 해결(두 통합테스트 파일 동일 적용).

- 검증: `mvnw test -Dtest=OrderServiceTest,OrderListEndToEndIntegrationTest,ApiKeyAuthIntegrationTest`
  전부 통과 후, `mvnw test` 전체 스위트 574건 전부 통과(BUILD SUCCESS) — 사례집(SR-307 #1) 교훈대로
  좁힌 클래스가 아니라 전체 스위트로 최종 확인.

재작업 완료 (2026-09-19) — round1 QA CONCERNS 권고 3건(사람 코멘트 지시대로) 전부 반영, 코드
로직(`list()`의 Clock 사용) 변경 없음 — 문서화 주석 추가 + 테스트 리터럴 통합 + 판단 기록만.

- 재작업 지시 (1) 함정 경고 주석 추가 (create()는 범위 밖, 코드 변경 없음 — 주석만):
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java` — `create()`
    본문 첫 줄(주문번호 `LocalDate.now()` 위)에 "SR-300 범위 밖 — 시스템 시계 유지, list()만
    주입 Clock 사용, 통합테스트가 고정 시계로 '오늘'을 과거에 둔 상태에서 이 메서드로 주문을
    만들면 ordered_at이 실제 오늘이라 직후 날짜 없이 목록 조회 시 고정창 밖으로 조용히 빠질 수
    있음" 경고 주석 추가.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/controller/OrderListEndToEndIntegrationTest.java` —
    `FixedClockTestConfig` 바로 위에 동일 취지 경고 주석 추가.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/web/ApiKeyAuthIntegrationTest.java` — 동일하게
    `FixedClockTestConfig` 바로 위에 경고 주석 추가(이 클래스는 60여 건을 공유하는 컨텍스트라는
    점을 문구에 명시).
  - 이 3개 주석은 QA가 명시적으로 요청한 "향후 개발자용 함정 경고"이며 dev-agent 일반 규율의
    "추적 주석 금지"(linked_func류) 대상이 아니다(예외로 합의됨).

- 재작업 지시 (2) 고정 시계 리터럴 3곳 → 공용 상수 1곳 통합:
  - `modules/shop-api/src/test/java/com/sm/lab/shop/support/TestClocks.java` — 신규. `public static
    final Clock SEED_TODAY`(2026-08-20 09:00, `ZoneId.systemDefault()`)를 유일한 정의로 둠. 기존
    `AdminApiKeyTestConfig`와 같은 `com.sm.lab.shop.support` 테스트 지원 패키지에 배치(관례 일치).
  - `OrderServiceTest.java` — `Clock fixedClock = Clock.fixed(...)` 리터럴 조립을
    `TestClocks.SEED_TODAY` 참조로 교체, 미사용 `import java.time.ZoneId` 제거.
  - `OrderListEndToEndIntegrationTest.java` — `fixedTestClock()` 빈 본문을
    `return TestClocks.SEED_TODAY;`로 교체, 미사용 `LocalDateTime`/`ZoneId` import 제거.
  - `ApiKeyAuthIntegrationTest.java` — 동일 교체. 단 `LocalDateTime`은 이 클래스의 다른 테스트
    메서드(가입/로그인 관련, 7곳)가 별도로 쓰고 있어 import는 유지 — `ZoneId`만 제거.

- 재작업 지시 (3) 판단 기록: `harness/decisions.all.md`에 "시계 주입은 통합테스트 override
  필요 여부로 가른다: 스프링 빈 vs 내부 생성+시임" 5줄(정한 것/왜/틀렸으면 어떻게 아나/되돌리기)
  추가(2026-09-19 항목, 파일 맨 끝 append). 재동기화 입력(INF-ORD-003 anchors·요청 파라미터 표)은
  STEP 5.5 소관으로 이번 재작업에서 손대지 않음(사람 코멘트 명시).

- 검증(재작업): `mvnw -o test -Dtest=OrderServiceTest,OrderListEndToEndIntegrationTest,
  ApiKeyAuthIntegrationTest` 전부 통과 후, `mvnw -o test` 전체 스위트 재실행 — surefire 리포트
  집계로 574 tests run / 0 failures / 0 errors / 0 skipped 확인(BUILD 성공, exit 0). `create()`의
  `LocalDate.now()` 자체는 그대로(범위 밖 유지) — 변경 없음을 diff로 재확인.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-19 — CONCERNS
- **Layer1 스펙**: pass. TO-BE AC 3건 모두 충족. `list()`는 `LocalDate.now(clock)` 2줄만 바뀌고 역전 판정·오류 문구 2갈래·offset·DAO 호출·리턴 `Map.of("totalCount","page","items")`가 전부 그대로다(회귀 AC 7건 유지). 고정 시계 `2026-08-20 09:00`은 리터럴 Instant이라 기본창 `2026-07-21~2026-08-20`이 시드 주문일(08-15~17)을 날짜와 무관하게 포함한다. 신규 단위 테스트는 `ArgumentCaptor`로 DAO에 실제로 넘어간 `LocalDate`를 고정 기대값과 정확히 비교해 사례집 "자정 경계 테스트 허수아비 통과"(SR-297 #2)를 피했고, 통합테스트 2건은 시드 주문번호(`20260816-0002`·`20260815-0001`)를 직접 단언하므로 고정 시계가 빠지면 실제로 깨지는 유효한 가드다. 계획(사람 확인분)과의 이탈 없음 — Dev 기록의 `fixedTestClock()` 개명은 빈 이름 충돌(`BeanDefinitionOverrideException`) 회피로 계획 골격 유지.
- **Layer2 보안**: pass. 인증·인가 경로 무변경(`ApiKeyAuthFilter`·소유권 판정 미접촉). 새 오류 코드·응답 봉투 없음. 신설 `Clock` 빈의 다른 소비자 없음(액추에이터·Spring Security 전체 미탑재, `spring-security-crypto`만). 부수효과: `memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`의 부정 단언(타 회원 미노출)이 빈 목록에서 공허하게 통과하던 구간이 사라져 IDOR 회귀 가드가 오히려 강해졌다.
- **Layer3 회귀**: concerns. 신설 `@Bean Clock clock()`이 다른 컴포넌트의 생성자 해석을 바꾸지 않음을 실측 확인 — `Clock`을 받는 8개 서비스(`MemberAddressService`·`MemberLoginService`·`MemberSessionService`·`MemberSignupService`·`MemberRegistrationService`·`MemberPasswordResetService`·`MemberPasswordResetConfirmationService`·두 Scheduler) 전부 **비-Clock 생성자에 명시 `@Autowired`**가 붙어 있어 타입 기준 주입이 끼어들 여지가 없다. `exportCsv`(null,null) 무영향. 전체 스위트 **독립 재실행 574건 전부 통과(BUILD SUCCESS)** — 사례집 SR-307 #1대로 좁힌 클래스가 아니라 전량 실행으로 확인. 다만 아래 권고1의 잠재 함정이 남는다.
- 권고(CONCERNS시):
  1. **(medium)** `OrderService`가 "절반만 시계화"됐다 — `list()`는 주입 `clock`을 쓰는데 같은 클래스 `create()`(`OrderService.java:189`)는 여전히 raw `LocalDate.now()`로 주문번호를 만든다. 지금은 두 고정시계 테스트 클래스 어느 쪽도 주문을 생성하지 않아 실패가 없지만, 앞으로 `ApiKeyAuthIntegrationTest`(67건, 이 랩이 인증 회귀를 계속 붙이는 클래스)에 "주문 생성 → 날짜 없이 목록 조회" 테스트가 하나라도 들어오면 `ordered_at`이 실제 오늘(고정창 `~2026-08-20` 밖)이라 **조용히 0건**이 나오고, 증상은 인가/필터 결함처럼 보인다(오진 비용이 큰 형태 — 사례집의 고정시계 오해 계열 SR-297 #2·SR-234와 동종). `create()`의 시계 변경은 확정 문답 scope_freeze 밖이므로 **코드 변경 없이 문서화로 차단**할 것: `create()`에 "주문번호의 '오늘'은 SR-300 범위 밖 — 시스템 시계 유지" 주석 1줄 + 두 테스트 클래스 javadoc에 "고정 시계는 `list()` 기본 조회창에만 적용된다(주문 생성 시각은 실제 시계)" 1줄.
  2. **(low)** 고정 시계 리터럴 `LocalDateTime.of(2026, 8, 20, 9, 0)`이 3곳(두 `FixedClockTestConfig` + `OrderServiceTest`)에 각각 독립 기술돼 있다. 한 곳만 바뀌면 증상이 갈린다 — 공용 테스트 상수 1곳으로 모으는 편이 안전하다(후속 TODO 가능).
  3. **(low)** 이 코드베이스에 시계 패턴이 두 갈래가 됐다 — 다른 8개 서비스는 "내부 `Clock.systemDefaultZone()` + package-private 시임", `OrderService`만 "컨텍스트 빈 주입". 승인된 계획의 의도된 결과(통합테스트에서 컨텍스트 override가 필요했음)지만, 다음에 시계를 다루는 서비스가 어느 쪽을 따라야 하는지 판단 기록(`harness/decisions.all.md`)에 남겨 두면 좋겠다.
- **재동기화 입력(STEP 5.5로 넘김 — 권고 아님)**:
  1. `docs/05_설계서/order/INF/INF-ORD-003.md` frontmatter `anchors:` — `src/main/java/com/sm/lab/shop/service/OrderService.java:58-72`가 이제 시임 생성자 구간을 가리킨다. `list()`는 76-90으로 이동 → 줄 범위 갱신 필요.
  2. `docs/05_설계서/order/INF/INF-ORD-003.md` 요청 파라미터 표(`startDate`/`endDate`) — "오늘"의 출처가 주입된 `Clock`(운영 기본값 = 시스템 시계, 결과는 AS-IS 동일)이라는 TO-BE 사실이 본문에 없다.

### QA Gate — 2026-09-19 — PASS (round 2)
- **round1 권고 3건 반영 확인**: 3건 전부 실측 확인.
  1. **(반영)** 함정 경고 주석 3곳 — `OrderService.create()` 본문 첫 줄(`OrderService.java:189-193`, 주문번호 `LocalDate.now()` **바로 위**), `OrderListEndToEndIntegrationTest.FixedClockTestConfig` 위, `ApiKeyAuthIntegrationTest.FixedClockTestConfig` 위(60여 건 공유 컨텍스트라는 사실을 문구에 명시). 3개 모두 사람 코멘트가 요구한 요지("주문 생성은 아직 시스템 시계 — 생성 후 날짜 없이 목록 조회하면 고정창 밖으로 조용히 빠질 수 있음, 후속 SR에서 시계화")를 담았다. `create()`의 `LocalDate.now()` 자체는 diff상 **무변경**(scope_freeze 준수 — 코드가 아니라 문서화로 차단하라는 지시 그대로).
  2. **(반영)** 고정 시계 리터럴 통합 — 신규 `modules/shop-api/src/test/java/com/sm/lab/shop/support/TestClocks.java`의 `SEED_TODAY` 하나가 유일한 정의가 됐고, 3개 호출부가 전부 이 상수를 참조한다(`Clock.fixed(...)` 조립 리터럴은 코드베이스에 더 이상 없음 — grep 확인). 기존 `AdminApiKeyTestConfig`와 같은 `com.sm.lab.shop.support` 패키지에 둬 관례도 일치. `OrderServiceTest:224`에 남은 `LocalDate.of(2026,8,20)`은 중복이 아니라 **기대값**이다 — 상수를 바꾸면 이 단언이 큰 소리로 깨지는(조용히 어긋나지 않는) 올바른 방향.
  3. **(반영)** `harness/decisions.all.md` 맨 끝에 2026-09-19 항목 추가 — 정한 것(통합테스트 컨텍스트 override 필요 → 빈 주입 / 단위테스트로 충분 → 내부 생성+시임)·왜·**틀렸으면 어떻게 아나**(시임 8개 서비스 중 하나가 실 DB 고정시계 통합테스트 요구를 받으면 전환)·되돌리기까지 4요소 전부 기술. 지시한 형식 충족.
- **Layer1 스펙**: pass. round2는 로직 무변경(주석·테스트 상수·판단 기록만) — `list()`의 `LocalDate.now(clock)` 2줄, 역전 판정 2갈래 문구, `offset`, `selectOrders`/`countOrders` 호출, 리턴 `Map.of("totalCount","page","items")`가 round1 대비 diff 없음. TO-BE AC 3건·회귀 AC 7건 유지. 02_변경명세의 "제외" 7항목도 전부 미접촉(요청 파라미터·응답 스키마·오류 계약·30일 정책·시드 날짜·export 경로(null,null)·다른 서비스 시계).
- **Layer2 보안**: pass. 인증·인가 경로 무변경(`ApiKeyAuthFilter`·소유권 판정 미접촉), 새 오류 코드·응답 봉투 없음. round2 추가분은 주석·테스트 전용 상수(`src/test` 한정, 운영 클래스패스 밖)·문서뿐이라 공격면 증가 없음. `memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`의 IDOR 부정 단언이 실제 데이터 위에서 행사되는 상태(round1에서 강화된 지점) 유지.
- **Layer3 회귀**: pass. **QA 독립 재실행** `mvnw -o test` 전량 — surefire 72개 클래스 집계 **574 tests / 0 failures / 0 errors / 0 skipped, exit 0**(사례집 SR-307 #1대로 좁힌 클래스가 아니라 전체 스위트). `@Primary` 고정 시계를 얹은 `ApiKeyAuthIntegrationTest`의 다른 `/api/orders` 호출부(`adminApiKey_toApiEndpoint_returns200`, `staticMapKeys_stillWorkUnaffectedByDbFallback_regressionCheck`, 무키/오키 401 3건, `//api/orders` raw 경로)는 전부 상태코드만 단언하거나 인증 단계에서 끝나 고정창 변화에 영향받지 않음을 코드로 확인. 신설 `TestClocks`는 `src/test`에만 있어 운영 빌드 무영향. 규칙: `no-sysout`·`no-printstacktrace`·`no-select-star`·`ddl-idempotent`·`controller-has-test` 해당 변경 없음, `file-size-cap`은 `modules/shop-api/src/main/**` 한정이라 1019줄 테스트 파일은 대상 밖(`OrderService.java` 244줄, 상한 내).
- **라운드 규율**: round2에서 **새 medium 없음**. 이번 재작업이 만든 코드(`TestClocks` + 주석 3곳 + 판단 기록)에 기인하는 결함을 찾지 못했고, 이전 라운드에도 있던 코드에서 새 게이트를 세우지 않는다.
- 후속 TODO(게이트 아님, low):
  1. `OrderService.create()`의 시스템 시계는 여전히 잠재 함정이다 — 이번엔 지시대로 주석 3곳으로 차단했으나 근본 해소는 후속 SR(`create()` 시계화)에서. 주석이 지워지면 함정이 되살아나므로 후속 SR 등록 시 이 3개 주석을 참조점으로 쓴다.
- **재동기화 입력(STEP 5.5로 넘김 — 권고 아님, round1에서 이월)**:
  1. `docs/05_설계서/order/INF/INF-ORD-003.md` frontmatter `anchors:` — `src/main/java/com/sm/lab/shop/service/OrderService.java:58-72`가 현재 시임 생성자·주석 구간을 가리킨다(`list()`는 76-90으로 이동). 줄 범위 갱신 필요.
  2. `docs/05_설계서/order/INF/INF-ORD-003.md` 요청 파라미터 표(`startDate`/`endDate`) — "오늘"의 출처가 주입된 `Clock`(운영 기본값 = 시스템 시계, 결과는 AS-IS 동일)이라는 TO-BE 사실이 본문에 없다.

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/regression] OrderService가 절반만 시계화됨 — list()는 주입 clock을 쓰지만 같은 클래스 create()(OrderService.java:189)는 여전히 raw LocalDate.now()로 주문번호를 만든다. 두 고정시계 테스트 컨텍스트(OrderListEndToEndIntegrationTest, ApiKeyAuthIntegrationTest 67건)는 '오늘'이 2026-08-20으로 고정된 상태에서 주문 생성 시각만 실제 오늘이 되어, 앞으로 '주문 생성 후 날짜 없이 목록 조회' 테스트가 들어오면 ordered_at이 고정창(~2026-08-20) 밖이라 조용히 0건이 나오고 증상은 인가/필터 결함처럼 보인다. 현재 실패는 없음(잠재). → create() 시계 변경은 scope_freeze 밖이므로 코드 대신 문서화로 차단 — OrderService.create()에 '주문번호의 오늘은 SR-300 범위 밖, 시스템 시계 유지' 주석 1줄 + 두 테스트 클래스 javadoc에 '고정 시계는 list() 기본 조회창에만 적용된다(주문 생성 시각은 실제 시계)' 1줄 추가
2. [low/regression] 고정 시계 리터럴 LocalDateTime.of(2026,8,20,9,0)이 3곳(두 FixedClockTestConfig + OrderServiceTest)에 각각 독립 기술돼 한 곳만 바뀌면 증상이 갈린다. → 공용 테스트 상수 1곳으로 통합(후속 TODO 가능)
3. [low/spec] 코드베이스에 시계 패턴이 두 갈래가 됐다 — 다른 8개 서비스는 '내부 Clock.systemDefaultZone() + package-private 시임', OrderService만 '컨텍스트 빈 주입'. 승인된 계획의 의도된 결과지만 어느 쪽이 다음 기본인지 기록이 없다. → harness/decisions.all.md에 '통합테스트 override가 필요하면 빈 주입, 아니면 내부 생성+시임' 판단 기록 추가

사람 코멘트: QA CONCERNS 재작업 지시(라운드1): (1) create()는 범위 밖 — 코드는 바꾸지 말고 OrderService.java의 create() 내 LocalDate.now() 옆, 그리고 OrderListEndToEndIntegrationTest·ApiKeyAuthIntegrationTest의 @TestConfiguration(Clock override) 옆에 '주문 생성은 아직 시스템 시계 — 생성 후 날짜 없이 목록 조회하는 테스트는 고정창 밖으로 빠질 수 있음(후속 SR에서 시계화)' 주석 추가. (2) 고정 시계 리터럴(2026-08-20 등) 3곳(OrderServiceTest·OrderListEndToEndIntegrationTest·ApiKeyAuthIntegrationTest)을 테스트 공용 상수 하나(예: test 패키지 TestClocks.SEED_TODAY)로 통합해 중복 제거. (3) harness/decisions.all.md에 시계 주입 기준 5줄 기록: 스프링 빈 주입(@Autowired Clock)=통합 테스트에서 덮어써야 하는 곳, 내부 생성+시임 생성자(package-private (dao, clock))=단위 테스트만 필요한 곳, 반증 조건 포함. 재동기화 입력(INF-ORD-003 anchors 줄 번호·'오늘'이 주입 Clock 기준)은 STEP 5.5에서 반영 — 이번 재작업 범위 아님.

## test-agent 결과 (2026-09-19)

### AC 기반 TC 작성 및 매핑 확인

**작성 현황**: dev-agent가 이미 생성한 TC들이 AC를 완벽히 1:1 매핑하므로 추가 작성 불필요.

#### AC-1: '오늘'을 읽는 방법만 주입 가능한 Clock으로 바꾼다
- **매핑 TC**: `OrderServiceTest#list_noDateParams_usesInjectedClockNotSystemClock` (신규 단위)
  - Clock 고정 생성자로 OrderService 구성
  - ArgumentCaptor로 DAO에 넘어간 실제 LocalDate를 고정 기대값(2026-07-21/2026-08-20)과 정확히 비교
  - 시스템 시계와 무관한 고정값으로 검증 ✅

#### AC-2: 운영 기본값은 시스템 시계 — 파라미터·기본값·응답 스키마 변경 없음
- **매핑 TC**: 
  - `OrderServiceTest#list_responseSchema_unchangedTotalCountPageItems` — totalCount/page/items 스키마 미변경 확인
  - `OrderServiceTest#list_periodFilterCombinesWithOrderStateAsAnd` — 기간 필터가 orderState와 AND로 결합 유지
  - `OrderServiceTest#list_explicitDateParams_passThroughUnchanged` — 명시 파라미터는 보정 없이 통과
  - 회귀 테스트: `list_noDateParams_defaultsToLast30DaysInclusive` (시스템 LocalDate.now() 상대값과 비교) ✅

#### AC-3: 테스트는 고정 Clock으로 시드 주문일(2026-08-15~17) 기준 창을 재현해 날짜와 무관하게 통과
- **매핑 TC**: 
  - `OrderListEndToEndIntegrationTest#orderList_realDbEndToEnd_rendersLatestDeliveryStateColumnForEachOrder` (통합, 고정 2026-08-20)
    - 시드 주문: 20260816-0002(배송상태 READY) 직접 단언
  - `OrderListEndToEndIntegrationTest#orderList_realDbEndToEnd_showsDashWhenOrderHasNoDeliveryHistory` (통합, 고정 2026-08-20)
    - 시드 주문: 20260817-0001(배송 이력 없음) 직접 단언
  - `ApiKeyAuthIntegrationTest#memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders` (회귀, 고정 2026-08-20)
    - 소유권 판정(타 회원 미노출) + 기본 조회창(고정 시계) 동시 검증 ✅

### 회귀 테스트 실행 결과

**대상 3건** (SR-300에서 지정한 회귀 TC):
1. `OrderListEndToEndIntegrationTest#orderList_realDbEndToEnd_rendersLatestDeliveryStateColumnForEachOrder` — **PASS**
2. `OrderListEndToEndIntegrationTest#orderList_realDbEndToEnd_showsDashWhenOrderHasNoDeliveryHistory` — **PASS**
3. `ApiKeyAuthIntegrationTest#memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders` — **PASS**

회귀 AC(INF-ORD-003의 "바뀌면 안 되는 것" 7건) 검증:
- del_yn='N' 상시필터(논리삭제 주문 제외) — PASS
- memberId/orderState 파라미터 필터 — PASS
- offset 계산(페이징) — PASS
- ORDERS/MEMBERS 조인·memberName 반환 — PASS
- 목록 응답의 items/deliveries 미포함 — PASS
- 기간 필터 AND 결합 — PASS
- ordered_at(DATETIME) 기준 필터 — PASS

### 전체 스위트 실행

```
[2026-09-19 16:08:23] shop-api: 574 tests
  - Failures: 0
  - Errors: 0
  - Skipped: 0
  - Build: SUCCESS

[2026-09-19 16:08:23] shop-web: 203 tests
  - Failures: 0
  - Errors: 0
  - Skipped: 0
```

**통과율**: 100% (777/777 통과, exit 0)

### 최종 판정

✅ **납품 가능**
- AC 3건 모두 1:1 매핑된 TC로 검증됨
- 회귀 AC 7건 + 회귀 TC 3건 전부 PASS
- 전체 스위트(shop-api 574 + shop-web 203) 100% 통과
- 규칙 위반 없음(no-sysout, no-printstacktrace, no-select-star, ddl-idempotent, controller-has-test, file-size-cap 모두 준수)
- 변경 범위: OrderService.list()의 LocalDate.now(clock) 2줄 + 생성자 4벌 + Clock 빈 추가 — 계획 대로 이탈 없음

### TC 저장 위치

- 수용 기준 TC: `docs/07_테스트케이스/TC_v1.0.md`(dev-agent 작성, test-agent 재확인)
- 테스트 결과 보고서: `docs/08_테스트결과보고서/TR_v1.0.md`(별도 생성, 아래 참조)
- 테스트 실행 로그: `docs/08_테스트결과보고서/test_run_2026-09-19.json`
