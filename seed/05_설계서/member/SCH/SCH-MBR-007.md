---
sch-id: SCH-MBR-007
table: MEMBER_SIGNUP_RATE_LIMITS
domain: member
domain-code: MBR
inf: [INF-MBR-001]
---

# SCH-MBR-007: MEMBER_SIGNUP_RATE_LIMITS

> **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** INF-MBR-001 | **화면:** [TBD]

**근거 소스:** `modules/shop-api/src/main/resources/db/member_signup_rate_limits.sql` — CREATE TABLE 문에서 컬럼·타입·NULL·기본값·키를 그대로 옮겼다(DB 실측이 아니라 DDL 기준).

### 컬럼 설명
| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
|--------|------|------|----|--------|------|
| target | VARCHAR(100) | N | PK | — | 인증 대상(이메일 또는 휴대폰번호 원문) — 채널 무관, target 단위로 레이트리밋 |
| day_key | DATE | N | PK | — | 요청 날짜(앱 시계 기준 로컬 날짜) — daily_count의 하루 경계 |
| daily_count | INT | N |  | 1 | 이 target의 이 날짜에 허용된 요청 횟수(거부된 요청은 세지 않음) |
| last_requested_at | DATETIME(3) | N |  | — | 이 target의 이 날짜 마지막으로 허용된 요청 시각(쿨다운 60초 판정 기준) |
| last_token | VARCHAR(36) | Y |  | NULL | round5 — 마지막으로 "허용"을 기록한 요청의 UUID 토큰. touchRateLimit이 조건부로 갱신하고, selectRateLimit이 재조회한 값이 호출자 자신의 토큰과 같으면 허용(200)으로 판정 |

### 인덱스
| 인덱스명 | 컬럼 | 타입 | 목적 |
|---------|------|------|------|
| idx_msrl_day_key | day_key | INDEX | [TBD] |

### 코드값

[TBD]

### 관계 (FK / 관찰된 조인)
> 출처 `DB FK`=DB 선언 제약, `쿼리관찰(N)`=소스 SQL에서 N회 등장한 등가조인(논리 FK).

| 자식 컬럼 | 참조 테이블 | 참조 컬럼 | 출처 | ON DELETE |
|----------|-----------|----------|------|-----------|
| — | — | — | — | — |

### mini-ERD
```
MEMBER_SIGNUP_RATE_LIMITS
```

### 비즈니스 주의사항

[TBD] — 보강: `/sl-sync --apply --kind=sch` 또는 ddd-db-agent
