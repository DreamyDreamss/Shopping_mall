// SR-311 — 앱 셸 QR 팝업. 현재 페이지 URL을 담은 QR 코드를 보여준다(AC). `components/common/
// BottomSheet`(무변경 재사용)로 감싸고 `qrcode.react`의 `QRCodeSVG`로 로컬에서 SVG를 그린다 — 외부
// QR 생성 API를 쓰지 않아 네트워크 요청 자체가 없다(실패 사례집 SR-306 #1 대조, 아래 참조).
import { QRCodeSVG } from 'qrcode.react'
import { BottomSheet } from '../../components/common/BottomSheet'

export interface QrPopupProps {
  open: boolean
  onClose: () => void
}

export function QrPopup({ open, onClose }: QrPopupProps) {
  // HashRouter라 `window.location.href`에 현재 해시 경로까지 포함된다(AC "현재 페이지 URL").
  const url = window.location.href

  return (
    <BottomSheet open={open} onClose={onClose} ariaLabel="QR 코드">
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 'var(--space-4)' }}>
        <QRCodeSVG value={url} size={200} />
        <p style={{ margin: 0, fontSize: 'var(--text-xs)', color: 'var(--color-text-secondary)', wordBreak: 'break-all', textAlign: 'center' }}>
          {url}
        </p>
      </div>
    </BottomSheet>
  )
}
