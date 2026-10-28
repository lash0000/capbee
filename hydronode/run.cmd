@echo off
setlocal
rem Compile and run Hydronode on Windows (cmd.exe). No build system needed.
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo [!] javac not found. Install JDK 17+ and add it to PATH.
    exit /b 1
)

rem UTF-8 so the box drawing and spinner glyphs render correctly.
chcp 65001 >nul

if exist out rmdir /s /q out
mkdir out

set "SOURCES=%TEMP%\hydronode-sources.txt"
if exist "%SOURCES%" del "%SOURCES%" >nul 2>nul

rem One quoted path per line: javac reads this as an argument file, so spaces
rem in the project path are safe.
for /r "src" %%f in (*.java) do echo "%%f">>"%SOURCES%"

javac -Xlint:all -d out @"%SOURCES%"
if errorlevel 1 (
    del "%SOURCES%" >nul 2>nul
    exit /b 1
)

java -Dstdout.encoding=UTF-8 -cp out com.hydronode.Main %*
set "EXIT_CODE=%ERRORLEVEL%"

del "%SOURCES%" >nul 2>nul
exit /b %EXIT_CODE%
