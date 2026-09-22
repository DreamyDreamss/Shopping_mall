---
sch-id: SCH-MBR-006
table: MEMBER_REFRESH_TOKENS
domain: member
domain-code: MBR
inf: [INF-MBR-003, INF-MBR-004, INF-MBR-005]
---

# SCH-MBR-006: MEMBER_REFRESH_TOKENS

> **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** INF-MBR-003, INF-MBR-004, INF-MBR-005 | **화면:** [TBD]

**근거 소스:** `modules/shop-api/src/main/resources/db/V4__members_login.sql` — CREATE TABLE 문에서 컬럼·타입·NULL·기본값·키를 그대로 옮겼다(DB 실측이 아니라 DDL 기준).

### 컬럼 설명
| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
|--------|------|------|----|--------|------|
| token_hash | VARCHAR(64) | N | PK | — | SHA-256 hex(소문자) — 원문 미저장 |
| member_id | VARCHAR(20) | N |  | — | MEMBERS.member_id |
| issued_at | DATETIME(3) | N |  | — | 발급 시각 |
| expires_at | DATETIME(3) | N |  | — | 만료 시각(발급+30일) |
| revoked_at | DATETIME(3) | Y |  | NULL | 폐기 시각(로그아웃) — FUNC-006 전용, 이 FUNC(005)은 항상 NULL로만 INSERT |

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
MEMBER_REFRESH_TOKENS
```

### 비즈니스 주의사항

[TBD] — 보강: `/sl-sync --apply --kind=sch` 또는 ddd-db-agent
