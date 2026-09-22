// SR-302 — 쇼핑 홈(메인). "최근 본 상품"은 타 도메인 테이블·집계에 파급이 없어(확정 답변 db_ripple)
// DB가 아니라 브라우저 저장(localStorage)에 둔다. `session.ts`(`sl.member.session`)와 다른 키를
// 써서 세션 데이터와 섞이지 않는다.
const STORAGE_KEY = 'sl.shop.recentlyViewed'

/** 최대 보관 개수 — 오래된 것부터 밀려난다. */
const MAX_ITEMS = 8

/**
 * 상품을 "최근 본 상품"에 기록한다 — 같은 sku를 다시 보면 맨 앞으로 옮기고 중복은 만들지 않는다
 * (dedupe). 최대 개수를 넘으면 가장 오래된 항목부터 제거한다.
 */
export function recordViewed(sku: string): void {
  if (!sku) return
  const rest = loadRecentlyViewedSkus().filter(s => s !== sku)
  const next = [sku, ...rest].slice(0, MAX_ITEMS)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
}

/** 최근순(맨 앞이 가장 최근)으로 sku 목록을 읽는다. 기록이 없거나 손상됐으면 빈 배열. */
export function loadRecentlyViewedSkus(): string[] {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return []
  try {
    const parsed: unknown = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.filter((s): s is string => typeof s === 'string') : []
  } catch {
    return []
  }
}
