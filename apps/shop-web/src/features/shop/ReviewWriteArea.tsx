// SR-351 — 작성 영역(로그인 여부 분기 + `ReviewWriteForm`). `ProductReviewsSection.tsx`에서 분리
// (STORY "파일" 절 300줄 상한 대비 조립 파일 크기 절감). 로그인 판정이 최우선(순서·보안 2) — 비로그인
// 이면 폼 자체를 그리지 않는다. fetch 없음(규칙 web-fetch-only-in-api).
import { ReviewWriteForm, type ReviewWriteFormProps } from './ReviewWriteForm'

export type { EligibleOrder, OrderLookupStatus, WriteFieldErrors, WriteStatus } from './ReviewWriteForm'

export interface ReviewWriteAreaProps extends ReviewWriteFormProps {
  loggedIn: boolean
}

const noticeBoxStyle: React.CSSProperties = {
  border: '1px solid #eef0f2', background: '#f8f9fa', color: '#555', borderRadius: 6,
  padding: '16px 18px', fontSize: 13,
}

export function ReviewWriteArea({ loggedIn, ...formProps }: ReviewWriteAreaProps) {
  if (!loggedIn) {
    return <div role="note" style={noticeBoxStyle}>로그인 후 작성할 수 있습니다</div>
  }
  return <ReviewWriteForm {...formProps} />
}
