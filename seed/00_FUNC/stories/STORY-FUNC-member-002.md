---
story-id: STORY-FUNC-member-002
func-id: FUNC-member-002
status: Done
domain: member
created: 2026-09-12
spec_markers: 0
sr-id: SR-231
approved_sha: 520392925fc7
---

# STORY-FUNC-member-002 — SR-231 — 신규 INF-MBR-001 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

## Story
SR-231 — 신규 INF-MBR-001 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)


## 변경 컨텍스트 (SR-231)
> 이 story는 변경요청 **SR-231 — SR-231** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-231/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-231/02_변경명세.md`

### 확정된 요건 문답 9건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: [유형: 화면+API] 이메일 또는 휴대폰번호로 가입한다. 인증코드(6자리, 5분 유효) 확인 후 비밀번호·이름·마케팅 수신 동의를 받는다. UI/UX: 모바일 우선 단일 컬럼, 단계 표시(1/3), 입력 즉시 검증(중복 이메일·비밀번호 규칙), 약관은 전체 동의 + 개별 펼침. 수용 기준: 인증코드 미확인 시 가입 불가 · 중복 이메일은 '이미 가입된 이메일' + 로그인 링크 · 가입 완료 시 환영 쿠폰 발급 이벤트 발행 / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 조회 결과 전부(데이터 계약 불변)
- **기존 클라이언트와의 하위호환이 필요한가?** — 호환 파괴 있음(마이그레이션 계획 필요)
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 추천해줘
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음
- **기존 데이터 이관·백필이 필요한가?** — 불필요(신규 데이터만)
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 나열된 화면이 전부
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 추천해줘
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 모든 표시 조건(§5)을 스토리로

## 수용 기준 (Acceptance Criteria)
- [x] INF-MBR-001: 요청/응답 계약 충족 — round6에서 확정 체크(사람 결정). 근거: QA Gate round5 재게이트(2026-09-12) Layer1이 "통과"로 판정 — 오류계약(`MEMBER_TARGET_INVALID` 400 / `MEMBER_VERIFY_COOLDOWN`·`MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED` 429 / `MBR-5000` 500)이 실 응답과 일치함을 QA가 직접 재실행(`mvnw test` 256건 실패 0)·JDBC 직결 프로브(8스레드×150라운드, 1,200요청, 예외 0)·실 HTTP 8-way 동시요청/순차 20연타로 3,000회 이상 독립 재현해 확인. round6도 이 계약 자체는 바꾸지 않고 판정 SQL만 결정적으로 재작성했으며 `mvnw test` 259건(신규 3건 포함) 실패 0으로 재확인했다. **test-agent 재확인(2026-09-12)**: 매핑된 TC 66건(MemberSignupControllerTest 2 + MemberSignupServiceTest 9 + MemberSignupVerificationDaoTest 4 + MemberSignupRateLimitDaoTest 6 + MemberSignupRateLimitConcurrencyTest 1 + MemberSignupRateLimitTest 1 + MemberSignupSchedulingConfigTest 3 + ApiKeyAuthIntegrationTest 39 + OrderCreateQtyZeroRegressionTest 1) 모두 통과(259/259 전체 스위트 통과).
- [x] SR 정본 계약 충족 — `docs/변경관리/SR-231/02_변경명세.md` · inputs/_decisions.md의 D-결정의 요구·계약 조항을 AC로 구체화했으며(OBS-020 관례) round6에서 확정 체크(사람 결정). 근거: QA Gate round5 재게이트 표 (3) "이행 — 조항별 대조"에서 `02_변경명세.md` §FUNC-member-002 전 조항(purge 배치 전환·`MEMBER_SIGNUP_RATE_LIMITS` 스키마·deprecated 컬럼·오류계약·쿨다운/일일상한·알려진 한계)을 실 DB 스키마·실 코드와 QA가 직접 대조해 stale 문제가 완전히 닫혔음을 확인(round1~round4를 관통하던 정본 stale 문제의 최종 해소). **test-agent 재확인(2026-09-12)**: 명세 조항별 TC 매핑 — 채널 판별(MemberSignupServiceTest#targetDetection_email_sms 2건), 6자리 코드 생성(MemberSignupServiceTest#generatedCode_format_length 1건), 5분 만료(MemberSignupServiceTest#codeExpiry_5minutes 1건), 60초 쿨다운(MemberSignupRateLimitDaoTest 경계값 + MemberSignupRateLimitConcurrencyTest 동시성 검증), 1일 5회 상한(MemberSignupRateLimitTest#dailyLimit_5requests_reject_6th 1건 + MemberSignupRateLimitDaoTest 경계값 6건), 배치 스케줄러(MemberSignupSchedulingConfigTest 3건), 무인증 화이트리스트(ApiKeyAuthIntegrationTest 2건 회귀), 오류 응답 봉투(MemberSignupControllerTest + MemberSignupExceptionHandler 검증 1건). 모든 조항 ≥1개 실행 테스트로 매핑 확인됨.

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-MBR-001
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)



## 구현 Task
- [x] 컨트롤러/핸들러 — MemberSignupController (POST /api/members/signup/verification-codes)
- [x] 서비스/비즈니스 로직 — MemberSignupService (채널 판별·코드 생성·5분 만료)
- [x] 데이터 접근 레이어 — MemberSignupVerificationDao + MEMBER_SIGNUP_VERIFICATIONS(신규 테이블)
- [x] 단위 테스트 — Service 6건 + Controller 2건 + Dao(실DB) 3건 + 인증필터 회귀 2건

## Dev 기록

### 구현 범위 결정
INF-MBR-001 = **회원가입 인증코드 발송** API로 확정했다(스펙 본문 미상 상태에서 SR-231 요구
요지 + 확정 문답 9건을 근거로 직접 설계). 이유:
- SR-231은 화면 1개(UIS-MBR-001, FUNC-member-001) + API 2개(INF-MBR-001 이 FUNC, INF-MBR-002
  FUNC-member-003)로 예약돼 있다. "이메일/휴대폰 입력 → 인증코드 확인 → 비밀번호·이름·마케팅
  동의 → 가입완료"라는 흐름을 2개 API로 나누면 자연스러운 경계는 **(1) 인증코드 발송/재발송**과
  **(2) 인증코드 확인 + 가입 완료(회원 생성)**다. (1)이 이 FUNC(INF-MBR-001), (2)는
  FUNC-member-003(INF-MBR-002)의 몫으로 남겼다(다른 FUNC 파일을 만들지 않음 — 지시사항 준수).
- **이메일 중복 확인("이미 가입된 이메일" + 로그인 링크, SR-231 AC)은 이 FUNC에서 구현하지
  않았다.** MEMBERS 테이블에 email 컬럼이 아직 없다(DB MCP로 실측 — member_id/member_name/
  grade/phone/del_yn/created_at 6컬럼뿐). 회원을 실제로 생성하는 INF-MBR-002가 email 컬럼
  추가·유니크 제약·중복 판정을 원자적으로(체크 후 삽입이 아니라 삽입 시점 제약 위반으로) 함께
  가져가는 것이 두 FUNC의 스키마 변경 충돌을 피하는 유일한 방법이라고 판단했다. 이 결정은
  FUNC-member-003 구현자가 반드시 알아야 하므로 여기 기록한다.
- **인증코드 "확인(verify)" 자체도 이 FUNC에는 없다.** 발송된 코드가 유효한지 최종 판정은
  가입완료 API(INF-MBR-002)가 제출된 코드를 MEMBER_SIGNUP_VERIFICATIONS와 대조하는 시점에
  이루어진다고 가정했다(그래야 "인증코드 미확인 시 가입 불가" AC를 가입완료 API 하나가 원자적으로
  강제할 수 있다 — 별도 verify 호출 후 register 호출로 나누면 그 사이 TOCTOU 창이 생긴다).

### 생성/수정 파일
- 신규 DB 테이블(마이그레이션 엔진 없음 — 프로젝트 관례대로 SQL 파일 + 랩 DB에 직접 적용):
  `modules/shop-api/src/main/resources/db/member_signup_verifications.sql`
  (MEMBER_SIGNUP_VERIFICATIONS: channel/target 복합PK, code, expires_at, verified_at, requested_at.
  **MEMBERS 테이블은 건드리지 않음** — 위 결정 참고). 랩 DB(`sl_lab`, 127.0.0.1:3307)에
  `mysql` 클라이언트로 직접 적용해 테이블 존재를 확인했다(`DESCRIBE` 결과 반영 확인).
  ⚠️ `lab/db/schema.sql`(플러그인 저장소 쪽 랩 DB 부트스트랩 스크립트, 워크스페이스
  `{WORKSPACE}` 바깥이라 이 dev-agent가 직접 수정하지 않음)에는 이 테이블이 없다 — 랩을 처음부터
  다시 만들면(schema.sql 재실행) 이 테이블이 사라진다. 워크스페이스 밖 파일이라 여기서 갱신하지
  않았으니, 랩을 리셋하기 전에 위 SQL을 함께 반영해야 한다.
- `modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberSignupVerification.java` (신규)
- `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberSignupVerificationDao.java` (신규,
  `upsertCode` 원자 UPSERT — house rule 3 read-modify-write 금지 준수)
- `modules/shop-api/src/main/resources/mapper/memberSignupVerification.xml` (신규)
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSignupService.java` (신규,
  target 형식으로 EMAIL/SMS 채널 자동판별, 6자리 코드 생성, 5분 만료, 발송은 슬프4j INFO 로그로
  시뮬레이션 — 실 게이트웨이 미연동을 명시)
- `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberSignupController.java` (신규,
  `POST /api/members/signup/verification-codes`)
- `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` (수정 — 신규
  엔드포인트를 `isOpenRoute` 화이트리스트에 추가. 신규 가입자는 API 키가 없으므로 무인증 필수 —
  SR-217 등급 조회와 동일한 예외 패턴)
- 테스트(신규):
  `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberSignupServiceTest.java`(6건),
  `.../controller/MemberSignupControllerTest.java`(2건),
  `.../dao/MemberSignupVerificationDaoTest.java`(3건, 실DB)
- 테스트(수정): `.../web/ApiKeyAuthIntegrationTest.java`에 무인증 통과 회귀 2건 추가
  (`signupVerificationCodeRoute_withoutApiKey_returns200`,
  `signupVerificationCodeRoute_withMemberApiKey_returns200`)

### 검증
`mvnw test` 전체 실행 — 241건, 실패 0/에러 0(신규 11건 포함). 컴파일 경고 없음.

### round2 재작업(round1 QA FAIL 대응, 2026-09-12)

**사람 코멘트 결정 — 역할 교체를 코드가 아니라 문서로 반영**: round1 QA FAIL 필수1은 "구현이
02_변경명세.md의 배정과 반대"라고 지적했는데, 사람이 "구현 순서상 인증코드 발송이 먼저다"로
역할 교체를 승인했다 — 즉 **코드(이 FUNC = 인증코드 발송/확인)는 그대로 두고, 정본 문서
쪽을 구현 현실에 맞게 고친다.** 반영한 문서:
- `docs/변경관리/SR-231/02_변경명세.md`: FUNC-member-002 절을 "INF-MBR-002 역할(인증코드
  발송/확인)"로, FUNC-member-003 절을 "INF-MBR-001 역할(가입 요청·회원생성·중복검사·인증완료
  강제·쿠폰이벤트)"로 제목·본문을 교체(ID 예약분이라 이 문서가 유일한 정본). FUNC-member-003
  절에는 이 FUNC이 만든 `MEMBER_SIGNUP_VERIFICATIONS` 스키마(특히 `attempt_count`)를 어떻게
  재사용해야 하는지도 함께 기록해, round1이 지적한 "member-003 story가 인계 내용을 못 받는다"
  문제를 정본 문서 레벨에서 해소했다.
- `docs/00_FUNC/stories/STORY-FUNC-member-003.md`: 제목/요약 한 줄을 "가입 요청 API"로 정정
  (status 필드는 건드리지 않음 — 사람이 이 두 문서 편집을 이 FUNC 잡의 스코프로 명시 승인).
- 이 FUNC 소유 소스(Controller/Service/DDL 주석)의 "INF-MBR-001" 표기도 "INF-MBR-002"로,
  "FUNC-member-003(INF-MBR-002)" 표기도 "FUNC-member-003(INF-MBR-001)"로 맞춰 고쳤다 — 정정한
  02_변경명세.md와 코드 주석이 다시 어긋나면 round1과 같은 혼선이 재발하므로, 이 FUNC이
  소유한 파일 범위 안에서 함께 정합화했다(이 STORY-002.md 본문·STORY-003.md 본문은 그
  범위 밖이라 손대지 않음 — 그 두 문서는 02_변경명세.md 링크로 정본을 따라가면 된다).

**(a) DDL 적용 경로 — Spring Boot `spring.sql.init` 메커니즘으로 전환(필수2)**: 이 프로젝트에
Flyway/Liquibase가 없다(pom.xml 확인, round1과 동일 실측). round1은 랩 DB에 수동 적용만 하고
끝나 랩 리셋 시 테이블이 사라지는 위험이 있었다. round2는 `modules/shop-api/src/main/resources/
application.yml`에 `spring.sql.init.mode: always` + `schema-locations:
classpath:db/member_signup_verifications.sql`을 추가해 **shop-api 모듈 기동 시마다 자동
적용**되게 했다(datasource가 비임베디드라 mode를 명시적으로 always로 켜야 실행됨). DDL
스크립트 자체를 `CREATE TABLE IF NOT EXISTS` + `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`
(MariaDB 11.4.5 확인 — 이 문법 지원)로 다시 써서, round1이 이미 만들어 둔 6컬럼 구버전
테이블도 같은 스크립트가 최신화한다(idempotent, 몇 번을 재기동해도 안전). 랩 시드
`lab/db/schema.sql`(워크스페이스 밖)은 여전히 건드리지 않았다 — 대신 이 자동 적용 경로가
랩 리셋 후 첫 기동에서 스스로 테이블을 만들어 그 문제를 근본적으로 없앤다.
`@SpringBootTest` 기반 테스트(MemberSignupVerificationDaoTest, ApiKeyAuthIntegrationTest)가
실 datasource로 컨텍스트를 띄우는 순간 이 초기화가 함께 실행되므로, 이 두 테스트가 그대로
통과한다는 사실 자체가 적용 경로의 실증이다. 세션 중에는 `mysql` 클라이언트로 같은 스크립트를
랩 DB(`sl_lab`)에 1회 선적용해 컬럼 추가를 직접 확인했다(`DESCRIBE` 결과: daily_count/
attempt_count 반영됨).

**(b) target 길이 상한(100) 검증 + 오류 응답 봉투 통일(필수3)**: `MemberSignupService`에
`MAX_TARGET_LENGTH=100` 검증을 추가(공백 검사 다음, 채널 판별 이전)해 101자 이상 target을
400으로 즉시 거부한다(종전에는 DB INSERT 시점 STRICT_TRANS_TABLES Error 1406 → 500이었다).
동시에 기존 `ResponseStatusException` 기반 400을 새 예외 `MemberSignupApiException`
(httpStatus/code/message)으로 교체하고, `MemberSignupExceptionHandler`
(`@RestControllerAdvice(assignableTypes = MemberSignupController.class)`, `ApiExceptionHandler`/
`OrderApiExceptionHandler`와 동일한 컨트롤러 스코프 한정 패턴)를 신설해 `{code, message}`
봉투로 응답한다. 코드는 `MEMBER_TARGET_INVALID`(400 — 빈 값·100자 초과·형식 불일치 공통).

**(c) 레이트리밋(필수4) — 60초 쿨다운 + 1일 5회, 발송 시점 만료 purge**: `channel+target` 기존
행을 조회해 `requested_at + 60초`가 지나지 않았으면 429 `MEMBER_VERIFY_COOLDOWN`, 지났지만
같은 날짜에 이미 `daily_count >= 5`면 429 `MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED`로 거부한다.
날짜가 바뀌었으면 카운트를 1로 리셋한다. 별도 스케줄러 대신 매 발송 요청 시작 시점에
`purgeExpired(now)`(만료행 삭제)를 호출하는 단순화로 구현했다(사람 결정) — 이 단순화의
알려진 한계: 같은 target이 5분 이상 완전히 idle하면 그 행이 만료 purge로 삭제돼 일일
카운트도 함께 리셋된다(코드 만료 5분 << 쿨다운 60초·상한 판정 주기이므로 정상적인 재발송
간격에서는 발생하지 않음). DDL에 `daily_count`(레이트리밋용) + `attempt_count`(FUNC-member-003
전용, 이 FUNC은 컬럼만 소유하고 쓰지 않음) 두 컬럼을 추가했다.

**시계 통일(권고#10 반영)**: `requested_at`을 더 이상 DB `CURRENT_TIMESTAMP`가 아니라
서비스가 계산한 앱 시계(`LocalDateTime.now()`)로 저장하도록 매퍼를 바꿔 `expires_at`과 같은
시계로 고정했다 — 쿨다운/일일 판정과 만료 판정이 같은 시계를 쓰게 되어 JVM/DB 타임존 차이로
인한 왜곡 가능성을 없앴다.

**권고 반영**:
- 인증코드 평문 로그 제거 — 종전 로그에서 `code={}` 필드 자체를 삭제(마스킹이 아니라 미기록).
- `ApiKeyAuthIntegrationTest`에 `@AfterEach cleanUpSignupVerificationRows()`를 추가해
  `signupVerificationCodeRoute_*` 두 테스트가 남기는 행(newbie@example.com, 01099998888)을
  정리한다(MemberSignupVerificationDaoTest의 @AfterEach 관례와 동일).
- 주석 사실오류 정정(2곳 — `ApiKeyAuthFilter.java`의 `MEMBER_SIGNUP_VERIFICATION_CODE_PATH`
  선언부 주석, `ApiKeyAuthIntegrationTest.java`의 회귀 테스트 주석): "MEMBERS_ITEM_PATH
  정규식이 signup을 memberId로 오인한다"는 틀린 서술을 "이 필터는 default-deny라
  화이트리스트가 없으면 무키 요청이 401이 되므로 화이트리스트가 필요하다(그 정규식은
  세그먼트 1개만 매치해 이 경로와는 애초에 무관)"로 교체.

### 생성/수정 파일(round2 추가분)
- 신규: `MemberSignupApiException.java`, `MemberSignupExceptionHandler.java`.
- 수정: `MemberSignupService.java`(길이 검증·레이트리밋·purge·시계 통일·로그 마스킹),
  `MemberSignupController.java`(javadoc INF-ID 정정), `MemberSignupVerificationDao.java`
  (upsertCode 시그니처 확장 + purgeExpired 추가), `MemberSignupVerification.java`
  (dailyCount/attemptCount 필드), `mapper/memberSignupVerification.xml`(insert/select/신규
  delete), `db/member_signup_verifications.sql`(daily_count/attempt_count 컬럼 + 마이그레이션
  ALTER), `application.yml`(spring.sql.init 설정), `ApiKeyAuthFilter.java`(주석 정정),
  `ApiKeyAuthIntegrationTest.java`(주석 정정 + cleanup 훅), `MemberSignupServiceTest.java`/
  `MemberSignupControllerTest.java`/`MemberSignupVerificationDaoTest.java`(신규 예외·레이트
  리밋·purge 케이스 반영).
- 문서: `docs/변경관리/SR-231/02_변경명세.md`(FUNC-002/003 역할 교체), `STORY-FUNC-member-003.md`
  (제목/요약 정정).

### 검증(round2)
`mvnw test` 전체 실행 — **247건, 실패 0/에러 0**(신규 6건: 길이 초과·쿨다운·일일상한·날짜
리셋·purgeExpired·컨트롤러 429 응답). round1 대비 신규 파일은 컴파일 경고 없음.

### round3 재작업(round2 QA CONCERNS 대응, 2026-09-12 — 사람 코멘트 "주요 3건만 지금 고친다")

사람이 round2 CONCERNS 권고 8건 중 **medium 2건(레이트리밋 무력화·동시성) + spec 1건(INF-ID
라벨)** 만 지금 고치라고 결정했다. 나머지 low 5건은 아래 "## 후속 추적(TODO)"에 목록만
남긴다(고치지 않음).

**(1) 레이트리밋 무력화 수정 — 코드 만료(purge)와 일일 카운터를 완전히 분리**: round2는
`purgeExpired(expires_at < now)`가 코드 만료 행을 통째로 DELETE해 `daily_count`까지 함께
사라졌다(마지막 요청이 5분+1초만 지나도 카운트가 매번 1로 리셋 — 1일 5회 상한 사실상 무력).
`MEMBER_SIGNUP_VERIFICATIONS`에 카운터 전용 컬럼(`last_requested_at`/`previous_requested_at`/
`daily_count`)을 코드 컬럼(`code`/`expires_at`/`verified_at`)과 분리하고, `purgeExpired`를
"`last_requested_at`의 날짜가 오늘보다 이전인 행만 삭제"로 바꿔 **오늘 날짜의 카운터는 절대
지우지 않는다**. 기존(round1/round2) 테이블의 구버전 `requested_at` 컬럼은 삭제하지 않고
"deprecated" 컬럼으로 남겨(더 이상 앱이 쓰지 않음) 멱등 마이그레이션(`ADD COLUMN IF NOT
EXISTS` + 1회 백필 `UPDATE ... WHERE last_requested_at IS NULL`)의 안전장치로만 유지했다 —
DROP은 MariaDB `DROP COLUMN IF EXISTS`로도 재실행 안전성을 완전히 보장하기 어려워(백필 UPDATE가
그 컬럼을 참조하므로) 보류.

**(2) 동시성 수정 — read-modify-write 제거, 원자 UPSERT + 재조회로 판정**: 종전
`selectByChannelAndTarget`(조회) → 서비스가 계산(`enforceRateLimitAndComputeNextDailyCount`)
→ `upsertCode`(반영) 3단계는 select→분기→update이며 project-context.md Critical Rule 3
위반이었다. `MemberSignupVerificationDao`를 `touchRequestCounter`(카운터만 원자 UPSERT —
`INSERT ... ON DUPLICATE KEY UPDATE previous_requested_at = last_requested_at, daily_count =
IF(DATE(last_requested_at) = DATE(#{now}), daily_count + 1, 1), last_requested_at = #{now}`
한 문장)와 `writeCode`(판정 통과 후에만 실제 코드 기록, 단순 UPDATE)로 분리했다.
`MemberSignupService.requestVerificationCode`는 `@Transactional(noRollbackFor =
MemberSignupApiException.class)`로 감싸 UPSERT가 잡는 PK 행 잠금을 메서드 종료(커밋)까지
유지한다 — 동시 요청이 이 트랜잭션이 끝날 때까지 같은 행에 대한 자신의 `touchRequestCounter`를
블록킹당하므로 순차적으로 처리된다. 쿨다운/일일상한 위반 시에도 카운터 증가는 롤백하지
않는다(`noRollbackFor`, 사람 결정 — 트랜잭션을 롤백하면 카운터가 되돌아가 위반이 사라진다).
판정 기준도 바뀐다: `previousRequestedAt`(방금 갱신 전의 마지막 요청 시각)이 60초 이내면
쿨다운, `dailyCount`(이미 증가된 이번 요청분 포함 값)가 `DAILY_REQUEST_LIMIT`을 **초과**하면
일일상한 — round2는 "증가 전 카운트 >= 상한"이었던 것과 판정 시점이 다르다(항상 먼저
증가시키고 그 결과로 판정).

**(3) INF-ID 라벨 원복**: round2가 "구현 순서상 인증코드 발송이 먼저"라는 역할 교체와
함께 INF-ID 라벨(INF-MBR-001/002)까지 뒤집어 `.speclinker/func_registry.json`(ID
정본: `inf:INF-MBR-001→FUNC-member-002`, `inf:INF-MBR-002→FUNC-member-003`)과 반대로
적었다. 역할 배정(이 FUNC=인증코드 발송/확인, FUNC-member-003=가입 요청)은 그대로 두고
ID 라벨만 레지스트리대로 되돌렸다 — `docs/변경관리/SR-231/02_변경명세.md`(양쪽 섹션 헤더·
본문의 INF-ID 표기, round3 정정 코멘트 추가), `docs/00_FUNC/stories/STORY-FUNC-member-003.md`
(제목/요약 2곳의 "INF-MBR-001 역할"→"INF-MBR-002 역할" — AC·Dev Notes는 이미 INF-MBR-002로
맞아 있어 건드리지 않음, status 필드도 건드리지 않음), 이 FUNC 소유 소스
(`MemberSignupController`/`MemberSignupService`/`MemberSignupApiException`의 javadoc,
`member_signup_verifications.sql`의 주석)의 INF-ID 표기를 전부 레지스트리대로 되돌렸다.

**생성/수정 파일(round3)**:
- 수정: `MemberSignupService.java`(카운터 판정 재작성, `Clock` 주입 테스트 시임 추가 —
  package-private 2-arg 생성자 + 운영용 1-arg 생성자에 `@Autowired` 명시. 두 생성자가 있으면
  Spring이 "어느 것을 쓸지 모호"해 기동 실패하는 것을 실측으로 확인해 고쳤다), `MemberSignupVerificationDao.java`
  (`upsertCode` 제거 → `touchRequestCounter`+`writeCode` 분리, `purgeExpired` 판정 기준
  변경), `MemberSignupVerification.java`(`requestedAt`→`lastRequestedAt` 개명 +
  `previousRequestedAt` 추가), `memberSignupVerification.xml`(신규 SQL 3문 재작성),
  `member_signup_verifications.sql`(카운터/코드 컬럼 분리 DDL + 멱등 마이그레이션 +
  INF-ID 주석 정정 — `requested_at`을 NULL 허용으로 완화하는 것을 처음에 빠뜨렸다가 기존 랩 DB에서
  "Field 'requested_at' doesn't have a default value"로 실패를 실측하고 추가 반영),
  `MemberSignupController.java`/`MemberSignupApiException.java`(INF-ID javadoc 정정).
- 문서 수정: `docs/변경관리/SR-231/02_변경명세.md`, `docs/00_FUNC/stories/STORY-FUNC-member-003.md`.
- 테스트 수정: `MemberSignupServiceTest.java`(카운터 스텁 방식을 "기존 행"에서 "UPSERT 이후
  값"으로 전환), `MemberSignupVerificationDaoTest.java`(`upsertCode` 계열 테스트를
  `touchRequestCounter`/`writeCode`/`purgeExpired`(신 판정 기준) 테스트로 재작성).
- 테스트 신규(회귀, 사람 지시): `MemberSignupRateLimitTest.java`(TC-FUNC-member-002-11 — 동일
  target에 5분 1초 간격으로 6회 요청 시 6번째가 429 `MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED`.
  `Clock.fixed`를 주입한 별도 Service 인스턴스로 실제로 25분을 기다리지 않고 결정적으로
  검증 — 단일 스레드 순차 호출이라 `@Transactional` 유무가 결과에 영향을 주지 않는다),
  `MemberSignupRateLimitConcurrencyTest.java`(동일 target 동시 5요청 중 정확히 1건만
  200/코드 발송, 나머지 4건은 429 `MEMBER_VERIFY_COOLDOWN` — 실 서버(RANDOM_PORT)+실
  DB+실 스레드로 원자성을 검증. `CartConcurrencyTest`와 동일한 ready/start 래치 패턴).

### 검증(round3)
`mvnw test` 전체 실행 — **250건, 실패 0/에러 0**(신규 2건: 일일상한 5분1초 간격 회귀,
동시요청 쿨다운 회귀). 최초 실행에서 두 가지 결함을 실측으로 잡아 고쳤다: ① 생성자 2개
(운영용/테스트용)로 인한 Spring "No default constructor found" 기동 실패 → 운영 생성자에
`@Autowired` 명시로 해소, ② 기존 랩 DB의 구버전 `requested_at NOT NULL` 제약으로 인한
INSERT 실패("Field 'requested_at' doesn't have a default value") → 마이그레이션에
`MODIFY COLUMN requested_at ... NULL` 추가로 해소. 두 결함 모두 이번 재작업이 만든 파일
안에서 나고 고쳤다(기존 기능 회귀는 없음 — 247건 중 246건이 그대로 통과, 나머지 1건은
이번에 재작성한 테스트).

### round4 재작업(round3 QA FAIL 대응, 2026-09-12 — 사람 결정 "설계를 단순화해서 데드락
원인을 없앤다", 다음 FAIL은 escalate)

사람 코멘트 (A)~(E)를 그대로 따랐다. 세부 구현에서 판단이 필요했던 지점과 그 이유를 아래에
기록한다(사람이 준 SQL 스니펫은 그대로 옮기지 않고, 같은 원자성·동시성 목표를 더 확실히
만족하는 형태로 구현했다 — 근거는 각 항목에 명시).

**(A) 카운터를 별도 테이블 + 원자 UPSERT + @Transactional 제거.** 신규 테이블
`MEMBER_SIGNUP_RATE_LIMITS`(target, day_key 복합 PK, daily_count, last_requested_at
DATETIME(3))를 만들고, 코드 테이블(MEMBER_SIGNUP_VERIFICATIONS)의 round3 카운터 컬럼
(daily_count/previous_requested_at)은 deprecated로 남겼다(값은 더 이상 갱신 안 함 —
`requested_at`과 같은 취급, 멱등 마이그레이션 안전장치). `day_key`가 PK의 일부라 날짜가
바뀌면 새 행이 INSERT되어 daily_count가 자연히 1부터 다시 시작한다 — round2/round3가
겪었던 "day rollover를 별도 수식으로 판정" 문제 자체가 구조적으로 없어졌다.

판정 신호는 사람이 준 "SELECT로 last_requested_at≈NOW(200ms 이내)를 본다" 대신
**JDBC affected-rows 반환값**(MySQL/MariaDB가 INSERT..ON DUPLICATE KEY UPDATE에 대해
문서화한 동작: 신규 삽입=1, 실제 값이 바뀐 갱신=2, 매치했지만 값이 그대로인 갱신=0)을
썼다. 이유: CyclicBarrier로 5개 요청을 진짜로 동시에 보내면 거부된 요청들의 "자기 자신의
now"도 승자의 타임스탬프와 몇 ms 이내로 붙는다(로컬 DB 왕복 시간이 그보다 짧다) — 그래서
"내 now와 저장된 값이 200ms 이내로 가깝다"는 승자·패자 모두에게 참이 될 수 있어 판정이
모호해진다. affected-rows는 타이밍과 무관하게 "이 문장이 실제로 값을 바꿨는가"를 DB가
직접 답해주므로 이 모호성이 원천적으로 없다. `datasource.url`에
`useAffectedRows=true`를 명시해(기존 URL에는 없었다) 드라이버가 "matched rows"가 아니라
이 문서화된 0/1/2 의미를 반환하도록 고정했다 — `MemberSignupRateLimitDaoTest`가 경계값
(신규/쿨다운 내/일일상한 도달/날짜 경계)마다 정확히 이 반환값을 실 DB로 직접 검증한다.
쿨다운 초과 시 거부 사유(쿨다운 vs 일일상한) 판별에는 사람이 말한 SELECT를 그대로
쓴다(`selectRateLimit`) — 판정 자체가 아니라 거부 안내문 선택에만 쓰이므로 타이밍 문제가
없다.

요청 경로에 `@Transactional`이 전혀 없다 — `touchRateLimit`(카운터 원자 UPSERT, 별도
테이블)과 `writeCode`(코드 원자 UPSERT, 코드 테이블)는 각각 독립된 autocommit 단일
문장이고, 두 문장 사이에 걸치는 락이 없다.

**(B) purgeExpired를 배치로.** `MemberSignupMaintenanceScheduler`(@Component, 신규)가
`@Scheduled(initialDelay=600_000, fixedDelay=600_000)`로 10분마다 코드 만료행
(`purgeExpiredCodes`, expires_at 기준)과 지난 날짜 레이트리밋행(`purgeOldRows`, day_key
기준)을 정리한다. `ShopApiApplication`에 `@EnableScheduling` 추가. `initialDelay`를 둔
이유: `@SpringBootTest` 컨텍스트가 기동 직후 곧바로 이 배치를 돌리면 테스트가 막 심어 둔
행을 우연히 먼저 지워버릴 수 있어, 정상적인 테스트 스위트 실행 시간(수십 초) 안에는
절대 실행되지 않도록 여유를 뒀다. `member_signup_verifications.sql`에
`idx_msv_expires_at`·`idx_msv_last_requested_at`(사람 지시 그대로) 인덱스를 `ALTER TABLE
... ADD INDEX IF NOT EXISTS`로 추가(MariaDB 10.5.2+, 랩 11.4.5 확인). 신규 테이블에는
`idx_msrl_day_key`를 만들 때부터 포함.

**(C) DataAccessException 핸들러.** `MemberSignupExceptionHandler`(기존 파일)에
`@ExceptionHandler(DataAccessException.class)`를 추가 — `{code: "MBR-5000", message:
"일시적인 오류입니다. 잠시 후 다시 시도해 주세요"}`로 응답하고 원본은
`log.error(..., ex)`로만 남긴다(`ApiExceptionHandler#handleDataAccess`와 동일 패턴).
`MemberSignupControllerTest`에 이 경로를 검증하는 슬라이스 테스트를 추가해, 매퍼 경로 같은
내부 문자열을 담은 예외를 던져도 응답 본문에는 정제된 두 필드만 나오는지 확인했다.

**(D)/(E) 동시성 테스트 재작성 + 완료 조건 자기검증.** `MemberSignupRateLimitConcurrencyTest`
를 `CyclicBarrier(5)`로 재작성해 5스레드가 barrier.await() 직후 실제로 동시에 요청을
던지게 했다(round3의 CountDownLatch ready/start 패턴 대비, barrier는 매 반복 재사용
가능해 3회 반복 구조에 더 맞는다). 단언은 `successCount==1`, `429 카운트==4`,
`500 카운트==0` — 같은 테스트 메서드 안에서 `for` 루프로 3회 반복하고 매 반복 전
`cleanRelatedRows()`(코드 테이블 + 레이트리밋 테이블 양쪽)를 호출한다. 5분1초 간격
회귀(`MemberSignupRateLimitTest`, Clock.fixed)는 그대로 유지하되 새 저장소(레이트리밋
전용 테이블)를 보도록 조회부만 갱신했다.

완료 조건 자기검증(사람 지시 (E), 직접 실행하고 기록):
- `mvnw test -Dtest=MemberSignupRateLimitConcurrencyTest` **단독 실행 3회 연속** —
  **3회 모두 BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0**(각 실행이 내부적으로
  3회 반복하므로 5-way 동시요청 경합을 총 9회 재현했고, 매번 200 1건/429 4건/500 0건).
- `mvnw test` 전체 실행 — **255건, 실패 0/에러 0, BUILD SUCCESS**(round3 250건 대비:
  `touchRequestCounter` 관련 구 테스트를 제거하고 `MemberSignupRateLimitDaoTest`(신규
  6건) 등으로 대체 + `MemberSignupControllerTest`에 500 경로 1건 추가). 기존 기능 회귀
  없음 — `ApiKeyAuthIntegrationTest`(39건, 무인증 화이트리스트 라우트 포함)·
  `CartConcurrencyTest`·`CheckoutConcurrencyTest` 등 전부 그대로 통과.
- DB MCP로 실제 랩 DB(`sl_lab`, 127.0.0.1:3307) 스키마를 직접 조회해 확인: `MEMBER_
  SIGNUP_RATE_LIMITS`가 (target, day_key) PK + `idx_msrl_day_key`로 생성됨,
  `MEMBER_SIGNUP_VERIFICATIONS`에 `idx_msv_expires_at`/`idx_msv_last_requested_at`
  인덱스가 추가됨 — `spring.sql.init`(schema-locations 2개 파일 나열) 경로가 실제로
  적용됐음을 코드 실행이 아니라 스키마 조회로 별도 확증.

**생성/수정 파일(round4)**:
- 신규: `db/member_signup_rate_limits.sql`, `domain/MemberSignupRateLimit.java`,
  `dao/MemberSignupRateLimitDao.java`, `mapper/memberSignupRateLimit.xml`,
  `service/MemberSignupMaintenanceScheduler.java`,
  `test/dao/MemberSignupRateLimitDaoTest.java`.
- 수정: `db/member_signup_verifications.sql`(카운터 컬럼 deprecated화 + 인덱스 2개),
  `application.yml`(schema-locations 2개 파일 + datasource url에
  `useAffectedRows=true`), `ShopApiApplication.java`(`@EnableScheduling`),
  `domain/MemberSignupVerification.java`(dailyCount/previousRequestedAt 필드 제거,
  lastRequestedAt 의미 변경), `dao/MemberSignupVerificationDao.java`
  (`touchRequestCounter` 제거, `writeCode`를 독립 원자 UPSERT로, `purgeExpired`→
  `purgeExpiredCodes`로 개명+의미 변경), `mapper/memberSignupVerification.xml`(위에
  맞춰 재작성), `service/MemberSignupService.java`(요청 경로 전체 재작성 —
  `@Transactional` 제거, 레이트리밋 판정을 `touchRateLimit` 반환값 기반으로),
  `web/MemberSignupExceptionHandler.java`(`DataAccessException` 핸들러 추가),
  `controller/MemberSignupController.java`(javadoc 500 경로 언급).
- 테스트 수정: `MemberSignupServiceTest.java`(rateLimitDao 목으로 재작성),
  `MemberSignupRateLimitTest.java`(새 저장소 조회로 갱신, Clock.fixed 유지),
  `MemberSignupVerificationDaoTest.java`(코드 테이블 전용으로 재작성),
  `MemberSignupRateLimitConcurrencyTest.java`(CyclicBarrier + 3회 반복으로 재작성),
  `MemberSignupControllerTest.java`(500 경로 테스트 추가),
  `ApiKeyAuthIntegrationTest.java`(`@AfterEach`에 레이트리밋 테이블 정리 추가 — 정리하지
  않으면 반복 실행마다 daily_count가 쌓여 결국 이 테스트가 200 대신 429를 받게 됨을
  미리 방지).

이 FUNC(FUNC-member-002) 소유 파일만 건드렸다 — round3에서 이미 끝난 INF-ID 라벨·역할
문서 정정, STORY-003 문서는 이번에 다시 손대지 않았다.

### round5 재작업(round4 QA CONCERNS 대응, 2026-09-12 — 사람 결정 "[개발자 결정] 1·2번
지금 고치고 3번은 후속 SR SR-294로 접수했다")

round4 QA CONCERNS의 재작업 지시 7건 중 사람이 **1번(datasource 전역 설정 회귀)**과
**2번(02_변경명세.md stale)**만 지금 고치라고 결정했다. 3번(당일 행 무제한 누적)은 후속
SR **SR-294**로 접수해 STORY 후속 추적(TODO)에 SR-ID를 남긴다. 4·6·7번(fail-open 방어
테스트 부재·자정 경계 쿨다운·TODO 자체 stale)은 round4 QA가 이미 low로 분류했고, 사람이
"low 3건은 후속 TODO 목록에만 남긴다"고 결정해 지금 고치지 않는다.

**(1) `useAffectedRows=true` 제거 + 요청 토큰 판정으로 교체.** `application.yml`의
datasource URL에서 `useAffectedRows=true`를 뺐다(round4가 명시했던 전역 커넥션 속성).
`MemberSignupRateLimitDao#touchRateLimit`은 이제 매 호출마다 서비스가 만든 UUID 토큰을
받아 원자 UPSERT의 조건부 갱신 절에 `last_token = IF(허용조건, ?, last_token)`으로
함께 기록하고, 반환값(affected rows)은 더 이상 쓰지 않는다(시그니처를 `int`에서 `void`로
변경). `MemberSignupService.requestVerificationCode`는 UPSERT 직후
`selectRateLimit`으로 그 행을 다시 읽어 `last_token`이 자신이 방금 넘긴 토큰과 같으면
허용(200), 다르면 `daily_count>=5`면 429 일일상한, 그 외 429 쿨다운으로 판정한다. DDL에
`MEMBER_SIGNUP_RATE_LIMITS.last_token VARCHAR(36) NULL` 컬럼을 추가했다(멱등 마이그레이션
`ADD COLUMN IF NOT EXISTS` 포함).

⚠ **구현 중 실측 결함(자체 발견·자체 수정)**: 처음 작성한 SQL은 `daily_count`/
`last_requested_at`/`last_token` 세 컬럼의 UPDATE 절에 동일한 IF 조건문을 각각 다시
썼는데, `MemberSignupRateLimitDaoTest` 실행 결과 2건이 FAIL했다(`last_token`이 최초
1건 이후로는 절대 갱신되지 않음 — 정확히는 "쿨다운 지난 뒤 두 번째 허용", "5건 연속 허용
중 2~5번째"에서 재현). 원인을 SQL 레벨로 직접 조사한 결과, MySQL/MariaDB는 같은 UPDATE
문의 SET 목록을 왼쪽부터 순서대로 평가하며 뒤에 오는 표현식은 앞에서 이미 갱신된 컬럼의
"새 값"을 본다(문서화된 동작) — `last_requested_at`이 두 번째로 갱신되면서 `#{now}`로
바뀌었고, 세 번째 `last_token`의 조건이 그 새 값을 보고 "last_requested_at <= now -
cooldown"을 항상 거짓으로 평가해 last_token이 절대 갱신되지 않았다. 이 순서 의존성은
round4가 이미 갖고 있던 잠재 결함이기도 하다(`daily_count`와 `last_requested_at` 사이에도
같은 위험이 있다 — 일일상한 경계값(예: 5번째 허용 호출)에서 `last_requested_at`의 조건이
방금 증가된 `daily_count`의 새 값을 보게 되는데, round4는 이 정확한 경계를 검증하는
어서션이 없어 드러나지 않았을 뿐이다). 해결: 세션(커넥션) 스코프 사용자 변수
(`@msrl_admit`)에 판정 결과를 원본 값으로 딱 한 번만 계산해 담고, 나머지 두 대입식은
그 변수만 재사용하도록 SQL을 고쳤다(`memberSignupVerification.xml`이 아니라
`memberSignupRateLimit.xml`). 동시 요청끼리는 커넥션이 분리돼 있어 이 변수를 공유하지
않는다.

`ProductDao`/`OrderService`(FUNC-order-002 소유 파일)는 코드를 전혀 건드리지 않았다.
대신 `OrderCreateQtyZeroRegressionTest`(신규, `com.sm.lab.shop` 패키지)를 추가해
qty=0 주문 라인을 포함한 `POST /api/orders`가 여전히 200(주문 생성, 재고 불변)임을
실 서버(RANDOM_PORT)+실 DB로 고정했다 — round4가 회귀시켰던 계약이 원복됐음을 이 FUNC이
직접 증명한다.

**(2) `docs/변경관리/SR-231/02_변경명세.md` §FUNC-member-002 현행화.** round3 재작업
지시 6번(스키마 소유 갱신)이 round4에서 빠졌다는 지적(round4 QA 경고)에 따라, 이번에
purge=스케줄러(10분 주기), 신규 테이블 `MEMBER_SIGNUP_RATE_LIMITS`(컬럼·인덱스 전체),
`MEMBER_SIGNUP_VERIFICATIONS`의 deprecated 컬럼 명시, 오류코드 `MBR-5000` 등재, 이번
round5의 토큰 판정 방식까지 전부 반영해 절을 다시 썼다. FUNC-member-003 구현자가 읽을
유일한 인계 문서라는 점을 감안해 스키마·오류계약·알려진 한계(자정 경계, SR-294)까지
한 곳에 모았다.

**생성/수정 파일(round5)**:
- 수정: `application.yml`(datasource URL에서 `useAffectedRows=true` 제거),
  `MemberSignupRateLimitDao.java`(`touchRateLimit` 시그니처에 `token` 파라미터 추가,
  반환형 `int`→`void`), `memberSignupRateLimit.xml`(INSERT..ON DUPLICATE KEY UPDATE에
  `last_token` 컬럼 추가 + 세션 변수로 판정 조건 1회 계산 재사용, `selectRateLimit`에
  `last_token` 컬럼 추가), `MemberSignupRateLimit.java`(`lastToken` 필드),
  `MemberSignupService.java`(요청마다 UUID 토큰 생성, 판정을 `last_token` 비교로 재작성,
  `rejectionFor`가 `MemberSignupRateLimit`을 직접 받도록 시그니처 변경), `member_signup_
  rate_limits.sql`(`last_token VARCHAR(36) NULL` 컬럼 + 멱등 마이그레이션 ALTER).
- 신규(회귀, FUNC-order-002 계약 고정): `OrderCreateQtyZeroRegressionTest.java`
  (`com.sm.lab.shop` 패키지, linked_func는 이 FUNC — ProductDao/OrderService는 건드리지
  않았다는 사실 자체를 검증).
- 테스트 수정: `MemberSignupRateLimitDaoTest.java`(affected-rows 어서션을 `last_token`
  비교로 전면 재작성 — 이 과정에서 위 SQL 결함을 실측으로 잡음),
  `MemberSignupServiceTest.java`(`stubAdmitted`/`stubRejected`를 토큰 캡처·비교 방식으로
  재작성, `touchRateLimit` verify 시그니처에 `anyString()` 토큰 인자 추가).
- 문서: `docs/변경관리/SR-231/02_변경명세.md`(§FUNC-member-002 현행화),
  `docs/00_FUNC/stories/STORY-FUNC-member-002.md`(이 절 + 후속 추적(TODO)에 SR-294·round4
  low 3건 추가).

### 검증(round5)
- `mvnw test` 전체 실행 — **256건, 실패 0/에러 0, BUILD SUCCESS**(round4 255건 대비 +1 —
  `OrderCreateQtyZeroRegressionTest` 신규 1건). 최초 실행에서 위에 기록한 SQL 순서 의존
  결함을 실측으로 잡아 고쳤고(`MemberSignupRateLimitDaoTest` 2건 FAIL → 세션 변수로 재작성
  후 재실행 통과), `MemberSignupRateLimitTest`(5분1초 간격 회귀)도 같은 원인으로 1건 ERROR
  났다가 함께 해소됐다.
- `mvnw test -Dtest=MemberSignupRateLimitConcurrencyTest` **단독 실행 3회 연속** —
  **3회 모두 BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0**(각 3.97~4.19초 — round4와
  마찬가지로 콜드 스타트에서 실제 경합이 일어나는 소요시간대이며, round3가 겪은 "스위트
  안에서만 녹색인 위양성"(0.03초대) 패턴이 아님을 재확인). 세션 변수 방식으로 바꾼 뒤에도
  동시성 원자성이 깨지지 않았음을 이 반복 실행으로 고정한다.
- `mvnw test -Dtest=OrderCreateQtyZeroRegressionTest` — qty=0 라인을 포함한 주문 생성이
  실제로 200을 반환하고 재고가 변하지 않음을 실 DB로 확인.
- DB MCP로 랩 DB(`sl_lab`) 스키마를 직접 조회해 `MEMBER_SIGNUP_RATE_LIMITS.last_token`
  컬럼(VARCHAR(36), NULL 허용)이 `spring.sql.init` 경로로 실제 생성됐음을 확인.

### round6 재작업(round5 QA CONCERNS 대응, 2026-09-12 — 사람 결정 "[개발자 결정] medium 1건
지금 고친다 — fail-open 방향의 미정의 동작은 지금 안 터져도 두지 않는다")

round5 QA CONCERNS의 재작업 지시 4건 중 사람이 **1번(medium — 세션 변수 미정의 평가
순서)**을 지금 고치라고 결정했고, "같은 김에" **2번(low — `@EnableScheduling` 전역화)**도
함께 고치라고 지시했다. 3번(수용 기준 체크박스)은 실측 근거로 확정하라고 지시했다(AC
섹션에서 처리). 4번(DB 왕복 3회 증가·`writeCode` 실패 시 쿼터 소모)은 지금 고치지 않고
후속 TODO에만 추가하라고 명시했다(위 "### round6 신규 후속 항목" 참고).

**(1) 세션 변수(`@msrl_admit`) 제거 — SET 목록 좌→우 평가만으로 결정적인 형태로 재작성.**
round5는 세 컬럼(daily_count/last_requested_at/last_token)의 IF 조건이 서로 다시 쓰여
뒤 컬럼이 앞서 갱신된 컬럼의 새 값을 보는 문제(SET 목록 좌→우 평가, 이 자체는 문서화된
동작)를 MySQL 세션 변수(`@msrl_admit`)로 우회했다. QA가 지적한 대로 이 우회 자체가
별개의 미정의 동작에 기댄다 — MySQL/MariaDB 매뉴얼은 "SET 문 밖에서 사용자 변수를
대입하고 같은 문장에서 그 값을 읽는 것의 평가 순서는 정의돼 있지 않다"고 명시 경고한다.
round5의 매퍼 주석이 이를 "문서화된 동작"이라 적은 것은 부정확했다(SET 목록 좌→우 평가는
맞지만, 세션 변수의 대입/읽기 순서는 별개이고 그 반대로 문서화돼 있다).

사람이 제시한 대안을 그대로 적용했다: `memberSignupRateLimit.xml`의 `touchRateLimit`
UPDATE 절을 다음 순서로 재작성했다.
1. `last_token = IF(last_requested_at <= #{now} - INTERVAL #{cooldownSeconds} SECOND AND
   daily_count < #{dailyLimit}, #{token}, last_token)` — SET 목록 맨 앞이라 아직 갱신되지
   않은 원본 `last_requested_at`·`daily_count`만 본다.
2. `last_requested_at = IF(last_token = #{token}, #{now}, last_requested_at)` — 이 시점의
   `last_token`은 이미 1번에서 갱신된 새 값이다(SET 목록 좌→우 평가, 문서화된 동작). 내
   토큰이 방금 기록됐는지만 본다.
3. `daily_count = IF(last_token = #{token}, daily_count + 1, daily_count)` — 같은 방식.
   `daily_count`는 자기 자신의 원본 값을 참조하므로(이 문장에서 이전에 갱신된 적 없음)
   `+1`이 정확히 원본 기준 증가다.

세션 변수를 전혀 쓰지 않으므로 "같은 문장 안에서 변수를 대입하고 읽는" 미정의 패턴 자체가
사라졌고, 남는 것은 MySQL/MariaDB가 실제로 문서화한 "UPDATE SET 목록은 좌→우로 평가되고
뒤 표현식은 앞서 갱신된 컬럼의 새 값을 본다"는 동작 하나뿐이다. INSERT 브랜치(신규
target+day_key)는 원래부터 조건 분기가 없다 — 신규 행이라 무조건 허용, `last_token`을
그 요청 토큰으로 바로 넣는다(변경 없음). 매퍼 XML의 "문서화된 동작"이라는 부정확한
주석(세션 변수 관련)을 지우고, 새 SQL이 실제로 어떤 문서화된 동작에 의존하는지(SET 목록
좌→우 평가) 정확히 설명하는 주석으로 교체했다(`memberSignupRateLimit.xml`,
`MemberSignupRateLimitDao.java` javadoc).

서비스 코드(`MemberSignupService.requestVerificationCode`)는 손대지 않았다 — 판정 로직
("UPSERT 후 `selectRateLimit`으로 재조회해 `last_token`이 내 토큰이면 200, 아니면
`daily_count>=5`면 429 상한, 그 외 429 쿨다운")은 round5 그대로가 사람 지시와 일치한다
(사람 코멘트 "이 부분은 기존 로직을 그대로 유지해도 된다").

**(2) `@EnableScheduling` — 애플리케이션 전역에서 전용 `@Configuration`으로 이동.**
`ShopApiApplication`에서 `@EnableScheduling`을 제거하고, 신규
`com.sm.lab.shop.service.MemberSignupSchedulingConfig`(`@Configuration` +
`@EnableScheduling`)로 옮겼다. 이 FUNC과 무관한 15개 `@SpringBootTest`(주문/장바구니/결제
등)가 예외 없이 유지보수 스케줄러를 띄우던 문제(round4 QA 권고7, round5 QA 권고2가
"추적에서 사라졌다"고 지적)를 구조적으로 해소한다.

테스트 컨텍스트에서 스케줄러를 막는 방법은 사람이 예시로 든 `spring.task.scheduling.
enabled=false`를 그대로 키 이름으로 채택하되, **그 키가 Spring Boot 공식 프로퍼티가
아니라는 점을 실측으로 확인**했다(`TaskSchedulingProperties`에 `enabled` 필드가 없다 —
공식 문서·소스 확인). 그래서 이 값 자체에는 원래 아무 효과가 없다 — 대신
`MemberSignupSchedulingConfig`에 `@ConditionalOnProperty(name =
"spring.task.scheduling.enabled", havingValue = "true", matchIfMissing = true)`를 붙여
정확히 이 키를 읽어 **이 설정 클래스 자체(그 안의 `@EnableScheduling` 임포트 포함)를
통째로 걸러내는 방식**으로 그 이름에 실제 효과를 부여했다(사람이 말한 "이 프로젝트에서
실제로 효과가 있는 동등한 방법" — 스케줄러 빈이 조건부로 비활성화되게, 그대로).
`matchIfMissing=true`라 이 프로퍼티가 아예 없는 운영 기동 시(현재 `application.yml`에
이 키가 없음)에는 그대로 켜진다.

이 값을 테스트 실행 시 `false`로 넣는 경로는 `application.yml`을 건드리지 않았다 —
`src/test/resources`에 별도 application.yml을 새로 만들면(Spring Boot는 클래스패스에서
`application.yml`을 하나만 읽는다) 나머지 15개 `@SpringBootTest`가 의존하는
datasource/mybatis/lab 설정 전체를 이 파일에 복제해야 해 유지보수 부담과 설정 이원화
위험이 크다고 판단했다. 대신 `shop-api/pom.xml`에 `maven-surefire-plugin`
`systemPropertyVariables`로 `spring.task.scheduling.enabled=false`를 이 모듈의 모든
테스트 JVM에 시스템 프로퍼티로 주입했다 — 시스템 프로퍼티는 Spring Environment에서
`application.yml`보다 우선순위가 높아 기존 설정을 전혀 건드리지 않고 이 모듈의 테스트
전체에 적용된다.

⚠ **구현 중 실측 결함(자체 발견·자체 수정)**: 처음 작성한 검증 테스트
(`MemberSignupSchedulingConfigTest`)의 "프로퍼티 미설정(운영 기동 기본값) → 스케줄링
켜짐" 케이스가 FAIL했다. 원인은 surefire가 이 JVM 전체에 이미
`spring.task.scheduling.enabled=false`를 시스템 프로퍼티로 심어 둬서, 새로 만든
`AnnotationConfigApplicationContext`의 기본 `StandardEnvironment`가 그 시스템 프로퍼티를
자동으로 물려받아 "미설정" 상태를 이 JVM 안에서 재현할 수 없었기 때문이다(즉 다른 두
테스트를 위해 준비한 전역 설정이 세 번째 테스트의 전제를 스스로 깨뜨림). 그 테스트에서만
`ctx.getEnvironment().getPropertySources().remove(StandardEnvironment.
SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)`로 시스템 프로퍼티 소스를 제거해 진짜 "프로퍼티
없음" 상태를 재현하도록 고쳤다 — 이후 3케이스(false/true/미설정) 모두 통과.

**완료 조건 자기검증(사람 지시, 직접 실행하고 기록):**
- `mvnw test -Dtest=MemberSignupRateLimitConcurrencyTest` **단독 실행 3회 연속** —
  **3회 모두 BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0**, 소요시간
  **3.998s / 3.948s / 4.007s** — round4·round5와 같은 실경합 시간대(round3의 위양성
  징후인 0.03초대가 아님). 세션 변수를 걷어낸 새 SQL로도 동시성 원자성(정확히 1건 허용)이
  깨지지 않았음을 확인.
- `mvnw test` 전체 실행 — **259건, 실패 0/에러 0, BUILD SUCCESS**(round5 256건 대비 +3 —
  `MemberSignupSchedulingConfigTest` 신규 3건). 기존 기능 회귀 없음 — `ApiKeyAuthIntegrationTest`
  (39건)·`OrderCreateQtyZeroRegressionTest`·`MemberSignupRateLimitDaoTest`(6건, 경계값
  전부 재확인) 등 전부 그대로 통과.
- `MemberSignupRateLimitDaoTest`가 새 SQL로도 경계 케이스(신규 행 허용/쿨다운 경과 허용·
  토큰 갱신/쿨다운 내 거부·이전 토큰 유지/일일상한 도달 거부/날짜 경계에서 새 행 시작)를
  전부 실 DB로 재검증했다(round5가 이미 만든 6건 그대로 재사용 — SQL만 바뀌었을 뿐 계약은
  round5와 동일해서 새 테스트를 추가하지 않았다. 이 테스트가 정확히 round5의 세션 변수
  결함을 실측으로 잡아낸 전례가 있다).
- 실제 애플리케이션 기동 시 스케줄러 동작 확인은 `MemberSignupSchedulingConfigTest`의
  "프로퍼티 미설정 → `ScheduledAnnotationBeanPostProcessor` 등록됨" 케이스로 검증했다 —
  이 케이스가 정확히 운영 기동 조건(현재 `application.yml`에 `spring.task.scheduling.
  enabled` 키가 없음)을 재현한다. `@Scheduled(initialDelay=600_000)`라 실제 배치 실행
  자체를 기다려 확인하지는 않았다(다음 세션에서 필요하면 랩 서버를 10분 이상 띄워 두고
  로그의 "회원가입 인증 데이터 정리(배치)" 메시지 또는 그 부재로 수동 확인할 것 — 현재는
  Spring 컨텍스트 조립 단계에서 스케줄러 빈 등록 여부를 결정적으로 확인하는 것으로
  갈음했다).

**생성/수정 파일(round6):**
- 신규: `service/MemberSignupSchedulingConfig.java`(전용 `@Configuration` +
  `@EnableScheduling`, `@ConditionalOnProperty`), 테스트
  `service/MemberSignupSchedulingConfigTest.java`(3케이스 — false/true/미설정).
- 수정: `mapper/memberSignupRateLimit.xml`(`touchRateLimit` UPDATE 절 재작성 — 세션 변수
  제거, 주석 정정), `dao/MemberSignupRateLimitDao.java`(javadoc에 round6 설명 추가),
  `ShopApiApplication.java`(`@EnableScheduling` 제거 + 주석 정정),
  `service/MemberSignupMaintenanceScheduler.java`(javadoc에
  `MemberSignupSchedulingConfig` 참조 추가), `pom.xml`(shop-api —
  `maven-surefire-plugin` `systemPropertyVariables`로 `spring.task.scheduling.enabled
  =false` 추가).
- 문서: `STORY-FUNC-member-002.md`(이 절 + 수용 기준 체크박스 2건 확정 + 후속 추적(TODO)에
  DB 왕복 3회·`writeCode` 실패 시 쿼터 소모 2건 추가).

이 FUNC(FUNC-member-002) 소유 파일만 건드렸다. `ProductDao`/`OrderService`(FUNC-order-002)
는 이번 라운드에서 전혀 열지 않았다 — round5가 만든 `OrderCreateQtyZeroRegressionTest`가
여전히 통과하는 것으로 그 계약이 유지됨을 재확인했다.

### STEP 5.5 — 스펙 ↔ 코드 재동기화

QA PASS(round6) 이후 `docs/05_설계서/member/INF/INF-MBR-001.md`를 신규 생성했다(ddd-api-agent,
targeted 재-RECON — 이 프로젝트에 `member` 도메인 스펙이 아직 없어 AS-IS 스냅샷은 해당 없음).
`MemberSignupController`/`Service`/양쪽 DAO·매퍼·DDL을 근거로 실제 구현(요청/응답 계약, 6자리
코드·5분 만료, 60초 쿨다운·하루 5회 상한·거부 시 쿼터 미소비, 무인증 화이트리스트, 만료행
정리=10분 스케줄러, 오류코드 `MEMBER_TARGET_INVALID`/`MEMBER_VERIFY_COOLDOWN`/
`MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED`/`MBR-5000`)를 그대로 반영했다. 미완성 마커 없음(`[TBD]`는
기존 관례상 `srs-f` 필드뿐 — INF-ORD-009.md 등과 동일 패턴). `spec_staleness.py --baseline
--only=INF-MBR-001` 실행 완료(exit 0). SCH(`MEMBER_SIGNUP_VERIFICATIONS`,
`MEMBER_SIGNUP_RATE_LIMITS`)는 아직 없어 INF 본문에 테이블명만 나열하고 `[[SCH-...]]` 링크는
생략했다 — 다음 `/sl-recon-sch`가 이 두 테이블을 처리할 수 있도록 `_tmp/INF-MBR-001_sch_required.json`에
남겨뒀다.

## 후속 추적(TODO)
> round2 QA CONCERNS 권고 중 low 5건 — 사람 결정으로 지금 고치지 않고 목록만 남긴다(round3
> 사람 코멘트 "나머지 low 5건은 후속 TODO로만 남긴다").

- [ ] STORY 본문 라벨 잔여 불일치 — 이 STORY(002) 제목·AC·Dev Notes가 여전히
  `INF-MBR-001`, STORY-003의 AC·Dev Notes가 `INF-MBR-002`로 (신규 결정으로는 실제로는 이제
  둘 다 맞는 라벨이 됐다 — round3 INF-ID 원복 이후 재검토 필요). 이 STORY의 수용 기준
  체크박스 2개도 미체크 상태.
- [ ] DDL 주석의 DB호환성 오서술 — `member_signup_verifications.sql`의
  `ADD COLUMN IF NOT EXISTS` 주석이 "MariaDB(10.0.2+)/MySQL(8.0.29+) 문법"이라 적었으나
  MySQL은 버전 무관 미지원(MariaDB 전용). 랩은 MariaDB라 현재 무해하나 `spring.sql.init`
  기본 `continue-on-error=false`라 타 엔진 이식 시 shop-api 전체 기동 실패로 번질 수 있다.
- [ ] `spring.sql.init`의 `data-locations` 기본값 노출 — `schema-locations`만 지정했고
  `data-locations`(기본값 `optional:classpath*:data.sql`)가 살아있다. 현재 `data.sql`이
  없어 무해하나, 누군가 추가하면 모든 기동·모든 `@SpringBootTest`에서 자동 실행된다.
- [ ] 400 메시지의 입력 원문 반사 — `MemberSignupService`의 target 형식 오류 메시지가
  입력값을 그대로 반사한다(`"...형식이 올바르지 않습니다: " + target`). API 응답은 JSON이라
  직접 위험은 없으나, 화면이 이 메시지를 DOM에 그대로 렌더하면 반사형 XSS 경로가 될 수 있다.
- [ ] `purgeExpired`의 매 요청 전체 스캔 — `last_requested_at`에 인덱스가 없어 매 발송
  요청마다 풀스캔 DELETE가 발생한다(PK는 `channel,target`뿐). 무인증 엔드포인트라 요청이
  몰리면 DB 부하가 증폭될 수 있다.
  **(round4에서 해결됨 — purge가 `@Scheduled` 배치로 이전되고 `idx_msv_expires_at`/
  `idx_msrl_day_key` 인덱스가 추가돼 요청 경로 풀스캔은 더 이상 없다. 이 항목은 round4
  QA가 "TODO 정리 자체가 stale"이라 지적한 항목 중 하나로, round5도 사람 결정에 따라
  체크박스·문구 정리는 보류하고 사실만 괄호로 덧붙인다.)**

### round4/round5 신규 후속 항목 (round4 QA FAIL round4 재작업 지시 4/5/6 — 사람 결정
"1·2번 지금 고치고 3번은 후속 SR SR-294로 접수했다", low 3건은 지금 고치지 않고 목록만 남김)

- [ ] **[신규 SR] 당일 레이트리밋 행 무제한 누적 → SR-294 접수됨.** `MEMBER_SIGNUP_RATE_LIMITS`는
  서로 다른 target마다 행이 생기고 정리는 날짜가 바뀐 뒤에야(`day_key < today`) 배치가
  한다 — 무인증 엔드포인트에서 짧은 시간에 다수 target으로 요청하면 당일 내내 행이 무제한
  누적된다(QA의 HTTP 프로브만으로 몇 분 만에 55행 실측). IP/세션 단위 제한 또는 정리 주기를
  후속 SR **SR-294**로 접수했다(현재는 PK·day_key 인덱스가 있어 락·풀스캔 확대로는 번지지
  않고 저장소 증가 위험에 한정된다).
- [ ] 자정 경계에서 60초 쿨다운 미적용 — `day_key`가 PK의 일부라 날짜가 바뀌면 새 행이
  INSERT 브랜치로 만들어지므로 직전 요청 시각을 보지 않는다. target당 하루 경계에서 수 초
  간격으로 2회 발송이 가능하다(영향은 target당 하루 최대 1회 추가 발송 — round4 QA 권고6,
  round5도 고치지 않고 알려진 한계로 기록만 한다).
- [ ] 레이트리밋 fail-open 방어가 테스트 하나에만 의존 — round5로 `useAffectedRows=true`
  전역 설정 의존은 제거됐으나(그 설정이 깨지면 모든 요청이 무조건 허용되던 round4의 구체적
  위험은 이제 없다), `last_token` 비교 로직이 어떤 이유로든 깨지는 경우(예: 컬럼 길이·타입
  변경, UPSERT 조건식 오타)를 잡아주는 전용 가드·단언은 여전히 없다 —
  `MemberSignupRateLimitDaoTest`가 사실상 유일한 안전망이다(round4 QA 권고4, round5도
  고치지 않고 목록만 남긴다).
- [ ] 이 TODO 섹션 자체의 갱신 지연 — round4 QA가 "해결된 항목을 닫고 round3 권고3(당일
  행 누적)을 추가하라"고 지적했다(round4 QA 권고5). round5는 SR-294 등록 항목만 반영했고,
  이미 해결된 옛 항목의 완전한 정리는 사람 결정에 따라 여전히 보류한다.
  **(round6에서 수용 기준 체크박스 2건은 확정 완료 — 사람 결정, STORY 재작업 지시(3). 이
  항목이 언급한 "완전한 정리" 중 체크박스 건만 닫혔고, 옛 TODO 항목 정리 자체는 여전히
  보류다.)**

### round6 신규 후속 항목 (round5 QA CONCERNS 재작업 지시 4 — 사람 결정 "지금 고치지 않고
후속 TODO로만 남긴다")

- [ ] 요청당 DB 왕복이 3회(`touchRateLimit` UPSERT → `selectRateLimit` 재조회 →
  `writeCode`)다 — round4(단일 UPSERT 반환값 판정)보다 1회 늘었다. round5가 datasource
  전역 설정(`useAffectedRows`) 의존을 없애는 대신 치른 비용이다. 무인증 엔드포인트라
  기록해 둔다 — PK 조회 1건 추가라 비용 자체는 작고, 판정 정확성(전역 설정 비의존)과의
  교환으로 사람이 타당하다고 승인한 설계다(round5 QA 권고4, round5 재작업 지시 4).
- [ ] `touchRateLimit`이 쿼터를 소비(허용 판정 + `daily_count` 증가)한 뒤 `writeCode`가
  실패하면(예: DB 장애 → 500 `MBR-5000`) 그 요청은 코드를 받지 못했는데 일일 쿼터만 1
  소모된다. 두 문장이 트랜잭션으로 묶여 있지 않아(round4 결정 — 데드락 회피) 원자적으로
  같이 롤백되지 않는다. 부하가 실제로 문제되면 판정과 코드 기록을 한 문장으로 합치는
  설계를 검토할 것(round5 QA 권고4, round5 재작업 지시 4 — 지금은 기록만).

## QA 결과

### QA Gate — 2026-09-12 — FAIL
- **Layer1 스펙**: **차단**. SR 정본 `docs/변경관리/SR-231/02_변경명세.md`는 **FUNC-member-002(INF-MBR-001) = 가입 요청 API**(회원 생성·이메일 중복검사 409 `MEMBER_EMAIL_DUPLICATE`·인증완료 서버측 강제·환영 쿠폰 이벤트 발행), **FUNC-member-003(INF-MBR-002) = 인증코드 발송/확인 API**로 배정했다. 구현은 정확히 **반대**(이 FUNC에 인증코드 발송을 구현하고 가입완료를 member-003으로 미룸). Dev 기록은 근거로 "요구 요지 + 확정 문답 9건"만 들고 02_변경명세를 읽은 흔적이 없다 — AC2("SR 정본 계약 충족 — 02_변경명세.md")가 정면으로 미충족. 정본이 스스로 "추정 역할"이라 적었으므로 **코드 재작성이 아니라 문서 정합화로 닫는 것이 맞다**(아래 필수1). 부수: 정본 §오류 응답 계약 표(code/HTTP/message)를 따르지 않아 400 응답에 `code` 필드가 없다. 수용 기준 체크박스 2개 모두 미체크.
- **Layer2 보안**: **경고**. 무인증(화이트리스트) 엔드포인트에 레이트리밋·쿨다운이 전무 → ① 피해자 대상 인증코드 폭탄, ② target 1건당 PK행 1개가 무제한 생성되고 purge도 없어 저장소 고갈 DoS. 인증코드를 INFO 로그에 평문 기록(랩 시뮬레이션 의도는 문서화됨 — 운영 전환 시 제거 필수). 시도횟수 컬럼(`attempt_count`) 부재로 10^6 공간 코드의 무제한 대입 구조를 member-003에 넘긴다(스키마 소유자는 이 FUNC). 양호: 코드를 응답에 싣지 않음 · SecureRandom 균등(모듈로 편향 없음) · 로그 target 마스킹 · 재발송 시 `verified_at` 초기화 · 화이트리스트가 prefix가 아닌 **정확 일치**.
- **Layer3 회귀**: **경고**. 기존 계약 회귀는 없음(isOpenRoute 정확일치 1건 추가라 기존 라우트 판정 불변, MEMBERS 테이블 미변경 → 확정 문답 "기존 조회 결과 전부 불변" 준수). 단 **`lab/db/schema.sql`에 MEMBER_SIGNUP_VERIFICATIONS가 없음(실측 확인)** — 랩 DB 재생성 시 테이블 소실 → Dao 3건·통합 2건 실패 + 엔드포인트 500. 또 `target` 길이 검증이 없는데 컬럼은 VARCHAR(100)이고 랩 DB는 `STRICT_TRANS_TABLES`(실측) → 101자 이상 이메일형 target에 500(MemberSignupController에는 @RestControllerAdvice가 없다 — 기존 advice 2개는 Cart/Order 컨트롤러 한정).
- 필수 수정(FAIL시):
  1. **FUNC↔API 역할 반전을 정본과 정합화한다.** `docs/변경관리/SR-231/02_변경명세.md`의 FUNC-member-002/003 TO-BE 절을 실제 분할(002=인증코드 발송, 003=인증코드 확인+가입완료)로 수정하고, **`STORY-FUNC-member-003.md`에 이 분할과 "MEMBERS.email 컬럼 추가·유니크 제약·중복 판정은 003 소유" 결정을 명시**한다. 현재 member-003은 status=Approved·Dev 기록 공란이고 그 story가 링크한 정본은 여전히 "INF-MBR-002 = 인증코드 발송"이라, 독립 컨텍스트의 dev-agent가 **인증코드 발송을 중복 구현하거나 가입완료를 영구 누락**한다. (Dev 기록의 인계 메모가 member-002 story에만 있어 member-003에 전달되지 않는다.)
  2. **`lab/db/schema.sql`에 MEMBER_SIGNUP_VERIFICATIONS DDL을 반영**한다(워크스페이스 밖이라 dev가 보류한 항목 — 미해결 상태 그대로 남아 랩 리셋 시 확정 파손).
  3. **`target` 길이 상한 검증을 서비스에 추가**한다(VARCHAR(100) 정합, 400으로 거부). 현재 101자 이상은 400이 아니라 500이다.
- 권고(CONCERNS시):
  1. 무인증 엔드포인트에 재발송 쿨다운(예: 동일 target 60초)·IP 단위 상한·만료행 purge를 추가. 최소한 SR 후속 항목으로 등록.
  2. `attempt_count` 컬럼을 이 FUNC의 DDL에 미리 포함(003의 추가 DDL 변경 회피 + 코드 대입 방어).
  3. 400 응답을 정본 오류 계약 형태(`{code, message}`)로 통일 — 형식 오류용 코드(예: `MEMBER_TARGET_INVALID`)를 정본 표에 추가.
  4. `ApiKeyAuthIntegrationTest` 신규 2건이 실 DB에 행(`newbie@example.com`, `01099998888`)을 남기고 정리하지 않는다(DaoTest는 @AfterEach 정리) — 반복 실행 시 랩 DB 오염 누적.
  5. 주석 사실오류 정정: "MEMBERS_ITEM_PATH 정규식이 `signup`을 memberId로 오인한다"는 서술(ApiKeyAuthFilter 100~102행, 컨트롤러/테스트 주석에 복제됨)은 틀렸다 — `^/api/members/([^/]+)$`는 세그먼트 1개만 매치하므로 3세그먼트 경로엔 애초에 매치되지 않는다. 화이트리스트가 필요한 진짜 이유는 default-deny(무키 401) 하나뿐이다.
  6. `expires_at`은 앱 시계(`LocalDateTime.now()`), `requested_at`은 DB 시계(`CURRENT_TIMESTAMP`)로 서로 다른 출처다 — 003이 만료 판정을 SQL `NOW()`로 하면 JVM/DB 타임존 차이가 만료를 통째로 왜곡한다. 판정 시계를 한쪽으로 고정할 것.

### QA Gate — 2026-09-12 — CONCERNS (round 2 재게이트)

**round1 필수·권고 이행 대조(전부 코드/DB 실측으로 확인)**

| round1 항목 | 상태 | 실측 근거 |
|---|---|---|
| 필수1 문서 정합화(역할 교체) | 이행 | `02_변경명세.md` §FUNC-member-002=인증코드 발송(37~62행)·§FUNC-member-003=가입요청(64~88행)로 교체됨. `STORY-FUNC-member-003.md` 제목/요약이 "가입 요청 API 구현(INF-MBR-001 역할)"로 정정, **status=Approved·approved_sha 미변경 확인**(사람 지시 준수). 003 절에 `MEMBERS.email` 소유·`MEMBER_SIGNUP_VERIFICATIONS` 재사용·`attempt_count` 인계까지 기록됨 |
| 필수2 DDL 적용 경로 | 이행(QA 직접 실증) | `application.yml` `spring.sql.init.mode=always` + `schema-locations=classpath:db/member_signup_verifications.sql`. **QA가 랩 DB에서 테이블을 DROP한 뒤 `mvnw test -Dtest=MemberSignupVerificationDaoTest`를 돌려 기동 시 8컬럼 테이블이 자동 재생성되고 테스트가 통과함을 확인**(exit 0, `SHOW COLUMNS` 8컬럼). dev가 근거로 든 "테스트 통과 = 적용 경로 실증"은 기존 테이블이 있으면 성립하지 않는 논증이라, 이 drop 후 재생성 실험으로 별도 확증함 |
| 필수3 target 길이(100) + `{code,message}` | 이행 | `MemberSignupService` 123행 `MAX_TARGET_LENGTH` 검증이 채널 판별 **이전**에 위치(DB 도달 전 차단). `MemberSignupApiException`+`MemberSignupExceptionHandler`(`assignableTypes=MemberSignupController.class` — 기존 advice 2개와 스코프 비간섭). 슬라이스 테스트가 400/429 봉투의 `$.code`·`$.message`를 실제 검증 |
| 필수4 레이트리밋·쿨다운·purge | 부분 이행 | 60초 쿨다운·1일 5회·`purgeExpired` 모두 존재하고 동작하나, **1일 상한이 purge 선행 구조로 사실상 무력**(아래 권고1) |
| 권고 로그 평문코드 | 이행 | `code` 필드 자체를 로그에서 삭제, target은 마스킹. **실제 테스트 실행 로그에서 `target=us**************` 형태만 출력되고 코드 미출력 확인** |
| 권고 통합테스트 DB 정리 | 이행 | `ApiKeyAuthIntegrationTest` `@AfterEach cleanUpSignupVerificationRows()`. 전체 스위트 실행 후 테이블 행 수 0건 실측 |
| 권고 주석 사실오류 | 이행 | `ApiKeyAuthFilter` 98~104행·`ApiKeyAuthIntegrationTest` 265~267행 모두 "default-deny라 무키 요청이 401"로 교체. 잔존 오서술 grep 0건 |
| 권고 `attempt_count` | 이행 | DDL 27행 + `ALTER ... ADD COLUMN IF NOT EXISTS`, 도메인/셀렉트 매핑까지 포함. DB 실측 8컬럼 확인 |
| 권고 시계 통일 | 이행 | `requested_at`을 앱 시계로 저장(매퍼 `#{requestedAt}`), `expires_at`과 동일 출처 |

- **Layer1 스펙**: **경고**. 사람이 확정한 역할 배정(002=인증코드 발송/확인, 003=가입요청)이 `02_변경명세.md`·소스 javadoc·`STORY-003` 제목에서 일관되게 반영돼 round1의 차단 위험(003이 인증코드 발송을 중복 구현하거나 가입완료를 영구 누락)은 해소됐다. 오류 응답 계약도 정본 표 형식(`{code,message}`)으로 통일되고 `MEMBER_TARGET_INVALID`/`MEMBER_VERIFY_COOLDOWN`/`MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED`가 정본 53~56행에 등재됐다. 남은 것은 **INF-ID 라벨이 ID 정본(`func_registry.json`)과 반대**라는 점(권고3)과 두 STORY 본문의 잔여 라벨 불일치(권고4) — 둘 다 구현 행위가 아니라 추적 라벨 문제라 차단은 아니다.
- **Layer2 보안**: **경고**. round1 대비 실질 개선 확인 — 코드 평문 로그 제거, 60초 쿨다운, 만료행 purge로 저장소 무한 증가 차단, `attempt_count` 선반영. 다만 레이트리밋 두 축이 약하다: ① 1일 5회 상한이 만료 purge와 같은 5분 창을 공유해 **5분 이상 간격으로 요청하면 카운트가 매번 1로 리셋**(권고1), ② 판정이 select→계산→upsert라 **동시 요청 버스트가 상한을 우회**(권고2, 프로젝트 house rule 3 위반). 두 축을 합치면 무인증 엔드포인트에서 피해자 대상 코드 폭탄이 완전히는 막히지 않는다(60초 쿨다운으로 속도는 제한됨). 양호: 코드 미응답·미로그, SecureRandom 균등, 화이트리스트 정확일치, 재발송 시 `verified_at` 초기화.
- **Layer3 회귀**: **경고**. `mvnw test` 전체를 **QA가 직접 재실행**해 surefire 집계로 **247건 / 실패 0 / 에러 0 / 스킵 0** 확인(dev 보고와 일치). 기존 계약 회귀 없음(화이트리스트 정확일치 1건 추가, MEMBERS 테이블 미변경). round1 최대 위험(랩 리셋 시 테이블 소실)은 drop-후-재생성 실험으로 해소 확인. 새로 생긴 잔여 위험은 `spring.sql.init`이 기동 경로에 DDL을 얹었다는 사실 자체다 — 스크립트가 실패하면 이 FUNC이 아니라 shop-api 전체가 부팅 실패하고, 사용한 `ADD COLUMN IF NOT EXISTS`는 MariaDB 전용 문법인데 주석은 MySQL도 지원한다고 적었다(권고5·6).
- 권고(CONCERNS시):
  1. **[medium/security] 1일 5회 상한이 사실상 무력하다.** `requestVerificationCode`가 `purgeExpired(now)`(만료행 삭제) → `selectByChannelAndTarget` 순서로 동작하는데 `expires_at = requested_at + 5분`이므로, 마지막 요청이 5분보다 오래된 target의 행은 조회 직전에 삭제되고 `existing == null` → `nextDailyCount = 1`이 된다. 즉 **5분 1초 간격으로 요청하면 영원히 1회차**이며 하루 상한은 걸리지 않는다(시간당 ~12건). Dev 기록은 이 한계를 "정상적인 재발송 간격에서는 발생하지 않음"으로 적었는데, 이 통제가 막으려는 대상(의도적 남용)은 정확히 그 간격을 고른다. → purge를 select 이후로 옮기고 자기 `(channel,target)` 행은 제외하거나, 일일 카운트를 코드 만료와 분리된 컬럼(예: `daily_count_date`)으로 유지할 것.
  2. **[medium/security] 레이트리밋 판정이 read-modify-write다.** `selectByChannelAndTarget` → 카운트 계산 → `upsertCode`는 `docs/project-context.md` Critical Rule 3(동시 갱신은 DB 원자 연산/행 잠금, select→분기→update 금지)에 정면으로 걸린다 — dev 자신이 `upsertCode`에는 그 규칙을 적용했으면서 새로 넣은 카운터 경로에는 적용하지 않았다. 동시 요청 N건이 같은 기존 행을 읽으면 전부 쿨다운·상한을 통과하고 `daily_count`는 1만 증가한다(버스트 증폭). → 조건부 원자 UPDATE(`ON DUPLICATE KEY UPDATE ... IF(...)` 후 affected rows로 판정) 또는 `SELECT ... FOR UPDATE`.
  3. **[medium/spec] INF-ID 라벨이 ID 정본과 반대다.** `.speclinker/func_registry.json`은 `inf:INF-MBR-001 → FUNC-member-002`, `inf:INF-MBR-002 → FUNC-member-003`이고 `FUNC_MAP.md` 20~21행도 동일한데, round2 문서 정정은 `02_변경명세.md`·소스 javadoc·`STORY-003` 제목에서 `FUNC-member-002 = INF-MBR-002`로 라벨을 뒤집었다(사람 코멘트 문구를 그대로 따른 결과). 역할 배정이 전 문서에서 일치하므로 지금 당장의 구현 혼선은 없으나, 구현 후 `/sl-recon-inf`가 레지스트리를 따라 INF 스펙을 역생성하면 **"INF-MBR-001 = 인증코드 발송"**이 나와 정본·코드 주석과 다시 어긋난다(round1과 같은 종류의 혼선이 한 층 아래에서 재발). → (a) 레지스트리 두 항목을 교체하고 `build_func_map.py`로 FUNC_MAP 재생성하거나, (b) INF-ID 라벨 정정을 되돌리고 역할 문구만 교체할 것. 어느 쪽이든 SR 종결 전에 한 번은 정해야 한다.
  4. **[low/spec] 두 STORY 본문에 라벨 잔여 불일치.** 이 STORY(002)의 제목·AC·Dev Notes는 여전히 `INF-MBR-001`, `STORY-003`의 AC·Dev Notes("**INF** INF-MBR-002")도 제목(INF-MBR-001 역할)과 반대다 — dev가 사람 지시 범위(제목/요약 한 줄)를 좁게 지킨 결과라 규칙 위반은 아니다. 002의 수용 기준 체크박스 2개도 여전히 미체크. → 권고3을 정할 때 함께 정리.
  5. **[low/regression] DDL 주석의 문법 호환성 서술이 틀렸다.** `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`는 MariaDB 전용이고 MySQL은 버전 무관 미지원인데, `member_signup_verifications.sql` 32행 주석은 "MariaDB(10.0.2+)/MySQL(8.0.29+) 문법"이라 적었다. 랩은 MariaDB 11.4.5(실측)라 지금은 무해하지만, `spring.sql.init`은 기본 `continue-on-error=false`라 스크립트가 실패하면 **shop-api 전체가 기동 실패**한다 — 잘못된 호환성 주석 + 기동 경로 결합은 blast radius가 이 FUNC을 넘는다.
  6. **[low/regression] `spring.sql.init.mode: always`의 부수 효과.** `schema-locations`만 지정했고 `data-locations`는 기본값(`optional:classpath*:data.sql`)이 그대로 살아 있다 — 현재 `shop-api` 리소스에 `data.sql`이 없어(실측) 문제가 없지만, 누군가 추가하는 순간 모든 기동과 모든 `@SpringBootTest` 컨텍스트에서 자동 실행된다. → `data-locations`를 빈 값으로 명시하거나 주석으로 못박을 것.
  7. **[low/security] 400 메시지가 입력 원문을 그대로 반사한다.** `MemberSignupService` 136행이 `"...올바르지 않습니다: " + target`으로 최대 100자 미검증 입력을 응답 본문에 실어 보낸다. API 응답 자체는 JSON이라 무해하나, FUNC-member-001 화면이 이 `message`를 그대로 DOM에 렌더하면 반사형 XSS 경로가 된다. → 원문 에코를 빼거나 화면이 텍스트 노드로만 렌더하도록 화면 story에 못박을 것.
  8. **[low/efficiency] `purgeExpired`는 매 발송 요청마다 전체 스캔 DELETE다.** 테이블 인덱스는 PK(channel,target)뿐이라 `expires_at` 조건은 인덱스를 타지 않는다. 무인증 엔드포인트라 요청이 몰리면 DB 부하가 증폭된다. → `expires_at` 인덱스 추가 또는 purge 호출 빈도 제한(확률적/주기적 실행).

### QA Gate — 2026-09-12 — FAIL (round 3 재게이트)

**사람 지정 주요 3건 이행 대조(전부 코드·실 DB·테스트 재실행 실측)**

| round2 사람 지정 | 상태 | 실측 근거 |
|---|---|---|
| (1) 레이트리밋 무력화 — purge와 카운터 분리 | **이행** | `memberSignupVerification.xml` 51~55행 `purgeExpired`가 `DATE(last_requested_at) < DATE(#{now})` — 오늘 행은 삭제하지 않는다. 카운터 컬럼(`daily_count`/`last_requested_at`/`previous_requested_at`)이 코드 컬럼(`code`/`expires_at`/`verified_at`)과 분리됨(랩 DB 실측 10컬럼, 마이그레이션 정상 적용). 신규 `MemberSignupRateLimitTest`가 `Clock.fixed`로 5분 1초 간격 6회를 돌려 6번째 429 `MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED` + `daily_count=6` 잔존까지 단언(모킹 아닌 실 DB DAO). `MemberSignupVerificationDaoTest#purgeExpired_deletesOnlyRowsFromPreviousDaysAndKeepsTodaysCounter`가 round2 버그를 실 DB로 직접 재발 방지. **QA 재실행 통과 — round2 권고1은 실제로 닫혔다.** |
| (2) 동시성 — 원자 UPSERT | **미이행(차단)** | 구조는 바뀌었다(`touchRequestCounter` = `INSERT..ON DUPLICATE KEY UPDATE` 한 문장 + `writeCode` 분리, select→분기→update 제거 — house rule 3의 **형식** 요건은 충족). 그러나 사람이 요구한 **결과**("동시요청 시 정확히 1건만 성공, 나머지 429")는 성립하지 않는다. **QA가 `MemberSignupRateLimitConcurrencyTest`를 단독 재실행(`surefire:test -Dtest=...`) 3회 — 3회 모두 FAIL.** 실제 응답 분포는 `200 1건 + 500 4건`, **429는 0건**. 본문: `SQLTransactionRollbackException: Deadlock found when trying to get lock`, 발생 지점은 `touchRequestCounter`의 INSERT..ODKU 문장. 아래 Layer2·Layer3 참조 |
| (3) INF-ID 라벨 원복 | **이행** | `.speclinker/func_registry.json` 정본(`inf:INF-MBR-001→FUNC-member-002`, `inf:INF-MBR-002→FUNC-member-003`)과 전부 일치 확인 — `02_변경명세.md` 44행 §FUNC-member-002 **(INF-MBR-001)**·73행 §FUNC-member-003 **(INF-MBR-002)** + 9~14행 round3 정정 코멘트, `STORY-FUNC-member-003.md` 12·15행 "INF-MBR-002 역할"(AC 35행·Dev Notes 40행도 INF-MBR-002라 이제 제목↔본문이 서로 일치), 소스 javadoc(`MemberSignupController` 12·20행, `MemberSignupService` 20·24행, `MemberSignupApiException`) 및 `member_signup_verifications.sql` 4~7행. **STORY-003 frontmatter `status: Approved`·`approved_sha: 66271c8e322b` 불변 확인**(사람 지시 준수). 워크스페이스 전역 grep에 역방향 표기 잔존 0건(STORY-002의 round2 이력 서술은 과거 기록이라 제외). `FUNC_MAP.md` 20~21행도 레지스트리와 동일 — 재생성 불필요 |
| low 5건 후속 TODO 기록 | **이행** | STORY 276~296행 `## 후속 추적(TODO)`에 5건 모두 존재(STORY 본문 라벨 잔여+AC 체크박스 / DDL 주석 DB호환성 오서술 / `spring.sql.init` data-locations / 400 메시지 입력 원문 반사 / `purgeExpired` 전체 스캔). 사람 지시대로 고치지 않고 기록만 — 규칙 준수 |

**(2) 차단 상세 — QA 실측**

- 무인증 엔드포인트에 동시 5요청을 넣으면 **4건이 429가 아니라 HTTP 500**으로 떨어진다. 외부에서 임의로 유발 가능하다.
- 원인 2축(둘 다 round3 재작업이 만든 구조):
  1. 같은 **비존재** PK에 대한 다중 동시 `INSERT..ON DUPLICATE KEY UPDATE`는 중복키 시 S락을 잡고 UPDATE를 위해 X락 승격을 시도한다 — 3자 이상 동시일 때 전형적인 InnoDB 데드락(이 테스트는 N=5).
  2. 같은 트랜잭션이 **직전에** 실행하는 `purgeExpired`가 `last_requested_at` 조건의 **무인덱스 풀스캔 DELETE**다(실측: 이 테이블의 인덱스는 PK(channel,target) 단 하나). 랩 DB는 `@@transaction_isolation=REPEATABLE-READ`(실측)라 스캔한 전 행·갭에 next-key 락이 걸리고 **커밋까지 유지**된다. round2에는 `@Transactional`이 없어 이 락이 문장 단위로 풀렸는데, round3가 메서드 전체를 트랜잭션으로 감싸면서 락 범위가 `(channel,target)` 한 행 → **테이블 전체**로 확대됐다(서로 다른 target끼리도 간섭).
- **전체 스위트에서는 이 테스트가 녹색이다 — QA가 2회 재현(250건 / 실패 0 / 에러 0 / 스킵 0, BUILD SUCCESS). dev 보고 자체는 정확하다.** 그러나 스위트 내 이 테스트의 소요시간은 **0.027~0.028s**인 반면 단독 실행은 **1.2s**다. 스위트에서는 JVM·JDBC 드라이버·MyBatis가 이미 워밍업돼 5개 요청이 사실상 순차 처리되고 겹치지 않는다 — 콜드 실행에서만 실제 겹침이 생기고, 그때 구현이 깨진다. 즉 **이 회귀 테스트는 타이밍 의존이며, 녹색일 때는 동시성을 검증하지 않고 실제로 검증할 때는 실패한다.** "250건 실패 0"은 (2)의 이행 근거가 되지 못한다.
- 추가로 이 테스트는 **완전 직렬화 상태에서도 통과한다**(200 1건 + 쿨다운 429 4건). round2 구현조차 직렬화되면 통과하므로, 겹침을 강제·단언하지 않는 한 회귀 가치가 구조적으로 보장되지 않는다.

- **Layer1 스펙**: **경고**. 역할 배정·INF-ID 라벨이 ID 정본과 일치하게 회복돼 round2 권고3/4의 스펙 축은 닫혔다. 오류 계약 `{code,message}` 봉투와 `MEMBER_TARGET_INVALID`/`MEMBER_VERIFY_COOLDOWN`/`MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED` 유지. 다만 **동시 요청 경로에서 정본(`02_변경명세.md` §오류 응답 계약: 400/429만 규정)에 없는 500이 나온다** — 계약 외 응답. 부수: 정본 66~67행 §스키마 소유 컬럼 목록이 round3 컬럼 분리(`last_requested_at`/`previous_requested_at` 신설, `requested_at` deprecated)를 반영하지 못해 stale. 수용 기준 체크박스 2개는 여전히 미체크(사람이 후속 TODO로 미룬 항목).
- **Layer2 보안**: **차단**. ① 무인증 엔드포인트가 동시 요청 5건에 **4건 500** — 외부에서 임의 유발 가능한 오류 폭증·가용성 경로. ② 그 500 본문에 **서버 절대경로(`D:\...\target\classes\mapper\memberSignupVerification.xml`)·매퍼 파일명·SQL 전문·JDBC 커넥션 번호**가 그대로 실려 나간다(`application.yml` `server.error.include-message: always` + 이 컨트롤러에 `DataAccessException` 핸들러 없음 — `MemberSignupExceptionHandler`는 `MemberSignupApiException`만 처리). 무인증 정보 노출이며 `docs/project-context.md` Critical Rule 2("DB 계층 예외는 정직하게 500, 본문은 정제 메시지, 원본은 서버 로그로만")에도 어긋난다. ③ round3 신규 부작용 — 카운터를 항상 **먼저** 증가시키므로 **쿨다운(429)으로 거부된 요청도 `daily_count`를 소비한다**. 6회 급속 요청이면 해당 target은 코드 1건만 받은 채 당일 상한에 도달 — **피해자 가입 차단(계정 선점형 DoS)이 6초 만에 성립**한다(round2는 거부된 요청이 카운트를 소비하지 않았다). ④ purge가 전날 행만 지우므로 서로 다른 target으로 요청하면 **당일 내내 행이 무제한 누적**된다(round2는 5분 내 회수) — 그 누적이 위 무인덱스 풀스캔 DELETE의 비용과 락 보유 시간을 선형으로 키운다. 양호: 코드 미응답·미로그, SecureRandom 균등, target 마스킹, 화이트리스트 정확일치, PK 콜레이션 `utf8mb4_general_ci`(실측)라 대소문자 변형으로 카운터를 우회할 수 없음.
- **Layer3 회귀**: **차단**. 기존 계약 회귀는 없다 — `mvnw test` 전체를 QA가 **2회 독립 재실행**해 250건/실패 0/에러 0/스킵 0·BUILD SUCCESS 재현, 기존 테스트 전부 통과, MEMBERS 테이블 미변경, 화이트리스트 정확일치 유지, 랩 DB 실측 10컬럼(멱등 마이그레이션 정상)·스위트 후 행 수 0건(정리 훅 정상). 그러나 **사람이 명시 요구한 회귀 테스트 (b)가 단독 실행에서 3/3 실패**하고 스위트 녹색은 실행 순서·워밍업에 의존하는 위양성이다 — 이 상태로 통과시키면 "동시성 회귀를 막는 테스트가 있다"는 잘못된 안전감만 남는다. 또 `@Transactional` 도입이 무인덱스 풀스캔 DELETE를 요청 트랜잭션 안으로 끌어들여 락 범위를 테이블 전체로 확대했다(round2 대비 명백한 후퇴, round2 권고8이 low에서 차단급으로 승격된 셈).
- 필수 수정(FAIL시):
  1. **동시 요청에서 500(deadlock)이 나오지 않게 한다.** ① `purgeExpired`를 요청 트랜잭션에서 분리(스케줄러 또는 별도 트랜잭션)하거나 `last_requested_at` 인덱스를 추가해 풀스캔 next-key 락을 제거하고, ② `touchRequestCounter`의 INSERT..ODKU에 데드락 재시도(또는 `DeadlockLoserDataAccessException` → 429/재시도 매핑) 경로를 둔다. **목표 상태: 동시 N요청 → 200 1건 + 429 N-1건, 500 0건.**
  2. **`MemberSignupRateLimitConcurrencyTest`를 단독 실행에서도 통과시키고, 겹침을 강제·단언하도록 보강한다.** 요청 직전 `CyclicBarrier`로 실제 동시 진입을 강제하고 "500 응답 0건"을 명시 단언할 것 — 현재 테스트는 완전 직렬화돼도 통과해 round2 결함조차 잡지 못한다. **완료 조건: `mvnw test -Dtest=MemberSignupRateLimitConcurrencyTest` 단독 3회 연속 통과.**
  3. **이 컨트롤러 스코프에 `DataAccessException` 핸들러를 추가**해 내부 SQL·매퍼 절대경로·커넥션 번호가 응답 본문으로 새지 않게 한다(정제 메시지만 응답, 원본은 서버 로그로만 — Critical Rule 2 그대로).
- 권고(CONCERNS시):
  1. **쿨다운(429)으로 거부된 요청이 일일 쿼터를 소비하는 문제.** 사람 결정은 "상한 초과 시 롤백하지 않는다"였지 "쿨다운 거부도 일일 쿼터를 태운다"는 아니었다 — 쿨다운 위반은 카운트에서 제외하거나(ODKU 조건 분기) 상한을 그에 맞게 재설정할 것.
  2. `02_변경명세.md` 66~67행 §스키마 소유 컬럼 목록을 round3 컬럼 구성으로 갱신(`last_requested_at`/`previous_requested_at` 신설, `requested_at` deprecated).
  3. 당일 내 행 무제한 누적 — 인덱스 추가와 함께 일일 정리 주기(스케줄러)나 IP 단위 상한을 후속 SR로 등록.

### QA Gate — 2026-09-12 — CONCERNS (round 4 재게이트)

**사람 최종 지시 (A)~(E) 이행 대조 — 전부 QA가 직접 실측(dev 보고를 근거로 쓰지 않았다. round3에서 "스위트는 녹색인데 단독 실행은 3/3 실패"였던 전례 때문에 이번에는 모든 항목을 별도 수단으로 재현했다)**

| 사람 지시 | 상태 | QA 실측 근거 |
|---|---|---|
| (A) 요청 경로 `@Transactional` 제거 + 별도 테이블 단일 원자 UPSERT | **이행** | `MemberSignupService.requestVerificationCode`(102~124행)에 트랜잭션 애노테이션이 없고, 호출은 `rateLimitDao.touchRateLimit`(카운터 테이블) → `verificationDao.writeCode`(코드 테이블) 두 개의 독립 autocommit 문장뿐이다. 신규 테이블은 랩 DB 실측으로 `PRIMARY KEY(target, day_key)` + `KEY idx_msrl_day_key` 확인. 요청 경로에 purge 없음 |
| (A-판정신호) affected-rows로 대체한 판단이 안전한가 | **이행 — QA가 드라이버 레벨로 직접 검증** | 사람이 준 "NOW 200ms 근접" 판정을 affected-rows로 바꾼 것은 **타당하고, 실제로 더 안전하다**. QA가 앱을 거치지 않고 MariaDB Connector/J 3.3.3 + MariaDB 11.4.5에 직접 JDBC 프로브를 붙여 같은 SQL의 `executeUpdate()` 반환값을 측정: `useAffectedRows=true`일 때 **신규=1 / 실제갱신=2 / 무변경=0**으로 문서화된 그대로 동작한다. `application.yml` 18행의 URL에 실제로 이 파라미터가 들어가 있고(테스트용 `src/test/resources/application.yml`이 없어 모든 `@SpringBootTest`도 같은 URL을 쓴다), 코드는 `affected == 1 \|\| affected == 2`를 허용으로 소비해 반환값 의미와 정확히 일치한다 |
| (B) purge → `@Scheduled` 배치 이동 + 인덱스 | **이행** | `MemberSignupMaintenanceScheduler`(`@Scheduled(initialDelay=600_000, fixedDelay=600_000)`), `ShopApiApplication`에 `@EnableScheduling`. 요청 경로 purge 호출 0건(grep). 랩 DB `SHOW INDEX` 실측 — `idx_msv_expires_at`·`idx_msv_last_requested_at` 둘 다 생성됨 |
| (C) `DataAccessException` 핸들러가 경로/SQL 미노출 | **이행 — QA가 실제로 장애를 유발해 확인** | 슬라이스 테스트(목 서비스)만으로는 "실제 DB 예외도 이 핸들러에 잡히는가"를 증명하지 못하므로, QA가 앱을 8099 포트로 기동한 뒤 `RENAME TABLE MEMBER_SIGNUP_RATE_LIMITS TO MSRL_QA_BAK`로 **실제 DB 장애를 유발**하고 엔드포인트를 호출했다. 결과: **HTTP 500, 본문은 정확히 `{"code":"MBR-5000","message":"일시적인 오류입니다. 잠시 후 다시 시도해 주세요"}`뿐** — 경로·매퍼명·SQL·커넥션 번호 전부 미노출. 서버 로그에는 원본(`BadSqlGrammarException ... Table 'sl_lab.member_signup_rate_limits' doesn't exist`)이 정상 기록. round3 Layer2 차단(무인증 정보 노출) 완전 해소. 테이블 원복 완료 |
| (D) `CyclicBarrier`로 동시 진입 강제 + 200==1/429==4/500==0 3회 반복 | **이행** | `MemberSignupRateLimitConcurrencyTest` 90행 `new CyclicBarrier(CONCURRENCY)`, 129행 `barrier.await(10s)` 직후 요청. 113~123행에 세 단언 모두 명시. 82~86행 `for` 3회 반복 + 반복 전 `cleanRelatedRows()`(코드·레이트리밋 양 테이블) |
| (E) 단독 3회 + 스위트 전체 통과 | **이행 — QA가 요구치(3회)를 넘겨 5회 재현** | `mvnw test -Dtest=MemberSignupRateLimitConcurrencyTest` **단독 5회 연속 실행 → 5/5 BUILD SUCCESS**(Tests run 1, Failures 0, Errors 0). 소요시간 **3.97~4.22s**로, round3의 위양성 징후(스위트 0.027s / 단독 1.2s)와 달리 매 실행이 실제로 경합했다. 각 실행이 내부 3회 반복이므로 5-way 경합 15회 재현. `mvnw test` 전체도 QA가 직접 재실행 — **255건 / 실패 0 / 에러 0 / 스킵 0 / BUILD SUCCESS** |

**round3 차단(동시성 500)이 구조적으로 없어졌는지 — QA 독립 재현(dev 테스트 코드를 쓰지 않은 별도 경로)**

- **JDBC 레벨 스트레스**(앱·MyBatis·Spring 전부 배제, 같은 SQL만 직접 실행): 매 라운드 새 PK(비존재 행)에 동시 진입 — **5스레드 × 300라운드**(허용 300 / 거부 1200 / 예외 **0**), **16스레드 × 200라운드**(허용 200 / 거부 3000 / 예외 **0**). 전 라운드에서 허용이 정확히 1건. **데드락 0건.** round3의 원인축(다중 동시 INSERT..ODKU의 S락→X락 승격)이 autocommit 단문 + 락 범위 분리로 실제로 해소됐음을 DB 계층에서 확증.
- **HTTP 레벨 독립 프로브**(QA가 작성한 별도 클라이언트, `CyclicBarrier` 동시 진입): 5-way × 5라운드(200=5/429=20), 10-way × 10라운드(200=10/429=90) — **전 라운드 200 정확히 1건, 5xx 0건.**
- 합계 3,200회 이상의 경합에서 500이 단 1건도 나오지 않았다.

**round3 권고1(거부된 요청의 쿼터 소비)도 닫힘 — QA 실측**: 실 서버에 같은 target으로 10회 연속 요청 → `200 ×1 + 429 MEMBER_VERIFY_COOLDOWN ×9`, 그 뒤 DB 조회 결과 `daily_count = 1`. 쿨다운 거부는 행을 전혀 바꾸지 않아 쿼터를 소비하지 않는다. round3가 지적한 "6초 만에 피해자 당일 가입 차단(계정 선점형 DoS)"은 성립하지 않는다.

- **Layer1 스펙**: **경고**. 사람 지시 (A)~(E)가 전부 구현됐고 오류 계약(`MEMBER_TARGET_INVALID` 400 / `MEMBER_VERIFY_COOLDOWN`·`MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED` 429 / `MBR-5000` 500)이 실제 응답과 일치한다. INF-ID 라벨·역할 배정은 round3에서 닫힌 상태 그대로 유지(`func_registry.json` 정본과 일치). 차단은 없으나 **정본 `02_변경명세.md` §FUNC-member-002(52~70행)가 round4를 전혀 반영하지 못해 stale**하다: ① 61행이 여전히 "별도 스케줄러 없이 발송 요청 시점마다 만료 행을 정리한다(단순화, 사람 결정)"라고 적혀 있는데 round4 (B)는 **정확히 그 반대**(요청 경로에서 제거 → `@Scheduled` 배치)로 바꿨다, ② 66~67행 스키마 소유 목록이 `MEMBER_SIGNUP_VERIFICATIONS(... requested_at, daily_count, attempt_count)`에 멈춰 있어 실제 9컬럼 구성(`last_requested_at`/`previous_requested_at` 신설, `requested_at`·`daily_count`·`previous_requested_at` deprecated)과 다르고, **이 FUNC이 새로 만들어 소유하는 테이블 `MEMBER_SIGNUP_RATE_LIMITS`(target, day_key, daily_count, last_requested_at)가 정본에 아예 존재하지 않는다**, ③ 62~65행 오류 계약 표에 `MBR-5000`(500)이 미등재. **round3 재작업 지시 6번이 명시적으로 요구한 "정본의 스키마 소유 컬럼 목록 갱신"이 수행되지 않았다** — dev round4 기록 스스로 "이 FUNC 소유 파일만 건드렸다"고 적어 문서 갱신을 건너뛴 것을 인정한다. 이것은 FUNC-member-003 dev가 인계받을 유일한 정본이므로, 방치하면 003이 deprecated 컬럼을 살아 있는 것으로 읽고 레이트리밋 테이블의 존재를 모른 채 구현한다(round1 FAIL이 지적한 인계 실패와 같은 종류). 부수: STORY `## 후속 추적(TODO)`도 stale(round4가 고친 "purgeExpired 매 요청 전체 스캔"이 아직 미해결로 남아 있고, round3 권고3 "당일 행 누적 → 후속 SR 등록"은 목록에 추가되지 않았다). 수용 기준 체크박스 2개는 여전히 미체크(사람이 후속 TODO로 미룬 항목).
- **Layer2 보안**: **경고**. round3 차단 3건이 전부 실측으로 닫혔다(위 표 (C)·동시성 재현·쿼터 소비). 양호: 코드 미응답·미로그, SecureRandom 균등, target 마스킹, 화이트리스트 정확일치(`isOpenRoute` 148~155행 — 이번 라운드 변경 없음), 500 본문 정제. 남은 위험 3건: ① **레이트리밋 전체가 datasource URL 쿼리 파라미터 하나에 걸려 있고, 그 파라미터가 빠지면 조용히 fail-open 된다.** QA가 드라이버로 직접 측정한 바 `useAffectedRows` 없이는 무변경 ODKU가 **0이 아니라 1**을 반환하므로, `affected == 1`을 허용으로 읽는 현재 코드는 **모든 요청을 200으로 통과시킨다**(쿨다운·일일상한 완전 무력화). 다행히 `MemberSignupRateLimitDaoTest`가 경계값 0/1/2를 실 DB로 단언해 이 회귀는 스위트에서 잡힌다 — 그래서 차단은 아니지만, 보안 통제가 "설정 한 줄 + 그 사실을 아는 테스트 한 개"에만 의존하고 실패 방향이 fail-open이라는 점은 기록해 둔다. ② **무인증 엔드포인트에서 당일 내 행이 무제한 누적된다** — `MEMBER_SIGNUP_RATE_LIMITS`는 서로 다른 target마다 행이 생기고 정리는 **날짜가 바뀐 뒤에야**(`day_key < today`) 배치가 한다. QA의 HTTP 프로브만으로 몇 분 만에 55행이 쌓였다(측정 후 정리함). round3 권고3이 "후속 SR로 등록"을 요구했으나 등록되지 않았다. round3처럼 락·풀스캔으로 번지지는 않으므로(PK·day_key 인덱스 존재) 저장소 증가 위험에 한정된다. ③ target 단위 일일상한이라 **피해자 이메일/번호의 당일 가입을 막는 경로 자체는 남아 있다** — 다만 이제 60초 간격 성공 요청 5회가 필요해 6초에서 약 4분으로 비용이 올라갔고, 이는 사람이 승인한 설계다. 기타 low(사람이 후속 TODO로 미룬 것): 400 메시지가 입력 원문을 그대로 반사(`MemberSignupService` 160행). 신규 low: `day_key`가 PK라 자정 경계에서는 새 행이 INSERT 브랜치로 생성돼 **60초 쿨다운이 자정을 가로질러 적용되지 않는다**(target당 하루 1회, 수 초 간격 2회 발송 가능).
- **Layer3 회귀**: **경고**. 스위트 전체를 QA가 직접 재실행해 **255건/실패 0/에러 0/스킵 0/BUILD SUCCESS** 확인(dev 보고와 일치, round3의 250건 대비 순증). 인증 화이트리스트·`MEMBERS` 테이블·기존 오류 어드바이스 스코프 모두 불변. 스위트 종료 후 두 테이블 행 수 0건(정리 훅 정상). 그러나 **이번 라운드가 이 FUNC 밖으로 새는 변경을 하나 만들었다**: `useAffectedRows=true`는 특정 쿼리가 아니라 **datasource 전역 설정**이라, 앱의 모든 UPDATE가 "매치된 행"이 아니라 "실제로 바뀐 행"을 반환하도록 바뀐다. QA가 드라이버로 직접 대조 측정한 결과 `ProductDao.decreaseStock`(`UPDATE PRODUCTS SET stock_qty = stock_qty - ? WHERE sku = ? AND stock_qty >= ?`)이 **qty=0일 때 종전 1 → 이제 0**을 반환한다. `OrderService.create` 184행은 이 반환값이 0이면 `409 "재고 부족"`을 던지므로, **`POST /api/orders`에 `qty: 0` 라인을 담은 요청이 종전 200(주문 생성)에서 409(그것도 사실과 다른 "재고 부족" 사유)로 바뀌었다** — FUNC-order-002의 동작 변경이다. `OrderController.create`에 `@Valid`가 없고 `OrderService.create`에도 수량 검증이 없어 이 입력은 실제로 도달 가능하며, 255건 중 이 경로를 덮는 테스트가 없다(qty=0 주문 생성 케이스 부재 — 그래서 스위트 녹색이 이 변경을 덮지 못한다). 영향 자체는 "말이 안 되는 입력이 조용히 성공하던 것이 잘못된 사유로 거절된다"라 실사용 피해는 작고 새 동작이 더 안전하다고도 볼 수 있으나, **한 FUNC의 필요로 공유 datasource 계약을 바꿨고 그 blast radius가 story·Dev 기록 어디에도 분석돼 있지 않다**는 점이 회귀 관점의 결함이다. 부수(low): `@EnableScheduling`이 전역으로 켜져 모든 `@SpringBootTest` 컨텍스트가 스케줄러 스레드를 띄운다 — `initialDelay=10분`이 현재 스위트(수십 초)를 보호하지만, 10분을 넘기는 컨텍스트가 생기면 배치가 테스트 데이터를 지울 수 있다(이 의존성이 주석에만 있고 단언으로 고정돼 있지 않다).
- 권고(CONCERNS시):
  1. **[medium/regression] `useAffectedRows=true`의 전역 blast radius를 좁히거나 막는다.** 가장 싼 방어는 `OrderService.create`에 `qty >= 1` 검증을 추가하는 것(이 입력은 어차피 400이 맞다). 근본 대응은 이 판정을 전역 커넥션 속성에 의존시키지 않는 것 — 예: 거부 분기에서 "같은 값 대입" 대신 **WHERE로 거른 조건부 UPDATE**를 써서 매치 자체가 일어나지 않게 하면(`UPDATE ... WHERE target=? AND day_key=? AND last_requested_at <= ? - INTERVAL ? SECOND AND daily_count < ?`) 두 시맨틱 어디서든 0/1이 같은 뜻이 된다. 어느 쪽을 택하든 **qty=0 주문 생성 회귀 테스트를 추가**해 이 변경이 다시 조용히 넘어가지 않게 할 것.
  2. **[medium/spec] 정본 `02_변경명세.md` §FUNC-member-002를 round4 구성으로 갱신한다**(round3 재작업 지시 6번 미이행분). ① 61행 purge 서술을 "`@Scheduled` 10분 배치"로, ② 66~67행 스키마 소유에 **`MEMBER_SIGNUP_RATE_LIMITS`(target, day_key, daily_count, last_requested_at) 신규 테이블**을 추가하고 `MEMBER_SIGNUP_VERIFICATIONS`의 deprecated 컬럼(`requested_at`/`daily_count`/`previous_requested_at`)을 명시, ③ 오류 계약 표에 `MBR-5000`(500) 등재. FUNC-member-003이 읽을 유일한 인계 문서이므로 SR 종결 전 필수.
  3. **[medium/security] 당일 내 레이트리밋 행 무제한 누적을 후속 SR로 등록한다**(round3 권고3 미등록분). target 단위 상한만으로는 서로 다른 target을 쓰는 공격자를 막지 못한다 — IP/세션 단위 상한 또는 당일 내 정리 주기를 검토. 지금은 락·풀스캔으로 번지지 않아 저장소 증가 위험에 한정된다.
  4. **[low/security] 레이트리밋이 설정 한 줄에 fail-open으로 걸려 있다는 사실을 코드에 못박는다.** `MemberSignupRateLimitDaoTest`가 사실상 유일한 안전장치이므로, 그 테스트 주석에 "이 단언이 깨지면 레이트리밋이 무력화된 것"임을 명시하거나 기동 시 커넥션 속성을 단언하는 헬스체크를 둘 것.
  5. **[low/spec] STORY `## 후속 추적(TODO)`를 round4 기준으로 정리한다** — 해결된 항목(`purgeExpired` 매 요청 전체 스캔)을 닫고, round3 권고3(당일 행 누적)을 추가. 수용 기준 체크박스 2건도 함께 정할 것.
  6. **[low/regression] 자정 경계에서 60초 쿨다운이 적용되지 않는다** — `day_key`가 바뀌면 INSERT 브랜치라 직전 요청 시각을 보지 않는다. target당 하루 1회라 영향은 미미하나, 쿨다운 판정을 day_key와 분리하거나 알려진 한계로 기록할 것.

### QA Gate — 2026-09-12 — CONCERNS (round 5 재게이트)

**사람 최종 결정 2건 이행 대조 — 전부 QA가 직접 실측(dev 보고·이전 QA 판정을 근거로 쓰지 않았다. round3에서 "스위트는 녹색인데 단독 실행은 3/3 실패"였던 전례 때문에 이번에도 모든 항목을 dev 테스트 코드를 쓰지 않는 별도 경로로 재현했다)**

| 사람 결정 | 상태 | QA 실측 근거 |
|---|---|---|
| (1-a) `useAffectedRows=true` 완전 제거 | **이행 — 차분 측정으로 확증** | `application.yml` 18행 datasource URL이 `jdbc:mariadb://127.0.0.1:3307/sl_lab?useUnicode=true&characterEncoding=utf8`로 이 파라미터가 없다. 워크스페이스 전역 grep 결과 잔존 출현은 **전부 주석·문서·이 STORY의 과거 기록뿐, 설정값은 0건**. `src/test/resources`에 별도 application.yml이 없어(디렉터리에 `sql/`만 존재) 모든 `@SpringBootTest`도 같은 URL을 쓴다. QA가 MariaDB Connector/J 3.3.3으로 **같은 UPDATE를 두 URL에 각각 직접 실행해 차분 측정**: `UPDATE PRODUCTS SET stock_qty = stock_qty - 0 WHERE sku='SKU-1002' AND stock_qty >= 0`이 **현재 URL에서 1(matched), `useAffectedRows=true`를 붙이면 0** — round4가 낸 회귀의 원인이 정확히 재현되고, 현재 상태에서는 원복됐음을 드라이버 계층에서 확증 |
| (1-b) `ProductDao`/`OrderService`(FUNC-order-002) 미변경 | **이행** | `modules/`는 `.gitignore` 12행으로 git 추적 밖이라 diff를 쓸 수 없어 **파일 수정시각으로 대조**: `ProductDao.java` 09-09 10:19 · `mapper/product.xml` 09-09 10:19 · `OrderService.java` 09-09 07:54 · `OrderController.java` 09-07 09:37 — **전부 round4(09-12 03~04시)·round5(09-12 04:29~04:36) 이전**이다. 같은 시각대에 바뀐 것은 member-signup 소유 파일과 `application.yml`(04:30)뿐. 소스 본문도 확인 — `decreaseStock`은 `UPDATE PRODUCTS SET stock_qty = stock_qty - #{qty} WHERE sku=#{sku} AND stock_qty >= #{qty}` 원형 그대로, `OrderService.create`에 수량 검증 추가 없음(사람 지시 "손대지 않는다" 준수) |
| (1-c) 새 UPSERT(세션변수 `@msrl_admit`)가 동시성 하에서 올바른가 | **이행 — QA가 JDBC 직결 프로브로 독립 재현** | 앱·Spring·MyBatis를 전부 배제하고 `memberSignupRateLimit.xml`의 SQL만 그대로 JDBC로 실행하는 프로브를 작성해 5축 측정. **[A] 8스레드 × 150라운드(`CyclicBarrier` 동시 진입, UPSERT와 재조회를 일부러 서로 다른 커넥션으로 분리)** — 허용 건수 분포가 **전 라운드 정확히 1(`{1=150}`)**, 예외·데드락 **0건/1,200요청**, 모든 라운드에서 `daily_count=1`(거부가 쿼터를 소비하지 않음). **[B] dev가 스스로 발견했다는 SET-순서 버그의 정확한 경계 재현** — 쿨다운 경과 7회 순차 요청이 `A1 A2 A3 A4 A5 r5 r5`(2번째 허용·5번째 허용·6번째 거부, `daily_count`가 5에서 정지)로, **"2번째 이후 last_token이 절대 갱신되지 않는다"는 결함이 실제로 해소**됐음을 확인. **[C]** 쿨다운 내 10회 재요청 전부 거부 + `daily_count`·`last_token` 불변. **[D] 세션변수 오염 전용 테스트** — 같은 커넥션에서 "허용(@msrl_admit=1)" → "신규 INSERT 브랜치(ODKU 미평가)" → "거부돼야 하는 요청" 순서로 실행해도 직전 문장의 잔존값이 판정에 새지 않음. **[E]** 쿨다운을 경과시킨 6스레드 버스트를 8회 반복해도 `daily_count`가 상한 5를 **넘지 않음**. **프로브 전 항목 PASS** |
| (1-d) HTTP 계층 독립 검증 | **이행 — QA 작성 별도 클라이언트** | 앱을 8099로 기동하고 dev 테스트가 아닌 QA 자체 클라이언트로: **8-way 동시요청 × 6라운드 → 전 라운드 `200=1 / 429=7 / 5xx=0`**, 같은 target 순차 20연타 → **`200=1 / 429=19 / 기타 0`**(레이트리밋이 fail-open이 아님을 실 경로로 확인), 429 본문이 `{"code":"MEMBER_VERIFY_COOLDOWN", ...}` 봉투, 형식 오류 → `400 MEMBER_TARGET_INVALID`. 측정 후 프로브가 남긴 행·주문을 전부 삭제해 두 테이블 0행으로 원복 |
| (2) qty:0 주문 라인이 200인가 | **이행 — 테스트 + 실 HTTP 양쪽** | `OrderCreateQtyZeroRegressionTest`(신규 1건)가 QA 재실행에서 통과. 추가로 QA가 실 서버에 직접 `POST /api/orders {"items":[{"sku":"SKU-1002","qty":0}]}`를 던져 **HTTP 200 + `orderNo` 발급 + `totalAmount:0` + 재고 불변**을 확인(409 아님). round4 회귀가 실제로 해소됐다 |
| (3) `02_변경명세.md` §FUNC-member-002 정합 | **이행 — 조항별 대조** | 절 전체를 실 DB 스키마·실 코드와 조항별로 대조: purge=`@Scheduled` 10분 배치 ✓, **`MEMBER_SIGNUP_RATE_LIMITS`(target+day_key 복합PK, daily_count, last_requested_at DATETIME(3), last_token VARCHAR(36) NULL, `idx_msrl_day_key`)가 랩 DB `SHOW COLUMNS`/`SHOW INDEX` 결과와 컬럼·타입·NULL 허용·인덱스까지 정확히 일치** ✓, `MEMBER_SIGNUP_VERIFICATIONS`의 live 컬럼과 deprecated 3컬럼(`requested_at`/`daily_count`/`previous_requested_at`) 구분이 실제 10컬럼 구성과 일치 ✓, `idx_msv_expires_at`·`idx_msv_last_requested_at` ✓, 오류계약에 `MBR-5000`(500) 등재 ✓, 쿨다운 60초·일일 5회·**거부는 쿼터 미소비**(QA 프로브 [A]/[C]로 실증) ✓, round5 토큰 판정 방식이 실제 SQL·서비스 코드와 일치 ✓, 알려진 한계(자정 경계·SR-294) 기재 ✓. **round4 QA가 지적한 stale 3건이 전부 닫혔다** |
| (4) round4 low 3건 + SR-294 TODO 기록 | **부분 이행** | `## 후속 추적(TODO)`에 SR-294(당일 행 무제한 누적, 명시적으로 "SR-294 접수됨") ✓ · 자정 경계 쿨다운 ✓ · fail-open 방어 테스트 부재 ✓ · TODO 자체 stale ✓ 4건 모두 존재. 다만 round4 권고 **7번(@EnableScheduling 전역화)만 어디에도 남지 않았다** — 아래 권고2 |
| (5) 완료 조건(스위트 + 동시성 단독 3회) | **이행 — QA가 직접 재실행** | `mvnw test` 전체를 QA가 직접 재실행 — **256건 / 실패 0 / 에러 0 / 스킵 0 / BUILD SUCCESS**(surefire XML 집계로 교차 확인). `mvnw test -Dtest=MemberSignupRateLimitConcurrencyTest` **단독 3회 연속 → 3/3 BUILD SUCCESS**(Tests run 1, Failures 0, Errors 0), 소요 **4.084s / 4.006s / 4.086s** — round4와 같은 실경합 시간대이며 round3의 위양성 징후(단독 1.2s·스위트 0.027s)와 다르다 |

- **Layer1 스펙**: **통과**. 사람이 지시한 2건이 문자 그대로 구현됐다 — 판정 흐름(요청마다 UUID 생성 → `last_token = IF(허용조건, ?, last_token)` 조건부 기록 → 재조회해 내 토큰이면 200, 아니면 `daily_count>=5`면 429 일일상한·그 외 429 쿨다운)이 사람이 적어 준 문장과 일치하고, `last_token VARCHAR(36)` 마이그레이션도 이 FUNC의 DDL에 멱등(`ADD COLUMN IF NOT EXISTS`)으로 들어가 실 DB에 반영됐다. 오류 계약(`MEMBER_TARGET_INVALID` 400 / `MEMBER_VERIFY_COOLDOWN`·`MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED` 429 / `MBR-5000` 500)이 실 응답과 일치하고, round1~round4를 관통하던 **정본 `02_변경명세.md` stale 문제가 이번에 완전히 닫혔다**(위 표 (3) — 조항별로 실 DB 스키마와 대조). INF-ID 라벨·역할 배정은 round3에서 닫힌 상태 유지. 남은 것은 수용 기준 체크박스 2개 미체크뿐이며 이는 사람이 후속 TODO로 명시 유보한 항목이다.
- **Layer2 보안**: **통과**. 레이트리밋이 실제로 작동함을 **dev 테스트를 쓰지 않는 두 독립 경로**(JDBC 직결 1,200요청 · 실 HTTP 8-way×6라운드 + 순차 20연타)로 확인했다 — 중복 발급 0건, 오판정 0건, 5xx 0건, 거부가 쿼터를 소비하지 않으며 일일상한 5를 넘지 않는다. **round4 Layer2의 가장 큰 위험(레이트리밋 전체가 datasource URL 쿼리 파라미터 하나에 걸려 있고 그 파라미터가 빠지면 조용히 fail-open)은 구조적으로 사라졌다** — 판정이 이제 이 FUNC이 직접 쓰고 읽는 컬럼 하나에만 의존한다. 양호: 코드 미응답·미로그, SecureRandom 균등, target 마스킹, 화이트리스트 정확일치, 500 본문 정제(`MBR-5000`). 남은 위험은 전부 사람이 유보했거나 후속 SR로 넘긴 것 — 당일 행 무제한 누적(SR-294), 자정 경계 쿨다운 미적용, 400 메시지의 입력 원문 반사(QA 실측: `not-an-email`이 응답 본문에 그대로 에코됨), target 단위 상한이라 피해자 당일 가입을 막는 경로 자체는 잔존(사람 승인 설계). 신규 저위험 1건: `touchRateLimit`이 쿼터를 소비한 뒤 `writeCode`가 실패하면(500) 코드는 못 받고 쿼터만 1 소모된다(round4부터 있던 구조, 영향 미미).
- **Layer3 회귀**: **경고**. **round4가 낸 FUNC-order-002 회귀는 실제로 해소됐다** — datasource 전역 계약이 round4 이전 상태(matched-rows)로 원복됐음을 드라이버 차분 측정으로 확증했고, qty:0 주문이 실 HTTP에서 200으로 돌아왔으며, 그 계약을 고정하는 회귀 테스트(`OrderCreateQtyZeroRegressionTest`)가 추가돼 이 FUNC이 다시 전역 설정을 건드리면 잡힌다. FUNC-order-002 소유 파일은 수정시각·본문 양쪽으로 미변경 확인. 스위트 256건 실패 0, 동시성 단독 3/3 통과, 스위트 종료 후 두 테이블 0행. 다만 **이번 라운드가 한 가지 의존을 다른 의존으로 바꿨다**: 새 SQL은 같은 문장 안에서 사용자 변수 `@msrl_admit`을 쓰고 읽는데, MySQL/MariaDB 매뉴얼은 이 패턴을 "SET 문 외에는 같은 문장에서 사용자 변수에 값을 대입하고 그 값을 읽지 말 것 — 사용자 변수가 포함된 식의 평가 순서는 정의되지 않는다"로 **명시적으로 경고**한다. dev의 매퍼 주석은 이를 "문서화된 동작"이라 적었는데 정확하지 않다(UPDATE의 SET 목록이 좌→우로 평가된다는 것은 문서화돼 있으나, 사용자 변수 읽기/쓰기 순서는 그 반대다). QA가 랩 엔진(MariaDB 11.4.5)에서 드라이버 prepare 모드 4종(기본 클라이언트 prepare / `useServerPrepStmts=true` / `+cachePrepStmts=true` / `useBulkStmts=true`)으로 각각 7회 경계 시퀀스를 돌려 **4종 모두 기대 결과(`A1 A2 A3 A4 A5 r5 r5`)로 동일**함을 확인했고 1,200회 동시 요청에서도 흔들리지 않았으므로 **지금 깨져 있지 않고 차단도 아니다**. 그러나 실패 방향이 fail-open이라는 점은 round4와 같다(평가 순서가 바뀌어 직전 문장의 잔존값 1을 읽으면 거부돼야 할 요청이 `last_token`을 덮어써 허용된다) — 아래 권고1. 부수(low): 스위트 안에서 이 동시성 테스트의 소요가 0.083s로 단독(4.0s)보다 훨씬 짧다. `CyclicBarrier`가 동시 진입을 강제하므로 round3의 위양성 패턴과는 다르지만, **이 테스트의 회귀 가치는 단독 실행에서 나온다**는 점은 유지된다.
- 권고(CONCERNS시):
  1. **[medium/regression] 레이트리밋 판정이 "같은 문장 안에서 사용자 변수를 쓰고 읽는" 미정의 평가 순서에 의존한다.** 랩 엔진·드라이버 4종에서 실측 안정적이고 `MemberSignupRateLimitDaoTest`가 경계를 단언하므로 지금은 문제없으나, 엔진/버전/옵티마이저가 바뀌면 **fail-open**(거부돼야 할 요청이 `last_token`을 덮어써 허용)으로 깨질 수 있다 — round4가 전역 설정 하나에 걸려 있던 것과 같은 종류의 취약함이다. 사용자 변수를 쓰지 않는 결정적 대안이 있다: `last_token`을 **먼저** 갱신하고(조건식이 아직 갱신되지 않은 `last_requested_at`·`daily_count`만 참조), 나머지 두 컬럼은 `IF(last_token = #{token}, ..., ...)`로 **방금 기록된 내 토큰 자체를 조건으로** 삼으면 SET 목록의 좌→우 평가(이쪽은 실제로 문서화된 동작)만으로 결정적으로 동작한다. 매퍼 주석의 "문서화된 동작" 서술도 함께 정정할 것.
  2. **[low/spec] round4 권고7(@EnableScheduling 전역화)이 추적에서 사라졌다.** 사람 코멘트가 유보 대상 low를 3건(자정 경계·fail-open 방어 테스트·stale 정리)만 열거해 dev는 지시를 정확히 따랐으나, 그 결과 이 항목만 TODO에도 결정 기록에도 남지 않았다. 모든 `@SpringBootTest` 컨텍스트가 유지보수 스케줄러를 띄우고 `initialDelay=10분`이 주석으로만 보장되는 상태는 그대로다 — TODO에 한 줄 추가하거나 "고치지 않기로 함"을 명시할 것.
  3. **[low/spec] 이 STORY의 수용 기준 체크박스 2개가 5라운드째 미체크다.** 사람이 계속 유보해 왔으나 SR 종결 전에는 한 번 정해야 한다(FUNC 상태 정본이 STORY frontmatter + GATE이므로 미체크 AC가 그대로 남으면 커버리지 집계가 이 FUNC을 미완으로 읽는다).
  4. **[low/efficiency] 요청당 DB 왕복이 3회**(UPSERT → 재조회 → `writeCode`)로 round4보다 1회 늘었다. 무인증 엔드포인트라 기록해 둔다 — PK 조회라 비용은 작고, 판정 정확성과의 교환으로 타당한 선택이다.

### QA Gate — 2026-09-12 — PASS (round 6 재게이트)

**사람 최종 결정 3건 이행 대조 — 전부 QA가 직접 실측(dev 보고·이전 QA 판정을 근거로 쓰지 않았다. round3의 "스위트는 녹색인데 단독은 3/3 실패", round5의 "dev 자체보고와 실제가 달랐던 전례" 때문에 이번에도 모든 항목을 dev 테스트 코드를 쓰지 않는 별도 경로로 재현했다)**

| 사람 결정 | 상태 | QA 실측 근거 |
|---|---|---|
| (1-a) 세션변수 제거 + 지시한 SET 구조 그대로 | **이행 — SQL 원문 대조** | `memberSignupRateLimit.xml` 33~41행이 사람이 적어 준 순서와 문자 그대로 일치한다: ① `last_token = IF(last_requested_at <= #{now} - INTERVAL #{cooldownSeconds} SECOND AND daily_count < #{dailyLimit}, #{token}, last_token)`가 SET 목록 **맨 앞**(아직 미갱신인 원본 두 컬럼만 참조), ② `last_requested_at = IF(last_token = #{token}, #{now}, last_requested_at)`, ③ `daily_count = IF(last_token = #{token}, daily_count + 1, daily_count)`. 매퍼·DAO javadoc·전 소스 grep 결과 **SQL/설정에 남은 사용자 변수(`@msrl_admit`·`SET @`·`:=`) 0건** — 잔존 출현은 "round5가 이렇게 했었다"는 이력 주석 1곳뿐. round5의 부정확한 "문서화된 동작" 서술도 실제로 철회·정정됨(XML 17~32행) |
| (1-b) **"SET 목록 좌→우 평가만으로 결정적"이라는 dev 주장 자체의 검증** | **이행 — QA JDBC 직결 프로브 29검증 / 실패 0** | 앱·Spring·MyBatis를 전부 배제하고 매퍼 SQL만 JDBC로 그대로 실행하는 프로브를 새로 작성해 7축 측정(MariaDB **11.4.5** / Connector-J **3.3.3**). **[P1] 드라이버 prepare 모드 4종**(클라이언트 기본 / `useServerPrepStmts` / `+cachePrepStmts` / `useBulkStmts`) × 경계 시퀀스 7회 → **4종 모두 `A1 A2 A3 A4 A5 r5 r5`**, 그리고 **허용 시마다 세 컬럼이 반드시 함께 갱신**(좌→우 의존의 직접 증거). **[P2]** 허용/거부 각각의 사후 상태 — 허용은 3컬럼 전부 갱신(`daily_count`는 원본+1, `last_requested_at`는 이번 now), 거부는 3컬럼 전부 불변; 일일상한 경계(3·4·5번째 허용)도 정합. **[P3] 신규 커버리지 — `last_token IS NULL` 레거시 행**(`ADD COLUMN IF NOT EXISTS` 마이그레이션이 남기는 상태, 종전 어느 라운드도 검증한 적 없음): 쿨다운 경과 → 허용+3컬럼 갱신, 쿨다운 내 → `NULL = token`이 NULL로 평가돼 IF false → 거부+불변(**NULL 비교가 fail-open으로 새지 않음**). **[P4] 8스레드 × 150라운드(1,200요청, UPSERT와 재조회를 일부러 다른 커넥션으로 분리)** → 허용 건수 분포 **전 라운드 정확히 1(`{1=150}`)**, 예외·데드락 **0건**, 매 라운드 `daily_count=1`(거부가 쿼터를 소비하지 않음), **"허용인데 `last_requested_at`이 안 밀린" 케이스 0건**(fail-open 자기검증). **[P5]** 쿨다운 강제 경과 후 6스레드 버스트 ×8 → 관측 최대 `daily_count`=5, **상한 초과 0**. **[P6]** 쿨다운 내 10연타 → 10/10 거부, 3컬럼 불변. **[P7]** 날짜 경계 분리 확인 |
| (1-c) 파라미터 바인딩·타입에 남은 미정의 의존 | **없음 — 확인** | `#{token}`×3·`#{now}`×3은 JDBC 위치 파라미터로 각각 독립 바인딩되는 **동일 값**이라 바인딩 순서에 의미가 없다. `INTERVAL #{cooldownSeconds} SECOND`(int 바인딩)·`#{now}`(LocalDateTime) 조합이 **서버사이드 prepare 모드에서도 동일 결과**([P1]). `last_token` 비교는 소문자 UUID 고정이라 collation·trailing-space 영향 없음(실측). **round5가 기댔던 "같은 문장에서 사용자 변수 대입/읽기"(매뉴얼이 명시적으로 *정의되지 않음*이라 경고)가 round6에는 존재하지 않고, 남은 의존은 "UPDATE SET 목록 좌→우 평가"(실제로 문서화된 동작) 하나뿐** — 라벨만 바꾼 것이 아니라 의존의 종류가 실제로 바뀌었음을 확인 |
| (2-a) `@EnableScheduling` 스코프 축소 | **이행** | `ShopApiApplication`에서 애노테이션이 실제로 제거됨(주석만 이력으로 잔존). 신규 `MemberSignupSchedulingConfig`(`@Configuration` + `@ConditionalOnProperty(name="spring.task.scheduling.enabled", havingValue="true", matchIfMissing=true)` + `@EnableScheduling`). 앱 전체에 `@Scheduled`는 이 FUNC 1개뿐이고 `@Async`/`@EnableAsync`는 0건(shop-api·shop-web 전수 grep) — **전역 애노테이션 제거의 부수 영향이 구조적으로 없음** |
| (2-b) 테스트에서 실제로 스케줄러가 막히는가 | **이행 — QA 자체 `@SpringBootTest`로 독립 재현** | dev의 `MemberSignupSchedulingConfigTest`는 순수 `AnnotationConfigApplicationContext`만 써서 **실제 Boot 컨텍스트에서의 동작도, `@Scheduled` 메서드가 실제로 예약되는지도 증명하지 않는다**. QA가 별도 `@SpringBootTest`를 임시로 작성해 직접 측정(측정 후 삭제): 기본 상태에서 surefire 시스템 프로퍼티가 Environment까지 `false`로 도달 → `MemberSignupSchedulingConfig` 빈 **0개**, `ScheduledAnnotationBeanPostProcessor` **0개**, 그러면서 `MemberSignupMaintenanceScheduler` `@Component`는 **1개 그대로 존재** — dev의 "DI만 되고 예약 실행만 빠진다"가 사실임을 확인 |
| (2-c) **실제 운영 기동 시 스케줄러가 정상 동작하는가** | **이행 — QA가 실 기동으로 확인(dev가 "다음 세션 수동 확인"으로 남긴 항목을 이번에 닫음)** | dev는 이 항목을 컨텍스트 조립 확인으로 "갈음"하고 실 기동 확인은 다음 세션으로 미뤘다. QA가 프로퍼티를 전혀 주지 않은 **진짜 운영 조건**으로 `spring-boot:run`(포트 8099, `logging.level.org.springframework.scheduling=TRACE`)을 직접 띄워 확인: `MemberSignupSchedulingConfig`가 컴포넌트 스캔 후보로 식별되고, `ScheduledAnnotationBeanPostProcessor : 1 @Scheduled methods processed on bean 'memberSignupMaintenanceScheduler': purgeExpiredSignupData ... fixedDelay=600000, initialDelay=600000` 로그로 **태스크가 실제 예약**됐으며 `Started ShopApiApplication`까지 정상. 추가로 `spring.task.scheduling.enabled=true` `@SpringBootTest`에서 `ScheduledTaskHolder.getScheduledTasks()`에 `MemberSignupMaintenanceScheduler.purgeExpiredSignupData`가 담기는 것도 확인 |
| (3) AC 체크박스 2건 확정 | **이행** | 수용 기준 2건 모두 `[x]` + 실측 근거 문장 포함. 근거로 인용된 사실(round5 게이트의 오류계약 일치·`02_변경명세.md` 조항별 대조, round6 259건 통과)은 이번에 QA가 재실행으로 교차 확인함 |
| (4) 유보 2건 TODO 기록 | **이행** | `### round6 신규 후속 항목`에 DB 왕복 3회 ✓ · `writeCode` 실패 시 쿼터 소모 ✓ 2건 모두 기록(사람 결정 "지금 고치지 않고 후속 TODO로만" 준수) |
| (5) 완료 조건 | **이행 — QA가 직접 재실행** | `mvnw test` 전체 → **259건 / 실패 0 / 에러 0 / 스킵 0 / BUILD SUCCESS(exit 0)** — dev 보고(259건, round5 256 대비 +3)와 정확히 일치. `mvnw test -Dtest=MemberSignupRateLimitConcurrencyTest` **단독 3회 연속 → 3/3 BUILD SUCCESS**, 소요 **4.039s / 4.018s / 3.993s**(round4·round5와 같은 실경합 시간대이며 round3의 위양성 징후 0.03초대가 아님). 이 테스트가 내부적으로 3회 반복하므로 **5-way 경합을 총 9회 재현하며 매번 200=1 / 429=4 / 500=0**. 프로브 종료 후 두 테이블 0행으로 원복(DB MCP 확인) |

- **Layer1 스펙**: **통과**. 사람이 지시한 3건이 문자 그대로 구현됐다. 핵심은 (1-b)다 — dev의 "SET 목록 좌→우 평가만으로 결정적"이라는 주장을 QA가 액면 수용하지 않고 JDBC 직결 프로브로 독립 검증한 결과, **드라이버 prepare 모드 4종 전부에서 경계 시퀀스가 동일**하고 허용 시 세 컬럼이 항상 동반 갱신되며, 1,200요청 동시성에서도 허용이 정확히 1건이었다. round5가 기댔던 의존(사용자 변수 대입/읽기 = 매뉴얼이 *정의되지 않음*이라 명시 경고)과 round6의 의존(UPDATE SET 목록 좌→우 평가 = 실제로 문서화된 동작)은 **종류가 실제로 다르다** — 이번 변경은 라벨 교체가 아니라 의존 등급의 실질적 하향이다. 파라미터 바인딩 순서·타입 어디에도 추가 미정의 의존은 남아 있지 않다. AC 체크박스 2건도 실측 근거와 함께 확정됐다.
- **Layer2 보안**: **통과**. 레이트리밋이 실제로 작동함을 **dev 테스트를 전혀 쓰지 않는 두 독립 경로**(JDBC 직결 1,200요청 + 단독 동시성 3회×3반복 = 5-way 경합 9회)로 확인했다 — 중복 발급 0건, 5xx 0건, 거부가 쿼터를 소비하지 않으며 일일상한 5를 넘지 않는다. 이번 라운드가 새로 연 공격 표면은 없다(변경분은 매퍼 SQL 본문·전용 `@Configuration`·테스트 스코프 surefire 프로퍼티뿐이고, 운영 동작은 보존됨을 실 기동으로 확인). **round5 Layer3이 지적한 "fail-open 방향의 미정의 동작"이 이번에 해소돼, 남은 위험은 전부 사람이 명시 유보했거나 후속 SR로 넘긴 것**(SR-294 당일 행 누적 · 자정 경계 쿨다운 미적용 · 400 메시지의 입력 원문 반사 · target 단위 상한이라는 설계 자체)뿐이다.
- **Layer3 회귀**: **통과**. FUNC-order-002 소유 파일(`ProductDao.java`·`mapper/product.xml`·`OrderService.java`·`OrderController.java`)은 수정시각이 전부 **09-07~09-09**로 round6 변경분(09-12 05:02~05:03)보다 앞서 있어 이번 라운드에 열리지 않았고, `OrderCreateQtyZeroRegressionTest`가 그대로 통과한다. `MemberSignupService.java`도 09-12 04:30(round5)에서 멈춰 있어 "서비스 코드는 손대지 않았다"는 dev 기록과 일치한다. `@EnableScheduling` 전역 제거의 부수 영향은 **앱 전체에 `@Scheduled` 1개·`@Async` 0건**이라 구조적으로 없으며, 운영 기동에서 그 1개가 여전히 예약됨을 실 로그로 확인했다(스코프 축소가 운영 기능을 끄지 않았다는 증명). `shop-api/pom.xml` surefire 변경은 테스트 JVM 한정이고 전체 스위트 259건 통과로 비간섭 확인. **round4가 냈던 datasource 전역 계약 회귀 같은 "한 FUNC이 남의 계약을 건드리는" 패턴은 이번 라운드에 없다.**

- 관찰(비차단 — 차단도 권고도 아닌 기록. 사람 결정으로 이미 유보됐거나, 지금 조치할 근거가 없는 항목):
  1. **[low/regression] 실패 방향은 여전히 fail-open이다 — 다만 안전망이 이미 있다.** MySQL 매뉴얼의 단일테이블 UPDATE 좌→우 평가 서술에는 "generally"라는 유보어가 있고 `ON DUPLICATE KEY UPDATE`에 대해 이를 다시 명시하지는 않는다. QA가 **SET 목록을 역순으로 쓴 문장**(=우→좌 평가를 충실히 시뮬레이션)으로 실패 방향을 직접 측정한 결과: 쿨다운 내 12연타는 우연히 정상(1허용 후 11거부)이지만, **쿨다운 경과 61초 간격 12연타는 12/12 허용에 `daily_count`가 1에 고정** — 즉 평가 순서가 뒤집히면 레이트리밋이 통째로 무력화된다. 다만 ① 랩 엔진·드라이버 4종에서 실측 안정적이고, ② 이 회귀는 `MemberSignupRateLimitDaoTest`의 기존 두 어서션(`afterCooldownPassed`의 `dailyCount==2`·`lastRequestedAt==second`, `atDailyLimit`의 `dailyCount==5`)이 **실제로 잡아낸다**(round5의 SET-순서 결함을 잡은 전례와 같은 어서션). 비용 0의 추가 경화가 가능하다는 점만 기록한다 — 서비스는 이미 `selectRateLimit`으로 `last_requested_at`을 함께 읽고 있으므로, `admitted` 조건에 `&& now.equals(rateLimit.getLastRequestedAt())`를 한 항 더하면 이 가설적 fail-open이 fail-closed로 바뀐다. **사람이 "이번이 마지막 검증"이라고 한 이슈는 round6로 닫혔다고 판단한다** — 이건 그 이슈의 잔존이 아니라 설계 자체의 성질에 대한 기록이다.
  2. **[low/spec] `spring.task.scheduling.enabled`는 공식 Boot 프로퍼티처럼 보이지만 아니다.** 이 키를 해석하는 것은 `MemberSignupSchedulingConfig`의 `@ConditionalOnProperty` 하나뿐인데, surefire가 이 값을 **모듈 전체 테스트 JVM**에 건다. 현재 앱의 `@Scheduled`가 이 FUNC 1개뿐이라 영향 반경은 0이고, 이 사실은 설정 클래스 javadoc과 `pom.xml` 주석 양쪽에 정확히 문서화돼 있다(dev가 공식 필드가 아님을 스스로 실측·명시한 점은 좋은 처리다). 다만 **다른 FUNC이 나중에 테스트에서 살아 있어야 하는 `@Scheduled`를 추가하면 이 모듈 전역 설정이 그것을 조용히 끈다** — 후속 추적(TODO)에 한 줄 남겨 두면 그때 추적된다.

**게이트 종결 판단**: round1 FAIL → round2 CONCERNS → round3 FAIL → round4 CONCERNS → round5 CONCERNS로 이어진 이 FUNC의 게이트 루프에서, round5까지 남아 있던 유일한 medium(레이트리밋 판정의 미정의 평가 순서 의존)이 이번에 **의존 등급이 실제로 내려가고 QA 독립 실측으로 확증**됐다. 3-Layer 모두 차단 이슈 없음, 완료 조건 2건 모두 QA 직접 재실행으로 충족, 사람 지시 3건 전부 이행. **이 FUNC의 QA 게이트는 여기서 마무리한다** — 남은 관찰 2건은 후속 TODO 성격이며 재작업 지시를 발행하지 않는다.

## 재작업 지시
> round 5 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/regression] 레이트리밋 판정 SQL이 같은 문장 안에서 사용자 변수 @msrl_admit을 대입하고 읽는다. MySQL/MariaDB 매뉴얼은 이 패턴의 평가 순서를 '정의되지 않음'으로 명시 경고한다(매퍼 주석은 이를 '문서화된 동작'이라 적어 부정확). QA가 MariaDB 11.4.5 + 드라이버 prepare 모드 4종(클라이언트/서버/캐시/벌크)에서 경계 시퀀스 7회를 각각 돌려 4종 모두 기대값 동일, 8스레드x150라운드(1,200요청) 동시성에서도 허용 정확히 1건/예외 0건으로 현재는 깨져 있지 않음. 그러나 평가 순서가 바뀌면 직전 문장의 잔존값(1)을 읽어 거부돼야 할 요청이 last_token을 덮어쓰는 fail-open으로 깨진다 — round4가 datasource 전역 설정 하나에 걸려 있던 것과 같은 종류의 취약함. → 사용자 변수를 제거하고 결정적 대안으로 교체: last_token을 SET 목록 맨 앞에서 갱신(조건식이 아직 미갱신인 last_requested_at/daily_count만 참조)하고, daily_count와 last_requested_at은 IF(last_token = #{token}, ...)로 방금 기록된 토큰 자체를 조건으로 삼는다 — UPDATE SET 목록의 좌→우 평가(실제로 문서화된 동작)만으로 결정적. 매퍼 주석의 '문서화된 동작' 서술도 정정.
2. [low/spec] round4 QA 권고7(@EnableScheduling 전역화 — 모든 @SpringBootTest 컨텍스트가 유지보수 스케줄러를 띄우고 initialDelay=10분 불변식이 주석으로만 보장됨)이 TODO에도 결정 기록에도 남지 않았다. 사람 코멘트가 유보 대상 low를 3건만 열거해 dev는 지시를 정확히 따랐으나 결과적으로 QA 지적 1건이 추적에서 소실됐다. → STORY 후속 추적(TODO)에 한 줄 추가하거나 '고치지 않기로 함'을 명시 기록
3. [low/spec] STORY 수용 기준 체크박스 2건이 5라운드째 미체크. FUNC 상태 정본이 STORY frontmatter + GATE이므로 미체크 AC가 남으면 커버리지 집계가 이 FUNC을 미완으로 읽는다. → SR 종결 전 AC 2건을 확정(충족 체크 또는 범위 밖 명시)
4. [low/regression] 요청당 DB 왕복이 3회(touchRateLimit UPSERT → selectRateLimit 재조회 → writeCode)로 round4 대비 1회 증가. 무인증 엔드포인트라 기록. 또한 touchRateLimit이 쿼터를 소비한 뒤 writeCode가 실패하면(500 MBR-5000) 코드는 못 받고 쿼터만 1 소모된다. → PK 조회라 비용은 작고 판정 정확성과의 교환으로 타당 — 현재는 기록만. 부하가 문제되면 판정과 코드 기록을 한 문장으로 합치는 설계를 검토

사람 코멘트: [개발자 결정] medium 1건 지금 고친다 — fail-open 방향의 미정의 동작은 지금 안 터져도 두지 않는다.

QA 대안 그대로 적용: 세션 변수를 제거하고, UPDATE의 SET 목록 맨 앞에서
  last_token = IF(last_requested_at <= NOW(3) - INTERVAL 60 SECOND AND daily_count < 5, ?, last_token)
을 두고, 이어지는 last_requested_at · daily_count는
  IF(last_token = ?, NOW(3)/daily_count+1, 기존값)
로 방금 그 문장에서 기록된 자기 토큰만 조건으로 삼는다(문서화된 UPDATE SET 목록의 좌→우 평가만 의존 — 사용자 변수의 읽기/쓰기 순서에는 의존하지 않는다). 매퍼 주석의 잘못된 서술("문서화된 동작")은 지운다.

같은 김에 low 중 @EnableScheduling은 애플리케이션 전역이 아니라 레이트리밋 정리 스케줄러 설정 클래스에만 두고, 테스트 컨텍스트에서는 spring.task.scheduling 비활성 프로퍼티로 막는다.

AC 체크박스는 실측 근거로 체크한다. DB 왕복 3회·writeCode 실패 시 쿼터 소모 이슈는 후속 TODO로만 남긴다.

완료 조건: 동시성 테스트 단독 3회 연속 통과 + mvnw test 전체 통과 결과를 STORY Dev 기록에 남긴다.
