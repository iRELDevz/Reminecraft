@echo off
title Reminecraft - Build Auth Plugin
setlocal
set "ROOT=%~dp0.."
set "LOG=[ReMinecraft^|BUILD^|]"
set "SRC=%ROOT%\source\auth"
set "MVN=%ROOT%\apache-maven-3.9.16\bin\mvn.cmd"

echo ====================================================
echo        REMINE-CRAFT BUILD AUTH PLUGIN
echo ====================================================

if not exist "%SRC%\pom.xml" (
    echo %LOG% source\auth tidak ditemukan.
    pause & exit /b 1
)

set "JAVA_HOME=C:\reminecraft\jdk-25"
if not exist "%JAVA_HOME%\bin\java.exe" call "%ROOT%\_javadetect.bat"

echo %LOG% Building ReminecraftAuth...
cd /d "%SRC%"
call "%MVN%" package -DskipTests -q
if %errorlevel% neq 0 (
    echo %LOG% Build GAGAL.
    pause & exit /b 1
)

set "JAR=%SRC%\target\reminecraft-auth-1.0.0-SNAPSHOT.jar"
copy /Y "%JAR%" "%ROOT%\runfolder\plugins\reminecraft-auth.jar" >nul

echo %LOG% Deployed: reminecraft-auth.jar
echo.
echo ====================================================
echo %LOG% Done. Restart server untuk apply.
echo ====================================================
cd /d "%ROOT%"
pause
