$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
$python = Get-Command py -ErrorAction SilentlyContinue
if ($python) { & py -3.11 --version; if ($LASTEXITCODE -ne 0) { & py -3 --version } } else { python --version }
if ($LASTEXITCODE -ne 0) { throw "Python 3.10+ is required. Install Python and retry." }
if (-not (Test-Path ".venv")) { if ($python) { & py -3 -m venv .venv } else { & python -m venv .venv } }
& .\.venv\Scripts\python.exe -m pip install --upgrade pip
& .\.venv\Scripts\python.exe -m pip install -r backend\requirements.txt
if (-not (Test-Path ".env")) { Copy-Item .env.example .env; Write-Host "Created .env. Add at least one provider API key before AI chat." }
& .\.venv\Scripts\python.exe -m compileall -q backend
if ($LASTEXITCODE -ne 0) { throw "Backend compilation failed." }
& .\.venv\Scripts\python.exe -m pytest -q
if ($LASTEXITCODE -ne 0) { throw "Tests failed. Fix errors before starting the app." }
Write-Host "Setup complete. Run scripts\run_windows.bat"
