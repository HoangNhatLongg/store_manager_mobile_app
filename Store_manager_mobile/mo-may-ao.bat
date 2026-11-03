@echo off
REM ============================================================================
REM  Mo may ao Android ma KHONG can mo Android Studio.
REM  Dung AVD: Pixel_6
REM ============================================================================
setlocal

set "SDK=%LOCALAPPDATA%\Android\Sdk"
set "EMULATOR=%SDK%\emulator\emulator.exe"
set "AVD=Pixel_6"

if not exist "%EMULATOR%" (
    echo [LOI] Khong tim thay emulator tai: %EMULATOR%
    echo Hay cai dat Android SDK Emulator hoac sua duong dan trong file nay.
    pause
    exit /b 1
)

echo Dang mo may ao "%AVD%"...
echo (Dong cua so nay khong tat may ao. Dung taskkill de tat: taskkill /IM emulator.exe /F)
echo.

start "Android Emulator - %AVD%" "%EMULATOR%" -avd %AVD%
if errorlevel 1 (
    echo [LOI] Khong khoi dong duoc may ao.
    pause
    exit /b 1
)

endlocal
