-- Speclinker 실증 랩 스키마 (sl_shop) — 주문 도메인 5테이블 + 실FK.
-- 컬럼 COMMENT는 ddd-db-agent enrichment·DB MCP 권위 조회 실증의 근거 데이터다.
CREATE DATABASE IF NOT EXISTS sl_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE sl_shop;

DROP TABLE IF EXISTS CART_ITEMS;
DROP TABLE IF EXISTS ORDER_DELIVERY;
DROP TABLE IF EXISTS ORDER_ITEMS;
DROP TABLE IF EXISTS ORDERS;
DROP TABLE IF EXISTS PRODUCTS;
DROP TABLE IF EXISTS MEMBERS;

CREATE TABLE MEMBERS (
  member_id   VARCHAR(20)  NOT NULL COMMENT '회원 ID (M-접두)',
  member_name VARCHAR(50)  NOT NULL COMMENT '회원명',
  grade       VARCHAR(10)  NOT NULL DEFAULT 'BRONZE' COMMENT '등급: BRONZE/SILVER/GOLD/VIP',
  phone       VARCHAR(20)  NULL COMMENT '휴대폰(암호화 저장 대상 — 랩에서는 평문)',
  del_yn      CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '탈퇴 여부 (soft delete)',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일시',
  PRIMARY KEY (member_id)
) COMMENT='회원 마스터';

CREATE TABLE PRODUCTS (
  sku         VARCHAR(20)  NOT NULL COMMENT '상품 SKU',
  product_name VARCHAR(100) NOT NULL COMMENT '상품명',
  price       BIGINT       NOT NULL COMMENT '판매가(원)',
  stock_qty   INT          NOT NULL DEFAULT 0 COMMENT '가용 재고',
  sale_yn     CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '판매 여부',
  PRIMARY KEY (sku)
) COMMENT='상품 마스터';

-- SR-202: 장바구니 품목 (담기 전용 보관 — 재고 비차감). PK(member_id, sku)로 재담기=합산(UPSERT)을 강제.
CREATE TABLE CART_ITEMS (
  member_id   VARCHAR(20)  NOT NULL COMMENT '회원 ID (FK→MEMBERS)',
  sku         VARCHAR(20)  NOT NULL COMMENT '상품 SKU (FK→PRODUCTS)',
  qty         INT          NOT NULL COMMENT '수량(1 이상 — 애플리케이션에서 강제, D4)',
  added_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '담은 일시',
  PRIMARY KEY (member_id, sku),
  CONSTRAINT fk_cart_member  FOREIGN KEY (member_id) REFERENCES MEMBERS (member_id),
  CONSTRAINT fk_cart_product FOREIGN KEY (sku) REFERENCES PRODUCTS (sku)
) COMMENT='장바구니 품목(SR-202) — 재담기는 PK로 합산(UPSERT), 재고는 차감하지 않음(보관 전용)';

CREATE TABLE ORDERS (
  order_no    VARCHAR(20)  NOT NULL COMMENT '주문번호 (yyyymmdd+seq)',
  member_id   VARCHAR(20)  NOT NULL COMMENT '주문 회원',
  order_state VARCHAR(20)  NOT NULL DEFAULT 'PLACED' COMMENT '상태: PLACED/PAID/SHIPPED/PARTIAL_SHIPPED/CANCELED/DONE',
  total_amount BIGINT      NOT NULL COMMENT '주문 총액(원)',
  del_yn      CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '논리 삭제 (soft delete — 전 조회 상시필터)',
  ordered_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '주문일시',
  PRIMARY KEY (order_no),
  CONSTRAINT fk_orders_member FOREIGN KEY (member_id) REFERENCES MEMBERS (member_id)
) COMMENT='주문 마스터';

CREATE TABLE ORDER_ITEMS (
  order_no    VARCHAR(20)  NOT NULL COMMENT '주문번호',
  line_no     INT          NOT NULL COMMENT '주문 라인 번호',
  sku         VARCHAR(20)  NOT NULL COMMENT '상품 SKU',
  qty         INT          NOT NULL COMMENT '수량',
  unit_price  BIGINT       NOT NULL COMMENT '주문 시점 단가(원)',
  PRIMARY KEY (order_no, line_no),
  CONSTRAINT fk_items_order   FOREIGN KEY (order_no) REFERENCES ORDERS (order_no),
  CONSTRAINT fk_items_product FOREIGN KEY (sku) REFERENCES PRODUCTS (sku)
) COMMENT='주문 상세(라인)';

CREATE TABLE ORDER_DELIVERY (
  delivery_no VARCHAR(20)  NOT NULL COMMENT '배송번호',
  order_no    VARCHAR(20)  NOT NULL COMMENT '주문번호',
  delivery_state VARCHAR(20) NOT NULL DEFAULT 'READY' COMMENT '상태: READY/SHIPPED/DELIVERED/CANCELED',
  invoice_no  VARCHAR(30)  NULL COMMENT '송장번호',
  shipped_at  DATETIME     NULL COMMENT '출고일시',
  PRIMARY KEY (delivery_no),
  CONSTRAINT fk_delivery_order FOREIGN KEY (order_no) REFERENCES ORDERS (order_no)
) COMMENT='주문 배송';
