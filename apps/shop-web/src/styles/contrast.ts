// SR-309 — WCAG 2.1 상대휘도·명도대비 순수함수. Design Tokens 스토리(배경×본문 대비표)가 쓴다.
// 부수효과 없음(DOM·전역 상태 접근 없음) — hex 색상 문자열만 받아 계산만 한다.

function hexToRgb(hex: string): [number, number, number] {
  const normalized = hex.trim().replace('#', '')
  const full = normalized.length === 3 ? normalized.split('').map(c => c + c).join('') : normalized
  if (!/^[0-9a-fA-F]{6}$/.test(full)) {
    // 비-hex 입력(rgba(...) 문자열, tokens.css 미로드 시 getPropertyValue의 빈 문자열 등)을
    // NaN으로 조용히 흘려보내지 않는다 — 호출부가 잘못된 값을 바로 알 수 있도록 명시 예외.
    throw new Error(`hexToRgb: '${hex}'는 유효한 hex 색상 문자열이 아니다(#rgb 또는 #rrggbb 형식만 허용)`)
  }
  const r = parseInt(full.slice(0, 2), 16)
  const g = parseInt(full.slice(2, 4), 16)
  const b = parseInt(full.slice(4, 6), 16)
  return [r, g, b]
}

function channelLuminance(channel8bit: number): number {
  const s = channel8bit / 255
  return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4)
}

/** WCAG 상대휘도(0~1). `#rgb`·`#rrggbb` 형식의 hex 문자열을 받는다. 그 외 형식(빈 문자열·rgba(...) 등)은 예외를 던진다. */
export function hexLuminance(hex: string): number {
  const [r, g, b] = hexToRgb(hex)
  return 0.2126 * channelLuminance(r) + 0.7152 * channelLuminance(g) + 0.0722 * channelLuminance(b)
}

/** 두 색의 명도 대비율(1~21). 인자 순서는 무관 — 더 밝은 색을 분자로 자동 정렬한다. */
export function contrastRatio(hexA: string, hexB: string): number {
  const lA = hexLuminance(hexA)
  const lB = hexLuminance(hexB)
  const lighter = Math.max(lA, lB)
  const darker = Math.min(lA, lB)
  return (lighter + 0.05) / (darker + 0.05)
}

/** WCAG AA 통과 여부 — 일반 텍스트 4.5:1, 큰 텍스트(24px 이상 또는 18.66px 이상 굵게) 3:1.
 * (WCAG 2.1 large-scale text 정의는 18pt/14pt bold 기준이며 1pt=1.333px로 환산하면
 * 24px/18.66px다 — round 2까지 "18px/14px bold"로 잘못 옮겨 적혀 있었다, round 3 재작업 지시 2.) */
export function meetsAA(ratio: number, isLargeText = false): boolean {
  return isLargeText ? ratio >= 3 : ratio >= 4.5
}
