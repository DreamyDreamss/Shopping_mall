// SR-357 — 마이페이지 최근 본 상품(`/shop/mypage/recently-viewed`) 컨테이너. `WishListPage.tsx`를 그대로
// 본뜬 구조(비로그인 리다이렉트 navigate(), useRef requestId 가드, GNB 장바구니 배지 fetchCartItemCount
// 재사용, 로그아웃 logout 재사용) — 컨테이너라 스토리 대상이 아니다(규칙 story-per-component 예외,
// 기존 관례). 조회 전용 화면이라 WishListPage의 낙관적 삭제·연타방지 ref는 없다.
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchCartItemCount, fetchRecentlyViewed, logout } from '../api'
import { clearSession, loadSession } from '../session'
import type { RecentlyViewedItem, SessionResult } from '../types'
import { AppShell } from '../features/shop/AppShell'
import { RecentlyViewedSection, type RecentlyViewedStatus } from '../features/shop/RecentlyViewedSection'

export default function RecentlyViewedPage() {
  const navigate = useNavigate()

  const [session, setSession] = useState<SessionResult | null>(() => loadSession())
  const [searchValue, setSearchValue] = useState('')
  const [cartItemCount, setCartItemCount] = useState(0)

  const [items, setItems] = useState<RecentlyViewedItem[]>([])
  const [status, setStatus] = useState<RecentlyViewedStatus>('loading')

  const requestIdRef = useRef(0)

  // 비로그인 진입 — `WishListPage`와 동일 리다이렉트 규약(AC 명시 경로, 이번 화면의 유일한 진입점).
  useEffect(() => {
    if (!session?.memberId) {
      navigate('/login?redirect=' + encodeURIComponent('/shop/mypage/recently-viewed'), { replace: true })
    }
  }, [session?.memberId, navigate])

  const load = async (apiKey: string) => {
    const id = ++requestIdRef.current
    setStatus('loading')
    try {
      const rows = await fetchRecentlyViewed(apiKey)
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

  // GNB 장바구니 배지 — `WishListPage`와 동일하게 기존 `fetchCartItemCount` 재사용(신규 API 없음).
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

  return (
    <AppShell session={session} cartItemCount={cartItemCount} searchValue={searchValue}
              onSearchChange={setSearchValue} onSearchSubmit={handleSearchSubmit} onLogout={handleLogout}>
      <div style={{ fontFamily: 'system-ui, sans-serif', color: '#222', padding: '16px 18px 40px' }}>
        <h1 style={{ fontSize: 18, marginBottom: 14 }}>최근 본 상품</h1>
        {!session?.memberId ? (
          <div role="status" style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>이동 중…</div>
        ) : (
          <RecentlyViewedSection status={status} items={items} onRetry={handleRetry} />
        )}
      </div>
    </AppShell>
  )
}
