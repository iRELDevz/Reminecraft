@echo off
title Reminecraft - Build Server
setlocal
set "ROOT=%~dp0.."
set "LOG_BUILD=[ReMinecraft^|BUILD^|]"
set "LOG_ERROR=[ReMinecraft^|ERROR^|]"

echo ====================================================
echo          REMINE-CRAFT BUILD SERVER (FULL)
echo  Waktu: 10-40 menit. Hanya perlu jika mengubah
echo  Purpur patches / Paper core internals.
echo ====================================================

call "%ROOT%\_javadetect.bat"
if %errorlevel% neq 0 ( pause & exit /b 1 )

cd /d "%ROOT%\source\java"

echo %LOG_BUILD% Applying all patches...
call gradlew.bat applyAllPatches
if %errorlevel% neq 0 (
    echo %LOG_ERROR% applyAllPatches failed.
    cd /d "%ROOT%"
    pause & exit /b 1
)

if exist "..\..\log4j2.xml" (
    copy /Y "..\..\log4j2.xml" "paper-server\src\main\resources\log4j2.xml" >nul
)

echo %LOG_BUILD% Building server JAR...
call gradlew.bat :purpur-server:createPaperclipJar
if %errorlevel% neq 0 (
    echo %LOG_ERROR% Gradle build failed.
    cd /d "%ROOT%"
    pause & exit /b 1
)

set JARFILE=
for /f "delims=" %%f in ('dir /b "purpur-server\build\libs\purpur-paperclip-*.jar" 2^>nul') do set JARFILE=%%f
if "%JARFILE%"=="" (
    echo %LOG_ERROR% No paperclip jar found.
    cd /d "%ROOT%"
    pause & exit /b 1
)

if not exist "%ROOT%\runfolder" mkdir "%ROOT%\runfolder"
copy /Y "purpur-server\build\libs\%JARFILE%" "%ROOT%\runfolder\reminecraft-server.jar" >nul
echo %LOG_BUILD% Server JAR updated: %JARFILE%

cd /d "%ROOT%"
echo.
echo ====================================================
echo %LOG_BUILD% Build selesai. Jalankan localhost.bat atau runner.bat.
echo ====================================================
pause
