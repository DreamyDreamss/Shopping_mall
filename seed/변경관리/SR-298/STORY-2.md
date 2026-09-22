---
story-id: STORY-SR-298.2
item: SR-298.2
title: 비밀번호 재설정 확정
status: Done
domain: member
created: 2026-09-16
spec_markers: 0
sr-id: SR-298
approved_sha: 41f84d4b160e
---

# STORY-SR-298.2 — 인증코드 시도 상한의 병렬 버스트 우회 차단(가입·재설정 공통) — 비밀번호 재설정 확정

## Story
인증코드 시도 상한의 병렬 버스트 우회 차단(가입·재설정 공통) — 비밀번호 재설정 확정


## 변경 컨텍스트 (SR-298)
> 이 story는 변경요청 **SR-298 — 인증코드 시도 상한의 병렬 버스트 우회 차단(가입·재설정 공통)** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-298/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-298/02_변경명세.md`

### 확정된 요건 문답 6건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 시도 횟수 증가를 단일 조건부 UPDATE(attempt_count = attempt_count + 1 WHERE attempt_count < 5)로 원자화 — ① 비밀번호 재설정 확정(MEMBER_PASSWORD_RESETS, INF-MBR-007) ② 회원가입 인증(MEMBER_SIGNUP_VERIFICATIONS, INF-MBR-002). 제외: 레이트리밋·코드 발급·만료 정책, 화면.
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 5회 초과 시 409(MBR-4093 등 기존 코드) 유지 · 올바른 코드 1회 성공 · 코드 소비·만료 판정 불변 · 응답 형식 불변.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필요 — 요청·응답 형식 변경 없음(내부 SQL 원자화).
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 변경 없음 — 5회 초과 409 계약 그대로. 동시 요청이 몰려도 성공은 최대 1회, 나머지는 기존 오류 코드.
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음 — 두 테이블의 attempt_count 갱신 문장만 바뀐다(다른 도메인 참조 없음).
- **기존 데이터 이관·백필이 필요한가?** — 불필요 — 스키마 변경 없음(기존 attempt_count 컬럼 사용).

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-298/02_변경명세.md`에서 도출)
- [x] SR-298.1: 시도 횟수 증가를 단일 조건부 UPDATE `attempt_count = attempt_count + 1 WHERE attempt_count < 5`(대상 조건: `channel`+`target`, 미만료/미소비/미검증 등 매치 조건은 기존 `markVerifiedIfCodeMatches`/`incrementAttemptCount`가 쓰던 것을 그대로 유지 — 정확한 WHERE 절 전체 구성은 [미상], 근거 파일에 SQL 원문 없음)로 원자화한다.
- [x] SR-298.1: 영향행수(affected rows)로 성공/실패를 판별 — 0행이면 이미 상한(5) 도달로 판단해 추가 조회 없이 409 `MBR-4093`, 1행이면 정상 증가 후 409 `MBR-4091`.
- [x] SR-298.1: STEP 1의 코드 검증 UPDATE(`markVerifiedIfCodeMatches`) 자체의 매치 조건·순서(존재 오라클 방지를 위한 STEP1→STEP0→STEP2 순서)는 변경 대상이 아니다.
- [x] SR-298.2: 시도 횟수 증가를 단일 조건부 UPDATE `attempt_count = attempt_count + 1 WHERE attempt_count < 5`로 원자화한다.
- [x] SR-298.2: 영향행수로 상한 도달 여부를 판별해 409 `MBR-4103`(코드 확인 시도 횟수 초과) 여부를 결정한다.
- [x] SR-298.2: 코드 확정(`confirmIfCodeMatches`) 자체의 매치 조건, `del_yn='N'` 이중 필터, 코드 확정이 회원 조회보다 항상 먼저 실행되는 순서(존재 오라클 방지), 1회용(`consumed_at`) 판정, 트랜잭션 경계(Writer 별도 빈)는 변경 대상이 아니다.

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**
- [x] INF-MBR-007 (POST /api/members/password-resets/confirmations): **존재 오라클 방지(이 API의 핵심 위험)**: 코드 확정(`confirmIfCodeMatches`)이 회원 조회보다 항상 먼저 실행되고, "행 없음"(요청한 적 없는 target)도 단순 코드 불일치와 완전히 동일한 409 `MBR-4102`로 수렴한다(`handleConfirmFailure`가 두 경우 모두 같은 `mismatchException()` 팩토리 호출). 회원 조회(코드 확정 성공 후 1회) 결과가 있든 없든 응답은 204로 동일 — 이 API의 어떤 응답도 회원 존재 여부를 드러내지 않는다.
- [x] INF-MBR-007 (POST /api/members/password-resets/confirmations): **del_yn='N' 이중 필터**: 회원 매칱 조회(`selectMemberIdByResetTarget`)와 비밀번호 반영 UPDATE(`updatePasswordHash`) 양쪽 모두에 `del_yn = 'N'`을 건다 — 탈퇴 회원의 비밀번호가 되살아나거나 탈퇴 회원 명의로 세션이 재발급되는 경로를 원천 차단한다.
- [x] INF-MBR-007 (POST /api/members/password-resets/confirmations): **코드 확정이 회원 조회보다 항상 먼저**: `confirmPasswordReset`은 target/newPassword 형식 검증 → 코드 확정(`confirmIfCodeMatches`) → (성공 시에만) BCrypt → 회원 조회 순서로 진행한다. 회원 조회 이전 어떤 단계도 `MEMBERS` 테이블을 참조하지 않는다.
- [x] INF-MBR-007 (POST /api/members/password-resets/confirmations): **BCrypt는 확정 성공 시에만, 발견 여부와 무관하게 항상 실행**: 코드 확정이 성공한 직후 새 비밀번호를 BCrypt로 해싱한다 — 이후 회원을 찾든 못 찾든 이 해싱 자체는 항상 실행됐던 상태이므로, 회원 발견 여부가 처리 시간(타이밍)으로 새지 않는다.
- [x] INF-MBR-007 (POST /api/members/password-resets/confirmations): **회원 미발견 시 조용히 204**: 코드 확정에 성공했지만 매칭되는 활성 회원이 없으면(가짜 target 등) 로그 없이 그대로 반환한다 — 완료 로그는 실제로 비밀번호가 반영된 경우에만 남긴다.
- [x] INF-MBR-007 (POST /api/members/password-resets/confirmations): **트랜잭션 경계(Writer 별도 빈)**: 이 서비스는 `@Transactional`을 쓰지 않는다 — 코드 확정/시도횟수 증가는 각각 autocommit 단일 UPDATE 문(실패 시도 카운터가 트랜잭션 롤백에 함께 사라지는 것을 방지). 비밀번호 반영+전 기기 로그아웃만 별도 스프링 빈(`MemberPasswordResetConfirmationWriter`)의 `@Transactional` 메서드로 묶는다 — 서비스 클래스 안에서 자기 메서드를 직접 호출(self-invocation)하면 Spring 프록시가 트랜잭션 어드바이스를 적용하지 못하는 함정을 피하기 위함이다.
- [x] INF-MBR-007 (POST /api/members/password-resets/confirmations): **0행이면 세션 폐기 스킵**: `updatePasswordHash`가 0행(그 사이 회원 탈퇴 등으로 반영 실효 없음)이면 리프레시 토큰·API 키 폐기 두 문장을 건너뛰고 즉시 반환한다.
- [x] INF-MBR-007 (POST /api/members/password-resets/confirmations): **전 기기 로그아웃(사이드이펙트)**: 비밀번호 반영에 성공하면 그 회원의 리프레시 토큰 전체와 API 키를 폐기한다(순서: 리프레시 먼저, apiKey 나중 — INF-MBR-005 로그아웃과 동일 순서). 이 트랜잭션이 중간에 실패하면(예: 세션 폐기 UPDATE 예외) 비밀번호 반영까지 함께 롤백된다 — "비밀번호는 바뀌었는데 세션은 살아있는" 위험한 중간 상태를 만들지 않는다.

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-MBR-007: > [반영: FUNC-member-009] 2026-09-13 / INF-MBR-007: POST /api/members/password-resets/confirmations — 비밀번호 재설정 확정 / 요청 / - Method: POST — [docs/05_설계서/member/INF/INF-MBR-007.md](../../05_설계서/member/INF/INF-MBR-007.md)
- **SCH**: (연결 없음)
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
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberPasswordResetDao.java` — `incrementAttemptCount(target, now)`(52~63행)에 `@Param("maxAttempts") int maxAttempts`를 추가한다(선언만, SQL 없음). javadoc을 "이 문장 자체가 `attempt_count < maxAttempts`를 조건으로 갖는 원자 증가이며, 반환값 0은 상한 도달·이미 소비·이미 만료·행 없음 중 하나를 뜻한다(호출부가 재조회로 구분)"로 갱신.
  - `modules/shop-api/src/main/resources/mapper/memberPasswordReset.xml` — `incrementAttemptCount` UPDATE(51~57행)에 `AND attempt_count &lt; #{maxAttempts}` 절을 추가한다. 기존 매치 조건(`target = #{target}`·`consumed_at IS NULL`·`expires_at > #{now}`)은 한 글자도 바꾸지 않는다(AC 명시). 상단 주석에 SR-298 원자화 근거를 덧붙인다.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetConfirmationService.java` — `handleConfirmFailure`(143~158행)를 재작성한다. **AS-IS**: `selectByTarget`로 먼저 읽어 null/만료·소비/상한(`stored.getAttemptCount() >= MAX_CONFIRM_ATTEMPTS`, 이 비교가 TOCTOU 지점)을 차례로 분기한 뒤에야 `incrementAttemptCount`를 호출한다. **TO-BE**: 원자 `incrementAttemptCount(normalizedTarget, now, MAX_CONFIRM_ATTEMPTS)`를 **가장 먼저, 무조건** 시도한다 — 1행이면 그 자체로 "미만료·미소비·상한 미만·단순 오답"이 확정된 것이므로 `selectByTarget` 없이 바로 `mismatchException()`(409 `MBR-4102`)을 던진다. 0행일 때만 `selectByTarget`로 재조회해 원인만 분류한다(재증가 없음): 행 없음 → `mismatchException()`(409 `MBR-4102`, AS-IS와 동일 코드), 소비됨 또는 만료 → 410 `MBR-4101`, `attemptCount >= MAX_CONFIRM_ATTEMPTS` → 409 `MBR-4103`, 그 외(이론상 도달 불가 — 위 세 조건 모두 거짓이면 원자 UPDATE 자체가 1행을 반환했어야 함, 방어용 폴백) → `mismatchException()`. 클래스 상단 javadoc의 "존재 오라클 방지" 설명, `confirmIfCodeMatches` 호출부(85~137행)와 그 순서는 손대지 않는다 — 변경은 `handleConfirmFailure` 내부의 판정 방식뿐이다.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberPasswordResetConfirmationServiceTest.java` — 아래 "테스트" 절 그대로 갱신(시그니처 3-인자화 + 분기별 mock 재배치).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberPasswordResetDaoTest.java` — 기존 `incrementAttemptCount` 호출부 4곳(`incrementAttemptCount_activeRow_incrementsByOne`·`_expiredRow_doesNotIncrement`·`_consumedRow_doesNotIncrement`·`_fiveConcurrentCallsSameTarget_noLostUpdatesFinalCountFive`, 220~286행)에 `maxAttempts=5` 인자를 추가(기대값 무변경). 신규 테스트 1개 추가(아래 "테스트" 참고).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/MemberPasswordResetConfirmationFlowTest.java` — 신규 HTTP 시나리오 1개 추가: 순차 오답 5회 → 각각 409/`MBR-4102` → 6회째(오답이든 정답이든) → 409/`MBR-4103`, DB `attempt_count`=5 실측(AS-IS에도 없던 회귀 테스트 공백 — 이번에 메운다).
  - 신규 파일 `modules/shop-api/src/test/java/com/sm/lab/shop/MemberPasswordResetConfirmationConcurrencyTest.java` — `MemberRegistrationConcurrencyTest`와 동일 패턴(실 서버 RANDOM_PORT, `CyclicBarrier`)의 이 SR 핵심 회귀 테스트(아래 "테스트" 참고).

- **데이터**: 스키마 변경 없음(`MEMBER_PASSWORD_RESETS.attempt_count` 기존 컬럼). DDL 파일 변경 없음. 트랜잭션 경계 무변경 — `confirmPasswordReset`/`handleConfirmFailure`는 AS-IS와 동일하게 완전히 비트랜잭션(각 UPDATE가 autocommit 개별 문장), `MemberPasswordResetConfirmationWriter.applyNewPassword`(비밀번호 반영+전기기 로그아웃)만 별도 빈의 `@Transactional`이며 이번 변경 대상이 아니다. 락: `attempt_count < maxAttempts` 조건이 더해져도 잠그는 대상은 여전히 PK(`target`) 행 하나뿐 — `confirmIfCodeMatches`와 동일한 단일 행 락 모델이라 SR-231 r3(락 순서 뒤섞임) 해당 없음. 별도 애플리케이션 레벨 락은 두지 않는다.

- **순서·보안**: 바깥 흐름(1 target 검증 → 2 password 검증 → 3 정규화 → 4 코드 해싱 → 5 `confirmIfCodeMatches` 시도 → 0행이면 `handleConfirmFailure` → 6 BCrypt → 7 회원 조회 → 8 미발견 시 조용히 204 → 9 Writer → 10 완료 로그)는 무변경 — 코드 확정이 회원 조회보다 항상 먼저인 순서, "행 없음도 단순 오답과 동일 코드"라는 존재 오라클 방지 원칙도 그대로다. 바뀌는 것은 `handleConfirmFailure` **내부**의 판정 순서뿐이다(읽고-분기하던 것을 원자 쓰기-먼저·실패시에만 읽기로 뒤집는다). 부수효과(로그)는 여전히 성공 경로(Writer 커밋 후)에만 있고 실패 분기(`handleConfirmFailure`)에는 어떤 로그·발송·이벤트도 없다 — 이번 변경으로도 추가하지 않는다. 오류 응답은 기존 코드·상태·문구 그대로(신규 코드 없음). 레이트리밋은 범위 밖(확정 문답 제외 목록).

- **계약**: 신규 오류 코드 없음. `MBR-4101`(410)·`MBR-4102`(409)·`MBR-4103`(409) 발생 조건은 관찰 가능한 결과 기준으로 AS-IS와 동일(행 없음/단순오답→4102, 소비·만료→4101, 상한 도달→4103) — 판정 메커니즘만 "별도 읽기값 비교"에서 "원자 UPDATE의 영향행수, 실패 시에만 원인 재조회"로 바뀐다. 요청/응답 파라미터·204 응답 형태 전부 무변경.

- **테스트**:
  - HTTP 레벨(회귀, 무변경 기대): `MemberPasswordResetConfirmationFlowTest`의 기존 5개(`confirmSucceeds_...`·`withdrawnMember_...`·`neverRegisteredEmail_...`·`confirmTwiceWithSameCode_...`·`rowNeverRequested_and_simpleWrongCode_produceByteIdenticalResponses`)는 파일을 고치지 않아도 그대로 통과해야 한다.
  - HTTP 레벨(신규, 이번에 메우는 공백): `wrongCodeFiveTimesThenSixth_locksOutAtFiveWithMbr4103` — 시드 코드에 대해 순차 오답 5회 각각 409/`MBR-4102` 확인, DB `attempt_count`=5 실측, 6회째(오답 또는 정답 무관) 409/`MBR-4103`이며 `attempt_count`는 5에 머묾.
  - DAO 레벨(시그니처 갱신 + 신규): 기존 4개 호출부에 `maxAttempts=5` 인자 추가 후 무변경 통과. 신규 `incrementAttemptCount_atCap_returnsZeroRowsAndDoesNotIncrementBeyondFive` — 신선한 행 시딩 후 `incrementAttemptCount(TARGET, now, 5)`를 5회 호출(각각 영향행수 1, `attempt_count` 1→5 확인) 후 6회째 호출은 영향행수 0·`attempt_count`는 5 그대로임을 단언 — 이 원자 캡이 이 SR의 핵심 산출물.
  - 서비스 단위(Mockito, 시그니처 갱신 + 분기 재배치):
    - `confirmPasswordReset_simpleMismatchUnderCap_incrementsOnceAndThrows409Mbr4102WithoutSelectByTarget` — `incrementAttemptCount(any(), any(), anyInt())`가 1을 반환하도록 스텁, 409/`MBR-4102` 확인, `verify(passwordResetDao, never()).selectByTarget(any())`(핵심 전환 — AS-IS는 항상 먼저 읽었으나 TO-BE는 증가 성공 시 아예 읽지 않는다).
    - `confirmPasswordReset_incrementZeroAndRowMissing_throws409Mbr4102` — `incrementAttemptCount`가 0을 반환하도록 스텁(이제 **호출된다**, AS-IS의 `never()` 기대를 `times(1)`로 뒤집는다), `selectByTarget`이 null을 반환하도록 스텁, 409/`MBR-4102`.
    - `confirmPasswordReset_incrementZeroAndAlreadyConsumedOrExpired_throws410Mbr4101` — `incrementAttemptCount`=0, `selectByTarget`이 소비됨/만료 행을 반환 → 410/`MBR-4101`(두 서브케이스 유지, 기존 `resetRow` 헬퍼 재사용).
    - `confirmPasswordReset_incrementZeroAndAttemptCountAtCap_throws409Mbr4103` — `incrementAttemptCount`=0, `selectByTarget`이 `attemptCount=5`인 행을 반환 → 409/`MBR-4103`.
    - `confirmPasswordReset_rowMissingAndSimpleMismatch_throwSameExceptionShape`(기존, 사람 수정 (1) 핵심 증거) — mock 배선만 갱신(행 없음 경로는 `incrementAttemptCount`=0+`selectByTarget`=null, 단순 오답 경로는 `incrementAttemptCount`=1)해 두 예외가 여전히 동일한 타입·코드·메시지·상태임을 유지 확인.
  - 동시성(신규, 이 SR의 회귀 대상 그 자체): `MemberPasswordResetConfirmationConcurrencyTest#concurrentWrongCode_burstBeyondCap_doesNotExceedMaxAttempts` — 실 서버(RANDOM_PORT)에 코드 하나를 시드한 뒤, 상한(5)보다 많은 동시 스레드(10, `CyclicBarrier`로 동시 도착 강제)가 같은 target에 **오답** 코드를 동시 제출. 기대: 정확히 5건만 409/`MBR-4102`(증가 성공), 나머지 5건은 409/`MBR-4103`(캡 도달, 미증가), 500 0건, 완료 후 DB `attempt_count`가 정확히 5(그 이상도 이하도 아님) — AS-IS 결함(read-check→write 사이 레이스로 5를 초과해 늘어날 수 있었던 것)이 재발하지 않았음을 실측으로 증명.
  - 기준선 영향: surefire 테스트 총 개수가 늘어난다(DAO 1 + HTTP 1 + 서비스 net 증가 + 동시성 1) — `.speclinker/test_baseline.json` 갱신 필요(다음 단계). 기존 통과 테스트의 기대값은 하나도 바뀌지 않는다(응답 스냅샷 영향 없음, 신규 오류 코드 없음).

- **테스트 격리**: 신규 동시성 테스트는 UUID 접미 target(예: `pwreset-confirm-concurrency-` + `UUID.randomUUID()` + `@example.com`, `MemberPasswordResetDaoTest`의 기존 `concurrencyTarget` 관례와 동일)을 쓰고, `@AfterEach`에서 `passwordResetDao.deleteByTarget(...)`으로 그 행을 지운다(SR-232 r2 재발 방지 — `attempt_count`가 다음 실행/다른 테스트로 새지 않는다). 신규 DAO 캡 테스트와 신규 HTTP 5회 잠금 테스트도 각각 기존 `TARGET`/`seededResetTargets` 관례를 그대로 재사용해도 안전(기존 `@AfterEach cleanUp()`이 매 테스트 후 해당 행을 삭제).

- **폴백·우회 경로의 자격 판정**: 해당 없음 — 이 SR은 기존 `MEMBER_PASSWORD_RESETS` 확정 실패 경로 안의 한 UPDATE 문만 원자화할 뿐, 새로운 인증·조회·폴백 경로를 열지 않는다. `del_yn='N'` 이중 필터·회원 조회 순서·1회용 판정·트랜잭션 경계는 AC에서 명시적으로 변경 대상이 아니다.

- **프레임워크 실행 모델 함정**: 없음 — MyBatis 매퍼 문장 하나의 WHERE 절 추가와 그 반환값 분기이며, 트랜잭션 프록시·self-invocation·스케줄러·React StrictMode 어느 것도 관련되지 않는다(`MemberPasswordResetConfirmationWriter`의 별도 빈 구조·`@Transactional` 경계는 이번 변경 대상이 아니다).

- **범위 밖**: 레이트리밋·코드 발급/만료 정책·화면(확정 문답에서 명시적으로 제외). `confirmIfCodeMatches` 자체의 매치 조건·순서·트랜잭션 경계·1회용 판정(AC 3번째 항목이 명시적으로 변경 대상 아님으로 지정). `handleConfirmFailure`의 "그 외(방어용 폴백)" 분기는 원자 UPDATE의 조건 구성상 이론적으로 도달 불가능한 코드이므로 별도 테스트를 강제하지 않는다(구현은 하되 커버리지 100%를 목표하지 않음).

- **실패 사례집 대조**:
  - "인증코드 시도 횟수를 트랜잭션 안에서 올렸다 → 롤백과 함께 카운터 소실"(SR-231 r2) — 조건: 이 변경이 `incrementAttemptCount` 호출을 트랜잭션으로 감싸는가? 아니다 — `handleConfirmFailure`는 AS-IS와 동일하게 무(無)트랜잭션이고 이번 변경은 그 안의 UPDATE 문 자체(SQL WHERE절)와 호출 순서만 손댄다. 조건 불성립 — 재발 없음.
  - "재작업 지시가 '존재 판정 → 인증' 순서였다 → 존재 오라클"(SR-231 r5) — 조건: 이 변경이 `confirmIfCodeMatches`(코드 확정)와 `memberDao` 조회의 순서를 건드리는가? 아니다 — 이번 변경은 `handleConfirmFailure`(코드 확정이 이미 실패로 끝난 뒤의 분기) 내부에만 있고 `MEMBERS` 테이블은 여전히 조회하지 않는다. 조건 불성립.
  - "같은 리터럴 target 재사용으로 실패 카운터가 실행을 가로질러 누적 → 기대 코드가 다른 코드로 깨짐"(SR-232 r2) — 조건: 신규 동시성 테스트가 리터럴 target을 재사용하고 그 행을 안 지우는가? 아니다 — 위 "테스트 격리"에서 명시한 대로 UUID 접미 + `@AfterEach` 삭제. 조건 불성립.
  - **SR-298.1(같은 SR의 자매 story, STORY-1)의 "사람 수정" 선례** — "0행 사유는 상한 도달 하나만이 아니다(이미 소비·이미 인증/만료·행 없음도 0행), 0행일 때만 원인 판별용 읽기를 허용한다"는 정정이 이미 이 SR 안에서 한 번 발생했다. 조건: 이 story도 동일하게 원자 UPDATE의 기존 매치 조건(`consumed_at IS NULL`·`expires_at > now`)을 그대로 둔 채 `attempt_count < maxAttempts`만 더하므로, 0행 사유가 (행 없음/소비/만료/상한) **4갈래**로 더 많다. 조건 성립 — 선례를 무시하고 "0행=상한"으로만 단순화하면 소비·만료·행없음 케이스가 전부 409 `MBR-4103`으로 오분류돼 회귀(AS-IS 코드 계약 위반)가 난다. 이 계획은 처음부터 0행일 때 `selectByTarget` 재조회로 4갈래를 분류하도록 설계해 동일 실수를 선제적으로 피한다(위 "파일"·"테스트" 절의 4개 분기 테스트가 그 증거).
  - "락 순서를 한 곳에 고정하지 않아 데드락"(SR-231 r3) — 조건: 이 변경이 여러 행·여러 문장에 걸친 락 순서를 새로 만드는가? 아니다 — 잠그는 행은 여전히 PK(`target`) 하나, 문장도 여전히 단일 UPDATE. 조건 불성립.

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
계획 확인: 계획대로 진행 (2026-09-16) — 사람 코멘트: 4갈래 0행 원인 판별은 STORY-1 선례대로(원인 판별 읽기는 0행일 때만). 동시성 테스트는 성공 증가 수·최종 attempt_count 둘 다 단언. 소비된 코드·만료 코드 입력 시 기존 오류 코드(MBR-4101/4102/4103) HTTP 단언 유지. 테스트 데이터는 UUID target + 정리.

### 구현 완료 (2026-09-16)
- **수정 파일(소스, 3개)**:
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberPasswordResetDao.java` — `incrementAttemptCount`에 `@Param("maxAttempts") int maxAttempts` 추가, javadoc을 원자 조건부 UPDATE 서술(반환 0의 4갈래 사유)로 갱신.
  - `modules/shop-api/src/main/resources/mapper/memberPasswordReset.xml` — `incrementAttemptCount` UPDATE에 `AND attempt_count < #{maxAttempts}` 절 추가. 기존 매치 조건(`target`·`consumed_at IS NULL`·`expires_at > #{now}`)은 문자 하나 바꾸지 않음.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetConfirmationService.java` — `handleConfirmFailure`를 재작성: `incrementAttemptCount(normalizedTarget, now, MAX_CONFIRM_ATTEMPTS)`를 가장 먼저·무조건 시도 → 1행이면 `selectByTarget` 없이 바로 `mismatchException()`(409 `MBR-4102`) → 0행일 때만 `selectByTarget`로 원인 재분류(행 없음→4102, 소비/만료→410 `MBR-4101`, 상한 도달→409 `MBR-4103`, 방어용 폴백→4102). `confirmIfCodeMatches` 호출부·순서·존재 오라클 방지 서술은 무변경.
- **수정 파일(테스트, 3개)**:
  - `MemberPasswordResetConfirmationServiceTest.java` — 5단계 실패 분기 테스트 전면 재배선(시그니처 3-인자화): `confirmPasswordReset_simpleMismatchUnderCap_incrementsOnceAndThrows409Mbr4102WithoutSelectByTarget`(신규 — `verify(...).selectByTarget(any())`가 `never()`임이 핵심 전환 증거), `confirmPasswordReset_incrementZeroAndRowMissing_throws409Mbr4102`, `confirmPasswordReset_incrementZeroAndAlreadyConsumed_throws410Mbr4101`, `confirmPasswordReset_incrementZeroAndExpired_throws410Mbr4101`, `confirmPasswordReset_incrementZeroAndAttemptCountAtCap_throws409Mbr4103`, `confirmPasswordReset_rowMissingAndSimpleMismatch_throwSameExceptionShape`(사람 수정 (1) 핵심 증거, mock 배선만 갱신). 테스트 수는 기존과 동일(10개, 1:1 재배선).
  - `MemberPasswordResetDaoTest.java` — 기존 `incrementAttemptCount` 호출부 5곳(단위 4 + 동시성 1)에 `maxAttempts=5` 인자 추가(기대값 무변경). 신규 `incrementAttemptCount_atCap_returnsZeroRowsAndDoesNotIncrementBeyondFive`(5회 연속 증가 1→5 확인 후 6회째 0행·attempt_count 5 유지 — 이 SR의 핵심 산출물).
  - `MemberPasswordResetConfirmationFlowTest.java` — 신규 `wrongCodeFiveTimesThenSixth_locksOutAtFiveWithMbr4103`(순차 오답 5회 각각 409/`MBR-4102`+DB `attempt_count` 실측, 6회째(정답 코드 입력)도 409/`MBR-4103`이며 `attempt_count`는 5 유지 — AS-IS에도 없던 회귀 공백을 메움).
- **신규 파일(테스트, 1개)**: `modules/shop-api/src/test/java/com/sm/lab/shop/MemberPasswordResetConfirmationConcurrencyTest.java` — `MemberRegistrationConcurrencyTest`와 동일 패턴(실 서버 RANDOM_PORT, `TestRestTemplate`, `CyclicBarrier` 10스레드 동시 도착 강제). 오답 버스트 10건 동시 제출 → 정확히 5건 409/`MBR-4102`(증가 성공), 나머지 5건 409/`MBR-4103`(상한 도달, 미증가), 500 0건, 최종 DB `attempt_count`=5(성공 증가 수·최종 카운트 둘 다 단언 — 사람 코멘트 반영). UUID 접미 target + `@AfterEach deleteByTarget`으로 격리.
- **테스트 결과**: 변경 영향 범위 전부 통과 — `MemberPasswordResetDaoTest` 14/14, `MemberPasswordResetConfirmationServiceTest` 10/10, `MemberPasswordResetConfirmationFlowTest` 6/6(기존 5개 무변경 통과 + 신규 1개), `MemberPasswordResetConfirmationConcurrencyTest` 1/1(신규). 전체 스위트 518건 중 516 통과/2 실패 — 실패 2건은 이 SR과 무관(`OrderListEndToEndIntegrationTest`·`ApiKeyAuthIntegrationTest`, `OrderService`의 30일 상대 조회창 날짜 드리프트, STORY-1 QA에서 이미 원인 규명·SR-300/SR-299로 이월된 기존 실패 — 이 story의 diff와 어떤 파일도 공유하지 않음).
- **주요 결정**: 계획·사람 코멘트를 그대로 반영 — 4갈래 0행 원인 판별(행없음/소비/만료/상한)을 STORY-1 선례대로 0행일 때만 재조회. 동시성 테스트는 TestRestTemplate 기반(모든 응답이 409 하나의 상태코드로 통일돼 있어 `MemberPasswordResetConfirmationFlowTest`가 JDK HttpClient로 갈아탄 사유였던 "다양한 상태 코드 혼재" 문제가 없음 — `MemberRegistrationConcurrencyTest`와 동일하게 TestRestTemplate 사용).
- **기준선 영향**: surefire 테스트 총 개수가 515(STORY-1 완료 시점) → 518로 증가(신규 3개: DAO 1 + HTTP 플로우 1 + 동시성 1). `.speclinker/test_baseline.json` 갱신 필요(다음 단계).
- **추적**: 코드에 추적 주석 없음(v5) — 코드→SR은 git(이 SR 브랜치/커밋), 코드→스펙은 INF-MBR-007 anchors.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-16 — CONCERNS
- **Layer1 스펙**: 코드 불일치 0건. AC 전항 충족 실측 — 변경 AC 3항(원자 조건부 UPDATE·영향행수 판별·`confirmIfCodeMatches` 무변경)과 회귀 AC 8항(존재 오라클 방지·`del_yn='N'` 이중 필터·코드확정 선행 순서·BCrypt 항상 실행·미발견 조용히 204·Writer 트랜잭션 경계·0행 세션폐기 스킵·전 기기 로그아웃) 모두 diff로 확인. `confirmIfCodeMatches` 매퍼 문장·`confirmPasswordReset` 1~10단계는 바이트 무변경, `@Transactional` 추가 없음. 사람 코멘트 4건(0행 4갈래 판별·동시성 이중 단언·기존 오류코드 HTTP 단언·UUID target+정리) 전부 반영. **concerns 사유는 코드가 아니라 스펙 문서 지연** — INF-MBR-007이 AS-IS 서술을 유지하고 frontmatter anchors가 이번 변경 라인을 잘라낸다(권고 1·2). STORY-1은 INF-MBR-002의 동일 항목을 `spec=pass`로 기록했으나, MBR-007은 anchors 절단이 추가돼 concerns로 올린다(차단 아님).
- **Layer2 보안**: pass. 인증·인가 무변경(`isOpenRoute` 무인증 계약 그대로), 신규 입력 경로·신규 오류코드 없음, SQL 전부 `#{}` 파라미터 바인딩(주입 경로 없음), 실패 분기에 로그·발송·이벤트 0건(성공 로그만 마스킹 유지). `no-sysout`·`no-printstacktrace` 위반 없음. **존재 오라클 재검증(SR-231 r5 사례집)**: `handleConfirmFailure`가 `MEMBERS`를 여전히 조회하지 않고, "행 없음"과 "단순 오답"이 동일 `mismatchException()`으로 수렴 — `rowNeverRequested_and_simpleWrongCode_produceByteIdenticalResponses` 무변경 통과로 실측 확인. 문장 수 비대칭은 방향만 뒤집혔고 회원 존재가 아닌 "재설정 요청 이력"만 드러난다(권고 6).
- **Layer3 회귀**: concerns. 전체 스위트 **실측 518건/실패 2건** — 실패 2건은 `OrderListEndToEndIntegrationTest`·`ApiKeyAuthIntegrationTest`로, STORY-1 게이트가 원인 규명(`OrderService.java:63` `LocalDate.now().minusDays(30)` 상대 조회창 드리프트)하고 사람이 SR-300/299로 이월·waive한 기존 실패다. 이 story diff와 공유 코드·테이블 없음 — **신규 실패 0건**. 영향 범위 재실행: `MemberPasswordResetDaoTest` 14/14 · `MemberPasswordResetConfirmationServiceTest` 10/10 · `MemberPasswordResetConfirmationFlowTest` 6/6 · `MemberPasswordResetConfirmationConcurrencyTest` 1/1 · `MemberPasswordResetServiceTest` 12/12 · 컨트롤러·Writer 포함 58/58. 변경 blast radius 확인 — `incrementAttemptCount` 호출부는 서비스 1곳 + 테스트뿐(`MemberSignupCompletionDao`의 동명 메서드는 STORY-1 소관, 별개 인터페이스). **동시성 테스트 단독 3회 반복 전부 통과**(SR-231 r3 사례집 규율) — `attempt_count < maxAttempts`가 PK 행 락 위에서 재평가되므로 10건 버스트에서 4102 정확히 5건/4103 정확히 5건이 결정론적. 사례집 5건(SR-231 r2 트랜잭션 내 카운터·r3 락 순서·r5 존재 오라클·SR-232 r2 리터럴 target 누적·SR-298.1 0행 단순화) 재발 조건 전부 불성립 확인. concerns 사유는 기준선 미갱신(권고 3).
- 권고(CONCERNS시):
  1. **(medium, 스펙)** INF-MBR-007.md가 SR-298.2 미반영 — `## 트랜잭션 순서` 3항이 "0행이면 재조회(`selectByTarget`)로 사유 판별"만 서술해 **원자 증가 선행 단계가 통째로 빠졌다**. 액면대로 읽으면 이 SR이 제거한 select→분기→update(read-modify-write)를 그대로 재구현하게 된다. `## 비즈니스 규칙`에도 "5회 상한 보장 주체가 `incrementAttemptCount`의 `attempt_count < #{maxAttempts}` 조건절로 옮겨갔다"는 문장이 없다(INF-MBR-002는 STORY-1 스펙 이력에서 "병렬 버스트 우회 차단(SR-298)" 불릿을 받았으나 MBR-007은 대응 불릿 없음). 변경 이력 표도 없다. → 스펙 재동기화 단계에서 INF-MBR-002의 SR-298 편집과 대칭으로 갱신(트랜잭션 순서 3항 + 병렬 버스트 불릿 + 변경 이력 행).
  2. **(medium, 스펙)** INF-MBR-007 frontmatter anchors가 이번 변경 라인을 절단한다. v5는 코드에 추적 주석을 두지 않으므로 anchors가 코드→스펙 유일 링크인데: `memberPasswordReset.xml:35-57`은 57행(`AND consumed_at IS NULL`)에서 끊겨 **이 SR의 핵심 산출물인 59행 `AND attempt_count &lt; #{maxAttempts}`를 못 보여준다**. `MemberPasswordResetDao.java:41-63`은 javadoc만 담고 64~65행 시그니처(신규 `maxAttempts` 파라미터)를 잘라낸다. `MemberPasswordResetConfirmationService.java:139-162`는 162행에서 끊겨 0행 4갈래 분류의 410/4103/폴백 분기(163~170)를 잘라낸다. → 각각 `35-60`·`41-65`·`139-171`로 갱신.
  3. **(medium, 회귀)** `.speclinker/test_baseline.json`이 `git_head c29747e`(SR-295 이전) 시점 510/1이라 실측 518/2와 8건·1실패 어긋난다. STORY-1이 515/2 시점에 같은 항목을 올렸고 아직 재기록되지 않았으며 이 story가 3건을 더 늘렸다. 실패 2건이 사람 waive 상태여도, 기준선이 낡은 동안은 다음 게이트의 baseline 축이 신규 실패와 waive된 기존 실패를 가를 수 없다. → SR-300 조회창 드리프트 처리 후 `test_baseline_ws.py record . --force`로 518/2 재기록.
  4. **(low, 스펙)** `MemberPasswordResetDao.java:55-56` javadoc에 **이번 변경으로 거짓이 된 절이 남았다** — "위 `confirmIfCodeMatches`가 0행일 때만 서비스가 호출한다(**\"행 없음\"이면 증가시킬 행이 없으므로 호출하지 않는다**)". TO-BE는 행 존재 여부를 보지 않고 무조건 호출하며, 판정을 UPDATE의 WHERE절에 맡기는 것이 이 SR의 요지다. 나머지 javadoc은 SR-298로 갱신됐는데 이 괄호절만 살아남았다. 다음 에이전트가 이 문장을 근거로 증가 앞에 `selectByTarget != null` 가드를 되돌리면 read-then-write 레이스가 그대로 복원된다 — 저위험이나 원복 벡터로는 가장 직접적. → 괄호절 삭제.
  5. **(low, 정확성)** `MemberPasswordResetConfirmationService.java:168-170`의 "이론상 도달 불가능" 폴백 주석은 과장이다. 실제 도달 경로가 있다 — 0행 증가 직후 진단용 `selectByTarget` 사이에 INF-MBR-006 재요청(쿨다운 60초 경과)이 끼면 `touchRequest`가 같은 행의 `attempt_count`를 0, `consumed_at`을 NULL, `expires_at`을 +10분으로 리셋하므로 세 분류 조건이 모두 거짓이 되어 폴백에 도달한다. 결과는 여전히 옳다(진짜 오답이므로 409 `MBR-4102`) — **코드 변경 불필요**, 주석 문구와 "테스트 면제" 근거만 부정확하다. → "동시 재발송 리셋이 끼어들면 도달 가능, 결과는 오답과 동일"로 문구 정정.
  6. **(low, 보안)** 문장 수 비대칭의 방향이 뒤집혔다 — TO-BE: 단순 오답 2문장(confirm+increment, SELECT 없음) / 0행 4갈래 3문장(+`selectByTarget`). AS-IS: 행 없음 2문장 / 그 외 3문장. 크기는 동일한 PK 조회 1회 차이이고 양쪽 모두 `MEMBERS`를 조회하지 않는다. 드러나는 것은 "이 target으로 재설정을 요청한 적 있는가"뿐이며, INF-MBR-006(`requestPasswordResetCode`)은 `MEMBERS`를 조회하지 않고 어떤 target에든 202를 반환하므로 재설정 행 존재는 회원 존재 신호가 아니다 — 존재 오라클 AC 유지. STORY-1 권고 5와 동일 관찰. → 권고 1의 스펙 갱신 시 문장 수 차이를 함께 명문화(코드 변경 불필요).
