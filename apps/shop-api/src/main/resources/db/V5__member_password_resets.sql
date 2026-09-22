-- linked_func: FUNC-member-008
-- spec: docs/00_FUNC/stories/STORY-FUNC-member-008.md
-- SR-234 — 비밀번호 재설정 코드 요청 API(INF-MBR-006) 전용 테이블. SR 원문은 파일명을
-- "V4__member_password_resets.sql"이라 적었으나 V4__members_login.sql(FUNC-member-005 소유)이
-- 이미 V4를 쓰고 있어 V5로 정정한다(STORY "파일" 절 참고, Flyway 미사용 — 실제 적용은
-- application.yml spring.sql.init.schema-locations 목록 순서다).
--
-- PK는 target(단일 컬럼) — SR 원문의 "대상 email/phone_norm"은
-- MEMBER_SIGNUP_VERIFICATIONS.target과 동일하게 정규화된 값 하나로 저장한다(email은
-- trim+소문자, phone은 숫자만). 같은 대상의 다른 표기('010-1234-5678' vs '01012345678',
-- 'Foo@Bar.COM' vs 'foo@bar.com')는 정규화 후 같은 행을 공유해 같은 쿨다운이 걸린다(STORY
-- "데이터" 절, 사람 수정 (1)). channel 컬럼은 두지 않는다 — 요청마다 정규화된 target 형식으로
-- 다시 판별하면 되고 영속화가 필요 없다.
--
-- 이 테이블은 이 FUNC(008, 요청 API)만 쓴다. 코드 확인 + 새 비밀번호 반영(FUNC-member-009,
-- 확정 API)이 consumed_at/attempt_count를 갱신하며 이 테이블을 재사용할 예정이다(STORY "폴백·
-- 우회 경로의 자격 판정" 절 — 이 FUNC 범위 밖이지만 다음 FUNC이 마주칠 조건으로 남긴다).
CREATE TABLE IF NOT EXISTS MEMBER_PASSWORD_RESETS (
  target         VARCHAR(100) NOT NULL COMMENT '재설정 대상(이메일 또는 휴대폰번호, 정규화된 값) — 단일 컬럼(email trim+소문자 또는 phone 숫자만)',
  code_hash      CHAR(64)     NOT NULL COMMENT 'SHA-256 hex(소문자) — 코드 원문은 저장하지 않음',
  expires_at     DATETIME(3)  NOT NULL COMMENT '코드 만료 시각(발급+10분)',
  consumed_at    DATETIME(3)  NULL     COMMENT '코드 소비(확정 완료) 시각 — 이 FUNC(008)은 항상 NULL로만 쓰고 세팅하지 않음. FUNC-009(확정 API) 소관',
  attempt_count  INT          NOT NULL DEFAULT 0 COMMENT '확정 시도 횟수 — 이 FUNC(008)은 항상 0으로만 두고 증가시키지 않음. FUNC-009(확정 API) 소관',
  created_at     DATETIME(3)  NOT NULL COMMENT '이 코드가 마지막으로 발급된 시각(쿨다운 60초 판정 기준) — ON DUPLICATE KEY UPDATE SET 목록의 맨 뒤에서만 갱신되어 앞선 IF 조건들이 항상 갱신 전 값을 참조한다(MEMBER_SIGNUP_RATE_LIMITS round6과 동일 원리)',
  PRIMARY KEY (target)
) COMMENT='비밀번호 재설정 코드 요청(SR-234, FUNC-member-008) — target 단위 쿨다운(60초)·만료(10분) 관리';
