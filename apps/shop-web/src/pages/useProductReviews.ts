// SR-351 — 상품 상세(`/shop/products/:sku`) 상품평 탭 전용 훅. `useZipcodeSearch.ts`와 동일한
// "페이지 전용 훅"(컴포넌트가 아니라 규칙 story-per-component 대상이 아니다). `ProductDetailPage.tsx`가
// 호출하고, 목록 조회(enabled 게이팅)·주문 자격 조회·작성/삭제 상태·이 기기에서 만든 리뷰 소유 표시
// (myReviewsStorage)를 전부 캡슐화한다. Hooks-rule(must): 아래 useState/useEffect는 전부 최상위 호출,
// 조건부 호출 없음.
import { useEffect, useRef, useState } from 'react'
import { ApiError, createReview, deleteReview, fetchOrder, fetchOrders, fetchReviews } from '../api'
import { GENERIC_ERROR_MESSAGE } from '../features/shop/httpErrorMessage'
import { isReviewOwned, markReviewOwned } from '../features/shop/myReviewsStorage'
import type { ListStatus, DeleteStatus } from '../features/shop/ProductReviewsSection'
import type { EligibleOrder, OrderLookupStatus, WriteFieldErrors, WriteStatus } from '../features/shop/ReviewWriteArea'
import type { ReviewInput, ReviewListResult, ReviewSort, SessionResult } from '../types'

export interface UseProductReviewsOptions {
  sku: string
  session: SessionResult | null
  /** SR-361 — 목록 조회(아래 첫 번째 effect)는 이제 이 값과 무관하게 always-on이다(AC "화면 진입
   * 즉시 GET"). 이름은 호출부·기존 단위테스트의 불필요한 치환을 피하려 그대로 뒀다 — 지금은 주문
   * 자격 조회(작성 폼 전용 부수효과)에만 영향을 준다. */
  enabled: boolean
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

/**
 * 이 SKU를 구매한(취소 제외) 주문만 골라낸다. `startDate`를 아주 이른 날짜로 고정한다 — 주문 조회
 * 화면의 기본 30일 창을 여기 그대로 쓰면 30일보다 오래된 구매가 자격에서 빠지는 오판이 생긴다(사례집
 * SR-300과 동일 함정, STORY "구매 이력 조회 — 결정됨" 절 근거). `fetchOrders`/`fetchOrder` 자체가
 * 실패하면(네트워크·5xx) 예외를 그대로 던져 호출자가 "구매 이력 없음"과 구분해 처리하게 한다(사람
 * 수정 결정 1번 — 여기서 개별 실패를 삼키면 진짜 원인이 오판으로 뭉개진다).
 */
async function lookupEligibleOrders(memberId: string, sku: string): Promise<EligibleOrder[]> {
  const orders = await fetchOrders({ memberId, orderState: '', startDate: '2000-01-01', endDate: todayIso() })
  const candidates = orders.filter(o => o.orderState !== 'CANCELED')
  const details = await Promise.all(candidates.map(o => fetchOrder(o.orderNo)))
  return candidates
    .filter((_, i) => details[i].items.some(item => item.productNo === sku))
    .map(o => ({ orderNo: o.orderNo, orderedAt: o.orderedAt }))
}

/** 400 RVW-4001 메시지는 "field: 설명" 형식(서버 validationError 관례, 실측 확인) — 첫 ": "로 필드를
 * 가른다. 미상 필드·파싱 실패는 배너로 뭉뚱그린다(AC "400 필드별 안내"). */
function applyCreateError(
  e: unknown,
  setFieldErrors: (errors: WriteFieldErrors) => void,
  setBannerMessage: (message: string | undefined) => void,
): void {
  if (e instanceof ApiError && e.code === 'RVW-4001') {
    const idx = e.message.indexOf(': ')
    const field = idx >= 0 ? e.message.slice(0, idx) : ''
    const detail = idx >= 0 ? e.message.slice(idx + 2) : e.message
    if (field === 'rating') { setFieldErrors({ rating: detail }); return }
    if (field === 'content') { setFieldErrors({ content: detail }); return }
    if (field === 'orderNo') { setFieldErrors({ orderNo: detail }); return }
    setBannerMessage(e.message)
    return
  }
  if (e instanceof ApiError && e.code === 'RVW-4031') {
    setBannerMessage('이 상품을 구매한 회원만 작성할 수 있습니다')
    return
  }
  if (e instanceof ApiError && e.code === 'RVW-4091') {
    setBannerMessage('이미 이 주문으로 작성한 상품평이 있습니다')
    return
  }
  setBannerMessage(GENERIC_ERROR_MESSAGE)
}

export function useProductReviews({ sku, session, enabled }: UseProductReviewsOptions) {
  // 재작업(round2, QA CONCERNS 권고 1 + 사람 지시 4) — 정렬·페이지는 "요청 대상"(target)과 "화면에
  // 표시할 확정값"(applied)을 분리한다. target은 사용자가 클릭한 즉시 바뀌고 fetch effect를 그대로
  // 구동한다(재시도 시에도 이 값 그대로 재호출 — "마지막 요청 파라미터로 재호출"). applied는 그
  // target으로 보낸 요청이 **성공**했을 때만 갱신된다 — 실패하면 건드리지 않아 직전 성공값 그대로
  // 남는다. 부모(ProductReviewsSection)에는 applied만 `sort`/`page`로 노출해 정렬 버튼의 활성 표시·
  // 페이지네이션이 항상 "실제로 화면에 보이는 목록"과 일치하게 한다(재현: 최신순 3건 렌더 중 별점높은순
  // 클릭이 500 → 버튼이 별점높은순으로 눌린 채 화면은 여전히 최신순 3건이라 사용자가 결과를 오해하고
  // 재시도 수단도 없었다 — QA round1 발견).
  const [targetSort, setTargetSort] = useState<ReviewSort>('latest')
  const [targetPage, setTargetPage] = useState(1)
  const [appliedSort, setAppliedSort] = useState<ReviewSort>('latest')
  const [appliedPage, setAppliedPage] = useState(1)
  const [listStatus, setListStatus] = useState<ListStatus>('idle')
  const [listResult, setListResult] = useState<ReviewListResult | null>(null)
  const [listReloadTick, setListReloadTick] = useState(0)
  // 재작업(round3, 사람 지시 1) — 정렬·페이지 변경이 실패한 뒤, 사용자가 원래 누르려던 값을 다시
  // 누르면 targetSort/targetPage가 "이미 그 값"이라(직전 실패한 시도가 이미 그 값을 target으로 삼아
  // 있었으므로) React가 동일 state로 판단해 리렌더·effect 재실행을 생략한다 — 요청이 아예 나가지
  // 않는 dead click(QA round2 low 1). 클릭 "시도" 자체를 별도 nonce로 세어 목록 조회 effect의 의존
  // 배열에 포함시키면, 정렬·페이지 값이 바뀌지 않아도 매 클릭마다 effect가 재실행된다(사람 지시 —
  // listStatus==='error'일 때만 분기하는 1줄 땜질 대신, 요청 트리거 자체에 시도 카운터를 포함).
  const [listRequestAttempt, setListRequestAttempt] = useState(0)
  // React 19 StrictMode 이중 effect·정렬/페이지 연타 대응(프레임워크 실행 모델 함정 절, 사람 수정
  // 결정 3번 — 먼저 보낸 요청의 응답이 늦게 도착해도 최신 요청 결과를 덮어쓰지 않는다).
  const listRequestIdRef = useRef(0)

  const [orderLookupStatus, setOrderLookupStatus] = useState<OrderLookupStatus>('idle')
  const [eligibleOrders, setEligibleOrders] = useState<EligibleOrder[]>([])
  const [orderLookupReloadTick, setOrderLookupReloadTick] = useState(0)
  const orderLookupRequestIdRef = useRef(0)

  const [orderNo, setOrderNo] = useState('')
  const [rating, setRating] = useState(0)
  const [content, setContent] = useState('')
  const [fieldErrors, setFieldErrors] = useState<WriteFieldErrors>({})
  const [writeStatus, setWriteStatus] = useState<WriteStatus>('idle')
  const [bannerMessage, setBannerMessage] = useState<string | undefined>(undefined)
  // 연타 방지(`ProductDetailPage.inFlightRef`와 동일 원리) — disabled만으로는 같은 이벤트 루프 안의
  // 연속 클릭을 못 막는다(프레임워크 실행 모델 함정 절).
  const writeInFlightRef = useRef(false)

  const [deleteTarget, setDeleteTarget] = useState<number | null>(null)
  const [deleteStatus, setDeleteStatus] = useState<DeleteStatus>('idle')
  const [deleteErrorMessage, setDeleteErrorMessage] = useState<string | null>(null)
  const deleteInFlightRef = useRef(false)

  // 목록 조회 — SR-361: 화면(상품 상세) 진입 즉시 always-on으로 나간다(AC "화면 진입 즉시 GET"). 탭
  // 활성 여부(enabled)는 더 이상 이 effect를 게이팅하지 않는다 — 미리보기(ProductReviewPreview)와
  // 리뷰 탭(ProductReviewsSection)이 같은 결과를 재사용해 중복 호출하지 않는다(AC). 요청 시점의
  // target을 클로저에 고정해 두고, 성공했을 때만 그 값을 applied로 승격한다(실패하면 applied는
  // 그대로 — 위 "target/applied 분리" 주석 참고). requestId 가드는 기존 그대로(건드리지 않는다).
  useEffect(() => {
    const id = ++listRequestIdRef.current
    const requestSort = targetSort
    const requestPage = targetPage
    setListStatus('loading')
    fetchReviews(sku, requestSort, requestPage)
      .then(result => {
        if (id !== listRequestIdRef.current) return
        setListResult(result)
        setListStatus('ready')
        setAppliedSort(requestSort)
        setAppliedPage(requestPage)
      })
      .catch(() => {
        if (id !== listRequestIdRef.current) return
        setListStatus('error')
      })
  }, [sku, targetSort, targetPage, listReloadTick, listRequestAttempt])

  // 마지막 페이지의 마지막 1건을 지운 경우 빈 페이지가 남지 않게 한 페이지 내려 재조회한다(데이터 절).
  useEffect(() => {
    if (listStatus === 'ready' && listResult && listResult.items.length === 0 && targetPage > 1) {
      setTargetPage(p => p - 1)
    }
  }, [listStatus, listResult, targetPage])

  // 작성 가능 주문 자격 조회 — 로그인 판정이 최우선(순서·보안 2, RUN8/SR-234 FUNC-member-008 r1 대조):
  // 세션이 없으면 이 effect는 아무 것도 하지 않는다(부수효과 호출 자체가 나가지 않음).
  useEffect(() => {
    if (!enabled || !session?.memberId) return
    const id = ++orderLookupRequestIdRef.current
    setOrderLookupStatus('loading')
    lookupEligibleOrders(session.memberId, sku)
      .then(orders => {
        if (id !== orderLookupRequestIdRef.current) return
        setEligibleOrders(orders)
        setOrderLookupStatus('ready')
      })
      .catch(() => {
        if (id !== orderLookupRequestIdRef.current) return
        setOrderLookupStatus('error')
      })
  }, [sku, session?.memberId, enabled, orderLookupReloadTick])

  // 재작업(round2 사람 지시 4) — 정렬 변경 시 page를 1로 리셋한다(작성 성공 시 1페이지 강제 이동
  // 방침과 일관). target만 바꾼다 — applied는 이 요청이 성공해야 갱신된다(위 target/applied 분리 참고).
  const handleSortChange = (v: ReviewSort) => {
    setTargetSort(v)
    setTargetPage(1)
    setListRequestAttempt(a => a + 1)
  }
  const handlePageChange = (v: number) => {
    setTargetPage(v)
    setListRequestAttempt(a => a + 1)
  }

  const handleOrderNoChange = (v: string) => {
    setOrderNo(v)
    setFieldErrors(errs => (errs.orderNo === undefined ? errs : { ...errs, orderNo: undefined }))
  }
  const handleRatingChange = (v: number) => {
    setRating(v)
    setFieldErrors(errs => (errs.rating === undefined ? errs : { ...errs, rating: undefined }))
  }
  const handleContentChange = (v: string) => {
    setContent(v)
    setFieldErrors(errs => (errs.content === undefined ? errs : { ...errs, content: undefined }))
  }

  const handleSubmit = () => {
    if (writeInFlightRef.current || !session) return
    const errors: WriteFieldErrors = {}
    if (!orderNo) errors.orderNo = '구매하신 주문을 선택해 주세요'
    if (!Number.isInteger(rating) || rating < 1 || rating > 5) errors.rating = '별점을 선택해 주세요'
    // 재작업(round2 사람 지시 3) — 서버 validateContent는 trim하지 않은 length로 본다. 클라이언트가
    // trim 후 검증하면 "abc"+공백 7자(총 10자, 서버는 통과)를 클라이언트만 막는 어긋남이 생긴다
    // (QA CONCERNS 권고 3). 전송값도 그대로(비trim) content라 검증 대상과 저장값을 일치시킨다.
    if (content.length < 10 || content.length > 1000) errors.content = '본문은 10자 이상 1000자 이하로 입력해 주세요'
    if (Object.keys(errors).length > 0) { setFieldErrors(errors); return }

    writeInFlightRef.current = true
    setFieldErrors({})
    setBannerMessage(undefined)
    setWriteStatus('pending')
    const input: ReviewInput = { rating, content, orderNo }
    createReview(session.apiKey, sku, input)
      .then(result => {
        markReviewOwned(session.memberId, result.reviewId)
        setOrderNo('')
        setRating(0)
        setContent('')
        setWriteStatus('success')
        // 사람 수정 결정 — 작성 성공 시 1페이지·최신순으로 강제 이동(제출 전 정렬이 무엇이었든).
        // 재작업(round3 사람 지시 2) — target만 옮기고 applied는 승격하지 않는다. 작성 자체는
        // 성공했어도 뒤따르는 재조회(GET)가 실패할 수 있어, 여기서 applied를 낙관적으로 먼저
        // 바꾸면 화면(정렬 버튼·페이지 표시)이 실제 목록보다 앞서가 어긋난다(QA round2 low 2
        // CONCERNS). applied 승격은 목록 effect의 성공 경로(위 `.then`)에만 맡긴다 — 재조회가
        // 실패하면 인라인 경고(ReviewListErrorNotice)가 뜨고 버튼은 직전 성공값 그대로 남는다.
        setTargetSort('latest')
        setTargetPage(1)
        setListReloadTick(t => t + 1)
      })
      .catch(e => {
        setWriteStatus('error')
        applyCreateError(e, setFieldErrors, setBannerMessage)
      })
      .finally(() => { writeInFlightRef.current = false })
  }

  const handleDeleteRequest = (reviewId: number) => {
    setDeleteTarget(reviewId)
    setDeleteErrorMessage(null)
  }
  const handleDeleteCancel = () => setDeleteTarget(null)

  const handleDeleteConfirm = () => {
    if (deleteInFlightRef.current || deleteTarget == null || !session) return
    const reviewId = deleteTarget
    deleteInFlightRef.current = true
    setDeleteStatus('pending')
    deleteReview(session.apiKey, sku, reviewId)
      .then(() => {
        setDeleteTarget(null)
        setListReloadTick(t => t + 1)
      })
      .catch(e => {
        setDeleteTarget(null)
        if (e instanceof ApiError && e.status === 404) {
          setDeleteErrorMessage('이미 삭제되었거나 존재하지 않는 상품평입니다')
          setListReloadTick(t => t + 1) // 다른 곳에서 이미 지워졌을 수 있으므로 최신 상태로 갱신
        } else if (e instanceof ApiError && e.status === 403) {
          setDeleteErrorMessage('본인이 작성한 상품평만 삭제할 수 있습니다')
        } else {
          setDeleteErrorMessage(GENERIC_ERROR_MESSAGE)
        }
      })
      .finally(() => {
        deleteInFlightRef.current = false
        setDeleteStatus('idle')
      })
  }

  return {
    // 화면에는 applied(마지막으로 성공한 요청의 값)만 노출한다 — 실패한 요청의 target으로 버튼이
    // 앞서가지 않게 한다(위 target/applied 분리 주석 참고).
    sort: appliedSort,
    onSortChange: handleSortChange,
    page: appliedPage,
    onPageChange: handlePageChange,
    listStatus,
    listResult,
    onRetryList: () => setListReloadTick(t => t + 1),
    isOwn: (reviewId: number) => (session ? isReviewOwned(session.memberId, reviewId) : false),

    orderLookupStatus,
    eligibleOrders,
    onRetryOrderLookup: () => setOrderLookupReloadTick(t => t + 1),

    orderNo,
    onOrderNoChange: handleOrderNoChange,
    rating,
    onRatingChange: handleRatingChange,
    content,
    onContentChange: handleContentChange,
    fieldErrors,
    writeStatus,
    bannerMessage,
    onSubmit: handleSubmit,

    deleteTarget,
    deleteStatus,
    deleteErrorMessage,
    onDeleteRequest: handleDeleteRequest,
    onDeleteCancel: handleDeleteCancel,
    onDeleteConfirm: handleDeleteConfirm,
  }
}

export type UseProductReviewsResult = ReturnType<typeof useProductReviews>
