@echo off
title Reminecraft - Build Perms Plugin
setlocal
set "ROOT=%~dp0.."
set "LOG=[ReMinecraft^|BUILD^|]"
set "SRC=%ROOT%\source\perms"
set "MVN=%ROOT%\apache-maven-3.9.16\bin\mvn.cmd"

echo ====================================================
echo        REMINE-CRAFT BUILD PERMS PLUGIN
echo ====================================================

if not exist "%SRC%\pom.xml" (
    echo %LOG% source\perms tidak ditemukan.
    pause & exit /b 1
)

set "JAVA_HOME=C:\reminecraft\jdk-25"
if not exist "%JAVA_HOME%\bin\java.exe" call "%ROOT%\_javadetect.bat"

echo %LOG% Building ReminecraftPerms...
cd /d "%SRC%"
call "%MVN%" package -DskipTests -q
if %errorlevel% neq 0 (
    echo %LOG% Build GAGAL.
    pause & exit /b 1
)

set "JAR=%SRC%\target\reminecraft-perms-1.0.0-SNAPSHOT.jar"
copy /Y "%JAR%" "%ROOT%\plugin\reminecraft-perms.jar" >nul
copy /Y "%JAR%" "%ROOT%\runfolder\plugins\reminecraft-perms.jar" >nul

echo %LOG% Deployed: reminecraft-perms.jar
echo.
echo ====================================================
echo %LOG% Done. Restart server untuk apply.
echo ====================================================
cd /d "%ROOT%"
pause
