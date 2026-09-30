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
$tokenFile = [System.IO.Path]::Combine($bundleDir, "github-token.txt")

$repo = "julienlemyre92-commits/tutorial-patches"
# Prefer the GitHub API, which sees a published marker immediately. Keep the raw
# CDN as a fallback for machines without the optional token or during API outages.
$cacheBuster = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$versionUrl = "https://raw.githubusercontent.com/$repo/main/version.txt?t=$cacheBuster"
$patchBaseUrl = "https://raw.githubusercontent.com/$repo/main/patches"
$apiVersionUrl = "https://api.github.com/repos/$repo/contents/version.txt?ref=main"
$apiPatchUrl = "https://api.github.com/repos/$repo/contents/patches/patch-{0}.zip?ref=main"

function Get-GitHubHeaders {
    $headers = @{ Accept = "application/vnd.github+json"; "User-Agent" = "tutorial-supervisor" }
    if (Test-Path -LiteralPath $tokenFile) {
        $token = (Get-Content -LiteralPath $tokenFile -Raw).Trim()
        if ($token) { $headers.Authorization = "Bearer $token" }
    }
    return $headers
}

function Get-RemoteVersion {
    try {
        $json = Invoke-RestMethod -Uri $apiVersionUrl -Headers (Get-GitHubHeaders) -UseBasicParsing
        return [int]([Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($json.content)).Trim())
    } catch {
        return [int]((Invoke-WebRequest -Uri $versionUrl -UseBasicParsing).Content.Trim())
    }
}

function Download-Patch {
    param([int]$Version, [string]$Destination)
    try {
        $apiUrl = [string]::Format($apiPatchUrl, $Version)
        $headers = Get-GitHubHeaders
        $headers.Accept = "application/vnd.github.raw"
        Invoke-WebRequest -Uri $apiUrl -Headers $headers -OutFile $Destination -UseBasicParsing
    } catch {
        $patchUrl = "$patchBaseUrl/patch-$Version.zip?t=$cacheBuster"
        (New-Object System.Net.WebClient).DownloadFile($patchUrl, $Destination)
    }
}

function Test-JarValid {
    param([string]$jarPath)
    # A valid jar is a valid zip with the expected main class
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zip = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
        try {
            $hasScript = $false
            $hasManifest = $false
            $hasMainClass = $false
            foreach ($entry in $zip.Entries) {
                if ($entry.FullName -eq "net/runelite/client/plugins/microbot/tutorialisland/TutorialIslandScript.class") {
                    $hasScript = $true
                }
                if ($entry.FullName -eq "META-INF/MANIFEST.MF") {
                    $hasManifest = $true
                    $reader = [System.IO.StreamReader]::new($entry.Open())
                    try {
                        $manifestText = $reader.ReadToEnd()
                        $hasMainClass = $manifestText -match '(?m)^Main-Class: net\.runelite\.client\.RuneLite\r?$'
                    } finally {
                        $reader.Dispose()
                    }
                }
                if ($hasScript -and $hasManifest -and $hasMainClass) { break }
            }
            return ($hasScript -and $hasManifest -and $hasMainClass)
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
    $remoteVersion = Get-RemoteVersion
    Write-Host "Remote version: $remoteVersion"

    if ($remoteVersion -le $localVersion) {
        Write-Host "Already up to date."
        exit 0
    }

    if ($CheckOnly) {
        $hotHelper = Join-Path $bundleDir 'hot_update.py'
        if (Test-Path -LiteralPath $hotHelper) {
            & python -u $hotHelper $remoteVersion
            if ($LASTEXITCODE -eq 0) { exit 0 }
        }
        Write-Host "New patch v$remoteVersion available!" -ForegroundColor Yellow
        exit 2
    }

    # Verify the current jar is valid before touching it
    if (-not (Test-JarValid -jarPath $jar)) {
        Write-Host "ERROR: Current jar is invalid/corrupt. Refusing to patch." -ForegroundColor Red
        Write-Host "Restore from a fresh Build download." -ForegroundColor Red
        exit 1
    }

    Write-Host "Downloading patch v$remoteVersion..." -ForegroundColor Cyan

    # Use a temp dir without spaces to avoid path issues
    $tmpDir = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), "tutorial-patch")
    $zipPath = [System.IO.Path]::Combine($tmpDir, "patch.zip")
    $extractDir = [System.IO.Path]::Combine($tmpDir, "extracted")
    if (Test-Path -LiteralPath $tmpDir) { Remove-Item -LiteralPath $tmpDir -Recurse -Force }
    [System.IO.Directory]::CreateDirectory($extractDir) | Out-Null

    Download-Patch -Version $remoteVersion -Destination $zipPath
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
