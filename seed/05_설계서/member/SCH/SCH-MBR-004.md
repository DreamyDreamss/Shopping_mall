---
sch-id: SCH-MBR-004
table: MEMBER_LOGIN_ATTEMPTS
domain: member
domain-code: MBR
inf: [INF-MBR-003]
---

# SCH-MBR-004: MEMBER_LOGIN_ATTEMPTS

> **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** INF-MBR-003 | **화면:** [TBD]

**근거 소스:** `modules/shop-api/src/main/resources/db/V4__members_login.sql` — CREATE TABLE 문에서 컬럼·타입·NULL·기본값·키를 그대로 옮겼다(DB 실측이 아니라 DDL 기준).

### 컬럼 설명
| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
|--------|------|------|----|--------|------|
| email | VARCHAR(255) | N | PK | — | 로그인 시도 대상 이메일(회원 미존재도 포함) — FUNC-member-005 |
| fail_count | INT | N |  | 0 | 연속 실패 횟수(잠금 만료 후 재실패 시 1로 리셋 — 무한 누적 방지) |
| locked_until | DATETIME(3) | Y |  | NULL | 이 시각까지 잠김(NULL이면 미잠김) — 5회 도달 시 세팅 |
| last_failed_at | DATETIME(3) | Y |  | NULL | 마지막 실패 시각 |

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
MEMBER_LOGIN_ATTEMPTS
```

### 비즈니스 주의사항

[TBD] — 보강: `/sl-sync --apply --kind=sch` 또는 ddd-db-agent
