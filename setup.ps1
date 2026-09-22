# =============================================================================
# setup.ps1 — 테스트베드를 Windows에 올린다 (Shopping_mall 단독 진입점)
# =============================================================================
#   1. python 의존(로컬 지라 MCP · DB MCP · 시드)
#   2. 포터블 MariaDB 준비·기동(:3307) + db\dump.sql 적재
#   3. 워크스페이스 생성 + seed\ 전개 + 경로 토큰 치환
#   4. project.env · .mcp.json 생성
#   5. shop-web npm·빌드 · shop-api jar
#
# 사용: powershell -ExecutionPolicy Bypass -File setup.ps1 -Plugin D:\speclinker [옵션]
#   -Plugin <경로>  speclinker 클론 (필수)
#   -Ws <경로>      워크스페이스 (기본 D:\sl-shop)
#   -DbName <이름>  기본 sl_shop
#   -Force          기존 워크스페이스를 지우고 다시 만든다
#   -SkipDb         DB 준비·적재 생략
#   -SkipBuild      빌드 생략
# =============================================================================
param(
    [Parameter(Mandatory = $true)][string]$Plugin,
    [string]$Ws = $(if ($env:SL_LAB_WS) { $env:SL_LAB_WS } else { Join-Path $env:USERPROFILE 'sl-shop' }),
    [string]$DbName = 'sl_shop',
    [switch]$Force, [switch]$SkipDb, [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$Repo      = $PSScriptRoot
$Runtime   = Join-Path $Repo '.runtime'
$MavenVer  = '3.9.9'
$MavenHome = Join-Path $Runtime "apache-maven-$MavenVer"
$SrcApi    = Join-Path $Repo 'apps\shop-api'
$SrcWeb    = Join-Path $Repo 'apps\shop-web'
$Dump      = Join-Path $Repo 'db\dump.sql'

if (-not (Test-Path (Join-Path $Plugin 'scripts\speclens_server.py'))) {
    throw "speclinker 클론이 아닙니다(scripts\speclens_server.py 없음): $Plugin"
}
$Plugin = (Resolve-Path $Plugin).Path

# 네이티브 종료코드는 $ErrorActionPreference='Stop'이 잡지 않는다 — 직접 본다.
function Assert-Exit([string]$What) {
    if ($LASTEXITCODE -ne 0) { throw "[shop] $What 실패 (exit $LASTEXITCODE)" }
}

# 설정 파일은 BOM 없이 쓴다. PowerShell 5.1의 `Out-File -Encoding utf8`은 BOM을 붙이는데, BOM 붙은 .mcp.json은
# JSON 파서가 거부할 수 있다(2026-09-19 실측: doctor가 등록된 DB MCP를 "항목 없음"으로 판정).
function Write-NoBom([string]$Path, [string]$Text) {
    # Out-File처럼 끝에 줄바꿈 하나 — project.env를 줄 단위로 덧붙이는 도구가 마지막 줄에 이어 쓰지 않게
    [System.IO.File]::WriteAllText($Path, $Text.TrimEnd("`r", "`n") + "`r`n", (New-Object System.Text.UTF8Encoding($false)))
}

function Get-Maven {
    if (Test-Path $MavenHome) { return }
    New-Item -ItemType Directory -Force -Path $Runtime | Out-Null
    $zip = Join-Path $Runtime "apache-maven-$MavenVer-bin.zip"
    if (-not (Test-Path $zip)) {
        Write-Host "[shop] Maven $MavenVer 내려받는 중…"
        Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/$MavenVer/binaries/apache-maven-$MavenVer-bin.zip" -OutFile $zip -UseBasicParsing
    }
    Expand-Archive -Path $zip -DestinationPath $Runtime -Force
}

# ── 1. python 의존 ───────────────────────────────────────────────────────────
# 2026-09-19 실측: 이 단계가 없어 `pymysql` 미설치로 시드가 통째로 실패했는데 다음 단계가 그대로 진행됐다.
Write-Host '== 1/5 python 의존(mcp/fastmcp · sqlalchemy · pymysql · pandas)'
$PyPkgs = @('mcp[cli]', 'fastmcp', 'sqlalchemy', 'pymysql', 'pandas', 'python-dotenv')
& python -m pip install --quiet @PyPkgs
if ($LASTEXITCODE -ne 0) { Write-Warning '[shop] pip 설치 실패 — 지라/DB MCP가 안 뜹니다' }

# ── 2. DB ────────────────────────────────────────────────────────────────────
if ($SkipDb) {
    Write-Host '== 2/5 DB — 건너뜀(-SkipDb)'
} else {
    Write-Host "== 2/5 포터블 MariaDB :3307 · $DbName 적재"
    & (Join-Path $Repo 'db\db.ps1') init
    & (Join-Path $Repo 'db\db.ps1') start
    $client = Get-ChildItem $Runtime -Recurse -Filter 'mariadb.exe' -ErrorAction SilentlyContinue |
              Select-Object -First 1 -ExpandProperty FullName
    if (-not $client) { throw '[shop] mariadb.exe 없음 — db\db.ps1 init 을 먼저' }
    if (Test-Path $Dump) {
        Write-Host "[shop] db\dump.sql 적재(현행 DB — SR 마이그레이션 포함)…"
        cmd /c "`"$client`" -h127.0.0.1 -P3307 -uroot --default-character-set=utf8mb4 < `"$Dump`""
        Assert-Exit 'dump.sql 적재'
    } else {
        cmd /c "`"$client`" -h127.0.0.1 -P3307 -uroot -e `"CREATE DATABASE IF NOT EXISTS $DbName CHARACTER SET utf8mb4`""
        cmd /c "`"$client`" -h127.0.0.1 -P3307 -uroot --default-character-set=utf8mb4 $DbName < `"$(Join-Path $Repo 'db\schema.sql')`""
        cmd /c "`"$client`" -h127.0.0.1 -P3307 -uroot --default-character-set=utf8mb4 $DbName < `"$(Join-Path $Repo 'db\seed.sql')`""
        Assert-Exit 'schema+seed 적재'
    }
    $n = cmd /c "`"$client`" -h127.0.0.1 -P3307 -uroot -N -e `"select count(*) from information_schema.tables where table_schema='$DbName'`""
    if (-not $n -or $n -eq '0') { throw "[shop] TCP(127.0.0.1:3307)로 $DbName 을 볼 수 없습니다" }
    Write-Host "  OK $DbName 테이블 $n개"
}

# ── 3. 워크스페이스 + seed 전개 ──────────────────────────────────────────────
Write-Host "== 3/5 워크스페이스 ($Ws)"
if ((Test-Path $Ws) -and (Get-ChildItem $Ws -Force -ErrorAction SilentlyContinue)) {
    if ($Force) { Remove-Item -Recurse -Force $Ws; Write-Warning '기존 워크스페이스 삭제(-Force)' }
    else { throw "이미 있습니다: $Ws — 다시 만들려면 -Force" }
}
New-Item -ItemType Directory -Force -Path "$Ws\docs", "$Ws\_lab" | Out-Null
# robocopy는 성공에도 0이 아닌 종료코드를 낸다(1=복사함) — 8 이상만 실패다.
function Copy-Tree([string]$From, [string]$To) {
    robocopy $From $To /E /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "[shop] 복사 실패: $From → $To (robocopy $LASTEXITCODE)" }
    $global:LASTEXITCODE = 0
}
Copy-Tree "$Repo\seed\변경관리"     "$Ws\docs\변경관리"
Copy-Tree "$Repo\seed\09_납품"      "$Ws\docs\09_납품"
Copy-Tree "$Repo\seed\_lab\catalog" "$Ws\_lab\catalog"
Copy-Tree "$Repo\seed\.speclinker"  "$Ws\.speclinker"
Copy-Tree "$Repo\seed\ws"           $Ws
$srCount = (Get-ChildItem "$Ws\docs\변경관리" -Directory -Filter 'SR-*' -ErrorAction SilentlyContinue).Count
Write-Host "  OK seed 전개: SR $srCount건"

# seed는 절대 경로를 토큰으로 담는다 — 여기서 이 환경의 실제 경로로 되돌린다.
Write-Host '== 3-b 경로 토큰 치환'
$fwd = @{
    '{{SRC_SHOP_API}}' = $SrcApi.Replace('\', '/')
    '{{SRC_SHOP_WEB}}' = $SrcWeb.Replace('\', '/')
    '{{PLUGIN_PATH}}'  = $Plugin.Replace('\', '/')
    '{{WS}}'           = $Ws.Replace('\', '/')
}
$exts = '*.md', '*.json', '*.jsonl', '*.txt', '*.html', '*.yaml', '*.yml', '*.csv', '*.xml'
$touched = 0
Get-ChildItem $Ws -Recurse -File -Include $exts -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch '\\node_modules\\' } |
    ForEach-Object {
        $t = [System.IO.File]::ReadAllText($_.FullName)
        if ($t -like '*{{*}}*') {
            foreach ($k in $fwd.Keys) { $t = $t.Replace($k, $fwd[$k]) }
            [System.IO.File]::WriteAllText($_.FullName, $t, (New-Object System.Text.UTF8Encoding($false)))
            $touched++
        }
    }
Write-Host "  OK 토큰 치환 $touched개 파일"

# ── 4. project.env · .mcp.json ───────────────────────────────────────────────
Write-Host '== 4/5 project.env · .mcp.json 생성'
$pluginFwd = $Plugin.Replace('\', '/')
$repoFwd   = $Repo.Replace('\', '/')
Write-NoBom (Join-Path $Ws 'project.env') @"
# Speclinker 테스트베드 — 이커머스 쇼핑몰(shop-api · shop-web)
PROJECT_NAME=sl-shop
NETWORK=open
PLUGIN_PATH=$pluginFwd
SOURCE_COUNT=2
SOURCE_1_LABEL=shop-api
SOURCE_1_PATH=$repoFwd/apps/shop-api
SOURCE_2_LABEL=shop-web
SOURCE_2_PATH=$repoFwd/apps/shop-web
PREVIEW_BASE_URL=http://localhost:8087
MCP_DB_MARIADB=true
E2E_BASE_URL_local=http://localhost:8087
API_TRY_BASE=http://localhost:8087

# v4.87.0 — 앱 실행 계약(app_runner.py). {PORT}가 있어야 SR 공간마다 포트를 가른다.
APP_RUN_CMD=mvnw.cmd -q spring-boot:run -Dspring-boot.run.arguments=--server.port={PORT}
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
"@

$mcpPath = Join-Path $Ws '.mcp.json'
if (-not (Test-Path $mcpPath)) {
    Write-NoBom $mcpPath @"
{
  "mcpServers": {
    "db-main": {
      "command": "python",
      "args": ["$pluginFwd/mcp-servers/mariadb_schema_server.py"],
      "env": {
        "MDB_HOST": "127.0.0.1",
        "MDB_PORT": "3307",
        "MDB_DATABASE": "$DbName",
        "MDB_USER": "root",
        "MDB_PASSWORD": ""
      }
    },
    "mcp-atlassian": {
      "command": "python",
      "args": ["$repoFwd/mocks/jira-mcp/server.py"],
      "env": { "LAB_JIRA_STORE": "$($Ws.Replace('\','/'))/_lab/jira_issues.json" }
    }
  }
}
"@
}

# APP_RUN_CMD가 mvnw.cmd를 부른다 — 포터블 Maven을 가리키는 shim을 여기서 만든다.
# 저장소에 두지 않는 이유: 절대 경로라 다른 PC에서 깨진다(.gitignore에 있다).
Get-Maven
Write-NoBom (Join-Path $SrcApi 'mvnw.cmd') "@echo off`r`ncall `"$MavenHome\bin\mvn.cmd`" %*"
Write-Host "  OK mvnw.cmd → $MavenHome"

# ── 5. 빌드 ──────────────────────────────────────────────────────────────────
if ($SkipBuild) {
    Write-Host '== 5/5 빌드 — 건너뜀(-SkipBuild)'
} else {
    Write-Host '== 5/5 빌드 (shop-web dist·스토리북 · shop-api jar)'
    Push-Location $SrcWeb
    & npm ci --silent; if ($LASTEXITCODE -ne 0) { & npm install --silent }
    & npm run build --silent
    if ($LASTEXITCODE -ne 0) { Write-Warning "SPA 빌드 실패 — cd $SrcWeb; npm run build" }
    & npm run build-storybook --silent
    if ($LASTEXITCODE -ne 0) { Write-Warning "스토리북 빌드 실패" }
    Pop-Location
    Push-Location $SrcApi
    & "$MavenHome\bin\mvn.cmd" -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { Write-Warning "mvn package 실패 — cd $SrcApi" }
    Pop-Location
}

Write-Host ""
Write-Host "테스트베드 준비 끝."
Write-Host "  워크스페이스 : $Ws"
Write-Host "  소스         : $Repo\apps\{shop-api,shop-web}"
Write-Host "  DB           : $DbName @ 127.0.0.1:3307"
Write-Host "  플러그인      : $Plugin"
Write-Host ""
Write-Host "다음:"
Write-Host "  powershell -File $Repo\run-app.ps1            # shop-api 기동(:8087)"
Write-Host "  cd $Ws; claude   ->  /sl-doctor"
