---
sch-id: SCH-ORD-007
table: ZIPCODES
domain: order
domain-code: ORD
inf: []
---

# SCH-ORD-007: ZIPCODES

> **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** [TBD] | **화면:** [TBD]

**근거 소스:** `modules/shop-api/src/main/resources/db/V8__zipcodes.sql` — CREATE TABLE 문에서 컬럼·타입·NULL·기본값·키를 그대로 옮겼다(DB 실측이 아니라 DDL 기준).

### 컬럼 설명
| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
|--------|------|------|----|--------|------|
| zipcode_id | BIGINT | N | PK | NULL | [TBD] |
| zipcode | VARCHAR(10) | N |  | — | [TBD] |
| road_address | VARCHAR(200) | N |  | — | [TBD] |
| sido | VARCHAR(50) | N |  | — | [TBD] |
| sigungu | VARCHAR(50) | N |  | — | [TBD] |

### 인덱스
| 인덱스명 | 컬럼 | 타입 | 목적 |
|---------|------|------|------|
| uq_zipcodes_zipcode | zipcode | UNIQUE | [TBD] |

### 코드값

[TBD]

### 관계 (FK / 관찰된 조인)
> 출처 `DB FK`=DB 선언 제약, `쿼리관찰(N)`=소스 SQL에서 N회 등장한 등가조인(논리 FK).

| 자식 컬럼 | 참조 테이블 | 참조 컬럼 | 출처 | ON DELETE |
|----------|-----------|----------|------|-----------|
| — | — | — | — | — |

### mini-ERD
```
ZIPCODES
```

### 비즈니스 주의사항

[TBD] — 보강: `/sl-sync --apply --kind=sch` 또는 ddd-db-agent

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-19 | SR-235 | #3 | ZIPCODES 테이블 신설(V8__zipcodes.sql) — 시드 100건(도로명 검색용), round2 인코딩 수정 + round3 COMMENT 정본화 | shop-api@1c82e8c, shop-web@8e1e3e3 |
