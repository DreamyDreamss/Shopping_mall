# SR-204 의사결정 기록

## D13. 인증·통제 계약 (2026-08-23)
- **개발팀 제안**:
  - 인증: `/api/**` 전역 서블릿 필터 — 헤더 `X-Api-Key`. 미제시/무효 → **401**
    `{"error":"unauthorized"}`. 키·회원 매핑은 앱 설정(application.yml `lab.api-keys`):
    `lab-admin-key`→`*`(전 회원), `lab-member-0001-key`→`M-0001`.
  - IDOR: member 스코프 키로 **타 회원 자원 접근 시 403** — memberId 파라미터/본문/경로가
    키의 회원과 불일치하면 거부(admin 키는 통과). 대조 지점: cart 계열·orders 계열·members/{id}.
  - export 상한: 결과 **1000행 초과 시 413** + `{"error":"payload_too_large","limit":1000}`
    (부분 반출로 위장하지 않는다 — 정직 거부).
  - 범위 제외: 화면 무인증 유지(데모 접근성)·CSRF(위 근거 — 01_분석서)·키 로테이션·요율 제한.
  - 테스트 편의: 기존 자동화 대다수는 인증 무관 검증 — @SpringBootTest 웹 계층 테스트에
    admin 키 헤더 기본 주입(테스트 유틸), 인증 자체 검증 테스트는 무키/오키/member키 케이스.
- **결정**: 제안대로 확정. 결정자: 발주자 대행 [자기승인]

## D14. 수행 방식 — 뷰어 구동 (2026-08-23)
- 이 SR의 dev는 **SpecLens 리뷰 드로어의 [▶ AIDD 백그라운드 실행](F-73)으로 기동**한다 —
  P8에서 LLM 비용으로 BLOCKED였던 뷰어 축(F-73·F-28 ask 인라인·F-174~176 AI 도크)을 실LLM으로
  함께 소진하는 이중 목적. 결정자: 발주자 대행 [자기승인]

## D15. IDOR 검증 방식 정정 (2026-08-23, QA r5/r6)
- D13이 IDOR을 \'memberId 토큰 대조\'로만 기술했으나, QA r5가 토큰 없는 자원(orders/{no}·
  members·export)이 member 키로 전부 통과함을 실측(우회). r6에서 **자원 소유권 검증**으로 전환:
  orders/{orderNo}는 DB member_id 대조, /api/members·/api/orders/export는 admin 전용,
  무토큰 GET /api/orders는 caller memberId로 스코프 축소. 이것이 정본.
- 경로 판정도 raw URI→서블릿 정규화(getServletPath+getPathInfo)+default-deny(QA r5 %61pi 우회).
- INF 15건의 401/403 오류 계약 재생성은 인증 실효 후 sl-sync --apply로 수행(드리프트 해소).
- 결정자: 발주자 대행 [자기승인]
