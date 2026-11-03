@echo off
REM ============================================================================
REM  Build + cai dat + mo app Store Manager len may ao.
REM  KHONG can mo Android Studio. Can may ao dang chay (xem mo-may-ao.bat).
REM ============================================================================
setlocal

set "SDK=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%SDK%\platform-tools\adb.exe"
set "PACKAGE=com.example.store_manager"
set "ACTIVITY=%PACKAGE%/.ManHinhKhoiDong"

cd /d "%~dp0"

echo [1/4] Kiem tra may ao...
"%ADB%" wait-for-device
"%ADB%" shell getprop sys.boot_completed >nul 2>&1
:doi_boot
for /f "usebackq tokens=2 delims==" %%a in (`"%ADB%" shell getprop sys.boot_completed 2^>nul`) do (
    if "%%a"=="1" goto boot_xong
)
echo     May ao chua boot xong, cho them...
timeout /t 3 /nobreak >nul
goto doi_boot
:boot_xong
echo     May ao da boot xong.

echo [2/4] Build APK (gradlew installDebug)...
call gradlew.bat installDebug
if errorlevel 1 (
    echo [LOI] Build that bai.
    pause
    exit /b 1
)

echo [3/4] Kiem tra app da duoc cai...
"%ADB%" shell pm list packages | findstr /b /c:"package:%PACKAGE%" >nul
if errorlevel 1 (
    echo [LOI] App khong duoc cai dat. Hay chay lai len tren.
    pause
    exit /b 1
)

echo [4/4] Mo app...
"%ADB%" shell am start -n %ACTIVITY% >nul
echo.
echo Xong! App da duoc mo tren may ao.
pause

endlocal
