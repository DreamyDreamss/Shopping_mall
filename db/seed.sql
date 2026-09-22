-- 실증 랩 표본 데이터 — 앱 화면·API·SCH enrichment(코드값 분포)가 실제로 먹을 데이터.
USE sl_shop;

INSERT INTO MEMBERS (member_id, member_name, grade, phone, del_yn) VALUES
  ('M-0001', '김실증', 'GOLD',   '010-1111-2222', 'N'),
  ('M-0002', '이도그', 'SILVER', '010-3333-4444', 'N'),
  ('M-0003', '박푸딩', 'BRONZE', NULL,            'N'),
  ('M-0004', '탈퇴한회원', 'BRONZE', NULL,         'Y');

INSERT INTO PRODUCTS (sku, product_name, price, stock_qty, sale_yn) VALUES
  ('SKU-1001', '스탠딩 데스크', 390000, 12, 'Y'),
  ('SKU-1002', '기계식 키보드', 129000, 40, 'Y'),
  ('SKU-1003', '4K 모니터',     450000,  7, 'Y'),
  ('SKU-1004', '단종 마우스',    35000,  0, 'N');

INSERT INTO ORDERS (order_no, member_id, order_state, total_amount, del_yn, ordered_at) VALUES
  ('20260815-0001', 'M-0001', 'DONE',            519000, 'N', '2026-08-15 10:12:00'),
  ('20260816-0001', 'M-0002', 'PAID',            390000, 'N', '2026-08-16 14:30:00'),
  ('20260816-0002', 'M-0001', 'PARTIAL_SHIPPED', 579000, 'N', '2026-08-16 16:05:00'),
  ('20260817-0001', 'M-0003', 'PLACED',          129000, 'N', '2026-08-17 09:00:00'),
  ('20260817-0002', 'M-0002', 'CANCELED',        450000, 'N', '2026-08-17 09:40:00');

INSERT INTO ORDER_ITEMS (order_no, line_no, sku, qty, unit_price) VALUES
  ('20260815-0001', 1, 'SKU-1002', 1, 129000),
  ('20260815-0001', 2, 'SKU-1001', 1, 390000),
  ('20260816-0001', 1, 'SKU-1001', 1, 390000),
  ('20260816-0002', 1, 'SKU-1003', 1, 450000),
  ('20260816-0002', 2, 'SKU-1002', 1, 129000),
  ('20260817-0001', 1, 'SKU-1002', 1, 129000),
  ('20260817-0002', 1, 'SKU-1003', 1, 450000);

INSERT INTO ORDER_DELIVERY (delivery_no, order_no, delivery_state, invoice_no, shipped_at) VALUES
  ('D-0815-1', '20260815-0001', 'DELIVERED', 'INV-88010', '2026-08-15 18:00:00'),
  ('D-0816-1', '20260816-0002', 'SHIPPED',   'INV-88031', '2026-08-17 08:20:00'),
  ('D-0816-2', '20260816-0002', 'READY',     NULL,        NULL);
