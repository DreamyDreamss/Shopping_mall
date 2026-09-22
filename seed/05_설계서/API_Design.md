# API 설계서 — sl-shop

> 자동 생성 (merge_index.py) — 직접 편집 금지

## INF 색인

| INF-ID | 엔드포인트·기능명 |
|--------|-----------------|
| INF-ORD-001 | [GET /api/members/ — 회원 목록 조회](./order/INF/INF-ORD-001.md) |
| INF-ORD-002 | [GET /api/members/{memberId} — 회원 상세 조회](./order/INF/INF-ORD-002.md) |
| INF-ORD-003 | [GET /api/orders/ — 주문 목록 조회](./order/INF/INF-ORD-003.md) |
| INF-ORD-004 | [GET /api/orders/{orderNo} — 주문 상세 조회](./order/INF/INF-ORD-004.md) |
| INF-ORD-005 | [POST /api/orders/ — 주문 생성](./order/INF/INF-ORD-005.md) |
| INF-ORD-006 | [PATCH /api/orders/{orderNo}/cancel — 주문 취소](./order/INF/INF-ORD-006.md) |
| INF-ORD-007 | [GET /api/orders/{orderNo}/deliveries — 주문 배송 목록 조회](./order/INF/INF-ORD-007.md) |
| INF-ORD-008 | [GET /api/products/ — 판매중 상품 목록 조회](./order/INF/INF-ORD-008.md) |
| INF-ORD-009 | [GET /api/products/{sku} — 상품 단건 조회](./order/INF/INF-ORD-009.md) |

## 도메인별 파일 목록

| 도메인 | API 색인 | DB 스키마 | UI 색인 |
|--------|---------|----------|--------|
| order | [API_order.md](./order/API_order.md) | [DB_order.md](./order/DB_order.md) | [_TOC.md](./order/UIS/_TOC.md) |
