// SR-305 — 우편번호 검색 모달 상태들(검색전/결과있음/결과없음/검색중/조회실패).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ZipcodeSearchModal } from './ZipcodeSearchModal'

const meta = {
  title: '주문서/우편번호 검색',
  component: ZipcodeSearchModal,
  args: {
    open: true, query: '', onQueryChange: () => {}, onSearch: () => {}, results: [],
    onSelect: () => {}, onClose: () => {},
  },
  tags: ['UIS-ORD-011'],
} satisfies Meta<typeof ZipcodeSearchModal>
export default meta

type Story = StoryObj<typeof meta>

/** 검색전 — 결과 없음(아직 검색 안 함과 동일 표시). */
export const 검색전: Story = {}

/** 결과있음 — 목록에서 항목 선택 가능. */
export const 결과있음: Story = {
  args: {
    query: '테헤란로',
    results: [
      { zipcode: '06236', roadAddress: '서울 강남구 테헤란로 1', sido: '서울', sigungu: '강남구' },
      { zipcode: '06237', roadAddress: '서울 강남구 테헤란로 3', sido: '서울', sigungu: '강남구' },
    ],
  },
}

/** 결과없음 — 검색했지만 0건. */
export const 결과없음: Story = { args: { query: '존재하지않는주소xyz' } }

/** 검색중 — 로딩 표시. */
export const 검색중: Story = { args: { query: '테헤란로', loading: true } }

/** 조회실패 — 검색어 형식 오류 등 서버 사유 그대로 노출. */
export const 조회실패: Story = {
  args: { query: 'a', error: '검색어를 2자 이상 입력해 주세요' },
  tags: ['shows-error'],
}
