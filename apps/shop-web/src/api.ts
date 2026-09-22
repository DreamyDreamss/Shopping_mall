// linked_func: FUNC-order-001 / FUNC-order-002 — shop-api 호출부
// linked_func: FUNC-member-004 — 로그인/리프레시 호출부 추가(post 헬퍼·ApiError)
// linked_func: FUNC-member-007 — 비밀번호 재설정 호출부 추가(parseErrorBody 분리·postVoid 신설)
// INF-ORD-003(GET /api/orders) · INF-ORD-004(GET /api/orders/{orderNo})
// spec: docs/05_설계서/member/INF/INF-MBR-003.md · INF-MBR-005.md · INF-MBR-006.md · INF-MBR-007.md
import type {
  ApiErrorBody,
  CartItem,
  CartRow,
  CheckoutResult,
  MemberAddress,
  MemberAddressInput,
  OrderDetail,
  OrderQuery,
  OrderRow,
  Product,
  SessionResult,
  VerificationCodeResult,
  ZipcodeResult,
} from './types'

async function get<T>(url: string): Promise<T> {
  const r = await fetch(url, { headers: { Accept: 'application/json' } })
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}`)
  return (await r.json()) as T
}

/** 서버가 `{code, message}`(+429의 retryAfterSeconds)로 낸 오류를 그대로 옮겨 담는다(재해석 금지). */
export class ApiError extends Error {
  status: number
  code: string
  retryAfterSeconds?: number
  constructor(status: number, body: ApiErrorBody) {
    super(body.message)
    this.status = status
    this.code = body.code
    this.retryAfterSeconds = body.retryAfterSeconds
  }
}

/** 오류 응답 바디 파싱 — 바디가 없거나 JSON이 아니면 500 대체 바디로 앉힌다(post/postVoid 공유). */
async function parseErrorBody(r: Response): Promise<ApiErrorBody> {
  try { return (await r.json()) as ApiErrorBody }
  catch { return { code: 'MBR-5000', message: `${r.status} ${r.statusText}` } }
}

async function post<T>(url: string, body: unknown): Promise<T> {
  const r = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(body),
  })
  if (!r.ok) throw new ApiError(r.status, await parseErrorBody(r))
  return (await r.json()) as T
}

/**
 * 성공 시 바디를 읽지 않는 POST — 204(No Content) 응답(INF-MBR-007 확정 API)에 `post`를 그대로 쓰면
 * `r.json()`이 빈 바디 파싱 예외를 던진다. 오류 시 파싱 로직은 `post`와 동일(`parseErrorBody` 공유).
 */
async function postVoid(url: string, body: unknown): Promise<void> {
  const r = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(body),
  })
  if (!r.ok) throw new ApiError(r.status, await parseErrorBody(r))
}

/** INF-MBR-003 — 이메일+비밀번호 로그인. 잠금·존재 판정은 서버가 하고 이 함수는 그대로 옮긴다. */
export function login(email: string, password: string): Promise<SessionResult> {
  return post<SessionResult>('/api/members/login', { email, password })
}

/** INF-MBR-005 — refreshToken으로 세션 롤링 갱신(30일 자동 로그인의 실제 갱신 지점은 App.tsx). */
export function refreshSession(refreshToken: string): Promise<SessionResult> {
  return post<SessionResult>('/api/members/sessions/refresh', { refreshToken })
}

export function fetchOrders(q: OrderQuery): Promise<OrderRow[]> {
  const p = new URLSearchParams()
  if (q.memberId) p.set('memberId', q.memberId)
  if (q.orderState) p.set('orderState', q.orderState)
  if (q.startDate) p.set('startDate', q.startDate)
  if (q.endDate) p.set('endDate', q.endDate)
  const qs = p.toString()
  // INF-ORD-003 응답은 `{ totalCount, page, items[] }` 봉투다(실측). 배송상태는 목록 응답에 없고 deliveries가
  // null이라, 화면 계약(UIS-ORD-001 §5: 이력 없으면 "-")대로 최신 이력에서 고르되 없으면 null.
  return get<{ items?: OrderRow[] } | OrderRow[]>('/api/orders' + (qs ? '?' + qs : ''))
    .then(r => (Array.isArray(r) ? r : (r.items ?? [])).map(withDeliveryState))
}

function withDeliveryState<T extends OrderRow>(o: T & { deliveries?: { state: string }[] | null }): T {
  const last = o.deliveries?.length ? o.deliveries[o.deliveries.length - 1].state : null
  return { ...o, deliveryState: (o.deliveryState ?? last ?? null) as OrderRow['deliveryState'] }
}

export function fetchOrder(orderNo: string): Promise<OrderDetail> {
  return get<OrderDetail>('/api/orders/' + encodeURIComponent(orderNo)).then(withDeliveryState)
}

// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-006.md · INF-MBR-007.md

/**
 * INF-MBR-006 — 비밀번호 재설정 코드 요청. 형식 오류(400 MBR-4100) 외엔 항상 202(존재 오라클 방지,
 * STORY "순서·보안" 절) — 화면은 이 응답을 "코드가 발송됐다"로만 표시한다.
 */
export function requestPasswordResetCode(target: string): Promise<VerificationCodeResult> {
  return post<VerificationCodeResult>('/api/members/password-resets/codes', { target })
}

/**
 * INF-MBR-007 — 코드 확인+새 비밀번호 반영을 한 번에 원자 처리(204). 코드 확인은 별도 API가 아니다 —
 * 2단계(코드) 화면은 로컬 형식 검증만 하고, 실제 코드 검증은 이 호출 안에서 일어난다(계약 메모).
 * 성공 시 그 회원의 모든 기기가 로그아웃된다(서버 사이드이펙트, 프론트는 관여하지 않음).
 */
export function confirmPasswordReset(req: {
  target: string
  code: string
  newPassword: string
}): Promise<void> {
  return postVoid('/api/members/password-resets/confirmations', req)
}

// SR-302 — 쇼핑 홈(메인). 신규 API 없음 — 기존 상품/장바구니 계약을 그대로 소비한다(확정 답변 api_compat).

/**
 * INF-ORD-008 — 판매중 상품 목록(`GET /api/products`). `keyword`는 부분일치 검색, `inStock`은 `true`일
 * 때만 쿼리에 실어 재고 있는 상품만 받는다(미지정 시 서버 기본값 그대로 — 품절 포함, 하위호환).
 * `ShopHomePage`는 여전히 무인자로 호출해(품절 배지·흐림 처리를 보여줘야 해서) 이 확장으로 그 호출부
 * 동작은 바뀌지 않는다(SR-303, 확정 답변 api_compat). `ProductController.list`는 `List<Product>`를
 * 직접 반환해 실측상 raw 배열이지만, 목록은 `{items}` 봉투라는 일반 서술과 실제 코드가 달라
 * `fetchOrders`처럼 양쪽을 방어적으로 받는다.
 */
export function fetchProducts(keyword?: string, inStock?: boolean): Promise<Product[]> {
  const p = new URLSearchParams()
  if (keyword) p.set('keyword', keyword)
  if (inStock) p.set('inStock', 'true')
  const qs = p.toString()
  return get<{ items?: Product[] } | Product[]>('/api/products' + (qs ? '?' + qs : ''))
    .then(r => (Array.isArray(r) ? r : (r.items ?? [])))
}

/**
 * 장바구니 수량 배지 — 신규 API가 아니라 기존 `GET /api/cart`(장바구니 조회)를 재사용해
 * `items[].qty`를 합산한다(STORY "파일" 절, 신규 API 없음 확정 답변). 응답은 항상
 * `{items, totalAmount}` 봉투(`CartController.get`)이지만, `fetchProducts`와 동일하게 raw 배열
 * 가능성도 방어적으로 처리한다. 호출자(`ShopHomePage`)는 세션이 있을 때만, 세션의 `memberId`
 * 그대로만 호출한다(신규 신원 확인 경로 아님).
 */
export function fetchCartItemCount(memberId: string): Promise<number> {
  return get<{ items?: { qty?: number }[] } | { qty?: number }[]>(
    '/api/cart?memberId=' + encodeURIComponent(memberId),
  ).then(r => {
    const items = Array.isArray(r) ? r : (r.items ?? [])
    return items.reduce((sum, it) => sum + (it.qty ?? 0), 0)
  })
}

// SR-302 재작업(round 1 QA 권고 2) — GNB 로그아웃이 clearSession()만 호출해 서버 토큰(apiKey·
// refreshToken)이 만료까지 살아있던 문제. 신규 API가 아니라 기존 INF-MBR-004를 재사용한다(`/api/cart`
// 재사용과 같은 성격).

/**
 * INF-MBR-004 — `POST /api/members/sessions/logout`. 이 경로는 `ApiKeyAuthFilter` 화이트리스트
 * 밖이라(인증 필요) 회원 `apiKey`를 `X-Api-Key`로 직접 실어 보낸다 — 이 SPA에서 클라이언트 코드가
 * 처음으로 `X-Api-Key`를 직접 세팅하는 호출이다(`vite.config.ts` 프록시는 클라이언트가 이미 보낸
 * `X-Api-Key`는 admin 키로 덮어쓰지 않고 그대로 통과시킨다 — FUNC-member-004 사람 결정 1).
 * 204 No Content라 성공 시 바디를 읽지 않는다(`postVoid`와 동일 관례이나 헤더가 달라 별도 구현).
 */
export function logout(apiKey: string): Promise<void> {
  return fetch('/api/members/sessions/logout', {
    method: 'POST',
    headers: { 'X-Api-Key': apiKey, Accept: 'application/json' },
  }).then(async r => {
    if (!r.ok) throw new ApiError(r.status, await parseErrorBody(r))
  })
}

// SR-304 — 상품 상세(`/shop/products/:sku`). `ProductController`/`CartController`는 `ApiExceptionHandler`
// 스코프(assignableTypes=CartController) 밖의 오류(400/404/409, ResponseStatusException)를 Spring 기본
// 오류 바디(`{timestamp,status,error,message,path}`, code 필드 없음)로 낸다 — member 계열의 `{code,message}`
// 계약과 다르므로 `ApiError`를 재사용하지 않고 상태코드만 보관하는 경량 오류 타입을 새로 둔다(STORY
// "API 사전 확인" 절).
export class OrderHttpError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

/** 오류 응답 바디가 `{message}`면 그 값을, 파싱 실패/필드 없음이면 `${status} ${statusText}`로 대체한다. */
async function parseOrderErrorMessage(r: Response): Promise<string> {
  try {
    const body = (await r.json()) as { message?: string }
    return body?.message ?? `${r.status} ${r.statusText}`
  } catch {
    return `${r.status} ${r.statusText}`
  }
}

/** INF-ORD-008 — `GET /api/products/{sku}`. 404를 포함해 실패는 전부 `OrderHttpError`로 던진다. */
export function fetchProduct(sku: string): Promise<Product> {
  return fetch('/api/products/' + encodeURIComponent(sku), { headers: { Accept: 'application/json' } })
    .then(async r => {
      if (!r.ok) throw new OrderHttpError(r.status, await parseOrderErrorMessage(r))
      return (await r.json()) as Product
    })
}

/** 기존 `POST /api/cart/items` 그대로(신규 API 아님) — 400(qty)/404(회원·상품)/409(재고·품절)는
 * `OrderHttpError`로 옮긴다(STORY "API 사전 확인" 절, `CartController.AddCartItemRequest` 필드 그대로). */
export function addCartItem(memberId: string, sku: string, qty: number): Promise<CartItem> {
  return fetch('/api/cart/items', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ memberId, sku, qty }),
  }).then(async r => {
    if (!r.ok) throw new OrderHttpError(r.status, await parseOrderErrorMessage(r))
    return (await r.json()) as CartItem
  })
}

// SR-305 — 장바구니(`/shop/cart`)·주문서(`/shop/order`). 여기서부터는 전부 기존 4개 API
// (GET/PATCH/DELETE `/api/cart*`, POST `/api/cart/checkout`) + 우편번호 검색(`/api/zipcodes`)를
// 그대로 소비만 한다(신규 API·신규 요청/응답 필드 없음, 확정 답변). `CartController`/`ZipcodeController`
// 모두 `ApiExceptionHandler`(assignableTypes=CartController) 스코프 밖이라 `{message}`만 오고
// `code` 필드가 없다 — `fetchProduct`/`addCartItem`과 같은 `OrderHttpError` 계열을 그대로 쓴다.

/** GET /api/cart — `CartService.get` 응답(`{items, totalAmount}`)에서 items만 옮긴다(404: 회원 없음). */
export function fetchCart(memberId: string): Promise<CartRow[]> {
  return fetch('/api/cart?memberId=' + encodeURIComponent(memberId), { headers: { Accept: 'application/json' } })
    .then(async r => {
      if (!r.ok) throw new OrderHttpError(r.status, await parseOrderErrorMessage(r))
      const body = (await r.json()) as { items?: CartRow[] }
      return body.items ?? []
    })
}

/** PATCH /api/cart/items/{sku} — 수량 변경. 400(qty&lt;1)/404(품목없음)/409(재고초과)는 그대로 옮긴다. */
export function updateCartItemQty(memberId: string, sku: string, qty: number): Promise<CartRow> {
  return fetch(`/api/cart/items/${encodeURIComponent(sku)}?memberId=${encodeURIComponent(memberId)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ qty }),
  }).then(async r => {
    if (!r.ok) throw new OrderHttpError(r.status, await parseOrderErrorMessage(r))
    return (await r.json()) as CartRow
  })
}

/** DELETE /api/cart/items/{sku} — 204 No Content. 404(품목없음)는 `OrderHttpError`로 옮긴다. */
export function deleteCartItem(memberId: string, sku: string): Promise<void> {
  return fetch(`/api/cart/items/${encodeURIComponent(sku)}?memberId=${encodeURIComponent(memberId)}`, {
    method: 'DELETE',
  }).then(async r => {
    if (!r.ok) throw new OrderHttpError(r.status, await parseOrderErrorMessage(r))
  })
}

/**
 * POST /api/cart/checkout — 요청 바디는 항상 `{memberId}`뿐(배송지·결제수단은 서버로 전송되지 않는다,
 * STORY "순서·보안" 6 — 계약 변경 금지 확정 답변). 400(빈 장바구니)/404(회원없음)/409(재고부족).
 */
export function checkoutCart(memberId: string): Promise<CheckoutResult> {
  return fetch('/api/cart/checkout', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ memberId }),
  }).then(async r => {
    if (!r.ok) throw new OrderHttpError(r.status, await parseOrderErrorMessage(r))
    return (await r.json()) as CheckoutResult
  })
}

/** GET /api/zipcodes?q= — `ZipcodeController.search` 응답(`{items}`, 0건도 200). 400(검색어 미달/초과). */
export function searchZipcodes(q: string): Promise<ZipcodeResult[]> {
  return fetch('/api/zipcodes?q=' + encodeURIComponent(q), { headers: { Accept: 'application/json' } })
    .then(async r => {
      if (!r.ok) throw new OrderHttpError(r.status, await parseOrderErrorMessage(r))
      const body = (await r.json()) as { items?: ZipcodeResult[] }
      return body.items ?? []
    })
}

// SR-235 — 배송지 관리(`/shop/mypage/addresses`). `MemberAddressController`(INF-MBR-008)는
// `MemberAddressExceptionHandler`로 `{code,message}` 봉투를 낸다 — `login`/`requestPasswordResetCode`와
// 같은 `ApiError` 계열(`OrderHttpError`가 아니다). `/api/members/me/addresses*`는 `ApiKeyAuthFilter`의
// `SCOPE_TO_SELF`가 memberId를 강제하므로(INF-MBR-008 "인증·스코프" 절), `memberId` 쿼리는 어느 함수에도
// 싣지 않는다 — 대신 회원 자신의 `apiKey`를 `X-Api-Key`에 명시적으로 실어 보낸다(`logout`과 동일 패턴,
// 헤더를 빠뜨리면 vite 프록시가 admin 키로 채워 `SCOPE_TO_SELF`가 아예 안 타고 400 `MBR-4200`으로 막힌다).

async function getAuth<T>(url: string, apiKey: string): Promise<T> {
  const r = await fetch(url, { headers: { 'X-Api-Key': apiKey, Accept: 'application/json' } })
  if (!r.ok) throw new ApiError(r.status, await parseErrorBody(r))
  return (await r.json()) as T
}

async function sendAuth<T>(method: 'POST' | 'PUT', url: string, apiKey: string, body?: unknown): Promise<T> {
  const r = await fetch(url, {
    method,
    headers: { 'X-Api-Key': apiKey, 'Content-Type': 'application/json', Accept: 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!r.ok) throw new ApiError(r.status, await parseErrorBody(r))
  return (await r.json()) as T
}

/** GET /api/members/me/addresses — 목록(최근 사용 순, `{items}` 봉투). 0건도 200. */
export function fetchAddresses(apiKey: string): Promise<MemberAddress[]> {
  return getAuth<{ items: MemberAddress[] }>('/api/members/me/addresses', apiKey).then(r => r.items)
}

/** POST /api/members/me/addresses — 등록, 201 + 생성된 배송지(봉투 없음). 409 `MBR-4201`(최대 10개). */
export function registerAddress(apiKey: string, input: MemberAddressInput): Promise<MemberAddress> {
  return sendAuth<MemberAddress>('POST', '/api/members/me/addresses', apiKey, input)
}

/** PUT /api/members/me/addresses/{addressId} — 수정, 200 + 수정된 배송지. `isDefault`는 서버가 무시한다. */
export function updateAddress(apiKey: string, addressId: number, input: MemberAddressInput): Promise<MemberAddress> {
  return sendAuth<MemberAddress>('PUT', `/api/members/me/addresses/${addressId}`, apiKey, input)
}

/** DELETE /api/members/me/addresses/{addressId} — 204 No Content(`postVoid`와 동일하게 바디를 읽지 않는다). */
export function deleteAddress(apiKey: string, addressId: number): Promise<void> {
  return fetch(`/api/members/me/addresses/${addressId}`, {
    method: 'DELETE',
    headers: { 'X-Api-Key': apiKey, Accept: 'application/json' },
  }).then(async r => {
    if (!r.ok) throw new ApiError(r.status, await parseErrorBody(r))
  })
}

/** PUT /api/members/me/addresses/{addressId}/default — 기본 설정, 200 + 배송지(이미 기본이면 no-op). */
export function setDefaultAddress(apiKey: string, addressId: number): Promise<MemberAddress> {
  return sendAuth<MemberAddress>('PUT', `/api/members/me/addresses/${addressId}/default`, apiKey)
}
