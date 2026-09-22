---
uis-id: UIS-CMN-001
name: 디자인 토큰
domain: common
domain-code: CMN
layer: ui
route: (없음 — 화면 라우트 아님, 전역 CSS 토큰 + 스토리북 문서 페이지)
screens_role: 디자인 시스템 문서(Storybook)
api_hints: []
access_control: []
anchors:
  - "modules/shop-web/src/styles/tokens.css"
  - "modules/shop-web/src/styles/tokenCatalog.ts"
  - "modules/shop-web/src/styles/contrast.ts"
  - "modules/shop-web/src/styles/DesignTokens.stories.tsx"
  - "modules/shop-web/src/main.tsx:1"
  - "modules/shop-web/.storybook/preview.ts:1"
revision_history:
  - "2026-09-19 SR-309.1 구현 후 역생성(코드 기준, STEP 5.5)"
---

# UIS-CMN-001: 디자인 토큰

> SR-309.1로 신규 도입된 shop-web 전역 디자인 토큰(CSS 커스텀 프로퍼티) + Pretendard 웹폰트 + 스토리북 'Design Tokens' 문서 페이지.
> 화면 라우트가 아니라 전 화면이 참조할 공유 계약이다 — 후속 SR-310(공통 컴포넌트)·SR-311(앱 셸)·SR-322/326(리스킨)이 이 토큰을 참조한다.

## 1. 개요

- 정본 파일: `modules/shop-web/src/styles/tokens.css` — `:root`에 선언된 CSS 커스텀 프로퍼티 전체가 값의 유일한 정본이다. 다른 파일(카탈로그·스토리)은 이 값을 다시 적지 않고 `getComputedStyle`로 읽는다(이중관리 금지).
- 로드 경로: `src/main.tsx`(앱 진입점)와 `.storybook/preview.ts`(스토리북 프리뷰) 각각 1줄 import — 외부 CDN 없이 Vite가 해시된 정적 자산으로 번들한다.
- 문서 페이지: `DesignTokens.stories.tsx`(Storybook, title `Design Tokens`) — 색 견본·배경×본문 명도 대비 표·타이포/간격/radius/그림자 견본을 `getComputedStyle`로 읽은 실제 적용값으로 렌더한다.
- 이 SR은 토큰을 **추가만** 한다 — 기존 화면·컴포넌트가 이 토큰을 참조하도록 바꾸는 작업은 범위 밖(SR-310/311/322/326).

## 2. 색 토큰

| CSS 변수 | 값 | 용도 |
|---|---|---|
| `--color-text` | `#101010` | 기본 본문 글자색 |
| `--color-text-secondary` | `#767676` | 설명·캡션 등 부차 정보 |
| `--color-text-tertiary` | `#aaaaaa` | 비활성·플레이스홀더 |
| `--color-surface-1` | `#f2f2f2` | 옅은 배경면(카드·구분 영역) |
| `--color-surface-2` | `#f5f5f5` | 더 옅은 배경면 |
| `--color-surface-3` | `#f9f9f9` | 가장 옅은 배경면(페이지 바탕 등) |
| `--color-primary` | `#713fc5` | 주요 강조·CTA 텍스트/아이콘 |
| `--color-primary-bg` | `#f2ebff` | 포인트 보라의 옅은 배경 |
| `--color-link` | `#177bc3` | 텍스트 링크 |
| `--color-price` | `#ff5259` | 할인율·판매가 강조(배경 제약 — 아래 3절) |
| `--color-brand` | `#ed1c24` | 브랜드 로고·강조 요소(배경 제약 — 아래 3절) |
| `--color-dim` | `rgba(16, 16, 16, 0.3)` | 모달·오버레이 배경 딤 |
| `--color-success` / `-bg` | `#136b2f` / `#e9f7ec` | 성공 상태(기존 `DeliveryBadge.tsx` 배지색과 정합) |
| `--color-warning` / `-bg` | `#8a5a00` / `#fff4e5` | 경고 상태(기존 `DeliveryBadge.tsx` 배지색과 정합) |
| `--color-error` / `-bg` | `#b42318` / `#fdecea` | 오류 상태(성공·경고와 같은 톤 규칙으로 파생) |
| `--color-info` / `-bg` | `#0b4ea2` / `#e8f1ff` | 정보 상태(성공·경고와 같은 톤 규칙으로 파생) |

그라데이션 2종: `--gradient-primary`(파랑→보라, 구매 버튼·헤더 토글) · `--gradient-accent`(보라→파랑, 최근 본 상품 헤더).

## 3. 명도 대비(WCAG) — 사용 제약

`DesignTokens.stories.tsx`의 대비표가 배경 5종(`#ffffff` · `--color-surface-3` `#f9f9f9` · `--color-surface-2` `#f5f5f5` · `--color-surface-1` `#f2f2f2` · `--color-primary-bg` `#f2ebff`) × 텍스트 토큰 전건을 `contrast.ts`(WCAG 상대휘도 공식)로 계산해 렌더한다. QA 4라운드 독립 재계산으로 아래 값이 실측 확정됐다(배경 순서는 위와 동일):

| 텍스트 토큰 | #ffffff | surface-3 | surface-2 | surface-1 | primary-bg | 판정 |
|---|---|---|---|---|---|---|
| `--color-text` | 19.03 | 18.07 | 17.45 | 17.00 | 16.40 | 본문 AA(4.5:1) 전배경 통과 |
| `--color-text-secondary` | 4.54 | 4.31 | 4.17 | 4.06 | 3.92 | `#ffffff`만 AA 통과, 나머지는 큰 글씨(3:1) 전용 |
| `--color-text-tertiary` | 2.32 | 2.21 | 2.13 | 2.08 | 2.00 | 전배경 AA·큰 글씨 기준 모두 미달 — 본문 사용 금지 |
| `--color-primary` | 6.52 | 6.20 | 5.98 | 5.83 | 5.62 | 본문 AA 전배경 통과 |
| `--color-link` | 4.51 | 4.28 | 4.14 | 4.03 | 3.89 | `#ffffff`만 AA 통과, 나머지는 큰 글씨 전용 |
| `--color-price` | **3.18** | **3.02** | 2.92 | 2.84 | 2.74 | `#ffffff`·`surface-3` 위 **큰 글씨(18.66px 이상 굵게, 또는 24px 이상) 전용** — 그 외 배경은 큰 글씨도 3:1 미달, 본문 사용 금지 |
| `--color-brand` | **4.38** | **4.16** | **4.02** | **3.91** | **3.78** | 5배경 전부 큰 글씨(3:1) 통과, AA 본문(4.5:1)은 전배경 미달 — 본문 사용 금지, 큰 글씨 전용 |
| 상태색 4종(성공/경고/오류/정보) | 6.62~8.00 | ~5.71~6.89 | | | 5.11 최저 | 5배경 전부 AA 본문 기준 통과 |

**사용 규칙(값의 정본 `tokens.css:44-55` 주석과 스토리 `describeUsage()` 산출 문장이 일치, STEP 5.3 축 B에서 기계 대조 확인됨)**:
- `--color-price`는 흰 배경(`#ffffff`)·`--color-surface-3` 위의 굵은 큰 글씨에만 쓴다. `--color-surface-1`/`-2`·`--color-primary-bg` 위에서는 큰 글씨도 3:1 미달이라 쓰지 않는다 — 그 위의 가격은 `--color-text`를 쓰고, 강조는 별도 배지로 대신한다.
- `--color-brand`는 5배경 전부에서 큰 글씨로만 쓴다(본문 텍스트에는 쓰지 않는다).
- 4.5:1 미달 조합은 표에서 "본문 사용 금지"로, 대비 계산 자체가 실패한 조합(비-hex 값 등)은 "계산 불가"로 별도 표기한다(두 사유를 혼동하지 않는다).

## 4. 타이포그래피

- 폰트: Pretendard(가변 아님, weight별 개별 `@font-face` 3개) — `Regular`(400)·`SemiBold`(600)·`Bold`(700). 실패 시 시스템 한글 폰트(`-apple-system, BlinkMacSystemFont, 'Malgun Gothic', 'Apple SD Gothic Neo', sans-serif`)로 대체(`font-display: swap`).
- 폰트 파일: `src/assets/fonts/Pretendard-{Regular,SemiBold,Bold}.woff2`(npm 공식 배포 패키지 `pretendard`(OFL-1.1) 원본, 서브셋 미적용 — 경량화는 후속 SR 추적, `## 후속 추적(TODO)` 참조) + `LICENSE-Pretendard.txt`.
- 크기 스케일 8단(`--text-2xs` 11px ~ `--text-2xl` 24px, 본문 `--text-base` 14px 기준) + 배너 카피용 `--text-display`(40px).
- 굵기 3종: `--font-weight-regular`(400)·`--font-weight-semibold`(600)·`--font-weight-bold`(700).
- **적용 범위 제약**: 이 SR이 바꾼 전역 스타일은 `body { font-family }` 선택자 1개뿐이다. 실측(QA 4라운드) 결과 프로덕션 페이지 10개(부품 포함 15개 파일)가 루트 요소에 인라인 `fontFamily: 'system-ui, sans-serif'`를 갖고 있어 `body` 규칙을 가린다 — **이 토큰 폰트가 프로덕션 화면에 실제로 적용되는 곳은 현재 0곳**이다(스토리북 프리뷰에서는 `preview.ts` import로 정상 적용·`loaded` 확인됨). 인라인 제거는 SR-310/SR-311의 선행 조건으로 이월한다(`## 후속 추적(TODO)` 참조).

## 5. 간격·radius·그림자·z-index·레이아웃

- 간격: `--space-1`(4px) ~ `--space-8`(40px), 4px 배수 8단.
- radius 3종: `--radius-sm`(4px)·`--radius-md`(8px, 카드 썸네일용)·`--radius-lg`(16px).
- 그림자 3종: `--shadow-sm`/`-md`/`-lg`(모두 `rgba(16,16,16,*)` 톤).
- z-index 층 6단: `--z-base`(0) < `--z-dropdown`(100) < `--z-sticky`(200) < `--z-overlay`(300) < `--z-modal`(400) < `--z-toast`(500).
- 레이아웃: `--layout-content-width`(750px, 본문 단일 컬럼) — 값만 정의, 실제 레이아웃 적용은 후속 SR(범위 밖).

## 6. 참조 무결성(정적 자산 경로)

Vite 자산 파이프라인이 `src/assets/fonts/*.woff2`를 상대경로 `url()` 참조로 해시+베이스를 자동 처리한다(SR-301 #1 함정 회피 — `public/`에 두지 않음):
- `npm run build` 산출물: `/shop/assets/Pretendard-*-<hash>.woff2` (base `/shop/`)
- `npm run build-storybook` 산출물: `./Pretendard-*-<hash>.woff2` (base `/`)

두 base가 다르므로 후속 SR이 폰트를 서브셋으로 교체할 때 두 경로 모두 재확인해야 한다.

## 7. 스토리북 문서 페이지

`design-tokens--tokens`(`DesignTokens.stories.tsx`) 1개 — 색 견본(토큰명·값·용도), 배경×본문 명도 대비 표(55행 = 배경 5 × 텍스트 토큰 11), 타이포 스케일 견본, 간격/radius/그림자 견본을 포함한다. 대비 계산은 행 단위로 격리되어(오류 주입 실측 확인) 한 조합의 계산 실패가 페이지 전체를 깨뜨리지 않는다.

## 8. 범위 밖(후속 SR)

- 기존 화면·컴포넌트가 이 토큰을 실제로 참조하도록 바꾸는 작업(SR-310 공통 컴포넌트·SR-311 앱 셸·SR-322/326 리스킨).
- 다크 모드, `--layout-content-width`의 실제 레이아웃 적용.
- Pretendard woff2 서브셋 경량화(SR-291 성능, 현재 원본 3개 합계 약 2.34MB).

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-19 | SR-309 | #1 | 신규 생성 — shop-web 전역 디자인 토큰(색·그라데이션·타이포·간격·radius·그림자·z-index·레이아웃 폭)과 Pretendard 웹폰트 계약, 명도 대비 사용 규칙을 코드 기준으로 역생성 | shop-web@a4a53fc |
