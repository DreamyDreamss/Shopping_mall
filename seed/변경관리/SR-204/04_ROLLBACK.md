# SR-204 롤백 플랜

## 코드 복원
- linked_func: FUNC-order-013(SR-204 커밋) `git revert` — 필터·설정·상한 제거로 무인증 복귀.

## 스펙 복원
- INF 재생성분은 sl-sync --apply 재실행으로 무인증 계약 복귀(스키마·데이터 무변경).

## 검증
- 무키 /api/orders 200(종전), 전체 스위트 그린.
