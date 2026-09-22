---
uis-id: UIS-MBR-003
name: 비밀번호 재설정
domain: member
domain-code: MBR
layer: ui
screen-id: password-reset
route: /password-reset
screens_role: 주화면
api_hints:
  - "POST /api/members/password-resets/codes"
  - "POST /api/members/password-resets/confirmations"
access_control: []
anchors:
  - "modules/shop-web/src/pages/PasswordResetPage.tsx:37-226"
  - "modules/shop-web/src/features/member/PasswordResetRequestStep.tsx:28-49"
  - "modules/shop-web/src/features/member/PasswordResetCodeStep.tsx:42-92"
  - "modules/shop-web/src/features/member/PasswordResetPasswordStep.tsx:29-50"
  - "modules/shop-web/src/features/member/PasswordResetDoneStep.tsx:12-19"
  - "modules/shop-web/src/features/member/requestCodeOnce.ts:25-35"
  - "modules/shop-web/src/pages/LoginPage.tsx:45-47"
  - "modules/shop-web/src/App.tsx:9,52"
  - "modules/shop-web/src/api.ts:102-117"
revision_history:
  - version: 1.0
    date: 2026-09-13
    author: ddd-ui-agent (source-authority)
    change: 최초 생성(소스폴백 모드 — FUNC-member-007 신규 구현 반영, DOM 스냅샷·라이브 캡처 없음)
---

> [반영: FUNC-member-007] 2026-09-13

# UIS-MBR-003: 비밀번호 재설정

> **근거 소스(권위):** `modules/shop-web/src/pages/PasswordResetPage.tsx`(컨테이너 — 상태기계·응답코드
> 매핑) + `src/features/member/PasswordReset{Request,Code,Password,Done}Step.tsx`(4개 순수 프레젠테이션
> 부품) + `src/features/member/requestCodeOnce.ts`(단일 비행 가드) + `src/pages/LoginPage.tsx`(진입
> 링크) + `src/App.tsx`(라우트) + `src/api.ts`(API 호출). 소스폴백 모드 — DOM 스냅샷·스크린샷 없음
> (AIDD 작업 중 `npm run dev`를 켜지 않는다는 제약, HMR OOM 회피). 4개 부품 각각의 `.stories.tsx`
> (기본·입력됨·제출중·형식오류·재전송쿨다운끝·코드오류·만료·시도초과·비밀번호규칙오류 등 총 12개
> 상태)를 §5 표시조건의 1급 증거로 사용했다. 응답코드→화면전이 매핑은
> `docs/00_FUNC/stories/STORY-FUNC-member-007.md`의 "AC5"·"구현 계획 › 순서·보안"에서 사람이 확정한
> 표를 그대로 옮겼다(재해석 금지).

> **라우트 참고:** `HashRouter` 사용(`App.tsx:47-54`, UIS-MBR-002와 동일) — 브라우저 주소창 상 실제
> 경로는 `/#/password-reset`이다. 이 화면 안에서는 라우트 전이가 없다 — 4단계 전부 같은 라우트에서
> 컨테이너의 로컬 상태(`step`)로 스위치 렌더된다(§3 참조, 탭이 아니라 상태기계).

## 1. 화면 목적

비밀번호를 잊은 회원이 이메일 또는 휴대폰번호로 받은 6자리 코드를 확인하고 새 비밀번호를 설정하는
3단계(+완료) 화면이다. 코드 발급·검증·비밀번호 반영의 실제 판정은 전부 서버(INF-MBR-006/007)가
하며, 이 화면은 그 결과(응답 코드·문구·`expiresInSeconds`)를 상태기계로 옮기고 표시만 한다 — 계정
존재 여부를 프론트가 판단해 노출하지 않는다(요청 API는 형식 오류가 아닌 한 항상 202, 존재 오라클
방지). 재설정이 완료되면 그 회원의 모든 기기가 로그아웃된다(서버 사이드이펙트).

## 2. 주요 작업 시나리오

**시나리오: 비밀번호 재설정**
1. 로그인 화면의 [비밀번호를 잊으셨나요?] 링크로 진입한다(`LoginPage.tsx:45-47`) — 또는 주소창에
   직접 `/#/password-reset`으로 진입.
2. **1단계(계정 확인)**: 이메일 또는 휴대폰번호를 입력하고 [코드 요청]을 누른다 →
   `PasswordResetPage.handleRequestSubmit` → `requestCodeOnce`(단일 비행 가드) →
   POST /api/members/password-resets/codes(INF-MBR-006). 휴대폰은 하이픈·공백을 제거해 숫자만,
   이메일은 입력 그대로 전송한다(`normalizeTargetForSend`).
   - **202 성공**: 서버가 돌려준 `target`(정규화됨)·`channel`·`expiresInSeconds`를 저장하고 2단계로
     전이. 유효시간 카운트다운과 재전송 60초 쿨다운을 벽시계(epoch ms) 기준으로 시작.
   - **400 MBR-4100**(형식 오류): 1단계 유지, 인라인 오류 표시.
3. **2단계(코드 확인)**: 발송 채널·대상(`target`)과 남은 유효시간을 보여준다. 6자리 숫자를 입력하면
   (로컬 형식 검증만, 서버 호출 없음) [다음]이 활성화되어 3단계로 넘어간다. [재전송](60초 쿨다운
   경과 후 활성)을 누르면 코드를 다시 요청하고 남은 유효시간·쿨다운을 재시작하며 옛 코드 입력·오류
   문구를 지운다.
4. **3단계(새 비밀번호)**: 새 비밀번호를 입력하고 [비밀번호 변경] 제출 →
   `PasswordResetPage.handlePasswordSubmit` → POST /api/members/password-resets/confirmations
   (INF-MBR-007, `target`+`code`+`newPassword`를 원자 전송 — "코드 확인"은 별도 API가 아니라 이 호출
   안에서 함께 검증된다).
   - **204 성공**: `clearSession()`으로 이 기기의 로컬 세션(apiKey/refreshToken)을 즉시 지우고
     4단계(완료)로 전이.
   - **410 MBR-4101**(코드 만료) · **409 MBR-4103**(시도 초과): 2단계로 되돌리고 "다시 요청" 버튼만
     보여준다(코드 입력 UI는 숨김). [다시 요청]을 누르면 전체 상태를 초기화하고 1단계로 돌아간다.
   - **409 MBR-4102**(코드 불일치): 2단계로 되돌리고 코드 인라인 오류를 표시하되 **입력한 새
     비밀번호 값은 상태에 유지**(코드만 고치면 3단계 재진입 시 다시 칠 필요 없음).
   - **400 MBR-4001**(비밀번호 규칙 오류): 3단계 유지, 비밀번호 필드 인라인 오류.
   - **400 MBR-4100**(방어적 — 정상 흐름에서는 도달하지 않음): 1단계로, 계정 필드 인라인 오류.
   - **그 외(미정의·네트워크·5xx, 예 MBR-5000)**: 3단계를 유지한 채 재시도 가능한 인라인 오류만
     표시(1단계로 되돌리지 않는다 — 입력한 코드·새 비밀번호를 보존).
5. **4단계(완료)**: "비밀번호가 변경되었습니다 — 모든 기기에서 로그아웃되었습니다" 안내 + [로그인으로
   이동] 링크.

**부가 시나리오: 재전송·만료·시도초과 되돌이표**
1. 2단계에서 유효시간이 다 되기 전에 [재전송]을 누르면 같은 target으로 코드를 다시 받아 카운트다운을
   재시작한다(§2-3).
2. 카운트다운이 남아 있어도 3단계에서 confirm이 410/409(4103)을 돌려주면 그 즉시 2단계 "만료/시도초과"
   화면으로 강제 전환된다 — 여기서부터는 [다시 요청]으로 1단계부터 다시 시작하는 것 외엔 진행할 수
   없다.

## 3. 화면 구성 (블록)

> 4단계는 서로 다른 라우트가 아니라 **한 라우트(`/password-reset`) 안의 상태기계**다(컨테이너의
> `step` state로 스위치 렌더, `PasswordResetPage.tsx:199-225`). 탭이 아니라 배타적 화면 상태이므로
> 마커 ②~⑤는 "동시에 보이지 않는" 블록이다.

| 마커 | 블록 | 역할 | 주요 위젯 | 소스 근거 |
|---|------|------|----------|----------|
| ① | 단계 헤더 | 제목 + 진행 표시(1/3·2/3·3/3·완료) | `<h1>`, 단계 라벨 텍스트 | `PasswordResetPage.tsx:192-202` |
| ② | 1단계: 계정 확인 폼 | 이메일/휴대폰 입력 + 코드 요청 | target input, 코드 요청 버튼 | `PasswordResetRequestStep.tsx` |
| ③ | 2단계: 코드 확인 폼 | 6자리 코드 입력 + 유효시간/재전송 | code input, 다음 버튼, 재전송 버튼, 다시요청 버튼(오류 시) | `PasswordResetCodeStep.tsx` |
| ④ | 3단계: 새 비밀번호 폼 | 새 비밀번호 입력 + 제출 | password input, 비밀번호 변경 버튼 | `PasswordResetPasswordStep.tsx` |
| ⑤ | 4단계: 완료 | 안내 문구 + 로그인 이동 | 안내 텍스트, 로그인 이동 링크 | `PasswordResetDoneStep.tsx` |

## 4. 위젯·액션

**① 단계 헤더**

| 번호 | 위젯 | 타입 | 레이블 | 동작 | 연결 API(raw) | 결과 |
|---|------|------|--------|------|--------------|------|
| (1) | 단계 라벨 | text | "1/3 · 계정 확인" 등 4종 | `step` state에 따라 표시 전용 | — | — |

**② 1단계: 계정 확인 폼**

| 번호 | 위젯 | 타입 | 레이블 | 동작 | 연결 API(raw) | 결과 |
|---|------|------|--------|------|--------------|------|
| (2) | target input | input(text) | 이메일 또는 휴대폰번호 | 입력값 갱신(`onChange`) | — | — |
| (3) | 코드 요청 버튼 | button(type=submit) | 코드 요청 / 요청 중… | 폼 제출 → `handleRequestSubmit`(내부에서 `normalizeTargetForSend`로 정규화 후 `requestCodeOnce` 단일비행 가드 경유) | POST /api/members/password-resets/codes | 202: 2단계로 전이(target·channel·expiresInSeconds 저장, 카운트다운 시작) / 400 MBR-4100: 폼 유지+인라인 오류 |

**③ 2단계: 코드 확인 폼**

| 번호 | 위젯 | 타입 | 레이블 | 동작 | 연결 API(raw) | 결과 |
|---|------|------|--------|------|--------------|------|
| (4) | 안내 문구 | text | "(이메일\|휴대폰)(target)로 발송된 6자리 코드를 입력하세요" | 표시 전용 | — | — |
| (5) | 남은 유효시간 문구 | text | "남은 유효시간 N초" | `remainingSeconds`(벽시계 파생) 표시, 만료/시도초과 오류 시 숨김 | — | — |
| (6) | code input | input(numeric, maxLength=6) | 인증코드 | 숫자만 필터링해 입력값 갱신, 6자리 숫자 형식이면 로컬 유효 | — | — |
| (7) | 다음 버튼 | button | 다음 | 로컬 형식 검증(6자리 숫자) 통과 시에만 활성 → 클릭 시 `handleCodeNext` | — (서버 호출 없음) | 3단계로 전이(코드 실제 검증은 3단계 confirm 호출에 포함) |
| (8) | 재전송 버튼 | button | 재전송 / 재전송 (N초) | `resendRemainingSeconds`가 0일 때만 활성 → 클릭 시 `handleResend` | POST /api/members/password-resets/codes | 성공: 카운트다운·쿨다운 재시작 + 옛 코드/오류 초기화 / 실패: 조용히 무시(기존 상태 유지) |
| (9) | 다시 요청 버튼(오류 시) | button | 다시 요청 | 만료(MBR-4101)·시도초과(MBR-4103) 오류일 때만 표시 → `handleRestart` | — | 전체 상태 초기화 후 1단계로 |
| (10) | 코드 오류 인라인(role=alert) | div | (서버 message) | `error.code === 'MBR-4102'`일 때만 표시, 입력은 유지 | — | — |

**④ 3단계: 새 비밀번호 폼**

| 번호 | 위젯 | 타입 | 레이블 | 동작 | 연결 API(raw) | 결과 |
|---|------|------|--------|------|--------------|------|
| (11) | password input | input(password) | 새 비밀번호 | 입력값 갱신(`onChange`) | — | — |
| (12) | 비밀번호 변경 버튼 | button(type=submit) | 비밀번호 변경 / 변경 중… | 폼 제출 → `handlePasswordSubmit`(target+code+newPassword 원자 전송) | POST /api/members/password-resets/confirmations | §2 응답코드 매핑 참조(204→완료 / 410·409-4103→2단계 다시요청 / 409-4102→2단계 코드오류(비번유지) / 400-4001→3단계 유지+인라인 / 400-4100→1단계(방어적) / 그외→3단계 유지+인라인 |
| (13) | 비밀번호 오류 인라인(role=alert) | div | (서버 message) | `error`가 non-null일 때 표시(규칙오류·방어적 400·미정의 오류 공용) | — | — |

**⑤ 4단계: 완료**

| 번호 | 위젯 | 타입 | 레이블 | 동작 | 연결 API(raw) | 결과 |
|---|------|------|--------|------|--------------|------|
| (14) | 안내 문구 | text | "비밀번호가 변경되었습니다 — 모든 기기에서 로그아웃되었습니다" | 표시 전용 | — | — |
| (15) | 로그인으로 이동 | a href="#/login" | 로그인으로 이동 | 순수 앵커 이동(react-router `Link`가 아니라 `<a>` — Storybook에서 Router 컨텍스트 없이도 렌더되게 하려는 의도적 선택) | — | `/#/login`으로 이동 |

## 5. 접근 권한·표시 조건

> 4개 부품 각각의 `.stories.tsx`가 이 화면의 상태를 1급으로 남긴다(총 12개 상태). 한 행 = 한 스토리.

| 요소 | 표시 조건 | 근거 | 스토리 |
|------|----------|------|--------|
| 1단계 기본(빈 입력) | 초깃값(`target: ''`) | `PasswordResetRequestStep.tsx`, stories: ./src/features/member/PasswordResetRequestStep.stories.tsx | [기본](story:회원-비밀번호-재설정-1단계-요청--기본) |
| 1단계 입력됨 | target에 값이 채워짐, 오류 없음 | 위와 동일 | [입력됨](story:회원-비밀번호-재설정-1단계-요청--입력됨) |
| 1단계 제출중(버튼 비활성) | `busy === true` | `PasswordResetRequestStep.tsx:29,41` | [제출중](story:회원-비밀번호-재설정-1단계-요청--제출중) |
| 1단계 형식오류 인라인 | `error?.code === 'MBR-4100'` | `PasswordResetRequestStep.tsx:38-40` | [형식오류](story:회원-비밀번호-재설정-1단계-요청--형식오류) |
| 2단계 기본(카운트다운·쿨다운 진행중) | `remainingSeconds > 0`, `resendRemainingSeconds > 0`, `error` 없음 | `PasswordResetCodeStep.tsx:55-88` | [기본](story:회원-비밀번호-재설정-2단계-코드--기본) |
| 2단계 재전송 활성화 | `resendRemainingSeconds === 0` | `PasswordResetCodeStep.tsx:82-87` | [재전송쿨다운끝](story:회원-비밀번호-재설정-2단계-코드--재전송쿨다운끝) |
| 2단계 코드오류(입력 유지) | `error?.code === 'MBR-4102'` | `PasswordResetCodeStep.tsx:73-75` | [코드오류](story:회원-비밀번호-재설정-2단계-코드--코드오류) |
| 2단계 만료(다시 요청만 표시) | `error?.code === 'MBR-4101'` | `PasswordResetCodeStep.tsx:48,58-66` | [만료](story:회원-비밀번호-재설정-2단계-코드--만료) |
| 2단계 시도초과(다시 요청만 표시) | `error?.code === 'MBR-4103'` | `PasswordResetCodeStep.tsx:48,58-66` | [시도초과](story:회원-비밀번호-재설정-2단계-코드--시도초과) |
| 3단계 기본 | 초깃값(`value: ''`) | `PasswordResetPasswordStep.tsx` | [기본](story:회원-비밀번호-재설정-3단계-새-비밀번호--기본) |
| 3단계 제출중(버튼 비활성) | `busy === true` | `PasswordResetPasswordStep.tsx:30,42` | [제출중](story:회원-비밀번호-재설정-3단계-새-비밀번호--제출중) |
| 3단계 비밀번호규칙오류 인라인 | `error?.code === 'MBR-4001'`(그 외 미정의 코드도 같은 인라인 자리에 표시됨) | `PasswordResetPasswordStep.tsx:39-41` | [비밀번호규칙오류](story:회원-비밀번호-재설정-3단계-새-비밀번호--비밀번호규칙오류) |
| 4단계 완료 | confirm 204 수신 직후(`step === 'done'`) | `PasswordResetDoneStep.tsx`, `PasswordResetPage.tsx:157-158` | [기본](story:회원-비밀번호-재설정-4단계-완료--기본) |

## 6. 팝업·연계 화면

팝업/모달 없음. 다만 이 화면과 다른 화면 사이의 SPA 내 이동이 둘 있다:

| 트리거 위젯 | 연계 화면 | 연결 API/화면(raw) | 용도 |
|------------|----------|---------------------|------|
| 로그인 화면의 `Link to="/password-reset"` | 이 화면(1단계) | GET /#/password-reset (INF 없음 — 화면 전환) | 비밀번호를 잊은 회원의 진입점(`LoginPage.tsx:45-47`) |
| 4단계 `<a href="#/login">` | UIS-MBR-002(로그인) | GET /#/login (INF 없음 — 화면 전환) | 재설정 완료 후 재로그인 유도 |

## 7. 데이터 출처·연결

- **연결 API(raw → INF):**
  - `POST /api/members/password-resets/codes` — INF-MBR-006. 1단계 제출 + 2단계 재전송이 공유 호출
    (`requestCodeOnce` 단일 비행 가드로 같은 target의 동시 호출을 중복 발사하지 않는다).
  - `POST /api/members/password-resets/confirmations` — INF-MBR-007. 3단계 제출의 유일한 호출.
    코드 검증과 비밀번호 반영을 원자 처리한다(별도의 "코드 확인 API"는 없다).
  - 이 화면은 두 API의 요청/응답 계약을 그대로 소비만 한다 — INF-MBR-006/007 본문이 정본이며 여기서
    재정의하지 않는다.
- **로컬 부수효과(API 아님):** confirm 204 성공 직후 `clearSession()`(`src/session.ts`, FUNC-member-004
  소유)을 호출해 이 기기의 localStorage 세션(apiKey/refreshToken)을 지운다 — 서버가 이미 모든 기기의
  리프레시 토큰/API 키를 폐기했으므로, 이 화면의 로컬 상태를 그 사실과 맞추기 위한 자기 정리다.
- **참조 테이블(SCH):** 이 화면 자체는 DB를 직접 호출하지 않는다(모두 INF-MBR-006/007 경유). 참조
  테이블은 해당 INF 문서가 정본 — `MEMBER_PASSWORD_RESETS`, `MEMBERS`.

## 8. 미확인 사항

- **완료(4단계) 화면에서도 카운트다운 인터벌이 계속 돈다** — `PasswordResetPage.tsx:76-81`의
  `useEffect`가 `requestSeq > 0`인 한 `step`과 무관하게 매초 `setNow`를 갱신한다(재작업 지시 1로
  "단계와 무관하게 흐르게" 고친 부작용). 언마운트 시 cleanup되어 누수는 아니고 무의미한 리렌더
  비용만 있다 — QA가 남긴 후속 TODO(low, 게이트 비차단, `STORY-FUNC-member-007.md` "후속 TODO" 2번).
- **`clearSession()`이 계정을 가리지 않는다** — 로그인 상태(계정 A)에서 다른 계정(B)의 비밀번호를
  재설정하면 A의 로컬 세션까지 지워진다. 세션에 email이 없어 `normalizedTarget`과 대조할 수단이
  현재 계약엔 없다. QA 판단: "더 로그아웃"이라 안전 방향이며 재로그인으로 즉시 복구됨(low, 후속 TODO
  3번).
- **실제 이메일/SMS 발송 게이트웨이 연동**은 서버가 로그 시뮬레이션으로 대체하고 있다 — 프론트가
  관여할 부분이 없다(STORY "범위 밖").
- **비밀번호 재설정 링크(URL 클릭형) 방식**은 구현되지 않았다 — 이번 SR은 "코드" 방식만 채택했다
  (STORY "범위 밖", INF-MBR-006/007도 코드 기반).
- 보호되지 않은 라우트(비로그인 전용 화면이라 로그인 여부와 무관하게 접근 가능)의 접근 제어는 이
  FUNC 범위가 아니다(STORY에 명시된 확정 사항).
