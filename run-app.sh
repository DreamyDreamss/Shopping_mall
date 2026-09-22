#!/usr/bin/env bash
# run-app.sh — <repo>/apps/shop-api 를 빌드하고 :8087 로 **배경** 기동한다 (Linux판 run-app.ps1).
#   bash run-app.sh            # 빌드 + 기동(이미 떠 있으면 그대로)
#   bash run-app.sh --no-build # 빌드 생략
#   bash run-app.sh --stop     # 중지
# 로그: $WS/.speclinker/app_shop-api.log · PID: $WS/.speclinker/app_shop-api.pid
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WS="${WS:-${SL_LAB_WS:-$HOME/sl-shop}}"   # 플러그인 밖(2026-09-13)
MOD="$REPO/apps/shop-api"
LOG="$WS/.speclinker/app_shop-api.log"; PIDF="$WS/.speclinker/app_shop-api.pid"
PORT=8087
up() { (exec 3<>/dev/tcp/127.0.0.1/$PORT) 2>/dev/null; }

if [ "${1:-}" = "--stop" ]; then
  if [ -f "$PIDF" ] && kill "$(cat "$PIDF")" 2>/dev/null; then echo "[shop] 중지: PID $(cat "$PIDF")"; rm -f "$PIDF"; else echo "[shop] 실행 중인 앱 없음"; fi
  exit 0
fi
[ -d "$MOD" ] || { echo "[shop] $MOD 없음 — 먼저 bash setup-linux.sh --plugin <speclinker 경로>"; exit 1; }
if up; then echo "[shop] 이미 실행 중 (127.0.0.1:$PORT)"; exit 0; fi
(exec 3<>/dev/tcp/127.0.0.1/3307) 2>/dev/null || { echo "[shop] MariaDB 3307이 안 떠 있습니다 — sudo systemctl start mariadb"; exit 1; }
if [ "${1:-}" != "--no-build" ]; then
  # jar가 shop-web/dist를 /shop으로 싣는다 — 화면을 고친 SR이 재기동에 반영되려면 SPA부터(2026-09-19)
  if [ -d "$REPO/apps/shop-web/node_modules" ]; then
    echo "[shop] shop-web SPA 빌드…"; ( cd "$REPO/apps/shop-web" && npm run build --silent >/dev/null )
  fi
  echo "[shop] shop-api 빌드…"; ( cd "$MOD" && mvn -q -DskipTests package )
fi
JAR="$(ls "$MOD"/target/shop-api-*.jar 2>/dev/null | grep -v original | head -1 || true)"   # head의 SIGPIPE로 스크립트가 조용히 죽지 않게(2026-09-16)
[ -n "$JAR" ] || { echo "[shop] jar 없음 — 빌드 실패?"; exit 1; }
mkdir -p "$(dirname "$LOG")"
nohup java -jar "$JAR" > "$LOG" 2>&1 &
echo $! > "$PIDF"
for i in $(seq 1 60); do up && break; sleep 1; done
if up; then echo "[shop] 기동: $JAR → http://127.0.0.1:$PORT/ (PID $(cat "$PIDF"), 로그 $LOG)"; else echo "[shop] 60초 안에 안 떴습니다 — tail -50 $LOG"; exit 1; fi
