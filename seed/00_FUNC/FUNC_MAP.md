<!-- _meta: {"schema": 1, "generated_at": "2026-09-17T14:57:08", "generator": "build_func_map@1", "sources": {"_tmp/funcs_index.json": "1789294620348294400:37187|02abdfee94360528a61503fc93a4a3c2aec938de", "docs/00_FUNC/stories": "dir-sha1:e33605a5bff376640679847dd30b5c7e9acefde7", "docs/00_FUNC/gates": "dir-sha1:f9a81f6383e61a321e659ee309369d29b4b0a92a"}} -->
# FUNC_MAP — 기능 추적 매트릭스

> 생성: 2026-09-17T14:57:08 · 자동(build_func_map, zero-LLM)
> 총 23개 기능 = 화면 8 + 배치 0 + INF 폴백 15(화면 미캡처 엔드포인트).
> 행 1개 = FUNC 1개. ID 클릭 → 해당 스펙. 상단 필터/정렬/CSV는 뷰어 표 엔진.
> ⚠ **이 파일은 파생 뷰입니다 — 직접 편집하지 마세요.** 상태 정본은 STORY frontmatter + GATE JSON이며,
> 재생성 시 이 표는 통째로 다시 그려집니다. 상태 변경은 `gate_io.py story-status`를 쓰세요.

## 매핑표

| FUNC-ID | 기능 | 도메인 | 유형 | UIS | SRS | INF | SCH | 구현상태 |
|---------|------|--------|------|-----|-----|-----|-----|----------|
| FUNC-member-004 | 로그인 | member | 화면 | [UIS-MBR-002](docs/05_설계서/member/UIS/UIS-MBR-002_로그인/spec.md) |  | [INF-MBR-003](docs/05_설계서/member/INF/INF-MBR-003.md), [INF-MBR-004](docs/05_설계서/member/INF/INF-MBR-004.md), [INF-MBR-005](docs/05_설계서/member/INF/INF-MBR-005.md) |  | ✅ 완료 |
| FUNC-member-007 | 비밀번호 재설정 | member | 화면 | [UIS-MBR-003](docs/05_설계서/member/UIS/UIS-MBR-003_비밀번호재설정/spec.md) |  | [INF-MBR-006](docs/05_설계서/member/INF/INF-MBR-006.md), [INF-MBR-007](docs/05_설계서/member/INF/INF-MBR-007.md) |  | ✅ 완료 |
| FUNC-order-001 | 주문 목록 | order | 화면 | [UIS-ORD-001](docs/05_설계서/order/UIS/UIS-ORD-001_주문목록/spec.md) |  | [INF-ORD-003](docs/05_설계서/order/INF/INF-ORD-003.md) | SCH-ORD-001, SCH-ORD-004 | 📋 승인 |
| FUNC-order-002 | 주문 상세 | order | 화면 | [UIS-ORD-002](docs/05_설계서/order/UIS/UIS-ORD-002_주문상세/spec.md) |  | [INF-ORD-004](docs/05_설계서/order/INF/INF-ORD-004.md), [INF-ORD-006](docs/05_설계서/order/INF/INF-ORD-006.md) | SCH-ORD-001, SCH-ORD-002, SCH-ORD-003, SCH-ORD-004, SCH-ORD-005 | 📋 승인 |
| FUNC-order-009 | 상품 목록 | order | 화면 | [UIS-ORD-003](docs/05_설계서/order/UIS/UIS-ORD-003_상품목록/spec.md) |  | [INF-ORD-008](docs/05_설계서/order/INF/INF-ORD-008.md) | SCH-ORD-005 | 📋 승인 |
| FUNC-order-010 | 상품 상세 | order | 화면 | [UIS-ORD-004](docs/05_설계서/order/UIS/UIS-ORD-004_상품상세/spec.md) |  | [INF-ORD-009](docs/05_설계서/order/INF/INF-ORD-009.md), [INF-ORD-010](docs/05_설계서/order/INF/INF-ORD-010.md), [INF-ORD-011](docs/05_설계서/order/INF/INF-ORD-011.md) | SCH-ORD-005, SCH-ORD-001, SCH-ORD-006 | ✅ 완료 |
| FUNC-order-011 | 장바구니 | order | 화면 | [UIS-ORD-005](docs/05_설계서/order/UIS/UIS-ORD-005_장바구니/spec.md) |  | [INF-ORD-010](docs/05_설계서/order/INF/INF-ORD-010.md), [INF-ORD-011](docs/05_설계서/order/INF/INF-ORD-011.md), [INF-ORD-012](docs/05_설계서/order/INF/INF-ORD-012.md), [INF-ORD-013](docs/05_설계서/order/INF/INF-ORD-013.md), [INF-ORD-014](docs/05_설계서/order/INF/INF-ORD-014.md) | SCH-ORD-001, SCH-ORD-005, SCH-ORD-006 | ✅ 완료 |
| FUNC-member-011 | (예약: INF-MBR-008 배송지 CRUD API — SR-235, 구현 후 스펙 저작) | member | INF폴백 |  |  |  |  | ✅ 완료 |
| FUNC-member-012 | (예약: INF-MBR-009 우편번호(도로명) 검색 API — SR-235, 구현 후 스펙 저작) | member | INF폴백 |  |  |  |  | 🔨 진행중 |
| FUNC-member-010 | (예약: UIS-MBR-004 배송지 관리 화면 — SR-235, 구현 후 스펙 저작) | member | 화면 | UIS-MBR-004 |  |  |  | 📋 승인 |
| FUNC-member-002 | 회원가입 인증코드 발송 | member | INF폴백 |  |  | [INF-MBR-001](docs/05_설계서/member/INF/INF-MBR-001.md) |  | ✅ 완료 |
| FUNC-member-003 | 가입 요청(인증코드 검증 포함) | member | INF폴백 |  |  | [INF-MBR-002](docs/05_설계서/member/INF/INF-MBR-002.md) |  | ✅ 완료 |
| FUNC-member-005 | 로그인 | member | INF폴백 |  |  | [INF-MBR-003](docs/05_설계서/member/INF/INF-MBR-003.md) |  | ✅ 완료 |
| FUNC-member-006 | 로그아웃 | member | INF폴백 |  |  | [INF-MBR-004](docs/05_설계서/member/INF/INF-MBR-004.md) |  | ✅ 완료 |
| FUNC-member-008 | 비밀번호 재설정 코드 요청 | member | INF폴백 |  |  | [INF-MBR-006](docs/05_설계서/member/INF/INF-MBR-006.md) |  | ✅ 완료 |
| FUNC-member-009 | 비밀번호 재설정 확정 | member | INF폴백 |  |  | [INF-MBR-007](docs/05_설계서/member/INF/INF-MBR-007.md) |  | ✅ 완료 |
| FUNC-order-003 | 회원 목록 조회 | order | INF폴백 |  |  | [INF-ORD-001](docs/05_설계서/order/INF/INF-ORD-001.md) | SCH-ORD-001 | 미구현 |
| FUNC-order-004 | 회원 상세 조회 | order | INF폴백 |  |  | [INF-ORD-002](docs/05_설계서/order/INF/INF-ORD-002.md) | SCH-ORD-001, SCH-ORD-004 | ✅ 완료 |
| FUNC-order-005 | 주문 생성 | order | INF폴백 |  |  | [INF-ORD-005](docs/05_설계서/order/INF/INF-ORD-005.md) | SCH-ORD-001, SCH-ORD-002, SCH-ORD-003, SCH-ORD-004, SCH-ORD-005 | 미구현 |
| FUNC-order-006 | 주문 배송 목록 조회 | order | INF폴백 |  |  | [INF-ORD-007](docs/05_설계서/order/INF/INF-ORD-007.md) | SCH-ORD-001, SCH-ORD-002, SCH-ORD-003, SCH-ORD-004 | 미구현 |
| FUNC-order-007 | 판매중 상품 목록 조회 | order | INF폴백 |  |  | [INF-ORD-008](docs/05_설계서/order/INF/INF-ORD-008.md) | SCH-ORD-005 | ✅ 완료 |
| FUNC-order-012 | 장바구니 체크아웃 | order | INF폴백 |  |  | [INF-ORD-014](docs/05_설계서/order/INF/INF-ORD-014.md) |  | ✅ 완료 |
| FUNC-order-013 | 주문 목록 CSV 내보내기 | order | INF폴백 |  |  | [INF-ORD-015](docs/05_설계서/order/INF/INF-ORD-015.md) |  | ✅ 완료 |

## 도메인 요약

| 도메인 | FUNC | 화면 FUNC | 연결 INF |
|--------|------|----------|----------|
| member | 11 | 3 | 11 |
| order | 12 | 5 | 19 |

## 갭 — 연결 INF 없는 FUNC

- FUNC-member-011
- FUNC-member-012
- FUNC-member-010
