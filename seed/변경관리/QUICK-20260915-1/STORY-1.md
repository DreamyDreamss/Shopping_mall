---
story-id: STORY-QUICK-20260915-1.1
item: QUICK-20260915-1.1
title: 로그인 실패 5회 잠금 안내 문구를 '잠시 후 다시 시도'로 바꾼다
status: Done
domain: member
created: 2026-09-15
spec_markers: 0
sr-id: QUICK-20260915-1
approved_sha: 59c3051ea805
---

# STORY-QUICK-20260915-1.1 — QUICK-20260915-1 — 로그인 실패 5회 잠금 안내 문구를 '잠시 후 다시 시도'로 바꾼다

## Story
QUICK-20260915-1 — 로그인 실패 5회 잠금 안내 문구를 '잠시 후 다시 시도'로 바꾼다


## 변경 컨텍스트 (QUICK-20260915-1)
> 이 story는 변경요청 **QUICK-20260915-1 — QUICK-20260915-1** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/QUICK-20260915-1/00_요구사항.md`

### 확정된 요건 문답 1건 — 이대로 구현한다(재해석 금지)
- **이번 변경에서 하는 것은?** — 로그인 실패 5회 잠금 안내 문구를 '잠시 후 다시 시도'로 바꾼다

## 수용 기준 (Acceptance Criteria)
- [x] INF-MBR-003 (POST /api/members/login): **순서(사람이 직접 확정, 임의 변경 금지)**: ① 잠금 여부 확인(`MEMBER_LOGIN_ATTEMPTS`, 비밀번호 검증 전 — BCrypt 비용 절약) → ② `MEMBERS`를 email로 조회(탈퇴 회원도 포함해서, `del_yn` 필터 없이) → ③ 비밀번호 대조 → ④ 성공 시 실패카운터 reset → 세션 발급.
- [x] INF-MBR-003 (POST /api/members/login): **존재 오라클 방지(3경우 완전 동일 응답)**: "회원 없음" / "탈퇴 회원(`del_yn='Y'`)" / "비밀번호 불일치" — 이 세 경우는 완전히 동일한 401 `MBR-4011`, 동일한 일반화 문구(`"이메일 또는 비밀번호가 올바르지 않습니다 (n/5)"`, 사유를 절대 구분해 노출하지 않음), 동일한 카운터 증가 의미로 응답한다. 회원이 없거나 탈퇴한 경우에도 더미 BCrypt 해시로 `matches()`를 돌려 응답시간을 비슷하게 만든다(타이밍 사이드채널 완화, should).
- [x] INF-MBR-003 (POST /api/members/login): **잠금 카운터는 회원 존재와 무관**: `MEMBER_LOGIN_ATTEMPTS`는 email 문자열 자체가 PK — 가입되지 않은 이메일도 카운트된다. 이렇게 해야 "이 이메일이 잠겼는가"라는 관찰 자체가 계정 존재의 오라클이 되지 않는다.
- [x] INF-MBR-003 (POST /api/members/login): **5번째 실패 = 즉시 429(사람 확인 2로 확정)**: 4번째까지는 401(`n/5` 문구), **5번째 실패는 401이 아니라 곧바로 429 `MBR-4291`**이다. `touchFailure` 한 문장(원자 UPSERT)이 `fail_count` 증가와 `locked_until` 세팅을 같은 SET 목록 좌→우 평가로 처리한다(세션변수 미사용). 잠금 만료 후 재실패는 `fail_count`를 1로 리셋(무한 누적 방지).
- [x] INF-MBR-003 (POST /api/members/login): **DB 폴백 인증(사람 확인 3으로 확정)**: `ApiKeyAuthFilter`는 정적 `lab.api-keys` 맵(admin·M-0001만 등록됨) 조회가 실패했을 때만(요청당 최대 1회) `MemberApiKeyDao#selectMemberIdByApiKey`로 DB 폴백 조회한다. 이 폴백은 `MEMBERS` 조인 + `del_yn='N'` + `revoked_at IS NULL`로 좁혀져 있어(round9 재작업), 탈퇴 회원이나 폐기된 키는 종전과 동일한 401을 받는다. 기존 정적 맵 경로(admin·M-0001)는 이 추가로 코드·동작이 전혀 바뀌지 않는다(정적 맵 히트 시 DB 조회 자체가 일어나지 않음).
- [x] INF-MBR-003 (POST /api/members/login): **리프레시 토큰 하우스키핑**: 로그인 성공마다 새 토큰 INSERT 직후 이 회원의 만료·폐기 행을 지우고(`purgeExpiredOrRevoked`), 활성 토큰이 상한(5개)을 넘으면 `issued_at` 오래된 순으로 초과분을 지운다(`enforceActiveCap`). 정확한 상한 강제가 아니라 하우스키핑 — 동시 로그인이 6건 이상이면 방금 발급한 토큰이 지워질 수도 있다(사람이 넘긴 low 이슈).
- [x] INF-MBR-003 (POST /api/members/login): **API 키 동시 최초발급 레이스 방지**: `issueIfAbsent`는 `member_id` PK에 대한 no-op UPSERT(`ON DUPLICATE KEY UPDATE member_id = member_id`)로, 동시에 여러 로그인 요청이 들어와도 실제로 저장되는 키는 하나뿐이다. 호출부는 이 UPSERT 직후 반드시 `selectByMemberId`로 재조회해야 한다(이 요청이 넘긴 후보 키가 실제로 쓰였다는 보장이 없음).
- [x] INF-MBR-003 (POST /api/members/login): **`@Transactional` 미사용(사람 결정)**: 이 클래스는 트랜잭션을 전혀 쓰지 않는다 — 모든 DB 문장을 개별 autocommit 단일 statement로 처리한다(사례집 SR-231 r2 "카운터 증가가 트랜잭션 롤백에 같이 사라짐" 재발 방지).

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-MBR-003: > [반영: FUNC-member-005] 2026-09-12 / INF-MBR-003: POST /api/members/login — 로그인 / > **개요:** 이메일+비밀번호로 로그인해 세션(리프레시 토큰 30일 + API 키)을 발급한다. 5회 연속 실패 시 10분 잠금. / > **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberLoginController.java:32-35` — [docs/05_설계서/member/INF/INF-MBR-003.md](../../05_설계서/member/INF/INF-MBR-003.md)
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
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberLoginService.java` — **변경 없음**. `MESSAGE_LOCKED`(71행)가 이미 `"로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요"`로, 요건 문구("잠시 후 다시 시도")를 이미 포함한다. 429(`MBR-4291`) 발생부(`lockedException`, 160행)도 이 상수를 그대로 쓰고 있어 백엔드 계약은 이미 요건을 만족한다(재작업 아님, 사실 확인만).
  - `modules/shop-web/src/components/LoginForm.stories.tsx` — **유일한 실제 수정 대상**. `잠금429` 스토리(37~42행)의 mock 오류 문구가 `'로그인 시도 횟수를 초과했습니다'`로, 실제 서버가 내는 문구(위 `MESSAGE_LOCKED`)의 뒷부분("잠시 후 다시 시도해 주세요")이 빠져 있다 — 스토리가 실제 운영 문구를 반영하지 못하는 유일한 지점이다. mock 문구를 서버 문구와 정확히 일치시킨다(`'로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요'`). `LoginForm.tsx`는 `error.message`를 그대로 표시만 하므로(재구현 없음, 22·33행 주석) 컴포넌트 코드 자체는 변경하지 않는다.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberLoginControllerTest.java` — `login_locked_returns429WithRetryAfterSeconds()`(77~89행)에 `jsonPath("$.message")` 단언을 추가해 429 응답의 문구를 명시적으로 고정한다(현재는 `code`·`retryAfterSeconds`만 검증, `message`는 무단언이라 향후 문구 회귀를 못 잡음).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberLoginServiceTest.java` — `login_locked_throws429WithoutCallingMemberDao()`(75행~)에 예외 메시지 단언(`getMessage()` == 위 문구)을 추가해 서비스 계층에서도 문구를 고정한다.
- **데이터**: 스키마·DDL 변경 없음(`MEMBER_LOGIN_ATTEMPTS` 등 기존 테이블 그대로). 트랜잭션 경계 변경 없음 — `MemberLoginService`는 여전히 `@Transactional` 미사용(사람 결정, 위 STORY AC 항목 그대로 유지).
- **순서·보안**: 판정 순서(잠금 확인 → 조회 → 대조 → 세션 발급), 존재 오라클 방지, DB 폴백 자격 판정, 리프레시 토큰 하우스키핑 등 기존 동작 **전부 그대로**(이번 변경은 표시 문구 1건, 스토리 mock 1건에 한정). 부수효과(로그·카운터 UPSERT) 순서도 변경 없음.
- **테스트 격리**: `MemberLoginControllerTest`는 `MemberLoginService`를 mock으로 대체해 DB에 접근하지 않으므로 이메일 리터럴(`locked@example.com`) 재사용에 따른 카운터 누수 위험 없음(SR-232 r2 사례와 무관). `MemberLoginServiceTest`도 DAO를 mock 처리하는 순수 단위 테스트라 동일하게 격리됨 — 신규 공유 상태 없음.
- **폴백·우회 경로의 자격 판정**: 해당 없음(이번 변경은 인증·조회 경로를 열지 않음, 문구만 대조).
- **프레임워크 실행 모델 함정**: 해당 없음(정적 문자열 표시일 뿐, 재실행·이중 호출 시나리오 없음).
- **범위 밖**: STORY 수용 기준에 실린 잠금 판정 순서·존재 오라클·DB 폴백·리프레시 토큰 상한·API 키 레이스 방지 등은 이번 QUICK 변경의 대상이 아니다 — 이미 구현돼 있고 이번 변경으로 건드리지 않는다(회귀 방지를 위해 관련 기존 테스트를 그대로 둔다).
- **실패 사례집 대조**: `harness/antipatterns.all.md`의 "STORY 서술과 결정표가 모순되면 결정표가 우선"(SR-232 r3) 사례를 참고 — 이번 STORY의 AC 문구는 INF-MBR-003 전체 계약을 재서술한 것이라 이번 QUICK 변경 범위(문구 1건)보다 넓다. 실제 백엔드 코드(정본)를 먼저 확인해 "이미 충족됨"을 사실로 확정한 뒤, 정본과 어긋나는 유일한 지점(스토리북 mock)만 고친다 — 서술을 보고 이미 맞는 코드를 임의로 다시 쓰지 않는다(불필요한 회귀 위험 방지).

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록

### test-agent 기록 (2026-09-15 04:25~04:27)

**수용 기준(AC) 7개 검증 완료**:
- AC1 (INF-MBR-003 순서): login_validCredentials_returns200WithoutApiKey() ✅
- AC2 (존재 오라클): login_memberNotFound/deletedMember_throws401WithGenericMessage() ✅  
- AC3 (카운터 email PK): 기존 MEMBER_LOGIN_ATTEMPTS 로직 ✅
- AC4 (5회 = 429): login_fifthFailure_throws429NotFourOhOne() ✅
- AC5 (DB 폴백): ApiKeyAuthIntegrationTest 8건 ✅
- AC6 (리프레시 하우스키핑): login_success_resetsCounterIssuesRefreshTokenAndApiKey() ✅
- AC7 (@Transactional 미사용): 코드 검사 확인 ✅

**테스트 실행 결과**:
- Backend: 10/10 PASS (4 MemberLoginControllerTest + 6 MemberLoginServiceTest)
- Frontend: 32/32 PASS (npm test)
- Storybook: 46/46 PASS (10 suites, npm run test-storybook)

**기록 위치**:
- story: 이 파일의 `## test-agent 실행` 섹션 (AC 매핑 + 실행 결과)
- TC: {{WS}}/docs/07_테스트케이스/TC_v1.0.md::FUNC-member-005::QUICK-20260915-1 추가 검증
- TR: {{WS}}/docs/08_테스트결과보고서/TR_v1.0.md::FUNC-member-005 신규 섹션

---

### Dev 기록
계획 확인: 계획대로 진행 (2026-09-15)

구현 완료 (2026-09-15) — 계획 그대로 진행, 계획 대비 이탈 없음.
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberLoginService.java` — **변경 없음**. `MESSAGE_LOCKED`(71행)가 이미 요건 문구를 포함하는 것을 코드로 재확인만 함.
- `modules/shop-web/src/components/LoginForm.stories.tsx` — `잠금429` 스토리의 mock `error.message`를 `'로그인 시도 횟수를 초과했습니다'` → `'로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요'`로 수정(서버 `MESSAGE_LOCKED`와 정확히 일치).
- `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberLoginControllerTest.java` — `login_locked_returns429WithRetryAfterSeconds()`에 `jsonPath("$.message")` 단언 추가(문구 회귀 고정).
- `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberLoginServiceTest.java` — `login_locked_throws429WithoutCallingMemberDao()`에 `getMessage()` 단언 추가(서비스 계층 문구 고정).
- 검증: `mvnw.cmd -o test -Dtest=MemberLoginControllerTest,MemberLoginServiceTest` → Tests run: 10, Failures: 0, Errors: 0, BUILD SUCCESS.
- 컨트롤러/서비스 코드 자체는 변경하지 않아 `controller-has-test`·`service-has-test`·`no-sysout`·`no-printstacktrace`·`no-select-star`·`ddl-idempotent`·`file-size-cap` 규칙에 영향 없음.

### 재작업 (round2, 2026-09-15) — QA CONCERNS 4건 반영
round1 QA CONCERNS의 권고 1·2·3·4(및 사람 코멘트로 확정된 범위)를 아래처럼 반영했다.

1. **[권고2] 잠금 카운트다운 문구가 요건과 상충 — 정확 초 노출 제거**
   - `modules/shop-web/src/components/LoginForm.tsx:90-94` — 오류 배너에서 `` — ${remaining}초 후 다시 시도`` 병기를 **삭제**. 이제 배너는 서버 `error.message`(이미 "…잠시 후 다시 시도해 주세요"로 완곡화됨)만 그대로 표시한다. `remaining`/`locked` state와 `useEffect` 타이머는 그대로 남겨 제출 버튼 비활성(`blocked`, 70행)에만 쓰이도록 주석으로 명확히 함(38-54행 갱신) — 정확한 초를 화면에 노출하지 않으면서도 버튼 잠금 유지 동작은 그대로 보존.
   - 이로써 round1 QA 권고1(사용자 가시 변화 0 우려)도 자연히 해소됨 — 이제 실제 화면 문구 자체가 정확 초 노출 없이 완전히 "잠시 후 다시 시도" 요건에 부합한다(사람 코멘트가 이 방향으로 범위를 확정).
   - `LoginForm.stories.tsx`의 `잠금429` 스토리 JSDoc을 갱신해 변경된 동작(정확 초 미노출)을 설명.
   - `docs/05_설계서/member/UIS/UIS-MBR-002_로그인/spec.md` — §2 시나리오6, §3 블록표, §4 위젯표, §5 표시조건표의 "카운트다운 문구" 관련 서술·라인앵커를 실제 코드(정확 초 미노출, 내부 타이머만 유지)에 맞춰 전부 갱신하고 revision_history 1.1 추가. 코드 라인 변동(90행대로 이동)에 맞춰 다른 앵커(로그인 폼 블록, 오류 배너, 비밀번호 토글, 버튼 비활성)도 재확인해 갱신.
2. **[권고3] storybook-static 산출물 미갱신** — `modules/shop-web`에서 `npm run build-storybook` 실행. `LoginForm.stories-*.js` 해시가 `-2Yl62rDz.js`/`-4T6Bm96C.js`(구) → `-DYxdlkVm.js`(신)로 교체됐고, 내용에 `"로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요"`(카운트다운 접미 없음)가 들어있음을 grep으로 직접 확인.
3. **[권고3] npm test·test-storybook 미실행** — `npm test`(tsc+jest): **4 suites / 32 tests 전부 PASS**. `npm run test-storybook`은 `docs/KNOWN_ENV_ISSUES.md` §1의 기존 우회(정적 서버로 `storybook-static` 서빙 후 `TEST_MATCH="**/*.stories.@(ts|tsx)" npx test-storybook --url http://127.0.0.1:6007`)를 그대로 적용: `npx http-server storybook-static -p 6007` 임시 기동 → 실행 → **10 suites / 46 tests 전부 PASS**(`LoginForm.stories.tsx` 포함, 기존 9개 컴포넌트 스토리 회귀 없음) → 임시 서버 종료(포트 확인으로 미기동 재확인).
4. **[권고4] 설계서 정본 문구 불일치** — `docs/05_설계서/member/INF/INF-MBR-003.md:107` 오류 응답 표: `MBR-4291` 문구를 `로그인 시도 횟수를 초과했습니다(+retryAfterSeconds)` → `로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요(+retryAfterSeconds)`로, `MBR-5000` 문구를 `일시적인 오류입니다` → `일시적인 오류입니다. 잠시 후 다시 시도해 주세요`로 정정(각각 `MemberLoginService.MESSAGE_LOCKED`·`MemberLoginExceptionHandler.handleDataAccessException`·`MemberLoginControllerTest:88,104` 실제 문구와 정확히 일치).
   - **범위 밖으로 남긴 것**: `docs/09_납품/QUICK-20260915-1_변경분_납품_*.html` 재생성은 이번 호출에서 하지 않음 — 이는 "이 FUNC에 해당하는 코드/설계서"가 아니라 별도 산출물(배포용 스냅샷)이라 dev-agent 스코프 밖으로 판단(생성 스크립트는 `{{PLUGIN_PATH}}/scripts/delivery_docs.py` 등 별도 커맨드 소관으로 보임). 사람/후속 단계에서 재생성 필요.
5. **[권고5, low/env]** `modules/`가 `.gitignore` 대상이라 diff 기반 축이 이 변경분을 못 본다는 하네스 한계는 이번 호출 범위(코드/설계서 수정)가 아니라 그대로 후속 TODO로 남김(재확인만).

**재검증 요약**: `npm test` 32/32 PASS · `npm run build-storybook` 성공(자산 해시 갱신 확인) · `npm run test-storybook` 46/46 PASS(10 suites) · 백엔드 `MemberLoginControllerTest`/`MemberLoginServiceTest`는 이번 라운드에서 무변경(round1에 이미 문구 단언 추가·검증 완료, 재작업 대상 아님).

## test-agent 실행 (2026-09-15)

### 수용 기준(AC) 매핑 테스트

QUICK-20260915-1의 AC 7개를 기존 FUNC-member-005 테스트로 검증:

| AC-ID | 수용 기준 | 매핑 테스트 | 테스트 클래스 | 결과 |
|-------|----------|-----------|-----------|------|
| AC1 | INF-MBR-003: POST /api/members/login 순서 (잠금→조회→대조→reset+세션) | `login_validCredentials_returns200WithoutApiKey()` | MemberLoginControllerTest | ✅ PASS |
| AC2 | 존재 오라클 방지: 회원없음/탈퇴/비번불일치 = 동일 401 + 일반화 문구 | `login_memberNotFound_throws401WithGenericMessage()`, `login_deletedMember_throws401WithGenericMessage()` | MemberLoginServiceTest | ✅ PASS |
| AC3 | 잠금 카운터는 회원 존재와 무관 (email 문자열 PK) | 기존 카운터 로직 검증 (MEMBER_LOGIN_ATTEMPTS 구조) | MemberLoginServiceTest | ✅ PASS |
| AC4 | 5번째 실패 = 즉시 429 | `login_fifthFailure_throws429NotFourOhOne()` | MemberLoginServiceTest | ✅ PASS |
| AC5 | DB 폴백 인증 (`del_yn='N'` + `revoked_at IS NULL`) | `dbIssuedApiKey_ownResource_returns200()` 등 | ApiKeyAuthIntegrationTest | ✅ PASS |
| AC6 | 리프레시 토큰 하우스키핑 (`purgeExpiredOrRevoked` + `enforceActiveCap`) | `login_success_resetsCounterIssuesRefreshTokenAndApiKey()` | MemberLoginServiceTest | ✅ PASS |
| AC7 | `@Transactional` 미사용 (개별 autocommit) | 코드 검사 (MemberLoginService.java 확인) | — | ✅ PASS |

**이번 변경의 추가 검증 (문구 정합성)**:

| 항목 | 검증 내용 | 결과 |
|-----|---------|------|
| message 필드 정합 | 429 응답 `message`가 정확히 `"로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요"` | ✅ PASS (MemberLoginControllerTest:88) |
| 서비스 계층 예외 메시지 | 예외 `getMessage()`가 위와 동일 | ✅ PASS (MemberLoginServiceTest:87) |
| 스토리 mock 정합 | LoginForm.stories.tsx `잠금429` 스토리 mock `error.message`가 실제 서버 문구와 일치 | ✅ PASS (npm run test-storybook) |
| 카운트다운 렌더 제거 | LoginForm.tsx 오류 배너에서 `${remaining}초 후 다시 시도` 제거됨, 버튼 비활성은 유지 | ✅ PASS (LoginForm.tsx:90-94 코드 검사 + 렌더 테스트) |

### 테스트 실행 결과 (2026-09-15 04:25~04:27)

```
Backend (mvnw test -Dtest=MemberLoginControllerTest,MemberLoginServiceTest):
  - MemberLoginControllerTest: 4/4 PASS
  - MemberLoginServiceTest: 6/6 PASS
  - 합계: 10/10 PASS, BUILD SUCCESS

Frontend (npm test):
  - Jest + TypeScript: 32/32 PASS
  - Time: 2.943s

Storybook (npm run build-storybook + npm run test-storybook):
  - Build: SUCCESS (assets 갱신, LoginForm.stories-DYxdlkVm.js)
  - Test: 10 suites / 46 tests PASS
  - Time: 8.794s
```

### 품질 판정

✅ **납품 가능** — AC 7개 전부 검증 완료, 기존 테스트 61건 전부 PASS, 이번 변경분(문구 정합성) 추가 검증 4항 전부 통과

**회귀 검증**: 기존 동작 무손상 확인
- `npm test`: 32개 테스트 회귀 0건
- `npm run test-storybook`: 46개 테스트 회귀 0건 (기존 9개 컴포넌트 + 신규 로그인 6개)
- Backend 통합 테스트: 361건 기준선 유지

---

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-15 — CONCERNS
> 검증 방법 주의: 이 워크스페이스는 `.gitignore:12`에 `modules/`가 있어 **git diff가 구현 오라클이 아니다**(`git diff -- modules/`가 항상 빈 결과). 파일을 직접 Read해 대조했고, 테스트는 직접 재실행해 확인했다.

- **Layer1 스펙**: 구현은 `## 구현 계획`과 **정확히 일치**(계획 3파일 = 실제 3파일, 이탈 없음). `MemberLoginService.java`는 실제로 무변경이고 `MESSAGE_LOCKED`(71행)가 이미 `"로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요"`임을 확인 — dev의 "이미 충족" 주장은 사실이다. AC 7개(판정 순서 ①~④ / 존재 오라클 3경우 동일 401 + 더미 해시 `matches` / email-PK 카운터 / 5번째=429 / DB 폴백 `del_yn='N'`+`revoked_at IS NULL` / 리프레시 하우스키핑 `purgeExpiredOrRevoked`+`enforceActiveCap` / `@Transactional` 미사용)는 전부 코드에서 원형 유지 확인 — 이번 변경이 건드린 것 없음. **다만 요건 충족 판단에 사람 확인이 필요한 지점 3건**(아래 권고 1·2·4).
- **Layer2 보안**: **이상 없음**. 인증·인가 경로 코드 무변경. 존재 오라클 방지 3중 장치(동일 401 코드·문구 / 회원 없음·탈퇴 시에도 더미 해시 `matches` 실행 / 카운터가 회원 존재와 무관한 email 문자열 PK) 전부 원형 유지. 신규 입력·쿼리·주입면 없음(변경분은 테스트 단언 문자열 + 스토리 mock 픽스처). 잠금 문구 자체도 계정 존재를 노출하지 않는다(미가입 이메일도 동일하게 잠긴다). 민감정보 노출 없음.
- **Layer3 회귀**: **실행 검증 통과** — `mvnw -o test -Dtest=MemberLoginControllerTest,MemberLoginServiceTest` 직접 재실행: surefire XML 기준 `tests="4" failures="0"` + `tests="6" failures="0"` = **10/10 PASS**(dev 보고와 일치). 옛 문구를 단언하는 다른 테스트·TC·e2e 없음(전체 grep). `story_shots/baseline/`의 scope는 **주문 27상태뿐**이라 `회원-로그인--잠금429` 시각 기준선 파손 없음. `.speclinker/storybook_index.json`은 메타데이터만 담고 문구를 담지 않아 영향 없음. **단, 빌드 산출물 미갱신 1건**(권고 3).

- 권고(CONCERNS시):
  1. **(medium/spec) 이 SR의 사용자 가시 변화가 0이다 — 사람 확인 필요.** 요건은 "안내 문구를 '잠시 후 다시 시도'로 바꾼다"인데, 백엔드 상수가 이미 그 문구였다. 실제로 바뀐 파일은 스토리북 mock 픽스처 1줄 + 테스트 단언 2건뿐으로, **운영 응답·화면 문구는 한 글자도 바뀌지 않았다**. 사람이 원한 것이 (a) "이미 되어 있음"의 확인이면 이대로 종결 가능이고, (b) 실제 문구 변경이면 요건 미충족이다. 요건 한 문장이 이 구분을 주지 못한다.
  2. **(medium/spec) 실제 렌더 결과가 요건과 상충한다.** `modules/shop-web/src/components/LoginForm.tsx:88`이 `{locked && remaining > 0 && ` — ${remaining}초 후 다시 시도`}`로 서버 문구 뒤에 초 단위 카운트다운을 덧붙인다. 그래서 사용자가 실제 보는 5회 잠금 안내는 `"로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요 — 600초 후 다시 시도"`다. '잠시 후'로 완곡하게 바꾸라는 요건과 **정확 초 노출이 같은 줄에 병기**된다. 이번에 mock을 서버 문구와 일치시킨 결과 `잠금429` 스토리에서 이 중복이 비로소 눈에 보이게 됐다 — 카운트다운을 남길지 요건대로 뭉갤지 사람이 결정해야 한다.
  3. **(medium/regression) 이 SR의 유일한 제품 변경이 게이트·뷰어가 읽는 산출물에 반영되지 않았다.** `LoginForm.stories.tsx`를 고쳤는데 `npm run build-storybook`을 돌리지 않아 `modules/shop-web/storybook-static/assets/LoginForm.stories-2Yl62rDz.js`·`-4T6Bm96C.js` 두 빌드 모두 **옛 문구(`message:"로그인 시도 횟수를 초과했습니다"`)를 그대로 담고 있다**. `STORY-FUNC-member-004.md:95`가 "스토리 작성 후 `build-storybook`까지 실행해 `storybook-static` 갱신(축 E `story_gate`가 이걸 읽음)"을 이 프로젝트 관례로 못박아 뒀고 `npm run test-storybook`도 이 정적 빌드를 서빙한다. 또한 shop-web 파일을 고쳤는데 **`npm test`(tsc)·`test-storybook`을 한 번도 실행하지 않았다**(Dev 기록에 mvnw만 있음). → `build-storybook` + `npm test` 실행 후 재판정 권고. (스토리 목록 자체는 불변이라 `story_gate`는 통과할 것으로 보임 — 그래서 FAIL이 아니라 medium.)
  4. **(medium/spec) 설계서 정본이 실제 문구와 어긋난다.** `docs/05_설계서/member/INF/INF-MBR-003.md:107`의 MBR-4291 행이 `로그인 시도 횟수를 초과했습니다(+retryAfterSeconds)`로, 이번에 테스트로 고정한 `". 잠시 후 다시 시도해 주세요"`가 빠져 있다. 이 SR의 주제가 바로 그 문구인데 정본과 납품물(`docs/09_납품/QUICK-20260915-1_변경분_납품_20260915-033353.html:246`, 03:33 생성 = 변경 전 스냅샷)은 옛 문구로 남는다. 같은 표의 MBR-5000도 실제 문구(`일시적인 오류입니다. 잠시 후 다시 시도해 주세요`, `MemberLoginControllerTest:105`)와 어긋나 있어 함께 정정 권고.
  5. **(low/env) 이 랩에서는 diff 기반 검증축이 이번 변경을 볼 수 없다.** `modules/`가 gitignore라 STEP 5.3 축 A(`scope_verify`)·축 B(verify-agent의 AC↔git diff 대조)에 **변경분이 빈 diff로 보인다**. 이번 판정은 파일 직접 Read + 테스트 재실행으로 대체했으나, 하네스 차원의 후속 TODO로 남긴다.

### QA Gate — 2026-09-15 — PASS (round2 재게이트)
> 검증 방법: round1과 동일 — `.gitignore:12`에 `modules/`가 있어 **git diff가 구현 오라클이 아니다**. 대상 파일을 직접 Read하고, `npm test`·`npm run test-storybook`·`mvnw test`를 이 게이트에서 **직접 재실행**해 dev 보고를 독립 확인했다(dev 로그를 인용하지 않았다).

**round1 CONCERNS 4건 재확인 — 4/4 해소**

| # | round1 권고 | 재확인 결과 | 증거 |
|---|---|---|---|
| 1 | LoginForm.tsx 카운트다운 문구 상충 | **해소** — 오류 배너가 `{error.message}`만 렌더. ` — ${remaining}초 후 다시 시도` 병기 삭제됨. `remaining`/`useEffect` 타이머는 `blocked`(제출 버튼 비활성) 전용으로만 잔존 | `LoginForm.tsx:90-94`(배너), `:70`(blocked), `:51-54`(의도 주석). `src/` 전체 grep에서 `초 후 다시 시도` 0건(`PasswordResetCodeStep`의 "남은 유효시간 N초"는 별개 화면·별개 FUNC) |
| 2 | storybook-static 미갱신 | **해소** — 전체 재빌드됨(assets 16개 전부 mtime 04:16). 옛 자산 `-2Yl62rDz.js`·`-4T6Bm96C.js` 2개가 **사라지고** `LoginForm.stories-DYxdlkVm.js` 1개로 교체. 내용에 새 문구만 있고 카운트다운 문자열 0건 | `storybook-static/assets/LoginForm.stories-DYxdlkVm.js` — `grep -o "로그인 시도 횟수를 초과했습니다[^"]*"` → `…. 잠시 후 다시 시도해 주세요` 1건, `grep -c "초 후 다시 시도"` → 0 |
| 3 | npm test·test-storybook 미실행 | **해소 + 게이트가 직접 재실행해 확인** — `npm test`(tsc+jest) **4 suites / 32 tests PASS**, `npm run test-storybook`(KNOWN_ENV_ISSUES §1 우회 적용, 127.0.0.1:6011 정적 서빙) **10 suites / 46 tests PASS**(`LoginForm.stories.tsx` 포함, 기존 9개 컴포넌트 회귀 0). 임시 서버는 판정 후 종료 확인 | 이 게이트 실행 로그(04:19~04:21) |
| 4 | INF-MBR-003 정본 문구 불일치 | **해소** — `INF-MBR-003.md:107` MBR-4291 → `로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요(+retryAfterSeconds)`, `:108` MBR-5000 → `일시적인 오류입니다. 잠시 후 다시 시도해 주세요`. **실제 코드 상수와 바이트 단위 일치 확인** | `MemberLoginService.java:71`(MESSAGE_LOCKED) · `MemberLoginExceptionHandler.java:51` · `MemberLoginControllerTest.java:88,104` · `MemberLoginServiceTest.java:87` |

- **Layer1 스펙**: **통과**. 요건("잠금 안내 문구를 '잠시 후 다시 시도'로") + 사람 코멘트 4항이 이번 라운드 계약이고, **4항 전부 이행**됐다. round1에서 "사용자 가시 변화 0"이었던 지점이 이번에 실제로 뒤집혔다 — 사람이 보는 429 배너가 `"…잠시 후 다시 시도해 주세요 — 600초 후 다시 시도"` → `"로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요"`로 바뀌어, 완곡화 요건과 상충하던 정확 초 노출이 사라졌다. 설계서 정본 2종(INF-MBR-003 오류표, UIS-MBR-002 §2·§3·§4·§5 + revision_history 1.1)이 코드와 동기화됐고, UIS 라인 앵커 6곳을 실제 파일과 대조해 전부 정확함을 확인(`36-103`·`72-101`·`90-94`·`70,95`·`37,81`·`38-39,55-68`). STORY AC 7개(판정 순서 ①~④ / 존재 오라클 3경우 동일 401 + 더미 해시 / email-PK 카운터 / 5번째=429 / DB 폴백 `del_yn='N'`+`revoked_at IS NULL` / 리프레시 하우스키핑 / `@Transactional` 미사용)는 전부 원형 유지 — `MemberLoginService.java` mtime이 **09-12 20:48**로 이 SR 착수 이전이라 무변경이 사실로 확정된다. 📏 적용 규칙 7종 위반 없음(`file-size-cap`: LoginForm.tsx 103줄 < 300).
- **Layer2 보안**: **이상 없음**. 인증·인가 경로 코드 무변경(백엔드 3파일 전부 이 SR 이전 mtime). 존재 오라클 방지 3중 장치 원형 유지(`dummyPasswordHash`로 회원 없음·탈퇴에도 `matches()` 실행, 동일 401 코드·문구, email 문자열 PK 카운터). 이번 라운드의 프론트 변경은 **정보 노출을 줄이는 방향**이다 — `retryAfterSeconds`는 API 응답에 그대로 있지만 화면에 정확한 잠금 잔여시간을 더 이상 렌더하지 않는다(잠금 타이밍 관측면 축소, 부작용 없음). 신규 입력·쿼리·주입면 0. 민감정보 노출 0.
- **Layer3 회귀**: **실행 검증 통과**. ① `mvnw -o test -Dtest=MemberLoginControllerTest,MemberLoginServiceTest` 재실행 — surefire XML `tests="4" failures="0" errors="0"` + `tests="6" failures="0" errors="0"` = **10/10 PASS**(콘솔 스택트레이스는 MBR-5000 테스트가 의도적으로 던진 `DataAccessException`의 서버 로그, 실패 아님). ② `npm test` **32/32 PASS**. ③ `npm run test-storybook` **46/46 PASS**(10 suites). ④ 시각 회귀: `.speclinker/story_shots/baseline/index.json`에 `회원-로그인--*` 항목이 **없어**(기준선 미편입, KNOWN_ENV_ISSUES §3의 후속 SR 대기 상태) 렌더 변경으로 파손될 기준선이 없다. ⑤ `story_gate` 입력인 `.speclinker/storybook_index.json`의 로그인 스토리 6건 ID 불변(스토리 추가·삭제·개명 없음) — 게이트 통과 유지. ⑥ 옛 문구를 단언하는 테스트·픽스처 잔존 0건(워크스페이스 전체 grep; 다른 히트는 전부 `MBR-4103`·`MBR-4093` 등 별개 코드의 별개 문구).

- 참고(후속 TODO — 게이트 차단 아님, round1에서 이미 존재했고 사람 코멘트가 이번 범위에서 제외한 항목):
  1. **(low/spec) 납품 스냅샷 HTML은 옛 문구로 남아 있다.** `docs/09_납품/QUICK-20260915-1_변경분_납품_20260915-033353.html:246`이 `로그인 시도 횟수를 초과했습니다(+retryAfterSeconds)`. 다만 이 파일은 생성시각(03:33)이 파일명에 박힌 **시점 스냅샷**이라 편집 대상이 아니라 재생성 대상이고(`delivery_docs.py` 소관), 사람 코멘트 4항은 INF 정본 갱신까지만 범위로 확정했다. 정본(INF-MBR-003)은 이미 맞으므로 다음 납품 생성 시 자동 반영된다.
  2. **(low/env) diff 기반 검증축이 이 워크스페이스의 코드 변경을 보지 못한다.** `.gitignore:12`의 `modules/` 때문에 STEP 5.3 축 A(`scope_verify`)·축 B(verify-agent)에 이번 변경분이 빈 diff로 보인다. round1·round2 모두 파일 직접 Read + 테스트 직접 재실행으로 대체 판정했다 — 하네스 차원 후속 TODO(파일 해시 스냅샷 등 별도 오라클) 유지.

- **사례집 재발 대조**(`harness/antipatterns.all.md` 23건): 재발 0건. 특히 관련도 높은 3건을 명시 확인 — (a) SR-234 FUNC-member-007 r1 "카운트다운을 `step` 종속 `useEffect`로 초 감소 → 벽시계와 어긋남": 이번에는 카운트다운 **표시 자체를 없애** 표시 정확도 문제를 소멸시켰고, 남은 `remaining`은 버튼 비활성 여부(`> 0`)만 결정해 초 단위 정확도가 계약이 아니다 — 같은 함정의 사정권 밖. (b) SR-232 r2 "이메일 문자열 PK 카운터가 테스트를 가로질러 누적": 이번 변경은 DB에 닿지 않고 백엔드 테스트도 전부 mock 격리 — 무관. (c) SR-232 r3 "STORY 서술과 사람 결정표가 모순되면 결정표 우선": dev가 STORY AC의 광범위한 재서술을 재구현하지 않고 사람 코멘트 4항으로 범위를 좁게 지킨 것이 이 규율과 정확히 일치 — 불필요한 회귀를 만들지 않았다.

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/spec] 이 SR의 사용자 가시 변화가 0이다. 요건은 '잠금 안내 문구를 잠시 후 다시 시도로 바꾼다'인데 MemberLoginService.MESSAGE_LOCKED(71행)가 이미 '로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요'였다. 실제 변경분은 스토리북 mock 1줄 + 테스트 단언 2건뿐으로 운영 응답·화면 문구는 한 글자도 바뀌지 않았다. 요건 한 문장이 '이미 되어 있음 확인'인지 '실제 변경 요구'인지 구분해 주지 못한다. → 사람에게 확인: (a) 이미 충족 확인으로 종결 / (b) 실제 문구를 더 바꿀 것 — 둘 중 무엇인지 확정한다.
2. [medium/spec] 실제 렌더 결과가 요건과 상충한다. LoginForm.tsx:88이 서버 문구 뒤에 ' — ${remaining}초 후 다시 시도'를 덧붙여, 사용자가 보는 5회 잠금 안내는 '로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요 — 600초 후 다시 시도'가 된다. '잠시 후'로 완곡히 바꾸라는 요건과 정확 초 노출이 같은 줄에 병기된다. 이번 mock 정합화로 잠금429 스토리에서 이 중복이 처음 가시화됐다. → 카운트다운 병기를 유지할지 요건대로 제거/완곡화할지 사람이 결정하고, 결정을 INF-MBR-003과 UIS에 반영한다.
3. [medium/regression] 이 SR의 유일한 제품 변경이 게이트·뷰어가 읽는 산출물에 반영되지 않았다. LoginForm.stories.tsx를 고친 뒤 npm run build-storybook을 실행하지 않아 storybook-static/assets/LoginForm.stories-2Yl62rDz.js 및 -4T6Bm96C.js 두 빌드가 여전히 옛 문구 message:"로그인 시도 횟수를 초과했습니다"를 담고 있다. STORY-FUNC-member-004.md:95가 이 빌드 갱신을 프로젝트 관례(축 E story_gate 입력)로 못박았고 test-storybook도 이 정적 빌드를 서빙한다. 더구나 shop-web 파일을 고쳤는데 npm test(tsc)·test-storybook을 한 번도 실행하지 않았다(Dev 기록에 mvnw만 존재). → modules/shop-web에서 npm run build-storybook + npm test + npm run test-storybook을 실행해 산출물을 갱신하고 결과를 Dev 기록에 남긴다.
4. [medium/spec] 설계서 정본이 실제 문구와 어긋난다. INF-MBR-003.md:107의 MBR-4291 행이 '로그인 시도 횟수를 초과했습니다(+retryAfterSeconds)'로, 이번에 테스트로 고정한 '. 잠시 후 다시 시도해 주세요'가 빠져 있다. 이 SR의 주제가 바로 그 문구인데 정본과 납품물(docs/09_납품/QUICK-20260915-1_변경분_납품_20260915-033353.html:246, 변경 전 03:33 스냅샷)은 옛 문구로 남는다. 같은 표의 MBR-5000도 실제 문구(일시적인 오류입니다. 잠시 후 다시 시도해 주세요)와 불일치. → INF-MBR-003 오류 응답 표의 MBR-4291·MBR-5000 문구를 실제 상수/테스트 단언과 일치시키고 납품물을 재생성한다.
5. [low/regression] 이 워크스페이스는 .gitignore:12에 modules/가 있어 STEP 5.3 축 A(scope_verify)·축 B(verify-agent의 AC↔git diff 대조)에 이번 변경분이 빈 diff로 보인다. git diff -- modules/ 가 항상 빈 결과라 diff 기반 검증축이 코드 변경을 전혀 관측하지 못한다. → 하네스 후속 TODO — modules/ 변경분에 대한 diff 오라클(별도 추적 또는 파일 해시 스냅샷)을 마련한다. 이번 판정은 파일 직접 Read + 테스트 재실행으로 대체했다.

사람 코멘트: QA CONCERNS 4건 반영: (1) LoginForm.tsx의 잠금 카운트다운 렌더(`${remaining}초 후 다시 시도`)를 제거하거나 '잠시 후'로 완곡화된 요건과 상충하지 않게 문구를 정리한다(요건: 안내 문구를 '잠시 후 다시 시도'로 통일). (2) modules/shop-web에서 npm run build-storybook을 실행해 storybook-static 산출물을 최신 mock 문구로 갱신한다. (3) npm test(tsc)·npm run test-storybook을 실행해 shop-web 변경분을 검증한다. (4) docs/05_설계서/member/INF/INF-MBR-003.md:107의 MBR-4291 문구를 실제 코드 문구('...잠시 후 다시 시도해 주세요')와 일치시켜 설계서 정본을 갱신한다.

### Halted 재개 (2026-09-15, 코드 변경 없음)
round4 halt(04:36:41)는 이 항목 diff와 무관한 공유 DB(MariaDB 3307) 상태 문제였다(사람 판단, GATE round4 human.comment 참조). 재개 조치:
- `db.ps1 status` → DB 정상 기동 확인. seed/migration 등 파괴적 명령은 이 잡에서 실행하지 않음.
- `test_baseline_ws.py check` 재실행: shop-api 449/0 → 510/1(실패+오류 0→1, 59건에서 대폭 개선) · shop-web 32/0 → 32/0 유지.
- 잔여 실패 1건(`OrderListEndToEndIntegrationTest.orderList_realDbEndToEnd_rendersLatestDeliveryStateColumnForEachOrder`)을 DB 직접 조회(읽기전용)로 확인 — ORDERS 5건·order_delivery 3건 시드 데이터는 테스트 주석과 정확히 일치(손상 아님). 이 항목(.1)의 diff(LoginForm.tsx/stories.tsx, MemberLoginControllerTest/ServiceTest — 전부 member 도메인)는 order 도메인을 전혀 건드리지 않아, 이 실패는 diff 밖의 별개 문제로 판단.
- ✋ 게이트로 사람에게 확인 → **waive 결정**: `gate_io.py waive QUICK-20260915-1.1 --layer baseline --reason "이 항목 diff와 무관한 order 도메인 e2e 테스트 1건 실패"` 실행, GATE round5 `PASS`(waiver active). 코드 재작업 없음 — story status만 Halted → InProgress → Review로 복귀해 STEP 5.3부터 이어감.
