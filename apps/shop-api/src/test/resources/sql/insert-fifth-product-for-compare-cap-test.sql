-- SR-360(#2) — 비교함 상한(4개) 경계 테스트 전용 임시 5번째 상품(SKU-TEST-CMP-5TH).
-- 실 카탈로그는 4개(SKU-1001~1004)뿐이라 "5번째 거절"을 검증하려면 카탈로그 밖 5번째 상품이
-- 필요하다(사례집 SR-353 #1 — 판별력 없는 테스트 방지). @Transactional이 자동 롤백하므로 cleanup
-- 불필요(insert-out-of-stock.sql 관례와 동일).
INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, PRICE, STOCK_QTY, SALE_YN)
VALUES ('SKU-TEST-CMP-5TH', 'Test 5th Product For Compare Cap', 10000, 5, 'Y');
