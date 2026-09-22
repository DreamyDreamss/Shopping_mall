# LAB-104 의사결정 기록

## D10. CSV 내보내기 계약 (2026-08-23)
- **개발팀 제안**:
  - `GET /api/orders/export?memberId=&status=` — [[INF-ORD-003]]과 동일 필터 파라미터·동일
    정렬(최신순)·동일 상시필터(del_yn='N' — DAO 재사용으로 "삭제 주문 제외" 충족).
  - 응답: 200 `text/csv; charset=UTF-8`, `Content-Disposition: attachment; filename="orders_{yyyyMMdd_HHmmss}.csv"`.
  - 본문: **UTF-8 BOM(EF BB BF) 선두** + 헤더 행 `orderNo,memberId,status,totalAmount,orderedAt`
    (목록 응답 필드와 동일 축) + 데이터 행. 필드 내 콤마·따옴표·개행은 RFC 4180 인용 처리.
  - 규모: 랩 데이터 소규모 — 메모리 조립(스트리밍은 후속 판단). 빈 결과도 200 + 헤더 행만.
  - 오류: 잘못된 status 값은 목록 API와 동일 거동(빈 결과) — 새 오류 계약을 만들지 않는다.
  - **범위 제외**: 화면 버튼(요구 없음 — 후속 SR 후보), 페이징/최대행 제한(소규모 전제).
- **결정**: 제안대로 확정. 결정자: 발주자 대행 [자기승인]

## D11. D10 정정 — 파라미터 축·CSV 방어 (2026-08-23, QA r1 FAIL 지적)
- **경위**: D10이 AS-IS 목록 API의 실제 파라미터명(`orderState` — INF-ORD-003 정본)을 대조하지
  않고 `status`로 명명 → 구현이 계약서대로 만들었는데 요구("현재 필터 조건 그대로")가 깨짐.
  분석 단계 결함이 계약서를 타고 코드까지 전파된 사례(QA 라이브 실측: orderState= 필터 무효).
- **정정**: 필터 파라미터 정본은 **`orderState`**(목록 API와 동일 축), `status`는 별칭으로 수용.
  CSV 헤더 열 이름은 `status` 유지(응답 필드 축 — 목록 응답과 동일).
- **추가 결정**: ①`orderedAt`은 `ISO_LOCAL_DATE_TIME` 고정 패턴 ②CSV 수식 인젝션 방어 —
  선두 `=`,`+`,`-`,`@` 필드는 작은따옴표 프리픽스(OWASP 권고 계열).
- **이월**: 무인증 대량 반출(SR-203 IDOR/CSRF 묶음 후속 SR)·행 상한/스트리밍·memberName.
- 결정자: 발주자 대행 [자기승인]

## D12. D11 근거 정정 — CSV 헤더 열 이름 (2026-08-23, QA r2 권고 #1)
- D11이 "CSV 헤더 `status` 유지" 근거를 "목록 응답 필드 축과 동일"이라고 적었으나 **거짓** —
  목록 응답의 실제 필드명은 `orderState`다(라이브 JSON 실측). r1 FAIL을 만든 *AS-IS 미대조*가
  응답 컬럼 축에서 반복된 사례(문서 결함, 구현은 AC-3와 정합).
- **정정**: 헤더 열 이름은 `status` **유지**하되, 근거는 "수용기준 AC-3에서 확정한 CSV 열 축"
  (요청 파라미터 축과 응답 열 축은 별개 계약)으로 바로잡는다. INF-ORD-015 역생성 시
  "요청: orderState(+status 별칭) / CSV 열: status"로 명문화할 것.
- 결정자: 발주자 대행 [자기승인]
