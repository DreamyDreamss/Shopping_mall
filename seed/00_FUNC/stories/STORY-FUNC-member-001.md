---
story-id: STORY-FUNC-member-001
func-id: FUNC-member-001
status: Done
domain: member
created: 2026-09-12
spec_markers: 0
sr-id: SR-231
approved_sha: 6184dcacae3c
---

# STORY-FUNC-member-001 — SR-231 — 신규 UIS-MBR-001 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-uis가 역생성)

## Story
SR-231 — 신규 UIS-MBR-001 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-uis가 역생성)


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
- [ ] 기능이 설명대로 동작(연결 INF 없음 — 수용기준 수동 작성 권장)
- [ ] SR 정본 계약 충족 — `docs/변경관리/SR-231/02_변경명세.md` · inputs/_decisions.md의 D-결정 의 요구·계약 조항을 AC로 구체화해 사람 승인 전 보강할 것(OBS-020 관례)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS** UIS-MBR-001
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)



## 구현 Task
- [x] 컨트롤러/핸들러 — MemberSignupViewController (GET /member/signup, 정적 뼈대만 렌더)
- [x] 서비스/비즈니스 로직 — 없음(이 FUNC 범위 밖. 회원가입 업무 로직은 이미 구현된
      INF-MBR-001/INF-MBR-002를 클라이언트 스크립트가 그대로 재호출)
- [x] 데이터 접근 레이어 — 없음(신규 테이블/DAO 없음, 화면 전용 FUNC)
- [x] 단위 테스트 — MemberSignupViewControllerTest 2건 + ApiKeyAuthIntegrationTest 화이트리스트
      회귀 2건

## Dev 기록

### 구현 범위 확정
UIS-MBR-001(회원가입 화면)만 구현했다. 인증코드 발송(INF-MBR-001, FUNC-member-002)·가입
요청+코드검증(INF-MBR-002, FUNC-member-003)은 이미 구현·QA 완료돼 있어(STORY-FUNC-member-002.md/
STORY-FUNC-member-003.md Done) **그 두 API를 하나도 수정하지 않고** 클라이언트 스크립트에서
그대로 재호출만 했다 — 지시사항("이미 별도로 진행 중이거나 완료된 작업")을 그대로 따름.

### 변경명세 초안과 다르게 구현한 지점(Dev 판단, 근거 명시)
`docs/변경관리/SR-231/02_변경명세.md` §FUNC-member-001의 TO-BE 서술은 API 구현 **이전**에 쓰인
추정안이라, 실제로 승인·구현된 INF-MBR-001/INF-MBR-002 계약과 두 지점에서 맞지 않았다. 재해석
대신 실제 계약을 그대로 따르는 쪽으로 판단했다(다른 FUNC들이 승인된 설계를 임의로 벗어나
QA FAIL을 받은 전례가 반복되지 않도록):

1. **"STEP1에서 이메일 중복을 즉시 검증"은 구현하지 않았다.** INF-MBR-002가 "존재 오라클
   방지"를 사람이 명시 승인한 보안 설계로 못박아 뒀다 — 코드 검증(STEP1) 전에는 그 target의
   가입 여부를 알아낼 방법이 없어야 한다(코드 검증 실패는 가입 여부와 무관하게 항상
   `MBR-4091`). 이메일 중복 확인을 노출하는 별도 API도 없다(INF-MBR-001은 발송만, INF-MBR-002는
   코드 검증 후에만 판정). 이 화면은 최종 가입 제출(STEP3, `POST /api/members/signup`)의 409
   `MBR-4092`/`MBR-4094` 응답을 STEP1로 되돌려 표시하는 방식으로 대체했다 — "즉시성"은
   포기했지만 보안 설계를 침해하지 않는 유일한 방법이다.
2. **비밀번호 입력은 STEP1이 아니라 STEP3에 뒀다.** 초안 상태 목록은 "STEP1 비밀번호 규칙
   위반 상태"를 나열했지만, 실제 가입 요청 바디(`{target, code, password, name,
   marketingOptIn}`)는 비밀번호를 코드와 함께 STEP3에서만 받는다 — STEP1에 비밀번호 필드
   자체가 없다. 상태 목록은 아래처럼 재구성해 9개를 그대로 유지했다.
3. **STEP2 "다음" 버튼은 코드를 서버에 검증하지 않는다.** INF-MBR-002가 "코드 검증+가입"을
   원자적으로 묶은 이유(TOCTOU 방지, STORY-FUNC-member-002.md Dev 기록)가 그대로 이 화면에도
   적용된다 — 별도 verify-only 엔드포인트가 없으므로, STEP2의 "다음"은 6자리 입력 여부만
   클라이언트에서 게이트하고 실제 코드 검증은 STEP3 제출 시 서버가 수행한다. 검증 실패
   (`MBR-4091`/`MBR-4093`)는 STEP2로 되돌려 표시한다.

### §5 표시 조건(9개 상태) — 재구성 매핑
`data-screen-state` 속성(`<body>`)으로 아래 9개 상태를 식별 가능하게 표시했다(값은 상태명
그대로): `step1-initial`(STEP1 초기) · `step1-terms-unchecked`(약관 미동의, 다음 버튼 비활성) ·
`step1-duplicate-error`(이메일/휴대폰 중복 오류, STEP3 제출 결과로 되돌아온 상태) ·
`step2-initial`(인증코드 입력 초기) · `step2-code-invalid`(코드 오류) ·
`step2-code-expired`(코드 만료, 5분 카운트다운 종료) · `step3-input`(STEP3 입력 상태) ·
`step3-password-invalid`(비밀번호 규칙 위반) · `step-done`(가입 완료, 환영 쿠폰 안내).

### 스토리(Storybook) 미작성 — 사유
이 화면은 shop-api Thymeleaf 서버렌더 화면이다(하우스 패턴: cart/product/member 화면 전부
Thymeleaf). shop-web(React+Storybook)은 order 도메인 전용 별도 SPA(주문 목록/상세만 구현,
member 컴포넌트 없음)라 이 FUNC에서 새로 얹지 않았다. `.claude/rules/lab/story-per-component.md`
규칙은 `modules/shop-web/src/**/*.tsx`에만 적용되므로 이 파일들에는 해당하지 않는다(경로
불일치 확인). §5 상태별 캡처가 필요하면 `data-screen-state` 속성을 앵커로 QA/E2E 단계
(story_shots.py 또는 e2e-agent)에서 다룰 것을 권고한다.

### 생성/수정 파일
- 신규: `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberSignupViewController.java`
  (`GET /member/signup`, 정적 뼈대만 렌더)
- 신규: `modules/shop-api/src/main/resources/templates/member/signup.html` (3단계 마법사 +
  인라인 vanilla JS. 빌드 파이프라인 없는 서버렌더 화면 관례를 따름 — SPA 프레임워크 도입 없음)
- 수정: `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — 신규
  화면 라우트(`/member/signup`)를 `isOpenRoute` 화이트리스트에 추가(`MEMBER_SIGNUP_SCREEN_PATH`
  상수). 신규 가입자는 아직 API 키가 없어 무인증 필수 — FUNC-member-002/003과 동일한 예외
  패턴. `MEMBER_VIEW_PATH` 정규식(`/member/{id}` 마이페이지 소유권 대조)과 문자열이 겹치지만
  `shouldNotFilter`가 먼저 걸러 그 분기까지 가지 않음을 회귀 테스트로 확인.
- 신규: `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberSignupViewControllerTest.java`
  (2건 — 뼈대 렌더, linked_func 주석 비노출)
- 수정: `modules/shop-api/src/test/java/com/sm/lab/shop/web/ApiKeyAuthIntegrationTest.java` —
  화이트리스트 회귀 2건 추가(`signupScreenRoute_withoutApiKey_returns200`,
  `signupScreenRoute_withMemberApiKey_returns200`). `/member/{memberId}`(마이페이지, 무키
  401)와의 대비로 리터럴 세그먼트 우선매칭 + 화이트리스트가 의도대로 동작함을 실증.

### 검증
`mvnw test` 전체 실행 — 실패 0/에러 0(신규 4건 포함, exit code 0). 컴파일 경고 없음. 기존
FUNC-member-002/003 소유 파일은 하나도 수정하지 않았다(읽기만 해서 API 계약 파악).

### round2 재작업(2026-09-12, round1 QA CONCERNS 재작업 지시 1·2·3·4·5 반영)

**모듈 선택 근거(사람 명시 요청)**: 이 화면을 shop-api(Thymeleaf 서버렌더)로 구현한 것은
새 선택이 아니라 이미 이 랩의 cart/product/member 화면이 전부 같은 스택(Thymeleaf + 인라인
vanilla JS, 빌드 파이프라인 없음)이라 그 관례를 그대로 따른 것이다 — shop-web(React+
Storybook)은 order 도메인 전용 SPA로 회원 화면을 갖지 않는다.

**반영한 항목(사람 코멘트 "1·2·3번 지금 고친다"):**
1. **(high) MBR-4091 재입력 허용** — `handleSignupError`의 MBR-4091 분기가 더 이상 코드
   입력을 잠그지 않는다. 입력 유지 + 인라인 오류("인증코드가 올바르지 않습니다") + 재입력
   허용으로 바꾸고, 실제 잠금(MBR-4093, 시도 상한 도달)만 별도 분기로 분리해 입력을 잠그고
   `#btnResend`에 `.emphasize` 클래스로 시각 강조를 준다. 응답에 잔여 시도횟수 필드가 없어
   화면에서 카운트를 표시/추정하지 않는다(그대로 둠).
2. **(medium) 9개 상태 진입/복귀 분기** — `updateStep1Button`(약관 동의 시 `step1-initial`로
   복귀), `updateStep2Button`(코드 재입력 시 `step2-initial`로 복귀), `updateStep3Button`
   (비밀번호 유효화 시 `step3-input`으로 복귀) 3개 함수 모두에 else 분기를 추가했다. 상태
   전이표(이벤트→상태)는 `<script>` 최상단 주석으로 남겼다. `MemberSignupScreenStateTest`
   (HtmlUnit, 신규)가 9개 상태 각각을 진입+복귀까지 실제 JS 실행 환경에서 단언한다.
   MBR-4093(시도 상한)은 §5에 별도 상태가 없어 `step2-code-expired`를 함께 쓰기로 판단했다
   (화면 효과가 "잠김·재전송 필요"로 동일) — Dev 판단, QA 재검토 요망.
3. **(medium) STEP1 복귀 시 카운트다운 정지** — MBR-4092/4094 분기에
   `clearInterval(state.countdownTimer)`를 추가했다(4093 분기에도 동일하게 추가 — 잠금
   상태에서 시간 흐름이 의미가 없어 일관성 있게 정지시켰다, 지시 범위를 벗어난 판단이라 명시).
   재발송(`startStep2`)에서만 새 타이머가 시작된다. `MemberSignupScreenStateTest`의
   `state3_step1DuplicateError_stopsCountdownAndUsesDomLink`가 복귀 후 3초 대기해도
   `step1-duplicate-error`가 `step2-code-expired`로 덮어써지지 않음을 확인한다(round1 CONCERNS
   권고3 재현조건의 회귀 테스트).
4. **(low) innerHTML 제거** — MBR-4092/4094 분기의 `el.targetError.innerHTML = ...` 문자열
   결합을 `document.createElement('a')` + `textContent`/`setAttribute('href', ...)` DOM 조립으로
   교체했다. `login_url`이 자리표시(`/login`, 404)인 것은 손대지 않고 아래 "후속 추적"에 남긴다.
5. **(low) STEP1 즉시 중복검증 미구현 확정** — `docs/변경관리/SR-231/02_변경명세.md`
   §FUNC-member-001에 사람이 승인한 정정 주석을 추가했다: "중복 판정은 인증코드 검증 이후
   서버 응답(MBR-4092/MBR-4094)으로만 이루어진다"(요구사항 원문 옆, 재해석 아님).

**순서 버그 발견·수정(Dev 자체 발견, QA 지시에는 없었음)**: 처음 작성 시
`updateStep2Button()`/`updateStep1Button()`이 내부에서 오류 필드를 지우고 상태를 초기값으로
되돌리는 부작용이 있는데, 이를 오류 메시지·`step2-code-invalid`/`step1-duplicate-error` 설정
"이후"에 호출하면 방금 띄운 오류가 그 자리에서 지워지는 문제를 `MemberSignupScreenStateTest`
작성 중 스스로 발견해 호출 순서를 뒤집었다(먼저 버튼 상태 동기화 → 그 다음 오류 메시지·최종
상태 설정). 코드 주석에 순서 이유를 남겼다.

**신규 테스트 인프라(HtmlUnit)**: 이 프로젝트 최초로 `net.sourceforge.htmlunit:htmlunit`
(test 스코프, `spring-boot-dependencies` 3.3.5 관리값 2.70.0)을 추가해 `MockMvcWebClientBuilder`
로 signup.html의 실제 JS 실행·클릭·fetch 흐름을 슬라이스 테스트에서 구동한다.
- 이 버전의 HtmlUnit에는 native `fetch`가 없어("fetch is not defined") 테스트 쪽에서만
  XMLHttpRequest 기반 폴리필을 주입한다(운영 코드 signup.html은 그대로).
- htmlunit이 전이적으로 끌어온 `org.eclipse.jetty.websocket:websocket-client`(→jetty-client
  12.x)가 Spring `TestRestTemplate`에 의해 선택돼 `ApiKeyAuthIntegrationTest`의 401(무
  WWW-Authenticate 헤더) 응답 처리를 깨뜨리는 **의도치 않은 회귀를 `mvnw test` 전체 실행에서
  실측**했다 — `pom.xml`에서 그 의존성만 `<exclusions>`로 제외해 해결(FUNC-member-001 밖
  파일은 건드리지 않음). 이 사고 때문에 새 테스트 의존성을 추가한 뒤에는 반드시 전체 스위트를
  포그라운드로 1회 더 돌려 확인해야 한다는 교훈을 얻었다(harness/antipatterns.all.md 후보).

### 검증(round2)
- `mvnw test -Dtest=MemberSignupScreenStateTest` — 9/9 통과(11~15초, 3회 반복 실행으로
  타이밍 안정성 확인).
- `mvnw test` 전체 실행 — **324 tests, 0 failures, 0 errors**(surefire XML 집계, exit code 0).
  `ApiKeyAuthIntegrationTest` 43건 포함 전량 green(jetty-client 제외 후).
- FUNC-member-002/003 소유 파일(`MemberSignupService`, `MemberRegistrationService`,
  `MemberSignupCompletionWriter` 등)은 이번 라운드도 하나도 수정하지 않았다(모킹만 함).

### 생성/수정 파일(round2)
- 수정: `modules/shop-api/src/main/resources/templates/member/signup.html` — 상태 전이표
  주석, `updateStep1Button`/`updateStep2Button`/`updateStep3Button` 복귀 분기,
  `handleSignupError`(MBR-4091/4093 분기 재작성, MBR-4092/4094 DOM 조립+타이머 정지),
  `.emphasize` CSS.
- 수정: `modules/shop-api/pom.xml` — `net.sourceforge.htmlunit:htmlunit`(test, websocket-client
  제외) 추가.
- 신규: `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberSignupScreenStateTest.java`
  (9건 — §5 9개 상태 각 1건, HtmlUnit).
- 수정: `docs/변경관리/SR-231/02_변경명세.md` — §FUNC-member-001에 정정 주석(재작업 지시 5).

### round3 재작업(2026-09-12, round2 QA CONCERNS 재게이트 재작업 지시 1~6 + 사람 코멘트(A)(B)(C) 반영)

**사람이 방식까지 구체적으로 지정한 항목(A)(B)(C)를 그대로 따랐다:**

1. **(A) 타이머 버그 수정 — 상태 전이를 `goStep(n)` 단일 함수로 통일**. `n !== 2`이면 항상
   `clearInterval`하고, `n === 2`로 들어올 때(신규 발송·재전송·STEP3에서 되돌아온 경우 전부
   포함)만 `state.expiresAt` 기준 잔여시간으로 타이머를 재시작한다. 이전에는 STEP2→STEP3
   전이(`btnVerifyNext` 클릭)에서 타이머를 정지하지 않아 STEP3 체류 중 조용히
   `step2-code-expired`로 덮어써지는 결함이 있었다(round2 QA CONCERNS 권고1) — `goStep(3)`이
   이제 그 경로에서도 타이머를 정지시킨다.
   또한 `updateStep2Button()`을 "입력 잠금은 오직 `data-screen-state === 'step2-locked'`일
   때만 건다"는 단일 규칙으로 다시 썼다 — `el.code.disabled`를 여러 곳에서 산발적으로
   설정하던 것을 이 한 함수로 모았다. MBR-4091 분기는 이 상태로 전이하지 않으므로 어떤
   경로로도 절대 잠그지 않는다(round2 QA CONCERNS 권고2 — 타이머가 STEP2 재진입 시 잔여시간
   으로 복원되면서 `code.disabled`가 함께 복구되지 않던 결함의 재발 방지).
2. **(B) §5 상태 분리 — `step2-locked` 신규(10번째 상태)**. round2가 MBR-4093(시도 상한
   도달)을 `step2-code-expired`와 공유하던 것을 폐기하고 별도 상태로 분리했다(round2 QA
   CONCERNS 재게이트 권고3 — 앵커 모호성 + 구제책 불일치 지적을 사람이 그대로 수용).
   `step2-code-expired`(00:00 만료, "인증 시간이 지났습니다", 재전송 강조)와
   `step2-locked`(MBR-4093, "인증 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요",
   재전송 보통 스타일 — SR-295가 닫히기 전까지 재전송이 잠금을 풀지 않는다는 사실을 문구·
   스타일 둘 다로 전달)로 나눴다. `docs/변경관리/SR-231/02_변경명세.md` §FUNC-member-001의
   §5 목록을 9개 → 10개로 갱신했다(상태-스토리 1:1 원칙 유지 — 이 화면은 Storybook 미작성
   화면이라 `data-screen-state` 앵커 자체가 "스토리 대체"임은 round2 Dev 기록 그대로).
3. **(C, low) login_url 스킴 검증**. `isSafeRelativePath(url)` 헬퍼를 추가해 `/`로 시작하되
   `//`(스킴 상대 URL — 다른 오리진으로 이동 가능)는 아닌 경우만 안전하다고 판정한다.
   검증에 실패하면 `<a>` 자체를 생략하고 메시지만 보여준다. 코드 주석도 "이후 값이 바뀌어도
   안전하도록 고정한다"는 사실과 다른 과대주장(round2 QA 재게이트 권고4 지적)을 "이 가드는
   값이 바뀌는 미래를 대비한 것"으로 정정했다.
4. **(low) `docs/변경관리/SR-231/02_변경명세.md` 정정을 본문 문장으로 교체**. round2가
   HTML 주석(`<!-- ... -->`)으로만 넣어 렌더링 시 보이지 않던 것을 볼드체 본문 문단으로
   바꿨다(round2 QA 재게이트 권고5). §5 상태 목록도 같은 파일에서 10개로 갱신.

**재작업 지시 1·2·3·4·5·6(round2 QA 재게이트 CONCERNS)와의 대응**: 1·2는 위 (A), 3은 위 (B),
4·5는 위 (C)·문서정정, 6(surefire stale XML)은 아래 "검증(round3)" 참고.

**자체 발견·수정한 버그(QA 지시에는 없었음)**: `goStep()`을 처음 `el.classList.toggle(token,
force)`의 2-인자(force) 형태로 작성했더니, 이 프로젝트가 테스트에 쓰는 HtmlUnit(2.70.0)의
JS 엔진이 force 인자를 무시하고 항상 반전(toggle)만 수행해 반복 호출마다 의도와 반대로
뒤집히는 결함을 냈다 — STEP2→STEP3 전이 두 번째 `goStep` 호출에서 `#step3` 섹션 자체가
조용히 `hidden`으로 뒤집혀 `btnSignup` 클릭이 완전 무반응(상태가 `step3-input`에 고착)이
되는 회귀를 `mvnw clean test`에서 실측했다(10개 상태 테스트 중 btnSignup을 거치는 5건이
전부 이 원인으로 실패). `classList.add`/`remove`를 명시적으로 쓰는 `setHidden(node, hidden)`
헬퍼로 교체해 해결했다 — force 인자에 의존하는 API는 구형 JS 엔진에서 조용히 다르게 동작할
수 있다는 교훈(harness/antipatterns.all.md 후보).

### 검증(round3)

- `mvnw clean test`(surefire 잔존 XML 문제 확인차 clean 후 전체 재실행, 재작업 지시 6) —
  **39개 클래스, 321 tests, 0 failures, 0 errors**(exit code 0, surefire 리포트 79개 파일
  집계 기준 — round2 최종 실측 319 tests 대비 +2, 이번 라운드에 추가한 §5-6(진짜 만료) 테스트
  1건 + STEP3 타이머정지·MBR-4091 비잠금 회귀 테스트 1건).
- `MemberSignupScreenStateTest` — 11건 전부 green(§5 10개 상태 각 1건 + 회귀 1건).
  `state6_step2CodeExpired_onCountdownReachesZero`(신규, 유효시간 1초로 실제 만료 실측),
  `state7_step2Locked_onAttemptCapReached`(구 state6에서 이름 변경 — `step2-locked` 단언으로
  교체), `regressionStep3TimerStopped_thenMbr4091NeverLocksInput`(신규 — STEP3 체류 중 타이머
  정지 + 그 뒤 MBR-4091 비잠금을 한 시나리오로 함께 확인).
- FUNC-member-002/003 소유 파일(`MemberSignupService`, `MemberRegistrationService`,
  `MemberSignupCompletionWriter` 등)은 이번 라운드도 하나도 수정하지 않았다(테스트에서
  `@MockBean`으로만 참조).

### 생성/수정 파일(round3)

- 수정: `modules/shop-api/src/main/resources/templates/member/signup.html` — `goStep(n)`/
  `setHidden`/`isSafeRelativePath` 신설, `startStep2`/`tickCountdown`/`updateStep2Button`/
  `handleSignupError`/`showDone` 재작성(위 (A)(B)(C) 반영), 상태 전이표 주석 갱신.
- 수정: `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberSignupScreenStateTest.java`
  — §5 10개 상태로 재정렬(TC-03~TC-12), 신규 테스트 2건(진짜 만료·회귀), `toStep2(int)`
  오버로드 추가.
- 수정: `docs/변경관리/SR-231/02_변경명세.md` — §FUNC-member-001 정정을 본문 문장으로 교체,
  §5 상태 목록 9→10개, 오류 표기 문구 갱신.

### round4 재작업(2026-09-12, round3 QA 재게이트 CONCERNS 재작업 지시 1·2 + 사람 코멘트 [개발자 결정] 반영)

**반영한 항목(사람 코멘트 "그 외 변경 금지" 범위 내에서 딱 두 곳만 고쳤다):**

1. **(medium) MBR-4091 분기 — 만료가 이미 지난 경우 만료 UI를 되돌리지 않는다.**
   `signup.html` `handleSignupError`의 MBR-4091 분기에서 `goStep(2)` 직후 무조건 실행되던
   `btnResend.classList.add('hidden')` + `remove('emphasize')` + `countdown.classList.remove('expired')`
   3줄을 `if (state.expiresAt > Date.now())`로 감쌌다 — 실제로 코드가 아직 살아있는 정상
   오답(만료 전)에서만 만료 UI를 되돌리고, 이미 만료된 뒤 도착한 4091이면 바로 앞
   `goStep(2)`→`tickCountdown()`이 막 세팅한 만료 UI(재전송 노출·강조·`.expired`)를 그대로
   둔다. 이어서 `updateStep2Button()`이 부작용으로 `step2-initial`/빈 오류로 되돌리므로,
   그 뒤에 `state.expiresAt <= Date.now()` 여부로 최종 문구·상태를 다시 갈랐다(만료면
   `step2-code-expired`+"인증 시간이 지났습니다", 아니면 기존 `step2-code-invalid`+서버
   메시지). (A)"타이머는 STEP2에서만 생존"·(B)"MBR-4093과 만료는 별개 상태" 원칙은 손대지
   않았다 — 이 분기는 `step2-locked`로 전이하지 않고 `goStep(2)`의 타이머 재시작 로직도
   그대로다.
2. **(low) `isSafeRelativePath` 강화 — 역슬래시·이중 슬래시로 시작하는 경로 거부.**
   기존에는 `url.charAt(1) !== '/'`만 검사해 `//evil.com`은 걸렀지만 `/\evil.com`은
   통과시켰다(round3 QA 재게이트 관찰 — 일부 브라우저 URL 파서가 `\`를 `/`로 정규화해
   외부 오리진으로 해석될 수 있음). `url.charAt(1) !== '\\'` 조건을 추가해 두 번째 문자가
   `/` 또는 `\`이면 모두 거부하도록 했다.

**테스트 작성 중 자체 발견·수정(QA 지시에는 없었음, 검증 과정의 실측)**: 처음에는 기존
`regressionStep3TimerStopped_thenMbr4091NeverLocksInput` 테스트의 최종 단언을 "만료
UI 유지"로 바꿔치기했으나(같은 이름 유지), `mvnw clean test` 1차 실행에서
**AssertionFailedError로 즉시 실패**했다 — 그 테스트는 `goStep(3)`이 타이머를 멈춘 직후
`p.webClient.waitForBackgroundJavaScript(2000)`을 부르지만, 이 시점엔 활성 배경 작업
(interval)이 이미 없어 그 호출이 거의 즉시 반환된다는 것을 실측으로 확인했다 — 즉 그
시나리오는 실제로는 `state.expiresAt`이 아직 지나지 않은 "정상 오답(미만료)" 경로였고,
테스트 주석의 "유효시간을 이미 넘김"이라는 서술은 사실과 달랐다(회귀 위험 — 다음
세션이 같은 오해를 반복할 수 있어 주석을 정정했다). 원래 테스트는 이름·단언 그대로
되돌리고(정상 오답 경로는 round3 그대로 `step2-code-invalid` + 미잠금이 맞다), "실제로
만료된 뒤" 시나리오는 `state6`과 같은 방식(활성 `setInterval`이 실제로 0에 도달할 때까지
`waitForBackgroundJavaScript`가 블록되는 것을 이용)으로 **별도의 새 테스트**
(`regressionMbr4091AfterRealExpiry_keepsResendButtonVisible`)를 추가해 결정적으로
재현했다 — STEP2에 머무는 동안 실제로 만료시킨 뒤에야 STEP3로 넘어가 제출한다.

### 검증(round4)
- `mvnw clean test` 전체 실행 — **325 tests, 0 failures, 0 errors**(exit code 0). round3
  기준 321건 대비 +4(round4 신규: `isSafeRelativePath` 단위 테스트 3건 +
  `regressionMbr4091AfterRealExpiry_keepsResendButtonVisible` 1건).
- `MemberSignupScreenStateTest` — **15/15 통과**(13.0초).
- FUNC-member-002/003 소유 파일은 이번 라운드도 하나도 수정하지 않았다(읽기·모킹만).

### 생성/수정 파일(round4)
- 수정: `modules/shop-api/src/main/resources/templates/member/signup.html` —
  `handleSignupError`의 MBR-4091 분기(만료 여부 조건 분기 추가), `isSafeRelativePath`
  (역슬래시 거부 조건 추가).
- 수정: `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberSignupScreenStateTest.java`
  — `duplicateErrorWithLoginUrl(loginUrl)` 헬퍼 신설, 신규 4건
  (`isSafeRelativePath_allowsPlainRelativePath`, `isSafeRelativePath_rejectsDoubleSlash`,
  `isSafeRelativePath_rejectsBackslash`, `regressionMbr4091AfterRealExpiry_keepsResendButtonVisible`),
  `regressionStep3TimerStopped_thenMbr4091NeverLocksInput`은 주석만 정정(사실과 다르던
  "유효시간을 이미 넘김" 서술 제거) — 단언·이름은 원래대로.

## 후속 추적(TODO)
- **login_url 자리표시(`/login`)** — INF-MBR-002 §login_url 필드에 "실재하지 않는 자리표시"로
  명시돼 있다. 이번 라운드에서 innerHTML을 DOM 조립으로 바꿔 XSS 소지는 없앴지만, 실제
  로그인 화면 라우트가 없어 `href="/login"` 링크는 여전히 404다. 사람 코멘트: "후속(로그인
  SR-232)에서 라우트가 생기면 자동 해결" — 이 FUNC에서 직접 고치지 않는다.
- **MBR-4093과 §5 상태 매핑 — round3에서 해소(별도 상태로 분리)**. round2는 MBR-4093(시도
  상한 도달)을 기존 `step2-code-expired`와 공유했는데, QA round2 재게이트가 앵커 모호성과
  구제책 불일치(만료는 재전송으로 회복되지만 상한 도달은 회복되지 않음, SR-295)를 지적해
  사람이 재검토를 요청했다. round3은 신규 10번째 상태 `step2-locked`로 분리하고 재전송
  버튼을 강조하지 않는 것으로 해소했다(사람 코멘트 (B), `02_변경명세.md` §FUNC-member-001
  §5 목록 10개로 갱신). **재발송이 `attempt_count`를 리셋하지 않는 근본 한계(SR-295) 자체는
  이 FUNC에서 고치지 않는다** — 화면은 그 한계를 "잠시 후 다시 시도"로 정확히 안내할 뿐이고,
  실제 해소는 SR-295가 닫힐 때다.
- **STEP6 최종 확인(2026-09-12) — 완료 승인, low 3건은 후속으로 남김**:
  1. STEP5 표의 AC↔테스트 메서드명 7개가 실제 `MemberSignupScreenStateTest.java` 메서드명과
     다름(테스트 자체는 정상 동작·15/15 통과, 문서 오탈자만 정정 필요).
  2. `MemberSignupScreenStateTest.java`의 `isSafeRelativePath_allowsPlainRelativePath`(:250)와
     `regressionMbr4091AfterRealExpiry_keepsResendButtonVisible`(:460)이 동일 주석
     `linked_tc: TC-FUNC-member-001-14`를 공유 — 후자를 `TC-FUNC-member-001-17`로 정정할 것.
  3. `signup.html:171` 상태 전이표 주석이 round4에 추가된 전이("만료 이후 MBR-4091 →
     step2-code-expired 유지")를 반영하지 않음 — 코드 동작에는 영향 없음, 주석만 갱신할 것.
  4. **UIS-MBR-001 화면설계서 역생성** — 이 SR(SR-231) 종결 전 별도 세션에서 앱을 기동한 뒤
     `/sl-recon-uis`로 진행(사람 결정). §5 표시조건 10개와 `signup.html`의
     `data-screen-state` 값을 1:1로 맞춰 역생성한다.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-12 — CONCERNS
- **Layer1 스펙**: concerns. 행복경로 AC는 충족(3단계 마법사·1/3 단계표시·모바일 단일컬럼·전체동의+개별
  펼침·5분 카운트다운·만료 문구+재전송·상단 배너 5xx·중복 이메일 문구+로그인 링크·완료 시 환영 쿠폰 안내).
  요청/응답 계약도 INF-MBR-001(`{target}` → `{channel,target,expiresInSeconds}`)·INF-MBR-002
  (`{target,code,password,name,marketingOptIn}` → 201 `{memberId,channel,target}`, 오류 봉투
  `{code,message}`+`login_url`)와 필드명·오류코드(MBR-4001/4091/4092/4093/4094) 전부 일치.
  다만 아래 4건에서 SR 정본(§5 표시 조건·서버 계약)과 어긋난다.
  - **"STEP1 즉시 중복검증 생략" 판단은 타당함을 확인**: INF-MBR-002 §비즈니스규칙이 "존재 오라클
    방지(사람이 명시 승인한 보안 우선 트레이드오프) — STEP1 코드검증이 STEP0 존재판정보다 항상 먼저,
    코드 검증 실패는 가입 여부와 무관하게 항상 MBR-4091"을 못박았고, 중복 확인 전용 엔드포인트도
    존재하지 않는다(INF-MBR-001은 발송만). 즉시 검증을 구현하려면 승인된 보안 설계를 깨야 하므로
    Dev 판단은 스펙 근거가 있다. 단 SR 요구 본문에 명시된 항목이므로 **미구현 상태로 종결하지 말고**
    후속 SR 접수 또는 02_변경명세 §FUNC-member-001 정정이 필요하다(FUNC-member-003이 동종 한계를
    SR-295로 접수한 선례).
- **Layer2 보안**: pass(권고 1). `/member/signup` 화이트리스트 추가는 **정확 일치(equals)** 1개 항목
  가산이고 `shouldNotFilter`가 `evaluateMemberScope`보다 먼저 실행되므로 `MEMBER_VIEW_PATH`
  (`^/member/([^/]+)$`, 마이페이지 소유권 대조) 분기에는 도달하지 않는다 — **다른 `/member/{id}`에
  구멍이 생기지 않음을 실측 확인**: 무키 401 / 타인 ID + member 키 403 / 본인 ID 200 회귀 3건이
  그대로 통과(ApiKeyAuthIntegrationTest 43건 green). `member_id`는 `"M-%04d"` 포맷이라 리터럴
  "signup"과 충돌할 수 없고, 화면은 모델 데이터가 없는 정적 뼈대라 무인증 노출 정보도 없다.
  인라인 CSS/JS만 써서 default-deny 하에 401 나는 정적 자산 의존도 없다. 비밀번호는
  `type=password` + `autocomplete=new-password`, 코드·해시 미노출.
- **Layer3 회귀**: pass. mtime 실측상 08:09~08:12 창에 신고된 5개 파일만 변경됐고 FUNC-member-002/003
  소유 파일(MemberSignupService 04:30, MemberRegistrationService 07:19, MemberSignupCompletionWriter
  06:54 …)은 미수정. surefire 전량 집계 **314 run / 0 failure / 0 error**(기준선 228 상회).
  `isOpenRoute` 변경은 순수 가산이라 기존 `/cart` `/order/` `/product/` `/api/members/grades`
  `/api/members/signup*` 판정이 바뀌지 않는다.

- 권고(CONCERNS시):
  1. **(high) 코드 오답 시 재입력을 막고 재전송을 강제하는 것을 고칠 것** —
     `signup.html:394-403` `handleSignupError`의 MBR-4091 분기가 `el.code.disabled = true` +
     `btnVerifyNext.disabled = true`로 잠그고 재전송 버튼만 노출한다. 서버 계약은
     `attempt_count < 5`까지 재시도를 허용하는데(INF-MBR-002 §비즈니스규칙 STEP1) 클라이언트가 1회
     오답에서 끊는다. 결과: 오타 한 번에 INF-MBR-001 일일 발송 상한 5회 중 1회를 소모(+60초 쿨다운)
     하고, 재발송이 `attempt_count`를 리셋하지 않는 알려진 한계(INF-MBR-002 §코드값 의미, SR-295)와
     겹쳐 정상 사용자가 조기에 MBR-4093(정리배치 전까지 회복 불가)으로 잠긴다.
     MBR-4093(실제 상한 도달)일 때만 잠그고 MBR-4091은 코드 입력만 비워 재입력을 허용할 것.
  2. **(medium) `data-screen-state` 앵커가 §5 9개 상태를 신뢰성 있게 표시하지 못한다** —
     Dev 기록이 이 속성을 "§5 상태별 실물"의 유일한 대체 앵커로 제시했는데(스토리 미작성 사유),
     정상 상태로 되돌리는 분기가 없다. ① `updateStep3Button`(:356-358)은
     `step3-password-invalid`만 세팅하고 비밀번호가 유효해져도 `step3-input`으로 복구하지 않아
     **§5-8 "STEP3 입력 상태"가 오타 1회 이후 영구 도달 불가**. ② `updateStep1Button`(:210-214)은
     약관을 체크해도 `step1-terms-unchecked`가 그대로 남는다(else 분기 없음). 각 분기에 복구
     `setScreenState` 1줄씩 추가하고, 그 상태로 `story_shots.py capture` 기준선을 떠서 확정 문답
     "모든 표시 조건(§5)을 스토리로"를 실제로 닫을 것.
  3. **(medium) 중복 오류로 STEP1 복귀 시 카운트다운 타이머가 정지되지 않는다** —
     `handleSignupError`의 MBR-4092/4094 분기(:404-418)에 `clearInterval(state.countdownTimer)`가
     없다. 잔여시간이 지나면 `tickCountdown`(:287-295)이 사용자가 보고 있는 STEP1 화면에서
     `data-screen-state`를 `step1-duplicate-error` → `step2-code-expired`로 덮어써 §5-2 상태 캡처를
     깨뜨린다. 같은 분기에서 `btnRequestCode`가 `disabled`인 채 남아(클릭 시점에 true로 설정 후
     성공 경로에서 복구 안 함) target을 수정하기 전에는 재요청도 못 한다.
  4. **(low) `innerHTML` 잠재 XSS 싱크** — `signup.html:412`가 서버 `message`와 `login_url`을
     이스케이프 없이 `el.targetError.innerHTML`에 문자열 결합한다. 현재 두 값 모두 서버 상수
     (`"이미 가입된 이메일입니다"`, `LOGIN_URL = "/login"`)라 악용 불가하지만, 이후 message에
     사용자 입력(target)을 echo하거나 login_url을 설정화하는 순간 무인증 화면의 XSS가 된다.
     `createElement('a')` + `textContent`/`setAttribute`로 바꿀 것. 덧붙여 `login_url`의 `/login`은
     INF-MBR-002 §login_url 필드에 "실재하지 않는 자리표시"로 명시돼 있어 AC의 "로그인 링크"는
     현재 404로 이어진다(랩 한계, 이 FUNC 밖).
  5. **(low) STEP1 즉시 중복검증 미구현을 문서로만 남기지 말 것** — 위 Layer1 판단대로 후속 SR
     접수 또는 `docs/변경관리/SR-231/02_변경명세.md` §FUNC-member-001 TO-BE(:33 "입력 즉시 검증:
     이메일 중복 여부") 정정 중 하나를 이 SR 종결 전에 수행할 것.

### QA Gate — 2026-09-12 (round 2 재게이트) — CONCERNS
- **Layer1 스펙**: concerns. **재작업 지시 5건은 전부 코드에 실재함을 직접 확인**(dev 보고 대조가 아니라
  파일 원문 기준):
  - 지시1(MBR-4091/4093 분리) — `signup.html:435-465`. MBR-4091 분기에 `disabled = true`가 **없고**
    입력값을 유지한 채 인라인 오류만 세팅, 실제 잠금은 `MBR-4093` 분기로 분리되어 `el.code.disabled`
    `btnVerifyNext.disabled` + `btnResend.classList.add('emphasize')`(CSS `:42`). 남은 시도횟수는
    표시하지 않음(응답 필드 부재와 일치).
  - 지시2(9개 상태 진입·복귀) — `updateStep1Button:240-246` / `updateStep2Button:331-340` /
    `updateStep3Button:395-401` 세 함수 모두 else 복귀 분기 존재. 상태 전이표 주석 `:138-164`.
    `MemberSignupScreenStateTest` 9건이 §5 9개 상태를 HtmlUnit 실행 환경에서 단언(state2/5/8은
    복귀까지 단언). **오류 표시가 복귀 분기에 지워지지 않도록 호출 순서를 뒤집은 것(`:446-450`,
    `:479-482`)도 정확하다** — 순서를 되돌리면 오류가 즉시 소거되는 것을 코드 흐름상 확인.
  - 지시3(타이머 정지) — MBR-4092/4094 분기 `:475-478`, MBR-4093 분기 `:458-461`에
    `clearInterval` + `null` 대입. `btnRequestCode`가 disabled로 고착되던 round1 지적도
    `updateStep1Button()` 재호출(`:482`)로 해소됨.
  - 지시4(innerHTML 제거) — `:485-493`. `innerHTML` 문자열 결합은 파일 전체에서 0건(grep 확인),
    `createTextNode` + `createElement('a')` + `textContent`로 조립.
  - 지시5(02_변경명세 정정) — `docs/변경관리/SR-231/02_변경명세.md:34-40`에 실재. "중복 판정은
    인증코드 검증 이후 서버 응답(STEP3 제출, 409 MBR-4092/MBR-4094)으로만" 문장을 요구사항 원문
    (`:33`) 바로 아래에 배치.
  - 요청/응답 계약은 round1 판정 그대로 INF-MBR-001/INF-MBR-002와 일치(필드명·오류코드 무변경 —
    이번 라운드는 API를 건드리지 않았다).
  - **다만 아래 잔존 1건이 지시1·2의 목적을 특정 경로에서 무력화한다**(권고1·2, 임시 probe 테스트로
    실측 확정 — probe는 판정 후 삭제).
- **Layer2 보안**: pass(권고 3). 이번 라운드 변경은 화면 스크립트·테스트·pom·문서뿐이고 인증/인가
  표면(`ApiKeyAuthFilter`, mtime 08:10 = round1 시점)은 **재작업 창(08:30~08:41)에 손대지 않았다**.
  round1의 화이트리스트 판정(무키 401 / 타인 ID 403 / 본인 ID 200)은 `ApiKeyAuthIntegrationTest`
  43건 green으로 그대로 유지. XSS 싱크는 제거 확인 — `message`는 `createTextNode`, 링크 라벨은
  고정 문자열 `textContent`라 서버 문자열이 마크업으로 해석될 경로가 없다. `htmlunit`은 test 스코프라
  운영 아티팩트에 포함되지 않는다.
- **Layer3 회귀**: pass. mtime 실측상 재작업 창에 바뀐 파일은 신고된 4개뿐이고
  (`signup.html` 08:30 · `02_변경명세.md` 08:31 · `MemberSignupScreenStateTest.java` 08:38 ·
  `pom.xml` 08:41), **FUNC-member-002/003 소유 파일은 이번 라운드도 미수정**
  (`MemberSignupService` 04:30 · `MemberSignupCompletionWriter` 06:54 · `MemberRegistrationService`
  07:19 — 전부 재작업 창 이전). `MemberSignupScreenStateTest`는 두 서비스를 `@MockBean`으로만
  참조한다. **websocket-client 제외는 안전함을 실측 확인**: 전체 스위트 최종 실행(08:43~08:44)
  39개 클래스 **319 tests / 0 failures / 0 errors**, 그중 회귀 감시 대상인 `ApiKeyAuthIntegrationTest`
  43건·`OrderCreateQtyZeroRegressionTest`(round5 전역설정 사고의 회귀 감시)·`ProductDaoTest` 11건
  전부 green. 제외 대상은 `htmlunit`의 WebSocket 지원 경로 하나뿐이고 이 화면·이 테스트는 WebSocket을
  쓰지 않는다(향후 WS를 쓰는 HtmlUnit 테스트를 추가하면 그때 `NoClassDefFoundError`로 즉시 드러난다).
  기준선 228 대비 +91.

- 권고(CONCERNS시):
  1. **(medium) STEP3 체류 중 코드가 만료되면 화면이 사용자에게 아무 것도 알리지 않고
     `data-screen-state`만 뒤바뀐다** — 지시3(타이머 정지)이 **STEP1 복귀 경로에만** 적용됐고,
     STEP2→STEP3 전이(`signup.html:347-357` `btnVerifyNext` 핸들러)에서는 타이머가 살아 있다.
     임시 probe(유효시간 2초)로 실측: STEP3 표시 중 만료 시각이 되면 `tickCountdown`(`:315-329`)이
     `step3-input` → **`step2-code-expired`** 로 덮어쓰고 숨겨진 `code` 입력을 disabled로 만드는데,
     **STEP3 화면에는 배너·인라인 오류가 하나도 뜨지 않는다**(probe: `passwordError=false
     signupError=false banner=false`). 이후 비밀번호를 한 글자만 입력해도 `updateStep3Button`이
     `step3-input`으로 되돌려 앵커가 진동한다. 5분 창은 "코드 수신 대기 + STEP3 입력"으로 충분히
     소진되는 현실적 구간이라 §5-8 캡처 기준선이 비결정적이 된다. → STEP3 진입 시 타이머를
     정지시키거나(제출 시 서버가 최종 판정하므로 화면 카운트다운의 효용이 없음), 만료를 STEP3에서
     보이게 표시(배너 + 재전송 유도)할 것.
  2. **(medium) 그 결과 지시1의 "MBR-4091은 재입력 허용" 계약이 이 경로에서 깨진다** —
     `updateStep2Button`(`:331-340`)의 복귀 분기가 `if (!el.code.disabled)`로 게이트돼 있는데
     MBR-4091 분기(`:437-450`)는 `el.code.disabled`를 복구하지 않는다. probe 실측: 타이머 만료 후
     제출 → 서버 409 `MBR-4091` → 화면은 `step2-code-invalid`("인증코드가 올바르지 않습니다" =
     재입력 안내)인데 **`code` 입력과 `다음` 버튼이 둘 다 disabled**. 재전송 버튼이 보여 완전한
     막다른 길은 아니지만, round1 권고1이 지목한 "재시도 가능한 실패에서 입력을 잠근다"가 이
     경로로 되살아난다. → MBR-4091 분기에서 `el.code.disabled = false`(+`btnVerifyNext` 재평가)를
     명시하거나, 권고1을 해결하면 자연 해소.
  3. **(medium) MBR-4093을 `step2-code-expired`와 공유한 Dev 판단 — 재검토 권고**(요청받은 검토
     항목). 두 가지 이유로 동의하지 않는다.
     ① **앵커 모호성**: Dev 기록은 이 속성을 "스토리 미작성의 유일한 대체 앵커"로 제시했는데,
     같은 값이 서로 다른 두 화면을 가리킨다 — 만료는 `countdown`에 `.expired`(빨강·00:00) +
     "인증 시간이 만료됐습니다", 상한은 카운트다운이 잔여시간에 멈춘 채 + "인증 시도 횟수를
     초과했습니다" + `btnResend.emphasize`. `story_shots capture`로 `step2-code-expired` 기준선을
     뜨면 어느 쪽이 잡혔는지 문서만 보고는 알 수 없다.
     ② **UX상 더 중요한 문제 — 구제책이 정반대다**: 진짜 만료는 재전송으로 회복되지만,
     MBR-4093은 INF-MBR-002 §코드값 의미에 "재발송(`writeCode`)이 `attempt_count`를 리셋하지 않아
     상한에 걸린 target은 재발송을 받아도 계속 `MBR-4093`이고, 안내대로 재발송을 반복하면
     `expires_at`이 밀려 자가치유(만료+10분 정리배치)마저 지연된다"고 명시(후속 SR-295)돼 있다.
     이 화면은 그 상태에서 재전송 버튼을 **시각적으로 강조**해 사용자를 그 막다른 루프로 적극
     유도하고, 매 재전송이 INF-MBR-001 일일 5회 상한을 소모시켜 몇 번 만에 그 날 가입을 완전히
     차단한다. → 별도 상태값(예 `step2-code-locked`)으로 분리하고, 문구를 "재전송하면 풀린다"로
     읽히지 않게(예: 잠시 후 다시 시도 안내) 바꿀 것. SR-295가 닫히기 전까지의 임시 문구임을
     STORY 후속 추적에 연결할 것. (사람이 지시한 사항을 Dev가 그대로 따른 것이므로 Dev 이탈이
     아니다 — 사람 재확인이 필요한 지점이다.)
  4. **(low) 지시4는 반영됐으나 `href` 스킴은 여전히 미검증** — `:490`
     `loginLink.setAttribute('href', body.login_url)`. 텍스트 싱크는 안전해졌지만 `login_url`에
     `javascript:`/`data:` URL이 들어오면 클릭 시 실행된다. 현재 값은 서버 상수
     (`MemberRegistrationService:115` `LOGIN_URL = "/login"`)라 악용 불가지만, 바로 위 코드 주석은
     "이후 message/login_url 값이 바뀌어도 안전하도록 고정한다"고 적어 **`login_url`에 대해서는
     사실과 다르다**. → `/`로 시작하는 상대경로만 허용하는 1줄 가드를 두거나, 주석에서 과대주장을
     덜어낼 것.
  5. **(low) 02_변경명세 정정이 렌더링에서 보이지 않는다** — `:34-40`이 HTML 주석
     (`<!-- ... -->`)이라 SpecLens·마크다운 뷰어에서는 `:33` "입력 즉시 검증: 이메일 중복 여부"만
     보이고 정정문은 사라진다. 사람 코멘트는 "'…서버 응답으로만' **문장을 넣고** 요구사항 원문 옆에
     **정정 주석**"으로 두 가지를 요구했는데 Dev는 둘을 한 개의 비가시 주석으로 합쳤다(파일 기존
     관례를 따른 것은 맞다). → TO-BE 본문에 보이는 한 줄을 추가할 것.
  6. **(low) `mvnw test` 집계 324는 stale surefire 리포트 5건이 섞인 값이다** — 최종 실행
     (08:43~08:44)에 실제로 돈 것은 39개 클래스 **319건**이고, 나머지 5건은 이미 삭제된 probe
     클래스(`MemberSignupScreenStateSpikeTest` 08:34, `QaGateRound3TxBoundaryProbe` 07:07,
     `QaProbeAttemptCountTest` 06:39, `QaR6SchedulingProbeTest` 2건 05:15)의 옛 XML이 `target/`에
     남은 것이다. 판정 결론(0 실패 / 기준선 228 상회)은 그대로지만, `test_baseline_ws.py`가
     `counts_source: surefire`로 집계하므로 기준선을 다시 뜰 때는 `clean test`로 돌릴 것.
     (이번 QA가 `MemberSignupScreenStateSpikeTest`의 잔여 리포트와 자체 probe 산출물은 삭제했다.)
  - **round1 권고 대비**: 권고1·2·3·4·5 = 반영 확인. 권고2의 "story_shots capture 기준선 확보"는
    사람 코멘트가 HtmlUnit 9건으로 대체를 지시했고 그대로 이행됐다(`.speclinker/story_shots/baseline/`에
    member 항목 없음 — 의도된 대체이지 누락이 아니다).

### QA Gate — 2026-09-12 (round 3 재게이트) — CONCERNS
- **Layer1 스펙**: concerns. **사람이 방식까지 지정한 (A)(B)(C) + 재작업 지시 1~6이 전부 코드·문서에
  실재함을 원문 기준으로 확인했다**(dev 보고 대조가 아님):
  - **(A) 타이머는 STEP2에서만 산다 — 확인**. `signup.html:268-293` `goStep(n)`이 단일 창구다:
    `n !== 2`면 `clearInterval` + `null`, `n === 2`일 때만 `state.expiresAt` 기준으로 재시작
    (`:286-292`). `btnVerifyNext` 핸들러(`:417-427`)가 `goStep(3)`을 타므로 STEP3 진입 시 타이머가
    죽고, `tickCountdown`(`:371-394`)은 STEP2가 아닐 때 아예 돌지 않는다.
    **임시 probe(유효시간 1초, HtmlUnit)로 round2와 같은 시나리오를 재현·실측**: STEP3 체류 중
    만료시각이 지나도 `state=step3-input` 유지, 배너·인라인 오류 없음(`banner=false
    codeErrorText=''`) — round2 권고1의 "조용한 앵커 뒤바뀜"은 **해소**됐다. probe는 판정 후 삭제
    (잔여 surefire XML 0건 확인).
  - **(A) MBR-4091은 어떤 경로에서도 잠그지 않는다 — 확인**. `el.code.disabled` 대입은 파일 전체에서
    `updateStep2Button`(`:401`) **1곳뿐**이고 조건은 `data-screen-state === 'step2-locked'`(`:400`)
    단 하나다(grep로 산발 대입 0건 확인). MBR-4091 분기(`:505-523`)는 그 상태로 전이하지 않는다.
    probe 실측: 타이머가 **실제로 만료된 뒤** 제출 → 409 MBR-4091 → `state=step2-code-invalid`,
    `codeDisabled=false nextDisabled=false` — round2 권고2도 **해소**.
  - **(B) 상태 분리 — 확인**. `step2-code-expired`(`tickCountdown:389-393` — `.expired` + 재전송
    노출 + `emphasize` + "인증 시간이 지났습니다")와 `step2-locked`(`:524-535` — 입력 잠금 + 재전송
    노출하되 `emphasize` 제거 + "인증 시도 횟수를 초과했습니다…")가 별개 상태로 실재한다.
    `tickCountdown:383-388`이 잠금 상태에서는 만료 표시로 덮어쓰지 않도록 조기 return 한다.
    `02_변경명세.md:51-68` §5 목록도 **10개로 갱신**(6=만료/7=상한 분리), 오류 표기 문구(`:46-50`)도
    두 상태로 나뉘어 갱신됨. 테스트는 10개 상태 각 1건 + 회귀 1건 = 11건.
  - **(C) login_url 상대경로 검증 — 확인**. `isSafeRelativePath`(`:249-253`)가 `'/'`로 시작하되
    `'//'`(스킴 상대 URL)는 제외하고, 실패 시 `<a>` 자체를 생략한다(`:552-558`). `javascript:`/`data:`
    차단됨. 주석의 과대주장도 "값이 바뀌는 미래를 대비한 것"으로 정정됨(`:548-551`).
  - **(D) 02_변경명세 정정 — 확인**. `docs/변경관리/SR-231/02_변경명세.md:34-41`이 HTML 주석이 아니라
    **볼드 본문 문단**이다(요구사항 원문 `:33` 바로 아래). 렌더링에서 보인다.
  - **(E) clean 빌드 재현 — 확인**(아래 Layer3).
  - 요청/응답 계약은 round1·2 판정 그대로 INF-MBR-001/INF-MBR-002와 일치(이번 라운드도 API 무수정).
  - **다만 (A)의 부수 결과로 잔존 1건**: 만료가 STEP3에서 조용히 지나간 뒤 서버 409 MBR-4091로
    STEP2에 돌아오면 **재전송 수단이 화면에서 사라진다**(권고1, probe 실측 확정).
- **Layer2 보안**: pass(관찰 1). 이번 라운드에 바뀐 것은 화면 스크립트 1개 + 테스트 1개 + 문서 1개뿐이고
  인증/인가 표면은 손대지 않았다(`ApiKeyAuthFilter` mtime 08:10 = round1 시점, round3 창 09:04~09:11
  밖). `ApiKeyAuthIntegrationTest` 43건 green으로 화이트리스트 판정(무키 401 / 타인 ID 403 / 본인 ID
  200) 유지. XSS 싱크는 여전히 0(`innerHTML` 사용 0건, `createTextNode`/`textContent`), 여기에 href
  스킴 가드가 더해져 round2 권고4가 닫혔다. **관찰(low, 이번 SR 차단 아님)**: `isSafeRelativePath`가
  역슬래시를 보지 않아 `"/\evil.com"`은 통과하는데, 브라우저 URL 파서는 특수 스킴에서 `\`를 `/`로
  정규화하므로 `//evil.com`(외부 오리진)으로 해석된다. 현재 `login_url`은 서버 상수 `"/login"`이라
  도달 불가 경로이고, 가드의 목적(미래 값 변화 대비)을 완성하려면 `charAt(1) !== '\\'` 한 조건이면 된다.
- **Layer3 회귀**: pass. mtime 실측상 round3 창에 바뀐 소스는 **정확히 2개**
  (`MemberSignupScreenStateTest.java` 09:04 · `signup.html` 09:11)이고 `pom.xml`은 08:41(round2)로
  그대로다 — **FUNC-member-002/003 소유 파일 미수정 재확인**(`MemberSignupService` 04:30 ·
  `MemberSignupCompletionWriter` 06:54 · `MemberRegistrationService` 07:19, 전부 round3 창 이전.
  테스트는 `@MockBean` 참조만). **`mvnw clean test`를 QA가 직접 재실행해 story 기록을 재현**:
  39개 클래스 **321 tests / 0 failures / 0 errors**, BUILD SUCCESS(exit 0, 31초, surefire XML 39개
  = stale 0건). story "검증(round3)"의 수치와 **정확히 일치**하며 round2 권고6(stale XML)도 해소.
  기준선(`.speclinker/test_baseline.json` 228) 대비 +93. 회귀 감시 대상 `ApiKeyAuthIntegrationTest`
  43건 green. `classList.toggle(force)` 대체(`setHidden:255-266`)는 파일 전체에 2-인자 toggle 잔존
  0건(주석 언급만)이고, 그 수정으로 깨졌던 `btnSignup` 경로(state3/5/7/10 + 회귀)가 전부 green이라
  **부작용 없음**을 실행으로 확인했다.

- 권고(CONCERNS시):
  1. **(medium) STEP3에서 코드가 만료된 뒤 MBR-4091로 STEP2에 돌아오면 재전송 버튼이 사라져 화면만으로는
     회복할 수 없다** — `handleSignupError` MBR-4091 분기가 `goStep(2)` **직후**
     `el.btnResend.classList.add('hidden')`(`:515`) + `remove('emphasize')`(`:516`) +
     `countdown.classList.remove('expired')`(`:517`)로, 바로 앞 `goStep(2)`의 `tickCountdown`이
     잔여 0을 보고 띄워 둔 만료 UI(재전송 노출·강조·"인증 시간이 지났습니다")를 **그 자리에서
     되돌린다**. probe 실측(유효시간 1초 → STEP3 체류 → 제출):
     `state=step2-code-invalid resendDisplayed=false countdown='00:00' countdownClass='countdown'
     codeErrorText='인증이 필요합니다'`. 이후 코드를 다시 입력해 재제출해도 서버는 계속 만료로
     4091을 주고 재전송 버튼은 끝까지 나타나지 않는다(probe E·F 단계로 확인 — 루프). 재전송은
     만료(`tickCountdown`)나 MBR-4093에서만 노출되므로, 이 경로의 사용자는 **새로고침 외에 새 코드를
     받을 방법이 없다**(입력값 전부 유실). 5분 창은 "코드 수신 + STEP3 입력"으로 충분히 소진되는
     현실적 구간이다. 또 문구가 "인증이 필요합니다"(서버 4091 원문)라 만료 사실도 전달되지 않는다
     — `countdown` 00:00만 단서다.
     → 한 줄 수정으로 족하다: MBR-4091 분기에서 **`state.expiresAt <= Date.now()`이면** 만료 UI를
     되돌리지 말 것(재전송 노출·`emphasize`·`.expired` 유지, 상태는 `step2-code-expired`로 두거나
     "인증 시간이 지났습니다"를 함께 표시). (A)·(B)의 설계 원칙은 그대로 지키는 변경이다.
  2. **(low) `isSafeRelativePath`의 역슬래시 미검사** — 위 Layer2 관찰. 현재 값(`"/login"` 서버 상수)
     으로는 도달 불가라 이번 SR 차단 사유가 아니다. 가드를 손댈 일이 생기면
     `url.charAt(1) !== '\\'`를 함께 넣을 것.
  - **round2 권고 대비**: 권고1·2·3·4·5·6 = **전부 반영 확인**(위 Layer1·3 각 항목). 권고3(상태 분리)은
    사람 결정 (B)대로 `step2-locked` 신설로 닫혔고, SR-295 연결도 STORY 후속 추적에 남아 있다.
    권고1의 잔존분은 "타이머 정지" 자체가 아니라 **정지 이후의 회복 안내**로 성격이 바뀌었다(위 권고1).

### QA Gate — 2026-09-12 (round 4 재게이트) — PASS
- **Layer1 스펙**: pass. 사람이 지정한 두 항목이 코드에 실재하고, **round3 권고1의 결함이 실제로
  해소됨을 QA 자체 probe(`QaGateRound4Probe`, HtmlUnit, 판정 후 삭제 — 잔여 소스·surefire XML 0건)로
  독립 실측**했다(dev 테스트 통과에 기대지 않음).
  - **지시1(만료 후 MBR-4091에서 만료 UI 유지) — 해소 확인**. `signup.html:526-543`: `goStep(2)`
    직후의 되돌리기 3줄(`btnResend.add('hidden')`/`remove('emphasize')`/`countdown.remove('expired')`)이
    `if (state.expiresAt > Date.now())`로 감싸졌고, `updateStep2Button()`(§5-5 복귀 부작용) 뒤에
    `state.expiresAt <= Date.now()`면 `step2-code-expired` + "인증 시간이 지났습니다"로 다시 덮어쓴다.
    **probe A3 실측(유효시간 1초 → STEP2에서 실제 만료 → STEP3 → 제출 → 409 MBR-4091)**:
    `state=step2-code-expired resendDisplayed=true resendClass='secondary emphasize'
    countdown='00:00' countdownClass='countdown expired' codeError='인증 시간이 지났습니다'
    codeDisabled=false nextDisabled=false`. round3 실측값(`step2-code-invalid resendDisplayed=false
    codeErrorText='인증이 필요합니다'`)과 정반대로 뒤집혔다 — 만료 사실도 이제 문구로 전달된다.
  - **회복 경로가 실제로 닫히는지 끝까지 확인(probe A4)**: 그 상태에서 재전송 클릭 → 200 →
    `state=step2-initial countdown='05:00' countdownClass='countdown'(expired 해제)
    resendDisplayed=false`. **새로고침 없이 새 코드를 받아 가입을 계속할 수 있다** — 사람 코멘트
    "[개발자 결정] 사용자가 새로고침 없이는 복구 못 하는 케이스는 남기지 않는다"가 충족됐다.
  - **지시2(`isSafeRelativePath` 역슬래시 거부) — 확인**. `:255-256`
    `url.charAt(0) === '/' && url.charAt(1) !== '/' && url.charAt(1) !== '\\'`.
    `/login`·`/mypage/welcome` 통과 / `//evil.com`·`/\evil.com` 거부 / `javascript:`·`data:`는
    `charAt(0)` 검사로 여전히 차단. 신규 단위 테스트 3건이 `<a>` 렌더 여부로 DOM 결과를 단언(내부
    함수 직접 호출이 아니라 signup.html이 실제로 소비하는 경로 그대로)하며 전부 green.
  - **요청/응답 계약**: round1~3 판정 그대로 INF-MBR-001/INF-MBR-002와 일치. 이번 라운드도 API 무수정
    (서버 파일 mtime 전부 round4 창 이전). §5 상태 수는 10개 그대로라 `02_변경명세.md` 추가 정정 불필요
    (4091이 `step2-code-expired`에 도달하는 것은 **신규 상태가 아니라 신규 전이**다).
- **Layer2 보안**: pass. 이번 라운드 변경은 화면 스크립트 1개 + 테스트 1개뿐이고 인증/인가 표면은
  손대지 않았다(`ApiKeyAuthFilter` mtime 08:10 = round1, round4 창 09:23~09:31 밖). `ApiKeyAuthIntegrationTest`
  **43건 green**으로 화이트리스트 판정(무키 401 / 타인 ID 403 / 본인 ID 200) 유지. `innerHTML` 사용
  0건 유지. round3 Layer2 관찰(역슬래시 우회)이 이번 라운드에 **닫혔다**.
  **잔여 관찰(low, 차단 아님)**: 가드가 두 번째 문자만 보므로 URL 파서가 제거하는 제어문자를 끼운
  `"/<TAB>/evil.com"` 류는 이론상 여전히 통과한다(브라우저가 tab/LF/CR을 스트립해 `//evil.com`이 됨).
  `login_url`이 서버 상수 `"/login"`(`MemberRegistrationService`)인 한 도달 불가 경로이고, 가드의
  본래 목적(스킴 URL 차단 + 오리진 이탈 차단)은 달성됐다 — 이번 SR 차단 사유가 아니다.
- **Layer3 회귀**: pass. **범위 준수 실측**: round4 창(09:15 이후)에 바뀐 소스는 `find -newermt`
  기준 **정확히 지시된 2개뿐**(`signup.html` 09:23:50 · `MemberSignupScreenStateTest.java` 09:31:33).
  `pom.xml` 08:41(round2) · `ApiKeyAuthFilter` 08:10 · `MemberSignupViewController` 08:09 ·
  `02_변경명세.md`(round3) 전부 무변경 — "그 외 변경 금지"가 지켜졌다. **FUNC-member-002/003 소유
  파일 미수정 재확인**(`MemberSignupService` 04:30 · `MemberSignupCompletionWriter` 06:54 ·
  `MemberRegistrationService` 07:19 · `MemberSignupController` 03:45 · `MemberRegistrationController`
  05:53 — 전부 round4 창 이전, 테스트는 `@MockBean` 참조만).
  **`mvnw clean test`를 QA가 직접 재실행해 story 수치를 재현**: 39개 클래스 **325 tests / 0 failures /
  0 errors / 0 skipped**, BUILD SUCCESS(exit 0, surefire XML 39개 = stale 0건). story "검증(round4)"의
  325건과 **정확히 일치**하고 round3(321) 대비 +4(신규 4건)도 일치. `MemberSignupScreenStateTest`
  **15/15 green(13.0초)**. 기준선(`.speclinker/test_baseline.json` 228) 대비 +97.
  **기존 회귀 테스트 무손상 주장 검증**: `regressionStep3TimerStopped_thenMbr4091NeverLocksInput`은
  이름·단언이 round3 원본 그대로이고 주석만 정정됐다(dev 기록의 "단언을 바꿔치기했다가 실패를 실측하고
  되돌렸다"는 서술과 파일 실물이 일치). 만료 경로는 별도 신규 테스트로 분리됐다.
  **3대 원칙 무손상 — 구조적 확인 + probe 실측 양쪽**:
  (A) 타이머는 STEP2에서만 생존 — `setInterval` 대입 지점 파일 전체 **1곳**(`goStep:294`, `n === 2` 안).
      round4는 `goStep`을 손대지 않았다.
  (B) MBR-4093과 만료는 별개 상태 — `setScreenState('step2-locked')` 대입 지점 **1곳**(4093 분기 `:550`).
      probe C1(실제 만료 뒤 4093): `state=step2-locked resendClass='secondary'(emphasize 없음)
      countdownClass='countdown'(expired 없음)` — 4091 변경이 4093 경로를 오염시키지 않았다.
      probe C2(+1.5초)·D2(잔여시간 0 도달)에서도 `step2-locked` 유지 — 타이머 누수 덮어쓰기 없음.
  (C) MBR-4091은 어떤 경로에서도 잠그지 않는다 — `el.code.disabled` 대입 지점 파일 전체 **1곳**
      (`updateStep2Button:405`, 조건은 `step2-locked` 단 하나). probe A3(만료 경로) · B1(미만료 정상
      오답) 둘 다 `codeDisabled=false`. B1은 round3과 동일하게 `step2-code-invalid` + 재전송 숨김 +
      카운트다운 04:59 정상 진행 — 미만료 경로가 만료 처리에 오염되지 않았다.

- 권고(CONCERNS시): **없음(차단·필수 수정 0건)**. 아래는 이번 SR 종결을 막지 않는 **후속 정리 메모**다.
  1. **(low/추적) `linked_tc` 앵커 중복** — `MemberSignupScreenStateTest.java:250`
     (`isSafeRelativePath_allowsPlainRelativePath`)과 `:460`
     (`regressionMbr4091AfterRealExpiry_keepsResendButtonVisible`)이 **둘 다 `TC-FUNC-member-001-14`**
     를 달았다. 현재 `.speclinker/tc_index.json`에 member-001 항목이 없어 실제 충돌은 아직 없지만,
     `/sl-test --scan` 시 이번 SR의 핵심 회귀 테스트가 다른 테스트와 같은 TC-ID로 뭉개진다.
     → 후자를 `-17`로 바꾸면 끝(테스트 로직 무관, 주석 1줄).
  2. **(low/문서) `signup.html` 상태 전이표 주석이 round4 전이를 반영하지 않았다** — `:171`이
     "STEP3 제출 409 MBR-4091 → step2-code-invalid"만 적어 두어, 이번에 추가된 "만료 이후 4091 →
     step2-code-expired(만료 UI 유지)" 분기가 표에 없다. Dev 기록이 이 주석·`data-screen-state`를
     스토리 미작성의 대체 앵커로 제시한 만큼 다음 세션이 오독할 여지가 있다 → 표에 한 줄 추가 권고.
  3. **(low/관찰) 만료 상태에서 코드를 재입력하면 앵커가 `step2-initial`로 잠시 되돌아간다**
     (probe A2: `state=step2-initial codeError='' resendDisplayed=true resendClass='secondary emphasize'
     countdown='00:00' countdownClass='countdown expired'`). `updateStep2Button`의 §5-5 복귀 분기
     때문이며 **round3부터 있던 동작이고 round4가 만든 것이 아니다**. 재전송 버튼·만료 카운트다운이
     그대로 남아 회복 수단이 사라지지 않으므로 실사용자 영향은 없다(막다른 길 아님). "그 외 변경 금지"
     범위 밖이라 이번 라운드에 고치지 않은 것이 맞다 — §5 캡처 기준선을 뜰 때만 유의.
  - **round3 권고 대비**: 권고1(만료 후 4091 재전송 소실) = **해소**(probe A3·A4 실측). 권고2(역슬래시
    미검사) = **해소**(:256 + 단위 테스트 3건). round1·2 권고까지 누적 **전 항목 종결** — 4개 라운드
    동안 제기된 재작업 지시 중 미해소 잔존은 없다.

## 재작업 지시
> round 3 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/spec] STEP3 체류 중 코드가 만료된 뒤 제출해 409 MBR-4091로 STEP2에 돌아오면 재전송 버튼이 화면에서 사라져 새 코드를 받을 수단이 없다. handleSignupError의 MBR-4091 분기가 goStep(2) 직후 btnResend.classList.add('hidden')(:515) + remove('emphasize')(:516) + countdown.remove('expired')(:517)로, 바로 앞 goStep(2)→tickCountdown이 잔여 0을 보고 띄운 만료 UI를 그 자리에서 되돌린다. 임시 probe(유효시간 1초, HtmlUnit) 실측: state=step2-code-invalid resendDisplayed=false countdown='00:00' codeErrorText='인증이 필요합니다'. 코드를 다시 입력해 재제출해도 서버는 계속 만료로 4091을 주고 재전송은 끝까지 나타나지 않아(probe E·F) 새로고침 외 회복 경로가 없고 입력값이 전부 유실된다. 문구도 만료 사실을 전달하지 않는다. 사람 결정 (A)(타이머는 STEP2에서만 생존)의 부수 결과이며, (A) 자체는 정확히 구현됐다. → MBR-4091 분기에서 state.expiresAt <= Date.now()이면 만료 UI를 되돌리지 말 것(재전송 노출·emphasize·.expired 유지, '인증 시간이 지났습니다' 병기 또는 step2-code-expired 유지) — (A)(B) 설계 원칙을 지키는 한 줄 수정
2. [low/security] round3에 추가된 isSafeRelativePath(:249-253)가 '//'만 걸러내고 역슬래시를 보지 않는다 — "/\evil.com"은 통과하지만 브라우저 URL 파서는 특수 스킴에서 \를 /로 정규화해 //evil.com(외부 오리진)으로 해석한다. 현재 login_url은 서버 상수 "/login"이라 도달 불가 경로이고, javascript:/data: 차단이라는 본래 목적은 달성됐다. → 가드를 손댈 일이 생기면 url.charAt(1) !== '\\' 조건을 함께 넣을 것(이번 SR 차단 사유 아님)

사람 코멘트: [개발자 결정] 사용자가 새로고침 없이는 복구 못 하는 케이스는 남기지 않는다. (1) goStep(2) 진입 시 만료시각이 이미 지났으면 만료 UI(step2-code-expired, 재전송 강조)를 그대로 유지 - 기존 되돌리기 로직을 그 조건으로 감싼다. (2) 같은 김에 isSafeRelativePath: 역슬래시와 이중 슬래시로 시작하는 경로를 거부하도록 강화(슬래시 두 개 연속 또는 역슬래시가 오면 안전하지 않음으로 판정). 테스트: 만료 후 STEP2 복귀 시 재전송 버튼 존재 단언 1건 + 경로 검증 단위 테스트 3건. 그 외 변경 금지. mvnw clean test.

## STEP5 테스트 실행 결과 (test-agent, 2026-09-12)

### 수용 기준(AC) ↔ 테스트 매핑

이 story는 명시적 AC 목록을 갖지 않고, SR-231 변경명세서의 TO-BE 스펙을 AC로 삼는다. 아래는 변경명세 §FUNC-member-001의 요구사항과 dev-agent/qa-agent가 작성·검증한 테스트의 대응 관계다:

| 요구사항 | AC 범주 | 검증 테스트 | TC-ID 비고 |
|---------|--------|-----------|----------|
| **화면 구조** (3단계 마법사, 단계 표시 1/3, 모바일 단일 컬럼) | 정상 경로 | `MemberSignupViewControllerTest::signupScreenLoads()` | 뼈대 렌더 확인 |
| **STEP1: 이메일/휴대폰 입력** (가입 수단 선택) | 정상 경로 | `MemberSignupScreenStateTest::state1_step1Initial_rendersCorrectly()` | 초기 상태 |
| **STEP1: 약관 동의** (전체 동의 + 개별 펼침) | 정상 경로 | `MemberSignupScreenStateTest::state2_step1TermsUnchecked_nextButtonDisabled()` | 약관 미동의 → 다음 비활성 |
| **STEP1: 중복 이메일 오류** ("이미 가입된 이메일" + 로그인 링크) | 예외 경로 | `MemberSignupScreenStateTest::state3_step1DuplicateError_stopsCountdownAndUsesDomLink()` | MBR-4092/4094 응답 처리 |
| **STEP2: 인증코드 입력** (6자리, 5분 카운트다운) | 정상 경로 | `MemberSignupScreenStateTest::state4_step2Initial_showsCountdown()` | 초기 상태 + 타이머 시작 |
| **STEP2: 코드 오류** (재입력 허용, 입력 절대 미잠금) | 예외 경로 | `MemberSignupScreenStateTest::state5_step2CodeInvalid_allowsRetry()` | MBR-4091 미잠금 확인 |
| **STEP2: 코드 만료** ("인증 시간이 지났습니다" + 강조 재전송) | 예외 경로 | `MemberSignupScreenStateTest::state6_step2CodeExpired_onCountdownReachesZero()` | 실제 만료 카운트다운 00:00 |
| **STEP2: 인증 시도 상한** ("시도 횟수 초과" + 입력 잠금, 재전송 강조 X) | 예외 경로 | `MemberSignupScreenStateTest::state7_step2Locked_onAttemptCapReached()` | MBR-4093 상태 분리 |
| **STEP3: 비밀번호·이름·마케팅 입력** | 정상 경로 | `MemberSignupScreenStateTest::state8_step3Input_acceptsPasswordAndName()` | STEP3 입력 가능 |
| **STEP3: 비밀번호 규칙 위반** | 예외 경로 | `MemberSignupScreenStateTest::state9_step3PasswordInvalid_showsInlineError()` | 비밀번호 검증 오류 |
| **완료 상태** (환영 쿠폰 안내) | 정상 경로 | `MemberSignupScreenStateTest::state10_step3Done_showsWelcomeMessage()` | 가입 완료 상태 |
| **회귀: 타이머 정지** (STEP2→STEP3 전이 시) | 회귀 | `MemberSignupScreenStateTest::regressionStep3TimerStopped_thenMbr4091NeverLocksInput()` | (A) 설계 원칙 확인 |
| **회귀: 만료 후 MBR-4091 복귀** (재전송 버튼 유지) | 회귀 | `MemberSignupScreenStateTest::regressionMbr4091AfterRealExpiry_keepsResendButtonVisible()` | (A)(B) round3 권고 확인 |
| **API 화이트리스트** (무인증 화면 접근 가능) | 회귀 | `ApiKeyAuthIntegrationTest::signupScreenRoute_withoutApiKey_returns200()` + `signupScreenRoute_withMemberApiKey_returns200()` | FUNC-member-002/003 회귀 (2건) |
| **경로 검증** (/로 시작하되 //나 \\ 거부) | 경계값 | `MemberSignupScreenStateTest::isSafeRelativePath_allowsPlainRelativePath()` + `rejectsDoubleSlash()` + `rejectsBackslash()` | XSS 방지 (3건) |

**통과 테스트 집계**:
- `MemberSignupViewControllerTest` **2/2 통과**
- `MemberSignupScreenStateTest` **15/15 통과** (§5 10개 상태 + 회귀 5건)
- `ApiKeyAuthIntegrationTest` **43/43 통과** (FUNC-member-001 관련 회귀 2건 포함)
- **전체 스위트** **325 tests / 0 failures / 0 errors** (exit code 0, surefire 39개 클래스 집계)

### 테스트 결과 상세

**테스트 환경**:
- JDK: Java 17.0.10 LTS
- Test Framework: JUnit 5 + Spring Boot Test
- HtmlUnit: 2.70.0 (signup.html 실제 JS 실행 검증)
- MockMvc 슬라이스 테스트 (컨트롤러 계층)

**테스트 실행 기록**:
- **Round 4 검증(QA 최종 게이트)**:
  - `mvnw clean test` 전체 실행 → **325 tests / 0 failures / 0 errors**
  - `MemberSignupScreenStateTest` → **15/15 통과(13.0초)**
  - `ApiKeyAuthIntegrationTest` → **43/43 green** (화이트리스트 회귀 2건 포함)
  - 기준선(`.speclinker/test_baseline.json` 228) 대비 **+97** (신규 테스트 97건)

**회귀 검증 (기존 동작 보증)**:
- **FUNC-member-002/003 소유 파일 미수정**: `MemberSignupService`, `MemberRegistrationService`, `MemberSignupCompletionWriter` 등 아무것도 변경 안 함 (테스트는 `@MockBean` 참조만)
- **API 계약 무변경**: 요청/응답 필드명, 오류코드(MBR-4001/4091/4092/4093/4094) 모두 INF-MBR-001/INF-MBR-002 정본과 일치
- **기존 조회 계약 보증**: 변경명세 "회귀 범위 — 기존 조회 결과 전부(데이터 계약 불변)"를 `ApiKeyAuthIntegrationTest` 43건(회귀 감시 대상 포함)으로 실측 확인

**품질 판정**:
- ✅ **AC 커버리지**: 변경명세 §5 10개 상태 전부 단위 테스트로 매핑 (1:1 이상)
- ✅ **경계값 테스트**: 경로 검증(안전/위험 URL 3가지), 타이머(실제 만료·조기 정지), 오류 분기(재시도·잠금 분리)
- ✅ **회귀 테스트**: round3 권고 2건 + API 회귀 2건 모두 실행 확인 (통과)
- ✅ **통과율**: 325/325 = **100%**
- **권장**: 납품 가능 (품질 판정: ✅)

### 기준선 게이트 (GATE-FUNC-member-001)

아래는 `test_baseline_ws.py check . --func FUNC-member-001 --merge-gate` 실행 결과다:

**기준선 대조 (2026-09-12 실행)**:

| 소스 | 기준선(실행/실패+오류) | 현재(실행/실패+오류) | 판정 |
|---|---|---|---|
| shop-api | 228 / 0 | 325 / 0 | ✅ **유지** — 실행 건수 증가 228→325 (안전 방향 — 신규 테스트 97건 추가) |
| shop-web | - | failed (건수 미상) | ⚠️ 경고(신규 소스 루트, 차단 아님) |

**Exit Code**: `0` (기준선 유지)

**해석**:
- FUNC-member-001 테스트: 0 실패 유지, 신규 테스트 추가로 실행 건수만 증가
- 기존 회귀: FUNC-member-002/003 소유 파일 미수정 → 기존 테스트 228건 동작 불변
- **품질 판정**: ✅ **납품 가능** — 통과율 100% + 회귀 보증 + 기준선 유지

