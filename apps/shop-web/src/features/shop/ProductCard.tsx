// SR-302 — 상품 카드(썸네일·상품명·판매가·정가·할인율·품절 배지).
// SR-306 — 정가·이미지는 이제 `product.listPrice`/`product.imageUrl`에서 직접 파생한다(표시 전용
// 필드, 값 없으면 자동으로 감춤). 할인율은 저장하지 않고 카드에서 내림 정수 %로 계산한다.
// SR-306.1 round2(재작업) — 할인율·취소선 표시 조건 계산을 `./discountRate`로 옮겼다(정수 연산 —
// round1의 `Math.floor((1 - price/listPrice) * 100)`은 부동소수점 오차로 17개 비율이 1%p 낮게
// 나왔다). 스토리·테스트도 같은 함수를 쓴다 — 계산식을 여기 복제하지 않는다.
import { useState } from 'react'
import type { Product } from '../../types'
import { calcDiscountRate, hasListPriceDiscount } from './discountRate'

export interface ProductCardProps {
  product: Product
  onSelect?: (sku: string) => void
}

const won = (n: number) => n.toLocaleString('ko-KR') + '원'

export function ProductCard({ product, onSelect }: ProductCardProps) {
  const soldOut = product.stockQty <= 0
  const initial = product.productName.trim().charAt(0) || '?'
  const [imgLoadFailed, setImgLoadFailed] = useState(false)

  const hasDiscount = hasListPriceDiscount(product.price, product.listPrice)
  const discountRate = calcDiscountRate(product.price, product.listPrice)
  const showImage = !!product.imageUrl && !imgLoadFailed

  return (
    <div role={onSelect ? 'button' : undefined} tabIndex={onSelect ? 0 : undefined}
         onClick={onSelect ? () => onSelect(product.sku) : undefined}
         onKeyDown={onSelect ? e => { if (e.key === 'Enter' || e.key === ' ') onSelect(product.sku) } : undefined}
         style={{
           border: '1px solid #eef0f2', borderRadius: 8, overflow: 'hidden',
           cursor: onSelect ? 'pointer' : 'default', opacity: soldOut ? 0.55 : 1,
           background: '#fff', fontFamily: 'system-ui, sans-serif',
         }}>
      <div style={{
        aspectRatio: '1 / 1', background: '#f2f3f5', position: 'relative',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        {showImage ? (
          <img src={product.imageUrl!} alt={product.productName} onError={() => setImgLoadFailed(true)}
               style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        ) : (
          <span aria-label={`${product.productName} 대표이미지 없음`}
                style={{ fontSize: 30, fontWeight: 700, color: '#aab0b8' }}>
            {initial}
          </span>
        )}
        {soldOut && (
          <span style={{
            position: 'absolute', top: 8, left: 8, background: '#333', color: '#fff',
            fontSize: 11, fontWeight: 600, borderRadius: 4, padding: '2px 6px',
          }}>
            품절
          </span>
        )}
      </div>
      <div style={{ padding: '10px 10px 12px' }}>
        <div style={{ fontSize: 13, color: '#222', marginBottom: 6, minHeight: 34, overflow: 'hidden' }}>
          {product.productName}
        </div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, flexWrap: 'wrap' }}>
          {discountRate > 0 && (
            <span style={{ color: '#e0392c', fontSize: 13, fontWeight: 700 }}>{discountRate}%</span>
          )}
          <span style={{ fontSize: 14, fontWeight: 700 }}>{won(product.price)}</span>
        </div>
        {hasDiscount && (
          <div style={{ fontSize: 12, color: '#999', textDecoration: 'line-through' }}>{won(product.listPrice!)}</div>
        )}
      </div>
    </div>
  )
}
