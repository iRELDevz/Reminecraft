@echo off
title Reminecraft - Setup
setlocal
set "ROOT=%~dp0.."
set "LOG_INIT=[ReMinecraft^|INIT^|]"
set "LOG_ERROR=[ReMinecraft^|ERROR^|]"

echo ====================================================
echo              REMINE-CRAFT SETUP
echo ====================================================

call "%ROOT%\_javadetect.bat"
if %errorlevel% neq 0 ( pause & exit /b 1 )
echo %LOG_INIT% Java OK: %JAVA_EXE%

if not exist "%ROOT%\runfolder"              mkdir "%ROOT%\runfolder"
if not exist "%ROOT%\runfolder\plugins"      mkdir "%ROOT%\runfolder\plugins"
if not exist "%ROOT%\scripts"                mkdir "%ROOT%\scripts"
echo %LOG_INIT% Folders ready.

if exist "%ROOT%\plugin" (
    for %%f in ("%ROOT%\plugin\*.jar") do (
        copy /Y "%%f" "%ROOT%\runfolder\plugins\" >nul
        echo %LOG_INIT% Plugin: %%~nxf
    )
) else (
    echo %LOG_ERROR% plugin\ folder not found. Pastikan pre-built JARs ada di plugin\.
    pause & exit /b 1
)

if exist "%ROOT%\reminecraft-server.jar" (
    copy /Y "%ROOT%\reminecraft-server.jar" "%ROOT%\runfolder\" >nul
    echo %LOG_INIT% Server JAR copied.
) else if not exist "%ROOT%\runfolder\reminecraft-server.jar" (
    echo %LOG_ERROR% reminecraft-server.jar tidak ditemukan.
    echo %LOG_ERROR% Letakkan reminecraft-server.jar di root folder, atau jalankan builder\buildserver.bat dulu.
    pause & exit /b 1
) else (
    echo %LOG_INIT% Server JAR already in runfolder.
)

(
    echo #https://aka.ms/MinecraftEULA
    echo eula=true
) > "%ROOT%\runfolder\eula.txt"

if not exist "%ROOT%\runfolder\server.properties" (
    (
        echo server-ip=0.0.0.0
        echo server-port=25565
        echo max-players=100
        echo online-mode=false
        echo enable-command-block=true
        echo spawn-protection=0
        echo view-distance=10
        echo simulation-distance=8
        echo level-name=world
        echo level-type=minecraft\:normal
        echo gamemode=survival
        echo difficulty=normal
        echo allow-nether=true
        echo pvp=true
        echo white-list=false
        echo enforce-whitelist=false
        echo motd=Reminecraft - Java + Bedrock
    ) > "%ROOT%\runfolder\server.properties"
    echo %LOG_INIT% server.properties created.
)

if not exist "%ROOT%\config.json" (
    (
        echo {
        echo   "server": {
        echo     "java-ip": "0.0.0.0",
        echo     "java-port": 25565,
        echo     "bedrock-ip": "0.0.0.0",
        echo     "bedrock-port": 19132,
        echo     "max-players": 100,
        echo     "motd": "§bReminecraft Hybrid Server §7- §aReady"
        echo   },
        echo   "database": {
        echo     "provider": "none",
        echo     "host": "localhost",
        echo     "port": 3306,
        echo     "name": "reminecraft",
        echo     "username": "root",
        echo     "password": "",
        echo     "pool-size": 10,
        echo     "connection-timeout-ms": 30000
        echo   },
        echo   "features": {
        echo     "enable-native-compression": false,
        echo     "enable-bun-scripting": false,
        echo     "bedrock-prefix": "."
        echo   },
        echo   "shop": {
        echo     "enable-shop": true,
        echo     "shop-items": []
        echo   }
        echo }
    ) > "%ROOT%\config.json"
    echo %LOG_INIT% config.json created ^(template^).
)

echo.
echo ====================================================
echo %LOG_INIT% Setup selesai. Jalankan localhost.bat atau runner.bat.
echo ====================================================
pause
