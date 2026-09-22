-- linked_func: FUNC-member-002
-- spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
-- SR-231 — 회원가입 인증코드(6자리, 5분 유효) 저장 테이블
-- (FUNC-member-002 = INF-MBR-001 역할 — 인증코드 발송/확인. 가입 요청/회원 생성은
-- FUNC-member-003(INF-MBR-002)의 책임).
--
-- 이 프로젝트에는 마이그레이션 엔진(Flyway/Liquibase)이 없다(pom.xml 확인). Spring Boot의
-- spring.sql.init 스키마 파일 메커니즘으로 기동 시 자동 적용된다(application.yml
-- spring.sql.init.mode=always + schema-locations=이 파일 + member_signup_rate_limits.sql).
--
-- MEMBERS 테이블은 이 변경에서 건드리지 않는다 — 이메일 중복확인·비밀번호·마케팅 동의 저장은
-- FUNC-member-003(INF-MBR-002, 가입 완료 API)의 책임이다.
--
-- round4(2026-09-12, 사람 결정 — "설계를 단순화해서 데드락 원인을 없앤다") — round3는 레이트
-- 리밋 카운터(daily_count/last_requested_at/previous_requested_at)를 이 코드 테이블의 같은
-- 행에 뒀고, 요청 경로가 그 카운터 갱신 + purgeExpired(무인덱스 풀스캔 DELETE)를 같은
-- @Transactional 안에서 실행해 락 범위가 테이블 전체로 확대되어 InnoDB 데드락(500)이 났다
-- (round3 QA FAIL 필수1). round4는 카운터를 전용 테이블(member_signup_rate_limits.sql,
-- MemberSignupRateLimitDao)로 완전히 옮겼다 — 이 테이블은 이제 "인증코드 자체"만 책임진다
-- (code/expires_at/verified_at). round3가 남긴 daily_count/previous_requested_at은
-- deprecated 처리(더 이상 갱신하지 않음, requested_at과 같은 취급 — 멱등 마이그레이션
-- 안전장치로만 유지). last_requested_at은 의미를 바꿔 "이 채널·타깃으로 코드가 마지막으로
-- 실제 발송된 시각"(MemberSignupVerificationDao#writeCode가 매 성공 시 갱신)으로 재정의한다
-- — 더 이상 레이트리밋 판정에 쓰지 않는 순수 정보성 컬럼이다.
--
-- purgeExpired(요청 경로 → 배치로 이전, STORY 재작업 지시 round4-(B), 메서드명도
-- purgeExpiredCodes로 정정)가 이제 순수하게 "코드가 만료된 행"(expires_at < now)만 지운다.
-- 카운터가 이 테이블에 더 이상 없으므로 round2/round3가 겪은 "코드 만료 삭제가 카운터도
-- 함께 지운다" 결합 버그 자체가 구조적으로 재발할 수 없다.
CREATE TABLE IF NOT EXISTS MEMBER_SIGNUP_VERIFICATIONS (
  channel                VARCHAR(10)  NOT NULL COMMENT '인증 채널: EMAIL/SMS',
  target                  VARCHAR(100) NOT NULL COMMENT '인증 대상(이메일 주소 또는 휴대폰번호 원문)',
  code                    CHAR(6)      NULL COMMENT '6자리 인증코드 — 레이트리밋(별도 테이블) 판정을 통과한 요청에만 채워짐',
  expires_at              DATETIME     NULL COMMENT '만료 시각(코드 발급 시점 + 5분, 앱 시계 기준)',
  verified_at             DATETIME     NULL COMMENT '인증 확인 시각(가입완료 단계, FUNC-member-003에서 기록 예정)',
  requested_at            DATETIME     NULL COMMENT '(deprecated, round3) 과거 버전 컬럼 — 더 이상 앱이 쓰지 않는다',
  last_requested_at       DATETIME     NULL COMMENT 'round4 — 의미 변경: 이 채널·타깃으로 코드가 마지막으로 실제 발송된 시각(정보용, writeCode가 갱신). round3까지는 레이트리밋 판정 기준이었으나 round4부터 그 역할은 MEMBER_SIGNUP_RATE_LIMITS로 이전',
  previous_requested_at   DATETIME     NULL COMMENT '(deprecated, round4) round3 레이트리밋 판정용 컬럼 — 카운터가 전용 테이블로 이전되어 더 이상 앱이 쓰지 않는다',
  daily_count             INT          NOT NULL DEFAULT 1 COMMENT '(deprecated, round4) round3 레이트리밋 판정용 컬럼 — MEMBER_SIGNUP_RATE_LIMITS.daily_count로 대체',
  attempt_count           INT          NOT NULL DEFAULT 0 COMMENT '인증코드 대입 시도 횟수 — 이 FUNC은 컬럼만 소유하고 쓰지 않음, FUNC-member-003(가입완료 API)이 코드 검증 시 사용',
  PRIMARY KEY (channel, target)
) COMMENT='회원가입 인증코드(SR-231, FUNC-member-002/INF-MBR-001) — round4부터 레이트리밋 카운터는 별도 테이블(MEMBER_SIGNUP_RATE_LIMITS)';

-- round1→round4 누적 마이그레이션 — 이미 만들어진 구버전 테이블을 안전하게 최신화한다.
-- MariaDB 전용 문법(ADD COLUMN/INDEX IF NOT EXISTS) — 랩은 MariaDB 11.4.5(실측, ADD INDEX IF
-- NOT EXISTS는 10.5.2+ 요구).
ALTER TABLE MEMBER_SIGNUP_VERIFICATIONS
  ADD COLUMN IF NOT EXISTS daily_count INT NOT NULL DEFAULT 1
    COMMENT '(deprecated, round4) round3 레이트리밋 판정용 컬럼 — MEMBER_SIGNUP_RATE_LIMITS.daily_count로 대체';
ALTER TABLE MEMBER_SIGNUP_VERIFICATIONS
  ADD COLUMN IF NOT EXISTS attempt_count INT NOT NULL DEFAULT 0
    COMMENT '인증코드 대입 시도 횟수 — FUNC-member-003(가입완료 API)이 코드 검증 시 사용';
ALTER TABLE MEMBER_SIGNUP_VERIFICATIONS
  ADD COLUMN IF NOT EXISTS last_requested_at DATETIME NULL
    COMMENT 'round4 — 코드가 마지막으로 실제 발송된 시각(정보용, writeCode가 갱신)';
ALTER TABLE MEMBER_SIGNUP_VERIFICATIONS
  ADD COLUMN IF NOT EXISTS previous_requested_at DATETIME NULL
    COMMENT '(deprecated, round4) round3 레이트리밋 판정용 컬럼 — 더 이상 앱이 쓰지 않는다';

-- round3 백필 — 구버전 requested_at 값을 last_requested_at으로 1회 이전한다(멱등: 이미 채워진
-- 행은 WHERE로 제외되어 재실행해도 안전).
UPDATE MEMBER_SIGNUP_VERIFICATIONS
   SET last_requested_at = requested_at
 WHERE last_requested_at IS NULL
   AND requested_at IS NOT NULL;

-- round3 — code/expires_at을 NULL 허용으로 완화(카운터와 코드 기록이 분리되어 있어 카운터만
-- 있는 신규 target 행은 code/expires_at이 잠시 NULL일 수 있었다). round4는 writeCode 자체가
-- 원자 UPSERT이므로 이 완화가 더 이상 필수는 아니지만, 이미 NULL 허용인 기존 컬럼을 다시
-- NOT NULL로 되돌리지 않는다(불필요한 위험 회피).
ALTER TABLE MEMBER_SIGNUP_VERIFICATIONS
  MODIFY COLUMN code CHAR(6) NULL COMMENT '6자리 인증코드 — 레이트리밋 판정을 통과한 요청에만 채워짐';
ALTER TABLE MEMBER_SIGNUP_VERIFICATIONS
  MODIFY COLUMN expires_at DATETIME NULL COMMENT '만료 시각(코드 발급 시점 + 5분, 앱 시계 기준)';
ALTER TABLE MEMBER_SIGNUP_VERIFICATIONS
  MODIFY COLUMN requested_at DATETIME NULL COMMENT '(deprecated) 과거 버전 컬럼 — 더 이상 앱이 쓰지 않는다';

-- round4(STORY 재작업 지시(B)) — "expires_at·last_requested_at에 인덱스를 둔다": 배치
-- (MemberSignupMaintenanceScheduler)의 purgeExpiredCodes가 expires_at으로 스캔하므로
-- 인덱스가 필요하고, last_requested_at도 향후 조회/운영 조사용으로 함께 인덱싱한다.
ALTER TABLE MEMBER_SIGNUP_VERIFICATIONS ADD INDEX IF NOT EXISTS idx_msv_expires_at (expires_at);
ALTER TABLE MEMBER_SIGNUP_VERIFICATIONS ADD INDEX IF NOT EXISTS idx_msv_last_requested_at (last_requested_at);
