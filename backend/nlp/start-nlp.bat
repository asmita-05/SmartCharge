@echo off
setlocal
cd /d "%~dp0"
if not exist ".venv\Scripts\python.exe" (
    echo Local Python environment is missing. Run setup-nlp.ps1 first.
    pause
    exit /b 1
)
if not exist "model\config.json" (
    echo The NLP model is missing. Run download-model.ps1 from this folder first.
    pause
    exit /b 1
)
".venv\Scripts\python.exe" emergency_classifier.py
if errorlevel 1 (
    echo Local NLP service stopped. Check the error above.
    pause
)
