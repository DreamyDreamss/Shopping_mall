---
story-id: STORY-FUNC-order-012
func-id: FUNC-order-012
status: Done
domain: order
created: 2026-08-23
spec_markers: 0
sr-id: SR-203
---

# STORY-FUNC-order-012 — 체크아웃 (장바구니→주문 전환, INF-ORD-014 신규)

## Story
운영자가 장바구니 화면에서 [주문하기]를 누르면 담긴 전체 품목이 기존 주문 규칙 그대로 주문으로
전환된다 — 원자적(주문 생성·재고 차감·장바구니 비움 전부 또는 전무). (SR-203 7-N: INF-ORD-014
스펙은 구현 후 recon이 저작 — 이 AC가 계획 정본.)


## 변경 컨텍스트 (SR-203)
> 이 story는 변경요청 **SR-203 — 체크아웃 — 장바구니를 주문으로 전환** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-203/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-203/02_변경명세.md`
- **회귀 TC**: `docs/변경관리/SR-203/03_TC.md` — STEP 5에서 수용기준 TC와 **함께 실행**한다.
- AS-IS 스냅샷: `docs/변경관리/SR-203/_asis_snapshot` (변경 전 상태 대조용)
- 롤백: `docs/변경관리/SR-203/04_ROLLBACK.md`

## 수용 기준 (Acceptance Criteria) — 02_변경명세.md 기준(SM 수동 작성)
- [ ] POST /api/cart/checkout {memberId}: 200 {orderNo,totalAmount,itemCount} — 주문은 **기존 OrderService 규칙 재사용**(채번·PLACED·decreaseStock, 신규 규칙 금지) (TC-FUNC-order-012-01)
- [ ] 성공 시 장바구니 비움·재고 차감·ORDER_ITEMS 일치 (TC-02)
- [ ] 400 빈 장바구니 / 404 회원 없음 (TC-03/04)
- [ ] 409 재고 부족 — 부족 품목별 사유·**전량 거부**(주문 0·차감 0·장바구니 보존) (TC-05)
- [ ] **원자성**: 다품목 중 후순위 부족이어도 선순위 차감·주문 반영 없음(단일 트랜잭션 롤백) (TC-06)
- [ ] 화면: 품목 있으면 [주문하기] 노출·빈 장바구니 미노출 (TC-07/08), 성공 redirect /order/{orderNo} (TC-09), 실패 redirect /cart+flash 사유·보존 (TC-10) — PRG·파서주석·4xx만 흡수(docs/project-context.md 준수)
- [ ] 회귀: 기존 POST /api/orders·장바구니 CRUD·주문 조회 무변경 (TC-11~13)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-014
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)


## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
- **재사용 결정**: 신규 CheckoutService를 만들지 않고 기존 `CartService`에 `checkout(memberId)`를
  추가(응집 판단 — 장바구니의 생애주기 종료 동작이라 CartService 소관, OrderService를 신규
  의존성으로 주입). 주문 생성은 `OrderService.create(memberId, List<OrderItem>)`를 그대로 호출
  (채번·PLACED 상태·`decreaseStock` — 복제 없음). `OrderService.create`가 기본 전파(REQUIRED)의
  `@Transactional`이라 `CartService.checkout`(역시 `@Transactional`)이 연 트랜잭션에 그대로
  참여 — 별도 전파 속성 지정 불필요. 재고 부족 409는 `OrderService.create`가 이미 sku를 포함한
  메시지("재고 부족: SKU-xxxx")로 던지므로 별도 변환 없이 그대로 전파(D6 "품목별 사유" 충족).
  장바구니 비움(`CartDao.deleteAllItems` 신설)은 주문 생성이 트랜잭션 내에서 확정된 뒤에만 호출.
- **생성/수정 파일**:
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/CartDao.java` — `deleteAllItems` 추가
  - `modules/shop-api/src/main/resources/mapper/cart.xml` — `deleteAllItems` 매퍼 추가
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/CartService.java` — `checkout()` 추가,
    `OrderService` 의존성 주입
  - `modules/shop-api/src/main/java/com/sm/lab/shop/controller/CartController.java` —
    `POST /api/cart/checkout` 추가
  - `modules/shop-api/src/main/java/com/sm/lab/shop/controller/CartViewController.java` —
    `POST /cart/checkout` 추가(PRG, 성공 `redirect:/order/{orderNo}`, 4xx만 흡수·5xx rethrow)
  - `modules/shop-api/src/main/resources/templates/cart/list.html` — [주문하기] 폼(품목 있을 때만)
    + 체크아웃 실패 flash 표시
  - 테스트: `CartServiceTest`(+6), `CartControllerTest`(+4), `CartViewControllerTest`(+4),
    신규 `CheckoutAtomicityTest`(실DB, TC-06)
- **itemCount 정의**: 응답 `{orderNo,totalAmount,itemCount}`의 `itemCount`는 생성된 주문의
  품목 라인 수(distinct SKU 수, `order.getItems().size()`) — 총 수량(Σqty)이 아님. D6에 수량
  단위 명시가 없어 REST 관례상 "품목 종류 수"로 해석.
- **테스트 결과**: `mvn -o -Dfile.encoding=UTF-8 test` — 87건 전체 그린(기존 72건 + 신규 15건:
  CartServiceTest 6 + CartControllerTest 4 + CartViewControllerTest 4 + CheckoutAtomicityTest 1).
  TC-06(원자성)은 실DB `@SpringBootTest`로 검증(M-0002, SKU-1002 선순위 성공권+SKU-1001 999개
  후순위 재고초과 유도 → 선순위 차감·주문·장바구니 비움 전부 롤백 확인, `@AfterEach` 정리).

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-08-23 — FAIL

> 검증 방식: 소스 직접 Read + **격리 복사본**(scratchpad, 별도 target·RANDOM_PORT)에서 라이브 재현.
> 운영 8087·`modules/shop-api/target` 무접촉, DB(sl_lab)는 기준선(ORDERS 5 / ORDER_ITEMS 7 /
> CART_ITEMS 0 / stock 12·40·7·0)으로 원복 완료 실측.

- **Layer1 스펙: concerns** — 전체 87건(기존 72 + 신규 15) 그린을 격리 복사본에서 **독립 재현**
  (CartServiceTest 22·CartControllerTest 18·CartViewControllerTest 10·CheckoutAtomicityTest 1 등).
  라이브 e2e 실측: 200 `{"itemCount":2,"orderNo":"20260823-0101","totalAmount":708000}`, 재고
  40→38·7→6, 장바구니 0건, 직후 담기 CRUD 정상 공존(TC-01/02/07~10, ④ 충족).
  ①**원자성 실코드 확인** — `OrderService.create`는 전파속성 없는 `@Transactional`(=REQUIRED)이라
  `CartService.checkout`의 트랜잭션에 그대로 참여함을 코드로 확인, 라이브에서도 후순위(SKU-1003×999)
  부족 시 선순위(SKU-1002) 재고 38→38 무변동·장바구니 2건 보존·주문 0건 재현(TC-05/06 충족).
  ②**409 사유의 sku 포함 실측** — 응답 본문 `{"status":409,...,"message":"재고 부족: SKU-1003"}`.
  다만 D6는 "부족 품목별 사유 **목록**"인데 구현은 `OrderService.create` 루프의 **첫 부족 품목
  하나만** fail-fast로 전파한다 — 3품목이 동시에 부족해도 사용자는 한 번에 한 품목만 알게 되어
  수량 조정을 반복해야 한다(D6 계약 부분 미충족).
  ⑤**itemCount** — 라인 수(=2)로 실측 확인. D6 미명시 상태에서 Dev가 REST 관례로 판단하고 기록한
  것은 적절하나 계약 정본(D6/INF-ORD-014)에 명문화되지 않아 계약 공백이 남았다.
  추가 발견: `OrderService.create`는 미존재·판매중지(sale_yn='N') 상품에 **400**("판매 중인 상품이
  아님")을 던진다. 담은 뒤 판매중지된 상품이 장바구니에 있으면 체크아웃이 400을 반환하는데,
  D6의 400은 "빈 장바구니"로만 정의돼 있어 계약 밖 응답이다(화면은 4xx 흡수라 오동작은 아니나
  사용자에게는 "빈 장바구니"와 같은 등급으로 보인다).
- **Layer2 보안: concerns** — SQL 주입 없음(MyBatis `#{}` 바인딩만, `${}` 0건). 신규 `deleteAllItems`는
  `member_id` 단일 조건으로 스코프가 좁다. 4xx 흡수 시 flash로 나가는 값은 `e.getReason()`(sku·재고)
  수준으로 내부정보 유출 없음. **다만 체크아웃은 재고를 확정 차감하는 금전적 행위인데 인증 없이
  요청 본문/폼의 `memberId`만으로 실행된다** — 임의 회원 명의 주문 생성이 가능하다(IDOR). 랩의
  기존 패턴(로그인 미구현)과 동일해 신규 결함은 아니지만, 조회·담기와 달리 되돌리기 비용이 큰
  행위라 위험 등급이 질적으로 올라갔다.
- **Layer3 회귀: fail** — ③기존 `POST /api/orders` **무변경 확인**: `.lab-ws`는 `.gitignore` 대상이라
  git diff 대조가 불가하여 (a) 파일 mtime(OrderService 08-18 21:48 / OrderController 08-18 01:24 /
  order.xml 08-18 01:36 — SR-203 작업시각 08-23 06:13 이전, 변경 파일은 Cart* + cart.xml +
  templates/cart/list.html뿐)과 (b) 라이브 호출로 대체 검증했다. 깨끗한 기준선에서
  `POST /api/orders` 200·재고 40→39 정상, 장바구니 CRUD 정상, 체크아웃으로 만든 주문이 기존
  `GET /api/orders/{no}`·목록에 동일하게 노출(TC-11/12/13 충족).
  **⑥ 차단 이슈 — 같은 회원 동시 체크아웃에서 중복 주문이 10/10 라운드 재현됐다.**
  담긴 수량이 **SKU-1002 1개뿐인 장바구니**에 동시 요청 4건을 넣었더니 `[200 200 200 200]` →
  **주문 4건 생성·재고 4 차감**(r2~r9 동일, r1은 2건, r10은 3건). `checkout`은 `selectItems`(비잠금
  일관읽기) → `OrderService.create` → `deleteAllItems`의 애플리케이션 read-modify-write이고,
  MariaDB 기본 REPEATABLE-READ에서 각 트랜잭션이 자기 스냅샷의 장바구니를 보므로 서로를 막지
  못한다. 이는 `docs/project-context.md` **Critical Rule #3**(동시 갱신은 DB 원자 연산, select→분기→
  update 금지)과 **금지 패턴 1번**이 명시적으로 금지한 패턴이며, SR-202 round1이 같은 이유로 QA
  FAIL 났던 사례의 재발이다. 화면 경로의 PRG는 F5 재제출만 막을 뿐 더블클릭·다중 탭은 막지 못한다.
  또한 실패한 요청이 받는 메시지가 "장바구니가 비어 있습니다"(400)라 성공 직후 실패 안내가 뜬다.
  **추가(중) — 채번 충돌이 409로 오분류된다.** `OrderService.seq`는 in-memory `AtomicInteger`(100
  시작)라 같은 날 재기동하면 `yyyyMMdd-0101`을 재발급한다. 별도 컨텍스트(=재기동 동치)로 재현한
  결과 체크아웃은 `409 {"message":"동시 요청이 겹쳤습니다. 다시 시도하세요"}`를 반환했다 —
  `ApiExceptionHandler`가 `CartController` 스코프로 `DuplicateKeyException`→409를 매핑하는데,
  그 매핑의 전제("CartController의 DuplicateKey = CART_ITEMS PK 동시 담기 경합 = 클라이언트 귀책")가
  체크아웃이 ORDERS를 만들면서 깨졌다. 같은 결함이 `POST /api/orders`에서는 500으로 나온다(실측)
  — 서버측 채번 결함을 클라이언트 귀책으로 표기하는 것은 Critical Rule #2가 금지한 "장애 위장"
  계열이다(데이터 정합은 보존됨 — 주문 0·장바구니 보존 확인).

- 필수 수정(FAIL시):
  1. **동시 체크아웃 중복 주문 차단**(Critical Rule #3 준수). 장바구니 전량 삭제를 선점 근거로
     삼는 원자 연산(예: `DELETE FROM CART_ITEMS WHERE member_id=?`의 영향 행수가 0이면 이미
     다른 요청이 가져간 것으로 보고 중단) 또는 `SELECT ... FOR UPDATE`로 대상 라인을 잠근 뒤
     진행하는 방식 중 하나로 재작업할 것. 애플리케이션 select→분기 유지는 불가.
  2. 위 수정의 **동시성 회귀 테스트 신설**(`CheckoutConcurrencyTest`, `@SpringBootTest(RANDOM_PORT)`,
     N≥4 — `CartConcurrencyTest` 관례 준수). 현재 신규 15건에는 동시성 케이스가 0건이라
     이 결함이 게이트를 그대로 통과했다.
  3. **AC-05 "부족 품목별 사유" 계약 정합**: 전 품목을 검사해 부족 품목 전체를 모아 409로 반환하거나,
     D6/03_TC를 "첫 부족 품목만 통지"로 개정할 것(구현·계약 중 하나를 맞춰야 함).

- 권고(CONCERNS시):
  1. 판매중지·미존재 상품이 담긴 장바구니의 체크아웃이 계약 밖 400을 반환한다 — 409로 정규화하거나
     D6에 명문화.
  2. `ApiExceptionHandler`의 `DuplicateKeyException`→409 매핑 재점검(체크아웃 도입으로 ORDERS 채번
     충돌까지 포함됨). 근본 해소는 `OrderService.seq`를 DB 채번(시퀀스/테이블)으로 대체하는 것.
  3. `itemCount` = 주문 라인 수(distinct SKU) 정의를 D6/INF-ORD-014에 명문화(현재 story Dev 기록에만 존재).
  4. 더블클릭 2번째 요청이 "장바구니가 비어 있습니다"(400) flash로 뜨는 UX — 필수수정 1과 함께 정리.
  5. 인증 부재 상태에서 `memberId`만으로 타인 명의 주문이 가능한 점을 `docs/KNOWN_LIMITATIONS.md`에 기록.

- **하우스 스타일(docs/project-context.md) 준수 관찰**: Critical Rule 5개 중 4개는 모범적으로
  준수했다 — #1 뷰 컨트롤러 4xx만 흡수·5xx rethrow(`!e.getStatusCode().is4xxClientError()` 분기 +
  `checkout_serviceThrowsNon4xxStatus_isNotAbsorbed` 전용 테스트), #4 PRG(성공 `redirect:/order/{no}`,
  실패 `redirect:/cart` + `addFlashAttribute`), #5 Thymeleaf 파서레벨 주석 전량(+ 렌더 본문에
  `linked_func` 미노출 테스트), #2 advice 스코프 유지(신규 전역 advice 없음). 서비스 계층 재사용
  (뷰가 REST를 호출하지 않음)·`linked_func` 2줄 주석·명명 규칙(`deleteAllItems`/`cart.xml`)도 일치.
  **깨진 것은 #3(동시 갱신은 DB 원자 연산)뿐이며, "원자성"을 트랜잭션 롤백으로만 해석하고
  동시성(경합)으로는 해석하지 않은 것이 원인이다** — 정본이 명시적으로 금지하고 선례(SR-202 r1)까지
  남긴 패턴이 재발했으므로, 정본 도입 첫 사이클의 준수도는 "규칙을 읽고 따랐으나 가장 비싼 규칙
  하나를 좁게 해석"으로 요약된다.

### QA Gate — 2026-08-23 — CONCERNS (round 2 · r1 FAIL 재게이트)

> 검증 방식: 소스 직접 Read + **격리 복사본**(scratchpad, 별도 target·별도 포트 8099)에서 전체
> 스위트 독립 재현 + **라이브 재현**(r1 재현법 그대로) + **변이(mutation) 검증**.
> 운영 8087 무접촉(재현 전후 200 실측·PID 유지), `modules/shop-api/target` 무접촉(jar mtime
> 08-23 05:26:17 유지 — 내 빌드는 전부 scratchpad 사본), DB(sl_lab)는 기준선
> (ORDERS 5 / ORDER_ITEMS 7 / CART_ITEMS 0 / stock 12·40·7·0 / max order_no 20260817-0002)으로
> **원복 완료 실측**. dev 주장은 신뢰하지 않고 전부 독립 재실행했다.

- **Layer1 스펙: pass** — `mvn -o -Dfile.encoding=UTF-8 test` **91/91 그린을 격리 복사본에서 독립
  재현**(2회, 상태 잔류 0). 내역 실측: CartServiceTest 24(+2)·CartDaoTest 8(+1)·
  CheckoutConcurrencyTest 1(신규)·CartControllerTest 18·CartViewControllerTest 10·
  ProductViewControllerTest 18·ProductControllerTest 4·ProductDaoTest 4·ProductServiceTest 2·
  CartConcurrencyTest 1·CheckoutAtomicityTest 1 = **91** — dev의 "기존 87 + 신규 4" 주장과 정확히 일치.
  **②필수2 해소 실측** — 다품목 부족 장바구니(SKU-1001×99, SKU-1002×1, SKU-1003×50) 체크아웃이
  `409 {"message":"재고 부족: SKU-1001(가용12/요청99), SKU-1003(가용7/요청50)"}` — **부족 품목
  전수**를 한 응답에 담았다(r1은 첫 품목 하나만 fail-fast). **전량 거부**도 실측 확인(재고
  12/40/7 무변동·주문 0건·장바구니 3건 보존). 혼합 케이스도 3건 전수 통지 확인
  (`SKU-1001(가용12/요청99), SKU-1003(가용7/요청50), SKU-1004(판매중지)`).
  **③필수3 해소 실측** — 판매중지(SKU-1004, sale_yn='N') 포함 체크아웃이 **409**를 반환(r1은
  계약 밖 400). 400/404 계약도 유지: 빈 장바구니 400·회원없음 404 실측.
  정상 경로 실측: `200 {"totalAmount":2130000,"orderNo":"20260823-0121","itemCount":2}`,
  재고 12→10·7→4, 장바구니 0 — `itemCount`=라인 수로 **D8**(2026-08-23 추기)과 일치, r1 계약
  공백이 문서로 닫혔음을 확인.
- **Layer2 보안: concerns** — 신규 `selectItemsForUpdate`는 `#{memberId}` 바인딩 단일 조건이고
  매퍼 전체에 `${}` 0건(주입 없음). 409 본문이 노출하는 재고 수량은 `GET /api/products`가 이미
  `stockQty`를 공개하므로 **신규 정보 노출 아님**(실측 확인). 이월분 3건(seq 채번 오분류 409 ·
  IDOR · CSRF)은 **손대지 않은 상태 그대로**임을 실측 확인 — `ApiExceptionHandler`(mtime 08-22
  22:31)·`OrderService`(08-18 21:48)가 r2 변경분(08-23 06:38~06:40)에 포함되지 않았고,
  `OrderService.seq`는 여전히 in-memory `AtomicInteger(100)`. story r2 Dev 기록과 사람 코멘트에
  이월이 명시돼 **이월 확정은 성립**하나, 기록이 story 본문에만 있어 종결 시 근거가 묻힌다
  (SR-202는 `## 후속 추적(TODO)` 섹션으로 승격한 선례가 있음 — 권고 6).
- **Layer3 회귀: concerns** — **①필수1 해소를 3중으로 실측했다.**
  (a) **라이브 4병렬 × 10라운드**(r1 재현법 그대로, SKU-1002 1개 장바구니):
  **10/10 라운드 전부 `[200 400 400 400]`** — 주문 정확히 1건·재고 정확히 1 차감·장바구니 0.
  r1의 `[200 200 200 200]`(주문 4건·재고 4 차감, 10/10 재현)이 **완전히 소멸**했다.
  (b) **라이브 8병렬 × 10라운드**: 10/10 전부 `[200 400×7]`, 주문 1·재고 1. 500(데드락) 0건.
  (c) **다품목(3라인) 장바구니 8병렬 × 5라운드**: 5/5 전부 성공 1건, ORDER_ITEMS 라운드당 +3,
  부분 차감·데드락 0건 — 잠금 대상이 여러 행일 때도 안전.
  패자 응답은 `400 {"message":"장바구니가 비어 있습니다"}`로, 사람 코멘트가 지시한
  "첫 트랜잭션 커밋 후 후속은 빈 장바구니 400"과 정확히 일치.
  **②필수2(회귀 테스트 신설)는 변이 검증으로 실효성을 입증했다** — 격리 사본에서
  `selectItemsForUpdate`를 r1의 `selectItems`로 되돌리자 `CheckoutConcurrencyTest`가
  `Expected size: 1 but was: 8 … [200 OK ×8]`로 **실패**했고, 원복 후 재통과했다. 즉 이 테스트는
  통과를 위한 장식이 아니라 **정확히 r1 결함을 잡는 실제 가드**다(r1 필수수정 2의 취지 충족).
  기타 회귀 실측(격리 8099): `POST /api/orders` 200·재고 40→39 정상, 장바구니 CRUD 전량 정상
  (담기 200·재담기 합산 qty 3·조회 200·수량변경 200·재고초과 409·삭제 204), 화면 PRG 유지
  (성공 302→`/order/20260823-0123`, 실패 302→`/cart?memberId=…`). r2 변경 파일은 mtime상
  `CartService.java`·`CartDao.java`·`cart.xml`(06:38) + 테스트 3종(06:39~06:40)뿐으로,
  `OrderService`/`OrderController`/`order.xml`/`CartViewController`/`list.html` 무변경 확인.
  **다만 회귀 테스트 자체에 정리 누락이 있다(권고 1)** — 변이 검증 중 실측: 테스트가 실패하면
  공용 랩 DB에 주문 8건·재고 8 차감이 그대로 남는다.

- 필수 수정(FAIL시): **없음** — r1 필수 3건 전부 실측 해소.
  ① FOR UPDATE 동시 차단: 해소(라이브 4·8병렬 20라운드 + 다품목 5라운드 전부 1건 성공)
  ② 재고 부족 품목 전수 목록 409: 해소(전수 통지·전량 거부 실측)
  ③ 판매중지 409 정합: 해소(400→409 전환 실측)

- 권고(CONCERNS시):
  1. **[medium] `CheckoutConcurrencyTest`가 실패 시 공용 DB를 오염시킨다.** `@AfterEach`는
     `createdOrderNos`에 담긴 주문만 지우는데, `createdOrderNos.add(orderNo)`가 **성공 단언 이후**에
     실행된다. 따라서 이 테스트가 잡으려는 바로 그 회귀가 발생하면 정리 대상이 비어 있고,
     생성된 주문·차감된 재고가 그대로 남는다 — 변이 검증에서 **주문 8건 잔류·SKU-1002 40→32**를
     실측했다(내가 수동 원복). 2xx 응답의 orderNo를 **단언 전에** 전부 수집하거나, `@AfterEach`가
     회원·시각 범위로 정리하도록 고칠 것.
  2. **[low] 409 메시지 접두어가 사유와 어긋난다** — 판매중지·상품없음도
     `"재고 부족: SKU-1004(판매중지)"`로 나간다. 목록 항목은 정확하나 헤드라인이 거짓이다.
     `"주문할 수 없는 품목:"` 같은 중립 접두어 권고. **아울러 D6의 409 정의가 아직
     "재고 부족"뿐이므로, 판매중지를 409에 넣은 이번 결정을 D6/INF-ORD-014에
     "409 = 주문 불가 품목(재고 부족·판매중지)"로 계약 개정해야 한다**(구현이 계약을 앞서 있음).
  3. **[low] 사전 스윕은 스냅샷 읽기라 경합 시 "목록" 보장이 깨진다.** `sweepStockIssues`의
     `productDao.selectBySku`는 비잠금 일관읽기여서 REPEATABLE-READ 스냅샷(트랜잭션 첫 읽기 시점)의
     재고를 본다. 다른 회원이 그사이 차감·커밋하면 스윕은 통과하고 `OrderService.create`의
     `decreaseStock`가 0행 → **r1과 같은 단건 fail-fast 409**("재고 부족: SKU-xxxx")가 되살아난다.
     데이터 정합은 안전하고 dev도 "최종 진실"로 문서화했으나, 계약상 "전수 목록"이 경합 구간에서만
     단건으로 퇴화한다는 점은 D6에 명문화하거나 스윕을 잠금 읽기로 올려 닫을 것.
  4. **[low] `sweepStockIssues`가 N+1 쿼리**를 CART_ITEMS 행 잠금 보유 중에 수행한다 — 품목 수만큼
     왕복이 늘어 잠금 보유 시간이 길어지고 동시 처리량이 떨어진다. `IN`절 일괄 조회 권고.
  5. **[info] `(상품 없음)` 분기는 현재 도달 불가** — `CART_ITEMS.sku`가 `PRODUCTS` FK라
     미존재 SKU는 삽입 자체가 되지 않는다(`INSERT IGNORE`로 0행 실측). 방어 코드로 무해하나
     테스트로 고정할 수 없는 경로임을 인지할 것.
  6. **[low] 이월 3건(seq 채번 오분류 409 · IDOR · CSRF)을 story 밖 원장으로 승격할 것.**
     현재 근거가 story 본문에만 있어 Done 이후 묻힌다. SR-202 story의
     `## 후속 추적(TODO)` 섹션 선례를 따르거나 후속 SR 초안으로 등록 권고.
     (참고: r1 권고 5가 지목한 `docs/KNOWN_LIMITATIONS.md`는 `.lab-ws`에 **존재하지 않는다** —
     대상 파일부터 신설하거나 다른 원장을 지정해야 한다.)
  7. **[low] 동시 요청 패자 메시지 "장바구니가 비어 있습니다"(400)** — r1 권고 4 그대로 남았다.
     데이터 정합은 안전해졌으므로 등급은 내려가나, 더블클릭 사용자에게는 여전히 원인을 오인시킨다.

- **하우스 스타일(docs/project-context.md) 준수**: r1에서 유일하게 깨졌던 **Critical Rule #3
  (동시 갱신은 DB 원자 연산, select→분기→update 금지)이 해소됐다** — 애플리케이션
  read-modify-write를 `SELECT ... FOR UPDATE` 선점으로 대체했고, 라이브·변이 양쪽으로 검증했다.
  r1에서 모범적이던 #1(4xx만 흡수)·#2(advice 스코프)·#4(PRG)·#5(파서주석)는 이번 변경이 해당
  파일을 건드리지 않아 그대로 유지됨을 mtime·라이브로 확인. **정본 도입 첫 사이클의 "가장 비싼
  규칙 하나를 좁게 해석"이 2회차에 교정됐고, 그 교정이 회귀 테스트로 고정됐다**는 것이 이번
  라운드의 요지다. 남은 권고는 전부 계약 문서화·테스트 위생·성능 계열로 차단성이 없다.

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [high/regression] 같은 회원 동시 체크아웃에서 중복 주문 10/10 라운드 재현 — 담긴 수량 1개 장바구니에 동시요청 4건 투입 시 주문 4건 생성·재고 4 차감. checkout이 selectItems→OrderService.create→deleteAllItems의 애플리케이션 read-modify-write이고 REPEATABLE-READ 스냅샷이 서로를 막지 못함. project-context.md Critical Rule #3·금지패턴 1번 위반이며 SR-202 round1 QA FAIL의 재발. 화면 PRG는 F5만 막고 더블클릭·다중탭은 못 막음 → 장바구니 전량 삭제 영향행수를 선점 근거로 삼는 원자 연산(DELETE 먼저, 0행이면 중단) 또는 SELECT ... FOR UPDATE로 대상 라인 선점 후 진행하도록 재작업
2. [high/regression] 신규 15건에 동시성 테스트가 0건이라 위 결함이 게이트를 그대로 통과했다(CheckoutAtomicityTest는 단일 트랜잭션 롤백만 검증) → CheckoutConcurrencyTest 신설 — @SpringBootTest(RANDOM_PORT), N>=4 동시 체크아웃, 주문 1건·재고 1차감만 발생함을 단언(CartConcurrencyTest 관례 준수)
3. [medium/spec] AC-05/D6는 '부족 품목별 사유 목록'인데 구현은 OrderService.create 루프의 첫 부족 품목 하나만 fail-fast로 전파(라이브 실측: message='재고 부족: SKU-1003'). 다품목 부족 시 사용자가 수량 조정을 반복해야 함 → 전 품목 검사 후 부족 품목 전체를 모아 409로 반환하거나, D6/03_TC를 '첫 부족 품목만 통지'로 개정
4. [medium/regression] OrderService.seq가 in-memory AtomicInteger(100 시작)라 같은 날 재기동 시 yyyyMMdd-0101을 재발급 → DuplicateKeyException이 CartController 스코프 ApiExceptionHandler에 걸려 409 '동시 요청이 겹쳤습니다'로 오분류(재현 실측). 같은 결함이 POST /api/orders에서는 500. advice의 전제(CartController의 DuplicateKey = CART_ITEMS 담기 경합)가 체크아웃의 ORDERS 생성으로 깨짐 — Critical Rule #2가 금지한 장애 위장 계열 → advice의 DuplicateKeyException 매핑 범위를 재점검(서버 채번 결함은 500), 근본은 OrderService.seq를 DB 채번으로 대체
5. [medium/spec] OrderService.create가 미존재·판매중지(sale_yn='N') 상품에 400을 던지므로, 담은 뒤 판매중지된 상품이 있으면 체크아웃이 400을 반환 — D6의 400은 '빈 장바구니'로만 정의된 계약 밖 응답 → 409로 정규화하거나 D6/INF-ORD-014에 400의 두 번째 사유로 명문화
6. [low/spec] itemCount = 주문 라인 수(distinct SKU, 라이브 실측 2) 결정이 D6에 없고 story Dev 기록에만 존재 — 계약 공백 → D6/INF-ORD-014에 itemCount 단위를 명문화
7. [low/regression] 더블클릭 시 두 번째 요청이 400 '장바구니가 비어 있습니다' flash로 표시되어 성공 직후 실패 안내가 뜬다 → 필수수정 1(선점 처리)과 함께 중복 제출 응답 문구 정리
8. [low/security] 체크아웃은 재고를 확정 차감하는 금전적 행위인데 인증 없이 본문/폼의 memberId만으로 실행 가능(IDOR). 랩 기존 패턴과 동일해 신규 결함은 아니나 되돌리기 비용이 큰 행위라 위험 등급 상승 → docs/KNOWN_LIMITATIONS.md에 인증 부재 스코프로 기록

대상 파일: modules/shop-api/src/main/java/com/sm/lab/shop/service/CartService.java, modules/shop-api/src/main/resources/mapper/cart.xml

사람 코멘트: 필수1: 동시 체크아웃 차단 — checkout에서 장바구니 행 SELECT..FOR UPDATE 잠금(selectItemsForUpdate 신설) → 첫 트랜잭션 커밋 후 후속은 빈 장바구니 400. 8병렬 동시성 테스트(주문 1·나머지 400·재고 정확) 필수. 필수2: 재고 부족 사유를 품목별 '목록'으로 — 사전 스윕으로 부족 품목 전수 수집해 409 본문에 배열(최종 진실은 트랜잭션 차감 유지). 필수3: 판매중지 포함 체크아웃 409 계열 정합. 이월: seq 채번 오분류(선재 in-memory 채번 설계 — 후속 SR 후보 TODO), itemCount 정의는 D6에 추기(문서).

## Dev 기록 (r2 — QA FAIL round1 재작업)

- **필수1 — 동시 체크아웃 차단**: `CartDao`에 `selectItemsForUpdate`(SELECT...FOR UPDATE, 상품
  조인 없이 CART_ITEMS만 잠금)를 신설하고, `CartService.checkout`이 회원 검증 직후 이 메서드로
  장바구니 행을 먼저 잠근 뒤 진행하도록 변경(기존 `selectItems` 비잠금 호출을 대체). InnoDB
  락킹 리드는 트랜잭션 스냅샷이 아니라 최신 커밋 데이터를 읽고 잠그므로, 선행 트랜잭션이 이
  행을 삭제·커밋하면 잠금 대기 중이던 후속 트랜잭션은 잠금 해제 후 빈 결과를 보고 400(빈
  장바구니)으로 정직하게 거부된다 — 이것이 round1(REPEATABLE-READ 스냅샷이 서로를 못 막던
  read-modify-write)을 근본적으로 대체하는 지점.
- **필수2 — 재고 부족 품목별 사유 목록**: `checkout`에 `sweepStockIssues` 사전 스윕을 추가 —
  잠금 직후 전 품목을 순회하며 `productDao.selectBySku`로 재고·판매상태를 검사하고, 부족·
  판매중지·미존재 품목 전체를 `"SKU-A(가용3/요청5), SKU-B(...)"` 형식으로 모아 하나의 409에
  담는다(round1은 `OrderService.create` 루프의 첫 부족 품목 하나만 fail-fast 통지). 사전 스윕은
  안내용이며, 스윕 통과 후에도 `OrderService.create`의 실제 `decreaseStock` 실패 경로는 그대로
  남겨(시점 차로 인한 경합 대비) 최종 진실로 유지.
- **필수3 — 판매중지 상품 정합**: 위 사전 스윕이 `sale_yn='N'` 품목을 `"(판매중지)"` 사유로 함께
  판정해 409로 응답한다(round1은 `OrderService.create`가 400을 던져 D6 "400=빈 장바구니" 계약
  밖 응답이었음).
- **이월(손대지 않음)**: `OrderService`의 in-memory `AtomicInteger` 채번 설계·IDOR·CSRF는 이번
  재작업 범위 밖(별도 SR 후보로 남김).
- **생성/수정 파일**:
  - `modules/shop-api/src/main/resources/mapper/cart.xml` — `selectItemsForUpdate` 신설
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/CartDao.java` — `selectItemsForUpdate` 선언 추가
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/CartService.java` — `checkout()`이
    `selectItemsForUpdate` 사용하도록 변경 + `sweepStockIssues` 신설
  - 테스트: `CartServiceTest`(+2 신규: 다품목 부족 목록 취합, 판매중지 409 / 기존 4건은
    `selectItemsForUpdate` 반영으로 갱신), `CartDaoTest`(+1: `selectItemsForUpdate`), 신규
    `CheckoutConcurrencyTest`(+1, 실DB·RANDOM_PORT, N=8 동시 체크아웃)
- **동시성 테스트 실측**(`CheckoutConcurrencyTest`, M-0001/SKU-1002 qty=1 장바구니에 8병렬
  `/api/cart/checkout` 동시 요청, `CartConcurrencyTest`와 동일한 submit→ready 정족→start 해제
  패턴 — `start.countDown()` 누락 없이 구현): 8건 중 **성공(2xx) 정확히 1건**·**나머지 7건
  400**(빈 장바구니), **주문 정확히 1건 생성**, **재고 정확히 1개만 차감**, 체크아웃 후 장바구니
  0건 — round1 재현(4건 동시요청 전부 200·주문 4건·재고 4 차감)이 해소됨을 확인. 격리 없이
  `modules/shop-api`에서 직접 실행(운영 8087과 별도 프로세스·RANDOM_PORT, `mvn clean` 미실행으로
  target 보존), 테스트 후 `@AfterEach`가 생성된 주문(ORDER_ITEMS/ORDERS)·재고·장바구니를 전부
  원복 — 전체 스위트 2회 연속 실행으로 상태 잔류 없음(멱등) 확인.
- **테스트 결과**: `mvn -o -Dfile.encoding=UTF-8 test` — **91건 전체 그린**(기존 87건 + 신규 4건:
  CartServiceTest +2, CartDaoTest +1, CheckoutConcurrencyTest +1). 운영 8087·`modules/shop-api/target`
  무접촉(`clean` 미사용, 증분 컴파일만).

## 후속 추적(TODO) — QA r2 CONCERNS 이월 (2026-08-23, [track] 결정)
| # | 항목 | 등급 | 처리 |
|---|------|------|------|
| 1 | CheckoutConcurrencyTest 실패 시 DB 오염(정리 등록이 단언 이후) | medium | **즉수정 완료** — 성공 응답 전수 등록을 단언 이전으로 이동, 단건 재실행 그린 |
| 2 | 409 접두어 부정합(판매중지도 \'재고 부족:\') + D6 계약 소급 | low | D9 추기 완료(계열 확정), 접두어 통일은 후속 |
| 3 | 사전 스윕이 스냅샷 읽기 — 경합 시 단건 fail-fast 409로 퇴화(정합 안전) | low | 문서화로 종결 |
| 4 | sweepStockIssues N+1 쿼리를 행 잠금 보유 중 수행 | low | 후속(품목 수 소규모 — 현재 무해) |
| 5 | 패자 응답 메시지 \'장바구니가 비어 있습니다\'(의미상 \'이미 처리됨\') | low | 후속 |
| 6 | OrderService in-memory seq 채번(재기동 충돌·advice 409 오분류) | 이월 | 후속 SR 후보 |
| 7 | IDOR(memberId 본문 신뢰)·CSRF 미대응 | 이월 | 랩 전제(인증 없음) — 후속 SR 후보 |
