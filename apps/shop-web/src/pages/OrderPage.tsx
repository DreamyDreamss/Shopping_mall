// SR-305 — 주문서·주문완료(`/shop/order`) 컨테이너. 진입 시 자체적으로 GET `/api/cart`를 재조회한다
// (라우터 state에 의존하지 않음 — 새로고침·직접 URL 진입에도 견고, STORY "파일" 절). 결제 성공은
// 같은 라우트 안에서 완료 상태로 전환한다(라우트 이동 없음). 컨테이너라 스토리 대상이 아니다(규칙
// `story-per-component` 제외 — `src/pages/`).
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { checkoutCart, fetchCart, logout, OrderHttpError, searchZipcodes } from '../api'
import { clearSession, loadSession } from '../session'
import type { CartRow, CheckoutResult, SessionResult, ZipcodeResult } from '../types'
import { AppShell } from '../features/shop/AppShell'
import { OrderItemsSummary } from '../features/shop/OrderItemsSummary'
import { DeliveryAddressForm } from '../features/shop/DeliveryAddressForm'
import { PaymentMethodSelect, type PaymentMethod } from '../features/shop/PaymentMethodSelect'
import { ZipcodeSearchModal } from '../features/shop/ZipcodeSearchModal'
import { OrderCompleteNotice } from '../features/shop/OrderCompleteNotice'
import { OrderFailureNotice } from '../features/shop/OrderFailureNotice'
import { calcCartTotals } from '../features/shop/cartTotals'
import { hasDeliveryAddressErrors, validateDeliveryAddress, type DeliveryAddressErrors } from '../features/shop/deliveryAddressValidation'
// 재작업(round 2, QA FAIL 필수3) — 내부 상세 문자열 필터를 공용 모듈로 올려 로드 실패·우편번호 검색
// 실패 두 곳(이 파일) 모두 같은 함수 하나만 쓴다(CartPage 두 곳과 동일 헬퍼).
import { toDisplayMessage } from '../features/shop/httpErrorMessage'

type CheckoutStatus = 'idle' | 'pending' | 'error' | 'success'

const won = (n: number) => n.toLocaleString('ko-KR') + '원'
const totalsRowStyle: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', fontSize: 13, padding: '4px 0' }

export default function OrderPage() {
  const navigate = useNavigate()

  const [session, setSession] = useState<SessionResult | null>(() => loadSession())
  const [searchValue, setSearchValue] = useState('')

  const [cartRows, setCartRows] = useState<CartRow[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [recipientName, setRecipientName] = useState('')
  const [phone, setPhone] = useState('')
  const [zipcode, setZipcode] = useState('')
  const [roadAddress, setRoadAddress] = useState('')
  const [detailAddress, setDetailAddress] = useState('')
  const [addressErrors, setAddressErrors] = useState<DeliveryAddressErrors>({})
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CARD')

  const [zipcodeModalOpen, setZipcodeModalOpen] = useState(false)
  const [zipcodeQuery, setZipcodeQuery] = useState('')
  const [zipcodeResults, setZipcodeResults] = useState<ZipcodeResult[]>([])
  const [zipcodeLoading, setZipcodeLoading] = useState(false)
  const [zipcodeError, setZipcodeError] = useState<string | null>(null)

  const [checkoutStatus, setCheckoutStatus] = useState<CheckoutStatus>('idle')
  const [checkoutErrorStatus, setCheckoutErrorStatus] = useState(0)
  const [checkoutErrorMessage, setCheckoutErrorMessage] = useState<string | null>(null)
  const [checkoutResult, setCheckoutResult] = useState<CheckoutResult | null>(null)

  const inFlightKeyRef = useRef<string | null>(null)
  const requestIdRef = useRef(0)
  // [결제하기] 연타 방지 — `ProductDetailPage.inFlightRef`와 같은 동기 잠금(STORY "순서·보안" 8).
  const checkoutInFlightRef = useRef(false)

  const load = async (memberId: string) => {
    const key = 'order-cart:' + memberId
    if (inFlightKeyRef.current === key) return
    inFlightKeyRef.current = key
    const id = ++requestIdRef.current
    setLoading(true)
    setLoadError(null)
    try {
      const rows = await fetchCart(memberId)
      if (id !== requestIdRef.current) return
      setCartRows(rows)
    } catch (e) {
      if (id !== requestIdRef.current) return
      setLoadError(toDisplayMessage(e))
    } finally {
      if (inFlightKeyRef.current === key) inFlightKeyRef.current = null
      if (id === requestIdRef.current) setLoading(false)
    }
  }

  // 세션이 없으면 GET /api/cart를 호출하지 않는다(STORY "폴백·우회 경로의 자격 판정" 절).
  useEffect(() => {
    if (!session?.memberId) { setLoading(false); return }
    void load(session.memberId)
  }, [session?.memberId])

  const totals = calcCartTotals(
    cartRows.map(r => ({ sku: r.sku, lineTotal: r.lineTotal })),
    new Set(cartRows.map(r => r.sku)),
  )

  const handleZipcodeSearch = () => {
    setZipcodeLoading(true)
    setZipcodeError(null)
    searchZipcodes(zipcodeQuery.trim())
      .then(setZipcodeResults)
      .catch(e => setZipcodeError(toDisplayMessage(e)))
      .finally(() => setZipcodeLoading(false))
  }

  const handleZipcodeSelect = (result: ZipcodeResult) => {
    setZipcode(result.zipcode)
    setRoadAddress(result.roadAddress)
    setZipcodeModalOpen(false)
    setAddressErrors(errs => { const { zipcode: _drop, ...rest } = errs; return rest })
  }

  /**
   * [결제하기] — 배송지 클라이언트 검증 먼저(API 호출 없음), 통과 시에만 `checkoutCart` 호출(요청
   * 바디는 여전히 `{memberId}`뿐 — 배송지·결제수단은 서버로 전송되지 않는다, STORY "순서·보안" 6).
   * 실패해도 입력값은 그대로 유지한다(재입력 방지, 확정 답변 "입력을 잃지 않는다").
   */
  const handleCheckout = () => {
    if (checkoutInFlightRef.current) return
    if (!session?.memberId) return
    const errors = validateDeliveryAddress({ recipientName, phone, zipcode, roadAddress, detailAddress })
    if (hasDeliveryAddressErrors(errors)) {
      setAddressErrors(errors)
      return
    }
    setAddressErrors({})
    checkoutInFlightRef.current = true
    setCheckoutStatus('pending')
    checkoutCart(session.memberId)
      .then(result => {
        setCheckoutResult(result)
        setCheckoutStatus('success')
        // 재작업(round 2, 권고 2) — 서버는 이미 `deleteAllItems`로 장바구니를 비웠다. `cartRows`를
        // 그대로 두면 완료 화면에 머무는 동안 GNB 배지가 주문 전 수량을 계속 보여준다.
        setCartRows([])
      })
      .catch(e => {
        setCheckoutStatus('error')
        setCheckoutErrorStatus(e instanceof OrderHttpError ? e.status : 0)
        setCheckoutErrorMessage(e instanceof OrderHttpError ? e.message : null)
      })
      .finally(() => { checkoutInFlightRef.current = false })
  }

  const handleLogout = () => {
    const apiKey = session?.apiKey
    const clear = () => { clearSession(); setSession(null) }
    if (!apiKey) { clear(); return }
    void logout(apiKey).catch(() => {}).finally(clear)
  }

  const handleSearchSubmit = () => {
    const keyword = searchValue.trim()
    navigate(keyword ? `/shop/products?keyword=${encodeURIComponent(keyword)}` : '/shop/products')
  }

  const cartItemCount = cartRows.reduce((sum, r) => sum + r.qty, 0)

  return (
    <AppShell session={session} cartItemCount={cartItemCount} searchValue={searchValue}
              onSearchChange={setSearchValue} onSearchSubmit={handleSearchSubmit} onLogout={handleLogout}>
      <div style={{ fontFamily: 'system-ui, sans-serif', color: '#222', padding: '16px 18px 40px' }}>
        <h1 style={{ fontSize: 18, marginBottom: 14 }}>주문서</h1>

        {!session ? (
          <div style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>
            <p style={{ marginBottom: 12 }}>로그인이 필요합니다</p>
            <a href="#/login" style={{ color: '#0b4ea2' }}>로그인 하러 가기</a>
          </div>
        ) : loading ? (
          <div role="status" style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>불러오는 중…</div>
        ) : loadError ? (
          <div role="alert" style={{ border: '1px solid #e0b4b4', background: '#fff6f6', color: '#912d2b',
                                      borderRadius: 6, padding: '16px 18px', display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'flex-start' }}>
            <span>불러오지 못했습니다 — {loadError}</span>
            <button type="button" onClick={() => void load(session.memberId)}
                    style={{ border: '1px solid #912d2b', borderRadius: 5, background: '#fff', color: '#912d2b',
                             fontSize: 12.5, padding: '5px 12px', cursor: 'pointer' }}>
              다시 시도
            </button>
          </div>
        ) : checkoutStatus === 'success' && checkoutResult ? (
          <OrderCompleteNotice orderNo={checkoutResult.orderNo} totalAmount={checkoutResult.totalAmount}
                                itemCount={checkoutResult.itemCount} />
        ) : cartRows.length === 0 ? (
          <div style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>
            <p style={{ marginBottom: 12 }}>주문할 상품이 없습니다</p>
            <a href="#/shop/cart" style={{ color: '#0b4ea2' }}>장바구니로 이동</a>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            <OrderItemsSummary items={cartRows} />
            <DeliveryAddressForm recipientName={recipientName} phone={phone} zipcode={zipcode}
                                  roadAddress={roadAddress} detailAddress={detailAddress} errors={addressErrors}
                                  onRecipientNameChange={setRecipientName} onPhoneChange={setPhone}
                                  onDetailAddressChange={setDetailAddress}
                                  onOpenZipcodeSearch={() => setZipcodeModalOpen(true)} />
            <PaymentMethodSelect value={paymentMethod} onChange={setPaymentMethod} />

            {/* 재작업(round 2, QA FAIL 필수2) — 주문서·주문완료가 같은 3줄(상품금액/배송비/결제금액)
                구성을 같은 계산 함수(`calcCartTotals`/`calcTotalsFromProductAmount`)로 보여준다.
                서버 checkout 응답 `totalAmount`는 상품금액만이라(`CartService.checkout`, 배송비
                개념 없음) 한 줄짜리 "결제예정금액"만 보여주면 완료 화면과 3,000원 어긋난다. */}
            <aside aria-label="결제 합계" style={{ border: '1px solid #eef0f2', borderRadius: 8, padding: '14px 16px' }}>
              <div style={totalsRowStyle}><span>상품금액</span><span>{won(totals.productAmount)}</span></div>
              <div style={totalsRowStyle}><span>배송비</span><span>{won(totals.shippingFee)}</span></div>
              <div style={{ ...totalsRowStyle, fontWeight: 800, fontSize: 15, borderTop: '1px solid #eef0f2', marginTop: 8, paddingTop: 10 }}>
                <span>결제예정금액</span><span>{won(totals.payableAmount)}</span>
              </div>
            </aside>

            {checkoutStatus === 'error' && (
              <OrderFailureNotice status={checkoutErrorStatus} message={checkoutErrorMessage} />
            )}

            <button type="button" onClick={handleCheckout} disabled={checkoutStatus === 'pending'}
                    style={{ border: 0, borderRadius: 6, padding: '12px 0', fontSize: 14, fontWeight: 700,
                             color: '#fff', background: checkoutStatus === 'pending' ? '#7ea3cf' : '#0b4ea2',
                             cursor: checkoutStatus === 'pending' ? 'not-allowed' : 'pointer' }}>
              {checkoutStatus === 'pending' ? '결제 처리 중…' : '결제하기'}
            </button>
          </div>
        )}

        <ZipcodeSearchModal open={zipcodeModalOpen} query={zipcodeQuery} onQueryChange={setZipcodeQuery}
                            onSearch={handleZipcodeSearch} results={zipcodeResults} loading={zipcodeLoading}
                            error={zipcodeError} onSelect={handleZipcodeSelect}
                            onClose={() => setZipcodeModalOpen(false)} />
      </div>
    </AppShell>
  )
}
