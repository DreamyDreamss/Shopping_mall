---
inf-id: INF-ORD-014
method: POST
path: /api/cart/checkout
domain: order
domain-code: ORD
srs-f: [TBD]
screens: ["UIS-ORD-005"]
tables:
  - CART_ITEMS
  - MEMBERS
  - PRODUCTS
  - ORDERS
  - ORDER_ITEMS
anchors:
  - src/main/java/com/sm/lab/shop/controller/CartController.java:53-56
  - src/main/java/com/sm/lab/shop/service/CartService.java:130-158
  - src/main/java/com/sm/lab/shop/service/CartService.java:166-179
  - src/main/java/com/sm/lab/shop/service/OrderService.java:137-166
  - src/main/java/com/sm/lab/shop/dao/CartDao.java:32-40
  - src/main/java/com/sm/lab/shop/dao/MemberDao.java:12
  - src/main/java/com/sm/lab/shop/dao/ProductDao.java:15-16
  - src/main/java/com/sm/lab/shop/dao/OrderDao.java:36-37
  - src/main/resources/mapper/cart.xml:54-71
  - src/main/resources/mapper/member.xml:14-19
  - src/main/resources/mapper/product.xml:18-29
  - src/main/resources/mapper/order.xml:90-98
---

# INF-ORD-014: POST /api/cart/checkout — 장바구니 체크아웃

> **개요:** 장바구니 전체 품목을 기존 주문 생성 규칙으로 전환해 주문을 만들고 장바구니를 비운다.

> **근거 소스:** `src/main/java/com/sm/lab/shop/controller/CartController.java:53-56`

## 요청

- Method: POST
- Path: /api/cart/checkout
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| memberId | Body | string | Y | 회원 ID |

```json
{
  "memberId": "M001"
}
```

## 응답 (200 OK)

```json
{
  "orderNo": "20260823-0101",
  "totalAmount": 0,
  "itemCount": 0
}
```

## 비즈니스 규칙

- 주문 생성은 새 규칙을 만들지 않고 `OrderService.create`를 그대로 재사용한다(채번·PLACED 상태·재고 차감 — 검증/차감 로직 미복제)
- 장바구니 조회는 `SELECT ... FOR UPDATE`로 잠근다 — 동일 회원의 동시 체크아웃에서 스냅샷이 아닌 최신 커밋 데이터를 잠그므로, 중복 주문이 발생하지 않는다
- 잠금 직후 전 품목을 사전 스윕하여 상품없음·판매중지(`sale_yn='N'`)·재고부족을 한 번에 모아 409 사유 목록 하나로 응답한다(품목별 사유, 부분 주문 없이 전량 거부)
- 사전 스윕을 통과해도 `OrderService.create`의 실제 재고 차감(`decreaseStock`) 실패가 최종 판정이다(스윕-차감 사이 시점 차 대비)
- 장바구니는 주문 생성이 트랜잭션 내에서 확정된 뒤에만 비운다(실패 시 품목 보존)

## 트랜잭션 순서

1. `CART_ITEMS` 잠금 조회(`FOR UPDATE`) — 비어 있으면 400
2. 품목별 `PRODUCTS` 사전 스윕(상품없음/판매중지/재고부족) — 이슈 있으면 409
3. `OrderService.create` 위임: `PRODUCTS.stock_qty` 차감 → `ORDERS` 1건 INSERT → `ORDER_ITEMS` N건 INSERT (동일 트랜잭션 참여)
4. `CART_ITEMS` 전체 삭제 (주문 확정 후)

## 사이드이펙트

- 체크아웃 성공 시 품목별로 `PRODUCTS.stock_qty`가 차감된다(`OrderService.create` → `decreaseStock`)
- 체크아웃 성공 시 `CART_ITEMS`에서 해당 회원의 품목이 전부 삭제된다(`deleteAllItems`)

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 400 | 빈 장바구니 | memberId의 장바구니에 품목 없음 |
| 400 | 판매 중인 상품이 아님 | `OrderService.create` 내부 판정 — 사전 스윕 통과 후 시점 차로 발생 가능 |
| 404 | 회원 없음 | memberId로 조회된 회원 없음 |
| 409 | 재고 부족/판매중지/상품없음 | 사전 스윕에서 품목별 사유 목록으로 응답(가용재고/요청수량 포함) |
| 409 | 재고 부족(차감 실패) | `OrderService.create`의 `decreaseStock` 0건 — 스윕 이후 경합 |

## 참조 테이블

- `CART_ITEMS`
- `MEMBERS`
- `PRODUCTS`
- `ORDERS`
- `ORDER_ITEMS`

## curl 예시

```bash
curl -X POST /api/cart/checkout \
  -H "Content-Type: application/json" \
  -d '{"memberId": "M001"}'
```
