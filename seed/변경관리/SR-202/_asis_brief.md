# AS-IS 브리프 — SR-202

요청 엔티티: products, members, 상품, 상세, 상품, 목록

> 요약 스펙 본문 대신 **소스앵커**를 싣는다. 에이전트는 아래 file:line을 Read하여 최신·정밀 AS-IS를 확보한다.
> 영향 INF 9건 중 관련도 상위 9건. 편재 공통자원은 하단 별도 격리.

## 영향 INF (관련도 순 — 점수·연결경로·소스앵커)
- **INF-ORD-001** `1.00` ((직접))  GET /api/members/  ·  docs/05_설계서/order/INF/INF-ORD-001.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/MemberController.java:21-25`
    - 소스: `src/main/java/com/sm/lab/shop/service/MemberService.java:24-26`
    - 소스: `src/main/java/com/sm/lab/shop/dao/MemberDao.java:11`
    - 소스: `src/main/resources/mapper/member.xml:7-12`
- **INF-ORD-002** `1.00` ((직접))  GET /api/members/{memberId}  ·  docs/05_설계서/order/INF/INF-ORD-002.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/MemberController.java:27-31`
    - 소스: `src/main/java/com/sm/lab/shop/service/MemberService.java:28-36`
    - 소스: `src/main/java/com/sm/lab/shop/dao/MemberDao.java:12`
    - 소스: `src/main/java/com/sm/lab/shop/dao/OrderDao.java:20`
    - 소스: `src/main/resources/mapper/member.xml:14-19`
    - 소스: `src/main/resources/mapper/order.xml:45-52`
- **INF-ORD-008** `1.00` ((직접))  GET /api/products  ·  docs/05_설계서/order/INF/INF-ORD-008.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/ProductController.java:24-31`
    - 소스: `src/main/java/com/sm/lab/shop/service/ProductService.java:19-22`
    - 소스: `src/main/java/com/sm/lab/shop/dao/ProductDao.java:11`
    - 소스: `src/main/resources/mapper/product.xml:7-16`
- **INF-ORD-009** `1.00` ((직접))  GET /api/products/{sku}  ·  docs/05_설계서/order/INF/INF-ORD-009.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/ProductController.java:27-31`
    - 소스: `src/main/java/com/sm/lab/shop/service/ProductService.java:23-29`
    - 소스: `src/main/java/com/sm/lab/shop/dao/ProductDao.java:12`
    - 소스: `src/main/resources/mapper/product.xml:14-18`
- **INF-ORD-004** `0.20` (PRODUCTS)  GET /api/orders/{orderNo}  ·  docs/05_설계서/order/INF/INF-ORD-004.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/OrderController.java:31-34`
    - 소스: `src/main/java/com/sm/lab/shop/service/OrderService.java:38-46`
    - 소스: `src/main/java/com/sm/lab/shop/dao/OrderDao.java:18,21-22`
    - 소스: `src/main/resources/mapper/order.xml:35-41,54-67`
- **INF-ORD-005** `0.20` (PRODUCTS)  POST /api/orders/  ·  docs/05_설계서/order/INF/INF-ORD-005.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/OrderController.java:36-40`
    - 소스: `src/main/java/com/sm/lab/shop/service/OrderService.java:48-77`
    - 소스: `src/main/java/com/sm/lab/shop/dao/ProductDao.java:12-13`
    - 소스: `src/main/java/com/sm/lab/shop/dao/OrderDao.java:18,21-24`
    - 소스: `src/main/resources/mapper/product.xml:14-25`
    - 소스: `src/main/resources/mapper/order.xml:35-41,54-67,69-77`
- **INF-ORD-006** `0.20` (PRODUCTS)  PATCH /api/orders/{orderNo}/cancel  ·  docs/05_설계서/order/INF/INF-ORD-006.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/OrderController.java:42-46`
    - 소스: `src/main/java/com/sm/lab/shop/service/OrderService.java:79-99`
    - 소스: `src/main/java/com/sm/lab/shop/dao/OrderDao.java:25`
    - 소스: `src/main/java/com/sm/lab/shop/dao/ProductDao.java:15`
    - 소스: `src/main/resources/mapper/order.xml:79-86`
    - 소스: `src/main/resources/mapper/product.xml:27-32`
- **INF-ORD-003** `0.17` (MEMBERS)  GET /api/orders/  ·  docs/05_설계서/order/INF/INF-ORD-003.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/OrderController.java:21-28`
    - 소스: `src/main/java/com/sm/lab/shop/service/OrderService.java:30-36`
    - 소스: `src/main/java/com/sm/lab/shop/dao/OrderDao.java:15-17`
    - 소스: `src/main/resources/mapper/order.xml:8-33`
- **INF-ORD-007** `0.17` (MEMBERS)  GET /api/orders/{orderNo}/deliveries  ·  docs/05_설계서/order/INF/INF-ORD-007.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/OrderController.java:48-52`
    - 소스: `src/main/java/com/sm/lab/shop/service/OrderService.java:38-46`
    - 소스: `src/main/java/com/sm/lab/shop/dao/OrderDao.java:18,21-22`
    - 소스: `src/main/resources/mapper/order.xml:35-41,54-67`

## 영향 화면 (UIS)
- **UIS-ORD-001** 주문 목록 `/order/list`  ·  연결 INF: INF-ORD-003  ·  docs/05_설계서/order/UIS/UIS-ORD-001_주문목록/spec.md
    - 소스: `"src/main/resources/templates/order/list.html"`
    - 소스: `"src/main/java/com/sm/lab/shop/controller/OrderViewController.java:20-28"`
    - 소스: `{{SRC_SHOP_API}}/src/main/java/com/sm/lab/shop/controller/OrderViewController.java`
- **UIS-ORD-002** 주문 상세 `/order/{orderNo}`  ·  연결 INF: INF-ORD-004, INF-ORD-006  ·  docs/05_설계서/order/UIS/UIS-ORD-002_주문상세/spec.md
    - 소스: `"src/main/resources/templates/order/detail.html"`
    - 소스: `"src/main/java/com/sm/lab/shop/controller/OrderViewController.java:31-35"`
    - 소스: `"src/main/resources/templates/order/detail.html:56-59"`
    - 소스: `{{SRC_SHOP_API}}/src/main/java/com/sm/lab/shop/controller/OrderViewController.java`
- **UIS-ORD-003** 상품 목록 `/product/list`  ·  연결 INF: INF-ORD-008  ·  docs/05_설계서/order/UIS/UIS-ORD-003_상품목록/spec.md
    - 소스: `"src/main/resources/templates/product/list.html"`
    - 소스: `"src/main/java/com/sm/lab/shop/controller/ProductViewController.java:32-37"`
    - 소스: `{{SRC_SHOP_API}}/src/main/java/com/sm/lab/shop/controller/ProductViewController.java`
- **UIS-ORD-004** 상품 상세 `/product/{sku}`  ·  연결 INF: INF-ORD-009  ·  docs/05_설계서/order/UIS/UIS-ORD-004_상품상세/spec.md
    - 소스: `"src/main/resources/templates/product/detail.html"`
    - 소스: `"src/main/java/com/sm/lab/shop/controller/ProductViewController.java:46-60"`
    - 소스: `"src/main/java/com/sm/lab/shop/service/ProductService.java:26-32"`
    - 소스: `{{SRC_SHOP_API}}/src/main/java/com/sm/lab/shop/controller/ProductViewController.java`

## 영향 SCH
- **SCH-ORD-001** (MEMBERS)  ·  docs/05_설계서/order/SCH/SCH-ORD-001.md
    - 소스: `[미확인]`
- **SCH-ORD-004** (ORDERS)  ·  docs/05_설계서/order/SCH/SCH-ORD-004.md
    - 소스: `[미확인]`
- **SCH-ORD-005** (PRODUCTS)  ·  docs/05_설계서/order/SCH/SCH-ORD-005.md
    - 소스: `[미확인]`

## 영향 테이블
MEMBERS, ORDERS, PRODUCTS
