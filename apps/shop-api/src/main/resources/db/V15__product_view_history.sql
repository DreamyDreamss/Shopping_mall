-- 최근 본 상품 기록/조회를 위한 PRODUCT_VIEW_HISTORY 신설.
--
-- 이 프로젝트에는 마이그레이션 엔진이 없다(CLAUDE.md) — CREATE TABLE IF NOT EXISTS만 쓰고 DROP은
-- 금지한다(규칙 ddl-idempotent). 복합 PK(member_id, sku)가 "회원·상품 한 쌍에 한 행"을 강제한다
-- (PRODUCT_WISHES와 동일 근거) — 별도 UNIQUE 제약을 추가하지 않는다.
--
-- 보조 인덱스 ix_product_view_history_member_viewed_at(member_id, viewed_at) — 목록 조회
-- (viewed_at DESC)와 상한(20) 초과분 삭제 판정(ORDER BY viewed_at) 둘 다 이 인덱스로 커버된다.
--
-- 기존 데이터 이관·백필은 불필요(신규 데이터만). 타 도메인 테이블·정산/통계 집계에 대한 파급은 없다.
CREATE TABLE IF NOT EXISTS PRODUCT_VIEW_HISTORY (
  member_id   VARCHAR(20) NOT NULL,
  sku         VARCHAR(20) NOT NULL,
  viewed_at   DATETIME NOT NULL,
  PRIMARY KEY (member_id, sku),
  KEY ix_product_view_history_member_viewed_at (member_id, viewed_at),
  CONSTRAINT fk_product_view_history_member FOREIGN KEY (member_id) REFERENCES MEMBERS(member_id),
  CONSTRAINT fk_product_view_history_product FOREIGN KEY (sku) REFERENCES PRODUCTS(sku)
) COMMENT='최근 본 상품 — 회원·상품 한 쌍에 한 행, viewed_at으로 최근 순 정렬';
