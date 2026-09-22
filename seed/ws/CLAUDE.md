# sl-shop — 쇼핑몰 테스트베드 워크스페이스 (CLAUDE.md)

Speclinker 플러그인의 실증 랩. 스펙·원장·게이트는 `docs/`에, 소스는 `modules/`에 있다. 이 파일은 **이 프로젝트에서 코드를
만지는 세션·에이전트가 먼저 알아야 할 사실**만 적는다(플러그인 규율은 `{{PLUGIN_PATH}}/CLAUDE.md`).

## 모듈과 스택

| 모듈 | 스택 | 포트 | 실행 | 테스트 |
|---|---|---|---|---|
| `modules/shop-api` | Spring Boot 3 · MyBatis(XML 매퍼) · Thymeleaf(서버 렌더 화면) · MariaDB | 8087 | `lab/run-app.ps1`(플러그인 쪽) 또는 `mvnw spring-boot:run` | `mvnw test` (surefire) |
| `modules/shop-web` | React 19 · Vite · TypeScript · Storybook | 5273(dev) | `npm run dev` — **AIDD 잡이 도는 동안은 띄우지 않는다**(HMR 3.2GB OOM 실측) | `npm test`(= 타입 검사) · `npm run test-storybook` |
| DB | MariaDB 포터블 `sl_lab` | 3307 | `lab/db/db.ps1 start` | — |

- API 인증: `X-Api-Key: lab-admin-key`(`web/ApiKeyAuthFilter.java`). shop-web은 vite 프록시가 붙인다 — 부품에서 직접 `fetch` 금지.
- 오류 계약: `web/*ExceptionHandler.java`가 `{code, message}`로 낸다(`ORD-4001`·`MBR-4092`…). 새 오류는 코드부터 정한다.
- 응답 봉투: 목록은 `{items: [...]}`. SPA(`src/api.ts`)가 이 봉투를 푼다.
- DDL: 마이그레이션 엔진 없음. `resources/db/*.sql`이 기동마다 재실행 — `IF NOT EXISTS`만, DROP 금지(규칙 `ddl-idempotent`).
- 화면: 신규 고객 화면은 **shop-web**(React)에, 관리자·기존 서버 렌더 화면은 shop-api Thymeleaf에. 예약 폼의 "구현 모듈"이 STORY 제약으로 실린다.
- DDL 파일 번호: `resources/db/V<n>__*.sql`는 **다음 번호**로 새 파일을 만든다(기존 파일 수정 금지 — 기동마다 전부 재실행된다). 현재 최신 `V9__member_password_reset_rate_limits.sql`.
- 배치(@Scheduled): 전역 `@EnableScheduling`은 `MemberSignupSchedulingConfig` 하나뿐 — 새 배치는 그것을 재사용하고 별도 Configuration을 만들지 않는다. 정리 DELETE는 요청 경로와 트랜잭션을 섞지 않는다(독립 autocommit).
- 시도 횟수·요청 횟수 같은 카운터는 **단일 조건부 UPDATE/UPSERT**로 판정한다(읽고 비교한 뒤 쓰지 않는다). 0행일 때만 원인 재조회로 오류 코드를 가른다.
- 시간 의존: 주문 조회 기본 창은 `오늘-30일`이다. 테스트는 시계를 고정하거나 명시 기간을 넘긴다(시드는 고정 날짜라 그냥 두면 날짜가 흐르며 깨진다 — SR-300).
- 값 대조 주소록(`.speclinker/snapshots/api.urls`)에는 상대 기간 조회를 넣지 않는다(기간을 명시한다).

## 규칙(하네스) — `.claude/rules/lab/`

STORY의 "📏 적용 규칙" 절에 실리고 AIDD STEP 5.3 축 C(`rules_check.py`)가 집행한다. `must`는 게이트를 막는다.

| 규칙 | 종류 | 무엇 |
|---|---|---|
| `no-sysout` · `no-printstacktrace` | must | 콘솔 출력·스택 덤프 금지 — 로거·예외 핸들러 |
| `no-select-star` | must | 매퍼 XML `SELECT *` 금지(축 D 스냅샷이 통째로 깨진다) |
| `ddl-idempotent` | must | 기동 DDL은 `IF NOT EXISTS`, DROP 금지 |
| `controller-has-test` | must | `XxxController` ↔ `XxxControllerTest`(MockMvc) 짝 |
| `web-fetch-only-in-api` | must | 부품·페이지 직접 fetch 금지, `console.log` 금지 |
| `story-per-component` | must | `components/*.tsx`마다 `*.stories.tsx` |
| `service-has-test` | should | `XxxService` ↔ `XxxServiceTest` 짝(기존 4개 미비 — 새 것부터) |
| `file-size-cap` | should | 자바 450줄 · tsx 300줄 |

## 기준선(정본) — 바꿀 때는 사유와 함께

- 테스트 기준선 `.speclinker/test_baseline.json` — `python {{PLUGIN_PATH}}/scripts/test_baseline_ws.py record . --force`
- 응답 값 스냅샷 `.speclinker/snapshots/api.urls` + `api.json` — `resp_snapshot.py capture . api --force`(앱 8087 필요)
- 화면 상태 기준 `.speclinker/story_shots/baseline/` — `story_shots.py capture . --force`(스토리북 필요)

## 이 프로젝트가 이미 밟은 것 — `harness/antipatterns.all.md`

코드를 고치기 전에 30초만 훑는다. 같은 실수(역할 반전·전역 커넥션 속성·세션 변수 순서·존재 오라클·타이머 락)가 RUN7에서
$154.6를 썼다. 판단 기록은 `harness/decisions.all.md`.
