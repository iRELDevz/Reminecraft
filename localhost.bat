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

if not exist "runfolder\plugins" mkdir "runfolder\plugins"
for %%f in (plugin\*.jar) do (
    copy /Y "%%f" "runfolder\plugins\" >nul
    echo %LOG_INIT% Plugin: %%~nxf
)

(echo #https://aka.ms/MinecraftEULA & echo eula=true) > "runfolder\eula.txt"

echo %LOG_JAVA% Starting localhost (4G RAM)...
cd runfolder
%JAVA_EXE% -Xms3G -Xmx4G -XX:+UseZGC -Dreminecraft.localhost=true -jar reminecraft-server.jar nogui
cd ..
