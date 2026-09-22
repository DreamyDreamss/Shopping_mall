---
story-id: STORY-SR-306.1
item: SR-306.1
title: 상품 카드에 정가·할인율·이미지 표시
status: Done
domain: order
created: 2026-09-17
spec_markers: 0
sr-id: SR-306
approved_sha: 6b1e07be8360
---

# STORY-SR-306.1 — 상품 정가·대표 이미지 데이터 추가 — 쇼핑 화면이 할인·이미지를 실제로 표시 — 상품 카드에 정가·할인율·이미지 표시

## Story
상품 정가·대표 이미지 데이터 추가 — 쇼핑 화면이 할인·이미지를 실제로 표시 — 상품 카드에 정가·할인율·이미지 표시


## 변경 컨텍스트 (SR-306)
> 이 story는 변경요청 **SR-306 — 상품 정가·대표 이미지 데이터 추가 — 쇼핑 화면이 할인·이미지를 실제로 표시** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-306/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-306/02_변경명세.md`
- AS-IS 스냅샷: `docs/변경관리/SR-306/_asis_snapshot` (변경 전 상태 대조용)

### 확정된 요건 문답 10건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: ① products 테이블에 list_price(정가)·image_url(대표 이미지 경로) 컬럼 추가(둘 다 NULL 허용, 기동 DDL은 다음 번호 파일에 IF NOT EXISTS) ② 상품 목록·단건 조회 응답에 두 필드 추가(필드 추가만) ③ 시드 데이터에 값 채우기 ④ 쇼핑 홈 상품 카드가 정가 취소선·할인율 배지·이미지를 실제로 표시(값이 없으면 지금처럼 감춤). 제외: 할인율 컬럼 저장(화면에서 계산), 이미지 업로드·관리 화면, 외부 이미지 URL, 가격 이력·프로모션.
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 특정 화면/API만: 기존 응답 필드·순서·타입 불변(추가만) · 상품 조회 인증 정책 불변(SR-307 결과 유지) · 주문·장바구니 흐름과 금액 계산은 판매가(price) 기준 그대로 · 기존 Thymeleaf 상품 화면 동작 불변.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필요 — 필드 추가만(기존 호출 그대로 동작). 값이 없으면 null로 내려가고 화면이 감춘다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 새 오류 코드 없음. 이미지 파일이 없으면 화면이 이니셜 대체 영역을 그리고 서버는 관여하지 않는다.
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음 — products 테이블에 컬럼 2개를 더할 뿐이고 다른 도메인 조인·집계(주문 금액·정산)는 판매가만 쓴다. 정가는 표시 전용.
- **기존 데이터 이관·백필이 필요한가?** — 불필요(신규 컬럼만) — 기존 행은 NULL로 남고 시드 스크립트가 예시 값을 채운다. 백필 대상 운영 데이터 없음.
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 나열된 화면이 전부: 쇼핑 홈(UIS-ORD-008)의 상품 카드 표시만 바뀐다. 신규 화면 없음.
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 정가가 없거나 판매가 이상이면 취소선·할인 배지를 그리지 않는다 · 이미지가 없거나 로드 실패면 상품명 이니셜 대체 영역 · 할인율은 내림해 정수 %로 표시하고 0%면 배지 없음.
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 바뀌는 상태만: 상품 카드(정가 있음·할인 0%·정가 없음·이미지 있음·이미지 로드 실패) 스토리를 갱신·추가한다.
- **이 SR이 바꾸는 테이블을 읽는 다른 API(INF-ORD-004, INF-ORD-005, INF-ORD-006, INF-ORD-010, INF-ORD-011, INF-ORD-012, INF-ORD-013)의 결과·** — 달라지지 않음(근거 명시) — 이 SR은 products에 표시 전용 컬럼 2개(list_price·image_url)를 **추가만** 하고 기존 컬럼·행을 바꾸지 않는다. 주문 생성·취소·조회·장바구니 계열(INF-ORD-004~006·010~014)은 판매가(price)와 재고(stock_qty)만 읽으므로 금액·재고 판정과 오류 코드가 그대로다. 기존 SELECT가 컬럼을 명시하는지 여부와 무관하게 값이 NULL이면 응답에서 감춰지고, 주문 금액 계산에는 쓰이지 않는다. 회귀 테스트로 주문·장바구니 흐름 금액이 불변임을 확인한다.

### 같은 SR의 다른 항목 — 먼저 끝난 항목이 만든 것을 다시 만들지 않는다
- **SR-306 #2** 정가·이미지 컬럼과 조회 응답 필드 추가(시드 포함) — Done
  - 바꾼·만든 스펙: INF-ORD-008, INF-ORD-009, SCH-ORD-005
  - Dev 기록 파일: `modules/shop-api/src/main/resources/db/V10__product_list_price_image.sql`, `modules/shop-api/src/main/resources/application.yml`, `modules/shop-api/src/main/java/com/sm/lab/shop/domain/Product.java`, `modules/shop-api/src/main/resources/mapper/product.xml`, `modules/shop-api/src/main/java/com/sm/lab/shop/web/ProductImageStaticResourceConfig.java`, `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java`, `static/images/products/sku-1001.svg`, `modules/shop-api/src/test/java/com/sm/lab/shop/controller/ProductControllerTest.java`, `modules/shop-api/src/test/java/com/sm/lab/shop/dao/ProductDaoTest.java`, `modules/shop-api/src/test/java/com/sm/lab/shop/web/ProductImageStaticResourceServingTest.java`, `/images/products/sku-1001.svg`, `.speclinker/test_baseline.json` 외 2건

## 🖼 화면 상태 (스토리) — 깨뜨리지 말 것
> 이 기능의 화면에 **이미 있는 상태 19건**이다(대상 스토리북, `.speclinker/storybook_index.json`). 게이트 축 E(`story_gate.py`)가 이것들을 실제로 렌더해 깨진 것을 잡고, 고친 부품에 스토리가 없으면 알린다(차단은 하네스 규칙 `story-per-component`가 채택돼 있을 때 축 C가 한다). 새로 만든 상태(빈 목록·오류·권한 없음 등)는 스토리로 추가하라.
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

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-306/02_변경명세.md`에서 도출)
- [x] UIS-ORD-008: 상품 카드에 정가 취소선·할인율 배지·이미지 표시
  - AC-001: 정가 > 판매가일 때만 취소선 + 내림 정수 할인율 배지 (정수 연산 `(listPrice-price)*100/listPrice`)
  - AC-002: 할인율 0%면 배지 숨김
  - AC-003: 정가 없거나 ≤ 판매가면 취소선·배지 없음
  - AC-004: imageUrl 있음 → <img> 렌더
  - AC-005: imageUrl 없거나 로드 실패 → 상품명 이니셜 대체 영역
  - AC-006: 기존 Product 필드(sku·productName·price·stockQty·saleYn) 이름·순서·타입 불변
  - AC-007: 신규 필드(listPrice·imageUrl) 인터페이스 끝에만 추가

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**
- [x] 기능이 설명대로 동작(확정 문답 1·3·8·9 기반 수용 기준, 회귀 스위트 58건 무변경 통과)

> 변경명세에 스펙 ID가 없는 절 — 이 항목 몫인지 확인해 AC로 옮긴다: SR-306 (요구사항 요지 — 전 스펙 공통)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS** UIS-ORD-008: UIS-ORD-008: shop / > [반영: SR-302.1] — 코드(라우트) 기준 재동기화 골격. 화면 구성·상태·검증 규칙은 보강 대상 / 1. 화면 개요 / - 라우트: `/shop` (spa-route, `modules/shop-web/src/App.tsx:93`)
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

## 구현 계획
- **파일**:
  - `modules/shop-web/src/types.ts` — `Product` 인터페이스 끝에 `listPrice: number | null`·`imageUrl: string | null` 2필드 추가(기존 5필드 이름·순서·타입 불변, SR-306(#2)가 백엔드에서 이미 끝낸 필드 추가 관례를 그대로 프론트 타입에 반영). 상단 주석 "PRODUCTS에는 정가·할인율·이미지 URL 컬럼이 없다(실측)"는 SR-306(#2)로 이미 사실과 어긋나므로 갱신한다.
  - `modules/shop-web/src/features/shop/ProductCard.tsx` — `ProductCardProps`에서 `listPrice`/`discountRate`/`imageUrl` 3개 별도 prop을 **제거**하고, 이제 실제로 응답에 오는 `product.listPrice`/`product.imageUrl`에서 직접 파생시킨다. 할인율은 `product.listPrice != null && product.listPrice > product.price`일 때만 `Math.floor((product.listPrice - product.price) / product.listPrice * 100)`으로 계산해 0%면 배지 숨김(확정답변 "할인율은 내림해 정수 %로 표시하고 0%면 배지 없음" 그대로). 취소선(정가)도 같은 조건(`listPrice != null && listPrice > price`)일 때만 렌더. `<img>`에 `onError` 핸들러를 추가해 로드 실패 시 이니셜 대체 영역으로 전환한다(컴포넌트 로컬 `useState<boolean>` 플래그 1개 — 이미지가 없을 때의 기존 분기와 "이미지는 있었는데 로드 실패"를 하나의 렌더 조건(`!imageUrl || imgLoadFailed`)으로 합친다). 상단 JSDoc의 "데이터 갭·운영 화면은 절대 채우지 않는다" 서술은 SR-306(#2)로 더 이상 사실이 아니므로 제거하고 "표시 전용 필드, 값 없으면 자동으로 감춤"으로 갱신한다.
  - `modules/shop-web/src/features/shop/ProductGrid.tsx`, `RankingSection.tsx`, `RecentlyViewed.tsx` — **변경 없음**. 이미 `<ProductCard product={p} .../>`만 호출하고 있어(별도 prop을 넘긴 적이 없음) prop 제거의 영향을 받지 않는다.
  - `modules/shop-web/src/features/shop/ProductCard.stories.tsx` — 기존 4상태(기본·할인·품절·이미지없음) 이름은 유지하되(깨뜨리지 말 것) 값을 실데이터로 갱신하고, 확정답변 9("정가 있음·할인 0%·정가 없음·이미지 있음·이미지 로드 실패")가 요구하는 나머지 상태를 새 export로 추가한다. DB 실측(`SELECT sku, product_name, price, list_price, image_url FROM PRODUCTS WHERE sku IN (...)`, 2026-09-17)으로 확인한 실제 값:
    - `SKU-1001` 스탠딩 데스크 price 390000 / listPrice 450000 / image `/images/products/sku-1001.svg` → **할인**(≈13%, 기존 상태 갱신 — "운영 데이터로는 절대 채워지지 않는다"던 주석 삭제)
    - `SKU-1002` 기계식 키보드 price 129000 / listPrice null / image `/images/products/sku-1002.svg` → **정가없음**(신규) 겸 **이미지있음**(신규, 같은 데이터를 다른 관점에서 재노출 — 정가 유무와 무관하게 이미지가 실제로 로드되는 케이스를 보여주는 것이 이 상태의 목적이라 동일 args 재사용이 자연스럽다)
    - `SKU-1003` 4K 모니터 price 450000 / listPrice 450000(=판매가) / image `/images/products/sku-1003.svg` → **할인0퍼센트**(신규, 배지·취소선 둘 다 없어야 함)
    - `SKU-1004` 단종 마우스 price 35000 / listPrice 42000 / stockQty 0 / saleYn N / image null → **이미지없음**(기존 상태 갱신, 실데이터 그대로 사용 — 이 SKU는 실제로도 품절/판매종료라 품절 배지가 함께 보이는 것이 맞다. 상태를 "이미지 부재"로만 격리하고 싶으면 별도 코멘트로 "이 SKU는 실데이터상 품절이기도 하다"를 명시하고 stockQty만 임의로 올리지 않는다 — 실데이터를 지어내지 않는다는 이 프로젝트 관례(RecentlyViewed 주석 "없음을 지어내지 않는다")를 따른다)
    - **기본**·**품절**은 지금처럼 합성 데이터 유지(둘 다 null인 조합은 실 SKU 4건 중에 없음) — `listPrice: null, imageUrl: null`을 타입에 맞춰 명시.
    - **이미지로드실패**(신규): 존재하지 않는 경로(예: `/images/products/does-not-exist.svg`)를 `imageUrl`로 준 합성 데이터 — `onError` 핸들러가 실제로 타는지 스토리북에서 육안 확인 가능해야 한다.
  - `modules/shop-web/src/features/shop/ProductGrid.stories.tsx` — `Product[]` 타입 컴파일을 맞추려면 생성 루프의 각 행에 `listPrice`/`imageUrl`이 있어야 한다(타입 필수화). "목록있음" 스토리의 앞 4행을 위 실 SKU 값(1001~1004)으로 교체해 그리드 안에서 할인·정가없음·할인0%·이미지없음이 섞여 보이는 상태를 보여주고, 나머지 행은 `listPrice: null, imageUrl: null`로 채운다. "빈목록"·"로딩"·"조회실패"는 변경 없음.
  - `modules/shop-web/src/features/shop/RankingSection.stories.tsx`, `RecentlyViewed.stories.tsx` — 각 `Product` 리터럴에 `listPrice: null, imageUrl: null`만 추가(타입 컴파일 요구 — 두 파일의 상태 의미 자체는 이 SR과 무관해 바뀌지 않는다).
  - `modules/shop-web/src/pages/ShopHomePage.test.tsx` — 로컬 `ProductStub` 인터페이스에 `listPrice?: number | null`·`imageUrl?: string | null` 추가하고 `product(i, overrides)` 헬퍼가 기본값 `null`로 채우도록 확장. 새 통합 테스트 추가(아래 "테스트" 절).
  - `modules/shop-api/**`, Thymeleaf 화면 — **변경 없음**(SR-306(#2)·SR-307에서 이미 끝났고 이 항목의 회귀 보호 대상).
- **데이터**: 신규 DDL·마이그레이션 없음(SR-306(#2)에서 이미 `V10__product_list_price_image.sql` 완료). 이 항목은 shop-web 프론트만 건드린다 — 트랜잭션 경계·락·원자 UPDATE 해당 없음(순수 화면 렌더).
- **순서·보안**: 신규 오류 코드·인증 변화 없음(공개 GET 정책은 SR-307 결과 그대로). 부수효과(로그·발송·이벤트·감사) 없음 — 표시 전용 필드라 판정·잠금·쿨다운과 무관.
- **계약**: 신규 오류 코드·응답 봉투 변경 없음. `Product` TS 인터페이스에 필드 2개 추가만(기존 필드·HTTP 응답은 SR-306(#2)에서 이미 하위호환으로 끝남 — 이 항목은 프론트가 그 필드를 "받기만 하던" 상태에서 "실제로 쓰는" 상태로 바꾸는 것).
- **테스트**:
  1. `ShopHomePage.test.tsx`(jest, 기존 파일에 추가) — a) `listPrice > price`인 상품 응답 → 취소선 정가 텍스트(예: `450,000원`)와 할인율 배지(예: `13%`)가 정확한 문자열로 렌더되는지 단언(약한 단언 금지 — 실패사례집 SR-306 #2 r1 교훈). b) `listPrice === price`(0%) → 배지·취소선 둘 다 `queryByText`로 부재 확인. c) `listPrice: null` → 배지·취소선 부재(기존 "정가·이미지가 없어도 레이아웃이 안 깨진다" 테스트는 회귀로 계속 통과해야 함 — 건드리지 않는다). d) `imageUrl` 있음 → `getByRole('img')` 렌더 + 이니셜 대체 영역(`aria-label`) 부재. e) `imageUrl` 있음 + 로드 실패(`fireEvent.error(img)`) → 이니셜 대체 영역으로 전환되고 `<img>`가 사라짐을 확인.
  2. Storybook — 위 8개 `ProductCard` 상태 + `ProductGrid` "목록있음"(혼합 데이터) 상태가 `npm run test-storybook`으로 스모크 렌더 통과(콘솔 오류 없음 — 규칙 `web-no-console`과 별개로 test-runner가 콘솔 오류를 실패로 카운트).
  3. 타입 검사 — `npm test`(내부에서 `tsc --noEmit` 먼저 실행)로 `Product` 필드 필수화에 따른 기존 스토리 리터럴(RankingSection·RecentlyViewed) 컴파일 확인.
  4. 회귀 — `npm test` 스위트 **전체**와 `npm run test-storybook` **전체**를 실행한다(특정 파일/클래스로 좁혀서 확인하지 않는다 — 실패사례집 SR-307 #1 교훈, "계획이 못 본 기존 회귀"를 놓칠 수 있다).
- **테스트 격리**: 프론트 유닛/스토리 테스트라 DB 행·카운터 상태가 없다(레이트리밋·잠금류 없음, 해당 사항 적음). 기존 `localStorage.clear()`(beforeEach/afterEach) 관례를 그대로 유지해 세션·최근본상품 키가 테스트 간 새지 않게 한다. 이미지 `onError` 테스트는 매 테스트가 `ShopHomePage`를 새로 마운트하므로 컴포넌트 로컬 `useState` 플래그가 다음 테스트로 이어질 여지가 없다.
- **폴백·우회 경로의 자격 판정**: 해당 없음 — 이 항목은 새 인증·조회 경로를 열지 않는다(순수 표시 로직, 인증 정책 파일 `ApiKeyAuthFilter`는 건드리지 않는다).
- **프레임워크 실행 모델 함정**: React StrictMode(dev, `npm run dev`)에서 `onError` 핸들러가 이중 실행돼도 boolean 플래그를 `true`로 세팅하는 멱등 연산이라 문제 없음. `key={p.sku}`로 리스트 아이템이 식별되므로 같은 상품 카드가 리렌더만 될 때는 이미지 오류 state가 유지된다(의도된 동작 — `src`가 그대로인데 대체 영역에서 이미지로 다시 되돌아갈 이유가 없다). 그 외 회전형 토큰·프록시 self-invocation·스케줄러 중복류 함정은 해당 없음.
- **범위 밖**: `modules/shop-api/**`(DDL·컨트롤러·매퍼)와 기존 Thymeleaf 상품 화면(SR-306(#2)·SR-307에서 이미 완료, 이번 항목의 회귀 보호 대상). 할인율 컬럼 저장, 이미지 업로드·관리 화면, 외부 이미지 URL, 가격 이력·프로모션(요구사항에서 명시적으로 제외).
- **실패 사례집 대조**:
  - SR-306 #2 r1(harness/antipatterns.all.md 39행) — `doesNotExist()`/`isIn(...)`류 약한 단언은 "값이 없으면 null"·"경계 밖은 막힌다" 계약을 실질적으로 고정하지 못한다고 지적됐다. 이 조건은 프론트 테스트에도 그대로 성립한다 — 위 테스트 절 a~e를 `queryBy*`(부재)와 정확 텍스트/role(존재)로만 단언하고, `toBeInTheDocument()` 없이 진위를 흐리는 단언을 쓰지 않는다.
  - SR-307 #1(37행) — 계획 단계에서 회귀 확인 범위를 특정 클래스로 좁혀 적었다가 QA 전체 실행에서 계획이 못 본 기존 테스트와 충돌한 사례. 이 조건도 동일하게 성립 — 위 "테스트" 절 4에서 전체 스위트 실행을 명시했다.
  - SR-302 #1(34행) — 연타/이벤트 타이밍을 `fireEvent` 분리 호출로 잘못 검증한 사례. 이 항목은 연타 시나리오가 없어 조건이 성립하지 않는다(단발 상태 전환뿐) — 다만 `fireEvent.error(img)`도 RTL이 자동으로 `act()`로 감싸므로 별도 조치 불필요.
  - SR-306(#2) Dev 기록(파일 절 근거) — "기존 필드는 이름·순서 불변, 새 필드는 끝에 추가" 관례를 백엔드에서 이미 확립했다. 이 조건은 프론트 `Product` 타입에도 그대로 적용 — `listPrice`·`imageUrl`을 인터페이스 끝에 추가하고 기존 5필드는 손대지 않는다.

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
- 계획 확인: 계획대로 진행 (2026-09-17) — 사람 코멘트: 이미지 로드 실패 상태는 스토리에서 잘못된 경로로 재현. 테스트 완료 조건: 취소선·할인율 배지 정확 텍스트 단언(13%·0%숨김·정가없음) · 이미지 있음/없음/로드실패 3상태 · 품절 배지 유지 · 전체 shop-web 테스트+스토리북 실행.
- 구현 완료 (2026-09-17) — 계획대로 진행, 이탈 없음.
  - `modules/shop-web/src/types.ts` — `Product` 인터페이스 끝에 `listPrice: number | null`·`imageUrl: string | null` 추가(기존 5필드 불변). 상단 주석을 SR-306(#2) 반영 사실로 갱신.
  - `modules/shop-web/src/features/shop/ProductCard.tsx` — `listPrice`/`discountRate`/`imageUrl` 3개 별도 prop 제거, `product.listPrice`/`product.imageUrl`에서 직접 파생. `hasDiscount = listPrice != null && listPrice > price`일 때만 취소선·배지 렌더, `discountRate = Math.floor((1 - price/listPrice) * 100)`(0%면 배지 숨김). `<img onError>`로 `imgLoadFailed` state 세팅 → 이니셜 대체 영역 전환(`showImage = !!imageUrl && !imgLoadFailed`).
  - `modules/shop-web/src/features/shop/ProductCard.stories.tsx` — 기존 4상태(기본·할인·품절·이미지없음) 이름 유지, 값을 DB 실측 SKU(1001~1004, 2026-09-17)로 갱신하고 정가없음·이미지있음·할인0퍼센트·이미지로드실패 4상태 추가(총 8상태).
  - `modules/shop-web/src/features/shop/ProductGrid.stories.tsx` — "목록있음"의 앞 4행을 실 SKU(1001~1004)로 교체, 나머지 6행은 `listPrice: null, imageUrl: null`.
  - `modules/shop-web/src/features/shop/RankingSection.stories.tsx`, `RecentlyViewed.stories.tsx` — 각 `Product` 리터럴에 `listPrice: null, imageUrl: null` 추가(타입 컴파일용, 상태 의미 불변).
  - `modules/shop-web/src/pages/ShopHomePage.test.tsx` — `ProductStub`에 `listPrice`/`imageUrl` optional 필드 추가, `product()` 헬퍼 기본값 `null`. 신규 테스트 5건: 할인 정확 텍스트(13%·450,000원) · 할인0% 배지·취소선 부재(같은 텍스트가 판매가 span과 겹치는 함정을 `selector: 'div'`로 구분) · 정가없음 부재 · 이미지 렌더+이니셜 부재 · 이미지 로드실패→이니셜 전환.
  - `modules/shop-api/**`, Thymeleaf 화면 — 변경 없음(범위 밖, 회귀 보호 대상만).
  - 검증: `npm test`(tsc --noEmit + jest 전체 58건 통과) · `npm run storybook`(백그라운드 기동) 후 `npx test-storybook --url`(전체 18 스위트 69건 통과, 기본 6006 포트가 점유돼 있어 실제 기동 포트로 지정) · 테스트 후 storybook dev 프로세스 종료.
- 재작업 완료(round2, 2026-09-17) — round1 QA FAIL 필수수정 1·2 반영. **이탈 명시**: 이번 라운드의
  할인율 식(`Math.floor((listPrice - price) * 100 / listPrice)`, 먼저 곱하고 나중에 나눈다)은 원래
  STORY `## 구현 계획`이 적어 둔 식(`Math.floor((product.listPrice - product.price) / product.listPrice * 100)`,
  나누고 곱한다)과 다르다 — QA가 계획서 식도 29%/57%/58%에서 틀린다고 실측했고, 사람 코멘트가
  "정수 연산으로(먼저 곱하고 나중에 나눈다)"를 명시적으로 지시해 계획보다 그 지시를 우선했다(round1
  Dev 기록이 "계획대로, 이탈 없음"이라 적었으나 실제로는 계획과 다른 식을 썼던 것이 round1 QA
  권고 1의 지적 — 이번엔 이탈을 이렇게 명시한다).
  - **수정 전 실패 재현(사람 지시)** — `discountRate.ts`를 만들 때 일부러 round1의 버그 식
    (`Math.floor((1 - price/listPrice) * 100)`)을 그대로 옮겨 먼저 넣고, 아래 표 기반 테스트
    (`discountRate.unit.test.ts`, QA가 지목한 17개 비율 + 명시 예시 3건 + 경계값 2건 + 0%/null 가드
    4건 = 29건)를 그 버그 식에 대해 실행했다. 결과: **17건 실패, 12건 통과**(`npx jest --config
    jest.config.cjs discountRate.unit.test.ts`). 실패 로그 발췌(원문 그대로):
    ```
    ● calcDiscountRate ... price=90000 listPrice=100000 → 10%(QA 지목 비율)
      Expected: 10
      Received: 9
    ● calcDiscountRate ... 90,000/100,000 = 10.0% (QA 실측: round1은 9%를 반환)
      Expected: 10
      Received: 9
    Test Suites: 1 failed, 1 total
    Tests:       17 failed, 12 passed, 29 total
    ```
    QA 리포트가 지목한 "90,000/100,000 → 9%"가 이 테스트로 그대로 재현됨을 확인한 뒤,
    `discountRate.ts`의 식만 정수 연산으로 교체했다(`Math.floor(((listPrice - price) * 100) / listPrice)`).
    재실행: **29건 전부 통과**(`Test Suites: 1 passed, Tests: 29 passed, 29 total`).
  - `modules/shop-web/src/features/shop/discountRate.ts`(신규) — `calcDiscountRate(price, listPrice)`·
    `hasListPriceDiscount(price, listPrice)` 2개 함수로 계산 로직을 한 곳에 모았다(사람 지시
    "계산 함수는 한 곳에 두고 스토리·테스트가 같은 함수를 쓰게 할 것"). listPrice가 없거나 price
    이상이면 계산 자체를 하지 않고 0을 반환(가드 우선).
  - `modules/shop-web/src/features/shop/discountRate.unit.test.ts`(신규) — 표 기반 테스트 29건.
    QA가 지목한 17개 비율(정가 100,000 기준 7·8·9·10·11·19·20·21·22·29·32·33·44·45·57·58·66%) +
    QA 리포트 원문 예시 3건(90000/100000·80000/100000·2000/2500) + 사람 지시 내림 케이스
    (35000/42000→16%) + 경계값 2건(1%·99%) + 0%/null 가드 4건.
  - `modules/shop-web/src/features/shop/ProductCard.tsx` — 로컬 계산식 제거, `discountRate.ts`의
    두 함수로 교체(`hasDiscount`/`discountRate` 도출부만 변경, 렌더 JSX·prop 계약은 round1과 동일).
  - **[low/regression, 재작업 지시 4]** `ProductCard.stories.tsx`의 `이미지있음`이 `정가없음`과 동일
    args(SKU-1002)인 문제는 **이번 라운드에서 데이터를 바꾸지 않기로 결정**했다 — 사유: 실제로
    브라우저에서 로드 성공을 보여주려면 존재하는 이미지 파일이 필요한데, 현재 실 SKU 중 이미지가
    있는 자산은 3개(`sku-1001.svg`=할인, `sku-1002.svg`=정가없음/이미지있음, `sku-1003.svg`=
    할인0퍼센트)뿐이라 다른 실 SKU로 바꿔도 다른 상태와의 중복이 그대로 옮겨갈 뿐이다(실데이터를
    지어내지 않는다는 프로젝트 관례상 새 합성 이미지 자산을 만들지 않음). QA도 "계획·사람 승인분이라
    차단 아님"으로 판정했으므로, 상태를 유지할지/제거할지는 QA 권고대로 후속 라운드 정리 항목으로
    남긴다(이번 라운드 변경 없음, 명시적 결정).
  - **[low/security, 재작업 지시 5]** `imageUrl` 경로 접두 검증은 QA 스스로 "외부 이미지 URL을 허용하는
    후속 SR에서 도입"이라 적어 이번 SR 범위 밖으로 명시했다 — 이번 라운드에서 변경하지 않음.
  - 회귀 검증: `npm test`(tsc --noEmit + jest **전체 8 스위트/87건** 통과, discountRate 테스트
    29건 포함 — 기존 58건 + 신규 29건) · storybook 6017 포트로 기동 후 `npx test-storybook --url`
    (**전체 18 스위트/69건** 통과, round1과 동일 — ProductCard 관련 렌더 계약 불변 확인) · 테스트 후
    storybook dev 프로세스(PID) 종료.
- 재작업 완료(round3, 2026-09-17) — round2 축E(story_gate.py, 격리 렌더) FAIL 반영. 원인:
  `ProductCard`·`ProductGrid` 스토리가 쓰는 `/images/products/sku-1001~1003.svg`는 shop-api의
  정적 자산(`modules/shop-api/src/main/resources/static/images/products/`, SR-306 #2 정본)인데,
  스토리북은 백엔드 없이 단독 렌더돼(vite dev 서버만 뜬다) 그 경로를 못 찾아 404였다. 사람 지시대로
  **사본을 두지 않고** `.storybook/main.ts`의 `staticDirs`로 원본 디렉터리를 그대로 가리키게 했다
  (환경 제약 없어 사본 폴백은 쓰지 않음).
  - `modules/shop-web/.storybook/main.ts` — `staticDirs: ['../../shop-api/src/main/resources/static']`
    추가(주석으로 "정본은 shop-api, 사본 아님" 명시). 경로는 `.storybook` 설정 파일 기준 상대경로임을
    실측으로 확인(아래 검증) — `.storybook` → `shop-web` → `modules` → `shop-api/...` 2단계 상승.
    사람 코멘트가 준 문자열(`../shop-api/src/main/resources/static`, shop-web 루트 기준 1단계
    상승)은 storybook이 실제로 해석하는 기준(cwd가 아니라 config 파일 위치)과 달라 그대로 쓰면
    작동하지 않았을 것 — `npx storybook build`로 먼저 실측한 뒤(사본 없이 output에 이미지가
    복사되는지로 판정) 맞는 단계 수로 교정했다. 나머지는 사람 지시 그대로: `does-not-exist.svg`
    (이미지로드실패 상태)는 건드리지 않아 여전히 404.
  - 검증(둘 다 성공, 사본 없음):
    1. `npm run build-storybook` → `storybook-static/images/products/{sku-1001,sku-1002,sku-1003}.svg`
       실제로 존재(원본 디렉터리에서 정적 복사됨). 확인 후 `storybook-static/` 삭제(빌드 산출물,
       버전관리 대상 아님).
    2. `npx storybook dev --no-open --ci --port 6018` 기동 후 `curl` — `sku-1001.svg`·
       `sku-1002.svg`·`sku-1003.svg` 전부 `200`, `does-not-exist.svg`는 여전히 `404`(의도 유지).
    3. 같은 dev 서버에 `npx test-storybook --url http://localhost:6018` — **18 스위트/69건 전부
       통과**(축E가 지목한 ProductCard·ProductGrid 6건 포함, round2와 동일 건수 — 렌더 계약 불변).
       테스트 후 dev 서버 프로세스 종료.
  - `npm test`(tsc --noEmit + jest) **전체 8 스위트/87건 통과**(round2와 동일 — 이번 라운드는
    `.storybook/main.ts` 설정 1건만 바꿔 소스·테스트 변경 없음).
  - `modules/shop-api/**`는 읽기만 했다(정적 자산 경로 확인용) — 수정 없음(정본은 그쪽, 이 항목의
    회귀 보호 대상).
- 재작업 완료(round4, 2026-09-17) — round3 축E FAIL(재작업 지시 1·2·3) 반영.
  - `modules/shop-web/src/features/shop/ProductCard.stories.tsx` — `이미지로드실패`의 `imageUrl`을
    404 경로(`/images/products/does-not-exist.svg`)에서 깨진 data URI(`data:image/png;base64,AAAAAAAA`)로
    교체. 상태 이름·의미(=`onError` → 이니셜 대체 영역, 확정답변 9)는 그대로 유지. JSDoc에 "축E
    (story_gate.py → storybook_render.js)는 콘솔 error 1건이면 스토리를 깨진 것으로 판정하므로
    404 경로를 쓰지 않는다"는 사유를 남김.
  - `modules/shop-web/.storybook/main.ts` — 코드 동작 변경 없이 주석 2건 추가: (1) round4에서
    `does-not-exist.svg` 대신 data URI를 쓰는 이유(축E 콘솔 error 판정), (2) `staticDirs`가
    shop-api 저장소가 shop-web과 형제 경로에 체크아웃돼 있어야 한다는 전제(재작업 지시 3 —
    별도 git 저장소 간 크로스 참조, 없으면 storybook dev/build 하드 실패).
  - `project.env`의 `STORYBOOK_TEST_CMD`는 사람 지시대로 건드리지 않음.
  - **검증(재작업 지시 2 반영 — 이번 라운드부터 화면 축은 `story_gate.py`로 직접 확인)**:
    `python {{PLUGIN_PATH}}/scripts/story_gate.py {{WS}} --func SR-306.1` 실행 결과
    `"verdict": "pass"`(`uncovered: []`, `render.total: 23`, `render.failed: 0`, `issues: []`).
    `쇼핑홈-상품-카드--이미지로드실패` 개별 결과도 `"ok": true, "errors": []`로 round3의 404
    콘솔 error가 사라짐을 확인. `spec_drift`(`UIS-ORD-008`) `ok: true`.
  - `npm test`(tsc --noEmit + jest) **전체 8 스위트/87건 통과**(round3와 동일 수치 — 이번
    라운드는 스토리 데이터 1줄 + 설정 주석뿐이라 소스 로직·테스트 변경 없음). 회귀로서
    `ShopHomePage.test.tsx:290-297`(`imageUrl이 있어도 이미지 로드가 실패하면 이니셜 대체
    영역으로 전환되고 img는 사라진다`, `fireEvent.error` → `getByLabelText('...대표이미지 없음')`
    존재 확인 + `queryByRole('img')` 부재 확인)가 이미 onError 폴백을 단언하고 있어 이번
    라운드에 새로 추가하지 않음(스토리 자체의 `imageUrl` 값은 컴포넌트 레벨 이 테스트의
    입력이 아니라 스토리북 전용 픽스처라 값 교체가 이 테스트에 영향 없음).
  - `modules/shop-api/**`는 건드리지 않음(범위 밖, 회귀 보호 대상).

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-17 — FAIL
- **Layer1 스펙**: **fail** — 확정답변 8("할인율은 **내림해 정수 %**로 표시")을 만족하지 못한다. `ProductCard.tsx:20`이 계획서의
  `Math.floor((listPrice - price) / listPrice * 100)`가 아닌 `Math.floor((1 - product.price / product.listPrice!) * 100)`로
  구현돼(계획 이탈, Dev 기록은 "이탈 없음"으로 적었다), 나눗셈 결과를 먼저 1에서 빼면서 배정밀도 오차가 경계 아래로 떨어진다.
  정가 100,000 기준 1~90% 중 **17개 비율이 1%p 틀리게 표시**된다(실측: `dev=Math.floor((1-p/l)*100)` vs 정확값) —
  판매가 90,000/정가 100,000 = 실제 10.0%인데 **9%**, 80,000/100,000 = 20.0%인데 **19%**, 2,000/2,500 = 20.0%인데 **19%**.
  계획서 공식도 정확하지 않다(29%→28, 57%→56, 58%→57) — 되돌리는 것이 아니라 **정수 연산**으로 고쳐야 한다:
  `Math.floor((product.listPrice - product.price) * 100 / product.listPrice)`는 1~99% 전 구간 정확(실측 확인).
  나머지 스펙 항목은 일치 — `listPrice != null && listPrice > price`일 때만 취소선·배지(확정답변 8의 "판매가 이상이면 안 그린다"),
  0%면 배지 숨김, `onError` → 이니셜 대체 영역, 기존 5필드 이름·순서·타입 불변 + 새 2필드 끝 추가(확정답변 1·3), 백엔드
  `Product.listPrice/imageUrl`(camelCase, nullable)과 프론트 타입 정합 확인.
- **Layer2 보안**: **pass** — 인증·인가·오류 계약 변경 없음(`ApiKeyAuthFilter` 미변경, SR-307 공개 GET 정책 유지). 새 fetch 없음
  (규칙 `web-fetch-only-in-api` 유지), `console.*` 없음. `alt`/`src`는 React가 이스케이프하며 `<img src>`는 스크립트 실행 벡터가
  아니다. 단, `imageUrl` 값 형태를 프론트가 검증하지 않아 DB에 절대 URL이 들어오면 그대로 외부 요청이 나간다(확정답변 1이
  "외부 이미지 URL"을 제외했으므로 현 데이터로는 미발생 — low).
- **Layer3 회귀**: **pass** — `npm test` 독립 실행 7 스위트/**58건 전부 통과**(tsc --noEmit 포함, 값 재확인). 기존 스토리 4상태
  이름(기본·할인·품절·이미지없음) 보존, 기존 테스트 "정가·이미지가 없어도 레이아웃이 깨지지 않는다" 그대로 통과.
  `ProductCardProps`에서 제거한 3 prop은 세 호출자(`ProductGrid.tsx:54`·`RankingSection.tsx:58`·`RecentlyViewed.tsx:23`)가
  모두 `product`만 넘기고 있어 영향 없음. `Product` 필수 필드 추가에 따른 리터럴 5파일 전부 갱신됨. `api.ts`는 응답을 필드
  매핑 없이 통과시켜 새 필드가 그대로 흐른다. `modules/shop-api/**`·Thymeleaf 무변경(git status로 확인) —
  `selectProductsForList`(Thymeleaf 전용)가 두 컬럼을 SELECT하지 않는 것은 SR-306(#2)가 의도적으로 남긴 범위 밖.
  `imgLoadFailed` state는 세 호출자 모두 `key={p.sku}`로 식별해 랭킹 탭 전환(재정렬) 시 다른 상품에 새지 않음(실측 확인).
  파일 크기 상한 내(ProductCard 70 / stories 108 / test 294줄).
- 필수 수정(FAIL시):
  1. **[high · Layer1]** `modules/shop-web/src/features/shop/ProductCard.tsx:20` 할인율 계산을 정수 연산으로 교체 —
     `const discountRate = hasDiscount ? Math.floor((product.listPrice! - product.price) * 100 / product.listPrice!) : 0`.
     계획서 공식으로 되돌리지 말 것(29%·57%·58%에서 같은 클래스의 오차가 남는다).
  2. **[medium · 테스트 커버리지]** `ShopHomePage.test.tsx`의 할인율 단언이 390,000/450,000(=13.33%, **내림이 필요한**
     비율)만 쓰고 있어 이 결함을 원리적으로 잡지 못한다 — 실패 사례집 39행("가드를 임시로 걷어내 테스트가 실제로 깨지는지
     확인한 뒤 되돌린다")이 그대로 재발한 형태다. **실제 비율이 정수인 케이스**(예: price 90,000 / listPrice 100,000 → `10%`)를
     추가하고, 고치기 **전**에 그 테스트가 `9%`로 실패하는 것을 눈으로 확인한 뒤 수정할 것. 내림 케이스(35,000/42,000 → `16%`)도
     함께 고정한다.
- 권고(다음 라운드로 넘기지 않음):
  1. **[low]** Dev 기록의 "계획대로 진행, 이탈 없음"이 사실과 다르다(계획 공식과 다른 식을 썼고, 같은 기록에 다른 식을
     적어 놓았다). 계획과 다른 식을 쓸 때는 이탈로 적고 사유를 남긴다 — 리뷰어가 "계획대로"를 믿고 식을 다시 유도하지 않는다.
  2. **[low]** `ProductCard.stories.tsx`의 `이미지있음`은 `정가없음`과 args가 완전히 동일해(SKU-1002) 새로운 시각 상태를
     추가하지 않는다(계획·사람 승인분이라 차단 아님). 후속에서 "정가 있음 + 이미지 있음"은 이미 `할인` 상태가 덮으므로,
     이 상태를 남길지/다른 데이터로 차별화할지 정리 권고.
- 재동기화 입력(STEP 5.5 몫 — QA 권고 아님):
  1. `UIS-ORD-008` — 본문이 "코드(라우트) 기준 재동기화 골격. 화면 구성·상태·검증 규칙은 보강 대상" 상태라 상품 카드의
     정가 취소선·할인율 배지(내림 정수 %, 0% 숨김)·이미지 대체 영역 3상태가 스펙 본문에 없다. STORY 수용 기준도
     `UIS-ORD-008: [미상]`으로 남아 AC 대조는 확정문답 8·9를 기준으로 수행했다.

### QA Gate — 2026-09-17 (round 2) — PASS
- **Layer1 스펙**: **pass** — round1 FAIL 사유(할인율 부동소수점 오차)가 **해소됨**. 계산이
  `modules/shop-web/src/features/shop/discountRate.ts:19`의 `Math.floor(((listPrice - price) * 100) / listPrice)`
  정수 연산으로 이동했고, `ProductCard.tsx:23-24`가 그 두 함수(`hasListPriceDiscount`/`calcDiscountRate`)만 쓴다
  (로컬 계산식 복제 없음 — `grep`으로 shop-web 전체에 다른 할인율 식이 남지 않은 것 확인).
  **QA 독립 실측**(node, 정가 100,000 기준 1~99% 전수 + `listPrice ≤ 3,000` 전 조합 + 100,000~100,500 구간 표본):
  round1 식은 14개 비율(7·8·9·10·11·19·20·21·22·32·33·44·45·66%), 계획서 식은 3개(29·57·58%)가 1%p 틀리고,
  **round2 식은 불일치 0건**(BigInt 정확값 대조). 90,000/100,000 → `10`, 80,000/100,000 → `20`, 2,000/2,500 → `20`,
  35,000/42,000 → `16`(내림) 전부 정확. 정수 인자에서는 `(l-p)*100`이 정확하고 나눗셈만 1회 반올림돼(상대 오차
  ~1e-16 ≪ 눈금 간격 1/l ~1e-5) 경계 아래로 떨어질 수 없음을 확인 — `Product.java:13,16`이 `long`/`Long`, DDL
  `V10`이 `BIGINT`라 비정수 입력 경로도 없다.
  확정답변 8 전 항목 일치: `listPrice != null && listPrice > price`일 때만 취소선(`ProductCard.tsx:68`)·배지,
  0%면 배지 숨김(`discountRate > 0`), `onError` → 이니셜 대체 영역(`showImage = !!imageUrl && !imgLoadFailed`),
  확정답변 1·3(기존 5필드 이름·순서·타입 불변 + 새 2필드 끝 추가)도 유지. 가드가 `null`·`listPrice < price`
  (비정상 데이터)를 계산 전에 걷어내 "계산 자체를 하지 않는다"는 사람 지시대로 동작.
- **Layer2 보안**: **pass** — round2 신규 코드는 순수 함수 2개(`discountRate.ts`)와 그 유닛 테스트뿐이다.
  인증·인가·오류 계약 변경 없음(`ApiKeyAuthFilter` 무변경, SR-307 공개 GET 정책 유지), 새 `fetch` 없음
  (규칙 `web-fetch-only-in-api` 유지), `console.*` 없음, 외부 입력 파싱·문자열 조립 없음.
  round1 권고였던 `imageUrl` 경로 접두 검증은 round1 QA가 "외부 URL을 허용하는 후속 SR 몫"으로 범위 밖을
  명시했고 dev가 그 판정대로 미변경 — 재게이트하지 않는다(후속 SR TODO 유지).
- **Layer3 회귀**: **pass** — QA 독립 실행 `npm test`(= `tsc --noEmit` + jest) **8 스위트 / 87건 전부 통과**
  (기존 58건 + 신규 29건, Dev 기록 수치와 일치). `modules/shop-api`·Thymeleaf는 워킹트리 완전 무변경(별도
  저장소 `git status` 공백 확인) — 범위 밖 회귀 보호 유지. 기존 스토리 4상태 이름(기본·할인·품절·이미지없음)
  보존, `RankingSection` play 함수가 단언하는 텍스트(`인기 4위 상품(최저가)`) 불변.
  스토리북 렌더 결과는 round1과 동일함을 계산으로 확인(할인 배지가 실제로 보이는 상태는 `할인`·ProductGrid
  `목록있음`의 SKU-1001 하나뿐이고 390,000/450,000은 두 식 모두 `13`) — round2 diff가 `.tsx` 스토리를 건드리지
  않아 `story_shots` 기준선도 round1 대비 무변동. 파일 크기 상한 내(ProductCard 74 / stories 108 /
  discountRate 20 / ShopHomePage.test 294줄). `story-per-component`(must)는 `.tsx` 대상이라 신규 `.ts` 2개는
  비대상 — 우회 아님(순수 계산 모듈, 렌더 부품 아님).
- 필수 수정: 없음.
- 권고: 없음(차단·medium 이상 0건).
  - 참고(무조치): 재작업 지시 2가 지목한 "정수 비율 케이스"는 `ShopHomePage.test.tsx`가 아니라 신규
    `discountRate.unit.test.ts`의 표 기반 29건으로 들어갔다 — 사람 코멘트가 "계산 함수를 한 곳에 두고 표 기반
    테스트 추가"로 방향을 바꿨고, 계산이 한 함수로 모인 뒤에는 그쪽이 결함을 원리적으로 잡는 자리라 지시 의도
    충족으로 본다(통합 테스트는 와이어링만 13%로 단언). 실패 선재현(버그 식으로 17건 실패 → 수정 후 29건 통과)
    로그가 Dev 기록에 원문으로 남아 사례집 39행 교훈도 이행됨.
  - 참고(무조치): round1 권고 2(`이미지있음` = `정가없음` 동일 args)는 dev가 "실 이미지 자산이 3개뿐이라 다른
    실 SKU로 옮겨도 중복이 이동할 뿐"이라는 사유로 이번 라운드 미변경을 명시 결정했다 — round1에서 이미
    비차단으로 판정한 low라 재게이트하지 않는다(후속 정리 항목 유지).
- 재동기화 입력(STEP 5.5 몫 — QA 권고 아님):
  1. `UIS-ORD-008` — 본문이 아직 "코드(라우트) 기준 재동기화 골격" 상태라 상품 카드의 정가 취소선·할인율
     배지(**내림 정수 %, 정수 연산**, 0% 숨김)·이미지 대체 영역(없음/로드실패) 상태가 스펙 본문에 없다.
     round1과 동일 항목이며, 이번 라운드에서 확정된 계산 규칙(`(정가-판매가)*100/정가`를 정수 연산으로 내림)을
     본문에 함께 반영할 것. STORY 수용 기준의 `UIS-ORD-008: [미상]`도 같이 채워야 AC 대조가 확정문답 8·9
     대리 기준을 벗어난다.

### QA Gate — 2026-09-17 (round 3) — FAIL
- **Layer1 스펙**: **fail** — 재작업의 목표(round2 축E FAIL 해소)가 **절반만** 달성됐다.
  - **해소된 것(실측 확인)**: `staticDirs` 방식이 실제로 동작한다. `npm run build-storybook` 산출물
    `modules/shop-web/storybook-static/images/products/`에 `sku-1001·1002·1003.svg`가 실제로 복사돼 있고
    (사본을 만들지 않고 원본 디렉터리를 참조 — 사람 지시 준수), 정적 서버로 `200` 확인. round2에 깨졌던
    6건(할인·정가없음·이미지있음·할인0퍼센트·그리드 목록있음·빈목록)이 **전부 통과**로 돌아섰다.
    `.storybook/main.ts` 경로 단계 수(`../../shop-api/...`, config 파일 기준 2단계)도 사람 코멘트의 1단계
    문자열이 아니라 dev가 실측 교정한 쪽이 맞다 — 확인됨.
  - **해소되지 않은 것(차단)**: 축E는 **여전히 fail**이다. QA 독립 실행
    `python {{PLUGIN_PATH}}/scripts/story_gate.py {{WS}} --func SR-306.1` → `"verdict": "fail"`,
    남은 이슈 1건 그대로: `스토리가 깨졌습니다: 쇼핑홈-상품-카드--이미지로드실패 — Failed to load
    resource: the server responded with a status of 404 (File not found)`.
    축E의 렌더러(`{{PLUGIN_PATH}}/scripts/storybook_render.js:71-72`)는 **favicon을 뺀 콘솔 error가 1건이라도
    있으면** 그 스토리를 깨진 것으로 센다 — "의도적 404"를 예외로 선언할 수단이 없다(`renders-nothing`
    태그는 *빈 렌더* 전용이고 콘솔 error에는 적용되지 않는다). 즉 `does-not-exist.svg`를 그대로 두는 한
    이 스토리는 **구조적으로 축E를 통과할 수 없다**.
  - **round2에 이 스토리가 통과로 보였던 이유(원인 규명)**: 404 콘솔 error가 비동기로 늦게 도착해
    `page.off` 이후 **다음 스토리**에 붙는다. round2 FAIL 목록에 이미지가 하나도 없는 `빈목록`이 404
    3건으로 올라온 것이 그 증거다. 이번 라운드에 다른 404가 사라지자 이 404가 자기 스토리에 정확히
    붙었다 — **round3 코드가 만든 새 결함이 아니라 round2에도 있던 미해소 high**다(라운드 규율상 새
    medium으로 올리지 않는다). 어느 스토리에 귀속될지가 비결정적이라, 고치지 않으면 다음 라운드에
    `그리드 목록있음`이 대신 실패로 뜰 수도 있다.
  - **스토리 삭제는 해법이 아니다**: 확정답변 9가 "이미지 로드 실패"를 남길 상태로 **명시**했으므로
    축E의 권고("상태가 없어졌다면 스토리를 지우세요")를 그대로 따르면 스펙 위반이다. 상태는 유지하고
    **재현 방식만** 바꿔야 한다.
  - **대체 재현 방식 — QA가 실측으로 검증했다**(`{{WS}}/_tmp/qa_img_probe.js`, 축E와 같은 판정 기준으로
    playwright 실행): `data:image/png;base64,AAAAAAAA`(깨진 data URI)와 200이지만 이미지가 아닌 응답은
    **`onerror`를 그대로 발생시키면서 콘솔 error 0건** → 축E 통과. 현행 404만 error 1건. 대조군
    `sku-1001.svg`는 `onload`/error 0건.
  - **나머지 스펙 항목은 round2 판정 그대로 유효**(round3 diff가 `.storybook/main.ts` 한 파일뿐임을
    `git diff --stat`으로 확인 — `ProductCard.tsx`·`discountRate.ts`·테스트 무변경): 할인율 정수식
    `Math.floor(((listPrice - price) * 100) / listPrice)` 유지, QA 재실측 결과 `listPrice ≤ 3,000` 전 조합과
    정가 100,000 기준 1~99% 전 구간 **불일치 0건**, 명시 예시 90000/100000→`10`·80000/100000→`20`·
    2000/2500→`20`·35000/42000→`16`(내림)·390000/450000→`13` 전부 정확, 가드(null·동일·역전) 모두 `0`.
    AC-001~007 충족(취소선·배지 조건 `listPrice != null && listPrice > price`, 0% 배지 숨김,
    `onError` → 이니셜 대체 영역, 기존 5필드 이름·순서·타입 불변 + 새 2필드 끝 추가).
- **Layer2 보안**: **pass** — round3 변경은 스토리북 빌드 설정 1줄뿐이다. 인증·인가·오류 계약 변화 없음
  (`ApiKeyAuthFilter` 무변경, SR-307 공개 GET 정책 유지), 새 `fetch` 없음(`web-fetch-only-in-api` 유지),
  `console.*` 없음. `staticDirs`는 **디렉터리 전체를 미러링**하는데 현재 shop-api `static/` 실물은 제품
  SVG 3개뿐이라(실측: `find ... -type f` = 3건) 노출 위험 없음 — 다만 앞으로 그 디렉터리에 무엇을 두든
  스토리북 dev 서버와 `storybook-static/` 산출물로 자동 공개된다(low, 아래 권고 2).
- **Layer3 회귀**: **pass** — QA 독립 실행 `npm test`(= `tsc --noEmit` + jest) **8 스위트 / 87건 전부 통과**
  (Dev 기록 수치와 일치, exit 0). `modules/shop-api` 워킹트리 **완전 무변경**(별도 저장소 `git status`
  공백 실측 — dev 주장대로 읽기만 했다), Thymeleaf·DDL·매퍼 무변경. 축E의 나머지 22건 렌더 통과,
  `uncovered`(스토리 없는 변경 부품) 0건, `story_link` drift 이슈 0건(UIS-ORD-008 §5 링크가 아직 0행이라
  `linked: false` — 해당 없음). 신규 `discountRate.ts`는 `.ts`라 `story-per-component`(must) 비대상.
  파일 크기 상한 내.
- 필수 수정(FAIL시):
  1. **[high · Layer1/stories]** `modules/shop-web/src/features/shop/ProductCard.stories.tsx`의
     `이미지로드실패` 상태를 **네트워크 404 없이** 로드 실패를 일으키는 값으로 바꾼다 — 예:
     `imageUrl: 'data:image/png;base64,AAAAAAAA'`(QA 실측: `onerror` 발생 + 콘솔 error 0건). 상태 이름과
     의미(=`onError` → 이니셜 대체 영역)는 그대로 유지하고(확정답변 9), JSDoc에 "축E 렌더러가 콘솔
     error를 실패로 세므로 실제 404 경로를 쓰지 않는다"는 사유를 남긴다.
     **검증은 `test-storybook`이 아니라 축E로 한다** — `python {{PLUGIN_PATH}}/scripts/story_gate.py
     {{WS}} --func SR-306.1`이 `"verdict": "pass"`가 되는 것으로 확인(아래 필수수정 2와 같은 이유).
  2. **[medium · 검증 방법]** Dev 기록이 **3라운드 연속** `npx test-storybook`(18 스위트/69건 통과)으로
     화면 축 통과를 대리 증명했는데, 이 대상에는 `STORYBOOK_TEST_CMD`가 **설정돼 있지 않다**
     (`project.env` 실측: `STORYBOOK_SOURCE`·`RUN_CMD`·`STATIC`·`BUILD_CMD`만 있다). 따라서 축E는
     `storybook_render.js`(콘솔 error 1건 = 실패)를 쓰고, 두 하네스의 **실패 기준이 다르다** —
     test-runner는 이 404를 실패로 세지 않는다. round2·round3의 "69건 통과"가 축E FAIL과 공존한 것이
     그 증거다. 다음 라운드부터 화면 축은 위 `story_gate.py` 명령으로 직접 확인하고, 그 출력(verdict)을
     Dev 기록에 적을 것.
  3. **[medium · 회귀/크로스 리포]** `staticDirs: ['../../shop-api/src/main/resources/static']`는
     **별도 git 저장소**인 shop-web에 shop-api 디렉터리 존재를 빌드 전제로 심는다. Storybook 10.6.0은
     그 경로가 없으면 `Failed to load static files, no such directory: ...`로 **하드 실패**한다
     (`node_modules/storybook/dist`에서 메시지 확인) — shop-web만 단독 체크아웃/CI하면
     `npm run storybook`·`build-storybook` **양쪽이 죽고**, 그러면 축E가 스토리 1건이 아니라 **축 전체가
     unknown(판정 불능)으로 꺼진다**. 사본을 만들지 않는다는 결정은 유지하되(원본 어긋남 방지가 맞다),
     이 크로스 모듈 의존을 **한 줄로 문서에 남긴다**(워크스페이스 `CLAUDE.md` 모듈 표 각주 또는
     shop-web `package.json`/README 주석) — 코드 변경 없이 끝나는 항목이라 필수수정 1과 같은 라운드에
     처리한다.
- 권고(이번 라운드에 전부 열거 — 다음 라운드로 새로 올리지 않는다):
  1. **[low]** 축E 렌더러에 "이 스토리는 콘솔 error가 정상"을 선언할 수단이 없다(`renders-nothing`은 빈
     렌더 전용). 위 필수수정 1은 워크스페이스 쪽 우회이고, 근본은 플러그인 쪽 개선 여지다 —
     `expects-console-error` 같은 태그, 또는 이 대상에 `STORYBOOK_TEST_CMD`를 채택(축E 설계 주석이
     "play function까지 도는 더 강한 판정"으로 권하는 쪽)하는 선택지. 단 후자는 test-runner가 기동된
     스토리북을 요구해 AIDD 잡 중 dev 서버 기동 금지(CLAUDE.md OOM 실측)와 충돌하므로, 채택은 사람
     판단 사항으로 남긴다. **이번 SR에서 고치지 않는다.**
  2. **[low/security]** `staticDirs`가 shop-api `static/` 전체를 스토리북 산출물로 공개한다(현재 파일 3개
     이므로 무해). 앞으로 그 디렉터리에 비공개 자산을 두게 되면 `staticDirs`를
     `.../static/images`처럼 **필요한 하위 경로로 좁힐 것**(후속 TODO, 지금 바꿀 필요 없음).
  3. **[low]** `story_shots` 기준선(`.speclinker/story_shots/baseline/`)은 이미지가 404였던 시점의 픽셀을
     담고 있다. round3로 4개 상태(할인·정가없음/이미지있음·할인0퍼센트)와 그리드 `목록있음`의 렌더
     결과가 **실제로 달라졌다**(깨진 이미지 → 실 SVG). 기준선 재기록은 필수수정 1까지 끝난 뒤
     한 번에 하고, 사유("SR-306.1 — 상품 이미지가 스토리북에서 실제로 서빙됨")를 함께 남긴다.
  4. **[low]** round1 권고 2 / 재작업 지시 7(`이미지있음` = `정가없음` 동일 args, SKU-1002)은 round2에서
     명시적 유지 결정이 났고 round3도 무변경 — 재게이트하지 않는다(후속 정리 항목 유지).
  5. **[low]** 참고: QA가 축E를 실행하면서 `modules/shop-web/storybook-static/`이 재생성됐다(gitignore
     대상, 빌드 산출물). 다음 축E 실행 입력이 되므로 삭제하지 않아도 무해하다.
- 재동기화 입력(STEP 5.5 몫 — QA 권고 아님):
  1. `UIS-ORD-008` — round1·round2와 동일 항목. 본문이 아직 "코드(라우트) 기준 재동기화 골격"이라 상품
     카드의 정가 취소선·할인율 배지(**정수 연산 내림 정수 %**, 0% 숨김)·이미지 대체 영역(없음/로드실패)
     상태가 스펙 본문에 없고, STORY 수용 기준의 `UIS-ORD-008: [미상]`도 비어 있다.
  2. `UIS-ORD-008 §5`(표시 조건 ↔ 스토리 링크)가 **0행**이다(`story_gate` 실측: `linked: false`, 스토리
     23건). 이번 SR로 상품 카드 상태가 4→8건으로 늘었으니 5.5에서 §5 행과 `[이름](story:ID)` 링크를
     함께 채우면 다음 SR부터 축E가 "행 없는 스토리/스토리 없는 행"까지 잡는다.

### QA Gate — 2026-09-17 (round 4) — PASS
- **Layer1 스펙**: **pass** — round3 FAIL(축E 구조적 충돌)이 **실제로 해소**됐다. QA 독립 실행
  `python {{PLUGIN_PATH}}/scripts/story_gate.py {{WS}} --func SR-306.1` → `"verdict": "pass"`,
  `render.total 23 / failed 0`, `uncovered: []`, `issues: []`. `쇼핑홈-상품-카드--이미지로드실패`도
  `"ok": true, "errors": []`(round3의 404 콘솔 error 소멸).
  - **빌드 신선도까지 확인(dev 보고를 그대로 믿지 않기 위해)**: 이번 게이트는 `fresh.rebuilt: false`로
    기존 `storybook-static`을 재사용했다. 그 빌드가 round4 소스를 담았는지 별도 대조 — `iframe.html`
    (09:36:21) → `assets/iframe-Q5YHbbsK.js` → `ProductCard.stories-Bx-0jtCF.js`(09:36:21, data URI 포함).
    404 경로를 담은 구 번들 `ProductCard.stories-CFDUQa-G.js`(09:26:48)는 **어디서도 참조되지 않는
    잔존물**이다. 소스 mtime(09:35:45)보다 빌드가 나중 → pass는 round4 소스에 대한 판정이 맞다.
  - **사람 완료조건("이미지 로드 실패 스토리가 여전히 이니셜 대체 영역을 보여주는지 단언") — QA가 실제
    브라우저 DOM으로 직접 단언**(`{{WS}}/_tmp/qa_r4_fallback_probe.js`, 축E와 같은 playwright-core,
    `storybook-static`을 127.0.0.1:6099로 서빙): 이미지로드실패 → `<img>` **없음**,
    `aria-label="이미지 깨짐 상품 대표이미지 없음"` 이니셜 `이` 렌더, 콘솔 error **0건**. 즉 깨진 data URI가
    네트워크 요청 없이 `onError`를 그대로 태워 상태의 의미(=이니셜 대체 영역)를 잃지 않았다.
  - **대조군도 같은 실측으로 확인**: `이미지있음`·`정가없음`·`할인`·`할인0퍼센트` → 실 SVG 로드
    (`naturalWidth 480`, static 200), `할인`(SKU-1001) = 배지 `13%` + 취소선 `450,000원`,
    `할인0퍼센트`(SKU-1003)·`정가없음`(SKU-1002) = 배지·취소선 **둘 다 없음**, `이미지없음`(SKU-1004) =
    이니셜 `단` + 품절 배지 + `16%` + 취소선 `42,000원`. AC-001~005를 화면 수준에서 충족.
  - **round1 수정(할인율 정수연산) 유효**: `discountRate.ts:19`
    `Math.floor(((listPrice - price) * 100) / listPrice)` 그대로이고 `ProductCard.tsx:23-24`가 그 두 함수만
    쓴다(계산식 복제 없음). QA 재실측: `listPrice ≤ 3,000` **전 조합** BigInt 정확값 대조 불일치 **0건**,
    정가 100,000 기준 1~99% 불일치 **0건**, 90000/100000→`10`·80000/100000→`20`·2000/2500→`20`·
    35000/42000→`16`·390000/450000→`13`, 가드(null·동일·역전) 모두 `0`.
  - **round2 수정(staticDirs) 유효**: `storybook-static/images/products/`에 `sku-1001~1003.svg` 실존(사본
    아님, 원본 디렉터리 복사), 정적 서버 `200` 확인.
  - AC-006/007: `types.ts` 기존 5필드 이름·순서·타입 불변 + 새 2필드 끝 추가(diff 확인).
  - round4 변경 범위 = `ProductCard.stories.tsx`(09:35:45) + `.storybook/main.ts`(09:35:52) **2건뿐**
    (mtime 실측, 나머지 8파일은 09:12 이전) — Dev 기록과 일치, 소스 로직·테스트 무변경.
  - 사람 지시 3 준수: `project.env`에 `STORYBOOK_TEST_CMD` 없음 그대로(`STORYBOOK_SOURCE`·`RUN_CMD`·
    `STATIC`·`BUILD_CMD`만, git 무변경). 재작업 지시 3(크로스 리포 의존 문서화)은 사람 코멘트가
    `main.ts` 주석으로 지정했고 그대로 이행됐다(하드 실패가 나는 바로 그 파일에 전제가 적혀 있다).
- **Layer2 보안**: **pass** — round4 변경은 스토리북 전용 픽스처 1줄 + 주석 2건이다. 인증·인가·오류 계약
  무변경(`ApiKeyAuthFilter` 무변경, SR-307 공개 GET 정책 유지), 새 `fetch` 없음, `console.*` 없음
  (`src/features`·`src/components` grep 0건), `rules_check.py --func SR-306.1` **must 0 / should 0 / info 0**
  (파일 44 · 면제 1). `<img src>`의 `data:` 스킴은 스크립트 실행 벡터가 아니고 네트워크 요청도 내지 않는다 —
  운영 데이터 경로와 무관한 스토리 픽스처다. `staticDirs`가 shop-api `static/` 전체를 노출하는 low
  (round3 권고 2)는 현재 실물이 제품 SVG 3개뿐이라 그대로 유지. `imageUrl` 스킴·접두 검증 부재는 round1
  이래 "외부 URL을 허용하는 후속 SR" 몫으로 범위 밖(재게이트하지 않음).
- **Layer3 회귀**: **pass** — QA 독립 실행 `npm test`(= `tsc --noEmit` + jest) **8 스위트 / 87건 전부 통과**
  (exit 0, Dev 기록 수치 일치). 축E 23건 전부 `ok`, 오류 0 — round2에 404가 비동기로 `빈목록`에 귀속됐던
  오염도 사라졌다(`빈목록 errors: []`). `modules/shop-api` 워킹트리 **완전 무변경**(별도 저장소
  `git status --porcelain` 공백 — dev 주장대로 읽기만 했다), 워크스페이스 git status에도 DDL·매퍼·Thymeleaf
  변경 없음. 기존 스토리 4상태 이름(기본·할인·품절·이미지없음) 보존, 신규 4상태 추가로 23건.
  파일 크기: ProductCard 74 / stories 114 / discountRate 20 / ShopHomePage.test **299**줄(tsx 상한 300).
  - **사실 정정(round3 권고 3 / 재작업 지시 6)**: `story_shots` 기준선은 "404였던 시점의 픽셀"을 담고 있지
    **않다**. `.speclinker/story_shots/baseline/index.json` 실측 — 46건이 전부 주문·회원 스토리이고
    **쇼핑홈 스토리는 0건**(2026-09-16 기록, SR-302 이후 한 번도 캡처되지 않았다). 따라서 이번 라운드에
    기준선을 재기록하지 않은 것은 실질 결함이 아니고(낡은 픽셀이 없다), 남는 것은 "쇼핑홈 23건이 시각 회귀
    기준선 밖"이라는 후속 커버리지 항목이다(아래 권고 1). 축E는 픽셀을 보지 않으므로 이 게이트와 무관.
- 필수 수정: 없음.
- 권고(이번 라운드에 전부 열거 — 차단·medium 이상 **0건**, 전부 후속 TODO):
  1. **[low]** `story_shots` 기준선에 쇼핑홈 23건이 없다(위 사실 정정). SR 종결 시
     `python {{PLUGIN_PATH}}/scripts/story_shots.py capture . --force`로 한 번 기록하고 사유
     ("SR-306.1 — 쇼핑홈 상품 카드 8상태, 이미지가 실제로 서빙됨")를 남길 것.
  2. **[low]** 크로스 리포 전제(`staticDirs` → shop-api가 형제 경로에 체크아웃돼 있어야 함)가 `main.ts`
     주석에만 있다(사람 지시대로). 워크스페이스 `CLAUDE.md` 모듈 표 각주는 여전히 비어 있어, shop-web
     단독 CI를 새로 만드는 사람은 하드 실패를 본 뒤에야 알게 된다.
  3. **[low]** 플러그인 쪽에 이미 정도(正道) 수단이 들어왔다 — `{{PLUGIN_PATH}}/scripts/storybook_render.js`가
     `error-ok`(`shows-error` 태그)를 지원한다(스크립트 주석에 "2026-09-17 랩 … 3라운드 막았고"로 이 사례가
     근거로 적혀 있다). 현 data URI 우회는 유효하고 통과하므로 이번에 바꾸지 않되, 후속에 **진짜 404
     상태**를 화면으로 보여야 하면 태그 쪽이 맞다.
  4. **[low]** `ShopHomePage.test.tsx` 299줄 — tsx 상한 300(should)에 1줄 남았다. 다음에 테스트를 더할 때 분할.
  5. **[low]** `이미지있음` = `정가없음` 동일 args(SKU-1002)는 round2의 명시 유지 결정 그대로 — 재게이트 안 함.
  6. **[low]** `storybook-static/assets/`에 구 번들(`ProductCard.stories-CFDUQa-G.js`, 404 경로 포함)이 남아
     있다. 참조되지 않아 무해하지만 storybook build가 출력 디렉터리를 비우지 않는다는 뜻이고, 축E가
     `rebuilt: false`로 신선도 검사를 건너뛰는 경로와 겹치면 혼동을 만든다 — 기준선 재기록 전에
     `storybook-static/`을 삭제하고 한 번 빌드할 것.
- 재동기화 입력(STEP 5.5 몫 — QA 권고 아님):
  1. `UIS-ORD-008` — round1~3과 동일 항목(이번 라운드에도 변동 없음). 본문이 "코드(라우트) 기준 재동기화
     골격"이라 상품 카드의 정가 취소선·할인율 배지(**정수 연산 내림 정수 %**, 0% 숨김)·이미지 대체 영역
     (없음/로드실패) 상태가 스펙 본문에 없고, STORY 수용 기준의 `UIS-ORD-008: [미상]`도 비어 있다.
  2. `UIS-ORD-008 §5`(표시 조건 ↔ 스토리 링크)가 **0행**이다(`story_gate` 실측: `linked: false`, 스토리 23건).
     이번 SR로 상품 카드 상태가 4→8건이 됐으니 5.5에서 §5 행과 `[이름](story:ID)` 링크를 함께 채울 것.

## 재작업 지시
> round 8 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [high/stories] round2 축E FAIL이 해소되지 않았다. staticDirs 수정으로 sku-1001~1003.svg 404 6건은 실제로 사라졌으나(QA 독립 실측: build 산출물에 이미지 복사됨, 200 확인), QA가 직접 실행한 story_gate.py는 여전히 verdict=fail이다 — 남은 1건 '쇼핑홈-상품-카드--이미지로드실패 — Failed to load resource: 404'. 축E 렌더러(storybook_render.js:71-72)는 favicon 외 콘솔 error 1건이면 그 스토리를 깨진 것으로 세고 '의도적 404'를 예외로 둘 수단이 없어(renders-nothing 태그는 빈 렌더 전용), 사람 코멘트가 지시한 '잘못된 경로를 그대로 둔다'를 지키면 이 스토리는 구조적으로 통과할 수 없다. round2에 이 스토리가 통과로 보인 것은 404 콘솔 error가 비동기로 다음 스토리(빈목록)에 귀속됐기 때문으로, round3가 만든 새 결함이 아니라 미해소 high다. 확정답변 9가 이 상태를 명시해 스토리 삭제는 스펙 위반이다. → ProductCard.stories.tsx의 이미지로드실패 imageUrl을 네트워크 404 없이 onerror를 일으키는 값으로 교체한다 — QA 실측(_tmp/qa_img_probe.js, 축E와 동일 판정 기준)으로 data:image/png;base64,AAAAAAAA 는 onerror 발생 + 콘솔 error 0건 확인. 상태 이름·의미는 유지하고 사유를 JSDoc에 남긴 뒤, 검증은 python {{PLUGIN_PATH}}/scripts/story_gate.py {{WS}} --func SR-306.1 이 verdict=pass 가 되는 것으로 한다.
2. [medium/spec] Dev 기록이 3라운드 연속 `npx test-storybook`(18 스위트/69건 통과)으로 화면 축 통과를 대리 증명했으나 이 대상 project.env에는 STORYBOOK_TEST_CMD가 없다(STORYBOOK_SOURCE·RUN_CMD·STATIC·BUILD_CMD만 존재). 따라서 축E는 storybook_render.js(콘솔 error 1건 = 실패)를 쓰며 test-runner와 실패 기준이 다르다 — round2·round3의 '69건 통과'가 축E FAIL과 공존한 것이 그 증거다. 잘못된 도구로 검증해 온 것이 라운드가 반복된 직접 원인이다. → 다음 라운드부터 화면 축은 python {{PLUGIN_PATH}}/scripts/story_gate.py {{WS}} --func SR-306.1 로 직접 확인하고 그 verdict를 Dev 기록에 적는다.
3. [medium/regression] staticDirs: ['../../shop-api/src/main/resources/static'] 가 별도 git 저장소인 shop-web에 shop-api 디렉터리 존재를 빌드 전제로 심는다. Storybook 10.6.0은 경로가 없으면 'Failed to load static files, no such directory'로 하드 실패하므로(설치본 dist에서 메시지 확인), shop-web 단독 체크아웃/CI에서는 storybook dev·build 양쪽이 죽고 축E가 스토리 1건이 아니라 축 전체 unknown(판정 불능)으로 꺼진다. → 사본을 만들지 않는 결정은 유지하되 크로스 모듈 의존을 한 줄로 문서화한다(워크스페이스 CLAUDE.md 모듈 표 각주 또는 shop-web package.json/README 주석). 코드 변경이 없어 필수수정 1과 같은 라운드에 처리한다.
4. [low/stories] 축E 렌더러에 '이 스토리는 콘솔 error가 정상'을 선언할 수단이 없다(renders-nothing은 빈 렌더 전용). 필수수정 1은 워크스페이스 쪽 우회이고 근본은 플러그인 개선 여지다. → expects-console-error 류 태그 도입, 또는 STORYBOOK_TEST_CMD 채택(축E 설계가 권하는 더 강한 판정)을 사람이 판단. 단 후자는 test-runner가 기동된 스토리북을 요구해 AIDD 잡 중 shop-web dev 서버 기동 금지(CLAUDE.md OOM 실측)와 충돌한다. 이번 SR에서는 고치지 않는다.
5. [low/security] staticDirs가 shop-api static/ 디렉터리 전체를 스토리북 dev 서버와 storybook-static/ 산출물로 공개한다. 현재 실물은 제품 SVG 3개뿐이라(실측) 노출 위험 없음. → 그 디렉터리에 비공개 자산을 두게 되면 staticDirs를 .../static/images 처럼 필요한 하위 경로로 좁힌다(후속 TODO).
6. [low/regression] story_shots 기준선(.speclinker/story_shots/baseline/)은 이미지가 404였던 시점의 픽셀을 담고 있다. round3로 할인·정가없음/이미지있음·할인0퍼센트와 그리드 목록있음의 렌더 결과가 실제로 달라졌다(깨진 이미지 → 실 SVG). → 필수수정 1까지 끝난 뒤 기준선을 한 번에 재기록하고 사유('SR-306.1 — 상품 이미지가 스토리북에서 실제로 서빙됨')를 남긴다.
7. [low/regression] round1 권고 2 / 재작업 지시 7(ProductCard.stories.tsx의 이미지있음이 정가없음과 동일 args, SKU-1002)은 round2에서 명시적 유지 결정이 났고 round3도 무변경 — 사실 기록이며 차단 아님. → 없음(후속 정리 항목으로 유지, 재게이트하지 않는다).

사람 코멘트: QA 제안대로 반영. 이미지 로드 실패 상태는 없애지 않는다(확정 문답 9). 대신 네트워크 404 대신 깨진 data URI(data:image/png;base64,AAAAAAAA)를 써서 onError 폴백은 그대로 검증하고 콘솔 오류는 남기지 않는다. 1) 이미지로드실패 스토리의 imageUrl을 깨진 data URI로 교체하고 왜 404 경로를 쓰지 않는지 한 줄 주석(축E가 콘솔 오류를 실패로 본다)을 남긴다. 2) .storybook/main.ts staticDirs에 shop-api 저장소가 형제 경로에 있어야 한다는 전제를 주석으로 적는다. 3) project.env STORYBOOK_TEST_CMD는 건드리지 말 것(사람이 별도 판단). 완료 조건: 축E(story_gate.py)로 전 스토리 통과 확인 — test-storybook 통과만으로 판단하지 말 것. 이미지 로드 실패 스토리가 여전히 이니셜 대체 영역을 보여주는지 단언.
