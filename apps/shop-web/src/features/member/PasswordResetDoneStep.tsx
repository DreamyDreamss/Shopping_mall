// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-007.md

/**
 * 완료 화면(4단계) — confirm(INF-MBR-007) 204 응답 후 표시. 서버가 이미 그 회원의 모든 기기를
 * 로그아웃시켰으므로(사이드이펙트) 여기서는 안내 문구와 로그인 이동 링크만 보여준다(AC5).
 *
 * `react-router-dom`의 `Link`가 아니라 순수 `<a href="#/login">`을 쓴다 — `App.tsx`가
 * `HashRouter`를 쓰므로 실제 앱에서도 이 앵커로 정상 이동하고, Storybook에서 Router 컨텍스트
 * 데코레이터 없이도(다른 부품 스토리와 동일 환경) 렌더가 깨지지 않는다.
 */
export function PasswordResetDoneStep() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12, maxWidth: 320 }}>
      <p style={{ fontSize: 14 }}>비밀번호가 변경되었습니다 — 모든 기기에서 로그아웃되었습니다.</p>
      <a href="#/login" style={{ fontSize: 13, color: '#0b4ea2' }}>로그인으로 이동</a>
    </div>
  )
}
