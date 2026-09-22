---
inf-id: INF-ORD-017
name: shop spa static serving
layer: static
method: GET
path: /shop/**
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables: []
anchors:
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ShopStaticResourceConfig.java:75-95
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/ShopIndexController.java:46-56
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:230
  - modules/shop-api/pom.xml:93-119
  - modules/shop-web/vite.config.ts
---

# INF-ORD-017: GET /shop/** — 쇼핑 SPA 정적 서빙 + 딥링크 폴백

> **개요:** shop-web(React/Vite) 빌드 산출물(`dist`)을 shop-api가 `/shop/**` 경로로 직접 서빙한다.
> 별도 개발 서버(:5273) 재기동 없이, shop-api 재기동 한 번으로 화면 변경이 반영되게 하는 것이 목적(SR-301).
> 새 REST 엔드포인트가 아니라 정적 리소스 매핑 + SPA 딥링크 폴백이며, 새 오류 코드·인증 계약 변경은 없다.

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/web/ShopStaticResourceConfig.java:75-95`,
> `modules/shop-api/src/main/java/com/sm/lab/shop/controller/ShopIndexController.java:46-56`

## 요청

- Method: GET
- Path 패턴 2종(경로 특이도로 자동 우선순위 결정, 등록 순서 무관):
  - `/shop/assets/**` — Vite 빌드 산출물 중 해시 붙은 JS/CSS(장기 캐시 대상)
  - `/shop/**` — 그 외 전부(정적 파일이 실재하면 그 파일, 없으면 SPA 딥링크로 간주)
  - `/shop`·`/shop/`(경로 세그먼트 없음, 정확 일치) — `ShopIndexController`가 전담(아래 "구현 세부" 참고)
- 인증: 무인증(`ApiKeyAuthFilter.isOpenRoute`에 `/shop`·`/shop/**` 화이트리스트, `modules/shop-api/.../ApiKeyAuthFilter.java:230`). 실제 데이터 접근은 여전히 `/api/**`(불변)에서만 일어나므로 이 화이트리스트가 자격 판정을 우회하지 않는다.

## 응답

| 요청 형태 | 응답 |
|---|---|
| `/shop/assets/{해시파일}` (실재) | 200, 원본 Content-Type, `Cache-Control: max-age=31536000, public, immutable` |
| `/shop`, `/shop/`, `/shop/index.html` | 200, `text/html`, `Cache-Control: no-cache`, index.html 본문 |
| `/shop/{미매칭 딥링크}`(예: `/shop/products/123`) | 200, `text/html`, `Cache-Control: no-cache`, index.html 본문(SPA 클라이언트 라우팅 위임) |
| 빌드 산출물 자체가 없음(`index.html` 부재) | 404(Spring 표준, `NoResourceFoundException` 계열) — 200으로 위장하지 않는다 |
| `/shop/assets/{존재하지 않는 파일}` | 404(자산은 폴백하지 않는다 — MIME 오류를 index.html로 감추지 않는다) |

## 비즈니스 규칙

- 정적 자산(`/shop/assets/**`)과 SPA 폴백(`/shop/**`)은 서로 다른 캐시 정책을 쓴다: 해시 붙은 자산은 불변으로 간주(1년, `immutable`), `index.html`은 배포 직후 옛 화면이 남지 않도록 `no-cache`.
- SPA 폴백은 `PathResourceResolver`를 확장해 `super.getResource()`(Spring 내장 위치-이탈 방지)로 실제 파일을 먼저 찾고, 없을 때만 `index.html`로 대체한다 — 새 경로 순회(traversal) 표면을 만들지 않는다.
- `ResourceHttpRequestHandler`는 매핑 패턴을 벗겨낸 나머지 경로가 빈 문자열이면(정확히 `/shop`·`/shop/`) 리졸버 체인 진입 전에 자체 가드로 404를 내는 실측 제약이 있다 — 이 두 정확 경로만 `ShopIndexController`(`@GetMapping({"/shop","/shop/"})`)가 별도로 index.html을 직접 읽어 반환한다. 하위 미매칭 경로(`/shop/products/123` 등)는 리소스 경로가 비어있지 않으므로 `ShopStaticResourceConfig`의 폴백 리졸버가 정상 처리한다.
- 빌드 산출물이 없어도 shop-api 기동은 성공한다 — 기동 시 1회(`@PostConstruct`) `index.html` 존재를 확인해 없으면 WARN 로그("...해결: modules/shop-web에서 npm run build 실행 후 shop-api 재빌드·재기동.")를 남기고, 요청마다 반복 로깅하지 않는다.
- `shop.static-location` 프로퍼티(기본값 `classpath:/static/shop/`)는 후행 슬래시 유무와 무관하게 정규화되어 동작한다(`ShopStaticResourceConfig`·`ShopIndexController` 양쪽 생성자에서 동일하게 보정).
- `/shop`으로 시작하지만 `/shop/`이 아닌 경로(예: `/shopkeeper`)는 화이트리스트 오매칭 없이 여전히 401(`ApiKeyAuthFilter`가 `equals("/shop") || startsWith("/shop/")` 정확 조합 사용).
- **클라이언트 진입 규칙(SR-302.1, 서버 계약 불변)**: 이 엔드포인트가 `index.html`을 반환한 뒤, 그 안의 SPA가 해시 없이 최초 로드되면 쇼핑 홈(`UIS-ORD-008`)으로 1회 이동하고 이후 `#/`는 주문 목록이다 — 자세한 내용은 `UIS-ORD-008`의 "진입/라우팅" 참고. 이 문서의 요청·응답·앵커 줄 범위는 변경되지 않았다(클라이언트 라우팅은 이 서버 엔드포인트 밖의 동작).

## 빌드·배포 절차 (AC "빌드 산출물 위치·갱신 방법" 확정값)

1. `modules/shop-web`에서 `npm run build` 실행 → `modules/shop-web/dist` 생성. 이때 `vite.config.ts`의 `base`가 `command==='build'`일 때만 `/shop/`으로 설정되어(개발 서버 `npm run dev`, :5273은 `base:'/'` 그대로 무변경) 산출물의 자산 참조 경로가 `/shop/assets/...`로 나온다.
2. `modules/shop-api`에서 Maven 빌드(`mvnw` 등) 시 `generate-resources` 단계에서 `maven-resources-plugin`(`pom.xml:93-119`, 실행 id `copy-shop-spa-dist`)이 `../shop-web/dist`를 `target/classes/static/shop`으로 복사한다. `src/main/resources`(소스 트리)는 건드리지 않으므로 `.gitignore` 변경 불필요.
3. `shop-web/dist`가 없는 환경(예: 아직 한 번도 빌드하지 않음)에서도 이 복사 단계는 조용히 건너뛰고 빌드·기동이 실패하지 않는다.
4. 화면을 갱신하려면: shop-web에서 `npm run build` 재실행 → shop-api 재빌드·재기동 1회. (SR-301의 목적 — 별도 개발 서버 재기동 불필요)

## 오류 응답

새 오류 코드는 만들지 않는다(`{code,message}` 오류 계약 대상 아님 — 정적 리소스 표준 응답만 사용).

| 상태 | 조건 |
|------|------|
| 404 | 요청 경로에 대응하는 정적 파일·`index.html` 모두 없음(빌드 산출물 없음) |
| 404 | `/shop/assets/**` 하위 자산 요청인데 실제 파일 없음(SPA 폴백 대상 아님) |
| 401 | `/shop`·`/shop/**` 밖의 경로(`/api/**` 등)를 인증 헤더 없이 요청(기존 계약 불변) |

## 범위 밖

- 빌드된 SPA가 실제로 `/api/**`를 호출해 정상 동작하는지(런타임 API 연동)는 이 SR 범위 밖 — 후속 SR 후보.
- `npm run build` + Maven 빌드를 한 명령으로 묶는 배포 자동화는 이 SR 범위 밖.
- 개발 서버(:5273)의 `server.proxy` 등 구성 변경 없음 — `vite.config.ts`는 `base`(빌드 전용) 한 줄만 조건부 변경.

## 스펙 ID 참고

`INF-ORD-017`은 SR-212(`docs/00_FUNC/stories/STORY-FUNC-order-016.md`, "상품 리뷰 API")가 과거 예약했던 것과 같은 번호이나, SR-212는 원장에서 SUPERSEDED(대체)되어 그 예약이 살아있는 점유가 아니고(claim 해제됨, v5.1부터 실행 불가한 레거시 FUNC STORY로 읽기 전용 이력), 실제 `INF-ORD-017.md` 스펙 파일 자체가 이 SR 이전까지 존재하지 않았다(사람 확인·승인 — SR-301.1 STORY `## Dev 기록`).

## curl 예시

```bash
curl -i http://localhost:8087/shop/
curl -i http://localhost:8087/shop/products/123
curl -i http://localhost:8087/shop/assets/index-9yozd6aq.js
```

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-17 | SR-301 | #1 | 신규: shop-web 빌드 산출물을 /shop/**로 정적 서빙 + SPA 딥링크 폴백(ShopStaticResourceConfig·ShopIndexController), /shop 인증 화이트리스트, maven-resources-plugin dist 복사, vite base 조건부 변경 | shop-api@0f75df7, shop-web@7feb578 |
| 2026-09-17 | SR-302 | #1 | 클라이언트 진입 규칙 각주 추가(서버 계약·앵커 불변) — index.html 반환 후 SPA가 쇼핑 홈으로 이동하는 규칙을 UIS-ORD-008에 위임 | shop-web@efd0e7c |
