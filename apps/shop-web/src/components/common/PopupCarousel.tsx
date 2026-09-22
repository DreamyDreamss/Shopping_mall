// SR-310 — 이미지 팝업 캐러셀 + "오늘은 그만 보기". 슬라이드는 실제 이미지가 아니라 배경색 블록
// (`HeroBannerCarousel`과 같은 합성 데이터 패턴)이다 — 이 SR엔 팝업 운영 이미지 데이터가 없어
// (범위 밖, SR-339 몫) 실제 <img src="..."> 를 쓰면 백엔드 없이 홀로 렌더되는 Storybook에서 404
// 콘솔 오류가 나 축E가 막힌다(실패 사례집 SR-306 #1 r3와 같은 함정).
//
// 자동 넘김은 HeroBannerCarousel과 같은 패턴 — 의존성을 [open, slides.length]로 고정하고 함수형
// 업데이트, cleanup으로 StrictMode 이중 마운트에서 인터벌이 중복 등록되지 않게 한다. 포커스 트랩·
// Esc·복귀는 BottomSheet와 공유하는 `useFocusTrap`을 쓴다.
//
// round1 QA 재작업(low) —
//  (2) "오늘은 그만 보기"가 옵션 버튼 클릭 여부와 무관하게 항상 같은 모습이라 스토리에서 '체크됨'
//      상태를 구별해 보여줄 수 없었다. 로컬 UI 상태 `dismissChecked`로 체크박스형 표시를 추가한다
//      (계약 자체는 그대로 — 클릭 시 onClose 뒤 onDismissToday를 별도 호출하는 순서는 불변).
//  (3) 4초 자동 넘김이 스토리북에서도 계속 돌아 '마지막' 상태 스토리가 시간에 따라 바뀌는 문제 —
//      `autoAdvanceMs` prop(기본 4000, 0이면 정지)을 열어 스토리에서만 끌 수 있게 한다.
import { useEffect, useState } from 'react'
import { useFocusTrap } from './useFocusTrap'

export interface PopupSlide {
  id: string
  background: string
}

export interface PopupCarouselProps {
  open: boolean
  slides: PopupSlide[]
  onClose: () => void
  onDismissToday: () => void
  /** 스토리북에서 특정 슬라이드로 시작하기 위한 초기값. 기본은 0. */
  initialIndex?: number
  /** 자동 넘김 주기(ms). 0이면 자동 넘김을 하지 않는다(시간 의존 스토리 방지용). 기본 4000. */
  autoAdvanceMs?: number
}

const DEFAULT_AUTO_ADVANCE_MS = 4000

export function PopupCarousel({
  open, slides, onClose, onDismissToday, initialIndex = 0, autoAdvanceMs = DEFAULT_AUTO_ADVANCE_MS,
}: PopupCarouselProps) {
  const [index, setIndex] = useState(initialIndex)
  // "오늘은 그만 보기" 클릭 여부를 보여주기 위한 로컬 UI 상태(체크박스형 표시). 계약(onClose→
  // onDismissToday 호출 순서)과는 무관 — 순수 시각 표시용.
  const [dismissChecked, setDismissChecked] = useState(false)
  const { containerRef } = useFocusTrap(open, onClose)

  useEffect(() => {
    if (open) {
      setIndex(initialIndex)
      setDismissChecked(false)
    }
  }, [open, initialIndex])

  useEffect(() => {
    if (!open || slides.length <= 1 || autoAdvanceMs <= 0) return
    const id = setInterval(() => setIndex(i => (i + 1) % slides.length), autoAdvanceMs)
    return () => clearInterval(id)
  }, [open, slides.length, autoAdvanceMs])

  if (!open || slides.length === 0) return null

  const current = slides[index % slides.length]

  // Esc·오버레이 클릭·닫기 버튼 전부 이 하나로 모인다 — 닫기 자체는 onClose만 한다.
  const handleClose = () => onClose()
  // "오늘은 그만 보기" — 체크 표시(로컬 UI)를 먼저 켠 뒤, 닫기(onClose) 뒤에 억제 기록
  // (onDismissToday)을 별도 호출한다(둘을 하나로 묶지 않는다 — 향후 실제 영속화(SR-339)가 붙을 때
  // 닫기와 억제 기록을 독립적으로 재작업할 수 있게).
  const handleDismissToday = () => {
    setDismissChecked(true)
    onClose()
    onDismissToday()
  }

  return (
    <div
      style={{
        position: 'fixed', inset: 0, background: 'var(--color-dim)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 'var(--z-modal)',
      }}
      onClick={handleClose}
    >
      <div
        ref={containerRef}
        role="dialog"
        aria-modal="true"
        aria-label="이벤트 팝업"
        style={{ background: '#fff', borderRadius: 'var(--radius-md)', overflow: 'hidden', width: 320 }}
        onClick={e => e.stopPropagation()}
      >
        <div aria-label={`${index + 1}/${slides.length}번째 이미지`} style={{ background: current.background, height: 320 }} />
        <div
          style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            padding: 'var(--space-3) var(--space-4)',
          }}
        >
          <span style={{ fontSize: 'var(--text-xs)', color: 'var(--color-text-secondary)' }}>
            {index + 1} / {slides.length}
          </span>
          <button
            type="button"
            onClick={handleDismissToday}
            aria-pressed={dismissChecked}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 'var(--space-1)',
              border: 0, background: 'none', fontSize: 'var(--text-xs)',
              color: 'var(--color-text-secondary)', cursor: 'pointer',
            }}
          >
            <span
              aria-hidden="true"
              style={{
                display: 'inline-block', width: 12, height: 12, borderRadius: 2,
                border: `1px solid ${dismissChecked ? 'var(--color-text)' : 'var(--color-text-tertiary)'}`,
                background: dismissChecked ? 'var(--color-text)' : 'transparent',
              }}
            />
            오늘은 그만 보기
          </button>
          <button
            type="button"
            aria-label="닫기"
            onClick={handleClose}
            style={{ border: 0, background: 'none', fontSize: 18, cursor: 'pointer' }}
          >
            ✕
          </button>
        </div>
      </div>
    </div>
  )
}
