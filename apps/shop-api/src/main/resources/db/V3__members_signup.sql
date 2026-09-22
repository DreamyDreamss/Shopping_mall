-- linked_func: FUNC-member-003
-- spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
-- SR-231 — 가입 요청 API(INF-MBR-002, FUNC-member-003)를 위한 MEMBERS 컬럼 추가.
--
-- 이 프로젝트에는 마이그레이션 엔진(Flyway/Liquibase)이 없다(pom.xml 확인, FUNC-member-002와
-- 동일 실측) — Spring Boot spring.sql.init 스키마 파일 메커니즘으로 기동 시 자동 적용된다
-- (application.yml spring.sql.init.mode=always + schema-locations에 이 파일 추가).
-- 파일명은 STORY-FUNC-member-003.md "승인된 설계 확정"이 명시한 그대로(V3__members_signup.sql)
-- 사용한다 — 실제로는 Flyway가 아니라 이 프로젝트의 spring.sql.init 관례로 적용되는 일반 SQL
-- 파일이다(member_signup_verifications.sql/member_signup_rate_limits.sql과 동일한 실행 경로).
--
-- MEMBERS 테이블은 이미 존재한다(member_id/member_name/grade/phone/del_yn/created_at 6컬럼,
-- DB MCP로 실측). 이 파일은 컬럼 추가만 한다 — 기존 컬럼의 이름·타입·NULL 허용·기본값은 절대
-- 바꾸지 않는다(확정 문답 regression_keep: "기존 조회 결과 전부 불변").
--
-- phone 컬럼은 이미 존재(varchar(20), nullable)하므로 여기서는 UNIQUE 인덱스만 추가한다.
-- 사전 실측(DB MCP): 기존 4개 시드 행 중 NULL 2건(M-0003/M-0004)뿐이고 중복 값은 없어 UNIQUE
-- 인덱스 추가가 안전함을 확인했다(MariaDB는 NULL을 UNIQUE 제약에서 여러 번 허용).
ALTER TABLE MEMBERS
  ADD COLUMN IF NOT EXISTS email VARCHAR(255) NULL
    COMMENT '이메일(SR-231 가입 채널이 이메일인 경우) — FUNC-member-003';
ALTER TABLE MEMBERS
  ADD COLUMN IF NOT EXISTS password_hash VARCHAR(100) NULL
    COMMENT 'BCrypt 비밀번호 해시(SR-231) — FUNC-member-003';
ALTER TABLE MEMBERS
  ADD COLUMN IF NOT EXISTS marketing_opt_in TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '마케팅 수신 동의(SR-231, 0/1) — FUNC-member-003';

-- 이메일 중복 판정은 애플리케이션의 select→분기가 아니라 이 UNIQUE 인덱스 위반
-- (DuplicateKeyException)으로 원자적으로 수행한다(STORY "승인된 설계 확정" — 선조회 금지,
-- 동시 요청 레이스 방지). MariaDB 11.4.5(랩 실측) — ADD INDEX/UNIQUE INDEX IF NOT EXISTS는
-- 10.5.2+ 지원.
ALTER TABLE MEMBERS ADD UNIQUE INDEX IF NOT EXISTS uq_members_email (email);
ALTER TABLE MEMBERS ADD UNIQUE INDEX IF NOT EXISTS uq_members_phone (phone);

-- ============================================================================
-- round2(2026-09-12, SR-231 round1 QA FAIL 재작업 지시, 사람 결정 — 방식까지 지정) — 세 축:
-- (A) 휴대폰 중복 판정 정규화, (B) 회원 ID 원자 채번(ID_SEQUENCES), (C) 인증코드 대입 시도
-- 제한 + 1회성 소비(MEMBER_SIGNUP_VERIFICATIONS.consumed_at 신설). 전부 이 파일(FUNC-member-003
-- 소유 마이그레이션)에 누적한다 — member_signup_verifications.sql(FUNC-member-002 소유 파일)은
-- 이번에도 손대지 않는다(round1과 동일 경계 — attempt_count 컬럼처럼 "테이블은 FUNC-member-002가
-- 소유하되 FUNC-member-003이 쓸 컬럼은 FUNC-member-003 마이그레이션에서 추가"하는 전례를 그대로
-- 따른다).
-- ============================================================================

-- (A) 휴대폰 중복 판정 정규화(QA FAIL 필수2) — DB 실측: 기존 시드 회원의 phone은 하이픈 포함
-- ('010-1111-2222'), round1은 target(숫자만)을 그대로 phone에 저장해 uq_members_phone(원문
-- phone 대상)이 같은 번호의 재가입을 막지 못했다. phone_norm(숫자만) 전용 컬럼 + 그 컬럼의
-- UNIQUE 인덱스로 중복 판정을 통일한다. 기존 phone 컬럼·값·응답은 절대 바꾸지 않는다(그대로 둔다).
ALTER TABLE MEMBERS
  ADD COLUMN IF NOT EXISTS phone_norm VARCHAR(20) NULL
    COMMENT '휴대폰번호 숫자만 정규화(SR-231 round2) — 하이픈 포함 기존 값과 신규 숫자만 값의 중복 판정을 이 컬럼 하나로 통일';

-- 기존 행 백필(멱등 — 이미 채워진 행은 WHERE로 제외). 신규 가입 INSERT는 서비스가 이미 숫자만인
-- target을 그대로 phone/phone_norm 양쪽에 넣으므로 REPLACE가 필요 없지만, 기존 시드 데이터는
-- 하이픈·공백이 섞여 있어 백필 시에만 정규화한다.
UPDATE MEMBERS
   SET phone_norm = REPLACE(REPLACE(phone, '-', ''), ' ', '')
 WHERE phone_norm IS NULL
   AND phone IS NOT NULL;

-- round1의 uq_members_phone(원문 phone 대상)을 그대로 두면, 신규 가입끼리 같은 숫자를 다시
-- 넣을 때 uq_members_phone과 uq_members_phone_norm 두 제약이 동시에 위반되어 MariaDB가 어느
-- 키 이름을 예외 메시지에 실을지 보장되지 않는다(DuplicateKeyException 메시지의 제약 이름으로
-- 분기하는 서비스 로직이 흔들릴 위험) — 그래서 원문 인덱스는 제거하고 정규화 인덱스 하나만
-- 남긴다(중복 판정은 이 인덱스로 통일, 사람 결정).
ALTER TABLE MEMBERS DROP INDEX IF EXISTS uq_members_phone;
ALTER TABLE MEMBERS ADD UNIQUE INDEX IF NOT EXISTS uq_members_phone_norm (phone_norm);

-- (B) 회원 ID 원자 채번(QA FAIL 필수3) — round1의 in-memory AtomicInteger(JVM 기동마다 리셋)를
-- 완전히 제거하고, 범용 채번 테이블로 대체한다. MEMBERS.member_id는 AUTO_INCREMENT가 아닌
-- 접두 포맷("M-%04d") PK라 DB의 LAST_INSERT_ID(expr) 트릭(MySQL/MariaDB 매뉴얼이 명시하는
-- "AUTO_INCREMENT 없는 원자 채번" 관용구)으로 재기동에도 안전하게 채번한다.
CREATE TABLE IF NOT EXISTS ID_SEQUENCES (
  name      VARCHAR(50) NOT NULL COMMENT '시퀀스 이름(예: MEMBER_ID) — 이 SR은 MEMBER_ID만 사용',
  next_val  BIGINT      NOT NULL COMMENT '직전에 채번된 값(다음 채번은 INSERT..ON DUPLICATE KEY UPDATE로 이 값+1을 원자적으로 반환)',
  PRIMARY KEY (name)
) COMMENT='범용 원자 채번 테이블(SR-231 round2, FUNC-member-003) — AUTO_INCREMENT를 쓸 수 없는 접두 포맷 PK의 원자 채번용';

-- 시드 — 이미 존재하는 회원(M-0001~M-0004 등)과 겹치지 않도록 현재 최대 번호로 시작한다.
-- 멱등: MEMBER_ID 행이 이미 있으면(재기동) 다시 시드하지 않는다 — 이미 진행된 채번을 되돌리면
-- 그 사이 발급된 member_id와 충돌한다.
-- 주의: MAX(...)에 GROUP BY가 없는 집계 쿼리는 WHERE가 걸러낸 행이 0건이어도 결과가 항상 1행
-- (NULL→COALESCE로 0)이다 — WHERE NOT EXISTS를 MEMBERS 쪽에 바로 걸면 재기동마다 이 조건이
-- 거짓이 되어도 "빈 집계"의 그 1행이 매번 INSERT되어 next_val이 0으로 리셋된다(치명적 회귀).
-- 그래서 후보 시드값을 서브쿼리(seed)로 먼저 1행 확정한 뒤, 바깥 WHERE NOT EXISTS가 그 1행
-- 자체를 거를지 말지만 판정하게 한다(존재하면 0행 반환 → INSERT 대상 없음, 존재하지 않으면
-- 1행 반환 → INSERT).
INSERT INTO ID_SEQUENCES (name, next_val)
SELECT seed.name, seed.next_val
  FROM (
    SELECT 'MEMBER_ID' AS name,
           COALESCE(MAX(CAST(SUBSTRING(member_id, 3) AS UNSIGNED)), 0) AS next_val
      FROM MEMBERS
  ) seed
 WHERE NOT EXISTS (SELECT 1 FROM ID_SEQUENCES existing WHERE existing.name = seed.name);

-- (C) 인증코드 대입 시도 제한 + 1회성 소비(QA FAIL 필수1 + 필수4) — MEMBER_SIGNUP_VERIFICATIONS는
-- FUNC-member-002 소유 테이블이지만, consumed_at은 FUNC-member-003(가입완료 API)이 "코드를
-- 한 번만 쓸 수 있게" 하기 위해 전적으로 이 FUNC이 쓰고 읽는 컬럼이다(attempt_count와 동일한
-- 소유 경계 — round1에 이미 그 컬럼을 FUNC-member-002가 "컬럼만 갖고 쓰지 않음"으로 마련해준
-- 전례를 그대로 따른다). MemberSignupCompletionDao(이 FUNC 전용 매퍼)만 이 컬럼을 다룬다 —
-- memberSignupVerification.xml(FUNC-member-002 소유)은 이번에도 건드리지 않는다.
ALTER TABLE MEMBER_SIGNUP_VERIFICATIONS
  ADD COLUMN IF NOT EXISTS consumed_at DATETIME NULL
    COMMENT '코드 소비 시각(가입 성공 트랜잭션 안에서 기록, SR-231 round2, FUNC-member-003 전용) — 같은 코드의 재사용을 막는다';
