@echo off
title Reminecraft - Build Plugin
setlocal
set "ROOT=%~dp0.."
set "LOG_BUILD=[ReMinecraft^|BUILD^|]"
set "LOG_PLUGIN=[ReMinecraft^|PLUGIN^|]"
set "LOG_ERROR=[ReMinecraft^|ERROR^|]"

set "PLUGIN_JAVA_HOME=C:\reminecraft\jdk-25"
if not exist "%PLUGIN_JAVA_HOME%\bin\java.exe" (
    call "%ROOT%\_javadetect.bat"
    set "PLUGIN_JAVA_HOME=%JAVA_EXE:\bin\java.exe=%"
)
set "MVN=%ROOT%\apache-maven-3.9.16\bin\mvn.cmd"

set "TARGET=%~1"
if "%TARGET%"=="" (
    echo Penggunaan: buildplugin.bat [authme^|fastlogin^|floodgate^|geyser]
    echo.
    echo   authme    - Build AuthMe dari source
    echo   fastlogin - Build FastLogin dari source
    echo   floodgate - Build Floodgate dari source
    echo   geyser    - Build Geyser dari source
    pause & exit /b 1
)

if /i "%TARGET%"=="authme" goto :build_authme
if /i "%TARGET%"=="fastlogin" goto :build_fastlogin
if /i "%TARGET%"=="floodgate" goto :build_floodgate
if /i "%TARGET%"=="geyser" goto :build_geyser
echo %LOG_ERROR% Plugin tidak dikenal: %TARGET%
pause & exit /b 1

:build_authme
echo %LOG_BUILD% Building AuthMe...
set "JAVA_HOME=%PLUGIN_JAVA_HOME%"
cd /d "%ROOT%\source\authme"
call "%MVN%" package -Pjava21-modules -pl authme-paper,authme-core,authme-paper-common -am -DskipTests -q
if %errorlevel% neq 0 ( echo %LOG_ERROR% AuthMe build failed. & cd /d "%ROOT%" & pause & exit /b 1 )
for /f "delims=" %%f in ('dir /b "authme-paper\target\AuthMe-*-Paper.jar" 2^>nul') do (
    copy /Y "authme-paper\target\%%f" "%ROOT%\plugin\AuthMe.jar" >nul
    copy /Y "authme-paper\target\%%f" "%ROOT%\runfolder\plugins\AuthMe.jar" >nul
)
echo %LOG_PLUGIN% AuthMe built and deployed.
goto :done

:build_fastlogin
echo %LOG_BUILD% Building FastLogin...
set "JAVA_HOME=%PLUGIN_JAVA_HOME%"
cd /d "%ROOT%\source\fastlogin"
call "%MVN%" package -pl bukkit -am -DskipTests -Dcheckstyle.skip -q
if %errorlevel% neq 0 ( echo %LOG_ERROR% FastLogin build failed. & cd /d "%ROOT%" & pause & exit /b 1 )
copy /Y "bukkit\target\FastLoginBukkit.jar" "%ROOT%\plugin\FastLogin.jar" >nul
copy /Y "bukkit\target\FastLoginBukkit.jar" "%ROOT%\runfolder\plugins\FastLogin.jar" >nul
echo %LOG_PLUGIN% FastLogin built and deployed.
goto :done

:build_floodgate
echo %LOG_BUILD% Building Floodgate...
cd /d "%ROOT%\source\floodgate"
call gradlew.bat :spigot:shadowJar --no-daemon -q
if %errorlevel% neq 0 ( echo %LOG_ERROR% Floodgate build failed. & cd /d "%ROOT%" & pause & exit /b 1 )
copy /Y "spigot\build\libs\floodgate-spigot.jar" "%ROOT%\plugin\floodgate-spigot.jar" >nul
copy /Y "spigot\build\libs\floodgate-spigot.jar" "%ROOT%\runfolder\plugins\floodgate-spigot.jar" >nul
echo %LOG_PLUGIN% Floodgate built and deployed.
goto :done

:build_geyser
echo %LOG_BUILD% Building Geyser...
cd /d "%ROOT%\source\geyser"
call gradlew.bat :bootstrap:spigot:shadowJar --no-daemon -q
if %errorlevel% neq 0 ( echo %LOG_ERROR% Geyser build failed. & cd /d "%ROOT%" & pause & exit /b 1 )
for /f "delims=" %%f in ('dir /b "bootstrap\spigot\build\libs\Geyser-Spigot*.jar" 2^>nul') do (
    copy /Y "bootstrap\spigot\build\libs\%%f" "%ROOT%\plugin\Geyser-Spigot.jar" >nul
    copy /Y "bootstrap\spigot\build\libs\%%f" "%ROOT%\runfolder\plugins\Geyser-Spigot.jar" >nul
)
echo %LOG_PLUGIN% Geyser built and deployed.
goto :done

:done
cd /d "%ROOT%"
echo.
echo ====================================================
echo %LOG_BUILD% Build selesai. Restart server untuk apply plugin baru.
echo ====================================================
pause
