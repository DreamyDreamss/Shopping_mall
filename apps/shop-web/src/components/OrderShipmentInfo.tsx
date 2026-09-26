// LAB-105.1 — 주문 상세 "배송 정보" 블록(택배사·송장번호·[배송 조회] 링크·송장 복사).
// "배송 이력" 섹션(deliveries[])과는 별도 파생 배열(shipments[])이라 그 섹션의 문구·표시는
// 건드리지 않는다(공존). 직접 fetch 없음 — shipments prop만 받는다(web-fetch-only-in-api 준수).
import { useState } from 'react'
import type { OrderShipment } from '../types'
import { carrierTrackingUrl } from '../features/shop/carrierTracking'

export interface OrderShipmentInfoProps {
  // `OrderDetail.shipments`는 계약상 필수(`OrderShipment[]`, types.ts)이지만, 그 계약을 어긴 런타임
  // 응답(키 자체가 없는 경우)을 방어적으로 구분해야 해서 이 prop 타입만 `| undefined`를 허용한다
  // (round2 재작업 지시 1 — AC 68). `OrderDetailCard`는 `order.shipments`를 `??`로 뭉개지 않고 그대로
  // 넘긴다.
  shipments: OrderShipment[] | undefined
}

const rowStyle = { padding: '7px 10px', borderBottom: '1px solid #eef0f2', fontSize: 13.5 }

/**
 * 배송 정보 — 세 상태를 가른다(round2 재작업 지시 1, AC 68·66 — round1 QA FAIL 필수수정 1).
 * 1. `shipments === undefined`(응답에 키 자체가 없음 — 정보 부재) → "배송 정보를 불러오지 못했습니다".
 * 2. `shipments.length === 0`(정상 응답, 출고 전) → "출고 준비 중"(AC 66).
 * 3. 그 외 → 항목마다 한 줄(AC 65, 부분 출고 2건 이상 포함).
 * 조건 분기는 있지만 이 컴포넌트 자체는 훅이 없다 — 복사 상태는 항목별 `ShipmentRow`(비-export)가
 * 최상위에서 선언한다(hooks-rules 준수).
 */
export function OrderShipmentInfo({ shipments }: OrderShipmentInfoProps) {
  if (shipments === undefined) {
    return <div role="alert" style={{ color: '#912d2b', fontSize: 13 }}>배송 정보를 불러오지 못했습니다</div>
  }
  if (shipments.length === 0) {
    return <div style={{ color: '#666', fontSize: 13 }}>출고 준비 중</div>
  }
  return (
    <ul style={{ margin: 0, padding: 0, listStyle: 'none' }}>
      {shipments.map(s => <ShipmentRow key={s.deliveryNo} shipment={s} />)}
    </ul>
  )
}

type CopyStatus = 'idle' | 'done' | 'failed'

/**
 * 배송 정보 1건. `useState`는 조건/반복 뒤가 아니라 함수 최상위에서 부른다(hooks-rules must).
 * 송장번호가 없으면(READY 등) "송장 등록 대기"만 보이고 복사 버튼·[배송 조회] 링크는 렌더하지
 * 않는다(AC 67) — 이 이른 반환도 훅을 전부 부른 뒤에 온다.
 */
function ShipmentRow({ shipment }: { shipment: OrderShipment }) {
  const [copyStatus, setCopyStatus] = useState<CopyStatus>('idle')

  if (!shipment.invoiceNo) {
    return (
      <li style={rowStyle}>
        <span style={{ color: '#999' }}>송장 등록 대기</span>
      </li>
    )
  }

  const invoiceNo = shipment.invoiceNo
  const carrierLabel = shipment.carrierName ?? shipment.carrierCode
  const trackingUrl = carrierTrackingUrl(shipment.carrierCode, invoiceNo)

  const copy = () => {
    // 클립보드 권한 거부 등으로 reject되면 성공 표시와 별개 상태("복사하지 못했습니다")로 넘어간다.
    navigator.clipboard.writeText(invoiceNo)
      .then(() => setCopyStatus('done'))
      .catch(() => setCopyStatus('failed'))
  }

  return (
    <li style={rowStyle}>
      {carrierLabel && <span style={{ marginRight: 8, color: '#555' }}>{carrierLabel}</span>}
      <span style={{ fontVariantNumeric: 'tabular-nums' }}>{invoiceNo}</span>
      <button type="button" onClick={copy} style={{ marginLeft: 8 }}>복사</button>
      {copyStatus === 'done' && <span style={{ marginLeft: 6, fontSize: 12, color: '#136b2f' }}>복사됨</span>}
      {copyStatus === 'failed' && <span style={{ marginLeft: 6, fontSize: 12, color: '#912d2b' }}>복사하지 못했습니다</span>}
      {trackingUrl && (
        <a href={trackingUrl} target="_blank" rel="noreferrer" style={{ marginLeft: 8 }}>배송 조회</a>
      )}
    </li>
  )
}
