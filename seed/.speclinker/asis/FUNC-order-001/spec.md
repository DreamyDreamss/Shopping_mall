---
화면ID: orderList
화면명: 주문 목록
라우트: /order/list
도메인: order
UIS-ID: UIS-ORD-001
screens_role: 주화면
api_hints:
  - "GET /order/list"
  - "GET /order/{orderNo}"
access_control:
  - "권한 게이팅 없음 — 화면 진입/버튼 노출 모두 무조건 표시(소스에 auth 슬롯·disabled 분기 없음)"
anchors:
  - "src/main/resources/templates/order/list.html"
  - "src/main/java/com/sm/lab/shop/controller/OrderViewController.java:20-28"
revision_history:
  - version: 1.0
    date: 2026-08-22
    author: ddd-ui-agent (source-authority)
    change: 최초 생성
---

# UIS-ORD-001: 주문 목록

> **근거 소스(권위):** `src/main/resources/templates/order/list.html` (Thymeleaf 서버 렌더) +
> `src/main/java/com/sm/lab/shop/controller/OrderViewController.java`. 스크린샷은 보조.

## 0. 화면 미리보기

![개요](preview_annotated.png)

> 원 안 번호 = 「4. 위젯·액션」 표의 번호와 1:1.

## 1. 화면 목적

회원 ID와 주문상태 조건으로 주문 내역을 조회하는 목록 화면이다. 각 행의 주문번호를 클릭하면
해당 주문의 상세 화면(UIS-ORD-002)으로 이동한다.

## 2. 주요 작업 시나리오

**시나리오: 주문 조회**
1. 「회원 ID」에 조회할 회원의 ID를 입력한다(선택 — 비우면 전체 회원 대상). (1)
2. 「주문상태」 드롭다운에서 상태를 선택한다(선택 — 기본값 "전체"). (2)
3. 「검색」 버튼을 클릭한다. 폼이 `GET /order/list`로 재요청되며 `memberId`·`orderState` 쿼리
   파라미터로 필터링된 목록이 다시 렌더링된다. (3)
4. 목록에서 원하는 주문의 주문번호 링크를 클릭하면 `GET /order/{orderNo}`로 이동해 주문 상세
   화면(UIS-ORD-002)을 확인할 수 있다. (4~8)

(별도의 등록/수정/삭제 시나리오 없음 — 이 화면은 조회·상세이동 전용)

## 3. 화면 구성 (블록)

| 블록 | 역할 | 주요 위젯 | 소스 근거 |
|------|------|----------|----------|
| 검색조건 | 회원 ID·주문상태로 주문 필터링 | `memberId` 입력, `selState` 드롭다운, `btnSearch` 버튼 | `list.html:20-30` |
| 주문목록 테이블 | 조회 결과 표시(주문번호/회원/상태/총액/주문일시) + 상세 이동 링크 | 주문번호 링크 5건(현재 페이지 기준) | `list.html:32-45` |
| 총건수 | 조회 결과 총 건수 표시 | `전체 N건` | `list.html:46` |

## 4. 위젯·액션

> 번호 = 「0. 화면 미리보기」 마커 번호와 1:1.

| 번호 | 위젯 | 타입 | 레이블 | 동작 | 연결 API(raw) | 결과 |
|---|------|------|--------|------|--------------|------|
| (1) | `memberId` | input(text) | 회원 ID | 검색 조건 입력(placeholder 예시: M-0001) | — | 폼 파라미터로 (3) 검색 시 전송 |
| (2) | `selState` | select | 주문상태 | 검색 조건 선택(전체/PLACED/PAID/SHIPPED/PARTIAL_SHIPPED/CANCELED/DONE) | — | 폼 파라미터로 (3) 검색 시 전송 |
| (3) | `btnSearch` | button(submit) | 검색 | 폼(`#searchForm`, method=GET) 제출 | GET /order/list | 동일 화면 재렌더(필터 적용된 목록·총건수 갱신) |
| (4) | 주문번호 링크(예: 20260817-0002) | a | {주문번호} | 해당 주문 상세로 이동 | GET /order/{orderNo} | 주문 상세 화면(UIS-ORD-002) 이동 |
| (5) | 주문번호 링크(예: 20260817-0001) | a | {주문번호} | 해당 주문 상세로 이동 | GET /order/{orderNo} | 주문 상세 화면(UIS-ORD-002) 이동 |
| (6) | 주문번호 링크(예: 20260816-0002) | a | {주문번호} | 해당 주문 상세로 이동 | GET /order/{orderNo} | 주문 상세 화면(UIS-ORD-002) 이동 |
| (7) | 주문번호 링크(예: 20260816-0001) | a | {주문번호} | 해당 주문 상세로 이동 | GET /order/{orderNo} | 주문 상세 화면(UIS-ORD-002) 이동 |
| (8) | 주문번호 링크(예: 20260815-0001) | a | {주문번호} | 해당 주문 상세로 이동 | GET /order/{orderNo} | 주문 상세 화면(UIS-ORD-002) 이동 |

> (4)~(8)은 조회 결과 행 수만큼 반복 렌더되는 **동일 패턴의 링크**다(`list.html:38`,
> `th:each="o : ${result['items']}"`). 캡처 시점 조회 결과가 5건이라 5개로 나타났을 뿐, 표는
> 위젯 **패턴**을 설명한다.

## 5. 접근 권한·표시 조건

| 요소 | 표시 조건 | 근거 |
|------|----------|------|
| 검색조건·목록·상세이동 링크 전체 | 조건 없음(항상 표시) — 소스에 `auth:` 슬롯·권한 분기 없음 | `list.html` 전체 |

## 7. 데이터 출처·연결

- **연결 API(raw → INF):** `GET /order/list`(화면 자기갱신), `GET /order/{orderNo}`(상세 화면 이동) —
  둘 다 Thymeleaf 서버 렌더 라우트(`kind:form`)이며, `docs/05_설계서/order/INF/`의 `/api/orders/*`
  REST 엔드포인트(INF-ORD-003~007)와는 별개 계층이다. 화면은 `OrderService`를 서버 내부에서
  직접 호출하므로 이 화면에서 INF로 치환되는 API 호출은 없다(정상 — INF 없음).
- **참조 테이블(SCH):** `docs/05_설계서/order/SCH/`(주문·회원·상품 테이블 — `DB_order.md` 참고)

## 8. 미확인 사항

- 목록 페이징 UI(다음/이전 페이지 이동 컨트롤)는 컨트롤러가 `page` 파라미터(`defaultValue="1"`)를
  받지만 `list.html`에는 페이지네이션 위젯이 렌더되지 않는다 — 현재 뷰에는 페이징 컨트롤 없음.
- 자동 마커 선택 스크립트(`select_tab_widgets.py`)는 이 화면에서 0~1건만 선택했다(주문번호
  링크가 `id`/`onclick` 없이 순수 `href`만 가진 `<a>`라 필터 조건 `id 있음 OR onclick 있음`을
  충족하지 못했고, 검색 버튼은 기본 `--toolbar-y 90` 임계값(버튼 y=64)에 걸려 제외됨). 이 spec의
  0·4번 마커는 DOM 스냅샷 전수(8건)를 에이전트가 수동으로 `preview_widgets.json`에 반영해
  생성했다 — 누락 없음을 확인했으나, 순수 href 링크 화면에 대한 스크립트 개선이 필요하다.
