# SRS — order 도메인

> 화면 1개 = SRS-F 1건. 합성형 6섹션(기능 요약·업무 흐름·비즈니스 규칙 종합·예외 제약·연관 산출물·데이터 영향).
> ③⑥은 UIS/INF/SCH 사실을 종합한 것이며, 출처를 병기했다.

---

## SRS-F-001: 주문 목록 조회  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-order-001](../../00_FUNC/FUNC_MAP.md) · 화면 [UIS-ORD-001](../../05_설계서/order/UIS/UIS-ORD-001_주문목록/spec.md) · API 1개 · 테이블 2개

### 기능 요약

업무 담당자가 회원 ID·주문상태 조건으로 전체 주문 현황을 한눈에 조회하기 위한 기능이다. 조건을
비우면 전 회원의 주문을 대상으로 하며, 등록·수정 기능 없이 순수 조회·상세 이동 전용으로
동작한다. 목록에서 특정 주문번호를 선택하면 SRS-F-002(주문 상세)로 이어지는, 주문 업무의
진입점 화면이다.

### 업무 흐름

1. 사용자가 주문 목록 화면(`/order/list`)에 진입한다.
2. (선택) 「회원 ID」·「주문상태」 조건을 입력하고 「검색」을 클릭하면 화면이 동일 라우트로
   재요청되어 조건에 맞는 목록을 다시 렌더한다 — 개념적으로 `GET /api/orders/`(INF-ORD-003)가
   수행하는 것과 동일한 조회(ORDERS·MEMBERS 조인, `del_yn='N'` 상시필터)를 서버가 내부적으로
   수행한다(화면 자체는 서버 렌더 라우트라 REST INF로 직접 치환되지는 않음 — UIS §7).
3. 목록의 각 행(주문번호/회원/상태/총액/주문일시)을 확인한다.
4. 특정 주문번호를 클릭하면 `GET /order/{orderNo}`로 이동해 SRS-F-002(주문 상세) 화면을 연다.

### 비즈니스 규칙  (종합)

- `del_yn = 'N'` 상시필터 적용 → 논리삭제된 주문은 목록에서 제외 (근거: INF-ORD-003 비즈니스 규칙,
  SCH-ORD-004 비즈니스 주의사항)
- `memberId` 입력 시 해당 회원 주문만, `orderState` 선택 시 해당 상태만 필터 (근거: INF-ORD-003)
- `ORDERS`·`MEMBERS` 조인 시 회원 탈퇴 여부와 무관하게 항상 조인 → 탈퇴 회원의 과거 주문도 목록에
  표시됨 (근거: INF-ORD-003, SCH-ORD-004 "member_id 조인 시 회원이 탈퇴했어도 주문 기록은 유지")
- 코드값: `order_state` — PLACED=주문접수, PAID=결제완료, SHIPPED=배송중,
  PARTIAL_SHIPPED=부분배송, CANCELED=주문취소, DONE=주문완료 (출처 SCH-ORD-004)
- 페이지 파라미터(`page`)는 서버가 받지만(기본 1) 화면에는 페이지네이션 컨트롤이 없다 —
  현재 뷰에서는 첫 페이지 결과만 노출된다 (근거: UIS-ORD-001 §8)

### 예외·제약

- 권한 게이팅 없음 — 화면 진입·검색·상세 이동 모두 조건 없이 항상 가능 (근거: UIS-ORD-001 §5)
- 검색 조건을 모두 비우면 전체 회원 대상으로 조회된다(기본 동작)
- (REST 계층 기준) 파라미터 타입 오류 시 400, 인증 없음/만료 시 401 — 단 이 화면 자체는 인증
  분기가 없어 실제 발생 지점은 REST API 계층에 한정된다 (근거: INF-ORD-003 오류 응답)

### 연관 산출물  (funcs_index 기계 조립)

- 화면: [UIS-ORD-001](../../05_설계서/order/UIS/UIS-ORD-001_주문목록/spec.md)
- 호출 API: [INF-ORD-003](../../05_설계서/order/INF/INF-ORD-003.md) ×1 (GET /api/orders/ — 주문 목록 조회)
- 관련 테이블: [SCH-ORD-004](../../05_설계서/order/SCH/SCH-ORD-004.md)(orders) ·
  [SCH-ORD-001](../../05_설계서/order/SCH/SCH-ORD-001.md)(members) ×2

### 데이터 영향

- 조회(SELECT) 전용 — `orders`, `members`. 이 기능으로 생성/수정/삭제되는 데이터는 없다.

---

## SRS-F-002: 주문 상세 조회·취소  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-order-002](../../00_FUNC/FUNC_MAP.md) · 화면 [UIS-ORD-002](../../05_설계서/order/UIS/UIS-ORD-002_주문상세/spec.md) · API 2개 · 테이블 5개

### 기능 요약

업무 담당자가 주문 1건의 상태·회원·총액·상품 라인·배송 이력을 한 화면에서 확인하고, 아직
출고되지 않은 주문이라면 그 자리에서 즉시 취소할 수 있게 하는 기능이다. "조회"와 "취소(상태
전이 + 재고 원복)"라는 두 업무를 하나의 화면이 함께 수행한다.

### 업무 흐름

**조회:** 사용자가 주문 목록에서 주문번호를 클릭하거나 `/order/{orderNo}`로 직접 진입하면,
서버가 주문 1건을 상품 라인·배송 이력까지 포함해 조회해 화면에 렌더한다(개념적으로
`GET /api/orders/{orderNo}`(INF-ORD-004)와 동일하게 `ORDERS`·`MEMBERS`·`ORDER_ITEMS`·
`PRODUCTS`·`ORDER_DELIVERY`를 조인). 배송 이력이 없으면 "배송 없음" 1행이 표시된다.

**취소:** 사용자가 「주문 취소」 버튼을 클릭하면(별도 확인 다이얼로그 없이) 즉시
`PATCH /api/orders/{orderNo}/cancel`(INF-ORD-006)이 호출된다. 서버는 취소 가능 상태인지
확인한 뒤 주문 상태를 `CANCELED`로 전이하고, 주문 라인 수량만큼 상품 재고를 원복한다.
완료되면 화면이 새로고침되어 갱신된 상태를 반영한다.

### 비즈니스 규칙  (종합)

- `del_yn = 'N'` 상시필터 → 삭제된 주문 조회 시 404 (근거: INF-ORD-004)
- 취소 가능 조건: ①현재 상태가 `CANCELED`/`DONE`이 아니어야 하고, ②배송 중 하나라도
  `SHIPPED`/`DELIVERED` 상태면 취소 불가(`ORD-4001`) (근거: INF-ORD-006 비즈니스 규칙)
- 동시성 가드: 상태 전이는 조건부 UPDATE(`WHERE order_state NOT IN ('CANCELED','DONE')`)로
  **먼저** 확정하고, 0행이면 동시 요청이 선점한 것으로 보고 재고 원복 없이 즉시 409 처리한다
  (근거: INF-ORD-006 — LAB-102 QA r1 레이스 가드)
- 상태 전이 확정 후 라인별(`ORDER_ITEMS`) 수량만큼 `PRODUCTS.stock_qty`를 증가시켜 원복한다
  (근거: INF-ORD-006 트랜잭션 순서)
- 코드값: `order_state`(6종, 출처 SCH-ORD-004) · `delivery_state` — READY=배송준비,
  SHIPPED=배송중, DELIVERED=배송완료, CANCELED=배송취소 (출처 SCH-ORD-002)
- 배송이 아직 생성되지 않은 주문이 대부분이며, `READY` 상태일 때는 `invoice_no`·`shipped_at`이
  NULL이다 (근거: SCH-ORD-002 비즈니스 주의사항)

### 예외·제약

- 「주문 취소」 버튼에 확인(confirm) 다이얼로그가 없어 클릭 즉시 요청이 전송된다 — 의도된 UX인지
  미확인 (근거: UIS-ORD-002 §8)
- 화면 소스에는 주문 상태에 따른 버튼 비활성화 분기가 없다 — 이미 취소된 주문에서도 버튼이 그대로
  노출되며, 실제 취소 가능 여부는 서버(INF-ORD-006) 상태 검증에 전적으로 의존한다 (근거:
  UIS-ORD-002 §8)
- 주문 없음(`orderNo` 오류) 시 404 (근거: INF-ORD-004, INF-ORD-006)
- 권한 게이팅 없음 — 화면 진입·버튼 노출 모두 조건 없이 항상 표시 (근거: UIS-ORD-002 §5)

### 연관 산출물  (funcs_index 기계 조립)

- 화면: [UIS-ORD-002](../../05_설계서/order/UIS/UIS-ORD-002_주문상세/spec.md)
- 호출 API: [INF-ORD-004](../../05_설계서/order/INF/INF-ORD-004.md) ×1 (GET /api/orders/{orderNo} — 상세 조회),
  [INF-ORD-006](../../05_설계서/order/INF/INF-ORD-006.md) ×1 (PATCH /api/orders/{orderNo}/cancel — 취소) — 총 2개
- 관련 테이블: [SCH-ORD-004](../../05_설계서/order/SCH/SCH-ORD-004.md)(orders) ·
  [SCH-ORD-001](../../05_설계서/order/SCH/SCH-ORD-001.md)(members) ·
  [SCH-ORD-003](../../05_설계서/order/SCH/SCH-ORD-003.md)(order_items) ·
  [SCH-ORD-005](../../05_설계서/order/SCH/SCH-ORD-005.md)(products) ·
  [SCH-ORD-002](../../05_설계서/order/SCH/SCH-ORD-002.md)(order_delivery) ×5

### 데이터 영향

- 조회(SELECT) — `orders`, `members`, `order_items`, `products`, `order_delivery`
- 취소 시(UPDATE) — `orders.order_state` → `CANCELED`, `products.stock_qty` → 라인 수량만큼 증가(재고 원복)

## SRS-F-003: 회원 상세 조회  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-order-004](../../00_FUNC/FUNC_MAP.md) · SR: SR-207 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-order-004.md`

### 기능 요약

회원 상세 조회

### 수용 기준(STORY에서)

- INF-ORD-002: 요청: 변경 없음(필드 추가만 하기로 확정 문답에서 답변됐으나, 이번 SR 범위인 회원 상세 화면·최근 주문 5건 표시에는 요청 파라미터 추가 요구가 없음).
- INF-ORD-002: 응답(200): 필드 구성·타입·정렬·5건 고정·취소 주문 포함 규칙 모두 **그대로 유지**(요구사항 "API 응답 계약은 바꾸지 않는다", 확정 문답 `api_compat`: "필요 — 필드 추가만"이나 이번 SR에서 실제 필드 추가 요구는 없음).
- INF-ORD-002: 비즈니스 규칙: 변경 없음.
- INF-ORD-002: 오류: 변경 없음(확정 문답 `api_error`: 기존 오류 계약 그대로 — 404/401/5xx 공통 배너, 신규 오류 코드 없음).
- INF-ORD-002: 화면 연결: `screens: []` → 신규 화면(회원 상세/마이페이지, UIS 신규 채번, ID [미상])이 이 API를 소비하도록 **추가**. INF-ORD-002 본문의 `screens` 필드 갱신 필요(스펙 현행화는 본 변경명세의 스코프 밖 — 승인 후 AIDD 재동기화에서 처리).
- 신규: 데이터 소스: INF-ORD-002 응답의 `recentOrders`를 그대로 사용(신규 API 없음).
- 신규: 표시 항목: 최근 주문 5건을 최신순(API 정렬 그대로: `ordered_at DESC, order_no DESC`)으로 나열 — 항목별 `orderNo`(주문번호), `orderedAt`(일시), `orderState`(상태), `totalAmount`(금액).
- 신규: 빈 상태: `recentOrders`가 빈 배열이면 "최근 주문 없음" 문구 표시.

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-004: 판매중 상품 목록 조회  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-order-007](../../00_FUNC/FUNC_MAP.md) · SR: SR-214 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-order-007.md`

### 기능 요약

판매중 상품 목록 조회

### 수용 기준(STORY에서)

- INF-ORD-008 (GET /api/products): `sale_yn = 'Y'` 상시필터 → 판매중 상품만 반환(단건 조회인 [[INF-ORD-009]]는 이 필터가 없음)
- INF-ORD-008 (GET /api/products): [반영: FUNC-order-007] `keyword` 파라미터가 있으면 `product_name` 부분 일치(LIKE) 검색 —
- INF-ORD-008 (GET /api/products): [반영: FUNC-order-007](SR-220) `inStock=true`면 `stock_qty >= 1` 조건을 `sale_yn = 'Y'` 상시필터와
- 컨트롤러/핸들러
- 서비스/비즈니스 로직
- 데이터 접근 레이어
- 단위 테스트

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-005: 상품 상세  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-order-010](../../00_FUNC/FUNC_MAP.md) · SR: SR-216 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-order-010.md`

### 기능 요약

상품 상세

### 수용 기준(STORY에서)

- INF-ORD-009 (GET /api/products/{sku}): 목록 조회([[INF-ORD-008]])와 달리 `sale_yn` 필터가 없다 — 판매종료 상품도 SKU를 알면 조회 가능 (기존 ProductController/ProductService/ProductDao — 미변경, 검증만)
- INF-ORD-010 (POST /api/cart/items): 같은 상품 재담기는 DB 원자 UPSERT(`INSERT ... ON DUPLICATE KEY UPDATE qty = qty + VALUES(qty)`)로 수량 합산(PK `member_id,sku`) — 동시 담기에도 lost update·PK 중복 500 없음 (기존 CartService.addItem/CartDao.upsertMergeQty — FUNC-order-011/012 소유, 미변경·재사용만)
- INF-ORD-010 (POST /api/cart/items): 판매중지(`sale_yn='N')` 상품은 담기 거부(409) (기존 CartService.requireOnSale — 미변경)
- INF-ORD-010 (POST /api/cart/items): 품절(`stock_qty=0`)은 UPSERT 이전에 조기 거부(409) — 합산·원복 사이클 자체가 불필요 (기존 CartService.addItem — 미변경)
- INF-ORD-010 (POST /api/cart/items): UPSERT 직후 최종(합산) qty가 재고를 초과하면, 방금 더한 만큼만 되돌리고(원복값 0 이하면 행 삭제) 409 — UPSERT가 잡은 행 잠금이 트랜잭션 종료까지 유지되어 판정·원복 사이 다른 트랜잭션 개입 불가 (기존 CartService.revertMerge — 미변경)
- INF-ORD-010 (POST /api/cart/items): 담기는 재고를 차감하지 않는다(보관 전용) (기존 구현 — 미변경)
- INF-ORD-011 (GET /api/cart/): 품목은 담은 순(`added_at, sku`)으로 정렬된다 (기존 CartDao.selectItems 매퍼 — 미변경, 이 화면(product/detail)에서는 직접 소비하지 않음)
- INF-ORD-011 (GET /api/cart/): `totalAmount`는 각 품목의 `price × qty`(lineTotal) 합계로, 조회 시점에 애플리케이션에서 계산한다(저장 컬럼 아님) (기존 CartService.get — 미변경)

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-006: 장바구니  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-order-011](../../00_FUNC/FUNC_MAP.md) · SR: SR-224 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-order-011.md`

### 기능 요약

장바구니

### 수용 기준(STORY에서)

- INF-ORD-010 (POST /api/cart/items): 같은 상품 재담기는 DB 원자 UPSERT(`INSERT ... ON DUPLICATE KEY UPDATE qty = qty + VALUES(qty)`)로 수량 합산(PK `member_id,sku`) — 동시 담기에도 lost update·PK 중복 500 없음
- INF-ORD-010 (POST /api/cart/items): 판매중지(`sale_yn='N')` 상품은 담기 거부(409)
- INF-ORD-010 (POST /api/cart/items): 품절(`stock_qty=0`)은 UPSERT 이전에 조기 거부(409) — 합산·원복 사이클 자체가 불필요
- INF-ORD-010 (POST /api/cart/items): UPSERT 직후 최종(합산) qty가 재고를 초과하면, 방금 더한 만큼만 되돌리고(원복값 0 이하면 행 삭제) 409 — UPSERT가 잡은 행 잠금이 트랜잭션 종료까지 유지되어 판정·원복 사이 다른 트랜잭션 개입 불가
- INF-ORD-010 (POST /api/cart/items): 담기는 재고를 차감하지 않는다(보관 전용)
- INF-ORD-011 (GET /api/cart/): 품목은 담은 순(`added_at, sku`)으로 정렬된다
- INF-ORD-011 (GET /api/cart/): `totalAmount`는 각 품목의 `price × qty`(lineTotal) 합계로, 조회 시점에 애플리케이션에서 계산한다(저장 컬럼 아님)
- INF-ORD-012 (PATCH /api/cart/items/{sku}): qty < 1은 400으로 거부한다 — 삭제는 명시적 DELETE로만 수행(실수 삭제 방지)

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-007: 체크아웃 (장바구니→주문 전환, INF-ORD-014 신규)  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-order-012](../../00_FUNC/FUNC_MAP.md) · SR: SR-203 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-order-012.md`

### 기능 요약

체크아웃 (장바구니→주문 전환, INF-ORD-014 신규)

### 수용 기준(STORY에서)

- POST /api/cart/checkout {memberId}: 200 {orderNo,totalAmount,itemCount} — 주문은 **기존 OrderService 규칙 재사용**(채번·PLACED·decreaseStock, 신규 규칙 금지) (TC-FUNC-order-012-01)
- 성공 시 장바구니 비움·재고 차감·ORDER_ITEMS 일치 (TC-02)
- 400 빈 장바구니 / 404 회원 없음 (TC-03/04)
- 409 재고 부족 — 부족 품목별 사유·**전량 거부**(주문 0·차감 0·장바구니 보존) (TC-05)
- **원자성**: 다품목 중 후순위 부족이어도 선순위 차감·주문 반영 없음(단일 트랜잭션 롤백) (TC-06)
- 화면: 품목 있으면 [주문하기] 노출·빈 장바구니 미노출 (TC-07/08), 성공 redirect /order/{orderNo} (TC-09), 실패 redirect /cart+flash 사유·보존 (TC-10) — PRG·파서주석·4xx만 흡수(docs/project-context.md 준수)
- 회귀: 기존 POST /api/orders·장바구니 CRUD·주문 조회 무변경 (TC-11~13)
- 컨트롤러/핸들러

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-008: 주문 목록 CSV 내보내기  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-order-013](../../00_FUNC/FUNC_MAP.md) · SR: SR-223 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-order-013.md`

### 기능 요약

주문 목록 CSV 내보내기

### 수용 기준(STORY에서)

- INF-ORD-015 (GET /api/orders/export): 조회 규칙은 [[INF-ORD-003]]과 완전히 동일(`OrderDao.selectOrders` 재사용) — 별도 조회 규칙 없음. `del_yn = 'N'` 상시필터 포함
- INF-ORD-015 (GET /api/orders/export): 페이징 미적용: `offset=0`, `size=Integer.MAX_VALUE`로 고정 호출(전체 건 CSV화)
- INF-ORD-015 (GET /api/orders/export): `orderedAt`은 `ISO_LOCAL_DATE_TIME` 고정 패턴으로 직렬화(가변폭 금지)
- INF-ORD-015 (GET /api/orders/export): CSV 필드는 RFC 4180 인용 규칙 적용(콤마·큰따옴표·개행 포함 시 큰따옴표로 감싸고 내부 큰따옴표는 이중화)
- INF-ORD-015 (GET /api/orders/export): CSV 수식 인젝션 방어: 필드 선두 문자가 `=`,`+`,`-`,`@`이면 작은따옴표(`'`)를 앞에 붙여 Excel이 수식으로 해석하지 않게 함(D11)
- INF-ORD-015 (GET /api/orders/export): 파일 본문은 UTF-8 BOM 프리픽스 포함(Excel에서 한글 깨짐 방지)
- 컨트롤러/핸들러
- 서비스/비즈니스 로직

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)
