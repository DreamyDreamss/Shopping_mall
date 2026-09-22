---
uis-id: UIS-CMN-002
name: 공통 컴포넌트
domain: common
domain-code: CMN
layer: ui
route: (없음 — 화면 라우트 아님, 재사용 컴포넌트 세트 + 스토리북 문서)
screens_role: 공통 UI 컴포넌트 라이브러리(Storybook)
api_hints: []
access_control: []
anchors:
  - "modules/shop-web/src/components/common/Button.tsx"
  - "modules/shop-web/src/components/common/TextInput.tsx"
  - "modules/shop-web/src/components/common/QuantityStepper.tsx"
  - "modules/shop-web/src/components/common/quantityClamp.ts"
  - "modules/shop-web/src/components/common/Badge.tsx"
  - "modules/shop-web/src/components/common/Tabs.tsx"
  - "modules/shop-web/src/components/common/resolveSwipeTab.ts"
  - "modules/shop-web/src/components/common/useFocusTrap.ts"
  - "modules/shop-web/src/components/common/BottomSheet.tsx"
  - "modules/shop-web/src/components/common/PopupCarousel.tsx"
  - "modules/shop-web/src/components/common/Toast.tsx"
  - "modules/shop-web/src/components/common/ToastStack.tsx"
  - "modules/shop-web/src/components/common/toastQueue.ts"
  - "modules/shop-web/src/components/common/Skeleton.tsx"
  - "modules/shop-web/src/components/common/EmptyState.tsx"
  - "modules/shop-web/src/components/common/ErrorState.tsx"
revision_history:
  - "2026-09-19 SR-310.1 구현 후 역생성(코드 기준, STEP 5.5)"
  - "2026-09-19 SR-311.1 — Tabs에 badge?:boolean 계약 추가(탭 안 빨간 점, STEP 5.5)"
---

# UIS-CMN-002: 공통 컴포넌트

> SR-310.1로 신규 도입된 shop-web 공통 UI 컴포넌트 11종 — Button·TextInput·QuantityStepper·Badge·Tabs·BottomSheet·PopupCarousel·Toast/ToastStack·Skeleton·EmptyState·ErrorState.
> 모두 [[UIS-CMN-001]] 디자인 토큰만 참조하는 신규 추가 컴포넌트다. 이번 SR은 어느 화면에도 조립하지 않는다 — 실제 화면 적용은 리스킨 SR(SR-322·326) 몫.

## 1. 개요

- 위치: `modules/shop-web/src/components/common/`(신규 디렉터리). 기존 도메인 컴포넌트(`src/components/`의 Order*·DeliveryBadge·LoginForm, `src/features/shop|member/`)와 분리되어 있고 그쪽은 이 SR에서 변경하지 않는다.
- 컴포넌트마다 상태별 `*.stories.tsx`가 옆에 있다(`story-per-component` must 규칙).
- 인증·API 호출 없음(순수 프레젠테이션). `fetch`·`console.*` 0건.

## 2. Button

`Button.tsx` — 3종 variant: `primary`(검정 채움) · `secondary`(흰 바탕+회색 테두리) · `purchase`(파랑→보라 그라데이션, `--gradient-primary`). `loading?: boolean`이면 클릭 차단 + 라벨을 "처리 중…"으로 대체 + `aria-busy`. 네이티브 `<button>` 속성을 그대로 확장(`ButtonHTMLAttributes` 상속)한다.
`:hover`/`:focus-visible` 의사클래스는 `Button.css`로 처리 — 이 컴포넌트가 이 코드베이스에서 처음으로 별도 CSS 파일을 쓴 지점이다.
스토리: 3종 × (기본·호버·포커스·비활성·로딩) = 15개.

## 3. TextInput

`TextInput.tsx` — `label`(필수)·`error?: string | null`·표준 `input` 속성. id 미지정 시 `useId()`로 인스턴스 고유 id를 생성해 `label htmlFor`와 연결한다(같은 label을 가진 입력이 한 화면에 여럿이어도 충돌하지 않는다). 오류 시 `aria-invalid` + 빨간 테두리, 비활성 시 회색 배경.
스토리: 기본·포커스·오류·비활성.

## 4. QuantityStepper

`QuantityStepper.tsx` — `value`·`min`·`max`·`onChange(committed: number)`. `−`/`+` 버튼은 `value===min`/`value===max`에서 `disabled`. 직접 입력은 편집 중 로컬 문자열(draft)만 바꾸고, blur·Enter 커밋 시 `quantityClamp.ts`의 순수함수 `clampQuantity(raw, min, max)`로 경계 보정한 뒤 값이 실제로 바뀌었을 때만 `onChange`를 호출한다.
스토리: 중간값·최솟값·최댓값·직접입력 보정.

## 5. Badge

`Badge.tsx` — variant별 판별 유니온(`BadgeProps`): `tv`(검정)·`freeShipping`·`installment`(`months: number` 필수, "무이자 {N}개월")·`live`(`countdownSlot?: ReactNode` — 실제 초단위 갱신 타이머는 범위 밖, 슬롯만 제공)·`discount`(`rate: number` 필수, 호출부가 기존 `discountRate.ts`의 `calcDiscountRate`로 계산해 넘긴다 — Badge 안에서 재계산하지 않는다).
스토리: 5종 각 1개.

## 6. Tabs

`Tabs.tsx` — `tabs: {key,label,badge?:boolean}[]`·`activeKey`·`onChange`·`sticky?: boolean`(스크롤 시 `position: sticky` 고정). 스와이프 전환 판정은 `resolveSwipeTab.ts`의 순수함수 `resolveSwipeTab(startX, endX, thresholdPx, activeIndex, count)`로 분리되어 있고, 컴포넌트는 `onTouchStart/End`로 델타를 계산해 이 함수만 호출한다(스와이프 임계값 40px). 기존 `ProductDetailTabs`(상품 상세 전용 고정 4탭)와는 별개이며 대체하지 않는다.
`badge?: boolean`(SR-311.1 추가) — true인 탭 버튼 안(라벨 위 작은 줄)에 점을 렌더한다. 종전 라운드는 이 표시를 `Tabs` 바깥의 별도 오버레이 `<span>`으로 그렸으나, 활성 탭만 bold인 `.cmn-tabs__tab`과 폭 계산이 어긋나 탭 2개 이상에서 점 위치가 밀렸다 — 그래서 `Tabs` 자체의 계약으로 편입해 버튼 내부에서 그린다(위치가 활성 여부·탭 수와 무관해짐).
스토리: 기본·스티키 고정·배지 탭 1개 포함(SR-311.1 추가).

## 7. 포커스 트랩 공유 로직 — `useFocusTrap`

`useFocusTrap.ts`(hook) — `BottomSheet`·`PopupCarousel`이 공유하는 포커스 트랩·Esc 닫기·닫힌 뒤 트리거 요소로 포커스 복귀 로직. 두 컴포넌트에 각각 구현하지 않고 한 곳에만 두어 한쪽만 고쳐지는 사고를 막는다.
- 초기 포커스 이동: "닫힘→열림" 전이당 최초 1회만(`wasOpenRef`로 가드, `[open]` 단독 의존) — React 19 StrictMode 이중 이펙트에서 이미 옮겨진 포커스를 "직전 포커스"로 잘못 캡처하는 것을 방지.
- Esc/Tab 리스너: 최신 `onClose`를 `onCloseRef`로 읽어 effect 의존성에서 `onClose`를 제외(`[open]` 단독 의존) — 부모가 인라인 화살표 `onClose`를 넘기고 재렌더해도 시트/팝업 내부 포커스가 매번 첫 포커스 가능 요소로 튀지 않는다.
- 포커스 복귀: `open`이 실제로 `false`로 떨어질 때(effect cleanup)만 트리거 요소로 되돌린다(Esc 키다운 시점의 동기 처리가 아니다).

## 8. BottomSheet

`BottomSheet.tsx` — 구매 시트 용도. `open`·`onClose`·`children`·`ariaLabel?: string`(기본 `'구매 시트'`). controlled(오버레이+패널) 프레젠테이션, `useFocusTrap` 사용. `open=false`이면 아무것도 렌더하지 않는다(스토리북 '닫힘' 스토리는 `tags: ['renders-nothing']`로 표시된 정답 상태).
스토리: 열림·닫힘.

## 9. PopupCarousel

`PopupCarousel.tsx` — 이미지 팝업 캐러셀. `open`·`slides: {id, background}[]`(실제 이미지가 아니라 배경색 블록 — 팝업 운영 이미지 데이터는 범위 밖(SR-339), Storybook 단독 렌더에서 `<img>` 404를 원천 차단)·`onClose`·`onDismissToday`·`initialIndex?`(기본 0)·`autoAdvanceMs?`(기본 4000, 0이면 정지 — 스토리에서 시간 의존성 제거용). "오늘은 그만 보기"는 로컬 체크 표시(`dismissChecked`) 후 `onClose()` 호출, **그 뒤에** 별도로 `onDismissToday()`를 호출한다(닫기와 억제 기록을 독립적으로 재작업할 수 있도록 하나로 묶지 않는다). `useFocusTrap` 사용.
스토리: 첫번째(1/5)·마지막·오늘은그만보기.

## 10. Toast / ToastStack

`Toast.tsx` — 토스트 1개 프레젠테이션(`message`·`variant: 'default'|'success'|'error'`·`onDismiss`).
`ToastStack.tsx` — `toasts: ToastItem[]`·`onDismiss`·`max?: number`(기본 3). `toastQueue.ts`의 순수함수 `capToasts(toasts, max)`로 **자체적으로** 최대 개수를 강제한다(초과 시 가장 오래된 것부터 제거) — 호출부가 상한 없이 넘겨도 방어된다.
스토리: 1개·3개·초과 시 제거.

## 11. Skeleton

`Skeleton.tsx` — `variant: 'card'|'list'`. 기존 `ProductDetailSkeleton`(상품 상세 고정형)과 별개.
스토리: 카드·리스트.

## 12. EmptyState

`EmptyState.tsx` — 가운데 라인 아이콘(인라인 SVG) + `message`(한 줄 안내) + `actionLabel`/`onAction`(내부적으로 `Button variant="secondary"` 재사용) + `railSlot?: ReactNode`(하단 추천 레일 자리, 벤치마크 bench/screenshot-10 빈 장바구니 구성 — 실제 추천 데이터는 범위 밖). 기존 `CartEmptyState`(장바구니 전용)와 별개.
스토리: 기본·추천레일포함.

## 13. ErrorState

`ErrorState.tsx` — 원형 느낌표 아이콘(인라인 SVG) + 고정 문구 "이용에 불편을 드려 죄송합니다." + `subMessage`(호출부가 고정 카피만 전달 — 서버 원문을 이 컴포넌트가 조합하지 않는다) + `onHome`(`Button variant="secondary"`, "홈으로") + `onBack`(`Button variant="primary"`, "돌아가기"). 벤치마크 bench/screenshot-11 404 구성과 동일.
스토리: 기본.

## 14. 범위 밖(후속 SR)

- 이 11개 컴포넌트를 기존 화면에 실제로 조립하는 작업(리스킨 SR-322·326).
- PopupCarousel의 실제 이미지·"오늘은 그만 보기" 영속화(localStorage/DB) — `onDismissToday` 콜백만 제공, 실제 저장은 SR-339.
- Badge `live`의 실제 초 단위 카운트다운 갱신 타이머 — 이 배지를 쓰는 화면 SR 몫.
- Tabs/EmptyState/ErrorState/Skeleton이 기존 `ProductDetailTabs`/`CartEmptyState`/`ProductDetailSkeleton`을 대체하는 마이그레이션.

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-19 | SR-310 | #1 | 신규 생성 — 공통 컴포넌트 11종(Button/TextInput/QuantityStepper/Badge/Tabs/BottomSheet/PopupCarousel/Toast·ToastStack/Skeleton/EmptyState/ErrorState)과 상태별 스토리·단위/RTL 테스트를 코드 기준으로 역생성 | shop-web@7d32de0 |
| 2026-09-19 | SR-311 | #1 | Tabs에 badge?:boolean 계약 추가(탭 안 빨간 점 표시) | shop-web@0acc411 |
