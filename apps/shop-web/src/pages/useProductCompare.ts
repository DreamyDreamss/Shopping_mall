// SR-360 — 상품 상세(`/shop/products/:sku`) 비교함 토글 전용 훅. `useProductWish.ts`와 동일한
// "페이지 전용 훅"(컴포넌트가 아니라 규칙 `story-per-component` 대상이 아니다). INF-ORD-047이
// 무페이지 단일 GET이라 `useProductWish`의 "최대 10페이지 순차 조회" 루프는 필요 없다 —
// `fetchCompares` 1회 호출 → `items.some(it => it.sku === sku)`로 `compared`를 정한다. 그래도
// "확인 요청이 진행 중인 동안 사용자가 토글하면 늦게 도착한 확인 결과가 토글을 덮어쓴다"는 경쟁은
// 페이지 루프가 없어도 여전히 성립하므로(단일 fetch도 비동기라 토글과 순서가 뒤집힐 수 있음),
// `useProductWish`의 `wishGenerationRef`와 동일한 `compareGenerationRef` 패턴을 그대로 적용한다
// (세대 불일치 시 결과 폐기, 실패 사례집 SR-356 #2 round1 대조). Hooks-rule(must): 아래
// useState/useEffect/useRef는 전부 최상위 호출, 조건부 호출 없음.
import { useEffect, useRef, useState } from 'react'
import { addCompare, fetchCompares, removeCompare } from '../api'
import { toDisplayMessage } from '../features/shop/httpErrorMessage'
import type { CompareToggleStatus } from '../features/shop/ProductInfoPanel'
import type { ProductDetail, SessionResult } from '../types'

export interface UseProductCompareOptions {
  sku: string
  session: SessionResult | null
  /** 현재 로드된 상품 — 토글 대상 sku 확정에 쓴다. null이면(로딩/오류) 토글은 아무 일도 하지 않는다
   * (`useProductWish`와 동일 원칙). */
  product: ProductDetail | null
  /** 세션이 없을 때 호출 — 로그인 화면 이동은 호출자(`ProductDetailPage`) 몫이다(신규 리다이렉트
   * 규약을 만들지 않는다, `useProductWish.onRequireLogin`과 동일 원칙). 로컬 상태는 건드리지 않은
   * 채 반환한다. */
  onRequireLogin: () => void
}

export function useProductCompare({ sku, session, product, onRequireLogin }: UseProductCompareOptions) {
  const [compared, setCompared] = useState(false)
  const [compareStatus, setCompareStatus] = useState<CompareToggleStatus>('idle')
  const [compareErrorMessage, setCompareErrorMessage] = useState<string | null>(null)
  // 연타 방지(`useProductWish.wishInFlightRef`와 동일 원리) — 동기 잠금.
  const compareInFlightRef = useRef(false)
  // 확인 effect와 토글 둘 다 이 값을 올린다(`useProductWish.wishGenerationRef`와 동일 원칙 — 위 문서
  // 주석 참고).
  const compareGenerationRef = useRef(0)

  // 새 상품이 로드되면 로컬 상태를 리셋한다 — 호출자(`ProductDetailPage.load()`)가 `setProduct(data)`와
  // 같은 동기 블록에서 직접 호출한다(`wish.resetForProduct`와 동일 시점, 렌더 지연 없음 원칙).
  // `compared` 자체는 아래 별도 effect가 정한다(`useProductWish`의 `wished`와 동일 분리).
  const resetForProduct = () => {
    setCompareStatus('idle')
    setCompareErrorMessage(null)
    compareInFlightRef.current = false
  }

  // 비교함 담김 여부 확인 — 로그인이면 `fetchCompares` 1회로 이 sku가 있는지 판정한다(무페이지, AC
  // "화면 진입 시 로그인이면 GET…로 판단"). 비로그인이면 API를 아예 호출하지 않고 compared=false로
  // 고정(판단 자체를 안 함, `useProductWish`와 동일 게이트). 전부 GET(멱등)이라 StrictMode 이중 실행이
  // 있어도 화면 정합성엔 무해하다 — 다만 진행 중에 사용자가 토글하면(세대 불일치, 위 문서 주석) 이전
  // 결과로 compared를 덮어쓰지 않는다.
  useEffect(() => {
    if (!session?.memberId || !sku) { setCompared(false); return }
    const generation = ++compareGenerationRef.current
    let cancelled = false
    fetchCompares(session.apiKey)
      .then(items => {
        if (cancelled || generation !== compareGenerationRef.current) return
        setCompared(items.some(it => it.sku === sku))
      })
      .catch(() => {
        // 조회 실패 — compared는 건드리지 않는다(에러 표시 없이 조용한 기본값, `useProductWish`와 동일).
      })
    return () => { cancelled = true }
  }, [session?.memberId, sku])

  // 비교함 토글. 순서(STORY "순서·보안" 2): ①연타 방지(ref) ②세션 존재를 가장 먼저 확인 — 없으면
  // 어떤 로컬 상태도 바꾸지 않고 즉시 로그인 화면으로 이동(AC "버튼을 누를 때 로그인 안내") ③product가
  // 없으면 아무 것도 안 함 ④세대 카운터 증가(진행 중이던 확인 결과를 무효화) ⑤낙관적 토글 + pending
  // ⑥addCompare/removeCompare 호출 ⑦성공 시 idle, 실패 시 이전 값으로 롤백 + error +
  // toDisplayMessage(`useProductWish.handleToggleWish`와 완전히 동일한 순서·주석 — 새 순서를 발명하지
  // 않는다).
  const handleToggleCompare = () => {
    if (compareInFlightRef.current) return
    if (!session?.memberId) { onRequireLogin(); return }
    if (!product) return
    compareGenerationRef.current++ // 이 시점부터는 사용자 액션이 compared를 소유한다 — 확인 결과는 버림.
    const prevCompared = compared
    compareInFlightRef.current = true
    setCompared(!prevCompared)
    setCompareStatus('pending')
    setCompareErrorMessage(null)
    const request = prevCompared
      ? removeCompare(session.apiKey, product.sku)
      : addCompare(session.apiKey, product.sku)
    request
      .then(() => { setCompareStatus('idle') })
      .catch(e => {
        setCompared(prevCompared)
        setCompareStatus('error')
        setCompareErrorMessage(toDisplayMessage(e))
      })
      .finally(() => { compareInFlightRef.current = false })
  }

  return { compared, compareStatus, compareErrorMessage, onToggleCompare: handleToggleCompare, resetForProduct }
}

export type UseProductCompareResult = ReturnType<typeof useProductCompare>
