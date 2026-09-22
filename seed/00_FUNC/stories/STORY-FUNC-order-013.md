---
story-id: STORY-FUNC-order-013
func-id: FUNC-order-013
status: Done
domain: order
created: 2026-09-09
spec_markers: 0
sr-id: SR-223
approved_sha: 5942ada20fb2
---

# STORY-FUNC-order-013 — 주문 목록 CSV 내보내기

## Story
주문 목록 CSV 내보내기


## 변경 컨텍스트 (SR-223)
> 이 story는 변경요청 **SR-223 — SR-223** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-223/00_요구사항.md`

### 확정된 요건 문답 8건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 내려받은 CSV의 헤더가 영문 컬럼명이라 담당자가 매번 물어본다. - 헤더를 주문번호·주문일시·회원·상태·금액으로 바꾼다 - 데이터 행·순서·인코딩은 그대로다 - 헤더만 바꾸고 파일명 규칙은 건드리지 않는다 / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 조회 결과 전부(데이터 계약 불변)
- **기존 클라이언트와의 하위호환이 필요한가?** — 필드 추가·표시 변경만이라 하위호환이 유지된다. 기존 필드명·타입·의미는 그대로 둔다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 이 SR은 새 오류 계약을 만들지 않는다(요구 본문에 코드가 명시된 경우 그 코드를 따른다). 기존 오류 응답 형식·상태코드는 그대로다.
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음
- **기존 데이터 이관·백필이 필요한가?** — 불필요(신규 데이터만)
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 요구 본문에 나열된 화면이 전부다. 같은 데이터를 쓰는 다른 화면은 이번 범위가 아니다.
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 빈 값은 '-'로, 조회 결과 0건은 안내 문구로 표시한다. 오류는 배너로 따로 알린다.

## 수용 기준 (Acceptance Criteria)
- [ ] INF-ORD-015 (GET /api/orders/export): 조회 규칙은 [[INF-ORD-003]]과 완전히 동일(`OrderDao.selectOrders` 재사용) — 별도 조회 규칙 없음. `del_yn = 'N'` 상시필터 포함
- [ ] INF-ORD-015 (GET /api/orders/export): 페이징 미적용: `offset=0`, `size=Integer.MAX_VALUE`로 고정 호출(전체 건 CSV화)
- [ ] INF-ORD-015 (GET /api/orders/export): `orderedAt`은 `ISO_LOCAL_DATE_TIME` 고정 패턴으로 직렬화(가변폭 금지)
- [ ] INF-ORD-015 (GET /api/orders/export): CSV 필드는 RFC 4180 인용 규칙 적용(콤마·큰따옴표·개행 포함 시 큰따옴표로 감싸고 내부 큰따옴표는 이중화)
- [ ] INF-ORD-015 (GET /api/orders/export): CSV 수식 인젝션 방어: 필드 선두 문자가 `=`,`+`,`-`,`@`이면 작은따옴표(`'`)를 앞에 붙여 Excel이 수식으로 해석하지 않게 함(D11)
- [ ] INF-ORD-015 (GET /api/orders/export): 파일 본문은 UTF-8 BOM 프리픽스 포함(Excel에서 한글 깨짐 방지)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-015: INF-ORD-015: GET /api/orders/export — 주문 목록 CSV 내보내기 / > **개요:** [[INF-ORD-003]]과 동일한 필터·정렬·상시필터로 조회한 주문 목록을 CSV 파일로 내려받는다. (linked_func: FUNC-order-013, LAB-104) / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/OrderController.java:79-90` / 요청 — [docs/05_설계서/order/INF/INF-ORD-015.md](../../05_설계서/order/INF/INF-ORD-015.md)
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)
- **기존 구현 파일**: _src/shop-api/src/main/java/com/sm/lab/shop/controller/OrderController.java, _src/shop-api/src/main/java/com/sm/lab/shop/service/ExportRowLimitExceededException.java, _src/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java, _src/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java, _src/shop-api/src/main/java/com/sm/lab/shop/web/OrderApiExceptionHandler.java, _src/shop-api/src/test/java/com/sm/lab/shop/controller/OrderControllerTest.java, _src/shop-api/src/test/java/com/sm/lab/shop/service/OrderExportIntegrationTest.java, _src/shop-api/src/test/java/com/sm/lab/shop/service/OrderServiceTest.java, _src/shop-api/src/test/java/com/sm/lab/shop/support/AdminApiKeyTestConfig.java, _src/shop-api/src/test/java/com/sm/lab/shop/web/ApiKeyAuthIntegrationTest.java


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

### r3 (SR-223) — CSV 헤더 한글화
- **사전 확인**: `INF-ORD-015`의 AC 6개(조회규칙 재사용·페이징 미적용·ISO 날짜 포맷·RFC4180 인용·CSV 수식
  인젝션 방어·UTF-8 BOM)는 이미 `OrderService.exportCsv`/`csvField`에 전부 구현돼 있었음(round1/round2,
  D10/D11/SR-204 R-3에서 완료). 이번 SR-223 범위는 헤더 텍스트 한글화 1건뿐 — 나머지는 재구현하지 않음.
- **변경 파일**:
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java` — `CSV_HEADER` 상수를
    `"orderNo,memberId,status,totalAmount,orderedAt"` → `"주문번호,회원,상태,금액,주문일시"`로 변경.
    데이터 컬럼 순서·필드명·타입·의미(`orderNo,memberId,status,totalAmount,orderedAt`)는 그대로 유지 —
    한글 라벨은 같은 위치의 컬럼에 1:1 매핑(주문번호↔orderNo, 회원↔memberId, 상태↔status,
    금액↔totalAmount, 주문일시↔orderedAt).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/service/OrderServiceTest.java` —
    `exportCsv_headerRowMatchesListResponseFieldAxis`, `exportCsv_emptyResult_returnsOnlyBomAndHeader`
    두 테스트의 기대 헤더 문자열을 한글로 갱신.
- **결정(컬럼 순서 vs SR 문구 순서)**: SR-223 본문은 "주문번호·주문일시·회원·상태·금액" 순으로 나열했지만,
  같은 SR의 확정 문답(`api_compat`)이 "기존 필드명·타입·의미는 그대로 둔다"(표시 변경만) 명시하고
  본문도 "데이터 행·순서·인코딩은 그대로"라고 못박아 컬럼 재배치를 배제함 — SR 문구는 헤더에 쓸
  한글 용어 집합을 제시한 것으로 해석하고, 기존 데이터 컬럼 순서(orderNo,memberId,status,totalAmount,
  orderedAt)에 맞춰 라벨을 배치함(주문번호,회원,상태,금액,주문일시).
- **회귀 확인**: `OrderServiceTest`(20/20), `OrderControllerTest`(13/13) 전체 통과.
  `OrderExportIntegrationTest`(실 DB)는 헤더 텍스트를 직접 단언하지 않아 영향 없음(데이터 행만 검사).

### r4 (round 9 QA CONCERNS carry-back) — 스펙·TC 문서 현행화 + SR-223 헤더 순서 근거 기록 + 테스트 픽스처 정리
- **재작업 대상**: round 9 QA CONCERNS 3건(모두 medium/low, 차단 사유 아님) — 코드 동작 변경 없음, 문서·테스트 정리만.
- **변경 파일**:
  1. `docs/05_설계서/order/INF/INF-ORD-015.md` — 응답 예시(46행 부근)의 CSV 헤더를
     `orderNo,memberId,status,totalAmount,orderedAt` → `주문번호,회원,상태,금액,주문일시`로 갱신하고,
     데이터 컬럼 순서·의미는 불변이며 라벨만 1:1 치환했다는 설명과 SR-223 근거 링크를 예시 아래에 추가.
  2. `docs/변경관리/LAB-104/03_TC.md` — TC-FUNC-order-013-01 기대 헤더를 한글 헤더로 갱신(SR-223·데이터
     컬럼 순서 불변 근거 병기).
  3. `docs/변경관리/SR-223/00_요구사항.md` — 확정 문답에 `header_label_order` 항목을 신규 추가.
     SR 본문 나열 순서(주문번호·주문일시·회원·상태·금액)는 순서 지시가 아니라 "사용할 한글 용어 집합"
     제시로 해석하고, 기존 확정 답변(`api_compat`: 필드명·타입·의미 불변, `scope_freeze`: 데이터 행·
     순서·인코딩 그대로)과의 충돌을 피하려면 데이터 컬럼 순서(orderNo,memberId,status,totalAmount,
     orderedAt)에 라벨을 1:1 매핑하는 해석이 유일하게 안전하다는 근거를 명시 기록(요청자 확인용 —
     이미 승인된 해석의 문서화이며 재질문 아님).
  4. `modules/shop-api/src/test/java/com/sm/lab/shop/controller/OrderControllerTest.java` —
     `export_noFilter_returns200WithCsvContentType`의 스텁 본문 문자열을 영문 헤더 →
     `주문번호,회원,상태,금액,주문일시`로 교체(실제 서비스 출력과 정합).
  5. `modules/shop-api/src/test/java/com/sm/lab/shop/service/OrderServiceTest.java` —
     `exportCsv_headerRowMatchesListResponseFieldAxis` → `exportCsv_headerRow_usesKoreanLabels`로 개명
     (헤더가 이제 응답 필드 축과 분리되어 구 이름이 동작을 더 이상 설명하지 못함).
- **범위 밖으로 남긴 것**: `docs/00_FUNC/gates/GATE-FUNC-order-013.json`, `docs/08_테스트결과보고서/
  TR-FUNC-order-013.md`는 이전 라운드 QA/TR의 이력 기록이라 옛 메서드명이 남아 있음 — 이는 qa-agent/
  test-agent가 다음 라운드에 재생성하는 산출물이라 dev 재작업 범위(코드+정본 스펙)에서 제외.
- **회귀 확인**: `mvn -o -Dtest=OrderServiceTest,OrderControllerTest test` → **33/33 green**
  (OrderServiceTest 20, OrderControllerTest 13). 프로덕션 코드(`OrderService.java` `CSV_HEADER`)는
  이번 라운드에서 무변경 — 문서·테스트 텍스트만 정리했으므로 회귀 위험 없음.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-09 — CONCERNS
대상: r3 (SR-223 — CSV 내보내기 헤더 한글화) / 변경 2파일(`OrderService.java` `CSV_HEADER` 상수, `OrderServiceTest.java` 기대값 2건)

- **Layer1 스펙**: AC 6건 전부 유지 확인 — 조회 재사용(`selectOrders(memberId, orderState, null, null, 0, Integer.MAX_VALUE)`),
  페이징 미적용, `ISO_LOCAL_DATE_TIME` 고정 직렬화, RFC 4180 인용, 수식 인젝션 방어(`csvField`), UTF-8 BOM —
  모두 이번 변경으로 손상되지 않음(`exportCsv` 본문은 헤더 상수 외 무변경). SR-223 요건(헤더 한글화)은 반영됨.
  다만 ① 정본 `INF-ORD-015.md`의 응답 예시(46행)와 `docs/변경관리/LAB-104/03_TC.md`(TC-FUNC-order-013-01)의
  기대 헤더가 여전히 영문(`orderNo,memberId,...`)이라 **구현과 문서가 어긋난 상태**, ② SR 본문의 나열 순서
  (주문번호·주문일시·회원·상태·금액)와 구현 라벨 순서(주문번호·회원·상태·금액·주문일시)가 다름 — dev의
  근거("데이터 순서 그대로" + api_compat)는 타당하고 라벨-데이터 오매핑을 피하는 유일한 해석이지만
  요청자 확인이 남는다. 둘 다 차단 사유는 아님.
- **Layer2 보안**: 이상 없음. `CSV_HEADER`는 사용자 입력이 섞이지 않는 정적 상수이며 선두 문자가 한글이라
  수식 인젝션 표면이 아니다(데이터 필드의 `csvField` 방어는 무변경). 인증·인가(admin 키 전용), 413 반출
  상한, 오류 계약 모두 무변경. 헤더 한글화로 새로 열린 취약점 없음.
- **Layer3 회귀**: 실측 통과 — `mvn -o -Dtest=OrderServiceTest,OrderControllerTest test` → **33/33 green**
  (OrderServiceTest 20, OrderControllerTest 13). `OrderExportIntegrationTest`는 `dataLines()`가 첫 행을
  버려 헤더 비의존이므로 영향 없음(실 DB 미기동으로 이번 회차 미실행). 데이터 행 직렬화 경로·컬럼 순서·
  BOM·CRLF·파일명 규칙 무변경. **인코딩 실측**: `target/classes/.../OrderService.class` 상수 풀에 한글이
  정상 UTF-8(`ec a3 bc eb ac b8 ...`)로 들어있음 — spring-boot-starter-parent의 `sourceEncoding=UTF-8`이
  적용돼 mojibake 없음(테스트만으로는 main/test 동시 깨짐을 못 잡으므로 클래스 파일로 직접 확인).
- 권고(CONCERNS):
  1. **[medium] 스펙·TC 문서 현행화** — `docs/05_설계서/order/INF/INF-ORD-015.md:46` 응답 예시와
     `docs/변경관리/LAB-104/03_TC.md:8`의 기대 헤더를 `주문번호,회원,상태,금액,주문일시`로 갱신.
     방치하면 다음 QA·`/sl-sync`가 stale 문서를 기준으로 구현을 오판한다(SR-223 종결 전 필수).
  2. **[medium] 헤더 라벨 순서 요청자 확인** — SR 본문 문구 순서와 구현 순서가 다르다는 사실과 그 근거
     (데이터 컬럼 재배치 배제)를 SR-223 확정 기록에 남겨 사후 이견을 차단.
  3. **[low] 테스트 픽스처·이름 stale** — `OrderControllerTest.export_noFilter_returns200WithCsvContentType`
     (50행)의 스텁 본문이 아직 영문 헤더 문자열이고(스텁-단언 자기일관이라 통과하지만 실제 서비스 출력과
     불일치), `OrderServiceTest.exportCsv_headerRowMatchesListResponseFieldAxis`는 이름이 더 이상 동작을
     설명하지 못한다(헤더가 응답 필드 축과 분리됨). 다음 손댈 때 정리 권고.

### QA Gate — 2026-09-09 — CONCERNS
대상: r4 (round 9 QA CONCERNS carry-back 검증) / 변경 5파일(INF-ORD-015.md, LAB-104/03_TC.md, SR-223/00_요구사항.md, OrderControllerTest.java, OrderServiceTest.java). 프로덕션 코드 무변경.

**round 9 top_issues 3건 해소 판정: #1 해소 · #2 부분해소 · #3 해소**

- **Layer1 스펙**: AC 6건 전부 유지 — `exportCsv` 본문(`OrderService.java:113-140`)을 재확인해
  `selectOrders(memberId, orderState, null, null, 0, Integer.MAX_VALUE)`(조회 재사용·페이징 미적용),
  `ORDERED_AT_FORMAT = ISO_LOCAL_DATE_TIME`, `csvField`의 RFC 4180 인용 + 수식 인젝션 프리픽스,
  `UTF8_BOM` 프리픽스가 모두 무변경임을 확인(이번 라운드는 프로덕션 델타 0).
  ① **round9 #1 해소** — `INF-ORD-015.md:46` 응답 예시가 `주문번호,회원,상태,금액,주문일시`로 갱신되고
  50-53행에 라벨 1:1 치환 근거 + SR-223 링크가 병기됨. `LAB-104/03_TC.md:8` TC 기대 헤더도 한글로 갱신됨.
  ② **round9 #2 부분해소** — 해석 근거가 `SR-223/00_요구사항.md:50-60`(`clarify:header_label_order`)에
  기록됐으나 **정본인 `docs/변경관리/SR-223/_clarify.json`에는 반영되지 않았다**(항목 8건 그대로).
  `build_story.py:357`이 STORY의 "확정된 요건 문답"을 `_clarify.json`에서 생성하므로, story를 재생성하면
  이 결정이 다시 사라진다 — 마크다운은 파생 렌더링이라 사람은 읽어도 파이프라인은 못 본다.
  ③ **신규(low)** — `LAB-104/inputs/_decisions.md`는 D10:8에 영문 헤더를 남기고, D12:30-32가
  "INF-ORD-015 역생성 시 CSV 열: status로 명문화할 것"이라는 **현재 유효한 지시 형태**로 남아 SR-223에
  의해 뒤집힌 사실을 표시하지 않는다. append-only 결정 로그이므로 D10/D12 수정이 아니라 D13 supersede
  항목 추가가 맞다. 둘 다 차단 사유 아님.
- **Layer2 보안**: 이상 없음. 이번 라운드 변경은 문서 텍스트와 테스트 픽스처 문자열뿐이고 프로덕션
  델타가 0이다. `CSV_HEADER`는 사용자 입력이 섞이지 않는 정적 상수이며 선두가 한글이라 수식 인젝션
  표면이 아니다. 데이터 필드의 `csvField` 방어·admin 키 전용 인가·413 반출 상한·오류 계약 모두 무변경.
- **Layer3 회귀**: **독립 실측 통과** — `mvn -o -Dtest=OrderServiceTest,OrderControllerTest test` →
  **Tests run: 33, Failures: 0, Errors: 0 / BUILD SUCCESS**(OrderControllerTest 13, OrderServiceTest 20).
  **round9 #3 해소** — `OrderControllerTest.java:50` 스텁 본문이 한글 헤더로 교체됐고,
  `OrderServiceTest`의 메서드가 `exportCsv_headerRow_usesKoreanLabels`(70행)로 개명됨.
  **인코딩 바이트 실측**: 컨트롤러 스텁은 스텁-단언 자기일관이라 테스트 통과만으로는 mojibake를 못 잡으므로
  직접 확인 — 세 소스(`OrderControllerTest`/`OrderServiceTest`/`OrderService`)가 모두 동일한 UTF-8
  헤더 바이트열(`ec a3 bc eb ac b8 ...`)을 갖고 U+FFFD 0건, 컴파일된 `OrderService.class` 상수 풀도
  정상 UTF-8 — 스텁 바이트가 프로덕션 `CSV_HEADER`와 정확히 일치한다.
  **선재 실패(무관)**: `OrderListIntegrationTest` 1F+1E(리포트 09-07자 — startDate/endDate 404,
  members FK 위반)는 CSV export 경로와 무관하고 이번 라운드가 건드리지 않았다. 단 이 워크스페이스에는
  `.speclinker/test_baseline.json`이 없어 "선재"임을 기준선으로 대조하지는 못했다(리포트 날짜로만 판정).
  `TR-FUNC-order-013.md:29`의 구 메서드명은 test-agent가 이번 라운드에 재생성하는 산출물이라 수용.
- 권고(CONCERNS):
  1. **[medium] `_clarify.json`에 `header_label_order` 반영** — `docs/변경관리/SR-223/_clarify.json`에
     같은 문답을 추가해야 정본이 된다. 마크다운만 고친 현재 상태는 story 재생성 시 유실되고,
     round9 #2가 막으려던 "사후 이견"이 그대로 되살아난다.
  2. **[low] LAB-104 결정 로그 supersede 표시** — `docs/변경관리/LAB-104/inputs/_decisions.md`에
     D13을 추가해 D10:8 헤더와 D12의 "CSV 열: status 명문화" 지시가 SR-223으로 대체됐음을 남길 것.
     방치하면 다음 `/sl-change`·`/sl-sync`가 이 로그를 근거로 헤더를 영문으로 되돌릴 수 있다.

## 재작업 지시
> round 9 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/spec] INF-ORD-015.md:46 응답 예시와 docs/변경관리/LAB-104/03_TC.md:8 TC 기대 헤더가 영문(orderNo,memberId,status,totalAmount,orderedAt)으로 남아 구현(주문번호,회원,상태,금액,주문일시)과 불일치 — 정본 문서가 stale → INF-ORD-015 응답 예시와 LAB-104 03_TC.md 기대 헤더를 한글 헤더로 현행화(SR-223 종결 전)
2. [medium/spec] SR-223 본문 나열 순서(주문번호·주문일시·회원·상태·금액)와 구현 라벨 순서(주문번호·회원·상태·금액·주문일시)가 다름 — dev는 '데이터 순서 불변'·api_compat 근거로 데이터 컬럼 순서에 맞춰 배치(라벨-데이터 오매핑을 피하는 유일한 해석이나 요청자 확인 미완) → 헤더 라벨 순서 해석을 SR-223 확정 문답에 명시적으로 기록하고 요청자 확인 받기
3. [low/regression] OrderControllerTest.export_noFilter_returns200WithCsvContentType(50행) 스텁 본문이 여전히 영문 헤더 문자열이라 실제 서비스 출력과 괴리(스텁-단언 자기일관이라 통과), OrderServiceTest.exportCsv_headerRowMatchesListResponseFieldAxis는 이름이 현 동작을 설명하지 못함 → 컨트롤러 테스트 픽스처 문자열을 한글 헤더로 맞추고 서비스 테스트 메서드명을 헤더 라벨 검증 취지로 개명

사람 코멘트: QA CONCERNS 3건 함께 고치기: (1) INF-ORD-015.md:46 응답예시 + LAB-104/03_TC.md:8 TC 기대헤더를 한글헤더(주문번호,회원,상태,금액,주문일시)로 현행화 (2) SR-223 확정 문답에 헤더 라벨 순서 해석(데이터 컬럼 순서 불변 원칙에 따라 라벨만 1:1 매핑) 명시 기록 (3) OrderControllerTest 스텁 본문을 한글 헤더로 맞추고 OrderServiceTest의 exportCsv_headerRowMatchesListResponseFieldAxis 메서드명을 현재 동작(헤더 라벨 검증)에 맞게 개명
