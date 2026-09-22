---
bat-id: BAT-MBR-001
name: 비밀번호 재설정 만료 코드 및 상한 카운터 정리
domain: member
domain-code: MBR
layer: batch
trigger: [CRON]
schedule: "0 0 3 * * *"
status: ACTIVE
---

# BAT-MBR-001: 비밀번호 재설정 만료 코드 및 상한 카운터 정리

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetMaintenanceScheduler.java:74-84` (Scheduler 메서드)

## 개요

| 항목 | 내용 |
|------|------|
| 목적 | 비밀번호 재설정 시스템의 정기 유지보수 — 보존기간을 초과한 만료 코드 행과 지난 날짜의 일일 요청 상한 카운터를 데이터베이스에서 정리(DELETE)한다. 요청 경로(API)와 완전히 분리된 독립 배치로 실행되어 트랜잭션·락이 섞이지 않는다. |
| 트리거 | 매일 스케줄 기반(Cron) |
| 스케줄 | `0 0 3 * * *` (매일 새벽 3시 UTC 기준) — 설정값 `${lab.batch.password-reset-cleanup.cron}`으로 조정 가능 |
| 처리 단위 | 배치 단일 실행당 조건부 DELETE 2개(코드 행 + 카운터 행) |

## 입력 파라미터

| 파라미터 | 타입 | 출처 | 설명 |
|---------|------|------|------|
| `member.password-reset.purge-retention` | Duration(ISO-8601) | 환경변수 / application.yml | 만료 코드 보존 기간. 기본값 `P7D`(7일). 이 기간을 초과한 `expires_at` 미만 행만 DELETE 대상 |
| 현재 시각 (`now`) | LocalDateTime | 시스템 시계(Clock.systemDefaultZone()) | 보존기간 기준 시각 계산 및 지난 날짜 결정. 테스트 시에만 주입 가능(생성자 오버로드) |

## 처리 흐름

1. **시스템 시간 조회** — Clock에서 현재 LocalDateTime 획득
2. **보존기간 계산** — `beforeExpiresAt = now - purgeRetention` (기본 now - 7일)
3. **만료 코드 정리** — `MemberPasswordResetDao.purgeExpiredCodes(beforeExpiresAt)` 호출
   - SQL: `DELETE FROM MEMBER_PASSWORD_RESETS WHERE expires_at < #{beforeExpiresAt}`
   - 조건: 보존기간을 초과해 만료된 코드 행만 대상 (멱등)
4. **카운터 행 정리** — `MemberPasswordResetRateLimitDao.purgeOldRows(now.toLocalDate())` 호출
   - SQL: `DELETE FROM MEMBER_PASSWORD_RESET_RATE_LIMITS WHERE day_key < #{beforeDay}` (오늘 날짜)
   - 조건: 어제 이전 날짜의 카운터 행 즉시 DELETE (멱등, 보존기간 미적용)
5. **로깅** — 두 DELETE가 모두 완료된 뒤에만 삭제 건수를 slf4j info로 기록 (부수효과는 작업 후 마지막에)

## 데이터 흐름

| 방향 | 시스템/테이블 | 설명 |
|------|-------------|------|
| 삭제 대상 | MEMBER_PASSWORD_RESETS | `expires_at < (now - 7일)`인 만료 코드 행 |
| 삭제 대상 | MEMBER_PASSWORD_RESET_RATE_LIMITS | `day_key < 오늘` 날짜의 요청 상한 카운터 행 |

## 비즈니스 규칙

### 만료 코드 정리 규칙

- **보존 기간 의도**: 비밀번호 재설정 확정 API(`INF-MBR-007`)의 오류 계약 안정성 확보
  - 만료된 코드 행이 존재하면 410(`MBR-4101`, "만료됨") 응답
  - 만료된 코드 행이 없으면 409(`MBR-4102`, "오답") 응답
  - 보존기간 없이 즉시 삭제하면, "어제 받은 코드로 오늘 재확정 시도" 같은 정상 사용 시나리오에서 410→409로 오류 코드가 변경되어, 클라이언트 화면(FUNC-member-007)의 "재요청" 전이 로직이 작동하지 않음

- **조건부 DELETE 특성**: 재실행해도 결과가 동일 (멱등성 보장)
  - 배치 실패 후 재실행 가능
  - 스케줄 중복 실행 안전

### 카운터 행 정리 규칙

- **즉시 삭제**: 지난 날짜(`day_key < 오늘`) 행은 보존기간 없이 매일 정리
  - 일일 요청 상한 카운터(`daily_count`)는 day별로 격리되므로 즉시 삭제 가능
  - 다음 작업항목(#2, INF-MBR-006)이 필요한 원자 UPSERT 메서드(`touchDailyLimit`, `selectRateLimit`) 추가 예정 — 그 시점까지 이 테이블은 대부분 빈 상태

- **조건부 DELETE**: 오늘 카운터는 절대 건드리지 않음 (`day_key < 오늘` 조건)
  - 재실행 안전 (멱등성)

## 오류 처리

| 조건 | 처리 방식 |
|------|---------|
| DAO 호출 실패 (DB 접속 오류, 권한 부족 등) | 예외 발생 → Spring 전역 예외 핸들러로 위임 → 로그 기록 후 배치 중단 |
| 0행 DELETE | 정상 동작 (조건 만족 행이 없는 경우). 로그는 `purgedCodes > 0 \|\| purgedRateLimits > 0` 조건에서만 출력 (로그 스팸 방지) |
| 부분 실패 (코드 정리 성공, 카운터 정리 실패) | 코드는 이미 DELETE됨 (원자 autocommit). 예외 발생 시점 이후 작업은 미실행 |

## 재처리 방법

| 항목 | 내용 |
|------|------|
| 멱등성 | **안전** — 두 DELETE 문 모두 조건부이므로, 같은 파라미터(같은 시각 + 보존기간)로 재실행해도 이미 지워진 행은 다시 지워지지 않음 (0행). 스케줄 중복 실행이나 수동 재시도 안전. |
| 재실행 방법 | Spring 기동 후 자동 스케줄 실행. 또는 테스트/수동 트리거: `MemberPasswordResetMaintenanceScheduler.purgeExpiredPasswordResetData()` 메서드 직접 호출(생성자에 Clock·Duration 주입으로 테스트 가능). 같은 LocalDateTime·Duration 파라미터를 사용하면 결과 동일 보장. |
| 부분 실패 시 | 각 DELETE가 독립 autocommit이므로 둘이 분리. 코드 정리는 성공했지만 카운터 정리가 실패한 경우, 코드는 이미 지워짐(원복 불가). 다음 실행 주기에 카운터 정리 재시도. 데이터 일관성: 코드와 카운터는 별도 테이블이고 FK 없으므로 부분 삭제는 테이블 무결성 위반 아님. |
| 데이터 복구 | 만료된 코드 행이 실수로 지워진 경우 복구 불가(이미 보존기간 경과). 테스트 DB에서는 `MemberPasswordResetDao.deleteByTarget()` 등의 정리 메서드가 별도 존재하므로 테스트 데이터 초기화 시 사용. 프로덕션 오류 시 재발급 요청이 유일한 사용자 대응 경로. |

## 선행 Job 의존성

없음 — 이 배치는 독립 실행. 다른 배치나 API에 대한 완료 대기 조건 없음.

## 모니터링 / 로그

### 성공 로그
```
비밀번호 재설정 데이터 정리(배치) — 만료 후 보존기간 지난 코드 N건, 지난 날짜 일일상한 카운터 M건 삭제
```
- 로그 레벨: INFO
- 출력 조건: `purgedCodes > 0 || purgedRateLimits > 0` (0건이면 로그 없음)
- 기록 내용: 삭제 건수 숫자만 (타겟 이메일/휴대폰 원문 미포함, 개인정보 보호)

### 성공 판단 기준
- DAO 호출이 정상 반환 (예외 없음)
- 삭제 건수가 0 이상 정수 (음수는 기술적으로 불가)

### 실패 판단 기준
- 데이터베이스 연결 오류
- 매퍼 쿼리 실행 오류 (권한, 테이블 미존재 등)
- 예외 발생 후 Spring 로그에 스택 트레이스 기록

### 관련 테스트케이스
- `MemberPasswordResetMaintenanceSchedulerTest` (단위 테스트, Mockito)
  - 호출 순서 검증: 코드 정리 → 카운터 정리
  - 호출 횟수 검증: 각각 1회
  - 파라미터 검증: `beforeExpiresAt`(now - 7일), `beforeDay`(오늘)
  
- `MemberPasswordResetDaoTest` (실 DB DAO 테스트)
  - 조건부 DELETE 시맨틱 검증 (`expires_at < beforeExpiresAt`)

- `MemberPasswordResetRateLimitDaoTest` (실 DB DAO 테스트)
  - 조건부 DELETE 시맨틱 검증 (`day_key < beforeDay`)
  
- `MemberPasswordResetConfirmationFlowTest` (통합 흐름 테스트)
  - 보존기간 내 만료 코드도 여전히 410 응답 검증

### 알림/임계값
- 모니터링 시스템 미구성 (기본)
- 실패 시: 앱 로그 확인 필수
- 큰 DELETE 건수 이상 징후: 쿨다운 설정·코드 유효시간 등 비즈니스 파라미터 변경 여부 확인

---

## 관계 정보

| 관계 | 항목 | 설명 |
|------|------|------|
| 테이블 | SCH-MBR-005 | MEMBER_PASSWORD_RESETS (만료 코드 정리 대상) |
| 테이블 | SCH-MBR-009 | MEMBER_PASSWORD_RESET_RATE_LIMITS (카운터 행 정리 대상) |
| 연결 API | INF-MBR-006 | POST /api/members/password-resets/codes (코드 요청) — 이 배치가 정리하는 테이블 사용 |
| 연결 API | INF-MBR-007 | POST /api/members/password-resets/confirmations (코드 확정) — 이 배치가 정리하는 테이블 사용. 보존기간 내 만료 행 존재 시 410 응답 로직 의존 |
| SR | SR-297 #1 | 이 배치의 신규 구현 항목 |

---

## 버전 이력

| 일자 | SR | 변경 내용 |
|------|----|---------| 
| 2026-09-16 | SR-297 #1 | 신규 생성 — MemberPasswordResetMaintenanceScheduler 배치 명세서 |

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-16 | SR-297 | #1 | 신규 배치 명세서 생성 — 만료 코드 보존기간(P7D) 정리 + 카운터 즉시 정리 | shop-api@b7ae32f |
