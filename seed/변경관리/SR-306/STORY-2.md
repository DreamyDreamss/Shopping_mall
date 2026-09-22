---
story-id: STORY-SR-306.2
item: SR-306.2
title: 정가·이미지 컬럼과 조회 응답 필드 추가(시드 포함)
status: Done
domain: order
created: 2026-09-17
spec_markers: 0
sr-id: SR-306
approved_sha: 6edfc142ba46
---

# STORY-SR-306.2 — 상품 정가·대표 이미지 데이터 추가 — 쇼핑 화면이 할인·이미지를 실제로 표시 — 정가·이미지 컬럼과 조회 응답 필드 추가(시드 포함)

## Story
상품 정가·대표 이미지 데이터 추가 — 쇼핑 화면이 할인·이미지를 실제로 표시 — 정가·이미지 컬럼과 조회 응답 필드 추가(시드 포함)


## 변경 컨텍스트 (SR-306)
> 이 story는 변경요청 **SR-306 — 상품 정가·대표 이미지 데이터 추가 — 쇼핑 화면이 할인·이미지를 실제로 표시** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-306/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-306/02_변경명세.md`

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
- **SR-306 #1** 상품 카드에 정가·할인율·이미지 표시 — Blocked-SpecIncomplete

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-306/02_변경명세.md`에서 도출)
- [x] INF-ORD-008: [미상] — 위 SR-306 절의 요지 중 이 스펙에 해당하는 변경을 사람이 적는다
- [x] INF-ORD-009: [미상] — 위 SR-306 절의 요지 중 이 스펙에 해당하는 변경을 사람이 적는다
- [x] SCH-ORD-005: [미상] — 위 SR-306 절의 요지 중 이 스펙에 해당하는 변경을 사람이 적는다

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**
- [x] INF-ORD-008 (GET /api/products): `sale_yn = 'Y'` 상시필터 → 판매중 상품만 반환(단건 조회인 [[INF-ORD-009]]는 이 필터가 없음)
- [x] INF-ORD-008 (GET /api/products): [반영: FUNC-order-007] `keyword` 파라미터가 있으면 `product_name` 부분 일치(LIKE) 검색 —
- [x] INF-ORD-008 (GET /api/products): [반영: FUNC-order-007](SR-220) `inStock=true`면 `stock_qty >= 1` 조건을 `sale_yn = 'Y'` 상시필터와
- [x] INF-ORD-009 (GET /api/products/{sku}): 목록 조회([[INF-ORD-008]])와 달리 `sale_yn` 필터가 없다 — 판매종료 상품도 SKU를 알면 조회 가능

> 변경명세에 스펙 ID가 없는 절 — 이 항목 몫인지 확인해 AC로 옮긴다: SR-306 (요구사항 요지 — 전 스펙 공통)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-008: > [변경: SR-201] 2026-08-22 / INF-ORD-008: GET /api/products — 판매중 상품 목록 조회 / > **개요:** 판매중 상태인 상품 전체를 SKU 순으로 조회한다. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/ProductController.java:24-31` — [docs/05_설계서/order/INF/INF-ORD-008.md](../../05_설계서/order/INF/INF-ORD-008.md)
- **INF** INF-ORD-009: INF-ORD-009: GET /api/products/{sku} — 상품 단건 조회 / > **개요:** SKU로 상품 1건을 조회한다. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/ProductController.java:27-31` / 요청 — [docs/05_설계서/order/INF/INF-ORD-009.md](../../05_설계서/order/INF/INF-ORD-009.md)
- **SCH** SCH-ORD-005: SCH-ORD-005: products / > **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** [TBD] | **화면:** [TBD] / **근거 소스:** `DB 실측(db-main MCP describe)` — 컬럼 타입·NULL·기본값은 MariaDB(sl_lab) 실스키마 조회로 사실화(P1/SR-202 enrichment). [미확인] 헤더 잔존은 자기모순이라 정정(OBS-018, 2026-08-23) / 컬럼 설명 — [docs/05_설계서/order/SCH/SCH-ORD-005.md](../../05_설계서/order/SCH/SCH-ORD-005.md)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)


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

## 구현 계획
- **파일**:
  - `modules/shop-api/src/main/resources/db/V10__product_list_price_image.sql`(신규) — PRODUCTS에 `list_price`·`image_url` 컬럼 추가(ALTER, IF NOT EXISTS) + 기존 시드 4행(SKU-1001~1004) 값 채우기(UPDATE).
  - `modules/shop-api/src/main/resources/application.yml` — `spring.sql.init.schema-locations` 목록 맨 끝에 `classpath:db/V10__product_list_price_image.sql` 이어붙이기(기존 9개 항목 순서 불변, V6/V9 선례와 동일 관례).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/domain/Product.java` — `Long listPrice`, `String imageUrl` 필드+getter/setter 추가(기존 5필드 불변).
  - `modules/shop-api/src/main/resources/mapper/product.xml` — `selectProducts`(INF-ORD-008 소유)·`selectBySku`(INF-ORD-009 소유) 두 SELECT의 컬럼 목록에 `p.list_price, p.image_url`을 명시 추가(규칙 `no-select-star` — 컬럼 나열). `selectProductsForList`(FUNC-order-009, Thymeleaf 정렬 화면 전용)는 이 항목 스코프 밖이라 건드리지 않는다 — 주석으로 "이 쿼리는 list_price/image_url 미포함(SR-306.2 스코프 밖)"을 남겨 다음 사람이 자동으로 채워질 거라 오인하지 않게 한다.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/ProductImageStaticResourceConfig.java`(신규) — `/images/**` → `classpath:/static/images/` 정적 서빙 등록. `ShopStaticResourceConfig`와 같은 계열이지만 SPA 폴백이 없는 단순 버전(커스텀 `PathResourceResolver` 없이 Spring 기본 `ResourceHttpRequestHandler`만 사용 — 경로 이탈 방지는 기본 구현에 이미 있음). 컨트롤러·서비스가 아니므로 `controller-has-test`/`service-has-test` 대상 아님.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — `isOpenRoute`에 `|| path.startsWith("/images/")` 한 줄 추가(주석: 정적 이미지 자산은 회원 소유권 판정 대상이 아닌 순수 서빙이라 `/shop`과 동일 이유로 화이트리스트 — SR-307 #1 antipattern이 경고한 "판정이 필요한 `/api/**`를 화이트리스트로 스킵"과는 조건이 다름).
  - 이미지 자산(신규, 자체 제작 단색 대체 SVG — 상용 이미지·문구 금지): `modules/shop-api/src/main/resources/static/images/products/sku-1001.svg`(스탠딩 데스크, 예: 배경 `#4A5568` + 상품명 텍스트), `sku-1002.svg`(기계식 키보드, 배경 `#2C5282`), `sku-1003.svg`(4K 모니터, 배경 `#276749`). SKU-1004용 파일은 만들지 않는다 — "이미지 없음" 실데이터 상태를 의도적으로 남긴다.
  - 테스트: `ProductControllerTest.java`(기존 수정, 필드 단언 추가) · `ProductDaoTest.java`(기존 수정, 실DB 시드값 단언 추가) · `ProductImageStaticResourceServingTest.java`(신규, `ShopStaticResourceServingTest`와 동형).

- **데이터**:
  - DDL: `ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS list_price BIGINT NULL COMMENT '정가(원, 표시전용) — 판매가(price)와 무관, NULL=정가 없음'` / `ADD COLUMN IF NOT EXISTS image_url VARCHAR(300) NULL COMMENT '대표 이미지 경로(앱 서빙 정적 경로) — 외부 URL 금지'`. `spring.sql.init.mode=always`가 기동마다 재실행하므로 `IF NOT EXISTS` 필수(규칙 `ddl-idempotent`, 이미 있으면 no-op).
  - PRODUCTS는 이 레포의 버전 관리 DDL(V3~V9) 밖에서 만들어진 베이스라인 테이블이다(실측: `db-main describe` — CREATE 문이 레포 어디에도 없고 시드 4행이 이미 DB에 존재). 그래서 시드는 INSERT가 아니라 `UPDATE PRODUCTS SET list_price=…, image_url=… WHERE sku='SKU-1001'` 형태 4문 — UPDATE는 그 자체로 멱등(같은 값 재대입)이라 기동마다 재실행돼도 안전(V8 zipcode의 `ON DUPLICATE KEY UPDATE`와 같은 원리를 INSERT 없이 단순화).
  - 시드 값 설계(상태 커버리지를 의도적으로 섞는다): SKU-1001 `list_price=450000`(판매가 390000, 할인 있음) · SKU-1002 `list_price=NULL`("정가 없음" 상태) · SKU-1003 `list_price=450000`(판매가 450000과 동일, "할인 0%" 상태) · SKU-1004 `list_price=42000`(판매가 35000, 판매종료 상품 — 목록엔 안 뜨고 단건조회에서만 보임). image_url은 SKU-1001~1003에 채우고 SKU-1004는 NULL로 남겨 "이미지 없음" 상태를 실데이터로 보존한다.
  - 트랜잭션 경계: 컬럼 추가·시드는 기동 시점 spring.sql.init 스크립트(문장별 개별 실행, 트랜잭션 없음)일 뿐 런타임 쓰기 경로가 아니다. 이 항목은 새 런타임 트랜잭션을 열지 않는다 — 기존 `selectProducts`/`selectBySku`는 단순 SELECT로 경계 불변.
  - 락/원자 UPDATE 필요 행: 없음 — 카운터·재고 같은 동시 쓰기 경로를 추가하지 않는다(list_price/image_url은 애플리케이션이 런타임에 쓰지 않는 표시 전용 값).

- **순서·보안**:
  - 인증 순서 불변. `/images/**`는 회원 자격 개념이 없는 정적 자산이라 `isOpenRoute`(필터 전체 스킵) 화이트리스트가 적절하다 — SR-307 #1 사례(판정이 필요한 `/api/**`를 화이트리스트로 스킵해 소유권 판정까지 함께 우회된 사고)와는 "판정할 자격 자체가 없는 자원"이라는 점에서 조건이 다르다(아래 "실패 사례집 대조" 참고). `/api/products`·`/api/products/{sku}`의 기존 무키-GET 공개 판정(SR-307, `doFilterInternal` 내부 예외)은 이 항목이 손대지 않는다.
  - 정보 노출 금지: `/images/**` 핸들러는 커스텀 리졸버 없이 Spring 기본 `ResourceHttpRequestHandler`(경로 이탈 방지 내장)만 쓴다 — 임의 파일(`application.yml` 등) 비노출을 테스트로 확인.
  - 레이트리밋: 해당 없음(신규 쓰기 경로 없음).
  - 부수효과(로그·발송·이벤트·감사): 없음 — 이 항목은 컬럼 추가+조회 필드 추가+정적 서빙뿐, 새 부수효과를 만들지 않는다.

- **계약**: 새 오류 코드 없음. 응답 봉투 불변 — `GET /api/products`는 지금처럼 배열을 그대로 반환(이 엔드포인트는 `{items:[...]}` 봉투를 쓰지 않는 기존 계약이며 이번 변경 대상이 아님), `GET /api/products/{sku}`도 객체 그대로. 상태 코드 불변(200/401/404, 새 케이스 없음). 두 응답 모두 끝에 `listPrice`(number|null)·`imageUrl`(string|null) 필드만 추가되고 기존 5필드 이름·순서·타입은 불변.

- **테스트**:
  - HTTP 레벨(`ProductControllerTest`): list·get 응답에 `listPrice`/`imageUrl` 값 단언 케이스 추가(서비스 스텁에 새 필드 세팅). 기존 5개 케이스는 무수정으로 남겨 회귀를 그대로 지킨다.
  - DAO 통합(`ProductDaoTest`, 실DB): `selectBySku("SKU-1001")`.listPrice==450000·imageUrl=="/images/products/sku-1001.svg" · `selectBySku("SKU-1002")`.listPrice==null · `selectBySku("SKU-1004")`.imageUrl==null(판매종료지만 단건조회는 sale_yn 필터가 없다는 기존 회귀와 동일 전제) · `selectProducts(null,null)` 각 행의 listPrice/imageUrl이 시드값과 일치하는지 sku별 대조.
  - 정적 서빙(`ProductImageStaticResourceServingTest`, 신규): `/images/products/sku-1001.svg` 무키 200 · 존재하지 않는 파일 404 · path traversal(`/images/%2e%2e/application.yml`) 차단(임의 파일 미노출) · `/api/orders`는 여전히 401(화이트리스트가 `/api/**`로 새지 않았는지 감시, `ShopStaticResourceServingTest` 선례와 동형).
  - 회귀: 최종 확인은 `mvnw test` 전체 스위트로 한다(부분 클래스 지정 금지 — SR-307 #1 antipattern 교훈). 특히 기존 `containsExactly` sku 목록 단언들은 컬럼 추가만으로는 깨지지 않는지(SELECT * 미사용이라 안전) 확인.
  - 기준선 영향: `.speclinker/snapshots/api.json`(응답 값 스냅샷)은 이번 변경으로 실제 응답 바디가 늘어나므로 깨진다 — 재캡처(`resp_snapshot.py capture . api --force`, 앱 8087 기동 상태)는 이 dev 항목이 아니라 이후 QA/test 단계 몫으로 남긴다(범위 밖에도 명시).

- **테스트 격리**: `ProductDaoTest`는 고정 시드 4행만 읽는 순수 SELECT라 새 데이터를 만들지 않는다 — 기존과 동일하게 격리 이슈 없음(`@Transactional`/`@AfterEach` 불필요). 신규 `ProductImageStaticResourceServingTest`는 DB에 쓰지 않고 정적 파일만 읽는다. `ProductControllerTest`는 `@WebMvcTest`+`@MockBean`이라 상태가 없다(기존 패턴 그대로) — 세 테스트 모두 상태가 다음 테스트로 새지 않는다.

- **폴백·우회 경로의 자격 판정**: `/images/**`는 새로 여는 "인증 경로"가 아니라 애초에 자격 판정 대상이 아닌 정적 자산이다(탈퇴·폐기·만료 같은 자격 개념 자체가 없음 — 소유자가 없는 리소스). 그래서 이 항목엔 자격 판정 로직이 없다(의도적으로 없음). `/api/products`·`/api/products/{sku}`의 기존 무키-GET 공개 판정(SR-307)은 이 항목이 만든 새 폴백이 아니라 기존 로직을 그대로 재사용 — 새 컬럼이 응답에 추가된다고 그 판정 범위가 넓어지지 않는다.

- **프레임워크 실행 모델 함정**:
  - MyBatis `map-underscore-to-camel-case: true`가 `list_price`→`listPrice`, `image_url`→`imageUrl`을 자동 매핑하지만, **SELECT 목록에 그 컬럼을 명시하지 않은 다른 쿼리(`selectProductsForList`)는 여전히 null로 채워진다** — "컬럼을 테이블에 추가하면 모든 쿼리가 자동으로 값을 채워준다"는 오해 주의. mapper 주석으로 명시(위 "파일" 절 참고).
  - `spring.sql.init.mode=always`는 파일을 순서대로, 파일 내부는 문장 단위로 순차 실행하지만 트랜잭션 묶음이 없다 — V10 파일 안에서 "컬럼 추가(ALTER) 2문 → 시드(UPDATE) 4문" 순서만 지키면 충분(컬럼이 먼저 있어야 UPDATE 대상 컬럼이 존재).
  - Spring Boot의 기본 정적 리소스 매핑(`classpath:/static/` → `/**`)과 신규 `/images/**` 커스텀 핸들러가 중복 매핑일 수 있다 — 어느 쪽이 응답하는지(캐시 헤더로 구분 가능) 믿지 말고 신규 테스트(`ProductImageStaticResourceServingTest`)로 실측 확인한다(`ShopStaticResourceConfig` 자체 함정 선례와 동일 태도 — "구현 후 MockMvc로 확인, STORY가 예견 못한 항목"이 그 파일에도 있었다).

- **범위 밖**:
  - 쇼핑 화면(상품 카드)의 정가 취소선·할인율 배지·이미지 렌더링 — SR-306 #1(Blocked-SpecIncomplete) 몫, shop-web은 이 항목에서 건드리지 않는다.
  - 이미지 업로드·관리 화면, 외부 이미지 URL 허용, 가격 이력·프로모션 — SR-306 요건문답에서 명시적으로 제외.
  - 할인율 컬럼 저장 — 화면에서 계산(이 항목은 list_price만 저장, 할인율 계산 로직 없음).
  - `selectProductsForList`(FUNC-order-009, Thymeleaf 정렬 화면)·`list.html`/`detail.html` 템플릿 — SR-306 확정요건 "변경 대상 화면은 UIS-ORD-008(쇼핑 홈)뿐"에 Thymeleaf 화면이 없으므로 건드리지 않는다.
  - `.speclinker/snapshots/api.json`·`.speclinker/test_baseline.json` 재캡처 — QA/test 단계 몫.

- **실패 사례집 대조**:
  - SR-307 #1 "무인증을 요구하지 않는 것과 판정을 스킵하는 것은 다르다": `/images/**`를 `isOpenRoute`에 추가하는 게 표면적으로 그 사례와 비슷해 보이지만, 그 사례의 조건("그 경로가 회원 소유권 판정이 필요한 자원")이 이미지 정적 자산엔 성립하지 않는다(자산에 소유자가 없다) — 그래서 이번엔 화이트리스트 추가가 맞다. 혹시 모를 우회는 사례집처럼 테스트로 감시한다(`/api/orders` 401 유지 단언 포함).
  - `ddl-idempotent`(SR-231 r2, V6/V9 선례): `ALTER TABLE … ADD COLUMN IF NOT EXISTS`만 쓰고 `DROP` 없음 — 같은 멱등 원리를 시드 UPDATE에도 적용.
  - `no-select-star`: 두 SELECT에 `list_price, image_url`을 명시적으로 추가해 응답 값 스냅샷(축 D)이 "무엇이 바뀌었는지" 정확히 드러나게 한다.
  - SR-307 #1 "회귀 확인은 항상 스위트 전체로": 이 계획의 "테스트" 절은 부분 클래스 지정이 아니라 `mvnw test` 전체를 최종 확인으로 명시했다(위에 반영).
  - SR-300(시간 의존 상대창) · SR-297(정리 배치·카운터 원자 UPDATE) · SR-234(발송 로그 순서) — 이 항목은 시간 의존 쿼리·카운터·배치·부수효과를 추가하지 않으므로 해당 없음("없음").

## Dev 기록
계획 확인: 계획대로 진행 (2026-09-17) — 사람 코멘트: 시드 상태 분리(할인 있음·정가 없음·할인 0%·이미지 없음)와 이미지 자산을 /shop 밖 /images에 둔 판단 승인. 완료 조건으로 명시: 무키 GET /images/products/*.svg 200 · 없는 파일 404 · path traversal 차단 · 무키 /api/orders 401 유지 · 목록·단건 응답 listPrice·imageUrl 추가(기존 필드 불변) · list_price NULL 상품은 null 응답 · 주문·장바구니 금액 계산 회귀 불변. 최종 확인은 전체 스위트(mvnw test)로.

구현 완료 (2026-09-17) — 계획대로 생성/수정. 코드에 추적 주석(linked_func)은 추가하지 않았다(v5.1 — 코드→SR은 git, 코드→스펙은 스펙 anchors).

생성/수정 파일:
- 신규 `modules/shop-api/src/main/resources/db/V10__product_list_price_image.sql` — PRODUCTS에 `list_price`(BIGINT NULL)·`image_url`(VARCHAR(300) NULL) 컬럼 추가(ALTER, IF NOT EXISTS) + 시드 UPDATE 4문(SKU-1001 정가 있음·SKU-1002 정가 없음·SKU-1003 할인 0%·SKU-1004 이미지 없음).
- 수정 `modules/shop-api/src/main/resources/application.yml` — `spring.sql.init.schema-locations` 목록 끝에 V10 파일 이어붙임(기존 9개 항목 순서 불변).
- 수정 `modules/shop-api/src/main/java/com/sm/lab/shop/domain/Product.java` — `Long listPrice`, `String imageUrl` 필드+getter/setter 추가(기존 5필드 이름·타입 불변).
- 수정 `modules/shop-api/src/main/resources/mapper/product.xml` — `selectProducts`·`selectBySku`의 컬럼 목록에 `p.list_price, p.image_url` 명시 추가(no-select-star 준수). `selectProductsForList`(FUNC-order-009, Thymeleaf 전용)는 스코프 밖이라 컬럼 추가 없이 주석만 추가.
- 신규 `modules/shop-api/src/main/java/com/sm/lab/shop/web/ProductImageStaticResourceConfig.java` — `/images/**` → `classpath:/static/images/` 정적 서빙(Spring 기본 `ResourceHttpRequestHandler`, 커스텀 `PathResourceResolver` 없음).
- 수정 `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — `isOpenRoute`에 `path.startsWith("/images/")` 추가(정적 자산, 자격 판정 대상 아님 — `/api/products` 무키 GET 공개 판정(SR-307)과는 별도 메커니즘이라 영향 없음, 회귀 테스트로 확인).
- 신규 이미지 자산(자체 제작 단색 대체 SVG, XML 선언에 `encoding="UTF-8"` 명시 — 서버 Content-Type에 charset이 없어도 브라우저가 올바르게 렌더): `static/images/products/sku-1001.svg`·`sku-1002.svg`·`sku-1003.svg`. SKU-1004용 파일은 의도적으로 만들지 않음("이미지 없음" 상태 보존).
- 수정 `modules/shop-api/src/test/java/com/sm/lab/shop/controller/ProductControllerTest.java` — 목록/단건 응답에 `listPrice`/`imageUrl` 단언 케이스 3건 추가(값 있음·null 값·단건 조회). 기존 5개 케이스 무수정.
- 수정 `modules/shop-api/src/test/java/com/sm/lab/shop/dao/ProductDaoTest.java` — 실DB 시드값 단언 케이스 4건 추가(목록 sku별 대조, 판매종료 상품 imageUrl null, 정가 없음, 할인 0%).
- 신규 `modules/shop-api/src/test/java/com/sm/lab/shop/web/ProductImageStaticResourceServingTest.java` — 무키 200(UTF-8 명시 디코딩)·없는 파일 404·path traversal 차단(상태코드 200 아님으로 판정, 컨테이너가 404 대신 다른 코드로 먼저 막을 수 있어 보수적 단언)·`/api/orders` 401 유지 4케이스.

주요 결정:
- `ProductImageStaticResourceConfig`가 Spring Boot 기본 정적 매핑(`classpath:/static/` → `/**`)과 클래스패스 위치가 겹쳐 중복 매핑일 수 있음을 인지 — 실측(MockMvc) 결과 `/images/products/sku-1001.svg`가 200으로 정상 서빙됨을 확인(더 구체적인 패턴이 우선 매칭).
- SVG 자산에 한글 텍스트가 있어 최초 테스트에서 MockMvc 기본 인코딩(ISO-8859-1, `image/svg+xml`에 charset 파라미터 없음)으로 mojibake가 나 실패 — 실제 서빙 바이트는 정상이었으나(1) 브라우저 렌더 안전을 위해 SVG에 `<?xml version="1.0" encoding="UTF-8"?>` 선언을 추가하고 (2) 테스트는 `getContentAsString(StandardCharsets.UTF_8)`로 명시 디코딩하도록 수정.

검증:
- `mvnw test -Dtest=ProductControllerTest,ProductDaoTest,ProductImageStaticResourceServingTest,ShopStaticResourceServingTest,ShopStaticResourceMissingTest` — 38/38 PASS.
- `mvnw test`(전체 스위트) — 571 실행, 실패 3건(`OrderListEndToEndIntegrationTest` 2건, `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders` 1건). 이 3건은 이 항목의 변경과 무관 — 오늘 아침 기록된 기준선(`.speclinker/test_baseline.json`, `recorded_at: 2026-09-17T01:06:44`)에도 동일하게 `executed:552/passed:549/failed:3`으로 이미 실패 중이었다(주문 조회 기본 창 "오늘-30일" 대비 고정 시드 날짜가 흐른 SR-300 기지 이슈, CLAUDE.md 명시). 이번 변경으로 실행 552→571(신규 19건 전부 PASS), 실패 건수는 3건 그대로(증가 없음).
- 완료 조건 전항목 충족: 무키 GET `/images/products/sku-1001.svg` 200 · 없는 파일(`sku-1004.svg`) 404 · path traversal 차단(200 아님, `application.yml` 내용 미노출) · 무키 `/api/orders` 401 유지 · 목록·단건 응답 `listPrice`/`imageUrl` 추가(기존 필드 불변, `mvnw test` 통과) · `list_price` NULL(SKU-1002) 응답 null 확인 · 주문·장바구니 금액 계산 관련 기존 테스트 전건 통과(회귀 불변).

범위 밖(계획대로 미착수): 쇼핑 화면(상품 카드) 렌더링(SR-306 #1 몫, shop-web 미수정) · `selectProductsForList`/Thymeleaf 템플릿 · `.speclinker/snapshots/api.json`·`test_baseline.json` 재캡처(QA/test 단계 몫).

### 재작업 완료 (round 2, 2026-09-17) — QA CONCERNS 권고 1·2 반영

재작업 지시(round 1 QA CONCERNS) 항목 1·2만 반영했다(3·4·5는 사람 코멘트로 이번 라운드 보류 확정).

수정 파일:
- `modules/shop-api/src/test/java/com/sm/lab/shop/controller/ProductControllerTest.java` — `list_withNullListPrice_returnsNullField`의 `jsonPath("$[0].listPrice").doesNotExist()`와 `get_returnsListPriceAndImageUrl_withImageUrlNullWhenMissing`의 `jsonPath("$.imageUrl").doesNotExist()`를 각각 `jsonPath(...).value(org.hamcrest.Matchers.nullValue())`로 교체(정적 임포트 `org.hamcrest.Matchers.nullValue` 추가). `doesNotExist()`는 "필드 없음"과 "필드 있고 값 null" 둘 다 통과시켜 계약을 고정하지 못했다(QA 지적, 사례집 SR-302 #1 "거짓 보증 가드" 재발 형태).
- `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — `isOpenRoute`의 `path.startsWith("/images/")`를 `path.startsWith("/images/products/")`로 좁힘(실제 서빙 위치에 한정 — 훗날 `/images/` 아래 다른 매핑이 생겨도 자동 무인증이 되지 않게, QA 권고2).
- `modules/shop-api/src/test/java/com/sm/lab/shop/web/ProductImageStaticResourceServingTest.java` — 신규 경계 테스트 `imagesPathOutsideProductsPrefix_isNotWhitelisted` 추가: `GET /images/secret.txt`(좁혀진 화이트리스트 밖, `/images/products/` 아님)가 200이 아니라 401 또는 404로 막히는지 확인(완료 조건 "다른 경로는 401/404로 막힘").

확인(사람 코멘트 요구 — "필드를 빼 보고 테스트가 깨지는지 직접 확인"):
- `Product.java`의 `getListPrice()`에 `@JsonIgnore`를 임시로 붙이고(코드에는 남기지 않음) `ProductControllerTest`만 재실행 → 이전에 `nullValue()`로 교체한 3개 테스트(`list_returnsListPriceAndImageUrl_alongsideExistingFields`·`list_withNullListPrice_returnsNullField`·`get_returnsListPriceAndImageUrl_withImageUrlNullWhenMissing`)가 전부 `PathNotFoundException: No results for path: ...['listPrice']`로 실패함을 확인 — 필드가 응답에서 아예 빠지면 이 단언이 진짜로 깨진다(= `doesNotExist()`와 달리 실질적 가드). 확인 직후 `@JsonIgnore`와 import를 제거해 원상복구(git diff에 흔적 없음, `Product.java`는 재작업 전과 동일).
- (참고: `getListPrice()` 메서드 자체를 삭제하는 방식은 `ProductDaoTest`도 그 getter를 쓰고 있어 컴파일 실패로 이어져 "필드가 응답에서 빠짐" 시나리오를 순수하게 재현하지 못했다 — 그래서 컴파일은 유지한 채 직렬화에서만 제외하는 `@JsonIgnore` 방식으로 확인했다.)

검증:
- `mvnw -o test -Dtest=ProductControllerTest,ProductDaoTest,ProductImageStaticResourceServingTest,ShopStaticResourceServingTest,ShopStaticResourceMissingTest` — 39/39 PASS(신규 경계 테스트 1건 포함, 이전 라운드 38건 + 1건).
- `mvnw -o test`(전체 스위트, 최종 확인) — 572 실행 / 실패 3건(`OrderListEndToEndIntegrationTest` 2건 + `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders` 1건) — round 1과 동일한 SR-300 기지 이슈, 이번 재작업이 만든 신규 실패 없음(실행 571→572는 신규 경계 테스트 1건 추가분, 실패 건수 3 불변).
- 완료 조건 전항목 충족: 정가 NULL 상품(SKU-1002) 응답에서 `listPrice`가 "있고 null"임을 `nullValue()`로 고정 · `GET /images/products/*.svg` 무키 200 유지(`existingImage_isServedWithoutApiKey` 통과) · `/images/secret.txt`(products/ 밖) 무키 401(신규 테스트로 확인, 필터 default-deny 경로) · 기존 통과 테스트 전건 유지(회귀 없음).

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-17 — CONCERNS
- Layer1 스펙: **pass**. TO-BE(02_변경명세 + 확정 문답 10건) 전항목 일치 — PRODUCTS에 `list_price BIGINT(20) NULL`·`image_url VARCHAR(300) NULL` 추가(DB 실측 describe로 확인, 코멘트 한글 정상 = `encoding: UTF-8` 유효), 두 조회 SELECT에 컬럼 명시 추가, 기존 5필드 이름·타입·순서 불변(새 필드는 클래스 끝에 선언 → 응답 끝에 추가), 할인율 컬럼 미저장, 외부 URL 없음(앱 서빙 `/images/products/*.svg` 자체 제작 단색 SVG), DDL은 다음 번호 파일(V10) + `ADD COLUMN IF NOT EXISTS`(DROP 없음). 시드 4행 DB 실측: 1001(450000/이미지O)·1002(NULL/이미지O)·1003(450000=판매가/이미지O)·1004(42000/이미지X) — 계획의 상태 커버리지 그대로. must 규칙 위반 없음(`no-select-star`·`ddl-idempotent`·`no-sysout`·`no-printstacktrace` 준수, `controller-has-test`는 신규 컨트롤러 없음).
- Layer2 보안: **pass**. `/images/**`는 소유자가 없는 정적 자산이라 `isOpenRoute` 화이트리스트가 적절 — SR-307 #1 사례("판정이 필요한 `/api/**`를 스킵")와 조건이 다름을 확인했다. `/api/products` 무키 GET 공개 판정(SR-307)은 `doFilterInternal` 별도 경로로 diff에 변경 없음. 경로 이탈은 Spring 기본 `PathResourceResolver`가 차단(traversal 테스트 통과, `application.yml` 미노출). 화이트리스트 누출 감시 테스트(`/api/orders` 무키 401)를 QA가 직접 재실행해 통과 확인. (권고 2 — 접두사를 `/images/products/`로 좁히면 미래 표면이 더 작아진다)
- Layer3 회귀: **concerns**. QA가 전체 스위트를 독립 재실행(`mvnw -o test`) — **571 실행 / 실패 3**. 실패 3건은 `.speclinker/test_baseline.json`(recorded 2026-09-17T01:06:44, 552/549/**3**)과 **동일 테스트**(`OrderListEndToEndIntegrationTest` 2건 + `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`)로, SR-300 기지 이슈(상대 조회창 vs 고정 시드)다 — 이번 변경이 만든 실패 없음(신규 19건 전부 PASS). 금액 계산 회귀 없음: `cart.xml`·`order.xml`의 PRODUCTS 조인은 전부 컬럼 명시(`p.product_name`, `p.price`)라 컬럼 2개 추가의 영향이 없다. 다만 아래 권고 1(계약을 실제로 고정하지 못하는 단언)은 다음 변경에서 조용히 깨질 수 있다.
- 권고(CONCERNS시):
  1. **(medium) `listPrice`/`imageUrl`이 "null로 내려간다"를 고정하는 테스트가 없다.** `ProductControllerTest`의 `jsonPath("$[0].listPrice").doesNotExist()`(및 `$.imageUrl`)는 Spring `JsonPathExpectationsHelper.doesNotExist()`가 `assertTrue(reason, value == null)`로 끝나므로(spring-test 6.1.14 바이트코드 확인) **"필드 없음"과 "필드 있고 값 null" 둘 다 통과**한다. 현재 실제 응답은 `"listPrice": null`이 맞지만(Jackson 기본 inclusion=ALWAYS, 커스텀 `ObjectMapper`·`@JsonInclude`·`spring.jackson.*` 없음 확인), 누군가 `NON_NULL`을 켜거나 DTO를 도입하면 **테스트가 초록인 채로** 변경명세의 "값이 없으면 null로 내려가고 화면이 감춘다"와 사람 완료조건("list_price NULL 상품은 null 응답")이 깨진다. 사례집 SR-302 #1("가드를 지워도 통과하는 거짓 보증")의 재발 형태다. → `jsonPath("$[0].listPrice").value(org.hamcrest.Matchers.nullValue())`로 교체(경로가 없으면 실패, null이면 통과).
  2. (low) `ApiKeyAuthFilter.isOpenRoute`의 `path.startsWith("/images/")`는 필터 전체를 스킵하므로, 훗날 `/images/` 아래 매핑(예: 업로드 API)이 생기면 자동으로 무인증이 된다. 실제 서빙 위치인 `/images/products/`로 좁히기를 권고(현재는 그 아래 정적 자산뿐이라 무해).
  3. (low) `ProductImageStaticResourceConfig`는 Spring Boot 기본 정적 매핑(`classpath:/static/` → `/**`, `spring.web.resources.add-mappings` 미설정=true)과 기능이 겹쳐 **삭제해도 `ProductImageStaticResourceServingTest`가 그대로 통과**한다(즉 이 설정의 필요성을 테스트가 증명하지 못한다). 의도(위치 고정·명시)를 남길 거면 그대로 두되, 그 사실을 Javadoc에 적거나 기본 매핑에 위임하고 클래스를 지우는 편이 더 단순하다.
  4. (low) `selectProductsForList`(Thymeleaf 정렬 화면)만 두 컬럼을 빼서, 같은 화면이 `sort=latest`면 값이 차고 `priceAsc/Desc`면 null이 되는 비대칭이 생겼다(템플릿이 두 필드를 안 써서 현재는 무증상). 주석은 남아 있으니 후속 TODO로만.
  5. (low) `ApiKeyAuthFilter.java` 554줄 — `file-size-cap`(java 450, should) 초과. 이번 변경분(+7줄)이 원인이 아닌 기존 초과분이라 후속 분리 TODO.
- 재동기화 입력(STEP 5.5 — QA 뒤에 반영, 권고 아님):
  1. `INF-ORD-008` — `## 응답 (200 OK)` 예시 바디에 `listPrice`(number|null)·`imageUrl`(string|null) 추가. 본문 "응답 스키마(`sku`/`productName`/`price`/`stockQty`/`saleYn`) 변경 없음"(SR-201 이력 서술)이 이제 옛 동작이라 정정 필요.
  2. `INF-ORD-009` — 동일하게 단건 응답 스키마에 두 필드 추가(판매종료 상품도 `sale_yn` 필터 없이 그대로 반환).
  3. `SCH-ORD-005` — `### 컬럼 설명` 표와 erDiagram에 `list_price BIGINT(20) NULL`·`image_url VARCHAR(300) NULL` 추가(표시 전용, 금액 계산 미사용 주기).
  4. 기준선: `.speclinker/snapshots/api.json` 응답 값 스냅샷은 이번 필드 추가로 반드시 어긋난다 — 재캡처 필요. **주의**: 지금 8087에 떠 있는 앱은 스테일 빌드다(QA 실측 — 무키 `GET /api/products`가 401로, SR-307 반영 전 동작). 재캡처 전에 반드시 재빌드·재기동할 것.

### QA Gate — 2026-09-17 — CONCERNS (round 2)
> 이번 라운드 판정 대상은 사람이 지시한 재작업 1·2뿐이다. round 1 권고 3·4·5는 사람이 명시적으로 보류시켰으므로 재지적하지 않는다.

- **재작업 1 (doesNotExist → nullValue) — 반영 확인 O.** `ProductControllerTest.java:17`에 `import static org.hamcrest.Matchers.nullValue`가 들어왔고, `list_withNullListPrice_returnsNullField`(:177)·`get_returnsListPriceAndImageUrl_withImageUrlNullWhenMissing`(:198) 두 곳 모두 `jsonPath(...).value(nullValue())`로 교체됐다(잔존 `doesNotExist` 호출 없음 — 174·175·197줄은 근거 주석뿐). **QA 독립 실측(dev의 `@JsonIgnore` 실험과 다른 경로)**: spring-test 6.1.14 `JsonPathExpectationsHelper` 바이트코드를 직접 디스어셈블해 `assertValue(String, Matcher)`가 `evaluateJsonPath` → `JsonPath.read(content)`를 호출하고, `evaluateJsonPath`의 예외표가 `Class java/lang/Throwable`을 잡아 `AssertionError("No value at JSON path ...")`로 재던짐을 확인했다 — 즉 경로가 없으면(PathNotFoundException) 반드시 실패하고, 값이 null일 때만 `MatcherAssert.assertThat(null, nullValue())`로 통과한다. 반면 `doesNotExist`(:240~)는 같은 예외를 잡아 `return`하므로 "필드 없음"도 통과한다 — round 1 지적이 사실이었고 교체가 유효한 가드로 성립함을 바이트코드로 재확인. `Product.java`에 `@JsonIgnore`·잔여 import 흔적 없음(원상복구 확인).
- **재작업 2 (/images/ → /images/products/) — 반영 확인 O.** `ApiKeyAuthFilter.java:279` `path.startsWith("/images/products/")`. 판정에 쓰이는 `path`는 `resolveNormalizedPath`(:305)가 `getServletPath()+getPathInfo()`로 만든 **디코딩·정규화된** 값이라 `%2e%2e`·`;matrix` 인코딩 우회가 성립하지 않는다. 형제 경로 오탐도 없다(`/images/products-admin/...`은 접두사 불일치). 서빙 자산은 `static/images/products/sku-1001~1003.svg` 3개뿐이라(실측 `find static -type f`) 좁히기로 깨지는 기존 경로가 없다.
- Layer1 스펙: **pass**. round 1에서 확인한 TO-BE 일치(컬럼 2개 추가·두 SELECT 컬럼 명시·기존 5필드 불변·V10 `IF NOT EXISTS`·시드 4행 상태 커버리지)는 round 2가 건드리지 않았다(변경 3파일: `ProductControllerTest`·`ApiKeyAuthFilter`·`ProductImageStaticResourceServingTest`). 재작업 1이 오히려 "값이 없으면 null로 내려간다"는 변경명세 문구를 처음으로 실제 고정했다. must 규칙 위반 없음(변경 3파일에 `System.out`·`printStackTrace` 없음, `no-select-star`·`ddl-idempotent`는 무변경).
- Layer2 보안: **pass**. 좁힌 화이트리스트는 공격 표면을 단조 감소시킨다 — `/images/` 아래 `products/` 밖 경로는 이제 필터를 타고 default-deny로 떨어진다(`doFilterInternal`:328-338 — 무키이고 `isPublicProductReadPath`도 아니므로 401). `/api/orders` 무키 401 감시 테스트 유지, traversal 차단 테스트 유지(둘 다 PASS). `/api/products` 무키 GET 공개 판정(SR-307)은 별도 경로라 무영향.
- Layer3 회귀: **concerns**. QA가 전체 스위트를 독립 재실행(`mvnw -o test`) — **572 실행 / 실패 3**. 실패 3건은 round 1·기준선(`.speclinker/test_baseline.json`, recorded 2026-09-17T01:06:44, 552/549/**3**)과 동일한 SR-300 기지 이슈(`OrderListEndToEndIntegrationTest` 2건 + `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders` 1건, 실측 실패 메시지가 상대 조회창 vs 고정 시드 패턴 그대로). 재작업이 만든 신규 실패 0건, 실행 571→572(신규 경계 테스트 1건). 관련 클래스 surefire 실측: `ProductControllerTest` 9/9 · `ProductDaoTest` 15/15 · `ProductImageStaticResourceServingTest` 5/5 전건 PASS. 다만 아래 권고 1(이번 라운드가 새로 만든 가드가 다시 거짓 보증) 때문에 concerns.
- 권고(CONCERNS시):
  1. **(medium) 재작업 2를 지키라고 이번 라운드에 새로 만든 경계 테스트가, 정작 재작업 2를 되돌려도 통과한다.** `ProductImageStaticResourceServingTest.imagesPathOutsideProductsPrefix_isNotWhitelisted`(:82-84)는 `GET /images/secret.txt`의 상태코드를 `assertThat(status).isIn(401, 404)`로 단언한다. 좁힌 지금은 필터 default-deny로 **401**이지만, 누군가 화이트리스트를 다시 `/images/`로 넓히면 필터가 스킵되고 `/images/**` 리소스 핸들러가 파일 없음으로 **404**를 내므로 — **같은 테스트가 그대로 초록이다**. 즉 이 테스트는 "좁혀져 있음"을 전혀 증명하지 못한다. round 1 권고 1(=이번 재작업 1)과 **동일한 "거짓 보증 가드" 클래스이며, 이번 라운드가 새로 만든 코드**다(사례집 SR-302 #1 재발). → `assertThat(status).isEqualTo(401)`로 고정한다(넓히면 404가 되어 실패 = 진짜 가드). 사람 완료조건의 "401 또는 404"는 허용 동작의 느슨한 서술이고, 이 테스트의 선언된 목적(주석 :77-81 "형제 경로가 더 이상 필터를 자동 스킵하지 않아야 한다")은 401 고정으로만 달성된다.
  2. (low) 위 테스트 주석 :80 "정적 리소스 매핑도 `/images/products/**`로 고정돼 있어"는 사실과 다르다 — `ProductImageStaticResourceConfig`는 여전히 `/images/**` → `classpath:/static/images/`를 등록한다(:24-25). 이 오인이 권고 1의 느슨한 단언(404를 "매핑 없음"으로 본 것)을 낳은 것으로 보인다. 주석을 실제 매핑대로 정정한다(설정 자체는 손대지 않는다 — 사람이 보류한 round 1 권고 3 영역).
- 재동기화 입력(STEP 5.5 — QA 뒤에 반영, 권고 아님): round 1 목록 1~4 그대로 유효하며 이번 라운드가 새로 추가한 항목은 없다(INF-ORD-008·INF-ORD-009 응답 스키마에 `listPrice`/`imageUrl` 추가 · SCH-ORD-005 컬럼 표·erDiagram 추가 · `.speclinker/snapshots/api.json` 재캡처). **재캡처 주의 재확인(QA 실측 2026-09-17)**: 8087에 떠 있는 앱은 여전히 스테일 빌드다 — 무키 `GET /api/products` → **401**(SR-307 반영 전), 무키 `GET /images/products/sku-1001.svg` → **401**(SR-306.2 반영 전). 재캡처 전 반드시 재빌드·재기동할 것.

### QA Gate — 2026-09-17 — PASS (round 3)
> 이번 라운드 판정 대상은 사람이 지시한 재작업 1·2뿐이다. round 1 권고 3·4·5는 사람이 명시적으로 보류시켰으므로 재지적하지 않는다.
> 이번 라운드 변경 파일은 `ProductImageStaticResourceServingTest.java` 1개(테스트 단언 1개 + 주석)뿐이다 — 운영 코드 무변경.

- **재작업 1 (isIn(401,404) → 401 고정) — 반영 확인 O.** `ProductImageStaticResourceServingTest.java:86-87`이 `mockMvc.perform(get("/images/secret.txt")).andExpect(status().isUnauthorized())`로 교체됐다(지시문의 `assertThat(status).isEqualTo(401)`와 동치이며 MockMvc 관용 표현으로 더 낫다 — `MvcResult` 추출·상태코드 변수가 사라져 단언이 한 줄로 고정). 잔존 `isIn(` 없음.
  **QA 독립 실측(dev의 "화이트리스트 되돌리기" 실험과 다른 경로 — 코드를 건드리지 않고 검증)**: 이 단언이 진짜 가드임을 두 사실의 결합으로 확인했다. ① 필터 `doFilterInternal`(`ApiKeyAuthFilter.java:328-338`)은 무키 요청이 `isOpenRoute`도 `isPublicProductReadPath`도 아니면 `writeError(..., SC_UNAUTHORIZED)`로 떨어뜨린다 — 현재 `/images/secret.txt`는 좁혀진 접두사(`:279` `/images/products/`) 밖이라 **401**. ② 화이트리스트를 `/images/`로 되돌리면 필터가 통째로 스킵되고 `/images/**` 리소스 핸들러가 응답하는데, `static/images/` 아래 실파일은 `products/sku-1001~1003.svg` **3개뿐이고 `secret.txt`는 존재하지 않는다**(실측 `find`) — 같은 클래스의 `missingImage_returns404`가 이미 "없는 파일 = 404"를 증명하므로 되돌리면 **404**가 되어 401 고정 단언이 깨진다. 즉 round 2의 `isIn(401,404)`가 삼켰던 바로 그 차이를 이제 실제로 잡는다.
- **재작업 2 (주석 정정) — 반영 확인 O.** 같은 메서드 주석 `:80-85`가 "정적 리소스 매핑(ProductImageStaticResourceConfig)은 여전히 `/images/**` 전체를 `classpath:/static/images/`로 서빙 등록돼 있다(`/images/products/**`로 고정돼 있지 않음)"로 정정됐고, 이는 `ProductImageStaticResourceConfig.java:24-25`의 실제 등록(`addResourceHandler("/images/**")`)과 일치한다. 나아가 "화이트리스트가 좁혀져 있지 않으면 리소스 핸들러가 404를 낼 뿐이라 401·404 양자 허용으로는 증명하지 못한다"는 **판정 근거까지** 주석에 남겨 다음 사람이 같은 오인을 반복하지 않게 했다. 설정 클래스(보류 항목)는 무변경 확인.
- **보류 항목 원상복구 확인 O.** dev가 검증용으로 일시 되돌렸다는 `ApiKeyAuthFilter.isOpenRoute`는 `:279` `path.startsWith("/images/products/")`로 복귀해 있다(round 2 상태와 동일). `ProductImageStaticResourceConfig`·`Product.java`·`product.xml`·`V10__product_list_price_image.sql`에 실험 잔존물 없음. (`modules/`는 이 랩에서 `.gitignore:12`로 제외돼 git diff가 비므로, 원상복구는 git이 아니라 파일 실독으로 확인했다.)
- Layer1 스펙: **pass**. round 1·2에서 확인한 TO-BE 일치를 이번 라운드가 건드리지 않았다(운영 코드 무변경). 재확인 실측: `V10`이 `ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS list_price BIGINT NULL / image_url VARCHAR(300) NULL` + 시드 UPDATE 4문(1001 할인 있음·1002 정가 NULL·1003 할인 0%·1004 이미지 NULL)으로 상태 커버리지 유지, `selectProducts`(`product.xml:13`)·`selectBySku`(`:28`)만 `p.list_price, p.image_url`을 명시 포함하고 `selectProductsForList`(`:55`)는 스코프 밖 주석과 함께 제외 — 계획 그대로. must 규칙 위반 없음(변경 파일에 `System.out`·`printStackTrace` 없음, `no-select-star`·`ddl-idempotent` 무변경, 테스트 파일 89줄로 `file-size-cap` 여유).
- Layer2 보안: **pass**. 이번 라운드는 공격 표면을 넓히지 않았다(테스트 단언만 강화). 좁혀진 화이트리스트가 이제 **테스트로 잠겼다** — 되돌리면 CI가 깨지므로 round 2에서 얻은 보안 이득이 회귀 방지 장치를 갖췄다. `/api/orders` 무키 401 감시 테스트·traversal 비노출 테스트 유지(둘 다 PASS).
- Layer3 회귀: **pass**. QA가 전체 스위트를 독립 재실행(`mvn -o test`, 워크스페이스 `mvnw.cmd`가 가리키는 apache-maven-3.9.9 직접 호출) — **572 실행 / 실패 3**. 실패 3건은 round 1·2 및 기준선(`.speclinker/test_baseline.json`, recorded 2026-09-17T01:06:44, 552/549/**3**)과 **동일 테스트**(`OrderListEndToEndIntegrationTest` 2건 + `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`)이며, 실측 실패 메시지가 고정 시드 주문번호(`20260816-0002`) 미포함 — CLAUDE.md에 적힌 SR-300 기지 이슈(상대 조회창 `오늘-30일` vs 고정 시드) 그대로다. 이번 재작업이 만든 신규 실패 0건, 실행 수 572 불변(단언 교체뿐, 신규 테스트 없음 — dev 기록과 일치). 관련 클래스 surefire 실측: `ProductControllerTest` 9/9 · `ProductDaoTest` 15/15 · `ProductImageStaticResourceServingTest` 5/5 전건 PASS.
- 후속 TODO(비차단, low — 이번 라운드가 만든 코드가 아니므로 게이트를 세우지 않는다):
  1. (low) `pathTraversalAttempt_doesNotExposeArbitraryFile`(:53-64)은 round 2의 화이트리스트 좁히기 이후 **인증 필터의 401에 먼저 막힌다**(`/images/%2e%2e/application.yml`은 디코딩 여부와 무관하게 `/images/products/` 접두사에 걸리지 않는다) — 보안 속성("임의 파일 미노출")은 여전히 참이지만, 이 테스트가 원래 증명하려던 **리소스 리졸버의 경로 이탈 방지**는 더 이상 실행되지 않는다. 검증 대상을 되살리려면 `/images/products/%2e%2e/%2e%2e/application.yml`처럼 화이트리스트 **안쪽**에서 이탈을 시도하는 경로로 바꾼다. round 1 코드 + round 2 변경의 상호작용이라 이번 라운드 귀책이 아니고, 현재 무증상이다.
- 재동기화 입력(STEP 5.5 — QA 뒤에 반영, 권고 아님): round 1·2 목록이 **그대로 유효하며 아직 미반영**이다(실측: `docs/05_설계서/order` 전체에 `listPrice`·`imageUrl`·`list_price`·`image_url` 문자열이 하나도 없음). 이번 라운드가 새로 추가한 항목은 없다 — ① `INF-ORD-008` 응답 예시·스키마에 `listPrice`(number|null)·`imageUrl`(string|null) 추가 ② `INF-ORD-009` 동일 ③ `SCH-ORD-005` 컬럼 표·erDiagram에 `list_price BIGINT(20) NULL`·`image_url VARCHAR(300) NULL`(표시 전용, 금액 계산 미사용) 추가 ④ `.speclinker/snapshots/api.json` 재캡처 — **재캡처 전 반드시 재빌드·재기동**(8087 스테일 빌드 경고는 round 2 실측 그대로).

## 재작업 지시
> round 2 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/regression] round 2가 재작업 2(화이트리스트 좁히기)를 지키려고 새로 만든 경계 테스트 ProductImageStaticResourceServingTest.imagesPathOutsideProductsPrefix_isNotWhitelisted(:82-84)가 GET /images/secret.txt를 assertThat(status).isIn(401, 404)로 단언한다. 좁힌 지금은 필터 default-deny로 401이지만, 화이트리스트를 다시 /images/로 넓히면 필터가 스킵되고 /images/** 리소스 핸들러가 파일 없음으로 404를 내므로 같은 테스트가 그대로 통과한다 — 좁혀져 있음을 전혀 증명하지 못하는 거짓 보증 가드다. round 1 권고 1(=이번 재작업 1)과 동일한 클래스이며, 이번 재작업이 새로 만든 코드에 기인한다(사례집 SR-302 #1 재발). → assertThat(status).isEqualTo(401)로 고정한다 — 화이트리스트를 넓히면 404가 되어 실패하므로 진짜 가드가 된다. 사람 완료조건의 '401 또는 404'는 허용 동작의 느슨한 서술이고, 이 테스트가 주석(:77-81)에 선언한 목적(형제 경로가 더 이상 필터를 자동 스킵하지 않음)은 401 고정으로만 달성된다.
2. [low/spec] ProductImageStaticResourceServingTest 주석 :80의 '정적 리소스 매핑도 /images/products/**로 고정돼 있어'가 사실과 다르다 — ProductImageStaticResourceConfig는 여전히 /images/** -> classpath:/static/images/를 등록한다(:24-25). 이 오인이 위 medium의 느슨한 단언(404를 '매핑 없음'으로 해석)을 낳은 것으로 보인다. → 주석을 실제 매핑(/images/**)대로 정정한다. 설정 클래스 자체는 손대지 않는다 — 사람이 보류한 round 1 권고 3 영역.

사람 코멘트: round 2 재작업: 1) imagesPathOutsideProductsPrefix_isNotWhitelisted의 단언을 assertThat(status).isIn(401,404) 대신 assertThat(status).isEqualTo(401)로 고정한다 — 화이트리스트를 /images/ 전체로 되돌리면 404가 되어 테스트가 깨지는지 직접 확인하고 Dev 기록에 남길 것. 2) 같은 파일의 주석(:80, '정적 리소스 매핑도 /images/products/**로 고정')을 실제 설정(ProductImageStaticResourceConfig가 여전히 /images/**를 등록)에 맞게 정정한다. 설정 클래스 자체는 건드리지 않는다(보류된 항목). 완료 조건: GET /images/secret.txt = 401 고정 · /images/products/sku-1001.svg 200 · 없는 이미지 404 · 경로 순회 차단 · /api/orders 401 유지. 전체 스위트로 재확인.

### 재작업 완료 (round 3, 2026-09-17) — QA CONCERNS round 2 권고 1·2 반영

수정 파일:
- `modules/shop-api/src/test/java/com/sm/lab/shop/web/ProductImageStaticResourceServingTest.java` — `imagesPathOutsideProductsPrefix_isNotWhitelisted`의 단언을 `int status = ...; assertThat(status).isIn(401, 404)`에서 `mockMvc.perform(get("/images/secret.txt")).andExpect(status().isUnauthorized())`(401 고정)로 교체. 같은 메서드의 주석(구 :80 "정적 리소스 매핑도 /images/products/**로 고정돼 있어")을 실제 설정(`ProductImageStaticResourceConfig`가 여전히 `/images/**` → `classpath:/static/images/`를 등록)에 맞게 정정 — 화이트리스트가 넓어지면 필터가 스킵되고 리소스 핸들러가 파일 없음으로 404를 낼 뿐이라 "401 또는 404"로는 좁혀져 있음을 증명하지 못한다는 사실을 명시했다. `ApiKeyAuthFilter.java`·`ProductImageStaticResourceConfig.java`(설정 클래스)는 손대지 않았다(사람이 보류한 항목).

확인(사람 코멘트 요구 — "화이트리스트를 /images/ 전체로 되돌리면 404가 되어 테스트가 깨지는지 직접 확인"):
- `ApiKeyAuthFilter.isOpenRoute`의 `path.startsWith("/images/products/")`를 일시적으로 `path.startsWith("/images/")`로 되돌리고 `mvnw -o test -Dtest=ProductImageStaticResourceServingTest`만 재실행 → `imagesPathOutsideProductsPrefix_isNotWhitelisted`가 `AssertionError: Status expected:<401> but was:<404>`로 실제로 실패함을 확인했다(다른 4개 테스트는 그대로 PASS). 즉 화이트리스트가 넓어지면 이 테스트가 진짜로 깨진다 — round 2의 `isIn(401,404)`와 달리 실질적 가드가 됨을 검증.
- 확인 직후 `path.startsWith("/images/products/")`로 원상복구했다(git diff 확인 — round 1·2에서 이미 있던 변경분과 동일, 추가 잔존 흔적 없음).
- 원상복구 후 같은 테스트를 재실행해 5/5 PASS(경계 테스트 포함)로 복귀함을 확인.

검증:
- `mvnw -o test -Dtest=ProductImageStaticResourceServingTest` — 5/5 PASS(화이트리스트 원상복구 후).
- `mvnw -o test`(전체 스위트, 최종 확인) — 572 실행 / 실패 3건(`OrderListEndToEndIntegrationTest` 2건 + `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders` 1건) — round 1·2와 동일한 SR-300 기지 이슈(`.speclinker/test_baseline.json` recorded 2026-09-17T01:06:44 시점부터 동일 3건), 이번 재작업이 만든 신규 실패 없음(실행 수 572 불변 — 이번 라운드는 기존 테스트 내부 단언만 교체, 신규 테스트 추가 없음).
- 완료 조건 전항목 충족: `GET /images/secret.txt` → 401 고정(위 확인 절차로 직접 검증) · `GET /images/products/sku-1001.svg` → 200 유지 · 존재하지 않는 이미지(`sku-1004.svg`) → 404 유지 · path traversal 차단 유지 · 무키 `/api/orders` → 401 유지 · 기존 통과 테스트 전건 유지(회귀 없음).

## test-agent 완료 보고 (2026-09-17)

### AC와 테스트 매핑

| 수용 기준 (AC) | 검증 대상 | 테스트 케이스 | 상태 |
|---|---|---|---|
| **SCH-ORD-005**: 테이블에 `list_price`, `image_url` 컬럼 추가 | Schema 존재 및 타입 | ProductDaoTest 통합 테스트에서 DB 값 읽기 | PASS |
| **INF-ORD-008**: 목록 응답에 `listPrice`, `imageUrl` 필드 추가(기존 5필드 불변) | HTTP 응답 필드 | ProductControllerTest::list_returnsListPriceAndImageUrl_alongsideExistingFields | PASS |
| **INF-ORD-008**: `listPrice`/`imageUrl` null값 처리 | null 응답 필드 | ProductControllerTest::list_withNullListPrice_returnsNullField | PASS |
| **INF-ORD-008**: DAO 계층 시드 데이터 확인 | DB 시드값 검증 | ProductDaoTest::selectProducts_withoutKeyword_returnsSeededListPriceAndImageUrlPerSku | PASS |
| **INF-ORD-009**: 단건 응답에 `listPrice`, `imageUrl` 필드 추가 | HTTP 응답 필드 | ProductControllerTest::get_returnsListPriceAndImageUrl_withImageUrlNullWhenMissing | PASS |
| **INF-ORD-009**: 판매종료 상품도 조회 가능(sale_yn 필터 없음) + null imageUrl | DB 조회 필터링 | ProductDaoTest::selectBySku_offSaleProduct_returnsListPriceButNullImageUrl | PASS |
| **INF-ORD-009**: 정가 없는 상품(null listPrice) | null 응답 필드 | ProductDaoTest::selectBySku_withNullListPrice_returnsNull | PASS |
| **INF-ORD-009**: 할인 0% 상태(listPrice==price) | 값 그대로 반환 | ProductDaoTest::selectBySku_withListPriceEqualToPrice_returnsBothValues | PASS |
| **회귀**: 목록 조회 sale_yn='Y' 필터 유지 | 판매중 상품만 반환 | ProductDaoTest::selectProducts_withoutKeyword_returnsAllOnSaleSortedBySku | PASS |
| **회귀**: 목록 조회 keyword 검색 유지 | 부분일치 검색 | ProductDaoTest::selectProducts_withKeyword_returnsPartialMatchOnly 외 3건 | PASS |
| **회귀**: 목록 조회 inStock 필터 유지 | 재고 1 이상만 반환 | ProductDaoTest::selectProducts_withInStockTrue_* 2건 + 부하테스트 2건 | PASS |
| **정적 이미지 서빙**: `/images/products/*.svg` 무키 GET 200 | HTTP 200 응답 | ProductImageStaticResourceServingTest::existingImage_isServedWithoutApiKey | PASS |
| **정적 이미지 서빙**: 없는 파일 404 | HTTP 404 응답 | ProductImageStaticResourceServingTest::missingImage_returns404 | PASS |
| **보안**: 경로 순회(path traversal) 차단 | 임의 파일 비노출 | ProductImageStaticResourceServingTest::pathTraversalAttempt_doesNotExposeArbitraryFile | PASS |
| **보안**: 이미지 화이트리스트가 `/api/` 경로로 누출되지 않음 | /api/orders 401 유지 | ProductImageStaticResourceServingTest::apiWithoutApiKey_stillRejectedUnauthorized_imagesWhitelistDidNotLeak | PASS |
| **보안**: 이미지 화이트리스트 범위 제한(/images/products/) | 형제 경로 401 | ProductImageStaticResourceServingTest::imagesPathOutsideProductsPrefix_isNotWhitelisted | PASS |

### 테스트 실행 결과

```
총 실행: 572 테스트
  - 통과: 569 테스트 (99.5%)
  - 실패: 3 테스트 (0.5% — SR-300 기지 이슈, 이 항목과 무관)
  
SR-306.2 신규 테스트: 19건 전부 PASS
  - ProductControllerTest: 3개 (listPrice/imageUrl 필드 단언)
  - ProductDaoTest: 4개 (시드 데이터 및 null 처리)
  - ProductImageStaticResourceServingTest: 5개 (정적 서빙 및 보안)
  - round 2·3 개선 테스트: 7개 (경계 테스트 및 단언 강화)
```

### 회귀 검증 (변경 영향 범위)

이 항목은 **SR-306 Epic**이므로 기존 기능 회귀 검증이 중요함. 확정된 회귀 범위:
- **INF-ORD-008** 응답 스키마: `sku`, `productName`, `price`, `stockQty`, `saleYn` 5필드 불변 ✓
- **INF-ORD-009** 응답 스키마: 위와 동일 5필드 불변 ✓
- **INF-ORD-008** sale_yn='Y' 필터: 판매중 상품만 반환 ✓
- **INF-ORD-008** keyword 검색: 부분일치 LIKE 유지 ✓
- **INF-ORD-008** inStock 필터: stock_qty >= 1 조건 유지 ✓
- **INF-ORD-009** no sale_yn 필터: 판매종료 상품도 조회 가능 ✓
- **금액 계산**: 판매가(price) 기준, 정가(list_price) 사용 안 함 ✓
- **인증 정책**: /api/products 무키 GET 공개(SR-307), /api/orders 무키 401 ✓

### AC 커버리지 분석

**모든 AC가 테스트로 검증됨**:
- 필드 추가: 각 필드의 존재 여부, null 처리 테스트로 고정 (`jsonPath(...).value(nullValue())` 사용)
- 데이터 계층: DAO 통합 테스트에서 실DB 시드값과 대조 확인
- API 계층: MockMvc로 HTTP 응답 구조 및 상태코드 검증
- 정적 자산: 실 서버 컨텍스트에서 파일 서빙 및 보안 테스트
- 회귀: 기존 테스트 무수정으로 유지, 새 필드 추가만

### 품질 판정

✅ **납품 가능**
- 수용 기준 전항목 테스트 커버: 15개 AC + 5개 회귀 항목 검증
- 신규 테스트 19건 전부 PASS (SR-300 무관 기존 실패 3건 제외)
- 필드 단언 강화(round 2·3): "있고 null" vs "없음" 구분 가드 (`nullValue()` 적용)
- 경계 테스트 추가: 화이트리스트 범위 제한 검증
- 회귀 검증: 기존 필터·검색·계산 무변화 확인

### 미이행 항목

없음 — 모든 AC 검증 완료.

### 다음 단계 (범위 밖)

- `.speclinker/snapshots/api.json` 응답 스냅샷 재캡처 (QA/test 단계)
- `INF-ORD-008`, `INF-ORD-009` 응답 스키마 문서 갱신 (`listPrice`/`imageUrl` 필드 추가)
- `SCH-ORD-005` 테이블 스키마 문서 갱신 (컬럼 표·erDiagram 추가)
- 쇼핑 화면(shop-web) 상품 카드 렌더링 (SR-306 #1 — Blocked-SpecIncomplete)
