// SR-356 — 내 찜 목록(`/shop/mypage/wishes`) 컨테이너. `MyAddressesPage.tsx`를 그대로 본뜬 구조(순수
// HTTP 클라이언트 오케스트레이션) — 컨테이너라 스토리 대상이 아니다(규칙 `story-per-component` 제외,
// `src/pages/`).
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchCartItemCount, fetchWishes, logout, removeWish } from '../api'
import { clearSession, loadSession } from '../session'
import type { SessionResult, WishItem } from '../types'
import { AppShell } from '../features/shop/AppShell'
import { toDisplayMessage } from '../features/shop/httpErrorMessage'
import { WishList } from '../features/shop/WishList'

const alertBoxStyle: React.CSSProperties = {
  border: '1px solid #e0b4b4', background: '#fff6f6', color: '#912d2b', borderRadius: 6, padding: '10px 14px', fontSize: 12.5,
}

export default function WishListPage() {
  const navigate = useNavigate()

  const [session, setSession] = useState<SessionResult | null>(() => loadSession())
  const [searchValue, setSearchValue] = useState('')
  const [cartItemCount, setCartItemCount] = useState(0)

  const [items, setItems] = useState<WishItem[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [pendingSku, setPendingSku] = useState<string | null>(null)

  const requestIdRef = useRef(0)
  // 해제 연타 방지(`MyAddressesPage.inFlightAddressIdRef`와 동일 원리) — 동시 1건뿐이라 단일 ref로 충분.
  const inFlightSkuRef = useRef<string | null>(null)

  // 비로그인 진입 — `MyAddressesPage`와 동일 리다이렉트 규약(AC 명시 경로, 이번 SR의 유일한 진입점).
  useEffect(() => {
    if (!session?.memberId) {
      navigate('/login?redirect=' + encodeURIComponent('/shop/mypage/wishes'), { replace: true })
    }
  }, [session?.memberId, navigate])

  const load = async (apiKey: string) => {
    const id = ++requestIdRef.current
    setLoading(true)
    setLoadError(null)
    try {
      const rows = await fetchWishes(apiKey, 1)
      if (id !== requestIdRef.current) return
      setItems(rows)
    } catch (e) {
      if (id !== requestIdRef.current) return
      setLoadError(toDisplayMessage(e))
    } finally {
      if (id === requestIdRef.current) setLoading(false)
    }
  }

  useEffect(() => {
    if (!session?.memberId) { setLoading(false); return }
    void load(session.apiKey)
  }, [session?.memberId])

  // GNB 장바구니 배지 — `MyAddressesPage`와 동일하게 기존 `fetchCartItemCount` 재사용(신규 API 없음).
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

  // 찜 해제 — 클릭 즉시 그 행을 로컬에서 제거(낙관적) → 실패 시 그 sku 1건만 원래 자리에 복원
  // (`AddressList.handleDeleteConfirm`과 동일 원리, STORY "순서·보안" 5 — 전체 재조회는 하지 않는다).
  //
  // 재작업(round2, QA FAIL 필수2) — 이전 구현은 클릭 시점의 `items` 스냅샷 전체(`prevItems`)를 실패
  // 롤백 시 그대로 대입했다. A행 해제(실패 예정) → B행 해제(서버 204 성공) 순서로 누르면, A의 롤백이
  // "A·B 둘 다 있던" 스냅샷을 통째로 되돌려 서버에는 이미 없는 B가 화면에 되살아났다(QA 프로브 실측).
  // 고친 방식 — 함수형 갱신(rows => ...)으로 **그 sku 1건만** 복원한다: 제거 시점에 그 행 앞뒤로
  // "원래 뒤에 있던 sku들"을 캡처해 두고, 복원할 때 현재(rows) 배열에서 그 sku들 중 아직 남아 있는
  // 첫 행 앞에 끼워 넣는다(뒤 sku가 전부 이미 사라졌으면 끝에 붙인다) — 그 사이 다른 행의 성공한
  // 해제를 되살리지 않으면서도 원래 상대 위치를 지킨다.
  const handleRemove = (sku: string) => {
    if (!session) return
    if (inFlightSkuRef.current === sku) return
    inFlightSkuRef.current = sku
    setPendingSku(sku)
    setActionError(null)

    let removedItem: WishItem | undefined
    let followingSkus: string[] = []
    setItems(rows => {
      const idx = rows.findIndex(r => r.sku === sku)
      if (idx === -1) return rows
      removedItem = rows[idx]
      followingSkus = rows.slice(idx + 1).map(r => r.sku)
      return rows.filter(r => r.sku !== sku)
    })

    removeWish(session.apiKey, sku)
      .catch(e => {
        const restored = removedItem
        if (restored) {
          setItems(rows => {
            if (rows.some(r => r.sku === sku)) return rows // 이미 있으면(중복 호출 등) 그대로 둔다
            const insertBeforeIdx = rows.findIndex(r => followingSkus.includes(r.sku))
            if (insertBeforeIdx === -1) return [...rows, restored]
            return [...rows.slice(0, insertBeforeIdx), restored, ...rows.slice(insertBeforeIdx)]
          })
        }
        setActionError(toDisplayMessage(e))
      })
      .finally(() => {
        inFlightSkuRef.current = null
        setPendingSku(null)
      })
  }

  return (
    <AppShell session={session} cartItemCount={cartItemCount} searchValue={searchValue}
              onSearchChange={setSearchValue} onSearchSubmit={handleSearchSubmit} onLogout={handleLogout}>
      <div style={{ fontFamily: 'system-ui, sans-serif', color: '#222', padding: '16px 18px 40px' }}>
        <h1 style={{ fontSize: 18, marginBottom: 14 }}>내 찜 목록</h1>
        {!session ? (
          <div role="status" style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>이동 중…</div>
        ) : loading ? (
          <div role="status" style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>불러오는 중…</div>
        ) : loadError ? (
          <div role="alert" style={{ ...alertBoxStyle, padding: '16px 18px', display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'flex-start' }}>
            <span>불러오지 못했습니다 — {loadError}</span>
            <button type="button" onClick={() => void load(session.apiKey)}
                    style={{ border: '1px solid #912d2b', borderRadius: 5, background: '#fff', color: '#912d2b',
                             fontSize: 12.5, padding: '5px 12px', cursor: 'pointer' }}>
              다시 시도
            </button>
          </div>
        ) : (
          <>
            {actionError && <div role="alert" style={{ ...alertBoxStyle, marginBottom: 14 }}>{actionError}</div>}
            <WishList items={items} onRemove={handleRemove} pendingSku={pendingSku} />
          </>
        )}
      </div>
    </AppShell>
  )
}
