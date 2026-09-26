// SR-351 — 상품평 탭 컨테이너 상태(로딩/빈목록/조회실패/목록/비로그인/구매이력없음/주문조회실패/
// 작성409유지/삭제확인다이얼로그). 전부 컨트롤드 props로 렌더한다(네트워크 없음, `fetchReviews` 등
// 목 불필요, STORY "테스트" 절).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductReviewsSection } from './ProductReviewsSection'
import type { ReviewListResult } from '../../types'

const eligibleOrders = [{ orderNo: 'ORD-2026-0001', orderedAt: '2026-08-01T10:00:00Z' }]

const listResult: ReviewListResult = {
  totalCount: 2, page: 1, size: 10, avgRating: 4.5, reviewCount: 2,
  distribution: { '1': 0, '2': 0, '3': 0, '4': 1, '5': 1 },
  items: [
    { reviewId: 1, rating: 5, content: '배송이 빠르고 품질도 좋았습니다.', authorName: '홍*동', createdAt: '2026-09-20T10:00:00Z' },
    { reviewId: 2, rating: 4, content: '가격 대비 만족스러워요.', authorName: '김*수', createdAt: '2026-09-18T10:00:00Z' },
  ],
}

const meta = {
  title: '쇼핑상세/상품평/탭 컨테이너',
  component: ProductReviewsSection,
  args: {
    loggedIn: true,
    listStatus: 'ready', listResult, onRetryList: () => {}, isOwn: () => false,
    sort: 'latest', onSortChange: () => {}, page: 1, onPageChange: () => {},
    eligibleOrders, orderLookupStatus: 'ready', onRetryOrderLookup: () => {},
    orderNo: '', onOrderNoChange: () => {}, rating: 0, onRatingChange: () => {},
    content: '', onContentChange: () => {}, fieldErrors: {}, writeStatus: 'idle', onSubmit: () => {},
    deleteTarget: null, deleteStatus: 'idle', deleteErrorMessage: null,
    onDeleteRequest: () => {}, onDeleteCancel: () => {}, onDeleteConfirm: () => {},
  },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ProductReviewsSection>
export default meta

type Story = StoryObj<typeof meta>

/** 기본(목록) — 요약·정렬·아이템 2건·페이지네이션·작성 폼이 모두 보인다. */
export const 기본: Story = { tags: ['state:기본'] }

/** 로딩 — 목록 조회 진행 중. */
export const 로딩: Story = { args: { listStatus: 'loading', listResult: null } }

/** 빈목록 — "아직 상품평이 없습니다" + 평균 자리 '-'. */
export const 빈목록: Story = {
  args: {
    listResult: { totalCount: 0, page: 1, size: 10, avgRating: null, reviewCount: 0,
                  distribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 }, items: [] },
  },
}

/** 조회실패 — 최초 로드 자체가 실패(listResult 없음) — "상품평을 불러오지 못했습니다" + [다시 시도]가
 * 전체를 대체한다. */
export const 조회실패: Story = { args: { listStatus: 'error', listResult: null } }

/** 재조회실패목록있음 — 재작업(round2, QA CONCERNS 권고 1). 정렬 변경·페이지 이동 등으로 재조회가
 * 실패해도 직전 목록은 그대로 남고, 그 위에 인라인 경고+[다시 시도]가 뜬다(전체 대체가 아니다). */
export const 재조회실패목록있음: Story = { args: { listStatus: 'error' } }

/** 비로그인 — 작성영역이 "로그인 후 작성할 수 있습니다" 고정 문구. */
export const 비로그인: Story = { args: { loggedIn: false } }

/** 구매이력없음 — 작성영역이 403과 같은 의미의 고정 문구("이 상품을 구매한 회원만 작성할 수 있습니다"). */
export const 구매이력없음: Story = { args: { eligibleOrders: [] } }

/** 주문조회실패 — 사람 수정 결정 1번. 작성영역이 "주문 정보를 불러오지 못했습니다 — 다시 시도" +
 * 재시도 버튼(403 고정 문구와 다른 문구). */
export const 주문조회실패: Story = { args: { orderLookupStatus: 'error', eligibleOrders: [] } }

/** 작성409유지 — 사람 수정 결정 2번. 배너가 뜨고 입력 필드값(주문·별점·본문)이 초기화되지 않고 유지된다. */
export const 작성409유지: Story = {
  args: {
    orderNo: eligibleOrders[0].orderNo, rating: 4, content: '배송이 빠르고 좋았습니다.', writeStatus: 'error',
    bannerMessage: '이미 이 주문으로 작성한 상품평이 있습니다',
  },
}

/** 삭제확인다이얼로그 — 사람 수정 확정 문구(제목/본문/버튼) 그대로 렌더된다. */
export const 삭제확인다이얼로그: Story = { args: { deleteTarget: 1 } }
