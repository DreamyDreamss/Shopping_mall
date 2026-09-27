// SR-356 — 상품 상세(`/shop/products/:sku`) 찜하기(위시리스트) 전용 훅. `useProductReviews.ts`와 동일한
// "페이지 전용 훅"(컴포넌트가 아니라 규칙 `story-per-component` 대상이 아니다). 재작업(round2, QA FAIL
// 필수1 + 권고1) — 원래 `ProductDetailPage.tsx`에 있던 찜 상태·찜 여부 확인 루프·토글 핸들러를 여기로
// 옮긴다(파일 크기 상한 초과 해소를 겸함). Hooks-rule(must): 아래 useState/useEffect는 전부 최상위
// 호출, 조건부 호출 없음.
import { useEffect, useRef, useState } from 'react'
import { addWish, fetchWishes, removeWish, WISH_PAGE_SIZE } from '../api'
import { toDisplayMessage } from '../features/shop/httpErrorMessage'
import type { WishToggleStatus } from '../features/shop/ProductInfoPanel'
import type { ProductDetail, SessionResult } from '../types'

export interface UseProductWishOptions {
  sku: string
  session: SessionResult | null
  /** 현재 로드된 상품 — 토글 대상 sku 확정에 쓴다. null이면(로딩/오류) 토글은 아무 일도 하지 않는다
   * (원래 `ProductDetailPage`의 `if (!product) return`과 동일). */
  product: ProductDetail | null
  /** 세션이 없을 때 호출 — 로그인 화면 이동은 호출자(`ProductDetailPage`) 몫이다(신규 리다이렉트
   * 규약을 만들지 않는다, `handleNotifyRequest`와 동일 원칙). 로컬 상태는 건드리지 않은 채 반환한다. */
  onRequireLogin: () => void
}

/**
 * QA round1 FAIL 필수1 재현·수정 — 찜 여부 확인 effect가 내 찜 목록을 최대 10페이지까지 순차 조회하는
 * 동안, 그 사이 사용자가 찜을 토글하면 루프가 나중에 완료돼 토글 **이전** 스냅샷으로 `setWished`를
 * 실행해 성공한 토글을 되돌리는 결함이 있었다(실측: POST 200 성공 후에도 화면은 '♡ 찜하기'). `cancelled`
 * 가드는 sku/세션 변경만 보고 토글은 못 본다.
 *
 * 고친 방식 — 요청 세대 번호(`wishGenerationRef`): 찜 여부 확인 effect가 시작할 때, 그리고 토글이
 * 일어날 때 **둘 다** 이 카운터를 올린다. 확인 루프는 시작 시점에 캡처한 세대(`generation`)와 매 반복
 * (각 `await fetchWishes` 직후)마다 "현재" 세대를 비교해, 달라졌으면(=사용자가 그 사이 토글했거나
 * sku/세션이 바뀌었다) 즉시 멈추고 어떤 상태도 쓰지 않는다 — "사용자 액션이 진행 중이던 확인 루프보다
 * 우선한다"는 소유권 분리다. 사례집 SR-351 #3 round2→round3의 "표시 상태(applied)와 요청 대상 상태
 * (target)를 분리해 성공했을 때만 승격"과 같은 계열 — 여기서는 "사용자가 만든 결과(토글 성공/실패)"가
 * 곧 이미 승격된 값이고, 그보다 늦게 도착하는 확인 루프의 결과는 낡은 target으로 취급해 버린다.
 */
export function useProductWish({ sku, session, product, onRequireLogin }: UseProductWishOptions) {
  const [wishCount, setWishCount] = useState(0)
  const [wished, setWished] = useState(false)
  const [wishToggleStatus, setWishToggleStatus] = useState<WishToggleStatus>('idle')
  const [wishErrorMessage, setWishErrorMessage] = useState<string | null>(null)
  // 연타 방지(`ProductDetailPage.inFlightRef`와 동일 원리) — 동기 잠금.
  const wishInFlightRef = useRef(false)
  // 위 문서 주석 — 확인 루프와 토글 모두 이 값을 올린다.
  const wishGenerationRef = useRef(0)

  // 새 상품이 로드되면 로컬 상태를 서버 값으로 리셋한다 — 호출자(`ProductDetailPage.load()`)가 성공
  // 경로에서 `setProduct(data)`와 **같은 동기 블록**으로 직접 호출한다(`notifyStatus` 리셋과 동일
  // 시점). `useEffect([product])`로 미루지 않는다 — 미루면 "product가 이미 렌더된 프레임과 wishCount가
  // 반영되는 프레임"이 한 틱 어긋나(추가 effect-flush 사이클이 필요), product 로드와 같은 순간에
  // wishCount가 나와야 하는 화면 기대와 어긋난다(실측: 리팩터 중 `찜 7` 기대가 `찜 0`으로 나오는 회귀를
  // 만들었다 — effect 지연이 원인). `wished` 자체는 아래 별도 effect가 정한다.
  const resetForProduct = (newWishCount: number) => {
    setWishCount(newWishCount)
    setWishToggleStatus('idle')
    setWishErrorMessage(null)
    wishInFlightRef.current = false
  }

  // 찜 여부 확인 — 별도 "찜 여부 조회" API가 없어(INF-ORD-042만 있음) 내 찜 목록을 1페이지부터 최대
  // 10페이지(200건)까지 순차 조회해 이 sku가 있는지 찾는다(사람 수정 round1). 찾으면 즉시 멈추고
  // wished=true, 그 전에 마지막 페이지 신호(items.length < WISH_PAGE_SIZE)를 만나거나 10페이지를 다
  // 돌면 wished=false. 전부 GET(멱등)이라 StrictMode 이중 실행이 있어도 화면 정합성엔 무해하다(STORY
  // "프레임워크 실행 모델 함정" 절) — 다만 최대 10회 왕복이라는 위험 구간 동안 sku/세션이 바뀌거나
  // (cancelled) 사용자가 토글하면(세대 불일치, 위 문서 주석) 이전 결과로 wished를 덮어쓰지 않는다.
  useEffect(() => {
    if (!session?.memberId || !sku) { setWished(false); return }
    const generation = ++wishGenerationRef.current
    let cancelled = false
    const checkWished = async () => {
      for (let page = 1; page <= 10; page++) {
        let items
        try {
          items = await fetchWishes(session.apiKey, page)
        } catch {
          return // 조회 실패 — wished는 건드리지 않는다(에러 표시 없이 조용한 기본값)
        }
        if (cancelled || generation !== wishGenerationRef.current) return
        if (items.some(it => it.sku === sku)) { setWished(true); return }
        if (items.length < WISH_PAGE_SIZE) { setWished(false); return } // 마지막 페이지 신호
      }
      if (!cancelled && generation === wishGenerationRef.current) {
        setWished(false) // 10페이지를 다 돌아도 못 찾음(STORY "범위 밖" 절)
      }
    }
    void checkWished()
    return () => { cancelled = true }
  }, [session?.memberId, sku])

  // 찜 토글. 순서(STORY "순서·보안" 1~4): 1) 연타 방지(ref). 2) 세션 존재를 가장 먼저 확인 — 없으면
  // 어떤 로컬 상태도 바꾸지 않고 즉시 로그인 화면으로 이동(`handleNotifyRequest`와 동일 순서, SR-234
  // r1 사례 대조). 3) 세션이 있으면 세대 번호를 올려(진행 중이던 확인 루프를 무효화) 토글 직전 값을
  // 변수에 캡처한 뒤에만 낙관적으로 반영. 4) 실패 시 롤백은 캡처값을 그대로 대입한다(재연산 금지 —
  // 증감을 두 번 적용해 카운트가 어긋나는 산술 버그 예방, 실패 사례집 SR-306 #1과 같은 부류의 "연산
  // 순서·중복 적용" 함정 대조).
  const handleToggleWish = () => {
    if (wishInFlightRef.current) return
    if (!session?.memberId) { onRequireLogin(); return }
    if (!product) return
    wishGenerationRef.current++ // 이 시점부터는 사용자 액션이 wished를 소유한다 — 확인 루프 결과는 버림.
    const prevWished = wished
    const prevCount = wishCount
    wishInFlightRef.current = true
    setWished(!prevWished)
    setWishCount(prevCount + (prevWished ? -1 : 1))
    setWishToggleStatus('pending')
    setWishErrorMessage(null)
    const request = prevWished ? removeWish(session.apiKey, product.sku) : addWish(session.apiKey, product.sku)
    request
      .then(() => { setWishToggleStatus('idle') })
      .catch(e => {
        setWished(prevWished)
        setWishCount(prevCount)
        setWishToggleStatus('error')
        setWishErrorMessage(toDisplayMessage(e))
      })
      .finally(() => { wishInFlightRef.current = false })
  }

  return { wishCount, wished, wishToggleStatus, wishErrorMessage, onToggleWish: handleToggleWish, resetForProduct }
}

export type UseProductWishResult = ReturnType<typeof useProductWish>
