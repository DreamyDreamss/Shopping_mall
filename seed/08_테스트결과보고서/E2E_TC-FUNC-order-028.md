# E2E 실행 결과 — TC-FUNC-order-028

| 항목 | 값 |
|------|-----|
| TC | TC-FUNC-order-028 — 시나리오: 상품 조회 (검색→상세 이동) |
| 화면 | UIS-ORD-003 상품 목록 · `/product/list` |
| 대상 | http://localhost:8087 (로컬) |
| 실행 | 2026-08-23T15:55:00+09:00 · e2e-agent (CLI 폴백 — `e2e_drive.js`) |
| **판정** | **PASS** |

## 단계별 결과

| # | 절차 | 조작 | 화면 사실(근거) | 판정 |
|---|------|------|----------------|------|
| 1 | 「상품명」 입력란에 검색어 "키보드" 입력 | fill ref=e1(field=`keyword`) = "키보드" | snapshot에서 `value:"키보드"` 확인 | PASS |
| 2 | 「검색」 버튼 클릭 → GET /product/list?keyword=… 재요청 | click ref=e2(id=`btnSearch`) | 최초 1회 클릭은 URL·목록 불변(미반영, 아래 간극 참고) → 재시도 클릭에서 `url`이 `?keyword=%ED%82%A4%EB%B3%B4%EB%93%9C`로 바뀌고 목록이 SKU-1002(기계식 키보드) 1건으로 필터됨 | PASS (재시도 후) |
| 3 | 결과 목록에서 상품명 링크 클릭 → GET /product/{sku} 이동 | click ref=e3(`기계식 키보드` 링크) | url이 `/product/SKU-1002`로 이동, title="상품 상세" | PASS |
| 4 | 상세 필드 대조 | — | 본문 "SKU-1002 \| 기계식 키보드 \| 129,000원 \| 40 \| 판매중" 확인 | PASS |
| 5 | 목록 복귀 | click ref=e1(`← 목록으로`) | url이 `/product/list`로 복귀, 3건 전체 재표시 | PASS |

## 기대결과 대조

- 기대: "GET /product/list?keyword=…로 재요청되며 keyword LIKE 필터 적용된 목록 갱신"
  → 실측: 검색 후 url=`http://localhost:8087/product/list?keyword=%ED%82%A4%EB%B3%B4%EB%93%9C`, 목록이 SKU-1002 1건(기계식 키보드)으로 축소 → **PASS**
  (교차검증: `goto --arg "/product/list?keyword=..."` 직접 이동으로도 동일하게 1건 필터됨을 별도 확인 — 서버측 LIKE 필터는 정상)
- 기대: "상품명 링크 클릭 시 GET /product/{sku}로 이동해 상품 상세 화면 확인"
  → 실측: `/product/SKU-1002` 이동, "상품 상세 — SKU-1002" 제목·SKU/상품명/가격/재고/상태 5필드 모두 표시 → **PASS**
- 기대(문서 §4 표 (1)(2) 위젯 id `keyword`/`btnSearch`가 실제 DOM과 일치)
  → 실측: input `field=keyword`, button `id=btnSearch` — 문서 위젯 id와 실제 DOM 속성이 **완전히 일치**(간극 없음)

## 문서-DOM 간극 목록

- **위젯 id 간극: 없음.** 문서 §4의 `keyword`(입력)·`btnSearch`(버튼) 표기가 실제 DOM(`name=keyword`, `id=btnSearch`)과 정확히 일치했다. 자동 생성 스펙(`tests/e2e/UIS-ORD-003.spec.ts`)의 실패는 위젯 id 불일치가 아니라, 문서 서술만으로 "검색어를 무엇으로 채울지"·"검색 후 필터 결과를 어떻게 단언할지"가 `TODO(사람)`으로 비어 있던 **조작 미확정** 때문이었다(예: 2단계 test.step 코멘트가 절차 원문을 그대로 잘라 붙여 문장이 중간에서 끊김, 3단계는 `keyword` 위젯을 재사용해 상품명 링크를 클릭하려 시도 — 실제로는 별개 `<a>` 요소라 매핑이 틀림).
- **CLI 드라이버 재현성 간극(문서와 무관, 드라이버 이슈로 기록):** 첫 번째 `click(btnSearch)` 호출은 `ok:true`를 반환했지만 실제로는 URL·목록에 변화가 없었다(폼 제출 미발생 추정). 동일 조작을 재시도하니 정상 반영됐다. 좌표 기반 클릭(CDP `Input.dispatchMouseEvent`)이 첫 호출에서 폼 제출 이벤트를 놓친 것으로 보이며, occlusion은 스크린샷으로 배제했다(요소가 완전히 노출된 상태였음). JS 폴백 전환 없이 재시도만으로 해결되어 별도 폴백은 사용하지 않았다.

## 증거

- `_tmp/e2e_shots/before_search.png` — 검색어 입력 직후(제출 전)
- `_tmp/e2e_shots/after_search_click.png` — 검색 결과 필터 확인(SKU-1002 1건)
- `_tmp/e2e_shots/product_detail.png` — 상품 상세 화면(SKU-1002)

## 미수행·제약

- 상세 화면의 "담기"(장바구니 추가) 폼은 **쓰기 동작**으로 판단해 조작하지 않음(`E2E_WRITE=deny`, 절대규율 4) — `memberId`/`qty` 선택·제출 모두 SKIPPED.
- 검색어 "키보드"는 호출자 지시값 그대로 사용(임의 선택 아님).
