-- AC3(inStock=true) 회귀 테스트용 품절 상품(sale_yn='Y', stock_qty=0)
-- @Transactional이 자동 롤백하므로 cleanup 불필요
INSERT INTO PRODUCTS (SKU, PRODUCT_NAME, PRICE, STOCK_QTY, SALE_YN)
VALUES ('SKU-TEST-OOS', 'Test Out of Stock Product', 50000, 0, 'Y');
