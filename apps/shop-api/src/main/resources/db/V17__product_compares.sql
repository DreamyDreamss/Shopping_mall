-- SR-360(#2) — 상품 비교함 담기/빼기/목록 API(INF-ORD-045/046/047)를 위한 PRODUCT_COMPARES 신설
-- (SCH-ORD-011).
--
-- 이 프로젝트에는 마이그레이션 엔진이 없다(CLAUDE.md) — CREATE TABLE IF NOT EXISTS만 쓰고 DROP은
-- 금지한다(규칙 ddl-idempotent). 복합 PK(member_id, sku) 자체가 "회원·상품 한 쌍에 한 행"을
-- 강제한다(PRODUCT_WISHES와 동일 이유) — 별도 UNIQUE 제약을 추가하지 않는다.
--
-- FK는 V14__product_wishes.sql과 동일하게 ON DELETE 절을 명시하지 않는다 — InnoDB 기본 동작
-- 자체가 RESTRICT라 SCH-ORD-011이 요구하는 "FK members·products(RESTRICT)"를 별도 절 없이
-- 만족한다(이 코드베이스 기존 FK 전부와 동일한 관례).
--
-- 보조 인덱스는 sku 단독이 아니라 (member_id, created_at) — SCH-ORD-011이 명시한 조합이며 목록
-- (INF-ORD-047, created_at ASC 정렬)이 이 인덱스로 커버된다(PK가 이미 member_id 선두라 member_id
-- 단독 조회는 PK로 충분하지만, 정렬 컬럼까지 포함한 커버링 인덱스로 명시적으로 둔다). 최대 4개
-- 상한은 애플리케이션(단일 조건부 INSERT) 레벨에서 강제한다 — DB 제약이 아니다.
--
-- 기존 데이터 이관·백필은 불필요(신규 데이터만). 타 도메인 테이블·정산/통계 집계에 대한 파급은
-- 없다(SCH-ORD-011 확정 답변 — PRODUCT_COMPARES는 MEMBERS·PRODUCTS FK만 가진다).
CREATE TABLE IF NOT EXISTS PRODUCT_COMPARES (
  member_id   VARCHAR(20) NOT NULL,
  sku         VARCHAR(20) NOT NULL,
  created_at  DATETIME NOT NULL,
  PRIMARY KEY (member_id, sku),
  KEY ix_product_compares_member_created (member_id, created_at),
  CONSTRAINT fk_product_compares_member FOREIGN KEY (member_id) REFERENCES MEMBERS(member_id),
  CONSTRAINT fk_product_compares_product FOREIGN KEY (sku) REFERENCES PRODUCTS(sku)
) COMMENT='상품 비교함(SR-360 #2, INF-ORD-045/046/047) — 회원·상품 한 쌍에 한 행, 최대 4개(애플리케이션 레벨 제한)';
