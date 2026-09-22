---
story-id: STORY-FUNC-member-007
func-id: FUNC-member-007
status: Done
domain: member
created: 2026-09-13
spec_markers: 0
sr-id: SR-234
approved_sha: 9546a92b4e87
---

# STORY-FUNC-member-007 — SR-234 — 비밀번호 재설정 화면 · 신규 UIS-MBR-003 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-uis가 역생성)

## Story
SR-234 — 비밀번호 재설정 화면 · 신규 UIS-MBR-003 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-uis가 역생성)


## 변경 컨텍스트 (SR-234)
> 이 story는 변경요청 **SR-234 — SR-234** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-234/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-234/02_변경명세.md`

### 확정된 요건 문답 9건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: [유형: 화면+API] 이메일/휴대폰 인증 후 새 비밀번호 설정. 링크·코드는 10분 유효, 1회용. UI/UX: 현재 단계·남은 유효시간 표시 · 재전송은 60초 쿨다운. 수용 기준: 만료 링크는 '만료됨 + 다시 요청' · 재설정 후 모든 기기 로그아웃 / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 로그인(SR-232)·가입(SR-231) 흐름과 MEMBER_LOGIN_ATTEMPTS·리프레시 토큰 동작은 그대로. 재설정 완료 시 그 회원의 리프레시 토큰 전부 폐기(모든 기기 로그아웃)
- **기존 클라이언트와의 하위호환이 필요한가?** — 기존 필드명·타입·의미는 그대로 둔다(추가만)
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 기존 오류 계약(코드 MBR-4xxx + message) 그대로. 만료 링크/코드는 410 MBR-4101 '만료됨 — 다시 요청', 존재 여부는 새지 않게 요청 API는 항상 202
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — MEMBER_PASSWORD_RESETS 테이블 신설(대상 email/phone_norm · 코드 해시 · 만료 시각 · 소비 시각 · 시도 횟수 · 생성 시각) + MEMBERS.password_hash 갱신·updated_at. 기존 컬럼 의미 변경 없음
- **기존 데이터 이관·백필이 필요한가?** — Flyway V4__member_password_resets.sql — CREATE TABLE IF NOT EXISTS(랩 규칙 ddl-idempotent). 기존 행 이행 없음
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — shop-web에 비밀번호 재설정 화면 1개(요청→코드 확인→새 비밀번호 3단계, 현재 단계·남은 유효시간·재전송 60초 쿨다운 표시). 로그인 화면에 '비밀번호를 잊으셨나요' 링크만 추가
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 0건·오류·로딩 표시는 기존 규칙(UIS §5)을 그대로 따른다
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 바뀌거나 새로 생기는 상태마다 스토리를 남긴다(기존 스토리는 깨뜨리지 않는다)

### 구현 모듈(제약) — `shop-web` (`{{SRC_SHOP_WEB}}`)
이 FUNC의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약 폼에서 사람이 지정한 값).

### 계약 메모 — 008/009 API가 받는 target 표기 (사람 확정 2026-09-13)
- 휴대폰은 **숫자만**(`^01[016789][0-9]{7,8}$`) 보낸다 — 화면이 하이픈·공백을 제거해 보낸다. 하이픈 표기는 API가 400 MBR-4100으로 거부한다(가입 API와 같은 규칙).
- 이메일은 입력 그대로 보낸다(API가 trim+소문자로 정규화해 응답 `target`에 돌려준다).
- 코드 요청은 항상 202(존재 여부 노출 없음) — 화면은 "존재하지 않는 계정" 상태를 그리지 않는다.
- 2단계(코드)→3단계(새 비밀번호) 전이는 코드 형식(6자리 숫자)만 로컬 검증하고 실제 코드 검증은 3단계 confirm 호출에 원자 포함한다. confirm이 409 MBR-4102를 주면 2단계로 되돌아가되 입력한 새 비밀번호 값은 유지한다(사람 확정, 계획 확인 게이트 2026-09-13).

## 수용 기준 (Acceptance Criteria)
- [ ] AC1 3단계 화면(요청→코드 확인→새 비밀번호)과 현재 단계 표시
- [ ] AC2 휴대폰은 하이픈·공백을 제거해 숫자만 전송, 이메일은 그대로
- [ ] AC3 요청 202면 코드 단계로 — '존재하지 않는 계정' 상태는 없다
- [ ] AC4 남은 유효시간 카운트다운은 응답 `expiresInSeconds` 기준, 재전송 버튼은 60초 쿨다운 비활성+남은 초 표시
- [ ] AC5 확정 응답 매핑: 204→완료 화면('모든 기기에서 로그아웃됨') · 410 MBR-4101→'만료됨'+다시 요청 버튼(1단계로) · 409 MBR-4102→코드 오류 인라인(입력 유지) · 409 MBR-4103→시도 초과+다시 요청 · 400 MBR-4001→비밀번호 규칙 오류 인라인 · 400 MBR-4100→형식 오류 인라인
- [ ] AC6 로그인 화면에 '비밀번호를 잊으셨나요' 링크(기존 로그인 동작 무변경)
- [ ] AC7 부품·상태별 스토리(요청/코드/새 비밀번호/만료/시도초과/완료 — 규칙 `story-per-component`)
- [ ] AC8 테스트: jest+testing-library로 단계 전이·쿨다운 타이머·오류 매핑, 반드시 `npm test`(타입검사+jest)에 걸리게(RUN8 004 r2 재발 방지)
- [ ] AC9 React StrictMode(dev) 이중 effect에서 코드 요청 API가 두 번 나가지 않게 단일 비행 가드(RUN8 004 r1 재발 방지) — 테스트로 실증
- [ ] AC10 기존 OrderFilters 등 다른 부품·픽스처는 건드리지 않는다(SR-296 이월)

### 구현 방식(사람 확정)
- 새 부품은 `src/features/member/` 아래, API 호출은 기존 fetch 래퍼(X-Api-Key는 vite 프록시가 붙임) 재사용.

### 완료 조건(사람 확정)
- `npm test` 전량 통과(기준선 10/0 → 증가), 스토리 렌더 축E 통과.

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS** UIS-MBR-003
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)



## 구현 Task
- [x] 컨트롤러/핸들러 (해당 없음 — 이 FUNC은 프론트 전용, 백엔드 INF-MBR-006/007은 이미 Done)
- [x] 서비스/비즈니스 로직 (`PasswordResetPage.tsx` 상태기계 + `requestCodeOnce.ts` in-flight 가드)
- [x] 데이터 접근 레이어 (`api.ts` — `requestPasswordResetCode`/`confirmPasswordReset`/`postVoid`)
- [x] 단위 테스트 (`requestCodeOnce.unit.test.ts` · `PasswordResetPage.test.tsx`)

## Dev 기록
- 2026-09-13 승인(재승인): 008/009 계약 메모 포함 승인. 사람이 AC1~AC10 확정(3단계 화면·전화/이메일 포맷·확정 API 응답 코드 매핑·재전송 쿨다운·스토리·테스트·StrictMode 단일비행가드·타 부품 불가침). 구현은 `src/features/member/` 아래, 기존 fetch 래퍼 재사용. 완료 조건: `npm test` 전량 통과 + 스토리 축E 통과.
- 계획 확인: 계획대로 진행 (2026-09-13) — devDependencies 설치(testing-library 계열·jest-environment-jsdom, 이 FUNC 소속) 허용, 2단계 로컬 형식검증+3단계 confirm 원자검증 UX 수용(계약 메모에 반영 완료), jest.config testMatch 추가가 기존 unit.test.ts와 공존하는지 실측 지시.
- 2026-09-13 dev-agent 구현 완료 — 계획을 그대로 따랐다(이탈 없음). 생성/수정 파일(전부 `modules/shop-web`):
  - **수정**: `src/api.ts`(`parseErrorBody` 공유 추출 + `postVoid` 신설 + `requestPasswordResetCode`/`confirmPasswordReset` 추가), `src/types.ts`(`VerificationCodeResult` 추가), `src/App.tsx`(`/password-reset` 라우트 2줄), `src/pages/LoginPage.tsx`(`Link to="/password-reset"` 한 줄, `LoginForm.tsx` 자체는 무변경), `jest.config.cjs`(`testMatch`에 `**/*.test.tsx` 추가 + `setupFiles` 신설 + swc jsx `runtime: 'automatic'` 추가 — 아래 이탈 사유 참조), `package.json`(devDependencies 4종 추가, 버전은 `npm install`이 실제로 해석한 값으로 자동 정정됨: `@testing-library/react@^16.3.3` 등).
  - **신규**: `src/features/member/requestCodeOnce.ts`(+`.unit.test.ts`), `src/features/member/PasswordResetRequestStep.tsx`(+`.stories.tsx`), `src/features/member/PasswordResetCodeStep.tsx`(+`.stories.tsx`), `src/features/member/PasswordResetPasswordStep.tsx`(+`.stories.tsx`), `src/features/member/PasswordResetDoneStep.tsx`(+`.stories.tsx`), `src/pages/PasswordResetPage.tsx`(컨테이너, 스토리 대상 아님·+`.test.tsx`), `jest.setup.cjs`(신규, 아래 이탈 사유 참조).
  - **계획과의 이탈(2건, 코드 전 여기 기록)**:
    1. `@swc/jest` transform 옵션에 `jsc.transform.react.runtime: 'automatic'`을 추가했다 — 계획엔 없었지만, tsconfig가 `"jsx": "react-jsx"`(자동 런타임, `React` import 불필요)인데 swc 쪽 설정이 기본값(classic)이면 컴포넌트 테스트(.test.tsx)에서 JSX 컴파일이 `React is not defined`로 깨진다(실측). 기존 3개 `*.unit.test.ts`는 JSX가 없어 영향 없음.
    2. `jest.setup.cjs`를 신규로 만들고 `setupFiles`에 등록했다 — 계획엔 없었지만, `jest-environment-jsdom@30.5.1`(jsdom 26)이 `TextEncoder`/`TextDecoder`를 전역에 노출하지 않아 `react-router-dom` import 자체가 `ReferenceError: TextEncoder is not defined`로 테스트 스위트를 통째로 실패시켰다(실측, `PasswordResetPage.test.tsx`). Node 내장 `TextEncoder`/`TextDecoder`를 `global`에 채워 넣는 3줄짜리 가드로 해결 — node 테스트 환경(기존 3개)엔 이미 존재하므로 무해함을 확인(테스트 통과로 실증).
    3. (참고, 계획 범위 내 조정) jest-dom 매처 타입은 `import '@testing-library/jest-dom'`이 아니라 `import '@testing-library/jest-dom/jest-globals'`로 가져왔다 — 이 프로젝트가 전역 `expect`가 아니라 `@jest/globals`의 `expect`를 쓰므로(`redirectTarget.unit.test.ts`와 동일 관례), jest-dom v6의 서브패스 타입 증강이 `@jest/expect` 모듈을 대상으로 해야 tsc가 `toBeInTheDocument` 등을 인식한다(실측 tsc 에러로 확인 후 수정).
  - **`npm test` 실행 결과(2026-09-13, 2회 연속 실행으로 안정성 확인)**: `node scripts/typecheck.cjs && jest --config jest.config.cjs --passWithNoTests` → **Test Suites: 4 passed, 4 total / Tests: 27 passed, 27 total**(기존 3개 스위트 10건 + 신규 2개 스위트 17건 = 27건, 기준선 10/0 → 27/0 증가). 타입체크(`tsc --noEmit`)도 오류 0.
  - AC10(타 부품 불가침) 실측: `LoginForm.tsx`·`OrderFilters.tsx` 등 무변경 — 수정한 것은 `LoginPage.tsx`(페이지 컨테이너, 링크 1줄)뿐이다.
  - 규칙 점검(자체): `web-fetch-only-in-api`(부품·페이지 `fetch` 직접 호출 0건, `console.log`/`console.debug` 0건 — grep 실측) · `file-size-cap`(신규 tsx 전부 300줄 미만, 최대 `PasswordResetCodeStep.tsx` 92줄) · `story-per-component`(신규 부품 4종 모두 `.stories.tsx` 동반, `PasswordResetPage.tsx`는 `pages/`라 제외 대상).
- 2026-09-13 dev-agent 재작업(round2) 완료 — `## 재작업 지시` 4건(사람 확정, 구현 방식 그대로) 전부 반영. 수정 파일(전부 `modules/shop-web`, 새 파일 없음):
  - `src/pages/PasswordResetPage.tsx`:
    1. (지시1) `remainingSeconds`/`resendRemainingSeconds`를 상태로 직접 감소시키던 것을 제거하고, `expiresAt`/`resendAvailableAt`(epoch ms)와 `now`(매초 갱신) 상태로 바꿔 두 카운트다운을 매 렌더 `Math.max(0, Math.ceil((target-now)/1000))`로 파생시켰다. 카운트다운 `useEffect`의 의존성에서 `step` 조건을 빼 `requestSeq`가 있는 한 현재 단계와 무관하게 `now`가 흐른다 — 3단계(새 비밀번호) 체류 중에도 벽시계가 그대로 반영된다.
    2. (지시2) `handlePasswordSubmit`의 confirm 오류 switch에서 `MBR-4100`과 `default`를 분리했다. `MBR-4100`만 1단계 전이(방어적 경로 유지), `default`(미정의·네트워크·5xx)는 3단계를 유지한 채 `setPasswordError(err)`로 재시도 가능한 인라인 오류만 표시한다.
    3. (지시3) `handleResend` 성공 분기에 `setCode('')`·`setCodeError(null)`을 추가해 재전송 성공 시 옛 코드 입력·오류 문구를 지운다.
    4. (지시4) `handlePasswordSubmit`의 confirm 204 성공 직후 `clearSession()`(기존 `src/session.ts` 로그아웃 헬퍼, FUNC-member-004 소유·무변경)을 호출해 이 기기의 localStorage 세션을 지운다. `LoginForm.tsx`는 건드리지 않았다.
  - `src/features/member/PasswordResetPasswordStep.tsx`: `PasswordResetPasswordError.code` 타입을 `'MBR-4001' | 'MBR-4100'` 리터럴 유니언에서 `string`으로 넓혔다 — default 분기가 넘기는 미정의 코드(예: `MBR-5000`)를 그대로 담기 위함(컴포넌트의 렌더 로직 자체는 무변경, `error.message`만 표시).
  - `src/pages/PasswordResetPage.test.tsx`: 신규 케이스 5건 추가(지시1 1건 — 3단계 70초 체류 후 2단계 복귀 시 잔여 530초·재전송 열림 / 지시2 2건 — confirm 500·네트워크 오류 각각 3단계 유지+인라인 표시, 1단계로 미이동 실증 / 지시3 1건 — 재전송 성공 시 코드·오류 초기화 / 지시4 1건 — `saveSession`으로 세션을 미리 채운 뒤 confirm 204 후 `loadSession()`이 `null`임을 실증). `beforeEach`/`afterEach`에 `localStorage.clear()`를 추가해 지시4 테스트의 세션 상태가 다른 테스트로 새지 않게 했다(테스트 격리).
  - **1차 시도 실패→수정**: 지시3 테스트에서 `waitFor(fetchMock 호출 3회)` 직후 바로 `codeError` 소멸을 단언했다가 실패했다(`toHaveBeenCalledTimes`는 fetch 호출 시점에 이미 만족되지만, `setCode('')`/`setCodeError(null)`은 그 응답 Promise가 마저 settle된 뒤에 실행되는 후속 상태 갱신이라 타이밍이 어긋남) — `codeError` 소멸 자체를 `waitFor`로 재대기하도록 고쳐 해결(실측, 재시도 1회로 해결).
  - **`npm test` 실행 결과(2026-09-13, 2회 연속 실행)**: `node scripts/typecheck.cjs && jest --config jest.config.cjs --passWithNoTests` → **Test Suites: 4 passed, 4 total / Tests: 32 passed, 32 total**(기준선 27 → 32, 신규 5건 전부 포함). 타입체크 오류 0.
  - 규칙 재점검: `web-fetch-only-in-api`(수정 파일에 `fetch`/`console.*` 직접 호출 0건 — grep 실측) · `story-per-component`(신규 부품 없음, 기존 4개 스토리 무변경) · `file-size-cap`(`PasswordResetPage.tsx` 226줄 · `PasswordResetPasswordStep.tsx` 50줄, 둘 다 300줄 미만. `PasswordResetPage.test.tsx`는 314줄로 300줄 should 상한을 살짝 넘지만 새 케이스 5건 추가에 따른 테스트 파일 증가이고 must가 아니라 게이트 비차단으로 판단, 분할은 후속 필요 시 검토).
- **STEP 5.5 스펙 재동기화(2026-09-13)**: 이 FUNC이 신규 계약(화면)을 추가했으므로 `ddd-ui-agent`(소스폴백 모드 — UIS-MBR-002와 동일 사유로 `npm run dev` 미기동)를 디스패치해 `docs/05_설계서/member/UIS/UIS-MBR-003_비밀번호재설정/spec.md`를 신규 생성. 근거: `PasswordResetPage.tsx` 상태기계·4개 Step 부품+스토리·`requestCodeOnce.ts`·`api.ts`(INF-MBR-006/007 소비)·`LoginPage.tsx`/`App.tsx` 변경분. STORY AC5 응답코드→화면전이 표를 그대로 옮겨 §4 근거로 삼음(재해석 없음). `spec_staleness.py --baseline --only=UIS-MBR-003` 실행 완료(exit 0) — 재동기화한 스펙이 `/sl-sync`에서 곧바로 stale로 재검출되지 않도록 기준선 갱신.
(qa-agent가 이어서 gate 판정 기록)

## TC 검증 및 테스트 실행 (test-agent — 2026-09-13)

> test-agent가 AC별 TC 매핑 검증 + 테스트 실행 후 보고.

### AC ↔ TC 매핑 현황

| AC | 요구 | TC 커버 | 상태 |
|---|---|---|---|
| AC1 | 3단계 화면(요청→코드→비밀번호)·현재 단계 표시 | `초기 렌더는 1단계만 보인다` + 단계 전이 3건(202/재전송/코드) | ✓ |
| AC2 | 휴대폰 하이픈·공백 제거/숫자만 전송, 이메일은 그대로 | `휴대폰은 하이픈·공백을 제거해 숫자만` + `이메일은 입력 그대로` | ✓ |
| AC3 | 요청 202 → 코드 단계, "존재하지 않는 계정" 상태 없음 | `202 응답이면 코드 단계로 전이` + 존재 오라클 재현 0 | ✓ |
| AC4 | 유효시간 카운트다운(expiresInSeconds 기준), 재전송 60초 쿨다운 | `남은 유효시간 카운트다운`(3건) + `재전송 버튼 쿨다운`(2건) + `재작업 지시 1`(벽시계 정확성) | ✓ |
| AC5 | 응답 코드 매핑: 204/410/409×2/400×2 각각 화면 전이·오류 | `확정 응답 매핑`(7건) + `재작업 지시 2`(500·네트워크 오류 분리) | ✓ |
| AC6 | 로그인 화면에 '비밀번호를 잊으셨나요' 링크 | `LoginPage.tsx` 수정(링크 1줄) · `LoginForm.tsx` 무변경 | ✓(회귀) |
| AC7 | 부품·상태별 스토리(`*.stories.tsx`) | 신규 부품 4개 각각 `.stories.tsx` 동반 | ✓(축E) |
| AC8 | jest+testing-library, `npm test` 걸리게 | 32건 전량 통과 | ✓ |
| AC9 | StrictMode 이중 effect에서 네트워크 1회만 | `같은 target 두 번 호출 → fetch 1회 + Promise identity` | ✓ |
| AC10 | 기존 부품 불가침(OrderFilters 등) | 변경 6개 파일 · 무변경 확인 | ✓ |

### 테스트 실행 결과

**명령:** `npm test` (typecheck.cjs + jest --config jest.config.cjs --passWithNoTests)

```
Test Suites: 4 passed, 4 total
Tests:       32 passed, 32 total
Snapshots:   0 total
```

**TC 구성:**
- 기존 3개 unit test(10건): redirectTarget·refreshOnce·기타
- **신규 2개(AC1~AC5/AC8/AC9 검증):**
  - `requestCodeOnce.unit.test.ts` (3건, node): AC9 단일 비행 가드
  - `PasswordResetPage.test.tsx` (29건, jsdom): AC1~AC5 + round2 지시 4건

**회귀 검증:** 기존 로그인/가입 흐름 무변경(`LoginForm.tsx`) + 기존 10개 tc 통과 ✓

### 빠진 AC 없음

AC1~AC10 모두 실행 테스트로 1:1 이상 매핑됨.

---

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-13 — CONCERNS
- **Layer1 스펙**: AC1~AC10 전부 구현·실증됨. INF-MBR-006(202 + `{channel,target,expiresInSeconds}`)·INF-MBR-007(204 / 400 MBR-4100·MBR-4001 / 410 MBR-4101 / 409 MBR-4102·MBR-4103) 계약을 소스(`MemberPasswordResetController`·`MemberPasswordResetConfirmationController`·INF 본문)와 대조해 상태코드·오류코드·필드명 일치 확인. `post`가 204에서 깨지는 문제를 `postVoid`로 정확히 분리했고, AC5 매핑표 7행이 테스트 7건으로 1:1 단언됨. 구현 계획(사람 확인분) 대비 이탈은 story에 기록된 3건(swc automatic runtime · jest.setup TextEncoder · jest-dom 서브패스 import)뿐이고 전부 환경 제약에 대한 정당한 조정이다. 적용 규칙(must) 위반 없음: `web-fetch-only-in-api`(부품·페이지 `fetch`/`console` 0건 — 재실측) · `story-per-component`(신규 부품 4종 전부 `.stories.tsx`, 페이지 제외) · `file-size-cap`(최대 196줄). **다만 AC4의 "60초 쿨다운/남은 유효시간"이 벽시계가 아니라 2단계 체류시간 기준으로만 흘러 실제와 어긋나는 구간이 있다(아래 1).**
- **Layer2 보안**: 존재 오라클 재현 없음(요청 202 → 무조건 코드 단계, "존재하지 않는 계정" 상태 미구현·테스트로 단언). target/code/newPassword 로깅 0건, 코드 원문 비저장, 무인증 경로만 사용(API 키 요구 안 함). 사례집 SR-231 r5(존재 판정 선행)·SR-231 r1(ID만 보고 역할 반전) 재발 조건 없음 — 형제 API를 INF 본문+컨트롤러 소스로 대조했음을 실측 확인. 차단 이슈 없음.
- **Layer3 회귀**: 변경 표면이 최소(`api.ts` 오류파싱 공유 추출 — 기존 `post` 동작 동등 / `types.ts` 추가만 / `App.tsx` 라우트 1행 / `LoginPage.tsx` 링크 1행, `LoginForm.tsx`·`OrderFilters` 등 부품 무변경 = AC10 충족). `jest.config.cjs` 전역 변경(setupFiles·swc automatic runtime·testMatch)이 기존 3개 `*.unit.test.ts`를 깨지 않음을 **QA가 직접 재실행해 확인**(`npm test` → 4 suites / 27 tests pass, 기준선 10 → 27). `npm ls @testing-library/dom` 단일 버전(10.4.1) 중복 해소 확인 — 스토리북 축E 충돌 위험 낮음. 사례집 재발 0건.
- 권고(CONCERNS, 3건 — medium 2 / low 2):
  1. **(medium) 카운트다운이 2단계 체류시간만 센다 — 벽시계와 어긋난다.** `PasswordResetPage.tsx:62-69`의 `setInterval`이 `step !== 'code'`면 즉시 return이라 3단계(새 비밀번호) 체류 동안 `remainingSeconds`·`resendRemainingSeconds`가 멈춘다. 409 MBR-4102 후 2단계 복귀는 **설계된 정규 경로**인데(AC5), 3단계에서 60초를 쓰고 돌아오면 잔여 유효시간이 60초 과대 표시되고 서버 쿨다운이 이미 끝났는데도 재전송 버튼이 최대 60초 더 잠긴다. 권고: `expiresAt`/`resendAvailableAt`를 epoch ms로 저장하고 `Date.now()` 기준으로 매 tick 계산(인터벌은 단계와 무관하게 구동).
  2. **(medium) confirm의 `default` 분기가 500·네트워크 오류까지 1단계로 되돌린다.** `PasswordResetPage.tsx:150-155`가 `MBR-4100`과 `default`를 같은 처리로 묶어, 일시적 500(MBR-5000)·네트워크 실패에도 사용자를 1단계로 보내고 "일시적인 오류입니다"를 target 필드 오류로 띄운다. AC5 표는 `MBR-4100`에만 이 전이를 확정했다(재시도 가능한 오류의 전이는 미정의). 권고: 미정의 코드는 3단계 유지 + 재시도 가능한 인라인 오류로 두고, `MBR-4100`만 1단계로 보낸다.
  3. **(low) 재전송 후에도 이전 코드 오류·입력이 남는다.** `handleResend`가 `codeError`·`code`를 비우지 않아, 409 MBR-4102 직후 재전송하면 새 코드를 보냈는데도 "코드가 올바르지 않습니다"가 계속 보이고 옛 코드 입력이 남는다. 권고: 재전송 성공 시 `setCode('')`·`setCodeError(null)`.
  4. **(low) 204 후 이 기기의 로컬 세션은 남는다.** 완료 화면은 "모든 기기에서 로그아웃되었습니다"라고 알리지만 localStorage 세션(apiKey/refreshToken)은 그대로다 — 로그인 상태에서 이 화면을 쓴 경우 다음 부팅의 401까지 UI가 로그인 상태로 보인다(서버가 이미 폐기했으므로 자가치유되나 표시와 어긋남). 권고: confirm 204 직후 `clearSession()` 호출(후속 TODO 가능).

### QA Gate — 2026-09-13 — PASS (round 2)

**재작업 지시 4건 대조 — 4/4 해소(전부 코드+회귀테스트로 실증 확인)**

| 지시 | 요구 | 실제 구현 | 실증 |
|---|---|---|---|
| 1 (medium/spec) | `expiresAt`/`resendAvailableAt`를 epoch ms로, `Date.now()` 기준 tick 계산, 인터벌은 단계 무관 구동 | `PasswordResetPage.tsx:53-55`(`expiresAt`/`resendAvailableAt`/`now` 상태), `:69-70`(매 렌더 `Math.max(0, Math.ceil((target-now)/1000))` 파생), `:76-81` useEffect 의존성 `[requestSeq]` — **`step` 조건 제거됨** | `PasswordResetPage.test.tsx:224-245` — 3단계 70초 체류 후 409 MBR-4102로 2단계 복귀 시 `530초` 표시 + 재전송 버튼 활성. round1 코드였다면 `600초`가 나왔을 진짜 회귀테스트(fake timer가 `Date.now()`도 진행시키므로 단언이 유효) |
| 2 (medium/spec) | `default`를 `MBR-4100`과 분리, 미정의 코드는 3단계 유지 + 재시도 인라인 | `:175-179` `MBR-4100`만 `setStep('request')`, `:180-185` `default`는 `setPasswordError(err)`만(단계 전이 없음) | `:247-260`(500 MBR-5000 → 3단계 유지·입력값 보존·1·2단계 요소 부재 단언) / `:262-271`(fetch reject 네트워크 오류 동일) |
| 3 (low/spec) | 재전송 성공 시 `setCode('')`·`setCodeError(null)` | `:130-131` `handleResend` 성공 분기 | `:273-295` — 409 후 코드값 `123456` 확인 → 60초 경과 → 재전송 → 오류 문구 소멸·입력 `''` |
| 4 (low/regression) | confirm 204 직후 `clearSession()` | `:157` (`../session`의 기존 헬퍼 재사용, `LoginForm.tsx` 불가침) | `:297-313` — `saveSession`으로 채운 뒤 204 후 `loadSession()` === null |

- **Layer1 스펙**: 4건 전부 지시대로(사람 확정 "구현 방식" 그대로) 반영. 지시 1이 **AC4 위반을 실제로 닫았다** — 카운트다운이 벽시계 기준이라 3단계 체류·409 복귀 경로에서 잔여 유효시간·재전송 쿨다운이 어긋나지 않는다. 지시 2가 AC5 표의 경계를 정확히 복원했다(표는 `MBR-4100`에만 1단계 전이를 확정 — 미정의 코드의 전이는 표에 없으므로 만들지 않는 것이 맞다). AC1~AC10 전부 유지, 라운드 1에서 확인한 INF-MBR-006/007 계약 정합은 계약 표면을 건드리지 않았으므로 그대로 유효. **적용 규칙 must 위반 0** — `rules_check.py` QA 재실행 결과 `must 0 · should 2`(should 2건 중 1건은 `shop-api/ApiKeyAuthFilter.java` 선행 건, 다른 1건은 아래 3번).
- **Layer2 보안**: 새 공격면 없음. 존재 오라클 재현 없음(요청 202 경로·"존재하지 않는 계정" 상태 부재 그대로). 지시 2의 `default` 분기가 새로 사용자에게 보이는 문자열은 서버 `{code,message}` 또는 클라이언트 `Error.message`뿐이고 target/code/newPassword를 싣지 않는다(로깅 0건, `console.*` grep 0건 재실측). 지시 4의 `clearSession()`은 **로컬 저장소를 지우는 방향**(권한을 넓히지 않고 좁힌다)이라 안전 방향 오류다. 차단 이슈 없음.
- **Layer3 회귀**: 이번 라운드 변경 표면이 **정확히 3개 파일**(mtime 실측: `PasswordResetPage.tsx` 11:31 · `PasswordResetPasswordStep.tsx` 11:31 · `PasswordResetPage.test.tsx` 11:33 — 나머지는 전부 라운드 1(≤11:20) 또는 선행 FUNC 시각). `api.ts`·`types.ts`·`App.tsx`·`LoginPage.tsx`·`jest.config.cjs`·스토리 4종 무변경 = **라운드 1에서 이미 통과한 계약·설정 표면을 재검증할 필요 없음**. AC10 유지(`LoginForm.tsx`·`OrderFilters` 목록에 없음). `PasswordResetPasswordError.code`를 리터럴 유니언 → `string`으로 넓힌 것은 이 FUNC 전용 신규 타입이라 외부 소비자 없음(`PasswordResetCodeError`는 유니언 그대로 유지). **QA 직접 재실행**: `npm test`(`typecheck.cjs` + jest) → **Test Suites 4 passed / Tests 32 passed**, 타입 오류 0(dev 보고와 일치, 기준선 10 → 32). `clearSession` 도입이 전역 세션 상태를 깨지 않음을 확인 — `App.tsx`는 세션을 React 상태로 들지 않고 `loadSession()`을 필요 시점에 읽으므로(`:34-42`) 메모리·저장소 불일치가 생길 여지가 없다. 사례집(`harness/antipatterns.all.md`) 재발 0건.
- **후속 TODO(low 3건 — 게이트 비차단, 이번 라운드에 고치지 않는다)**:
  1. **(low) `clearSession()`이 계정을 가리지 않는다.** 로그인 상태(A)에서 다른 계정(B)의 비밀번호를 재설정하면 A의 로컬 세션까지 지워진다. 완료 화면 문구("모든 기기에서 로그아웃")도 A 기준으로는 사실과 다르다. 다만 ① 이 화면은 로그인 전 경로(로그인 화면 링크)가 정상 진입점이고 ② 세션에 email이 없어 `normalizedTarget`과 대조할 수단이 현재 계약엔 없으며 ③ 결과가 "덜 로그아웃"이 아니라 "더 로그아웃"이라 안전 방향이다. 재로그인으로 즉시 복구된다.
  2. **(low) 카운트다운 인터벌이 `done` 단계에서도 계속 돈다.** 지시 1로 `step` 조건을 뺀 결과, `requestSeq > 0`인 한 완료 화면에서도 매초 `setNow`가 돌아 무한 리렌더가 발생한다(언마운트 시 cleanup되므로 누수는 아니고 순수 렌더 비용). 후속: 종료 조건에 `step !== 'done'` 대신 **`remainingSeconds === 0 && resendRemainingSeconds === 0`이면 인터벌 정지**(단계가 아니라 남은 시간으로 끈다 — 지시 1의 취지를 깨지 않는 방식).
  3. **(low) `PasswordResetPage.test.tsx` 314줄** — `file-size-cap`(tsx 300줄, **should**) 초과. 케이스 5건 추가에 따른 증가이고 must가 아니라 비차단. 후속: 단계 전이 / 오류 매핑 / round2 회귀 3파일로 분할 검토.

## 구현 계획

- **파일**(전부 `modules/shop-web` — 다른 모듈 불가침, `linked_func: FUNC-member-007` 주석 필수):
  - `src/api.ts` (수정): `requestPasswordResetCode(target)` → `POST /api/members/password-resets/codes`(INF-MBR-006), `confirmPasswordReset({target,code,newPassword})` → `POST /api/members/password-resets/confirmations`(INF-MBR-007). **기존 `post<T>`는 성공 시 무조건 `r.json()`을 호출**하는데 확정 API는 204(No Content)라 그대로 쓰면 파싱 예외가 난다 — `post`의 오류-바디 파싱 블록을 `parseErrorBody(r)`로 뽑아 공유하고, 성공 시 바디를 읽지 않는 `postVoid(url, body): Promise<void>`를 새로 추가해 confirm 쪽은 이걸 쓴다.
  - `src/types.ts` (수정): `VerificationCodeResult { channel: 'EMAIL'|'SMS'; target: string; expiresInSeconds: number }` 추가(소스 실측: `MemberPasswordResetService.java:218` record와 필드명 일치 확인함). 오류 봉투는 기존 `ApiErrorBody` 재사용.
  - `src/features/member/requestCodeOnce.ts` (신규): `refreshOnce.ts`와 **동일한 모듈 스코프 단일 in-flight 가드 패턴**을 `target` 키로 구현(`requestCodeOnce(target, fn)`). AC9 근거 — 아래 "프레임워크 실행 모델 함정" 참조. 기존 `refreshOnce.ts`(테스트 있는 공용 인프라)는 건드리지 않고 이 FUNC 전용으로 작게 복제한다(타입이 다르고, 공용 유틸로 일반화하면 `refreshOnce.unit.test.ts` 회귀 위험만 늘어난다).
  - `src/features/member/PasswordResetRequestStep.tsx` + `.stories.tsx`(신규) — 1단계: target 입력(이메일/휴대폰), 제출 버튼, 형식 오류(MBR-4100) 인라인. 순수 프레젠테이션(fetch 없음, `LoginForm.tsx`와 동일한 제어 컴포넌트 패턴). 스토리: 기본·입력됨·제출중·형식오류.
  - `src/features/member/PasswordResetCodeStep.tsx` + `.stories.tsx`(신규) — 2단계: 코드 입력(6자리, 로컬 형식 검증만·서버 호출 없음), 남은 유효시간 표시(부모가 계산한 `remainingSeconds`를 그대로 받아 렌더 — 타이머 자체는 부모/훅에 둔다), 재전송 버튼(쿨다운 `resendRemainingSeconds` prop, 0이면 활성), `error` prop으로 만료(MBR-4101)/시도초과(MBR-4103)/코드불일치(MBR-4102) 세 변형을 렌더(만료·시도초과는 "다시 요청" 버튼, 코드불일치는 인라인 메시지+입력 유지). 스토리: 기본(카운트다운 진행중)·재전송쿨다운·코드오류·만료·시도초과 — AC7의 "코드/만료/시도초과" 세 상태를 여기서 커버.
  - `src/features/member/PasswordResetPasswordStep.tsx` + `.stories.tsx`(신규) — 3단계: 새 비밀번호 입력+제출. `error` prop으로 비밀번호 규칙 오류(MBR-4001)·형식 오류(MBR-4100, 방어적) 인라인. 스토리: 기본·제출중·비밀번호규칙오류.
  - `src/features/member/PasswordResetDoneStep.tsx` + `.stories.tsx`(신규) — 완료 화면: "비밀번호가 변경되었습니다 — 모든 기기에서 로그아웃되었습니다" + 로그인 이동 링크. 스토리: 기본. (AC7의 "완료" 상태)
  - `src/pages/PasswordResetPage.tsx`(신규, 컨테이너 — `pages/`라 스토리 대상 아님) — 단계 상태기계(`request|code|password|done`), 현재 단계 표시, `PasswordResetXStep` 4종을 스위치 렌더. target 정규화(휴대폰 하이픈·공백 제거 후 전송, 이메일은 입력 그대로 — AC2), 쿨다운/만료 카운트다운(`useEffect`+`setInterval`, `LoginForm.tsx`의 429 카운트다운과 동일 기법 — cleanup 있는 로컬 타이머라 StrictMode 이중 마운트에도 안전), 응답 코드→화면 전이 매핑(아래 "순서·보안" 절), API 호출은 전부 클릭 핸들러에서 `requestCodeOnce`/`confirmPasswordReset`로 수행.
  - `src/App.tsx`(수정, 2줄) — `import PasswordResetPage`, `<Route path="/password-reset" element={<PasswordResetPage />} />` 추가. 기존 라우트 3개 불변.
  - `src/pages/LoginPage.tsx`(수정) — `<LoginForm.../>` 아래에 `<Link to="/password-reset">비밀번호를 잊으셨나요?</Link>` 한 줄 추가(AC6). **`LoginForm.tsx`(부품)는 건드리지 않는다** — 기존 로그인 동작·`LoginForm.stories.tsx`·화면 상태 기준선에 영향을 주지 않으려는 의도적 선택(페이지 컨테이너는 스토리 축E 대상이 아니라 회귀 표면이 가장 작다).
  - `jest.config.cjs`(수정) — `testMatch`에 `'**/src/**/*.test.tsx'` 패턴 추가(기존 `**/*.unit.test.ts`는 유지, 두 패턴 공존). 전역 `testEnvironment`는 `'node'`로 유지(기존 3개 `.unit.test.ts` 회귀 방지) — 새 컴포넌트 테스트 파일은 파일 상단 `/** @jest-environment jsdom */` 독블록으로 개별 지정한다(Jest가 파일 단위 오버라이드 지원).
  - `package.json`(수정) — devDependencies 추가: `@testing-library/react`, `@testing-library/user-event`, `@testing-library/jest-dom`, `jest-environment-jsdom`(버전은 설치된 `jest@^30`/`react@^19`와 정합). **현재 `@testing-library/{dom,jest-dom,user-event}`는 `@storybook/test-runner`가 끌어온 간접 의존이라 node_modules엔 있지만 `@testing-library/react`와 `jest-environment-jsdom`은 전혀 없다(실측: `npm ls jest-environment-jsdom @testing-library/react` → empty)** — `npm install --save-dev`로 실제 설치가 필요한 신규 의존성이다(설정만 바꾸면 `Test environment jest-environment-jsdom cannot be found` 에러).
  - `src/features/member/requestCodeOnce.unit.test.ts`(신규, node env) — AC9 실증.
  - `src/pages/PasswordResetPage.test.tsx`(신규, jsdom+testing-library) — AC1~AC5, AC2 실증.

- **데이터**: 없음. 이 FUNC는 프론트엔드 전용(Dev Notes: INF/SCH 연결 없음) — 백엔드 INF-MBR-006/007(FUNC-member-008/009)은 이미 Done이고 계약을 바꾸지 않는다. DDL·트랜잭션·락 변경 없음.

- **순서·보안**:
  - 인증 불필요(로그인 전 사용자 화면, INF-MBR-006/007과 동일 이유) — 프론트에서 API 키·세션을 요구하지 않는다.
  - **존재 오라클 재현 금지**: 요청 단계는 서버가 항상 202만 준다(회원 존재 여부 노출 없음, INF-MBR-006 "형식 오류만 400, 그 외 항상 202") — 화면은 이 202를 "코드가 발송됐다"로만 표시하고 "존재하지 않는 계정" 상태를 만들지 않는다(AC3, STORY 계약 메모와 동일). 400 MBR-4100(형식 오류)만 요청 단계에서 인라인 표시.
  - **응답 코드 → 화면 전이 매핑(AC5, 확정·재해석 금지)**:
    | 트리거 | 서버 응답 | 화면 전이 |
    |---|---|---|
    | 1단계 제출 | 202 | 2단계(코드)로, `channel`/`target`/`expiresInSeconds` 저장, 쿨다운 타이머 시작 |
    | 1단계 제출 | 400 MBR-4100 | 1단계 유지, 인라인 오류 |
    | 3단계 제출(confirm) | 204 | 4단계(완료) |
    | 3단계 제출 | 410 MBR-4101 | 2단계(코드) 화면에 "만료됨" + "다시 요청"(클릭 시 전체 상태 초기화 후 1단계로) |
    | 3단계 제출 | 409 MBR-4103 | 2단계(코드) 화면에 "시도초과" + "다시 요청"(동일하게 1단계로 초기화) |
    | 3단계 제출 | 409 MBR-4102 | 2단계(코드)로 복귀, 코드 필드에 인라인 오류, **newPassword 값은 상태에 유지**(입력 유지 — 코드만 고치면 3단계 재진입 시 비밀번호를 다시 칠 필요 없음) |
    | 3단계 제출 | 400 MBR-4001 | 3단계 유지, 비밀번호 필드 인라인 오류, code/newPassword 값 유지 |
    | 3단계 제출 | 400 MBR-4100 | 방어적 처리 — 1단계로, target 인라인 오류(정상 흐름에서는 도달 안 함: target은 1단계 202 응답으로 이미 정규화됨) |
    - "코드 확인"은 별도 API 호출이 아니다 — INF-MBR-007이 코드 검증과 비밀번호 반영을 원자적으로 한 번에 하므로(계약 메모: 확정 API가 target+code+newPassword를 함께 받음), 2단계는 로컬 형식 검증(6자리 숫자)만 하고 실제 코드 검증은 3단계 제출이 트리거하는 confirm 호출 안에서 일어난다. 이 설계를 벗어나 2단계에서 별도로 confirm을 먼저 호출하면 새 비밀번호 없이 confirm을 부르게 되는데 API가 `newPassword` 필수라 성립하지 않는다.
  - **레이트리밋 표시는 UX일 뿐, 권위는 서버**: 60초 재전송 쿨다운은 화면에서 버튼을 막아 매너를 지키지만, 서버(`touchRequest` UPSERT)가 실제 판정자다 — 쿨다운 이내에 어떤 경로로든 재요청이 나가도(예: 새로고침 후 재시도) 서버는 조용히 no-op 처리하고 여전히 202를 준다. 프론트는 이를 구분하려 하지 않는다.
  - **정보 노출 금지**: `console.log` 금지(규칙 `web-fetch-only-in-api`의 `web-no-console` 체크), target·code를 어디에도 로그하지 않는다(서버 쪽 이미 마스킹·비원문 저장 — 프론트는 화면 표시 외 용도로 값을 보관하지 않는다).
  - **부수효과 순서**: 이 FUNC에 로그·발송·감사 부수효과 없음(전부 서버 쪽, 이미 Done) — 프론트는 응답을 받은 뒤에만 상태를 전이한다(낙관적 전이 없음, `busy` 동안 버튼 비활성으로 이중 제출 방지).

- **계약**: 새 프론트 전용 타입 `VerificationCodeResult`(위) 1개. 백엔드 오류 코드·상태코드 신설 없음(INF-MBR-006/007 기존 계약 그대로 소비). `postVoid` 헬퍼 신설(204 응답 파싱 처리).

- **테스트**:
  - `requestCodeOnce.unit.test.ts`(node env, `refreshOnce.unit.test.ts`와 동형): (1) 같은 target으로 연달아 두 번 호출 → `fetch` 1회만, 두 Promise가 동일 객체(`toBe`) — AC9 핵심 단언. (2) 첫 호출이 settle된 뒤 새 호출은 실제로 다시 fetch(영구 차단 방지). (3) 다른 target의 in-flight는 서로 뭉개지 않음.
  - `PasswordResetPage.test.tsx`(jsdom + `@testing-library/react` + `jest.useFakeTimers()`): 초기 렌더는 1단계만 보임 / 휴대폰 `010-1234-5678` 입력 후 제출 시 fetch body가 `{target:"01012345678"}`(하이픈·공백 제거, AC2) / 이메일은 입력 그대로 전송(trim·소문자 변환은 서버 책임, 프론트가 앞서 변형하지 않음) / 202 응답 후 2단계로 전이하고 `expiresInSeconds` 기준 카운트다운이 fake timer로 감소 / 재전송 버튼이 60초 동안 비활성+잔여초 표시 후 활성화, 클릭 시 fetch 재호출 / 6자리 코드 입력 후 "다음" 클릭 시 **fetch 호출 횟수 불변**(로컬 전이, 서버 호출 없음 확인 — 위 매핑 근거 검증) / 3단계 새 비밀번호 제출 → 204 mock → 완료 화면 텍스트("모든 기기에서 로그아웃") / 410·409(4102)·409(4103)·400(4001) 각각 위 표대로 전이·인라인 문구·"다시 요청" 유무를 단언.
  - StrictMode 이중 호출 자체의 증명은 컴포넌트 레벨에서 재현하지 않는다(RTL 버전별 `reactStrictMode` 옵션 지원 여부가 불확실) — `requestCodeOnce.unit.test.ts`가 `refreshOnce.unit.test.ts`와 같은 방식으로 유틸 경계에서 이미 증명하므로 충분하다(AC9는 "동일 target 두 번 호출 → 네트워크 1회"로 이미 검증됨).
  - Storybook: 새 4개 컴포넌트 각각 `.stories.tsx`(규칙 `story-per-component`) — 축E가 렌더 확인. 기존 `LoginForm.stories.tsx`·`OrderFilters` 등은 무변경(AC10).
  - `npm test`(`typecheck.cjs` + `jest --config jest.config.cjs`) 전량 통과 — 기존 3개(unit) + 신규(unit 1 + component 1, 케이스 다수) 전부 포함.

- **테스트 격리**:
  - `requestCodeOnce.ts`의 모듈 스코프 `inFlight`는 `refreshOnce.unit.test.ts`와 동일하게 각 테스트 `beforeEach`에서 `jest.resetModules()` 후 동적 `import()`로 새로 로드해 초기화한다 — 그렇지 않으면 이전 테스트의 in-flight 상태가 다음 테스트로 샌다.
  - `global.fetch` mock은 테스트마다 새로 지정(`jest.fn()`)하고, 호출 시퀀스 넘버로 서로 다른 응답 객체를 반환해(refreshOnce 테스트와 동일 기법) "항상 같은 mock 값이라 dedup 없이도 통과"하는 무효 단언을 방지한다.
  - `jest.useFakeTimers()`는 `afterEach`에서 `jest.useRealTimers()`로 복원 — 다음 테스트 파일/케이스로 타이머가 새지 않게 한다.
  - 백엔드 DB 상태에 의존하지 않는다(전부 mock fetch) — SR-232 r2류의 "이메일이 PK인 카운터가 테스트를 가로질러 누적" 문제는 이 FUNC엔 해당 사항 없음(서버 호출을 아예 하지 않는 순수 프론트 테스트).

- **폴백·우회 경로의 자격 판정**: 없음 — 이 FUNC은 새 인증·조회 경로를 열지 않는다. 서버(INF-MBR-006/007)가 이미 `del_yn='N'` 이중 필터·존재 오라클 방지를 끝낸 응답을 그대로 렌더링만 한다. 프론트가 "계정 존재/미존재"를 스스로 판단하는 코드를 절대 추가하지 않는다(예: target 형식이 맞으면 무조건 다음 단계로 — 서버 판정을 앞지르지 않음).

- **프레임워크 실행 모델 함정**: `main.tsx`가 앱 전체를 `<StrictMode>`로 감싸므로(실측: `src/main.tsx:6-9`), dev 모드에서 마운트 시 effect가 setup→cleanup→setup으로 두 번 실행된다. `App.tsx`의 `useSilentRefresh`(FUNC-member-004, AC9 원형 사고)가 이미 이 함정에 한 번 걸렸다(RUN8 004 r1 — 회전형 refresh를 두 번 호출해 401로 세션이 지워짐). 이 FUNC의 "코드 요청" 호출도 같은 계열의 함정에 노출될 수 있다: 1단계 제출과 재전송은 클릭 핸들러라 그 자체로는 StrictMode 이중 실행 대상이 아니지만, 구현 중 "단계 전환 시 자동으로 코드 요청을 재확인/재발사"하는 식으로 `useEffect`에 옮겨 적으면(코드 리뷰에서 흔한 리팩터링 유혹) 즉시 같은 증상이 재현된다. 그래서 실제 네트워크 호출 지점(`requestPasswordResetCode`)을 `requestCodeOnce`로 감싸 **호출 경로가 클릭이든 향후 effect로 바뀌든 구조적으로 이중 발사가 불가능**하게 만든다(호출부가 아니라 호출 대상 자체를 가드) — `refreshOnce.ts`가 증명한 것과 동일한 방어선을 이 FUNC에도 선제 적용한다. INF-MBR-006 자체는 idempotent(쿨다운 UPSERT no-op)라 이중 호출이 데이터 정합성을 깨지는 않지만, AC9는 "네트워크 호출 자체가 두 번 나가지 않을 것"을 요구하므로 idempotency만으로는 충족되지 않는다.

- **범위 밖**: 백엔드 변경 전부(INF-MBR-006/007, FUNC-member-008/009는 이미 Done — 계약 재해석·수정 금지). `LoginForm.tsx` 자체 수정(링크는 `LoginPage.tsx`에만 추가). `OrderFilters` 등 기존 부품·픽스처(AC10). 실제 이메일/SMS 발송 게이트웨이 연동(서버가 이미 로그 시뮬레이션으로 대체, 프론트가 관여할 부분 없음). 비밀번호 재설정 링크(URL 클릭형) 방식 — 이 SR은 "코드" 방식만 구현(요구사항 문서 "링크·코드는 10분 유효"가 코드 발송을 실제 채택 방식으로 명시, INF-MBR-006/007도 코드 기반).

- **실패 사례집 대조**(`harness/antipatterns.all.md`):
  - "React StrictMode(dev) 이중 effect… 회전형 토큰 호출"(FUNC-member-004 항목) — 조건: **네트워크 호출이 effect 경로를 탄다**. 이 FUNC은 현재 설계상 클릭 핸들러뿐이라 조건이 직접 성립하지 않지만, 향후 리팩터링 시 성립할 수 있어 `requestCodeOnce` 가드를 선제 적용한다(위 "프레임워크 실행 모델 함정" 참조) — 조건이 없다고 방어를 생략하지 않는다.
  - "예약 FUNC을 ID만 보고 구현해 역할이 반대로 붙었다"(SR-231 r1) — 이 FUNC은 이름(비밀번호 재설정 **화면**)과 형제 API(008 코드 요청/009 코드 확정)를 INF 문서+실제 컨트롤러 소스로 직접 대조했다(레코드 필드명까지 실측 확인: `target`/`code`/`newPassword`, `channel`/`target`/`expiresInSeconds`) — 같은 실수 조건(문서만 보고 짐작) 없음.
  - "재작업 지시가 '존재 판정 → 인증' 순서였다"(SR-231 r5) — 이 FUNC은 서버 존재 오라클 방지를 그대로 통과시킬 뿐 재구현하지 않으므로 조건 자체가 발생하지 않는다(위 "순서·보안" 절에 명시).
  - "테스트 픽스처에 linked_func 주석이 없어 축A가 FAIL"(SR-230) — 신규 테스트 파일(`requestCodeOnce.unit.test.ts`, `PasswordResetPage.test.tsx`) 상단에도 `linked_func: FUNC-member-007` 주석을 단다.
  - "통합 테스트가 같은 리터럴 값을 재사용해 카운터가 테스트를 가로질러 누적"(SR-232 r2) — 이 FUNC 테스트는 실제 DB/서버를 부르지 않는 mock-fetch 테스트라 조건(공유 DB 카운터)이 성립하지 않는다. 대신 동형 위험(모듈 스코프 `inFlight` 누수)은 `jest.resetModules()`로 방지(위 "테스트 격리" 절).

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/spec] PasswordResetPage.tsx:62-69 카운트다운 인터벌이 step==='code'일 때만 돌아 3단계 체류 동안 남은 유효시간·재전송 쿨다운이 멈춘다 — 409 MBR-4102 후 2단계 복귀(설계된 정규 경로)에서 잔여시간 과대 표시, 서버 쿨다운이 끝났는데도 재전송 버튼이 최대 60초 더 잠김(AC4 위반) → expiresAt/resendAvailableAt를 epoch ms로 저장하고 Date.now() 기준으로 tick마다 계산, 인터벌은 단계와 무관하게 구동
2. [medium/spec] PasswordResetPage.tsx:150-155 confirm 오류 switch의 default가 MBR-4100과 동일 처리라 일시적 500(MBR-5000)·네트워크 오류에도 사용자를 1단계로 되돌리고 target 인라인 오류로 표시한다 — AC5 표는 MBR-4100에만 이 전이를 확정했다 → 미정의 오류 코드는 3단계 유지 + 재시도 가능한 인라인 오류로 처리하고, MBR-4100만 1단계로 전이
3. [low/spec] handleResend가 code·codeError를 비우지 않아 409 MBR-4102 직후 재전송하면 새 코드를 보냈는데도 이전 오류 문구와 옛 코드 입력이 남는다 → 재전송 성공 시 setCode('') · setCodeError(null)
4. [low/regression] confirm 204 후 완료 화면은 '모든 기기에서 로그아웃'을 알리지만 이 기기의 localStorage 세션(apiKey/refreshToken)은 남는다 — 로그인 상태에서 사용한 경우 다음 부팅 401까지 표시와 실제가 어긋난다 → confirm 204 직후 clearSession() 호출(후속 TODO 가능)

사람 코멘트: [결정 요약] 4건 전부 이번에 고친다 — 같은 파일의 작은 수정이고 사용자에게 보이는 정확성(시간·전이)이다. [구현 방식] (1) expiresAt·resendAvailableAt을 epoch ms로 저장하고 매 tick Date.now() 기준으로 계산 — 단계와 무관하게 흐른다. 테스트: 3단계에서 fakeTimers로 70초 진행 뒤 2단계로 돌아오면 잔여시간이 70초 줄어 있고 재전송이 열려 있음을 실증. (2) confirm 오류 switch에서 default는 3단계 유지 + 재시도 인라인(네트워크·5xx 포함). MBR-4100만 1단계로. 테스트: 500 응답 케이스 추가. (3) handleResend가 code·codeError를 비운다. (4) 204 완료 시 이 기기의 로컬 세션(localStorage 토큰)을 즉시 지운다(기존 로그아웃 헬퍼 재사용, LoginForm 불가침) — 완료 화면 문구와 일치. [테스트·완료 조건] npm test 전량 통과(27+4 이상), 스토리 렌더 유지.
