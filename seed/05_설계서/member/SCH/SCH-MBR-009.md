---
sch-id: SCH-MBR-009
table: MEMBER_PASSWORD_RESET_RATE_LIMITS
domain: member
domain-code: MBR
inf: [INF-MBR-006]
---

# SCH-MBR-009: MEMBER_PASSWORD_RESET_RATE_LIMITS

> [반영: SR-297.2] — SR-297.1이 만든 DDL 골격에 SR-297.2가 일일 상한 원자 판정(UPSERT)을 배선. 아래 컬럼 설명·비즈니스 주의사항을 실제 사용 상태로 갱신.

> **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** INF-MBR-006 | **화면:** [TBD]

**근거 소스:** `modules/shop-api/src/main/resources/db/V9__member_password_reset_rate_limits.sql` — CREATE TABLE 문에서 컬럼·타입·NULL·기본값·키를 그대로 옮겼다(DB 실측이 아니라 DDL 기준).

### 컬럼 설명
| 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 |
|--------|------|------|----|--------|------|
| target | VARCHAR(100) | N | PK | — | 재설정 코드 요청 대상(이메일 또는 휴대폰번호, 정규화된 값) — target 단위로 일일 상한 |
| day_key | DATE | N | PK | — | 요청 날짜(앱 시계 기준 로컬 날짜) — daily_count의 하루 경계. 날짜가 바뀌면 새 행이 INSERT되어 daily_count가 1부터 재시작 |
| daily_count | INT | N |  | 1 | 이 target의 이 날짜에 허용된 요청 횟수 — `MemberPasswordResetRateLimitDao#touchDailyLimit`(SR-297.2)이 쿨다운·일일상한(5회) 둘 다 만족할 때만 원자 UPSERT로 갱신 |
| last_requested_at | DATETIME(3) | N |  | — | 이 target의 이 날짜 마지막으로 허용된 요청 시각 — `touchDailyLimit`(SR-297.2)의 쿨다운(60초) 판정 기준 |
| last_token | VARCHAR(36) | Y |  | NULL | `touchDailyLimit`(SR-297.2)이 조건부로 갱신하는 요청 토큰(UUID) — affected-rows 대신 `selectRateLimit` 재조회로 "내 토큰이 저장돼 있으면 허용" 판정 |

### 인덱스
| 인덱스명 | 컬럼 | 타입 | 목적 |
|---------|------|------|------|
| idx_mprl_day_key | day_key | INDEX | 정리 배치(BAT-MBR-001)가 `day_key < 오늘`로 지난 날짜 행을 조건부 DELETE할 때 스캔 범위를 좁힌다 |

### 코드값

없음 — 코드성(열거값) 컬럼이 없다.

### 관계 (FK / 관찰된 조인)
> 출처 `DB FK`=DB 선언 제약, `쿼리관찰(N)`=소스 SQL에서 N회 등장한 등가조인(논리 FK).

| 자식 컬럼 | 참조 테이블 | 참조 컬럼 | 출처 | ON DELETE |
|----------|-----------|----------|------|-----------|
| — | — | — | — | — |

### mini-ERD
```
MEMBER_PASSWORD_RESET_RATE_LIMITS
```

### 비즈니스 주의사항

- (target, day_key) PK — target별 하루 단위 카운터 행. 가입 도메인 `MEMBER_SIGNUP_RATE_LIMITS`와 동형 스키마이자 문자 단위로 동일한 UPSERT 본문(round6 원리, 세션 변수 없이 SET 좌→우 평가만으로 결정적).
- **SR-297.1(DDL·정리)과 SR-297.2(일일 상한 판정)가 이 테이블을 나눠 쓴다**: SR-297.1은 정리 배치(`purgeOldRows`, `day_key < 오늘` 조건부 DELETE)만, SR-297.2는 `daily_count`/`last_requested_at`/`last_token` 세 컬럼을 원자 UPSERT(`touchDailyLimit`)로 갱신하는 일일 상한 판정을 맡는다 — 요청 경로(SR-297.2)에는 정리 로직이 없고, 배치(SR-297.1)에는 판정 로직이 없다.
- **자정 경계(day_key 롤오버) 알려진 동작**: `day_key`가 PK 일부라 날짜가 바뀌면 새 행이 생겨 쿨다운 없이 admit되지만, 코드 테이블(`MEMBER_PASSWORD_RESETS.created_at`)의 자체 쿨다운은 어제 시각 기준으로 여전히 거부할 수 있다 — 이 경우 `daily_count`만 소비되고 코드는 갱신되지 않는다(INF-MBR-006 "알려진 동작" 참고, 응답은 항상 202로 동일해 오라클 없음).
- 정리 배치(BAT-MBR-001, `MemberPasswordResetMaintenanceScheduler`)가 `day_key < 오늘`인 행을 매일 조건부 DELETE(멱등, 재실행해도 결과 동일) — 신규 요청 트래픽과 무관한 시각에 실행되고 `@Transactional` 없이 독립 autocommit.
- 근거: DDL `V9__member_password_reset_rate_limits.sql`(소스 기준, DB 실측 아님) + `MemberPasswordResetRateLimitDao`/`memberPasswordResetRateLimit.xml`(SR-297.2 실 사용 코드).

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-16 | SR-297 | #1 | 코드값·인덱스 목적·비즈니스 주의사항 보강(daily_count 등은 이 항목 미사용, #2 몫 명시) | shop-api@b7ae32f |
| 2026-09-16 | SR-297 | #2 | daily_count/last_requested_at/last_token 실사용 시작(touchDailyLimit) — 컬럼 설명·비즈니스 주의사항을 실사용 상태로 갱신, inf 연결(INF-MBR-006) | shop-api@b7ae32f |
