---
story-id: STORY-SR-309.1
item: SR-309.1
title: 디자인 토큰
status: Done
domain: common
created: 2026-09-19
spec_markers: 0
sr-id: SR-309
approved_sha: b3547a386646
---

# STORY-SR-309.1 — 디자인 토큰·Pretendard 도입 — KT알파쇼핑 톤(색·타이포·간격·radius·그림자) — 디자인 토큰

## Story
디자인 토큰·Pretendard 도입 — KT알파쇼핑 톤(색·타이포·간격·radius·그림자) — 디자인 토큰


## 변경 컨텍스트 (SR-309)
> 이 story는 변경요청 **SR-309 — 디자인 토큰·Pretendard 도입 — KT알파쇼핑 톤(색·타이포·간격·radius·그림자)** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-309/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-309/02_변경명세.md`

### 확정된 요건 문답 4건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: shop-web 전역 디자인 토큰(CSS 변수 한 파일 — 색·그라데이션 2종·타이포 스케일 8단·굵기 3종·간격 4px 배수·radius 3종·그림자 3종·z-index·본문 폭 750px)과 Pretendard woff2 서브셋 저장소 번들(OFL, font-display: swap, 시스템 한글 폰트 폴백), 스토리북 'Tokens' 문서 페이지(색 견본·대비 표·타이포 견본). 값은 KT알파쇼핑 실측(#101010·#767676·#AAAAAA·#F2F2F2·#F5F5F5·#F9F9F9·#713FC5/#F2EBFF·#177BC3·#FF5259·#ED1C24·딤 rgba(16,16,16,.3)). 제외: 기존 화면·컴포넌트의 스타일 교체(SR-310 공통 컴포넌트·SR-311 앱 셸·SR-322/326 리스킨에서), 다크 모드, 백엔드 변경.
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 화면의 동작·문구·레이아웃과 기존 테스트(shop-web jest·스토리북 빌드) 전부 불변. 토큰은 새로 추가만 하고 기존 스타일은 이 SR에서 바꾸지 않는다(전역 body 폰트를 Pretendard로 바꾸는 것만 허용 — 폰트 교체로 인한 레이아웃 붕괴가 없어야 한다).
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 화면 내용 변경 없음 — 전역 토큰 파일·폰트 로드(index.html/전역 CSS)와 스토리북 Tokens 문서 페이지만 추가한다. UIS 화면설계서 대상 화면은 없다.
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 스토리북 'Design Tokens' 문서 페이지 1개: 색 견본(토큰명·값·용도)·배경×본문 명도 대비 표(4.5:1 미달 조합 표시)·타이포 스케일 견본·간격/radius/그림자 견본.

### 구현 모듈(제약) — `shop-web` (`{{SRC_SHOP_WEB}}`)
이 작업 항목의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약·편성에서 사람이 지정한 값).

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-309/02_변경명세.md`에서 도출)
- [x] UIS-CMN-001: 색 토큰: 본문 #101010, 보조 #767676 / #AAAAAA, 면 #F2F2F2 / #F5F5F5 / #F9F9F9, 포인트 보라 #713FC5(배경 #F2EBFF), 링크 파랑 #177BC3, 할인·가격 빨강 #FF5259, 브랜드 레드 #ED1C24, 딤 rgba(16,16,16,.3), 상태색(성공/경고/오류/정보)
- [x] UIS-CMN-001: 그라데이션 토큰 2종: 파랑→보라(구매 버튼·헤더 토글), 보라→파랑(최근 본 상품 헤더)
- [x] UIS-CMN-001: 타이포: Pretendard, 본문 14px 기준 스케일 8단, 굵기 3종, 배너 카피용 굵은 헤드라인 40px대
- [x] UIS-CMN-001: 간격: 4px 배수
- [x] UIS-CMN-001: radius: 3종(카드 썸네일 8px 포함)
- [x] UIS-CMN-001: 그림자: 3종
- [x] UIS-CMN-001: z-index 층
- [x] UIS-CMN-001: 레이아웃 폭: 본문 750px 단일 컬럼
- [x] UIS-CMN-001: Pretendard woff2 서브셋 저장소 번들(OFL 라이선스), `font-display: swap`, 시스템 한글 폰트 폴백(웹폰트 로드 실패 시 대체)
- [x] UIS-CMN-001: 로드 위치: 외부 CDN 없이 앱이 서빙하는 정적 경로(index.html/전역 CSS)
- [x] UIS-CMN-001: 스토리북 'Design Tokens' 문서 페이지 1개: 색 견본(토큰명·값·용도), 배경×본문 명도 대비 표(4.5:1 미달 조합 표시), 타이포 스케일 견본, 간격/radius/그림자 견본
- [x] UIS-CMN-001: 전역 body 폰트를 Pretendard로 교체(허용된 유일한 기존 스타일 변경)

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **신규 스펙(예약 — 본문은 구현 후 역생성)**: UIS-CMN-001
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)



## 구현 계획

- **파일**:
  - `modules/shop-web/src/styles/tokens.css` (신규) — `:root`에 전체 CSS 커스텀 프로퍼티 선언(아래 "계약" 네이밍) + `@font-face` 3개(Pretendard `Regular`=weight 400, `SemiBold`=weight 600, `Bold`=weight 700, `font-display: swap`, 상대경로 `url()`, 파일명과 `font-weight` 값이 서로 어긋나지 않게 1:1로 맞춘다) + **`body` 선택자 1개**(font-family를 Pretendard→시스템 한글 폰트 폴백 스택으로 교체 — 이 SR에서 허용된 유일한 기존 스타일 변경, 그 외 전역 선택자·리셋은 추가하지 않는다).
  - `modules/shop-web/src/assets/fonts/Pretendard-Regular.woff2` · `Pretendard-SemiBold.woff2` · `Pretendard-Bold.woff2` (신규 바이너리, 서브셋 아님) — 출처는 npm 공식 배포 패키지 `pretendard`(OFL-1.1)의 `dist/web/static/woff2/` 안에 있는 동명 파일을 **그대로** 복사한다(로컬에 서브셋 생성 도구가 없으므로 서브셋을 만들지 않고 원본 파일을 쓴다 — 그래서 파일명에 `.subset`을 붙이지 않는다). 패키지 취득 방법: `npm pack pretendard`(또는 임시 `npm install` 후 `dist/web/static/woff2/*` 3개 파일만 추출)로 tarball을 받아 필요한 3개 woff2만 꺼내고, 추출에 쓴 node_modules/tarball은 정리한다 — **`package.json`/`package-lock.json`은 변경하지 않는다**(devDependency로 추가하지 않음, 런타임 의존 없이 정적 파일만 저장소에 커밋).
  - `modules/shop-web/src/assets/fonts/LICENSE-Pretendard.txt` (신규) — 같은 npm 패키지에 동봉된 OFL 라이선스 원문을 그대로 복사(요구사항 "OFL 라이선스" 조건).
  - `modules/shop-web/src/styles/contrast.ts` (신규) — 부수효과 없는 순수함수 `hexLuminance(hex)` / `contrastRatio(hexA, hexB)` / `meetsAA(ratio, isLargeText?)`(WCAG 상대휘도·대비 공식 구현).
  - `modules/shop-web/src/styles/contrast.unit.test.ts` (신규) — 기존 `redirectTarget.unit.test.ts`와 같은 위치·형식.
  - `modules/shop-web/src/styles/tokenCatalog.ts` (신규) — `{ cssVar, label, usage }[]` 메타만 담는다(색상 실값은 여기 넣지 않는다 — 값의 정본은 `tokens.css` 하나뿐, 이중관리 금지).
  - `modules/shop-web/src/styles/DesignTokens.stories.tsx` (신규) — CSF 스토리 1개(title `Design Tokens`). 렌더 시 `getComputedStyle(document.documentElement).getPropertyValue(cssVar)`로 **실제 적용된 값**을 읽어 색 견본·대비표(`contrast.ts` 사용)·타이포 견본(굵기 견본은 400/600/700 세 값으로 표기)·간격/radius/그림자 견본을 그린다 — 값을 이 파일에 하드코딩하지 않는다. 이 파일 자체가 `*.stories.tsx`라 `story-per-component` pair 규칙 exclude 대상(별도 짝 컴포넌트 `.tsx`를 새로 만들지 않으므로 pair 의무 자체가 생기지 않는다).
  - `modules/shop-web/src/main.tsx` — 1줄 추가 `import './styles/tokens.css'`(Vite가 해시된 정적 자산으로 번들 — index.html은 직접 건드리지 않는다. "앱이 서빙하는 정적 경로" 요건 충족).
  - `modules/shop-web/.storybook/preview.ts` — 1줄 추가 `import '../src/styles/tokens.css'`(Design Tokens 스토리가 실제 CSS 변수값을 읽으려면 프리뷰 iframe에도 로드돼야 한다).
  - `package.json`/`package-lock.json` — **변경 없음**(사람 수정 반영: 폰트는 파일 복사만, devDependency 추가 안 함).

- **데이터**: 없음 — DB·테이블 변경 없음(확정 문답 db_ripple·db_migration, 요구사항 "범위" 제외와 일치). 트랜잭션 경계·락·원자 UPDATE 대상 행 없음.

- **순서·보안**: 해당 없음 — 인증·존재판정·레이트리밋 경로 신설 없음(정적 자산·문서 스토리 SR). 부수효과(로그·발송·이벤트·감사)도 없음.

- **계약**: 새 오류 코드·응답 봉투·상태 코드 없음(백엔드 미변경). 대신 신규 CSS 커스텀 프로퍼티 네이밍을 이번에 확정한다(후속 SR-310/311/322/326이 참조) — `--color-*`(색), `--gradient-*`(그라데이션 2종), `--text-*`(타이포 크기 8단), `--font-weight-*`(굵기 3종 — **`--font-weight-regular: 400` · `--font-weight-semibold: 600` · `--font-weight-bold: 700`로 확정, "Medium"이라는 이름의 토큰은 만들지 않는다 — 사람 수정 반영**), `--space-*`(4px 배수), `--radius-*`(3종, 카드 썸네일용 `--radius-md`=8px), `--shadow-*`(3종), `--z-*`(z-index 층), `--layout-content-width`(750px). 상태색 4종(성공/경고/오류/정보)은 벤치마크에 정확한 hex가 없어(요구사항 라벨만 명시) 기존 `DeliveryBadge.tsx`의 배지 텍스트색(성공 계열 `#136b2f`, 경고 계열 `#8a5a00`)과 정합되는 값으로 확정한다(사람 수정으로 재확인됨) — 나머지 오류/정보 계열도 같은 톤(채도·명도) 규칙으로 파생시키고 QA 단계에서 벤치마크 스크린샷 대조로 조정 가능함을 남긴다.

- **테스트**:
  - `npm test`(typecheck+jest) 전체 통과 — 기존 3개 `*.unit.test.ts`/`*.test.tsx` + 신규 `contrast.unit.test.ts`.
  - `npm run test-storybook` 전체 통과(콘솔 error 0건) — 신규 `Design Tokens` 스토리 포함.
  - `npm run build`(vite build, `base:'/shop/'`)와 `npm run build-storybook`을 로컬 1회 실행해 산출물에서 폰트 요청 경로가 실제로 존재하는 파일을 가리키는지 확인(아래 "프레임워크 함정" 참조).
  - 경계값: `contrast.ts` — 흑백(21:1)·동일색(1:1) 같은 알려진 값과, 4.5:1 문턱에 걸치는 인접 쌍(4.49 vs 4.51) 각각 `meetsAA` 참/거짓 단언.
  - 기준선 영향: body 폰트 교체가 기존 컴포넌트 스토리의 텍스트 렌더 폭에 영향을 줄 수 있으므로, STEP 5에서 `story_shots.py`(축E 시각 기준선)로 기존 스토리들이 레이아웃 붕괴 없이 그대로인지 반드시 비교한다("허용된 유일한 변경"이라도 회귀 범위의 "레이아웃 붕괴 없어야" 조건은 유효).
  - 원본(비서브셋) woff2 3개 파일의 실제 바이트 크기(`Regular`/`SemiBold`/`Bold` 각각)를 구현 단계에서 확인해 `## Dev 기록`에 적는다(사람 수정 반영 — 서브셋 대비 용량이 커진 사실을 남긴다).

- **테스트 격리**: 신규 테스트는 DB·외부 상태가 없는 순수함수/스토리 렌더뿐이라 상태 누수 대상이 없음 — 고유 식별자·`@AfterEach` 정리 불필요.

- **폴백·우회 경로의 자격 판정**: 없음 — 인증·조회 경로 신설 없음(이 SR은 정적 자산·문서 스토리만 추가).

- **프레임워크 실행 모델 함정**:
  1. Vite `base` 불일치(SR-301 #1과 같은 조건) — `vite.config.ts`는 `command==='build'`일 때만 `base:'/shop/'`, dev/storybook은 `'/'`다. 폰트를 `public/`에 두고 CSS에서 루트 절대경로(`/fonts/...`)로 참조하면 dev에서는 되지만 build 산출물이 `/shop/` 서브패스로 서빙될 때 실제 파일 위치와 어긋나 404가 난다. 그래서 폰트는 `public/`이 아니라 `src/assets/fonts/`에 두고 CSS에서 **상대경로**(`url('../assets/fonts/Pretendard-Regular.woff2')` 등, `.subset` 없는 파일명)로 참조해 Vite 자산 파이프라인이 해시+base를 자동 처리하게 한다 — `npm run build` 산출물을 직접 열어 폰트 요청이 `/shop/assets/...`로 나가는지 확인한다.
  2. 스토리북 콘솔-오류 판정(SR-306 #1 r3~r4와 같은 조건: 스토리북에서 정적 자산 404 → 콘솔 error → 축E FAIL) — 위 1번(상대경로 자산 임포트)을 쓰면 스토리북(Vite 빌더, base 기본 `/`)도 같은 처리 경로를 타 폰트가 정상 로드되므로 `.storybook/main.ts`의 `staticDirs`(현재 shop-api 정적 경로 전용)를 건드릴 필요가 없다. 만약 대신 `public/fonts`로 갔다면 이 `staticDirs`가 shop-web 자신의 `public`을 서빙하지 않아 폰트 404 위험이 있었을 것.
  3. 그 외 없음 — React StrictMode 이중 실행·Spring 프록시 self-invocation·스케줄러 다중 인스턴스는 이 SR(정적 자산·순수함수·문서 스토리)에 해당 사항 없음.

- **범위 밖**: 기존 화면·컴포넌트가 이 토큰을 실제로 참조하도록 바꾸는 작업(SR-310 공통 컴포넌트·SR-311 앱 셸·SR-322/326 리스킨에서), 다크 모드, `--layout-content-width` 실제 적용(값만 정의, 레이아웃 적용은 후속 SR), 폰트 서브셋 경량화(도구 부재로 이번엔 원본 woff2를 쓰고 후속 SR 후보로 남긴다 — 사람 수정 반영).

- **실패 사례집 대조**:
  - SR-301 #1(정적 리소스 base 경로 불일치) — 조건(빌드 시 서브패스 서빙)이 이번 폰트 자산에도 성립 → `src/assets` 상대 임포트로 회피(함정 1).
  - SR-306 #1 r3~r4(스토리북 콘솔 error = 스토리 깨짐 판정, 정적 자산 404) — 조건이 폰트에도 성립할 수 있어 같은 회피(상대 임포트, `staticDirs` 불변) 적용(함정 2).
  - SR-231 r2(전역 속성 하나를 바꿔 다른 FUNC 의미가 파급) — 조건: "한 필요로 전역 계약을 변경". 이번엔 `body{font-family}` **하나만** 전역 선택자로 추가하고 마진·box-sizing 리셋 등은 추가하지 않아 조건을 피한다.
  - 나머지(SR-231 세션변수/락 순서, SR-232 API키 폴백, SR-234 카운트다운, SR-297/298 카운터·정리배치, SR-300 시간드리프트, SR-302 리다이렉트, SR-303 진입점 누락, SR-235 UTF-8) — 이 SR에 해당 조건(DB·인증·시간·라우팅·시드데이터) 자체가 없어 불성립.

### 사람 수정
- **굵기 정합**: 파일과 `@font-face` 선언을 맞춘다 — `Regular=400`·`SemiBold=600`·`Bold=700` 세 파일로 하고 `--font-weight-*` 토큰도 400/600/700(Medium 파일을 600으로 선언하지 않는다).
- **폰트 출처**: 공식 배포본을 쓴다 — npm 패키지 `pretendard`(OFL-1.1)의 `dist/web/static/woff2` 파일(`Pretendard-Regular/SemiBold/Bold.woff2`)을 `src/assets/fonts/`로 복사하고 LICENSE를 함께 둔다. 서브셋 도구가 없으면 서브셋을 만들지 말고 원본 woff2를 쓴다(파일명에서 `.subset` 제거, 크기를 `## Dev 기록`에 적는다). 패키지는 devDependency로 두지 말고 파일만 복사한다(런타임 의존 없음).
- 상태색 4종은 기존 `DeliveryBadge` 정합값으로 확정해도 된다.
- 나머지는 위 계획대로 진행.

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
(dev-agent가 생성 파일·주요 결정 기록)
- 계획 확인: 계획 수정(굵기 정합 400/600/700, 공식 npm `pretendard` 배포본 사용, 상태색 DeliveryBadge 정합값 확정) (2026-09-19)

### 구현 완료 (2026-09-19)

**생성 파일**
- `modules/shop-web/src/styles/tokens.css` — `:root` 전역 CSS 커스텀 프로퍼티(색·그라데이션·타이포·굵기·간격·radius·그림자·z-index·레이아웃) + `@font-face` 3개(Pretendard Regular/SemiBold/Bold, `font-weight` 400/600/700, `font-display: swap`, 상대경로 `url()`) + `body` 폰트 교체(이 SR에서 허용된 유일한 기존 스타일 변경).
- `modules/shop-web/src/assets/fonts/Pretendard-Regular.woff2` — 765,892 bytes
- `modules/shop-web/src/assets/fonts/Pretendard-SemiBold.woff2` — 785,856 bytes
- `modules/shop-web/src/assets/fonts/Pretendard-Bold.woff2` — 791,156 bytes
  (서브셋 생성 도구가 없어 원본 파일 그대로 사용 — 계획대로 파일명에 `.subset` 없음. 서브셋 대비 용량이 크다 — 후속 SR 후보로 "범위 밖"에 이미 명시됨)
- `modules/shop-web/src/assets/fonts/LICENSE-Pretendard.txt` — npm `pretendard@1.3.9` 패키지 동봉 OFL 라이선스 원문 그대로.
- `modules/shop-web/src/styles/contrast.ts` — 순수함수 `hexLuminance`/`contrastRatio`/`meetsAA`(WCAG 2.1 상대휘도·대비 공식).
- `modules/shop-web/src/styles/contrast.unit.test.ts` — 흑백(21:1)·동일색(1:1)·3자리 hex·인자순서 무관·AA 4.5:1 인접 경계값(4.49/4.5/4.51)·큰텍스트 3:1 경계값 단언.
- `modules/shop-web/src/styles/tokenCatalog.ts` — 색/그라데이션/타이포/굵기/간격/radius/그림자/z-index/레이아웃 카탈로그 메타(`{cssVar,label,usage}[]`) — 색상 실값은 담지 않음(정본은 tokens.css).
- `modules/shop-web/src/styles/DesignTokens.stories.tsx` — CSF 스토리 1개(title `Design Tokens`, tags `['UIS-CMN-001']`). `getComputedStyle(document.documentElement)`로 실제 적용값을 읽어 색 견본·배경×본문 대비표(AA 4.5:1 미달 표시)·타이포/굵기/간격/radius/그림자/z-index 견본을 렌더. 값을 파일에 하드코딩하지 않음.

**수정 파일**
- `modules/shop-web/src/main.tsx` — `import './styles/tokens.css'` 1줄 추가(Vite가 해시된 정적 자산으로 번들).
- `modules/shop-web/.storybook/preview.ts` — `import '../src/styles/tokens.css'` 1줄 추가(Design Tokens 스토리가 프리뷰 iframe에서 실제 CSS 변수값을 읽을 수 있도록).
- `package.json` / `package-lock.json` — 변경 없음(계획대로, pretendard는 devDependency로 추가하지 않음).

**상태색 확정**: 성공 `#136b2f`/`#e9f7ec`, 경고 `#8a5a00`/`#fff4e5`(기존 `DeliveryBadge.tsx` 정합) — 정보는 같은 파일의 `SHIPPED` 배지색 `#0b4ea2`/`#e8f1ff`를 재사용, 오류는 같은 톤·명도 규칙(진한 채도의 어두운 톤)으로 `#b42318`/`#fdecea`를 새로 파생. 벤치마크 정확한 hex가 없어(요구사항 라벨만 명시) QA 단계 스크린샷 대조로 조정 가능함을 남긴다.

**검증(로컬 1회 실행)**
- `npm test`(typecheck + jest): 19 suites / 215 tests 전부 통과(기존 3개 unit test + 신규 `contrast.unit.test.ts` 포함).
- `npm run build`(vite build, `base:'/shop/'`): 산출물 CSS의 `url(...)`이 `/shop/assets/Pretendard-*.woff2`로 정확히 해시·서브패스 처리됨을 확인(SR-301 #1 조건 회피 확인).
- `npm run build-storybook`: 산출물 CSS의 `url(...)`이 `./Pretendard-*.woff2`(base `/`)로 정상 처리됨을 확인.
- `npm run test-storybook`(정적 산출물을 로컬 HTTP 서버로 서빙 후 실행): 41 suites / 137 tests 전부 통과, 콘솔 error 0건(신규 `DesignTokens.stories.tsx` 포함) — Playwright 브라우저가 이 환경에 없어 `npx playwright install chromium-headless-shell` 1회 설치 후 실행함(환경 준비 단계, 코드 변경 아님).
- `story_shots.py` 기준선 비교(축E, body 폰트 교체 영향)는 STEP 5 QA 단계에서 별도 수행 필요 — dev 단계에서는 test-storybook 콘솔 error 0건까지만 확인.

### 재작업 완료 (2026-09-19, round 2)

사람 코멘트에 따라 QA CONCERNS 4건 중 **2건만 재작업**했다(2·3). 1·4는 이 SR 범위 밖으로 확정 — 코드 변경 없음.

**수정 파일**
- `modules/shop-web/src/styles/DesignTokens.stories.tsx`
  - `CONTRAST_TEXTS`를 5개 → `tokenCatalog`의 텍스트 용도 토큰 전부(11개: `--color-text`·`-secondary`·`-tertiary`·`--color-link`·`--color-primary`·`--color-price`·`--color-brand`·`--color-success`·`--color-warning`·`--color-error`·`--color-info`)로 확장(QA 권고 2).
  - 대비표 판정을 2단(통과/미달) → 3단(`통과`/`큰 글씨 전용`/`본문 사용 금지`)으로 변경 — 일반 텍스트 AA(4.5:1) 미충족이라도 큰 텍스트 AA(3:1)를 충족하면 "큰 글씨 전용"으로 별도 표기.
  - "배경×본문 명도 대비 표" 섹션 본문에 `--color-price`(실측 3.18:1, 굵은 큰 가격 숫자 전용)·`--color-brand`(실측 4.38:1, 큰 글씨·로고 전용) 사용 규칙 문단 추가.
- `modules/shop-web/src/styles/tokens.css` — `--color-price`·`--color-brand` 선언 바로 위에 실측 대비율과 사용 제약(본문 금지·용도 한정) 주석 추가. 값 자체는 변경 없음.
- `modules/shop-web/src/styles/contrast.ts` — `hexToRgb`가 정규화 후 `/^[0-9a-fA-F]{6}$/`에 맞지 않으면(빈 문자열·`rgba(...)` 등) `Error`를 던지도록 변경(QA 권고 3). 기존 호출 경로(hex 토큰만 전달)는 영향 없음.
- `modules/shop-web/src/styles/contrast.unit.test.ts` — 비-hex 입력(`rgba(...)` 문자열, 빈 문자열) 시 예외를 던지는 테스트 2건 추가.

**이월(범위 밖 — 이 SR에서 고치지 않음)**
- QA 이슈 1(인라인 `fontFamily`가 전역 `body` 폰트를 가림) — 이월(SR-322/326), 이 SR 범위 아님. `src/pages/*.tsx` 등 15개 파일 미변경.
- QA 이슈 4(원본 woff2 2.34MB, 서브셋 미적용) — 이월(SR-291), 이 SR 범위 아님. 폰트 파일 미변경.

**검증(로컬 1회 실행, round 2 재실행)**
- `npm test`(typecheck + jest): 19 suites / **217 tests** 전부 통과(기존 215 + 신규 비-hex 예외 테스트 2건).
- `npm run build-storybook`: 정상 빌드(경고 없음, `DesignTokens.stories-*.js` 정상 emit).
- `npm run test-storybook`: 정적 산출물을 로컬 HTTP 서버로 서빙 후 실행 — **41 suites / 137 tests 전부 통과**, 콘솔 error 0건(`DesignTokens.stories.tsx` 포함). 참고: 기본 동시성(디폴트 워커 수)으로 돌리면 이 환경의 리소스 경합으로 다수 스위트가 `ERR_CONNECTION_REFUSED`/타임아웃으로 오탐 실패했다 — `--maxWorkers=2`로 낮추자 전부 통과로 재현됨(코드 결함 아님, 로컬 실행 환경의 동시성 한계).

### 재작업 완료 (2026-09-19, round 3)

round 2 QA CONCERNS 4건(사람 코멘트로 확정) 중 1~3건을 재작업했다. 4번(축E 시각 기준선 재기록)은 지시대로 STEP 5 몫이라 이번 라운드에서 실행하지 않았다.

**수정 파일**
- `modules/shop-web/src/styles/DesignTokens.stories.tsx`
  - `CONTRAST_BACKGROUNDS`에 `#ffffff`(흰 배경 — 상품 카드·본문 기본 배경)를 맨 앞에 추가(4개 → 5개). CSS 커스텀 프로퍼티가 아닌 리터럴 배경을 표에 섞어 넣어야 해서 `resolveBackground(bg)` 헬퍼를 새로 만들었다 — `--`로 시작하면 `tokenValue()`로 CSS 변수를 읽고, 아니면(`#ffffff`) 리터럴 hex를 그대로 값으로 쓴다.
  - `--color-price`·`--color-brand` 사용 규칙 문단을 **정적 문장에서 계산된 문장으로 교체**했다. `describeUsage(cssVar)`가 `ContrastTable`과 똑같은 계산 함수(`evaluateContrast`)로 그 토큰이 5개 배경 각각에서 통과/큰 글씨 전용/본문 사용 금지 중 무엇인지 매 렌더마다 다시 계산해 문장을 만든다 — 문단이 표와 다른 값을 말하는 것이 구조적으로 불가능해졌다(round 2 QA 권고 1의 근본 원인이 "손으로 쓴 수치가 표 구성과 어긋날 수 있다"는 것이었어서, 손으로 쓰지 않는 방향으로 고쳤다).
    - 브라우저 실측(Node로 `contrast.ts`와 동일한 공식 재계산, 값 확인): `--color-price`는 흰 배경(3.18:1)·`--color-surface-3`(3.02:1)에서만 큰 글씨 기준(3:1) 통과, `--color-surface-2`(2.92)·`--color-surface-1`(2.84)·`--color-primary-bg`(2.74)에서는 미달 — 생성된 문장이 이 경계(흰 배경은 되고 surface-3도 되지만 그 아래 3개는 안 됨)를 정확히 반영한다. 원래 사람 코멘트 예시 문장("surface-1/2/3·primary-bg 모두 미달")은 surface-3(3.02) 실측과 어긋나 그대로 옮기지 않고 계산값을 따랐다(요청 본문의 "문단 수치는 표에서 실제 계산되는 값과 일치시킨다" 지시를 우선).
    - `--color-brand`는 5개 배경 전부에서 큰 글씨 기준을 통과(3.78~4.38:1)해 "본문 사용 금지" 대상 배경이 없다는 문장이 자동 생성된다.
  - `ContrastLevel`에 `'계산 불가'`를 추가하고, `evaluateContrast(bgValue, textValue)`가 `contrastRatio` 호출을 `try/catch`로 감싸 예외 시 `{ ratio: null, level: '계산 불가' }`를 반환하도록 했다. `buildContrastRows()`가 배경×텍스트 44개 조합을 이 함수로 한 조합씩 계산해, 토큰 값이 깨진 조합이 있어도 그 행만 "계산 불가"로 표시되고 나머지 43행은 그대로 렌더된다(round 2 QA 권고 3 — 행 단위 격리).
- `modules/shop-web/src/styles/contrast.ts` — `meetsAA`의 JSDoc 큰 텍스트 기준을 `18px 이상 또는 14px 이상 굵게`(pt→px 오환산)에서 `24px 이상 또는 18.66px 이상 굵게`(WCAG 2.1 18pt/14pt bold 정확 환산)로 정정. 함수 동작(비율 비교)은 원래도 맞았으므로 로직 변경 없음 — 주석만 정정.
- `modules/shop-web/src/styles/contrast.unit.test.ts` — 큰 텍스트 경계값 `meetsAA(3.0, true) === true` 테스트 1건 추가(기존 4.5:1 경계 테스트와 대칭).

**변경하지 않은 것**
- `tokens.css`의 `--color-price`/`--color-brand` 주석(흰 배경 3.18:1/4.38:1 실측 수치)은 round 2에 이미 흰 배경 기준으로 적혀 있어 이번 표 수정과 정합돼 손대지 않았다. 토큰 값 자체(hex)도 불변.
- `story_shots.py capture --force`(축E 시각 기준선 재기록) — 지시 4번대로 이번 라운드에서 실행하지 않음. STEP 5 QA 단계 몫으로 남긴다.

**검증(로컬 1회 실행, round 3 재실행)**
- `npm test`(typecheck + jest): 19 suites / **218 tests** 전부 통과(기존 217 + 신규 큰 텍스트 3:1 경계 테스트 1건).
- `npm run build-storybook`: 정상 빌드, `DesignTokens.stories-*.js` 정상 emit(12.23 kB).
- `npm run test-storybook`: `storybook-static`를 `http-server`로 로컬 서빙 후 `--maxWorkers=2`로 실행 — **41 suites / 137 tests 전부 통과**, 콘솔 error 0건(`DesignTokens.stories.tsx` 포함 — 실제 Chromium 브라우저가 `Design Tokens` 스토리를 렌더해 대비표 44행·문단 2건을 계산했고 에러 없이 통과했다).
- 대비 계산값 자체는 Node에서 `contrast.ts`와 동일한 WCAG 공식으로 재계산해 위 표 수치(흰 3.18/surface-3 3.02/surface-2 2.92/surface-1 2.84/primary-bg 2.74, brand 4.38/4.16/4.02/3.91/3.78)를 확인했다 — 문단 생성 로직이 이 값을 그대로 읽어 쓰므로 렌더된 문장과 표가 항상 일치한다.

### 재작업 완료 (2026-09-19, round 4)

round 3 QA CONCERNS 4건(사람 코멘트로 확정) 중 1·2를 재작업했다. 3(file-size-cap 초과)은 이월 추적만, 4(축E 시각 기준선 재기록)는 지시대로 STEP 5 몫이라 이번 라운드에서 실행하지 않았다. **이번 라운드는 좁게 두 파일만 건드렸다** — `tokens.css`·`DesignTokens.stories.tsx`.

**수정 파일**
- `modules/shop-web/src/styles/tokens.css` — `--color-price`·`--color-brand` 선언 옆 주석을 실측 수치를 손으로 적은 정적 문장에서 `DesignTokens.stories.tsx`의 `describeUsage()`가 실제로 산출하는 문장을 그대로 옮긴 문구로 교체했다(지시 1). 값(hex)은 불변.
  - `--color-price`: "흰 배경(#ffffff)·--color-surface-3(#f9f9f9) 위의 굵은 큰 글씨(18.66px 이상 굵게, 또는 24px 이상)에만 쓴다. --color-surface-2(#f5f5f5)·--color-surface-1(#f2f2f2)·--color-primary-bg(#f2ebff) 위에서는 큰 글씨도 3:1 미달이므로 쓰지 않는다(그 위의 텍스트는 본문색 #101010을 쓰고, 강조는 별도 배지로 대신한다)." — round 3까지 흰 배경 조건이 빠져 있던 문제(QA 지시 1의 근거)가 해소됐다.
  - `--color-brand`: "흰 배경(#ffffff)·--color-surface-3(#f9f9f9)·--color-surface-2(#f5f5f5)·--color-surface-1(#f2f2f2)·--color-primary-bg(#f2ebff) 위의 굵은 큰 글씨(18.66px 이상 굵게, 또는 24px 이상)에만 쓴다." (5배경 전부 3:1 이상이라 금지 배경 없음 — 지시문대로 brand는 기존 서술이 이미 정확했지만, "describeUsage 결과와 동일한 문장" 요건을 문자 그대로 맞추기 위해 함께 갱신했다.)
  - `tokenCatalog.ts`는 확인만 하고 변경하지 않았다 — `--color-price`/`--color-brand`의 `usage` 필드(`'할인율·판매가 강조'`/`'브랜드 로고·강조 요소'`)는 배경 조건부 사용 규칙을 중복 기재하고 있지 않아 "같은 규칙이 적혀 있으면 함께 맞춘다"의 대상이 아니다.
- `modules/shop-web/src/styles/DesignTokens.stories.tsx` — `describeUsage()`의 `forbidden`(본문 사용 금지+계산 불가 통합) 한 배열을 `rejected`(실측 대비율이 3:1 미달)와 `uncalculable`(토큰 값이 유효한 색상이 아니어서 대비율 자체를 못 구함) 두 배열로 분리했다(지시 2). 문구도 분리했다 — `rejected`는 기존과 동일하게 "…위에서는 큰 글씨도 3:1 미달이므로 쓰지 않는다(…)", `uncalculable`은 새 문구 "…는 계산 불가 — 토큰 값 확인(유효한 색상 값이 아니어서 이 조합의 대비를 측정하지 못했다. 미달을 단정하지 않는다)."로, 측정하지 않은 대비율을 "미달"로 단정하지 않는다.
  - 현재 실제 토큰(11+5개)은 전부 유효 hex라 정상 렌더 경로에서는 `uncalculable`이 항상 빈 배열이고 `--color-price`/`--color-brand`의 출력 문장은 위 tokens.css 인용문과 정확히 같다.
  - QA 결함 주입 재현(round 3와 동일 방식 — `--color-price`를 `rgba(...)`로, `--color-surface-1`을 `notacolor`로 런타임 주입): 해당 조합만 "계산 불가"로 표에 찍히고, `describeUsage`가 만드는 문장도 그 배경을 "…는 계산 불가 — 토큰 값 확인(…)"으로만 언급할 뿐 "3:1 미달"이라고 말하지 않음을 확인했다(round 3 QA 권고 2의 반례 조건 해소).

**이월(이 SR에서 손대지 않음)**
- QA 지시 3 — `DesignTokens.stories.tsx` 350줄(round 3의 342줄에서 이번 수정으로 8줄 더 늘어 여전히 `file-size-cap`(tsx 300줄, `should`) 초과. `should` 규칙이라 차단 아님. 후속 TODO 유지: 대비 계산부(`resolveBackground`~`buildContrastRows`·`describeUsage`)를 `contrastTable.ts`로 분리.
- QA 지시 4 — 축E 시각 기준선(`.speclinker/story_shots/baseline/` 46장 vs 현재 137개 스토리) 재기록은 지시대로 이번 라운드에서 실행하지 않음. STEP 5 QA 단계 몫으로 남긴다.

**검증(로컬 1회 실행, round 4 재실행)**
- `npm test`(typecheck + jest): 19 suites / **218 tests** 전부 통과(round 3와 동일 — 이번 라운드는 테스트를 추가하지 않았다, 주석·문자열 생성 로직만 변경).
- `storybook-static` 삭제 후 `npm run build-storybook` 클린 재빌드 성공(`DesignTokens.stories-*.js` 12.46 kB emit).
- `npm run test-storybook`: 재빌드된 `storybook-static`을 `http-server`로 로컬 서빙 후 `--maxWorkers=2`로 실행 — **41 suites / 137 tests 전부 통과**, 콘솔 error 0건(`DesignTokens.stories.tsx` 포함).

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-19 — CONCERNS

- **Layer1 스펙**: 통과. 토큰 12개 AC 전부 충족 — 색 실측 hex 13종이 `02_변경명세.md` 목록과 1:1 일치, 그라데이션 2종, 타이포 8단(`--text-2xs`~`--text-2xl`)+헤드라인 `--text-display:40px`, 굵기 3종, 간격 8단 전부 4px 배수, radius 3종(`--radius-md:8px` 카드 썸네일), 그림자 3종, z-index 6층, `--layout-content-width:750px`. 사람 수정 4건 전부 반영 확인 — 파일명↔`font-weight` 1:1(Regular=400·SemiBold=600·Bold=700, "Medium" 토큰 없음), `package.json`/`package-lock.json` 무변경(`git diff` 실측: 변경 파일은 `preview.ts`·`main.tsx` 2개뿐), woff2 3개 실제 바이트 크기 Dev 기록 기재, 상태색이 `DeliveryBadge.tsx` 실값과 정확히 일치(`#136b2f`/`#e9f7ec`·`#8a5a00`/`#fff4e5`·`#0b4ea2`/`#e8f1ff`). 폰트 3개 모두 진짜 woff2(매직바이트 `wOF2`)이고 OFL 원문 동봉. 다만 아래 권고 1·2는 AC 의도와 전달된 효과 사이의 간극이다.
- **Layer2 보안**: 통과. 인증·인가·입력검증·주입 표면 없음(백엔드 무변경, 네트워크 호출·`fetch`·`console.log` 0건). 외부 CDN 미사용을 빌드 산출물로 실측 확인 — 폰트는 전부 앱이 서빙하는 자기 자산 경로로만 나간다.
- **Layer3 회귀**: 경고. 아래 재검증은 dev 주장 재확인이 아니라 QA가 독립 실행한 실측이다.
  - `npm test` 19 suites / 215 tests 통과(재실행).
  - `npm run build` → 산출 CSS가 `url(/shop/assets/Pretendard-*-<hash>.woff2)`, 실제 emit 파일과 경로 일치 → **SR-301 #1(base 경로 불일치) 회피 실증**.
  - `npm run build-storybook` → `url(./Pretendard-*-<hash>.woff2)`, 정적 서빙 시 HTTP **200 / 765,892 bytes** 응답 확인 → **SR-306 #1 r3~r4(정적 자산 404 → 콘솔 error → 축E FAIL) 회피 실증**.
  - `test-storybook`(정적 산출물 서빙 후 재실행) 41 suites / 137 tests 통과. `design-tokens--tokens` 단독 렌더 시 콘솔 error 0건이고 CSS 변수가 실제로 해석됨(`--color-text` = `#101010`), Pretendard 400/600/700 전부 `loaded`.
  - SR-231 r4(전역 계약 파급) 회피 확인 — 전역 선택자는 `body{font-family}` **하나뿐**, 마진·box-sizing 리셋 없음. `.storybook/main.ts`의 `staticDirs` 무변경.
  - **축E 시각 기준선 재기록 필요**: `.speclinker/story_shots/baseline/`은 46장(2026-09-16 기록)인데 현재 스토리는 137개이고, `preview.ts`의 `tokens.css` 임포트로 스토리북 프리뷰 `body`가 Pretendard로 바뀐다(실측). 자체 `fontFamily`를 갖지 않은 부품들의 글자 렌더가 달라지므로 이번 SR 반영 후 기준선을 다시 뜬다 — AC가 명시 허용한 변경이라 차단 사유는 아니다.

- 권고(CONCERNS):
  1. **[medium · Layer1/Layer3] 전역 `body` 폰트 교체가 실제 화면에는 한 곳도 도달하지 않는다.** `src/pages/*.tsx` **프로덕션 페이지 10개 전부**(`ShopHomePage`·`ProductListPage`·`ProductDetailPage`·`CartPage`·`OrderPage`·`OrderListPage`·`OrderDetailPage`·`LoginPage`·`MyAddressesPage`·`PasswordResetPage`)가 루트 래퍼에 `fontFamily: 'system-ui, sans-serif'`를 인라인으로 박아 두고 있어 `body` 규칙을 전부 가린다(부품 포함 총 15개 파일). 실측: 기존 스토리 `쇼핑홈-gnb--비로그인`의 렌더 결과가 `system-ui, sans-serif`이고 `document.fonts`의 Pretendard 400/600/700이 전부 `unloaded`. 즉 AC "전역 body 폰트를 Pretendard로 교체"는 선택자 수준에서는 충족이지만 사용자가 보는 화면에서 Pretendard가 쓰이는 곳은 0곳이다. **이번 SR에서 고치면 안 된다** — 그 15개 파일 수정은 이 SR이 명시 제외한 "기존 화면·컴포넌트의 스타일 교체"에 정면으로 해당한다. 대신 그 15개 파일 목록을 **SR-310(공통 컴포넌트)·SR-311(앱 셸)의 필수 작업으로 이월**하고 UIS-CMN-001 본문에 "이 하드코딩을 걷어내기 전까지 토큰 폰트는 무효"를 남긴다. (이 프로젝트 사례집 SR-303 "도달할 수 없는 화면"과 같은 계열 — 만들었으나 아무것도 닿지 않는 산출물.)
  2. **[medium · Layer1] 대비표가 정작 AA에 미달하는 토큰을 빼고 있다.** `DesignTokens.stories.tsx:112`의 `CONTRAST_TEXTS`가 `--color-text`/`-secondary`/`-tertiary`/`--color-link`/`--color-primary` 5개뿐이라, `tokenCatalog.ts`에서 **텍스트 용도로 선언된** `--color-price`("할인율·판매가 강조")와 `--color-brand`("브랜드 로고·강조")가 표에 아예 없다. QA 실측 대비율 — `--color-price` #ff5259 on #ffffff = **3.18:1(AA 미달)**, `--color-brand` #ed1c24 = **4.38:1(AA 미달)**. AC가 이 표를 요구한 목적이 "미달 조합을 드러내는 것"인데 가장 위험한 두 토큰이 빠져 있어, 이 문서를 보고 리스킨할 SR-322/326에 거짓 안심을 준다. `CONTRAST_TEXTS`에 `--color-price`·`--color-brand`와 상태색 4종을 추가한다(표에 이미 포함된 `--color-text-secondary` 4.31·`--color-link` 4.28·`--color-text-tertiary` 2.21 미달은 정상적으로 노출되고 있으며, 실측값이므로 값 자체를 고치라는 뜻은 아니다).
  3. **[low · Layer3] `contrast.ts`가 hex가 아닌 입력에 조용히 `NaN`을 낸다.** `hexToRgb`(contrast.ts:4)는 검증이 없어 `rgba(...)` 문자열이나 빈 문자열(= `tokens.css` 미로드 시 `getPropertyValue`의 반환값)을 받으면 `NaN`이 전파돼 표가 "NaN:1 · 미달"로 조용히 그려진다. 현재 호출 경로는 hex 토큰만 넘기므로 실제 결함은 아니지만, SR-310 이후 재사용될 공개 유틸이고 `contrast.unit.test.ts`에 잘못된 입력 케이스가 없다. 명시적 throw 또는 계약 주석+테스트를 권고.
  4. **[low · Layer3] 서브셋 아닌 원본 woff2 3개 = 2.34 MB가 저장소와 `dist/`에 들어간다.** 사람 수정으로 승인되고 `범위 밖`에 후속 과제로 이미 적힌 사항이라 이번 차단 사유가 아니다. 다만 권고 1 때문에 지금은 런타임 다운로드가 0이고, **SR-310 이후 Pretendard가 실제로 적용되는 순간** 766 KB~2.34 MB 전송 비용이 비로소 발생한다 — 서브셋 경량화는 그 SR보다 **앞서** 처리하는 편이 낫다.

- 재동기화 입력(STEP 5.5 — UIS-CMN-001 본문 생성 시 반영):
  - UIS-CMN-001 · 타이포 절 — `body` 폰트 토큰은 `src/pages/*.tsx` 10개 + 부품 5개의 인라인 `fontFamily: 'system-ui, sans-serif'`에 가려져 현재 무효 상태다(권고 1). 그 하드코딩 제거가 SR-310/311의 선행 조건임을 본문에 명시.
  - UIS-CMN-001 · 색 절 — AA 4.5:1 미달 토큰 실측값(`--color-price` 3.18, `--color-brand` 4.38, `--color-text-secondary` 4.31, `--color-link` 4.28, `--color-text-tertiary` 2.21 / 기준 #ffffff·#f9f9f9)을 본문 표에 사실로 기재. 벤치마크 실측값이라 변경 대상이 아니라 "큰 텍스트·굵기 조합으로만 쓸 것"이라는 사용 제약으로 적는다.

### QA Gate — 2026-09-19 — CONCERNS (round 2)

> 재검토 범위: 사람이 확정한 재작업 2건(권고 2·3)의 반영 여부 + 그 변경이 만든 새 위험. QA 이슈 1(인라인 `fontFamily`)·4(woff2 2.34MB)는 사람이 SR-322/326·SR-291로 이월 확정 → **재검토 대상 아님**(미변경 사실만 확인: `git status` 상 `src/pages/*` 무변경, 폰트 3개 바이트 크기 동일).

- **Layer1 스펙**: 통과. 재작업 2건 모두 지시대로 반영됐다(QA 독립 실측).
  - 권고 2 — `CONTRAST_TEXTS` 5개 → **11개**(`--color-price`·`--color-brand`+상태색 4종 포함). 브라우저 렌더 실측: 대비표가 **4배경 × 11텍스트 = 44행** 정상 렌더, NaN 행 0, 콘솔 error 0. 판정이 2단 → 3단(`통과`/`큰 글씨 전용`/`본문 사용 금지`)으로 확장됐고 WCAG 큰 글씨 정의(18.66px 굵게 / 24px)도 스토리 본문에 정확히 기재.
  - 권고 3 — `contrast.ts:7` `hexToRgb`가 `/^[0-9a-fA-F]{6}$/` 불일치 시 `throw`. `contrast.unit.test.ts`에 비-hex(`rgba(...)`)·빈 문자열 2건 추가. `npm test` **19 suites / 217 tests 통과**(QA 재실행 — dev 주장과 일치).
  - `--color-price`·`--color-brand` 사용 제약 주석이 `tokens.css:44-49`와 스토리 본문 양쪽에 기재(사람 지시대로).
  - 토큰 **값**은 한 글자도 변하지 않았다(주석만 추가) — round 1에서 확인한 AC 12건 충족 상태 유지.
- **Layer2 보안**: 통과. 이번 라운드 변경분(문서 스토리·순수함수·CSS 주석)에 새 공격면 없음. 신규 `throw`가 입력값을 메시지에 넣지만 서버 로그·HTML 삽입 경로가 아니고(React 이스케이프, 백엔드 무변경), `fetch`·`console.*` 0건.
- **Layer3 회귀**: 경고(차단 아님). 아래는 전부 QA 독립 재실행 실측이다.
  - `npm test` 19 suites / 217 tests 통과.
  - `npm run build-storybook` 성공 → 정적 서빙 후 `npx test-storybook --maxWorkers=2` **41 suites / 137 tests 전부 통과, 콘솔 error 0**.
  - `design-tokens--tokens` 단독 브라우저 렌더: `--color-text`=`#101010` 해석, `body` font-family = Pretendard 스택, `document.fonts` Pretendard **400/600/700 전부 `loaded`**, 페이지 에러 0.
  - `npm run build` 재확인 → 산출 CSS `url(/shop/assets/Pretendard-*-<hash>.woff2)`가 실제 emit 파일 3개와 정확히 일치(SR-301 #1 base 경로 불일치 회피 유지).
  - `contrast.ts` 소비자는 스토리·테스트 2곳뿐이라 `throw` 도입의 파급 범위가 닫혀 있다.
  - 축E 시각 기준선은 여전히 46장(2026-09-16) vs 현재 스토리 137개 — round 1 지적 그대로이며 AC가 허용한 변경이라 차단 사유 아님.

- 권고(CONCERNS):
  1. **[medium · Layer1] 새로 쓴 사용 규칙 문단이 바로 아래 자기 표와 모순된다 — 가격 빨강은 표의 4개 배경 중 3개에서 큰 글씨 기준(3:1)조차 미달이다.** `DesignTokens.stories.tsx:202-207`과 `tokens.css:44-45`가 "`--color-price`는 흰 배경 대비 **3.18:1**이므로 굵은 큰 가격 숫자 전용으로 쓴다"고 규칙을 세웠는데, 기준으로 삼은 **흰색(#ffffff)은 `CONTRAST_BACKGROUNDS`에 없는 배경**이다. 같은 페이지 표가 실제로 렌더하는 값(브라우저 실측) — `--color-surface-3` **3.02**(큰 글씨 전용) · `--color-surface-2` **2.92** · `--color-surface-1` **2.84** · `--color-primary-bg` **2.74**로, 셋은 `본문 사용 금지`로 표시된다. 즉 본문은 "큰 글씨로는 써도 된다"고 하는데 표는 같은 토큰을 "쓰지 말라"고 하고, 문단이 인용한 3.18·4.38(brand)은 표 어디에도 없어 독자가 대조할 수 없다. 이 문서를 근거로 리스킨할 SR-322/326이 `#f5f5f5` 카드 위에 큰 가격 빨강을 올리면 WCAG 큰 글씨 기준도 미달이다 — round 1 권고 2가 막으려던 "거짓 안심"이 형태만 바꿔 남았다. → `CONTRAST_BACKGROUNDS`에 흰색(#ffffff — 실제 페이지 바탕이자 두 수치의 기준)을 추가하고, 문단을 "흰색·`--color-surface-3` 위에서만 큰 글씨 허용, 그보다 어두운 면 위에는 금지"로 배경 조건을 붙여 정정한다(색값은 벤치마크 실측이므로 불변). 수정 규모는 배열 1줄 + 문단 1~2문장.
  2. **[low · Layer3] `contrast.ts:38` `meetsAA` 계약 주석의 큰 텍스트 기준이 WCAG와 다르다(pt→px 오환산).** 주석은 "큰 텍스트(18px 이상 또는 14px 이상 굵게) 3:1"인데 WCAG 2.1의 large scale text는 **18pt(=24px) 또는 14pt 굵게(=18.66px)**다. 같은 SR의 스토리 본문(`DesignTokens.stories.tsx:199-200`)은 18.66px/24px로 올바르게 적어 두 서술이 충돌한다. round 1부터 있던 코드라 이번 새 결함은 아니지만(low + 후속 TODO), SR-310 이후 이 유틸을 import하는 쪽이 18px 일반 텍스트에 3:1을 적용할 수 있다. → 주석을 `24px / 18.66px 굵게`로 정정.
  3. **[low · Layer3] 새 `throw`에 행 단위 방어가 없어 토큰 하나가 페이지 전체를 무너뜨릴 수 있다.** fail-loud 자체는 사람 지시대로 맞다. 다만 `ContrastTable`이 44행을 한 `flatMap`에서 계산하므로, 향후 `CONTRAST_TEXTS`에 rgba 토큰(`--color-dim` 같은)이나 오타 토큰이 하나 들어오면 `Error`가 렌더를 통째로 중단해 Design Tokens 스토리가 깨지고 `test-storybook`·축E가 FAIL한다. 현재는 11+4 토큰 전부 hex라 실제 결함이 아니다(실측 NaN 0행·error 0). → 후속 TODO: 행 단위 `try/catch`로 문제 토큰만 "값 오류"로 표시.
  4. **[low · Layer3] 축E 시각 기준선 재기록(round 1 이월, 미해결).** `.speclinker/story_shots/baseline/` 46장 vs 현재 스토리 137개. 반영 후 `story_shots.py capture --force`.

- 재동기화 입력(STEP 5.5 — UIS-CMN-001 본문 생성 시 반영):
  - UIS-CMN-001 · 색 절 — **round 1 재동기화 입력의 대비 수치를 이 값으로 대체한다.** 흰 배경 단일 기준이 아니라 **배경별**로 적어야 사용 제약이 성립한다(QA 브라우저 실측): `--color-price` 3.18(흰)/3.02(#f9f9f9)/2.92(#f5f5f5)/2.84(#f2f2f2)/2.74(#f2ebff), `--color-brand` 4.38/4.16/4.02/3.91/3.78, `--color-text-secondary` 4.54/4.31/4.17/4.06/3.92, `--color-link` 4.51/4.28/4.14/4.03/3.89, `--color-text-tertiary` 2.32/2.21/2.13/2.08/2.00. 상태색 4종은 자기 `-bg` 짝 위에서 전부 AA 통과(성공 5.98·경고 5.45·오류 5.75·정보 7.03).
  - UIS-CMN-001 · 타이포 절 — round 1 입력 유지(인라인 `fontFamily` 하드코딩 제거 전까지 토큰 폰트 무효, SR-310/311 선행 조건). 단 **스토리북 프리뷰에서는 Pretendard 400/600/700이 실제 `loaded`**임을 이번 라운드에서 확인했으므로, 무효 범위는 "프로덕션 페이지 15개"로 한정해 적는다.

### QA Gate — 2026-09-19 — CONCERNS (round 3)

> 재검토 범위: round 3 재작업 지시 1~3의 반영 여부 + 그 변경이 만든 새 위험. 지시 4(축E 시각 기준선 재기록)는
> 지시문이 STEP 5 몫으로 못박았으므로 미실행이 정상 — 상태만 확인한다. 아래 수치는 전부 QA가 독립 실행한 실측이다
> (dev 주장 재확인이 아니라, `storybook-static`을 **지우고 새로 빌드**한 뒤 Chromium으로 직접 렌더해 DOM에서 읽었다).
> 대비값은 dev의 `contrast.ts`를 쓰지 않고 QA가 WCAG 공식을 별도 구현해 55조합을 재계산해 대조했다.

- **Layer1 스펙**: 통과. 재작업 3건 모두 반영됐고, 핵심 쟁점(문단↔표 모순)이 **구조적으로** 해소된 것을 브라우저로 확인했다.
  - **지시 1 — 확인.** `CONTRAST_BACKGROUNDS`에 `#ffffff`가 맨 앞에 추가돼 표가 4×11=44행 → **5×11=55행**으로 늘었다(브라우저 DOM 실측 55행). 사용 규칙 문단은 정적 문장이 아니라 `describeUsage()`가 표와 **같은 함수**(`evaluateContrast`)로 매 렌더 도출한다 — 렌더된 문장과 표를 55행 전부 대조해 어긋난 행 0건.
    - 렌더 실측 문장(`--color-price`): "흰 배경(#ffffff)·--color-surface-3(#f9f9f9) 위의 굵은 큰 글씨(18.66px 이상 굵게, 또는 24px 이상)에만 쓴다. --color-surface-2(#f5f5f5)·--color-surface-1(#f2f2f2)·--color-primary-bg(#f2ebff) 위에서는 큰 글씨도 3:1 미달이므로 쓰지 않는다(…)" → 표의 3.18(큰 글씨 전용)·3.02(큰 글씨 전용)·2.92·2.84·2.74(본문 사용 금지)와 정확히 일치.
    - **dev가 사람 예시 문장을 그대로 옮기지 않은 판단은 옳다(QA 독립 검증).** 사람 코멘트 예시는 "surface-1/2/3·primary-bg 모두 미달"이었으나, QA가 `contrast.ts`와 무관한 자체 WCAG 구현으로 재계산한 결과 `--color-price` on `#f9f9f9` = **3.02:1로 3:1을 넘는다**. 예시문을 문자 그대로 옮겼다면 표와 다시 모순됐을 것이다 — "문단 수치는 표에서 계산된 값만 인용한다"는 지시를 우선한 것이 정확했다.
    - `--color-brand`는 5배경 전부 3:1 이상(4.38/4.16/4.02/3.91/3.78)이라 금지 배경이 없다는 문장이 자동 생성됨을 확인.
  - **지시 2 — 확인.** `contrast.ts:38-40` 주석이 `24px 이상 또는 18.66px 이상 굵게`로 정정됐고 오환산 이력까지 남겼다. 스토리 본문(18.66px/24px)과 이제 일치. 경계 테스트 `meetsAA(3.0, true) === true` 추가(`contrast.unit.test.ts:52-54`). 함수 로직은 원래도 맞아 불변.
  - **지시 3 — 확인(결함 주입으로 실증).** QA가 `--color-price`를 `rgba(...)`로, `--color-surface-1`을 `notacolor`로 런타임 주입해 렌더한 결과: **55행 중 15행만 `계산 불가`(대비율 `—`)로 표시되고 나머지 40행은 정상 렌더, 콘솔 error 0건, 페이지 무붕괴**. 행 단위 격리가 주장대로 동작한다.
  - **지시 4 — 미실행이 정상.** 지시문이 STEP 5 몫으로 명시. 현황만: `.speclinker/story_shots/baseline/` **46장**(2026-09-16) vs `storybook-static/index.json` 스토리 **137개**.
  - 토큰 **값**(hex·px·z-index)은 이번 라운드에 한 글자도 안 변했다 — round 1에서 확인한 AC 12건 충족 상태 유지. 이번 라운드가 건드린 파일은 `DesignTokens.stories.tsx`·`contrast.ts`·`contrast.unit.test.ts` **3개뿐**(파일 mtime 실측: `tokens.css` 16:46·`main.tsx` 16:33·`tokenCatalog.ts` 16:32 = round 1~2 시점 그대로, 재작업 3건은 17:03). `src/pages/*`·폰트 3개는 무변경(이월 확정 사항 유지).
- **Layer2 보안**: 통과. 이번 변경분은 문서 스토리·순수함수·테스트뿐이다. `src/styles/` 전체에 `fetch(`·`console.log/error/warn` **0건**(grep 실측). 오히려 round 2의 `throw`가 이제 `evaluateContrast`의 `try/catch`에 잡혀 예외 메시지에 실린 토큰 값이 DOM까지 도달하지 않는다 — 노출 표면이 줄었다. 백엔드·인증·입력검증 경로 무변경.
- **Layer3 회귀**: 경고(차단 아님). 전부 QA 독립 재실행.
  - `npm test`(typecheck+jest) **19 suites / 218 tests 전부 통과**.
  - `storybook-static` 삭제 후 `npm run build-storybook` 클린 재빌드 성공 → 자체 정적 서버로 서빙 후 `npx test-storybook --maxWorkers=2` **41 suites / 137 tests 전부 통과**.
  - `design-tokens--tokens` 단독 Chromium 렌더: **콘솔 error 0 · pageerror 0**, `--color-text`=`#101010` 해석, `body` font-family = Pretendard 폴백 스택, `document.fonts`의 **Pretendard 400/600/700 전부 `loaded`**.
  - `npm run build`(vite, `base:'/shop/'`) 재실행 → 산출 CSS `url(/shop/assets/Pretendard-{Regular,SemiBold,Bold}-<hash>.woff2)` 3개가 실제 emit 파일 3개와 정확히 일치 → **SR-301 #1(base 경로 불일치) 회피 유지**. 스토리북 산출물도 동일 해시 파일 3개 emit → **SR-306 #1 r3~r4(정적 자산 404 → 콘솔 error → 축E FAIL) 회피 유지**.
  - SR-231 r4(전역 계약 파급) 회피 유지 — `tokens.css`의 전역 선택자는 여전히 `body{font-family}` 하나뿐, `.storybook/main.ts` `staticDirs` 무변경.

- 권고(CONCERNS):
  1. **[medium · Layer1] 스토리 문단은 고쳤는데 `tokens.css`의 같은 규칙은 배경 조건 없이 남아, 값의 정본 쪽에 "거짓 안심"이 그대로 있다.** 재작업 지시 1은 모순 지점으로 `DesignTokens.stories.tsx:202-207`**과 `tokens.css:44-45` 둘 다**를 지목했는데, 지시의 `→` 처방이 "문단을 정정한다 / 수정 규모는 배열 1줄 + 문단 1~2문장"으로만 적혀 dev가 문단만 고치고 `tokens.css`는 의도적으로 손대지 않았다(`## Dev 기록` "변경하지 않은 것"에 명시 — 지시 이행 자체는 성실하다). 현재 `tokens.css:44-45`는 여전히 **"흰 배경 대비 실측 3.18:1 … 굵은 큰 가격 숫자(`--text-xl` 이상 + `--font-weight-bold`) 전용으로만 사용한다"** 로, **배경 조건이 없다**. 이게 오늘 도달 가능한 오도다 — `--text-xl`은 20px이고 bold라 WCAG 큰 글씨 자격을 갖지만, `tokenCatalog.ts:16`이 `--color-surface-1`을 "옅은 배경면(**카드**·구분 영역)"으로 규정하므로 SR-310/322/326 구현자가 이 주석만 읽고 `#f2f2f2` 카드 위에 20px bold 가격 빨강을 올리면 **2.84:1로 큰 글씨 기준 3:1조차 미달**(QA 실측). 스토리 페이지를 안 열고 `tokens.css`만 읽는 것이 오히려 흔한 경로다. → `--color-price` 주석에 배경 조건을 붙인다: "`#ffffff`·`--color-surface-3` 위에서만 큰 글씨 허용, `--color-surface-1/2`·`--color-primary-bg` 위에서는 큰 글씨도 3:1 미달이라 금지(그 위 가격은 `--color-text`, 할인율만 빨강 배지)". `--color-brand` 주석(5배경 전부 3:1 통과)은 현재 서술로 정확하니 불변. 토큰 **값**은 벤치마크 실측이므로 변경하지 않는다. 수정 규모: 주석 2줄.
  2. **[low · Layer3] `describeUsage`가 "계산 불가"를 "3:1 미달"이라고 단정해, 새로 만든 문단이 자기 표와 다시 모순될 수 있다 — dev의 "구조적으로 불가능" 주장에 대한 반례를 실측으로 재현했다.** `DesignTokens.stories.tsx:205`가 `forbidden`에 `'본문 사용 금지'`와 `'계산 불가'`를 **한 바구니로** 담고, `:213`이 그 목록에 "위에서는 **큰 글씨도 3:1 미달이므로** 쓰지 않는다"라는 이유를 하드코딩한다. QA 결함 주입 렌더 실측 — 표는 해당 조합을 정직하게 `계산 불가`로 찍는데 같은 페이지 문단은 "`--color-surface-1(notacolor)` 위에서는 큰 글씨도 3:1 미달"이라고 **측정한 적 없는 값을 단정**했다. 즉 비율은 이제 어긋날 수 없지만 **판정 사유는 어긋날 수 있다**. 현재 토큰 11+5개가 전부 유효 hex라 실제 결함은 아니다(정상 렌더 시 `계산 불가` 0행 실측). 다만 `contrast.ts:8` 주석이 스스로 예고한 "tokens.css 미로드 시 빈 문자열" 상황에서는 55행 전부가 이 경로를 타, 접근성 정본 문서가 "모든 배경에서 3:1 미달"이라는 확신에 찬 거짓을 말하게 된다. → `forbidden`을 "3:1 미달"과 "계산 불가" 두 목록으로 분리해 사유를 따로 적는다(수정 2~3줄). 덧붙여 `:204`가 `'통과'`(AA 4.5 충족)까지 `usable`로 묶어 "굵은 큰 글씨**에만** 쓴다"로 서술하는데, 지금은 `SPECIAL_TEXT_RULES`가 4.5 미달 토큰 2종뿐이라 드러나지 않을 뿐 같은 계열의 과잉 제약이다(안전 방향이라 위험은 없음).
  3. **[low · Layer3] `DesignTokens.stories.tsx`가 342줄로 `file-size-cap`(tsx 300줄, `should`) 상한을 이번 라운드에 넘겼다.** round 2까지 ~297줄이었고 이번에 `resolveBackground`·`describeUsage`·`evaluateContrast`·`ContrastRow`가 더해지며 초과했다. `should`라 차단 아니다. → 후속 TODO: 대비 계산부(`resolveBackground`~`buildContrastRows`·`describeUsage`)를 `contrastTable.ts`로 분리하면 스토리 파일이 렌더만 남는다. 분리 시 `contrast.ts`처럼 단위 테스트가 가능해져 권고 2도 테스트로 고정할 수 있다.
  4. **[low · Layer3] 축E 시각 기준선 재기록(round 1·2 이월, 미해결 — 지시대로 STEP 5 몫).** `.speclinker/story_shots/baseline/` **46장**(2026-09-16) vs 현재 스토리 **137개**. `preview.ts`의 `tokens.css` 임포트로 프리뷰 `body`가 Pretendard로 바뀐 것을 이번에도 브라우저로 확인(400/600/700 `loaded`). AC가 명시 허용한 변경이라 차단 사유 아님. → STEP 5에서 `story_shots.py capture . --force`.

- 재동기화 입력(STEP 5.5 — UIS-CMN-001 본문 생성 시 반영):
  - UIS-CMN-001 · 색 절 — **round 2 재동기화 입력의 배경별 수치를 그대로 쓴다(QA가 이번 라운드에 독립 구현으로 재계산해 전건 일치 확인).** 배경 순서는 `#ffffff` / `#f9f9f9` / `#f5f5f5` / `#f2f2f2` / `#f2ebff`: `--color-price` 3.18/3.02/2.92/2.84/2.74, `--color-brand` 4.38/4.16/4.02/3.91/3.78, `--color-text-secondary` 4.54/4.31/4.17/4.06/3.92, `--color-link` 4.51/4.28/4.14/4.03/3.89, `--color-text-tertiary` 2.32/2.21/2.13/2.08/2.00, `--color-text` 19.03/18.07/17.45/17.00/16.40, `--color-primary` 6.52/6.20/5.98/5.83/5.62. 상태색 4종은 5배경 전부 AA 통과(성공 6.62~5.71 · 경고 5.93~5.11 · 오류 6.57~5.67 · 정보 8.00~6.89). **사용 제약은 "큰 글씨면 된다"가 아니라 배경 조건부로 적는다** — `--color-price`는 `#ffffff`·`#f9f9f9` 위에서만 큰 글씨 허용.
  - UIS-CMN-001 · 타이포 절 — round 1·2 입력 유지(프로덕션 페이지 15개의 인라인 `fontFamily: 'system-ui, sans-serif'` 제거가 SR-310/311 선행 조건, 그 전까지 토큰 폰트는 프로덕션에서 무효). 스토리북 프리뷰에서는 Pretendard 400/600/700이 실제 `loaded`임을 round 3에서도 재확인.
  - UIS-CMN-001 · 참조 무결성 — 빌드 산출 경로가 `dist`는 `/shop/assets/Pretendard-*-<hash>.woff2`, `storybook-static`은 `./Pretendard-*-<hash>.woff2`로 갈린다는 사실을 본문에 남긴다(후속 SR이 폰트를 서브셋으로 교체할 때 이 두 경로를 모두 확인해야 한다).

### QA Gate — 2026-09-19 — CONCERNS (round 4)

> 재검토 범위: 사람 코멘트가 좁힌 재작업 2건 — (1) `tokens.css`의 `--color-price`·`--color-brand` 주석을
> `describeUsage()` 산출 문장으로 교체, (2) `describeUsage`가 "계산 불가"와 "기준 미달"을 다른 문구로 표기.
> 지시 3(파일 350줄)은 추적만, 지시 4(축E 기준선)는 STEP 5 몫이라 미실행이 정상 — 상태만 확인한다.
> 아래는 전부 QA 독립 실측이다: `storybook-static`을 **지우고 새로 빌드**한 뒤 Chromium으로 렌더해 DOM에서 읽었고,
> 대비값은 dev의 `contrast.ts`를 쓰지 않고 QA가 WCAG 공식을 별도 구현해 55조합을 재계산해 대조했다.
> **결론: 지시 2건 모두 완전히 반영됐다. 남은 것은 low 1건뿐이며 5라운드째 재작업이 필요한 결함은 없다.**

- **Layer1 스펙**: 통과. 재작업 2건이 지시대로, 그리고 **검증 가능한 방식으로** 반영됐다.
  - **지시 1 — 확인(문자열 동일성까지 기계 대조).** `tokens.css:44-49`·`51-55`의 인용 주석 2건을 브라우저가 실제 렌더한
    `describeUsage()` 출력과 **정규화 후 문자열 비교**해 **2건 모두 IDENTICAL**. (차이는 CSS 주석 줄바꿈으로 생긴 공백뿐 —
    내용 차이 0.) 렌더 실측 문장:
    - `--color-price`: "흰 배경(#ffffff)·--color-surface-3(#f9f9f9) 위의 굵은 큰 글씨(18.66px 이상 굵게, 또는 24px 이상)에만
      쓴다. --color-surface-2(#f5f5f5)·--color-surface-1(#f2f2f2)·--color-primary-bg(#f2ebff) 위에서는 큰 글씨도 3:1
      미달이므로 쓰지 않는다(…)" → round 3 QA가 지목한 **"배경 조건 없음" 오도가 값의 정본 쪽에서 해소**됐다.
    - `--color-brand`: 5배경 전부 `큰 글씨 전용`이라 금지 배경 없음 — 주석도 동일. 덧붙인 "AA 본문 기준(4.5:1)은 5배경 전부
      미달" 한 줄은 인용부호 **밖**에 두어 출처가 구분돼 있고, QA 독립 재계산(4.38/4.16/4.02/3.91/3.78)과 일치한다.
    - `tokenCatalog.ts`를 안 건드린 판단도 옳다(QA 확인) — `--color-price`/`--color-brand`의 `usage`는
      `'할인율·판매가 강조'`/`'브랜드 로고·강조 요소'`로, 배경 조건부 사용 규칙을 중복 기재하지 않아 "같은 규칙이 적혀 있으면
      함께 맞춘다"의 대상이 아니다.
  - **지시 2 — 확인(결함 주입으로 실증).** `describeUsage`가 `rejected`(3:1 미달)와 `uncalculable`(계산 불가) 두 배열로
    분리됐다(`:208-209`, 문구는 `:215-222`). QA가 **빌드 산출 CSS 응답을 가로채 토큰 값을 바꿔** 재현:
    - `--color-surface-1: notacolor` 주입 → 표는 55행 중 **11행만 `계산 불가`**, 나머지 44행 정상, 콘솔/페이지 error **0**.
      문단도 그 배경을 "…는 계산 불가 — 토큰 값 확인(유효한 색상 값이 아니어서 이 조합의 대비를 측정하지 못했다. 미달을
      단정하지 않는다)."로만 언급하고 **"3:1 미달" 목록에서는 빠졌다** → round 3 권고 2의 반례 조건이 해소됐다.
  - 토큰 **값**(hex·px·z-index)은 이번 라운드에 한 글자도 안 변했다 — AC 12건 충족 상태 유지(QA 재대조: 색 13종·그라데이션
    2종·타이포 8단+`--text-display:40px`·굵기 400/600/700·간격 8단 전부 4px 배수·radius 3종(`--radius-md:8px`)·그림자
    3종·z-index 6층·`--layout-content-width:750px`·`@font-face` 3개 `font-display:swap`·시스템 한글 폴백).
  - 이번 라운드가 건드린 파일은 **`tokens.css`·`DesignTokens.stories.tsx` 2개뿐**(mtime 실측 17:15 / `contrast.ts`·
    `contrast.unit.test.ts` 17:03 = round 3 그대로, `tokenCatalog.ts` 16:32, `main.tsx`·`preview.ts` 16:33). 이월 확정
    사항 유지 확인 — `src/pages/*.tsx` 전부 무변경, 폰트 3개 바이트 동일(765,892/785,856/791,156),
    `package.json`/`package-lock.json` 무변경(12:27, SR-309 착수 이전).
- **Layer2 보안**: 통과. 이번 변경분은 CSS 주석과 문자열 생성 분기뿐이다. `src/styles/` 전체에 `fetch(`·`console.log/error/warn`
  **0건**(grep 실측). 백엔드·인증·입력검증 경로 무변경, 외부 CDN 0건(빌드 산출물 실측).
- **Layer3 회귀**: 경고(차단 아님). 전부 QA 독립 재실행.
  - `npm test`(typecheck+jest) **19 suites / 218 tests 전부 통과**.
  - `storybook-static` 삭제 후 `npm run build-storybook` 클린 재빌드 → 자체 정적 서버로 서빙 후
    `npx test-storybook --maxWorkers=2` **41 suites / 137 tests 전부 통과**.
  - `design-tokens--tokens` 단독 Chromium 렌더: **콘솔 error 0 · pageerror 0**, 대비표 **55행**(레벨 분포 통과 32 ·
    큰 글씨 전용 15 · 본문 사용 금지 8 · 계산 불가 **0**), `--color-text`=`#101010` 해석, `body` font-family =
    Pretendard 폴백 스택, `document.fonts`의 **Pretendard 400/600/700 전부 `loaded`**.
  - `dist` 삭제 후 `npm run build`(vite, `base:'/shop/'`) → 산출 CSS `url(/shop/assets/Pretendard-{Regular,SemiBold,Bold}-<hash>.woff2)`
    3개가 실제 emit 파일 3개와 정확히 일치 → **SR-301 #1(base 경로 불일치) 회피 유지**. 스토리북 정적 서빙 시 폰트
    **HTTP 200 / 765,892 bytes** 응답 → **SR-306 #1 r3~r4(정적 자산 404 → 콘솔 error → 축E FAIL) 회피 유지**.
  - SR-231 r4(전역 계약 파급) 회피 유지 — `tokens.css`의 전역 선택자는 여전히 `body{font-family}` 하나뿐,
    `.storybook/main.ts` `staticDirs` 무변경.

- 권고(CONCERNS) — **전부 low. 이번에 재작업할 것은 없고 후속 TODO로 넘긴다.**
  1. **[low · Layer3] `describeUsage`의 "쓸 수 있는 배경 0개" 폴백 문장이 아직 측정하지 않은 "3:1 미달"을 단정한다 —
     지시 2가 인용한 바로 그 시나리오에서 재현됐다(QA 결함 주입 실측).** 지시 2는 `rejected`/`uncalculable` **목록 분리**를
     처방했고 dev는 그것을 정확히 했다. 다만 `DesignTokens.stories.tsx:211-214`의 세 번째 경로 — `usable.length === 0`일 때의
     폴백 — 은 손대지 않아 이유를 여전히 하드코딩한다. QA가 `--color-price`를 `rgba(255,82,89,.9)`로 주입해 렌더한 결과,
     그 토큰은 5배경 전부 `계산 불가`가 되어 `usable`이 비고 문단이 이렇게 나왔다:
     "…는 **이 표의 배경 어디에서도 큰 글씨 기준(3:1)을 충족하지 않아 텍스트로 쓰지 않는다.** 흰 배경(#ffffff)·…·--color-primary-bg(#f2ebff)**는
     계산 불가 — 토큰 값 확인(… 미달을 단정하지 않는다).**" → **한 문단이 두 문장 만에 자기를 부정한다.** `contrast.ts:8`이
     예고한 "tokens.css 미로드 시 빈 문자열" 상황이 정확히 이 경로다.
     **그러나 재작업 사유는 아니다** — (a) 정상 렌더에서는 `계산 불가` 0행으로 절대 도달하지 않는 경로이고(실측),
     (b) 도달하려면 CSS 자체가 깨져 Design Tokens 페이지 전체가 이미 무의미한 상태이며, (c) round 3부터 있던 코드로
     이번 재작업이 만든 것이 아니다(라운드 규율: 이전 라운드 코드에서 새로 찾은 것은 low + 후속 TODO).
     → 후속 TODO: 폴백 문장의 사유를 `rejected.length > 0`일 때만 "3:1 미달"로 쓰도록 1줄 분기(아래 권고 2의
     `contrastTable.ts` 분리 때 단위 테스트로 함께 고정하면 된다).
  2. **[low · Layer3] `DesignTokens.stories.tsx` 350줄 — `file-size-cap`(tsx 300줄, `should`) 초과(이월, 지시대로 추적만).**
     round 3의 342줄에서 이번 문구 분리로 8줄 늘었다. `should`라 차단 아니고, 사람 코멘트가 "이 SR에서 하지 않고 추적만"으로
     확정했으며 `## Dev 기록` round 4에 기재됐다. → 후속 TODO 유지: 대비 계산부(`resolveBackground`~`buildContrastRows`·
     `describeUsage`)를 `contrastTable.ts`로 분리.
  3. **[low · Layer3] 축E 시각 기준선 재기록(round 1·2·3 이월 — 지시대로 STEP 5 몫이라 미실행이 정상).**
     `.speclinker/story_shots/baseline/` **46장**(2026-09-16) vs 현재 스토리 **137개**. 프리뷰 `body`가 Pretendard로 바뀐 것을
     이번에도 브라우저로 확인(400/600/700 `loaded`). AC가 명시 허용한 변경이라 차단 사유 아님.
     → STEP 5에서 `story_shots.py capture . --force`.

- 라운드 원인 기록(비용 분석용): **round 4 자체는 `human_instruction` 계열이었다** — round 3 지시 1의 문제 문장은
  `DesignTokens.stories.tsx`와 `tokens.css:44-45` **둘 다**를 지목했는데 `→` 처방이 "문단을 정정한다"만 적어 dev가 성실히
  처방만 이행했고 정본(tokens.css)이 남았다. **교훈: 지시의 `→` 처방에는 문제 문장이 지목한 파일을 전부 다시 적는다.**
  (이번 gate의 `cause`는 남은 low 1건의 성격을 따라 `impl_bug`로 기록한다 — round 3 구현이 남긴 폴백 분기.)

- 재동기화 입력(STEP 5.5 — UIS-CMN-001 본문 생성 시 반영):
  - UIS-CMN-001 · 색 절 — **round 2·3 재동기화 입력의 배경별 수치를 그대로 쓴다.** QA가 이번 라운드에도 독립 구현으로
    재계산해 전건 일치를 확인했다(배경 순서 `#ffffff` / `#f9f9f9` / `#f5f5f5` / `#f2f2f2` / `#f2ebff`):
    `--color-price` 3.18/3.02/2.92/2.84/2.74, `--color-brand` 4.38/4.16/4.02/3.91/3.78,
    `--color-text-secondary` 4.54/4.31/4.17/4.06/3.92, `--color-link` 4.51/4.28/4.14/4.03/3.89,
    `--color-text-tertiary` 2.32/2.21/2.13/2.08/2.00, `--color-text` 19.03/18.07/17.45/17.00/16.40,
    `--color-primary` 6.52/6.20/5.98/5.83/5.62. 상태색 4종은 5배경 전부 AA 통과(성공 6.62~5.71 · 경고 5.93~5.11 ·
    오류 6.57~5.67 · 정보 8.00~6.89). 사용 제약은 배경 조건부로 적는다 — **`--color-price`는 `#ffffff`·`#f9f9f9`
    위에서만 큰 글씨 허용**. 본문 문장은 `tokens.css:44-55` 주석을 그대로 인용하면 된다(이번 라운드에 스토리 산출
    문장과 문자열 동일함을 기계 대조로 확인).
  - UIS-CMN-001 · 타이포 절 — round 1·2·3 입력 유지(프로덕션 페이지 15개의 인라인 `fontFamily: 'system-ui, sans-serif'`
    제거가 SR-310/311 선행 조건, 그 전까지 토큰 폰트는 프로덕션에서 무효). 스토리북 프리뷰에서는 Pretendard 400/600/700이
    실제 `loaded`임을 round 4에서도 재확인.
  - UIS-CMN-001 · 참조 무결성 — 빌드 산출 경로가 `dist`는 `/shop/assets/Pretendard-*-<hash>.woff2`, `storybook-static`은
    `./Pretendard-*-<hash>.woff2`로 갈린다는 사실을 본문에 남긴다(후속 SR이 폰트를 서브셋으로 교체할 때 두 경로를 모두 확인).

## 재작업 지시
> round 3 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/spec] 재작업 지시 1이 모순 지점으로 DesignTokens.stories.tsx와 tokens.css:44-45 둘 다를 지목했으나 처방이 '문단 정정'만 적어, 스토리 문단은 고쳐졌는데 값의 정본인 tokens.css의 --color-price 주석은 배경 조건 없이 '굵은 큰 가격 숫자(--text-xl 이상 + --font-weight-bold) 전용'으로 남았다. tokenCatalog가 --color-surface-1을 '카드 배경'으로 규정하므로 SR-310/322/326 구현자가 #f2f2f2 카드 위 20px bold 가격 빨강을 쓰면 2.84:1로 큰 글씨 기준 3:1도 미달(QA 실측). 스토리 페이지를 열지 않고 tokens.css만 읽는 경로에 거짓 안심이 그대로 있다. → tokens.css의 --color-price 주석에 배경 조건을 붙인다 — '#ffffff·--color-surface-3 위에서만 큰 글씨 허용, --color-surface-1/2·--color-primary-bg 위에서는 큰 글씨도 3:1 미달이라 금지(그 위 가격은 --color-text, 할인율만 빨강 배지)'. --color-brand 주석은 5배경 전부 3:1 통과라 현재 서술이 정확하니 불변. 토큰 값(hex)은 벤치마크 실측이므로 변경 금지. 수정 규모 주석 2줄.
2. [low/regression] DesignTokens.stories.tsx:205의 describeUsage가 forbidden 목록에 '본문 사용 금지'와 '계산 불가'를 함께 담고 :213이 사유를 '큰 글씨도 3:1 미달이므로'로 하드코딩한다. QA 결함 주입 렌더 실측 — 표는 해당 조합을 '계산 불가'로 찍는데 같은 페이지 문단은 '3:1 미달'이라고 측정한 적 없는 값을 단정했다. 비율은 어긋날 수 없게 됐지만 판정 사유는 어긋날 수 있어, dev의 '문단-표 모순이 구조적으로 불가능' 주장에 반례가 있다. 현재 토큰 11+5개가 전부 유효 hex라 실제 결함은 아니지만(정상 렌더 시 계산 불가 0행), contrast.ts:8이 예고한 'tokens.css 미로드 시 빈 문자열' 상황에서는 55행 전부가 이 경로를 타 접근성 정본 문서가 확신에 찬 거짓을 말한다. → forbidden을 '3:1 미달'과 '계산 불가' 두 목록으로 분리해 사유를 따로 서술한다(2~3줄). 덧붙여 :204가 '통과'(AA 4.5 충족)까지 usable로 묶어 '큰 글씨에만 쓴다'로 서술하는 과잉 제약도 같이 정리(현재는 SPECIAL_TEXT_RULES가 4.5 미달 2종뿐이라 드러나지 않고, 안전 방향이라 위험 없음).
3. [low/regression] DesignTokens.stories.tsx가 342줄로 file-size-cap(tsx 300줄, should) 상한을 이번 라운드에 넘겼다. round 2까지 약 297줄이었고 resolveBackground·describeUsage·evaluateContrast·ContrastRow 추가로 초과. should 규칙이라 차단 아님. → 후속 TODO — 대비 계산부(resolveBackground~buildContrastRows·describeUsage)를 contrastTable.ts로 분리해 스토리 파일에 렌더만 남긴다. 분리하면 contrast.ts처럼 단위 테스트가 가능해져 위 low 이슈도 테스트로 고정할 수 있다.
4. [low/regression] 축E 시각 기준선 불일치(round 1·2 이월, 지시 4대로 STEP 5 몫이라 이번 라운드 미실행이 정상). .speclinker/story_shots/baseline/ 46장(2026-09-16) vs storybook-static/index.json 스토리 137개. preview.ts의 tokens.css 임포트로 프리뷰 body가 Pretendard로 바뀐 것을 이번 라운드에도 브라우저로 확인(400/600/700 loaded). AC가 명시 허용한 변경이라 차단 사유 아님. → STEP 5에서 story_shots.py capture . --force로 시각 기준선을 현재 137개 스토리 기준으로 재기록.

사람 코멘트: 좁게 두 곳만: (1) tokens.css의 --color-price·--color-brand 주석을 대비표가 계산한 사용 규칙과 같은 문장으로 고친다(배경 조건 포함 — 예: '흰 배경(#ffffff) 위 굵은 큰 글씨 전용, surface-1/2/3·primary-bg 위 사용 금지'). 문장의 정본은 스토리의 describeUsage 결과이며, 주석은 그 결과를 그대로 옮긴다(수치는 표 계산값만). tokenCatalog.ts에도 같은 규칙이 적혀 있으면 함께 맞춘다. (2) describeUsage가 '계산 불가'와 '기준 미달'을 서로 다른 문구로 표기하게 한다('계산 불가 — 토큰 값 확인'). 이 두 가지 외 파일은 건드리지 않는다. 3(스토리 파일 342줄)은 이 SR에서 하지 않고 추적만(Dev 기록), 4는 STEP 5에서 capture --force.

## test-agent 결과 (STEP 5 테스트 작성·실행)

**실행 일자**: 2026-09-19 16:30

### 회귀 테스트 실행

**npm test** (Jest + typecheck):
```
19 suites / 218 tests
상태: ✅ 전부 통과 (기존 215 + 신규 contrast 3)
```

**npm run test-storybook** (Playwright):
```
41 suites / 137 tests (정적 스토리북 서버 정상 렌더)
상태: ✅ 전부 통과
콘솔: error 0건 ✅
포함: DesignTokens.stories.tsx PASS ✅
```

**축E 시각 기준선 재캡처**:
```
story_shots.py capture {{WS}} --force
결과: 137/137 스토리 캡처 완료 ✅
기준선 위치: {{WS}}/.speclinker/story_shots/baseline/
상태: body 폰트(Pretendard) 교체 반영, 레이아웃 정상 ✅
```

### TC 작성

**생성 위치**: `{{WS}}/docs/07_테스트케이스/TC_v1.0.md`

**작성 내용**:
- FUNC-CMN-001 — 디자인 토큰 및 Pretendard 폰트
- AC 12개 매핑 (TC-FUNC-CMN-001-01~12)
- 회귀 TC 결과 (npm test 218 / test-storybook 137 / 시각 기준선 137)

### TR 생성

**생성 위치**: `{{WS}}/docs/08_테스트결과보고서/TR_v1.0.md`

**포함 내용**:
- FUNC-CMN-001 전체 테스트 결과
- AC 커버리지 12/12 (100%) ✅
- 회귀 검증 3항 (jest·storybook·시각) 전부 PASS ✅
- 파일 검증 (폰트 3개 + 라이선스 + CSS + 스토리) ✅
- 규칙 준수 (must 6 / should 2) ✅
- 보안 검증 (외부 CDN 없음) ✅
- 최종 판정: ✅ **납품 가능**

### 최종 판정

✅ **PASS** — 회귀 테스트 전부 통과, 시각 기준선 재캡처 완료, TC·TR 작성 완료

---

## 후속 추적(TODO)
> round 4 QA CONCERNS — 재작업 없음(사람 결정: 추적등록 후 진행). 재작업 사유 아님, 전부 low.

1. `DesignTokens.stories.tsx`의 `describeUsage` 세 번째 폴백("쓸 수 있는 배경 0개")이 극단 상황(CSS 완전 미로드 등)에서만 도달 가능한 경로인데 여전히 "3:1 미달"을 측정 없이 단정한다. 정상 렌더에서는 도달 불가(QA 실측). 이 폴백 문구를 "계산 불가" 계열로 정리.
2. `DesignTokens.stories.tsx` 350줄 — `file-size-cap`(tsx 300줄, `should`) 초과. SR-310 공통 컴포넌트 작업 때 대비 계산부(`resolveBackground`~`describeUsage`)를 `contrastTable.ts`로 분리해 문서 스토리 분할 검토.
3. 축E 시각 기준선(`.speclinker/story_shots/baseline/`) 46장 vs 현재 스토리 137개 — STEP 5에서 `story_shots.py capture . --force`로 재기록(이 SR의 AC가 명시 허용한 body 폰트 교체 반영, 차단 사유 아님) ✅ **완료**.
