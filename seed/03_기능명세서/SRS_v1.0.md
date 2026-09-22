# SRS v1.0 — 소프트웨어 요구사항 명세서 (RECON, Screen-first)

> 화면(UIS) 1개 = SRS-F 1건 기준으로 집약했다. 도메인별 상세는 `domains/SRS_{도메인}.md` 참조.
> 근거 소스: `_tmp/funcs_index.json`(구조) + 해당 UIS spec.md / INF-*.md / SCH-*.md(내용 종합).

## 색인

| SRS-F | 화면명 | UIS-ID | 호출 INF | FUNC-ID |
|-------|--------|--------|----------|---------|
| SRS-F-001 | 주문 목록 | UIS-ORD-001 | INF-ORD-003 | FUNC-order-001 |
| SRS-F-002 | 주문 상세 | UIS-ORD-002 | INF-ORD-004,INF-ORD-006 | FUNC-order-002 |

## 도메인별 상세

- [SRS_order.md](domains/SRS_order.md) — SRS-F-001, SRS-F-002 (주문 도메인, 화면 2건)

## 범위·한계

- 이번 회차 소스 슬라이스에는 `order` 도메인 화면 2건만 확정되어 SRS-F도 2건이다(funcs_index.json 기준).
- 화면 라우트(`GET /order/list`, `GET /order/{orderNo}`)는 서버 렌더 진입점으로 INF 대상이 아니다(UIS §7).
  업무 흐름 서술 시 이 점을 구분해 표기했다.

- [member 도메인](domains/SRS_member.md) — spec_resync_check가 추가(AIDD 신규 기능)
