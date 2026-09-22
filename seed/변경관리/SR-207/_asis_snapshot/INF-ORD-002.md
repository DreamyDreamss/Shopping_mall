---
inf-id: INF-ORD-002
name: 회원 상세 조회
layer: api
method: GET
path: /api/members/{memberId}
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables:
  - MEMBERS
  - ORDERS
anchors:
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberController.java:27-31
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberService.java:28-36
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberDao.java:12
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/OrderDao.java:32
  - modules/shop-api/src/main/resources/mapper/member.xml:14-19
  - modules/shop-api/src/main/resources/mapper/order.xml:62-71
---

# INF-ORD-002: GET /api/members/{memberId} — 회원 상세 조회

> **개요:** 회원 상세 정보와 함께 최근 주문 5건 요약을 반환한다. (linked_func: FUNC-order-004, LAB-103)

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberController.java:27-31`

## 요청

- Method: GET
- Path: /api/members/{memberId}
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| memberId | PathVariable | string | Y | 회원 ID |

## 응답 (200 OK)

```json
{
  "memberId": "string",
  "memberName": "string",
  "grade": "BRONZE | SILVER | GOLD | VIP",
  "phone": "string",
  "createdAt": "2026-01-01T00:00:00",
  "recentOrders": [
    {
      "orderNo": "string",
      "orderState": "PLACED | PAID | SHIPPED | PARTIAL_SHIPPED | CANCELED | DONE",
      "totalAmount": 0,
      "orderedAt": "2026-01-01T00:00:00"
    }
  ]
}
```

## 비즈니스 규칙

- `del_yn = 'N'` 상시필터 → 탈퇴 회원 조회 시 404
- `recentOrders`는 최근 주문 **상위 5건 고정**(`RECENT_ORDER_LIMIT`), `ordered_at DESC, order_no DESC` 순
- 주문이 없으면 `recentOrders`는 빈 배열(`null` 금지, Member 도메인 필드 기본값)

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 404 | 회원 없음 | memberId로 조회된 회원이 없거나 탈퇴 회원 |

## 참조 테이블

- `MEMBERS`
- `ORDERS`

## curl 예시

```bash
curl -X GET /api/members/M0001 \
  -H "Content-Type: application/json"
```
