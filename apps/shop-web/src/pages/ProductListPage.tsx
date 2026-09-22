// SR-303 — 상품 목록(검색·필터, `/shop/products`) 컨테이너.
// SR-311 round4 재작업 지시 2(round3 QA FAIL 필수수정 2) — `Gnb`의 검색 아이콘이 빈 검색어로 이
// 화면에 도착시킬 때 `navigate('/shop/products', { state: { focusSearch: true } })`를 쓴다(`Gnb.tsx`).
// "도착 화면"인 이 페이지가 그 신호의 소비자다 — 마운트 시 `location.state.focusSearch`를 보고
// `AppShell`(→`Gnb`)에 `autoFocusSearch`를 내려 검색 입력을 포커스시키고, 곧바로 `replace` 이동으로
// state를 소거한다(뒤로가기로 이 화면에 돌아와도 다시 포커스되지 않게).
import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { fetchCartItemCount, fetchProducts, logout } from '../api'
import { clearSession, loadSession } from '../session'
import type { Product, SessionResult } from '../types'
import { AppShell } from '../features/shop/AppShell'
import { ProductFilterBar } from '../features/shop/ProductFilterBar'
import { ProductListGrid } from '../features/shop/ProductListGrid'
import { ProductPagination } from '../features/shop/ProductPagination'
import { PAGE_SIZE, filterByPriceRange, resolveEmptyReason, sortProducts, type SortKey } from '../features/shop/productListFilters'

/**
 * `/shop/products` — 검색(Gnb)·필터(재고·가격대)·정렬·페이지를 이 컨테이너가 총괄한다. 파생 순서는
 * 항상 원본 `products`(서버 응답) → `filterByPriceRange` → `sortProducts` → 페이지 slice 순으로
 * 고정한다(STORY "데이터" 절). 페이지라 스토리 대상이 아니다(규칙 `story-per-component` 제외 —
 * `src/pages/`). `ShopHomePage`의 세션/장바구니/로그아웃 로직은 훅으로 추출하지 않고(여러 라운드
 * QA를 거친 안정 코드라 리팩터링하지 않는다는 STORY "범위 밖" 결정) 이 파일에 최소 분량만 복제한다.
 */
export default function ProductListPage() {
  const navigate = useNavigate()
  const location = useLocation()
  // 재작업 지시 1 — Gnb 검색 제출·CategoryShortcuts 클릭이 검색어를 `?keyword=`로 들고 이 화면에
  // 진입한다(`ShopHomePage.tsx`). 최초 마운트 1회만 읽는다 — 이후 이 화면 안에서의 검색 제출은
  // URL을 다시 쓰지 않고 내부 상태(`appliedKeyword`)로만 처리한다(기존 동작 그대로).
  const [searchParams] = useSearchParams()
  const initialKeyword = () => searchParams.get('keyword')?.trim() ?? ''

  // round4 재작업 지시 2 — `Gnb`가 빈 검색어로 이 화면에 이동시킬 때 실어 보내는 신호(위 파일 상단
  // 주석). 최초 렌더에서만 읽는다 — 소거 이펙트(아래)가 곧바로 state를 비운다.
  const focusSearchOnMount = Boolean((location.state as { focusSearch?: boolean } | null)?.focusSearch)

  const [session, setSession] = useState<SessionResult | null>(() => loadSession())
  const [cartItemCount, setCartItemCount] = useState(0)
  const [searchValue, setSearchValue] = useState(initialKeyword)

  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // 서버에 실제로 반영되는 조건 — 이 둘이 바뀔 때만 재요청한다(STORY "데이터" 절).
  const [appliedKeyword, setAppliedKeyword] = useState(initialKeyword)
  const [inStockOnly, setInStockOnly] = useState(false)
  // 클라이언트 전용 상태 — 재요청 없이 이미 받은 목록 안에서만 계산한다.
  const [priceMin, setPriceMin] = useState('')
  const [priceMax, setPriceMax] = useState('')
  const [sortKey, setSortKey] = useState<SortKey>('recommend')
  const [page, setPage] = useState(1)

  // 프레임워크 실행 모델 함정(STORY 절) — React 19 StrictMode(dev)가 마운트 effect를 2번 실행해
  // 같은 파라미터(''+false)로 두 번째 요청이 나갈 수 있다(`ShopHomePage`의 `inFlightRef`와 같은
  // 위험). 다만 이 화면은 검색·재고 토글처럼 "서로 다른 파라미터의 요청 두 개가 겹쳐 나갈" 수도
  // 있어(연속 검색 제출, 빠른 토글) 단순 boolean 가드로는 부족하다 — 그 경우엔 겹쳐 나가는 것 자체는
  // 막지 않고 응답 적용 순서만 세대 카운터(`requestIdRef`)로 바로잡는다.
  // `inFlightKeyRef`는 "같은 파라미터의 요청이 이미 진행 중일 때만" 새 호출을 억제한다(같은 종류
  // 재시도만 억제 — [다시 시도] 연타·StrictMode 이중 마운트가 여기 해당) — 파라미터가 다른 요청은
  // 억제하지 않고 그대로 내보낸다.
  const inFlightKeyRef = useRef<string | null>(null)
  const requestIdRef = useRef(0)

  const load = async (keyword: string, inStock: boolean) => {
    const key = keyword + '|' + inStock
    if (inFlightKeyRef.current === key) return
    inFlightKeyRef.current = key
    const id = ++requestIdRef.current
    setLoading(true)
    setError(null)
    try {
      const data = await fetchProducts(keyword || undefined, inStock || undefined)
      if (id !== requestIdRef.current) return // 더 최신 요청이 이미 나간 뒤라 이 응답은 버린다(순서 역전 방지).
      setProducts(data)
    } catch (e) {
      if (id !== requestIdRef.current) return
      setError(e instanceof Error ? e.message : String(e))
      setProducts([])
    } finally {
      if (inFlightKeyRef.current === key) inFlightKeyRef.current = null
      if (id === requestIdRef.current) setLoading(false)
    }
  }

  useEffect(() => { void load(appliedKeyword, inStockOnly) }, [appliedKeyword, inStockOnly])

  // round4 재작업 지시 2 — 도착 시 딱 한 번 포커스 신호를 소비한 뒤 state를 소거한다(`replace`라
  // 히스토리 엔트리를 새로 쌓지 않는다 — 뒤로가기로 이 화면에 돌아오면 location.state는 이미 비어
  // 있어 다시 포커스되지 않는다). 같은 라우트로의 replace라 컴포넌트는 재마운트되지 않고
  // `location.search`만 유지한 채 state만 비워진다.
  useEffect(() => {
    if (focusSearchOnMount) {
      navigate(location.pathname + location.search, { replace: true })
    }
    // eslint: 마운트 시 1회만 — location이 이 effect 안에서 바뀌므로(재실행 방지) 의존성을 비워 둔다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 세션이 있을 때만 장바구니 수량을 조회한다(`ShopHomePage`와 동일 규칙 — 신규 신원 확인 경로 아님,
  // STORY "폴백·우회 경로의 자격 판정" 절). 조회 실패는 조용히 0으로 둔다.
  useEffect(() => {
    if (!session?.memberId) { setCartItemCount(0); return }
    let cancelled = false
    fetchCartItemCount(session.memberId)
      .then(count => { if (!cancelled) setCartItemCount(count) })
      .catch(() => { if (!cancelled) setCartItemCount(0) })
    return () => { cancelled = true }
  }, [session?.memberId])

  // `ShopHomePage.handleLogout`과 동일 로직(복제, 훅 추출하지 않음 — STORY "범위 밖" 결정).
  const handleLogout = () => {
    const apiKey = session?.apiKey
    const clear = () => { clearSession(); setSession(null) }
    if (!apiKey) { clear(); return }
    void logout(apiKey).catch(() => {}).finally(clear)
  }

  const handleSearchSubmit = () => {
    setAppliedKeyword(searchValue)
    setPage(1)
  }

  const handleClearKeyword = () => {
    setSearchValue('')
    setAppliedKeyword('')
    setPage(1)
  }

  const handleResetFilters = () => {
    // 재고·가격대만 리셋한다 — keyword는 유지(확정 문답, "필터 초기화"와 "검색어 지우기"는 별개 버튼).
    setInStockOnly(false)
    setPriceMin('')
    setPriceMax('')
    setPage(1)
  }

  const handleInStockOnlyChange = (value: boolean) => { setInStockOnly(value); setPage(1) }
  const handlePriceMinChange = (value: string) => { setPriceMin(value); setPage(1) }
  const handlePriceMaxChange = (value: string) => { setPriceMax(value); setPage(1) }
  const handleSortKeyChange = (value: SortKey) => { setSortKey(value); setPage(1) }

  // 파생 순서 고정: 원본 products → 가격대 필터 → 정렬 → 페이지 slice.
  const filtered = useMemo(() => filterByPriceRange(products, priceMin, priceMax), [products, priceMin, priceMax])
  const sorted = useMemo(() => sortProducts(filtered, sortKey), [filtered, sortKey])
  const totalPages = Math.max(1, Math.ceil(sorted.length / PAGE_SIZE))
  const pageRows = sorted.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  const hasActiveFilter = inStockOnly || !!priceMin.trim() || !!priceMax.trim()
  // 결과 개수 텍스트·빈 사유 판정은 정렬/페이지 적용 전(가격 필터까지 적용한) 배열 길이를 쓴다.
  const emptyReason = loading || error ? null : resolveEmptyReason(filtered.length, appliedKeyword, hasActiveFilter)

  // 카드 클릭은 상세 경로로 navigate만 한다(그 경로의 <Route>는 SR-304 몫, 확정 답변 scope_freeze).
  // 이 화면은 recordViewed(최근 본 상품 기록)를 호출하지 않는다(확정 문답에 없음, `ShopHomePage`
  // 전용 사이드이펙트를 과잉 복제하지 않는다 — STORY "순서·보안" 절).
  const handleSelect = (sku: string) => {
    navigate('/shop/products/' + encodeURIComponent(sku))
  }

  return (
    <AppShell session={session} cartItemCount={cartItemCount} searchValue={searchValue}
              onSearchChange={setSearchValue} onSearchSubmit={handleSearchSubmit} onLogout={handleLogout}
              autoFocusSearch={focusSearchOnMount}>
      <div style={{ fontFamily: 'system-ui, sans-serif', color: '#222', padding: '16px 18px 40px' }}>
        <h1 style={{ fontSize: 18, marginBottom: 14 }}>상품 목록</h1>
        <ProductFilterBar
          inStockOnly={inStockOnly} onInStockOnlyChange={handleInStockOnlyChange}
          priceMin={priceMin} onPriceMinChange={handlePriceMinChange}
          priceMax={priceMax} onPriceMaxChange={handlePriceMaxChange}
          sortKey={sortKey} onSortKeyChange={handleSortKeyChange}
          resultCount={filtered.length}
        />
        <section aria-label="상품 목록" style={{ marginTop: 16 }}>
          <ProductListGrid rows={pageRows} loading={loading} error={error} emptyReason={emptyReason}
                            onRetry={() => void load(appliedKeyword, inStockOnly)}
                            onSelect={handleSelect}
                            onClearKeyword={handleClearKeyword}
                            onResetFilters={handleResetFilters} />
        </section>
        {!loading && !error && sorted.length > 0 && (
          <div style={{ marginTop: 20, display: 'flex', justifyContent: 'center' }}>
            <ProductPagination page={page} totalPages={totalPages}
                                onPrev={() => setPage(p => Math.max(1, p - 1))}
                                onNext={() => setPage(p => Math.min(totalPages, p + 1))} />
          </div>
        )}
      </div>
    </AppShell>
  )
}
