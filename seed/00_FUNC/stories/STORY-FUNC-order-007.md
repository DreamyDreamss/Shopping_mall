---
story-id: STORY-FUNC-order-007
func-id: FUNC-order-007
status: Done
domain: order
created: 2026-09-09
spec_markers: 0
sr-id: SR-214
approved_sha: 2079eb6b264c
---

# STORY-FUNC-order-007 — 판매중 상품 목록 조회

## Story
판매중 상품 목록 조회


## 변경 컨텍스트 (SR-214)
> 이 story는 변경요청 **SR-214 — SR-214** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-214/00_요구사항.md`

### 확정된 요건 문답 8건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 상품 목록이 항상 등록순으로만 나온다. 정렬 셀렉트를 넣어 사용자가 고를 수 있게 한다. - 선택지: 최신순(기본)·가격 낮은순·가격 높은순 - 선택은 URL 파라미터로 유지된다(새로고침·뒤로가기에도 남는다) - 정렬 값이 이상하면 기본값으로 조용히 되돌린다(오류 화면 금지) / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 조회 결과 전부(데이터 계약 불변)
- **기존 클라이언트와의 하위호환이 필요한가?** — 필드 추가·표시 변경만이라 하위호환이 유지된다. 기존 필드명·타입·의미는 그대로 둔다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 이 SR은 새 오류 계약을 만들지 않는다(요구 본문에 코드가 명시된 경우 그 코드를 따른다). 기존 오류 응답 형식·상태코드는 그대로다.
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음
- **기존 데이터 이관·백필이 필요한가?** — 불필요(신규 데이터만)
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 요구 본문에 나열된 화면이 전부다. 같은 데이터를 쓰는 다른 화면은 이번 범위가 아니다.
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 빈 값은 '-'로, 조회 결과 0건은 안내 문구로 표시한다. 오류는 배너로 따로 알린다.

## 수용 기준 (Acceptance Criteria)
- [ ] INF-ORD-008 (GET /api/products): `sale_yn = 'Y'` 상시필터 → 판매중 상품만 반환(단건 조회인 [[INF-ORD-009]]는 이 필터가 없음)
- [ ] INF-ORD-008 (GET /api/products): [반영: FUNC-order-007] `keyword` 파라미터가 있으면 `product_name` 부분 일치(LIKE) 검색 —
- [ ] INF-ORD-008 (GET /api/products): [반영: FUNC-order-007](SR-220) `inStock=true`면 `stock_qty >= 1` 조건을 `sale_yn = 'Y'` 상시필터와

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-008: > [변경: SR-201] 2026-08-22 / INF-ORD-008: GET /api/products — 판매중 상품 목록 조회 / > **개요:** 판매중 상태인 상품 전체를 SKU 순으로 조회한다. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/ProductController.java:24-31` — [docs/05_설계서/order/INF/INF-ORD-008.md](../../05_설계서/order/INF/INF-ORD-008.md)
- **SCH** SCH-ORD-005: SCH-ORD-005: products / > **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** [TBD] | **화면:** [TBD] / **근거 소스:** `DB 실측(db-main MCP describe)` — 컬럼 타입·NULL·기본값은 MariaDB(sl_lab) 실스키마 조회로 사실화(P1/SR-202 enrichment). [미확인] 헤더 잔존은 자기모순이라 정정(OBS-018, 2026-08-23) / 컬럼 설명 — [docs/05_설계서/order/SCH/SCH-ORD-005.md](../../05_설계서/order/SCH/SCH-ORD-005.md)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)
- **기존 구현 파일**: modules/shop-api/src/main/java/com/sm/lab/shop/controller/ProductController.java, modules/shop-api/src/main/java/com/sm/lab/shop/dao/ProductDao.java, modules/shop-api/src/main/java/com/sm/lab/shop/service/ProductService.java, modules/shop-api/src/test/java/com/sm/lab/shop/controller/ProductControllerTest.java, modules/shop-api/src/test/java/com/sm/lab/shop/dao/ProductDaoTest.java, modules/shop-api/src/test/java/com/sm/lab/shop/service/ProductServiceTest.java

## 📏 적용 규칙 (JIT — .claude/rules)
> 이 FUNC가 건드리는 파일에 적용되는 프로젝트 규칙이다. 전문은 아래 파일을 **직접 Read**하고 지킬 것 — `must` 위반은 STEP 5.3 축 C(`rules_check.py`)가 차단한다. 정본: 워크스페이스 `.claude/rules/`(뷰어 [rules]에서 편집).

| 규칙 | severity | 파일 |
|---|---|---|
| 콘솔 출력 금지 | must | `.claude/rules/lab/no-sysout.md` |

## 🔧 쿼리 작성 가이드 (JIT — 실쿼리 관찰)
> AIDD로 쿼리/DAO 생성 시 준수. 소스 SQL에서 채굴한 사실(논리 FK·상시필터). 구조화 원천: `docs\05_설계서\_machine\query_patterns.json`.

**조인 경로 (논리 FK — DB 미선언이라도 코드에서 관찰됨)**
| A.컬럼 | = | B.컬럼 | 관찰 |
|--------|---|--------|------|
| CART_ITEMS.SKU | = | PRODUCTS.SKU | 4 |
| ORDER_ITEMS.SKU | = | PRODUCTS.SKU | 2 |

**상시 필터 (누락하면 결과가 틀어진다 — soft-delete·테넌트 스코프)**
| 테이블 | 조건 | 빈도 |
|--------|------|------|
| PRODUCTS | SALE_YN = 'Y' | 2 |

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

**SR-214 — 상품 목록 정렬 선택** (dev-agent, 2026-09-09)

**결론: 코드 변경 없음.** FUNC-order-007(REST API `GET /api/products`)은 이번 SR-214 라운드에서
손댈 대상이 아니다.

근거:
1. 사촌 FUNC인 FUNC-order-009(화면, `docs/00_FUNC/stories/STORY-FUNC-order-009.md`)의 Dev
   기록에 이미 사람 승인·QA(round 5/6) 완료된 명시적 결정이 있다: "REST API(`/api/products`,
   INF-ORD-008, FUNC-order-007)는 이 SR 범위 밖이라 전혀 건드리지 않음 — 정렬은 화면
   (`/product/list`) 전용 기능으로 한정."
2. 실제 코드 확인: `ProductController.java`/`ProductService.java`/`ProductDao.java`에는 `sort`
   파라미터가 없다. `normalizeSort`/`listSorted`/`selectProductsForList`는 전부
   `linked_func: FUNC-order-009`로 태깅되어 `ProductViewController.java`(화면 컨트롤러) 경로에만
   존재하고, `ProductController`(REST)는 호출하지 않는다.
3. `docs/05_설계서/order/INF/INF-ORD-008.md`(이 FUNC의 REST API 스펙)에도 `sort` 파라미터가
   없다 — SR-201(keyword)·SR-220(inStock) 반영만 있고 SR-214 흔적이 없어 스코프 밖이라는
   근거와 일치.
4. 이 FUNC의 기존 수용 기준(`sale_yn='Y'` 상시필터·keyword LIKE·inStock)은 이미 전부 구현·
   테스트되어 있고(`ProductControllerTest`/`ProductServiceTest`/`ProductDaoTest`), SR-214로
   인한 회귀 위험도 없다(REST API 3파일 무변경 확인).
5. SR 원장(`docs/변경관리/_sr_ledger.jsonl`)의 SR-214 최신 스냅샷은 FUNC-order-007/
   FUNC-order-009 둘 다 claim하지만, round 1 QA 재작업 지시(IN_QA→IN_DEV revert)는
   STORY-009의 "재작업 지시" 섹션(화면 라벨 텍스트 "최신순"→"기본순" 정정만)에만 있고
   FUNC-007에 대한 지시는 없다.

수정 파일: 없음(프로덕션 코드·테스트 무변경). 이 파일(Dev 기록)만 갱신.

후속: STEP 5.5에서 "## 수용 기준" 절이 코드 기준으로 갱신될 때 SR-214 관련 항목이 이 FUNC에는
추가되지 않아야 정합적이다(정렬은 FUNC-order-009 소유).

## Test 결과 (test-agent, 2026-09-09)

### TC 작성 및 실행 요약
- **총 TC 50개 실행**: 50/50 통과 (통과율 100%)
- **신규 AC3 회귀 테스트**: 2개 추가 및 실행 완료

### AC 매핑 TC 커버

| AC | 검증 내용 | TC-ID | 테스트 함수 | 상태 |
|----|---------|-------|-----------|------|
| AC1 | sale_yn='Y' 상시필터 | TC-FUNC-order-007-03 | `selectProducts_withoutKeyword_returnsAllOnSaleSortedBySku()` | ✅ |
| AC2 | keyword 파라미터 LIKE 검색 | TC-FUNC-order-007-01 | `selectProducts_withKeyword_returnsPartialMatchOnly()` | ✅ |
| AC2 | keyword 미매칭 → 빈 배열 | TC-FUNC-order-007-02 | `selectProducts_withKeywordNoMatch_returnsEmpty()` | ✅ |
| AC3 | inStock=true → stock_qty>=1 필터 | TC-FUNC-order-007-06 | `selectProducts_withInStockTrue_returnsOnlyPositiveStock()` | ✅ |
| AC3 | inStock=false/null → 하위호환 | TC-FUNC-order-007-07 | `selectProducts_withInStockFalseOrNull_returnsSameAsNoFilter()` | ✅ |

### AC3 회귀 검증 (SR-214 재오픈 라운드 신규)

**배경**: QA의 concerns 지적에 따라 AC3(inStock 필터) 실효성 검증을 강화.

| TC-ID | 검증 항목 | 구현 방식 | 결과 |
|-------|---------|---------|------|
| TC-FUNC-order-007-08 | inStock=true가 품절(sale_yn='Y', stock_qty=0) 행도 제외 | `@Transactional`+`@Sql` 픽스처 삽입 후 대조 | ✅ 통과 |
| TC-FUNC-order-007-09 | inStock=null/false가 품절 행 포함 (기존 동작 유지) | 동일 픽스처로 하위호환 검증 | ✅ 통과 |

**기술 상세**:
- SQL 파일: `src/test/resources/sql/insert-out-of-stock.sql` — `sale_yn='Y' AND stock_qty=0` 행 생성
- 테스트 메서드: `ProductDaoTest` 라인 117~144
- `@Transactional` 자동 롤백으로 cleanup 불필요
- 랩 고정 데이터(SKU-1001~1003)와 임시 픽스처(SKU-TEST-OOS) 함께 검증

### 테스트 실행 (2026-09-09)

```bash
cd {{WS}}\modules\shop-api
mvnw.cmd -o test -Dtest='Product*Test'
```

**결과**:
```
Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**클래스별**:
- ProductControllerTest: 6/6 ✅
- ProductViewControllerTest: 24/24 ✅ (FUNC-order-009 정렬 기능 포함)
- ProductDaoTest: 11/11 ✅ (기존 9개 + AC3 회귀 2개 신규)
- ProductServiceTest: 9/9 ✅

### 메모 및 미처리 사항

**미처리 (별도 SR 필요, 이번 라운드 스코프 外)**:
1. **QA 권고2: `product.xml` 상시필터 공통화** — `selectProducts`/`selectProductsForList`의 중복 WHERE를 `<sql id="productListWhere">` 공유 필요
2. **selectProductsForList에 inStock 분기 부재** — FUNC-order-009 소유의 화면 정렬 쿼리에는 inStock 필터가 없음. 의도된 것인지 명시(주석/스펙) 필요

**판정**: ✅ **AC 전부 검증 완료** — 회귀 리스크 2건(AC3 무보호 + 상시필터 이중화) 중 AC3은 실효 테스트로 보호. 상시필터 공통화는 설계 개선사항이지 현행 결함이 아님.

---

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-09 (SR-214 재오픈 라운드) — CONCERNS

> **판정 요지**: dev-agent의 **결론("SR-214는 REST API 계층 변경 불필요")은 타당하다** — 스코프 판정
> 오류가 아니다. 다만 그 결론에 도달한 **근거 5건 중 2건이 사실과 다르고**, 그 부정확한 근거가
> FUNC-007이 실제로 떠안은 위험 2건(상시필터 계약 이중화 · AC3 무보호 테스트)을 "이미 해결됨"으로
> 덮었다. 차단하지 않되 기록한다.

- **Layer1 스펙**: concerns.
  - **SR-214 요건 중 REST 계층에 필요한 것은 없음 — 확인**. SR-214 요구 본문(`docs/변경관리/SR-214/00_요구사항.md`)의
    3개 요건(정렬 셀렉트 UI · URL 파라미터 유지 · 잘못된 값 조용한 폴백)은 전부 **화면 표현 계층** 요건이다.
    확정문답 `scr_scope`("요구 본문에 나열된 화면이 전부다. 같은 데이터를 쓰는 다른 화면은 이번 범위가 아니다")와
    `api_compat`("기존 필드명·타입·의미는 그대로 둔다")가 REST 계약 변경을 명시적으로 배제한다.
    화면 `/product/list`는 Thymeleaf 서버렌더로 `ProductService`를 **직접** 호출하고 `/api/products`를
    경유하지 않으므로(`ProductViewController.java:51` → `productService.listSorted`), REST에 `sort`를
    추가할 기능적 필요가 없다. **INF-ORD-008에 누락된 SR-214 요건 없음 — 스펙 누락 아님.**
  - **[정정] Dev 근거 2는 사실과 다르다.** "`normalizeSort`/`listSorted`/`selectProductsForList`는 전부
    `ProductViewController.java`(화면 컨트롤러) **경로에만 존재**"라고 적었으나, 실측상 이 셋은 전부
    **FUNC-007 소유 파일 안에** 물리적으로 산다: `ProductService.java:17-18,32-51`(파일 헤더
    `linked_func: FUNC-order-007`), `ProductDao.java:23-26`(동일), `product.xml:43-58`(동일).
    멤버 단위 `linked_func: FUNC-order-009` 주석만 붙어 있을 뿐이다. **분리된 것은 파일이 아니라 호출
    그래프**다 — `ProductController.list()`는 이들을 호출하지 않음(전수 grep 확인). 결론은 유지되지만
    근거는 정정되어야 한다.
  - **[정정] Dev 근거 4의 "REST API 3파일 무변경 확인"도 사실이 아니다.** SR-214 정렬 작업 커밋
    `52a6313`(shop-api 저장소)이 `ProductService.java`(+32줄)·`ProductDao.java`(+5줄)·`product.xml`(+17줄)을
    **실제로 수정**했다. 정확한 서술은 "3파일 무변경"이 아니라 **"REST 엔드포인트 동작 무변경(신규 멤버가
    추가됐을 뿐 기존 호출 경로는 불변)"**이다.
  - **[관찰] 커밋 메시지 귀속이 뒤바뀌어 있다** — `git log -S` 실측: 커밋 `607e9bd`("SR-214" 제목)에 든 것은
    **SR-220의 inStock 코드**이고, 커밋 `52a6313`("SR-220" 제목)에 든 것이 **SR-214의 정렬 코드**다.
    커밋 메시지에 의존하는 하위 귀속 검사(scope_verify 등)는 두 FUNC 모두에서 오판할 수 있다
    (FUNC-009가 이미 같은 유형으로 scope waive를 받은 바 있음).
  - **[관찰] 이 FUNC는 SR-214·SR-220 양쪽에 claim되어 있다** — `GATE-FUNC-order-007.json`
    `story_transitions`에 `SR 승인(SR-214)`(07:35:50)·`SR 승인(SR-220)`(07:36:22)이 나란히 기록됐고,
    round 4 사람 승인 코멘트도 "SR-214 범위 내 구현 확인"인데 실제 round 4 내용물은 inStock(SR-220)이었다.
    "SR-214가 FUNC-007에 지시한 작업이 없다"는 dev 결론은 **이번 라운드 재작업 지시 부재** 근거로는 맞지만,
    "claim 자체가 없다"는 뜻은 아니다.
  - story AC 3건이 여전히 `- [ ]` 미체크인데 status는 Review다(사촌 009는 `[x]`로 갱신). 직전 라운드에서도
    동일 — 판정 근거를 AC 체크가 아니라 코드 실측에 의존해야 했다.

- **Layer2 보안**: **pass**.
  - 이번 라운드 **코드 변경 0건** → 새 공격면 없음.
  - **인증·인가 실측**: `/api/products`는 `ApiKeyAuthFilter`의 `isOpenRoute` 화이트리스트(`/error`,
    `/favicon.ico`, `/cart**`, `/order/**`, `/product/**`, `/api/members/grades`)에 **없다** → default-deny로
    `X-Api-Key` 필수. INF-ORD-008 오류표의 401 계약과 정합. member 스코프 키는 일반 규칙(memberId 토큰 대조)에
    걸리며 products 자원은 memberId 토큰이 없어 ALLOW — IDOR 표면 아님.
  - **ORDER BY 주입이 FUNC-007 경로에 닿지 않음**: `sort`는 REST 컨트롤러·테스트 어디에도 없고
    (`grep` 0건), 신규 `selectProductsForList`도 `${}` 보간 없이 `<choose>` 정적 SQL 조각으로만 분기한다.
  - 기존 추적분 유지(확대 없음): `keyword` LIKE 메타문자(`%`,`_`) 미이스케이프·길이 제한 없음(round 1~4 등록분).
  - 참고(이번 SR 소관 아님): 같은 PRODUCTS 데이터가 `/api/products`는 인증 필수인데 화면 `/product/**`는
    무인증(설계상 R-4)으로 노출된다. 공개 카탈로그라 실질 위험은 낮고 SR-214가 만든 것도 아니다.

- **Layer3 회귀**: concerns(차단 없음).
  - **독립 실측**: `mvnw.cmd -o test -Dtest='Product*Test'` **exit 0** — `ProductControllerTest` 6,
    `ProductViewControllerTest` 24, `ProductDaoTest` 9, `ProductServiceTest` 9 = **48 tests / 0 failures / 0 errors**
    (surefire 리포트 직접 파싱).
  - **기존 AC 무손상 확인**: AC1 `sale_yn='Y'`(`product.xml:13`, `ProductDaoTest` 무키워드·비판매상품 케이스),
    AC2 keyword LIKE(`product.xml:14-16`, DAO 3건 + 컨트롤러 2건), AC3 `inStock`(`product.xml:17-19`,
    컨트롤러 2건). `ProductController`·`ProductControllerTest`는 이번 라운드 무변경 — REST 계약 불변.
  - **[carry-over, medium] AC3(inStock)은 구현돼 있으나 테스트가 실효적으로 보호하지 못한다 — DB 실측으로 재확인.**
    `db-main` 조회 결과 PRODUCTS 4행 중 `sale_yn='Y'`인 3행의 `stock_qty`는 각각 12·40·7이고, `stock_qty=0`인
    유일한 행(SKU-1004)은 이미 `sale_yn='N'`으로 제외된다. 따라서
    `selectProducts(null,true)`와 `selectProducts(null,null)`의 결과 집합이 **항상 동일**하고,
    `ProductDaoTest:68`의 `allMatch(p -> p.getStockQty() >= 1)`은 **항상 참**이다.
    `product.xml:17-19`의 `<if test="inStock...">` 블록을 통째로 지워도 48건이 전부 통과한다.
    → **Dev 근거 4의 "기존 수용 기준…은 이미 전부 구현·테스트되어 있고"는 AC3에 대해 과장**이며,
    이 FUNC 자신의 게이트(`GATE-FUNC-order-007.json` round 4 top_issues[0], medium, 미해소)와 모순된다.
  - **[신규, medium] FUNC-007이 소유한 상시필터 계약이 이중화됐고 이미 갈라졌다.** AC1(`sale_yn='Y'`)·
    AC2(keyword LIKE)가 이제 FUNC-007 소유 `product.xml` 안에서 `selectProducts`(10-21)와
    `selectProductsForList`(46-58) **두 곳에 복제**돼 있는데, 후자에는 **AC3의 `inStock` 분기가 없다**.
    즉 FUNC-007의 AC 3종을 온전히 만족하는 쿼리는 하나뿐이다. 현재는 화면이 `inStock`을 보내지 않아
    실동작 결함이 없으나, **FUNC-007이 다음에 상시필터를 바꾸면(예: `del_yn` 추가, `sale_yn` 의미 변경)
    복제본은 조용히 갈라지고 이를 잡아낼 테스트가 없다.** 사촌 009 라운드에서 low로 후속 이관됐지만,
    **계약의 소유자는 FUNC-007**이므로 이 FUNC의 회귀 리스크로 재등록한다.
  - **[carry-over, low] FUNC-007 소유 파일에 거짓 javadoc이 이번 라운드에도 방치됨.**
    `ProductService.java:53-56`의 `list(String)` 오버로드 주석은 "inStock 미지정 호출부(예: FUNC-order-009
    화면 컨트롤러)는 변경 없이 그대로 사용"이라 서술하지만, **전수 grep상 호출부 0건**이다
    (`ProductController`→`list(keyword,inStock)`, `ProductViewController`→`listSorted`, 테스트 전부 2-arg).
    009의 QA r5 권고4가 "**메서드 제거는 FUNC-order-007 소유라 별도 SR**"로 이 FUNC에 명시 이관한 항목인데,
    FUNC-007이 열린 이번 라운드의 Dev 기록에 언급조차 없다 — 사람이 009에서 후속 수용한 건이라 차단하지는
    않으나, **"내 파일에 SR-214가 남긴 것이 없는가"를 점검하지 않은 스코프 판정의 사각지대**다.

- **필수 수정(FAIL시)**: 없음 — 차단 이슈 없음. "0개 파일 변경"은 **정당한 결론**이다(요건·확정문답·호출그래프
  3중 근거로 확인).
- **권고(CONCERNS시)**:
  1. **[medium] AC3(inStock) 실효 테스트 부재(round 4에서 이월, 미해소)** — 랩 데이터상 `sale_yn='Y'` 행이
     전부 재고 보유라 현재 단언이 공허하다. `ProductDaoTest`에 `@Transactional`(롤백) + `@Sql`로
     `sale_yn='Y' AND stock_qty=0` 픽스처 행을 넣고 `inStock=true`에서 제외 / `null·false`에서 포함을
     대조하는 테스트를 추가할 것. 이것이 없으면 AC3은 회귀 시 무방비다.
  2. **[medium] `product.xml` 상시필터 공통화** — `selectProducts`/`selectProductsForList`의 중복 WHERE를
     `<sql id="productListWhere">` 조각으로 공유해, FUNC-007이 소유한 상시필터를 한 곳에서만 고치게 할 것.
     동시에 `selectProductsForList`의 `inStock` 분기 부재가 **의도된 것인지** 명시(주석 또는 스펙)할 것.
  3. **[low] Dev 기록 근거 2·4 정정** — "화면 컨트롤러 경로에만 존재"→"FUNC-007 소유 파일에 상주하되
     호출 그래프가 분리됨", "REST API 3파일 무변경"→"REST 엔드포인트 동작 무변경(파일은 신규 멤버 추가로 변경됨)".
     다음 세션이 "FUNC-007 파일은 안 건드려졌다"로 오독하면 위 권고 2의 위험을 놓친다.
  4. **[low] `ProductService.list(String)` javadoc 정정** — 호출부 0건이므로 "FUNC-order-009 화면
     컨트롤러가 사용" 서술을 제거. 메서드 자체 제거는 공개 API 축소라 별도 SR 유지 권장.
  5. **[low] story AC 갱신** — AC 3건 체크박스가 미체크 상태로 Review에 진입했다. 이번 라운드가 무변경임을
     AC 옆에 한 줄(예: "SR-214 무변경 — 정렬은 FUNC-order-009 소유")로 남기고 체크를 갱신할 것.
  6. **[low] 커밋 귀속 혼선 기록** — `607e9bd`(제목 SR-214)↔`52a6313`(제목 SR-220)의 내용물이 뒤바뀌어 있다.
     SR 원장 또는 게이트 처리 기록에 한 줄 남겨 후속 scope_verify 오판을 예방할 것.
