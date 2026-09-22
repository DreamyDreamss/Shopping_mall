# SR-203 롤백 플랜

## 스펙 복원
- UIS-ORD-005: `docs/변경관리/SR-203/_snapshots/spec.md`로 복원.
- INF-ORD-014: recon 저작본 삭제(채번 ID는 tombstone 규약).

## 코드 복원
- 식별: `linked_func: FUNC-order-012` 주석 + SR-203 커밋. `git revert`(modules/shop-api).
- 예상 범위: CartService.checkout(또는 CheckoutService)·CartController/CartViewController의
  checkout 핸들러·cart/list.html 버튼·관련 테스트.

## 데이터/마이그레이션
- 스키마 무변경. 전환으로 생성된 주문 데이터는 업무 데이터 — 롤백 시에도 **삭제하지 않는다**
  (주문 취소 절차(기존 INF-ORD-006)로 처리 — 운영 판단).

## 검증
1. /cart에 [주문하기] 없음(스냅샷과 동일), /api/cart/checkout 404.
2. 기존 자동화 74건 그린(주문·장바구니 회귀 포함).
