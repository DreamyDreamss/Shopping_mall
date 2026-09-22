---
version: 1.0.0
mode: RECON
generated: 2026-08-22
---

# 기능 목록 (FUNC_v1.0)

> RECON 모드 — 소스 코드에서 도출된 구현 기능 목록.  
> 요구사항 추상화 없음. **구현 사실 기록**.

> **FUNC-ID 규약 (MUST)**: `FUNC-{도메인}-{NNN}` 의 {도메인}은 **funcs_index의 `domain` 값(디렉토리명, 소문자 예: `order`)**을 쓴다.
> **domain-code(예: ORD)·대문자 사용 금지** — srs-agent/rtm-agent와 ID가 일치해야 SRS↔FUNC↔FUNC_MAP 링크가 연결된다.

## 기능 색인표

| FUNC-ID | 기능명(화면) | 도메인 | INF 수 | DB 테이블 | SRS-F |
|---------|------------|--------|--------|-----------|-------|
| FUNC-order-001 | UIS-ORD-001 주문 목록 | order | 1 | [확인 필요] | [확인 필요] |
| FUNC-order-002 | UIS-ORD-002 주문 상세 | order | 2 | [확인 필요] | [확인 필요] |

---

## FUNC-order-001: UIS-ORD-001 주문 목록

- **도메인**: order
- **화면**: [UIS-ORD-001_주문목록](../05_설계서/order/UIS/UIS-ORD-001_주문목록/spec.md)
- **URL**: `/order/list`
- **핵심 API**:
  - GET `/api/orders/` [INF-ORD-003]
- **DB 테이블**: [확인 필요]
- **비즈니스 규칙**:
  - [확인 필요]
- **연결 SRS**: [확인 필요]

---

## FUNC-order-002: UIS-ORD-002 주문 상세

- **도메인**: order
- **화면**: [UIS-ORD-002_주문상세](../05_설계서/order/UIS/UIS-ORD-002_주문상세/spec.md)
- **URL**: `/order/{orderNo}`
- **핵심 API**:
  - GET `/api/orders/{orderNo}` [INF-ORD-004]
  - PATCH `/api/orders/{orderNo}/cancel` [INF-ORD-006]
- **DB 테이블**: [확인 필요]
- **비즈니스 규칙**:
  - [확인 필요]
- **연결 SRS**: [확인 필요]

---

## 요약

| 항목 | 수 |
|------|-----|
| 기능(FUNC) | 2 |
| 도메인 | 1 (order) |
| 화면(UIS) | 2 |
| API(INF) | 9 |
| DB 테이블(SCH) | 5 |

