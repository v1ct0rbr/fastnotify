@echo off
setlocal
rem FastNotify Destino - inicializacao com o Windows (duplo clique / Win10+)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0instalar-inicializacao-destino.ps1" %*
set ERR=%ERRORLEVEL%
echo.
pause
exit /b %ERR%
