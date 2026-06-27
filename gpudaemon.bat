@echo off
title ReminecraftGPU Daemon
setlocal
set "DIR=%~dp0"
set "JAR=%DIR%reminecraft-gpu.jar"
if not exist "%JAR%" set "JAR=%DIR%runfolder\plugins\reminecraft-gpu.jar"
if not exist "%JAR%" (
    echo [ReMinecraft^|GPU^|] reminecraft-gpu.jar tidak ditemukan.
    echo [ReMinecraft^|GPU^|] Copy jar plugin ke folder ini lalu jalankan lagi.
    pause & exit /b 1
)

set "PORT=%1"
if "%PORT%"=="" set "PORT=25599"
set "BIND=%2"
if "%BIND%"=="" set "BIND=0.0.0.0"

echo [ReMinecraft^|GPU^|] Starting daemon on %BIND%:%PORT% ...
echo [ReMinecraft^|GPU^|] Jalankan lewat tunnel privat (Tailscale/WireGuard), jangan expose ke publik.
java --enable-native-access=ALL-UNNAMED -cp "%JAR%" org.reminecraft.gpu.daemon.GpuDaemon %PORT% %BIND%
pause
