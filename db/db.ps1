# lab/db/db.ps1 — 포터블 MariaDB 관리 (설치 불필요·사용자 MySQL 서비스 불간섭, 포트 3307)
# 사용법: .\db.ps1 init | start | stop | status | seed
param([Parameter(Position = 0)][ValidateSet('init', 'start', 'stop', 'status', 'seed')][string]$Cmd = 'status')

$ErrorActionPreference = 'Stop'
$LabRoot   = Split-Path $PSScriptRoot -Parent
$Runtime   = Join-Path $LabRoot '.runtime'
$MariaVer  = '11.4.5'
$MariaZip  = "mariadb-$MariaVer-winx64.zip"
$MariaUrl  = "https://archive.mariadb.org/mariadb-$MariaVer/winx64-packages/$MariaZip"
$MariaHome = Join-Path $Runtime "mariadb-$MariaVer-winx64"
$DataDir   = Join-Path $Runtime 'data'
$Port      = 3307
$Bin       = Join-Path $MariaHome 'bin'

function Test-Up {
    try { (New-Object Net.Sockets.TcpClient('127.0.0.1', $Port)).Close(); return $true } catch { return $false }
}

switch ($Cmd) {
    'init' {
        New-Item -ItemType Directory -Force $Runtime | Out-Null
        if (-not (Test-Path $MariaHome)) {
            $zip = Join-Path $Runtime $MariaZip
            if (-not (Test-Path $zip)) {
                Write-Host "[db] MariaDB $MariaVer 포터블 다운로드 중… (약 90MB, 1회)"
                Invoke-WebRequest -Uri $MariaUrl -OutFile $zip -UseBasicParsing
            }
            Write-Host '[db] 압축 해제 중…'
            Expand-Archive -Path $zip -DestinationPath $Runtime -Force
        }
        if (-not (Test-Path (Join-Path $DataDir 'mysql'))) {
            Write-Host '[db] 데이터 디렉토리 초기화(mariadb-install-db)…'
            & (Join-Path $Bin 'mariadb-install-db.exe') --datadir="$DataDir" --port=$Port | Out-Null
        }
        Write-Host "[db] 준비 완료 — .\db.ps1 start 로 기동 (포트 $Port)"
    }
    'start' {
        if (Test-Up) { Write-Host "[db] 이미 실행 중 (127.0.0.1:$Port)"; break }
        if (-not (Test-Path (Join-Path $Bin 'mariadbd.exe'))) { throw '[db] 먼저 .\db.ps1 init 를 실행하세요' }
        Write-Host "[db] mariadbd 기동 (포트 $Port)…"
        Start-Process -FilePath (Join-Path $Bin 'mariadbd.exe') `
            -ArgumentList "--datadir=`"$DataDir`"", "--port=$Port", '--skip-grant-tables=0', '--console' `
            -WindowStyle Hidden
        $tries = 0
        while (-not (Test-Up) -and $tries -lt 30) { Start-Sleep -Milliseconds 500; $tries++ }
        if (Test-Up) { Write-Host "[db] OK — 127.0.0.1:$Port (root / 비밀번호 없음 — 로컬 랩 전용)" }
        else { throw '[db] 기동 실패 — .runtime/data 로그 확인' }
    }
    'stop' {
        if (-not (Test-Up)) { Write-Host '[db] 이미 중지됨'; break }
        & (Join-Path $Bin 'mariadb-admin.exe') -u root --port=$Port -h 127.0.0.1 shutdown
        Write-Host '[db] 중지됨'
    }
    'status' {
        if (Test-Up) { Write-Host "[db] RUNNING — 127.0.0.1:$Port" } else { Write-Host '[db] STOPPED' }
    }
    'seed' {
        if (-not (Test-Up)) { throw '[db] 먼저 .\db.ps1 start' }
        # mariadb.exe에 PowerShell 파이프로 넣으면 콘솔 코드페이지를 거치며 한글이 깨진다(실측)
        # — 파이썬(pymysql)이 파일을 UTF-8로 직접 실행한다.
        Write-Host '[db] 스키마·표본 데이터 적용(run_sql.py)…'
        python (Join-Path $PSScriptRoot 'run_sql.py') (Join-Path $PSScriptRoot 'schema.sql') (Join-Path $PSScriptRoot 'seed.sql') --port $Port
        # 네이티브 종료코드는 $ErrorActionPreference='Stop'이 잡지 않는다 — 직접 본다.
        # (2026-09-19 실측: pymysql 미설치로 적재가 통째로 실패했는데 아래 '시드 완료'가 그대로 찍혔고,
        #  setup.ps1은 다음 단계로 넘어가 빈 DB 위에 랩이 서는 것처럼 보였다)
        if ($LASTEXITCODE -ne 0) {
            throw "[db] 시드 실패(run_sql.py exit $LASTEXITCODE) — 의존성(pymysql)·DB 기동 상태를 확인하세요: python -m pip install pymysql"
        }
        Write-Host '[db] 시드 완료 — 스키마 sl_shop'
    }
}
