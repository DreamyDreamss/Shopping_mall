---
uis-id: UIS-MBR-002
name: 로그인
domain: member
domain-code: MBR
layer: ui
screen-id: login
route: /login
screens_role: 주화면
api_hints:
  - "POST /api/members/login"
  - "POST /api/members/sessions/refresh"
access_control: []
anchors:
  - "modules/shop-web/src/pages/LoginPage.tsx:11-46"
  - "modules/shop-web/src/components/LoginForm.tsx:36-103"
  - "modules/shop-web/src/App.tsx:32-53"
  - "modules/shop-web/src/redirectTarget.ts:21-24"
  - "modules/shop-web/src/refreshOnce.ts:30-40"
  - "modules/shop-web/src/session.ts:14-33"
  - "modules/shop-web/src/api.ts:41-49"
revision_history:
  - version: 1.0
    date: 2026-09-13
    author: ddd-ui-agent (source-authority)
    change: 최초 생성(소스폴백 모드 — FUNC-member-004 신규 구현 반영, DOM 스냅샷·라이브 캡처 없음)
  - version: 1.1
    date: 2026-09-15
    author: dev-agent
    change: QUICK-20260915-1.1 round2 재작업 — 429 정확 초 카운트다운 문구 표시를 제거(완곡화 요건 상충 해소), 관련 라인 앵커 갱신
---

> [반영: FUNC-member-004] 2026-09-13

# UIS-MBR-002: 로그인

> **근거 소스(권위):** `modules/shop-web/src/pages/LoginPage.tsx` + `src/components/LoginForm.tsx` +
> `src/App.tsx`(부팅 시 무음 리프레시) + `src/redirectTarget.ts` + `src/session.ts` + `src/api.ts`.
> 소스폴백 모드 — DOM 스냅샷·스크린샷 없음(AIDD 작업 중 `npm run dev`를 켜지 않는다는 제약, HMR OOM
> 회피). `LoginForm.stories.tsx`의 6개 상태(기본·입력됨·제출중·실패401·잠금429·비밀번호토글)를
> §5 표시조건의 1급 증거로 사용했다.

> **라우트 참고:** `HashRouter` 사용(`App.tsx:45-51`) — 브라우저 주소창 상 실제 경로는 `/#/login`이다.
> `redirect` 쿼리 파라미터는 `/#/login?redirect=<encodeURIComponent(path)>` 형태의 **프론트 전용
> 관례**(서버와 무관한 라우팅 계약, `LoginPage.tsx:24-27`) — 없으면 로그인 성공 후 기본 `/`로 이동한다.

## 1. 화면 목적

이메일+비밀번호로 로그인해 세션(리프레시 토큰 30일 + API 키)을 발급받고, 로그인 성공 시 원래
가려던 경로(`redirect` 파라미터) 또는 기본 `/`로 이동하는 화면이다. 잠금·존재·비밀번호 대조는
서버(INF-MBR-003)가 이미 판정하며, 이 화면은 그 결과(코드·문구·`retryAfterSeconds`)를 그대로
표시만 한다 — 회원 없음/탈퇴/비밀번호 오류를 프론트가 구분해 노출하지 않는다(존재 오라클 방지).

## 2. 주요 작업 시나리오

**시나리오: 로그인**
1. 이메일(`email`)·비밀번호(`password`)를 입력한다(`LoginForm.tsx`).
2. 필요하면 [보기] 버튼으로 비밀번호를 평문으로 확인한다(로컬 상태 토글, 서버 호출 없음).
3. [로그인] 버튼을 눌러 제출 → `LoginPage.handleSubmit` → POST /api/members/login(INF-MBR-003).
4. **성공**: 응답(`SessionResult`)을 `saveSession`으로 localStorage 단일 키(`sl.member.session`)에
   원자적으로 저장 → `resolveRedirectTarget(params.get('redirect'))`가 정한 경로로 이동(기본 `/`,
   절대 URL·프로토콜 상대·역슬래시 변형은 오픈 리다이렉트로 판단해 무시하고 `/`로 보냄).
5. **실패 401**(`MBR-4011`, "…(n/5)" 카운트 포함 문구): 오류 배너에 서버 문구를 그대로 표시, 재입력·재제출 가능.
6. **실패 429**(`MBR-4291`, 5회 연속 실패 도달): 오류 배너에 서버의 완곡 문구("…잠시 후 다시
   시도해 주세요")를 그대로 표시하고, `retryAfterSeconds`가 다 지날 때까지(내부 타이머) 로그인
   버튼이 비활성된다(재제출 자체가 막힌다) — **정확한 초는 화면에 노출하지 않는다**(round2 QA
   재작업: 정확 초 병기가 "잠시 후"로 완곡화하라는 요건과 상충했다).

**부가 시나리오: 부팅 시 자동 재로그인(사용자 조작 아님, 세션 생명주기 참고)**
1. 앱이 뜰 때(`App.tsx`) 저장된 세션에 `refreshToken`이 있으면, 이 화면과 무관하게 무음으로
   POST /api/members/sessions/refresh(INF-MBR-005)를 1회 호출해 세션을 30일 롤링 갱신한다
   (`refreshOnce.ts`의 모듈 스코프 in-flight 가드로 StrictMode 이중 발사를 막는다).
2. 응답이 오면 무조건 `saveSession`(회전 API라 구 토큰은 이미 폐기됨).
3. 401(`MBR-4012`: 미존재/만료/폐기/탈퇴 4가지 사유 동일 응답)만 저장된 세션을 조용히 지운다.
   그 외 오류(5xx·네트워크 장애)는 세션을 보존하고 다음 부팅 때 재시도한다.

## 3. 화면 구성 (블록)

| 마커 | 블록 | 역할 | 주요 위젯 | 소스 근거 |
|---|------|------|----------|----------|
| ① | 로그인 폼 | 이메일/비밀번호 입력 + 제출 | email input, password input, 보기/숨기기 토글, 로그인 버튼 | `LoginForm.tsx:72-101` |
| ② | 오류 배너 | 401/429 오류 표시(조건부) | `role=alert` div, 서버 문구 그대로(정확 초 없음) | `LoginForm.tsx:90-94` |

## 4. 위젯·액션

**① 로그인 폼**

| 번호 | 위젯 | 타입 | 레이블 | 동작 | 연결 API(raw) | 결과 |
|---|------|------|--------|------|--------------|------|
| (1) | email input | input(email) | 이메일 | 입력값 갱신(`onChange`) | — | — |
| (2) | password input | input(password/text) | 비밀번호 | 입력값 갱신, `showPassword`에 따라 `type` 전환 | — | — |
| (3) | 보기/숨기기 버튼 | button(type=button) | 보기 / 숨기기 | `showPassword` 로컬 토글(서버 호출 없음) | — | password ↔ text 전환 |
| (4) | 로그인 버튼 | button(type=submit) | 로그인 / 로그인 중… | 폼 제출 → `LoginPage.handleSubmit` | POST /api/members/login | 성공: 세션 저장+이동 / 401: 오류 배너 표시 / 429: 오류 배너(완곡 문구), 버튼 비활성(정확 초는 미노출) |

**② 오류 배너**

| 번호 | 위젯 | 타입 | 레이블 | 동작 | 연결 API(raw) | 결과 |
|---|------|------|--------|------|--------------|------|
| (5) | 오류 메시지 | div[role=alert] | (서버 `message` 그대로) | 표시 전용, 재해석 없음 | — | `MBR-4011`(401)·`MBR-4291`(429)·`MBR-5000`(500) 문구 렌더 |

## 5. 접근 권한·표시 조건

| 요소 | 표시 조건 | 근거 | 스토리 |
|------|----------|------|--------|
| 로그인 버튼 비활성(`disabled`) | `busy`(제출 중) 이거나 `locked && remaining > 0`(429 내부 타이머 진행 중, 화면에 초는 안 보임) | `LoginForm.tsx:70,95`, stories: ./src/components/LoginForm.stories.tsx | [제출중](story:회원-로그인--제출중) |
| 오류 배너 표시 | `error`가 non-null | `LoginForm.tsx:90-94`, stories: ./src/components/LoginForm.stories.tsx | [실패401](story:회원-로그인--실패401) |
| (제거됨, round2 QA 재작업) 정확 초 카운트다운 문구 | 종전에 `" — N초 후 다시 시도"`를 병기했으나, 서버의 "잠시 후" 완곡화 요건과 상충해 **표시를 제거**했다. `retryAfterSeconds`(`error?.retryAfterSeconds`)는 여전히 `remaining` 내부 타이머로 남아 버튼 비활성 유지에만 쓰인다 | `LoginForm.tsx:38-39,55-68` | [잠금429](story:회원-로그인--잠금429) |
| 비밀번호 평문 표시 | `showPassword === true`(보기 버튼 클릭) | `LoginForm.tsx:37,81`, play 인터랙션으로 검증 | [비밀번호토글](story:회원-로그인--비밀번호토글) |
| 기본(빈 입력) 상태 | 초깃값(`email:'', password:''`) | `LoginForm.tsx` | [기본](story:회원-로그인--기본) |
| 입력됨 상태 | 이메일·비밀번호 모두 채워짐, 오류 없음 | `LoginForm.tsx` | [입력됨](story:회원-로그인--입력됨) |

## 6. 팝업·연계 화면

없음(팝업/모달 트리거 없음). 로그인 성공 시 `useNavigate`로 같은 SPA 내 다른 라우트(`redirect`
파라미터가 가리키는 경로, 기본 `/`)로 이동한다 — 팝업이 아닌 페이지 이동(`LoginPage.tsx:28`).

## 7. 데이터 출처·연결

- **연결 API(raw → INF):**
  - `POST /api/members/login` — INF-MBR-003. 이 화면(로그인 폼)의 유일한 사용자 트리거 호출.
  - `POST /api/members/sessions/refresh` — INF-MBR-005. **이 화면이 직접 호출하지 않는다** — `App.tsx`의
    부팅 시 무음 리프레시(`useSilentRefresh`)가 호출하는 것으로, 로그인 폼 자체의 액션은 아니지만
    "세션 생명주기의 반대편"으로서 이 화면의 스펙에 참고 기록한다.
  - **`INF-MBR-004`(로그아웃)는 이 화면·이 FUNC이 호출하지 않는다** — `api.ts`에 `logout` 함수 자체가
    없다(직접 확인). 로그아웃 UI/트리거는 아직 shop-web 어디에도 없다(§8 참조).
- **참조 테이블(SCH):** 이 화면 자체는 DB를 직접 호출하지 않는다(모두 INF-MBR-003/005 경유). 참조
  테이블은 해당 INF 문서가 정본 — `MEMBERS`, `MEMBER_LOGIN_ATTEMPTS`, `MEMBER_REFRESH_TOKENS`,
  `MEMBER_API_KEYS`.

## 8. 미확인 사항

- 로그아웃 UI/트리거(헤더·계정메뉴)는 이 화면(UIS-MBR-002)의 범위가 아니다 — 백엔드(INF-MBR-004,
  FUNC-member-006)는 Done이지만 이를 호출할 컴포넌트가 shop-web에 아직 없다. **범위 밖(후속
  FUNC)** — STORY-FUNC-member-004 "범위 밖" 절에서 명시적으로 확정된 사항이며 미확인이 아니다.
- 보호 라우트(비로그인 시 접근 차단 + `?redirect=` 자동 채움)는 아직 없다 — 이 화면은 `redirect`
  파라미터를 "읽어서 돌아가는" 쪽만 구현하고, 채워 보내는 진입점(가드)은 shop-web의 어떤 화면에도
  없다. **범위 밖(후속 FUNC)**.
- `/login`에 이미 로그인된 상태로 접근했을 때 자동 리다이렉트하는 동작은 없다(AC에 없는 엣지케이스,
  STORY에 범위 밖으로 명시).
- 로그인 성공 후 발급받은 `apiKey`를 이후 인증이 필요한 API 호출 헤더에 실제로 붙이는 동작은 아직
  없다 — `api.ts`의 현재 함수(`get`/`fetchOrders`/`fetchOrder`) 중 인증이 필요한 엔드포인트를 호출하는
  것이 없다. 이 화면은 발급·저장까지만 담당한다. **범위 밖(그 엔드포인트를 쓰는 후속 FUNC의 책임)**.
- `refreshToken`을 localStorage에 저장하는 것은 XSS로 탈취 가능한 노출 위험이 있으나 랩 규모로
  수용된 임시 결정이다(`session.ts:10-12`, 사람 결정 3) — httpOnly 쿠키 전환은 백엔드 계약 변경이
  선행돼야 하는 **후속 SR 대상**으로 이미 이월 확정됨.

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-15 | QUICK-20260915-1 | #1 | 잠금429 카운트다운 정확 초 노출 제거 — 완곡화 요건 반영, 라인 앵커 갱신 |  |
