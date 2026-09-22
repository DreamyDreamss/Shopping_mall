// SR-304 — 상품 상세 이미지 영역(대표 이미지+썸네일 / 이미지없음 대체).
// 백엔드에 다중 이미지 배열이 없다(`Product.imageUrl` 단일 필드, "응답 필드 추가 요구 안 함" 확정
// 답변) — 썸네일은 같은 이미지 1장을 재사용해 시각적 골격만 채운다(여러 장인 것처럼 지어내지 않는다,
// STORY "데이터" 절). 재작업(round 2) — 이전 버전은 `THUMBNAIL_COUNT=3`으로 같은 이미지를 3번
// 반복 렌더해 "여러 장 있는 것처럼" 보였다(QA 권고 1, 사람 확정 이탈). 썸네일은 실제 이미지가 있을
// 때만 정확히 1개 렌더한다(이미지 수 = 썸네일 수, 0장 또는 1장).
// 재작업(round 2) — `ProductCard.tsx`와 동일한 onError 이미지 폴백을 추가한다(QA 권고 4, 사람 지시
// 4): 실제 URL이 깨져 로드에 실패하면 이니셜 대체 영역으로 떨어진다("이미지없음"과 같은 화면).
import { useEffect, useState } from 'react'

export interface ProductImageGalleryProps {
  imageUrl: string | null
  productName: string
}

const mainBoxStyle: React.CSSProperties = {
  aspectRatio: '1 / 1', background: '#f2f3f5', borderRadius: 8, overflow: 'hidden',
  display: 'flex', alignItems: 'center', justifyContent: 'center',
}

const thumbBoxStyle: React.CSSProperties = {
  width: 64, height: 64, background: '#f2f3f5', borderRadius: 6, overflow: 'hidden', flex: '0 0 auto',
}

export function ProductImageGallery({ imageUrl, productName }: ProductImageGalleryProps) {
  const initial = productName.trim().charAt(0) || '?'
  const [imgLoadFailed, setImgLoadFailed] = useState(false)
  // sku 이동으로 imageUrl이 바뀌면 이전 상품에서 실패했던 상태가 새 상품에 새지 않게 리셋한다
  // (`ProductDetailPage`가 이 부품을 sku 변경 간에 재사용/재마운트하지 않을 수 있어 방어적으로 둔다).
  useEffect(() => { setImgLoadFailed(false) }, [imageUrl])

  const showImage = !!imageUrl && !imgLoadFailed

  if (!showImage) {
    return (
      <div style={mainBoxStyle}>
        <span aria-label={`${productName} 대표이미지 없음`} style={{ fontSize: 40, fontWeight: 700, color: '#aab0b8' }}>
          {initial}
        </span>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={mainBoxStyle}>
        <img src={imageUrl} alt={productName} onError={() => setImgLoadFailed(true)}
             style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <div style={thumbBoxStyle}>
          <img src={imageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        </div>
      </div>
    </div>
  )
}
