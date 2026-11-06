@echo off
title MILO FastAPI Backend
echo ===================================================
echo           Starting MILO FastAPI Backend
echo ===================================================
cd /d "%~dp0"

:: Check if virtual environment exists
if exist ".venv\Scripts\python.exe" (
    set "PYTHON_EXEC=.venv\Scripts\python.exe"
) else if exist "C:\Users\kartikey\anaconda3\python.exe" (
    set "PYTHON_EXEC=C:\Users\kartikey\anaconda3\python.exe"
) else (
    set "PYTHON_EXEC=python"
)

echo Using Python: %PYTHON_EXEC%
echo Host: 0.0.0.0 (Port: 8000)
echo Accessible via:
echo   - Host: http://127.0.0.1:8000
echo   - Android Emulator: http://10.0.2.2:8000
echo   - Swagger Docs: http://127.0.0.1:8000/docs
echo ===================================================

"%PYTHON_EXEC%" -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Backend process terminated with exit code %ERRORLEVEL%.
    pause
)
