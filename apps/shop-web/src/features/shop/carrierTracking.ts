// LAB-105.1 — 주문 상세 배송 정보의 [배송 조회] 링크. URL 템플릿은 요구사항·스펙에 저장 위치 근거가
// 없어(표시 전용 파생 로직) DB가 아니라 이 상수 맵에 둔다. 랩 시드(V11)는 CJ만 쓰지만, URL 매칭
// 로직의 커버리지를 위해 게이트 회신(사람 수정 ②)에 따라 4개 택배사를 등록한다.

const CARRIER_TRACKING_URL: Record<string, (invoiceNo: string) => string> = {
  CJ: invoiceNo =>
    `https://www.doortodoor.co.kr/parcel/doortodoor.do?fsp_action=PARC_ACT_002&fsp_cmd=retrieveInvNoACT&invc_no=${encodeURIComponent(invoiceNo)}`,
  HANJIN: invoiceNo =>
    `https://www.hanjin.co.kr/kor/CMS/DeliveryMgr/WaybillResult.do?mCode=MN038&wblnumText2=${encodeURIComponent(invoiceNo)}`,
  LOTTE: invoiceNo =>
    `https://www.lotteglogis.com/home/reservation/tracking/linkView?InvNo=${encodeURIComponent(invoiceNo)}`,
  EPOST: invoiceNo =>
    `https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm?sid1=${encodeURIComponent(invoiceNo)}`,
}

/**
 * 택배사 조회 URL을 만든다. `carrierCode`·`invoiceNo` 둘 다 있고 등록된 택배사일 때만 값을
 * 반환하고, 그 외(미등록 코드·둘 중 하나라도 null)는 `null`이다 — 호출부는 `null`이면
 * [배송 조회] 링크 자체를 숨긴다(AC 63).
 *
 * round2 재작업 지시 4(low/security, round1 QA 권고 2) — `CARRIER_TRACKING_URL[carrierCode]`는 객체
 * 리터럴 인덱싱이라 `carrierCode`가 `'toString'`·`'constructor'` 같은 값이면 `Object.prototype`
 * 상속 함수가 잡혀 `null` 대신 쓰레기 href가 렌더될 수 있었다(값 출처가 DB VARCHAR(20)라 현실
 * 가능성은 낮고 주입·XSS는 아니었지만) — `Object.hasOwn`으로 그 상속 키를 걸러낸다.
 */
export function carrierTrackingUrl(carrierCode: string | null, invoiceNo: string | null): string | null {
  if (!carrierCode || !invoiceNo) return null
  if (!Object.hasOwn(CARRIER_TRACKING_URL, carrierCode)) return null
  return CARRIER_TRACKING_URL[carrierCode](invoiceNo)
}
