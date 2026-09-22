# AS-IS 브리프 — SR-268

요청 엔티티: 장바구니

> 요약 스펙 본문 대신 **소스앵커**를 싣는다. 에이전트는 아래 file:line을 Read하여 최신·정밀 AS-IS를 확보한다.
> 영향 INF 15건 중 관련도 상위 15건. 편재 공통자원은 하단 별도 격리.

## 영향 INF (관련도 순 — 점수·연결경로·소스앵커)
- **INF-ORD-010** `0.90` (화면 UIS-ORD-005)  POST /api/cart/items  ·  docs/05_설계서/order/INF/INF-ORD-010.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/CartController.java:22-26`
    - 소스: `src/main/java/com/sm/lab/shop/service/CartService.java:43-64`
    - 소스: `src/main/java/com/sm/lab/shop/dao/CartDao.java:13-14`
    - 소스: `src/main/java/com/sm/lab/shop/dao/CartDao.java:21-26`
    - 소스: `src/main/java/com/sm/lab/shop/dao/MemberDao.java:12`
    - 소스: `src/main/java/com/sm/lab/shop/dao/ProductDao.java:15`
    - 소스: `src/main/resources/mapper/cart.xml:6-13`
    - 소스: `src/main/resources/mapper/cart.xml:29-36`
    - 소스: `src/main/resources/mapper/member.xml:14-19`
    - 소스: `src/main/resources/mapper/product.xml:18-22`
- **INF-ORD-011** `0.90` (화면 UIS-ORD-005)  GET /api/cart/  ·  docs/05_설계서/order/INF/INF-ORD-011.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/CartController.java:28-32`
    - 소스: `src/main/java/com/sm/lab/shop/service/CartService.java:66-72`
    - 소스: `src/main/java/com/sm/lab/shop/dao/CartDao.java:16-17`
    - 소스: `src/main/java/com/sm/lab/shop/dao/MemberDao.java:12`
    - 소스: `src/main/resources/mapper/cart.xml:15-22`
    - 소스: `src/main/resources/mapper/member.xml:14-19`
- **INF-ORD-012** `0.90` (화면 UIS-ORD-005)  PATCH /api/cart/items/{sku}  ·  docs/05_설계서/order/INF/INF-ORD-012.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/CartController.java:34-39`
    - 소스: `src/main/java/com/sm/lab/shop/service/CartService.java:74-84`
    - 소스: `src/main/java/com/sm/lab/shop/dao/CartDao.java:13-14`
    - 소스: `src/main/java/com/sm/lab/shop/dao/CartDao.java:28`
    - 소스: `src/main/java/com/sm/lab/shop/dao/ProductDao.java:15`
    - 소스: `src/main/resources/mapper/cart.xml:6-13`
    - 소스: `src/main/resources/mapper/cart.xml:38-44`
    - 소스: `src/main/resources/mapper/product.xml:18-22`
- **INF-ORD-013** `0.90` (화면 UIS-ORD-005)  DELETE /api/cart/items/{sku}  ·  docs/05_설계서/order/INF/INF-ORD-013.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/CartController.java:41-46`
    - 소스: `src/main/java/com/sm/lab/shop/service/CartService.java:86-91`
    - 소스: `src/main/java/com/sm/lab/shop/dao/CartDao.java:13-14`
    - 소스: `src/main/java/com/sm/lab/shop/dao/CartDao.java:30`
    - 소스: `src/main/resources/mapper/cart.xml:6-13`
    - 소스: `src/main/resources/mapper/cart.xml:46-50`
- **INF-ORD-014** `0.90` (화면 UIS-ORD-005)  POST /api/cart/checkout  ·  docs/05_설계서/order/INF/INF-ORD-014.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/CartController.java:53-56`
    - 소스: `src/main/java/com/sm/lab/shop/service/CartService.java:130-158`
    - 소스: `src/main/java/com/sm/lab/shop/service/CartService.java:166-179`
    - 소스: `src/main/java/com/sm/lab/shop/service/OrderService.java:137-166`
    - 소스: `src/main/java/com/sm/lab/shop/dao/CartDao.java:32-40`
    - 소스: `src/main/java/com/sm/lab/shop/dao/MemberDao.java:12`
    - 소스: `src/main/java/com/sm/lab/shop/dao/ProductDao.java:15-16`
    - 소스: `src/main/java/com/sm/lab/shop/dao/OrderDao.java:36-37`
    - 소스: `src/main/resources/mapper/cart.xml:54-71`
    - 소스: `src/main/resources/mapper/member.xml:14-19`
    - 소스: `src/main/resources/mapper/product.xml:18-29`
    - 소스: `src/main/resources/mapper/order.xml:90-98`
- **INF-ORD-004** `0.20` (ORDERS)  GET /api/orders/{orderNo}  ·  docs/05_설계서/order/INF/INF-ORD-004.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/OrderController.java:46-50`
    - 소스: `src/main/java/com/sm/lab/shop/service/OrderService.java:127-135`
    - 소스: `src/main/java/com/sm/lab/shop/dao/OrderDao.java:30,33,35`
    - 소스: `src/main/resources/mapper/order.xml:54-60,73-79,83-88`
- **INF-ORD-005** `0.20` (ORDERS)  POST /api/orders  ·  docs/05_설계서/order/INF/INF-ORD-005.md
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/controller/OrderController.java:52-56`
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java:137-166`
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/dao/ProductDao.java:15-16`
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/dao/OrderDao.java:30,33,35-37`
    - 소스: `modules/shop-api/src/main/resources/mapper/product.xml:18-29`
    - 소스: `modules/shop-api/src/main/resources/mapper/order.xml:54-60,73-98`
- **INF-ORD-006** `0.20` (ORDERS)  PATCH /api/orders/{orderNo}/cancel  ·  docs/05_설계서/order/INF/INF-ORD-006.md
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/controller/OrderController.java:59-61`
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java:169-188`
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/dao/OrderDao.java:30-38`
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/dao/ProductDao.java:15-18`
    - 소스: `modules/shop-api/src/main/resources/mapper/order.xml:54-60,73-79,83-88,100-107`
    - 소스: `modules/shop-api/src/main/resources/mapper/product.xml:32-36`
- **INF-ORD-007** `0.20` (ORDERS)  GET /api/orders/{orderNo}/deliveries  ·  docs/05_설계서/order/INF/INF-ORD-007.md
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/controller/OrderController.java:64-68`
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java:127-135`
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/dao/OrderDao.java:30,33,34-35`
    - 소스: `modules/shop-api/src/main/resources/mapper/order.xml:54-60,73-79,81-88`
- **INF-ORD-002** `0.12` (ORDERS)  GET /api/members/{memberId}  ·  docs/05_설계서/order/INF/INF-ORD-002.md
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberController.java:27-31`
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberService.java:28-36`
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberDao.java:12`
    - 소스: `modules/shop-api/src/main/java/com/sm/lab/shop/dao/OrderDao.java:32`
    - 소스: `modules/shop-api/src/main/resources/mapper/member.xml:14-19`
    - 소스: `modules/shop-api/src/main/resources/mapper/order.xml:62-71`
- **INF-ORD-003** `0.12` (ORDERS)  GET /api/orders  ·  docs/05_설계서/order/INF/INF-ORD-003.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/OrderController.java:28-44`
    - 소스: `src/main/java/com/sm/lab/shop/service/OrderService.java:58-72`
    - 소스: `src/main/java/com/sm/lab/shop/dao/OrderDao.java:25-29`
    - 소스: `src/main/resources/mapper/order.xml:15-52`
- **INF-ORD-015** `0.12` (ORDERS)  GET /api/orders/export  ·  docs/05_설계서/order/INF/INF-ORD-015.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/OrderController.java:79-90`
    - 소스: `src/main/java/com/sm/lab/shop/service/OrderService.java:77-104`
    - 소스: `src/main/java/com/sm/lab/shop/dao/OrderDao.java:25-27`
    - 소스: `src/main/resources/mapper/order.xml:15-34`
    - 소스: `src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:180-183`
    - 소스: `src/main/java/com/sm/lab/shop/web/OrderApiExceptionHandler.java:24-30`
- **INF-ORD-001** `0.09` (MEMBERS)  GET /api/members/  ·  docs/05_설계서/order/INF/INF-ORD-001.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/MemberController.java:21-25`
    - 소스: `src/main/java/com/sm/lab/shop/service/MemberService.java:24-26`
    - 소스: `src/main/java/com/sm/lab/shop/dao/MemberDao.java:11`
    - 소스: `src/main/resources/mapper/member.xml:7-12`
- **INF-ORD-008** `0.09` (PRODUCTS)  GET /api/products  ·  docs/05_설계서/order/INF/INF-ORD-008.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/ProductController.java:24-31`
    - 소스: `src/main/java/com/sm/lab/shop/service/ProductService.java:19-22`
    - 소스: `src/main/java/com/sm/lab/shop/dao/ProductDao.java:11`
    - 소스: `src/main/resources/mapper/product.xml:7-16`
- **INF-ORD-009** `0.09` (PRODUCTS)  GET /api/products/{sku}  ·  docs/05_설계서/order/INF/INF-ORD-009.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/ProductController.java:27-31`
    - 소스: `src/main/java/com/sm/lab/shop/service/ProductService.java:23-29`
    - 소스: `src/main/java/com/sm/lab/shop/dao/ProductDao.java:12`
    - 소스: `src/main/resources/mapper/product.xml:14-18`

## 영향 화면 (UIS)
- **UIS-ORD-001** None `/order/list`  ·  연결 INF: INF-ORD-003  ·  docs/05_설계서/order/UIS/UIS-ORD-001_주문목록/spec.md
    - 소스: `"src/main/resources/templates/order/list.html"`
    - 소스: `"src/main/java/com/sm/lab/shop/controller/OrderViewController.java:47-76"`
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
- **UIS-ORD-004** 상품 상세 `/product/{sku}`  ·  연결 INF: INF-ORD-009, INF-ORD-010  ·  docs/05_설계서/order/UIS/UIS-ORD-004_상품상세/spec.md
    - 소스: `"src/main/resources/templates/product/detail.html"`
    - 소스: `"src/main/java/com/sm/lab/shop/controller/ProductViewController.java:56-60"`
    - 소스: `"src/main/java/com/sm/lab/shop/controller/ProductViewController.java:74-96"`
    - 소스: `"src/main/java/com/sm/lab/shop/controller/ProductViewController.java:102-118"`
    - 소스: `"src/main/java/com/sm/lab/shop/service/ProductService.java:26-32"`
    - 소스: `"src/main/java/com/sm/lab/shop/service/CartService.java:44-64"`
    - 소스: `{{SRC_SHOP_API}}/src/main/java/com/sm/lab/shop/controller/ProductViewController.java`
- **UIS-ORD-005** 장바구니 `/cart`  ·  연결 INF: INF-ORD-010, INF-ORD-011, INF-ORD-012, INF-ORD-013, INF-ORD-014  ·  docs/05_설계서/order/UIS/UIS-ORD-005_장바구니/spec.md
    - 소스: `"src/main/resources/templates/cart/list.html"`
    - 소스: `"src/main/java/com/sm/lab/shop/controller/CartViewController.java:40-53"`
    - 소스: `"src/main/java/com/sm/lab/shop/controller/CartViewController.java:56-60"`
    - 소스: `"src/main/java/com/sm/lab/shop/controller/CartViewController.java:63-67"`
    - 소스: `"src/main/java/com/sm/lab/shop/controller/CartViewController.java:78-91"`
    - 소스: `"src/main/java/com/sm/lab/shop/service/CartService.java:130-179"`
    - 소스: `{{SRC_SHOP_API}}/src/main/java/com/sm/lab/shop/controller/CartViewController.java`

## 영향 SCH
- **SCH-ORD-001** (MEMBERS)  ·  docs/05_설계서/order/SCH/SCH-ORD-001.md
    - 소스: `DB 실측(db-main MCP describe)`
- **SCH-ORD-003** (ORDER_ITEMS)  ·  docs/05_설계서/order/SCH/SCH-ORD-003.md
    - 소스: `DB 실측(db-main MCP describe)`
- **SCH-ORD-004** (ORDERS)  ·  docs/05_설계서/order/SCH/SCH-ORD-004.md
    - 소스: `DB 실측(db-main MCP describe)`
- **SCH-ORD-005** (PRODUCTS)  ·  docs/05_설계서/order/SCH/SCH-ORD-005.md
    - 소스: `DB 실측(db-main MCP describe)`
- **SCH-ORD-006** (CART_ITEMS)  ·  docs/05_설계서/order/SCH/SCH-ORD-006.md
    - 소스: `src/main/java/com/sm/lab/shop/controller/CartController.java`
    - 소스: `src/main/resources/mapper/cart.xml`

## 영향 테이블
CART_ITEMS, MEMBERS, ORDERS, ORDER_ITEMS, PRODUCTS

## ⚠️ 현행성 경고 (소스가 스펙보다 최신 — 스펙 stale, 재RECON 권장)
> 아래 INF는 근거소스가 스펙 생성 이후 변경되었다. 그래프/스펙이 현행과 어긋날 수 있으니 소스를 1차 진실로 본다.
- **INF-ORD-002** — 소스 `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberController.java` 가 스펙보다 최신
- **INF-ORD-002** — 소스 `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberService.java` 가 스펙보다 최신
- **INF-ORD-002** — 소스 `modules/shop-api/src/main/java/com/sm/lab/shop/dao/OrderDao.java` 가 스펙보다 최신
- **INF-ORD-002** — 소스 `modules/shop-api/src/main/resources/mapper/order.xml` 가 스펙보다 최신
- **INF-ORD-005** — 소스 `modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java` 가 스펙보다 최신
- **INF-ORD-005** — 소스 `modules/shop-api/src/main/java/com/sm/lab/shop/dao/ProductDao.java` 가 스펙보다 최신
- **INF-ORD-005** — 소스 `modules/shop-api/src/main/resources/mapper/product.xml` 가 스펙보다 최신
- **INF-ORD-006** — 소스 `modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java` 가 스펙보다 최신
- **INF-ORD-006** — 소스 `modules/shop-api/src/main/java/com/sm/lab/shop/dao/OrderDao.java` 가 스펙보다 최신
- **INF-ORD-006** — 소스 `modules/shop-api/src/main/java/com/sm/lab/shop/dao/ProductDao.java` 가 스펙보다 최신
- **INF-ORD-006** — 소스 `modules/shop-api/src/main/resources/mapper/product.xml` 가 스펙보다 최신
- **INF-ORD-007** — 소스 `modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java` 가 스펙보다 최신
