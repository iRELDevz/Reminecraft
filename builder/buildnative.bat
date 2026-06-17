@echo off
title Reminecraft - Build Native DLL
setlocal
set "ROOT=%~dp0.."
set "LOG_BUILD=[ReMinecraft^|BUILD^|]"
set "LOG_NATIVE=[ReMinecraft^|NATIVE^|]"
set "LOG_ERROR=[ReMinecraft^|ERROR^|]"

set "SRC=%ROOT%\source\native"
set "BUILD_DIR=%SRC%\build"

if not exist "%SRC%\CMakeLists.txt" (
    echo %LOG_ERROR% source\native tidak ditemukan.
    pause & exit /b 1
)

echo ====================================================
echo          REMINE-CRAFT BUILD NATIVE DLL
echo ====================================================

where cmake >nul 2>&1
if %errorlevel% neq 0 (
    echo %LOG_ERROR% CMake tidak ditemukan di PATH.
    echo %LOG_ERROR% Install CMake dari https://cmake.org/download/
    pause & exit /b 1
)

call "%ROOT%\_javadetect.bat"
if %errorlevel% neq 0 ( pause & exit /b 1 )
set "JAVA_HOME=%JAVA_EXE:\bin\java.exe=%"
set "JAVA_HOME_FWD=%JAVA_HOME:\=/%"

echo %LOG_BUILD% Configuring CMake...
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
cmake -S "%SRC%" -B "%BUILD_DIR%" -DCMAKE_BUILD_TYPE=Release -DJAVA_HOME="%JAVA_HOME_FWD%"
if %errorlevel% neq 0 (
    echo %LOG_ERROR% CMake configure failed.
    pause & exit /b 1
)

echo %LOG_BUILD% Building reminecraft_native.dll...
cmake --build "%BUILD_DIR%" --config Release
if %errorlevel% neq 0 (
    echo %LOG_ERROR% CMake build failed.
    pause & exit /b 1
)

set "DLL_PATH=%BUILD_DIR%\Release\reminecraft_native.dll"
if not exist "%DLL_PATH%" set "DLL_PATH=%BUILD_DIR%\reminecraft_native.dll"

if not exist "%DLL_PATH%" (
    echo %LOG_ERROR% DLL tidak ditemukan setelah build.
    pause & exit /b 1
)

copy /Y "%DLL_PATH%" "%ROOT%\runfolder\reminecraft_native.dll" >nul
echo %LOG_NATIVE% Deployed: reminecraft_native.dll ke runfolder\

echo.
echo ====================================================
echo %LOG_BUILD% Native DLL updated. Restart server untuk apply.
echo ====================================================
pause
