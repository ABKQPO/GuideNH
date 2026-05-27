[CmdletBinding()]
param(
    [string]$ConfigPath = ".\tools\runtime-bridge\runtime-bridge-config.sample.json",
    [switch]$LaunchClient,
    [switch]$SkipGuideVsc,
    [switch]$WhatIf
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$PSDefaultParameterValues['*:Encoding'] = 'utf8'

function Read-JsonFile {
    param([string]$Path)
    return Get-Content -LiteralPath $Path -Encoding utf8 -Raw | ConvertFrom-Json
}

function Ensure-RuntimeBridgeConfigBlock {
    param(
        [string]$ConfigFile,
        [string]$BridgeHost,
        [int]$Port,
        [string]$Token
    )

    $text = Get-Content -LiteralPath $ConfigFile -Encoding utf8 -Raw
    $block = @"

    runtimebridge {
        B:enabled=true
        S:host=$BridgeHost
        I:port=$Port
        S:token=$Token
        I:maxConnections=2
        I:maxDeltaEntries=200
        I:maxMessageBytes=262144
        I:maxPageSize=200
        I:maxSubscriptions=16
    }
"@

    if ($text -match '(?ms)^\s*runtimebridge\s*\{.*?^\s*\}') {
        $replacement = @"
    runtimebridge {
        B:enabled=true
        S:host=$BridgeHost
        I:port=$Port
        S:token=$Token
        I:maxConnections=2
        I:maxDeltaEntries=200
        I:maxMessageBytes=262144
        I:maxPageSize=200
        I:maxSubscriptions=16
    }
"@
        $updated = [System.Text.RegularExpressions.Regex]::Replace(
            $text,
            '(?ms)^\s*runtimebridge\s*\{.*?^\s*\}',
            $replacement.TrimEnd()
        )
    } else {
        $updated = $text.TrimEnd() + "`r`n" + $block.TrimEnd() + "`r`n"
    }

    if (-not $WhatIf) {
        Set-Content -LiteralPath $ConfigFile -Encoding utf8 -Value $updated
    }
}

function Wait-ForBridge {
    param(
        [string]$NodeExe,
        [string]$QueryScript,
        [string]$BridgeHost,
        [int]$Port,
        [string]$Token,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            & $NodeExe $QueryScript --host $BridgeHost --port $Port --token $Token --mode smoke --timeoutMs 3000 | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 3
        }
    }

    throw "Timed out waiting for the GuideNH runtime bridge at ws://$BridgeHost`:$Port."
}

function Invoke-GuideVscRuntimeVerification {
    param(
        [string]$GuideVscRoot,
        [string]$NodeExe,
        [string]$BridgeHost,
        [int]$Port,
        [string]$Token,
        [bool]$AllowRemote,
        [int]$TimeoutMs
    )

    $tscExe = Join-Path $GuideVscRoot "node_modules\.bin\tsc.cmd"
    $verifyScript = Join-Path $GuideVscRoot "out\scripts\verifyRuntimeBridgeLive.js"

    if (-not (Test-Path -LiteralPath $tscExe)) {
        throw "Missing TypeScript compiler at $tscExe."
    }

    Push-Location $GuideVscRoot
    try {
        & $tscExe -p "."
        if ($LASTEXITCODE -ne 0) {
            throw "guide-vsc TypeScript compile failed with exit code $LASTEXITCODE."
        }

        $arguments = @(
            $verifyScript,
            "--host", $BridgeHost,
            "--port", "$Port",
            "--token", $Token,
            "--timeoutMs", "$TimeoutMs"
        )
        if ($AllowRemote) {
            $arguments += "--allowRemote"
        }

        & $NodeExe @arguments
        if ($LASTEXITCODE -ne 0) {
            throw "guide-vsc live runtime verification failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

$config = Read-JsonFile -Path $ConfigPath
$guideNhRoot = [System.IO.Path]::GetFullPath($config.guideNhRoot)
$guideVscRoot = [System.IO.Path]::GetFullPath($config.guideVscRoot)
$runTask = if ($config.runTask) { [string]$config.runTask } else { "runClient25" }
$startupTimeoutSeconds = if ($config.startupTimeoutSeconds) { [int]$config.startupTimeoutSeconds } else { 240 }
$bridgeHost = [string]$config.host
$port = [int]$config.port
$token = [string]$config.token
$shouldLaunchClient = $LaunchClient.IsPresent -or [bool]$config.launchClient
$shouldVerifyGuideVsc = -not $SkipGuideVsc.IsPresent
$guideVscAllowRemote = if ($null -ne $config.guideVscAllowRemote) { [bool]$config.guideVscAllowRemote } else { $false }
$guideVscTimeoutMs = if ($config.guideVscTimeoutMs) { [int]$config.guideVscTimeoutMs } else { 30000 }

$configFile = Join-Path $guideNhRoot "run\client_new\config\guidenh\guidenh.cfg"
$nodeExe = (Get-Command node).Source
$queryScript = Join-Path $guideNhRoot "tools\runtime-bridge\query-runtime-bridge.mjs"

Ensure-RuntimeBridgeConfigBlock -ConfigFile $configFile -BridgeHost $bridgeHost -Port $port -Token $token

if ($WhatIf) {
    Write-Host "Would update $configFile for runtime bridge host=$bridgeHost port=$port."
    if ($shouldLaunchClient) {
        Write-Host "Would launch gradle task $runTask."
    }
    Write-Host "Would query bridge with $queryScript."
    if ($shouldVerifyGuideVsc) {
        Write-Host "Would compile guide-vsc and run its live runtime verification."
    }
    return
}

if ($shouldLaunchClient) {
    Start-Process -FilePath "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" `
        -ArgumentList "-Command", "& .\gradlew.bat $runTask" `
        -WorkingDirectory $guideNhRoot `
        -WindowStyle Hidden | Out-Null
}

Wait-ForBridge -NodeExe $nodeExe -QueryScript $queryScript -BridgeHost $bridgeHost -Port $port -Token $token -TimeoutSeconds $startupTimeoutSeconds

& $nodeExe $queryScript --host $bridgeHost --port $port --token $token --mode smoke --timeoutMs 15000
if ($LASTEXITCODE -ne 0) {
    throw "Raw runtime bridge smoke verification failed with exit code $LASTEXITCODE."
}

if ($shouldVerifyGuideVsc) {
    Invoke-GuideVscRuntimeVerification `
        -GuideVscRoot $guideVscRoot `
        -NodeExe $nodeExe `
        -BridgeHost $bridgeHost `
        -Port $port `
        -Token $token `
        -AllowRemote $guideVscAllowRemote `
        -TimeoutMs $guideVscTimeoutMs
}
