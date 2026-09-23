@echo off
setlocal
rem FastNotify Destino - remove inicializacao com o Windows (duplo clique / Win10+)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0remover-inicializacao-destino.ps1" %*
set ERR=%ERRORLEVEL%
echo.
pause
exit /b %ERR%
