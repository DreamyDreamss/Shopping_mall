---
sch-id: SCH-MBR-005
table: MEMBER_PASSWORD_RESETS
domain: member
domain-code: MBR
inf: [INF-MBR-006, INF-MBR-007]
---

# SCH-MBR-005: MEMBER_PASSWORD_RESETS

> **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** INF-MBR-006, INF-MBR-007 | **화면:** [TBD]

**근거 소스:** `modules/shop-api/src/main/resources/db/V5__member_password_resets.sql` — CREATE TABLE 문에서 컬럼·타입·NULL·기본값·키를 그대로 옮겼다(DB 실측이 아니라 DDL 기준).

### 컬럼 설명
| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
|--------|------|------|----|--------|------|
| target | VARCHAR(100) | N | PK | — | 재설정 대상(이메일 또는 휴대폰번호, 정규화된 값) — 단일 컬럼(email trim+소문자 또는 phone 숫자만) |
| code_hash | CHAR(64) | N |  | — | SHA-256 hex(소문자) — 코드 원문은 저장하지 않음 |
| expires_at | DATETIME(3) | N |  | — | 코드 만료 시각(발급+10분) |
| consumed_at | DATETIME(3) | Y |  | NULL | 코드 소비(확정 완료) 시각 — 이 FUNC(008)은 항상 NULL로만 쓰고 세팅하지 않음. FUNC-009(확정 API) 소관 |
| attempt_count | INT | N |  | 0 | 확정 시도 횟수 — 이 FUNC(008)은 항상 0으로만 두고 증가시키지 않음. FUNC-009(확정 API) 소관 |
| created_at | DATETIME(3) | N |  | — | 이 코드가 마지막으로 발급된 시각(쿨다운 60초 판정 기준) — ON DUPLICATE KEY UPDATE SET 목록의 맨 뒤에서만 갱신되어 앞선 IF 조건들이 항상 갱신 전 값을 참조한다(MEMBER_SIGNUP_RATE_LIMITS round6과 동일 원리) |

### 인덱스
| 인덱스명 | 컬럼 | 타입 | 목적 |
|---------|------|------|------|
| idx_mpr_expires_at | expires_at | INDEX | 정리 배치(BAT-MBR-001)가 `expires_at < now - 보존기간`으로 만료 행을 조건부 DELETE할 때 풀스캔을 피한다(SR-297 #1, `V9__member_password_reset_rate_limits.sql`에서 추가 — DDL 원본 `V5__member_password_resets.sql`은 무변경) |

### 코드값

없음 — 코드성(열거값) 컬럼이 없다. 상태는 `consumed_at`(NULL=미소비)·`expires_at`(현재 시각 비교)·`attempt_count`(확정 오답 누적, 상한 5)로 판정한다.

### 관계 (FK / 관찰된 조인)
> 출처 `DB FK`=DB 선언 제약, `쿼리관찰(N)`=소스 SQL에서 N회 등장한 등가조인(논리 FK).

| 자식 컬럼 | 참조 테이블 | 참조 컬럼 | 출처 | ON DELETE |
|----------|-----------|----------|------|-----------|
| — | — | — | — | — |

### mini-ERD
```
MEMBER_PASSWORD_RESETS
```

### 비즈니스 주의사항

- target당 1행(PK) — 재요청은 같은 행을 `INSERT … ON DUPLICATE KEY UPDATE`로 덮어쓴다(새 코드·만료·`consumed_at`/`attempt_count` 초기화). `created_at`은 SET 목록 맨 뒤에서만 갱신돼 쿨다운(60초) 판정이 갱신 전 값을 본다.
- 코드 원문은 저장하지 않는다(`code_hash` = SHA-256 hex).
- 만료된 행은 곧바로 지워지지 않는다 — 정리 배치(BAT-MBR-001, `MemberPasswordResetMaintenanceScheduler`)가 `expires_at < now - 보존기간(member.password-reset.purge-retention, 기본 P7D)`인 행만 매일 조건부 DELETE한다(SR-297 #1). 보존기간 안에서는 행이 남아 있어도 확정 API가 `expires_at`으로 계속 410(`MBR-4101`)을 낸다 — 보존기간을 넘겨 행이 지워지면 확정 시도는 409(`MBR-4102`, 오답과 동일 코드)로 바뀐다(알려진 동작, INF-MBR-007 참고).
- `attempt_count` 증가는 확정 API(INF-MBR-007)가 조건부 원자 UPDATE(`attempt_count < 상한`)로만 한다(SR-298 #2) — 읽고 비교한 뒤 쓰지 않는다.
- 근거: DDL `V5__member_password_resets.sql` 주석 + 매퍼 `memberPasswordReset.xml`(소스 기준, DB 실측 아님).

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-16 | SR-297 | #1 | expires_at 인덱스(idx_mpr_expires_at) 추가 + 만료 행 보존기간(P7D) 정리 배치 반영 — 확정 API 410→409 전환 시점 명시 | shop-api@b7ae32f |
| 2026-09-16 | SR-297 | #2 | 스키마 무변경 확인(컬럼·타입·NULL·기본값·PK 그대로) — 이 항목은 이 테이블을 읽거나 쓰지 않음, AC로 검증됨 | shop-api@b7ae32f |
