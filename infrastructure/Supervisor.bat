@echo off
REM ============================================================
REM Microbot Tutorial Island - Supervisor
REM Full automation: patches from GitHub, screenshots to GitHub,
REM auto-login, auto-restart on crash.
REM
REM One-time setup:
REM   1. Put your GitHub token in github-token.txt (needs 'repo' scope)
REM   2. pip install pyautogui pytesseract pillow
REM   3. Install Tesseract OCR: https://github.com/UB-Mannheim/tesseract/wiki
REM
REM Then double-click this and walk away.
REM ============================================================
title Microbot Supervisor
cd /d "%~dp0"

REM Start screenshot uploader in background (uploads to GitHub for Muse to review)
echo Starting screenshot uploader...
start "Screenshot Uploader" /min python "%~dp0screenshot_uploader.py"

set "GAME_PID="

:loop
echo.
echo [%date% %time%] === Supervisor cycle ===
echo.

REM Step 1: Check GitHub for new patches
echo Checking GitHub for patches...
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0Check-Update.ps1"
if errorlevel 1 (
    echo Update check failed, continuing with current version...
)

REM Step 2: Kill any leftover game instances, then launch fresh
echo Cleaning up old game instances...
call :kill_game
timeout /t 3 >nul
echo Launching game...
REM Launch and capture the PID
start "" "%~dp0runtime\bin\javaw.exe" -jar "%~dp0microbot-tutorial-island.jar"
REM Find the PID of the javaw we just started (most recent javaw.exe)
for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq javaw.exe" /FO LIST ^| find /i "PID:"') do set "GAME_PID=%%a"
echo Game PID: %GAME_PID%

REM Step 3: Auto-click Login (unbuffered, output shown live)
echo Waiting for launcher (auto-clicking Login)...
python -u "%~dp0launcher_clicker.py" --timeout 180
if errorlevel 1 (
    echo WARNING: Login clicker timed out. May need manual login.
)

REM Step 4: Wait for the game to exit OR for a new patch to arrive
echo.
echo Game is running. Screenshots uploading to GitHub for review.
echo Press Ctrl+C to stop the supervisor.
echo.
:waitloop
timeout /t 30 >nul
REM Check if our specific game PID is still running
if defined GAME_PID (
    tasklist /FI "PID eq %GAME_PID%" 2>nul | find /i "%GAME_PID%" >nul
    if errorlevel 1 (
        echo.
        echo [%date% %time%] Game exited. Restarting...
        set "GAME_PID="
        timeout /t 5 >nul
        goto loop
    )
) else (
    REM Fallback: check if any javaw is running
    tasklist /FI "IMAGENAME eq javaw.exe" 2>nul | find /i "javaw.exe" >nul
    if errorlevel 1 (
        echo.
        echo [%date% %time%] Game exited. Restarting...
        timeout /t 5 >nul
        goto loop
    )
)
REM Check for new patches while game is running
echo [%date% %time%] Checking for patches...
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0Check-Update.ps1" -CheckOnly
REM Check-Update with -CheckOnly exits 2 if a new patch is available
if errorlevel 2 (
    echo.
    echo [%date% %time%] New patch available! Closing game to apply...
    call :kill_game
    timeout /t 5 >nul
    goto loop
)
REM Self-heal login states every cycle: dismiss the disconnect dialog and
REM click CLICK HERE TO PLAY / Login / Play Now if visible. Single quick
REM pass; does nothing when the game is already in-game.
echo [%date% %time%] Checking login state...
python -u "%~dp0launcher_clicker.py" --check-once
goto waitloop

REM Subroutine: gracefully close the game (allows config save), force if needed
:kill_game
if not defined GAME_PID (
    REM No tracked PID, try to find any javaw running our jar
    echo No tracked game PID, checking for any game instance...
    tasklist /FI "IMAGENAME eq javaw.exe" 2>nul | find /i "javaw.exe" >nul
    if errorlevel 1 (
        echo No game running.
        goto :eof
    )
    echo Attempting graceful shutdown...
    REM Try graceful close first (allows RuneLite to save config)
    taskkill /IM javaw.exe >nul 2>&1
    timeout /t 10 >nul
    tasklist /FI "IMAGENAME eq javaw.exe" 2>nul | find /i "javaw.exe" >nul
    if errorlevel 1 (
        echo Game closed gracefully.
        goto :eof
    )
    echo Force killing...
    taskkill /F /IM javaw.exe >nul 2>&1
    goto :eof
)
echo Closing game (PID %GAME_PID%)...
REM Try graceful first
taskkill /PID %GAME_PID% >nul 2>&1
timeout /t 10 >nul
tasklist /FI "PID eq %GAME_PID%" 2>nul | find /i "%GAME_PID%" >nul
if errorlevel 1 (
    echo Game closed gracefully.
    set "GAME_PID="
    goto :eof
)
echo Force killing PID %GAME_PID%...
taskkill /F /PID %GAME_PID% >nul 2>&1
set "GAME_PID="
goto :eof
