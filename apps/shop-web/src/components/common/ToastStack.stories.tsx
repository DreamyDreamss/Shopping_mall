// SR-310 — ToastStack 상태별(1개·3개·초과 시 제거) 스토리.
import { useRef, useState } from 'react'
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ToastStack } from './ToastStack'
import { pushToast, type ToastItem } from './toastQueue'

const meta = {
  title: '공통/ToastStack',
  component: ToastStack,
  tags: ['UIS-CMN-002'],
} satisfies Meta<typeof ToastStack>
export default meta

type Story = StoryObj<typeof meta>

function toast(id: string, message: string): ToastItem {
  return { id, message }
}

export const 한개: Story = {
  args: { toasts: [toast('1', '장바구니에 담았습니다')], onDismiss: () => {} },
}

export const 세개: Story = {
  args: {
    toasts: [toast('1', '첫 번째 알림'), toast('2', '두 번째 알림'), toast('3', '세 번째 알림')],
    onDismiss: () => {},
  },
}

/**
 * 초과 시 제거 — 공개 API(`ToastStack`)는 이미 상한 적용된 배열을 받는 프레젠테이션일 뿐이라,
 * 큐 관리 자체(4번째 삽입 시 가장 오래된 것 제거)를 보여주려면 이 스토리만 로컬 `useState` 데모
 * 래퍼로 `pushToast`를 실제 호출한다. "알림 추가" 버튼을 누르면 4번째부터 1번째 토스트가 사라진다.
 */
export const 초과시제거: Story = {
  args: { toasts: [], onDismiss: () => {} }, // render가 실제 상태를 관리해 무시된다(타입 요건 충족용)
  render: () => {
    function Demo() {
      const [toasts, setToasts] = useState<ToastItem[]>([
        toast('1', '알림 1'), toast('2', '알림 2'), toast('3', '알림 3'),
      ])
      const seqRef = useRef(4)
      const addToast = () => {
        const id = String(seqRef.current)
        seqRef.current += 1
        setToasts(prev => pushToast(prev, toast(id, `알림 ${id}`)))
      }
      const dismiss = (id: string) => setToasts(prev => prev.filter(t => t.id !== id))
      return (
        <div>
          <button type="button" onClick={addToast}>알림 추가</button>
          <ToastStack toasts={toasts} onDismiss={dismiss} />
        </div>
      )
    }
    return <Demo />
  },
}
