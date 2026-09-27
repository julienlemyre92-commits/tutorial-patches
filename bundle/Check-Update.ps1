# Checks GitHub for new patches. Downloads and applies if newer.
# Used by Supervisor.bat before each game launch.
#
# -CheckOnly: just check if a new patch exists, exit 2 if yes, 0 if no.
#             Used by Supervisor while the game is running.
#
# Safety: backs up the jar before patching, validates after, rolls back on failure.

param(
    [switch]$CheckOnly
)

$ErrorActionPreference = "Stop"

$bundleDir = $PSScriptRoot
$jar = [System.IO.Path]::Combine($bundleDir, "microbot-tutorial-island.jar")
$jarBackup = [System.IO.Path]::Combine($bundleDir, "microbot-tutorial-island.jar.bak")
$versionFile = [System.IO.Path]::Combine($bundleDir, "patch-version.txt")

$repo = "julienlemyre92-commits/tutorial-patches"
# Cache-buster: raw.githubusercontent.com is CDN-cached; without this the Supervisor
# can see a stale version.txt for minutes after a new patch is uploaded.
$cacheBuster = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$versionUrl = "https://raw.githubusercontent.com/$repo/main/version.txt?t=$cacheBuster"
$patchBaseUrl = "https://raw.githubusercontent.com/$repo/main/patches"

function Test-JarValid {
    param([string]$jarPath)
    # A valid jar is a valid zip with the expected main class
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zip = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
        try {
            $hasScript = $false
            $hasManifest = $false
            foreach ($entry in $zip.Entries) {
                if ($entry.FullName -eq "net/runelite/client/plugins/microbot/tutorialisland/TutorialIslandScript.class") {
                    $hasScript = $true
                }
                if ($entry.FullName -eq "META-INF/MANIFEST.MF") {
                    $hasManifest = $true
                }
                if ($hasScript -and $hasManifest) { break }
            }
            return ($hasScript -and $hasManifest)
        } finally {
            $zip.Dispose()
        }
    } catch {
        return $false
    }
}

try {
    $localVersion = 0
    if (Test-Path -LiteralPath $versionFile) {
        $localVersion = [int]((Get-Content -LiteralPath $versionFile -Raw).Trim())
    }

    Write-Host "Local version: $localVersion"
    $remoteVersion = [int]((Invoke-WebRequest -Uri $versionUrl -UseBasicParsing).Content.Trim())
    Write-Host "Remote version: $remoteVersion"

    if ($remoteVersion -le $localVersion) {
        Write-Host "Already up to date."
        exit 0
    }

    if ($CheckOnly) {
        Write-Host "New patch v$remoteVersion available!" -ForegroundColor Yellow
        exit 2
    }

    # Verify the current jar is valid before touching it
    if (-not (Test-JarValid -jarPath $jar)) {
        Write-Host "ERROR: Current jar is invalid/corrupt. Refusing to patch." -ForegroundColor Red
        Write-Host "Restore from a fresh Build download." -ForegroundColor Red
        exit 1
    }

    $patchUrl = "$patchBaseUrl/patch-$remoteVersion.zip"
    Write-Host "Downloading patch v$remoteVersion..." -ForegroundColor Cyan

    # Use a temp dir without spaces to avoid path issues
    $tmpDir = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), "tutorial-patch")
    $zipPath = [System.IO.Path]::Combine($tmpDir, "patch.zip")
    $extractDir = [System.IO.Path]::Combine($tmpDir, "extracted")
    if (Test-Path -LiteralPath $tmpDir) { Remove-Item -LiteralPath $tmpDir -Recurse -Force }
    [System.IO.Directory]::CreateDirectory($extractDir) | Out-Null

    (New-Object System.Net.WebClient).DownloadFile($patchUrl, $zipPath)
    Expand-Archive -LiteralPath $zipPath -DestinationPath $extractDir -Force

    # Backup the working jar
    Write-Host "Backing up current jar..." -ForegroundColor Cyan
    Copy-Item -LiteralPath $jar -Destination $jarBackup -Force

    Write-Host "Injecting into jar..." -ForegroundColor Cyan
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::Open($jar, 'Update')
    try {
        Get-ChildItem -LiteralPath $extractDir -Recurse -File | ForEach-Object {
            $relPath = $_.FullName.Substring($extractDir.Length + 1) -replace '\\','/'
            $existing = $zip.Entries | Where-Object { $_.FullName -eq $relPath }
            if ($existing) { $existing.Delete() }
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, $relPath) | Out-Null
        }
    } finally {
        $zip.Dispose()
    }

    # Validate the patched jar
    Write-Host "Validating patched jar..." -ForegroundColor Cyan
    if (-not (Test-JarValid -jarPath $jar)) {
        Write-Host "ERROR: Patched jar failed validation! Rolling back..." -ForegroundColor Red
        Copy-Item -LiteralPath $jarBackup -Destination $jar -Force
        if (-not (Test-JarValid -jarPath $jar)) {
            Write-Host "CRITICAL: Rollback also failed. Jar is corrupt." -ForegroundColor Red
            Write-Host "Restore from a fresh Build download." -ForegroundColor Red
        } else {
            Write-Host "Rolled back to previous working jar." -ForegroundColor Yellow
        }
        exit 1
    }

    Set-Content -LiteralPath $versionFile -Value $remoteVersion
    Write-Host "Patch v$remoteVersion applied and validated!" -ForegroundColor Green
    try { Remove-Item -LiteralPath $tmpDir -Recurse -Force -ErrorAction SilentlyContinue } catch { }
    exit 0
} catch {
    Write-Host "Update check failed: $_" -ForegroundColor Red
    exit 1
}
