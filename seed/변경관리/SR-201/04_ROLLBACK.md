# SR-201 롤백 플랜

## 스펙 복원
- 기준 스냅샷: `docs/변경관리/SR-201/_snapshots/INF-ORD-008.md` (Step 9-0, 2026-08-22).
- 복원 대상: `docs/05_설계서/order/INF/INF-ORD-008.md` ← 위 스냅샷 덮어쓰기.
- UIS-ORD-003/004: 구현 후 recon이 저작한 스펙 디렉토리
  (`docs/05_설계서/order/UIS/UIS-ORD-003_*`, `UIS-ORD-004_*`)를 삭제.
  (채번 ID는 tombstone 규약상 재사용하지 않음 — func_registry가 관리.)

## 코드 복원
- 식별: `linked_func` 주석의 SR-201 유래 FUNC-ID(FUNC_MAP 재인덱싱 후 확정) 및
  커밋 메시지 `SR-201` 태그가 붙은 커밋 범위.
- 방법: 해당 커밋들을 `git revert` (shop-api 모듈 저장소 — .lab-ws/modules/shop-api).
  변경 파일 예상 범위: ProductController/ProductService/ProductDao/product.xml(검색 파라미터),
  신규 화면 컨트롤러·템플릿(product 목록/상세), 관련 테스트.

## 데이터/마이그레이션
- **데이터 변경 없음** — 스키마·시드 무변경이므로 역마이그레이션 불필요.

## 검증 (롤백 완료 판정)
1. `GET /api/products/` 응답이 스냅샷 시점과 동일(전체 판매중 목록·sku 정렬).
2. `/product/list` 접근 시 404 (화면 제거 확인).
3. 회귀 TC-FUNC-order-007-05~07 재실행 통과 (주문 생성·취소·상세 정상).
