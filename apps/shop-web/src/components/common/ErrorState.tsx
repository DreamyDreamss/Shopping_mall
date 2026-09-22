// SR-310 — 오류 상태 규격(bench/screenshot-11 404 구성): 원형 느낌표 아이콘 + 고정 문구 + 보조
// 문구 + [홈으로](테두리)·[돌아가기](검정). Button을 조립해 재사용한다(스타일 중복 정의 금지).
// 서버 원문 조합 로직은 이 컴포넌트에 넣지 않는다 — subMessage는 호출부가 고정 카피만 넣는다
// (실패 사례집 SR-234 FUNC-member-007 r1과 같은 함정을 예방).
import { Button } from './Button'

export interface ErrorStateProps {
  subMessage: string
  onHome: () => void
  onBack: () => void
}

export function ErrorState({ subMessage, onHome, onBack }: ErrorStateProps) {
  return (
    <div role="alert" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 'var(--space-4)', padding: 'var(--space-8) var(--space-4)' }}>
      <svg aria-hidden="true" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--color-error)" strokeWidth="1.5">
        <circle cx="12" cy="12" r="10" />
        <path d="M12 7v6" strokeLinecap="round" />
        <circle cx="12" cy="16.5" r="0.75" fill="var(--color-error)" stroke="none" />
      </svg>
      <p style={{ margin: 0, fontSize: 'var(--text-base)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-text)' }}>
        이용에 불편을 드려 죄송합니다.
      </p>
      <p style={{ margin: 0, fontSize: 'var(--text-sm)', color: 'var(--color-text-secondary)' }}>{subMessage}</p>
      <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
        <Button variant="secondary" onClick={onHome}>홈으로</Button>
        <Button variant="primary" onClick={onBack}>돌아가기</Button>
      </div>
    </div>
  )
}
