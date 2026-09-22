---
inf-id: INF-ORD-011
method: GET
path: /api/cart/
domain: order
domain-code: ORD
srs-f: [TBD]
screens: ["UIS-ORD-004", "UIS-ORD-005"]
tables:
  - CART_ITEMS
  - PRODUCTS
  - MEMBERS
anchors:
  - src/main/java/com/sm/lab/shop/controller/CartController.java:28-32
  - src/main/java/com/sm/lab/shop/service/CartService.java:66-72
  - src/main/java/com/sm/lab/shop/dao/CartDao.java:16-17
  - src/main/java/com/sm/lab/shop/dao/MemberDao.java:12
  - src/main/resources/mapper/cart.xml:15-22
  - src/main/resources/mapper/member.xml:14-19
---

# INF-ORD-011: GET /api/cart/ — 장바구니 조회

> **개요:** 회원의 장바구니 품목 전체와 총액을 조회한다.

> **근거 소스:** `src/main/java/com/sm/lab/shop/controller/CartController.java:28-32`

## 요청

- Method: GET
- Path: /api/cart/
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| memberId | Query | string | Y | 회원 ID |

## 응답 (200 OK)

```json
{
  "items": [
    {
      "memberId": "string",
      "sku": "string",
      "productName": "string",
      "price": 0,
      "qty": 0,
      "addedAt": "2026-08-23T10:00:00",
      "lineTotal": 0
    }
  ],
  "totalAmount": 0
}
```

## 비즈니스 규칙

- 품목은 담은 순(`added_at, sku`)으로 정렬된다
- `totalAmount`는 각 품목의 `price × qty`(lineTotal) 합계로, 조회 시점에 애플리케이션에서 계산한다(저장 컬럼 아님)

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 404 | 회원 없음 | memberId로 조회된 회원 없음 |

## 참조 테이블

| 테이블 | SCH |
|--------|-----|
| CART_ITEMS | [[SCH-ORD-006]] |
| PRODUCTS | [[SCH-ORD-005]] |
| MEMBERS | [[SCH-ORD-001]] |

## curl 예시

```bash
curl -X GET "/api/cart/?memberId=M001" \
  -H "Content-Type: application/json"
```
