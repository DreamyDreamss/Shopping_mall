// SR-311 — 앱 셸 우측 레일(≥1200px 전용, AC). (1) 최근 본 상품 카드: 그라데이션 헤더 + 최대 3개,
// 0개면 카드 자체를 렌더하지 않는다. (2) 보유 쿠폰/주문 내역 수 카드: `entitlementCounts`가 없거나
// 둘 다 0이면 렌더하지 않는다 — 이번 SR은 이 prop을 아무도 채우지 않는다(쿠폰은 SR-276, 주문 수는
// 기존 주문 도메인 몫, STORY "범위 밖"). 둘 다 없으면 컴포넌트 자체가 아무것도 그리지 않는다.
import { useEffect, useRef, useState } from 'react'
import { fetchProducts } from '../../api'
import type { Product } from '../../types'
import { loadRecentlyViewedSkus } from './recentlyViewedStorage'

export interface EntitlementCounts {
  coupons: number
  orders: number
}

export interface RightRailProps {
  entitlementCounts?: EntitlementCounts
}

const MAX_RECENT = 3

const railStyle: React.CSSProperties = {
  width: 220, flexShrink: 0, display: 'flex', flexDirection: 'column', gap: 'var(--space-5)',
  paddingTop: 'var(--space-6)',
}

const cardStyle: React.CSSProperties = {
  border: '1px solid var(--color-surface-1)', borderRadius: 'var(--radius-md)', overflow: 'hidden', background: '#fff',
}

const headerStyle: React.CSSProperties = {
  background: 'var(--gradient-accent)', color: '#fff', padding: 'var(--space-3) var(--space-4)',
  fontSize: 'var(--text-sm)', fontWeight: 'var(--font-weight-bold)',
}

const rowStyle: React.CSSProperties = {
  padding: 'var(--space-3) var(--space-4)', fontSize: 'var(--text-xs)', color: 'var(--color-text)',
  borderTop: '1px solid var(--color-surface-2)',
}

export function RightRail({ entitlementCounts }: RightRailProps) {
  const [products, setProducts] = useState<Product[]>([])
  // React 19 StrictMode(dev)가 이 effect를 두 번 실행할 수 있다 — `ShopHomePage.load`/`CartPage.load`와
  // 동일한 `inFlightRef` 가드로 막는다(신규 함정 아님, 기존 관례 재사용, STORY "프레임워크 실행 모델
  // 함정" 절). 화면 본문이 이미 같은 API를 호출하는 것과는 별개 호출이라 화면당 GET이 1회 더 생기지만
  // 트래픽은 미미하다(STORY "데이터" 절, 범위 밖 — 공유 최적화는 후속 과제).
  const inFlightRef = useRef(false)

  useEffect(() => {
    // round2 재작업 지시 4 — 최근 본 sku가 없으면(최초 방문·비로그인 등) 애초에 `fetchProducts()`를
    // 부르지 않는다. 이전에는 카드가 결국 숨어도(0개) GET이 이미 나간 뒤였고, 이 SR로 셸이 붙은
    // LoginPage/PasswordResetPage처럼 원래 상품 API를 전혀 부르지 않던 화면에서도 호출이 생겼다.
    if (loadRecentlyViewedSkus().length === 0) return
    if (inFlightRef.current) return
    inFlightRef.current = true
    fetchProducts()
      .then(setProducts)
      .catch(() => setProducts([]))
      .finally(() => { inFlightRef.current = false })
  }, [])

  const recentSkus = loadRecentlyViewedSkus()
  const bySku = new Map(products.map(p => [p.sku, p]))
  const recentProducts = recentSkus
    .map(sku => bySku.get(sku))
    .filter((p): p is Product => !!p)
    .slice(0, MAX_RECENT)

  const showEntitlement = !!entitlementCounts && (entitlementCounts.coupons > 0 || entitlementCounts.orders > 0)

  if (recentProducts.length === 0 && !showEntitlement) return null

  return (
    <aside aria-label="사이드 정보" style={railStyle}>
      {recentProducts.length > 0 && (
        <div style={cardStyle}>
          <div style={headerStyle}>최근 본 상품</div>
          <div>
            {recentProducts.map(p => (
              <div key={p.sku} style={rowStyle}>{p.productName}</div>
            ))}
          </div>
        </div>
      )}
      {showEntitlement && (
        <div style={cardStyle}>
          <div style={{ padding: 'var(--space-4)', display: 'flex', justifyContent: 'space-around', fontSize: 'var(--text-sm)' }}>
            <span>보유 쿠폰 {entitlementCounts!.coupons}장</span>
            <span>주문 내역 {entitlementCounts!.orders}건</span>
          </div>
        </div>
      )}
    </aside>
  )
}
