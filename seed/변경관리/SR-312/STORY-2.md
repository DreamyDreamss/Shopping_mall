---
story-id: STORY-SR-312.2
item: SR-312.2
title: 카드 v2 교체 — 홈·상품 목록·상세 관련 상품 레일
status: Approved
domain: order
created: 2026-09-19
spec_markers: 0
sr-id: SR-312
approved_sha: 13ef15cea9f5
---

# STORY-SR-312.2 — 상품 카드·가격·배지 표기 규격 — KT알파쇼핑 카드 — 카드 v2 교체 — 홈·상품 목록·상세 관련 상품 레일

## Story
상품 카드·가격·배지 표기 규격 — KT알파쇼핑 카드 — 카드 v2 교체 — 홈·상품 목록·상세 관련 상품 레일


## 변경 컨텍스트 (SR-312)
> 이 story는 변경요청 **SR-312 — 상품 카드·가격·배지 표기 규격 — KT알파쇼핑 카드** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-312/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-312/02_변경명세.md`

### 확정된 요건 문답 6건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 요구사항 '범위' 절 그대로 — 포함: shop-web ProductCard v2 한 컴포넌트(변형 grid·rail·ranking), 가격 규칙(할인율 = floor((정가-판매가)/정가×100), 정가 없거나 판매가 이상이면 할인 표기 없음 — SR-306 결정 유지), 배지 줄은 값 있는 배지만(TV상품·무료 배송·무이자 N — 데이터가 없으면 줄 자체 숨김), 별점·상품평 수는 데이터가 없으면 숨김(가짜 값 금지 — SR-304), 범위가('82,900원~', 옵션 가격 폭이 있을 때만), 순위 배지(ranking 변형), 품절 흐림+'품절' 배지(SR-216·230 문구), 이미지 lazy·대체 영역. 홈(ShopHomePage)·상품 목록(ProductListPage)·관련 상품 레일의 카드를 v2로 교체. SR-309 토큰·SR-310 Badge 사용. 제외: 배지 원천 데이터(SR-318·308·268), 좋아요 동작(SR-243), 리뷰 데이터(SR-246), 나의 최대 혜택가(SR-345), 백엔드.
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 홈·목록·상세의 상품 클릭 이동, 장바구니 담기, 검색·카테고리 필터, 정렬·페이지 동작과 기존 테스트 불변. 가격·할인 계산 결과(SR-306)와 품절 문구(SR-216·230) 불변 — 모양만 바뀐다.
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 대상 화면: 쇼핑 홈(UIS-ORD-008)·상품 목록(UIS-ORD-009)·상품 상세의 관련 상품 레일(UIS-ORD-010) — 카드 교체만. 새 공통 UIS: UIS-CMN-004 상품 카드 규격.
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 카드 단위: 이미지 없음 → 회색 대체 영역+상품명 첫 글자 없이 빈 썸네일 틀, 가격 정보 없음은 발생하지 않음(판매가 필수). 목록 0건 표기는 각 화면 기존 문구 유지(EmptyState 교체는 리스킨 SR).
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — ProductCard v2 스토리: grid 기본·할인·정가 없음·품절·배지 3종 모두·배지 없음·별점 있음/없음·긴 상품명 2줄 말줄임·이미지 없음 / rail 기본 / ranking 1위·10위 / 2열 그리드 행 높이 정렬(배지 유무 섞인 4개).
- **이 화면에 어디서 들어오는가? (진입점이 이번 범위에 포함되는가)** — 기존 화면 진입 그대로(홈·상품 목록·상품 상세) — 새 진입점 없음.

### 같은 SR의 다른 항목 — 먼저 끝난 항목이 만든 것을 다시 만들지 않는다
- **SR-312 #1** 상품 카드 규격 — Approved

## 🖼 화면 상태 (스토리) — 깨뜨리지 말 것
> 이 기능의 화면에 **이미 있는 상태 55건**이다(대상 스토리북, `.speclinker/storybook_index.json`). 게이트 축 E(`story_gate.py`)가 이것들을 실제로 렌더해 깨진 것을 잡고, 고친 부품에 스토리가 없으면 알린다(차단은 하네스 규칙 `story-per-component`가 채택돼 있을 때 축 C가 한다). 새로 만든 상태(빈 목록·오류·권한 없음 등)는 스토리로 추가하라.
- **UIS-ORD-008 · 카테고리 숏컷** (`./src/features/shop/CategoryShortcuts.stories.tsx`)
  - 기본
- **UIS-ORD-008 · GNB** (`./src/features/shop/Gnb.stories.tsx`)
  - 비로그인
  - 로그인
  - 장바구니담김
- **UIS-ORD-008 · 히어로 배너** (`./src/features/shop/HeroBannerCarousel.stories.tsx`)
  - 여러장
  - 한장
- **UIS-ORD-008 · 상품 카드** (`./src/features/shop/ProductCard.stories.tsx`)
  - 기본
  - 할인
  - 품절
  - 이미지없음
  - 정가없음
  - 이미지있음
  - 할인0퍼센트
  - 이미지로드실패
- **UIS-ORD-008 · 추천 상품 그리드** (`./src/features/shop/ProductGrid.stories.tsx`)
  - 목록있음
  - 빈목록
  - 로딩
  - 조회실패
- **UIS-ORD-008 · 랭킹** (`./src/features/shop/RankingSection.stories.tsx`)
  - 인기탭
  - 가격탭전환
- **UIS-ORD-008 · 최근 본 상품** (`./src/features/shop/RecentlyViewed.stories.tsx`)
  - 있음
  - 없음
- **UIS-ORD-008 · 푸터** (`./src/features/shop/ShopFooter.stories.tsx`)
  - 기본
- **UIS-ORD-009 · 상품 목록 필터바** (`./src/features/shop/ProductFilterBar.stories.tsx`)
  - 기본
  - 정렬변경
  - 재고필터켬
- **UIS-ORD-009 · 상품 목록 그리드** (`./src/features/shop/ProductListGrid.stories.tsx`)
  - 결과있음
  - 결과없음 검색어
  - 결과없음 필터
  - 로딩
  - 조회실패
- **UIS-ORD-009 · 상품 목록 페이지네이션** (`./src/features/shop/ProductPagination.stories.tsx`)
  - 기본
  - 마지막페이지
  - 첫페이지
- **UIS-ORD-010 · 로딩 스켈레톤** (`./src/features/shop/ProductDetailSkeleton.stories.tsx`)
  - 로딩
- **UIS-ORD-010 · 상세 탭** (`./src/features/shop/ProductDetailTabs.stories.tsx`)
  - 기본
  - 상품평
  - 상품문의
  - 탭전환
- **UIS-ORD-010 · 이미지 갤러리** (`./src/features/shop/ProductImageGallery.stories.tsx`)
  - 기본
  - 이미지없음
  - 로드실패
- **UIS-ORD-010 · 상품 정보 패널** (`./src/features/shop/ProductInfoPanel.stories.tsx`)
  - 기본
  - 할인있음
  - 품절
  - 담기실패
  - 담기중
  - 담기성공
  - 바로구매성공
  - 로그인필요
- **UIS-ORD-010 · 조회 실패 안내** (`./src/features/shop/ProductNotFoundNotice.stories.tsx`)
  - 상품없음
  - 조회실패
- **UIS-ORD-010 · 상품정보제공고시** (`./src/features/shop/ProductNoticeTable.stories.tsx`)
  - 기본
- **UIS-ORD-010 · 함께 보면 좋은 상품** (`./src/features/shop/RelatedProducts.stories.tsx`)
  - 기본
  - 비어있음

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-312/02_변경명세.md`에서 도출)
- [ ] UIS-ORD-008: 추천 상품(`ProductGrid.tsx`)의 카드가 `ProductCard` v2 `grid` 변형으로 교체 — 배지 줄·별점/상품평 수·범위가 요소 추가(값 있을 때만), 이미지 대체 영역이 이니셜 → 빈 회색 틀로 변경.
- [ ] UIS-ORD-008: 랭킹 섹션(`RankingSection.tsx`) 카드에 순위 배지가 필요하다는 점은 요구사항 범위("순위 배지(ranking 변형)")로 확정돼 있으나, 이 변형을 랭킹 섹션이 소비한다는 화면별 매핑은 확정 문답에 명시적 문장으로 나오지 않음 — **[미상, 확인 필요]**: `RankingSection.tsx`가 `ProductCard` v2 `ranking` 변형으로 교체되는지, 아니면 `ranking` 변형이 다른 화면(예: 목록 화면 정렬 결과) 용도인지는 STORY 단계에서 화면 담당자 확인 필요.
- [ ] UIS-ORD-008: §4 호출 API·§3 검증 규칙(정가·할인율 계산, 최근 본 상품 중복 제거 등)은 불변 — 값 계산 로직 변경 없음, 렌더링 컴포넌트만 교체.
- [ ] UIS-ORD-009: `ProductListGrid.tsx`의 카드가 `ProductCard` v2 `grid` 변형으로 교체(홈과 동일 변형 공유). §3의 파생 순서(서버 응답 → 가격대 필터 → 정렬 → 페이지 slice)·결과 개수 텍스트 산정 시점·§5의 스켈레톤 표시 상태 로직은 카드 내부 구성과 무관하므로 불변.
- [ ] UIS-ORD-010: `RelatedProducts.tsx`의 카드가 `ProductCard` v2 `rail` 변형으로 교체(가로 스크롤 레일).
- [ ] UIS-ORD-010: 후보 산출 로직(`relatedProductsPicker.ts`, 현재 상품 제외 앞 8개, 후보 0개 시 섹션 자체 숨김)은 불변 — 카드 렌더만 교체.

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**
- [ ] 기능이 설명대로 동작(연결 INF 없음 — 수용기준 수동 작성 권장)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS** UIS-ORD-008: UIS-ORD-008: shop / > [반영: SR-302.1] — 코드(라우트) 기준 재동기화 골격. 화면 구성·상태·검증 규칙은 보강 대상 / 1. 화면 개요 / - 라우트: `/shop` (spa-route, `modules/shop-web/src/App.tsx:93`)
- **UIS** UIS-ORD-009: UIS-ORD-009: shop_products / > [반영: SR-303.1] — 상품 목록(검색·필터) 화면 신규. 홈(`UIS-ORD-008`)의 GNB 검색·카테고리 숏컷에서 진입. / 1. 화면 개요 / - 라우트: `/shop/products` (spa-route, `modules/shop-web/src/App.tsx:97`)
- **UIS** UIS-ORD-010: UIS-ORD-010: shop_products_상세 / > **근거 소스(권위):** `modules/shop-web/src/pages/ProductDetailPage.tsx` 외 `features/shop/*`(SR-304.1 / > 신규 구현). DOM 스냅샷 없음(스토리북/앱 미기동, 소스폴백 모드) — 소스 슬라이스 + STORY-1.md / > (확정 답변·QA 결과) + `.speclinker/storybook_index.json`의 스토리 21건을 근거로 작성.
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)

## 📏 적용 규칙 (JIT — .claude/rules)
> 이 FUNC가 건드리는 파일에 적용되는 프로젝트 규칙이다. 전문은 아래 파일을 **직접 Read**하고 지킬 것 — `must` 위반은 STEP 5.3 축 C(`rules_check.py`)가 차단한다. 정본: 워크스페이스 `.claude/rules/`(뷰어 [rules]에서 편집).

| 규칙 | severity | 파일 |
|---|---|---|
| 파일 크기 상한 | should | `.claude/rules/lab/file-size-cap.md` |
| 부품마다 상태 스토리 | must | `.claude/rules/lab/story-per-component.md` |
| API 호출은 `src/api.ts` 한 곳 | must | `.claude/rules/lab/web-fetch-only-in-api.md` |

## 📖 도메인 용어 정본 (JIT — 용어집)
> 같은 대상을 새 코드명으로 만들지 말 것 — 아래가 이 도메인의 정본 용어다. 전체·확정 근거: `docs/viewer/glossary.json`(뷰어 [용어집]).
| 용어 | 정본 코드 | 정의 |
|------|----------|------|
| 가용 재고 | `STOCK_QTY` | 가용 재고 |
| 배송번호 | `DELIVERY_NO` | 배송번호 |
| 상태 | `ORDER_STATE` | 상태 (PLACED/PAID/SHIPPED/PARTIAL_SHIPPED/CANCELED/DONE) |
| 상품 SKU | `SKU` | 상품 SKU |
| 상품명 | `PRODUCT_NAME` | 상품명 |
| 주문번호 | `ORDER_NO` | 주문번호 (yyyymmdd+seq) |
| 주문일시 | `ORDERED_AT` | 주문일시 |
| 출고일시 | `SHIPPED_AT` | 출고일시 |
- ⚠ **논리 삭제**: 정본 미확정(충돌) — 코드 DEL_YN가 다른 용어(탈퇴 여부)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **수량**: 정본 미확정(충돌) — 코드 QTY가 다른 용어(장바구니 수량)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **장바구니 수량**: 정본 미확정(충돌) — 코드 QTY가 다른 용어(수량)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **주문 회원**: 정본 미확정(충돌) — 코드 MEMBER_ID가 다른 용어(회원 ID)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
(dev-agent가 생성 파일·주요 결정 기록)

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)
