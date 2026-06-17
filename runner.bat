@echo off
title Reminecraft - Production
setlocal
set "LOG_INIT=[ReMinecraft^|INIT^|]"
set "LOG_JAVA=[ReMinecraft^|JAVA^|]"
set "LOG_ERROR=[ReMinecraft^|ERROR^|]"

echo ====================================================
echo           REMINE-CRAFT PRODUCTION SERVER
echo ====================================================

if not exist "runfolder\reminecraft-server.jar" (
    echo %LOG_ERROR% Server JAR tidak ditemukan. Jalankan builder\setuper.bat dulu.
    pause & exit /b 1
)

call _javadetect.bat
if %errorlevel% neq 0 ( pause & exit /b 1 )

if not exist "runfolder\plugins" mkdir "runfolder\plugins"
for %%f in (plugin\*.jar) do (
    copy /Y "%%f" "runfolder\plugins\" >nul
    echo %LOG_INIT% Plugin: %%~nxf
)

(echo #https://aka.ms/MinecraftEULA & echo eula=true) > "runfolder\eula.txt"

echo %LOG_JAVA% Starting production (4G RAM, G1GC)...
cd runfolder
%JAVA_EXE% -Xms2G -Xmx4G -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1ReservePercent=15 -XX:G1HeapRegionSize=32m -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:G1OldCSetRegionThresholdPercent=5 -XX:SurvivorRatio=32 -XX:MaxTenuringThreshold=1 -Dterminal.jline=false -Dterminal.ansi=true -jar reminecraft-server.jar nogui
cd ..
