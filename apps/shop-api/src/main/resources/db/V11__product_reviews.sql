-- SR-351(#1) — 상품평 작성/삭제 API(INF-ORD-037/INF-ORD-039)를 위한 PRODUCT_REVIEWS 신설.
--
-- 이 프로젝트에는 마이그레이션 엔진이 없다(CLAUDE.md, V3~V10과 동일 실측) — spring.sql.init.mode=always가
-- 기동마다 db/*.sql 전체를 재실행하므로 CREATE TABLE IF NOT EXISTS만 쓰고 DROP은 금지한다(규칙
-- ddl-idempotent). 실측: 이 저장소의 최신 적용 파일은 V10__product_list_price_image.sql(파일시스템·
-- application.yml schema-locations 둘 다 확인) — 다음 번호는 V11.
--
-- order_no/sku/member_id는 ORDERS.order_no/PRODUCTS.sku/MEMBERS.member_id와 동일 폭(전부
-- VARCHAR(20), DB 실측)을 그대로 따른다. 리뷰 작성자의 이름은 이 테이블에 저장하지 않는다(확정
-- 문답 pii_format) — member_id만 저장하고, 조회 시 서버가 MEMBERS 조인 후 마스킹한다(마스킹·조회
-- API는 SR-351 #2 몫, 이 파일은 저장 스키마만 다룬다).
--
-- 유일성(사람 수정 확정, STORY "데이터" 절): 순수 UNIQUE(order_no, sku)는 "삭제 후 재작성"(확정
-- 문답 scope_freeze ⑥)을 영구히 막고, UNIQUE(order_no, sku, del_yn)는 같은 조합을 "재작성→재삭제"
-- 까지 반복하면 두 번째 삭제행(del_yn='Y')끼리 충돌한다. 생성열 active_key(del_yn='N'일 때만
-- order_no|sku, 그 외 NULL) + UNIQUE 인덱스로 대체한다 — MariaDB는 UNIQUE 인덱스에서 NULL 다건을
-- 허용하므로 del_yn='Y' 행은 유일성 검사 밖이다. 이 성질은 삭제할 때마다 매번 새로 성립하므로
-- (DB가 "직전에 몇 번 삭제됐었는지"를 기억하지 않고 그 시점의 del_yn만 봄) 작성→삭제→재작성→
-- 재삭제→재작성을 몇 번 반복해도 깨지지 않는다. 새 테이블이라 별도 ALTER 없이 이 CREATE TABLE
-- 안에 인덱스를 함께 정의한다(기동마다 전체가 no-op으로 재실행되므로 idempotent).
CREATE TABLE IF NOT EXISTS PRODUCT_REVIEWS (
  review_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no    VARCHAR(20) NOT NULL COMMENT '주문번호(ORDERS.order_no)',
  sku         VARCHAR(20) NOT NULL COMMENT '상품 SKU',
  member_id   VARCHAR(20) NOT NULL COMMENT '작성 회원(이름은 저장하지 않음, pii_format)',
  rating      TINYINT NOT NULL COMMENT '별점 1~5',
  content     VARCHAR(1000) NOT NULL COMMENT '본문 10~1000자',
  del_yn      CHAR(1) NOT NULL DEFAULT 'N' COMMENT '소프트 삭제 여부',
  created_at  DATETIME NOT NULL,
  updated_at  DATETIME NOT NULL,
  active_key  VARCHAR(41) AS (CASE WHEN del_yn = 'N' THEN CONCAT(order_no, '|', sku) END) PERSISTENT
              COMMENT '유일성 판정용 생성열 — del_yn=Y 행은 NULL이라 UNIQUE 검사 밖',
  UNIQUE KEY uq_product_reviews_active (active_key),
  KEY ix_product_reviews_member (member_id),
  CONSTRAINT fk_product_reviews_order FOREIGN KEY (order_no) REFERENCES ORDERS (order_no),
  CONSTRAINT fk_product_reviews_member FOREIGN KEY (member_id) REFERENCES MEMBERS (member_id)
) COMMENT='상품평(SR-351) — 구매 회원 작성, 소프트 삭제 후 같은 (주문번호, SKU) 재작성 허용';
