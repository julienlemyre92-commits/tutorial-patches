param([switch]$Once)
$ErrorActionPreference = 'SilentlyContinue'
$root = 'C:\Users\No 1\Desktop\New folder'
$clientLog = Join-Path $env:USERPROFILE '.runelite\logs\client.log'
$cookHotStatus = Join-Path $env:USERPROFILE '.runelite\cooks-hot\status.properties'
$ghostHotStatus = Join-Path $env:USERPROFILE '.runelite\restlessghost\status.properties'
$missionPath = Join-Path $env:USERPROFILE '.runelite\bot-mission.txt'
$jarPath = Join-Path $root 'microbot-tutorial-island.jar'
$patchPath = Join-Path $root 'patch-version.txt'
$screenshotDir = Join-Path $root 'screenshots'
$Host.UI.RawUI.WindowTitle = 'Microbot Live Status'
try {
    $raw = $Host.UI.RawUI
    if ($raw.BufferSize.Width -ge 76 -and $raw.BufferSize.Height -ge 14) {
        $raw.WindowSize = New-Object Management.Automation.Host.Size(76,12)
        $raw.BufferSize = New-Object Management.Automation.Host.Size(76,200)
        $raw.WindowPosition = New-Object Management.Automation.Host.Coordinates(0,0)
    }
} catch {}
$script:started = Get-Date
$script:manualPage = $null
$script:lastLoadedBuild = $null
function Read-Properties([string]$Path) {
    $map = @{}
    if (Test-Path -LiteralPath $Path) {
        foreach ($line in Get-Content -LiteralPath $Path) {
            if ($line -match '^([^#=]+)=(.*)$') { $map[$matches[1]] = $matches[2] }
        }
    }
    return $map
}
function Clean-LogLine([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) { return '' }
    return ($Value -replace '^.*? - \[(?:CooksAssistant(?:Script)?|RestlessGhost(?:Script)?)\]\s*','').Trim()
}
function Clip([string]$Value,[int]$Max=70) {
    if ([string]::IsNullOrWhiteSpace($Value)) { return 'not reported yet' }
    $v = ($Value -replace '\s+',' ').Trim()
    if ($v.Length -gt $Max) { return $v.Substring(0,$Max-3)+'...' }
    return $v
}
function Row([string]$Value,[ConsoleColor]$Color='Gray') {
    $v = Clip $Value 72
    Write-Host ('| {0,-72} |' -f $v) -ForegroundColor $Color
}
function Show-UpdateAnimation([string]$From,[string]$To) {
    $message = "NEW UPDATE $From ---> $To"
    $travel = [math]::Max(0,72-$message.Length)
    for ($frame=0; $frame -le 20; $frame++) {
        $left = [int][math]::Round($travel*$frame/20)
        $color = @('Yellow','White','Green','Cyan')[[int]([math]::Floor($frame/3) % 4)]
        Clear-Host
        Write-Host '+--------------------------------------------------------------------------+' -ForegroundColor Cyan
        Row 'MICROBOT / UPDATE' 'Cyan'
        Write-Host '+--------------------------------------------------------------------------+' -ForegroundColor Cyan
        Write-Host ('| {0,-72} |' -f '') -ForegroundColor Gray
        Row ((' '*$left)+$message) $color
        Write-Host ('| {0,-72} |' -f '') -ForegroundColor Gray
        Write-Host '+--------------------------------------------------------------------------+' -ForegroundColor Cyan
        Start-Sleep -Milliseconds 90
    }
}
function Show-Dashboard {
    $now = Get-Date
    $game = Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'microbot-tutorial-island\.jar' } | Select-Object -First 1
    $fileMission = if (Test-Path -LiteralPath $missionPath) { (Get-Content -LiteralPath $missionPath -Raw).Trim() } else { 'unknown' }
    $ghostHot = Read-Properties $ghostHotStatus
    $ghostFresh = $false
    if ($game -and $ghostHot.timestamp -and $ghostHot.pid) { try { $ghostAge = ($now.ToUniversalTime() - [DateTimeOffset]::FromUnixTimeMilliseconds([long]$ghostHot.timestamp).UtcDateTime).TotalSeconds; $ghostFresh = ($ghostAge -ge 0 -and $ghostAge -lt 20) -and ([long]$ghostHot.pid -eq [long]$game.ProcessId) -and (-not $ghostHot.error) } catch {} }
    $mission = if ($ghostFresh) { 'restlessghost' } else { $fileMission }
    $hot = if ($ghostFresh) { $ghostHot } else { Read-Properties $cookHotStatus }
    $hotFresh = $false
    if ($game -and $hot.timestamp -and $hot.pid) { try { $age = ($now.ToUniversalTime() - [DateTimeOffset]::FromUnixTimeMilliseconds([long]$hot.timestamp).UtcDateTime).TotalSeconds; $hotFresh = ($age -ge 0 -and $age -lt 20) -and ([long]$hot.pid -eq [long]$game.ProcessId) -and (-not $hot.error) } catch {} }
    $supervisor = Get-CimInstance Win32_Process -Filter "Name='cmd.exe'" | Where-Object { $_.CommandLine -match 'Supervisor\.bat' } | Select-Object -First 1
    $obs = Get-Process -Name obs64 -ErrorAction SilentlyContinue | Select-Object -First 1
    $diskPatch = if (Test-Path -LiteralPath $patchPath) { (Get-Content -LiteralPath $patchPath -Raw).Trim() } else { 'unknown' }
    $lines = if (Test-Path -LiteralPath $clientLog) { @(Get-Content -LiteralPath $clientLog -Tail 1800) } else { @() }
    $tag = if ($mission -eq 'cooks') { '\[CooksAssistant\]' } elseif ($mission -eq 'tutorial') { 'TutorialIslandScript' } elseif ($mission -eq 'restlessghost') { '\[RestlessGhost(?:Script)?\]' } else { '.' }
    $hotAge = 9999
    if ($hot.timestamp) { try { $hotAge=[int](($now.ToUniversalTime()-[DateTimeOffset]::FromUnixTimeMilliseconds([long]$hot.timestamp).UtcDateTime).TotalSeconds) } catch {} }
    $buildLine = @($lines | Where-Object { $_ -match 'RUNNING_BUILD=\d+' -and $_ -match $tag } | Select-Object -Last 1)
    $runtimePatch = 'unknown'
    if ($buildLine.Count -and $buildLine[0] -match 'RUNNING_BUILD=\d+\s+\(patch-(\d+)\)') { $runtimePatch=$matches[1] }
    $build = if ($hotFresh -and $hot.build -and $mission -eq 'restlessghost') { "Build $($hot.build) / Restless Ghost" } elseif ($hotFresh -and $hot.build) { "Build $($hot.build) / patch $runtimePatch" } elseif ($buildLine.Count) { Clean-LogLine $buildLine[0] } else { 'unverified' }
    $loadedBuild = if ($hotFresh -and $hot.build -match '^\d+$') { [int]$hot.build } else { $null }
    if ($null -ne $loadedBuild) {
        if ($null -ne $script:lastLoadedBuild -and $loadedBuild -gt $script:lastLoadedBuild) {
            Show-UpdateAnimation ([string]$script:lastLoadedBuild) ([string]$loadedBuild)
        }
        $script:lastLoadedBuild = $loadedBuild
    }
    $target = @($lines | Where-Object { $_ -match 'DIAG_CTL \[TARGET\]|DIAG_CTL \[STAGE\]' -and $_ -match $tag } | Select-Object -Last 1)
    $stateLine = @($lines | Where-Object { $_ -match 'State forced:|State transition:|Detected start stage:' -and $_ -match $tag } | Select-Object -Last 1)
    $stage = if ($ghostFresh -and $ghostHot.stage) { "Restless Ghost / $($ghostHot.stage) / varp $($ghostHot.questVarp)" } elseif ($target.Count) { Clean-LogLine $target[0] } elseif ($stateLine.Count) { Clean-LogLine $stateLine[0] } else { 'stage unknown' }
    $pos = @($lines | Where-Object { $_ -match 'DIAG_CTL \[COORDS\].*player=' -and $_ -match $tag } | Select-Object -Last 1)
    $inv = @($lines | Where-Object { $_ -match 'DIAG_CTL \[INVENTORY_DIALOGUE\].*inv=' -and $_ -match $tag } | Select-Object -Last 1)
    $evt = @($lines | Where-Object { $_ -match 'MILK_COW:|GET_BUCKET|QUEST_FINISHED|ROUTE_BLOCKED|TRAVERSAL|ERROR|Exception' -and $_ -match $tag } | Select-Object -Last 1)
    $logAge = if (Test-Path -LiteralPath $clientLog) { [int]($now-(Get-Item -LiteralPath $clientLog).LastWriteTime).TotalSeconds } else { -1 }
    $shot = Get-ChildItem -LiteralPath $screenshotDir -Filter '*_auto.png' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    $shotAge = if ($shot) { [int]($now-$shot.LastWriteTime).TotalSeconds } else { -1 }
    $world = if ($hotFresh -and $hot.currentWorld) {$hot.currentWorld} else {'?'}
    $gameState = if ($hotFresh -and $hot.gameState) {$hot.gameState} else {'STATUS STALE'}
    $page = if ($null -ne $script:manualPage) { $script:manualPage } else { [int]([math]::Floor(($now-$script:started).TotalSeconds/6)) % 3 }
    Clear-Host
    Write-Host '+--------------------------------------------------------------------------+' -ForegroundColor Cyan
    $title = @('MICROBOT / RUNTIME','MICROBOT / QUEST','MICROBOT / HEALTH')[$page]
    Row ("{0}  {1}  {2}" -f $title,$now.ToString('HH:mm:ss'),$mission.ToUpper()) 'Cyan'
    Write-Host '+--------------------------------------------------------------------------+' -ForegroundColor Cyan
    if ($page -eq 0) {
        Row ("Game: {0}  | World: {1}" -f $gameState,$world) $(if ($gameState -eq 'LOGGED_IN') {'Green'} else {'Yellow'})
        Row ("RuneLite: {0}" -f $(if($game){"RUNNING PID $($game.ProcessId)"}else{'OFFLINE'})) $(if($game){'Green'}else{'Red'})
        Row ("Script: {0}" -f $build) $(if($hotFresh){'Green'}else{'Yellow'})
        Row ("Supervisor: {0}" -f $(if($supervisor){"ON PID $($supervisor.ProcessId)"}else{'not found'})) $(if($supervisor){'Green'}else{'Yellow'})
        Row ("OBS: {0}  | Disk patch marker: {1}" -f $(if($obs){"ON PID $($obs.Id)"}else{'not detected'}),$diskPatch) 'Gray'
        Row 'OSRS revision is not exposed by the current client log.' 'DarkGray'
    } elseif ($page -eq 1) {
        Row ("Stage: {0}" -f $stage) 'Yellow'
        Row ("Tile: {0}" -f $(if($pos.Count){Clean-LogLine $pos[0]}else{'not reported'})) 'Cyan'
        Row ("Inventory: {0}" -f $(if($inv.Count){Clean-LogLine $inv[0]}else{'not reported'})) 'White'
        Row ("Latest: {0}" -f $(if($evt.Count){Clean-LogLine $evt[0]}else{'no event yet'})) 'Gray'
        Row ("Screenshot: {0}s ago" -f $shotAge) $(if($shotAge -ge 0 -and $shotAge -lt 90){'Green'}else{'Yellow'})
    } else {
        Row ("Client: {0}  | Hot status age: {1}s" -f $(if($game){'RUNNING'}else{'OFFLINE'}),$hotAge) $(if($hotAge -lt 20){'Green'}else{'Yellow'})
        Row ("Loaded build: {0}" -f $build) $(if($hotFresh){'Green'}else{'Yellow'})
        Row ("Runtime SHA: {0}" -f $(if($hot.sha256){$hot.sha256.Substring(0,[math]::Min(12,$hot.sha256.Length))}else{'unavailable'})) 'DarkGray'
        Row ("Log age: {0}s  | Last screenshot: {1}s" -f $logAge,$shotAge) $(if($logAge -lt 20){'Green'}else{'Yellow'})
        Row ("Reload error: {0}" -f $(if($hot.error){$hot.error}else{'none'})) $(if($hot.error){'Red'}else{'Green'})
    }
    Write-Host '+--------------------------------------------------------------------------+' -ForegroundColor Cyan
    Write-Host 'Auto-rotate 6s | 1/2/3 select view | R auto | Q close' -ForegroundColor DarkGray
}
do {
    Show-Dashboard
    if ($Once) { break }
    for ($i=0;$i -lt 20;$i++) {
        if ([Console]::KeyAvailable) {
            $key=[Console]::ReadKey($true).Key
            if ($key -eq [ConsoleKey]::Q) { break }
            if ($key -eq [ConsoleKey]::D1) { $script:manualPage=0 }
            if ($key -eq [ConsoleKey]::D2) { $script:manualPage=1 }
            if ($key -eq [ConsoleKey]::D3) { $script:manualPage=2 }
            if ($key -eq [ConsoleKey]::R) { $script:manualPage=$null; $script:started=Get-Date }
        }
        Start-Sleep -Milliseconds 100
    }
    if ($key -eq [ConsoleKey]::Q) { break }
    $key=$null
} while ($true)

