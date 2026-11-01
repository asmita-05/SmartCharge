$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

python -c "import sys; raise SystemExit(0 if sys.version_info >= (3, 10) else 'Python 3.10 or newer is required')"
if ($LASTEXITCODE -ne 0) { throw "Install 64-bit Python 3.11 and make sure the 'python' command is available, then retry." }

if (-not (Test-Path ".venv\Scripts\python.exe")) {
    python -m venv .venv
    if ($LASTEXITCODE -ne 0) { throw "Could not create the local Python environment." }
}

$Python = Join-Path $PSScriptRoot ".venv\Scripts\python.exe"
& $Python -m pip install --upgrade pip
if ($LASTEXITCODE -ne 0) { throw "Could not update pip." }
& $Python -m pip install --index-url https://download.pytorch.org/whl/cpu torch
if ($LASTEXITCODE -ne 0) { throw "Could not install CPU-only PyTorch." }
& $Python -m pip install -r requirements.txt
if ($LASTEXITCODE -ne 0) { throw "Could not install the NLP service requirements." }

Write-Host "Python environment ready. Next run .\download-model.ps1"
