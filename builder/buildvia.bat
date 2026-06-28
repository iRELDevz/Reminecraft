@echo off
title Reminecraft - Build Via (Multi-Version)
setlocal enabledelayedexpansion
set "ROOT=%~dp0.."
set "LOG_BUILD=[ReMinecraft^|BUILD^|]"
set "LOG_PLUGIN=[ReMinecraft^|PLUGIN^|]"
set "LOG_ERROR=[ReMinecraft^|ERROR^|]"

REM ── Java for Gradle builds ──────────────────────────
set "PLUGIN_JAVA_HOME=C:\reminecraft\jdk-25"
if not exist "%PLUGIN_JAVA_HOME%\bin\java.exe" (
    call "%ROOT%\_javadetect.bat"
    set "PLUGIN_JAVA_HOME=%JAVA_EXE:\bin\java.exe=%"
)
set "JAVA_HOME=%PLUGIN_JAVA_HOME%"

if not exist "%ROOT%\source" mkdir "%ROOT%\source"
if not exist "%ROOT%\plugin" mkdir "%ROOT%\plugin"
if not exist "%ROOT%\runfolder\plugins" mkdir "%ROOT%\runfolder\plugins"

REM ── ViaVersion : klien lebih BARU dari server bisa masuk ──
call :build_via ViaVersion   https://github.com/ViaVersion/ViaVersion.git   ViaVersion
if errorlevel 1 goto :fail

REM ── ViaBackwards : klien lebih LAMA (s/d 1.8) bisa masuk ──
call :build_via ViaBackwards https://github.com/ViaVersion/ViaBackwards.git ViaBackwards
if errorlevel 1 goto :fail

REM ── ViaRewind : dukungan klien 1.7.x / penyempurnaan 1.8.x ──
call :build_via ViaRewind    https://github.com/ViaVersion/ViaRewind.git    ViaRewind
if errorlevel 1 goto :fail

cd /d "%ROOT%"
echo.
echo ====================================================
echo %LOG_BUILD% Via stack selesai. Restart server untuk apply.
echo %LOG_PLUGIN% Java client semua versi (1.8+ ) kini bisa masuk.
echo ====================================================
pause
exit /b 0

REM ── subroutine: %1=name %2=git-url %3=jar-prefix ──
:build_via
set "VNAME=%~1"
set "VURL=%~2"
set "VJAR=%~3"
echo %LOG_BUILD% Building %VNAME%...
if not exist "%ROOT%\source\%VNAME%" (
    echo %LOG_BUILD% Cloning %VNAME% dari source...
    git clone --depth 1 "%VURL%" "%ROOT%\source\%VNAME%"
    if errorlevel 1 ( echo %LOG_ERROR% Clone %VNAME% gagal. & exit /b 1 )
)
cd /d "%ROOT%\source\%VNAME%"
call gradlew.bat build --no-daemon -q
if errorlevel 1 ( echo %LOG_ERROR% %VNAME% build failed. & cd /d "%ROOT%" & exit /b 1 )
set "FOUND="
for /f "delims=" %%f in ('dir /b "build\libs\%VJAR%-*.jar" 2^>nul ^| findstr /v /i "sources javadoc dev unshaded"') do (
    copy /Y "build\libs\%%f" "%ROOT%\plugin\%VJAR%.jar" >nul
    copy /Y "build\libs\%%f" "%ROOT%\runfolder\plugins\%VJAR%.jar" >nul
    set "FOUND=1"
)
if not defined FOUND ( echo %LOG_ERROR% Jar %VNAME% tidak ditemukan. & cd /d "%ROOT%" & exit /b 1 )
echo %LOG_PLUGIN% %VNAME% built and deployed.
cd /d "%ROOT%"
exit /b 0

:fail
cd /d "%ROOT%"
echo %LOG_ERROR% Build Via stack dibatalkan karena error.
pause
exit /b 1
