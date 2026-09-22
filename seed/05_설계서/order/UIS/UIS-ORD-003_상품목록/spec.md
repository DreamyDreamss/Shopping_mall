---
화면ID: productList
화면명: 상품 목록
라우트: /product/list
도메인: order
UIS-ID: UIS-ORD-003
screens_role: 주화면
api_hints:
  - "GET /product/list"
  - "GET /product/{sku}"
access_control:
  - "권한 게이팅 없음 — 화면 진입/버튼 노출 모두 무조건 표시(소스에 auth 슬롯·disabled 분기 없음)"
anchors:
  - "src/main/resources/templates/product/list.html"
  - "src/main/java/com/sm/lab/shop/controller/ProductViewController.java:32-37"
revision_history:
  - version: 1.0
    date: 2026-08-22
    author: ddd-ui-agent (source-authority)
    change: 최초 생성 (SR-201, FUNC-order-009)
  - version: 1.1
    date: 2026-09-09
    author: sl-aidd STEP 5.5 (재동기화)
    change: >-
      [반영: FUNC-order-009] 정렬 셀렉트(selSort, SR-214) 위젯 추가 + 빈 목록 안내 문구를
      검색조건 유무로 구분(SR-224)하도록 갱신. 앵커 3건 정정.
---

# UIS-ORD-003: 상품 목록

> **근거 소스(권위):** `src/main/resources/templates/product/list.html` (Thymeleaf 서버 렌더) +
> `src/main/java/com/sm/lab/shop/controller/ProductViewController.java`. 스크린샷은 보조.
> SR-201로 신규 구현된 화면(FUNC-order-009, Done). > [반영: FUNC-order-009] SR-214(정렬 셀렉트)·
> SR-224(빈 목록 문구 통일) 반영(2026-09-09).

## 0. 화면 미리보기

![개요](preview_annotated.png)

> 원 안 번호 = 「4. 위젯·액션」 표의 번호와 1:1.

## 1. 화면 목적

판매중 상품 목록을 상품명 키워드로 검색·조회하는 화면이다. 각 행의 상품명 링크를 클릭하면 해당
SKU의 상품 상세 화면(`/product/{sku}`, FUNC-order-010)으로 이동한다.

## 2. 주요 작업 시나리오

**시나리오: 상품 조회**
1. 「상품명」 입력란에 검색어를 입력한다(선택 — 비우면 전체 판매중 상품 대상). (1)
2. 「검색」 버튼을 클릭한다. 폼(`#searchForm`, method=GET)이 `GET /product/list?keyword=…`로
   재요청되며, 서버가 `ProductService.list(keyword)`로 상품명 부분일치(LIKE) 조회를 수행해
   동일 화면을 재렌더한다. (2)
3. 조회 결과 목록에서 원하는 상품의 상품명 링크를 클릭하면 `GET /product/{sku}`로 이동해 상품
   상세 화면을 확인할 수 있다. (3~5)

(별도의 등록/수정/삭제 시나리오 없음 — 이 화면은 조회·상세이동 전용 카탈로그 화면이다.)

**시나리오: 정렬 변경 (SR-214)** — [반영: FUNC-order-009]
1. 「정렬」 셀렉트(`selSort`)에서 기본순(기본값)·가격 낮은순·가격 높은순 중 하나를 고르면
   `onchange`로 폼이 즉시 `GET /product/list?sort=…`로 재제출된다(별도 버튼 클릭 불요). (6)
2. `sort` 값은 URL 파라미터로 남아 새로고침·뒤로가기에도 선택이 유지된다. 화이트리스트
   (`latest`/`priceAsc`/`priceDesc`) 밖의 값은 오류 화면 없이 서버가 조용히 `latest`로
   되돌린다. "기본순" 라벨은 등록일시 컬럼 부재로 `ORDER BY p.sku ASC`(기존 등록순)를
   그대로 쓰는 실제 동작에 맞춘 표기다 — 참 최신순이 아니다.

## 3. 화면 구성 (블록)

| 블록 | 역할 | 주요 위젯 | 소스 근거 |
|------|------|----------|----------|
| 검색조건 | 상품명 키워드로 목록 필터링 + 정렬 선택(SR-214) | `keyword` 입력, `selSort` 셀렉트, `btnSearch` 버튼 | `list.html:24-39` |
| 상품목록 테이블 | 조회 결과 표시(SKU/상품명/가격/재고/상태) + 상세 이동 링크 | 상품명 링크 N건(현재 페이지 기준) | `list.html:40-53` |
| 빈 목록 안내 | 조회 결과 0건일 때 안내 문구(SR-224 — 검색조건 유무로 문구 구분) | keyword 없음: "조회 결과가 없습니다" / keyword 있음: "조건에 맞는 결과가 없습니다" | `list.html:55-56` |

## 4. 위젯·액션

> 번호 = 「0. 화면 미리보기」 마커 번호와 1:1. **(6)은 SR-214로 추가된 위젯이라 기존
> `preview_annotated.png`에는 마커가 없다** — 화면 재캡처(`/sl-recon-uis` 재실행) 전까지는
> 아래 표만 정본으로 본다.

| 번호 | 위젯 | 타입 | 레이블 | 동작 | 연결 API(raw) | 결과 |
|---|------|------|--------|------|--------------|------|
| (1) | `keyword` | input(text) | 상품명 | 검색 조건 입력(부분일치 검색어) | — | 폼 파라미터로 (2) 검색 시 전송 |
| (2) | `btnSearch` | button(submit) | 검색 | 폼(`#searchForm`, method=GET) 제출 | GET /product/list | 동일 화면 재렌더(keyword LIKE 필터 적용된 목록 갱신) |
| (3) | 상품명 링크(예: 스탠딩 데스크, SKU-1001) | a | {상품명} | 해당 SKU 상세로 이동 | GET /product/{sku} | 상품 상세 화면(FUNC-order-010) 이동 |
| (4) | 상품명 링크(예: 기계식 키보드, SKU-1002) | a | {상품명} | 해당 SKU 상세로 이동 | GET /product/{sku} | 상품 상세 화면(FUNC-order-010) 이동 |
| (5) | 상품명 링크(예: 4K 모니터, SKU-1003) | a | {상품명} | 해당 SKU 상세로 이동 | GET /product/{sku} | 상품 상세 화면(FUNC-order-010) 이동 |
| (6) | `selSort` | select | 정렬 | 옵션 변경 시 `onchange="this.form.submit()"`로 폼 즉시 재제출(SR-214) | GET /product/list | 동일 화면 재렌더(선택 정렬 기준 적용된 목록, `sort` URL 파라미터 유지) |

> (3)~(5)는 조회 결과 행 수만큼 반복 렌더되는 **동일 패턴의 링크**다(`list.html:34-40`,
> `th:each="p : ${products}"`). 캡처 시점 조회 결과가 3건이라 3개로 나타났을 뿐, 표는 위젯
> **패턴**을 설명한다. `상태` 컬럼(판매중/품절)은 `p.stockQty == 0` 여부로 클라이언트 개입 없이
> 서버에서 계산되는 표시 전용 값(액션 없음, 마커 대상 아님).

## 5. 접근 권한·표시 조건

| 요소 | 표시 조건 | 근거 |
|------|----------|------|
| 검색조건·목록·상세이동 링크 전체 | 조건 없음(항상 표시) — 소스에 `auth:` 슬롯·권한 분기 없음 | `list.html` 전체, `ProductViewController.java:32-37` |

## 7. 데이터 출처·연결

- **연결 API(raw → INF):** `GET /product/list`(화면 자기갱신), `GET /product/{sku}`(상세 화면
  이동) — 둘 다 Thymeleaf 서버 렌더 라우트(`kind:form`)이며, 이 화면에서 INF로 치환되는 API
  호출은 없다(정상 — INF 없음).
- **데이터 원천(서버사이드, api_hints 아님):** 이 화면은 브라우저에서 `GET /api/products`를
  호출하지 않는다 — `ProductViewController.productList()`가 `ProductService`를 서버 내부에서
  **직접 주입·호출**해 렌더링 데이터를 만든다(`ProductViewController.java:32-37`). 다만 그 내부
  로직(판매중 상시필터 + `keyword` 부분일치, `ProductService.list(keyword)`)은 [[INF-ORD-008]]
  (`GET /api/products`)와 **동일한 서비스 메서드를 공유**하므로, INF-ORD-008을 이 화면의 데이터
  출처(사양상 원천 참고)로 명시한다. 두 계층은 진입 경로가 다를 뿐(REST vs 서버렌더) 조회 규칙은
  같다 — INF-ORD-008의 `screens:`에는 이 화면을 실제 호출자로 등재하지 않는다(호출 사실이 없으므로).
- **참조 테이블(SCH):** [[SCH-ORD-005]] (`PRODUCTS`) — [[INF-ORD-008]] 참조 테이블과 동일.

## 8. 미확인 사항

- 목록 페이징 UI(다음 페이지 등)는 뷰·컨트롤러 어디에도 존재하지 않는다 — 전체(또는 keyword
  필터) 결과가 페이징 없이 한 번에 렌더된다.
- 자동 마커 선택 스크립트(`select_tab_widgets.py`, 기본 `--toolbar-y 90`)는 이 화면에서 상품명
  링크(순수 `href`만 가진 `<a>`, `id`/`onclick` 없음)를 필터 조건 `id 있음 OR onclick 있음`
  미충족으로 제외했고, `keyword` 입력(input 태그)도 대상 태그(`button`/`a`)가 아니라 제외했다.
  `--toolbar-y 30`으로 낮춰 `btnSearch`(y=64)만 추가 확보(1건) 후, 나머지 4건(입력 1 + 링크 3)은
  DOM 스냅샷 전수를 에이전트가 수동으로 `preview_widgets.json`에 반영해 생성했다(UIS-ORD-001과
  동일 사례) — 누락 없음을 확인했으나, 순수 href 링크·input 태그 화면에 대한 스크립트 개선이
  필요하다.
