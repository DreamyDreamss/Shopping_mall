-- SR-364(#1) — 재입고 알림 신청 API(INF-ORD-048)를 위한 RESTOCK_ALERTS 신설(SCH-ORD-012).
--
-- 이 프로젝트에는 마이그레이션 엔진이 없다(CLAUDE.md) — CREATE TABLE IF NOT EXISTS만 쓰고 DROP은
-- 금지한다(규칙 ddl-idempotent). 복합 PK(member_id, sku) 자체가 "회원·상품 한 쌍에 한 행"을
-- 강제한다(PRODUCT_WISHES와 동일 논리) — 별도 UNIQUE 제약을 추가하지 않는다.
--
-- sku 단독 보조 인덱스는 만들지 않는다 — 이 SR은 재입고 시 알림 "발송"(sku로 구독자 역조회)을
-- 다루지 않고 저장·내 목록(member_id 리딩 PK로 충분)·상세 버튼 표시뿐이다(STORY "데이터" 절, 범위
-- 밖 참고).
CREATE TABLE IF NOT EXISTS RESTOCK_ALERTS (
  member_id   VARCHAR(20) NOT NULL,
  sku         VARCHAR(20) NOT NULL,
  created_at  DATETIME NOT NULL,
  PRIMARY KEY (member_id, sku),
  CONSTRAINT fk_restock_alerts_member FOREIGN KEY (member_id) REFERENCES MEMBERS(member_id),
  CONSTRAINT fk_restock_alerts_product FOREIGN KEY (sku) REFERENCES PRODUCTS(sku)
) COMMENT='재입고 알림 신청(SR-364 #1, INF-ORD-048) — 회원·상품 한 쌍에 한 행';
