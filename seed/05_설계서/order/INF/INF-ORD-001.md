---
inf-id: INF-ORD-001
method: GET
path: /api/members/
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables:
  - MEMBERS
anchors:
  - src/main/java/com/sm/lab/shop/controller/MemberController.java:21-25
  - src/main/java/com/sm/lab/shop/service/MemberService.java:24-26
  - src/main/java/com/sm/lab/shop/dao/MemberDao.java:11
  - src/main/resources/mapper/member.xml:7-12
---

# INF-ORD-001: GET /api/members/ — 회원 목록 조회

> **개요:** 탈퇴하지 않은 회원 전체를 회원ID 순으로 조회한다.

> **근거 소스:** `src/main/java/com/sm/lab/shop/controller/MemberController.java:21-25`

## 요청

- Method: GET
- Path: /api/members/
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| (없음) | - | - | - | - |

## 응답 (200 OK)

```json
[
  {
    "memberId": "string",
    "memberName": "string",
    "grade": "BRONZE | SILVER | GOLD | VIP",
    "phone": "string",
    "createdAt": "2026-01-01T00:00:00",
    "recentOrders": []
  }
]
```

## 비즈니스 규칙

- `del_yn = 'N'` 상시필터 → 탈퇴 회원은 목록에서 제외
- `recentOrders`는 이 API에서 채워지지 않고 항상 빈 배열([[INF-ORD-002]]에서만 채워짐)

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 400 | 유효성 실패 | 필수 필드 누락 |
| 401 | 인증 실패 | 토큰 없음/만료 |

## 참조 테이블

| 테이블 | SCH |
|--------|-----|
| MEMBERS | [[SCH-ORD-001]] |

## curl 예시

```bash
curl -X GET /api/members/ \
  -H "Content-Type: application/json"
```
