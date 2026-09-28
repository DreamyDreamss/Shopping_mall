// linked_func: FUNC-order-001 — 주문 목록 / FUNC-order-002 — 주문 상세
// shop-api(INF-ORD-003 · INF-ORD-004)의 응답 계약을 그대로 옮긴 타입.

/** 배송 이력 상태 — ORDER_DELIVERY.state. 이력이 없으면 화면은 "-"로 표시한다(UIS-ORD-001 §5). */
export type DeliveryState = 'READY' | 'SHIPPED' | 'DELIVERED' | null

export interface OrderRow {
  orderNo: string
  memberId: string
  memberName: string | null
  orderState: string
  totalAmount: number
  orderedAt: string
  /** SR-210으로 추가된 6번째 열 — 최신 배송 이력 상태(delivery_no DESC 기준) */
  deliveryState: DeliveryState
}

export interface OrderItem {
  productNo: string
  productName: string
  quantity: number
  unitPrice: number
}

// LAB-105.1 — 주문 상세(INF-ORD-004) 배송 정보(택배사·송장). `deliveries[]`(배송 이력)와는 별도
// 파생 배열이다 — 값이 없으면(미배정) null(빈 문자열 아님, 서버 `OrderShipment` 관례 그대로).
export interface OrderShipment {
  deliveryNo: string
  carrierCode: string | null
  carrierName: string | null
  invoiceNo: string | null
  shippedAt: string | null
}

export interface OrderDetail extends OrderRow {
  items: OrderItem[]
  deliveries: { deliveryNo: string; state: Exclude<DeliveryState, null>; shippedAt: string | null }[]
  // 필수 필드(LAB-105.1 round2 — round1 QA FAIL 필수수정 1). 계약상 항상 채워진다. 다만 `fetchOrder`가
  // 서버 JSON을 검증 없이 `as OrderDetail`로 캐스팅할 뿐이라, 실제 응답에 이 키가 없으면(구버전 응답
  // 등) 자바스크립트 런타임 값은 여전히 `undefined`일 수 있다 — 그 계약 위반을 `OrderShipmentInfo`가
  // 방어적으로 감지해 "배송 정보를 불러오지 못했습니다"로 표시한다(AC 68). `[]`(정상 응답, 출고 전 —
  // "출고 준비 중")와는 의미가 다르므로 `??`로 뭉개지 않는다(`OrderDetailCard`도 그대로 넘긴다).
  shipments: OrderShipment[]
}

export interface OrderQuery {
  memberId: string
  orderState: string
  startDate: string
  endDate: string
}

export const EMPTY_QUERY: OrderQuery = { memberId: '', orderState: '', startDate: '', endDate: '' }

/** 주문 상태 코드 — ORDERS.order_state. 값 목록은 SCH-ORD-004 코드값 절이 정본이다. */
export const ORDER_STATES = ['PLACED', 'PAID', 'SHIPPED', 'PARTIAL_SHIPPED', 'CANCELED', 'DONE'] as const   // 실측: /order/list 셀렉트·SCH-ORD-004

// linked_func: FUNC-member-004
// spec: docs/05_설계서/member/INF/INF-MBR-003.md · INF-MBR-005.md
/**
 * 로그인(INF-MBR-003)·리프레시(INF-MBR-005) 응답 — 두 API가 완전히 동일한 필드명·타입·순서로
 * 반환하는 `SessionResult` 계약을 그대로 옮긴 타입(하위호환 유지, 재해석 금지).
 */
export interface SessionResult {
  memberId: string
  memberName: string
  grade: string
  apiKey: string
  refreshToken: string
  refreshTokenExpiresAt: string
}

/** 오류 응답 봉투 — `{code, message}` 공통, 429(MBR-4291)만 retryAfterSeconds 추가(INF-MBR-003). */
export interface ApiErrorBody {
  code: string
  message: string
  retryAfterSeconds?: number
}

// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-006.md
/**
 * 비밀번호 재설정 코드 요청(INF-MBR-006) 응답 — `MemberPasswordResetService.VerificationCodeResult`
 * record(실측: `MemberPasswordResetService.java:218`)와 필드명·순서 일치.
 */
export interface VerificationCodeResult {
  channel: 'EMAIL' | 'SMS'
  target: string
  expiresInSeconds: number
}

// SR-302 — 쇼핑 홈(메인). `GET /api/products`(`ProductController.list`) 응답 그대로(하위호환 유지,
// 요청·응답 형식 변경 없음이 확정 답변). SR-306(#2)로 `PRODUCTS`에 list_price(정가)·image_url(대표
// 이미지) 컬럼이 추가됐다(실측, `Product.java`) — 기존 5필드는 이름·순서·타입 불변, 새 필드는 끝에
// 추가(표시 전용, 값 없으면 null — 주문·장바구니 금액 계산은 price만 쓴다).
export interface Product {
  sku: string
  productName: string
  price: number
  stockQty: number
  saleYn: string
  listPrice: number | null
  imageUrl: string | null
}

// SR-353 — 상품 상세(`/shop/products/:sku`) 전용 응답 타입. `GET /api/products/{sku}`(INF-ORD-009)에
// `soldOut`(boolean) 필드가 추가되고, `stockQty`는 "읽지 못하는 경우"에 한해 `null`을 허용하도록 타입이
// 열렸다(값 자체는 재고 있는 상품과 동일한 number, 확정 답변 api_error). 기존 `Product`(목록·카드·장바구니
// 등 다수 소비처 공유)는 건드리지 않고 상세 화면 전용으로 별도 선언한다(`MemberAddress`/`CartRow`와 같은
// "이름 겹침 방지, 별도 타입" 관례) — 기존 7필드는 이름·타입·순서 불변, `soldOut`은 끝에 추가.
export interface ProductDetail {
  sku: string
  productName: string
  price: number
  stockQty: number | null
  saleYn: string
  listPrice: number | null
  imageUrl: string | null
  soldOut: boolean
  // SR-356 — 상품 찜하기(위시리스트). `GET /api/products/{sku}`(INF-ORD-009)에 끝에 추가(하위호환,
  // 위 8필드 이름·순서·타입 불변, SR-356 #6 구현).
  wishCount: number
  // SR-358 — 상품 상세 조회수. `GET /api/products/{sku}`(INF-ORD-009)에 끝에 추가(하위호환, 위
  // 9필드 이름·순서·타입 불변). optional인 이유는 응답 부재가 아니라 이 타입을 공유하는 형제 테스트
  // 픽스처(ProductDetailPage.reviews.test.tsx · ProductDetailPage.wish.test.tsx)가 이 필드를
  // 채우지 않기 때문 — 화면은 `?? 0`으로 항상 「조회 0」 이상을 보장한다.
  viewCount?: number
}

// SR-356 — 내 찜 목록(`/shop/mypage/wishes`). `GET /api/members/me/wishes`(INF-ORD-042, SR-356 #2 구현)
// 응답 items[] 1건. wishedAt은 타입엔 있으나 화면에 렌더하지 않는다(STORY "범위 밖" 절).
export interface WishItem {
  sku: string
  productName: string
  price: number
  imageUrl: string | null
  soldOut: boolean
  wishedAt: string
}

// SR-357 — 최근 본 상품(회원별 서버 저장, `/shop/mypage/recently-viewed`). `GET
// /api/members/me/recently-viewed`(INF-ORD-044, SR-357 #2 구현) 응답 items[] 1건 — 실측
// (`ProductViewHistoryService.list`) 필드 그대로: sku·productName·price·imageUrl·soldOut·viewedAt.
export interface RecentlyViewedItem {
  sku: string
  productName: string
  price: number
  imageUrl: string | null
  soldOut: boolean
  viewedAt: string
}

// SR-304 — 상품 상세(`/shop/products/:sku`). `POST /api/cart/items` 응답(`CartController.addItem` →
// `CartItem` 도메인)에는 memberId·productName·price·addedAt 필드도 더 있지만, 화면이 실제로 쓰는
// 필드(담기 수량 반영용 qty, sku)만 선언한다 — 필요 최소만 옮긴다(하위호환 유지, 응답 형식은 그대로).
export interface CartItem {
  sku: string
  qty: number
  lineTotal: number
}

// SR-305 — 장바구니·주문서(`/shop/cart`, `/shop/order`). 신규 API 없음(확정 답변) — 기존
// `GET /api/cart`(`CartController.get` → `CartService.get`)의 `items[]` 원소(`CartItem` 도메인의
// getter 전부가 JSON 직렬화됨) 중 화면이 실제로 쓰는 필드만 선언한다(`CartItem`과 이름이 겹쳐
// 별도 타입으로 둔다 — 기존 `CartItem`은 addCartItem 반환용이라 건드리지 않는다, STORY "파일" 절).

/** GET /api/cart 응답 items[] 1건. */
export interface CartRow {
  sku: string
  productName: string
  price: number
  qty: number
  lineTotal: number
}

/** POST /api/cart/checkout 응답(`CartService.checkout`) — `{orderNo, totalAmount, itemCount}`. */
export interface CheckoutResult {
  orderNo: string
  totalAmount: number
  itemCount: number
}

/** GET /api/zipcodes 응답 items[] 1건(`Zipcode` 도메인, INF-MBR-009). */
export interface ZipcodeResult {
  zipcode: string
  roadAddress: string
  sido: string
  sigungu: string
}

// SR-235 — 배송지 관리(`/shop/mypage/addresses`). `GET/POST/PUT/DELETE /api/members/me/addresses*`
// (INF-MBR-008) 응답 셰이프 그대로(하위호환 유지, 필드명·타입 재해석 금지).

/** 배송지 — `MEMBER_ADDRESSES` 1행(INF-MBR-008 응답 셰이프). */
export interface MemberAddress {
  addressId: number
  memberId: string
  recipient: string
  phone: string
  phoneNorm: string
  zipcode: string
  roadAddress: string
  detailAddress: string
  entranceMethod: string | null
  deliveryMemo: string | null
  /** Y/N 플래그(`Product.saleYn`과 동일 관례) — boolean으로 바꾸지 않는다. */
  isDefault: 'Y' | 'N'
  lastUsedAt: string
  createdAt: string
  updatedAt: string
}

/** 배송지 등록/수정 요청 바디(INF-MBR-008). 수정 시 서버가 `isDefault`를 무시한다(별도 기본설정 API). */
export interface MemberAddressInput {
  recipient: string
  phone: string
  zipcode: string
  roadAddress: string
  detailAddress: string
  entranceMethod?: string
  deliveryMemo?: string
  isDefault?: boolean
}

// SR-351 — 상품평(리뷰·별점, `/shop/products/:sku` 상품평 탭). `GET/POST/DELETE
// /api/products/{sku}/reviews*`(INF-ORD-037/038/039, SR-351 #1·#2에서 이미 구현·Done)의 응답 계약을
// 그대로 옮긴 타입(이 항목이 계약을 정의하지 않음, STORY "계약" 절 그대로).

/** 정렬 — 최신순/별점높은순/별점낮은순. */
export type ReviewSort = 'latest' | 'ratingDesc' | 'ratingAsc'

/** 목록 응답 items[] 1건 — authorName은 서버가 이미 마스킹한 값(원본 이름은 API로 나가지 않는다). */
export interface ReviewItem {
  reviewId: number
  rating: number
  content: string
  authorName: string
  createdAt: string
}

/** GET /api/products/{sku}/reviews 응답. avgRating은 리뷰 0건이면 null(화면은 이때 '-'로 표시,
 * 0.0 표기 금지). */
export interface ReviewListResult {
  totalCount: number
  page: number
  size: number
  avgRating: number | null
  reviewCount: number
  distribution: Record<'1' | '2' | '3' | '4' | '5', number>
  items: ReviewItem[]
}

/** POST /api/products/{sku}/reviews 요청 바디. memberId는 싣지 않는다(`ApiKeyAuthFilter`의
 * SCOPE_TO_SELF가 회원 X-Api-Key에서 강제한다). */
export interface ReviewInput {
  rating: number
  content: string
  orderNo: string
}

/** POST /api/products/{sku}/reviews 성공(201) 응답. */
export interface ReviewCreateResult {
  reviewId: number
  sku: string
  orderNo: string
  rating: number
  content: string
  createdAt: string
}
