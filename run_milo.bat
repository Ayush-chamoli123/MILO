@echo off
setlocal enabledelayedexpansion
title MILO Development Runner

echo ================================================================
echo             MILO - Integrated Full-Stack Runner
echo ================================================================
echo.

set "PROJECT_ROOT=%~dp0"
set "BACKEND_DIR=%PROJECT_ROOT%backend"
set "SDK_DIR=C:\Users\kartikey\AppData\Local\Android\Sdk"
set "ADB=%SDK_DIR%\platform-tools\adb.exe"
set "EMULATOR=%SDK_DIR%\emulator\emulator.exe"

:: 1. Check & Start FastAPI Backend
echo [1/3] Checking FastAPI Backend...
curl -s -m 2 http://127.0.0.1:8000/api/health >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo   [OK] FastAPI backend is already running on http://127.0.0.1:8000.
) else (
    echo   [INFO] Starting FastAPI backend in a separate window...
    start "MILO FastAPI Backend" "%BACKEND_DIR%\start_backend.bat"
    
    :: Wait up to 12s for backend to become healthy
    echo   [INFO] Waiting for backend to become ready...
    set /a WAIT_COUNT=0
    :WAIT_BACKEND
    timeout /t 1 /nobreak >nul
    curl -s -m 2 http://127.0.0.1:8000/api/health >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo   [OK] FastAPI backend is ready!
    ) else (
        set /a WAIT_COUNT+=1
        if !WAIT_COUNT! LSS 12 (
            goto :WAIT_BACKEND
        ) else (
            echo   [WARN] Backend started, but health check is taking longer. Continuing...
        )
    )
)

:: 2. Check Android Device / Emulator
echo.
echo [2/3] Checking Android Device / Emulator...
if exist "%ADB%" (
    set "DEVICE_FOUND=0"
    for /f "skip=1 tokens=1,2" %%A in ('"%ADB%" devices') do (
        if "%%B"=="device" (
            set "DEVICE_FOUND=1"
            echo   [OK] Found active Android target: %%A
        )
    )
    if !DEVICE_FOUND! EQU 0 (
        echo   [INFO] No running device or emulator detected.
        if exist "%EMULATOR%" (
            echo   [INFO] Launching Pixel_9a emulator...
            start "" "%EMULATOR%" -avd Pixel_9a
            echo   [INFO] Waiting for emulator to boot...
            "%ADB%" wait-for-device
            echo   [OK] Emulator connected!
        )
    )
) else (
    echo   [INFO] Android SDK adb not found at default path. Skipping emulator auto-launch.
)

:: 3. Build & Install Android App
echo.
echo [3/3] Building and Installing Android App...
call "%PROJECT_ROOT%gradlew.bat" installDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ================================================================
    echo   BUILD SUCCESSFUL!
    if exist "%ADB%" (
        echo   Launching MILO on emulator...
        "%ADB%" shell am start -n com.example.milo_2/.MainActivity
    )
    echo ================================================================
    echo   Backend:  http://127.0.0.1:8000
    echo   Swagger:  http://127.0.0.1:8000/docs
    echo   Emulator: http://10.0.2.2:8000
    echo ================================================================
) else (
    echo.
    echo [ERROR] Build or installation failed. Check Gradle output above.
)

pause
