---
story-id: STORY-SR-295.1
item: SR-295.1
title: 재발송 시 인증코드 시도 횟수·소비 표시 초기화
status: Done
domain: member
created: 2026-09-15
spec_markers: 0
sr-id: SR-295
approved_sha: 4060df520f8d
---

# STORY-SR-295.1 — 인증코드 재발송 시 시도 횟수 초기화(MBR-4093 회복 경로) — 재발송 시 인증코드 시도 횟수·소비 표시 초기화

## Story
인증코드 재발송 시 시도 횟수 초기화(MBR-4093 회복 경로) — 재발송 시 인증코드 시도 횟수·소비 표시 초기화


## 변경 컨텍스트 (SR-295)
> 이 story는 변경요청 **SR-295 — 인증코드 재발송 시 시도 횟수 초기화(MBR-4093 회복 경로)** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-295/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-295/02_변경명세.md`

### 확정된 요건 문답 4건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 재발송(INF-MBR-001 writeCode) 시 같은 채널·타깃 행의 attempt_count=0, consumed_at=NULL, verified_at=NULL로 새 코드 시작 — 잠금(MBR-4093) 회복 경로. 제외: 비밀번호 재설정 코드(MEMBER_PASSWORD_RESETS)·레이트리밋 정책·화면.
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 재발송 레이트리밋(MEMBER_SIGNUP_RATE_LIMITS)·코드 만료(expires_at)·5회 오답 잠금 판정(INF-MBR-002)·가입 성공 시 코드 소비는 그대로. INF-MBR-001/002 응답 형식 불변.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필요 — 요청·응답 형식 변경 없음(서버 동작만 변경: 재발송이 시도 횟수·소비 표시를 초기화).
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 변경 없음 — 기존 오류 계약 유지(MBR-4093 시도 초과 등). 재발송 뒤에는 새 코드로 정상 검증되어 MBR-4093이 나오지 않아야 한다.

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-295/02_변경명세.md`에서 도출)
- [x] INF-MBR-001: 같은 채널·타깃 행에 대한 재발송(`writeCode`) 시 6자리 코드·만료시각 갱신과 함께 **`attempt_count=0`, `consumed_at=NULL`, `verified_at=NULL`로 함께 리셋**한다.
- [x] INF-MBR-001: 요청·응답 파라미터·형식(`target` 입력, `{channel, target, expiresInSeconds}` 응답)은 변경 없음 — 서버 내부 쓰기 동작만 확장.
- [x] INF-MBR-002: STEP 1 조건부 UPDATE 로직·조건식(`channel`+`target`+`code`+미만료+`consumed_at IS NULL`+`verified_at IS NULL`+`attempt_count < 5`), 3단계 흐름과 트랜잭션 경계, 오류코드·오류계약(`MBR-4091`/`MBR-4092`/`MBR-4093`/`MBR-4094`/`MBR-4001`/`MBR-5000`)은 **변경 없음**.
- [x] INF-MBR-002: INF-MBR-001의 재발송 리셋 덕분에, 잠긴(`attempt_count>=5`) target이 재발송을 받으면 새 코드 행은 `attempt_count=0`이므로 STEP 1 조건을 다시 만족해 정상 검증 가능해진다(=`MBR-4093` 회복 경로가 생김).
- [x] INF-MBR-002: STEP 1의 로직 자체는 그대로이고, INF-MBR-001이 써 두는 선행 상태만 바뀐다.

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**
- [x] INF-MBR-001 (POST /api/members/signup/verification-codes): 인증코드는 6자리 숫자, 발급 시점부터 5분(300초) 유효(`MemberSignupService.EXPIRES_IN_SECONDS`)
- [x] INF-MBR-001 (POST /api/members/signup/verification-codes): 채널 자동판별: 이메일 정규식 매칭 → `EMAIL`, 국내 휴대폰번호 패턴(`01[016789]` + 7~8자리, 하이픈 없음) 매칭 → `SMS`, 둘 다 아니면 400
- [x] INF-MBR-001 (POST /api/members/signup/verification-codes): 레이트리밋(동일 `target` 기준, 채널 무관): 60초 쿨다운 + 하루 5회 상한. **거부된 요청은 quota를 소비하지 않는다** — 원자 UPSERT의 조건부 갱신이라 `daily_count`/`last_token`이 갱신되지 않으면 그 요청은 카운트에 반영되지 않는다
- [x] INF-MBR-001 (POST /api/members/signup/verification-codes): 판정 방식(round5/round6, 사람 결정): 요청마다 생성한 UUID 토큰을 조건부로 `last_token`에 기록한 뒤 재조회한 값이 자기 토큰과 같으면 허용 — JDBC affected-rows나 datasource 전역 설정(`useAffectedRows`)에 더 이상 의존하지 않는다(그 설정이 이 FUNC 밖 `ProductDao.decreaseStock`의 반환값 시맨틱까지 바꿔 `POST /api/orders`를 200→409로 회귀시킨 적이 있어 제거됨)
- [x] INF-MBR-001 (POST /api/members/signup/verification-codes): 발송은 시뮬레이션(로그 1줄, 개인정보 마스킹 — 앞 2자 제외 `*` 처리) — 실제 이메일/SMS 게이트웨이 미연동. 코드 값은 응답·로그 어디에도 노출하지 않는다
- [x] INF-MBR-001 (POST /api/members/signup/verification-codes): 레거시 주의: `MEMBER_SIGNUP_VERIFICATIONS.requested_at`/`previous_requested_at`/`daily_count` 컬럼은 round3 잔재(deprecated) — round4부터 앱이 더 이상 쓰지 않는다(카운터는 `MEMBER_SIGNUP_RATE_LIMITS`로 완전히 이전됨). `attempt_count`는 이 API가 컬럼만 소유하고 쓰지 않는다(FUNC-member-003 가입완료 API가 코드 검증 시 사용 예정)
- [x] INF-MBR-001 (POST /api/members/signup/verification-codes): 만료행 정리(purge)는 이 요청 경로에 없다 — `MemberSignupMaintenanceScheduler`(`@Scheduled`, 10분 주기 배치)가 `purgeExpiredCodes`/`purgeOldRows`를 별도로 호출한다
- [x] INF-MBR-002 (POST /api/members/signup): **3단계 흐름과 트랜잭션 경계(사람이 직접 지정, round3~4 최종 확정 — 임의 변경 금지):**
- [x] INF-MBR-002 (POST /api/members/signup): **STEP 1 — 코드 검증**(`MemberRegistrationService#verifyCode`, 비트랜잭션): `markVerifiedIfCodeMatches` 조건부 UPDATE(`channel`+`target`+`code`+미만료+`consumed_at IS NULL`+`verified_at IS NULL`+`attempt_count < 5`)로 검증. 0행이면 현재 `attempt_count`를 먼저 읽어 **이미 5 이상이면 증가 없이 409 `MBR-4093`**, 아니면 `incrementAttemptCount`로 1 증가시킨 뒤 409 `MBR-4091`. 이 순서(증가 전에 현재값부터 확인)가 "5회 도달 후에는 추가 요청이 카운터를 더 늘리지 않는다"를 보장한다. 각 UPDATE는 autocommit 개별 문장 — 이후 어떤 예외로도 롤백되지 않는다.
- [x] INF-MBR-002 (POST /api/members/signup): **STEP 0 — 존재 판정**(`precheckDuplicate`, 비트랜잭션 읽기, **STEP 1 성공 뒤에만 호출**): `MEMBERS`를 `email`/`phone_norm`으로 직접 조회해 이미 있으면 이메일→409 `MBR-4092`(+`login_url`), 휴대폰→409 `MBR-4094`. 안내용일 뿐 최종 보장은 STEP 2의 UNIQUE 위반 캐치다.
- [x] INF-MBR-002 (POST /api/members/signup): **STEP 2 — 가입**(`MemberSignupCompletionWriter#completeSignup`, `@Transactional`, 이 메서드만 갖는 별도 스프링 빈): ①`ID_SEQUENCES` 원자 채번(`touchMemberIdSeq`+`selectLastMemberIdSeq`, 같은 커넥션 필수 — `LAST_INSERT_ID()` 세션 스코프) ②`MEMBERS` INSERT ③`MEMBER_SIGNUP_VERIFICATIONS.consumed_at` UPDATE. **이 세 문장만** — 별도 빈으로 분리한 이유는 self-invocation(같은 클래스 안 호출은 프록시를 거치지 않아 `@Transactional`이 적용되지 않는 Spring AOP 함정)을 원천 차단하기 위함(과거 라운드에서 이 함정으로 시도횟수 카운터가 롤백되는 결함이 있었다).
- [x] INF-MBR-002 (POST /api/members/signup): `MemberRegistrationService#signUp` 자체는 `@Transactional`이 전혀 없는 순수 오케스트레이션 메서드다.
- [x] INF-MBR-002 (POST /api/members/signup): **존재 오라클 방지(사람이 명시 승인한 보안 우선 트레이드오프)**: STEP 1(코드 검증)이 STEP 0(존재 판정)보다 항상 먼저 실행된다. 코드 검증에 실패하면(오답·만료·미요청·이미 소비/인증됨·시도상한 도달 등 **어떤 사유든**) STEP 0은 호출조차 되지 않는다 — 즉 **코드 검증 실패는 그 target의 가입 여부와 무관하게 항상 동일한 409 `MBR-4091`**(같은 문구, 존재 여부에 따른 추가 조회가 없어 응답 시간대도 갈리지 않음)이다. 코드를 받지 못한 요청자는 이 target의 가입 여부를 알아낼 방법이 없다.
- [x] INF-MBR-002 (POST /api/members/signup): **부작용(사람이 명시 수용, round4)**: 위 순서 때문에 "이미 가입 완료된 target"이 코드 검증에 성공한 뒤에도(=재발송을 받아 정답 코드를 낸 뒤에도) `consumed_at`이 채워진 채 남아 있으면 STEP 1 자체가 실패해 409 `MBR-4092`가 아니라 **409 `MBR-4091`**을 받는다(가입 완료 시 이미 그 target의 코드가 소비 처리되기 때문). 자가치유는 있다 — 코드 만료(5분) + 정리배치(`MemberSignupMaintenanceScheduler`, 10분 주기)가 행을 지우면 다음 발송분부터 정상적으로 `MBR-4092`가 나온다. 실사용 영향은 "가입 직후 같은 날 재시도" 구간으로 한정된다.
- [x] INF-MBR-002 (POST /api/members/signup): **코드값 의미**:

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-MBR-001: INF-MBR-001: POST /api/members/signup/verification-codes — 회원가입 인증코드 발송 / > **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberSignupController.java:38-42` / 요청 / - Method: POST — [docs/05_설계서/member/INF/INF-MBR-001.md](../../05_설계서/member/INF/INF-MBR-001.md)
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
  - `modules/shop-api/src/main/resources/mapper/memberSignupVerification.xml` — `writeCode`의 `ON DUPLICATE KEY UPDATE` 절에 `consumed_at = NULL`, `attempt_count = 0`을 추가한다. `INSERT` 브랜치는 손대지 않는다(신규 행은 이미 DDL 기본값으로 `consumed_at IS NULL`·`attempt_count=0`이라 리셋할 대상이 없다). 파일 상단의 "attempt_count는 여기서 건드리지 않는다(FUNC-member-003 소유 컬럼)" 주석은 이제 사실과 달라지므로 함께 고친다(추적 주석이 아니라 동작 설명 주석 — 갱신 대상).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberSignupVerificationDao.java` — `writeCode` 메서드 시그니처는 변경 없음. 클래스 javadoc(14~21행)이 "재발송이면 이전 코드를 완전히 덮어쓰고 verified_at을 초기화한다"고만 적혀 있어 이제 부정확하므로 attempt_count/consumed_at 리셋을 추가 서술한다(순수 문서 갱신, 인터페이스 변경 없음).
  - `MemberSignupService.java`, `MemberSignupVerification.java`(도메인), 컨트롤러 — **변경 없음**. 요청/응답 파라미터·서비스 오케스트레이션·오류 계약이 전부 그대로라는 것이 AC에 명시돼 있고, 실제로 이 SR의 전체 효과는 UPSERT의 SET 절 두 컬럼 추가로 끝난다(변경명세 "롤백 요지"도 이 SET 절만 제거하면 AS-IS 복귀라고 명시).
  - INF-MBR-002 쪽 파일(`MemberRegistrationService`, `MemberSignupCompletionDao`, `memberSignupCompletion.xml` 등) — **변경 없음**(STORY AC에 "STEP 1 조건부 UPDATE 로직·조건식은 변경 없음"이 명시돼 있고, 실제로 STEP 1의 SQL·순서는 그대로다 — 다만 그 SQL이 보는 선행 데이터 상태(consumed_at/attempt_count/verified_at)가 이번 변경으로 바뀐다. 이 차이를 테스트 절에서 다룬다).

- **데이터**:
  - 트랜잭션 경계 변경 없음 — `writeCode`는 지금도 단일 autocommit 문(원자 UPSERT)이고 이번 변경도 같은 한 문장 안에 SET 절만 추가하는 것이라 트랜잭션 경계·락 범위가 넓어지지 않는다. INF-MBR-001의 "1) 레이트리밋 UPSERT → 2) 재조회 판정 → 3) writeCode" 3단계 구조는 그대로다.
  - DDL 변경 없음 — `consumed_at`(V3__members_signup.sql에서 이미 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`로 추가됨), `attempt_count`(member_signup_verifications.sql에 이미 `DEFAULT 0`)는 기존 컬럼을 그대로 쓴다. 새 컬럼·인덱스·멱등 DDL 추가가 필요 없다.
  - 원자성: `INSERT ... ON DUPLICATE KEY UPDATE`는 그 자체로 원자적이므로 "읽고 판단하고 쓰는" 별도 단계가 생기지 않는다. SR-231 r2(카운터를 트랜잭션 안에서 올렸다가 롤백에 같이 사라짐)와 같은 함정은 애초에 성립하지 않는다 — `attempt_count=0` 리셋도 이 원자 UPSERT 한 문장 안에서 일어나 실패할 경로가 없다.

- **순서·보안**:
  - INF-MBR-001의 기존 순서(레이트리밋 판정 → writeCode → 발송 로그)는 바뀌지 않는다. 로그는 여전히 writeCode 성공 이후에만 찍히므로 SR-234 FUNC-member-008 r1(쿨다운 판정 전에 로그부터 남겨 로그와 실제 DB 상태가 어긋난 사례)과 같은 순서 결함이 새로 생길 여지가 없다 — 이번 변경은 로그 위치를 건드리지 않는다.
  - **존재 오라클 재검토(중요)**: INF-MBR-002의 STEP 1(코드 검증)이 STEP 0(존재 판정)보다 항상 먼저 실행된다는 순서는 이번 SR로 전혀 바뀌지 않는다(STORY AC에도 "STEP 1의 로직 자체는 그대로"라고 명시). 다만 재발송이 `consumed_at`을 리셋하게 되면서, "이미 가입 완료된 target"이 **올바른 새 코드**로 재시도했을 때 STEP 1이 이제 성공하고, 뒤이은 STEP 0에서 중복 이메일이 걸려 409 `MBR-4092`(+`login_url`)가 다시 나오게 된다(round4 전 동작으로 부분 회귀). 이것이 SR-231 r5류의 존재 오라클 재발인지 검토했다: STEP 1은 여전히 **정확한 코드값**을 요구하는 조건부 UPDATE이고, INF-MBR-001 응답은 코드값을 담지 않는다("코드 값은 응답·로그 어디에도 노출하지 않는다") — 즉 target을 소유하지 않은 제3자는 재발송 API를 아무리 호출해도 코드 자체를 알아낼 방법이 없다. 따라서 이 경로로 가입 여부를 알아낼 수 있는 것은 "실제로 그 채널(이메일/휴대폰)의 코드를 받을 수 있는 사람"뿐이고, 그 사람은 이미 자신이 그 target으로 가입했는지 알고 있으므로 새로운 정보 노출이 아니다. **오라클 방지 요건은 유지된다** — 다만 아래 테스트 절에서 이 의도된 부작용을 명시적으로 검증한다.
  - 부수효과(로그·발송) 없음 — writeCode 자체는 로그를 찍지 않는다(로그는 서비스 계층에서 writeCode 이후 별도로 찍힘, 이번 변경 대상 아님).

- **계약**: 새 오류 코드·응답 봉투·상태 코드 없음. INF-MBR-001 요청/응답 스키마 불변(AC 명시), INF-MBR-002 오류 계약(`MBR-4091`/`MBR-4092`/`MBR-4093`/`MBR-4094`/`MBR-4001`/`MBR-5000`) 불변 — 다만 **같은 시나리오가 어떤 코드로 떨어지는지**가 선행 데이터 상태 변화로 바뀌는 케이스가 하나 있다(아래 테스트 절 참고). 코드·계약 자체의 변경이 아니라 "어느 조건에서 어느 코드가 나오는가"의 자연스러운 결과다.

- **테스트**:
  - `MemberSignupVerificationDaoTest`(신규 케이스 추가, 기존 3건은 그대로 둠): `writeCode_reissue_resetsAttemptCountAndConsumedAt`. 흐름 — ①`dao.writeCode`로 신규 코드 기록 ②이미 있는 `MemberSignupCompletionDao`(FUNC-member-003)를 함께 autowire해 `incrementAttemptCount`를 3회 호출(조건이 `consumed_at IS NULL AND verified_at IS NULL`뿐이라 통과)해 `attempt_count=3`을 실제 프로덕션 경로로 만들고 ③`markVerifiedIfCodeMatches`(같은 코드, maxAttempts=5)로 `verified_at`을 세팅 ④`consumeVerifiedCode`로 `consumed_at`을 세팅 ⑤그 뒤 `dao.writeCode`를 **재호출**(재발송)해 새 코드로 덮어쓰고 ⑥`dao.selectByChannelAndTarget(...).getAttemptCount()==0`, `verifiedAt==null`(기존에 이미 검증되던 것)을, `completionDao.selectConsumedAt(...)==null`을 단언한다. 원시 SQL(JdbcTemplate 직접 UPDATE)로 상태를 조작하지 않고 기존 DAO 메서드 조합으로 실제와 같은 순서로 상태를 만든다 — 별도 SELECT 컬럼·도메인 필드 추가 불필요(`consumed_at` 조회는 이미 `MemberSignupCompletionDao#selectConsumedAt`가 제공).
  - `MemberRegistrationCompletionFlowTest`에 신규 HTTP 레벨 시나리오 추가(기존 target 리터럴 `attempt-cap@example.com`과 충돌하지 않는 새 target, 예: `attempt-cap-recovery@example.com`): 기존 (a) 테스트(`wrongCodeFiveTimesThenCorrect_locksOutAtFiveAndRejectsCorrectCode`)와 동일하게 5회 오답으로 잠근 뒤(`attempt_count==5`, 6번째 요청도 `MBR-4093`) **실제 재발송 엔드포인트**(`POST /api/members/signup/verification-codes`, `restTemplate`)를 호출해 200과 `{channel,target,expiresInSeconds}` 응답 형식을 단언하고, `verificationDao.selectByChannelAndTarget(...)`으로 새로 생성된 코드값을 읽어(응답에는 코드가 없으므로 DB에서 확인 — 기존 테스트들도 이 방식), 그 코드로 `callSignUp`을 호출해 **201**을 단언한다(=AC "재발송 뒤에는 새 코드로 정상 검증되어 MBR-4093이 나오지 않아야 한다"). 재발송 직후 `attempt_count==0`도 별도로 단언해 리셋을 직접 증명한다.
  - **기존 테스트 하나를 의도적으로 고친다**: `reSignUpWithAlreadyRegisteredEmail_afterResend_returns409VerifyRequiredNotEmailDuplicate`(151~170행)는 "재발송이 consumed_at/attempt_count를 리셋하지 않는다"는 AS-IS 전제로 짜여 있다. 이 SR로 그 전제가 깨진다 — 재발송(`writeCode`) 후 새 코드로 재시도하면 이제 STEP 1이 **성공**하고, STEP 0에서 기존 회원과 충돌해 409 `MBR-4092`(+`login_url`)가 나온다(위 "순서·보안" 절 분석). 이 클래스의 37~45행 javadoc이 이미 `{@link #reSignUpWithAlreadyRegisteredEmail_afterResend_returns409EmailDuplicateWithLoginUrl}`라는, **지금은 존재하지 않는 메서드명**을 가리키고 있다 — round4 작성자가 SR-295가 들어오면 이 테스트가 이 이름·이 assertion으로 돌아갈 것을 미리 예견해 남긴 표식으로 읽힌다. 계획: 메서드명을 그 `{@link}`가 가리키는 이름(`reSignUpWithAlreadyRegisteredEmail_afterResend_returns409EmailDuplicateWithLoginUrl`)으로 바꾸고, assertion을 `retry.getStatusCode()==409`, body에 `"code":"MBR-4092"`와 `login_url` 포함으로 교체하며, 161~163행 주석(재발송이 리셋하지 않는다는 설명)도 새 동작에 맞게 고친다. 클래스 상단 37~45행 javadoc도 "SR-295로 이 회귀가 해소됐다"는 취지로 갱신한다.
  - `reSignUpWithAlreadyRegisteredEmail_withoutRequestingNewCode_returns409VerifyRequiredNotEmailDuplicate`(177~194행)는 재발송을 호출하지 않는 시나리오라 이번 변경의 영향을 받지 않는다 — **그대로 둔다**(회귀 유지 확인용으로 남긴다).
  - `MemberRegistrationConcurrencyTest`, `MemberRegistrationPhoneNormalizationTest`, `MemberSignupCompletionDaoTest`, `ApiKeyAuthIntegrationTest`의 `writeCode` 호출은 모두 target당 1회뿐(재발송 없음)이라 영향 없음 — 확인만 하고 수정하지 않는다.
  - 기준선 영향: `.speclinker/test_baseline.json`의 shop-api 테스트 수가 신규 케이스 2건만큼 늘고(순증), 기존 테스트 1건의 이름·assertion이 바뀐다(테스트 총량 관점에선 net +2). `mvnw test` 전체 그린 확인 후 `test_baseline_ws.py record . --force`로 갱신 필요(QA/재기록은 이 계획의 범위 밖 — `/sl-test` 단계에서 처리).

- **테스트 격리**: 신규 target 리터럴(`attempt-cap-recovery@example.com` 등)은 기존 클래스의 다른 테스트가 쓰는 target과 겹치지 않게 고른다. `MemberSignupVerificationDaoTest`의 신규 케이스도 클래스의 `CHANNEL`/`TARGET` 상수를 그대로 쓰되(기존 관례), `@AfterEach`의 `dao.deleteByChannelAndTarget(CHANNEL, TARGET)`가 이미 정리하므로 추가 정리 코드는 불필요. `MemberRegistrationCompletionFlowTest`의 신규 케이스는 기존 `cleanupTarget`/`cleanupMemberId` 필드에 값만 채우면 기존 `@AfterEach`가 정리한다(레이트리밋 테이블 `MEMBER_SIGNUP_RATE_LIMITS`는 새 target이라 이전 실행의 쿨다운 상태가 없다 — SR-232 r2류의 카운터 누적 플레이키 위험 없음, 단 재실행 시 리터럴 target이 겹치면 레이트리밋 60초 쿨다운에 걸릴 수 있으므로 이미 이 클래스가 쓰는 "고유한 상수 문자열 target" 관례를 그대로 따른다).

- **폴백·우회 경로의 자격 판정**: 해당 없음 — 이번 SR은 새 인증·조회 경로(DB 폴백·캐시·화이트리스트)를 열지 않는다. 기존 무인증 화이트리스트(`MEMBER_SIGNUP_VERIFICATION_CODE_PATH`, `MEMBER_SIGNUP_REQUEST_PATH`)도 그대로다.

- **프레임워크 실행 모델 함정**: 없음 — `writeCode`는 지금도 단일 SQL 문(autocommit)이라 Spring 트랜잭션 프록시·self-invocation 문제가 개입할 여지가 없고(트랜잭션 자체가 없음), 스케줄러(`MemberSignupMaintenanceScheduler`)나 React effect 이중 실행 같은 요소는 이 변경 범위에 없다(화면 변경 없음, `screens: []`).

- **범위 밖**: 비밀번호 재설정 코드(`MEMBER_PASSWORD_RESETS`) — 확정 문답에서 명시 제외, 이번 변경이 만지는 테이블·매퍼와 완전히 별개. 레이트리밋 정책 로직(쿨다운·일일상한·토큰 판정) — 확정 문답에서 회귀유지로 명시, `touchRateLimit`/`selectRateLimit` 어느 것도 건드리지 않는다. 화면 — 두 INF 모두 `screens: []`. INF-MBR-002 STEP 1 SQL·조건식 자체의 수정 — AC가 "변경 없음"을 명시했고 실제로 손대지 않는다(그 SQL이 보는 선행 상태만 이번 변경으로 바뀔 뿐).

- **실패 사례집 대조**:
  - SR-231 r2("카운터를 트랜잭션 안에서 올렸다가 롤백에 같이 사라짐") — 조건(트랜잭션으로 감싸 롤백 가능한 경로) 자체가 이번 변경에 없다. `writeCode`는 원자 UPSERT 한 문장이고 `@Transactional`이 전혀 없다. 해당 없음.
  - SR-231 r4(전역 datasource 속성을 한 FUNC 필요로 바꿔 다른 FUNC 반환값 시맨틱이 깨짐) — 이번 변경은 SET 절 컬럼만 추가하고 datasource·전역 설정은 건드리지 않는다. 해당 없음.
  - SR-231 r5(재작업 지시가 "존재 판정 → 인증" 순서였고, 그대로 구현했다가 존재 오라클 재발) — 이번 변경으로 STEP 0/STEP 1 순서 자체는 그대로이지만, 순서가 지켜져도 **선행 데이터 상태 변화**로 다른 코드가 나오는 케이스가 생긴다. 위 "순서·보안" 절에서 이 조건(코드값이 응답에 노출되지 않는다는 것)이 여전히 성립함을 확인해 오라클이 재발하지 않음을 논증했다 — 판단을 코드 리뷰 때 재확인해야 하는 지점으로 표시해 둔다.
  - SR-232 r2(테스트가 리터럴 이메일을 재사용하며 `@AfterEach` 정리를 빠뜨려 카운터가 누적) — 신규 테스트는 클래스 기존 관례(고유 상수 target + 기존 `@AfterEach` 필드)를 그대로 따라 이 문제를 피한다.
  - SR-232 r3(STORY "테스트" 서술과 결정표가 모순됐을 때 결정표가 우선, 코드가 아니라 테스트를 고침) — 이번 건에서도 기존 테스트의 서술(재발송이 리셋 안 함)이 이번 SR의 변경명세(TO-BE)와 모순된다. 원칙대로 **테스트를 변경명세에 맞게 고친다**(프로덕션 코드가 아니라). 다행히 이 경우엔 이미 코드 자체(javadoc `{@link}`)가 그 새 기댓값의 정확한 이름까지 예견해 둬서 재해석 여지가 거의 없었다.
  - SR-234 FUNC-member-008 r1(부수효과가 판정보다 먼저 실행돼 로그와 DB가 어긋남) — 이번 변경은 로그 위치를 건드리지 않고, writeCode 자체도 로그를 찍지 않는다. 해당 없음(위 "순서·보안"에 재확인 기록).

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
- 계획 확인: 계획대로 진행 (2026-09-15) — 사람 코멘트: 기존 테스트(재발송 후 409 VERIFY_REQUIRED 전제)를 새 동작(409 MBR-4092+login_url)으로 고치는 것은 SR-295의 의도된 결과로 승인. 존재 오라클 재검토 결론에 동의 — 코드 리뷰에서 코드 비노출 유지 재확인 필요.
- 구현 완료 (2026-09-15) — 계획대로 진행, 계획과의 이탈 없음.
  - 수정: `modules/shop-api/src/main/resources/mapper/memberSignupVerification.xml` — `writeCode`의 `ON DUPLICATE KEY UPDATE` 절에 `attempt_count = 0`, `consumed_at = NULL` 추가(INSERT 브랜치는 미변경). 상단 동작 설명 주석도 새 동작에 맞게 갱신.
  - 수정: `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberSignupVerificationDao.java` — `writeCode` javadoc에 SR-295 리셋 동작 서술 추가(시그니처·인터페이스 변경 없음).
  - 수정(테스트): `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberSignupVerificationDaoTest.java` — `MemberSignupCompletionDao` autowire 추가 + 신규 케이스 `writeCode_reissue_resetsAttemptCountAndConsumedAt`(기존 3건은 그대로 둠).
  - 수정(테스트): `modules/shop-api/src/test/java/com/sm/lab/shop/MemberRegistrationCompletionFlowTest.java` — 클래스 javadoc에 SR-295 절 추가. 신규 케이스 `wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds`(target `attempt-cap-recovery@example.com`, 실제 `POST /api/members/signup/verification-codes` 호출로 MBR-4093 잠금 회복 검증). 기존 `reSignUpWithAlreadyRegisteredEmail_afterResend_returns409VerifyRequiredNotEmailDuplicate`를 계획대로 `reSignUpWithAlreadyRegisteredEmail_afterResend_returns409EmailDuplicateWithLoginUrl`로 개명하고 assertion을 409 `MBR-4092`+`login_url` 포함으로 교체.
  - 변경 없음(계획대로): `MemberSignupService.java`, `MemberSignupVerification.java`, 컨트롤러, INF-MBR-002 쪽 파일(`MemberRegistrationService`, `MemberSignupCompletionDao`, `memberSignupCompletion.xml`) 전부 미수정.
  - 검증: `mvnw test-compile` 통과, 대상 테스트 클래스(`MemberSignupVerificationDaoTest`·`MemberRegistrationCompletionFlowTest`·`MemberRegistrationConcurrencyTest`·`MemberRegistrationPhoneNormalizationTest`·`MemberSignupCompletionDaoTest`·`ApiKeyAuthIntegrationTest`) 개별 실행 그린. 전체 `mvnw test`는 512건 중 실패 1건 — `OrderListEndToEndIntegrationTest`(order 도메인, 이번 SR이 건드리지 않은 파일)만 실패, member 도메인은 전부 그린(순증 2건 반영). 이 실패는 이번 변경과 무관한 기존 환경 이슈로 보이며 SR-295 범위 밖(FUNC-order-001)이라 손대지 않음 — `/sl-test` 단계에서 별도 확인 필요.
  - 참고: 재발송 레이트리밋 엔드포인트는 target당 60초 쿨다운이 있어, 같은 target으로 짧은 간격에 테스트를 반복 실행하면 429가 날 수 있음(신규 테스트가 검증 중 실제로 겪음 — 쿨다운 경과 후 재실행해 정상 통과 확인). 코드 결함이 아니라 기존 클래스 관례(고유 target 리터럴)가 이미 전제하는 동작.

- 재작업 완료 (2026-09-15) — round1 QA CONCERNS 재작업 지시 1번(medium/regression, SR-232 r2류 레이트리밋 행 누적)만 반영. 지시 2(low/security, 범위 밖 근거 기록)·지시 3(low/regression, ApiKeyAuthIntegrationTest 잠복 이슈 → SR-299 이월)은 사람 코멘트대로 이번 라운드에서 코드 변경 없음.
  - 수정(테스트): `modules/shop-api/src/test/java/com/sm/lab/shop/MemberRegistrationCompletionFlowTest.java`
    - import 추가: `com.sm.lab.shop.dao.MemberSignupRateLimitDao`, `java.time.LocalDate`.
    - 필드 추가: `@Autowired MemberSignupRateLimitDao rateLimitDao`, `String cleanupRateLimitTarget`(null이면 정리 대상 없음 — 기존 테스트들은 모두 `writeCode` 직접 호출로 레이트리밋을 우회하므로 이 필드를 쓰지 않음).
    - `@AfterEach cleanUp()`에 `if (cleanupRateLimitTarget != null) rateLimitDao.deleteRateLimit(cleanupRateLimitTarget, LocalDate.now());` 추가 — 저장소 선례 `MemberSignupRateLimitConcurrencyTest:71-74`와 동일한 패턴(`(target, dayKey)` 키로 삭제, 없어도 0 반환이라 안전).
    - `wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds`(유일하게 실제 `POST /api/members/signup/verification-codes` 엔드포인트를 리터럴 target `attempt-cap-recovery@example.com`으로 호출하는 케이스)에 `cleanupRateLimitTarget = target;` 한 줄 추가.
  - 다른 파일(매퍼 XML, DAO, 서비스, 다른 테스트 클래스)은 이번 재작업에서 미수정 — round1 지적이 이 클래스의 `@AfterEach` 누락 하나였고 지시 범위도 그것뿐.
  - 검증(완료 조건 — 사람 코멘트): `mvnw test-compile` 통과. `MemberRegistrationCompletionFlowTest`를 연속 2회 단독 실행(`-Dtest=MemberRegistrationCompletionFlowTest`) — 1회차 5/5 그린(4.8초), 2회차(1회차 종료 직후, 60초 이내 재실행) 5/5 그린(4.8초) — 레이트리밋 행 정리로 쿨다운 429 재발 없음을 실측 확인. `mvnw test` 전체 스위트 512건 중 실패 1건(`OrderListEndToEndIntegrationTest`, order 도메인) — round1과 동일한 기존 실패(기준선에도 이미 있던 건, 이번 SR·이번 재작업과 무관), 기준선 대비 악화 0.
  - SR-299: ApiKeyAuthIntegrationTest:380,392의 동형 잠복 이슈(권고 3) 이월 확인 — 이번 재작업 커밋에 포함하지 않음.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-15 — CONCERNS
- Layer1 스펙: **pass**. `writeCode`의 `ON DUPLICATE KEY UPDATE`에 `attempt_count = 0`, `consumed_at = NULL` 추가(`verified_at = NULL`은 기존에 이미 있었음) — AC의 3컬럼 리셋을 정확히 충족. `MEMBER_SIGNUP_VERIFICATIONS`의 유일 키가 `PRIMARY KEY (channel, target)` 뿐임을 DDL에서 확인(`code`에 UNIQUE 없음) — 코드값 우연 충돌로 **다른 target 행이 리셋되는 경로는 존재하지 않는다**. INSERT 브랜치 미변경이 안전한 것도 확인(`consumed_at DATETIME NULL`, `attempt_count INT NOT NULL DEFAULT 0`). 요청/응답 계약 불변(`VerificationCodeResult(channel, target, expiresInSeconds)`, 컨트롤러·서비스·DTO 무변경). INF-MBR-002 쪽은 diff가 0바이트 — STEP 0/1/2 순서·조건식·트랜잭션 경계·오류계약 전부 그대로. 적용 규칙 7건 위반 없음(`no-select-star` 컬럼 명시 유지, DDL 변경 없음, 운영코드 신규 없음, 파일 246/126/45줄로 상한 이내).
- Layer2 보안: **pass**. 사람 코멘트가 요구한 "코드 비노출 재확인"을 소스에서 직접 검증 — `MemberSignupService.requestVerificationCode`는 `new VerificationCodeResult(channel, trimmed, EXPIRES_IN_SECONDS)`를 반환해 코드값이 응답에 없고, 로그도 `channel={}, target={}(mask), 유효기간={}초`로 코드를 찍지 않는다. 따라서 STEP 1 통과에는 여전히 **정확한 코드 소지**가 필요하고, 코드 검증 실패 시 STEP 0은 호출되지 않아 가입 여부와 무관하게 동일한 409 `MBR-4091`이 나간다 — round4의 존재 오라클 방지 요건은 유지된다. 새로 열린 `MBR-4092`+`login_url` 경로는 채널 소유자만 도달 가능하고 그는 이미 자신의 가입 여부를 안다. STEP 0 레이스의 최종 방어선인 `uq_members_email`/`uq_members_phone_norm` UNIQUE 인덱스도 존재 확인(SR-295가 이 경로 도달 빈도를 늘리므로 함께 점검).
- Layer3 회귀: **concerns**. 전체 스위트를 surefire 리포트로 독립 집계 — 512건 / 실패 1건 / 에러 0건. 유일 실패는 `OrderListEndToEndIntegrationTest`(order 도메인)이고, **SR-295 편집 이전 커밋(c29747e)에서 재기록된 `.speclinker/test_baseline.json`이 이미 510건 중 동일 1건 실패를 기록**하고 있어 기존 실패임이 확인된다 — 이번 SR의 회귀가 아니라는 dev 주장은 사실. 변경 두 클래스는 5/5 그린. `writeCode`의 운영 호출자는 `MemberSignupService:137` 하나뿐. 다만 아래 medium 1건.
- 권고(CONCERNS시):
  1. **(medium · 테스트 격리 — 사례집 SR-232 r2 재발)** `MemberRegistrationCompletionFlowTest`의 신규 케이스 `wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds`는 이 클래스에서 **처음으로 레이트리밋이 걸린 실제 엔드포인트**(`POST /api/members/signup/verification-codes`)를 호출하는데, 클래스 `@AfterEach`(75~83행)는 `MEMBER_SIGNUP_VERIFICATIONS`와 `MEMBERS`만 지우고 **`MEMBER_SIGNUP_RATE_LIMITS`를 지우지 않는다**. 리터럴 target `attempt-cap-recovery@example.com` + `(target, day_key)` 행이 실행을 가로질러 남아 ①직전 실행 60초 이내 재실행 → 쿨다운 429로 `resend==200` 단언 실패 ②같은 날 6번째 허용 실행 → 일일상한 429로 그날 내내 실패한다. dev가 검증 중 실제로 429를 겪고도(Dev 기록 136행) "기존 관례가 전제하는 동작"으로 분류했으나, 이 클래스의 기존 테스트들은 모두 `verificationDao.writeCode`를 직접 불러 레이트리밋을 우회하므로 기존 관례가 이 경우를 덮지 못한다. 구현 계획 "테스트 격리"(105행)가 단언한 "SR-232 r2류 카운터 누적 플레이키 위험 없음"은 이 케이스에 대해 성립하지 않는다. **수정**: 같은 저장소의 선례 `MemberSignupRateLimitConcurrencyTest:71-74`처럼 `MemberSignupRateLimitDao`를 autowire하고 `@AfterEach`에 `rateLimitDao.deleteRateLimit(target, LocalDate.now())`를 추가한다(정리 대상 target을 담을 필드 1개 추가).
  2. **(low · 정보성, 후속 TODO — 이번 라운드 수정 대상 아님)** 잠금 리셋으로 대입 예산이 "코드 수명당 5회"에서 "재발송 5회/일 × 5회 = 약 25~30회/일"로 늘어난다(코드 공간 10^6). 보상 통제는 변경되지 않은 일일 재발송 상한이고, 레이트리밋 정책은 확정 문답에서 범위 밖으로 명시됐으므로 이번 SR의 결함이 아니다 — 향후 상한 재검토 시 근거로만 남긴다.
  3. **(low · 기존 코드, 후속 TODO)** `ApiKeyAuthIntegrationTest:380,392`도 같은 실제 엔드포인트를 리터럴 target으로 호출하면서 레이트리밋 행을 정리하지 않는다 — 권고1과 같은 형태의 잠복 이슈이나 SR-295 이전부터 존재했다.

### QA Gate — 2026-09-15 (round 2) — PASS
> round1 CONCERNS 재작업 지시 1건(medium/regression — 레이트리밋 행 정리 누락) 해소 확인 중심. 재작업 diff는 테스트 1개 클래스 한정.

- **재작업 지시 해소 판정 — 지시 1(medium) 해소 확인**. `MemberRegistrationCompletionFlowTest`에 `MemberSignupRateLimitDao` autowire + `cleanupRateLimitTarget` 필드 + `@AfterEach`의 `rateLimitDao.deleteRateLimit(cleanupRateLimitTarget, LocalDate.now())`가 들어갔고, 실제 레이트리밋 엔드포인트를 부르는 유일한 케이스(`wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds`)에만 필드를 세팅한다 — 사람 코멘트가 지정한 구현 방식(선례 `MemberSignupRateLimitConcurrencyTest:73`)과 정확히 일치. `@TestInstance` 미지정이라 JUnit 기본 PER_METHOD 생명주기로 필드가 테스트마다 초기화되므로 다른 테스트로 정리가 새지 않는다. 지시 2(low/보안 근거 기록)·지시 3(SR-299 이월)은 사람 코멘트대로 코드 변경 없음 — 지시 범위 준수.
- **Layer1 스펙: pass**. 재작업 diff에 운영코드 변경 0 — `memberSignupVerification.xml`의 `writeCode` `ON DUPLICATE KEY UPDATE`(`attempt_count = 0`, `consumed_at = NULL`, 기존 `verified_at = NULL`)와 DAO javadoc은 round1 판정 그대로이며 AC의 3컬럼 리셋을 충족한다. INF-MBR-001 요청/응답 계약 불변(`VerificationCodeResult(channel, target, expiresInSeconds)` — `MemberSignupService:143` 확인), INF-MBR-002 쪽 파일 diff 0바이트로 STEP 0/1/2 순서·조건식·트랜잭션 경계·오류계약 전부 불변. 적용 규칙 7건 위반 없음(추가 라인에 `System.out`/`printStackTrace`/`SELECT *`/DDL 변경 없음, 파일 260/126/45줄로 상한 이내).
- **Layer2 보안: pass**. 재작업은 테스트 전용이라 공격면 변화 없음. 사람 코멘트가 요구한 "코드 비노출" 재확인 유지 — `MemberSignupService.requestVerificationCode`는 코드값을 응답에 담지 않고 로그도 `target=`을 마스킹(앞 2자 외 `*`)해 찍는다. 따라서 STEP 1 통과에는 여전히 정확한 코드 소지가 필요하고, 새로 열린 `MBR-4092`+`login_url` 경로는 채널 소유자만 도달 가능 — round4 존재 오라클 방지 요건 유지.
- **Layer3 회귀: pass (round1 concerns → 해소)**. 게이트가 독립 실측했다.
  - **연속 3회 단독 실행 전부 그린**(19:32:37 / 19:32:46 / 19:32:55 — 서로 9초 간격, 쿨다운 60초 이내). 정리가 없었다면 2회차가 429로 깨졌어야 하므로 이 결과가 수정의 직접 증거다(사람이 요구한 완료 조건 "연속 2회"를 초과 충족).
  - **실행 후 잔여 행 0 확인**(DB 직접 조회): `MEMBER_SIGNUP_RATE_LIMITS` 0행, `MEMBER_SIGNUP_VERIFICATIONS` 0행, `MEMBERS`의 테스트 target(`attempt-cap%`/`already-member%`/`no-such%`) 0행 — 세 테이블 모두 누수 없음.
  - **전체 스위트 기준선 대비 악화 0**: surefire 리포트 독립 집계 512건 / 실패 1건 / 에러 0건. 유일 실패는 `OrderListEndToEndIntegrationTest#orderList_realDbEndToEnd_...`로, 실패 메시지가 주문번호 `20260815-0001`을 기대한 order 도메인 화면 렌더 픽스처 문제이고 member 경로와 무관하다. 기준선(`test_baseline.json`, git_head c29747e)이 이미 510건 중 동일 1건 실패를 기록 — 순증 +2, 회귀 0.
- 권고(CONCERNS시) — **이번 라운드 medium 이상 0건**. 아래는 low 2건으로, 게이트를 다시 세우지 않는다(라운드 규율: 이전 라운드에도 있던 코드에서 새로 발견 → low + 후속 TODO).
  1. **(low · 프로세스 — round1 QA의 오탐 정정, 이번 SR 코드 결함 아님)** round1 권고3이 지목한 `ApiKeyAuthIntegrationTest`의 레이트리밋 정리 누락은 **사실이 아니다**. 같은 클래스 `@AfterEach`(HEAD 기준 139~140행, 이번 SR에서 미수정)가 이미 `memberSignupRateLimitDao.deleteRateLimit("newbie@example.com", LocalDate.now())`와 `...("01099998888", ...)`를 호출하고 있고, 바로 위 주석이 "round4 — 레이트리밋 카운터가 전용 테이블로 옮겨져 그쪽도 함께 정리한다(정리하지 않으면 반복 실행마다 daily_count가 쌓여 결국 이 테스트가 200 대신 429를 받게 된다)"로 그 의도를 명시한다. dev가 지시대로 손대지 않고 이월한 판단 자체는 옳았으나, **이월처인 SR-299는 존재하지 않는 결함으로 접수된 상태**다(`docs/변경관리/SR-299/00_요구사항.md`가 "380·392행이 … 정리하지 않아"를 그대로 옮겨 적음). → **SR-299를 무효 종결(invalid)** 처리할 것. 그대로 두면 다음 SR에서 사람이 한 라운드를 빈 결함에 쓴다.
  2. **(low · 문서 정합, 후속 TODO)** `MemberRegistrationService` 클래스 javadoc 말미(87~95행 부근)가 아직 "재발송이 consumed_at/attempt_count를 리셋하지 않아 재가입 시도가 MBR-4092 대신 MBR-4091을 받는다 … 근본 해결은 FUNC-member-002 소유 파일을 건드려야 해서 이 FUNC 범위 밖이고, **후속 SR-295로 접수돼 있다**"고 서술한다 — SR-295가 구현된 지금은 사실과 다르다. INF-MBR-002 파일 무수정은 AC·계획대로라 이번에 고치지 않은 것이 맞고, dev는 DAO·테스트 클래스 javadoc은 정확히 갱신했다. 다만 STEP 1 동작을 확인하러 오는 다음 독자가 가장 먼저 읽을 곳이라 다음 member SR에서 한 문단만 갱신 권장.
  - (정보성, 수정 불필요) `deleteRateLimit(target, LocalDate.now())`는 테스트 본문과 `@AfterEach` 사이에 자정이 끼면 어제 `day_key` 행을 남긴다. `day_key`가 PK 일부라 다음 날 요청은 새 행을 만들고 `purgeOldRows` 배치가 지우므로 무해하며, 저장소 선례 4곳이 모두 같은 형태다.
  - (이월 유지) round1 권고2(잠금 리셋으로 target당 대입 예산이 코드 수명당 5회 → 재발송 5회/일 × 5회로 확대, 코드 공간 10^6) — 레이트리밋 정책은 확정 문답에서 범위 밖으로 명시됐으므로 근거 기록으로만 유지한다.

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/regression] 신규 통합테스트 wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds가 이 클래스에서 처음으로 레이트리밋이 걸린 실제 엔드포인트(POST /api/members/signup/verification-codes)를 리터럴 target으로 호출하는데, MemberRegistrationCompletionFlowTest의 @AfterEach(75~83행)가 MEMBER_SIGNUP_RATE_LIMITS 행을 지우지 않는다. (target, day_key) 행이 실행을 가로질러 남아 60초 내 재실행은 쿨다운 429, 같은 날 6번째 허용 실행은 일일상한 429가 되어 resend==200 단언이 깨진다(dev가 검증 중 실제로 429를 겪었으나 기존 관례로 오분류 — 이 클래스의 기존 테스트는 모두 writeCode를 직접 불러 레이트리밋을 우회하므로 기존 @AfterEach가 이 경우를 덮지 못한다). antipatterns.all.md SR-232 r2 재발이며, 구현 계획 105행이 '위험 없음'으로 단언한 지점이다. → 선례 MemberSignupRateLimitConcurrencyTest:71-74처럼 MemberSignupRateLimitDao를 autowire하고 정리 대상 target 필드를 추가한 뒤 @AfterEach에서 rateLimitDao.deleteRateLimit(target, LocalDate.now())를 호출한다.
2. [low/security] 잠금 리셋으로 target당 코드 대입 예산이 '코드 수명당 5회'에서 '재발송 5회/일 x 5회 = 약 25~30회/일'로 확대된다(코드 공간 10^6). 보상 통제는 변경되지 않은 일일 재발송 상한 5회다. 레이트리밋 정책은 확정 문답에서 범위 밖으로 명시돼 이번 SR의 결함은 아니다. → 이번 라운드 수정 대상 아님 — 향후 레이트리밋 상한 재검토 시 근거로 기록만 남긴다.
3. [low/regression] ApiKeyAuthIntegrationTest:380,392도 같은 레이트리밋 엔드포인트를 리터럴 target(newbie@example.com, 01099998888)으로 호출하면서 MEMBER_SIGNUP_RATE_LIMITS를 정리하지 않는다 — 권고1과 동일한 잠복 형태이나 SR-295 이전부터 존재한 기존 코드다. → 후속 TODO로 분리 — 이번 재작업에 묶지 않는다.

사람 코멘트: [결정 요약] 권고 1(medium)은 이번에 고친다 — 신규 HTTP 테스트 @AfterEach에 레이트리밋 행 정리. 권고 2는 범위 밖(레이트리밋 정책) 근거 기록으로 수용. 권고 3은 후속 SR로. [구현 방식] 저장소 선례(MemberSignupRateLimitConcurrencyTest:71-74)대로 @AfterEach에 rateLimitDao.deleteRateLimit(target, LocalDate.now()) 추가. 신규 테스트가 쓰는 모든 리터럴 target에 대해. [테스트·완료 조건] MemberRegistrationCompletionFlowTest를 연속 2회 실행해 둘 다 통과(60초 이내 재실행 포함). shop-api 전체 스위트 기준선 대비 악화 0. [후속 SR·이월] SR-299 — ApiKeyAuthIntegrationTest 380·392행 같은 형태 잠복 이슈(권고 3).

## test-agent 수용기준(AC) 검증 기록 (2026-09-15)

### AC → TC 매핑

**변경(TO-BE) AC 검증**

| AC | 테스트 케이스 | 상태 | 근거 |
|---|---|---|---|
| INF-MBR-001: 재발송 시 `attempt_count=0`, `consumed_at=NULL`, `verified_at=NULL` 리셋 | `MemberSignupVerificationDaoTest#writeCode_reissue_resetsAttemptCountAndConsumedAt` | ✅ PASS | DAO 레벨 단위 테스트 — 프로덕션 경로(MemberSignupCompletionDao 조합)로 상태 조작 후 writeCode 재호출, 3개 컬럼 값 직접 검증 |
| INF-MBR-001: 요청/응답 파라미터 변경 없음 | `MemberRegistrationCompletionFlowTest#wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds` (line 166-171) | ✅ PASS | HTTP 재발송 응답 구조 검증 — `{channel, target, expiresInSeconds}` 형식 유지 확인 |
| INF-MBR-002: STEP 1 조건식/로직 변경 없음 | `MemberRegistrationCompletionFlowTest` 기존 테스트군 (wrongCodeFiveTimesThenCorrect_locksOutAtFiveAndRejectsCorrectCode, signUpSucceeds_thenSameCodeReuse_isRejected) | ✅ PASS | 5회 오답 잠금 조건·순서·증가 로직 그대로 동작 재확인 |
| INF-MBR-002: 재발송 리셋 덕분에 MBR-4093 잠금 회복 | `MemberRegistrationCompletionFlowTest#wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds` (line 147-183) | ✅ PASS | 통합(HTTP) 테스트 — 5회 오답(MBR-4093) → 재발송(`POST /api/members/signup/verification-codes`) → attempt_count 리셋(0) 확인 → 새 코드로 가입 성공(201) 검증 |
| INF-MBR-002: STEP 1 성공 후 STEP 0에서 중복 검출(MBR-4092 + login_url) 반환 | `MemberRegistrationCompletionFlowTest#reSignUpWithAlreadyRegisteredEmail_afterResend_returns409EmailDuplicateWithLoginUrl` (SR-295 기댓값 업데이트) | ✅ PASS | 기존 회원 이메일이 재발송받은 새 코드로 재시도 → 409 MBR-4092 + login_url 응답 검증 |

**회귀(AS-IS) AC 검증**

| 회귀 범위 | 테스트 케이스 | 상태 | 근거 |
|---|---|---|---|
| 재발송 레이트리밋 (60초 쿨다운, 일일 5회 상한, quota 미소비) | `MemberRegistrationCompletionFlowTest#wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds` | ✅ PASS | cleanupRateLimitTarget 필드로 @AfterEach 정리 추가 — 60초 이내 연속 2회 실행도 통과(QA round2 검증) |
| 코드 만료 판정 (5분/300초 고정값) | 기준선 유지 + 기존 테스트 미변경 | ✅ PASS | 이번 SR은 코드 만료 로직 미변경 — `MemberSignupVerificationDaoTest#purgeExpiredCodes_deletesOnlyExpiredCodeRows` 기존 테스트 통과 |
| 5회 오답 잠금 로직 (attempt_count < 5 조건, incrementAttemptCount 동작) | `MemberRegistrationCompletionFlowTest#wrongCodeFiveTimesThenCorrect_locksOutAtFiveAndRejectsCorrectCode` | ✅ PASS | attempt_count==5 이후 6번째 요청도 증가 없이 유지 — 기존 로직 불변 |
| 가입 성공 시 코드 소비 (consumed_at 세팅, STEP 2) | `MemberRegistrationCompletionFlowTest#signUpSucceeds_thenSameCodeReuse_isRejected`, `#wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds` | ✅ PASS | 가입 성공 후 같은 코드 재사용 거부 로직 유지 |
| INF-MBR-001/002 응답 형식 불변 | 모든 통합 테스트 | ✅ PASS | HTTP 상태 코드, 오류 코드, 응답 봉투 형식 모두 기존대로 유지 |
| 존재 오라클 방지 (코드 검증이 존재 판정보다 먼저) | 기존 STEP 0/1 순서 유지 | ✅ PASS | 재발송 후 새 코드로 재시도 시에도 STEP 1 성공 후 STEP 0 호출 — 순서 불변 |

### 전체 테스트 실행 결과

**실행 환경**
- 실행일시: 2026-09-15 19:37:27
- 러너: `python {{PLUGIN_PATH}}/scripts/run_tests.py {{WS}}`
- 소스: shop-api (Maven Surefire) + shop-web (Jest)

**수량 집계**
- **shop-api**: 512건 / 통과 511 / 실패 1 (기준선 510/509/1 대비 순증 +2)
  - 신규 테스트 2개 추가: `MemberSignupVerificationDaoTest#writeCode_reissue_resetsAttemptCountAndConsumedAt`, `MemberRegistrationCompletionFlowTest#wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds`
  - 기존 테스트 1개 assertion 변경: `reSignUpWithAlreadyRegisteredEmail_afterResend_returns409EmailDuplicateWithLoginUrl`
  - 유일 실패: `OrderListEndToEndIntegrationTest#orderList_realDbEndToEnd_...` (order 도메인, SR-295 범위 밖 — 기준선에도 이미 기록된 기존 실패)

- **shop-web**: 32건 / 통과 32 / 실패 0 (기준선 32/32/0 동일)

**통과율**
- 기준선: 509/510 = 99.8%
- 현재: 511/512 = 99.8%
- 회귀: 0 (기준선 대비 악화 없음)

### 회귀 TC 대상 (변경 컨텍스트)

회귀 TC 전용 파일(03_TC.md)이 없어, AC 내 회귀 범위를 기존 테스트로 검증:
- `MemberSignupVerificationDaoTest`: 기존 3개 케이스 + 신규 1개
- `MemberRegistrationCompletionFlowTest`: 기존 4개 + 신규 1개 (기존 1개 assertion 변경)
- `MemberSignupCompletionDaoTest`, `MemberRegistrationConcurrencyTest`, `ApiKeyAuthIntegrationTest` 등: 기존 테스트 미변경 (재발송 경로 미포함 확인)

### 품질 판정

✅ **수용 기준 충족**: 신규 TC 2개 모두 AC 매핑 커버 (각 AC당 ≥1개 테스트)
✅ **회귀 기준 충족**: 기존 동작 6개 항목 모두 유지 확인
✅ **테스트 실행 완료**: 전체 스위트 통과율 99.8%, 순증 +2, 회귀 0
✅ **격리 준수**: SR-295 재작업 지시(QA round1 medium) — 레이트리밋 @AfterEach 정리 추가로 연속 재실행 통과 확인 (QA round2 PASS)

### 위험 평가

- 낮음: 신규 테스트가 AC 명시 조건을 충분히 검증하고, 기존 회귀 테스트도 통과. QA 2라운드 통과. 기준선 대비 악화 없음.
