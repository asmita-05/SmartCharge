@echo off
setlocal
title SmartCharge Backend
cd /d "%~dp0backend"
if not defined ORS_API_KEY if exist "%~dp0.ors-key" set /p ORS_API_KEY=<"%~dp0.ors-key"
if not defined BREVO_API_KEY if exist "%~dp0.brevo-key" set /p BREVO_API_KEY=<"%~dp0.brevo-key"
if not defined BREVO_SENDER_EMAIL if exist "%~dp0.brevo-sender" set /p BREVO_SENDER_EMAIL=<"%~dp0.brevo-sender"
if not defined SMARTCHARGE_JWT_SECRET if exist "%~dp0.jwt-secret" set /p SMARTCHARGE_JWT_SECRET=<"%~dp0.jwt-secret"
if not defined ORS_API_KEY (
    echo ORS_API_KEY is not set. The backend will start, but road routing will be unavailable.
    echo Add your key to the ignored .ors-key file in the project folder.
)
if not defined BREVO_API_KEY echo BREVO_API_KEY is not set. Email verification will be unavailable until Brevo is configured.
if not defined BREVO_SENDER_EMAIL echo BREVO_SENDER_EMAIL is not set. Set it to your verified Brevo sender address.
echo Checking PostgreSQL on localhost:5432...
powershell -NoProfile -Command "if (Test-NetConnection -ComputerName localhost -Port 5432 -InformationLevel Quiet) { exit 0 } else { exit 1 }"
if errorlevel 1 (
    echo.
    echo PostgreSQL is not running or is not listening on localhost:5432.
    echo Start PostgreSQL and make sure the smartcharge database is available, then run this file again.
    echo.
    pause
    exit /b 1
)
echo Checking that port 8080 is available for this SmartCharge backend...
powershell -NoProfile -Command "if (Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue) { exit 1 } else { exit 0 }"
if errorlevel 1 (
    echo Port 8080 is already in use. Another backend may still be running.
    echo Close the old SmartCharge Backend window before starting this project.
    echo.
    pause
    exit /b 1
)
echo Checking local emergency NLP service at 127.0.0.1:8090...
powershell -NoProfile -Command "$listener = Get-NetTCPConnection -State Listen -LocalPort 8090 -ErrorAction SilentlyContinue; if (-not $listener) { exit 1 }; try { $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8090/health' -TimeoutSec 3 } catch { exit 2 }; if ($health.status -eq 'ready' -and $health.protocol -eq 'smartcharge-priority-policy-v2') { exit 0 } else { exit 2 }"
if errorlevel 2 (
    echo An incompatible or not-ready NLP service already owns port 8090.
    echo Close that old NLP window and run start-backend.bat again.
    echo The backend was not started to prevent sending requests to a stale service.
    echo.
    pause
    exit /b 1
)
if errorlevel 1 (
    echo Starting the local emergency NLP service in a separate window...
    start "SmartCharge NLP" "%~dp0backend\nlp\start-nlp.bat"
    echo Waiting for the model to load and the NLP service to become ready...
    powershell -NoProfile -Command "$deadline = (Get-Date).AddMinutes(3); do { try { $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8090/health' -TimeoutSec 3; if ($health.status -eq 'ready' -and $health.protocol -eq 'smartcharge-priority-policy-v2') { exit 0 } } catch {}; Start-Sleep -Seconds 2 } while ((Get-Date) -lt $deadline); exit 1"
    if errorlevel 1 (
        echo The NLP service did not become ready within 3 minutes.
        echo Check the SmartCharge NLP window for startup errors, then run this file again.
        echo The backend was not started.
        echo.
        pause
        exit /b 1
    )
)
echo Starting SmartCharge backend and web interface...
echo Rebuilding backend from current project source...
echo Your browser will open http://localhost:8080/ when the backend is ready.
echo.
start "" /b powershell.exe -WindowStyle Hidden -NoProfile -ExecutionPolicy Bypass -File "%~dp0open-frontend.ps1"
mvn clean spring-boot:run -Dspring-boot.run.jvmArguments="-Duser.timezone=Asia/Kolkata"
if errorlevel 1 (
    echo.
    echo Backend startup failed. Review the error above.
)
echo.
pause
