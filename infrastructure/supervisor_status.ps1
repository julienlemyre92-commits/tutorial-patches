<# supervisor_status.ps1 -- Build 391 mirror of Alex's parsing rule (2026-09-29).
 #
 # Alex's canonical rule, mirrored here so both sides parse evidence the same way:
 #   1. ONLY an explicit "STARTUP -- RUNNING_BUILD=<n>" line proves the loaded build.
 #   2. Feature evidence (SWITCH COMPLETE, MISSION_SELECT, stage/quest lines)
 #      is reported SEPARATELY from build proof -- never as build proof.
 #   3. Feature lines OLDER than the latest STARTUP marker are flagged STALE
 #      (they belong to a previous run; the diag log is truncated on startup,
 #      so a healthy run always has STARTUP first).
 #   4. Historical VARP / game-state text is NEVER treated as live build evidence.
 #
 # Usage: .\supervisor_status.ps1 [-LogPath <diag log>] [-Lines <n>]
 # Defaults to the Tutorial Island diag log; pass -LogPath for the
 # cooks-assistant-diag.log or client.log.
#>
param(
    [string]$LogPath = (Join-Path $env:USERPROFILE ".runelite\tutorial-island-diag.log"),
    [int]$Lines = 400
)

if (-not (Test-Path $LogPath)) {
    Write-Output "LOG MISSING: $LogPath"
    exit 1
}

$tail = Get-Content $LogPath -Tail $Lines -ErrorAction SilentlyContinue
if (-not $tail) {
    Write-Output "LOG EMPTY: $LogPath"
    exit 1
}

# --- 1. Loaded build: ONLY from explicit STARTUP -- RUNNING_BUILD markers ---
$startupHits = @()
for ($i = 0; $i -lt $tail.Count; $i++) {
    if ($tail[$i] -match 'STARTUP -- RUNNING_BUILD=(\d+)') {
        $startupHits += [pscustomobject]@{ Line = $i + 1; Build = $Matches[1]; Text = $tail[$i] }
    }
}

if ($startupHits.Count -eq 0) {
    Write-Output "LOADED BUILD: UNKNOWN -- no 'STARTUP -- RUNNING_BUILD=' marker in the last $Lines lines."
    Write-Output "(Refusing to infer the build from VARP text, patch-version.txt, or feature lines.)"
} else {
    $latest = $startupHits[-1]
    Write-Output ("LOADED BUILD: {0}  (line {1}: {2})" -f $latest.Build, $latest.Line, $latest.Text.Trim())
    if ($startupHits.Count -gt 1) {
        $older = ($startupHits[0..($startupHits.Count - 2)] | ForEach-Object { $_.Build }) -join ','
        Write-Output ("Older STARTUP markers in window (ignored): {0}" -f $older)
    }
    $startupLine = $latest.Line
}

# --- 2. Feature evidence: reported separately, never as build proof ---
$featurePatterns = @('SWITCH COMPLETE', 'MISSION_SELECT', 'owns the scheduler',
    'Tutorial Island complete!', 'QuestState', 'varp281', 'varp 281',
    'Cook''s Assistant', 'TALK_COOK_START', 'RETURN_COOK')
$features = @()
for ($i = 0; $i -lt $tail.Count; $i++) {
    foreach ($p in $featurePatterns) {
        if ($tail[$i] -like "*$p*") {
            $features += [pscustomobject]@{ Line = $i + 1; Text = $tail[$i].Trim() }
            break
        }
    }
}

Write-Output ""
Write-Output "FEATURE EVIDENCE (not build proof):"
if ($features.Count -eq 0) {
    Write-Output "  (none in window)"
} else {
    # --- 3. Flag feature lines older than the latest STARTUP marker as STALE ---
    foreach ($f in $features | Select-Object -Last 15) {
        $tag = ""
        if ($startupHits.Count -gt 0 -and $f.Line -lt $startupLine) { $tag = "  [STALE: older than latest STARTUP]" }
        Write-Output ("  line {0}: {1}{2}" -f $f.Line, $f.Text, $tag)
    }
}

# --- 4. VARP / state text is never build evidence (explicit non-use) ---
Write-Output ""
Write-Output "NOTE: VARP/state text above is shown for context only and was NOT used as build evidence."
