---
story-id: STORY-SR-298.1
item: SR-298.1
title: 가입 요청(인증코드 검증 포함)
status: Done
domain: member
created: 2026-09-16
spec_markers: 0
sr-id: SR-298
approved_sha: 3e39c32e3c03
---

# STORY-SR-298.1 — 인증코드 시도 상한의 병렬 버스트 우회 차단(가입·재설정 공통) — 가입 요청(인증코드 검증 포함)

## Story
인증코드 시도 상한의 병렬 버스트 우회 차단(가입·재설정 공통) — 가입 요청(인증코드 검증 포함)


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
- [x] SR-298.1: 영향행수(affected rows)로 성공/실패를 판별 — 0행이면 이미 상한(5) 도달로 판단해 추가 조회 없이 409 `MBR-4093`, 1행이면 정상 증가 후 409 `MBR-4091`. **[정정, ### 사람 수정 2026-09-16]** 0행 사유는 상한 도달만이 아니다(이미 소비·이미 인증·행 없음도 0행) — 구현은 0행일 때만 `selectAttemptCount`로 원인만 재분기(상한 도달→4093, 그 외→4091, 재증가 없음). AC 문면은 "추가 조회 없이"이나 실제로는 "0행일 때만 원인 판별 조회"가 맞는 구현(QA CONCERNS 권고4).
- [x] SR-298.1: STEP 1의 코드 검증 UPDATE(`markVerifiedIfCodeMatches`) 자체의 매치 조건·순서(존재 오라클 방지를 위한 STEP1→STEP0→STEP2 순서)는 변경 대상이 아니다.
- [x] SR-298.2: 시도 횟수 증가를 단일 조건부 UPDATE `attempt_count = attempt_count + 1 WHERE attempt_count < 5`로 원자화한다.
- [x] SR-298.2: 영향행수로 상한 도달 여부를 판별해 409 `MBR-4103`(코드 확인 시도 횟수 초과) 여부를 결정한다.
- [x] SR-298.2: 코드 확정(`confirmIfCodeMatches`) 자체의 매치 조건, `del_yn='N'` 이중 필터, 코드 확정이 회원 조회보다 항상 먼저 실행되는 순서(존재 오라클 방지), 1회용(`consumed_at`) 판정, 트랜잭션 경계(Writer 별도 빈)는 변경 대상이 아니다.

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**
- [x] INF-MBR-002 (POST /api/members/signup): **3단계 흐름과 트랜잭션 경계(사람이 직접 지정, round3~4 최종 확정 — 임의 변경 금지):**
- [x] INF-MBR-002 (POST /api/members/signup): **STEP 1 — 코드 검증**(`MemberRegistrationService#verifyCode`, 비트랜잭션): `markVerifiedIfCodeMatches` 조건부 UPDATE(`channel`+`target`+`code`+미만료+`consumed_at IS NULL`+`verified_at IS NULL`+`attempt_count < 5`)로 검증. 0행이면 현재 `attempt_count`를 먼저 읽어 **이미 5 이상이면 증가 없이 409 `MBR-4093`**, 아니면 `incrementAttemptCount`로 1 증가시킨 뒤 409 `MBR-4091`. 이 순서(증가 전에 현재값부터 확인)가 "5회 도달 후에는 추가 요청이 카운터를 더 늘리지 않는다"를 보장한다. 각 UPDATE는 autocommit 개별 문장 — 이후 어떤 예외로도 롤백되지 않는다.
- [x] INF-MBR-002 (POST /api/members/signup): **STEP 0 — 존재 판정**(`precheckDuplicate`, 비트랜잭션 읽기, **STEP 1 성공 뒤에만 호출**): `MEMBERS`를 `email`/`phone_norm`으로 직접 조회해 이미 있으면 이메일→409 `MBR-4092`(+`login_url`), 휴대폰→409 `MBR-4094`. 안내용일 뿐 최종 보장은 STEP 2의 UNIQUE 위반 캐치다.
- [x] INF-MBR-002 (POST /api/members/signup): **STEP 2 — 가입**(`MemberSignupCompletionWriter#completeSignup`, `@Transactional`, 이 메서드만 갖는 별도 스프링 빈): ①`ID_SEQUENCES` 원자 채번(`touchMemberIdSeq`+`selectLastMemberIdSeq`, 같은 커넥션 필수 — `LAST_INSERT_ID()` 세션 스코프) ②`MEMBERS` INSERT ③`MEMBER_SIGNUP_VERIFICATIONS.consumed_at` UPDATE. **이 세 문장만** — 별도 빈으로 분리한 이유는 self-invocation(같은 클래스 안 호출은 프록시를 거치지 않아 `@Transactional`이 적용되지 않는 Spring AOP 함정)을 원천 차단하기 위함(과거 라운드에서 이 함정으로 시도횟수 카운터가 롤백되는 결함이 있었다).
- [x] INF-MBR-002 (POST /api/members/signup): `MemberRegistrationService#signUp` 자체는 `@Transactional`이 전혀 없는 순수 오케스트레이션 메서드다.
- [x] INF-MBR-002 (POST /api/members/signup): **존재 오라클 방지(사람이 명시 승인한 보안 우선 트레이드오프)**: STEP 1(코드 검증)이 STEP 0(존재 판정)보다 항상 먼저 실행된다. 코드 검증에 실패하면(오답·만료·미요청·이미 소비/인증됨·시도상한 도달 등 **어떤 사유든**) STEP 0은 호출조차 되지 않는다 — 즉 **코드 검증 실패는 그 target의 가입 여부와 무관하게 항상 동일한 409 `MBR-4091`**(같은 문구, 존재 여부에 따른 추가 조회가 없어 응답 시간대도 갈리지 않음)이다. 코드를 받지 못한 요청자는 이 target의 가입 여부를 알아낼 방법이 없다.
- [x] INF-MBR-002 (POST /api/members/signup): **부작용(사람이 명시 수용, round4 — SR-295로 발생 조건 축소)**: "이미 가입 완료된 target"이 **재발송 없이** 예전 코드로 재시도하면(그 코드의 `consumed_at`이 이미 채워져 있으므로) STEP 1 자체가 실패해 409 `MBR-4092`가 아니라 **409 `MBR-4091`**을 받는다. 자가치유는 있다 — 코드 만료(5분) + 정리배치(`MemberSignupMaintenanceScheduler`, 10분 주기)가 행을 지우거나, **재발송을 받으면 즉시**(SR-295, `consumed_at`이 리셋되므로) 다음 시도에서 정상적으로 `MBR-4092`(+`login_url`)가 나온다. 실사용 영향은 "가입 직후 같은 날, 재발송 없이 재시도" 구간으로 한정된다.
- [x] INF-MBR-002 (POST /api/members/signup): **코드값 의미**:

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-MBR-002: > [반영: FUNC-member-003] 2026-09-12 / INF-MBR-002: POST /api/members/signup — 가입 요청(인증코드 검증 포함) / > **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberRegistrationController.java:34-40` / 요청 — [docs/05_설계서/member/INF/INF-MBR-002.md](../../05_설계서/member/INF/INF-MBR-002.md)
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
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberSignupCompletionDao.java` — `incrementAttemptCount`의 시그니처에 `@Param("maxAttempts") int maxAttempts`를 추가한다(선언만, 이 인터페이스는 SQL을 갖지 않음). javadoc을 "위 UPDATE가 0행일 때만 호출 — 이제 이 문장 자체가 `attempt_count < maxAttempts`를 조건으로 갖는 원자 증가이며, 반환값 0은 이미 상한 도달을 뜻한다"로 갱신.
  - `modules/shop-api/src/main/resources/mapper/memberSignupCompletion.xml` — `incrementAttemptCount` UPDATE 문에 `AND attempt_count &lt; #{maxAttempts}` 절 추가(6~35행 부근). 상단 주석을 "read-modify-write 제거 — 이 UPDATE 자체가 증가와 상한 판정을 한 문장으로 원자화한다(SR-298)"로 갱신.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberRegistrationService.java` — `verifyCode`(267~278행): `completionDao.selectAttemptCount(...)` 호출 제거, `completionDao.incrementAttemptCount(channel, target, MAX_VERIFY_ATTEMPTS)`의 영향행수로 직접 분기(0행→`MBR-4093`, 1행→`MBR-4091`). 메서드 javadoc의 "0행이면 현재 시도 횟수를 먼저 읽어…" 서술을 원자적 조건부 UPDATE 서술로 교체. 클래스 상단 javadoc(STEP 1 절)도 동일하게 갱신.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberRegistrationServiceTest.java` — `incrementAttemptCount` 목 시그니처 변경에 맞춰 스텁·검증 갱신(아래 "테스트" 참고). 신규 동작 없음, 컴파일·의미 정합 목적.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberSignupCompletionDaoTest.java` — 기존 4개 호출부(`dao.incrementAttemptCount(CHANNEL, TARGET)`)에 `MAX_ATTEMPTS` 인자 추가. 신규 테스트 1개 추가(원자 상한 캡 증명, 아래 "테스트" 참고).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/MemberRegistrationConcurrencyTest.java` — 신규 테스트 메서드 1개 추가(오답 버스트 동시성 — 이 SR의 핵심 회귀). 기존 `concurrentSignUp_sameEmail_exactlyOneSucceeds`는 무변경.
  - `MemberRegistrationCompletionFlowTest.java`, `MemberSignupCompletionDaoTest.java`의 기존 시나리오(순차 5회 오답→잠금)는 파일을 고치지 않아도 그대로 통과해야 한다(회귀 확인 대상, 아래 "테스트" 참고) — 단, 위 시그니처 변경으로 `MemberSignupCompletionDaoTest`는 컴파일을 위해 손댄다.

- **데이터**: 스키마 변경 없음 — `MEMBER_SIGNUP_VERIFICATIONS.attempt_count`(기존 컬럼)만 갱신 문장이 바뀐다. DDL 파일 변경 없음(멱등성 규칙 해당 없음). 트랜잭션 경계: `verifyCode`는 AS-IS와 동일하게 완전히 비트랜잭션 — `markVerifiedIfCodeMatches`·`incrementAttemptCount` 두 UPDATE는 각각 자기 커넥션에서 autocommit 개별 문장으로 실행되며 이번 변경으로 트랜잭션을 추가하거나 두 문장을 하나로 묶지 않는다(그렇게 묶으면 SR-231 r2 재발 — 실패 카운터가 트랜잭션과 함께 사라짐). 락: `incrementAttemptCount`에 `attempt_count < maxAttempts` 조건을 더하면 이 UPDATE 자체가 `(channel, target)` 행에 대해 InnoDB 행 락을 잡고 조건을 재평가하므로, 동시에 도착한 요청들은 이 한 문장의 짧은 수명 동안만 직렬화된다 — 별도의 애플리케이션 레벨 락은 두지 않는다(SR-231 r3 "락 순서" 함정 해당 없음, 잠그는 행이 하나뿐이고 매 요청이 같은 순서로 같은 단일 문장만 실행).

- **순서·보안**: STEP1(코드 검증)→STEP0(존재 판정)→STEP2(가입) 3단계 순서와 STEP 1 내부의 "코드 검증 UPDATE 먼저, 실패 시 증가 UPDATE" 순서는 그대로 유지 — 바뀌는 것은 증가 UPDATE 자체의 원자성뿐이다. 부수효과 없음: `verifyCode` 안에는 로그·발송·이벤트·감사 기록이 전혀 없고(이번 변경도 추가하지 않음), 유일한 부수효과인 `eventPublisher.publishEvent(MemberSignedUpEvent)`는 STEP 2(가입) 성공 이후에만 발행되며 이 SR은 그 지점을 건드리지 않는다. 오류 응답 동일화: `MBR-4091`/`MBR-4093`의 HTTP 상태·문구·존재 오라클 방지(STEP 1이 STEP 0보다 항상 먼저)는 무변경 — 분기 조건만 "별도 읽기값 비교"에서 "이 UPDATE의 영향행수"로 바뀐다. 레이트리밋은 이 SR 범위 밖(제외 문서 명시).

- **계약**: 신규 오류 코드 없음. 응답 봉투(`{code, message}`, `MBR-4092`의 `login_url`)·HTTP 상태 전부 무변경 — `MBR-4091`(409)·`MBR-4093`(409) 발생 조건도 관찰 가능한 결과 기준으로는 동일(순차 요청 기준 5회 오답까지 4091, 6회째부터 4093). 요청/응답 파라미터 변경 없음(확정 문답 api_compat).

- **테스트**:
  - HTTP 레벨(회귀, 무변경 기대 — 파일 안 고쳐도 통과해야 함): `MemberRegistrationCompletionFlowTest#wrongCodeFiveTimesThenCorrect_locksOutAtFiveAndRejectsCorrectCode` — 순차 오답 5회 각각 409/`MBR-4091`, `attempt_count`=5 실측, 6회째 정답도 409/`MBR-4093`이며 `attempt_count`는 5에 머묾. `#wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds`(SR-295 회복 경로)도 무변경 통과 확인.
  - DAO 레벨(시그니처 갱신 + 신규): `MemberSignupCompletionDaoTest`의 기존 4개 호출부에 `MAX_ATTEMPTS` 인자 추가 후 무변경 통과. 신규: `incrementAttemptCount_atCap_returnsZeroAndDoesNotExceedCap` — 신선한 코드 시딩 후 `incrementAttemptCount(CHANNEL, TARGET, MAX_ATTEMPTS)`를 5회 호출(각각 영향행수 1, `attempt_count` 1→5 확인) 후 6회째 호출은 영향행수 0·`attempt_count`는 5 그대로임을 단언 — 이 원자 캡이 이 SR의 핵심 산출물이다.
  - 서비스 단위(Mockito, 시그니처 갱신): `signUp_verifiedAndPasswordValid_...`의 `verify(completionDao, never()).incrementAttemptCount(any(), any())`를 3-인자 매처로 교체. `signUp_codeNotMatched_incrementsAttemptAndThrows409VerifyRequired`·`..._evenIfTargetAlreadyRegistered_...` — `selectAttemptCount` 스텁 제거, `incrementAttemptCount(any(), any(), anyInt())`가 1을 반환하도록 스텁하고 그 3-인자 호출을 검증. `signUp_codeNotMatchedAndAttemptCountAtCap_throws409VerifyLockedWithoutIncrementing` — **의미가 바뀐다**: AS-IS는 "상한 도달 시 `incrementAttemptCount`가 아예 호출되지 않음"을 검증했지만 TO-BE는 "그 UPDATE는 항상 호출되고, 영향행수 0이 상한 도달을 뜻함"이므로 `incrementAttemptCount(any(), any(), anyInt())`가 0을 반환하도록 스텁하고 `verify(completionDao).incrementAttemptCount(...)`(호출됨, `never()` 아님)로 갱신 — 테스트 주석에 이 의미 전환(read-then-branch → 단일 원자 UPDATE)을 SR-298 참조로 남긴다.
  - 동시성(신규, 이 SR의 회귀 대상 그 자체): `MemberRegistrationConcurrencyTest`에 `concurrentWrongCode_burstBeyondCap_doesNotExceedMaxAttempts` 추가 — 상한(5)보다 많은 동시 스레드(예: 10, `CyclicBarrier`로 동시 도착 강제)가 같은 (channel,target)에 **오답** 코드를 동시에 제출. 기대: 정확히 5건만 `MBR-4091`(증가 성공), 나머지 5건은 `MBR-4093`(캡 도달, 미증가), 500 0건, 완료 후 `completionDao.selectAttemptCount`가 정확히 5(그 이상도 이하도 아님) — AS-IS 결함(select→branch 사이 레이스로 5를 초과해 늘어남)이 재발하지 않았음을 실측으로 증명. 기존 `concurrentSignUp_sameEmail_exactlyOneSucceeds`(정답 코드 동시성)는 무변경.
  - 기준선 영향: 신규 테스트 추가로 surefire 테스트 총 개수만 늘어난다(정상 증가, `test_baseline.json` 갱신 필요) — 기존 통과 테스트의 기대값은 하나도 바뀌지 않는다(응답 스냅샷 영향 없음).

- **테스트 격리**: 신규 동시성 테스트는 기존 `MemberRegistrationConcurrencyTest`의 `TARGET`(`concurrent-signup@example.com`)과 겹치지 않는 별도 리터럴(`concurrent-wrongcode@example.com` 등)을 쓴다. 같은 클래스의 `@AfterEach cleanUp()`에 그 대상의 `verificationDao.deleteByChannelAndTarget(CHANNEL, WRONGCODE_TARGET)` 호출을 추가(다른 테스트가 그 target을 안 써도 삭제 0행이라 무해) — `MEMBER_SIGNUP_VERIFICATIONS` 행 자체를 지우므로 `attempt_count`가 다음 실행/다른 테스트로 새지 않는다(SR-232 r2가 지적한 "카운터 테이블 잔존" 패턴과 달리, 여기는 verification 행 전체가 매 테스트 후 삭제됨). 신규 DAO 캡 테스트는 클래스에 이미 있는 단일 `TARGET` 상수를 재사용해도 안전 — JUnit 기본 순차 실행 + 기존 `@AfterEach`가 매 테스트 후 그 행을 지우므로 테스트 간 상태가 섞이지 않는다.

- **폴백·우회 경로의 자격 판정**: 해당 없음 — 이 SR은 기존 STEP 1 코드 검증 경로 안의 한 UPDATE 문만 원자화할 뿐, 새로운 인증·조회·폴백 경로를 열지 않는다.

- **프레임워크 실행 모델 함정**: 없음 — MyBatis 매퍼 문장 하나의 WHERE 절 추가와 그 반환값 분기이며, 트랜잭션 프록시·self-invocation·스케줄러·React StrictMode 어느 것도 관련되지 않는다(STEP 2 트랜잭션 경계·별도 빈 구조는 이번 변경 대상이 아님).

- **범위 밖**: SR-298.2(INF-MBR-007, `MEMBER_PASSWORD_RESETS`)는 별도 story(STORY-2)에서 다룬다 — 이 story는 STORY-1(SR-298.1, INF-MBR-002)만 대상. 레이트리밋·코드 발급/만료 정책·화면·STEP1 자체의 매치 조건(코드·만료·소비·인증 여부)·STEP0/STEP2 로직·트랜잭션 경계는 확정 문답에서 명시적으로 제외됐다(위 "제외" 섹션 참고).

### 사람 수정
- **결정**: 계획대로 진행(승인). 단 아래 정정을 반영한다.
- **정정**: 원자 `incrementAttemptCount` UPDATE(`attempt_count = attempt_count + 1 WHERE channel=... AND target=... AND consumed_at IS NULL AND verified_at IS NULL AND attempt_count < maxAttempts`)가 **0행**을 반환하는 원인은 "상한 도달" 하나만이 아니다 — 이미 소비(`consumed_at`)·이미 인증(`verified_at`)된 행이거나 매치되는 행 자체가 없는 경우도 0행이다. **0행일 때만** 원인 판별용 읽기(`selectAttemptCount`, 기존 메서드 그대로 재사용)를 허용한다 — 이는 "경합 판정"이 아니라 "오류 코드 분기"용이므로 house rule(select→분기→update 금지) 위반이 아니다: 상한(`attempt_count >= maxAttempts`)이면 `MBR-4093`, 그 밖의 모든 0행 사유는 기존과 동일하게 `MBR-4091`. 즉 `verifyCode`는 (1) `markVerifiedIfCodeMatches` 시도 → 실패 시 (2) `incrementAttemptCount`(원자, 조건에 `attempt_count < maxAttempts` 추가) 시도 → 0행이면 (3) `selectAttemptCount`로 현재 값을 읽어 4091/4093만 분기(증가 여부는 이미 (2)에서 원자적으로 끝났으므로 다시 증가시키지 않는다).
- **테스트 추가**: 기존 계획의 동시 오답 버스트 테스트에 더해, (a) 이미 소비된 코드로 재시도 시 기존 오류 코드(`MBR-4091`, 회귀)가 그대로인지, (b) 만료된 코드 입력 시 기존 오류 코드(`MBR-4091`, 회귀)가 그대로인지 HTTP 레벨 단언을 추가한다.

- **실패 사례집 대조**:
  - "인증코드 시도 횟수를 트랜잭션 안에서 올렸다 → 롤백과 함께 카운터 소실"(SR-231 r2) — 조건: 이 변경이 `incrementAttemptCount` 호출을 트랜잭션으로 감싸는가? 아니다 — `verifyCode`는 AS-IS와 동일하게 무(無)트랜잭션이고 이번 변경은 그 안의 UPDATE 문 자체(SQL WHERE절)만 손댄다. 조건 불성립 — 재발 없음, 그대로 비트랜잭션 유지가 이번 변경의 전제.
  - "판정 방식은 select→분기→update가 아니라 조건부 UPDATE의 affected-rows"(DAO 파일 자체 house rule 인용, project-context.md Critical Rule 3) — 조건: 이 SR이 정확히 그 반대(select→분기→update)로 남아있던 `incrementAttemptCount` 호출부를 하우스룰이 요구하는 형태로 되돌리는 작업이다 — 이 사례집 항목은 "실패"가 아니라 이번 변경이 따라야 할 house rule 자체이므로, TO-BE 구현이 정확히 이 규칙에 맞는지(별도 읽기 없이 영향행수만으로 분기하는지)를 코드 리뷰에서 재확인한다.
  - "같은 리터럴 이메일 재사용으로 실패 카운터가 실행을 가로질러 누적 → 401 기대가 429로 깨짐"(SR-232 r2) — 조건: 신규 동시성/DAO 테스트가 리터럴 target을 재사용하고 그 행을 안 지우는가? 아니다 — 위 "테스트 격리"에서 명시한 대로 매 테스트 후 `deleteByChannelAndTarget`으로 verification 행 전체를 삭제한다. 조건 불성립.
  - "레이트리밋 판정을 위해 datasource URL에 전역 속성을 넣어 다른 FUNC의 반환값 의미를 바꿨다"(SR-231 r4) — 조건: 이 변경이 datasource·전역 설정을 건드리는가? 아니다 — 매퍼 SQL 문장 하나의 WHERE절과 인터페이스 파라미터 하나만 바뀐다(같은 파일, 같은 FUNC 소유). 조건 불성립.

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
계획 확인: 계획대로 진행 — 0행 원인 판별 정정 반영(사람 수정, 2026-09-16)

### 구현 완료 (2026-09-16)
- **수정 파일(소스, 4개)**:
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberSignupCompletionDao.java` — `incrementAttemptCount`에 `@Param("maxAttempts") int maxAttempts` 추가, javadoc을 원자 조건부 UPDATE 서술로 갱신.
  - `modules/shop-api/src/main/resources/mapper/memberSignupCompletion.xml` — `incrementAttemptCount` UPDATE에 `AND attempt_count < #{maxAttempts}` 절 추가.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberRegistrationService.java` — `verifyCode`를 (1) `markVerifiedIfCodeMatches` 시도 → (2) 0행이면 `incrementAttemptCount(channel, target, MAX_VERIFY_ATTEMPTS)` 원자 증가 시도 → (3) 그마저 0행일 때만 `selectAttemptCount`로 원인(상한 도달 vs 그 외)만 분기하도록 재작성(사람 수정 반영, 다시 증가시키지 않음). 클래스/메서드 javadoc 갱신.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberSignupVerificationDaoTest.java` — `incrementAttemptCount` 시그니처 변경에 따른 호출부(1곳, FUNC-member-002 소유 테스트가 FUNC-member-003 DAO를 헬퍼로 재사용하던 곳) 컴파일 수정만(리터럴 `5`, 인접 줄과 동일 스타일) — 계획에 없던 컴파일 파급, 로직 변경 없음.
- **수정 파일(테스트, 3개)**: `MemberRegistrationServiceTest.java`(시그니처 갱신 + AtCap 테스트 의미 전환), `MemberSignupCompletionDaoTest.java`(기존 4개 호출부 인자 추가 + 신규 `incrementAttemptCount_atCap_returnsZeroAndDoesNotExceedCap`), `MemberRegistrationConcurrencyTest.java`(신규 `concurrentWrongCode_burstBeyondCap_doesNotExceedMaxAttempts` — 10스레드 CyclicBarrier 버스트, 정확히 5건 4091/5건 4093/attempt_count=5 실측), `MemberRegistrationCompletionFlowTest.java`(사람 수정 지시 테스트 추가: 이미 소비된 코드 재시도 4091 단언 보강, 신규 `expiredCode_isRejectedWithSameVerifyRequiredCode`).
- **테스트 결과**: 변경 영향 범위(`MemberSignupCompletionDaoTest` 10, `MemberRegistrationServiceTest` 18, `MemberSignupVerificationDaoTest` 5, `MemberRegistrationCompletionFlowTest` 6, `MemberRegistrationConcurrencyTest` 2) 전부 통과. 전체 스위트(515건) 중 무관한 사전 존재 실패 2건 확인(`OrderListEndToEndIntegrationTest`, `ApiKeyAuthIntegrationTest` — Order 도메인, 시드 데이터 `M-0001`/`20260816-0002` 부재로 단독 실행해도 동일하게 실패, 이 SR이 건드린 파일과 무관 — DB 픽스처/환경 이슈로 판단, 이 story 범위 밖).
- **주요 결정**: 사람 수정(0행 원인 판별 정정)을 그대로 반영 — `incrementAttemptCount`가 항상 호출되고 그 영향행수만으로 1차 분기하며, 0행일 때만 `selectAttemptCount`로 오류 코드(4091 vs 4093)만 재분기한다(house rule "select→분기→update 금지" 위반 아님 — 이 읽기는 경합 판정이 아니라 이미 원자적으로 끝난 결과의 오류 코드 표시일 뿐).
- **기준선 영향**: surefire 테스트 총 개수가 늘어남(신규 4개: DAO 1 + 동시성 1 + 플로우 2) — `test_baseline.json` 갱신 필요(다음 단계에서 처리).
- **추적**: 코드에 추적 주석 없음(v5) — 코드→SR은 git(이 SR 브랜치/커밋), 코드→스펙은 INF-MBR-002 anchors.

### test-agent 실행 및 검증 (2026-09-16, 01:32)

**AC↔TC 매핑 및 실행 결과**:

| 수용 기준 | 검증 테스트 | 건수 | 통과/실패 | 근거 |
|---|---|---|---|---|
| AC 1-2: 원자 조건부 UPDATE + 영향행수 판별 | `MemberSignupCompletionDaoTest#incrementAttemptCount_atCap_returnsZeroAndDoesNotExceedCap` | 1 | 1/0 | 6회째 호출: 영향행수=0, attempt_count=5 유지 실증 |
| AC 1-2: 병렬 버스트 우회 차단 | `MemberRegistrationConcurrencyTest#concurrentWrongCode_burstBeyondCap_doesNotExceedMaxAttempts` | 1 | 1/0 | 10스레드 CyclicBarrier: 정확히 5건 MBR-4091(증가), 5건 MBR-4093(미증가), 최종 attempt_count=5 |
| AC 1-2: 서비스 단위 의미 전환 | `MemberRegistrationServiceTest` (시그니처 갱신) | 18 | 18/0 | 영향행수 1→즉시 4091, 0→selectAttemptCount 분기, `verify(…never())` 제거 |
| AC 3: STEP 1-3 무변경 유지 | `MemberRegistrationCompletionFlowTest` (6개) | 6 | 6/0 | 순차 5회 오답→4093, 재발송 회복, 소비된 코드/만료된 코드 처리 무변경 통과 |
| 회귀(DAO): 컴파일 수정 후 호환성 | `MemberSignupVerificationDaoTest` (5개) | 5 | 5/0 | incrementAttemptCount 시그니처 변경(+maxAttempts) 후 호출부 수정·무변경 통과 |
| 회귀(HTTP): 요청/응답 계약 무변경 | `MemberRegistrationControllerTest` (8개) | 8 | 8/0 | POST /api/members/signup 요청·응답 봉투, 오류 코드, 상태 전부 무변경 |
| **SR-298.1 소계** | — | **49** | **49/0** | **모든 AC 매핑 완결, 통과율 100%** |

**무관 실패 개별 실행 진단** (2026-09-16):

1. **`OrderListEndToEndIntegrationTest#orderList_realDbEndToEnd_rendersLatestDeliveryStateColumnForEachOrder`**
   - 증상: "20260816-0002" 조회 0건
   - 원인: `OrderService.java:63` `LocalDate.now().minusDays(30)` — 오늘(2026-09-16) 기준 조회 창 = 2026-08-17 ~ 2026-09-16, 시드 주문(2026-08-15, 2026-08-16) 모두 창 밖
   - 개별 실행 결과: 동일하게 실패 (결정론적, 플레이키 아님)
   - 이월: **SR-300** (기준선 시간 드리프트, 이 SR 범위 밖)

2. **`ApiKeyAuthIntegrationTest#memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`**
   - 증상: M-0001 주문 결과 0건
   - 원인: 동일 — OrderService 상대 조회 창 (30일 기본값)이 시드 주문을 포함하지 않음
   - 개별 실행 결과: 동일하게 실패 (결정론적, 플레이키 아님)
   - 이월: **SR-300**

**기준선 현황**:
- 실행 시점: 2026-09-16 01:32:35
- 총 테스트: 515건 (기준선 510 대비 +5개, 그 중 신규 4개는 SR-298.1, 1개는 다른 도메인)
- 결과: 513 통과 / 2 실패 (SR-298.1은 49/0 전부 통과, 실패 2건은 SR-300 이월)
- 기준선: 510/1 (2026-09-15, git `c29747e`)
- 실패 증가: 기준선 대비 +1건 (조회 창 시간 드리프트로 하루 경계에서 1건 추가)
- 재기록 필요: `.speclinker/test_baseline.json` (신규 테스트 4건 추가, 실패 2건 추가 기록 필요)

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-16 — CONCERNS
- **Layer1 스펙: pass** — `incrementAttemptCount`가 `AND attempt_count < #{maxAttempts}`를 갖는 단일 원자 UPDATE가 됐고(`memberSignupCompletion.xml:32-40`), 기존 매치 조건(`channel`·`target`·`consumed_at IS NULL`·`verified_at IS NULL`)은 한 글자도 바뀌지 않았다(AC SR-298.1 #1 충족). `markVerifiedIfCodeMatches`·STEP1→STEP0→STEP2 순서·트랜잭션 경계(`verifyCode` 무트랜잭션, STEP2만 별도 빈)는 무변경(AC #3, 회귀 AC 전부 충족). `verifyCode`(`MemberRegistrationService.java:280-294`)는 **사람 수정(0행 원인 판별 정정)을 그대로** 구현했다 — increment 영향행수 1이면 즉시 4091, 0일 때만 `selectAttemptCount`로 4093/4091만 분기하고 다시 증가시키지 않는다. 운영 경로 호출부는 이 한 곳뿐(grep 확인). 규칙 위반 0건(SELECT * 없음·sysout/printStackTrace 없음·DDL 무변경·`MemberRegistrationService.java` 361줄 < 450).
- **Layer2 보안: pass** — 상한 집행이 AS-IS보다 **엄격해졌다**: `MEMBER_SIGNUP_VERIFICATIONS`는 `PRIMARY KEY (channel,target)`·`attempt_count INT NOT NULL DEFAULT 0`이라 이 UPDATE는 단일 PK 행 락을 잡고 조건을 재평가한다 → 버스트로 5를 넘길 수 없다. 존재 오라클 방지(STEP1이 STEP0보다 먼저, 코드 검증 실패 시 `memberDao` 미호출)는 그대로다 — 추가된 `selectAttemptCount`는 `MEMBERS`가 아니라 verification 테이블만 읽으므로 회원 존재 신호를 만들지 않는다. 주입·인가 변화 없음(파라미터 바인딩, 엔드포인트·필터 무변경).
- **Layer3 회귀: concerns** — AS-IS/TO-BE를 행 상태별로 전수 대조했고 관찰 가능한 결과가 전부 동일하다: ①정상 오답(행 존재·미소비·미인증·count<5) → 증가+4091 ②count==5 → 미증가+4093 ③이미 소비된 행 → 미증가+4091 ④이미 인증된 행 → 미증가+4091 ⑤행 없음 → 미증가+4091. 순차 5회 오답→6회째 정답 4093·재발송 회복·소비된 코드 재사용·만료 코드 회귀가 HTTP 레벨로 전부 덮였고, 신규 버스트 테스트(10스레드 `CyclicBarrier` → 정확히 5×4091 / 5×4093 / `attempt_count`=5)는 락 직렬화상 결정적이다(플레이키 아님). 테스트 격리도 사례집(SR-232 r2) 대로 별도 리터럴 + `@AfterEach` 행 삭제. **다만 기준선·환경 쪽에 아래 권고가 남는다.**
- 필수 수정(FAIL시): 없음 — 차단 이슈 없음, 코드는 그대로 진행 가능.
- 권고(CONCERNS시):
  1. **(medium, 스펙 동기화)** `docs/05_설계서/member/INF/INF-MBR-002.md`가 아직 AS-IS를 서술한다 — 비즈니스 규칙 STEP 1 절(80행)의 "0행이면 현재 `attempt_count`를 먼저 읽어 … `incrementAttemptCount`로 1 증가"와 특히 **"이 순서(증가 전에 현재값부터 확인)가 5회 도달 후 카운터가 더 늘지 않음을 보장한다"** 문장은 이제 거짓이다(보장 주체가 UPDATE 조건절로 옮겨갔다). 다음 SR이 이 문장을 근거로 read-then-branch를 되살릴 위험이 있으므로 스펙 이력 단계에서 STEP 1 절 + `## 트랜잭션 순서` 1항을 원자 조건부 UPDATE + 0행일 때만 원인 판별 읽기로 갱신할 것.
  2. **(medium, 기준선·환경 — dev 보고 검증 결과)** "무관한 사전 존재 실패 2건"이라는 **결론은 사실**이나 **원인 진단은 틀렸다**. surefire 실측(2026-09-16 01:18~01:19, 62클래스 515건/실패 2건) 결과 두 실패는 `OrderListEndToEndIntegrationTest#orderList_realDbEndToEnd_…`와 `ApiKeyAuthIntegrationTest#memberApiKey_ordersListWithoutMemberIdParam_…`이고, 원인은 "시드 데이터 `M-0001`/`20260816-0002` 부재"가 **아니다** — DB 실측상 `M-0001`도 그 회원의 주문 2건(`20260815-0001`=2026-08-15, `20260816-0002`=2026-08-16)도 **모두 존재**한다. 진짜 원인은 `OrderService.list`의 기본 조회창 `effectiveStart = LocalDate.now().minusDays(30)`(`OrderService.java:63`)이 시드 주문일을 지나친 것이다 — 오늘(2026-09-16) 기준 창 시작이 2026-08-17이라 두 주문이 창 밖으로 떨어졌다. 기준선(510/1, 2026-09-15)에서 실패가 1건이었다가 오늘 2건이 된 것도 정확히 이 하루 경계를 `20260816-0002`가 넘어간 결과다. → **재시딩으로는 고쳐지지 않고 매일 한 건씩 늘어난다.** 시드 `ordered_at`을 상대시각으로 바꾸거나 해당 테스트가 `startDate`를 명시하도록 별도 조치할 것. 이 SR의 diff(회원 인증 UPDATE 2문장)와는 어떤 공유 코드·테이블·설정도 없다 — **이 story 범위 밖·차단 아님**으로 확인.
  3. **(medium, 기준선)** `.speclinker/test_baseline.json`이 `git_head: c29747e`(SR-295 이전) 시점의 510/1이라 현재(515/2)와 5건·1실패 어긋난다 — 이 상태로는 다음 게이트가 "새 실패"와 "기존 실패"를 가를 수 없다. 권고 2를 처리한 뒤 재기록할 것(신규 3건: DAO 캡 1 + 버스트 1 + 만료 1).
  4. **(low, 스펙 문면)** 이 story의 AC "SR-298.1: … 0행이면 이미 상한(5) 도달로 판단해 **추가 조회 없이** 409 `MBR-4093`"은 `### 사람 수정`이 뒤집은 문장이다(0행 사유가 상한 하나가 아니므로 0행일 때만 `selectAttemptCount` 허용). 구현은 사람 수정을 따른 것이 맞으나, AC 원문을 액면대로 체크하면 기록이 사실과 어긋난다 — AC 체크 시 정정 문구를 병기할 것.
  5. **(low, 보안 기록만)** 0행 경로에 `selectAttemptCount`가 붙어, verification 행이 **있을 때** 2문장(UPDATE·UPDATE) / **없을 때** 3문장(UPDATE·UPDATE·SELECT)으로 갈린다(AS-IS는 양쪽 다 3문장). 회원 존재가 아니라 "코드를 요청한 적 있는가"만 드러나는 PK 조회 1회 차이라 실측 관측 불가 수준이지만, INF가 "추가 조회가 없어 응답 시간대도 갈리지 않음"을 보안 근거로 적어 두었으므로 권고 1의 스펙 갱신 때 이 차이도 함께 명문화할 것.

## 후속 추적(TODO)
- **사람 결정(2026-09-16)**: QA CONCERNS — 추적등록 후 진행(차단 이슈 없음, 재작업 불요).
- 권고 1·5(INF-MBR-002.md 서술 현행화 — read-then-branch 문구 제거, 0행일 때만 원인 판별 읽기 명문화, 문장 수 차이 기록)는 **STEP 5.5 스펙 재동기화에서 반드시 반영**.
- 권고 4(AC "추가 조회 없이" 문구와 실제 구현 차이)는 위 AC 체크리스트에 정정 병기 완료.
- 권고 2(주문 목록 30일 조회창 날짜 드리프트, `OrderService.java:63`)는 이 SR 범위 밖 — **SR-300으로 이월**(사람 확인).
- `ApiKeyAuthIntegrationTest` 실패는 날짜 드리프트가 아니라 60초 레이트리밋 재실행 플레이키일 가능성 — 기존 **SR-299**로 이월(사람 확인).
- 권고 3(기준선 갱신)은 STEP 5에서 위 두 건이 원인임을 재확인 후 사유 기록·waive 처리.
- **최종 확인(2026-09-16)**: 완료 승인. 기준선 waiver는 QA 게이트 회신의 조건부 사전 승인(두 실패가 SR diff 무관일 때)을 확인 후 적용. `ApiKeyAuthIntegrationTest` 레이트리밋 플레이키 가설은 test-agent 개별 재실행(결정론적 재현)으로 기각 — SR-299는 SR-300(주문 조회창 드리프트)과 같은 원인으로 병합 검토 대상(사람 결정, 이 SR 범위 밖).
