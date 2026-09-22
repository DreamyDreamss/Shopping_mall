// SR-309 — 디자인 토큰 카탈로그 메타(스토리북 'Design Tokens' 문서 페이지가 쓴다).
// 값의 정본은 tokens.css 하나뿐이다 — 여기에는 CSS 변수명·라벨·용도만 담고 색상 실값을
// 다시 적지 않는다(이중관리 금지). 실제 적용된 값은 스토리에서
// getComputedStyle(document.documentElement)로 읽는다.

export interface TokenCatalogEntry {
  cssVar: string
  label: string
  usage: string
}

export const colorTokens: TokenCatalogEntry[] = [
  { cssVar: '--color-text', label: '본문 텍스트', usage: '기본 본문 글자색' },
  { cssVar: '--color-text-secondary', label: '보조 텍스트', usage: '설명·캡션 등 부차 정보' },
  { cssVar: '--color-text-tertiary', label: '3차 텍스트', usage: '비활성·플레이스홀더' },
  { cssVar: '--color-surface-1', label: '면 1', usage: '옅은 배경면(카드·구분 영역)' },
  { cssVar: '--color-surface-2', label: '면 2', usage: '더 옅은 배경면' },
  { cssVar: '--color-surface-3', label: '면 3', usage: '가장 옅은 배경면(페이지 바탕 등)' },
  { cssVar: '--color-primary', label: '포인트 보라', usage: '주요 강조·CTA 텍스트/아이콘' },
  { cssVar: '--color-primary-bg', label: '포인트 배경', usage: '포인트 보라의 옅은 배경' },
  { cssVar: '--color-link', label: '링크 파랑', usage: '텍스트 링크' },
  { cssVar: '--color-price', label: '가격/할인 빨강', usage: '할인율·판매가 강조' },
  { cssVar: '--color-brand', label: '브랜드 레드', usage: '브랜드 로고·강조 요소' },
  { cssVar: '--color-dim', label: '딤 처리', usage: '모달·오버레이 배경 딤(rgba)' },
  { cssVar: '--color-success', label: '성공', usage: '성공 상태 텍스트(DeliveryBadge 정합)' },
  { cssVar: '--color-success-bg', label: '성공 배경', usage: '성공 배지 배경' },
  { cssVar: '--color-warning', label: '경고', usage: '경고 상태 텍스트(DeliveryBadge 정합)' },
  { cssVar: '--color-warning-bg', label: '경고 배경', usage: '경고 배지 배경' },
  { cssVar: '--color-error', label: '오류', usage: '오류 상태 텍스트' },
  { cssVar: '--color-error-bg', label: '오류 배경', usage: '오류 배지 배경' },
  { cssVar: '--color-info', label: '정보', usage: '정보 상태 텍스트' },
  { cssVar: '--color-info-bg', label: '정보 배경', usage: '정보 배지 배경' },
]

export const gradientTokens: TokenCatalogEntry[] = [
  { cssVar: '--gradient-primary', label: '그라데이션 (파랑→보라)', usage: '구매 버튼·헤더 토글' },
  { cssVar: '--gradient-accent', label: '그라데이션 (보라→파랑)', usage: '최근 본 상품 헤더' },
]

export const typographyTokens: TokenCatalogEntry[] = [
  { cssVar: '--text-2xs', label: '텍스트 2XS', usage: '가장 작은 보조 정보' },
  { cssVar: '--text-xs', label: '텍스트 XS', usage: '캡션·라벨' },
  { cssVar: '--text-sm', label: '텍스트 SM', usage: '보조 본문' },
  { cssVar: '--text-base', label: '텍스트 기준(본문)', usage: '기본 본문(14px 기준)' },
  { cssVar: '--text-md', label: '텍스트 MD', usage: '강조 본문' },
  { cssVar: '--text-lg', label: '텍스트 LG', usage: '섹션 소제목' },
  { cssVar: '--text-xl', label: '텍스트 XL', usage: '카드/영역 제목' },
  { cssVar: '--text-2xl', label: '텍스트 2XL', usage: '페이지 제목' },
  { cssVar: '--text-display', label: '텍스트 디스플레이(40px대)', usage: '배너 카피용 굵은 헤드라인' },
]

export const fontWeightTokens: TokenCatalogEntry[] = [
  { cssVar: '--font-weight-regular', label: '굵기 Regular(400)', usage: '기본 본문' },
  { cssVar: '--font-weight-semibold', label: '굵기 SemiBold(600)', usage: '강조 텍스트·소제목' },
  { cssVar: '--font-weight-bold', label: '굵기 Bold(700)', usage: '헤드라인·강한 강조' },
]

export const spaceTokens: TokenCatalogEntry[] = [
  { cssVar: '--space-1', label: '간격 1', usage: '아이콘·텍스트 최소 간격' },
  { cssVar: '--space-2', label: '간격 2', usage: '인접 요소 사이' },
  { cssVar: '--space-3', label: '간격 3', usage: '컴포넌트 내부 패딩' },
  { cssVar: '--space-4', label: '간격 4', usage: '컴포넌트 사이 기본 간격' },
  { cssVar: '--space-5', label: '간격 5', usage: '섹션 내부 여백' },
  { cssVar: '--space-6', label: '간격 6', usage: '카드 패딩' },
  { cssVar: '--space-7', label: '간격 7', usage: '섹션 사이 간격' },
  { cssVar: '--space-8', label: '간격 8', usage: '페이지 상하 여백' },
]

export const radiusTokens: TokenCatalogEntry[] = [
  { cssVar: '--radius-sm', label: 'radius 작게', usage: '인풋·배지' },
  { cssVar: '--radius-md', label: 'radius 중간(8px)', usage: '카드 썸네일' },
  { cssVar: '--radius-lg', label: 'radius 크게', usage: '모달·큰 카드' },
]

export const shadowTokens: TokenCatalogEntry[] = [
  { cssVar: '--shadow-sm', label: '그림자 작게', usage: '카드 기본 그림자' },
  { cssVar: '--shadow-md', label: '그림자 중간', usage: '호버·드롭다운' },
  { cssVar: '--shadow-lg', label: '그림자 크게', usage: '모달·팝오버' },
]

export const zIndexTokens: TokenCatalogEntry[] = [
  { cssVar: '--z-base', label: 'z 기본층', usage: '평면 콘텐츠' },
  { cssVar: '--z-dropdown', label: 'z 드롭다운', usage: '드롭다운·셀렉트' },
  { cssVar: '--z-sticky', label: 'z 스티키', usage: '고정 헤더·스티키 요소' },
  { cssVar: '--z-overlay', label: 'z 오버레이', usage: '딤 배경' },
  { cssVar: '--z-modal', label: 'z 모달', usage: '모달·다이얼로그' },
  { cssVar: '--z-toast', label: 'z 토스트', usage: '토스트·알림(최상위)' },
]

export const layoutTokens: TokenCatalogEntry[] = [
  { cssVar: '--layout-content-width', label: '본문 폭(750px)', usage: '단일 컬럼 본문 최대 폭' },
]
