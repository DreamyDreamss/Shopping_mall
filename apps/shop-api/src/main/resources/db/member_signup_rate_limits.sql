-- linked_func: FUNC-member-002
-- spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
-- SR-231 round4(2026-09-12, 사람 결정 — "설계를 단순화해서 데드락 원인을 없앤다") — 회원가입
-- 인증코드 레이트리밋 카운터 전용 테이블. round3까지는 이 카운터가 코드 테이블
-- (MEMBER_SIGNUP_VERIFICATIONS)에 함께 있었고, 요청 경로가 @Transactional로 감싼 채
-- purgeExpired(무인덱스 풀스캔 DELETE)까지 같은 트랜잭션에서 돌려 락 범위가 테이블 전체로
-- 확대되어 InnoDB 데드락(500)이 났다(round3 QA FAIL 필수1). round4는:
--   (1) 카운터를 이 독립 테이블로 옮겨 코드 테이블의 락과 절대 섞이지 않게 하고,
--   (2) 요청 경로에서 @Transactional을 완전히 제거해 단일 UPSERT 문(autocommit)만 실행하고,
--   (3) 정리(purge)는 요청 경로 밖의 배치(@Scheduled)로 옮긴다(member_signup_verifications.sql
--       쪽의 purgeExpiredCodes와 이 테이블의 purgeOldRows, 둘 다
--       MemberSignupMaintenanceScheduler가 호출).
--
-- PK를 (target, day_key)로 잡아 "오늘 몇 번째 요청인가"를 날짜별로 분리했다 — 날짜가 바뀌면
-- 그 target의 새 (target, 새 day_key) 행이 INSERT 브랜치로 만들어지므로 daily_count가 자연히
-- 1부터 다시 시작한다(round2/round3가 겪었던 "day rollover를 별도 수식으로 판정" 문제 자체가
-- 구조적으로 사라진다).
--
-- 원자 판정: INSERT ... ON DUPLICATE KEY UPDATE 한 문장으로 쿨다운(60초)·일일상한(5회)을
-- 동시에 만족할 때만 daily_count/last_requested_at/last_token을 갱신한다. 조건 미달이면 그
-- UPDATE는 IF의 else 분기로 기존 값을 그대로 대입하므로 실제 행 변경이 없다.
--
-- round5(2026-09-12, 사람 결정 — round4 QA FAIL 재작업 지시(1)) — last_token 컬럼 추가.
-- round4는 200/429 판정을 JDBC affected-rows(신규 삽입=1/실제 값 변경=2/무변경=0)로 했는데,
-- 이 반환값의 의미는 datasource URL의 useAffectedRows=true(전역 커넥션 속성)에 의존했다.
-- 그런데 이 속성은 이 FUNC 밖 모든 UPDATE의 반환값 시맨틱을 바꿔, ProductDao.decreaseStock
-- (FUNC-order-002)이 qty=0 주문 라인에서 종전 1(matched) 대신 0(무변경)을 반환하게 됐고
-- OrderService.create가 그 0을 "재고 부족"으로 해석해 POST /api/orders가 200→409로
-- 회귀했다(QA 실측). round5는 useAffectedRows=true를 datasource URL에서 제거하고(다른 FUNC의
-- 전역 계약을 다시 건드리지 않음), 요청마다 만든 UUID 토큰을 last_token에 조건부로 기록해
-- MemberSignupRateLimitDao#selectRateLimit으로 재조회한 값이 "내 토큰"이면 허용으로
-- 판정한다 — affected-rows 의미에 더 이상 의존하지 않는다(application.yml, MemberSignupService
-- 참고).
CREATE TABLE IF NOT EXISTS MEMBER_SIGNUP_RATE_LIMITS (
  target             VARCHAR(100) NOT NULL COMMENT '인증 대상(이메일 또는 휴대폰번호 원문) — 채널 무관, target 단위로 레이트리밋',
  day_key            DATE         NOT NULL COMMENT '요청 날짜(앱 시계 기준 로컬 날짜) — daily_count의 하루 경계',
  daily_count        INT          NOT NULL DEFAULT 1 COMMENT '이 target의 이 날짜에 허용된 요청 횟수(거부된 요청은 세지 않음)',
  last_requested_at  DATETIME(3)  NOT NULL COMMENT '이 target의 이 날짜 마지막으로 허용된 요청 시각(쿨다운 60초 판정 기준)',
  last_token         VARCHAR(36)  NULL COMMENT 'round5 — 마지막으로 "허용"을 기록한 요청의 UUID 토큰. touchRateLimit이 조건부로 갱신하고, selectRateLimit이 재조회한 값이 호출자 자신의 토큰과 같으면 허용(200)으로 판정',
  PRIMARY KEY (target, day_key),
  KEY idx_msrl_day_key (day_key)
) COMMENT='회원가입 인증코드 레이트리밋 전용 카운터(SR-231 round4/round5) — 코드 테이블(MEMBER_SIGNUP_VERIFICATIONS)과 완전히 분리된 락 범위';

-- round4→round5 누적 마이그레이션 — 이미 만들어진 round4 테이블에 last_token 컬럼을 추가한다
-- (MariaDB 전용 문법 ADD COLUMN IF NOT EXISTS — 랩은 MariaDB 11.4.5 실측).
ALTER TABLE MEMBER_SIGNUP_RATE_LIMITS
  ADD COLUMN IF NOT EXISTS last_token VARCHAR(36) NULL
    COMMENT 'round5 — 마지막으로 "허용"을 기록한 요청의 UUID 토큰(affected-rows 판정 대체)';
