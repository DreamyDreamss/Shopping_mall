-- linked_func: FUNC-member-009
-- spec: docs/00_FUNC/stories/STORY-FUNC-member-009.md
-- SR-234 — 비밀번호 재설정 확정 API(INF-MBR-007). MEMBERS.updated_at 컬럼만 추가한다(SR db_ripple
-- 답변 "MEMBERS.password_hash 갱신·updated_at" 근거) — 기존 컬럼은 문자 하나도 건드리지 않는다
-- (add-only, 규칙 ddl-idempotent). 기존 회원의 이 컬럼은 계속 NULL(백필 없음, SR "기존 데이터
-- 이관·백필 불필요"와 일관). MEMBER_PASSWORD_RESETS(FUNC-008 소유 테이블)는 이 FUNC이 스키마를
-- 바꾸지 않는다 — consumed_at/attempt_count는 이미 그 테이블에 있고, 이 FUNC은 값만 갱신한다.
ALTER TABLE MEMBERS
  ADD COLUMN IF NOT EXISTS updated_at DATETIME(3) NULL
    COMMENT '마지막 갱신 시각 — 현재는 비밀번호 재설정 확정(FUNC-member-009)만 세팅. 기존 행은 계속 NULL';
