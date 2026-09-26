// SR-351 — 상품평 삭제 버튼 노출 판정(이 브라우저에서 만든 리뷰만, STORY "본인 리뷰 식별" 절 결정).
// 목록 API(INF-ORD-038) 응답에 memberId가 없어(pii_format 확정 답변상 원본 이름조차 안 줌) 서버 응답만
// 으로는 "이 리뷰가 내 것인지" 판정할 수 없다. `recentlyViewedStorage.ts`와 동일한 localStorage 관례.
// 회원별로 키를 분리해(`sl.shop.myReviews.<memberId>`) 같은 브라우저를 여러 회원이 쓸 때 다른 회원이
// 만든 리뷰에 삭제 버튼이 잘못 뜨지 않게 한다 — 실제 소유권은 서버가 매 요청마다 재검증하므로 이 표시가
// 틀려도(다른 기기에서 예전에 쓴 리뷰는 안 뜸) 보안 구멍이 아니라 기능 제약일 뿐이다.
function storageKey(memberId: string): string {
  return `sl.shop.myReviews.${memberId}`
}

function loadOwnedReviewIds(memberId: string): number[] {
  const raw = localStorage.getItem(storageKey(memberId))
  if (!raw) return []
  try {
    const parsed: unknown = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.filter((n): n is number => typeof n === 'number') : []
  } catch {
    return []
  }
}

/** 작성 성공 직후 이 브라우저에 "내 것" 기록을 남긴다(중복 기록 없음). */
export function markReviewOwned(memberId: string, reviewId: number): void {
  if (!memberId) return
  const ids = loadOwnedReviewIds(memberId)
  if (ids.includes(reviewId)) return
  localStorage.setItem(storageKey(memberId), JSON.stringify([...ids, reviewId]))
}

/** 이 브라우저가 "내 것"으로 기록한 리뷰인지 — 다른 기기/브라우저에서 쓴 리뷰는 기록이 없어 false다. */
export function isReviewOwned(memberId: string, reviewId: number): boolean {
  if (!memberId) return false
  return loadOwnedReviewIds(memberId).includes(reviewId)
}
