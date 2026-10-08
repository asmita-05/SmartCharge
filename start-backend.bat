@echo off
setlocal
cd /d "%~dp0backend"
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Duser.timezone=Asia/Kolkata"
if errorlevel 1 pause
