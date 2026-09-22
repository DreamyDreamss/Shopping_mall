# E2E 테스트케이스 (UIS 유래)

> 생성: `build_e2e_plan.py` · 2026-08-23 · 13건. 화면설계서 §2 작업 시나리오 1개 = TC 1개.
> 실행: `python <PLUGIN>/scripts/run_tests.py . --tc <TC-ID>` (또는 뷰어 [테스트 · AC]에서 케이스별 실행)
> ⚠️ 생성된 스펙에 `TODO(사람)`이 남아 있으면 그 단계는 아직 검증되지 않습니다.

| TC-ID | 화면 | 라우트 | 시나리오 | 단계 | 스펙 파일 |
|-------|------|--------|----------|-----:|-----------|
| TC-FUNC-order-025 | [UIS-ORD-001] 주문 목록 | `/order/list` | 시나리오: 주문 조회 | 4 | `tests/e2e/UIS-ORD-001.spec.ts` |
| TC-FUNC-order-038 | [UIS-ORD-001] 주문 목록 | `/order/list` | 시나리오: 조회기간 프리셋 버튼 클릭(SR-209) — `#btnPreset7`/`#btnPreset30`/`#btnPreset90` 클릭 시 `#startDate`/`#endDate`가 오늘 기준 N일 전~오늘로 채워지고 즉시 `GET /order/list` 재조회되며, 기존 `memberId`/`orderState` 입력값은 AND 결합으로 유지된다 | 4 | `tests/e2e/UIS-ORD-001.spec.ts` |
| TC-FUNC-order-026 | [UIS-ORD-002] 주문 상세 | `/order/{orderNo}` | 시나리오: 주문 상세 조회 | 6 | `tests/e2e/UIS-ORD-002.spec.ts` |
| TC-FUNC-order-027 | [UIS-ORD-002] 주문 상세 | `/order/{orderNo}` | 시나리오: 주문 취소 | 3 | `tests/e2e/UIS-ORD-002.spec.ts` |
| TC-FUNC-order-028 | [UIS-ORD-003] 상품 목록 | `/product/list` | 시나리오: 상품 조회 | 3 | `tests/e2e/UIS-ORD-003.spec.ts` |
| TC-FUNC-order-029 | [UIS-ORD-004] 상품 상세 | `/product/{sku}` | 시나리오: 상품 상세 조회 (정상 SKU) | 4 | `tests/e2e/UIS-ORD-004.spec.ts` |
| TC-FUNC-order-030 | [UIS-ORD-004] 상품 상세 | `/product/{sku}` | 시나리오: 존재하지 않는 SKU로 접근 | 4 | `tests/e2e/UIS-ORD-004.spec.ts` |
| TC-FUNC-order-031 | [UIS-ORD-004] 상품 상세 | `/product/{sku}` | 시나리오: 장바구니 담기 (재고 있음, SR-202 실측) | 7 | `tests/e2e/UIS-ORD-004.spec.ts` |
| TC-FUNC-order-032 | [UIS-ORD-004] 상품 상세 | `/product/{sku}` | 시나리오: 품절 상품 | 1 | `tests/e2e/UIS-ORD-004.spec.ts` |
| TC-FUNC-order-033 | [UIS-ORD-005] 장바구니 | `/cart` | 시나리오: 회원별 장바구니 조회 | 3 | `tests/e2e/UIS-ORD-005.spec.ts` |
| TC-FUNC-order-034 | [UIS-ORD-005] 장바구니 | `/cart` | 시나리오: 품목 수량 변경 | 4 | `tests/e2e/UIS-ORD-005.spec.ts` |
| TC-FUNC-order-035 | [UIS-ORD-005] 장바구니 | `/cart` | 시나리오: 품목 삭제 | 4 | `tests/e2e/UIS-ORD-005.spec.ts` |
| TC-FUNC-order-036 | [UIS-ORD-005] 장바구니 | `/cart` | 시나리오: 빈 장바구니 → 상품 목록으로 이동 | 3 | `tests/e2e/UIS-ORD-005.spec.ts` |
| TC-FUNC-order-037 | [UIS-ORD-005] 장바구니 | `/cart` | 시나리오: 체크아웃(주문하기) | 4 | `tests/e2e/UIS-ORD-005.spec.ts` |
