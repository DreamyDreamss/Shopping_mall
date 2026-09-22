-- linked_func: FUNC-member-005
-- spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md
-- SR-232 — 로그인 API(INF-MBR-003)를 위한 신규 3테이블. 기존 테이블(MEMBERS 등)은 컬럼 추가조차
-- 하지 않는다 — SR 확정 문답 "DB 변경 없음"은 "타 도메인 테이블·집계 파급 없음/마이그레이션
-- (기존 데이터 이관) 없음"으로 해석한다(SR-231 V3__members_signup.sql이 ID_SEQUENCES를 신설한
-- 전례와 동일 해석, STORY "데이터" 절 참고).
--
-- 이 프로젝트에는 마이그레이션 엔진(Flyway/Liquibase)이 없다(pom.xml 확인) — Spring Boot
-- spring.sql.init 스키마 파일 메커니즘으로 기동마다 재실행된다(application.yml
-- schema-locations에 이 파일을 추가해야 실제로 적용된다).

-- 로그인 실패 카운터 + 잠금 — email 문자열 자체가 PK다(회원 존재 여부와 무관하게 카운트).
-- 이렇게 해야 "이 이메일이 잠겼는가"라는 관찰이 계정 존재의 오라클이 되지 않는다 — 그 이메일로
-- 실패를 시도한 사람 자신만 그 사실을 안다.
-- 컬럼은 DATETIME(3)(밀리초 정밀도) — MEMBER_SIGNUP_RATE_LIMITS.last_requested_at과 동일 관례
-- (테스트가 LocalDateTime.now().truncatedTo(MILLIS)로 왕복 등치를 확인한다).
CREATE TABLE IF NOT EXISTS MEMBER_LOGIN_ATTEMPTS (
  email           VARCHAR(255) NOT NULL COMMENT '로그인 시도 대상 이메일(회원 미존재도 포함) — FUNC-member-005',
  fail_count      INT          NOT NULL DEFAULT 0 COMMENT '연속 실패 횟수(잠금 만료 후 재실패 시 1로 리셋 — 무한 누적 방지)',
  locked_until    DATETIME(3)  NULL     COMMENT '이 시각까지 잠김(NULL이면 미잠김) — 5회 도달 시 세팅',
  last_failed_at  DATETIME(3)  NULL     COMMENT '마지막 실패 시각',
  PRIMARY KEY (email)
) COMMENT='로그인 실패 카운터 + 잠금(SR-232, FUNC-member-005) — 5회 실패 시 10분 잠금';

-- 리프레시 토큰 — 원문은 저장하지 않는다(SHA-256 해시만, token_hash가 PK). FUNC-006(로그아웃/
-- 재발급)이 조회·폐기에 이 테이블을 그대로 재사용한다 — 006이 컬럼이 더 필요하면 006 소유
-- 마이그레이션 파일에서 ADD COLUMN IF NOT EXISTS로 추가하고 이 V4 파일(005 소유)은 건드리지
-- 않는다(MEMBER_SIGNUP_VERIFICATIONS를 002가 소유하고 consumed_at을 003이 자기 파일에서
-- 추가한 전례와 동일 경계).
CREATE TABLE IF NOT EXISTS MEMBER_REFRESH_TOKENS (
  token_hash   VARCHAR(64)  NOT NULL COMMENT 'SHA-256 hex(소문자) — 원문 미저장',
  member_id    VARCHAR(20)  NOT NULL COMMENT 'MEMBERS.member_id',
  issued_at    DATETIME(3)  NOT NULL COMMENT '발급 시각',
  expires_at   DATETIME(3)  NOT NULL COMMENT '만료 시각(발급+30일)',
  revoked_at   DATETIME(3)  NULL     COMMENT '폐기 시각(로그아웃) — FUNC-006 전용, 이 FUNC(005)은 항상 NULL로만 INSERT',
  PRIMARY KEY (token_hash)
) COMMENT='리프레시 토큰(SR-232, FUNC-member-005 소유 — FUNC-006이 조회/폐기만 재사용)';

-- 회원 API 키 — 계획 상단 "전제": 정적 lab.api-keys 맵(application.yml)에 없는 자가가입 회원이
-- 로그인 후 API를 쓸 수 있게 한다(사람 확인 3, ApiKeyAuthFilter DB 폴백). member_id가 PK라
-- 회원당 키 1개만 존재하고, 최초 로그인 동시 레이스는 issueIfAbsent(no-op UPSERT)+재조회로
-- 1개만 발급된다. api_key는 UNIQUE — ApiKeyAuthFilter가 키→memberId 역방향 조회에 쓴다.
CREATE TABLE IF NOT EXISTS MEMBER_API_KEYS (
  member_id   VARCHAR(20)  NOT NULL COMMENT 'MEMBERS.member_id',
  api_key     VARCHAR(64)  NOT NULL COMMENT '발급된 API 키(정적 lab.api-keys 맵과 별개 — DB 폴백 조회 전용)',
  issued_at   DATETIME(3)  NOT NULL COMMENT '최초 발급 시각',
  revoked_at  DATETIME(3)  NULL     COMMENT '폐기 시각 — round 9(재작업 지시 2) 추가. 로그인(이 FUNC)은 항상 NULL로만 INSERT하고 세팅하지 않는다. 폐기는 FUNC-006(로그아웃) 소관 — 006이 로그아웃 시 이 컬럼을 세팅하는 인터페이스로 못박는다(STORY "006과의 인터페이스 가정" 참고).',
  PRIMARY KEY (member_id),
  UNIQUE INDEX uq_member_api_keys_api_key (api_key)
) COMMENT='회원 API 키(SR-232, FUNC-member-005) — ApiKeyAuthFilter 정적 맵 미스 시 폴백 조회 대상';

-- round 9(재작업 지시 2, QA FAIL 필수2 재발 방지) — 이 DB에는 위 CREATE TABLE이 revoked_at 없이
-- 이미 실행된 상태라 IF NOT EXISTS는 컬럼을 추가하지 않는다. 기존 설치를 안전하게 올리기 위한
-- 멱등 ALTER(MariaDB 전용 문법, member_signup_verifications.sql·V3__members_signup.sql과 동일 관례).
ALTER TABLE MEMBER_API_KEYS
  ADD COLUMN IF NOT EXISTS revoked_at DATETIME(3) NULL
    COMMENT '폐기 시각 — round 9(재작업 지시 2) 추가. FUNC-006(로그아웃) 소관, 이 FUNC(005)은 세팅하지 않는다';
