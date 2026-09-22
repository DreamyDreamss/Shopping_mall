// SR-302 — 쇼핑 홈(메인). 배너·카테고리는 실제 배너 운영 도구·카테고리 테이블/API가 없어(확정 답변
// scope_freeze) 정적 목업 데이터로 둔다. 상용 사이트 로고·문구를 그대로 쓰지 않는다(요구사항 제약).

export interface BannerSlide {
  id: string
  title: string
  subtitle: string
  /** 실제 이미지 자산이 없어 색 배경으로 대신한다(디자인 자산 자체 제작 확정 답변). */
  background: string
}

export interface CategoryShortcut {
  id: string
  label: string
  icon: string
}

export const BANNER_SLIDES: BannerSlide[] = [
  { id: 'banner-1', title: '가을 신상 모음', subtitle: '새 시즌 상품을 만나보세요', background: '#0b4ea2' },
  { id: 'banner-2', title: '주간 특가', subtitle: '이번 주 한정 할인 상품', background: '#136b2f' },
  { id: 'banner-3', title: '무료배송 이벤트', subtitle: '5만원 이상 구매 시 무료배송', background: '#8a5a00' },
]

export const CATEGORY_SHORTCUTS: CategoryShortcut[] = [
  { id: 'cat-fashion', label: '패션', icon: '👗' },
  { id: 'cat-beauty', label: '뷰티', icon: '💄' },
  { id: 'cat-living', label: '리빙', icon: '🛋️' },
  { id: 'cat-digital', label: '디지털', icon: '💻' },
  { id: 'cat-food', label: '식품', icon: '🍎' },
  { id: 'cat-sports', label: '스포츠', icon: '⚽' },
]

// SR-311 — 앱 셸(AppShell). GNB 탭·퀵바/하단탭바 노출 항목은 "설정값 한 곳"(이 파일)에서 관리한다
// (AC, STORY "파일" 절) — 화면 쪽 컴포넌트는 이 목록을 그대로 읽어 렌더만 한다.

export interface GnbTabConfig {
  key: string
  label: string
  /** 도착 화면이 있을 때만 채운다 — null이면 `implemented`가 false라는 뜻(숨김 대상). */
  path: string | null
  /** 도착 화면이 실제로 존재하는 탭만 true. false인 탭은 GNB에서 숨긴다(AC). */
  implemented: boolean
  /** true면 탭 위에 빨간 라벨(신규/알림 표시)을 그린다. 현재는 어느 탭도 켜지 않는다. */
  badge?: boolean
}

/**
 * 요구사항 배경의 9개 라벨 그대로 순서를 유지한다 — 지금은 '홈'만 도착 화면이 있어
 * `implemented:true`, 나머지는 `path:null`로 숨긴다(AC "현재는 '홈'과 존재하는 화면만 노출").
 */
export const GNB_TAB_CONFIG: GnbTabConfig[] = [
  { key: 'schedule', label: '편성표', path: null, implemented: false },
  { key: 'tvShopping', label: 'TV쇼핑', path: null, implemented: false },
  { key: 'specialSale', label: '특가쇼', path: null, implemented: false },
  { key: 'home', label: '홈', path: '/shop', implemented: true },
  { key: 'weekendBonus', label: '주말엔보너스', path: null, implemented: false },
  { key: 'brandHall', label: '브랜드관', path: null, implemented: false },
  { key: 'ranking', label: '랭킹', path: null, implemented: false },
  { key: 'vipLounge', label: 'VIP라운지', path: null, implemented: false },
  { key: 'benefit', label: '혜택/이벤트', path: null, implemented: false },
]

export interface QuickNavItem {
  key: 'home' | 'onAir' | 'category' | 'my'
  label: string
  /** false면 좌측 퀵바·하단 탭바 둘 다에서 숨긴다(같은 설정값 재사용, STORY "파일" 절 — 사람 확인). */
  implemented: boolean
}

/** 좌측 퀵바(≥1200px)·하단 탭바(<750px)가 함께 쓰는 항목 목록. ON AIR는 SR-319, 카테고리는
 * SR-237 전까지 숨긴다(확정문답 scr_entry). '마이'는 세션 유무로 목적지만 갈리고 항목 자체는 항상 노출. */
export const QUICK_NAV_ITEMS: QuickNavItem[] = [
  { key: 'home', label: '홈', implemented: true },
  { key: 'onAir', label: 'ON AIR', implemented: false },
  { key: 'category', label: '카테고리', implemented: false },
  { key: 'my', label: '마이', implemented: true },
]
