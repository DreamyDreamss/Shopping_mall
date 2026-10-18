param(
    [string]$Plugin = 'D:/speclinker-main',
    [string]$Workspace = 'D:/sl-shop',
    [string]$Runtime = 'D:/sl-shop-runtime'
)
$ErrorActionPreference = 'Stop'
$Source = Split-Path $PSScriptRoot -Parent
$Maria = 'D:/Shopping_mall/.runtime/mariadb-11.4.5-winx64'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-17'
$env:PATH = "$env:JAVA_HOME/bin;D:/Shopping_mall/.runtime/apache-maven-3.9.9/bin;$env:PATH"
$env:PYTHONUTF8 = '1'
$env:SL_AI_PORT = '18773'
$Python = (Get-Command python).Source
$Logs = Join-Path $Runtime 'logs'
New-Item -ItemType Directory -Force -Path $Logs | Out-Null

function Start-Owned([int]$Port, [string]$Identity, [string]$File, [string[]]$Arguments, [string]$Name) {
    $listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        $ownerProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
        $actualCommand = ([string]$ownerProcess.CommandLine).Replace('\', '/').ToLowerInvariant()
        $expectedCommand = $Identity.Replace('\', '/').ToLowerInvariant()
        if (-not $actualCommand.Contains($expectedCommand)) {
            throw "Port $Port belongs to another process. No process was stopped: $($listener.OwningProcess)"
        }
        Write-Host "$Name already running on $Port"
        return
    }
    Start-Process -FilePath $File -ArgumentList $Arguments -WorkingDirectory $Source -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $Logs "$Name.out.log") `
        -RedirectStandardError (Join-Path $Logs "$Name.err.log") | Out-Null
}

Start-Owned 13328 "$Runtime/mariadb-data/my.ini" "$Maria/bin/mariadbd.exe" `
    @("--defaults-file=$Runtime/mariadb-data/my.ini", '--console') 'database'
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    if (Get-NetTCPConnection -LocalPort 13328 -State Listen -ErrorAction SilentlyContinue) { break }
    Start-Sleep -Milliseconds 500
}
Start-Owned 18985 "$Source/apps/shop-api/target/shop-api-0.1.0.jar" "$env:JAVA_HOME/bin/java.exe" `
    @('-Dfile.encoding=UTF-8', '-jar', "$Source/apps/shop-api/target/shop-api-0.1.0.jar",
      '--server.address=127.0.0.1', '--server.port=18985',
      '--spring.datasource.url=jdbc:mariadb://127.0.0.1:13328/sl_shop_modern',
      '--spring.datasource.username=root', '--spring.datasource.password=', '--spring.sql.init.mode=never') 'shop'
Start-Owned 18772 "$Workspace --port 18772" $Python `
    @('-X', 'utf8', "$Plugin/scripts/speclens_server.py", $Workspace, '--port', '18772') 'speclens'
foreach ($url in @('http://127.0.0.1:18985/shop/', 'http://127.0.0.1:18772/api/ping')) {
    $ready = $false
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri $url -Headers @{'X-SL'='1'} -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch { }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) { throw "Not ready: $url — inspect $Logs" }
}
Write-Host 'SpecLens: http://localhost:18772/docs/viewer2/index.html'
Write-Host 'Shop: http://127.0.0.1:18985/shop'
