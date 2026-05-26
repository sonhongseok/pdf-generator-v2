@echo off
title OP Certificate Generator - Debug Mode
echo ===================================================
echo   Starting OP Certificate Generator (DEBUG)...
echo ===================================================

:: [사전 정리] 이전 세션의 잔류 WINWORD 프로세스 종료 (복구 다이얼로그 방지)
taskkill /F /IM WINWORD.EXE /T >nul 2>&1

:: [사전 정리] Word 비정상 종료 복구 다이얼로그 레지스트리 초기화
:: 전원이 갑자기 꺼졌을 때도 다음 실행 시 복구 팝업이 뜨지 않게 함
reg delete "HKCU\Software\Microsoft\Office\16.0\Word\Resiliency" /f >nul 2>&1
reg delete "HKCU\Software\Microsoft\Office\15.0\Word\Resiliency" /f >nul 2>&1

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
