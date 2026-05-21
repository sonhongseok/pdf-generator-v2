@echo off
title OP Certificate Generator - Debug Mode
echo ===================================================
echo   Starting OP Certificate Generator (DEBUG)...
echo ===================================================

echo Checking port 8080...
netstat -ano | findstr :8080 >nul
if %errorlevel% neq 0 (
    echo [1/3] Backend is NOT running. Starting Spring Boot...
    start /min "OP Backend" cmd /c "cd /d C:\work\pdf-generator\backend && mvnw.cmd spring-boot:run"
    timeout /t 6 >nul
) else (
    echo [1/3] Backend is already running.
)

echo Checking port 5173...
netstat -ano | findstr :5173 >nul
if %errorlevel% neq 0 (
    echo [2/3] Frontend is NOT running. Starting Vite...
    start /min "OP Frontend" cmd /c "cd /d C:\work\pdf-generator\frontend && npm.cmd run dev"
    timeout /t 3 >nul
) else (
    echo [2/3] Frontend is already running.
)

echo [3/3] Detecting Chrome installation path...

if exist "C:\Program Files\Google\Chrome\Application\chrome.exe" (
    echo Found Chrome at Program Files. Launching app window...
    start "" "C:\Program Files\Google\Chrome\Application\chrome.exe" --app="http://localhost:5173"
    goto end
)

if exist "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" (
    echo Found Chrome at Program Files x86. Launching app window...
    start "" "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" --app="http://localhost:5173"
    goto end
)

if exist "%USERPROFILE%\AppData\Local\Google\Chrome\Application\chrome.exe" (
    echo Found Chrome at Local AppData. Launching app window...
    start "" "%USERPROFILE%\AppData\Local\Google\Chrome\Application\chrome.exe" --app="http://localhost:5173"
    goto end
)

echo Warning: Chrome not found in standard paths. Trying default command...
start chrome.exe --app="http://localhost:5173"

:end
echo ===================================================
echo   Launch completed.
echo ===================================================
pause
