# lab/run-app.ps1 — 실증 워크스페이스(D:\sl-shop)의 shop-api를 빌드·기동한다 (포트 8087).
# 코드를 수정한 뒤 이 스크립트로 재기동하면 실증 사이클(수정→확인)이 돈다.
param([switch]$NoBuild)

$ErrorActionPreference = 'Stop'
$Repo       = $PSScriptRoot
$Ws         = if ($env:SL_LAB_WS) { $env:SL_LAB_WS } else { 'D:\sl-shop' }   # 플러그인 밖(2026-09-13)
$Mod        = Join-Path $Repo 'apps\shop-api'
$MavenHome  = Get-ChildItem (Join-Path $Repo '.runtime') -Directory -Filter 'apache-maven-*' | Select-Object -First 1 -ExpandProperty FullName

if (-not (Test-Path $Mod)) { throw "[lab] $Ws 없음 — 먼저 lab\setup.ps1 (SL_LAB_WS로 위치 지정)" }
if (-not $MavenHome) { throw '[lab] Maven 없음 — 먼저 lab\setup.ps1' }

& (Join-Path $Repo 'db\db.ps1') start

if (-not $NoBuild) {
    # shop-api jar가 shop-web/dist를 /shop으로 싣는다 — 화면을 고친 SR이 재기동에 반영되려면 SPA부터(2026-09-19)
    $Web = Join-Path $Repo 'apps\shop-web'
    if (Test-Path (Join-Path $Web 'node_modules')) {
        Write-Host '[lab] shop-web SPA 빌드…'
        Push-Location $Web
        try { npm run build --silent | Out-Null; if ($LASTEXITCODE -ne 0) { throw '[lab] shop-web SPA 빌드 실패' } }
        finally { Pop-Location }
    }
    Write-Host '[lab] shop-api 빌드…'
    Push-Location $Mod
    try { & (Join-Path $MavenHome 'bin\mvn.cmd') -q -DskipTests package }
    finally { Pop-Location }
}

$jar = Get-ChildItem (Join-Path $Mod 'target') -Filter 'shop-api-*.jar' | Where-Object Name -notlike '*original*' | Select-Object -First 1 -ExpandProperty FullName
Write-Host "[lab] 기동: $jar  (http://localhost:8087 — 중지는 Ctrl+C)"
java -jar $jar
