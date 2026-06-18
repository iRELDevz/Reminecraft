@echo off
title Reminecraft - Localhost
setlocal
set "LOG_INIT=[ReMinecraft^|INIT^|]"
set "LOG_JAVA=[ReMinecraft^|JAVA^|]"
set "LOG_ERROR=[ReMinecraft^|ERROR^|]"

echo ====================================================
echo           REMINE-CRAFT LOCALHOST SERVER
echo ====================================================

if not exist "runfolder\reminecraft-server.jar" (
    echo %LOG_ERROR% Server JAR tidak ditemukan. Jalankan builder\setuper.bat dulu.
    pause & exit /b 1
)

call _javadetect.bat
if %errorlevel% neq 0 ( pause & exit /b 1 )

(echo #https://aka.ms/MinecraftEULA & echo eula=true) > "runfolder\eula.txt"

del /f /q "runfolder\plugins\AuthMe.jar"      >nul 2>&1
del /f /q "runfolder\plugins\FastLogin.jar"   >nul 2>&1
del /f /q "runfolder\plugins\ProtocolLib.jar" >nul 2>&1

powershell -NoProfile -WindowStyle Hidden -Command "Start-Process powershell -ArgumentList '-NoProfile -WindowStyle Hidden -File C:\reminecraft\resourcepack\packserver.ps1' -WindowStyle Hidden"

echo %LOG_JAVA% Starting localhost (2G RAM)...
cd runfolder
%JAVA_EXE% -Xms256M -Xmx2G -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ReservedCodeCacheSize=64m -XX:MaxMetaspaceSize=256m -Dreminecraft.localhost=true -jar reminecraft-server.jar nogui
cd ..
