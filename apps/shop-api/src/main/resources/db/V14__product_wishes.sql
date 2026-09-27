-- SR-356(#2) — 찜 등록/해제/목록 API(INF-ORD-040)를 위한 PRODUCT_WISHES 신설(SCH-ORD-009).
--
-- 이 프로젝트에는 마이그레이션 엔진이 없다(CLAUDE.md) — CREATE TABLE IF NOT EXISTS만 쓰고 DROP은
-- 금지한다(규칙 ddl-idempotent). 복합 PK(member_id, sku) 자체가 "회원·상품 한 쌍에 한 행"을
-- 강제한다(SCH-ORD-009 확정 답변 scope_freeze) — 별도 UNIQUE 제약을 추가하지 않는다(PK가 이미
-- 유니크).
--
-- CART_ITEMS(PK(member_id, sku), del_yn 없음)와 동일한 모양 — PRODUCT_REVIEWS처럼 소프트 삭제·
-- 생성열이 필요 없다(찜은 이력 보존 요구가 없고 하드 삭제가 정답, 요구사항 그대로).
--
-- sku 단독 보조 인덱스(ix_product_wishes_sku, PK가 (member_id, sku)라 sku 단독 조회는 커버 안 됨)를
-- 이 항목이 미리 만들어 둔다 — SR-356 #6(상품 단건 조회 wishCount 집계, INF-ORD-009, 아직 미생성)이
-- COUNT(*) WHERE sku=?로 쓸 자리를 준비하는 것뿐, 그 코드(ProductService)는 이 항목이 손대지 않는다.
--
-- 기존 데이터 이관·백필은 불필요(신규 데이터만, SCH-ORD-009 확정 답변 db_migration). 타 도메인
-- 테이블·정산/통계 집계에 대한 파급은 없음(SCH-ORD-009 확정 답변 db_ripple).
CREATE TABLE IF NOT EXISTS PRODUCT_WISHES (
  member_id   VARCHAR(20) NOT NULL,
  sku         VARCHAR(20) NOT NULL,
  created_at  DATETIME NOT NULL,
  PRIMARY KEY (member_id, sku),
  KEY ix_product_wishes_sku (sku),
  CONSTRAINT fk_product_wishes_member FOREIGN KEY (member_id) REFERENCES MEMBERS(member_id),
  CONSTRAINT fk_product_wishes_product FOREIGN KEY (sku) REFERENCES PRODUCTS(sku)
) COMMENT='상품 찜(SR-356 #2, INF-ORD-040) — 회원·상품 한 쌍에 한 행';
