// SR-305 — 주문서(`/shop/order`) 결제 실패 안내. 확정 답변("재고 초과·빈 장바구니·주문 실패는 서버가
// 주는 기존 오류 코드를 사유 문구로 표시")대로 서버 `message`를 지어내지 않고 그대로 보여준다.
//
// 계약표에 있는 상태(400 빈 장바구니/404 회원없음/409 재고부족)만 명시 분기하고, 그 외(5xx 등
// 미정의 상태)는 하나의 default로 뭉치지 않되(SR-234 r1 사례 대조 — 미정의 코드를 default 분기로
// 뭉쳐 잘못된 전이가 생긴 사례) 서버 원문 대신 별도의 고정 안내로 처리한다 — 계약 밖 응답의 원문을
// 그대로 노출하지 않는다(사람 수정: "스택트레이스·내부 경로가 섞인 문자열은 표시하지 않는다").
// 400/404/409의 message도 혹시 그런 문자열이 섞여 있으면(장애 상황에서 에러 핸들러가 우회된 경우
// 대비) 같은 안전장치로 걸러 일반 문구로 대체한다.
//
// 재작업(round 2, QA FAIL 필수3) — 이 필터(`looksLikeInternalDetail`)는 원래 이 부품 안에만 있었다.
// `CartPage` 라인 오류·`CartPage`/`OrderPage` 로드 실패·우편번호 검색 실패도 같은 위험(서버
// `include-message: always` + 5xx 예외 메시지)에 노출돼 있어 `httpErrorMessage.ts`로 올려 공유한다.
//
// 재작업(round 3, QA CONCERNS 권고2) — 이 부품이 원래 갖고 있던 상태코드 허용목록(`KNOWN_STATUSES`)이
// `httpErrorMessage.ts`의 `toDisplayMessage` 주 방어선으로 승격됐다. 값이 갈라지면(예: 여기만 새 상태를
// 추가하고 저긴 빼먹으면) 두 곳이 서로 다른 기준으로 판정하는 사고가 나므로, 이 부품도 그 공용 상수를
// 그대로 가져다 쓴다(중복 정의 금지 — httpErrorMessage.ts와 동일 원칙).
import { ALLOWED_MESSAGE_STATUSES, GENERIC_ERROR_MESSAGE, looksLikeInternalDetail } from './httpErrorMessage'

export interface OrderFailureNoticeProps {
  status: number
  message: string | null
}

const boxStyle: React.CSSProperties = {
  border: '1px solid #e0b4b4', background: '#fff6f6', color: '#912d2b', borderRadius: 6,
  padding: '14px 16px', fontSize: 13,
}

export function OrderFailureNotice({ status, message }: OrderFailureNoticeProps) {
  const isKnownStatus = ALLOWED_MESSAGE_STATUSES.includes(status)
  const safeMessage = isKnownStatus && message && !looksLikeInternalDetail(message) ? message : null
  const text = safeMessage ?? GENERIC_ERROR_MESSAGE

  return (
    <div role="alert" style={boxStyle}>
      {text}
    </div>
  )
}
