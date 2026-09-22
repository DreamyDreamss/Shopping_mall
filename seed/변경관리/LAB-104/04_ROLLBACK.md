# LAB-104 롤백 플랜

## 스펙 복원
- INF-ORD-015: recon 저작본 삭제(채번 ID는 tombstone 규약).

## 코드 복원
- 식별: linked_func 주석 + LAB-104 커밋 → `git revert`(modules/shop-api).
- 범위: OrderController export 핸들러·관련 테스트(스키마/DAO 무변경이라 데이터 이행 없음).

## 검증
1. /api/orders/export 404, 기존 목록/상세/취소 그린(전체 스위트).
