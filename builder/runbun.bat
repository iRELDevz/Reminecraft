@echo off
title Reminecraft - Bun Sidecar
setlocal
set "ROOT=%~dp0.."
set "BUN_DIR=%ROOT%\source\bun"
set "LOG=[ReMinecraft^|BUN^|]"

echo ====================================================
echo           REMINE-CRAFT BUN SIDECAR
echo ====================================================

where bun >nul 2>&1
if %errorlevel% neq 0 (
    echo %LOG% Bun tidak ditemukan. Install dari https://bun.sh
    pause & exit /b 1
)

if not exist "%BUN_DIR%\node_modules" (
    echo %LOG% Installing dependencies...
    cd /d "%BUN_DIR%"
    bun install
)

echo %LOG% Starting sidecar on http://localhost:25500
echo %LOG% Dashboard: http://localhost:25500/
echo %LOG% Tekan Ctrl+C untuk stop.
echo.

cd /d "%BUN_DIR%"
bun run src/server.ts
