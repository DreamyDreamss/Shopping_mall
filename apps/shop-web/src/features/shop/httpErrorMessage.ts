// 재작업(round 2, QA FAIL 필수3) — 사람 수정 지시("스택트레이스·내부 경로가 섞인 문자열은 표시하지
// 않는다")의 필터가 `OrderFailureNotice` 한 곳에만 있어, `CartPage` 라인 오류·`CartPage` 로드 실패·
// `OrderPage` 로드 실패·우편번호 검색 실패 네 곳은 `OrderHttpError.message`(서버 `{message}` 원문)를
// 무필터로 그대로 렌더했다. `application.yml`의 `server.error.include-message: always`와 겹쳐 5xx
// 예외 메시지가 그대로 화면에 노출되는 환경이라, 표시 직전에 이 필터를 반드시 거친다 — 공용 헬퍼로
// 올려 위 4곳 + `OrderFailureNotice`가 전부 같은 함수 하나만 쓴다(중복 정의 금지).
//
// 재작업(round 3, QA CONCERNS 권고2) — 위 필터는 정규식 거부목록(자바 예외 클래스명·스택프레임·경로
// 토큰) 하나뿐이라, 계약 밖 5xx라도 그 패턴에 안 걸리는 메시지(예: NPE의
// `Cannot invoke "…Cart.getItems()" because "cart" is null`처럼 이 정규식이 못 잡는 형태, 또는 아예
// 평범한 문장처럼 보이는 예외 메시지)는 그대로 새어 나갔다. `OrderFailureNotice`가 이미 쓰던 **상태코드
// 허용목록**(계약에 정의된 4xx만 원문 노출) 방식을 주 방어선으로 끌어올린다 — 정규식 거부목록은 그
// 허용목록을 통과한 메시지에 대한 보조 수단으로만 남긴다.
//
// 재작업(SR-235 round2, QA FAIL 필수1·필수2) — `MyAddressesPage`가 이 필터를 우회해 우편번호 검색
// 실패를 `e.message` 원문으로 그대로 렌더했고(필수1), 배송지 CRUD 오류도 상태코드를 안 보는 자체
// `toAddressErrorMessage`를 새로 정의해 계약 밖 상태에서 서버 문구가 그대로 샐 수 있었다(필수2). 새
// 필터를 또 만드는 대신 이 함수 자체를 `ApiError`(`{code,message}` 계열, `login`/배송지 CRUD가 던짐)도
// 받도록 넓혀 소비처가 타입과 무관하게 이 함수 하나만 쓰게 한다(중복 정의 금지).
import { ApiError, OrderHttpError } from '../../api'

export const GENERIC_ERROR_MESSAGE = '일시적 오류입니다. 다시 시도해 주세요'

/**
 * 이 SR이 소비하는 API(GET/PATCH/DELETE `/api/cart*`, POST `/api/cart/checkout`, GET `/api/zipcodes`)의
 * 계약에 정의된 상태코드 — 서버 `message`를 화면에 그대로 보여줘도 되는 응답만 여기 있다. 계약 밖
 * 상태(5xx 등 어떤 컨트롤러도 명시 처리하지 않는 상태)는 메시지가 아무리 정상 문구처럼 보여도 이
 * 목록에 없으면 무조건 일반 문구로 대체한다(주 방어선).
 */
export const ALLOWED_MESSAGE_STATUSES = [400, 404, 409]

/** 정상 사유 문구는 갖지 않는 패턴(자바 예외 클래스명·스택프레임·파일시스템 경로)을 걸러낸다.
 * 상태코드 허용목록(위)이 주 방어선이고, 이건 그 목록을 통과한 메시지에 대한 보조 수단이다. */
export function looksLikeInternalDetail(message: string): boolean {
  return /Exception|\bat\s+\S+\(|[A-Za-z]:\\|\/(main|src)\/(java|com)\//i.test(message)
}

/**
 * 서버 호출 실패를 화면에 표시할 문구로 바꾼다. `OrderHttpError`/`ApiError` 둘 다 아니면(네트워크 오류
 * 등) 일반 문구, 둘 중 하나라도 상태코드가 계약 밖(허용목록에 없음)이거나 메시지가 내부 상세로 보이면
 * 일반 문구로 대체한다. 계약에 정의된 상태코드의 정상 메시지만 서버 원문 그대로 보여준다(확정 답변
 * "서버가 주는 기존 오류 코드를 사유 문구로 표시"). 두 타입 모두 `{status, message}` 형태만 쓰므로
 * 판정 로직은 하나로 공유한다 — 소비처가 `{code,message}`(`ApiError`)든 `{message}`(`OrderHttpError`)든
 * 이 함수 하나만 거치면 된다.
 */
export function toDisplayMessage(e: unknown): string {
  if (
    (e instanceof OrderHttpError || e instanceof ApiError) &&
    ALLOWED_MESSAGE_STATUSES.includes(e.status) &&
    e.message &&
    !looksLikeInternalDetail(e.message)
  ) {
    return e.message
  }
  return GENERIC_ERROR_MESSAGE
}
