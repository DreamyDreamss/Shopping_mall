# Shopping_mall — speclinker 테스트베드

이커머스 쇼핑몰(`shop-api` · `shop-web`)과 그 위에 쌓인 시연 데이터를 담는다.
**clone 하나로 시연과 개발을 이어갈 수 있게** 앱 소스 · DB 데이터 전량 · SR 이력이 모두 들어 있다.

분석·생성 도구는 별도 저장소다 → https://github.com/DreamyDreamss/speclinker

## 세팅

두 저장소를 받는다.

```bash
git clone https://github.com/DreamyDreamss/speclinker.git      ~/speclinker
git clone https://github.com/DreamyDreamss/Shopping_mall.git   ~/Shopping_mall
```

### Linux (Ubuntu 22.04+)

```bash
cd ~/Shopping_mall
bash setup-linux.sh --plugin ~/speclinker --start
```

### Windows

```powershell
cd D:\Shopping_mall
powershell -ExecutionPolicy Bypass -File setup.ps1 -Plugin D:\speclinker
```

세팅이 만드는 것:

| 무엇 | 기본값 |
|---|---|
| 워크스페이스 | `$HOME/sl-shop` (Windows `D:\sl-shop`) — `--ws` / `-Ws` 로 변경 |
| DB | `sl_shop` @ MariaDB `127.0.0.1:3307` — `--db-name` / `-DbName` 로 변경 |
| `project.env` | 두 저장소를 잇는 **유일한 접합면**. 실행 시점 경로로 생성된다 |
| `.mcp.json` | DB MCP · 목업 지라 MCP 등록 (무비밀번호 — 127.0.0.1 전용) |

| 옵션 | 뜻 |
|---|---|
| `--plugin <경로>` / `-Plugin` | speclinker 클론 (**필수**) |
| `--ws <경로>` / `-Ws` | 워크스페이스 |
| `--db-name <이름>` / `-DbName` | DB 이름 |
| `--force` / `-Force` | 기존 워크스페이스를 지우고 다시 만든다 |
| `--no-db` / `-SkipDb` · `--no-build` / `-SkipBuild` | 이미 있을 때 생략 |
| `--start` | (Linux) 끝나면 shop-api(:8087)와 SpecLens 브리지(:5173)까지 띄운다 |

## 구조

| 경로 | 내용 |
|---|---|
| `apps/shop-api` | Spring Boot API (:8087). jar가 `shop-web`의 `dist`를 `/shop`으로 싣는다 |
| `apps/shop-web` | SPA (Vite + Storybook) |
| `db/dump.sql` | 데이터 전량 — 16테이블 · 124행. SR 마이그레이션 반영 현행분 |
| `db/schema.sql` · `seed.sql` | 덤프가 없을 때의 폴백 |
| `seed/` | 시연 초기 데이터 — SR 원장 · 납품 21건 · 설계서 139건 · 워크스페이스 루트 파일 |
| `mocks/jira-mcp` | 목업 지라 MCP |

### seed의 경로 토큰

`seed/` 안의 파일은 절대 경로를 `{{SRC_SHOP_API}}` · `{{SRC_SHOP_WEB}}` · `{{PLUGIN_PATH}}` ·
`{{WS}}` 토큰으로 담는다. setup이 전개하면서 이 환경의 실제 경로로 치환한다.
**seed를 손으로 고칠 때 절대 경로를 그대로 쓰지 마라** — 다음 환경에서 깨진다.

## 일상 사용

```bash
WS=~/sl-shop bash run-app.sh                          # shop-api 기동(:8087) — --stop 으로 중지
cd ~/sl-shop && bash ~/speclinker/scripts/serve-speclens.sh   # SpecLens 브리지(:5173)
#   --public <도메인> · --status · --stop
cd ~/sl-shop && claude   →  /sl-doctor
```

원격 서버라면 터널로 본다.

```bash
ssh -L 5173:127.0.0.1:5173 -L 8087:127.0.0.1:8087 <서버>
#  → http://127.0.0.1:5173/docs/viewer2/index.html
```

## 시연 데이터를 갱신하려면

워크스페이스에서 작업한 결과를 `seed/`로 되올린다. 되올릴 때 절대 경로를 토큰으로 바꾼다.
DB도 함께 뜬다 — `seed/`와 `db/dump.sql`은 **같은 시점의 짝**이고, 한쪽만 갈아끼우면
스펙이 가리키는 데이터와 실제 DB가 어긋난다.

## 왜 Docker가 없나

대상 서버(Azure Ubuntu 24.04)에 Docker가 없고, 네이티브 스택(Java 17 · Maven · Node 20 ·
MariaDB 10.11 · Caddy)이 이미 가동 중이며, 2코어에서 컨테이너 빌드가 시연 전 재빌드를
느리게 만들고, 컨테이너 MariaDB가 기존 `:3307`과 충돌한다. 설계 근거는 speclinker의
`docs/superpowers/specs/2026-09-22-testbed-split-design.md`에 있다.
