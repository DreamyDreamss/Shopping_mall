---
domain: order
version: 1.0.0
mode: RECON
generated: 2026-08-22
---

# Order 도메인 기능 명세 (FUNC_order)

## 도메인 개요

- **도메인명**: order (주문)
- **기능 수**: 2개
- **화면(UIS) 수**: 2개
- **API(INF) 수**: 3개 (핵심 사용)
- **전체 API(INF) 수**: 9개
- **DB 테이블(SCH) 수**: 5개

---

## FUNC-order-001: 주문 목록 조회

### 기본 정보

| 항목 | 내용 |
|------|------|
| **FUNC-ID** | FUNC-order-001 |
| **화면** | UIS-ORD-001_주문목록 |
| **기능명** | 주문 목록 조회 |
| **URL** | `/order/list` |
| **도메인** | order |

### 기능 상세

**사용자가 전체 주문 목록을 조회하는 화면입니다.**

#### 핵심 API

| INF-ID | Method | Path | 설명 |
|--------|--------|------|------|
| INF-ORD-003 | GET | `/api/orders/` | 주문 목록 조회 |

#### 참조 DB 테이블

| SCH-ID | 테이블명 | 설명 |
|--------|---------|------|
| SCH-ORD-004 | orders | 주문 기본 정보 |

#### 비즈니스 규칙

- [확인 필요]

#### 연결된 SRS

- [확인 필요]

---

## FUNC-order-002: 주문 상세 조회 및 취소

### 기본 정보

| 항목 | 내용 |
|------|------|
| **FUNC-ID** | FUNC-order-002 |
| **화면** | UIS-ORD-002_주문상세 |
| **기능명** | 주문 상세 조회 및 취소 |
| **URL** | `/order/{orderNo}` |
| **도메인** | order |

### 기능 상세

**사용자가 특정 주문의 상세 정보를 조회하고, 필요시 주문을 취소하는 화면입니다.**

#### 핵심 API

| INF-ID | Method | Path | 설명 |
|--------|--------|------|------|
| INF-ORD-004 | GET | `/api/orders/{orderNo}` | 주문 상세 조회 |
| INF-ORD-006 | PATCH | `/api/orders/{orderNo}/cancel` | 주문 취소 |

#### 참조 DB 테이블

| SCH-ID | 테이블명 | 설명 |
|--------|---------|------|
| SCH-ORD-003 | order_items | 주문 상품 항목 |
| SCH-ORD-002 | order_delivery | 주문 배송 정보 |
| SCH-ORD-004 | orders | 주문 기본 정보 |

#### 비즈니스 규칙

- [확인 필요]

#### 연결된 SRS

- [확인 필요]

---

## Order 도메인 전체 API 목록

| INF-ID | Method | Path | 설명 |
|--------|--------|------|------|
| INF-ORD-001 | GET | `/api/members/` | 회원 목록 조회 |
| INF-ORD-002 | GET | `/api/members/{memberId}` | 회원 상세 조회 |
| INF-ORD-003 | GET | `/api/orders/` | 주문 목록 조회 |
| INF-ORD-004 | GET | `/api/orders/{orderNo}` | 주문 상세 조회 |
| INF-ORD-005 | POST | `/api/orders/` | 주문 생성 |
| INF-ORD-006 | PATCH | `/api/orders/{orderNo}/cancel` | 주문 취소 |
| INF-ORD-007 | GET | `/api/orders/{orderNo}/deliveries` | 주문 배송 목록 조회 |
| INF-ORD-008 | GET | `/api/products/` | 판매중 상품 목록 조회 |
| INF-ORD-009 | GET | `/api/products/{sku}` | 상품 단건 조회 |

---

## Order 도메인 DB 테이블 목록

| SCH-ID | 테이블명 | 설명 |
|--------|---------|------|
| SCH-ORD-001 | members | 회원 정보 |
| SCH-ORD-002 | order_delivery | 주문 배송 정보 |
| SCH-ORD-003 | order_items | 주문 상품 항목 |
| SCH-ORD-004 | orders | 주문 기본 정보 |
| SCH-ORD-005 | products | 상품 정보 |

