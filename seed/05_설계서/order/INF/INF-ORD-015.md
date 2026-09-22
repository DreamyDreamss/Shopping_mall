---
inf-id: INF-ORD-015
method: GET
path: /api/orders/export
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables:
  - ORDERS
  - MEMBERS
anchors:
  - src/main/java/com/sm/lab/shop/controller/OrderController.java:79-90
  - src/main/java/com/sm/lab/shop/service/OrderService.java:77-104
  - src/main/java/com/sm/lab/shop/dao/OrderDao.java:25-27
  - src/main/resources/mapper/order.xml:15-34
  - src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:180-183
  - src/main/java/com/sm/lab/shop/web/OrderApiExceptionHandler.java:24-30
---

# INF-ORD-015: GET /api/orders/export — 주문 목록 CSV 내보내기

> **개요:** [[INF-ORD-003]]과 동일한 필터·정렬·상시필터로 조회한 주문 목록을 CSV 파일로 내려받는다. (linked_func: FUNC-order-013, LAB-104)

> **근거 소스:** `src/main/java/com/sm/lab/shop/controller/OrderController.java:79-90`

## 요청

- Method: GET
- Path: /api/orders/export
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| memberId | Query | string | N | 회원 ID 필터 |
| orderState | Query | string | N | 주문상태 필터(정본) |
| status | Query | string | N | 주문상태 필터 별칭 — `orderState`와 동시에 오면 `orderState` 우선(D11, QA r1 FAIL 정정) |

## 응답 (200 OK)

`Content-Type: text/csv; charset=UTF-8`, `Content-Disposition: attachment; filename="orders_{yyyyMMdd_HHmmss}.csv"`

UTF-8 BOM(`EF BB BF`)으로 시작하는 CSV 바이너리. 헤더 행 + [[INF-ORD-003]] 목록 조회 결과를 행 단위로 직렬화(페이징 미적용 — 전체 건).

```csv
주문번호,회원,상태,금액,주문일시
2026082300001-0101,M0001,PLACED,15000,2026-08-23T10:00:00
```

> **헤더 라벨(SR-223)**: 데이터 컬럼 순서·필드명·타입·의미(`orderNo,memberId,status,totalAmount,orderedAt`)는
> 그대로다. 헤더 텍스트만 같은 위치의 한글 라벨로 1:1 치환했다(주문번호↔orderNo, 회원↔memberId,
> 상태↔status, 금액↔totalAmount, 주문일시↔orderedAt). 근거: `docs/변경관리/SR-223/00_요구사항.md`
> 확정 문답 `header_label_order`.

## 비즈니스 규칙

- 조회 규칙은 [[INF-ORD-003]]과 완전히 동일(`OrderDao.selectOrders` 재사용) — 별도 조회 규칙 없음. `del_yn = 'N'` 상시필터 포함
- 페이징 미적용: `offset=0`, `size=Integer.MAX_VALUE`로 고정 호출(전체 건 CSV화)
- `orderedAt`은 `ISO_LOCAL_DATE_TIME` 고정 패턴으로 직렬화(가변폭 금지)
- CSV 필드는 RFC 4180 인용 규칙 적용(콤마·큰따옴표·개행 포함 시 큰따옴표로 감싸고 내부 큰따옴표는 이중화)
- CSV 수식 인젝션 방어: 필드 선두 문자가 `=`,`+`,`-`,`@`이면 작은따옴표(`'`)를 앞에 붙여 Excel이 수식으로 해석하지 않게 함(D11)
- 파일 본문은 UTF-8 BOM 프리픽스 포함(Excel에서 한글 깨짐 방지)

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 400 | 유효성 실패 | 파라미터 타입 오류 |
| 401 | 인증 실패 | 토큰 없음/만료 |

## 인증·인가 (SR-204 — 전역 정책)
> 이 오류 계약은 `/api/**` 전역에 적용되는 횡단 정책이다(구현: `ApiKeyAuthFilter`). 화면
> 라우트(/product/**·/cart·/order/**)는 무인증 유지. 개별 엔드포인트 INF는 이 정책을 상속한다.

| 조건 | 상태 | 본문 |
|------|------|------|
| `X-Api-Key` 미제시/무효 | 401 | `{"error":"unauthorized"}` |
| member 스코프 키로 타 회원 자원 접근(소유권 불일치) | 403 | `{"error":"forbidden"}` |
| 회원 목록·주문 export 등 관리 자원에 member 키 | 403 | admin 키 전용 |

- 키 매핑은 앱 설정 `lab.api-keys`(admin→`*`, member→회원ID). 경로 판정은 서블릿 정규화 경로
  기준(퍼센트 인코딩·matrix 파라미터 우회 차단, default-deny). 소유권은 자원의 실제 member_id를
  DB 대조(orders/{orderNo}) — 토큰 문자열 대조가 아니다(SR-204 D15).

## 반출 상한 (SR-204 R-3)
- 결과 1000행 초과 → **413** `{"error":"payload_too_large","limit":1000}` (부분 반출 위장 없이 전량 거부, CSV 조립 전 판정). 이 엔드포인트는 **admin 키 전용**.

## 참조 테이블

- ORDERS
- MEMBERS

## curl 예시

```bash
curl -X GET "/api/orders/export?memberId=M0001&orderState=PLACED" \
  -o orders.csv
```
