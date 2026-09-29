/** @jest-environment jsdom */
// SR-351 — useProductReviews 훅 단위 테스트. 이 저장소 관례상 훅 전용 .test.ts가 없어 이 항목이 처음
// 만든다(STORY "테스트" 절). `MyAddressesPage.test.tsx`의 `jest.mock` 관례(`jest.requireActual`로
// `ApiError`는 실물 유지) 그대로. 정렬·페이지 변경 시 재조회, 작성 성공 시 1페이지·최신순 리셋+재조회,
// 삭제 성공 시 재조회·마지막 페이지 클램프, 400 필드 파싱, 403/409 고정 문구, eligibleOrders 계산
// 로직을 직접 검증한다.
declare const jest: typeof import('@jest/globals').jest

import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, test } from '@jest/globals'
import type { OrderDetail, OrderItem, OrderRow, ReviewCreateResult, ReviewListResult, SessionResult } from '../types'

jest.mock('../api', () => {
  const actual = jest.requireActual('../api') as Record<string, unknown>
  return {
    ...actual,
    fetchReviews: jest.fn(),
    fetchOrders: jest.fn(),
    fetchOrder: jest.fn(),
    createReview: jest.fn(),
    deleteReview: jest.fn(),
  }
})

import { ApiError, createReview, deleteReview, fetchOrder, fetchOrders, fetchReviews } from '../api'
import { useProductReviews } from './useProductReviews'

const mockFetchReviews = jest.mocked(fetchReviews)
const mockFetchOrders = jest.mocked(fetchOrders)
const mockFetchOrder = jest.mocked(fetchOrder)
const mockCreateReview = jest.mocked(createReview)
const mockDeleteReview = jest.mocked(deleteReview)

function emptyList(overrides: Partial<ReviewListResult> = {}): ReviewListResult {
  return {
    totalCount: 0, page: 1, size: 10, avgRating: null, reviewCount: 0,
    distribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 }, items: [], ...overrides,
  }
}

const session: SessionResult = {
  memberId: 'm-rvw-1', memberName: '홍길동', grade: 'NORMAL',
  apiKey: 'key-rvw-1', refreshToken: 'r-rvw-1', refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
}

function orderRow(i: number, overrides: Partial<OrderRow> = {}): OrderRow {
  return {
    orderNo: `ORD-${i}`, memberId: session.memberId, memberName: session.memberName,
    orderState: 'DONE', totalAmount: 10000, orderedAt: '2026-01-01T00:00:00Z', deliveryState: null,
    ...overrides,
  }
}

function orderDetailFor(row: OrderRow, items: OrderItem[]): OrderDetail {
  return { ...row, items, deliveries: [], shipments: [] }
}

describe('useProductReviews', () => {
  beforeEach(() => {
    mockFetchReviews.mockReset()
    mockFetchOrders.mockReset()
    mockFetchOrder.mockReset()
    mockCreateReview.mockReset()
    mockDeleteReview.mockReset()
    localStorage.clear() // myReviewsStorage가 다음 테스트로 새지 않게 한다.
  })

  afterEach(() => {
    localStorage.clear()
  })

  // 클램프 효과(items.length===0 && page>1이면 1페이지로 되돌림, "데이터" 절)가 이 테스트의 임의
  // 페이지 이동과 간섭하지 않도록 항상 1건 이상 있는 응답을 쓴다.
  const oneItemList = emptyList({
    totalCount: 100, reviewCount: 100,
    items: [{ reviewId: 1, rating: 5, content: '고정 테스트용 리뷰 내용입니다', authorName: '홍*동', createdAt: '2026-09-01T00:00:00Z' }],
  })

  // 재작업(SR-361, AC "화면 진입 즉시 GET" + "탭 진입은 재사용, 중복 호출 안 함") — 목록 조회는 이제
  // 탭 활성 여부(`tabActive`, 구 `enabled` — 훅 옵션 이름 자체는 호출부·기존 테스트 영향을 줄이려
  // 그대로 뒀다)와 무관하게 always-on이다. 마운트 즉시(`tabActive=false`여도) 1회 나가고, 이후 탭
  // 진입(`tabActive=true`)해도 같은 결과를 재사용해 추가 호출이 없어야 한다(훅 레벨 직접 증거).
  test('목록 조회는 화면 진입 즉시(tabActive=false여도) 1회 나가고, 탭 진입(tabActive=true)은 재사용해 추가 호출이 없다', async () => {
    mockFetchReviews.mockResolvedValue(oneItemList)
    const { result, rerender } = renderHook(
      (props: { tabActive: boolean }) => useProductReviews({ sku: 'sku-1', session: null, enabled: props.tabActive }),
      { initialProps: { tabActive: false } },
    )
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(1))
    expect(mockFetchReviews).toHaveBeenLastCalledWith('sku-1', 'latest', 1)

    rerender({ tabActive: true })
    await waitFor(() => expect(result.current.listStatus).toBe('ready'))
    expect(mockFetchReviews).toHaveBeenCalledTimes(1) // 탭 진입 자체는 추가 GET을 만들지 않는다

    act(() => result.current.onSortChange('ratingDesc'))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(2))
    expect(mockFetchReviews).toHaveBeenLastCalledWith('sku-1', 'ratingDesc', 1)

    act(() => result.current.onPageChange(2))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(3))
    expect(mockFetchReviews).toHaveBeenLastCalledWith('sku-1', 'ratingDesc', 2)
  })

  // 재작업(round2 사람 지시 4) — 정렬 변경은 페이지를 1로 되돌린다(작성 성공 시 1페이지 강제 이동
  // 방침과 일관). 먼저 3페이지로 이동한 뒤 정렬을 바꿔, 다음 호출이 정확히 1페이지로 가는지 확인한다.
  test('정렬 변경 시 page가 1로 리셋된다', async () => {
    mockFetchReviews.mockResolvedValue(oneItemList)
    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session: null, enabled: true }))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(1))

    act(() => result.current.onPageChange(3))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(2))
    expect(mockFetchReviews).toHaveBeenLastCalledWith('sku-1', 'latest', 3)

    act(() => result.current.onSortChange('ratingDesc'))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(3))
    expect(mockFetchReviews).toHaveBeenLastCalledWith('sku-1', 'ratingDesc', 1)
  })

  // 재작업(round2, QA CONCERNS 권고 1) — 정렬 변경 요청이 실패하면 노출되는 sort/page(표시값, "applied")는
  // 실패한 요청의 값이 아니라 직전 성공값으로 남아야 한다(정렬 버튼 활성 표시가 실제 화면과 어긋나지
  // 않게). 재시도(onRetryList)는 실패했던 요청의 파라미터 그대로 재호출해야 한다.
  test('정렬 변경 요청이 실패하면 표시값은 이전 성공 값으로 유지되고, 재시도는 실패했던 파라미터로 다시 호출한다', async () => {
    mockFetchReviews.mockResolvedValueOnce(oneItemList) // 최초(latest,1) 성공
    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session: null, enabled: true }))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(1))
    expect(result.current.sort).toBe('latest')
    expect(result.current.page).toBe(1)

    mockFetchReviews.mockRejectedValueOnce(new Error('boom'))
    act(() => result.current.onSortChange('ratingDesc'))
    await waitFor(() => expect(result.current.listStatus).toBe('error'))
    // 실패한 'ratingDesc'가 아니라 성공했던 'latest'로 표시값이 유지된다.
    expect(result.current.sort).toBe('latest')
    expect(result.current.page).toBe(1)
    // 직전 성공 목록은 그대로 남아 있다(전체 대체가 아니다).
    expect(result.current.listResult).toBe(oneItemList)

    mockFetchReviews.mockResolvedValueOnce(oneItemList)
    act(() => result.current.onRetryList())
    // 재시도는 실패했던 요청의 파라미터('ratingDesc', 1) 그대로 다시 호출한다.
    await waitFor(() => expect(mockFetchReviews).toHaveBeenLastCalledWith('sku-1', 'ratingDesc', 1))
    await waitFor(() => expect(result.current.sort).toBe('ratingDesc'))
    expect(result.current.listStatus).toBe('ready')
  })

  // 재작업(round3 사람 지시 1, QA round2 low 1) — 정렬 변경이 실패한 뒤, "다시 시도" 버튼이 아니라
  // 사용자가 원래 누르려던 정렬 버튼 자체를 다시 눌러도(target과 같은 값) 재요청이 나가야 한다.
  // target이 이미 그 값이라 값만 보면 React가 동일 state로 판단하지만, 시도 nonce가 effect 의존을
  // 바꿔 재요청을 강제한다(dead click 방지).
  test('정렬 실패 후 같은 정렬값을 다시 클릭해도(다시 시도 버튼이 아니라) 재조회가 나간다', async () => {
    mockFetchReviews.mockResolvedValueOnce(oneItemList) // 최초(latest,1) 성공
    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session: null, enabled: true }))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(1))

    mockFetchReviews.mockRejectedValueOnce(new Error('boom'))
    act(() => result.current.onSortChange('ratingDesc'))
    await waitFor(() => expect(result.current.listStatus).toBe('error'))
    expect(mockFetchReviews).toHaveBeenCalledTimes(2)

    // "다시 시도"가 아니라 같은 정렬 버튼을 또 누른다 — target 값 자체는 실패한 시도와 동일하다.
    mockFetchReviews.mockResolvedValueOnce(oneItemList)
    act(() => result.current.onSortChange('ratingDesc'))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(3))
    expect(mockFetchReviews).toHaveBeenLastCalledWith('sku-1', 'ratingDesc', 1)
    await waitFor(() => expect(result.current.listStatus).toBe('ready'))
    await waitFor(() => expect(result.current.sort).toBe('ratingDesc'))
  })

  test('작성 성공 시 1페이지·최신순으로 리셋하고 목록을 재조회하며 입력값을 비운다', async () => {
    mockFetchReviews.mockResolvedValue(oneItemList)
    mockFetchOrders.mockResolvedValue([])
    const created: ReviewCreateResult = {
      reviewId: 42, sku: 'sku-1', orderNo: 'ORD-1', rating: 5, content: '충분히 긴 상품평 내용 테스트', createdAt: '2026-09-27T00:00:00Z',
    }
    mockCreateReview.mockResolvedValue(created)

    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session, enabled: true }))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(1))

    act(() => {
      result.current.onSortChange('ratingDesc')
      result.current.onPageChange(3)
    })
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(2))
    expect(mockFetchReviews).toHaveBeenLastCalledWith('sku-1', 'ratingDesc', 3)

    act(() => {
      result.current.onOrderNoChange('ORD-1')
      result.current.onRatingChange(5)
      result.current.onContentChange('충분히 긴 상품평 내용 테스트')
    })
    act(() => result.current.onSubmit())

    await waitFor(() => expect(result.current.writeStatus).toBe('success'))
    expect(result.current.orderNo).toBe('')
    expect(result.current.rating).toBe(0)
    expect(result.current.content).toBe('')
    // 재작업(round3 사람 지시 2, QA round2 low 2) — sort/page(applied)는 작성 성공에 낙관적으로
    // 딸려오지 않는다. 뒤따르는 재조회(target='latest'/1)가 실제로 성공한 뒤에만 갱신된다 —
    // 그래야 재조회가 실패할 때 화면(정렬 버튼·페이지 표시)이 실제 목록보다 앞서가지 않는다.
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(3))
    expect(mockFetchReviews).toHaveBeenLastCalledWith('sku-1', 'latest', 1)
    await waitFor(() => expect(result.current.sort).toBe('latest'))
    expect(result.current.page).toBe(1)
  })

  // 재작업(round3 사람 지시 2, QA round2 low 2) — 작성 성공 뒤 재조회(latest/1)가 실패하면 표시값
  // (sort/page)은 직전 성공값(ratingDesc/3)에 그대로 남는다 — 작성 성공 자체가 표시값을 앞당기지 않는다.
  test('작성 성공 뒤 재조회가 실패하면 표시값(sort/page)은 직전 성공값에 남는다', async () => {
    mockFetchReviews.mockResolvedValueOnce(oneItemList) // 최초(latest,1)
    mockFetchOrders.mockResolvedValue([])
    const created: ReviewCreateResult = {
      reviewId: 43, sku: 'sku-1', orderNo: 'ORD-1', rating: 5, content: '충분히 긴 상품평 내용 테스트', createdAt: '2026-09-27T00:00:00Z',
    }
    mockCreateReview.mockResolvedValue(created)

    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session, enabled: true }))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(1))

    mockFetchReviews.mockResolvedValueOnce(oneItemList) // (ratingDesc,3) 성공
    act(() => {
      result.current.onSortChange('ratingDesc')
      result.current.onPageChange(3)
    })
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(2))
    await waitFor(() => expect(result.current.sort).toBe('ratingDesc'))
    expect(result.current.page).toBe(3)

    act(() => {
      result.current.onOrderNoChange('ORD-1')
      result.current.onRatingChange(5)
      result.current.onContentChange('충분히 긴 상품평 내용 테스트')
    })
    mockFetchReviews.mockRejectedValueOnce(new Error('boom')) // 작성 직후 재조회(latest,1)는 실패시킨다.
    act(() => result.current.onSubmit())

    await waitFor(() => expect(result.current.writeStatus).toBe('success'))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(3))
    expect(mockFetchReviews).toHaveBeenLastCalledWith('sku-1', 'latest', 1)
    await waitFor(() => expect(result.current.listStatus).toBe('error'))
    // 재조회가 실패했으므로 표시값은 여전히 작성 전 직전 성공값(ratingDesc, 3)이다.
    expect(result.current.sort).toBe('ratingDesc')
    expect(result.current.page).toBe(3)
    // 직전 목록(oneItemList)은 그대로 남아 있다(전체 대체가 아니다).
    expect(result.current.listResult).toBe(oneItemList)
  })

  test('삭제 성공 시 재조회하고, 빈 페이지가 남으면 이전 페이지로 내려가 다시 조회한다', async () => {
    const pageTwoWithOneItem = emptyList({
      totalCount: 11, page: 2, size: 10, reviewCount: 11,
      items: [{ reviewId: 5, rating: 4, content: '삭제될 리뷰입니다 테스트', authorName: '홍*동', createdAt: '2026-09-01T00:00:00Z' }],
    })
    const page1 = emptyList({
      totalCount: 10, page: 1, size: 10, reviewCount: 10,
      items: [{ reviewId: 1, rating: 5, content: '1페이지 리뷰입니다 테스트', authorName: '김*수', createdAt: '2026-08-01T00:00:00Z' }],
    })
    const page2Empty = emptyList({ totalCount: 10, page: 2, size: 10, reviewCount: 10, items: [] })

    let deleted = false
    mockFetchReviews.mockImplementation((_sku: string, _sort: string, page: number) => {
      if (page === 1) return Promise.resolve(page1)
      return Promise.resolve(deleted ? page2Empty : pageTwoWithOneItem)
    })
    mockDeleteReview.mockImplementation(async () => { deleted = true })

    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session, enabled: true }))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(1)) // 최초(1페이지)

    act(() => result.current.onPageChange(2))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(2))
    await waitFor(() => expect(result.current.listResult?.items[0]?.reviewId).toBe(5))

    act(() => result.current.onDeleteRequest(5))
    act(() => result.current.onDeleteConfirm())
    await waitFor(() => expect(mockDeleteReview).toHaveBeenCalledTimes(1))
    // 삭제 성공 → 같은 페이지(2) 재조회(3번째 호출, 0건) → 빈 페이지 클램프로 1페이지 재조회(4번째 호출).
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(4))
    await waitFor(() => expect(result.current.page).toBe(1))
    await waitFor(() => expect(result.current.listResult?.items[0]?.reviewId).toBe(1))
  })

  // 재작업(round2 사람 지시 3) — 서버 validateContent는 trim하지 않은 length로 본다("abc"+공백 7자,
  // 총 10자는 서버 기준 통과). 클라이언트 검증도 같은 기준(비trim length)이어야 하고, 전송값도 그대로
  // (비trim) 나가야 한다(QA CONCERNS 권고 3 — 이전에는 trim한 길이로 막아 서버는 받을 값을 거절했다).
  test('본문 길이 검증 — trim 없이 length 기준(공백 포함 정확히 10자는 통과, 전송값도 비trim 그대로)', async () => {
    mockFetchReviews.mockResolvedValue(emptyList())
    mockFetchOrders.mockResolvedValue([])
    const paddedContent = 'abc       ' // 'abc'(3) + 공백 7 = 정확히 10자, trim하면 3자
    const created: ReviewCreateResult = {
      reviewId: 51, sku: 'sku-1', orderNo: 'ORD-1', rating: 5, content: paddedContent, createdAt: '2026-09-27T00:00:00Z',
    }
    mockCreateReview.mockResolvedValue(created)
    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session, enabled: true }))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(1))

    act(() => {
      result.current.onOrderNoChange('ORD-1')
      result.current.onRatingChange(5)
      result.current.onContentChange(paddedContent)
    })
    act(() => result.current.onSubmit())

    await waitFor(() => expect(result.current.writeStatus).toBe('success'))
    expect(result.current.fieldErrors.content).toBeUndefined()
    expect(mockCreateReview).toHaveBeenCalledWith(session.apiKey, 'sku-1', { rating: 5, content: paddedContent, orderNo: 'ORD-1' })
  })

  test('작성 400 RVW-4001 — "field: 설명" 파싱이 정확한 필드로 가고, 파싱 실패는 배너로 대체된다', async () => {
    mockFetchReviews.mockResolvedValue(emptyList())
    mockFetchOrders.mockResolvedValue([])
    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session, enabled: true }))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(1))

    const fillValidFields = () => act(() => {
      result.current.onOrderNoChange('ORD-1')
      result.current.onRatingChange(5)
      result.current.onContentChange('충분히 긴 상품평 내용 테스트')
    })

    fillValidFields()
    mockCreateReview.mockRejectedValueOnce(new ApiError(400, { code: 'RVW-4001', message: 'rating: 별점은 1~5 사이여야 합니다' }))
    act(() => result.current.onSubmit())
    await waitFor(() => expect(result.current.writeStatus).toBe('error'))
    expect(result.current.fieldErrors.rating).toBe('별점은 1~5 사이여야 합니다')

    fillValidFields()
    mockCreateReview.mockRejectedValueOnce(new ApiError(400, { code: 'RVW-4001', message: 'content: 본문은 10자 이상이어야 합니다' }))
    act(() => result.current.onSubmit())
    await waitFor(() => expect(result.current.fieldErrors.content).toBe('본문은 10자 이상이어야 합니다'))

    fillValidFields()
    mockCreateReview.mockRejectedValueOnce(new ApiError(400, { code: 'RVW-4001', message: 'orderNo: 유효하지 않은 주문번호입니다' }))
    act(() => result.current.onSubmit())
    await waitFor(() => expect(result.current.fieldErrors.orderNo).toBe('유효하지 않은 주문번호입니다'))

    fillValidFields()
    mockCreateReview.mockRejectedValueOnce(new ApiError(400, { code: 'RVW-4001', message: '알 수 없는 형식의 오류 메시지' }))
    act(() => result.current.onSubmit())
    await waitFor(() => expect(result.current.bannerMessage).toBe('알 수 없는 형식의 오류 메시지'))
  })

  test('작성 403/409 — 고정 문구로 매핑되고, 409에도 입력값이 유지된다', async () => {
    mockFetchReviews.mockResolvedValue(emptyList())
    mockFetchOrders.mockResolvedValue([])
    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session, enabled: true }))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(1))

    act(() => {
      result.current.onOrderNoChange('ORD-1')
      result.current.onRatingChange(4)
      result.current.onContentChange('충분히 긴 상품평 내용 테스트')
    })

    mockCreateReview.mockRejectedValueOnce(new ApiError(403, { code: 'RVW-4031', message: '구매 이력 없음' }))
    act(() => result.current.onSubmit())
    await waitFor(() => expect(result.current.bannerMessage).toBe('이 상품을 구매한 회원만 작성할 수 있습니다'))

    mockCreateReview.mockRejectedValueOnce(new ApiError(409, { code: 'RVW-4091', message: '중복' }))
    act(() => result.current.onSubmit())
    await waitFor(() => expect(result.current.bannerMessage).toBe('이미 이 주문으로 작성한 상품평이 있습니다'))
    expect(result.current.orderNo).toBe('ORD-1')
    expect(result.current.rating).toBe(4)
    expect(result.current.content).toBe('충분히 긴 상품평 내용 테스트')
  })

  test('삭제 403/404 — 고정 문구로 매핑되고, 404만 목록을 재조회한다', async () => {
    mockFetchReviews.mockResolvedValue(emptyList({
      totalCount: 1, reviewCount: 1,
      items: [{ reviewId: 7, rating: 3, content: '삭제 대상 리뷰 테스트', authorName: '홍*동', createdAt: '2026-09-01T00:00:00Z' }],
    }))
    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session, enabled: true }))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(1))

    mockDeleteReview.mockRejectedValueOnce(new ApiError(403, { code: 'RVW-4031', message: '타인 소유' }))
    act(() => result.current.onDeleteRequest(7))
    act(() => result.current.onDeleteConfirm())
    await waitFor(() => expect(result.current.deleteErrorMessage).toBe('본인이 작성한 상품평만 삭제할 수 있습니다'))
    expect(mockFetchReviews).toHaveBeenCalledTimes(1) // 403은 재조회하지 않는다

    mockDeleteReview.mockRejectedValueOnce(new ApiError(404, { code: 'RVW-4041', message: '없음' }))
    act(() => result.current.onDeleteRequest(7))
    act(() => result.current.onDeleteConfirm())
    await waitFor(() => expect(result.current.deleteErrorMessage).toBe('이미 삭제되었거나 존재하지 않는 상품평입니다'))
    await waitFor(() => expect(mockFetchReviews).toHaveBeenCalledTimes(2)) // 404는 최신 상태로 재조회한다
  })

  test('eligibleOrders — 취소 주문은 제외하고 이 SKU를 포함한 주문만 남긴다', async () => {
    mockFetchReviews.mockResolvedValue(emptyList())
    const rowOk = orderRow(1)
    const rowCanceled = orderRow(2, { orderState: 'CANCELED' })
    const rowOtherSku = orderRow(3)
    mockFetchOrders.mockResolvedValue([rowOk, rowCanceled, rowOtherSku])
    mockFetchOrder.mockImplementation((orderNo: string) => {
      if (orderNo === rowOk.orderNo) return Promise.resolve(orderDetailFor(rowOk, [{ productNo: 'sku-1', productName: 'p', quantity: 1, unitPrice: 1000 }]))
      if (orderNo === rowOtherSku.orderNo) return Promise.resolve(orderDetailFor(rowOtherSku, [{ productNo: 'sku-other', productName: 'p2', quantity: 1, unitPrice: 1000 }]))
      return Promise.reject(new Error('unexpected fetchOrder call: ' + orderNo))
    })

    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session, enabled: true }))
    await waitFor(() => expect(result.current.orderLookupStatus).toBe('ready'))
    expect(result.current.eligibleOrders).toEqual([{ orderNo: rowOk.orderNo, orderedAt: rowOk.orderedAt }])
    expect(mockFetchOrder).not.toHaveBeenCalledWith(rowCanceled.orderNo) // 취소 주문은 상세조회 자체를 안 한다
  })

  test('eligibleOrders — 주문이 아예 없으면 즉시 ready·0건(구매 이력 없음 판정의 전제)', async () => {
    mockFetchReviews.mockResolvedValue(emptyList())
    mockFetchOrders.mockResolvedValue([])
    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session, enabled: true }))
    await waitFor(() => expect(result.current.orderLookupStatus).toBe('ready'))
    expect(result.current.eligibleOrders).toEqual([])
    expect(mockFetchOrder).not.toHaveBeenCalled()
  })

  test('eligibleOrders — 자격 조회 자체가 실패하면 error(구매 이력 없음 403 문구로 오판하지 않는다)', async () => {
    mockFetchReviews.mockResolvedValue(emptyList())
    mockFetchOrders.mockRejectedValue(new Error('network'))
    const { result } = renderHook(() => useProductReviews({ sku: 'sku-1', session, enabled: true }))
    await waitFor(() => expect(result.current.orderLookupStatus).toBe('error'))
    expect(result.current.eligibleOrders).toEqual([])
  })
})
