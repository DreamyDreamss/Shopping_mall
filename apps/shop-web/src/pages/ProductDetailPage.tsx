// SR-304 — 상품 상세(`/shop/products/:sku`) 컨테이너. 데이터 오케스트레이션만, 렌더는 하위 부품에
// 위임한다(파일 크기 상한 300줄 고려). 컨테이너라 스토리 대상이 아니다(규칙 `story-per-component`
// 제외 — `src/pages/`).
import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { addCartItem, fetchCartItemCount, fetchProduct, fetchProducts, logout, OrderHttpError } from '../api'
import { clearSession, loadSession } from '../session'
import type { Product, SessionResult } from '../types'
import { AppShell } from '../features/shop/AppShell'
import { ProductDetailSkeleton } from '../features/shop/ProductDetailSkeleton'
import { ProductNotFoundNotice } from '../features/shop/ProductNotFoundNotice'
import { ProductImageGallery } from '../features/shop/ProductImageGallery'
import { ProductInfoPanel, type AddAction, type AddStatus } from '../features/shop/ProductInfoPanel'
import { ProductDetailTabs } from '../features/shop/ProductDetailTabs'
import { ProductNoticeTable } from '../features/shop/ProductNoticeTable'
import { RelatedProducts } from '../features/shop/RelatedProducts'
import { pickRelatedProducts } from '../features/shop/relatedProductsPicker'

type ErrorKind = 'notFound' | 'fetchError' | null

/**
 * 담기 실패(400/404/409)를 사유 문구로 매핑한다 — 서버 원문(`ResponseStatusException` 메시지)을
 * 그대로 노출하지 않는다(STORY "순서·보안" 5, 내부 정보 노출 방지). 그 외(네트워크/5xx)는 별도의
 * 포괄 문구로 뭉뚱그린다.
 */
function mapAddErrorMessage(e: unknown): string {
  if (e instanceof OrderHttpError) {
    if (e.status === 400) return '수량을 확인해 주세요'
    if (e.status === 404) return '상품 또는 회원 정보를 찾을 수 없습니다'
    if (e.status === 409) return '재고가 부족하거나 판매중지된 상품입니다'
  }
  return '일시적 오류입니다. 다시 시도해 주세요'
}

export default function ProductDetailPage() {
  const { sku = '' } = useParams<{ sku: string }>()
  const navigate = useNavigate()

  const [session, setSession] = useState<SessionResult | null>(() => loadSession())
  const [cartItemCount, setCartItemCount] = useState(0)
  const [searchValue, setSearchValue] = useState('')

  const [product, setProduct] = useState<Product | null>(null)
  const [loading, setLoading] = useState(true)
  const [errorKind, setErrorKind] = useState<ErrorKind>(null)
  // `ProductListPage`와 동일한 세대 카운터 — sku 변경 시 이전 응답이 더 최신 요청 결과를 덮어쓰지
  // 않게 한다(프레임워크 실행 모델 함정 절, React 19 StrictMode 이중 effect 대응).
  const requestIdRef = useRef(0)

  const [allProducts, setAllProducts] = useState<Product[]>([])

  const [qty, setQty] = useState(1)
  const [addStatus, setAddStatus] = useState<AddStatus>('idle')
  const [addErrorMessage, setAddErrorMessage] = useState<string | null>(null)
  const [lastAction, setLastAction] = useState<AddAction | null>(null)
  // `ShopHomePage.inFlightRef`와 같은 원리 — 클릭 핸들러 진입 시점에 ref로 즉시 잠근다(state 반영
  // 지연에 의존하지 않음, 사용자 더블클릭이 실제로 두 번의 POST를 만드는 것을 막는다).
  const inFlightRef = useRef(false)

  const load = async (targetSku: string) => {
    const id = ++requestIdRef.current
    setLoading(true)
    setErrorKind(null)
    try {
      const data = await fetchProduct(targetSku)
      if (id !== requestIdRef.current) return
      setProduct(data)
      setQty(1)
      setAddStatus('idle')
      setAddErrorMessage(null)
      setLastAction(null)
    } catch (e) {
      if (id !== requestIdRef.current) return
      setProduct(null)
      // 두 실패 사유를 하나의 default로 뭉치지 않는다(STORY "순서·보안" 1, SR-234 r1 사례 대조).
      setErrorKind(e instanceof OrderHttpError && e.status === 404 ? 'notFound' : 'fetchError')
    } finally {
      if (id === requestIdRef.current) setLoading(false)
    }
  }

  useEffect(() => { void load(sku) }, [sku])

  // 관련상품 후보 — 전용 API가 없어(STORY "데이터" 절) 기존 무인자 `fetchProducts()`를 1회만 호출한다.
  // product 로드와 서로 무관해 병렬로 요청한다.
  useEffect(() => {
    fetchProducts().then(setAllProducts).catch(() => setAllProducts([]))
  }, [])

  // 세션이 있을 때만 장바구니 수량을 조회한다(`ShopHomePage`/`ProductListPage`와 동일 규칙 — 신규
  // 신원 확인 경로 아님, STORY "폴백·우회 경로의 자격 판정" 절). 조회 실패는 조용히 0으로 둔다.
  useEffect(() => {
    if (!session?.memberId) { setCartItemCount(0); return }
    let cancelled = false
    fetchCartItemCount(session.memberId)
      .then(count => { if (!cancelled) setCartItemCount(count) })
      .catch(() => { if (!cancelled) setCartItemCount(0) })
    return () => { cancelled = true }
  }, [session?.memberId])

  // `ShopHomePage.handleLogout`/`ProductListPage.handleLogout`과 동일 로직(복제, 훅 추출하지 않음 —
  // 기존 화면들의 관례 일관성 유지).
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

  // 수량 클램프(1~재고) — `ProductInfoPanel`이 아니라 여기서 한다(STORY "상태관리 방식" 절, 클램프
  // 로직을 컴포넌트에 복제하지 않는다).
  const handleQtyChange = (next: number) => {
    if (!product) return
    const max = Math.max(1, product.stockQty)
    const safe = Number.isFinite(next) ? Math.trunc(next) : 1
    setQty(Math.min(Math.max(1, safe), max))
  }

  const handleAdd = (action: AddAction) => {
    if (inFlightRef.current) return // 연타 방지 — 동기 잠금(위 inFlightRef 주석)
    if (!session?.memberId || !product || product.stockQty <= 0) return
    inFlightRef.current = true
    setAddStatus('pending')
    setLastAction(action)
    addCartItem(session.memberId, product.sku, qty)
      .then(() => {
        // 순서 고정(STORY "순서·보안" 4) — 서버 응답 성공 확인 → 로컬 가산 → success 표시.
        // 낙관적으로 먼저 올리면 409/404 실패 시 배지만 늘고 실제 장바구니는 그대로인 불일치가 생긴다.
        setCartItemCount(c => c + qty)
        setAddStatus('success')
      })
      .catch(e => {
        // 사람 수정 — 담기 실패 시 qty·선택 상태는 초기화하지 않는다(재시도 시 다시 입력하지 않도록).
        setAddStatus('error')
        setAddErrorMessage(mapAddErrorMessage(e))
      })
      .finally(() => { inFlightRef.current = false })
  }

  const disabledReason = !session
    ? '로그인이 필요합니다'
    : product && product.stockQty <= 0
      ? '품절된 상품입니다'
      : null

  const relatedProducts = pickRelatedProducts(allProducts, sku, 8)

  return (
    <AppShell session={session} cartItemCount={cartItemCount} searchValue={searchValue}
              onSearchChange={setSearchValue} onSearchSubmit={handleSearchSubmit} onLogout={handleLogout}>
      <div style={{ fontFamily: 'system-ui, sans-serif', color: '#222', padding: '16px 18px 40px' }}>
        {loading ? (
          <ProductDetailSkeleton />
        ) : errorKind ? (
          <ProductNotFoundNotice reason={errorKind}
                                  onBackToList={() => navigate('/shop/products')}
                                  onRetry={() => void load(sku)} />
        ) : product ? (
          <>
            <div style={{ display: 'flex', gap: 32, flexWrap: 'wrap' }}>
              <div style={{ flex: '0 0 320px' }}>
                <ProductImageGallery imageUrl={product.imageUrl} productName={product.productName} />
              </div>
              <div style={{ flex: '1 1 320px' }}>
                <ProductInfoPanel product={product} qty={qty} onQtyChange={handleQtyChange}
                                   onAddToCart={() => handleAdd('cart')} onBuyNow={() => handleAdd('buyNow')}
                                   addStatus={addStatus} addErrorMessage={addErrorMessage} lastAction={lastAction}
                                   disabledReason={disabledReason} />
              </div>
            </div>
            <div style={{ marginTop: 32 }}>
              <ProductDetailTabs />
            </div>
            <div style={{ marginTop: 32 }}>
              <ProductNoticeTable />
            </div>
            <div style={{ marginTop: 32 }}>
              <RelatedProducts products={relatedProducts}
                                onSelect={(selectedSku: string) => navigate('/shop/products/' + encodeURIComponent(selectedSku))} />
            </div>
          </>
        ) : null}
      </div>
    </AppShell>
  )
}
