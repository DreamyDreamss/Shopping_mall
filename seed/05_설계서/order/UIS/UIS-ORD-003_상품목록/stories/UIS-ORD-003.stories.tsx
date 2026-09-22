// linked_func: FUNC-order-009 — UIS-ORD-003 화면 상태 초안
//
// 이 파일은 Speclinker가 **UIS-ORD-003 화면설계서 §5(표시 조건)** 를 읽어 만든 **초안**입니다.
// 각 상태는 사람이 확인한 표시 조건 한 행에 대응합니다. props는 비어 있습니다 —
// 추측한 값을 채우면 "스토리는 있는데 실제 상태가 아닌 것"이 되고, 화면 축(stories)이
// 거짓으로 통과합니다. 실제 값을 채운 뒤 소스 저장소로 옮기세요(SR 경유).
import type { Meta, StoryObj } from '@storybook/react-vite'
// TODO: 이 화면을 그리는 컴포넌트를 import 하세요

const meta = {
  title: 'UIS-ORD-003',
  // component: <컴포넌트>,
  // Speclinker 링크 — 이 태그로 화면설계서·기능과 이어집니다(index.json에 실리는 것은 tags뿐).
  tags: ['UIS-ORD-003', 'FUNC-order-009'],
} satisfies Meta
export default meta

type Story = StoryObj<typeof meta>

/** 검색조건·목록·상세이동 링크 전체 — 조건 없음(항상 표시) — 소스에 `auth:` 슬롯·권한 분기 없음  (근거: `list.html` 전체, `ProductViewController.java:32-37`) */
export const 검색조건목록상세이동링크전체: Story = {
  args: {},   // TODO: 이 상태를 만드는 실제 props
}
