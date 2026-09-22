-- linked_func: FUNC-member-011
-- spec: docs/00_FUNC/stories/STORY-FUNC-member-011.md
-- SR-235 — 배송지 CRUD API(INF-MBR-008, FUNC-member-011)를 위한 MEMBER_ADDRESSES 신설.
--
-- 이 프로젝트에는 마이그레이션 엔진(Flyway/Liquibase)이 없다(CLAUDE.md, 기존 V3~V6과 동일 실측).
-- "V7__" 접두는 기존 파일들의 네이밍 관례일 뿐이고 실제 실행은 spring.sql.init.mode=always가
-- 기동마다 db/*.sql 전체를 재실행한다 — 그래서 CREATE TABLE IF NOT EXISTS만 쓰고 DROP은 금지한다
-- (규칙 ddl-idempotent).
--
-- member_id는 MEMBERS.member_id(VARCHAR(20), "M-0001" 접두 포맷 — V3__members_signup.sql round2
-- 참고)를 그대로 참조한다. address_id는 접두 포맷이 아니라 이 SR 전용 신규 PK라 AUTO_INCREMENT를
-- 그대로 쓴다 — ID_SEQUENCES 원자채번은 접두 포맷 PK 전용 관례이므로 여기서는 불필요하다(계획
-- "데이터" 절).
--
-- ZIPCODES 테이블은 여기서 만들지 않는다(형제 FUNC-member-012 몫) — zipcode/road_address/
-- detail_address는 클라이언트가 보낸 값을 그대로 저장할 뿐, 이 FUNC은 ZIPCODES와 대조·검증하지
-- 않는다(STORY "범위 밖" 절).
CREATE TABLE IF NOT EXISTS MEMBER_ADDRESSES (
  address_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id        VARCHAR(20) NOT NULL,
  recipient        VARCHAR(50) NOT NULL,
  phone            VARCHAR(20) NOT NULL,
  phone_norm       VARCHAR(20) NOT NULL,
  zipcode          VARCHAR(10) NOT NULL,
  road_address     VARCHAR(200) NOT NULL,
  detail_address   VARCHAR(200) NOT NULL,
  entrance_method  VARCHAR(200) NULL,
  delivery_memo    VARCHAR(200) NULL,
  is_default       CHAR(1) NOT NULL DEFAULT 'N',
  last_used_at     DATETIME NULL,
  created_at       DATETIME NOT NULL,
  updated_at       DATETIME NOT NULL,
  del_yn           CHAR(1) NOT NULL DEFAULT 'N',
  CONSTRAINT fk_member_addresses_member FOREIGN KEY (member_id) REFERENCES MEMBERS(member_id)
) COMMENT='회원 배송지(SR-235, FUNC-member-011) — 회원당 최대 10개·기본 1개';
