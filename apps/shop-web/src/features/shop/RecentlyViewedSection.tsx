// SR-357 — 마이페이지 최근 본 상품(`/shop/mypage/recently-viewed`) 목록. `ProductReviewsSection.tsx`의
// controlled-props 패턴(listStatus 분기, 자체 fetch 없음 — 규칙 `web-fetch-only-in-api`)을 그대로 재사용
// — 불러오는 중/목록 있음/빈 목록/오류 4상태를 이 파일 하나에서 배치한다(파일명=컴포넌트명, 규칙
// `one-component-per-file`). 목록 조회·재시도 실행은 전부 부모(`RecentlyViewedPage`)가 한다.
//
// 재작업(round 2, QA FAIL 필수1) — 목록 행에 대표 이미지(imageUrl)가 빠져 있었다(AC·구현계획 테스트
// 4번이 명시했으나 소비처가 없었음). `ProductCard.tsx:25,41` / `CartLineItem.tsx:67,94`의 기존 패턴
// (`showImage = !!imageUrl && !imgLoadFailed`, `<img onError>`, 실패·null이면 이니셜 플레이스홀더)을
// 그대로 재사용한다 — `imgLoadFailed`는 행마다 독립이어야 하므로(훅은 최상위에서만, 반복문 안에서
// 직접 호출 불가) 내보내지 않는 보조 컴포넌트 `RecentlyViewedRow`로 분리한다(규칙
// `one-component-per-file`은 "내보내지 않는" 보조 컴포넌트를 허용).
import { useState } from 'react'
import type { RecentlyViewedItem } from '../../types'

export type RecentlyViewedStatus = 'loading' | 'ready' | 'error'

export interface RecentlyViewedSectionProps {
  status: RecentlyViewedStatus
  items: RecentlyViewedItem[]
  onRetry: () => void
}

const won = (n: number) => n.toLocaleString('ko-KR') + '원'

const rowStyle: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: 12, padding: '14px 4px', borderBottom: '1px solid #eef0f2',
}
const thumbBoxStyle: React.CSSProperties = {
  width: 64, height: 64, background: '#f2f3f5', borderRadius: 6, overflow: 'hidden', flex: '0 0 auto',
  display: 'flex', alignItems: 'center', justifyContent: 'center',
}
const soldOutBadgeStyle: React.CSSProperties = {
  display: 'inline-block', background: '#eef0f2', color: '#666', borderRadius: 4, fontSize: 10.5,
  fontWeight: 700, padding: '1px 6px', marginLeft: 8,
}
const alertBoxStyle: React.CSSProperties = {
  border: '1px solid #e0b4b4', background: '#fff6f6', color: '#912d2b', borderRadius: 6, padding: '16px 18px',
  display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'flex-start', fontSize: 12.5,
}

function RecentlyViewedRow({ it }: { it: RecentlyViewedItem }) {
  const [imgLoadFailed, setImgLoadFailed] = useState(false)
  const showImage = !!it.imageUrl && !imgLoadFailed
  const initial = it.productName.trim().charAt(0) || '?'

  return (
    <div role="listitem" style={rowStyle}>
      <div style={thumbBoxStyle}>
        {showImage ? (
          <img src={it.imageUrl!} alt={it.productName} onError={() => setImgLoadFailed(true)}
               style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        ) : (
          <span aria-label={`${it.productName} 대표이미지 없음`} style={{ fontSize: 22, fontWeight: 700, color: '#aab0b8' }}>
            {initial}
          </span>
        )}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <a href={`#/shop/products/${encodeURIComponent(it.sku)}`}
           style={{ fontSize: 13.5, color: '#222', textDecoration: 'none' }}>
          {it.productName}
        </a>
        {it.soldOut && <span style={soldOutBadgeStyle}>품절</span>}
        <div style={{ fontSize: 13, color: '#333', marginTop: 4, fontWeight: 700 }}>{won(it.price)}</div>
      </div>
    </div>
  )
}

export function RecentlyViewedSection({ status, items, onRetry }: RecentlyViewedSectionProps) {
  if (status === 'loading') {
    return <div role="status" style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>불러오는 중…</div>
  }

  if (status === 'error') {
    return (
      <div role="alert" style={alertBoxStyle}>
        <span>불러오지 못했습니다</span>
        <button type="button" onClick={onRetry}
                style={{ border: '1px solid #912d2b', borderRadius: 5, background: '#fff', color: '#912d2b',
                         fontSize: 12.5, padding: '5px 12px', cursor: 'pointer' }}>
          다시 시도
        </button>
      </div>
    )
  }

  if (items.length === 0) {
    return (
      <div style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>
        <p>최근 본 상품이 없습니다</p>
      </div>
    )
  }

  return (
    <div role="list" aria-label="최근 본 상품">
      {items.map(it => <RecentlyViewedRow key={it.sku} it={it} />)}
    </div>
  )
}
