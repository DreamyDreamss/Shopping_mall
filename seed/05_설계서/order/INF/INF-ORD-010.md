---
inf-id: INF-ORD-010
method: POST
path: /api/cart/items
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables:
  - CART_ITEMS
  - PRODUCTS
  - MEMBERS
anchors:
  - src/main/java/com/sm/lab/shop/controller/CartController.java:22-26
  - src/main/java/com/sm/lab/shop/service/CartService.java:43-64
  - src/main/java/com/sm/lab/shop/dao/CartDao.java:13-14
  - src/main/java/com/sm/lab/shop/dao/CartDao.java:21-26
  - src/main/java/com/sm/lab/shop/dao/MemberDao.java:12
  - src/main/java/com/sm/lab/shop/dao/ProductDao.java:15
  - src/main/resources/mapper/cart.xml:6-13
  - src/main/resources/mapper/cart.xml:29-36
  - src/main/resources/mapper/member.xml:14-19
  - src/main/resources/mapper/product.xml:18-22
---

# INF-ORD-010: POST /api/cart/items — 장바구니 담기

> **개요:** 회원 장바구니에 상품을 담는다. 같은 상품을 재담기하면 기존 수량에 합산된다.

> **근거 소스:** `src/main/java/com/sm/lab/shop/controller/CartController.java:22-26`

## 요청

- Method: POST
- Path: /api/cart/items
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| memberId | Body | string | Y | 회원 ID |
| sku | Body | string | Y | 상품 SKU |
| qty | Body | int | Y | 담을 수량 (1 이상) |

## 응답 (200 OK)

```json
{
  "memberId": "string",
  "sku": "string",
  "productName": "string",
  "price": 0,
  "qty": 0,
  "addedAt": "2026-08-23T10:00:00",
  "lineTotal": 0
}
```

## 비즈니스 규칙

- 같은 상품 재담기는 DB 원자 UPSERT(`INSERT ... ON DUPLICATE KEY UPDATE qty = qty + VALUES(qty)`)로 수량 합산(PK `member_id,sku`) — 동시 담기에도 lost update·PK 중복 500 없음
- 판매중지(`sale_yn='N')` 상품은 담기 거부(409)
- 품절(`stock_qty=0`)은 UPSERT 이전에 조기 거부(409) — 합산·원복 사이클 자체가 불필요
- UPSERT 직후 최종(합산) qty가 재고를 초과하면, 방금 더한 만큼만 되돌리고(원복값 0 이하면 행 삭제) 409 — UPSERT가 잡은 행 잠금이 트랜잭션 종료까지 유지되어 판정·원복 사이 다른 트랜잭션 개입 불가
- 담기는 재고를 차감하지 않는다(보관 전용)

## 트랜잭션 순서

1. CART_ITEMS UPSERT(수량 합산)
2. CART_ITEMS 병합 결과 SELECT(상품명·현재 단가 조인)
3. (재고 초과 시) CART_ITEMS 원복 — 복원값 > 0이면 UPDATE, 0 이하면 DELETE

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 400 | 유효성 실패 | qty < 1 |
| 404 | 회원 없음 | memberId로 조회된 회원 없음 |
| 404 | 상품 없음 | sku로 조회된 상품 없음 |
| 409 | 판매중지 상품 | 상품의 sale_yn = 'N' |
| 409 | 품절 상품 | 상품의 stock_qty = 0 |
| 409 | 재고 초과 | 합산된 qty가 가용 재고 초과 |

## 참조 테이블

| 테이블 | SCH |
|--------|-----|
| CART_ITEMS | [[SCH-ORD-006]] |
| PRODUCTS | [[SCH-ORD-005]] |
| MEMBERS | [[SCH-ORD-001]] |

## curl 예시

```bash
curl -X POST /api/cart/items \
  -H "Content-Type: application/json" \
  -d '{"memberId":"M001","sku":"SKU001","qty":2}'
```
