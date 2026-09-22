-- SR-297 #1(BAT-MBR-001) — 비밀번호 재설정 코드 요청 전용 일일 상한 카운터 테이블.
-- MEMBER_SIGNUP_RATE_LIMITS(SR-231 round4/round5, member_signup_rate_limits.sql)와 완전히 같은
-- 모양으로 만든다 — 별도 테이블이라 코드 테이블(MEMBER_PASSWORD_RESETS)의 락과 절대 섞이지 않고,
-- PK(target, day_key)라 날짜가 바뀌면 새 행이 INSERT되어 daily_count가 자연히 1부터 다시 시작한다.
--
-- 이 항목(#1)은 daily_count/last_requested_at/last_token 세 컬럼을 읽거나 쓰지 않는다 — 실제로 쓰는
-- 컬럼은 day_key뿐이다(정리 배치의 조건부 DELETE WHERE절). 일일 상한 판정(원자 UPSERT로 세 컬럼을
-- 갱신하는 로직)은 #2(INF-MBR-006) 몫이며, 그때 다시 ALTER TABLE을 하지 않도록 최종 컬럼 모양을
-- 여기서 한 번에 만들어 둔다.
CREATE TABLE IF NOT EXISTS MEMBER_PASSWORD_RESET_RATE_LIMITS (
  target             VARCHAR(100) NOT NULL COMMENT '재설정 코드 요청 대상(이메일 원문) — target 단위로 일일 상한',
  day_key            DATE         NOT NULL COMMENT '요청 날짜(앱 시계 기준 로컬 날짜) — daily_count의 하루 경계',
  daily_count        INT          NOT NULL DEFAULT 1 COMMENT '이 target의 이 날짜에 허용된 요청 횟수(#2가 원자 UPSERT로 갱신 — 이 항목은 쓰지 않음)',
  last_requested_at  DATETIME(3)  NOT NULL COMMENT '이 target의 이 날짜 마지막으로 허용된 요청 시각(#2의 쿨다운 판정 기준 — 이 항목은 쓰지 않음)',
  last_token         VARCHAR(36)  NULL COMMENT '#2가 조건부로 갱신할 요청 토큰(affected-rows 대신 재조회 판정용) — 이 항목은 쓰지 않음',
  PRIMARY KEY (target, day_key),
  KEY idx_mprl_day_key (day_key)
) COMMENT='비밀번호 재설정 코드 요청 일일 상한 전용 카운터(SR-297 #1) — MEMBER_PASSWORD_RESETS(코드 테이블)와 완전히 분리된 락 범위';

-- SR-297 #1 재작업(round1 QA CONCERNS 권고2) — MEMBER_PASSWORD_RESETS.expires_at에 인덱스가
-- 없어(실측: PRIMARY(target) 하나뿐) 정리 배치의 DELETE ... WHERE expires_at < #{beforeExpiresAt}가
-- 풀스캔이었다. InnoDB REPEATABLE READ에서는 스캔한 모든 행·갭에 next-key lock이 걸려 계획의
-- "매칭된 행만 잠근다" 전제가 성립하지 않는다(사례집 SR-231 r3와 동일 계열). 형제 배치가 이미 같은
-- 이유로 idx_msv_expires_at을 추가한 선례(member_signup_verifications.sql:78-82, round4)를
-- 그대로 따른다 — 단, 대상 테이블(MEMBER_PASSWORD_RESETS)의 원본 DDL(V5)은 건드리지 않고 이
-- V9 파일에 별도 인덱스 추가문만 둔다(기동마다 재실행되어도 안전하도록 IF NOT EXISTS).
CREATE INDEX IF NOT EXISTS idx_mpr_expires_at ON MEMBER_PASSWORD_RESETS (expires_at);
