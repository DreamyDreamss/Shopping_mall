// SR-302 — 랭킹(탭 전환: 인기·가격, 순위 숫자 1~5).
import { useState } from 'react'
import type { Product } from '../../types'
import { ProductCard } from './ProductCard'

/**
 * 가정값 — 랭킹 탭별 노출 개수(5)·가격 랭킹 정렬 방향(오름차순)은 SR-302 확정 문답에 없다
 * ("추천·랭킹 알고리즘 없음, 정렬 기준은 응답 순서와 가격으로 단순 계산"만 확정). 구현 계획에서
 * 정한 값이다(계획 확인 게이트, 사람 수정 승인) — 가격 랭킹을 내림차순으로 바꾸려면
 * `sortByPriceAscending`만 고치면 된다.
 */
export const RANKING_ITEM_COUNT = 5

function sortByPriceAscending(products: Product[]): Product[] {
  return [...products].sort((a, b) => a.price - b.price)
}

type RankingTab = 'popular' | 'price'

export interface RankingSectionProps {
  products: Product[]
  onSelect?: (sku: string) => void
}

export function RankingSection({ products, onSelect }: RankingSectionProps) {
  const [tab, setTab] = useState<RankingTab>('popular')

  // '인기' = 응답 순서 그대로 상위 N, '가격' = 가격 오름차순 상위 N(확정 답변 — 알고리즘 없음).
  const ranked = tab === 'popular'
    ? products.slice(0, RANKING_ITEM_COUNT)
    : sortByPriceAscending(products).slice(0, RANKING_ITEM_COUNT)

  return (
    <section aria-label="랭킹">
      <h2 style={{ fontSize: 16, marginBottom: 10 }}>랭킹</h2>
      <div role="tablist" aria-label="랭킹 탭" style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        {([['popular', '인기'], ['price', '가격']] as const).map(([key, label]) => (
          <button key={key} type="button" role="tab" aria-selected={tab === key} onClick={() => setTab(key)}
                  style={{
                    border: '1px solid #d5d8dd', borderRadius: 5, padding: '5px 14px', fontSize: 13,
                    cursor: 'pointer', background: tab === key ? '#0b4ea2' : '#fff',
                    color: tab === key ? '#fff' : '#333', fontWeight: tab === key ? 700 : 400,
                  }}>
            {label}
          </button>
        ))}
      </div>
      {!ranked.length ? (
        <div style={{ padding: 14, color: '#666' }}>표시할 상품이 없습니다</div>
      ) : (
        <ol style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', gap: 12, overflowX: 'auto' }}>
          {ranked.map((p, i) => (
            <li key={p.sku} style={{ minWidth: 150, display: 'flex', flexDirection: 'column', gap: 6 }}>
              {/* round1 QA 권고 5(low), 사람 코멘트 6) — aria-hidden이라 스크린리더에 순위 정보가
                  전달되지 않았다. 제거해 순위 숫자 자체를 보조기술에도 노출한다(확정문답의 "순위
                  숫자" 요건). */}
              <span style={{ fontSize: 18, fontWeight: 800, color: '#0b4ea2' }}>{i + 1}</span>
              <ProductCard product={p} onSelect={onSelect} />
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}
