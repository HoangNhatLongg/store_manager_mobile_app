@echo off
REM ============================================================================
REM  Chay web server Next.js cho API (can khi app ket noi may chu).
REM  App Android dung DIA_CHI_API = http://10.0.2.2:3000/ (alias may host).
REM ============================================================================
setlocal

cd /d "%~dp0..\store-manager_web"

if not exist "node_modules" (
    echo [LOI] Chua cai node_modules. Hay chay: npm install
    pause
    exit /b 1
)

echo Dang khoi dong Next.js tai http://localhost:3000 ...
echo Nhan Ctrl+C de dung.
echo.

call npm run dev

endlocal
