---
화면ID: productDetail
화면명: 상품 상세
라우트: /product/{sku}
도메인: order
UIS-ID: UIS-ORD-004
screens_role: 주화면
api_hints:
  - "GET /product/{sku}"
  - "GET /product/list"
access_control:
  - "권한 게이팅 없음 — 화면 진입은 무조건 렌더(소스에 auth 슬롯·disabled 분기 없음), 존재/미존재 SKU 모두 동일 라우트로 처리"
anchors:
  - "src/main/resources/templates/product/detail.html"
  - "src/main/java/com/sm/lab/shop/controller/ProductViewController.java:46-60"
  - "src/main/java/com/sm/lab/shop/service/ProductService.java:26-32"
revision_history:
  - version: 1.0
    date: 2026-08-22
    author: ddd-ui-agent (source-authority)
    change: 최초 생성 (SR-201, FUNC-order-010)
---

# UIS-ORD-004: 상품 상세

> **근거 소스(권위):** `src/main/resources/templates/product/detail.html` (Thymeleaf 서버 렌더) +
> `src/main/java/com/sm/lab/shop/controller/ProductViewController.java`. 스크린샷은 보조.

## 0. 화면 미리보기

![개요](preview.png)

> 이 화면은 인터랙션 위젯(id·onclick 보유 button/a/select)이 없다 — `select_tab_widgets.py` 전수
> 스캔 결과 0건(정적 텍스트/표 + href만 있는 목록 링크 1개). 따라서 마커 번호 없음, annotate 생략.

## 1. 화면 목적

상품 1건(`sku`)의 SKU·상품명·가격·재고·상태를 조회 전용으로 표시한다(주문 상세 화면과 동일 톤 —
스타일·목록 링크·숫자 포맷 패턴 재사용). 존재하지 않는 SKU로 접근하면 예외 페이지 대신
"상품을 찾을 수 없습니다" 안내 문구를 보여준다.

## 2. 주요 작업 시나리오

**시나리오: 상품 상세 조회 (정상 SKU)**
1. 상품 목록 화면(`/product/list`, UIS-ORD-003)에서 상품명을 클릭하거나, `/product/{sku}` 라우트로
   직접 진입한다.
2. 서버(`ProductViewController#productDetail`)가 `productService.get(sku)`를 호출해 상품 1건을 조회한다
   (GET 시점에 이미 데이터 확정 — 화면 내 별도 조회 액션 없음).
3. 표에서 SKU·상품명·가격(`#,###원` 포맷)·재고·상태(재고 0이면 "품절", 그 외 "판매중")를 확인한다.
4. 목록으로 돌아가려면 상단 「← 목록으로」 링크로 이동한다.

**시나리오: 존재하지 않는 SKU로 접근**
1. 등록되지 않은 `sku`로 `/product/{sku}`에 접근한다.
2. `productService.get(sku)`가 던지는 404(`ResponseStatusException`)를 컨트롤러가 흡수하고
   `product=null`로 모델에 담아 같은 뷰(`product/detail`)를 그대로 반환한다(예외 스택 트레이스
   노출 없음).
3. 화면에는 "상품을 찾을 수 없습니다" 안내 문구만 표시된다(상세 표 없음).
4. 「← 목록으로」 링크는 이 상태에서도 동일하게 노출되어 목록으로 복귀할 수 있다.

## 3. 화면 구성 (블록)

| 블록 | 역할 | 주요 위젯 | 소스 근거 |
|------|------|----------|----------|
| 상단 네비게이션 | 목록 화면으로 복귀 | `← 목록으로` 링크(href, 정상/미존재 양쪽 공통) | `detail.html:20` |
| 상품 상세 표 (정상 SKU) | SKU·상품명·가격·재고·상태 5행 표시 | 정적 테이블(위젯 없음) | `detail.html:28-37` |
| 안내 영역 (미존재 SKU) | "상품을 찾을 수 없습니다" 문구 | 정적 텍스트 | `detail.html:23-26` |

## 4. 위젯·액션

> 이 화면은 인터랙션 위젯(id/onclick 보유 button·a·select)이 없다 — `select_tab_widgets.py` 전수
> 스캔 결과 0건. 유일한 링크(`← 목록으로`, `href="/product/list"`, id/onclick 없는 정적 앵커)는
> 목록·상세 공통 패턴(UIS-ORD-002 참조)과 동일하게 위젯표 대상이 아니며 3번 「화면 구성」에 기록했다.
> 별도 클릭 액션(버튼 등)이 없어 아래 표는 생략한다.

## 5. 접근 권한·표시 조건

| 요소 | 표시 조건 | 근거 |
|------|----------|------|
| 화면 전체 | 권한 분기 없음 — `/product/{sku}` 진입 시 무조건 렌더 | `ProductViewController.java:46-60` |
| 상세 표(5행) | `product != null`(정상 조회된 SKU)일 때만 렌더 | `detail.html:28` |
| "상품을 찾을 수 없습니다" 문구 | `product == null`(404 흡수)일 때만 렌더 | `detail.html:23` |
| 「← 목록으로」 링크 | 위 두 분기와 무관하게 항상 노출 | `detail.html:20` |

## 6. 팝업·연계 화면

팝업 없음(섹션 생략 대상이나 이동 대상만 기록).

| 트리거 위젯 | 팝업/연계 화면 | 연결 API/화면 (raw → INF) | 용도 |
|------------|--------------|--------------------------|------|
| `← 목록으로` 링크 | 상품 목록 화면(UIS-ORD-003) | GET /product/list ← 화면 이동(INF 없음) | 목록으로 복귀 |

## 7. 데이터 출처·연결

- **연결 API(raw → INF):**
  - `GET /product/{sku}` — 화면 자체 라우트(서버 렌더 진입점). INF 대상 아님(view 반환, JSON API 아님).
    내부적으로 `ProductService.get(sku)`를 **자바 메서드로 직접 호출**한다(HTTP 호출 아님) — 이
    메서드는 REST API 컨트롤러([[INF-ORD-009]] `GET /api/products/{sku}`)와 **동일 서비스 인스턴스를
    공유**하지만, 상세 화면은 그 API를 HTTP로 부르지 않고 같은 서비스 계층을 재사용한다.
  - `GET /product/list` — 목록 화면 이동. INF 대상 아님.
- **참조 테이블(SCH):** PRODUCTS ([[SCH-ORD-005]], INF-ORD-009 기준)

## 8. 미확인 사항

- **미존재 SKU 응답 코드 불일치 (SR-201 QA 결정 수용):** 이 화면은 존재하지 않는 SKU에 대해
  **HTTP 200 + "상품을 찾을 수 없습니다" 안내 문구**를 렌더한다. 반면 같은 자원을 다루는 API
  [[INF-ORD-009]](`GET /api/products/{sku}`)는 **404**를 반환한다 — 두 계층의 "미존재" 의미가
  일부러 다르게 설계됐다. STORY-FUNC-order-010 QA(r1 CONCERNS 권고3, r2 재게이트 PASS 시 잔존·수용)에서
  "AC가 상태 코드를 규정하지 않고, 화면단은 사용자 안내가 목적이라 200 렌더가 화면 테스트·구현상
  더 단순하다"는 이유로 **수용(waive)** 결정됨 — 결함이 아니라 화면/API 계층 간 의도된 차이로 기록.
  (근거: `docs/00_FUNC/stories/STORY-FUNC-order-010.md` QA Gate 권고3/잔존권고1)
- **등록일 미표시 (SR-201 의사결정 D1):** 요구사항 원문은 등록일 표시를 언급하나, `PRODUCTS` 테이블에
  등록일 컬럼이 없어(DB 실측: sku·product_name·price·stock_qty·sale_yn 5개뿐) 이번 범위에서 **제외**했다.
  "DB 변경 없음" 원칙 유지가 이유이며, 등록일은 상품 등록 기능이 도입되는 추후 SR에서 컬럼과 함께
  재검토한다. (근거: `docs/변경관리/SR-201/inputs/_decisions.md` D1)
- 경로 변수 경계 케이스(`/product/` 빈 sku, `/` 포함 sku)는 이 화면의 친화 안내(200+문구) 대상이
  아니라 Spring 기본 404로 빠진다(스택 트레이스 노출은 없음) — 실 SKU 형식(`SKU-####`)에는
  해당 없어 조치 불요로 QA에 이미 기록됨(잔존 권고2).
