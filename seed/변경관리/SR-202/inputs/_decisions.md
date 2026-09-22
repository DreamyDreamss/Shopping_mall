# SR-202 의사결정 기록 (분석 게이트 3-G)

## D2. 장바구니 데이터 설계 (2026-08-22)
- **질문**: 요구사항이 신규 테이블 설계를 개발팀에 위임 — 설계안 확정 필요.
- **개발팀 제안**: 단일 품목 테이블 `CART_ITEMS`
  (member_id VARCHAR(20)→MEMBERS FK, sku VARCHAR(20)→PRODUCTS FK, qty INT(≥1),
  added_at DATETIME, **PK(member_id, sku)** — 같은 상품 재담기=합산이 PK로 자연 강제).
  헤더 테이블 없음(합계는 조회 시 계산). **기존 테이블 무변경.**
- **결정**: 제안대로 확정. 결정자: 발주자 대행 [자기승인]

## D3. 장바구니 API 계약 (2026-08-22)
- **개발팀 제안** (기존 /api/orders 관례 준수):
  - `POST /api/cart/items` {memberId, sku, qty} → 200 담긴 품목. 같은 상품 재담기 시 수량 합산.
  - `GET /api/cart?memberId=` → {items:[{sku, productName, price, qty, lineTotal}], totalAmount}
  - `PATCH /api/cart/items/{sku}?memberId=` {qty} → 200 변경 품목
  - `DELETE /api/cart/items/{sku}?memberId=` → 204
  - 오류 계약: 400(qty<1·유효성), 404(미존재 회원/상품/품목), 409(재고 초과·품절 — 사유 메시지 포함)
- **결정**: 제안대로 확정. 결정자: 발주자 대행 [자기승인]

## D4. 수량 1 미만 처리 (2026-08-22)
- **질문**: 요구사항 "1 미만 거부 또는 삭제 유도 — 개발팀 판단".
- **결정**: **거부(400)** + 화면에서 "삭제하려면 삭제 버튼을 사용하세요" 안내. 삭제는 명시적
  DELETE로만(실수 삭제 방지). 결정자: 발주자 대행 [자기승인]

## D5. FUNC 구성 스코핑 (2026-08-22, 분석가)
- 장바구니 API 4종은 별도 FUNC로 예약하지 않는다 — **화면 FUNC 2개가 구현 주체**:
  ① FUNC-order-010 **재개**(상품 상세 — 담기 폼+POST 경로) ② 신규 화면 FUNC(장바구니 화면
  — 조회/수량변경/삭제, UIS-ORD-005 예약). INF 4종·SCH(CART_ITEMS)는 **구현 후
  /sl-recon-inf·/sl-recon-sch가 소스에서 채번·역생성**한다(7-N 원칙 — 스펙을 지어내지 않음).
