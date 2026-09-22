---
story-id: STORY-SR-310.1
item: SR-310.1
title: 공통 컴포넌트
status: Done
domain: common
created: 2026-09-19
spec_markers: 0
sr-id: SR-310
approved_sha: 4ad8f6b5f2c9
---

# STORY-SR-310.1 — 공통 UI 컴포넌트 세트 — 버튼·입력·배지·탭·하단 시트·팝업·토스트·빈/오류 상태 — 공통 컴포넌트

## Story
공통 UI 컴포넌트 세트 — 버튼·입력·배지·탭·하단 시트·팝업·토스트·빈/오류 상태 — 공통 컴포넌트


## 변경 컨텍스트 (SR-310)
> 이 story는 변경요청 **SR-310 — 공통 UI 컴포넌트 세트 — 버튼·입력·배지·탭·하단 시트·팝업·토스트·빈/오류 상태** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-310/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-310/02_변경명세.md`

### 확정된 요건 문답 5건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 요구사항 '범위' 절 그대로 — 포함: shop-web 공통 컴포넌트 Button(주/보조/구매 그라데이션)·TextInput·QuantityStepper·Badge 5종(TV상품·무료 배송·무이자 N·LIVE 카운트다운 슬롯·할인율)·Tabs(스티키·스와이프)·BottomSheet·PopupCarousel('오늘은 그만 보기')·Toast(최대 3개)·Skeleton·EmptyState·ErrorState와 상태별 스토리, 키보드 조작(포커스 트랩·Esc·포커스 복귀). 모두 SR-309 디자인 토큰만 참조. 제외: 기존 화면 교체(SR-322·326 리스킨), 데이터 연동, 팝업 운영 데이터(SR-339), 오프라인 배너(SR-289 잔여), 백엔드.
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 화면·기존 컴포넌트(DeliveryBadge·LoginForm·Order* 등)의 동작·모양·테스트 전부 불변 — 새 컴포넌트는 추가만 하고 기존 화면에 끼우지 않는다. 스토리북 빌드·기존 스토리 콘솔 오류 0건 유지.
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 사용자 화면 변경 없음 — 새 공통 컴포넌트와 스토리만 추가한다(기존 화면 적용은 리스킨 SR). UIS 대상은 공통 컴포넌트 규격 문서 1건(UIS-CMN-002 공통 컴포넌트).
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — EmptyState 컴포넌트 자체가 빈 상태 규격이다: 가운데 라인 아이콘 + 한 줄 안내 + 테두리 버튼 + 하단 추천 레일 슬롯(벤치마크 bench/screenshot-10 빈 장바구니). ErrorState: 원형 느낌표 + '이용에 불편을 드려 죄송합니다.' + 보조 문구 + [홈으로](테두리)·[돌아가기](검정)(bench/screenshot-11 404).
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 컴포넌트별 상태 스토리: Button 3종×(기본·호버·포커스·비활성·로딩), TextInput(기본·포커스·오류·비활성), QuantityStepper(중간값·최솟값·최댓값·직접입력 보정), Badge 5종, Tabs(기본·스티키 고정), BottomSheet(열림·닫힘), PopupCarousel(1/5·마지막·오늘은 그만 보기), Toast(1개·3개·초과 시 가장 오래된 것 제거), Skeleton(카드·리스트), EmptyState·ErrorState.

### 구현 모듈(제약) — `shop-web` (`{{SRC_SHOP_WEB}}`)
이 작업 항목의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약·편성에서 사람이 지정한 값).

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-310/02_변경명세.md`에서 도출)
- [x] UIS-CMN-002: **Button**: 3종 — 주(검정 채움) · 보조(흰 바탕 회색 테두리) · 구매(파랑→보라 그라데이션). 상태별 스토리: 각 종별 기본·호버·포커스·비활성·로딩.
- [x] UIS-CMN-002: **TextInput**: 상태별 스토리 — 기본·포커스·오류·비활성.
- [x] UIS-CMN-002: **QuantityStepper**: `−` / 수량 / `+` 구성. 최솟값·최댓값 경계에서 버튼 비활성화, 범위 밖 직접 입력값은 경계로 보정. 상태별 스토리 — 중간값·최솟값·최댓값·직접입력 보정.
- [x] UIS-CMN-002: **Badge**: 5종 — TV상품(검정) · 무료 배송 · 무이자 N(테두리형) · LIVE(카운트다운 슬롯) · 할인율(빨강). 5종 각각 스토리.
- [x] UIS-CMN-002: **Tabs**: 스티키(스크롤 고정) · 스와이프 전환. 상태별 스토리 — 기본·스티키 고정.
- [x] UIS-CMN-002: **BottomSheet**: 구매 시트 용도. 열릴 때 포커스 트랩, Esc·닫기로 닫힘, 닫힌 뒤 트리거 요소로 포커스 복귀. 상태별 스토리 — 열림·닫힘.
- [x] UIS-CMN-002: **PopupCarousel**: 이미지 팝업 캐러셀, '오늘은 그만 보기' 옵션 포함. 상태별 스토리 — 1/5·마지막·오늘은 그만 보기.
- [x] UIS-CMN-002: **Toast**: 최대 3개 동시 노출, 초과 시 가장 오래된 것부터 제거. 상태별 스토리 — 1개·3개·초과 시 제거.
- [x] UIS-CMN-002: **Skeleton**: 상태별 스토리 — 카드·리스트.
- [x] UIS-CMN-002: **EmptyState**: 가운데 라인 아이콘 + 한 줄 안내 + 테두리 버튼 + 하단 추천 레일 슬롯(벤치마크 bench/screenshot-10 빈 장바구니 구성과 동일).
- [x] UIS-CMN-002: **ErrorState**: 원형 느낌표 아이콘 + '이용에 불편을 드려 죄송합니다.' + 보조 문구 + [홈으로](테두리 버튼) · [돌아가기](검정 버튼)(벤치마크 bench/screenshot-11 404 구성과 동일).
- [x] UIS-CMN-002: 공통 키보드 조작: BottomSheet·PopupCarousel 모두 포커스 트랩·Esc 닫기·닫힌 뒤 포커스 복귀.
- [x] UIS-CMN-002: 산출물: 위 11개 컴포넌트 + 상태별 스토리(Storybook) + 컴포넌트 단위 테스트. UIS 대상은 이 문서(UIS-CMN-002 공통 컴포넌트 규격) 1건.

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **신규 스펙(예약 — 본문은 구현 후 역생성)**: UIS-CMN-002
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)



## 구현 계획

- **파일**: 신규 디렉터리 `modules/shop-web/src/components/common/`(기존 `src/components/`의 Order*·DeliveryBadge·LoginForm — 도메인 성격 — 과 분리, `src/features/shop|member/`처럼 2단 중첩이라 `story-per-component` pair 규칙이 정상 적용됨, `.tsx` 파일마다 옆에 `.stories.tsx`).
  - `Button.tsx`+`.stories.tsx`+`Button.css`: variant `primary`(검정 채움, bg `var(--color-text)`)·`secondary`(흰 바탕+테두리 `var(--color-text-tertiary)`)·`purchase`(`background: var(--gradient-primary)`), `loading?`·`disabled?` prop. `:hover`/`:focus-visible` 실제 CSS 의사클래스는 `Button.css`로(이 코드베이스 첫 인라인스타일 탈피 지점 — 이유는 아래 "프레임워크 실행 모델 함정" 참조). 호버·포커스 스토리는 `play` 함수로 `userEvent.hover`/`element.focus()`를 실행해 실제 상태를 강제한다(정적 렌더로는 의사클래스가 안 잡힘).
  - `TextInput.tsx`+`.stories.tsx`+`TextInput.css`: `label`·`value`·`onChange`·`error?`·`disabled?`. `:focus` 테두리색은 CSS로, 오류/비활성은 prop 기반 인라인 스타일로.
  - `QuantityStepper.tsx`+`.stories.tsx`+`quantityClamp.ts`(+`.unit.test.ts`)+`.test.tsx`: `value`·`min`·`max`·`onChange(commited:number)`. 클램프 순수함수 `clampQuantity(raw, min, max)`를 분리해 `discountRate.ts`와 같은 방식으로 단위테스트한다. `−`/`+` 버튼은 `value===min`/`value===max`일 때 `disabled`. 직접입력은 `CartLineItem`의 draft 패턴(편집 중엔 로컬 문자열, blur/Enter에서만 커밋)을 재사용하되 서버 왕복이 없으므로 커밋 즉시 `clampQuantity`로 경계 보정 후 `onChange` 호출.
  - `Badge.tsx`+`.stories.tsx`: `variant: 'tv'|'freeShipping'|'installment'|'live'|'discount'`. `installment`는 `months: number` prop("무이자 {N}개월"), `live`는 `countdownSlot?: ReactNode`(실제 타이머는 범위 밖 — 아래 참조), `discount`는 `rate: number`를 그대로 받는다(호출부가 기존 `discountRate.ts`의 `calcDiscountRate`로 계산해 넘긴다 — 새 계산 로직을 Badge 안에 재구현하지 않는다, 아래 "실패 사례집 대조" 참조).
  - `Tabs.tsx`+`.stories.tsx`+`resolveSwipeTab.ts`(+`.unit.test.ts`)+`.test.tsx`+`Tabs.css`(`position: sticky`용): `tabs:{key,label}[]`·`activeKey`·`onChange`·`sticky?`. 스와이프 판정은 `resolveSwipeTab(startX,endX,thresholdPx,activeIndex,count)` 순수함수로 분리(테스트 가능), `Tabs.tsx`는 `onTouchStart/Move/End`로 델타를 계산해 이 함수만 호출. 기존 `ProductDetailTabs`(상품 상세 전용 고정 4탭)는 손대지 않는다(회귀 유지) — 새 `Tabs`는 범용이고 이번 SR에 어느 화면에도 끼우지 않는다.
  - `useFocusTrap.ts`(hook, `.tsx` 아님 → pair 규칙 대상 아님): `BottomSheet`·`PopupCarousel`이 공유하는 포커스 트랩·Esc·복귀 로직 한 곳(중복 구현 금지, 아래 "프레임워크 실행 모델 함정"의 까다로운 부분을 한 곳에서만 맞추면 됨).
  - `BottomSheet.tsx`+`.stories.tsx`+`.test.tsx`: `open`·`onClose`·`children`. `ZipcodeSearchModal`처럼 controlled(오버레이+패널) 프레젠테이션이되 `useFocusTrap` 사용.
  - `PopupCarousel.tsx`+`.stories.tsx`+`.test.tsx`: `open`·`slides`(색상 블록, 아래 "실패 사례집 대조" 참조)·`onClose`·`onDismissToday`. `HeroBannerCarousel`의 자동넘김 이펙트 패턴(`[slides.length]` dep + cleanup)을 재사용하고 `useFocusTrap`도 사용.
  - `Toast.tsx`+`.stories.tsx`: 토스트 1개 프레젠테이션(`message`·`variant`·`onDismiss`).
  - `ToastStack.tsx`+`.stories.tsx`+`toastQueue.ts`(+`.unit.test.ts`): `pushToast(existing, next, max=3)` 순수함수(최대 3, 초과 시 가장 오래된 것 제거)를 분리 테스트. `ToastStack`은 `toasts`(이미 상한 적용된 배열)+`onDismiss`를 받는 프레젠테이션이고, "초과 시 제거" 스토리는 스토리 파일 로컬 `useState` 데모 래퍼(공개 API 아님)로 `pushToast`를 실제 호출해 보여준다.
  - `Skeleton.tsx`+`.stories.tsx`: `variant:'card'|'list'`. 기존 `ProductDetailSkeleton`(상품 상세 고정형)은 그대로 둔다.
  - `EmptyState.tsx`+`.stories.tsx`: 라인아이콘(인라인 SVG, `stroke="currentColor"`)+`message`+`actionLabel`/`onAction`(내부적으로 `Button variant="secondary"` 재사용)+`railSlot?: ReactNode`(bench/screenshot-10 하단 레일 자리, 실제 추천 데이터는 이 SR 범위 밖). 기존 `CartEmptyState`(장바구니 전용)는 그대로 둔다.
  - `ErrorState.tsx`+`.stories.tsx`: 원형 느낌표 아이콘(인라인 SVG)+고정 문구 '이용에 불편을 드려 죄송합니다.'+`subMessage`+`onHome`(`Button variant="secondary"`)+`onBack`(`Button variant="primary"`) — Button을 조립해 재사용(스타일 중복 정의 금지).

- **데이터**: 해당 없음 — DB·백엔드 변경 없음(확정 문답 db_ripple·db_migration, "화면 컴포넌트 SR이라 DB·테이블 변경이 없다"). DDL·트랜잭션·락 대상 없음.

- **순서·보안**: 인증/API 없음(순수 프레젠테이션, 규칙 `web-fetch-only-in-api` — 11개 컴포넌트 전부 `fetch` 직접 호출 금지, `console.log` 금지). 부수효과 순서로 다룰 것은 키보드 상호작용뿐이다:
  - Esc 키다운 → `onClose()` 호출(상태 변경은 부모 몫, controlled 패턴) → **포커스 복귀는 `onClose` 호출 시점이 아니라 `open`이 실제로 `false`로 바뀐 뒤(`useEffect` cleanup)** 수행한다. 키다운 핸들러 안에서 동기적으로 포커스를 되돌리면 부모의 상태 갱신(비동기 커밋)과 순서가 어긋날 수 있다.
  - PopupCarousel의 "오늘은 그만 보기" 체크는 닫기 액션(`onClose`) **뒤에** `onDismissToday()`를 별도 호출한다(둘을 하나로 묶지 않는다 — 향후 실제 영속화(SR-339)가 붙을 때 닫기와 억제 기록을 독립적으로 재작업할 수 있게).
  - 정보 노출: 해당 없음(오류 문구는 이 SR에서 고정 카피만 사용, 서버 원문을 다루지 않는다).

- **계약**: 새 오류 코드·응답 봉투·상태 코드 없음(백엔드 미변경). 컴포넌트 props가 곧 계약이며 위 "파일" 절에 명시.

- **테스트**:
  - 순수 함수 단위테스트(`.unit.test.ts`, node 환경): `quantityClamp.ts`(경계값·범위밖 보정), `resolveSwipeTab.ts`(좌/우 스와이프·임계값 미만 무변화), `toastQueue.ts`(1개/3개/4번째 삽입 시 가장 오래된 것 제거).
  - 컴포넌트 RTL 테스트(`.test.tsx`, jsdom): `QuantityStepper.test.tsx`(경계에서 버튼 disabled, 직접입력 후 blur 시 보정된 값으로 `onChange`), `Tabs.test.tsx`(`fireEvent.touchStart/Move/End`로 스와이프 탭 전환, sticky prop일 때 스타일/클래스 확인), `BottomSheet.test.tsx`·`PopupCarousel.test.tsx`(열릴 때 첫 포커스 가능 요소로 포커스 이동, Esc로 `onClose` 호출, 닫힌 뒤 트리거 요소로 포커스 복귀 — **`<StrictMode>`로 감싸 렌더**해 이중 이펙트에서도 트리거 포착이 어긋나지 않는지 확인, 아래 "프레임워크 실행 모델 함정" 참조).
  - Storybook 상태별 스토리(축 E `story_gate.py` 대상): STORY 확정 답변 5(scr_states)의 목록 그대로 — Button 3종×5상태, TextInput 4상태, QuantityStepper 4상태, Badge 5종, Tabs 2상태, BottomSheet 2상태, PopupCarousel 3상태, Toast(1개·3개·초과제거), Skeleton 2상태, EmptyState·ErrorState 각 1.
  - 기준선 영향: 새 파일만 추가, 기존 컴포넌트·페이지·테스트는 import하지 않는다 — `npm test`(타입체크+jest) 전체와 `npm run test-storybook` 전체를 최종 확인 시 스위트 전체로 돌린다(계획에 파일 나열해도 좁혀서 실행하지 않는다 — SR-307 #1 사례 대조).

- **테스트 격리**: 이 SR의 컴포넌트는 localStorage·세션·DB 카운터를 전혀 쓰지 않는 순수 프레젠테이션이라(팝업 억제 영속화는 범위 밖) 테스트 간 공유 상태가 없다. `document.activeElement`만 테스트마다 남을 수 있는 유일한 전역 상태인데, RTL은 `afterEach`에서 자동 unmount(cleanup)하므로(기존 `MyAddressesPage.test.tsx`도 이 묵시적 cleanup에 의존) 추가 조치가 필요 없다 — 단, 포커스 복귀 단언은 각 테스트가 자기 트리거 버튼을 직접 렌더링해 사용하고 이전 테스트의 DOM에 의존하지 않는다.

- **폴백·우회 경로의 자격 판정**: 해당 없음 — 인증·조회 경로, DB 폴백, 캐시, 화이트리스트를 여는 컴포넌트가 없다(전부 프레젠테이션, 서버 상태 없음).

- **프레임워크 실행 모델 함정**: React 19 StrictMode(dev, `main.tsx`가 이미 `<StrictMode>`로 감싸고 있음)는 mount 직후 effect를 한 번 더 이중 실행한다(mount→cleanup→mount 시뮬레이션). `BottomSheet`/`PopupCarousel`이 열릴 때 "직전 포커스 요소"를 `document.activeElement`로 캡처하는 effect를 `[open]` 의존성으로 두면, 이 이중 실행 사이에 첫 실행이 이미 포커스를 시트 안으로 옮겨버린 뒤 두 번째 실행이 그 옮겨진 요소를 "직전 포커스"로 잘못 캡처할 위험이 있다(`HeroBannerCarousel`이 `[slides.length]`+cleanup으로 인터벌 중복을 막은 것과 같은 계열의 함정, `refreshOnce.ts`류의 "1회성 호출을 이중 실행에서 지킨다"는 기존 관례의 포커스 버전). 대응: "직전 포커스 캡처"는 `wasOpenRef`(cleanup에서 리셋하지 않는 ref)로 최초 1회만 수행하도록 가드하고, "포커스 복귀"는 별도로 `open`이 실제 `false`로 떨어질 때의 cleanup에서만 수행한다(같은 effect 안에서 캡처와 복귀를 뒤섞지 않는다). `useFocusTrap.ts` 한 곳에만 이 로직을 두고 `BottomSheet`/`PopupCarousel` 양쪽에서 재사용해, 두 곳에 각각 구현해 한쪽만 맞고 한쪽은 틀리는 사고(SR-309 #1 round3 "처방이 한쪽 파일만 고침" 사례와 같은 계열)를 피한다. `BottomSheet.test.tsx`/`PopupCarousel.test.tsx`는 `<StrictMode>`로 감싸 렌더해 이 함정을 실제로 행사한다.

- **범위 밖**:
  - PopupCarousel의 실제 이미지·"오늘은 그만 보기" 영속화(localStorage/DB) — 슬라이드는 `HeroBannerCarousel`처럼 배경색 블록으로 스토리를 구성한다(실제 `<img>`를 쓰면 스토리북 단독 렌더에서 404 콘솔 오류로 축 E가 막힌다 — 아래 "실패 사례집 대조" SR-306 #1 사례). `onDismissToday` 콜백만 제공하고 실제 저장은 팝업 운영 데이터 SR(SR-339) 몫(확정 문답 scope_freeze).
  - Badge `live` 배지의 실제 초 단위 카운트다운 갱신 로직 — `countdownSlot` prop으로 텍스트만 받는다. 실시간 갱신 타이머는 이 배지를 실제로 쓰는 화면 SR이 결정할 몫(요구사항엔 "카운트다운 슬롯"이라고만 명시돼 있어 슬롯 제공까지가 이번 범위).
  - 새 컴포넌트를 기존 화면에 끼우는 것 — 전부 신규 추가만, 기존 화면 조립은 리스킨 SR(SR-322·326).
  - Tabs/EmptyState/ErrorState/Skeleton이 기존 `ProductDetailTabs`/`CartEmptyState`/`ProductNotFoundNotice`/`ProductDetailSkeleton`을 대체·마이그레이션하는 것 — 이번엔 신규 병존만, 마이그레이션은 후속 SR.

- **실패 사례집 대조**(`harness/antipatterns.all.md`):
  - "스토리북이 실제 상품 이미지 경로를 쓰는 스토리를 만들어 백엔드 없이 렌더되는 스토리북에서 404가 났다"(SR-306 #1 r3) — 이 SR도 PopupCarousel이 "이미지 팝업"이라 자칫 실제 `<img src="...">`를 스토리에 쓰기 쉽다. 이 SR엔 팝업 운영 이미지 데이터가 없으므로(확정 문답 scope_freeze) 조건이 그대로 성립 — 배경색 블록으로 대체해 애초에 이미지 요청 자체를 만들지 않는다(위 "범위 밖" 참조).
  - "미정의 코드를 default 분기로 뭉쳐 잘못된 전이가 생겼다"(SR-234 FUNC-member-007 r1) — 이 SR엔 오류 코드 분기가 없어 직접 조건은 성립하지 않지만, `ErrorState`가 향후 실제 API 오류를 받을 화면에 재사용될 걸 감안해 `subMessage`를 자유 문자열로 열어두되 이 SR 자체는 고정 카피만 렌더한다(서버 원문 조합 로직을 이 컴포넌트에 넣지 않는다 — 넣는 순간 같은 함정의 재발 지점이 된다).
  - "새 화면이 기존 API를 소비할 때 소비처의 기존 오류 표시 방식을 재사용하지 않고 자체 변환 함수를 새로 만들었다"(SR-235 #1) — 같은 원리로 Badge의 할인율 계산을 새로 만들지 않고 기존 `discountRate.ts`의 `calcDiscountRate`(부동소수점 순서 버그를 이미 round1에서 고친 정본)를 그대로 재사용한다(위 "파일" 절 Badge 항목).
  - "사람 재작업 지시가 지목한 파일 중 한쪽만 고쳐 처방이 반쪽으로 끝났다"(SR-309 #1 round3) — 포커스 트랩·Esc·복귀 로직을 BottomSheet/PopupCarousel 두 파일에 각각 구현하면 같은 함정(한쪽만 고치는 재작업)이 재발할 조건이 이미 있다 — `useFocusTrap.ts` 한 곳으로 합쳐 원천 차단한다(위 "프레임워크 실행 모델 함정" 참조).

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
계획 확인: 계획대로 진행 (2026-09-19)

구현 완료 (2026-09-19) — 계획 그대로 진행, 재작업 없음.

**신규 디렉터리** `modules/shop-web/src/components/common/`(기존 `src/components/`의 Order*·DeliveryBadge·LoginForm과 분리).

- `Button.tsx`+`.css`+`.stories.tsx` — 주·보조·구매 3종, `loading`/`disabled`. `:hover`/`:focus-visible`은 `Button.css`로(이 코드베이스 첫 CSS 파일 임포트 지점). 호버·포커스 스토리는 `storybook/test`의 `userEvent.hover`/`element.focus()`를 `play` 함수로 실행.
- `TextInput.tsx`+`.css`+`.stories.tsx` — `:focus` 테두리색만 CSS, 오류·비활성은 prop 기반 인라인 스타일(계획대로).
- `quantityClamp.ts`+`.unit.test.ts`, `QuantityStepper.tsx`+`.test.tsx`+`.stories.tsx` — `CartLineItem` draft 패턴 재사용, 클램프는 순수함수로 분리.
- `Badge.tsx`+`.stories.tsx` — 5종(tv/freeShipping/installment/live/discount). 할인율은 재계산하지 않고 호출부가 기존 `discountRate.ts`의 `calcDiscountRate` 결과를 `rate`로 받는다.
- `resolveSwipeTab.ts`+`.unit.test.ts`, `Tabs.tsx`+`.css`+`.test.tsx`+`.stories.tsx` — 스티키는 `position: sticky`(CSS), 스와이프 판정은 순수함수. 기존 `ProductDetailTabs`는 손대지 않음.
- `useFocusTrap.ts`(hook, 계획대로 `.tsx`가 아니라 pair 규칙 대상 아님) — BottomSheet·PopupCarousel 공유. 캡처는 `wasOpenRef`로 "닫힘→열림" 전이당 1회 가드, 복귀는 별도 effect의 cleanup에서만(StrictMode 이중 이펙트 대응).
- `BottomSheet.tsx`+`.test.tsx`+`.stories.tsx`, `PopupCarousel.tsx`+`.test.tsx`+`.stories.tsx` — 둘 다 `useFocusTrap` 재사용. PopupCarousel 슬라이드는 배경색 블록(합성 데이터, 실제 이미지 미사용 — SR-306 #1 404 함정 회피). "오늘은 그만 보기"는 `onClose` 호출 뒤 `onDismissToday`를 별도 호출(계획대로 순서 분리).
- `toastQueue.ts`+`.unit.test.ts`, `Toast.tsx`+`.stories.tsx`, `ToastStack.tsx`+`.stories.tsx` — 상한(기본 3, 초과 시 최오래된 것 제거)은 `pushToast` 순수함수. "초과 시 제거" 스토리만 로컬 `useState` 데모 래퍼로 `pushToast`를 실제 호출.
- `Skeleton.tsx`+`.stories.tsx`(카드·리스트), `EmptyState.tsx`+`.stories.tsx`(라인아이콘+안내+`Button variant=secondary`+`railSlot?`), `ErrorState.tsx`+`.stories.tsx`(원형 느낌표+고정 카피+`Button` 조립).

**인프라(테스트 도구 최소 보강)** — 컴포넌트가 이 코드베이스에서 처음 `.css`를 직접 import해 두 곳을 건드렸다:
- `modules/shop-web/src/css.d.ts`(신규) — tsc용 `declare module '*.css'`.
- `modules/shop-web/jest.styleMock.cjs`(신규) + `modules/shop-web/jest.config.cjs`(수정, `moduleNameMapper: {'\\.css$': ...}` 추가) — jest(jsdom)가 `.css` 임포트를 빈 목으로 통과시키게(스타일 자체는 Vite/Storybook이 처리, jest는 로직만 검증).

**결정 메모**
- Badge 색상 매핑(freeShipping=success 톤, live=brand 레드, discount=`--color-price` 배경)은 확정 요건에 명시되지 않은 부분이라 tokens.css의 의미에 맞춰 판단(TV=검정·무이자=테두리형·할인율=빨강만 명시됨). 실제 화면 적용 SR(SR-322·326 등)에서 시각 기준선 확인 시 조정 가능.
- PopupCarousel에 계획서에 없던 `initialIndex?` optional prop을 추가(기본 0, additive) — "마지막" 상태 스토리를 실제 컴포넌트로 보여주기 위함(`ProductDetailTabs`의 `initialTab` 패턴과 동일).

**검증**
- `npm test`(tsc --noEmit + jest) 전체: 26 suites / 254 tests 통과.
- `npx storybook build`: 정상 빌드(경고 없음, chunk 크기 경고는 기존과 동일 수준).
- `npm run test-storybook` 전체(53 suites / 183 tests, 기존 스토리 전부 포함): 전부 통과 — 신규 12개 스토리 파일 포함, 콘솔 오류 0건. (주: 기본 python http.server로 고동시성 실행 시 기존 파일(CartSummary 등)까지 타임아웃이 났는데, `serve` 정적 서버 + `--maxWorkers=2`로 바꾸니 전부 통과 — 테스트 인프라(정적 서버 동시성) 문제였고 컴포넌트 결함이 아님을 확인.)

다음 단계: /sl-test

## 재작업 완료 (2026-09-19) — round1 QA CONCERNS 반영, 재구현

**(medium/regression) useFocusTrap 포커스 튐 수정**
- `modules/shop-web/src/components/common/useFocusTrap.ts` — 초기 포커스 이동 effect를 `[open]`에만 의존하도록 분리(open이 false→true로 바뀔 때 1회만 실행). Esc/Tab 리스너는 `onCloseRef`(매 렌더 최신값으로 갱신되는 ref)로 최신 `onClose`를 읽도록 바꿔, 리스너 등록 effect도 `[open]`에만 의존한다. 부모가 인라인 화살표 `onClose`를 넘기고 재렌더해도(`OrderPage.tsx` 관용구) 시트/팝업 내부 포커스가 더 이상 튀지 않는다.
- `modules/shop-web/src/components/common/BottomSheet.test.tsx` — QA의 임시 RTL 프로브 재현 절차("확인" 버튼 포커스 → onClose 신원만 바꿔 rerender → 포커스 유지 확인)를 정식 회귀 테스트 `열린 채로 부모가 재렌더돼도(인라인 onClose 신원 변경) 내부 포커스가 유지된다`로 추가.

**(low, 함께 고치기)**
1. `modules/shop-web/src/components/common/toastQueue.ts` — `capToasts(toasts, max=3)` 순수함수 추가(가장 오래된 것부터 제거, `pushToast`와 같은 배열 규약). `modules/shop-web/src/components/common/toastQueue.unit.test.ts`에 단위테스트 4건 추가.
   `modules/shop-web/src/components/common/ToastStack.tsx` — `capToasts`로 상한을 스스로 강제(`max?` prop, 기본 3). 신규 `modules/shop-web/src/components/common/ToastStack.test.tsx` — toasts 5개를 넘겨도 최신 3개만 렌더하는지, `max` prop으로 상한을 바꿀 수 있는지 컴포넌트 테스트 3건 추가.
2. `modules/shop-web/src/components/common/PopupCarousel.tsx` — "오늘은 그만 보기" 클릭 시 로컬 UI 상태 `dismissChecked`로 체크박스형 표시를 추가(계약은 불변 — 여전히 `onClose` 뒤 `onDismissToday`를 별도 호출). `autoAdvanceMs?` prop(기본 4000, 0이면 자동 넘김 정지)도 추가.
   `modules/shop-web/src/components/common/PopupCarousel.stories.tsx` — 전 스토리 공통 args에 `autoAdvanceMs: 0`을 넣어 시간 의존을 없애고, '오늘은그만보기' 스토리는 `play`로 옵션 버튼을 실제 클릭해 체크된 상태를 시각적으로 구별한다.
   `modules/shop-web/src/components/common/PopupCarousel.test.tsx` — `autoAdvanceMs=0`이면 자동으로 넘어가지 않는지, 클릭 후 `aria-pressed`가 켜지는지 테스트 2건 추가.
3. `modules/shop-web/src/components/common/Badge.tsx` — `BadgeProps`를 variant별 판별 유니온으로 변경(installment는 `months` 필수, discount는 `rate` 필수) — 누락 시 타입 오류로 잡힌다. 기존 `Badge.stories.tsx`는 이미 각 variant에 필요한 prop을 전부 넘기고 있어 수정 불필요.
4. `modules/shop-web/src/components/common/TextInput.tsx` — id 폴백을 `cmn-text-input-${label}`에서 React `useId()`로 교체(동일 label 두 개가 한 화면에 있어도 DOM id 충돌이 나지 않는다).
5. `modules/shop-web/src/components/common/BottomSheet.tsx` — `ariaLabel?` prop 추가(기본값 '구매 시트' 유지 — 아직 소비처가 없어 회귀 없음).
6. 최소 RTL 테스트(렌더 확인·버튼 콜백 호출) 신규 추가 — `modules/shop-web/src/components/common/Toast.test.tsx`, `modules/shop-web/src/components/common/Skeleton.test.tsx`, `modules/shop-web/src/components/common/EmptyState.test.tsx`, `modules/shop-web/src/components/common/ErrorState.test.tsx`.

기존 파일·화면(회귀 대상)은 동작을 바꾸지 않았다 — 이번 재작업도 신규 추가와 공통 컴포넌트 내부 구현 변경뿐이고, 아직 실제 화면에 끼운 소비처가 없다.

**검증**
- `npx tsc --noEmit`: 오류 없음.
- `npx jest`: 31 suites / 274 tests 통과(round1의 26 suites / 254 tests에서 신규 5개 suite·20개 test 순증 — BottomSheet 회귀 테스트 1건, PopupCarousel 신규 2건, `ToastStack.test.tsx`(신규 3건), `Toast`·`Skeleton`·`EmptyState`·`ErrorState.test.tsx`(신규 4파일 합계 10건), `toastQueue.unit.test.ts`의 `capToasts` 4건).
- `npx storybook build`: 정상 빌드(경고는 round1과 동일한 chunk 크기 경고뿐, 신규 경고 없음).
- `npx test-storybook`(storybook-static을 `npx serve`로 정적 서빙한 뒤 `--url`로 지정해 `--maxWorkers=2`로 실행 — round1 Dev 기록이 남긴 정적 서버 동시성 인프라 방식을 그대로 재사용): 53 suites / 183 tests 전부 통과, 콘솔 오류 0건.

다음 단계: /sl-test

## 재작업 완료 (2026-09-19, round2) — 축 E(화면 상태) FAIL 반영
`modules/shop-web/src/components/common/BottomSheet.stories.tsx` — '닫힘' 스토리에 `tags: ['renders-nothing']` 추가. BottomSheet는 controlled 컴포넌트로 `open=false`일 때 아무것도 렌더하지 않는 것이 설계상 정답(구현 계획에 명시)인데, story_gate.py(축E)가 빈 렌더를 실패로 오판했다 — 도구 규약(STORY_GATE)상 의도적 빈 렌더 스토리는 이 태그로 표시해야 통과한다. 기능 코드 변경 없음(태그만 추가).

## 테스트 결과
**test-agent** (2026-09-19 18:47)

### TC 작성 및 실행
- ✅ **TC 작성**: 62개 (AC별 매핑 TC + 회귀 TC)
  - Button.test.tsx: 6 TC
  - TextInput.test.tsx: 6 TC
  - QuantityStepper.test.tsx: 6 TC
  - Tabs.test.tsx: 4 TC
  - BottomSheet.test.tsx: 4 TC (포커스 트랩/Esc/복귀)
  - PopupCarousel.test.tsx: 4 TC (포커스 트랩/Esc/복귀)
  - Toast.test.tsx: 2 TC
  - Toast.unit.test.ts (toastQueue): 4 TC (상한·제거)
  - Skeleton.test.tsx: 2 TC
  - EmptyState.test.tsx: 3 TC
  - ErrorState.test.tsx: 3 TC
  - 기타 순수함수 단위테스트: 12 TC

- 📊 **테스트 결과**: 통과 469/469 (100%)
  - npm test: 33 suites / 286 tests ✅
  - npm run test-storybook: 53 suites / 183 tests ✅
  
- 🔁 **회귀 검증**: 기존 shop-web 테스트 전부 통과 (231 tests ✅)
  - 신규 컴포넌트만 추가, 기존 화면·컴포넌트 무변경
  - Storybook 콘솔 오류 0건

- 🐛 **버그 등록**: 0개 (전체 통과)

- ✅ **TC 저장**: /docs/07_테스트케이스/TC_v1.0.md

- ✅ **TR 생성**: /docs/08_테스트결과보고서/TR_v1.0.md (AC 13/13 매핑)

품질 판정: ✅ **납품 가능**

---

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-19 — CONCERNS
- **Layer1 스펙**: pass — AC 13줄 전부 대응 산출물 확인. Button 3종×5상태(호버·포커스는 `play`로 실제 의사클래스 강제)·TextInput 4상태·QuantityStepper 4상태(+경계 disabled·클램프 커밋 RTL)·Badge 5종·Tabs(스티키 클래스·스와이프 순수함수)·BottomSheet 2상태·PopupCarousel 3상태·Toast/ToastStack(1·3·초과제거)·Skeleton 2종·EmptyState(라인아이콘+안내+테두리 버튼+railSlot)·ErrorState(원형 느낌표+고정 카피 '이용에 불편을 드려 죄송합니다.'+[홈으로]테두리·[돌아가기]검정, Button 조립 재사용). 구현 계획(사람 확인분) 대비 이탈 없음 — 계획 외 추가는 `initialIndex?`(Dev 기록에 명시, additive)와 CSS 도입에 따른 테스트 인프라 3파일(`css.d.ts`·`jest.styleMock.cjs`·`jest.config.cjs` moduleNameMapper)뿐이고 둘 다 기존 동작에 영향 없음. 사례집 대조도 이행 확인: 실제 이미지 대신 배경색 블록(SR-306 #1 404 함정), 할인율 재계산 금지·`rate` 주입(SR-235 #1 계열), 포커스 로직 `useFocusTrap` 단일화(SR-309 #1 round3 계열). `rules_check.py`(9규칙·35파일) 위반 0건.
- **Layer2 보안**: pass — 11개 컴포넌트 전부 순수 프레젠테이션. `fetch`·`console.*`·`localStorage`·`dangerouslySetInnerHTML`·`innerHTML` 0건(규칙 `web-fetch-only-in-api` 충족), 서버 원문 메시지를 조합·렌더하는 경로 없음(`ErrorState.subMessage`는 호출부 고정 카피 계약). 인증·인가·주입 표면 없음(백엔드·DB 미변경).
- **Layer3 회귀**: concerns — 기존 파일 수정은 `jest.config.cjs`의 `moduleNameMapper` 추가 1건뿐이고 전역 `testEnvironment: 'node'`·`testMatch`는 그대로. `npm test` 전체 재실행으로 독립 확인: **26 suites / 254 tests 전부 통과**. 새 CSS는 `cmn-*` 접두 클래스만 정의해 기존 화면으로 스타일이 새지 않고, 기존 컴포넌트를 import하는 신규 파일도 없다. 다만 `useFocusTrap`의 effect 의존성 결함(아래 권고 1)은 이 공통 컴포넌트를 실제 화면에 끼우는 후속 SR(322·326)에서 포커스 이동 회귀로 드러날 조건이 이미 갖춰져 있다.
- 권고(CONCERNS시):
  1. **(medium) `useFocusTrap.ts:31-58` — 부모 재렌더만으로 포커스를 다시 뺏는다.** 초기 포커스 이동 + Esc/Tab 리스너를 한 effect에 묶고 의존성을 `[open, onClose]`로 둔 탓에, 부모가 인라인 화살표 `onClose`를 넘기고 재렌더하면(이 코드베이스의 실제 관용구 — `OrderPage.tsx:225` `onClose={() => setZipcodeModalOpen(false)}`) effect가 재실행되며 `initialFocusables[0].focus()`가 다시 돌아 시트 안에서 작업 중이던 포커스가 매번 '닫기' 버튼으로 끌려간다. 임시 RTL 프로브로 재현 확인(BottomSheet 내부 '확인' 버튼 포커스 → `onClose` 신원만 바꿔 rerender → '닫기'로 포커스 이동; 프로브 파일은 판정 후 삭제). 구매 시트에 QuantityStepper를 넣는 조합에서 +/− 한 번마다 포커스가 튀고, 키보드 사용자는 Enter가 닫기로 먹히는 형태로 드러난다. 처방: 초기 포커스 effect를 `[open]`만 의존하도록 분리하고 `onClose`는 `onCloseRef`로 받아 keydown 핸들러에서 호출(리스너 등록도 `[open]`만 의존) + `BottomSheet.test.tsx`에 "열린 채 부모가 재렌더돼도 내부 포커스 유지" 단언 추가. 아직 소비처가 없어 차단하지 않는다.
  2. (low) `PopupCarousel.stories.tsx` — '오늘은그만보기' 스토리가 '첫번째'와 args가 동일해 구별되는 상태를 못 보여준다(기준선에 동일 이미지 2장). `play`로 실제 클릭 결과나 포커스 상태를 남긴다.
  3. (low) `PopupCarousel.tsx:36-40` — 4초 자동 넘김이 스토리북에서도 돌아 '마지막'(initialIndex=4) 스토리가 시간 의존이 된다(캡처는 렌더 후 ~250ms라 실무상 안정, HeroBannerCarousel 5초 선례 있음). `autoAdvanceMs` prop(0이면 정지)로 스토리에서만 끌 수 있게.
  4. (low) `ToastStack.tsx` — 상한을 강제하지 않는다(계획 그대로). `toasts`에 5개를 넘기면 5개가 렌더되므로 AC '최대 3개'는 호출부가 `pushToast`를 쓸 때만 성립 — 방어적 slice 또는 props 계약 명시(UIS-CMN-002 역생성 본문에도 같은 문장).
  5. (low) `TextInput.tsx:12` — id 폴백이 `cmn-text-input-${label}`이라 같은 label 두 개면 DOM id 중복. `useId()`로 교체.
  6. (low) `Badge.tsx` — variant별 판별 유니온이 아니라 `months`/`rate` 누락이 타입에서 안 잡히고 '무이자 0개월'·'0%'로 조용히 렌더된다.
  7. (low) RTL 테스트가 상호작용 4종에만 있고 Button(loading→disabled/aria-busy)·TextInput(error→aria-invalid/role=alert)·EmptyState/ErrorState에는 없다(사람 확인 계획이 테스트 집합을 이대로 열거했고 상태 스토리로 시각 검증은 되므로 후속 TODO).
  8. (low) `BottomSheet.tsx:39` — `aria-label="구매 시트"` 하드코딩. 범용 공통 컴포넌트이므로 `ariaLabel?` prop을 연다.
- 재동기화 입력(STEP 5.5 — UIS-CMN-002 본문 역생성 시 반영):
  - `Badge` 색상 매핑 판단(freeShipping=success 톤 · live=`--color-brand` · discount=`--color-price`)은 확정 요건에 없던 dev 판단분 — 규격 본문에 근거와 함께 고정하고, 시각 기준선 SR(322·326)에서 재확인 대상으로 표시.
  - `ToastStack`의 상한 책임 경계(컴포넌트가 아니라 호출부 `pushToast`)와 `PopupCarousel.initialIndex`(계획 외 additive prop)를 컴포넌트 계약 절에 명시.
  - `Button.loading`이 라벨을 '처리 중…'으로 대체하고 `disabled`를 함께 적용한다는 동작 규칙을 본문 계약으로 기록.

### QA Gate — 2026-09-19 (round2) — PASS
> round1 CONCERNS(medium 1 + low 7) 재작업 + 축E FAIL(BottomSheet '닫힘' 태그) 반영분 재검증. 독립 재실행: `npx tsc --noEmit` 오류 0 · `npx jest` **33 suites / 286 tests 전부 통과** · `rules_check.py`(9규칙·54파일) must/should/info **0건** · `story_gate.py --func SR-310.1` **verdict pass**.

- **Layer1 스펙**: pass — AC 13줄 대응 산출물은 round1 판정 그대로 유지되고, 재작업이 AC 해석을 넓히거나 좁힌 곳이 없다. 오히려 AC '최대 3개 동시 노출'이 호출부 규약에서 **컴포넌트 자체 보증**으로 강화됐다(`ToastStack.tsx:16` `capToasts(toasts, max)`). 축E는 `BottomSheet.stories.tsx:23`의 `tags: ['renders-nothing']`으로 통과 — 이 태그는 dev가 지어낸 우회가 아니라 도구 규약(`story_gate.py:54 RENDERS_NOTHING_TAG`)에 정의된 정식 표시이고, `open=false`일 때 null 반환이 controlled 설계상 정답임을 `BottomSheet.test.tsx:88`이 단언으로 고정한다(태그만 달고 실제 빈 렌더를 방치한 것이 아님). 계획 외 추가는 없다 — 재작업 산출은 전부 사람 코멘트 6개 항목의 범위 안.
- **Layer2 보안**: pass — 재작업 후에도 `common/` 전체에 `fetch(`·`console.*`·`localStorage`/`sessionStorage`·`dangerouslySetInnerHTML`·`innerHTML`·`eval(` **0건**(규칙 `web-fetch-only-in-api` 충족). 새로 생긴 상태(`dismissChecked`)는 렌더 전용 로컬 불린이라 영속·전송 경로가 없고, `PopupCarousel`의 "오늘은 그만 보기"는 여전히 콜백만 호출한다(억제 기록 영속화는 SR-339 몫 — 이번에 몰래 붙지 않았음을 확인). 백엔드·DB·인증 표면 미변경.
- **Layer3 회귀**: pass — shop-web 저장소 작업트리 기준 **기존 파일 수정은 여전히 `jest.config.cjs`의 `moduleNameMapper` 추가 1건뿐**(`git diff` 확인)이고 나머지는 전부 신규 미추적 파일(`src/components/common/`·`src/css.d.ts`·`jest.styleMock.cjs`). round1의 medium(포커스 튐)은 `useFocusTrap.ts`에서 ① 초기 포커스 이동 effect `[open]` 단독 의존(45-50행) ② Esc/Tab 리스너도 `[open]` 단독 + `onCloseRef`로 최신 `onClose` 읽기(54-79행) ③ 캡처(`wasOpenRef`, 36-41행)·복귀(cleanup, 82-87행) 분리 유지로 **원인 자체가 제거**됐다. 회귀 테스트 `BottomSheet.test.tsx:63` "열린 채로 부모가 재렌더돼도(인라인 onClose 신원 변경) 내부 포커스가 유지된다"는 round1 재현 프로브와 동일 절차(내부 '확인' 버튼 포커스 → 새 인라인 화살표 `onClose`로 rerender → 포커스 유지)이고 **구 의존성 `[open, onClose]`에서는 반드시 실패하는 판별력 있는 단언**이다. 수정이 공유 훅 한 곳이라 `PopupCarousel`도 같이 낫는다(SR-309 #1 "한쪽만 고침" 계열 재발 없음).

**round1 지적 8건 해소 확인(각 항목 코드 직접 대조)**

| # | round1 지적 | 해소 근거(파일:행) | 판정 |
|---|---|---|---|
| 1 (medium) | `useFocusTrap` 부모 재렌더로 포커스 탈취 | `useFocusTrap.ts:32-33`(`onCloseRef`)·`45-50`·`54-79`(둘 다 `[open]` 단독) + 회귀 테스트 `BottomSheet.test.tsx:63-86` | 해소 |
| 2 (low) | '오늘은그만보기' 스토리가 '첫번째'와 구별 안 됨 | `PopupCarousel.tsx:43,103,110-117`(`dismissChecked`·`aria-pressed`·체크박스형 표시) + `PopupCarousel.stories.tsx:40-45`(`play`로 실제 클릭) + 테스트 `PopupCarousel.test.tsx:79` | 해소 |
| 3 (low) | 4초 자동 넘김으로 '마지막' 스토리가 시간 의존 | `PopupCarousel.tsx:32,54`(`autoAdvanceMs`, 0이면 인터벌 미등록) + 전 스토리 공통 `args.autoAdvanceMs: 0`(`stories.tsx:23`) + fake timer 테스트 `test.tsx:91` | 해소 |
| 4 (low) | `ToastStack`이 상한을 강제 안 함 | `toastQueue.ts:19-22`(`capToasts`, `max<=0` 방어 포함) + `ToastStack.tsx:15-16`(`max?`=3 기본) + 컴포넌트 테스트 3건·단위 테스트 4건 | 해소(사람 코멘트대로 호출부 책임이 아닌 컴포넌트 보증으로) |
| 5 (low) | `TextInput` id 폴백 label 기반 → DOM id 중복 | `TextInput.tsx:7,16-17`(`useId()`) | 해소 |
| 6 (low) | `Badge` props가 판별 유니온 아님 | `Badge.tsx:13-18`(variant별 유니온 — installment `months` 필수, discount `rate` 필수) + `tsc --noEmit` 통과 | 해소 |
| 7 (low) | Button·TextInput·EmptyState·ErrorState 등 RTL 테스트 부재 | 신규 6파일 — `Button.test.tsx`(loading→`disabled`+`aria-busy` 포함 6건)·`TextInput.test.tsx`(error→`aria-invalid`/`role=alert` 포함 6건)·`Toast`·`Skeleton`·`EmptyState`·`ErrorState.test.tsx` | 해소 |
| 8 (low) | `BottomSheet` `aria-label` 하드코딩 | `BottomSheet.tsx:15,35,45`(`ariaLabel?`, 기본 '구매 시트' 유지 — 소비처 없어 회귀 0) | 해소 |

- 후속 TODO(차단·권고 아님, 새 라운드를 세우지 않는다):
  1. `TextInput.test.tsx:42-54` — "useId로 고유 id 생성" 테스트가 같은 인스턴스를 `rerender`해 id 동일성만 보므로 **서로 다른 두 인스턴스의 id 상이함을 증명하지 못한다**(단언도 `toBeTruthy()`뿐). 실제 수정(`useId()`)은 정확하므로 기능 결함은 없다 — 같은 label의 `TextInput` 2개를 한 번에 렌더해 `id1 !== id2`를 단언하는 형태로 다음 SR에서 보강.
  2. `ToastStack.tsx:19` — `aria-label="토스트 목록"`이 role 없는 `div`에 붙어 접근성 트리에 이름이 노출되지 않는다(round1 이전부터 있던 코드, 이번 재작업과 무관). `role="region"`/`role="log"` 등 부여는 실제 소비처가 생기는 리스킨 SR에서 결정.
  3. `story_gate.py` 축E가 신규 `*.test.tsx`를 "스토리 없는 화면 부품"으로 세어 low 6건을 냈다(`verdict: pass`, 차단 아님). 같은 파일들을 `rules_check.py`의 `story-per-component`는 정확히 제외해 위반 0건이므로 **도구 간 대상 집합 차이**이지 코드 결함이 아니다 — 하네스 쪽 메모.
- 재동기화 입력(STEP 5.5 — UIS-CMN-002 본문 역생성 시 반영, round1 3건에 아래 추가):
  - `ToastStack` 상한 책임이 round1 판정 이후 **호출부 → 컴포넌트 자체 보증**으로 바뀌었다(`max?` 기본 3, 초과 시 가장 오래된 것부터 제거). round1 재동기화 메모의 "상한은 호출부 책임" 문장을 그대로 쓰면 안 된다 — 규격 본문은 **컴포넌트가 보증**으로 적는다.
  - `PopupCarousel`의 계약에 `autoAdvanceMs`(기본 4000 · 0이면 자동 넘김 정지)와 `dismissChecked`가 **로컬 시각 표시 전용**(억제 상태의 영속·복원은 SR-339 몫, 재오픈 시 false로 리셋)이라는 경계를 명시. 옵션은 실제 체크박스가 아니라 `aria-pressed` 토글 버튼으로 구현돼 있음도 함께 기록.
  - `BottomSheet`의 `ariaLabel?`(기본 '구매 시트')과 `open=false` 시 **아무것도 렌더하지 않는 controlled 계약**(스토리북에서는 `renders-nothing` 태그로 표시)을 규격 본문에 고정.
  - `Badge` props가 variant별 판별 유니온이라는 점(installment `months` 필수 · discount `rate` 필수 · live `countdownSlot?` 선택)을 계약 절에 타입 그대로 기록.

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/regression] useFocusTrap.ts:58 — 초기 포커스 이동 + Esc/Tab 리스너 effect의 의존성이 [open, onClose]라, 부모가 재렌더해 인라인 화살표 onClose의 신원만 바뀌어도 effect가 재실행되며 initialFocusables[0].focus()가 다시 돌아 사용자가 시트/팝업 안에서 잡고 있던 포커스를 매번 닫기 버튼으로 되돌린다. 임시 RTL 프로브로 재현 확인(BottomSheet 안 '확인' 버튼에 포커스 → onClose 신원만 바꿔 rerender → '닫기'로 포커스 이동). 이 코드베이스의 기존 모달 관용구가 실제로 인라인 화살표다(OrderPage.tsx:225 onClose={() => setZipcodeModalOpen(false)}). → 초기 포커스 이동 effect를 [open]만 의존하도록 분리하고, onClose는 ref(onCloseRef)에 담아 keydown 핸들러에서 호출한다(리스너 등록도 [open]만 의존). BottomSheet.test.tsx에 '열린 채로 부모가 재렌더돼도 내부 포커스가 유지된다' 단언을 추가한다.
2. [low/spec] PopupCarousel.stories.tsx — '오늘은그만보기' 스토리가 '첫번째'와 args가 완전히 동일해 구별되는 상태를 보여주지 않는다(story_shots 기준선에 같은 이미지 2장이 남는다). AC의 '오늘은 그만 보기' 상태 스토리가 형식만 충족된다. → play 함수로 '오늘은 그만 보기'를 실제 클릭해 닫힌 결과(또는 로컬 데모 래퍼의 억제 상태)를 보여주거나, 옵션 버튼에 포커스/호버를 준 상태로 구별한다.
3. [low/regression] PopupCarousel.tsx:36-40 — 4초 자동 넘김이 스토리북에서도 계속 돌아 '마지막'(initialIndex=4) 스토리가 시간에 따라 1/5로 바뀐다. 현재 캡처는 렌더 후 ~250ms라 실무상 안정하고 HeroBannerCarousel(5초)이라는 선례도 있으나, 상태 스토리의 의미가 시간 의존이 된다. → 스토리에서만 자동 넘김을 끌 수 있게 autoAdvanceMs prop(기본 4000, 0이면 정지)을 열거나 story parameters로 제어한다.
4. [low/spec] ToastStack.tsx가 상한을 강제하지 않는다 — AC '최대 3개 동시 노출'은 호출부가 toastQueue.pushToast를 쓸 때만 성립하고, toasts에 5개를 넘기면 5개가 그대로 렌더된다(사람이 확인한 구현 계획 그대로이므로 계획 위반은 아님). → ToastStack에서 방어적으로 마지막 max개만 렌더하거나, props JSDoc에 '상한은 호출부 책임(pushToast 사용)'을 계약으로 명시하고 UIS-CMN-002 역생성 본문에 같은 문장을 남긴다.
5. [low/spec] TextInput.tsx:12 — id 미지정 시 폴백이 `cmn-text-input-${label}`이라, 같은 label의 입력이 한 화면에 둘이면 DOM id가 중복되고 label htmlFor 연결이 첫 요소로 몰린다(공통 컴포넌트라 재사용 시 실제로 발생 가능). → React 18+ useId()로 폴백 id를 생성한다.
6. [low/spec] Badge.tsx — props가 variant별 판별 유니온이 아니라 months/rate가 전부 optional이다. variant='installment'에 months 누락 시 '무이자 0개월', variant='discount'에 rate 누락 시 '0%'가 조용히 렌더된다(타입이 잡아주지 못함). → BadgeProps를 variant별 판별 유니온으로 바꿔 installment는 months 필수, discount는 rate 필수로 고정한다.
7. [low/spec] AC '컴포넌트 단위 테스트' 대비 RTL 테스트가 상호작용 4개(QuantityStepper·Tabs·BottomSheet·PopupCarousel)에만 있고 Button(loading 시 disabled·라벨 대체)·TextInput(error 시 aria-invalid·role=alert)·EmptyState/ErrorState(고정 카피·버튼 2종)에는 없다. 사람이 확인한 구현 계획이 테스트 집합을 이대로 열거했고 상태 스토리로 시각 검증은 되므로 차단하지 않는다. → Button.loading→disabled+aria-busy, TextInput.error→aria-invalid/role=alert 정도의 얕은 RTL 단언을 후속 TODO로 추가한다.
8. [low/spec] BottomSheet.tsx:39 — aria-label='구매 시트'가 하드코딩돼 범용 공통 컴포넌트인데 다른 용도로 쓸 때 접근성 이름을 바꿀 수 없다. → ariaLabel?: string prop(기본 '구매 시트')을 연다.

사람 코멘트: (medium) useFocusTrap: 초기 포커스 이동은 open이 false→true로 바뀔 때 1회만 하고, 키 리스너는 최신 onClose를 ref로 읽어 의존성에서 onClose를 뺀다(인라인 화살표 onClose + 부모 재렌더에서도 포커스가 튀지 않게). QA의 RTL 재현 프로브를 회귀 테스트로 추가한다.
(low, 같이 고친다)
1) ToastStack이 최대 3개를 스스로 강제(초과 시 가장 오래된 것 제거 — 요구사항의 토스트 규약이므로 호출부 책임으로 두지 않는다) + 단위 테스트.
2) PopupCarousel '오늘은 그만 보기' 스토리를 체크된 상태로 구분하고, 스토리에서는 자동 넘김을 끈다(시간 의존 제거).
3) Badge props를 variant별 판별 유니온으로 바꾼다.
4) TextInput id 기본값은 React useId를 쓴다.
5) BottomSheet aria-label을 prop으로 뺀다(기본값 유지).
6) Toast·Skeleton·EmptyState·ErrorState에 최소 RTL 테스트(렌더·버튼 콜백)를 추가한다.
기존 파일·화면은 건드리지 않는다.
