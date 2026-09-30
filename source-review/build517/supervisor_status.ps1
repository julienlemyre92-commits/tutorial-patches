param(
    [string]$DiagPath,
    [string]$ClientLog = (Join-Path $env:USERPROFILE '.runelite\logs\client.log')
)

# Each Supervisor status refresh launches this file; no Supervisor restart needed.
$missionFile = Join-Path $env:USERPROFILE '.runelite\bot-mission.txt'
$mission = if (Test-Path -LiteralPath $missionFile) {
    (Get-Content -LiteralPath $missionFile -Raw).Trim()
} else { 'unknown' }
Write-Output "[STATUS] Desired mission: $mission"
if (!(Test-Path -LiteralPath $ClientLog)) {
    Write-Output '[STATUS] Runtime log unavailable; loaded build and stage unverified'
    exit 0
}
$tag = if ($mission -eq 'cooks') { '\[CooksAssistant\]' } elseif ($mission -eq 'tutorial') { 'TutorialIslandScript' } else { '.' }
$marker = Select-String -LiteralPath $ClientLog -Pattern ' - (?:\[[^\]]+\]\s+)?Build\s+\d+:\s+STARTUP\s+--\s+RUNNING_BUILD=\d+' |
    Where-Object { $_.Line -match $tag } | Select-Object -Last 1
$recent = @(Get-Content -LiteralPath $ClientLog -Tail 2500)
if ($marker) {
    $line = $marker.Line
    $buildMatch = [regex]::Match($line, 'RUNNING_BUILD=(\d+)(?:\s+\(patch-(\d+)\))?')
    Write-Output "[STATUS] Loaded build: $($buildMatch.Groups[1].Value) / patch $($buildMatch.Groups[2].Value)"
    Write-Output "[STATUS] Startup: $($line.Substring(0,23)) (runtime marker; not disk version)"
    $markerTime = $line.Substring(0,19)
    $recent = @($recent | Where-Object { $_.Length -ge 19 -and $_.Substring(0,19) -ge $markerTime })
} else {
    Write-Output '[STATUS] Loaded build: unverified (no explicit runtime startup marker)'
}
$hotStatusPath = Join-Path $env:USERPROFILE '.runelite\cooks-hot\status.properties'
if ($mission -eq 'cooks' -and (Test-Path -LiteralPath $hotStatusPath)) {
    $hot = @{}
    foreach ($row in Get-Content -LiteralPath $hotStatusPath) {
        if ($row -match '^([^#=]+)=(.*)$') { $hot[$matches[1]] = $matches[2] }
    }
    if ($hot.timestamp -and ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() - [long]$hot.timestamp) -lt 15000) {
        Write-Output "[STATUS] Live script: build $($hot.build) | PID $($hot.pid) | $($hot.gameState) | world $($hot.currentWorld)"
        Write-Output "[STATUS] Update mode: script hot reload (client remains open); disk patch is the cold-start baseline"
        if ($hot.error) { Write-Output "[STATUS] Reload held: $($hot.error)" }
    }
}
$missionLines = @($recent | Where-Object { $_ -match $tag })
$stage = $recent | Where-Object {
    $_ -match 'State forced:|State transition:|Detected start stage:|GET_BUCKET VERIFIED|DETECT: need|GET_BUCKET HOLD:'
} | Select-Object -Last 1
$inventory = $missionLines | Where-Object { $_ -match 'DIAG_CTL \[INVENTORY_DIALOGUE\]|GET_BUCKET ACTIVE:|GET_BUCKET VERIFIED:' } | Select-Object -Last 1
$position = $missionLines | Where-Object { $_ -match 'DIAG_CTL \[COORDS\]|DIAG_CTL \[COLLISION_LOS\].*center=' } | Select-Object -Last 1
$event = $missionLines | Where-Object { $_ -match 'LOGIN_GATE --|SWITCH COMPLETE --|GET_BUCKET HOLD:|Trade VERIFIED:|Buy-1 INVOKED=|Take INVOKED=' } | Select-Object -Last 1
foreach ($entry in @(@('Stage',$stage), @('Inventory',$inventory), @('Position',$position), @('Latest event',$event))) {
    if ($entry[1]) { Write-Output "[STATUS] $($entry[0]): $($entry[1])" }
}
$lastWrite = (Get-Item -LiteralPath $ClientLog).LastWriteTime
Write-Output "[STATUS] Runtime log updated: $($lastWrite.ToString('HH:mm:ss')); age $([int]((Get-Date)-$lastWrite).TotalSeconds)s"
