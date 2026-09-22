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

export interface OrderDetail extends OrderRow {
  items: OrderItem[]
  deliveries: { deliveryNo: string; state: Exclude<DeliveryState, null>; shippedAt: string | null }[]
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
