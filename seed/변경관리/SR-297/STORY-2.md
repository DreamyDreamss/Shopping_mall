---
story-id: STORY-SR-297.2
item: SR-297.2
title: 비밀번호 재설정 코드 요청
status: Done
domain: member
created: 2026-09-16
spec_markers: 0
sr-id: SR-297
approved_sha: a7a3317fec38
---

# STORY-SR-297.2 — 비밀번호 재설정 코드 요청 일일 상한·만료 행 정리 배치 — 비밀번호 재설정 코드 요청

## Story
비밀번호 재설정 코드 요청 일일 상한·만료 행 정리 배치 — 비밀번호 재설정 코드 요청


## 변경 컨텍스트 (SR-297)
> 이 story는 변경요청 **SR-297 — 비밀번호 재설정 코드 요청 일일 상한·만료 행 정리 배치** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-297/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-297/02_변경명세.md`
- AS-IS 스냅샷: `docs/변경관리/SR-297/_asis_snapshot` (변경 전 상태 대조용)

### 확정된 요건 문답 8건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: ① POST /api/members/password-resets/codes에 target별 일일 요청 상한 5회 — 전용 카운터 테이블(target, day_key PK) 단일 UPSERT 원자 판정, 가입 MEMBER_SIGNUP_RATE_LIMITS와 같은 모양 ② 만료 행 정리 배치(일 1회 @Scheduled) — 만료된 재설정 코드 행·지난 날짜 카운터 행. 제외: 쿨다운 60초 값, 재설정 확정 API, 가입 API·가입 정리 배치, 화면.
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 코드 요청은 상한 초과·쿨다운·미가입 대상 모두 항상 202(존재 오라클 없음, 응답 본문 동일) · 상한 이내 발급·확정 흐름 불변 · 가입 레이트리밋·가입 정리 배치 불변.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필요 — 요청·응답 형식 변경 없음. 상한 초과도 202(발송만 생략).
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 변경 없음 — 새 오류 응답을 만들지 않는다(상한 초과는 202로 흡수, 서버 로그만 남김).
- **배치 실행 주기와 재실행(중복 실행) 멱등성은?** — 일 1회(새벽, 주기는 설정값). 조건부 DELETE(만료 시각 < 지금, day_key < 오늘)라 재실행·중복 실행해도 결과 동일(멱등). 요청 경로 트랜잭션과 분리.
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 신규 전용 카운터 테이블 1개 추가(가입 MEMBER_SIGNUP_RATE_LIMITS와 같은 모양) — 기존 MEMBER_PASSWORD_RESETS는 정리 배치의 조건부 DELETE 대상일 뿐 스키마·다른 도메인 참조 변경 없음.
- **기존 데이터 이관·백필이 필요한가?** — 이관·백필 불필요 — 신규 테이블은 빈 상태로 시작(오늘 카운트 0부터). 기존 행은 배치가 만료분만 정리.
- **이 SR이 바꾸는 테이블을 읽는 다른 API(INF-MBR-007)의 결과·오류 판정이 달라지는가?** — 달라짐 — 확정 API(INF-MBR-007)는 행이 남아 있으면 만료 410(MBR-4101), 없으면 오답 409(MBR-4102)로 판정한다. 정리 배치는 만료 후 7일(설정값) 지난 행만 지워 7일 이내 만료 코드 확정은 기존 410 유지, 7일 초과 뒤 409는 알려진 동작으로 INF-MBR-007·BAT-MBR-001에 명시(SR-297 #1 QA 회신과 같은 결정).

## ✋ 사람 승인 회신 — 사실 정정 (2026-09-16, STEP 2)
> **형제 항목은 SR-297.1(이미 Done)이지 BAT-MBR-001이 아니다.** SR-297.1이 이미 만든 것:
> - DDL `V9__member_password_reset_rate_limits.sql` — 테이블 `MEMBER_PASSWORD_RESET_RATE_LIMITS`(target, day_key PK, daily_count, last_requested_at, last_token) + `idx_mpr_expires_at`
> - 스펙 `SCH-MBR-009`(docs/05_설계서/member/SCH/SCH-MBR-009.md)
> - `MemberPasswordResetRateLimitDao`(정리 전용 메서드 `purgeOldRows`/`deleteRateLimit`만 있음 — 스케줄러가 씀)
>
> **이 항목(#2)이 할 일은 정확히 이것뿐이다**: `MemberPasswordResetRateLimitDao`에 일일 상한 원자 판정용 메서드(`touchDailyLimit`/`selectRateLimit` 등, 매퍼 XML 포함)를 추가하고, `MemberPasswordResetService`의 코드 요청 경로에 그 판정을 쿨다운 판정과 나란히 넣는다.
> **테이블·DDL·정리 배치·스케줄러는 다시 만들지 않는다** — 아래 AC의 "신규 카운터 테이블"·"BAT-MBR-001"·"SCH-ID [미상]" 표현은 스펙 초안(변경명세) 작성 시점에 SR-297.1 완료를 몰랐던 잔재이므로, 참조 스펙은 **SCH-MBR-009**로 읽는다.

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-297/02_변경명세.md`에서 도출, 위 사람 회신으로 참조 스펙 보정)
- [x] INF-MBR-006: 요청 파라미터(`target`)·응답 모양(`channel`/`target`/`expiresInSeconds`)·오류 응답(`MBR-4100`/`MBR-5000`)은 **변경 없음**(하위호환 유지, 확정 문답 `api_compat`·`api_error`).
- [x] INF-MBR-006: 트랜잭션 순서에 target별 일일 요청 상한 5회 판정을 쿨다운 판정과 나란히 추가한다 — **기존 카운터 테이블(SCH-MBR-009, `MEMBER_PASSWORD_RESET_RATE_LIMITS`, target/day_key PK, SR-297.1이 이미 생성)**에 `MemberPasswordResetRateLimitDao`의 신규 메서드로 단일 UPSERT 원자 판정한다(확정 문답 `scope_freeze`, 가입 `MEMBER_SIGNUP_RATE_LIMITS`/`MemberSignupRateLimitDao`와 같은 모양).
- [x] INF-MBR-006: 상한 초과 시에도 발송·코드 갱신만 생략하고 응답은 그대로 202·동일 바디(존재 오라클·쿨다운 오라클과 같은 이유로 상한 오라클도 만들지 않음, 확정 문답 `regression_keep`).
- [x] INF-MBR-006: 새 오류 코드는 만들지 않는다(확정 문답 `api_error`) — 상한 초과는 서버 로그로만 남긴다.
- [x] INF-MBR-006: 참조 테이블이 `MEMBER_PASSWORD_RESETS` + 기존 카운터 테이블(SCH-MBR-009, SR-297.1이 이미 생성) 2개로 늘어난다 — **이 항목은 DDL을 추가하지 않는다**.
- [x] SCH-MBR-005: 테이블 스키마 자체는 **변경 없음**(컬럼·타입·NULL·기본값·PK 그대로) — 이 항목은 이 테이블을 건드리지 않는다(정리 배치는 SR-297.1이 이미 구현).
- [x] SCH-MBR-005: 스키마·다른 도메인 참조 변경 없음.
- [x] SCH-MBR-009: `daily_count`/`last_requested_at`/`last_token` 세 컬럼을 이 항목이 처음으로 읽고 쓴다(SR-297.1은 `day_key`만 사용) — DAO 신규 메서드 + 매퍼 XML 추가, **DDL은 변경하지 않는다**(`CREATE TABLE IF NOT EXISTS`이므로 컬럼이 이미 있다).

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**
- [x] INF-MBR-006 (POST /api/members/password-resets/codes): **형식 오류만 400, 그 외는 항상 202(존재 오라클·쿨다운 오라클 방지)**: 빈 값 / 100자 초과 / 이메일·휴대폰 형식 불일치만 400 `MBR-4100`. 회원 존재 여부, 쿨다운(60초 이내 재요청) 위반 여부는 응답에 전혀 드러나지 않는다 — 어느 경우든 동일한 202·`VerificationCodeResult` 바디(사례집 SR-231 "존재 판정 오라클"의 일반화 적용).
- [x] INF-MBR-006 (POST /api/members/password-resets/codes): **회원 테이블을 구조적으로 조회하지 않음**: `MemberPasswordResetService` 생성자는 `MemberDao`를 주입받지 않는다 — 회원 존재/탈퇴 여부와 무관하게 항상 같은 처리 경로를 타도록 클래스 구조 자체로 강제한다.
- [x] INF-MBR-006 (POST /api/members/password-resets/codes): **target 정규화·형식 정합**: 이메일은 `trim()+toLowerCase()`, 휴대폰은 `MemberRegistrationService.PHONE_PATTERN`(`^01[016789][0-9]{7,8}$`, 하이픈 불허)과 문자 단위로 동일한 정규식을 재사용한다(round2, 형제 가입 API와 판정 불일치를 없애기 위한 사람 수정) — 같은 입력이 재설정·가입 두 API에서 다르게 판정되는 것을 방지. PK(target)가 정규화 값이므로 같은 대상의 다른 표기는 같은 행·같은 쿨다운을 공유한다.
- [x] INF-MBR-006 (POST /api/members/password-resets/codes): **발송 로그는 재조회 대조 후에만 남긴다(round2, QA FAIL 재작업)**: `touchRequest` UPSERT 직후 `selectByTarget`으로 다시 읽어, 저장된 `code_hash`가 이번 요청이 만든 해시와 같을 때만(=쿨다운에 걸리지 않고 실제로 반영됐을 때만) "발송" 로그를 남긴다. round1은 이 순서 없이 무조건 로그를 남겨, 쿨다운에 걸린 요청도 로그가 남고 정작 저장된 코드는 이전 것이라 발송 로그와 실제 저장 코드가 불일치하는 결함이 있었다(SR-231 round5 재조회 대조와 동일 기법). 응답은 어느 경우든 무조건 202·상수 바디라 이 판정은 로그 여부에만 쓰이고 오라클을 만들지 않는다.
- [x] INF-MBR-006 (POST /api/members/password-resets/codes): **`memberPasswordReset.xml` UPSERT SET 순서 의존(구현 주의)**: `ON DUPLICATE KEY UPDATE`의 다섯 대입(`code_hash`/`expires_at`/`consumed_at`/`attempt_count`/`created_at`) 중 `created_at`이 반드시 맨 마지막이어야 한다 — MySQL/MariaDB는 SET을 좌→우로 평가하므로, 앞선 네 IF 조건의 `TIMESTAMPDIFF(SECOND, created_at, #{now})` 판정이 항상 갱신 전(원본) `created_at` 값을 보게 하기 위함이다(`MEMBER_SIGNUP_RATE_LIMITS` round6과 동일 원리). 순서가 바뀌면 쿨다운 판정이 첫 번째 IF에서만 정확하고 이후 컬럼은 이미 갱신된 `created_at`을 봐서 조용히 깨진다.
- [x] INF-MBR-006 (POST /api/members/password-resets/codes): **코드값 의미**: `cooldownSeconds=60`(동일 target 재요청 쿨다운), `EXPIRES_IN_SECONDS=600`(코드 유효 10분), `CODE_LENGTH=6`(숫자 코드) — 모두 SR-234 확정 요건.
- [x] INF-MBR-006 (POST /api/members/password-resets/codes): **레거시/round 이력**: round1은 휴대폰 정규식을 하이픈 허용으로 넓혔다가 형제 가입 API(`MemberRegistrationService`)와 같은 입력에 다른 판정(202 vs 400)을 내는 불일치가 QA에서 지적되어(재작업 지시 low(4)), round2에서 하이픈 불허로 되돌렸다(현재 코드 = round2 상태).

> 변경명세에 스펙 ID가 없는 절 — 이 항목 몫인지 확인해 AC로 옮긴다: 신규 카운터 테이블 (SCH-ID [미상] — 비밀번호 재설정 일일 요청 카운터)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-MBR-006: > [반영: FUNC-member-008] 2026-09-13 / INF-MBR-006: POST /api/members/password-resets/codes — 비밀번호 재설정 코드 요청 / > **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberPasswordResetController.java:39-44` / 요청 — [docs/05_설계서/member/INF/INF-MBR-006.md](../../05_설계서/member/INF/INF-MBR-006.md)
- **SCH** SCH-MBR-005: SCH-MBR-005: MEMBER_PASSWORD_RESETS / > **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** INF-MBR-006, INF-MBR-007 | **화면:** [TBD] / **근거 소스:** `modules/shop-api/src/main/resources/db/V5__member_password_resets.sql` — CREATE TABLE 문에서 컬럼·타입·NULL·기본값·키를 그대로 옮겼다(DB 실측이 아니라 DDL 기준). / 컬럼 설명 — [docs/05_설계서/member/SCH/SCH-MBR-005.md](../../05_설계서/member/SCH/SCH-MBR-005.md)
- **SCH** SCH-MBR-009(SR-297.1이 이미 생성, 이 항목의 실제 작업 대상): MEMBER_PASSWORD_RESET_RATE_LIMITS(target PK, day_key PK, daily_count, last_requested_at, last_token) — 컬럼 설명 [docs/05_설계서/member/SCH/SCH-MBR-009.md](../../05_설계서/member/SCH/SCH-MBR-009.md). DAO 골격 `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberPasswordResetRateLimitDao.java`(현재 `purgeOldRows`/`deleteRateLimit`만 있음 — 이 항목이 판정 메서드 추가). 형제 테이블 `MEMBER_SIGNUP_RATE_LIMITS`/`MemberSignupRateLimitDao`(가입 API)가 같은 모양의 참조 구현 — UPSERT SET 순서(`created_at`류 컬럼 마지막) 패턴을 그대로 따른다.
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)

## 📏 적용 규칙 (JIT — .claude/rules)
> 이 FUNC가 건드리는 파일에 적용되는 프로젝트 규칙이다. 전문은 아래 파일을 **직접 Read**하고 지킬 것 — `must` 위반은 STEP 5.3 축 C(`rules_check.py`)가 차단한다. 정본: 워크스페이스 `.claude/rules/`(뷰어 [rules]에서 편집).

| 규칙 | severity | 파일 |
|---|---|---|
| 컨트롤러 = HTTP 테스트 한 벌 | must | `.claude/rules/lab/controller-has-test.md` |
| 기동 DDL은 멱등해야 한다 | must | `.claude/rules/lab/ddl-idempotent.md` |
| 파일 크기 상한 | should | `.claude/rules/lab/file-size-cap.md` |
| 예외를 콘솔에 찍지 않는다 | must | `.claude/rules/lab/no-printstacktrace.md` |
| 매퍼 XML에서 SELECT * 를 쓰지 않는다 | must | `.claude/rules/lab/no-select-star.md` |
| 콘솔 출력 금지 | must | `.claude/rules/lab/no-sysout.md` |
| 서비스 = 단위 테스트 (권고) | should | `.claude/rules/lab/service-has-test.md` |


## 구현 계획

- **파일**:
  - `modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberPasswordResetRateLimit.java`(신규) — `MemberSignupRateLimit`을 그대로 미러링한 POJO(target, dayKey, dailyCount, lastRequestedAt, lastToken). 매퍼 `selectRateLimit`의 `resultType`.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberPasswordResetRateLimitDao.java`(수정) — 메서드 2개 추가: `void touchDailyLimit(target, dayKey, now, cooldownSeconds, dailyLimit, token)`, `MemberPasswordResetRateLimit selectRateLimit(target, dayKey)`. 기존 `purgeOldRows`/`deleteRateLimit`은 그대로 둔다. 클래스 상단 javadoc의 "#2가 만들 것" 문구를 "#2가 만듦"으로 갱신.
  - `modules/shop-api/src/main/resources/mapper/memberPasswordResetRateLimit.xml`(수정) — `<insert id="touchDailyLimit">` + `<select id="selectRateLimit">` 추가. `memberSignupRateLimit.xml#touchRateLimit`의 round6 본문(세션 변수 없이 SET 좌→우 평가만으로 결정적)을 테이블명만 `MEMBER_PASSWORD_RESET_RATE_LIMITS`로 바꿔 그대로 이식 — 새 세션 변수·새 판정 로직을 발명하지 않는다.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetService.java`(수정) — 생성자에 `MemberPasswordResetRateLimitDao` 추가, `DAILY_REQUEST_LIMIT=5` 상수 추가, `requestPasswordResetCode`에 일일 상한 게이트를 코드 UPSERT 앞에 배선.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberPasswordResetServiceTest.java`(수정) — `rateLimitDao` 목 추가 + 기본 "허용" 스텁(lenient) + 신규 시나리오 2~3개.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberPasswordResetRateLimitDaoTest.java`(수정) — `touchDailyLimit`/`selectRateLimit` 실DB 시나리오 5개 추가(이 파일 자체가 SR-297.1 시점에 이미 "#2가 추가할 것"이라고 예고해 둔 자리).
  - 컨트롤러(`MemberPasswordResetController.java`)·그 XML(`memberPasswordReset.xml`)·`MemberPasswordResetDao.java`·SCH-MBR-005/DDL은 **건드리지 않는다**.

- **데이터**: 트랜잭션 경계 없음(이 서비스 전체가 지금도 `@Transactional` 없이 원자 UPSERT 문장들의 나열이다 — SR-231 round4 데드락 회피 관례를 그대로 따른다, 새로 트랜잭션을 열지 않는다). 락은 MyBatis 단일 문장(`INSERT ... ON DUPLICATE KEY UPDATE`)의 PK(`target`, `day_key`) 행 잠금 하나로 충분 — `MEMBER_PASSWORD_RESET_RATE_LIMITS`와 `MEMBER_PASSWORD_RESETS`는 완전히 분리된 테이블이라 두 UPSERT 사이에 걸치는 락이 없다(SR-231 round4와 동일 이유로 두 테이블을 분리한 것이 SR-297.1의 설계 의도).

- **순서·보안**(결정 1 — 게이트 순서):
  1. target 검증·채널 판정·정규화(불변, 그대로).
  2. `now`/`dayKey` 계산(1회), `myToken = UUID.randomUUID().toString()`.
  3. **`rateLimitDao.touchDailyLimit(normalizedTarget, dayKey, now, COOLDOWN_SECONDS, DAILY_REQUEST_LIMIT, myToken)` → `rateLimitDao.selectRateLimit(...)` 재조회 → `admitted = (rateLimit != null && myToken.equals(rateLimit.getLastToken()))`** — 이 판정이 코드 테이블 UPSERT보다 **먼저** 실행된다.
  4. `admitted`일 때만 기존 흐름(코드 생성 → `dao.touchRequest` → `dao.selectByTarget` 재조회 대조 → 일치 시에만 발송 로그)을 그대로 실행한다. `admitted=false`면 **`dao.touchRequest`를 아예 호출하지 않는다** — "상한 초과 시 발송·코드 갱신만 생략"을 이 순서 자체로 강제한다(부수효과가 판정 뒤에 온다는 STORY 요구를 게이트 순서로 만족).
  5. 거부 시에도 서버 로그 한 줄은 남긴다(확정 문답 "상한 초과는 서버 로그로만 남긴다"): `mask(normalizedTarget)`, `rateLimit.getDailyCount()`(null이면 0)만 담고 원문 target·코드는 절대 넣지 않는다(기존 `mask()` 재사용, 사람 수정 (2)의 PII 최소화 관례 유지).
  6. 응답은 게이트 결과와 무관하게 항상 `new VerificationCodeResult(channel, normalizedTarget, EXPIRES_IN_SECONDS)` — 새 오류·새 분기 반환 타입 없음(확정 문답 `api_compat`/`api_error`, 존재·쿨다운 오라클과 같은 이유로 상한 오라클도 만들지 않는다).
  7. 회원 조회(`MemberDao`)는 여전히 주입하지 않는다 — 이 순서 변경이 생성자 구조적 강제(폴백·우회 경로 없음)를 깨지 않는다.

- **원자 UPSERT 조건 구성**(결정 2 — 쿨다운+일일상한 둘 다 vs 일일상한만): **둘 다 넣는다**(가입 `touchRateLimit`과 완전히 같은 모양). 근거: SCH-MBR-009 컬럼 설명이 `last_requested_at`을 명시적으로 "**#2의 쿨다운 판정 기준**"이라고 적어 뒀다 — 스키마 설계자가 이 항목에 쿨다운 판정까지 맡긴 것이다. AC도 "가입 `MEMBER_SIGNUP_RATE_LIMITS`/`MemberSignupRateLimitDao`와 같은 모양"을 3회 반복 지시한다(`scope_freeze`). 결과적으로 쿨다운 판정이 두 곳(기존 `memberPasswordReset.xml#touchRequest`의 `created_at` 기준 60초, 신규 카운터 테이블의 `last_requested_at` 기준 60초)에 이중으로 존재하게 되지만 — 둘 다 같은 `COOLDOWN_SECONDS` 상수와 같은 `now`로 항상 같은 타이밍에 함께 갱신되므로(4번 단계: 새 게이트가 admit할 때만 `touchRequest`가 불림) 서로 어긋나지 않는다(귀납: 최초 요청은 두 테이블 다 INSERT 분기로 동시 admit, 이후 매 admit마다 두 타임스탬프가 함께 전진). `memberPasswordReset.xml`은 손대지 않으므로(AC 제약) 이 중복은 새 코드를 깨지 않는 안전한 여분이다.

- **DAO 메서드명**(결정 3): `touchDailyLimit`(원자 UPSERT) / `selectRateLimit`(재조회) — STORY 사람 회신이 지목한 이름 그대로. `deleteRateLimit`(기존, 테스트 정리용)과 이름이 겹치지 않는다. `touchRateLimit`(가입 쪽 이름)을 그대로 쓰지 않는 이유: 이 메서드는 쿨다운뿐 아니라 "일일" 상한이 이름의 핵심 의미라 `touchDailyLimit`이 이 도메인에서 더 명확하다(사람 회신 명명 그대로 채택).

- **계약**: 새 오류 코드 없음, 새 HTTP 상태 없음, `VerificationCodeResult` 필드 변경 없음. `MemberPasswordResetController`·`memberPasswordReset.xml`(코드 테이블)·SCH-MBR-005 DDL 전부 무변경.

- **테스트**(결정 5):
  - `MemberPasswordResetServiceTest`(단위, Mockito):
    - `@Mock MemberPasswordResetRateLimitDao rateLimitDao` 추가, `service()` 헬퍼를 2-arg 생성자로 변경.
    - `@BeforeEach`에 **lenient** 기본 "허용" 스텁 추가(가입 `MemberSignupServiceTest#stubAdmitted`와 동일 기법 — `touchDailyLimit` 호출 시 넘어온 token을 캡처해 `selectRateLimit`이 그 토큰을 담은 행을 돌려주게 동적 응답). `lenient()`가 필요한 이유: 형식 오류로 즉시 400을 던지는 기존 테스트들(`invalidFormat`/`blankTarget`/`nullTarget`/`over100Chars`/`hyphenatedPhone`)은 DAO를 전혀 호출하지 않으므로 strict stubbing이면 `UnnecessaryStubbingException`이 난다.
    - 개별 테스트에서 상한 초과를 검증할 때만 `stubRejected(dailyCount)`로 override(가입 패턴과 동일 — `lastToken`을 내 토큰과 다른 고정 문자열로 스텁).
    - 신규: `requestPasswordResetCode_dailyLimitExceeded_skipsTouchRequestAndSendLog_stillReturns202` — `stubRejected(DAILY_REQUEST_LIMIT)` → `dao.touchRequest`가 **호출되지 않음**(`verify(dao, never())`), 로그가 비어 있음(발송 로그 없음), 그럼에도 응답은 기존 "정상" 케이스와 동일한 채널/target/`expiresInSeconds=EXPIRES_IN_SECONDS` 202.
    - 신규: `requestPasswordResetCode_touchDailyLimitArgs_cooldownAndDailyLimitConstants` — `verify(rateLimitDao).touchDailyLimit(eq(target), any(), any(), eq(COOLDOWN_SECONDS), eq(DAILY_REQUEST_LIMIT), anyString())`로 상수 배선을 고정(가입 `requestVerificationCode_emailTarget_...`의 `touchRateLimit` 인자 검증과 동일 패턴).
    - 신규: `requestPasswordResetCode_dailyLimitAdmitted_thenTouchRequestCalledAfterGate` — `verify` 순서(Mockito `InOrder`)로 `rateLimitDao.touchDailyLimit` → `dao.touchRequest` 순서를 고정해, 향후 리팩터링이 게이트 순서(결정 1)를 조용히 뒤집는 회귀를 잡는다.
    - 기존 `requestPasswordResetCode_withinCooldown_noSendLogAndStoredHashUnchanged`는 무변경(기본 lenient 허용 스텁으로 인해 `touchRequest`까지는 도달 — 코드 테이블 자체 쿨다운으로 로그가 안 남는 기존 시나리오, 새 게이트와 직교).
  - `MemberPasswordResetRateLimitDaoTest`(실DB): `MemberSignupRateLimitDaoTest`의 5개 시나리오(신규 target 허용, 쿨다운 경과 후 허용+토큰 갱신, 쿨다운 이내 거부, 일일상한 도달 거부, 날짜 바뀜=새 행=카운트 리셋)를 `touchDailyLimit`/`MEMBER_PASSWORD_RESET_RATE_LIMITS`로 이식 — **단, 이 파일 고유 관례(리터럴 `TARGET` 상수가 아니라 테스트별 UUID 접미 + `@AfterEach` 정리)를 유지**한다(아래 "테스트 격리" 참고, 가입 DAO 테스트의 리터럴 TARGET 패턴을 그대로 베끼지 않는다).
  - `MemberPasswordResetControllerTest`: 변경 불필요(컨트롤러·응답 타입 무변경, 기존 `requestCode_cooldownIgnoredByService_stillReturns202`가 이미 "서비스가 무엇을 하든 컨트롤러는 202"를 양성 확인한다 — 상한 초과도 서비스가 감싸는 같은 시나리오라 별도 컨트롤러 테스트는 불필요, 중복 방지).

- **테스트 격리**: `MemberPasswordResetRateLimitDaoTest`는 이미 `target` 필드 + `@AfterEach`에서 `DELETE ... WHERE target = ?`로 자기 행만 지우는 구조다(SR-232 r2 재발 방지 관례, 이 파일 자체가 "고정 리터럴이면 플레이키" 주석을 달아 뒀다). 새 테스트 5개도 각각 `target = "pwreset-dl-" + 시나리오명 + "-" + UUID.randomUUID() + "@example.com"`으로 고유 target을 쓰고 기존 `cleanUp()`에 얹힌다(추가 오버라이드 불필요 — 이미 `target` 하나만 지우는 구조). `MemberPasswordResetServiceTest`는 Mockito 목 기반이라 DB 상태가 없어 격리 이슈 없음.

- **폴백·우회 경로의 자격 판정**: 해당 없음 — 이 항목은 새 인증·조회 경로를 열지 않는다(회원 존재/탈퇴 판정과 무관, `MemberDao` 미주입 그대로 유지).

- **프레임워크 실행 모델 함정**: 없음 — Spring MVC 요청당 스레드 하나, `@Transactional` 없는 단일 원자 UPSERT 두 문장의 순차 호출이라 재진입·이중 실행 위험이 없다. 스케줄러·React 이펙트 등 이 SR과 무관.

- **범위 밖**: SCH-MBR-005/`MEMBER_PASSWORD_RESETS` DDL·정리 배치·`MemberPasswordResetMaintenanceScheduler`(전부 SR-297.1 완료분, 재작업 안 함). 공유 유틸로의 쿨다운/상한 로직 추출(가입·재설정 두 서비스가 구조적으로 거의 동일해졌지만, 기존 서비스별 복제 관례를 이 항목에서 바꾸지 않는다 — STORY 상단 "범위 밖" 관례와 동일 판단, 후속 SR 후보).

- **실패 사례집 대조**(`harness/antipatterns.all.md`):
  - "인증코드 시도 횟수를 트랜잭션 안에서 올렸다 → 롤백 시 카운터도 사라짐"(SR-231 r2) — 조건 재확인: 이 항목도 카운터(daily_count)를 다루지만 **트랜잭션을 전혀 열지 않고** 단일 원자 UPSERT로만 갱신하므로 이 실패 조건 자체가 성립하지 않는다(그대로 무트랜잭션 유지가 회피책).
  - "세션 변수를 여러 문장에 걸쳐 썼다 → 커넥션 풀에서 값이 사라짐"(SR-231 r3) — `touchDailyLimit`은 가입 round6과 동일하게 세션 변수를 아예 쓰지 않고 SET 좌→우 평가만 쓴다. 조건 성립 자체를 피함.
  - "존재 판정 → 인증 순서로 존재 오라클"(SR-231 r5) — 이 항목은 인증 순서를 바꾸지 않는다(회원 조회 자체가 없음). 다만 유사 원리로 "상한 초과 여부가 응답에 드러나면 상한 오라클"이 될 수 있어(스토리 요건 4) 응답을 절대 분기하지 않는다 — 이미 계획에 반영(항목 6).
  - "발송 로그를 UPSERT보다 먼저 무조건 남겼다 → 로그·DB 불일치"(SR-234 FUNC-member-008 r1, 바로 이 서비스의 기존 결함) — 이번 변경은 그 재조회 대조 로직(항목 4의 `dao.selectByTarget` 대조) 자체를 손대지 않고 앞단에 새 게이트만 얹는다. 새 게이트도 같은 원리(선-UPSERT/후-재조회 대조)를 따르므로 같은 실수를 반복하지 않는다.
  - "같은 리터럴 이메일을 여러 테스트가 공유 → 카운터가 누적돼 플레이키"(SR-232 r2) — 위 "테스트 격리" 절에서 UUID 접미 관례를 그대로 따른다.
  - 해당 없음: "역할 반전"(SR-231 r1, ID만 보고 구현 — 이 항목은 story 본문 전체를 읽고 진행), "`useAffectedRows=true` 전역 설정"(SR-231 r4, round4→round5로 이미 폐기된 패턴 —애초에 후보에 없음), "DDL `IF NOT EXISTS` 누락"(SR-231 r2 — 이 항목은 DDL 자체를 만들지 않음), "락 순서 불일치로 데드락"(SR-231 r3 — 두 테이블이 분리돼 있고 각각 단일 문장이라 락 순서 문제가 생길 구조가 아님).

### ✋ 사람 확인(STEP 3-0) — 계획대로 진행 + 추가 요구 (2026-09-16)
> 이중 쿨다운(결정2)의 경계 하나를 사람이 직접 짚었다 — **자정 경계**: 자정 직전 요청 뒤 60초 안에 날짜가 바뀌면, 카운터 테이블은 `day_key`가 PK라 새 `(target, 오늘)` 행이 생겨 쿨다운 없이 admit(일일 카운트 소비)하지만, 코드 테이블(`memberPasswordReset.xml`, 무변경)의 쿨다운은 여전히 `created_at`(어제) 기준 60초 이내라 거부할 수 있다 — 이 조합이면 **일일 카운트만 소비되고 코드는 안 나간다**. 응답은 이미 어느 경우든 202·동일 바디라 오라클은 생기지 않으므로 **이 동작을 그대로 허용**하기로 했다(카운터가 전날 행까지 보게 만드는 교차-day_key 조회는 이번 항목에서 만들지 않는다 — 범위 밖 유지).
> - **INF-MBR-006에 알려진 동작으로 기록**: "자정 경계(자정 직전 요청 후 60초 이내 날짜 변경)에는 일일 카운트가 소비되고 코드는 갱신되지 않을 수 있다(코드 테이블 쿨다운이 카운터보다 먼저 걸림) — 응답은 항상 202로 동일, 클라이언트에 드러나지 않음." 재동기화(STEP 5.5)에서 이 문장을 반영한다.
> - **신규 단위 테스트 1건(시계 주입)**: 자정 경계 시나리오 — 어제 23:59:xx에 코드 요청(코드 테이블 `created_at`=어제 기록) → 오늘 00:00:xx(60초 이내)에 재요청 → 카운터는 새 `day_key` 행이라 admit(daily_count=1로 시작)하지만 `dao.touchRequest`가 호출은 되되 코드 테이블 자체 쿨다운(옛 `created_at`)에 걸려 `code_hash`/`created_at`이 갱신되지 않음(발송 로그 없음) → 응답은 그래도 202·동일 바디.
> - **상한 초과 테스트 보강**: 기존 계획의 상한초과 테스트에 HTTP 단언(202 + 동일 바디, 컨트롤러 통합) 및 DB 단언(코드 테이블 `created_at`/`code_hash`이 요청 전후로 불변)을 추가한다.

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
(dev-agent가 생성 파일·주요 결정 기록)
- 계획 확인: 계획대로 진행 + 자정 경계 알려진 동작·테스트 보강 추가 (2026-09-16)
- 구현 완료 (2026-09-16):
  - 신규: `modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberPasswordResetRateLimit.java` — `MemberSignupRateLimit` 미러링 POJO(target/dayKey/dailyCount/lastRequestedAt/lastToken).
  - 수정: `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberPasswordResetRateLimitDao.java` — `touchDailyLimit`(원자 UPSERT)·`selectRateLimit`(재조회) 2개 메서드 추가. 기존 `purgeOldRows`/`deleteRateLimit`은 무변경. 클래스 javadoc의 "#2가 만들 것" 문구를 "#2가 만듦"으로 갱신.
  - 수정: `modules/shop-api/src/main/resources/mapper/memberPasswordResetRateLimit.xml` — `<insert id="touchDailyLimit">`(memberSignupRateLimit.xml#touchRateLimit의 round6 SQL 본문을 테이블명만 바꿔 그대로 이식) + `<select id="selectRateLimit">` 추가. 기존 `purgeOldRows`/`deleteRateLimit` 무변경.
  - 수정: `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetService.java` — 생성자에 `MemberPasswordResetRateLimitDao` 추가(2-arg `@Autowired` + 3-arg 시계 주입 package-private), `DAILY_REQUEST_LIMIT=5` 상수 추가. `requestPasswordResetCode`에 일일 상한 게이트(결정1 순서: touchDailyLimit → selectRateLimit → admitted 판정)를 코드 테이블 UPSERT보다 먼저 배선. 거부 시 `dao.touchRequest` 자체를 호출하지 않고 서버 로그 한 줄(마스킹된 target + dailyCount)만 남김. 클래스 javadoc에 일일 상한 설명 + 자정 경계 알려진 동작 문단 추가.
  - 수정: `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberPasswordResetServiceTest.java` — `rateLimitDao` 목 추가, `service(Clock)` 오버로드 추가, `@BeforeEach` 기본 "허용" lenient 스텁(`stubDailyLimitAdmitsByDefault`) + `stubDailyLimitRejected` 헬퍼 추가. 신규 테스트 4건: 상한초과 시 `touchRequest` 미호출·202 동일 바디, `touchDailyLimit` 인자 배선 고정, `InOrder`로 게이트→touchRequest 순서 고정, 자정 경계(시계 주입) 시나리오. **Dev 실측 수정**: 기본 lenient 스텁을 `ArgumentCaptor.capture()` 기반으로 짰더니 `stubDailyLimitRejected`가 `when(...).thenReturn(...)`으로 재스텁할 때 Mockito가 기존 스텁을 먼저 실행해 `ArgumentCaptor.getValue()`가 "No argument value was captured!"로 던졌다 — `AtomicReference`(이 파일의 기존 `stubDaoAdmitsWrite()`와 동일 기법)로 바꿔 해결(재스텁 시 안전하게 null 반환).
  - 수정: `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberPasswordResetRateLimitDaoTest.java` — `MemberSignupRateLimitDaoTest`의 5개 시나리오(신규 target 허용/쿨다운 경과 후 허용+토큰 갱신/쿨다운 이내 거부/일일상한 도달 거부/날짜 바뀜=새 행=카운트 리셋)를 `touchDailyLimit`으로 이식, 이 파일 고유의 UUID 접미 target + `@AfterEach` 정리 관례 유지. 기존 `purgeOldRows` 테스트 2건 무변경.
  - 신규: `modules/shop-api/src/test/java/com/sm/lab/shop/MemberPasswordResetRateLimitFlowTest.java` — STEP 3-0 추가 요구(상한 초과 테스트 보강) 반영. `@SpringBootTest(RANDOM_PORT)` 실 서버+실 DB 왕복: 오늘 카운터를 상한(5)까지 직접 시드 후 코드 테이블에도 이전 요청 행을 시드, HTTP POST로 상한 초과 요청 → 202 + 정상 케이스와 동일 바디(JsonNode 파싱) 단언 + DB 단언(코드 테이블 `code_hash`/`created_at`이 요청 전후 완전히 불변, `touchRequest` 미호출의 실측 증거). 컨트롤러·매퍼·DDL은 건드리지 않음.
  - 컨트롤러(`MemberPasswordResetController.java`)·`memberPasswordReset.xml`·`MemberPasswordResetDao.java`·SCH-MBR-005/DDL·`MemberPasswordResetControllerTest.java`(기존 계획대로 무변경) — 모두 건드리지 않음.
  - 문서: `docs/05_설계서/member/INF/INF-MBR-006.md` — "비즈니스 규칙" 절에 자정 경계 알려진 동작 문장 1건만 추가(STEP 3-0 지시대로 다른 절·anchors는 건드리지 않음, 전체 재동기화는 STEP 5.5로 유보).
  - 빌드/테스트 실측: `cd modules/shop-api && ./mvnw.cmd -q compile test-compile` 통과. 로컬 MariaDB(3307)·앱 의존 실DB 테스트 포함 `-Dtest=MemberPasswordResetServiceTest,MemberPasswordResetRateLimitDaoTest,MemberPasswordResetControllerTest,MemberPasswordResetRateLimitFlowTest` 전부 통과(DB 기동 확인함). 전체 스위트(`mvnw.cmd test`, 537건)에서 이 SR과 무관한 **기존 실패 2건**을 관찰: `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`, `OrderListEndToEndIntegrationTest.orderList_realDbEndToEnd_rendersLatestDeliveryStateColumnForEachOrder` — 둘 다 Order 도메인의 시드 데이터(`M-0001`/주문 `20260816-0002`) 의존이며, 이 두 테스트만 단독 실행해도 동일하게 실패해 이 항목의 변경과 무관함을 확인(로컬 DB 시드 상태 차이로 추정, 회귀 아님).

- 재작업 완료(2026-09-16, QA CONCERNS round1 재작업 지시 1+2 반영):
  - 수정: `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetService.java` — `requestPasswordResetCode`의 거부(admitted=false) 로그 문구를 사유별로 분기. 재조회한 `rateLimit.getDailyCount() >= DAILY_REQUEST_LIMIT`이면 "일일 상한 초과", 아니면 "쿨다운"으로 갈라 `비밀번호 재설정 코드 요청 거부({사유}) — target={mask}, dailyCount={count}` 한 줄로 남긴다(형제 `MemberSignupService#rejectionFor`와 같은 판별 기준, 기존 `mask()` PII 마스킹 유지). 로그 포맷만 바꿨고 게이트 순서·UPSERT·응답(`VerificationCodeResult`)은 무변경.
  - 수정: `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberPasswordResetServiceTest.java` —
    - 신규 테스트 2건: `requestPasswordResetCode_dailyLimitExceeded_logsDailyLimitExceededReason`(`stubDailyLimitRejected(DAILY_REQUEST_LIMIT)` → 로그에 "일일 상한 초과" 포함·"거부(쿨다운)" 미포함 단언), `requestPasswordResetCode_cooldownRejected_logsCooldownReason`(`stubDailyLimitRejected(1)` → 로그에 "거부(쿨다운)" 포함·"일일 상한 초과" 미포함 단언). 각각 재작업 지시 1의 두 사유를 실제로 행사·단언한다.
    - `requestPasswordResetCode_midnightBoundary_...`를 재작성 — 기존에는 기본 lenient 허용 스텁 덕에 게이트 로직을 실제로 행사하지 않고 통과했다(재작업 지시 2가 지목한 무력한 테스트). `ArgumentCaptor<LocalDate>`/`ArgumentCaptor<LocalDateTime>`로 `rateLimitDao.touchDailyLimit`에 전달된 `dayKey`/`now` 인자를 캡처해 `dayKey == LocalDate.of(2026,9,17)`(자정 롤오버 후 새 날짜), `now == 주입 시각(2026-09-17T00:00:10)` 그대로임을 직접 단언하도록 바꿨다 — 이 단언이 있으면 `LocalDateTime.now(clock).toLocalDate()` 게이트 로직이 실제로 행사된다.
    - import 추가: `java.time.LocalDate`.
  - 재검증: `cd modules/shop-api && ./mvnw.cmd -q compile test-compile` 통과. `-Dtest=MemberPasswordResetServiceTest,MemberPasswordResetRateLimitDaoTest,MemberPasswordResetControllerTest,MemberPasswordResetRateLimitFlowTest` 전부 통과(`MemberPasswordResetServiceTest` 16→18건, 나머지 3종 무변경 그대로 통과 — surefire 리포트 실측: Service 18/0/0, RateLimitDao 7/0/0, Controller 6/0/0, FlowTest 1/0/0).
  - 범위 밖(재작업 지시대로 손대지 않음): `INF-MBR-006`/`SCH-MBR-009` 재동기화 문구(STEP 5.5 몫), 전체 스위트 기존 실패 2건(Order 도메인 시드 무관, round1에서 이미 확인·이 항목 책임 아님).

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-16 — CONCERNS
> 검증 방식: story 자기보고를 신뢰하지 않고 `modules/shop-api`(별도 git 저장소) `git diff` + mtime으로 이 항목(#2, 04:45~04:49대)의 변경분을 SR-297.1/SR-298 변경분(03:57~04:25대)과 분리해 직접 확인했고, 대상 테스트 4종을 실행(`MemberPasswordResetServiceTest` 16 / `MemberPasswordResetRateLimitDaoTest` 7 / `MemberPasswordResetControllerTest` 6 / `MemberPasswordResetRateLimitFlowTest` 1 — 전부 통과)했다.

- **Layer1 스펙: concerns** — 사람이 지목한 핵심 결정 7개 중 6개 확인 완료.
  1. 게이트 순서 ✔ — `MemberPasswordResetService:149-159`가 `touchDailyLimit` → `selectRateLimit` → `admitted` 판정을 먼저 하고, `admitted`일 때만 `dao.touchRequest`를 호출한다(else 분기에 코드 테이블 접근이 전혀 없음). `InOrder` 단위 테스트 + 실 DB 왕복 테스트로 이중 고정.
  2. 응답 불변 ✔ — 반환은 게이트 결과와 무관하게 `new VerificationCodeResult(channel, normalizedTarget, EXPIRES_IN_SECONDS)` 한 줄뿐. 새 오류 코드·새 HTTP 상태·새 분기 반환 없음(상한 오라클 미생성).
  3. `MemberDao` 미주입 ✔ — 생성자는 `(MemberPasswordResetDao, MemberPasswordResetRateLimitDao[, Clock])`뿐. 전 소스에서 `new MemberPasswordResetService(` 호출은 테스트 2곳만.
  4. 무변경 확인 ✔ — `MemberPasswordResetController.java`(mtime 09-13, 무변경), `MemberPasswordResetMaintenanceScheduler`·`V9__*.sql`·`memberPasswordReset.xml`·`MemberPasswordResetDao.java`(전부 #1/SR-298 몫의 변경이며 #2 세션 시각과 불일치), SCH-MBR-005 DDL 컬럼·타입·PK 무변경(인덱스 1건 추가는 #1 몫).
  5. 자정 경계 △ — INF-MBR-006 「비즈니스 규칙」에 알려진 동작 1문장 기록 확인. 다만 단위 테스트가 그 시나리오를 실제로는 검증하지 않는다(아래 권고 2).
  6. 상한 초과 통합 테스트 ✔ — `MemberPasswordResetRateLimitFlowTest`가 실 서버(RANDOM_PORT)+실 DB로 202·`channel`/`target`/`expiresInSeconds` 동일 바디 + 코드 테이블 `code_hash`/`created_at` 전후 불변을 실제로 단언한다(실행 통과 확인).
  7. UPSERT SET 순서 ✔ — `memberPasswordResetRateLimit.xml#touchDailyLimit`은 세션 변수 없이 `last_token` → `last_requested_at` → `daily_count` 좌→우 평가만 쓰며, `memberSignupRateLimit.xml#touchRateLimit`(round6)과 문자 단위로 동일한 본문이다(테이블명만 상이). 사례집 SR-231 r3(세션 변수) 재발 없음.
  - **다만** 거부 경로 로그가 쿨다운 거부까지 "일일 상한 초과"로 기록한다(아래 권고 1) — 이 기능의 유일한 관측 신호가 최빈 케이스에서 틀린 값을 낸다.
- **Layer2 보안: pass** — 신규 SQL은 전부 `#{}` 바인딩(`${}` 없음), `SELECT *` 없음(규칙 `no-select-star`), `System.out`/`printStackTrace` 없음. 거부 로그는 `mask()`를 거쳐 원문 target·코드를 담지 않는다. 엔드포인트는 기존과 동일하게 `ApiKeyAuthFilter` 화이트리스트(로그인 전 호출) — 인증 구조 변경 없음. target별 상한이 "제3자가 피해자의 하루 5회를 소진시킬 수 있다"는 성질을 갖지만 이는 SR 확정 요건(가입 API와 동일 설계)이라 수용된 동작으로 본다.
- **Layer3 회귀: pass** — 코드 테이블 동작은 순수 가산(admit 경로에서 기존 UPSERT→재조회 대조 로직이 문자 단위로 보존, 변수명만 `admitted`→`writeReflected`). 쿨다운 거부 요청은 이제 `touchRequest`를 건너뛰지만 그 UPSERT가 원래 no-op이었으므로 DB 상태 결과는 동일. 기존 재설정 테스트 전부 통과. 전체 스위트의 실패 2건(`ApiKeyAuthIntegrationTest#memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`, `OrderListEndToEndIntegrationTest#orderList_realDbEndToEnd_...`)은 **QA가 직접 단독 실행해 재현·확인**했고 원인이 Order 도메인 시드 데이터 부재(`M-0001` 주문 0건, 주문 `20260816-0002` 없음)라 이 항목과 무관하다. 단, 기준선(`test_baseline.json`: 510건/실패 1건)보다 실패가 1건 늘어난 상태이므로 기준선 재기록 또는 시드 복구가 별도로 필요하다(이 항목 책임 아님).

- 권고(CONCERNS시):
  1. **(medium, 스펙/구현)** 거부 로그가 쿨다운 거부를 "일일 상한 초과"로 잘못 기록한다 — `MemberPasswordResetService.java:177`. `admitted=false`는 쿨다운 거부와 일일상한 거부 **둘 다**에서 발생하는데(UPSERT 조건이 `last_requested_at <= now-60 AND daily_count < limit`), 로그 문구는 무조건 "일일 상한 초과"다. 10:00:00 발급 성공(daily_count=1) 후 10:00:20 재클릭 → `비밀번호 재설정 코드 요청 일일 상한 초과 — target=us***, dailyCount=1`(상한은 5)이 남는다. 확정 문답이 "상한 초과는 서버 로그로만 남긴다"고 정한 탓에 이 로그가 기능의 **유일한 관측 신호**인데, 실제로는 재클릭(최빈 케이스)이 이 문구를 지배한다. 계획이 그대로 이식하라고 지목한 참조 구현 `MemberSignupService#rejectionFor`는 `dailyCount >= DAILY_REQUEST_LIMIT`로 사유를 가르며, 이 항목이 새로 쓴 `MemberPasswordResetRateLimitDao#selectRateLimit`의 javadoc 자체가 "거부 사유(쿨다운 vs 일일상한) 판별"이라고 적어 뒀다 — 서비스만 그 분기를 누락했다. 조치: `rateLimit != null && rateLimit.getDailyCount() >= DAILY_REQUEST_LIMIT`로 문구를 가르거나(가입과 동형), 최소한 중립 문구("요청 거부")로 바꾼다. 응답은 어느 쪽이든 202 동일이라 오라클과 무관하다. 단위 테스트도 함께 보강(현재 `withinCooldown_...` 테스트는 기본 lenient 허용 스텁 때문에 이 경로를 타지 않고, `dailyLimitExceeded_...`는 `doesNotContain("발송")`만 봐서 둘 다 이 결함을 못 잡는다).
  2. **(medium, 테스트 커버리지)** 자정 경계 테스트의 주입 시계가 무력하다 — `MemberPasswordResetServiceTest#requestPasswordResetCode_midnightBoundary_...`. 게이트가 기본 lenient 허용 스텁으로 무조건 admit하고, 단언은 202 바디·`touchRequest` 호출·로그 없음뿐이라 **`Clock.fixed(2026-09-17 00:00:10)`을 시스템 시계로 바꿔도 그대로 통과**한다(자정과 아무 관련이 없다). STEP 3-0에서 사람이 명시적으로 요구한 "시계 주입 자정 경계 1건"이 이름만 충족된 상태다. 조치: `verify(rateLimitDao).touchDailyLimit(eq(target), eq(LocalDate.of(2026,9,17)), eq(LocalDateTime.of(2026,9,17,0,0,10)), ...)`로 **day_key가 주입 시계에서 파생됨**을 고정하면 이 시나리오의 실제 메커니즘(날짜 롤오버 → 새 행 → admit)이 회귀망에 들어온다. 현재 구현(`LocalDateTime.now(clock).toLocalDate()`)은 올바르므로 코드 수정은 불필요하고 단언만 추가하면 된다.

- 재동기화 입력(STEP 5.5 — 권고 아님, 스펙 본문이 코드보다 늦은 정상 상태):
  - `INF-MBR-006` frontmatter `tables:` — `MEMBER_PASSWORD_RESET_RATE_LIMITS` 누락(AC "참조 테이블 2개"의 스펙 반영분).
  - `INF-MBR-006` frontmatter `anchors:` — `MemberPasswordResetService.java:108-139/141-171` 줄 범위가 이번 변경으로 밀렸고, 신규 `memberPasswordResetRateLimit.xml`·`MemberPasswordResetRateLimitDao`·`MemberPasswordResetRateLimit` 앵커가 없다.
  - `INF-MBR-006` 「트랜잭션 순서」 — 1~7단계에 일일 상한 게이트(코드 UPSERT 앞)가 빠져 있다.
  - `INF-MBR-006` 「사이드이펙트」 — "`MEMBER_PASSWORD_RESETS` 단일 테이블만 다루며 다른 어떤 테이블도 조회·변경하지 않는다"가 이제 사실과 다르다.
  - `SCH-MBR-009` — `inf: []`(→ `[INF-MBR-006]`), 컬럼 설명 3건의 "이 항목은 쓰지 않음"·비즈니스 주의사항의 "그때까지 이 테이블은 항상 빈 상태다"가 #2 구현으로 낡았다. `target` 컬럼 설명 "이메일 원문"도 SMS(휴대폰) target을 포괄하도록 정정 필요.

### QA Gate — 2026-09-16 — PASS (round2)
> 검증 방식: story 자기보고를 신뢰하지 않고 (a) mtime으로 round2 변경분을 분리(05:01대 2파일뿐 — `MemberPasswordResetService.java` 05:01:17, `MemberPasswordResetServiceTest.java` 05:01:58. round1 dev 산출물은 04:45~04:48대, SR-297.1/SR-298분은 03:57~04:25대로 이번 라운드에 안 건드림), (b) 대상 테스트 4종 직접 실행(surefire 실측: Service 18/0/0, RateLimitDao 7/0/0, Controller 6/0/0, FlowTest 1/0/0 — 전부 통과), (c) **뮤테이션 검증**으로 신규 단언이 실제로 무는지 실측(아래)했다. 이번 라운드는 round1 medium 권고 2건의 해소 여부에 집중했고, round1에서 ✔ 확인된 항목은 무변경을 확인해 재검증하지 않았다.

- **Layer1 스펙: pass** — round1 재작업 지시 2건 모두 실질 해소.
  1. **거부 로그 사유 분기 ✔ (재작업 지시 1)** — `MemberPasswordResetService.java:184-187`이 재조회한 `rateLimit`으로 `int dailyCount = rateLimit != null ? rateLimit.getDailyCount() : 0; String reason = dailyCount >= DAILY_REQUEST_LIMIT ? "일일 상한 초과" : "쿨다운";`를 계산해 `거부({reason}) — target={mask}, dailyCount={n}` 한 줄로 남긴다. 사람 지시가 지목한 참조 구현 `MemberSignupService#rejectionFor`(:150-156)와 **판별식·null 폴백까지 문자 단위로 동형**임을 대조 확인했다. 시맨틱도 정합: `touchDailyLimit` UPSERT(`memberPasswordResetRateLimit.xml:20-28`)는 거부 시 `daily_count`를 증가시키지 않으므로 거부 시점의 `dailyCount`는 항상 이전 상태값 — 쿨다운 거부는 `<5`, 상한 거부는 `=5`로 정확히 갈린다.
  2. **신규 테스트 2건이 각 문구를 실제로 단언 ✔** — `_dailyLimitExceeded_logsDailyLimitExceededReason`(`stubDailyLimitRejected(5)` → `contains("일일 상한 초과")` + `doesNotContain("거부(쿨다운)")`), `_cooldownRejected_logsCooldownReason`(`stubDailyLimitRejected(1)` → `contains("거부(쿨다운)")` + `doesNotContain("일일 상한 초과")`). 양방향 배타 단언이라 분기 반전·상수화 어느 쪽도 잡힌다.
  3. **자정 경계 테스트가 인자를 실제로 단언 ✔ (재작업 지시 2)** — `_midnightBoundary_...`(:423-429)가 `ArgumentCaptor<LocalDate>`/`<LocalDateTime>`로 `touchDailyLimit`의 `dayKey`/`now`를 캡처해 `isEqualTo(LocalDate.of(2026,9,17))`·`isEqualTo(2026-09-17T00:00:10)`을 직접 단언한다. 더 이상 lenient 허용 스텁만으로 통과하지 않는다. (`now`는 서비스가 `truncatedTo(MILLIS)`를 거치지만 주입값에 밀리초 미만이 없어 항등 — 단언 성립.) 날짜 롤오버의 **실제 DB 시맨틱**(새 `day_key` 행 → 카운트 리셋 → admit)은 `MemberPasswordResetRateLimitDaoTest#touchDailyLimit_newDay_startsFreshRowAndAdmitsWithNewToken`(실 DB)이 별도로 덮어 두 층이 함께 시나리오를 고정한다.
  4. **뮤테이션 실측(자기보고 불신 검증)** — 두 수정을 각각 되돌려 넣고(서비스의 `reason`을 round1처럼 상수 `"일일 상한 초과"`로, 자정 테스트의 `service(midnightClock)`을 `service()`로) 실행한 결과 **정확히 의도된 2건만 실패**: `_cooldownRejected_logsCooldownReason`(actual `거부(일일 상한 초과) … dailyCount=1` → `거부(쿨다운)` 미포함), `_midnightBoundary_...`(`expected: 2026-09-17 but was: 2026-09-16`). 두 신규 단언이 회귀망으로 실제 작동함을 실증했다. 뮤테이션 파일은 즉시 원복해 **md5 해시로 원본 동일성 확인**(`630ad42e…`/`4aedac2e…`)했고, 복원 후 재실행도 18/0/0 통과.
  5. **round1 PASS 항목 무변경 ✔** — 게이트 순서(`:149-159` touchDailyLimit → selectRateLimit → admitted, else 분기에 코드 테이블 접근 없음)·응답 계약(`:190` 단일 `new VerificationCodeResult(...)`)·`MemberDao` 미주입(생성자 `:118-129`)은 현 소스에서 그대로 확인. UPSERT 문(`memberPasswordResetRateLimit.xml` mtime 04:45)·`memberPasswordReset.xml`(03:57, SR-298/#1 몫)·`MemberPasswordResetController.java`(09-13)·`MemberPasswordResetControllerTest.java`(09-13)·V9 DDL은 round2 창(04:50 이후)에 **단 하나도 걸리지 않는다** — `find -newermt` 실측으로 확인.
- **Layer2 보안: pass** — round2 변경은 로그 문자열 분기 + 테스트 단언뿐이다. 새 SQL·새 엔드포인트·새 오류 코드 없음. 로그에 추가된 `reason`은 상수 문자열 2종이고 `target`은 여전히 `mask()`를 거친다 — PII 노출 증가 없음. `System.out`/`printStackTrace` 없음(규칙 `no-sysout`·`no-printstacktrace` 통과), `SELECT *` 없음. 사유 분기는 **서버 로그에만** 나타나고 응답은 여전히 무조건 202·상수 바디라 상한/쿨다운 오라클을 새로 만들지 않는다.
- **Layer3 회귀: pass** — 거부 로그 문구에 의존하는 다른 테스트가 전 테스트 트리에 없음을 grep으로 확인(`(없음)`). 기존 16건 + 신규 2건 = 18건 전부 통과하며 기존 단언은 무변경(`_dailyLimitExceeded_skipsTouchRequestAndSendLog_...`의 `doesNotContain("발송")`도 새 거부 문구와 충돌하지 않음). 실 서버+실 DB 왕복(`MemberPasswordResetRateLimitFlowTest`)도 그대로 통과했고, 그 실행 로그에서 `거부(일일 상한 초과) … dailyCount=5`와 `거부(쿨다운) … dailyCount=1`이 **실제 Spring 컨텍스트에서도** 갈려 찍히는 것을 육안 확인했다. 파일 크기 규칙 비해당(cap glob이 `src/main/**`, 서비스 271줄). 전체 스위트의 기존 실패 2건(Order 도메인 시드)은 round1에서 이미 이 항목 무관으로 확정 — 재작업 지시상 범위 밖이며 round2가 건드리지 않았다.

- 권고(CONCERNS시): 없음 — round1 medium 2건 모두 해소, 새 medium 이상 없음.

- 재동기화 입력(STEP 5.5 — 권고 아님, round1 목록 그대로 유효):
  - `INF-MBR-006` frontmatter `tables:`에 `MEMBER_PASSWORD_RESET_RATE_LIMITS` 추가, `anchors:` 줄 범위 갱신(`MemberPasswordResetService.java:108-139/141-171`이 밀림) + 신규 앵커 3종(`memberPasswordResetRateLimit.xml`·`MemberPasswordResetRateLimitDao`·`MemberPasswordResetRateLimit`).
  - `INF-MBR-006` 「트랜잭션 순서」에 일일 상한 게이트(코드 UPSERT 앞) 단계 삽입, 「사이드이펙트」의 "단일 테이블만 다루며 다른 어떤 테이블도 조회·변경하지 않는다" 정정.
  - `INF-MBR-006` 「비즈니스 규칙」 — round2 신설된 **거부 로그 사유 분기**(쿨다운 vs 일일 상한, `dailyCount >= 5` 기준)를 서버 로그 계약으로 1문장 추가 권장.
  - `SCH-MBR-009` — `inf: []` → `[INF-MBR-006]`, 컬럼 설명 3건의 "이 항목은 쓰지 않음"·"그때까지 이 테이블은 항상 빈 상태다" 정정, `target` 설명 "이메일 원문"을 SMS target 포괄로 정정.

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/spec] 거부 경로 로그가 쿨다운 거부까지 '일일 상한 초과'로 기록한다(MemberPasswordResetService.java:177). admitted=false는 쿨다운·일일상한 두 사유 모두에서 발생하는데 문구는 상한 초과로 고정 — 발급 직후 60초 내 재클릭(최빈 케이스)이 dailyCount=1인데도 '상한 초과'로 남는다. 확정 문답상 이 로그가 기능의 유일한 관측 신호이며, 계획이 이식하라고 지목한 참조 구현 MemberSignupService#rejectionFor는 dailyCount >= DAILY_REQUEST_LIMIT로 사유를 가른다(신규 DAO javadoc도 '거부 사유(쿨다운 vs 일일상한) 판별'이라 적어 뒀으나 서비스만 누락). → rateLimit.getDailyCount() >= DAILY_REQUEST_LIMIT로 로그 문구를 가르거나(가입과 동형) 중립 문구로 변경하고, 쿨다운 거부 경로를 타는 단위 테스트를 추가한다(현재 withinCooldown 테스트는 기본 허용 스텁 때문에 이 경로를 타지 않는다).
2. [medium/spec] 자정 경계 단위 테스트(MemberPasswordResetServiceTest#requestPasswordResetCode_midnightBoundary_...)의 주입 시계가 무력하다 — 게이트가 기본 lenient 허용 스텁으로 무조건 admit하고 단언이 202 바디·touchRequest 호출·로그 없음뿐이라 Clock.fixed(2026-09-17 00:00:10)을 시스템 시계로 바꿔도 동일하게 통과한다. STEP 3-0에서 사람이 명시 요구한 '시계 주입 자정 경계 1건'이 이름만 충족된 상태. → verify(rateLimitDao).touchDailyLimit(eq(target), eq(LocalDate.of(2026,9,17)), eq(LocalDateTime.of(2026,9,17,0,0,10)), ...)로 day_key가 주입 시계에서 파생됨을 단언한다(구현은 이미 올바르므로 단언만 추가).

사람 코멘트: QA CONCERNS round1 재작업 — 권고 1+2 둘 다 반영.
1) 거부 로그 사유 분기: MemberPasswordResetService 일일상한 거부 로그를, 재조회한 dailyCount >= DAILY_REQUEST_LIMIT 이면 "일일 상한 초과", 아니면 "쿨다운"으로 갈라 남긴다(형제 MemberSignupService#rejectionFor와 같은 판별 방식, PII 마스킹 유지). 각 사유별 로그 단언 테스트 1건씩 추가.
2) 자정 경계 테스트: lenient 허용 스텁에 기대지 말고 touchDailyLimit 호출 인자(dayKey=새 날짜, now=주입 시각)를 ArgumentCaptor로 실제 단언하도록 재작성 — 현재는 게이트 로직을 실제로 행사하지 않은 채 통과한다.
범위 밖(이번 라운드에서 하지 않음): 재동기화 입력(INF-MBR-006/SCH-MBR-009 문구 갱신)은 STEP 5.5 몫, 기준선 실패 2건(Order 도메인 시드 무관)은 잡이 건드리지 않는다(사람/별건 몫).
완료 조건: 기존 테스트 전부 통과 + 신규 사유분기 테스트 2건 + 자정 경계 테스트가 실제로 인자 단언.
