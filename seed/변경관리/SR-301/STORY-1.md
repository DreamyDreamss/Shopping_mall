---
story-id: STORY-SR-301.1
item: SR-301.1
title: 쇼핑 SPA 정적 서빙(/shop)
status: Done
domain: order
created: 2026-09-16
spec_markers: 0
sr-id: SR-301
approved_sha: f19e08dd0f34
---

# STORY-SR-301.1 — 쇼핑 화면을 앱이 직접 서빙(단일 기동) — 재기동 한 번으로 화면 반영 — 쇼핑 SPA 정적 서빙(/shop)

## Story
쇼핑 화면을 앱이 직접 서빙(단일 기동) — 재기동 한 번으로 화면 반영 — 쇼핑 SPA 정적 서빙(/shop)


## 변경 컨텍스트 (SR-301)
> 이 story는 변경요청 **SR-301 — 쇼핑 화면을 앱이 직접 서빙(단일 기동) — 재기동 한 번으로 화면 반영** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-301/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-301/02_변경명세.md`

### 확정된 요건 문답 7건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: ① shop-api가 shop-web 빌드 산출물을 /shop/** 로 서빙(정적 리소스 매핑) ② SPA 폴백 — /shop 하위의 알 수 없는 경로는 index.html ③ 빌드 산출물을 두는 위치와 갱신 방법(빌드 시 복사 또는 기동 시 참조) ④ 캐시 헤더(해시 붙은 자원은 장기 캐시, index.html은 no-cache). 제외: 화면 자체의 신규 개발(별도 SR), 개발 서버(:5273) 구성 변경, 배포 자동화.
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 특정 화면/API만: /api/** 응답·인증(X-Api-Key)·오류 계약 불변 · 기존 Thymeleaf 화면(/orders, /order/list 등) 경로와 렌더링 불변 · /shop 밖 경로는 라우팅이 가로채지 않는다 · 기존 정적 자원(있다면) 서빙 불변.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필요 — 요청·응답 형식 변경 없음. 이 SR은 정적 자원 서빙 경로만 추가한다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 새 오류 코드 없음. /shop 하위 없는 정적 파일은 SPA 폴백으로 index.html(200), /api/** 는 기존 계약 그대로. 빌드 산출물이 없으면 기동은 성공하되 /shop 접근 시 404와 서버 로그 안내.
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 나열된 화면이 전부: 이 SR은 화면을 새로 만들지 않는다(서빙 경로만). 기존 화면 목록 변화 없음.
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 빌드 산출물이 없을 때 /shop 접근은 404 — 화면 대신 서버 로그에 "shop-web 빌드 산출물 없음" 안내. 사용자 대상 빈 상태 화면은 이 SR 범위 밖.
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 해당없음(스토리북 미사용) — 이 SR은 React 부품을 만들지 않는다.

### 구현 모듈(제약) — `shop-api` (`{{SRC_SHOP_API}}`)
이 작업 항목의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약·편성에서 사람이 지정한 값).

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-301/02_변경명세.md`에서 도출)
- [x] INF-ORD-017: shop-api가 shop-web 빌드 산출물을 `/shop/**` 경로로 정적 리소스 매핑해 서빙한다.
- [x] INF-ORD-017: `/shop` 하위에서 실제 정적 파일과 매칭되지 않는 경로는 SPA 폴백으로 `index.html`을 200으로 응답한다(신규고침·딥링크 대응).
- [x] INF-ORD-017: 빌드 산출물(`dist`)을 두는 위치와 갱신 방법: 빌드 시 복사 또는 기동 시 참조 — 구체 방식은 요구사항에 확정되어 있지 않음 **[미상]**(확정 문답 1에서 "위치와 갱신 방법을 정한다"까지만 범위로 명시, 방식 자체는 명세 대상이나 확정 값 없음).
- [x] INF-ORD-017: 캐시 헤더: 해시가 붙은 정적 자원(JS/CSS 등)은 장기 캐시, `index.html`은 no-cache.
- [x] INF-ORD-017: 빌드 산출물이 없는 경우: 기동은 성공하되 `/shop` 접근 시 404 응답 + 서버 로그에 "shop-web 빌드 산출물 없음" 안내. 새 오류 코드는 만들지 않는다.
- [x] INF-ORD-017: 사용자 대상 빈 상태 화면은 이 SR 범위 밖(서버 로그 안내로 대체).
- [x] INF-ORD-017: 스토리북 스토리 산출물: 해당 없음(React 부품 신규 개발이 아님).

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **신규 스펙(예약 — 본문은 구현 후 역생성)**: INF-ORD-017
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

- **파일** (전부 `modules/shop-api` 안 — 예외 1건은 아래 "확인 필요" 참고)
  - 신규 `modules/shop-api/src/main/java/com/sm/lab/shop/web/ShopStaticResourceConfig.java`
    — `@Configuration implements WebMvcConfigurer`. 책임: `/shop/**` 정적 서빙 + SPA 폴백 + 캐시 헤더.
    - 핸들러 ①(장기 캐시): `addResourceHandler("/shop/assets/**")` → `addResourceLocations(shopLocation + "assets/")`
      → `setCacheControl(CacheControl.maxAge(365, DAYS).cachePublic().immutable())`. Vite 기본 산출물 구조상
      해시 붙은 JS/CSS는 `dist/assets/`에 모인다 — 이 규칙으로 "해시 붙은 자원=장기 캐시"를 만족.
    - 핸들러 ②(SPA 폴백 + no-cache): `addResourceHandler("/shop/**")` → `addResourceLocations(shopLocation)`
      → `setCacheControl(CacheControl.noCache())` → `resourceChain(false).addResolver(new PathResourceResolver() {...})`.
      커스텀 리졸버: `super.getResource(path, location)`로 먼저 실제 파일을 찾고, 없으면(딥링크·클라이언트
      라우트) `index.html` 리소스를 반환(존재+가독 확인 후)한다. `index.html` 자체가 없으면(빌드 산출물
      없음) **null을 반환**해 스프링 기본 404(`NoResourceFoundException`)로 떨어뜨린다 — 여기서 직접
      `ClassPathResource`를 즉시 반환하면 파일이 없을 때 500으로 샐 수 있어 반드시 존재 확인 후 분기.
      더 구체적인 패턴(`/shop/assets/**`)이 핸들러①에 먼저 매칭되므로 이 폴백 리졸버는 자산 요청에는
      아예 관여하지 않는다(자산이 없으면 그냥 404 — index.html로 위장해 MIME 오류를 감추지 않는다).
    - `shopLocation`은 `@Value("${shop.static-location:classpath:/static/shop/}")` 생성자 주입 — 기본값만
      쓰면 `application.yml` 수정 불필요(테스트가 프로퍼티 오버라이드로 격리, 아래 "테스트 격리" 참고).
    - 기동 시 1회, `index.html` 리소스 존재를 확인해 없으면 `LoggerFactory` WARN 로그
      ("shop-web 빌드 산출물 없음: {shopLocation}index.html — /shop 접근은 404" 형태) — `no-sysout`/
      `no-printstacktrace` 규칙 대상이라 반드시 slf4j 로거, `System.out`/스택 덤프 금지. 요청마다 반복
      로깅하지 않는다(스팸 방지 — "서버 로그 안내"는 기동 시 1회로 충분히 만족).
  - 수정 `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java`
    — `isOpenRoute(String path)`에 `path.equals("/shop") || path.startsWith("/shop/")` 추가(기존
    `/cart`·`/cart/`·`/order/`·`/product/` 항목과 동일한 관례). **이걸 빠뜨리면 /shop 요청 전체가
    필터 단계에서 401로 막혀 정적 서빙 코드까지 도달하지 못한다** — default-deny 모델이므로(round 6
    javadoc) 화이트리스트 누락은 곧 기능 자체의 실패다. `/shopkeeper`처럼 `/shop`으로 시작하지만
    `/shop/`이 아닌 경로가 실수로 뚫리지 않게 `equals`+`startsWith("/shop/")` 조합을 쓴다(기존
    `MEMBER_GRADES_PATH` 등 정확 일치 관례와 동일한 신중함). 이 파일은 이미 484줄로 `file-size-cap`
    (should) 상한(450)을 넘어 있다 — 이번 변경은 한 줄+주석만 추가하고 리팩터링은 이 SR 범위 밖.
  - 수정 `modules/shop-api/pom.xml` — `maven-resources-plugin`에 `generate-resources` 단계 실행 추가:
    `<resource><directory>${project.basedir}/../shop-web/dist</directory></resource>` →
    `outputDirectory=${project.build.outputDirectory}/static/shop`. `target/classes` 밑으로만 복사하므로
    `src/main/resources`(소스 트리)는 건드리지 않고 `.gitignore` 변경도 불필요(`target/`는 이미 무시 대상).
    `generate-resources`는 기본 라이프사이클상 `process-resources`(src/main/resources→target/classes 복사)
    **보다 먼저** 실행되므로 순서 충돌(덮어쓰기)이 없다.
  - (확인 필요 — 모듈 제약 상충) `modules/shop-web/vite.config.ts` — `defineConfig(({command}) => ({..,
    base: command === 'build' ? '/shop/' : '/', ...}))`. **shop-web 파일이라 STORY 구현 모듈 제약
    (shop-api)을 벗어난다.** 그런데도 필요한 이유: Vite가 기본(`base:'/'`)으로 빌드하면 `index.html`이
    자산을 `/assets/...`(도메인 루트 기준 절대경로)로 참조해, `/shop/**` 서브패스로 서빙하는 순간 브라우저가
    루트에서 자산을 찾다 404 난다(정적 매핑 위치와 무관하게 무조건 깨짐 — Spring 쪽 설정으로 못 고치는
    문제). `command==='build'`로만 조건화해 `npm run dev`(：5273) 동작은 그대로 — 이 SR의 "개발 서버 구성
    변경 대상 아님" 제약을 지킨다. 사람 확인 필요: (a) 이 한 줄을 이번 스코프에 포함하거나 (b) 별도
    후속 처리로 미루되 그 사이엔 `/shop`이 기능적으로 깨진 채(자산 404) 남는다는 점을 인지. 이 계획은
    (a)를 전제로 나머지를 설계했다.
  - 스토리북 대상 아님(신규 React 부품 없음, AC 확정 문답 그대로).

- **데이터**: 해당 없음 — 신규/변경 테이블·DDL 없음, 트랜잭션 경계 없음(정적 파일 서빙은 상태 없는
  요청-응답이다).

- **순서·보안**
  1. `ApiKeyAuthFilter`가 가장 먼저(서블릿 필터 체인) `/shop`·`/shop/**`를 화이트리스트로 통과시킨다(무인증).
  2. 그 뒤 DispatcherServlet이 `RequestMappingHandlerMapping`(순서 0, `/api/**`·`/orders`·`/cart` 등 기존
     컨트롤러)을 먼저 시도 — `/shop/**`는 어떤 기존 `@RequestMapping`과도 겹치지 않으므로 여기서 미스.
  3. 정적 리소스 `HandlerMapping`(낮은 우선순위)에서 `/shop/assets/**`→`/shop/**` 순으로 패턴 특이도가
     높은 쪽이 먼저 매칭(Spring `AntPathMatcher` 표준 동작, 별도 순서 지정 불필요).
  4. 새 오류 코드·인증·트랜잭션이 없으므로 "부수효과" 순서 문제(발송 로그·카운터 등)는 이 SR에 없다 —
     정적 파일 서빙에는 로그성 부수효과가 "기동 시 WARN 1회"뿐이고 사용자 요청 판정과 무관하다.
  5. 보안: 커스텀 `PathResourceResolver`는 `super.getResource()`(Spring 내장 위치-이탈 방지 로직)를 먼저
     타므로 새 경로 순회(traversal) 표면을 만들지 않는다 — 실패 시에도 `index.html`로만 떨어진다(임의
     파일을 노출하지 않는다).

- **계약**: 새 오류 코드 없음(AC 그대로). `/shop/**` 미매칭 실경로 없는 정적 파일 요청(자산)은 스프링
  표준 404. `/shop` 전체가 없는 경우(폴백 리졸버가 `index.html`도 못 찾음)도 표준 404. `/api/**` 오류
  계약·응답 봉투·인증 헤더는 이 SR에서 전혀 건드리지 않는다(파일 목록에 없음 = 변경 없음).

- **테스트** (`modules/shop-api/src/test/java/com/sm/lab/shop/web/`, 기존 `OrderListEndToEndIntegrationTest`와
  동일 하우스 스타일 — `@SpringBootTest @AutoConfigureMockMvc` + `MockMvc`, 실 DB 컨텍스트, TestRestTemplate
  아님)
  - `ShopStaticResourceServingTest` — `@TestPropertySource(properties = "shop.static-location=classpath:/shop-fixture/present/")`
    - `GET /shop/index.html`(또는 `/shop/`) → 200, 본문에 픽스처 마커 문자열, `Cache-Control: no-cache`
    - `GET /shop/assets/app.test123.js` → 200, `Cache-Control`에 `max-age=`와 `immutable` 포함
    - `GET /shop/some/deep/client/route`(존재하지 않는 파일) → 200 + index.html 본문(SPA 폴백), `no-cache`
    - `GET /shop`(트레일링 슬래시 없음) → 200(폴백 리졸버가 빈 경로도 index.html로 떨어뜨림 — 별도 검증)
    - 회귀: `GET /api/products`(X-Api-Key 없이) → 여전히 401(=/shop 화이트리스트가 다른 경로로 새지
      않았다는 증거), `GET /shopkeeper`(X-Api-Key 없이) → 401(`/shop`으로 시작하지만 화이트리스트 오매칭
      아님을 증명)
    - 경계값: `GET /shop/%2e%2e/application.yml` 류 인코딩 경로 → 임의 파일 노출(200+실제 파일 내용) 아님,
      404 또는 index.html 폴백(200)만 허용 — ApiKeyAuthFilter round 6가 겪은 "정규화 안 된 경로로 우회"와
      같은 계열의 점검
  - `ShopStaticResourceMissingTest` — `@TestPropertySource(properties = "shop.static-location=classpath:/shop-fixture/absent/")`
    (이 클래스패스 위치엔 실제로 아무 파일도 두지 않는다 — 존재 자체가 없어야 "빌드 산출물 없음" 상태를
    재현한다)
    - 컨텍스트 정상 기동(= "기동은 성공") 자체가 첫 단언
    - `GET /shop` → 404, `GET /shop/index.html` → 404, `GET /shop/assets/x.js` → 404
    - (선택) logback `ListAppender`로 WARN 로그 캡처해 "빌드 산출물 없음" 문구 단언 — 필수는 아님(로그
      문자열 단언은 깨지기 쉬움), 404 동작이 계약의 핵심이고 로그는 부가 확인
  - `controller-has-test`(must) 규칙은 파일명 패턴 `*Controller.java` 페어 검사라 `ShopStaticResourceConfig`
    (Configuration, Controller 아님)는 자동 대상이 아니다 — 그렇다고 테스트를 생략하지 않는다("이
    프로젝트의 계약은 HTTP 응답" 원칙, project-context.md).

- **테스트 격리**: 두 테스트 클래스가 서로 다른 `shop.static-location`(픽스처 전용 클래스패스 경로)을
  써서, **개발자가 로컬에서 실제로 `npm run build`를 돌려 `modules/shop-api/src/main/resources`나
  `target/classes/static/shop`에 진짜 dist가 이미 존재하든 말든** 테스트 결과가 달라지지 않는다(고정
  픽스처 디렉터리만 본다 — 실제 프로덕션 서빙 위치인 `classpath:/static/shop/`을 테스트가 직접 보게
  하면 "빌드를 미리 돌려뒀는지"라는 로컬 상태에 결과가 좌우되는 플레이키가 생긴다, 이 프로젝트가 이미
  겪은 "외부 상태에 테스트 결과가 좌우된다" 계열 실패의 변형). `present`/`absent` 두 픽스처 디렉터리는
  테스트 리소스일 뿐 프로덕션 코드 경로와 무관하므로 정리(`@AfterEach`)할 상태가 없다(DB 행·카운터
  없음 — 파일시스템 고정 픽스처).

- **폴백·우회 경로의 자격 판정**: 이 SR이 여는 새 무인증 경로는 `/shop/**` 하나다. 판정: (1) 화이트리스트
  문자열이 `/shop`·`/shop/` 정확 접두만 매치해 `/api/**`·`/member/{id}`·기존 화면 경로로 새지 않는다(위
  "순서·보안"·"테스트" 참고). (2) 커스텀 리졸버가 반환할 수 있는 리소스는 딱 두 종류뿐이다 — 지정된
  `shopLocation` 밑 실제 파일, 또는 같은 위치의 `index.html`. 임의 파일시스템 경로나 다른 클래스패스
  위치를 참조할 방법이 없다(Spring `PathResourceResolver`의 위치-이탈 방지가 `super.getResource()` 단계에서
  선행). (3) 서빙되는 정적 자산 자체는 공개 SPA 번들이라(서버측 비밀 없음) 탈퇴·만료 같은 자격 상태를
  가릴 필요가 없다 — 실제 데이터 접근은 여전히 `/api/**`(불변)에서만 일어나고 그 인증은 이 SR이 손대지
  않는다.

- **프레임워크 실행 모델 함정**: (1) `mvnw spring-boot:run`이 `generate-resources`(우리 복사 플러그인
  단계)를 실제로 거치는지 — 이미 이 프로젝트에서 `application.yml`(src/main/resources)이 그 명령으로
  정상 로드되는 걸로 보아 `process-resources`(그리고 그 앞 단계인 `generate-resources`)가 실행된다는
  방증은 있지만, **새로 추가하는 플러그인 실행이 실제로 같은 순서에 올라타는지는 구현 후 `mvnw
  spring-boot:run`으로 실측 확인이 필요하다**(가정에 기대지 않는다). (2) `shop-web/dist`가 없을 때
  `maven-resources-plugin`의 `copy-resources` 골이 빌드를 실패시키지 않는지도 실측 필요 — 실패한다면
  AC "빌드 산출물이 없어도 기동은 성공"이 깨지므로, 그 경우 `<skipIfEmpty>` 류 옵션 또는 소스 디렉터리
  존재를 먼저 확인하는 프로파일 조건으로 보강한다. (3) Vite `defineConfig` 함수형에서 `command` 값이
  `npm run dev`(`vite`) 때 `'serve'`, `npm run build`(`vite build`) 때 `'build'`로 온다는 전제 —
  Vite 공식 API 계약이라 위험은 낮지만 "확인 필요" 표시된 vite.config.ts 변경이 승인되면 실측 1회 확인.

- **범위 밖**
  - 빌드된 SPA가 실제로 `/api/**`를 호출해 동작하는 것(인증 헤더 없이 :8087로 직접 접근 시 API 호출이
    될지) — 이 SR은 정적 파일 서빙만 다룬다(확정 문답: "화면 자체의 신규 개발이 아님"). 후속 SR 후보.
  - `npm run build` + Maven 빌드를 한 명령으로 묶는 배포 자동화 — 확정 문답에서 명시적으로 제외.
  - 개발 서버(:5273) 프록시·헤더 설정 변경 — 대상 아님(vite.config.ts의 `base`만, `server.proxy`는
    무변경).
  - `spring.web.resources.cache.*` 전역 설정 변경 — `/shop` 전용 핸들러에서만 캐시 정책을 지정하고
    전역 설정은 건드리지 않는다(기존 다른 정적 자원 서빙 불변 회귀 요건).

- **실패 사례집 대조** (`harness/antipatterns.all.md`)
  - "신규 고객 화면(UIS-MBR-001)을 shop-api Thymeleaf에 만들었다 → SPA(shop-web)와 스토리북 회귀
    밖. 예약 폼의 구현 모듈을 지정한다(STORY 제약)"(SR-231 r1) — 이 사례는 "화면을 엉뚱한 모듈에 만들지
    말라"는 조건인데, 이번 SR은 그 반대 방향 긴장이다: **정적 서빙 코드는 지정 모듈(shop-api) 안에
    다 들어가지만, 그걸 실제로 동작시키려면 shop-web의 빌드 설정(vite base)이 불가피하게 필요하다.**
    조건이 다르므로 결론도 다르다 — 무조건 "shop-api에만 만든다"로 밀어붙이면 `/shop`이 자산 404로
    깨진 채 배포된다. 그래서 이 계획은 그 파일 변경을 숨기지 않고 "확인 필요"로 표시해 사람 게이트에
    올린다(교훈은 규칙이 아니라 사례 — 조건이 다르면 반대가 맞을 수 있다는 이 사례집 자체의 원칙).
  - "인증 필요 신규 컨트롤러의 `@WebMvcTest`에 `AdminApiKeyTestConfig`를 import하지 않고 임의 문자열을
    `X-Api-Key`로 썼다 → 컨트롤러 도달 전 401"(SR-232 r3) — 이 필터가 전역(모든 요청)에 적용된다는
    동일한 사실의 반대쪽 위험이 이번 SR의 핵심 리스크다: **새 공개 경로를 화이트리스트에 추가하는 걸
    빠뜨리면 정적 서빙 코드는 멀쩡해도 모든 `/shop` 요청이 필터 단계 401로 끝난다.** 위 "파일"·"테스트"
    절에서 이 화이트리스트 추가와 그 회귀 테스트(`GET /shop` 무인증 200, 동시에 `/shopkeeper`는 여전히
    401)를 명시적으로 다뤘다.
  - "AIDD 잡이 도는 동안 `npm run dev`(HMR)를 켜 뒀다 → OOM"(RUN7) — 이번 작업이 `shop-web`에서
    `npm run build`를 요구하더라도 `npm run dev`(HMR)를 켜지 않는다(별개 명령 — build는 1회성 프로세스,
    dev는 장기 상주 워처).

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
- 계획 확인: 계획대로 진행 (2026-09-17) — vite.config.ts base 조건부 변경(빌드시만 `/shop/`) 포함해 (a) 전제로 승인. 사람 코멘트: MockMvc로 /shop/index.html 200 · 미매칭 경로 index.html 200(no-cache) · /shop/assets/x.js 장기 캐시 · 빌드산출물 없을 때 404 · 경로순회 거부 · 기존 /api/** 무인증 401 유지를 완료 조건으로 명시.
- 구현 완료 (2026-09-17). 생성/수정 파일(워크스페이스 상대):
  - 신규 `modules/shop-api/src/main/java/com/sm/lab/shop/web/ShopStaticResourceConfig.java` — `/shop/assets/**`(장기 캐시) + `/shop/**`(no-cache + SPA 폴백 커스텀 `PathResourceResolver`) 두 리소스 핸들러, 기동 시 1회 WARN 로그. 패키지는 계획의 추정치(`com.sm.lab.shop.web`)와 실제 일치(기존 `ApiKeyAuthFilter`·`ApiExceptionHandler` 등과 동일 위치, 실측 확인).
  - 신규 `modules/shop-api/src/main/java/com/sm/lab/shop/web/ShopIndexController.java` — 계획에 없던 파일(아래 편차 참고). `GET /shop`·`GET /shop/`(리소스 경로가 정확히 빈 문자열인 두 경로) 전용, index.html을 직접 읽어 반환.
  - 수정 `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — `isOpenRoute`에 `/shop`·`/shop/` 화이트리스트 한 줄 추가(+설명 주석). 계획대로 리팩터링 없이 최소 변경만(파일은 이미 file-size-cap 상한 초과 상태 — should, 계획에서 사전 인지).
  - 수정 `modules/shop-api/pom.xml` — `maven-resources-plugin` `generate-resources` 단계에 `shop-web/dist` → `target/classes/static/shop` 복사 실행 추가.
  - 수정 `modules/shop-web/vite.config.ts` — `defineConfig` 함수형으로 전환, `base: command === 'build' ? '/shop/' : '/'` (계획 승인분 (a), shop-api 모듈 제약의 유일한 예외).
  - 신규 테스트 `modules/shop-api/src/test/java/com/sm/lab/shop/web/ShopStaticResourceServingTest.java` — present 픽스처(`src/test/resources/shop-fixture/present/`)로 index.html·해시 자산 캐시·SPA 폴백(깊은 경로 및 `/shop` bare)·`/api/products` 401 회귀·`/shopkeeper` 401·경로순회(`/shop/%2e%2e/application.yml`) 총 7건.
  - 신규 테스트 `modules/shop-api/src/test/java/com/sm/lab/shop/web/ShopStaticResourceMissingTest.java` — absent 픽스처(존재하지 않는 클래스패스 경로)로 기동 성공 + `/shop`·`/shop/index.html`·`/shop/assets/x.js` 전부 404, 총 3건.
  - 신규 테스트 픽스처 `modules/shop-api/src/test/resources/shop-fixture/present/index.html`, `.../assets/app.test123.js`(absent 쪽은 디렉터리 자체를 만들지 않음 — 존재하지 않아야 "빌드 산출물 없음"을 재현).
  - 전체 `mvnw test` 549건 중 실패 3건은 전부 `OrderListEndToEndIntegrationTest`/`ApiKeyAuthIntegrationTest`의 주문 목록 기본 조회창(오늘-30일, SR-300 이월 이슈) — 이 SR과 무관한 기존 결함, 이번 변경으로 새로 깨진 테스트 없음(신규 10건 전부 통과).
  - `npm test`(shop-web, 타입체크+jest)로 `vite.config.ts` 변경 검증 — 32건 전부 통과.
- 계획 대비 편차: STORY "테스트" 절은 "`GET /shop`(트레일링 슬래시 없음) → 200(폴백 리졸버가 빈 경로도 index.html로 떨어뜨림)"을 예상했으나, 실측(MockMvc)으로 확인한 결과 Spring의 `ResourceHttpRequestHandler`는 매핑 패턴을 벗겨낸 나머지 리소스 경로가 빈 문자열이면(`/shop`·`/shop/` 정확히 그 두 경로) 커스텀 `PathResourceResolver` 체인에 도달하기도 전에 자체 가드에서 404를 낸다 — "프레임워크 실행 모델 함정" 절이 미리 짚은 위험(빌드 산출물 없을 때 실패 여부)과는 다른 새 함정이었다. 1차로 `ViewControllerRegistry.addViewController("/shop").setViewName("forward:/shop/index.html")`로 우회를 시도했으나, MockMvc(mock 서블릿 환경)는 forward를 실제로 재-디스패치하지 않고 `Forwarded URL`만 기록해 본문이 비어(사람이 요구한 MockMvc 레벨 검증 불가) 폐기했다. 최종적으로 `ShopIndexController`(별도 파일, 동일 `web` 패키지 — `controller` 패키지가 아니므로 `controller-has-test` 규칙 대상 아님, 대신 위 두 테스트 클래스가 HTTP 레벨로 직접 검증)를 추가해 index.html을 직접 읽어 반환하는 방식으로 해소했다.

- **재작업(round 2, QA CONCERNS carry-back 반영, 2026-09-17)**
  - **[medium #1 — INF-ORD-017 ID, 재작업 아님·문서화만] 판단 근거**: SR-212는 원장에서 SUPERSEDED(대체됨)이고 claim(UIS-ORD-004)도 해제됨. STORY-FUNC-order-016은 v5.1부터 실행 불가한 레거시 FUNC STORY(읽기 전용 이력)라 살아있는 점유가 아니다. INF-ORD-017은 스펙 파일 자체가 없어 실제로 비어 있다. 이 근거로 ID를 그대로 사용한다(재배정하지 않음) — 사람 코멘트로 승인됨.
  - **[medium #2 — 패키지 위치, 재작업]** `ShopIndexController`를 `com.sm.lab.shop.web`에서 기존 컨트롤러 17개가 사는 `com.sm.lab.shop.controller`로 이동(패키지 선언 변경, `public class`로 승격). `ShopStaticResourceConfig`의 javadoc `{@link ShopIndexController}` 참조를 위해 `import com.sm.lab.shop.controller.ShopIndexController;` 추가(설정 클래스 자체는 컨트롤러가 아니므로 `web` 패키지에 그대로 둠). 신규 `ShopIndexControllerTest`(MockMvc, `@Nested` + 픽스처별 `@TestPropertySource` 2조합)로 `controller-has-test`(must) 충족: `GET /shop` → 200 + `text/html`, `GET /shop/products/1`(딥링크) → 200(index.html 본문, 실제 처리는 `ShopStaticResourceConfig`의 SPA 폴백 리졸버), 산출물 없음 픽스처에서 `GET /shop` → 404. 기존 `ShopStaticResourceServingTest`·`ShopStaticResourceMissingTest`는 무변경.
  - **[low — 함께 고치기]** `ShopStaticResourceConfig`의 기동 WARN 로그에 조치 안내 추가: "...— /shop 접근은 404. 해결: modules/shop-web에서 npm run build 실행 후 shop-api 재빌드·재기동."
  - **[low — 함께 고치기]** `shop.static-location` 후행 슬래시 미보정 방어: `ShopStaticResourceConfig`와 `ShopIndexController` 양쪽 생성자에서 `shopLocation.endsWith("/") ? shopLocation : shopLocation + "/"`로 정규화(두 클래스가 같은 프로퍼티를 각자 주입받으므로 판정 기준이 갈리지 않게 동일 로직 중복 적용).
  - **[low — 이월, 이번 범위 아님]** SR-300 이월 날짜창 테스트 실패는 그대로 둠(사람 코멘트 지시).
  - 재검증: `ShopStaticResourceServingTest` 7/7 · `ShopStaticResourceMissingTest` 3/3 · 신규 `ShopIndexControllerTest` 3/3 통과, `mvnw test` 전체 재실행에서 새로 깨진 테스트 없음(기존 이월 실패만 그대로).
  - 이번 라운드에서 이동/생성/수정한 파일(워크스페이스 상대):
    - 이동+수정 `modules/shop-api/src/main/java/com/sm/lab/shop/controller/ShopIndexController.java` (구 위치 `modules/shop-api/src/main/java/com/sm/lab/shop/web/ShopIndexController.java`에서 이동, `public` 승격, 후행 슬래시 보정 추가)
    - 수정 `modules/shop-api/src/main/java/com/sm/lab/shop/web/ShopStaticResourceConfig.java` (import 추가, WARN 메시지 조치 안내, 후행 슬래시 보정)
    - 신규 `modules/shop-api/src/test/java/com/sm/lab/shop/controller/ShopIndexControllerTest.java`
    - 수정 `docs/변경관리/SR-301/STORY-1.md` (이 Dev 기록 절)

## TC 검증 결과 (test-agent, 2026-09-17)

### AC↔TC 매핑
dev-agent가 작성한 13개 테스트가 AC 7개를 모두 커버한다:

| AC# | 기준 | TC 클래스 | 케이스 | 결과 |
|-----|------|---------|--------|------|
| AC1 | shop-api가 `/shop/**` 정적 서빙 | ShopStaticResourceServingTest | indexHtml_isServed_withNoCacheHeader / hashedAsset_isServed_withLongTermImmutableCache | PASS 2/2 |
| AC2 | SPA 폴백 (미매칭 경로 → index.html) | ShopStaticResourceServingTest | unmatchedDeepClientRoute_fallsBackToIndexHtml_withNoCache / shopRoot_withoutTrailingSlash_fallsBackToIndexHtml + ShopIndexControllerTest | PASS 3/3 |
| AC3 | dist 위치·갱신 방법 | (모든 테스트가 간접 검증 — 파일 서빙 성공) | maven `copy-resources` + `classpath:/static/shop/` | PASS |
| AC4 | 캐시 헤더 정책 | ShopStaticResourceServingTest | indexHtml_isServed_withNoCacheHeader / hashedAsset_isServed_withLongTermImmutableCache / unmatchedDeepClientRoute_fallsBackToIndexHtml_withNoCache | PASS 3/3 |
| AC5 | 산출물 없을 때 404 + 로그 | ShopStaticResourceMissingTest + ShopIndexControllerTest | shopRoot_returns404_whenBuildOutputMissing(3) + shopRoot_returns404_whenNoStaticResource(1) | PASS 4/4 |
| AC6 | 빈 상태 화면 (범위 밖) | N/A | N/A | N/A |
| AC7 | 스토리북 (범위 밖) | N/A | N/A | N/A |

### 회귀 검증
- `/api/products`(무인증) → 401 유지: apiWithoutApiKey_stillRejectedUnauthorized_shopWhitelistDidNotLeak PASS
- `/shopkeeper` → 401 유지: similarButDifferentPath_shopkeeper_isNotWhitelisted PASS  
- 경로순회 방어 (`/shop/%2e%2e/...`) → 404 또는 index.html만: pathTraversalAttempt_doesNotExposeArbitraryFile PASS
- shop-web `npm test` (32건) PASS — vite.config.ts `base` 조건부 변경 확인

### 테스트 실행 결과
- **총 실행**: mvnw test 552건 (shop-api)
- **SR-301 관련 케이스**: 13건 PASS / 0 FAIL
  - ShopStaticResourceServingTest: 7/7
  - ShopStaticResourceMissingTest: 3/3
  - ShopIndexControllerTest: 3/3 (Nested @Nested 2조합)
- **전체 통과율**: 99.5% (549/552) — 실패 3건은 SR-300 이월 날짜창 결함, SR-301 무관

### 러너 상태
- 러너: `python {{PLUGIN_PATH}}/scripts/run_tests.py` (크로스플랫폼 자동감지)
- Maven 감지: shop-api `mvnw test` 정상 실행
- shop-web: npm test 정상 실행 (TypeScript 검사)

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-17 — CONCERNS
- **검증 방식**: 소스 6종 직접 Read + 신규 테스트 10건 실측 재실행(`ShopStaticResourceServingTest` 7/7, `ShopStaticResourceMissingTest` 3/3 전부 통과) + **실제 기동 실측**(`npm run build` → `mvnw process-resources` → `mvnw spring-boot:run --server.port=8088`로 프로덕션 경로 end-to-end 확인). Dev 보고를 재현으로 검증했다.
- **Layer1 스펙: pass(권고 2)** — AC 7건 전부 충족을 실측으로 확인.
  - AC1 정적 서빙 ✓ — 실기동 `GET /shop/index.html` → 200 `text/html`. 프로덕션 배선(vite `base:'/shop/'` → `dist/` → maven `generate-resources` 복사 → `target/classes/static/shop/`)이 실제로 성립함을 직접 빌드해 확인(`dist/index.html`이 `/shop/assets/index-9yozd6aq.js`를 참조, 복사본이 `target/classes/static/shop/`에 생성됨). 이는 계획이 "구현 후 실측 확인 필요"로 남긴 "프레임워크 실행 모델 함정" (1)(2)(3)을 QA가 대신 닫은 것이다 — Dev 기록에는 이 실측이 없었다.
  - AC2 SPA 폴백 ✓ — `GET /shop/deep/client/route` → 200 + index.html 본문, `Cache-Control: no-cache`. 자산 경로(`/shop/assets/nope.js`)는 폴백하지 않고 404(MIME 위장 없음) — 올바른 분리.
  - AC3 dist 위치·갱신 방법(원래 **[미상]**) — maven `copy-resources`(generate-resources)로 확정, 실측 동작. `process-resources`를 덮지 않음도 확인(application.yml 정상 로드·기동 성공).
  - AC4 캐시 헤더 ✓ — 실측 `/shop/assets/*.js` → `max-age=31536000, public, immutable` / `/shop`·`/shop/`·`/shop/index.html` → `no-cache`.
  - AC5 산출물 없을 때 404+로그 ✓ — absent 픽스처 컨텍스트 정상 기동 + `WARN ... shop-web 빌드 산출물 없음: classpath:/shop-fixture/absent/index.html — /shop 접근은 404` 로그 실제 출력 확인, `/shop`·`/shop/index.html`·`/shop/assets/x.js` 전부 404.
  - AC6·AC7(빈 상태 화면 범위 밖 / 스토리북 해당없음) ✓ — 산출물 없음이 맞다.
  - 새 오류 코드 없음 ✓ — `ApiExceptionHandler` 계열 무변경. `/shop/assets/nope.js` 404 본문은 Spring Boot 기본 error JSON(기존 전역 동작)이지 새 코드가 아니다.
  - **계획 편차(ShopIndexController) 판정: 의도를 해치지 않음, 부작용 없음.** `ResourceHttpRequestHandler`는 매핑 패턴을 벗겨낸 경로가 비면 리졸버 이전 자체 가드(`!StringUtils.hasText(path)`)에서 404를 내므로 리소스 핸들러만으로는 `/shop` bare를 처리할 수 없다 — 별도 컨트롤러는 정당한 해법이다. 매핑이 `{"/shop","/shop/"}` 정확 2경로뿐이라 충돌 표면도 없음(실측: `/shopkeeper` 401 · `/orders` 401 · `/api/products` 401 유지). 남는 문제는 배치뿐 → 권고 2.
- **Layer2 보안: pass** — 실기동 공격 시도 전부 방어, 임의 파일 노출 0건.
  - 화이트리스트 오매칭 없음: `path.equals("/shop") || path.startsWith("/shop/")` — `/shopkeeper` → 401 실측.
  - `/api/**` 인증 불변: `/api/products` 무키 → 401 실측.
  - 경로순회 4종 실측 — `/shop/%2e%2e/application.yml` → 401(Tomcat 정규화 후 화이트리스트 밖), `/shop/..%2fapplication.yml` → 400, `/shop/../application.yml` → 401, `/shop/%252e%252e/application.yml` → 404. **인증 우회 시도 `/shop/%2e%2e/api/products` → 401**(뚫리지 않음). 커스텀 리졸버가 `super.getResource()`(위치-이탈 방지) 뒤에만 `index.html`로 떨어뜨려 새 노출 표면을 만들지 않는다.
  - 새로 열리는 무인증 자원은 공개 SPA 번들뿐 — 데이터 접근은 여전히 `/api/**`(불변)에서만.
- **Layer3 회귀: pass(권고 1)** — 회귀 범위 4항목 전부 실측 유지.
  - `/api/**` 인증·오류 계약 불변 ✓ · 기존 Thymeleaf(`/orders`) 라우팅 불변 ✓ · `/shop` 밖 경로 가로채기 없음 ✓ · 전역 정적/캐시 설정(`spring.web.resources.*`) 무변경 ✓.
  - `vite.config.ts`는 `command==='build'`에서만 `base`를 바꾸므로 `npm run dev`(:5273)·프록시 동작 불변 ✓(`npm test` 32건 통과, 본 QA에서 `npm run build` 성공 재현).
  - 테스트 수 539→549(신규 10건 전부 통과). 실패 2→3건은 실측 확인 결과 전부 **SR-300 이월 날짜창 결함**(시드 `20260816` < 오늘-30일=`2026-08-18` → 목록이 빈 배열)이며 이번 변경과 무관함을 실패 메시지로 확인했다(`OrderListEndToEndIntegrationTest` 2건, `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders` 1건). → 권고 3.
- 필수 수정(FAIL시): 없음 — 차단 이슈 없음.
- 권고(CONCERNS시):
  1. **[medium · 스펙 ID 충돌, 5.5 전에 사람 판정 필요]** `INF-ORD-017`은 이미 `docs/00_FUNC/stories/STORY-FUNC-order-016.md`(SR-212, status **Approved**, approved_sha `bb709b28d4bd`)가 "상품 리뷰 등록/목록 API"로 예약 중이며 `docs/viewer/spec_index.json`·`docs/변경관리/SR-212/_impact.json`에도 그 배정이 남아 있다. `02_변경명세.md`가 이 충돌을 **[미상] 승인권자 확인 필요**로 명시했는데 해소 없이 구현이 진행돼, 그 ID가 코드 javadoc 7곳(`ShopStaticResourceConfig`·`ShopIndexController`·`ApiKeyAuthFilter` 주석·테스트 2종·`vite.config.ts`)에 박혔다. 이대로 STEP 5.5가 `INF-ORD-017.md`를 정적 서빙 내용으로 역생성하면 SR-212 예약분이 조용히 덮여 스펙 그래프·커버리지가 어긋난다(사례집 "한 INF에 엔드포인트 2개 → spec_graph_build 전체 실패"와 같은 계열의 ID 규약 훼손). 조치: 어느 배정이 유효한지 사람이 판정하고, SR-301 쪽을 새 ID(예: `INF-ORD-018`)로 재배정한 뒤 주석 7곳을 일괄 치환한다.
  2. **[medium · 관례 이탈 + must 규칙 사각지대]** `ShopIndexController.java`가 기존 컨트롤러 17개가 전부 사는 `com.sm.lab.shop.controller`가 아니라 `web` 패키지에 있다(`web`에는 필터·예외핸들러만 있었고 컨트롤러는 이것이 유일). Dev 기록이 그 배치 근거로 "`controller` 패키지가 아니므로 `controller-has-test` 규칙 대상 아님"을 명시한 것은 **must 규칙(paths: `**/controller/*.java`)을 배치로 회피한 형태**다 — 규칙 검사기는 통과시키지만(오탐 아님) 관례는 깨진다. 실질 검증 공백은 없다(두 Shop 테스트가 HTTP 레벨로 `/shop`·`/shop/`을 직접 단언). 조치: `controller` 패키지로 옮기고 `ShopIndexControllerTest`(MockMvc) 페어를 만들거나, 옮기지 않기로 한다면 그 예외를 규칙 문서·STORY에 명시 기록해 "회피"가 아니라 "합의된 예외"로 남긴다.
  3. **[low · 이월, 이 SR의 결함 아님]** 테스트 기준선 `.speclinker/test_baseline.json`(539 executed / 2 failed, `git_head 97f28bc`)과 현재(549 / 3 failed)가 어긋난다. 늘어난 실패 1건은 날짜가 하루 흐르며 30일 창을 벗어난 SR-300 이월 결함(사례집 마지막 항목이 그대로 재발 중)이다. 이 SR 종료 시 기준선 재기록을 하더라도 **원인(시드 고정 날짜)을 고치지 않으면 다음 SR에서 또 다른 테스트가 같은 방식으로 넘어간다** — 후속 TODO.
  4. **[low]** 기동 WARN이 원인만 알리고 조치를 안내하지 않는다(`shop-web 빌드 산출물 없음: {loc}index.html — /shop 접근은 404`). AC 문구("빌드 산출물 없음 안내")는 충족하나, 이 SR의 목적("재기동 한 번으로 화면 반영")을 실제로 쓰려면 `shop-web`에서 `npm run build` 선행이 필요하다는 절차가 코드에도 문서에도 없다. 로그 끝에 그 한 문장을 덧붙이면 끝난다.
  5. **[low]** `shop.static-location` 프로퍼티를 후행 슬래시 없이 지정하면(`classpath:/static/shop`) `shopLocation + "assets/"` 연결이 깨진다(`.../shopassets/`). 기본값에는 슬래시가 있어 실동작 영향은 없다 — 생성자에서 슬래시 보정 한 줄이면 방어된다.
- **재동기화 입력(STEP 5.5로 넘김 — 권고 아님)**
  - `INF-ORD-017`(또는 권고 1의 재배정 결과 ID): 본문 없음. 역생성 대상 — 요청/응답이 아니라 "정적 자원 매핑 + SPA 폴백 + 캐시 정책" 형태의 인터페이스라, `anchors:`는 `ShopStaticResourceConfig#addResourceHandlers`와 `ShopIndexController#index`를 함께 가리켜야 한다.
  - `ApiKeyAuthFilter` 공개 경로 목록: `/shop`·`/shop/**` 추가분이 기존 화이트리스트 서술(있다면)에 반영돼야 한다.
  - `02_변경명세.md` AC3의 **[미상]**(dist 위치·갱신 방법)은 구현으로 확정됐다 — `maven-resources-plugin` `generate-resources`가 `modules/shop-web/dist` → `target/classes/static/shop`으로 복사, 기본 참조 위치 `classpath:/static/shop/`, 갱신은 `npm run build` 후 재빌드·재기동. 이 값으로 본문을 채운다.

### QA Gate — 2026-09-17 (round 2) — PASS
- **검증 방식**: 재작업 3파일 직접 Read(`controller/ShopIndexController.java`·`web/ShopStaticResourceConfig.java`·`controller/ShopIndexControllerTest.java`) + `ApiKeyAuthFilter`·기존 Shop 테스트 2종 대조 + `mvnw test` 전체 재실행(552건) + `rules_check.py` 실행 + 기동 WARN 로그 실출력 확인. Dev 보고를 재현으로 검증했다.
- **재작업 지시 4건 반영 확인 — 전부 충족**
  1. **[medium #2 패키지 이동 + controller-has-test] ✓ 완료.** `ShopIndexController.java`가 `com.sm.lab.shop.web` → `com.sm.lab.shop.controller`로 실제 이동(구 경로에 잔존 파일 없음, `package` 선언·`public` 승격 확인), 신규 `src/test/java/com/sm/lab/shop/controller/ShopIndexControllerTest.java`가 규칙이 요구하는 정확한 짝 경로에 존재. **`rules_check.py` 실측: `모드 changed · 파일 53 · 규칙 9 · must 0 · should 1`** — must 위반 0건으로 `controller-has-test` 충족(남은 should 1건은 `ApiKeyAuthFilter` 450줄 초과, 계획이 사전 인지한 기존 상태·이번 라운드 무변경). 코드 전체에 구 패키지(`web.ShopIndexController`) 잔존 참조 0건(grep 확인).
     - 테스트 실측: `ShopIndexControllerTest$WhenBuildOutputPresent` 2/2(`GET /shop` → 200 + `text/html` + 픽스처 마커, `GET /shop/products/1` 딥링크 → 200 + index.html 본문), `$WhenBuildOutputMissing` 1/1(`GET /shop` → 404). **사람이 명시한 완료 조건 3개 전부 단언에 존재하고 전부 통과.**
     - `@Nested` 2조합이 기존 두 Shop 테스트와 동일한 `@SpringBootTest`+`@TestPropertySource` 키를 써서 컨텍스트가 재사용된다(실측 0.52s·0.55s — 신규 컨텍스트 기동 비용 없음). 테스트 시간 회귀 없음.
  2. **[low WARN 조치 안내] ✓ 완료 — 문자열 확인이 아니라 실출력으로 검증.** `ShopStaticResourceConfig:59-62`에 "해결: modules/shop-web에서 npm run build 실행 후 shop-api 재빌드·재기동." 추가. `mvnw test` 로그에서 실제 출력 확인: `WARN c.s.l.o.web.ShopStaticResourceConfig : shop-web 빌드 산출물 없음: classpath:/shop-fixture/absent/index.html — /shop 접근은 404. 해결: modules/shop-web에서 npm run build 실행 후 shop-api 재빌드·재기동.` `no-sysout`/`no-printstacktrace` 여전히 준수(slf4j만).
  3. **[low 후행 슬래시 정규화] ✓ 완료 — 코드 Read로 양쪽 동작 확인.** `ShopStaticResourceConfig:47`·`ShopIndexController:43` 모두 생성자에서 `shopLocation.endsWith("/") ? shopLocation : shopLocation + "/"`. 이로써 `shop.static-location=classpath:/static/shop`(슬래시 없음)로 줘도 `shopLocation + "assets/"` → `classpath:/static/shop/assets/`로 정상 결합되고(구 결함 `.../shopassets/` 해소), `+"index.html"` 결합도 안전. 두 클래스가 같은 프로퍼티를 각자 주입받는 구조에서 **판정 기준이 갈리지 않게 동일 로직을 양쪽에 적용**한 점 확인 — 한쪽만 고쳤다면 `/shop`(컨트롤러)과 `/shop/index.html`(리소스 핸들러)의 존재 판정이 엇갈릴 수 있었다.
  4. **[medium #1 INF-ORD-017 ID 유지] ✓ 문서화 완료, 코드 변경 없음이 정상.** `## Dev 기록` 재작업 절에 판단 근거 4줄(SR-212 SUPERSEDED·claim 해제 / STORY-FUNC-order-016은 v5.1 실행 불가 레거시 이력 / INF-ORD-017 스펙 파일 부재 / 사람 승인) 기록됨. 코드 측 실측: `INF-ORD-017`이 javadoc 6파일 + pom.xml 주석에 그대로 유지, `INF-ORD-018` 등 재배정 흔적 0건 — **사람 판정(그대로 진행)과 코드 상태가 일치**한다. 아래 재동기화 입력에 5.5 반영분을 넘긴다.
- **Layer1 스펙: pass** — AC 7건은 round 1에서 실기동으로 전부 충족 확인됐고, 이번 라운드 변경(패키지 이동 · 로그 문구 · 슬래시 보정)은 **서빙 계약 자체를 건드리지 않는다**. 계약을 지키는 증거인 기존 테스트 2종이 무변경(파일 mtime 00:21·00:23 < 이번 라운드 00:42~00:43)으로 `ShopStaticResourceServingTest` 7/7 · `ShopStaticResourceMissingTest` 3/3 전부 통과 — 캐시 헤더(`no-cache` / `max-age=`+`immutable`) · SPA 폴백 · 산출물 없을 때 404 · 새 오류 코드 없음이 그대로 유지됨. `ApiKeyAuthFilter`·`pom.xml`·`vite.config.ts`는 이번 라운드 무변경(mtime 00:19)이라 배선·빌드 경로 회귀 표면 없음.
- **Layer2 보안: pass** — 이번 라운드에 보안 표면 변경 0건. 화이트리스트(`path.equals("/shop") || path.startsWith("/shop/")`)는 파일 자체가 무변경이고, 실측으로 `/shopkeeper` → 401 · `/api/products`(무키) → 401 · 경로순회(`/shop/%2e%2e/application.yml`, 임의 파일 본문 미노출) 단언이 모두 통과. 컨트롤러의 `public` 승격은 접근제어가 아닌 자바 가시성 문제고 매핑은 `{"/shop","/shop/"}` 정확 2경로 그대로 — 새로 열린 경로 없음. 커스텀 리졸버의 `super.getResource()` 선행(위치-이탈 방지)도 무변경.
- **Layer3 회귀: pass** — 새로 깨진 테스트 0건.
  - 전체 `mvnw test`: **552건 실행 / 실패 3건**(round 1: 549 / 3). 증가분 +3은 정확히 신규 `ShopIndexControllerTest` 3건이며 전부 통과.
  - 실패 3건은 round 1과 **동일한 목록**임을 실측 대조: `OrderListEndToEndIntegrationTest`(`...rendersLatestDeliveryStateColumn...`, `...showsDashWhenOrderHasNoDeliveryHistory`) 2건 + `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders` 1건. 실패 메시지가 전부 "시드 주문번호 `20260816-*` 미노출"로 SR-300 이월 날짜창(오늘-30일) 결함 — 이 SR과 무관함을 재확인(사람이 이번 범위 밖으로 지시한 항목).
  - 패키지 이동 부작용 없음: 컴포넌트 스캔 동일 베이스 패키지 내 이동이라 빈 등록 변화 없고, `@WebMvcTest` 계열 슬라이스도 전부 통과(`shop.static-location` 기본값 존재).
- 필수 수정(FAIL시): 없음 — 차단 이슈 없음.
- 권고(CONCERNS시): 없음 — round 1 권고 5건 중 사람이 "함께 고치기"로 지정한 3건(#2·#4·#5)은 이번에 해소, #1은 사람 판정으로 종결(문서화 확인), #3은 사람이 범위 밖으로 지정한 이월 항목이다. **이번 라운드에서 새 medium 이상은 없다.**
- **후속 TODO(low — 게이트를 막지 않음, 이번에 고치지 말 것)**
  1. `GET /shop/`(후행 슬래시)만 자동 테스트에 없다 — 컨트롤러가 `{"/shop","/shop/"}` 두 경로를 매핑하는데 단언은 `/shop`만 있다(round 1 QA가 실기동으로 `/shop/` → 200 no-cache를 확인했으므로 동작 자체는 검증됨, round 1 코드에서 이월된 공백이라 low). 다음에 이 파일을 만질 때 한 줄 추가.
  2. 후행 슬래시 정규화(이번 라운드 신규)는 테스트 없이 코드 Read로만 검증했다 — `@TestPropertySource(properties="shop.static-location=classpath:/shop-fixture/present")`(슬래시 없음) 조합 1건이면 회귀가 고정된다.
  3. `ShopStaticResourceConfig`(web) ↔ `ShopIndexController`(controller)가 **javadoc `{@link}` 용도로만 서로를 import**해 두 패키지 간 양방향 의존이 생겼다(런타임 사용 없음). 동작·규칙 영향 0, 정리한다면 FQN 링크로 바꾸면 끝난다.
  4. 테스트 기준선 `.speclinker/test_baseline.json`(539/2, `git_head 97f28bc`)과 현재(552/3)의 어긋남 — SR 종료 시 재기록 대상. 원인(SR-300 시드 고정 날짜)은 별도 후속(round 1 권고 3, 사람이 범위 밖 지정).
- **재동기화 입력(STEP 5.5로 넘김 — 권고 아님)**
  - round 1의 재동기화 입력 3건은 그대로 유효하되 **앵커 경로가 이번 라운드에 바뀌었다**: `INF-ORD-017`의 `anchors:`는 `com.sm.lab.shop.web.ShopStaticResourceConfig#addResourceHandlers`와 **`com.sm.lab.shop.controller.ShopIndexController#index`**(구 `web` 패키지 아님)를 함께 가리켜야 한다.
  - `INF-ORD-017` ID 유지 근거(SR-212 SUPERSEDED · STORY-FUNC-order-016은 레거시 이력 · 스펙 파일 부재 · 사람 승인)를 **스펙 변경 이력에 남긴다** — 사람 지시 #1의 명시 요구사항이며 STORY `## Dev 기록`에는 이미 기록됨.
  - INF 본문에 운영 절차 한 문장 포함: "`modules/shop-web`에서 `npm run build` 후 shop-api 재빌드·재기동"(재작업 지시 4의 후단 — 로그에는 반영 완료, 본문에는 5.5에서 반영).
  - `shop.static-location` 프로퍼티 계약 기술 시 "후행 슬래시 유무 무관(코드에서 정규화)"을 명시한다.

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/spec] INF-ORD-017 ID 충돌 미해소 — SR-212(STORY-FUNC-order-016, status Approved, approved_sha bb709b28d4bd)가 같은 ID를 '상품 리뷰 등록/목록 API'로 이미 예약 중이고 spec_index.json·SR-212/_impact.json에도 남아 있다. 02_변경명세.md가 이 충돌을 [미상] 승인권자 확인 필요로 명시했는데 해소 없이 구현돼, 그 ID가 코드 javadoc 7곳(ShopStaticResourceConfig·ShopIndexController·ApiKeyAuthFilter 주석·테스트 2종·vite.config.ts)에 박혔다. STEP 5.5가 INF-ORD-017.md를 정적 서빙 내용으로 역생성하면 SR-212 예약분이 조용히 덮여 스펙 그래프·커버리지가 어긋난다. → 어느 배정이 유효한지 사람이 판정하고 SR-301 쪽을 새 ID(예: INF-ORD-018)로 재배정한 뒤 주석 7곳을 일괄 치환한다. 5.5(스펙 역생성) 전에 처리.
2. [medium/spec] 계획에 없던 ShopIndexController.java가 기존 컨트롤러 17개가 전부 사는 com.sm.lab.shop.controller가 아니라 web 패키지에 놓였고(web에는 필터·예외핸들러만 있었다), Dev 기록이 그 배치 근거로 'controller 패키지가 아니므로 controller-has-test 규칙 대상 아님'을 명시했다 — must 규칙(paths **/controller/*.java)을 배치로 회피한 형태다. 규칙 검사기는 통과하고 실질 검증 공백도 없지만(두 Shop 테스트가 /shop·/shop/를 HTTP 레벨로 단언) 패키지 관례가 깨지고 이후 컨트롤러도 같은 사각지대를 쓸 수 있다. → controller 패키지로 옮기고 ShopIndexControllerTest(MockMvc) 페어를 만들거나, 옮기지 않기로 한다면 그 예외를 규칙 문서·STORY에 합의된 예외로 명시 기록한다.
3. [low/regression] 테스트 기준선(.speclinker/test_baseline.json: 539 executed / 2 failed, git_head 97f28bc)과 현재(549 / 3 failed)가 어긋난다. 늘어난 실패 1건(ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders)은 실측 확인 결과 이번 변경과 무관한 SR-300 이월 결함 — 시드 주문일(20260816)이 '오늘-30일'(2026-08-18) 창을 벗어나 목록이 빈 배열이 됐다. 사례집 마지막 항목이 그대로 재발 중이며, 원인을 두면 다음 SR에서 또 다른 테스트가 같은 방식으로 넘어간다. → 이 SR 종료 시 기준선 재기록과 별개로, 시드를 상대 날짜로 만들거나 테스트 시계를 고정하는 SR-300 후속 TODO를 남긴다.
4. [low/spec] 기동 WARN이 원인만 알리고 조치를 안내하지 않는다('shop-web 빌드 산출물 없음: {loc}index.html — /shop 접근은 404'). AC 문구는 충족하나, 이 SR의 목적(재기동 한 번으로 화면 반영)을 쓰려면 shop-web에서 npm run build 선행이 필요하다는 절차가 코드에도 문서에도 없다. → WARN 메시지 끝에 'modules/shop-web에서 npm run build 후 재빌드·재기동' 한 문장을 덧붙이고, 같은 절차를 INF 본문(5.5)에 적는다.
5. [low/regression] shop.static-location 프로퍼티를 후행 슬래시 없이 지정하면(classpath:/static/shop) ShopStaticResourceConfig의 shopLocation + "assets/" 문자열 연결이 .../shopassets/ 로 깨져 해시 자산 장기 캐시 핸들러가 통째로 무효가 된다. 기본값에는 슬래시가 있어 현재 실동작 영향은 없다. → 생성자에서 후행 슬래시를 보정하는 한 줄(endsWith("/") ? loc : loc + "/")을 넣는다.

사람 코멘트: QA CONCERNS 처리: 이번에 함께 고치기.

[medium #1 — INF-ORD-017 ID] 그대로 진행(사유 있음): SR-212는 원장에서 SUPERSEDED(대체됨)이고 claim(UIS-ORD-004)도 해제됨. STORY-FUNC-order-016은 v5.1부터 실행 불가한 레거시 FUNC STORY(읽기 전용 이력)라 살아있는 점유가 아니다. INF-ORD-017은 스펙 파일 자체가 없어 실제로 비어 있다. 이 판단 근거를 STORY '## Dev 기록'과 STEP 5.5 스펙 변경 이력에 남길 것(재작업 아님 — 문서화만).

[medium #2 — 패키지 위치] 재작업 대상: ShopIndexController를 com.sm.lab.shop.controller 패키지로 옮기고, controller-has-test 규칙대로 ShopIndexControllerTest(MockMvc)를 신규 추가한다. 기존 ShopStaticResourceServingTest·ShopStaticResourceMissingTest는 그대로 둔다.

[low 2건 — 함께 고치기] WARN 로그 메시지에 조치 안내 한 줄 추가. shop.static-location 프로퍼티 값의 후행 슬래시 미보정 처리(둘 다 한 줄 수준).

[low 1건 — SR-300 이월] 이 SR과 무관, 기준선 처리 대상이라 이번 재작업 범위 아님.

완료 조건: ShopIndexControllerTest - GET /shop 200 + content-type text/html, GET /shop/products/1 같은 딥링크 200(index.html), 정적 자원 없을 때 404. 기존 테스트 전부(신규 포함) 통과.
