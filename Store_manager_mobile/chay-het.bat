@echo off
REM ============================================================================
REM  Mo may ao, build + chay app, va bat web server API (theo thu tu).
REM  KHONG can mo Android Studio.
REM ============================================================================
setlocal

echo.
echo === Buoc 1/3: Bat web server API (cua so rieng) ===
start "Store Manager - Web API" cmd /k call "%~dp0chay-web-server.bat"

echo.
echo === Buoc 2/3: Mo may ao Android ===
call "%~dp0mo-may-ao.bat"
echo     May ao dang khoi dong, cho it nhat 1-2 phut...

echo.
echo === Buoc 3/3: Build + chay app ===
call "%~dp0build-va-chay-app.bat"

endlocal
