// SR-310 — BottomSheet·PopupCarousel이 공유하는 포커스 트랩·Esc 닫기·닫힌 뒤 포커스 복귀. 두 곳에
// 각각 구현하면 한쪽만 고쳐지는 사고(SR-309 #1 round3와 같은 계열)가 재발할 조건이 생겨, 한 곳으로
// 합쳐 원천 차단한다.
//
// React 19 StrictMode(dev, main.tsx)는 컴포넌트가 처음 마운트될 때 effect를 한 번 더 이중 실행한다
// (mount→cleanup→mount 시뮬레이션). "열릴 때 직전 포커스 요소를 캡처"하는 effect를 [open] 의존성만
// 으로 두면, 이 이중 실행 사이에 첫 실행이 이미 포커스를 컨테이너 안으로 옮겨버린 뒤 두 번째 실행이
// 그 옮겨진 요소를 "직전 포커스"로 잘못 캡처할 위험이 있다. 대응: "직전 포커스 캡처"는
// `wasOpenRef`(cleanup에서 리셋하지 않는 ref)로 "닫힘→열림" 전이당 최초 1회만 수행하고, "포커스
// 복귀"는 별도 effect의 cleanup(= open이 실제 false로 떨어질 때)에서만 수행한다 — 같은 effect
// 안에서 캡처와 복귀를 뒤섞지 않는다.
//
// round1 QA 재작업(medium) — 초기 포커스 이동 effect와 Esc/Tab 리스너 effect의 의존성에
// `onClose`가 섞여 있으면, 부모가 인라인 화살표 `onClose`를 넘기고 재렌더할 때마다(이 코드베이스의
// 실제 관용구 — `OrderPage.tsx` `onClose={() => setZipcodeModalOpen(false)}`) 신원이 바뀐 `onClose`
// 때문에 effect가 재실행돼 `initialFocusables[0].focus()`가 다시 돌아 사용자가 시트/팝업 안에서
// 잡고 있던 포커스를 매번 첫 포커스 가능 요소로 되돌린다. 대응: 초기 포커스 이동은 `[open]`에만
// 의존해 "닫힘→열림" 전이 시 1회만 수행하고, Esc/Tab 리스너는 최신 `onClose`를 `onCloseRef`로 읽어
// 리스너 등록 자체도 `[open]`에만 의존하게 분리한다(리스너 재등록 자체는 무해하지만, 굳이 매
// 렌더마다 등록·해제를 반복할 이유가 없다 — 등록은 open 전이당 1회로 충분).
import { useEffect, useRef } from 'react'

const FOCUSABLE_SELECTOR =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'

export function useFocusTrap(open: boolean, onClose: () => void) {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const triggerRef = useRef<HTMLElement | null>(null)
  const wasOpenRef = useRef(false)
  // 최신 onClose를 매 렌더마다 갱신되는 ref로 들고 있는다 — keydown 핸들러가 이 ref만 읽으면
  // 리스너 등록 자체(effect)는 onClose 신원 변화와 무관하게 [open]에만 의존할 수 있다.
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  // 직전 포커스 캡처 — "닫힘→열림" 전이당 최초 1회만.
  useEffect(() => {
    if (open && !wasOpenRef.current) {
      triggerRef.current = document.activeElement as HTMLElement | null
    }
    wasOpenRef.current = open
  }, [open])

  // 초기 포커스 이동 — open이 false→true로 바뀔 때 1회만 수행한다. onClose는 의존성에서 뺀다
  // (부모 재렌더로 onClose 신원만 바뀌어도 재실행되면 작업 중이던 내부 포커스가 튀는 회귀가 난다).
  useEffect(() => {
    if (!open) return
    const container = containerRef.current
    const initialFocusables = container ? Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)) : []
    initialFocusables[0]?.focus()
  }, [open])

  // Esc 닫기 + Tab 순환(포커스 트랩) — 리스너 등록은 [open]에만 의존하고, Esc 시 호출하는
  // onClose는 onCloseRef로 항상 최신 값을 읽는다.
  useEffect(() => {
    if (!open) return
    const container = containerRef.current

    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        onCloseRef.current()
        return
      }
      if (e.key !== 'Tab' || !container) return
      const nodes = Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR))
      if (nodes.length === 0) return
      const first = nodes[0]
      const last = nodes[nodes.length - 1]
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault()
        last.focus()
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [open])

  // 포커스 복귀 — open이 실제로 false로 떨어질 때(또는 언마운트)의 cleanup에서만 수행한다.
  useEffect(() => {
    if (!open) return
    return () => {
      triggerRef.current?.focus()
    }
  }, [open])

  return { containerRef }
}
