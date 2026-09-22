-- SR-306(#2) — PRODUCTS에 표시 전용 컬럼 2개를 추가한다: list_price(정가)·image_url(대표 이미지
-- 경로). 둘 다 NULL 허용 — 정가/이미지가 없는 상품은 화면이 그 상태를 감춘다(취소선·할인 배지
-- 미표시, 이니셜 대체 영역). 판매가(price)·재고(stock_qty) 판정에는 쓰이지 않는다(표시 전용,
-- 주문·장바구니 금액 계산은 여전히 price만 읽는다).
--
-- PRODUCTS는 이 레포의 버전 관리 DDL(V3~V9) 밖에서 만들어진 베이스라인 테이블이라(실측:
-- db-main describe — CREATE 문이 레포 어디에도 없고 시드 4행이 이미 DB에 존재) CREATE가 아니라
-- ALTER + 시드 UPDATE로만 다룬다. MariaDB 전용 문법(ADD COLUMN IF NOT EXISTS)으로 기동마다
-- 재실행돼도 안전하다(랩은 MariaDB 11.4.5 실측, member_signup_rate_limits.sql 선례와 동일 관례).
ALTER TABLE PRODUCTS
  ADD COLUMN IF NOT EXISTS list_price BIGINT NULL
    COMMENT '정가(원, 표시전용) — 판매가(price)와 무관, NULL=정가 없음',
  ADD COLUMN IF NOT EXISTS image_url VARCHAR(300) NULL
    COMMENT '대표 이미지 경로(앱 서빙 정적 경로, 예: /images/products/sku-1001.svg) — 외부 URL 금지';

-- 시드(UPDATE 4문, 멱등 — 같은 값을 재대입하므로 기동마다 재실행돼도 안전, INSERT는 쓰지 않는다).
-- 상태 커버리지를 의도적으로 섞는다:
--   SKU-1001 정가 450000(판매가 390000, 할인 있음) · 이미지 있음
--   SKU-1002 정가 없음(NULL, "정가 없음" 상태)        · 이미지 있음
--   SKU-1003 정가 450000(판매가 450000과 동일, 할인 0%) · 이미지 있음
--   SKU-1004 정가 42000(판매가 35000, 판매종료 상품)   · 이미지 없음(NULL, "이미지 없음" 상태를 실데이터로 보존)
UPDATE PRODUCTS SET list_price = 450000, image_url = '/images/products/sku-1001.svg' WHERE sku = 'SKU-1001';
UPDATE PRODUCTS SET list_price = NULL,   image_url = '/images/products/sku-1002.svg' WHERE sku = 'SKU-1002';
UPDATE PRODUCTS SET list_price = 450000, image_url = '/images/products/sku-1003.svg' WHERE sku = 'SKU-1003';
UPDATE PRODUCTS SET list_price = 42000,  image_url = NULL                            WHERE sku = 'SKU-1004';
