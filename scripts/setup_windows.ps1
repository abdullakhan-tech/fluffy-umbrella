$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

$pythonCommand = $null
$pythonArgs = @()
if (Get-Command py -ErrorAction SilentlyContinue) {
    $pythonCommand = "py"
    $pythonArgs = @("-3")
} elseif (Get-Command python -ErrorAction SilentlyContinue) {
    $pythonCommand = "python"
} else {
    throw "Python 3.10+ is required. Install Python from python.org and enable PATH."
}

$versionOutput = & $pythonCommand @pythonArgs -c "import sys; print('%d.%d' % sys.version_info[:2])"
if ($LASTEXITCODE -ne 0) { throw "Unable to start Python." }
$version = [version]$versionOutput
if ($version -lt [version]"3.10") { throw "Python 3.10+ is required. Detected $version." }
Write-Host "Using Python $version"

if (-not (Test-Path ".venv")) {
    & $pythonCommand @pythonArgs -m venv .venv
    if ($LASTEXITCODE -ne 0) { throw "Could not create virtual environment." }
}
$venvPython = Join-Path (Get-Location) ".venv\Scripts\python.exe"
& $venvPython -m pip install --upgrade pip
if ($LASTEXITCODE -ne 0) { throw "pip upgrade failed." }
& $venvPython -m pip install -r backend\requirements.txt
if ($LASTEXITCODE -ne 0) { throw "Dependency installation failed." }
if (-not (Test-Path ".env")) {
    Copy-Item .env.example .env
    Write-Host "Created .env. Add at least one provider API key before AI chat."
}
& $venvPython -m compileall -q backend
if ($LASTEXITCODE -ne 0) { throw "Backend compilation failed." }
& $venvPython -m pytest -q
if ($LASTEXITCODE -ne 0) { throw "Tests failed. Fix errors before starting the app." }
Write-Host "Setup complete. Run scripts\run_windows.bat"
