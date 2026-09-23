@echo off
setlocal
rem FastNotify Origem - inicializacao com o Windows (duplo clique / Win10+)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0instalar-inicializacao-origem.ps1" %*
set ERR=%ERRORLEVEL%
echo.
pause
exit /b %ERR%
