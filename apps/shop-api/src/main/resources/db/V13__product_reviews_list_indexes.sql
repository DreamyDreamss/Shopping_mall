-- SR-351(#2) — 상품평 목록·집계 조회(INF-ORD-038, GET /api/products/{sku}/reviews) 전용 인덱스.
--
-- 이 파일은 V11__product_reviews.sql(같은 SR #1이 만든 PRODUCT_REVIEWS 테이블)을 직접 고치지
-- 않는다 — 이 워크스페이스 CLAUDE.md 규칙("DDL 파일 번호는 다음 번호로 새 파일을 만든다 — 기존
-- 파일 수정 금지, 기동마다 전부 재실행된다")은 git 커밋 여부가 아니라 파일이 물리적으로 존재하는지를
-- 기준으로 한다(spring.sql.init.mode=always가 파일시스템의 db/*.sql 전부를 기동마다 재실행하는
-- 메커니즘 자체가 git 상태를 모른다). V11은 git status상 미커밋(??)이지만 이미 작업 트리에 실존하는
-- 파일이라 "기존 파일"로 취급한다(STORY "데이터" 절 "인덱스 위치" 결정 사유).
--
-- 실측: 이 파일 작성 시점에 V12__order_delivery_carrier.sql(다른 SR, 배송사 연동 건, 마찬가지로
-- 미커밋)이 이미 다음 번호를 선점했다 — 그래서 이 파일은 V13이다.
--
-- CREATE INDEX IF NOT EXISTS는 MariaDB 10.1.4+ 지원 문법이라(규칙 ddl-idempotent) 이미 인덱스가
-- 있는 DB에 재기동해도 오류 없이 no-op이다.
CREATE INDEX IF NOT EXISTS ix_product_reviews_sku_list
  ON PRODUCT_REVIEWS (sku, del_yn, created_at);
CREATE INDEX IF NOT EXISTS ix_product_reviews_sku_rating
  ON PRODUCT_REVIEWS (sku, del_yn, rating, created_at);
