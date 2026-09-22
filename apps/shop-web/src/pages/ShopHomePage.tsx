// SR-302 — 쇼핑 홈(메인) 컨테이너.
// SR-303 재작업 지시 1(round1) — Gnb 검색 제출이 상품 목록 화면(`/shop/products`)으로 진입하는
// 경로다(round1 QA CONCERNS medium: 진입 경로 0건). 검색어를 `?keyword=` 쿼리로 실어 이동한다 —
// `ProductListPage`가 그 값을 초기 `appliedKeyword`로 읽어 입력창에 반영한다.
// SR-303 재작업 지시(round2) — CategoryShortcuts 클릭은 round1에서 라벨을 keyword로 실어 보냈으나
// 시드 상품명과 라벨이 겹치지 않아 6/6이 항상 빈 결과였다(round2 QA CONCERNS medium — 사람 지시
// 자체의 결함, 사례집 SR-231 r5). keyword 없이 `/shop/products`로만 이동해 전체 목록을 연다.
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchCartItemCount, fetchProducts, logout } from '../api'
import { clearSession, loadSession } from '../session'
import type { Product, SessionResult } from '../types'
import { loadRecentlyViewedSkus, recordViewed } from '../features/shop/recentlyViewedStorage'
import { BANNER_SLIDES, CATEGORY_SHORTCUTS } from '../features/shop/shopStatic'
import { AppShell } from '../features/shop/AppShell'
import { HeroBannerCarousel } from '../features/shop/HeroBannerCarousel'
import { CategoryShortcuts } from '../features/shop/CategoryShortcuts'
import { ProductGrid } from '../features/shop/ProductGrid'
import { RankingSection } from '../features/shop/RankingSection'
import { RecentlyViewed } from '../features/shop/RecentlyViewed'

/**
 * 쇼핑 홈(메인, `/shop`) — `GET /api/products`를 **1회만** 호출해 그리드·랭킹·최근본상품이 같은
 * 결과를 공유한다(STORY "데이터" 절, 섹션마다 재요청하지 않음). 로그인 없이 접근 가능해야 한다
 * (확정 답변 — 비로그인이 정상 상태). 컨테이너라 스토리 대상이 아니다(규칙 `story-per-component`
 * 제외 — `pages/`).
 */
export default function ShopHomePage() {
  const navigate = useNavigate()
  const [session, setSession] = useState<SessionResult | null>(() => loadSession())
  const [cartItemCount, setCartItemCount] = useState(0)
  const [searchValue, setSearchValue] = useState('')

  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [recentlyViewedSkus, setRecentlyViewedSkus] = useState<string[]>(() => loadRecentlyViewedSkus())

  // round1 QA 권고 6(low) — [다시 시도] 연타 시 늦게 도착한 응답이 이기는 문제(last-write-wins) 예방.
  // `inFlightRef`는 ref라 동기적으로 즉시 반영된다(state였다면 연타 사이 setState가 아직 반영 전이라
  // 두 번째 클릭도 통과했을 것) — 첫 호출이 `await` 이전에 이미 true로 세팅해, 응답을 기다리는 동안의
  // 재클릭은 아무 요청도 만들지 않고 조용히 무시된다.
  const inFlightRef = useRef(false)

  const load = async () => {
    if (inFlightRef.current) return
    inFlightRef.current = true
    setLoading(true)
    setError(null)
    try {
      setProducts(await fetchProducts())
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
      setProducts([])
    } finally {
      setLoading(false)
      inFlightRef.current = false
    }
  }

  useEffect(() => { void load() }, [])

  // 세션이 있을 때만 장바구니 수량을 조회한다 — 신규 신원 확인 경로가 아니라 세션의 memberId를
  // 그대로 전달할 뿐이다(STORY "폴백·우회 경로의 자격 판정" 절). 세션이 없으면 호출 자체를 생략하고
  // 0으로 표시한다(추측성 폴백 없음). 조회 실패는 조용히 0으로 둔다 — 배지 하나 때문에 화면 전체를
  // 오류로 만들지 않는다.
  useEffect(() => {
    if (!session?.memberId) { setCartItemCount(0); return }
    let cancelled = false
    fetchCartItemCount(session.memberId)
      .then(count => { if (!cancelled) setCartItemCount(count) })
      .catch(() => { if (!cancelled) setCartItemCount(0) })
    return () => { cancelled = true }
  }, [session?.memberId])

  const handleSelect = (sku: string) => {
    recordViewed(sku)
    setRecentlyViewedSkus(loadRecentlyViewedSkus())
  }

  // 재작업 지시 1 — 검색어가 있으면 `?keyword=`로 실어 이동, 없으면(빈 검색창 제출) 무파라미터로
  // 이동한다(목록 화면은 무파라미터면 appliedKeyword=''로 시작해 전체 목록을 그대로 보여준다).
  const handleSearchSubmit = () => {
    const keyword = searchValue.trim()
    navigate(keyword ? `/shop/products?keyword=${encodeURIComponent(keyword)}` : '/shop/products')
  }

  // round2 재작업 지시 1 — 카테고리 트리·필터 API가 없어(범위 밖 그대로) 라벨을 keyword로 넘기지
  // 않는다(round1 결정이 틀렸음을 확인 — 시드 상품명과 라벨이 겹치지 않아 6/6 빈 결과). keyword
  // 없이 전체 목록을 연다.
  const handleCategorySelect = () => {
    navigate('/shop/products')
  }

  // round1 QA 권고 2(medium/security), 사람 코멘트 2) — clearSession()만 하면 서버의 apiKey·
  // refreshToken이 만료까지 유효했다. 기존 INF-MBR-004(logout)를 먼저 호출해 서버 토큰을 폐기하고,
  // 그 호출이 실패해도(네트워크 오류·5xx) 로컬 세션은 반드시 정리한다 — 로그아웃 자체가 네트워크
  // 장애로 막히면 안 된다(사람 코멘트 원문).
  const handleLogout = () => {
    const apiKey = session?.apiKey
    const clear = () => {
      clearSession()
      setSession(null)
    }
    if (!apiKey) { clear(); return }
    void logout(apiKey).catch(() => {}).finally(clear)
  }

  // 더 이상 판매중이 아니면(현재 목록에 없으면) 조용히 제외한다 — 없음을 지어내지 않는다.
  const recentlyViewedProducts = useMemo(() => {
    const bySku = new Map(products.map(p => [p.sku, p]))
    return recentlyViewedSkus.map(sku => bySku.get(sku)).filter((p): p is Product => !!p)
  }, [recentlyViewedSkus, products])

  return (
    <AppShell session={session} cartItemCount={cartItemCount} searchValue={searchValue}
              onSearchChange={setSearchValue} onSearchSubmit={handleSearchSubmit} onLogout={handleLogout}>
      <div style={{ fontFamily: 'system-ui, sans-serif', color: '#222', padding: '16px 18px 40px' }}>
        <HeroBannerCarousel slides={BANNER_SLIDES} />
        <CategoryShortcuts categories={CATEGORY_SHORTCUTS} onSelect={handleCategorySelect} />
        <section aria-label="추천 상품" style={{ marginTop: 24 }}>
          <h2 style={{ fontSize: 16, marginBottom: 10 }}>추천 상품</h2>
          <ProductGrid rows={products} loading={loading} error={error}
                       onRetry={() => void load()} onSelect={handleSelect} />
        </section>
        <div style={{ marginTop: 32 }}>
          <RankingSection products={products} onSelect={handleSelect} />
        </div>
        <div style={{ marginTop: 32 }}>
          <RecentlyViewed products={recentlyViewedProducts} />
        </div>
      </div>
    </AppShell>
  )
}
