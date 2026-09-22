// SR-302 — 푸터(사업자 정보·고객센터·약관 링크 영역, 정적).
export function ShopFooter() {
  return (
    <footer style={{
      borderTop: '1px solid #eef0f2', marginTop: 40, padding: '24px 18px',
      fontSize: 12, color: '#888', lineHeight: 1.7, fontFamily: 'system-ui, sans-serif',
    }}>
      <div>SL Shop(테스트베드) · 대표 홍길동 · 사업자등록번호 000-00-00000</div>
      <div>주소 서울특별시 어딘가 1길 1 · 통신판매업신고 제0000-서울-00000호</div>
      <div style={{ marginTop: 8 }}>
        고객센터 1544-0000(평일 09:00~18:00) · <span>이용약관</span> · <span>개인정보처리방침</span>
      </div>
    </footer>
  )
}
