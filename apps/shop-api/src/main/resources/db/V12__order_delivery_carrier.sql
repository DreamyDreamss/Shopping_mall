-- LAB-105.1 — ORDER_DELIVERY에 택배사 코드/명 컬럼 추가(표시 전용). ORDER_DELIVERY는 PRODUCTS와
-- 같은 성격의 베이스라인 테이블(레포 DDL 밖, db-main 실측 5컬럼)이라 CREATE가 아니라 ALTER +
-- 시드 UPDATE로만 다룬다(V10 PRODUCTS 선례와 동일 관례, MariaDB 11.4.5).
ALTER TABLE ORDER_DELIVERY
  ADD COLUMN IF NOT EXISTS carrier_code VARCHAR(20) NULL
    COMMENT '택배사 코드(표시전용) — NULL=미배정(READY 등)',
  ADD COLUMN IF NOT EXISTS carrier_name VARCHAR(50) NULL
    COMMENT '택배사명(표시전용)';

-- 시드(UPDATE 2문, 멱등 — 동일 값 재대입). invoice_no가 있는 기존 2건에만 채운다 — READY(D-0816-2,
-- invoice_no NULL)는 그대로 두어 "송장 등록 대기" 상태를 실데이터로 보존한다.
UPDATE ORDER_DELIVERY SET carrier_code = 'CJ', carrier_name = 'CJ대한통운' WHERE delivery_no = 'D-0815-1';
UPDATE ORDER_DELIVERY SET carrier_code = 'CJ', carrier_name = 'CJ대한통운' WHERE delivery_no = 'D-0816-1';
