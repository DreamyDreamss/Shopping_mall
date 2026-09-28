// SR-360 — 상품 비교 화면(`/shop/compare`) 컨테이너. `RecentlyViewedPage.tsx`/`WishListPage.tsx`를 그대로
// 본뜬 구조(비로그인 리다이렉트 navigate(), useRef requestId 가드, GNB 장바구니 배지 fetchCartItemCount
// 재사용, logout 재사용, handleSearchSubmit 재사용) — 컨테이너라 스토리 대상이 아니다(규칙
// `story-per-component` 기존 예외 관례, `WishListPage`/`RecentlyViewedPage`와 동일). `api.ts`의
// `fetchCompares`/`removeCompare`(SR-360 #2 구현)만 소비한다 — 신규 API 함수를 만들지 않는다.
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchCartItemCount, fetchCompares, logout, removeCompare } from '../api'
import { clearSession, loadSession } from '../session'
import type { ProductCompareItem, SessionResult } from '../types'
import { AppShell } from '../features/shop/AppShell'
import { toDisplayMessage } from '../features/shop/httpErrorMessage'
import { ProductCompareTable, type ProductCompareStatus } from '../features/shop/ProductCompareTable'

const alertBoxStyle: React.CSSProperties = {
  border: '1px solid #e0b4b4', background: '#fff6f6', color: '#912d2b', borderRadius: 6, padding: '10px 14px', fontSize: 12.5,
}

export default function ProductComparePage() {
  const navigate = useNavigate()

  const [session, setSession] = useState<SessionResult | null>(() => loadSession())
  const [searchValue, setSearchValue] = useState('')
  const [cartItemCount, setCartItemCount] = useState(0)

  const [items, setItems] = useState<ProductCompareItem[]>([])
  const [status, setStatus] = useState<ProductCompareStatus>('loading')
  const [actionError, setActionError] = useState<string | null>(null)
  const [pendingSku, setPendingSku] = useState<string | null>(null)

  const requestIdRef = useRef(0)
  // 빼기 연타 방지(`WishListPage.inFlightSkuRef`와 동일 원리) — 동시 1건뿐이라 단일 ref로 충분.
  const inFlightSkuRef = useRef<string | null>(null)

  // 비로그인 진입 — `WishListPage`/`RecentlyViewedPage`와 동일 리다이렉트 규약(AC "비로그인은 로그인
  // 화면으로 보낸다", 이 화면의 유일한 진입점은 상품 상세 [비교하기] 링크).
  useEffect(() => {
    if (!session?.memberId) {
      navigate('/login?redirect=' + encodeURIComponent('/shop/compare'), { replace: true })
    }
  }, [session?.memberId, navigate])

  const load = async (apiKey: string) => {
    const id = ++requestIdRef.current
    setStatus('loading')
    try {
      const rows = await fetchCompares(apiKey)
      if (id !== requestIdRef.current) return
      setItems(rows)
      setStatus('ready')
    } catch {
      if (id !== requestIdRef.current) return
      setItems([])
      setStatus('error')
    }
  }

  useEffect(() => {
    if (!session?.memberId) { setStatus('ready'); return }
    void load(session.apiKey)
  }, [session?.memberId])

  // GNB 장바구니 배지 — `WishListPage`/`RecentlyViewedPage`와 동일하게 기존 `fetchCartItemCount` 재사용
  // (신규 API 없음).
  useEffect(() => {
    if (!session?.memberId) { setCartItemCount(0); return }
    let cancelled = false
    fetchCartItemCount(session.memberId)
      .then(count => { if (!cancelled) setCartItemCount(count) })
      .catch(() => { if (!cancelled) setCartItemCount(0) })
    return () => { cancelled = true }
  }, [session?.memberId])

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

  const handleRetry = () => {
    if (session?.apiKey) void load(session.apiKey)
  }

  // 빼기 — ①연타 방지 ref → ②removeCompare 호출 → ③성공하면 AC가 명시한 대로 목록을 다시 읽는다
  // (낙관적 로컬 제거 없음, STORY "순서·보안" 3 — 재조회로 대체해 SR-356류 낙관적 롤백 버그 클래스가
  // 애초에 성립하지 않는다) → ④실패하면 목록을 건드리지 않고 인라인 오류만 보여준다.
  const handleRemove = (sku: string) => {
    if (!session) return
    if (inFlightSkuRef.current === sku) return
    inFlightSkuRef.current = sku
    setPendingSku(sku)
    setActionError(null)

    removeCompare(session.apiKey, sku)
      .then(() => load(session.apiKey))
      .catch(e => { setActionError(toDisplayMessage(e)) })
      .finally(() => {
        inFlightSkuRef.current = null
        setPendingSku(null)
      })
  }

  return (
    <AppShell session={session} cartItemCount={cartItemCount} searchValue={searchValue}
              onSearchChange={setSearchValue} onSearchSubmit={handleSearchSubmit} onLogout={handleLogout}>
      <div style={{ fontFamily: 'system-ui, sans-serif', color: '#222', padding: '16px 18px 40px' }}>
        <h1 style={{ fontSize: 18, marginBottom: 14 }}>상품 비교</h1>
        {!session?.memberId ? (
          <div role="status" style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>이동 중…</div>
        ) : (
          <>
            {actionError && <div role="alert" style={{ ...alertBoxStyle, marginBottom: 14 }}>{actionError}</div>}
            <ProductCompareTable status={status} items={items} onRemove={handleRemove}
                                  pendingSku={pendingSku} onRetry={handleRetry} />
          </>
        )}
      </div>
    </AppShell>
  )
}
