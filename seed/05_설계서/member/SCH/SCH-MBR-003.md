---
sch-id: SCH-MBR-003
table: MEMBER_API_KEYS
domain: member
domain-code: MBR
inf: [INF-MBR-003, INF-MBR-004, INF-MBR-005]
---

# SCH-MBR-003: MEMBER_API_KEYS

> **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** INF-MBR-003, INF-MBR-004, INF-MBR-005 | **화면:** [TBD]

**근거 소스:** `modules/shop-api/src/main/resources/db/V4__members_login.sql` — CREATE TABLE 문에서 컬럼·타입·NULL·기본값·키를 그대로 옮겼다(DB 실측이 아니라 DDL 기준).

### 컬럼 설명
| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
|--------|------|------|----|--------|------|
| member_id | VARCHAR(20) | N | PK | — | MEMBERS.member_id |
| api_key | VARCHAR(64) | N |  | — | 발급된 API 키(정적 lab.api-keys 맵과 별개 — DB 폴백 조회 전용) |
| issued_at | DATETIME(3) | N |  | — | 최초 발급 시각 |
| revoked_at | DATETIME(3) | Y |  | NULL | 폐기 시각 — round 9(재작업 지시 2) 추가. 로그인(이 FUNC)은 항상 NULL로만 INSERT하고 세팅하지 않는다. 폐기는 FUNC-006(로그아웃) 소관 — 006이 로그아웃 시 이 컬럼을 세팅하는 인터페이스로 못박는다(STORY "006과의 인터페이스 가정" 참고). |

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
MEMBER_API_KEYS
```

### 비즈니스 주의사항

[TBD] — 보강: `/sl-sync --apply --kind=sch` 또는 ddd-db-agent
