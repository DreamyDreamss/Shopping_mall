---
sch-id: SCH-MBR-002
table: MEMBER_ADDRESSES
domain: member
domain-code: MBR
inf: [INF-MBR-008]
---

# SCH-MBR-002: MEMBER_ADDRESSES

> **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** INF-MBR-008 | **화면:** [TBD]

**근거 소스:** `modules/shop-api/src/main/resources/db/V7__member_addresses.sql` — CREATE TABLE 문에서 컬럼·타입·NULL·기본값·키를 그대로 옮겼다(DB 실측이 아니라 DDL 기준).

### 컬럼 설명
| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
|--------|------|------|----|--------|------|
| address_id | BIGINT | N | PK | NULL | [TBD] |
| member_id | VARCHAR(20) | N |  | — | [TBD] |
| recipient | VARCHAR(50) | N |  | — | [TBD] |
| phone | VARCHAR(20) | N |  | — | [TBD] |
| phone_norm | VARCHAR(20) | N |  | — | [TBD] |
| zipcode | VARCHAR(10) | N |  | — | [TBD] |
| road_address | VARCHAR(200) | N |  | — | [TBD] |
| detail_address | VARCHAR(200) | N |  | — | [TBD] |
| entrance_method | VARCHAR(200) | Y |  | NULL | [TBD] |
| delivery_memo | VARCHAR(200) | Y |  | NULL | [TBD] |
| is_default | CHAR(1) | N |  | N | [TBD] |
| last_used_at | DATETIME | Y |  | NULL | [TBD] |
| created_at | DATETIME | N |  | — | [TBD] |
| updated_at | DATETIME | N |  | — | [TBD] |
| del_yn | CHAR(1) | N |  | N | [TBD] |

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
| member_id | MEMBERS | member_id | DB FK(DDL) | — |

### mini-ERD
```
MEMBER_ADDRESSES ─member_id → MEMBERS.member_id
```

### 비즈니스 주의사항

[TBD] — 보강: `/sl-sync --apply --kind=sch` 또는 ddd-db-agent
