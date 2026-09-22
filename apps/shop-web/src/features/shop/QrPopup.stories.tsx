// SR-311 — 앱 셸 QR 팝업 상태(열림·닫힘). `qrcode.react`는 SVG를 로컬에서 그려 네트워크 요청이 없다
// (실패 사례집 SR-306 #1 대조 — 외부 QR 생성 API였다면 스토리북 콘솔 오류로 이어졌을 것).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { QrPopup } from './QrPopup'

const meta = {
  title: '쇼핑셸/QrPopup',
  component: QrPopup,
  tags: ['UIS-CMN-003'],
} satisfies Meta<typeof QrPopup>
export default meta

type Story = StoryObj<typeof meta>

export const 열림: Story = { args: { open: true, onClose: () => {} } }

export const 닫힘: Story = {
  args: { open: false, onClose: () => {} },
  tags: ['renders-nothing'],
}
