@echo off
title Reminecraft - Build Core Plugin
setlocal
set "ROOT=%~dp0.."
set "LOG_BUILD=[ReMinecraft^|BUILD^|]"
set "LOG_ERROR=[ReMinecraft^|ERROR^|]"

set "PLUGIN_JAVA_HOME=C:\reminecraft\jdk-25"
if not exist "%PLUGIN_JAVA_HOME%\bin\java.exe" (
    call "%ROOT%\_javadetect.bat"
    set "PLUGIN_JAVA_HOME=%JAVA_EXE:\bin\java.exe=%"
)
set "MVN=%ROOT%\apache-maven-3.9.16\bin\mvn.cmd"

if not exist "%ROOT%\core" (
    echo %LOG_ERROR% C:\reminecraft\core belum ada.
    echo %LOG_ERROR% Taruh source code plugin core di folder C:\reminecraft\core dulu.
    pause & exit /b 1
)

echo %LOG_BUILD% Building reminecraft-core...
set "JAVA_HOME=%PLUGIN_JAVA_HOME%"
cd /d "%ROOT%\core"
call "%MVN%" package -DskipTests -q
if %errorlevel% neq 0 (
    echo %LOG_ERROR% reminecraft-core build failed.
    cd /d "%ROOT%"
    pause & exit /b 1
)

for /f "delims=" %%f in ('dir /b "target\reminecraft-core-*.jar" 2^>nul') do (
    copy /Y "target\%%f" "%ROOT%\plugin\reminecraft-core.jar" >nul
    copy /Y "target\%%f" "%ROOT%\runfolder\plugins\reminecraft-core.jar" >nul
    echo %LOG_BUILD% Deployed: %%f
)

cd /d "%ROOT%"
echo.
echo ====================================================
echo %LOG_BUILD% Core plugin updated. Restart server untuk apply.
echo ====================================================
pause
