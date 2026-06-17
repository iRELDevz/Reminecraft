@echo off
set "LOG_INIT=[ReMinecraft^|INIT^|]"
set "LOG_ERROR=[ReMinecraft^|ERROR^|]"
set JAVA_EXE=java
set JAVA_MAJOR=0

java -version > "%TEMP%\rmjver.txt" 2>&1
if %errorlevel% neq 0 goto :rmjdet_search

for /f "usebackq tokens=3 delims= " %%v in ("%TEMP%\rmjver.txt") do (
    set "JVERAW=%%v"
    goto :rmjdet_strip
)

:rmjdet_strip
set "JVERAW=%JVERAW:"=%"
for /f "tokens=1 delims=." %%m in ("%JVERAW%") do set "JAVA_MAJOR=%%m"
del "%TEMP%\rmjver.txt" >nul 2>&1
if %JAVA_MAJOR% GEQ 25 goto :rmjdet_done
echo %LOG_INIT% PATH Java is %JAVA_MAJOR%, needs 25+. Searching...

:rmjdet_search
del "%TEMP%\rmjver.txt" >nul 2>&1
if exist "c:\reminecraft\jdk-25\bin\java.exe" (
    set "JAVA_EXE=c:\reminecraft\jdk-25\bin\java.exe"
    goto :rmjdet_done
)
if exist "C:\Users\%USERNAME%\.antigravity\extensions\redhat.java-1.54.0-win32-x64\jre\21.0.10-win32-x86_64\bin\java.exe" (
    set "JAVA_EXE=C:\Users\%USERNAME%\.antigravity\extensions\redhat.java-1.54.0-win32-x64\jre\21.0.10-win32-x86_64\bin\java.exe"
    goto :rmjdet_done
)
for /d %%d in ("C:\Program Files\Java\jdk-2*") do (
    if exist "%%d\bin\java.exe" ( set "JAVA_EXE=%%d\bin\java.exe" & goto :rmjdet_done )
)
for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-2*") do (
    if exist "%%d\bin\java.exe" ( set "JAVA_EXE=%%d\bin\java.exe" & goto :rmjdet_done )
)
for /d %%d in ("C:\Program Files\Microsoft\jdk-2*") do (
    if exist "%%d\bin\java.exe" ( set "JAVA_EXE=%%d\bin\java.exe" & goto :rmjdet_done )
)
for /d %%d in ("C:\Program Files\BellSoft\LibericaJDK-2*") do (
    if exist "%%d\bin\java.exe" ( set "JAVA_EXE=%%d\bin\java.exe" & goto :rmjdet_done )
)
for /d %%d in ("C:\Program Files\Zulu\zulu-2*") do (
    if exist "%%d\bin\java.exe" ( set "JAVA_EXE=%%d\bin\java.exe" & goto :rmjdet_done )
)
for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\JavaSoft\JDK" /s /v JavaHome 2^>nul ^| findstr "JavaHome"') do (
    if exist "%%b\bin\java.exe" ( set "JAVA_EXE=%%b\bin\java.exe" & goto :rmjdet_done )
)

echo %LOG_ERROR% Java 25+ not found. Please install JDK 25 or run buildfirstrun.bat.
exit /b 1

:rmjdet_done
