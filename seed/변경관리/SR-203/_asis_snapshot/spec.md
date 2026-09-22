---
화면ID: cartList
화면명: 장바구니
라우트: /cart
도메인: order
UIS-ID: UIS-ORD-005
screens_role: 주화면
api_hints:
  - "GET /cart"
  - "POST /cart/items/{sku}/update"
  - "POST /cart/items/{sku}/delete"
  - "GET /product/list"
access_control:
  - "권한 게이팅 없음 — 소스에 auth 슬롯·disabled 분기 없음. 로그인 기능 자체가 없어 회원 셀렉트로 장바구니 소유자를 선택하는 방식(담기 폼과 동일 원칙, CartViewController.java:19-20 주석)"
anchors:
  - "src/main/resources/templates/cart/list.html"
  - "src/main/java/com/sm/lab/shop/controller/CartViewController.java:34-47"
  - "src/main/java/com/sm/lab/shop/controller/CartViewController.java:50-54"
  - "src/main/java/com/sm/lab/shop/controller/CartViewController.java:57-61"
revision_history:
  - version: 1.0
    date: 2026-08-23
    author: ddd-ui-agent (source-authority)
    change: 최초 생성 (SR-202, FUNC-order-011 구현 완료 후 역생성)
---

# UIS-ORD-005: 장바구니

> **근거 소스(권위):** `src/main/resources/templates/cart/list.html`(Thymeleaf 서버 렌더) +
> `src/main/java/com/sm/lab/shop/controller/CartViewController.java`. 스크린샷은 보조.

## 0. 화면 미리보기

![개요](preview.png)

> `select_tab_widgets.py` 전수 스캔 결과 **0건**(공통제외 0, 탭바제외 0) — 이 화면의 조회/변경/삭제
> 버튼이 전부 `id`·`onclick` 없는 순수 HTML `<form method="post"><button type="submit">` 네이티브 제출
> 방식이라(JS 바인딩 없음), 스크립트의 선택 기준("button/a + id 또는 onclick 보유")에 걸리지 않는다.
> 이는 결함이 아니라 **JS 트리거 없이 브라우저 기본 폼 제출만 쓰는 스택 패턴**이다(UIS-ORD-004와 같은
> 사유). 따라서 자동 마커·annotate를 생략하고, 아래 표는 **DOM 스냅샷의 위젯 번호(`n`, 1~9)** 를
> 그대로 참조 번호로 사용해 전수 문서화한다(위젯 개수 9 = 아래 4번 표 행 수, 누락 없음).

## 1. 화면 목적

회원별로 서버에 보관된 장바구니(품목·수량·합계)를 조회하고, 그 자리에서 수량을 바꾸거나 품목을
삭제한다. 로그인이 없어 상단 셀렉트로 조회 대상 회원을 고른다. 담기 자체(신규 추가)는 이 화면의
책임이 아니다 — 상품 상세 화면(UIS-ORD-004)에서 담고, 여기서는 이미 담긴 품목만 다룬다.

## 2. 주요 작업 시나리오

**시나리오: 회원별 장바구니 조회**
1. `/cart` 또는 `/cart?memberId=`로 진입한다. `memberId`를 안 주면 서버가 `MemberService.list()`의
   첫 회원을 기본 선택한다(`CartViewController#cart`).
2. 상단 회원 셀렉트(1)에서 다른 회원을 고르면 `onchange="this.form.submit()"`으로 **즉시 재조회**된다
   (`GET /cart?memberId=...`). [조회] 버튼(2)은 셀렉트를 안 바꾼 상태에서 폼을 다시 제출하고 싶을 때의
   보조 수단이다(JS로 이미 자동 제출되므로 평소엔 클릭할 일이 없다).
3. 품목 표에서 상품명·단가·수량·품목합계를 확인하고, 표 아래 총합계(모든 품목의 `lineTotal` 합)를
   확인한다.

**시나리오: 품목 수량 변경**
1. 해당 행의 수량 입력칸(예: (3) 또는 (6))을 원하는 값으로 고친다(`min="1"`).
2. 같은 행의 [변경] 버튼(예: (4) 또는 (7))을 누르면 `POST /cart/items/{sku}/update`가 `memberId`(hidden)
   와 `qty`를 실어 제출된다.
3. 서버(`CartViewController#updateQty` → `CartService.updateQty`)가 처리 후 `redirect:/cart?memberId=...`
   로 돌아온다(PRG 패턴 — 새로고침 시 중복 제출 없음).
4. 재조회된 화면에서 바뀐 수량·품목합계·총합계를 확인한다. `qty<1`이면 400, 재고 초과면 409로 거부되며
   두 경우 모두 사유 문구는 응답 본문에 담기지만 **이 화면 자체로 재렌더되지 않고 오류 페이지로 이탈**한다
   (8번 참조 — 알려진 한계).

**시나리오: 품목 삭제**
1. 해당 행의 [삭제] 버튼(예: (5) 또는 (8))을 누른다. **수량을 0으로 낮춰 사라지게 하는 방식이 아니라
   삭제는 반드시 이 버튼으로만** 한다(SR-202 D4 — 수량 1 미만은 거부하고 삭제 유도는 별도 버튼으로).
2. `POST /cart/items/{sku}/delete`가 `memberId`(hidden)를 실어 제출된다.
3. 서버(`CartViewController#delete` → `CartService.delete`)가 처리 후 `redirect:/cart?memberId=...`.
4. 재조회된 화면에서 해당 행이 사라지고 총합계가 다시 계산된다. 마지막 품목을 지우면 빈 상태로
   전환된다.

**시나리오: 빈 장바구니 → 상품 목록으로 이동**
1. 선택된 회원의 장바구니에 품목이 없으면 표·총합계 대신 "장바구니가 비어 있습니다" 문구와
   "상품 목록으로" 링크만 노출된다(SR-202 확정 `scr_empty_state`).
2. 링크(9)를 클릭하면 `GET /product/list`(UIS-ORD-003)로 이동해 담을 상품을 고른다.
3. 품목이 있는 상태에서도 표 아래에 별도의 "상품 목록으로" 링크가 한 번 더 노출된다(line 76) — 추가
   담기를 위해 목록으로 돌아가는 용도로 항상 존재.

## 3. 화면 구성 (블록)

| 블록 | 역할 | 주요 위젯 | 소스 근거 |
|------|------|----------|----------|
| 회원 선택 영역 | 조회 대상(장바구니 소유자) 전환 | (1) 회원 셀렉트, (2) 조회 버튼 | `list.html:28-37` |
| 품목 표 (품목 있음) | 상품명·단가·수량·품목합계 표시 + 행별 수량변경/삭제 | 수량 input + [변경]/[삭제] 버튼 (행마다 반복) | `list.html:39-65` |
| 총합계 | 전 품목 합계 표시 | 정적 텍스트(`cart.totalAmount`) | `list.html:66-68` |
| 빈 상태 안내 (품목 없음) | "장바구니가 비어 있습니다" + 이동 링크 | 정적 텍스트 + `a` | `list.html:70-74` |
| 하단 네비게이션 | 상품 목록으로 복귀(품목 있을 때도 항상 노출) | (9) `a` 링크 | `list.html:76` |

## 4. 위젯·액션

> 번호는 「0. 화면 미리보기」에서 설명한 대로 **DOM 스냅샷 위젯 번호(`n`)** 를 그대로 쓴다(annotate 마커
> 아님 — 자동 마커 스캔 0건). (3)(4)(5)는 "기계식 키보드" 행, (6)(7)(8)은 "4K 모니터" 행(캡처 시점 2품목
> 상태) — DOM에서 버튼 라벨이 상품명으로 잘못 캡처됐던 부분은 소스(`list.html:49-61`)를 읽어 실제
> 버튼 텍스트(변경/삭제)로 보정했다.

| 번호 | 위젯 | 타입 | 레이블 | 동작 | 연결 API(raw) | 결과 |
|---|------|------|--------|------|--------------|------|
| (1) | `#selMember` | select | 회원 (이름 (ID) 목록) | 회원 전환 시 `onchange`로 자동 제출 | GET /cart | 선택 회원의 장바구니로 재조회 |
| (2) | `#btnSelect` | button(submit) | 조회 | 회원 셀렉트 폼 수동 재제출(백업 수단) | GET /cart | 그리드 갱신(현재 선택 회원 기준) |
| (3) | qty input (기계식 키보드 행) | input(number, min=1) | 수량 | 값 수정 | — (같은 행 (4)의 파라미터로 제출) | — |
| (4) | 변경 버튼 (기계식 키보드 행) | button(submit) | 변경 | 수량변경 폼 제출(hidden memberId + qty) | POST /cart/items/{sku}/update | redirect:/cart → 수량·품목합계·총합계 갱신. 실패: 400(qty&lt;1)/409(재고초과) |
| (5) | 삭제 버튼 (기계식 키보드 행) | button(submit) | 삭제 | 명시적 삭제(D4 — 수량 0 유도 금지) | POST /cart/items/{sku}/delete | redirect:/cart → 해당 행 제거, 총합계 갱신 |
| (6) | qty input (4K 모니터 행) | input(number, min=1) | 수량 | 값 수정 | — (같은 행 (7)의 파라미터로 제출) | — |
| (7) | 변경 버튼 (4K 모니터 행) | button(submit) | 변경 | 수량변경 폼 제출 | POST /cart/items/{sku}/update | redirect:/cart → 갱신. 실패: 400/409 |
| (8) | 삭제 버튼 (4K 모니터 행) | button(submit) | 삭제 | 명시적 삭제(D4) | POST /cart/items/{sku}/delete | redirect:/cart → 행 제거 |
| (9) | `a[href=/product/list]` | a | 상품 목록으로 | 화면 이동 | GET /product/list | 상품 목록(UIS-ORD-003)으로 이동, 담기 재진입 |

## 5. 접근 권한·표시 조건

| 요소 | 표시 조건 | 근거 |
|------|----------|------|
| 회원 셀렉트 옵션 | `MemberService.list()` 전체 회원(권한 분기 없음) | `list.html:31-33`, `CartViewController.java:36` |
| 품목 표 + 총합계 | `cart.items`가 비어있지 않을 때만 렌더 | `list.html:39,66` |
| 빈 상태 안내("장바구니가 비어 있습니다") | `cart.items`가 비어있을 때만 렌더 | `list.html:71` |
| 하단 "상품 목록으로" 링크((9)) | `cart.items`가 비어있지 않을 때 별도로 다시 노출(빈 상태 링크와 별개 위치) | `list.html:76` |
| 각 행의 [변경]/[삭제] 폼 | 품목이 존재하는 행마다 항상 노출(권한·상태 분기 없음) | `list.html:47-62` |

## 6. 팝업·연계 화면

팝업 없음. 이동 대상만 기록.

| 트리거 위젯 | 팝업/연계 화면 | 연결 API/화면 (raw → INF) | 용도 |
|------------|--------------|--------------------------|------|
| (9) `a[href=/product/list]` (빈 상태·품목 있음 상태 공통) | 상품 목록 화면(UIS-ORD-003) | GET /product/list ← 화면 이동(INF 없음) | 목록에서 추가 상품 담기(UIS-ORD-004로 이어짐) |

## 7. 데이터 출처·연결

- **연결 API(raw → INF):**
  - `GET /cart` — 화면 자체 라우트(서버 렌더 진입점). INF 대상 아님(view 반환). 내부적으로
    `CartService.get(memberId)`를 **자바 메서드로 직접 호출**한다(HTTP 아님).
  - `POST /cart/items/{sku}/update`, `POST /cart/items/{sku}/delete` — 서버렌더 폼 제출(PRG,
    redirect 반환). JSON API가 아니라 INF 대상 아님. 내부적으로 `CartService.updateQty`/`CartService.delete`를
    자바 메서드로 직접 호출하며, 이는 **REST API 컨트롤러(`CartController`)와 동일 서비스 인스턴스를
    공유**한다 — 다만 이 화면은 그 REST API를 HTTP로 부르지 않는다. 같은 서비스가 노출하는 대응 REST
    엔드포인트: 담기 [[INF-ORD-010]](`POST /api/cart/items`), 조회 [[INF-ORD-011]](`GET /api/cart/`).
    **수량변경(`PATCH /api/cart/items/{sku}`)·삭제(`DELETE /api/cart/items/{sku}`)에 대응하는 REST
    INF는 이 시점에 아직 생성되지 않았다**(`docs/05_설계서/order/INF/`에 010·011만 존재) — 추후
    `/sl-recon-inf` 재실행 대상.
  - `GET /product/list` — 목록 화면 이동. INF 대상 아님.
- **참조 테이블(SCH):** `CART_ITEMS`, `PRODUCTS`, `MEMBERS`(INF-ORD-010/011 `tables` 기준) — 단
  **`CART_ITEMS`의 SCH 문서는 아직 생성되지 않았다**(`docs/05_설계서/order/SCH/`에 SCH-ORD-001~006만
  존재, CART_ITEMS 언급 0건) — `/sl-recon-sch` 재실행 대상. `PRODUCTS`는 [[SCH-ORD-005]].

## 8. 미확인 사항

- **REST API(`/api/cart*`)와 화면 폼(`/cart/items/{sku}/*`)은 별개 경로다.** 이 화면의 수량변경·삭제는
  브라우저가 직접 REST API(`PATCH`/`DELETE /api/cart/items/{sku}`)를 호출하는 게 아니라, 서버렌더
  폼이 `CartViewController`의 자체 라우트로 POST하고 그 안에서 `CartService`를 자바 메서드로 호출한다.
  REST API(담기 [[INF-ORD-010]], 조회 [[INF-ORD-011]])는 별도 클라이언트(외부 연동 등)를 위한 것으로
  보이며 같은 비즈니스 로직을 공유할 뿐 이 화면이 호출하는 경로가 아니다 — 두 계층을 혼동하지 않도록
  api_hints도 화면 경로만 담았다.
- **폼 오류 시 Whitelabel 이탈(QA r1~r3 CONCERNS, 잔존 이월):** `qty<1`(400) 또는 재고 초과(409)로
  수량변경이 실패하면 이 화면으로 재렌더되지 않고 Spring 기본 오류 페이지(Whitelabel)로 이동한다.
  사유 문구 자체는 응답 본문에 정상 전달되지만(`server.error.include-message: always`), 장바구니
  화면 컨텍스트는 유지되지 않는다. QA에서 권고로 남기고 수용됨(`STORY-FUNC-order-011.md` 후속
  추적 TODO) — 화면 재렌더+사유 표시로 개선하는 것은 이번 범위 밖.
- **CSRF 미방어(D3/QA 결정 수용):** `/cart/items/{sku}/update`·`/cart/items/{sku}/delete`는 앱
  최초의 상태변경 서버렌더 폼이며, 앱 전역에 spring-security가 없어 CSRF 토큰이 없다. 사람 코멘트로
  이번 SR 범위에서 명시 수용됐고 후속 SR/KNOWN_LIMITATIONS 등재가 후속 추적 TODO로 남아 있다
  (`STORY-FUNC-order-011.md` 참조).
- **수량 1 미만은 거부만, 삭제 유도 없음(D4):** `qty<1` 입력 시 자동으로 삭제 처리되지 않고 400으로
  거부된다 — 사용자가 품목을 없애려면 반드시 (5)/(8) [삭제] 버튼을 눌러야 한다. 소스 주석
  (`list.html:57`)에도 이 원칙이 명시돼 있다.
- **PATCH/DELETE REST INF 미생성, `CART_ITEMS` SCH 미생성:** 7번 참조 — 이번 UIS 작성 시점에는
  아직 없어 raw 경로/테이블명만 기록했다. 재수집 시 채워질 예정.
