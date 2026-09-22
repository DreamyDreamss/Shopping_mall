# SR-202 롤백 플랜

## 스펙 복원
- UIS-ORD-004: `docs/변경관리/SR-202/_snapshots/spec.md`(Step 9-0)로 복원.
- UIS-ORD-005·신규 INF(카트 4종)·SCH(CART_ITEMS): recon 저작본 디렉토리/파일 삭제
  (예약 ID는 tombstone 규약 — 재사용 안 함).

## 코드 복원
- 식별: `linked_func: FUNC-order-010`(r2 이후 담기 폼분)·`FUNC-order-011` 주석 + 커밋 메시지
  SR-202 태그. `git revert` (modules/shop-api).
- 예상 범위: CartController/CartService/CartDao/cart mapper·템플릿(cart), ProductViewController·
  detail.html(담기 폼), 관련 테스트.

## 데이터/마이그레이션
- 역변경: `DROP TABLE CART_ITEMS` (신규 테이블 — 다른 데이터 무영향).
  담긴 장바구니 데이터는 소실됨(보관 데이터 — 발주자 고지 후 실행).

## 검증 (롤백 완료 판정)
1. /cart·/api/cart* 404, 상세 화면에 담기 폼 없음(스냅샷과 동일).
2. 기존 자동화 세트(회귀 포함) 전체 그린.
3. SHOW TABLES에 CART_ITEMS 없음.
