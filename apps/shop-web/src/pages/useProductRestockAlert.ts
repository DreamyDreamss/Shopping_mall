// SR-364 — 상품 상세(`/shop/products/:sku`) 재입고 알림 신청 전용 훅. `useProductCompare.ts`와 동일한
// "페이지 전용 훅"(컴포넌트가 아니라 규칙 `story-per-component` 대상이 아니다). 기존 `ProductDetailPage`의
// `notifyStatus`/`notifyInFlightRef`/`handleNotifyRequest`(SR-353, 서버 저장 없는 프론트 전용 구현)를
// 이 훅으로 대체하고, 신청을 `RestockAlertController`(INF-ORD-048 POST, SR-364 #1 Done)에 실제로
// 저장한다. 확인 GET(INF-ORD-049, SR-364 #2 Done — round2 QA가 코드 대조로 계약 일치를 확인)은
// STORY-3 "구현 계획" 절이 전제한 계약(`{items:[{sku,createdAt}]}`, 최근 신청 순·무페이지)을 그대로
// 소비한다. Hooks-rule(must): 아래 useState/useEffect/useRef는 전부 최상위 호출, 조건부 호출 없음.
//
// SR-364 재작업(round2, QA FAIL 필수1) — round1은 "확인 결과(신청됨 여부)"와 "액션 진행 상태(pending)"
// 를 `notifyStatus` 한 값에 겸했다. `resetForProduct()`가 그 한 값을 무조건 'idle'로 되돌리는 바람에,
// 품절 A → 관련상품 클릭 → 품절 B로 SPA param-only 이동할 때 확인 GET(B)이 `fetchProduct(B)`보다 먼저
// 도착하면 `resetForProduct()`가 확인 GET이 이미 써 넣은 결과를 덮어썼다. round2는 `notified`
// (확인 GET/신청 성공만 쓴다)와 `notifyPending`(신청 POST 왕복 중만 쓴다)을 별도 `useState`로 분리해
// 이 문제는 구조적으로 고쳤다(형제 훅 `useProductWish`/`useProductCompare`와 동일 소유권 분리).
//
// SR-364 재작업(round3, QA FAIL round2 필수1) — round2는 `notified`를 sku에 묶지 않은 단일 boolean으로
// 뒀다. round1 처방의 후반절("idle 초기화를 확인 effect 진입부로 옮긴다")이 빠져, notified를 false로
// 되돌리는 주체가 "확인 GET 성공 응답"과 얼리리턴뿐이었다 — 품절 A(신청됨)→품절 B(미신청) 이동에서 B의
// 확인 GET이 실패하면 notified가 true로 남아 B가 「신청됨」(비활성)으로 굳어 사용자가 신청 자체를 할 수
// 없었다(AC3/확정 답변 6 위반, QA 실측 재현). 같은 뿌리로 (b) 품절→품절 이동마다 확인 GET 왕복 동안
// 잘못된 「신청됨」 과도 상태가 보였고(확인 effect가 sku 변경 직후 "이전 상품"의 soldOut을 읽는 게이트
// 였다), (c) POST 성공 핸들러가 세대·sku 검사 없이 setNotified(true)를 해 클릭 직후 이동하면 늦게
// 도착한 A의 결과가 B를 신청됨으로 만들었다.
//
// 처방(QA 제안, 사람 승인 — [구현 방식]) — notified를 "그 결과가 어느 sku의 것인지"와 쌍으로 소유하게
// 바꿨다: `notifiedSku`(확인 GET 성공 또는 신청 POST 성공만 쓴다) + `notified = notifiedSku === sku`
// (파생값 — 지금 렌더링 중인 sku와 매 렌더 다시 비교한다). 이 sku로 신청됨을 기록한 적이 한 번도
// 없으면(다른 상품이었거나, 확인이 실패했거나, 아직 안 끝났으면) `notified`는 별도 리셋 코드 없이
// 자동으로 false다 — (a)·(b)를 한 번에 닫는다(이월 자체가 구조적으로 불가능해진다). POST 성공은
// **클릭 시점에 캡처한 sku**로만 `setNotifiedSku`를 호출해 (c)도 같은 방식으로 닫는다(이동 후 늦게
// 도착해도 그 결과는 캡처된 sku에만 적용되고, 지금 화면(다른 sku)의 `notified` 파생값에는 영향이
// 없다). 확인 effect 게이트에 `product?.sku === sku`(로드된 상품이 지금 라우트의 sku와 실제로
// 일치하는지)를 더해, sku가 막 바뀐 직후 아직 로드되지 않은 이전 상품의 `soldOut`을 잘못 읽어 불필요한
// GET을 내보내던 것도 함께 막는다(round2 권고 2, (b)의 오염 창도 같이 닫는다). 두 완료 처리(확인 GET
// 성공·POST 성공) 모두에서 "지금 렌더링 중인 sku"를 `skuRef`로 한 번 더 명시적으로 대조한다(세대
// 카운터로 이미 막히는 경쟁이지만, 사람 지시대로 이중으로 방어한다 — 해는 없다).
import { useEffect, useRef, useState } from 'react'
import { fetchRestockAlerts, requestRestockAlert } from '../api'
import { toDisplayMessage } from '../features/shop/httpErrorMessage'
import type { NotifyStatus } from '../features/shop/ProductInfoPanel'
import type { ProductDetail, SessionResult } from '../types'

export interface UseProductRestockAlertOptions {
  sku: string
  session: SessionResult | null
  /** 현재 로드된 상품 — 품절 여부로 확인 GET 호출 여부를 가른다(비품절이면 버튼 자체가 없어 호출
   * 자체가 의미 없음, STORY "순서·보안" 3). null이거나 아직 이 sku로 로드되지 않았으면(`product.sku
   * !== sku`, 라우트 이동 중 과도 구간) 확인하지 않는다(round3 — round2 권고 2). */
  product: ProductDetail | null
  /** 세션이 없을 때 호출 — 로그인 화면 이동은 호출자(`ProductDetailPage`) 몫이다(신규 리다이렉트
   * 규약을 만들지 않는다, `useProductCompare.onRequireLogin`과 동일 원칙). 로컬 상태는 건드리지 않은
   * 채 반환한다. */
  onRequireLogin: () => void
  /** 신청 POST 결과 — 성공/실패 토스트 표시는 호출자(`ProductDetailPage`)가 기존 `toasts` 채널로
   * 한다(신규 인라인 오류 상태를 만들지 않는다, STORY "테스트" 절). */
  onNotifyResult: (result: { success: true } | { success: false; message: string }) => void
}

export function useProductRestockAlert({ sku, session, product, onRequireLogin, onNotifyResult }: UseProductRestockAlertOptions) {
  // 확인 GET/신청 POST 성공이 "어느 sku에 대해" 신청됨을 확인했는지를 값 자체에 함께 담는다(round3).
  // 화면에 쓰는 `notified`는 이 값과 "지금 렌더링 중인 sku"의 일치 여부로만 파생한다 — 다른 sku였거나
  // 확인이 실패/미완료면 별도 리셋 코드 없이 항상 false다(이월 불가, round2 FAIL 필수1 재발 방지).
  const [notifiedSku, setNotifiedSku] = useState<string | null>(null)
  const notified = notifiedSku === sku
  // 신청 진행 상태(POST 왕복 대기) — `resetForProduct()`가 리셋하는 것은 이 값뿐이다(`wishToggleStatus`/
  // `compareStatus`와 동일 소유권 분리, round2부터 유지).
  const [notifyPending, setNotifyPending] = useState(false)
  // 연타 방지(`useProductCompare.compareInFlightRef`와 동일 원리) — 동기 잠금.
  const notifyInFlightRef = useRef(false)
  // 확인 effect와 신청 클릭 둘 다 이 값을 올린다(`useProductCompare.compareGenerationRef`와 동일 원칙 —
  // 확인이 진행 중인 동안 사용자가 클릭하면 늦게 도착한 확인 결과가 클릭 결과를 덮어쓰지 않는다,
  // 실패 사례집 SR-356 #1 round1→round2 대조. 다만 여기는 단방향 신청이라 롤백 대상 자체가 없어
  // 위험도는 그 사례보다 낮다).
  const notifyGenerationRef = useRef(0)
  // round3 사람 지시([구현 방식]) — "현재 렌더링 중인 sku와 일치하는지"를 세대 카운터와 별개로 한 번
  // 더 명시적으로 검사하기 위한 최신 sku 참조(매 렌더 갱신). 세대 카운터만으로도 경쟁은 막히지만, 이
  // 값은 그 의도를 코드로도 명시적으로 드러낸다(이중 방어, 해는 없다).
  const skuRef = useRef(sku)
  skuRef.current = sku

  // 새 상품이 로드되면 "신청 진행 상태"만 리셋한다 — 호출자(`ProductDetailPage.load()`)가
  // `setProduct(data)`와 같은 동기 블록에서 직접 호출한다(`compare.resetForProduct`와 동일 시점, 렌더
  // 지연 없음 원칙). `notifiedSku`는 여기서 건드리지 않는다 — `notified` 파생값이 sku 비교로 자동
  // 정해지므로 리셋이 필요 없다(round3 — round2도 이 자리에 리셋을 두지 않았지만, round2는 `notified`가
  // sku에 묶이지 않은 단일 boolean이라 "리셋 없음"이 오히려 이월을 만들었다. round3은 상태를 sku와
  // 쌍으로 바꿔 "리셋 없음"이 안전해지게 한 것이지, 새로 리셋을 추가한 게 아니다).
  const resetForProduct = () => {
    setNotifyPending(false)
    notifyInFlightRef.current = false
  }

  // 신청 여부 확인 — 품절 상품이고, 로드된 상품이 지금 라우트의 sku와 실제로 일치할 때만(round3 —
  // round2는 `product?.soldOut`만 봐서 sku가 막 바뀐 직후 "이전 상품"의 soldOut을 읽는 창이 있었다,
  // round2 권고 2) `fetchRestockAlerts` 1회로 이 sku가 있는지 판정한다(무페이지, AC "품절 상품 상세
  // 진입 시 로그인 회원이면 GET…로 신청 여부를 읽어 버튼을 신청됨으로"). 조건이 안 맞으면 API를 아예
  // 호출하지 않는다 — `notified`는 `notifiedSku`가 지금 sku와 다르면 자동으로 false이므로 별도로
  // false를 대입할 필요가 없다(STORY "순서·보안" 3, `useProductCompare`의 얼리리턴과 동일 원칙이지만
  // round3부터는 "대입"이 아니라 "파생"으로 닫는다). GET 실패는 `notifiedSku`를 건드리지 않는다·토스트
  // 없음(확정 답변) — `.catch`에서 어떤 상태도 바꾸지 않아도 AC3이 자동으로 성립한다(round2 FAIL 필수1
  // 재발 방지의 핵심: 실패해도 notifiedSku가 이 sku를 가리키지 않으면 notified는 저절로 false).
  useEffect(() => {
    const productMatchesRoute = product?.sku === sku
    if (!session?.memberId || !sku || !productMatchesRoute || !product?.soldOut) return
    const generation = ++notifyGenerationRef.current
    let cancelled = false
    fetchRestockAlerts(session.apiKey)
      .then(items => {
        // 세대 불일치(그 사이 다른 확인/신청이 끼어듦) 또는 언마운트/재실행이면 폐기. 추가로 "지금
        // 렌더링 중인 sku"와도 명시 대조한다(사람 지시 — 세대 카운터와 이중 방어, 해는 없다).
        if (cancelled || generation !== notifyGenerationRef.current || skuRef.current !== sku) return
        setNotifiedSku(items.some(it => it.sku === sku) ? sku : null)
      })
      .catch(() => {
        // 조회 실패 — notifiedSku를 건드리지 않는다. 이 sku로 신청됨을 기록한 적이 없다면 notified는
        // (notifiedSku === sku) 파생값이 그대로 false다(이전 상품의 notifiedSku가 남아 있어도 다른
        // 문자열이라 무관) — AC3.
      })
    return () => { cancelled = true }
  }, [session?.memberId, sku, product?.sku, product?.soldOut])

  // 재입고 알림 신청 클릭. 순서(STORY "순서·보안" 1~2, RUN8/SR-234 FUNC-member-008 r1 사례 대조 —
  // 부수효과가 판정보다 먼저 나가지 않는다): 1) 연타 방지(ref). 2) 로그인 판정이 가장 먼저 — 없으면
  // 어떤 로컬 상태도 바꾸지 않고 즉시 로그인 화면으로 이동(`handleToggleCompare`와 동일 순서). 3) 클릭
  // 시점의 sku를 캡처한다(`targetSku`, round3 — 응답이 늦게 도착해도 이 값으로만 반영한다) 4) 세대
  // 카운터 증가(진행 중이던 확인 결과를 무효화) 5) pending 표시(비낙관적 — 서버 확인 후에만 notified로
  // 승격) 6) POST 7) 성공 시 notifiedSku=targetSku + 성공 콜백, 실패 시 pending만 내리고 notifiedSku는
  // 그대로 둔 채 실패 콜백(`toDisplayMessage` 필수 경유 — 실패 사례집 SR-235 #1 대조, 새 필터를 만들지
  // 않고 기존 공용 필터를 그대로 재사용한다).
  const handleNotifyRequest = () => {
    if (notifyInFlightRef.current) return
    if (!session?.memberId) { onRequireLogin(); return }
    const targetSku = sku // 클릭 시점 캡처 — 이동 후 늦게 도착해도 이 값으로만 반영한다(round3 필수1 (c)).
    notifyGenerationRef.current++ // 이 시점부터는 사용자 액션이 확인 결과보다 우선한다 — 진행 중이던 확인 결과는 버림.
    notifyInFlightRef.current = true
    setNotifyPending(true)
    requestRestockAlert(session.apiKey, targetSku)
      .then(() => {
        // round4(QA FAIL round3 필수1) — round3의 "대조 대상이 없다" 판단은 틀렸다: notifiedSku는 sku를
        // 하나만 담는 단일 슬롯이라, 이동 후 늦게 도착한 targetSku(A)의 성공 처리가 지금 화면(B)의
        // notifiedSku를 targetSku(A)로 덮어써 `notified = notifiedSku === sku`를 false로 만들고
        // (B의 확정된 「신청됨」을 해제), onNotifyResult도 무조건 불려 B 화면에 A의 성공 토스트가 뜬다
        // (QA PROBE2·PROBE3 실측 재현). 지금 렌더링 중인 sku(skuRef.current)가 클릭 시점 sku(targetSku)와
        // 같을 때만 상태 반영과 성공 콜백을 실행한다 — 다르면(그 사이 다른 상품으로 이동) 이 POST 결과는
        // 지금 화면에 아무 영향도 주지 않는다(QA가 패치까지 실측 검증한 처방, 사람 승인).
        if (skuRef.current === targetSku) {
          setNotifiedSku(targetSku)
          onNotifyResult({ success: true })
        }
        setNotifyPending(false)
      })
      .catch(e => {
        setNotifyPending(false)
        onNotifyResult({ success: false, message: toDisplayMessage(e) })
      })
      .finally(() => { notifyInFlightRef.current = false })
  }

  // 외부 계약(`ProductInfoPanel.NotifyStatus`)은 바꾸지 않는다 — 위에서 분리한 두 내부 상태를 여기서만
  // 합성한다(`ProductInfoPanel.tsx`·그 스토리·테스트는 이 항목의 범위가 아니다).
  const notifyStatus: NotifyStatus = notified ? 'requested' : notifyPending ? 'pending' : 'idle'

  return { notifyStatus, onNotifyRequest: handleNotifyRequest, resetForProduct }
}

export type UseProductRestockAlertResult = ReturnType<typeof useProductRestockAlert>
