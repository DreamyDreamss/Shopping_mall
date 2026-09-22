#!/usr/bin/env bash
# =============================================================================
# setup-linux.sh — 테스트베드를 Linux에 올린다 (Shopping_mall 단독 진입점)
# =============================================================================
#   1. apt: MariaDB · OpenJDK 17 · Maven
#   2. MariaDB를 3307로 띄우고 db/dump.sql 적재 (DB 이름 $DB_NAME, 기본 sl_shop)
#   3. python: 로컬 지라 MCP(fastmcp) · DB MCP(sqlalchemy·pymysql·pandas)
#   4. 워크스페이스 생성 + seed/ 전개 + project.env·.mcp.json 생성
#   5. shop-web npm ci·빌드 · shop-api mvn package
#
# 사용:  bash setup-linux.sh --plugin <speclinker 경로> [옵션]
#   --plugin <경로>   speclinker 클론 (필수)
#   --ws <경로>       워크스페이스 (기본 $HOME/sl-shop)
#   --db-name <이름>  기본 sl_shop
#   --force           기존 워크스페이스를 지우고 다시 만든다
#   --no-db           MariaDB 설치·적재 생략(이미 있을 때)
#   --no-build        빌드 생략
#   --start           끝나면 shop-api(:8087)와 SpecLens 브리지(:5173)까지 띄운다
#
# 전제: speclinker 쪽 setup.sh(플러그인 원샷)를 먼저 — node·python3가 거기서 깔린다.
# =============================================================================
set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WS="${WS:-$HOME/sl-shop}"
DB_NAME="${DB_NAME:-sl_shop}"
PLUGIN=""
FORCE=0; NO_DB=0; NO_BUILD=0; START=0

while [ $# -gt 0 ]; do
  case "$1" in
    --plugin)  PLUGIN="$2"; shift 2 ;;
    --ws)      WS="$2"; shift 2 ;;
    --db-name) DB_NAME="$2"; shift 2 ;;
    --force)   FORCE=1; shift ;;
    --no-db)   NO_DB=1; shift ;;
    --no-build) NO_BUILD=1; shift ;;
    --start)   START=1; shift ;;
    -h|--help) sed -n '2,21p' "$0"; exit 0 ;;
    *) echo "알 수 없는 인자: $1" >&2; exit 2 ;;
  esac
done

step() { printf '\n\033[1;36m== %s\033[0m\n' "$*"; }
ok()   { printf '  \033[32m✓\033[0m %s\n' "$*"; }
warn() { printf '  \033[33m!\033[0m %s\n' "$*"; }
die()  { printf '  \033[31m✗\033[0m %s\n' "$*"; exit 1; }

[ -n "$PLUGIN" ] || die "speclinker 경로가 필요합니다: --plugin <경로>"
PLUGIN="$(cd "$PLUGIN" && pwd)" || die "speclinker 경로를 찾을 수 없습니다: $PLUGIN"
[ -f "$PLUGIN/scripts/speclens_server.py" ] || die "speclinker 클론이 아닙니다(scripts/speclens_server.py 없음): $PLUGIN"

SUDO=""; [ "$(id -u)" -ne 0 ] && SUDO="sudo"
PY="$(command -v python3 || command -v python || true)"; [ -n "$PY" ] || die "python3가 없습니다"

SRC_API="$REPO/apps/shop-api"
SRC_WEB="$REPO/apps/shop-web"

# ── 1. OS 패키지 ─────────────────────────────────────────────────────────────
step "1/5 OS 패키지 (MariaDB · OpenJDK 17 · Maven)"
if command -v apt-get >/dev/null 2>&1; then
  $SUDO apt-get update -qq
  PKGS="openjdk-17-jdk maven"
  [ "$NO_DB" = 1 ] || PKGS="$PKGS mariadb-server mariadb-client"
  $SUDO apt-get install -y -qq $PKGS
  ok "$PKGS"
else
  warn "apt가 아닙니다 — MariaDB·JDK17·Maven을 직접 설치한 뒤 --no-db 로 다시 실행하세요"
fi

# ── 2. MariaDB 3307 + $DB_NAME ──────────────────────────────────────────────
if [ "$NO_DB" = 1 ]; then
  step "2/5 MariaDB — 건너뜀(--no-db)"
else
  step "2/5 MariaDB 3307 · root@127.0.0.1 · $DB_NAME 적재"
  CNF="/etc/mysql/mariadb.conf.d/99-speclinker-lab.cnf"
  if [ -f "$CNF" ]; then                                            # 옛 설치 보정(2026-09-16)
    grep -q '^skip-name-resolve' "$CNF" || printf 'skip-name-resolve\n' | $SUDO tee -a "$CNF" >/dev/null
    grep -q '^lower_case_table_names' "$CNF" || printf 'lower_case_table_names=1\n' | $SUDO tee -a "$CNF" >/dev/null
  fi
  if [ ! -f "$CNF" ]; then
    # skip-name-resolve — 2026-09-16 Azure Ubuntu 24.04 실측: 없으면 서버가 127.0.0.1을 `localhost`로 역해석해
    # TCP 접속이 `root@127.0.0.1`이 아니라 `root@localhost`(다른 인증) 계정에 매칭돼 ERROR 1698로 막힌다.
    # lower_case_table_names=1 — Windows 랩은 식별자를 소문자로 저장하고 대소문자를 구분하지 않는다. 리눅스 기본(0)에서는
    # 덤프의 `members`와 앱 SQL의 `MEMBERS`가 다른 테이블이라 기동 DDL이 실패하고, 같은 테이블이 대·소문자 두 벌로 생긴다(실측).
    printf '[mysqld]\nport=3307\nbind-address=127.0.0.1\nskip-name-resolve\nlower_case_table_names=1\ncharacter-set-server=utf8mb4\ncollation-server=utf8mb4_unicode_ci\n' | $SUDO tee "$CNF" >/dev/null
    ok "$CNF (port 3307 — 랩 계약: 앱 yml·.mcp.json이 3307을 본다)"
  fi
  $SUDO systemctl enable --now mariadb >/dev/null 2>&1 || $SUDO service mariadb start
  $SUDO systemctl restart mariadb 2>/dev/null || $SUDO service mariadb restart
  for i in $(seq 1 30); do $SUDO mariadb -e "select 1" >/dev/null 2>&1 && break; sleep 1; done
  $SUDO mariadb -e "select 1" >/dev/null 2>&1 || die "MariaDB가 뜨지 않습니다 — journalctl -u mariadb"
  # 로컬 랩 전용: TCP root 무비밀번호(앱 application.yml · .mcp.json 계약). 127.0.0.1에만 열려 있다.
  $SUDO mariadb -e "CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY ''; GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION; FLUSH PRIVILEGES;"
  if [ -f "$REPO/db/dump.sql" ]; then
    $SUDO mariadb --default-character-set=utf8mb4 < "$REPO/db/dump.sql"
    ok "db/dump.sql 적재(현행 DB — SR 마이그레이션 포함)"
  else
    $SUDO mariadb -e "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4"
    $SUDO mariadb --default-character-set=utf8mb4 "$DB_NAME" < "$REPO/db/schema.sql"
    $SUDO mariadb --default-character-set=utf8mb4 "$DB_NAME" < "$REPO/db/seed.sql"
    ok "schema.sql + seed.sql 적재"
  fi
  # 앱·MCP가 쓰는 경로(TCP root@127.0.0.1)로 **직접** 확인한다 — 여기서 막히면 뒤 단계가 전부 헛돈다.
  N=$(mariadb -h127.0.0.1 -P3307 -uroot -N -e "select count(*) from information_schema.tables where table_schema='$DB_NAME'" 2>/dev/null || true)
  [ -n "$N" ] && [ "$N" != "0" ] || die "TCP(127.0.0.1:3307)로 $DB_NAME 을 볼 수 없습니다 — sudo mariadb -e \"select user,host,plugin from mysql.user where user='root'\" 로 계정을 보고 99-speclinker-lab.cnf 의 skip-name-resolve 를 확인하세요"
  ok "$DB_NAME 테이블 $N개 (127.0.0.1:3307)"
fi

# ── 3. python 의존(로컬 지라 MCP · DB MCP) ───────────────────────────────────
step "3/5 python 의존 (mcp/fastmcp · sqlalchemy · pymysql · pandas)"
PIPQ="$PY -m pip install --quiet"
$PIPQ "mcp[cli]" fastmcp sqlalchemy pymysql pandas python-dotenv 2>/dev/null \
  || $PIPQ --user "mcp[cli]" fastmcp sqlalchemy pymysql pandas python-dotenv 2>/dev/null \
  || $PIPQ --user --break-system-packages "mcp[cli]" fastmcp sqlalchemy pymysql pandas python-dotenv \
  || warn "pip 설치 실패 — 지라/DB MCP가 안 뜹니다(수동: $PY -m pip install 'mcp[cli]' fastmcp sqlalchemy pymysql pandas)"
ok "python 의존"

# ── 4. 워크스페이스 + seed 전개 ─────────────────────────────────────────────
step "4/5 워크스페이스 ($WS)"
if [ -d "$WS" ] && [ -n "$(ls -A "$WS" 2>/dev/null)" ]; then
  if [ "$FORCE" = 1 ]; then rm -rf "$WS"; warn "기존 워크스페이스 삭제(--force)"
  else die "이미 있습니다: $WS — 다시 만들려면 --force, 그대로 쓰려면 이 단계는 필요 없습니다"; fi
fi
mkdir -p "$WS/docs" "$WS/_lab"
cp -a "$REPO/seed/변경관리/."     "$WS/docs/변경관리/"  2>/dev/null || { mkdir -p "$WS/docs/변경관리"; cp -a "$REPO/seed/변경관리/." "$WS/docs/변경관리/"; }
mkdir -p "$WS/docs/09_납품" "$WS/_lab/catalog" "$WS/.speclinker"
cp -a "$REPO/seed/09_납품/."      "$WS/docs/09_납품/"
cp -a "$REPO/seed/_lab/catalog/." "$WS/_lab/catalog/"
cp -a "$REPO/seed/.speclinker/."  "$WS/.speclinker/"
cp -a "$REPO/seed/ws/."           "$WS/"                # CLAUDE.md · package.json · tests · harness · .claude
ok "seed 전개: SR $(ls -d "$WS"/docs/변경관리/SR-* 2>/dev/null | wc -l)건 · 설계서 $(ls "$WS/_lab/catalog" | wc -l)건 · 납품 $(ls "$WS/docs/09_납품" | wc -l)건"

# seed는 절대 경로를 토큰으로 담는다 — 여기서 이 환경의 실제 경로로 되돌린다.
step "4-b 경로 토큰 치환"
SUBST=$(find "$WS" -type f \( -name '*.md' -o -name '*.json' -o -name '*.jsonl' -o -name '*.txt' \
        -o -name '*.html' -o -name '*.yaml' -o -name '*.yml' -o -name '*.csv' -o -name '*.xml' \) \
        -not -path "*/node_modules/*" -print0 \
        | xargs -0 -r grep -l '{{\(WS\|PLUGIN_PATH\|SRC_SHOP_API\|SRC_SHOP_WEB\)}}' 2>/dev/null | wc -l)
find "$WS" -type f \( -name '*.md' -o -name '*.json' -o -name '*.jsonl' -o -name '*.txt' \
     -o -name '*.html' -o -name '*.yaml' -o -name '*.yml' -o -name '*.csv' -o -name '*.xml' \) \
     -not -path "*/node_modules/*" -print0 \
  | xargs -0 -r sed -i \
      -e "s|{{SRC_SHOP_API}}|$SRC_API|g" \
      -e "s|{{SRC_SHOP_WEB}}|$SRC_WEB|g" \
      -e "s|{{PLUGIN_PATH}}|$PLUGIN|g" \
      -e "s|{{WS}}|$WS|g"
ok "토큰 치환 $SUBST개 파일 → SRC=$REPO/apps · PLUGIN=$PLUGIN · WS=$WS"

step "4-c project.env · .mcp.json 생성"
cat > "$WS/project.env" <<ENV
# Speclinker 테스트베드 — 이커머스 쇼핑몰(shop-api · shop-web)
PROJECT_NAME=sl-shop
NETWORK=open
PLUGIN_PATH=$PLUGIN
SOURCE_COUNT=2
SOURCE_1_LABEL=shop-api
SOURCE_1_PATH=$SRC_API
SOURCE_2_LABEL=shop-web
SOURCE_2_PATH=$SRC_WEB
PREVIEW_BASE_URL=http://localhost:8087
MCP_DB_MARIADB=true
E2E_BASE_URL_local=http://localhost:8087
API_TRY_BASE=http://localhost:8087

# v4.87.0 — 앱 실행 계약(app_runner.py). {PORT}가 있어야 SR 공간마다 포트를 가른다.
APP_RUN_CMD=mvn -q spring-boot:run -Dspring-boot.run.arguments=--server.port={PORT}
APP_HEALTH_PATH=/orders
APP_READY_TIMEOUT=240

# --- 스토리북(화면 부품 상태)
STORYBOOK_SOURCE=shop-web
STORYBOOK_RUN_CMD=npm run storybook -- -p {PORT}
STORYBOOK_STATIC=storybook-static
STORYBOOK_BUILD_CMD=npm run build-storybook
STORYBOOK_TEST_CMD=npm run test-storybook:ci

# --- 응답 값 대조(축 D, resp_snapshot.py)
SNAP_BASE_URL=http://localhost:8087
SNAP_AUTH_HEADER=X-Api-Key: lab-admin-key
ENV
ok "project.env"

if [ ! -f "$WS/.mcp.json" ]; then
  cat > "$WS/.mcp.json" <<MCP
{
  "mcpServers": {
    "db-main": {
      "command": "$PY",
      "args": ["$PLUGIN/mcp-servers/mariadb_schema_server.py"],
      "env": {
        "MDB_HOST": "127.0.0.1",
        "MDB_PORT": "3307",
        "MDB_DATABASE": "$DB_NAME",
        "MDB_USER": "root",
        "MDB_PASSWORD": ""
      }
    },
    "mcp-atlassian": {
      "command": "$PY",
      "args": ["$REPO/mocks/jira-mcp/server.py"],
      "env": { "LAB_JIRA_STORE": "$WS/_lab/jira_issues.json" }
    }
  }
}
MCP
  ok ".mcp.json (DB=$DB_NAME · 무비밀번호 — 127.0.0.1 전용)"
else
  ok ".mcp.json 이미 있음 — 그대로 둠"
fi

# ── 5. 빌드 ─────────────────────────────────────────────────────────────────
if [ "$NO_BUILD" = 1 ]; then
  step "5/5 빌드 — 건너뜀(--no-build)"
else
  step "5/5 빌드 (shop-web dist·스토리북 · shop-api jar)"
  if [ -d "$SRC_WEB" ]; then
    ( cd "$SRC_WEB" && npm ci --silent 2>/dev/null || npm install --silent ) && ok "shop-web npm"
    # shop-api jar가 dist를 /shop으로 싣는다 — jar보다 먼저, 매번 빌드(dist는 git 밖이라 낡는다, 2026-09-19)
    ( cd "$SRC_WEB" && npm run build --silent >/dev/null 2>&1 ) && ok "shop-web dist" || warn "SPA 빌드 실패 — cd $SRC_WEB && npm run build"
    ( cd "$SRC_WEB" && npm run build-storybook --silent >/dev/null 2>&1 ) && ok "storybook-static" || warn "스토리북 빌드 실패 — cd $SRC_WEB && npm run build-storybook"
  fi
  if [ -d "$SRC_API" ]; then
    ( cd "$SRC_API" && mvn -q -DskipTests package ) && ok "shop-api jar" || warn "mvn package 실패 — cd $SRC_API && mvn -DskipTests package"
  fi
  ( cd "$WS" && [ -f package.json ] && npm install --silent >/dev/null 2>&1 ) || true
fi

if [ "$START" = 1 ]; then
  step "기동 (shop-api :8087 · SpecLens :5173)"
  WS="$WS" bash "$REPO/run-app.sh" || warn "앱 기동 실패 — WS=$WS bash $REPO/run-app.sh 로 로그 확인"
  ( cd "$WS" && bash "$PLUGIN/scripts/serve-speclens.sh" ) || warn "브리지 기동 실패 — cd $WS && bash $PLUGIN/scripts/serve-speclens.sh"
fi

cat <<EOF

테스트베드 준비 끝.
  워크스페이스 : $WS
  소스         : $REPO/apps/{shop-api,shop-web}
  DB           : $DB_NAME @ 127.0.0.1:3307
  플러그인      : $PLUGIN

다음:
  WS=$WS bash $REPO/run-app.sh                       # shop-api 기동(:8087, 배경) — --stop 으로 중지
  cd $WS && bash $PLUGIN/scripts/serve-speclens.sh   # SpecLens 브리지(:5173) — --stop · --status · --public <도메인>
  # 노트북에서:  ssh -L 5173:127.0.0.1:5173 -L 8087:127.0.0.1:8087 <서버>
  #              →  http://127.0.0.1:5173/docs/viewer2/index.html
  cd $WS && claude    →  /sl-doctor
EOF
