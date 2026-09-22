// SR-302 — 히어로 배너 캐러셀(자동 넘김·좌우 이동·현재 위치 표시).
import { useEffect, useState } from 'react'
import type { BannerSlide } from './shopStatic'

export interface HeroBannerCarouselProps {
  slides: BannerSlide[]
}

const AUTO_ADVANCE_MS = 5000

function navButtonStyle(side: 'left' | 'right'): React.CSSProperties {
  return {
    position: 'absolute', top: '50%', [side]: 10, transform: 'translateY(-50%)',
    border: 0, borderRadius: '50%', width: 32, height: 32, background: 'rgba(0,0,0,0.35)',
    color: '#fff', fontSize: 18, cursor: 'pointer', lineHeight: '32px', padding: 0,
  }
}

/**
 * 자동 넘김은 `useEffect` 의존성을 `[slides.length]`로 고정하고 함수형 업데이트
 * (`setIndex(i => (i + 1) % slides.length)`)를 쓴다 — 인덱스를 의존성에 넣으면 렌더마다 타이머가
 * 재시작돼 사실상 자동 넘김이 안 되고, cleanup(`clearInterval`)이 없으면 React 19 StrictMode(dev)
 * 마운트→언마운트→재마운트에서 인터벌이 중복 등록돼 두 배 속도로 넘어간다(STORY "프레임워크 실행
 * 모델 함정" 절).
 */
export function HeroBannerCarousel({ slides }: HeroBannerCarouselProps) {
  const [index, setIndex] = useState(0)

  useEffect(() => {
    if (slides.length <= 1) return
    const id = setInterval(() => setIndex(i => (i + 1) % slides.length), AUTO_ADVANCE_MS)
    return () => clearInterval(id)
  }, [slides.length])

  if (!slides.length) return null

  const current = slides[index % slides.length]
  const goPrev = () => setIndex(i => (i - 1 + slides.length) % slides.length)
  const goNext = () => setIndex(i => (i + 1) % slides.length)

  return (
    <div style={{ position: 'relative', borderRadius: 8, overflow: 'hidden', height: 220 }}>
      <div style={{
        background: current.background, color: '#fff', height: '100%',
        display: 'flex', flexDirection: 'column', justifyContent: 'center', padding: '0 32px',
      }}>
        <h2 style={{ fontSize: 24, margin: 0 }}>{current.title}</h2>
        <p style={{ fontSize: 14, marginTop: 8, opacity: 0.9 }}>{current.subtitle}</p>
      </div>
      {slides.length > 1 && (
        <>
          <button type="button" aria-label="이전 배너" onClick={goPrev} style={navButtonStyle('left')}>‹</button>
          <button type="button" aria-label="다음 배너" onClick={goNext} style={navButtonStyle('right')}>›</button>
          <div style={{ position: 'absolute', bottom: 10, left: 0, right: 0,
                        display: 'flex', justifyContent: 'center', gap: 6 }}>
            {slides.map((s, i) => (
              <button key={s.id} type="button" aria-label={`배너 ${i + 1}번으로 이동`} aria-current={i === index}
                      onClick={() => setIndex(i)}
                      style={{ width: 8, height: 8, borderRadius: '50%', border: 0, padding: 0,
                               background: i === index ? '#fff' : 'rgba(255,255,255,0.5)', cursor: 'pointer' }} />
            ))}
          </div>
        </>
      )}
    </div>
  )
}
