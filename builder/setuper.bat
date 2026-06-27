@echo off
title Reminecraft - Setup
setlocal enabledelayedexpansion
set "ROOT=%~dp0.."
set "LOG=[ReMinecraft^|SETUP^|]"
set "ERR=[ReMinecraft^|ERROR^|]"

echo ====================================================
echo              REMINE-CRAFT SETUP
echo ====================================================

REM ── 1. JDK ──────────────────────────────────────────
if not exist "%ROOT%\jdk-25\bin\java.exe" (
    echo %LOG% JDK 25 tidak ditemukan. Mengunduh dari Eclipse Adoptium...
    set "JDK_ZIP=%TEMP%\jdk25.zip"
    powershell -NoProfile -Command ^
        "Invoke-WebRequest 'https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile '!JDK_ZIP!'"
    if %errorlevel% neq 0 (
        echo %ERR% Gagal unduh JDK. Pastikan ada koneksi internet.
        pause & exit /b 1
    )
    echo %LOG% Mengekstrak JDK...
    powershell -NoProfile -Command ^
        "Expand-Archive -Path '!JDK_ZIP!' -DestinationPath '%ROOT%\jdk-tmp' -Force"
    for /d %%d in ("%ROOT%\jdk-tmp\jdk-25*") do (
        move "%%d" "%ROOT%\jdk-25" >nul
    )
    rd /s /q "%ROOT%\jdk-tmp" >nul 2>&1
    del "!JDK_ZIP!" >nul 2>&1
    echo %LOG% JDK 25 siap.
) else (
    echo %LOG% JDK 25 OK.
)

REM ── 2. Maven ────────────────────────────────────────
if not exist "%ROOT%\apache-maven-3.9.16\bin\mvn.cmd" (
    echo %LOG% Maven tidak ditemukan. Mengunduh...
    set "MVN_ZIP=%TEMP%\maven.zip"
    powershell -NoProfile -Command ^
        "Invoke-WebRequest 'https://downloads.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip' -OutFile '!MVN_ZIP!'"
    if %errorlevel% neq 0 (
        echo %ERR% Gagal unduh Maven.
        pause & exit /b 1
    )
    echo %LOG% Mengekstrak Maven...
    powershell -NoProfile -Command ^
        "Expand-Archive -Path '!MVN_ZIP!' -DestinationPath '%ROOT%' -Force"
    del "!MVN_ZIP!" >nul 2>&1
    echo %LOG% Maven siap.
) else (
    echo %LOG% Maven OK.
)

set "JAVA_HOME=%ROOT%\jdk-25"
set "JAVA_EXE=%ROOT%\jdk-25\bin\java.exe"
set "MVN=%ROOT%\apache-maven-3.9.16\bin\mvn.cmd"

REM ── 3. Server JAR ───────────────────────────────────
if not exist "%ROOT%\runfolder\reminecraft-server.jar" (
    echo %LOG% Server JAR tidak ditemukan. Mengunduh Purpur...
    if not exist "%ROOT%\runfolder" mkdir "%ROOT%\runfolder"
    powershell -NoProfile -Command ^
        "Invoke-WebRequest 'https://api.purpurmc.org/v2/purpur/26.1.2/latest/download' -OutFile '%ROOT%\runfolder\reminecraft-server.jar'"
    if %errorlevel% neq 0 (
        echo %ERR% Gagal unduh server JAR dari Purpur.
        echo %ERR% Download manual dari https://purpurmc.org/downloads
        echo %ERR% Simpan sebagai: runfolder\reminecraft-server.jar
        pause & exit /b 1
    )
    echo %LOG% Server JAR siap.
) else (
    echo %LOG% Server JAR OK.
)

REM ── 4. Folders ──────────────────────────────────────
if not exist "%ROOT%\runfolder\plugins" mkdir "%ROOT%\runfolder\plugins"
if not exist "%ROOT%\scripts"           mkdir "%ROOT%\scripts"

REM ── 5. Build custom plugins ─────────────────────────
echo %LOG% Build ReminecraftCore...
cd /d "%ROOT%\core"
call "%MVN%" package -DskipTests -q
if %errorlevel% neq 0 ( echo %ERR% Build core gagal. & pause & exit /b 1 )
for %%f in ("%ROOT%\core\target\reminecraft-core-*.jar") do (
    copy /Y "%%f" "%ROOT%\runfolder\plugins\reminecraft-core.jar" >nul
)

echo %LOG% Build ReminecraftPerms...
cd /d "%ROOT%\source\perms"
call "%MVN%" package -DskipTests -q
if %errorlevel% neq 0 ( echo %ERR% Build perms gagal. & pause & exit /b 1 )
for %%f in ("%ROOT%\source\perms\target\reminecraft-perms-*.jar") do (
    copy /Y "%%f" "%ROOT%\runfolder\plugins\reminecraft-perms.jar" >nul
)

echo %LOG% Build ReminecraftAuth...
cd /d "%ROOT%\source\auth"
call "%MVN%" package -DskipTests -q
if %errorlevel% neq 0 ( echo %ERR% Build auth gagal. & pause & exit /b 1 )
for %%f in ("%ROOT%\source\auth\target\reminecraft-auth-*.jar") do (
    copy /Y "%%f" "%ROOT%\runfolder\plugins\reminecraft-auth.jar" >nul
)

echo %LOG% Build ReminecraftDevmode...
cd /d "%ROOT%\source\devmode"
call "%MVN%" package -DskipTests -q
if %errorlevel% neq 0 ( echo %ERR% Build devmode gagal. & pause & exit /b 1 )
for %%f in ("%ROOT%\source\devmode\target\reminecraft-devmode-*.jar") do (
    copy /Y "%%f" "%ROOT%\runfolder\plugins\reminecraft-devmode.jar" >nul
)

echo %LOG% Build ReminecraftGPU...
cd /d "%ROOT%\source\gpu"
call "%MVN%" package -DskipTests -q
if %errorlevel% neq 0 ( echo %ERR% Build gpu gagal. & pause & exit /b 1 )
for %%f in ("%ROOT%\source\gpu\target\reminecraft-gpu-*.jar") do (
    copy /Y "%%f" "%ROOT%\runfolder\plugins\reminecraft-gpu.jar" >nul
)

REM ── 7. EULA ─────────────────────────────────────────
echo #https://aka.ms/MinecraftEULA> "%ROOT%\runfolder\eula.txt"
echo eula=true>> "%ROOT%\runfolder\eula.txt"

REM ── 8. server.properties ────────────────────────────
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
    echo %LOG% server.properties dibuat.
)

REM ── 9. config.json ──────────────────────────────────
if not exist "%ROOT%\config.json" (
    (
        echo {
        echo   "server": {
        echo     "java-ip": "0.0.0.0",
        echo     "java-port": 25565,
        echo     "bedrock-ip": "0.0.0.0",
        echo     "bedrock-port": 19132,
        echo     "max-players": 100,
        echo     "motd": "§bReminecraft §7- §aHybrid Java + Bedrock"
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
        echo   }
        echo }
    ) > "%ROOT%\config.json"
    echo %LOG% config.json dibuat.
)

REM ── 10. Bun (optional) ──────────────────────────────
where bun >nul 2>&1
if %errorlevel% equ 0 (
    echo %LOG% Bun ditemukan, install dependencies...
    cd /d "%ROOT%\source\bun"
    call bun install --silent
    echo %LOG% Bun OK.
) else (
    echo %LOG% Bun tidak terinstall ^(opsional^). Scripting engine dinonaktifkan.
)

cd /d "%ROOT%"
echo.
echo ====================================================
echo %LOG% Setup selesai!
echo %LOG% Jalankan: localhost.bat
echo ====================================================
pause
