# order 도메인 개요 (신규자·신규요건 분석용)

> 사람용 SOP 레이어. 기계용 인덱스(INF/SCH frontmatter·앵커)와 분리된 개념 설명이다.

## 목적
주문 생성·조회·취소·배송 조회를 축으로 회원(Member)·상품(Product) 조회까지 포함하는 단일 도메인. Member/Order/Product 3개 컨트롤러는 개념적으로 구분되지만, controller/service/dao/domain 전 계층이 com.sm.lab.shop 패키지 하위에 layer-by-type(계층별)으로 flat 배치되어 있어(도메인별 하위 디렉토리 없음) 디렉토리 경로 기준으로는 분리되지 않는다. rootPaths는 prefix 매칭(in_roots)만 지원하므로 3개 도메인에 동일 경로를 배정하면 각 도메인이 17개 파일을 전부 중복 매칭하게 되어 기계적으로 성립하지 않는다. 파일수 기준(10개 미만 인접 도메인 흡수)으로도 Member(4개: Controller/Service/Dao/Member.java)·Product(4개)는 흡수 대상이라 Order(8개: Controller/ViewController/Service/Dao/Order·OrderDelivery·OrderItem·OrderSummary.java)에 통합한다.

## 핵심 엔티티 (사용 빈도순 — 이 테이블부터 이해)

- **MEMBERS** (7개 기능에서 사용) → SCH-ORD-001
- **ORDERS** (6개 기능에서 사용) → SCH-ORD-004
- **PRODUCTS** (5개 기능에서 사용) → SCH-ORD-005
- **ORDER_ITEMS** (4개 기능에서 사용) → SCH-ORD-003
- **ORDER_DELIVERY** (4개 기능에서 사용) → SCH-ORD-002

## 대표 화면 (2개) — 화면으로 이해

> 이 시스템은 화면으로 이해한다. 각 화면이 무슨 API를 호출하는지로 동작을 파악.

- **주문 상세** `/order/{orderNo}` — 호출 API: INF-ORD-004, INF-ORD-006 [UIS-ORD-002](docs/05_설계서/order/UIS/UIS-ORD-002_주문상세/spec.md)
- **주문 목록** `/order/list` — 호출 API: INF-ORD-003 [UIS-ORD-001](docs/05_설계서/order/UIS/UIS-ORD-001_주문목록/spec.md)

## 대표 기능 (9개 중 진입점)

- GET `/api/members/` — [INF-ORD-001](INF/INF-ORD-001.md)
- GET `/api/members/{memberId}` — [INF-ORD-002](INF/INF-ORD-002.md)
- GET `/api/orders/` — [INF-ORD-003](INF/INF-ORD-003.md)
- POST `/api/orders/` — [INF-ORD-005](INF/INF-ORD-005.md)
- GET `/api/orders/{orderNo}` — [INF-ORD-004](INF/INF-ORD-004.md)
- PATCH `/api/orders/{orderNo}/cancel` — [INF-ORD-006](INF/INF-ORD-006.md)
- GET `/api/orders/{orderNo}/deliveries` — [INF-ORD-007](INF/INF-ORD-007.md)
- GET `/api/products/` — [INF-ORD-008](INF/INF-ORD-008.md)
- GET `/api/products/{sku}` — [INF-ORD-009](INF/INF-ORD-009.md)

## 신규자 진입점
1. **대표 화면**부터: 화면 1~2개를 열어 "무슨 버튼→무슨 API→무슨 결과"(UIS 5) 파악
2. **핵심 엔티티** 상위 2~3개의 SCH(DB_order.md)로 데이터 구조 파악
3. **대표 기능** INF 1~2개로 요청/응답·비즈니스 규칙 확인
4. 변경 시 `/sl-change`가 영향슬라이스+소스앵커로 정밀 그라운딩

