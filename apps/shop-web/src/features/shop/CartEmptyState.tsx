// SR-305 — 장바구니(`/shop/cart`) 빈 상태("담긴 상품이 없습니다" + [쇼핑 계속하기]).
// `PasswordResetDoneStep.tsx`와 같은 이유로 `react-router-dom`의 `Link`가 아니라 순수
// `<a href="#/...">`를 쓴다(`App.tsx`가 `HashRouter`, Storybook에 Router 데코레이터가 없어도 렌더됨).
export function CartEmptyState() {
  return (
    <div style={{ padding: '48px 16px', textAlign: 'center', color: '#666' }}>
      <p style={{ fontSize: 14, marginBottom: 14 }}>담긴 상품이 없습니다</p>
      <a href="#/shop/products"
         style={{ display: 'inline-block', border: '1px solid #0b4ea2', borderRadius: 5, color: '#0b4ea2',
                  fontSize: 13, fontWeight: 700, padding: '8px 18px', textDecoration: 'none' }}>
        쇼핑 계속하기
      </a>
    </div>
  )
}
