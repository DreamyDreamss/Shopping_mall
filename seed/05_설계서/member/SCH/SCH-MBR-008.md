---
sch-id: SCH-MBR-008
table: MEMBER_SIGNUP_VERIFICATIONS
domain: member
domain-code: MBR
inf: [INF-MBR-001, INF-MBR-002]
---

# SCH-MBR-008: MEMBER_SIGNUP_VERIFICATIONS

> **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** INF-MBR-001, INF-MBR-002 | **화면:** [TBD]

**근거 소스:** `modules/shop-api/src/main/resources/db/member_signup_verifications.sql` — CREATE TABLE 문에서 컬럼·타입·NULL·기본값·키를 그대로 옮겼다(DB 실측이 아니라 DDL 기준).

### 컬럼 설명
| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
|--------|------|------|----|--------|------|
| channel | VARCHAR(10) | N | PK | — | 인증 채널: EMAIL/SMS |
| target | VARCHAR(100) | N | PK | — | 인증 대상(이메일 주소 또는 휴대폰번호 원문) |
| code | CHAR(6) | Y |  | NULL | 6자리 인증코드 — 레이트리밋(별도 테이블) 판정을 통과한 요청에만 채워짐 |
| expires_at | DATETIME | Y |  | NULL | 만료 시각(코드 발급 시점 + 5분, 앱 시계 기준) |
| verified_at | DATETIME | Y |  | NULL | 인증 확인 시각(가입완료 단계, FUNC-member-003에서 기록 예정) |
| requested_at | DATETIME | Y |  | NULL | (deprecated, round3) 과거 버전 컬럼 — 더 이상 앱이 쓰지 않는다 |
| last_requested_at | DATETIME | Y |  | NULL | round4 — 의미 변경: 이 채널·타깃으로 코드가 마지막으로 실제 발송된 시각(정보용, writeCode가 갱신). round3까지는 레이트리밋 판정 기준이었으나 round4부터 그 역할은 MEMBER_SIGNUP_RATE_LIMITS로 이전 |
| previous_requested_at | DATETIME | Y |  | NULL | (deprecated, round4) round3 레이트리밋 판정용 컬럼 — 카운터가 전용 테이블로 이전되어 더 이상 앱이 쓰지 않는다 |
| daily_count | INT | N |  | 1 | (deprecated, round4) round3 레이트리밋 판정용 컬럼 — MEMBER_SIGNUP_RATE_LIMITS.daily_count로 대체 |
| attempt_count | INT | N |  | 0 | 인증코드 대입 시도 횟수 — 이 FUNC은 컬럼만 소유하고 쓰지 않음, FUNC-member-003(가입완료 API)이 코드 검증 시 사용 |

### 인덱스
| 인덱스명 | 컬럼 | 타입 | 목적 |
|---------|------|------|------|
| — | — | — | — |

### 코드값

[TBD]

### 관계 (FK / 관찰된 조인)
> 출처 `DB FK`=DB 선언 제약, `쿼리관찰(N)`=소스 SQL에서 N회 등장한 등가조인(논리 FK).

| 자식 컬럼 | 참조 테이블 | 참조 컬럼 | 출처 | ON DELETE |
|----------|-----------|----------|------|-----------|
| — | — | — | — | — |

### mini-ERD
```
MEMBER_SIGNUP_VERIFICATIONS
```

### 비즈니스 주의사항

[TBD] — 보강: `/sl-sync --apply --kind=sch` 또는 ddd-db-agent
