# PowerShell launcher for MILO FastAPI Backend
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location "$scriptDir\backend"

$pythonExec = if (Test-Path ".\.venv\Scripts\python.exe") {
    ".\.venv\Scripts\python.exe"
} elseif (Test-Path "C:\Users\kartikey\anaconda3\python.exe") {
    "C:\Users\kartikey\anaconda3\python.exe"
} else {
    "python"
}

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "          Starting MILO FastAPI Backend" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Python Executable: $pythonExec"
Write-Host "Host: 0.0.0.0 (Port: 8000)"
Write-Host "  - Host: http://127.0.0.1:8000"
Write-Host "  - Android Emulator: http://10.0.2.2:8000"
Write-Host "  - Swagger Docs: http://127.0.0.1:8000/docs"
Write-Host "===================================================" -ForegroundColor Cyan

& $pythonExec -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
