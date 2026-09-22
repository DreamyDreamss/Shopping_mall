// SR-305 — 장바구니(`/shop/cart`) 컨테이너. 데이터 오케스트레이션만, 렌더는 하위 부품에 위임한다
// (파일 크기 상한 300줄 고려, `ProductDetailPage` 관례). 컨테이너라 스토리 대상이 아니다(규칙
// `story-per-component` 제외 — `src/pages/`).
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { deleteCartItem, fetchCart, fetchProducts, logout, updateCartItemQty } from '../api'
import { clearSession, loadSession } from '../session'
import type { CartRow, Product, SessionResult } from '../types'
import { AppShell } from '../features/shop/AppShell'
import { CartLineItem } from '../features/shop/CartLineItem'
import { CartEmptyState } from '../features/shop/CartEmptyState'
import { CartSummary } from '../features/shop/CartSummary'
import { calcCartTotals } from '../features/shop/cartTotals'
// 재작업(round 2, QA FAIL 필수3) — 내부 상세 문자열 필터를 공용 모듈로 올려 라인 오류·로드 실패 두
// 곳(이 파일) 모두 같은 함수 하나만 쓴다(OrderPage 두 곳과 동일 헬퍼).
import { toDisplayMessage } from '../features/shop/httpErrorMessage'

interface CartLine {
  sku: string
  productName: string
  imageUrl: string | null
  price: number
  qty: number
  lineTotal: number
  stockQty: number
}

export default function CartPage() {
  const navigate = useNavigate()

  const [session, setSession] = useState<SessionResult | null>(() => loadSession())
  const [searchValue, setSearchValue] = useState('')

  const [cartRows, setCartRows] = useState<CartRow[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [selected, setSelected] = useState<Set<string>>(new Set())

  const [pendingSkus, setPendingSkus] = useState<Set<string>>(new Set())
  const [lineWarnings, setLineWarnings] = useState<Record<string, string | null>>({})
  const [lineErrors, setLineErrors] = useState<Record<string, string | null>>({})

  // `ProductListPage`와 동일 조합(같은 파라미터 재요청 억제 + 응답 적용 순서 세대 카운터) — 프레임워크
  // 실행 모델 함정 절, React 19 StrictMode(dev) 이중 effect 대응.
  const inFlightKeyRef = useRef<string | null>(null)
  const requestIdRef = useRef(0)
  // 수량 변경/삭제 연타 방지 — `ProductDetailPage.inFlightRef`와 같은 원리, 품목(sku)별로 잠근다.
  const inFlightSkusRef = useRef<Set<string>>(new Set())
  const orderNavigateRef = useRef(false)

  const load = async (memberId: string) => {
    const key = 'cart:' + memberId
    if (inFlightKeyRef.current === key) return
    inFlightKeyRef.current = key
    const id = ++requestIdRef.current
    setLoading(true)
    setLoadError(null)
    try {
      const [rows, productList] = await Promise.all([fetchCart(memberId), fetchProducts()])
      if (id !== requestIdRef.current) return
      setCartRows(rows)
      setProducts(productList)
      setSelected(new Set(rows.map(r => r.sku)))
    } catch (e) {
      if (id !== requestIdRef.current) return
      setLoadError(toDisplayMessage(e))
    } finally {
      if (inFlightKeyRef.current === key) inFlightKeyRef.current = null
      if (id === requestIdRef.current) setLoading(false)
    }
  }

  // 세션이 없으면 GET /api/cart 자체를 호출하지 않는다(STORY "순서·보안" 1, "폴백·우회 경로의 자격
  // 판정" 절 — 신규 신원확인 경로를 만들지 않는다).
  useEffect(() => {
    if (!session?.memberId) { setLoading(false); return }
    void load(session.memberId)
  }, [session?.memberId])

  const productBySku = useMemo(() => new Map(products.map(p => [p.sku, p])), [products])
  const lineItems: CartLine[] = useMemo(() => cartRows.map(row => {
    const product = productBySku.get(row.sku)
    // 상품 정보를 못 찾은 경우(비정상) 재고 상한을 현재 수량으로 두어 조작을 부당하게 막지 않는다.
    return {
      sku: row.sku, productName: row.productName, price: row.price, qty: row.qty, lineTotal: row.lineTotal,
      stockQty: product?.stockQty ?? row.qty, imageUrl: product?.imageUrl ?? null,
    }
  }), [cartRows, productBySku])

  const cartItemCount = cartRows.reduce((sum, r) => sum + r.qty, 0)
  const totals = calcCartTotals(lineItems.map(i => ({ sku: i.sku, lineTotal: i.lineTotal })), selected)

  const toggleSelected = (sku: string) => {
    setSelected(prev => {
      const next = new Set(prev)
      if (next.has(sku)) next.delete(sku); else next.add(sku)
      return next
    })
  }

  /**
   * 수량 변경 — 클라이언트 클램프(1~재고)가 1차 방어선이다(STORY "순서·보안" 2). 클램프 결과가 현재
   * 서버 확인 수량과 같으면(경계에서의 시도) 안내만 하고 API를 호출하지 않는다. 낙관적으로 먼저
   * 올리지 않으므로(표시값은 항상 서버 확인 수량) 실패 시 별도로 "되돌릴" 값이 없다 — 그대로 두면
   * 이미 직전 확인된 수량이다.
   */
  const handleQtyChange = (sku: string, requestedQty: number) => {
    if (!session?.memberId) return
    if (inFlightSkusRef.current.has(sku)) return
    const item = lineItems.find(i => i.sku === sku)
    if (!item) return
    const max = Math.max(1, item.stockQty)
    const safeRaw = Number.isFinite(requestedQty) ? Math.trunc(requestedQty) : item.qty
    const clamped = Math.min(Math.max(1, safeRaw), max)

    if (clamped === item.qty) {
      const warning = safeRaw < 1 ? '1개 미만으로는 변경할 수 없습니다' : safeRaw > max ? `최대 수량은 ${max}개입니다` : null
      setLineWarnings(w => ({ ...w, [sku]: warning }))
      return
    }

    setLineWarnings(w => ({ ...w, [sku]: null }))
    setLineErrors(e => ({ ...e, [sku]: null }))
    inFlightSkusRef.current.add(sku)
    setPendingSkus(prev => new Set(prev).add(sku))

    updateCartItemQty(session.memberId, sku, clamped)
      .then(row => {
        // 수량·금액은 서버 응답을 정본으로 다시 그린다(사람 수정) — 클라이언트 클램프값을 남기지 않는다.
        setCartRows(rows => rows.map(r => (r.sku === sku ? { ...r, qty: row.qty, lineTotal: row.lineTotal } : r)))
      })
      .catch(e => setLineErrors(err => ({ ...err, [sku]: toDisplayMessage(e) })))
      .finally(() => {
        inFlightSkusRef.current.delete(sku)
        setPendingSkus(prev => { const next = new Set(prev); next.delete(sku); return next })
      })
  }

  const handleDelete = (sku: string) => {
    if (!session?.memberId) return
    if (inFlightSkusRef.current.has(sku)) return
    inFlightSkusRef.current.add(sku)
    setPendingSkus(prev => new Set(prev).add(sku))

    deleteCartItem(session.memberId, sku)
      .then(() => {
        setCartRows(rows => rows.filter(r => r.sku !== sku))
        setSelected(prev => { const next = new Set(prev); next.delete(sku); return next })
        setLineWarnings(w => { const { [sku]: _drop, ...rest } = w; return rest })
        setLineErrors(e => { const { [sku]: _drop, ...rest } = e; return rest })
      })
      .catch(e => setLineErrors(err => ({ ...err, [sku]: toDisplayMessage(e) })))
      .finally(() => {
        inFlightSkusRef.current.delete(sku)
        setPendingSkus(prev => { const next = new Set(prev); next.delete(sku); return next })
      })
  }

  // 전체선택 === 전체개수일 때만 이동한다(부분선택 상태에선 `CartSummary`가 버튼 자체를 비활성화한다).
  const handleOrder = () => {
    if (orderNavigateRef.current) return
    orderNavigateRef.current = true
    navigate('/shop/order')
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

  return (
    <AppShell session={session} cartItemCount={cartItemCount} searchValue={searchValue}
              onSearchChange={setSearchValue} onSearchSubmit={handleSearchSubmit} onLogout={handleLogout}>
      <div style={{ fontFamily: 'system-ui, sans-serif', color: '#222', padding: '16px 18px 40px' }}>
        <h1 style={{ fontSize: 18, marginBottom: 14 }}>장바구니</h1>

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
        ) : lineItems.length === 0 ? (
          <CartEmptyState />
        ) : (
          <div style={{ display: 'flex', gap: 24, alignItems: 'flex-start', flexWrap: 'wrap' }}>
            <div role="list" aria-label="장바구니 목록" style={{ flex: '1 1 480px' }}>
              {lineItems.map(item => (
                <CartLineItem key={item.sku} sku={item.sku} productName={item.productName} imageUrl={item.imageUrl}
                              price={item.price} qty={item.qty} lineTotal={item.lineTotal} stockQty={item.stockQty}
                              selected={selected.has(item.sku)} onToggleSelected={toggleSelected}
                              onQtyChange={handleQtyChange} onDelete={handleDelete}
                              pending={pendingSkus.has(item.sku)}
                              warningMessage={lineWarnings[item.sku]} errorMessage={lineErrors[item.sku]} />
              ))}
            </div>
            <CartSummary productAmount={totals.productAmount} shippingFee={totals.shippingFee}
                         payableAmount={totals.payableAmount} selectedCount={selected.size}
                         totalCount={lineItems.length} onOrder={handleOrder} />
          </div>
        )}
      </div>
    </AppShell>
  )
}
