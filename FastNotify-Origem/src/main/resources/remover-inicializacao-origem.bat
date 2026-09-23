@echo off
setlocal
rem FastNotify Origem - remove inicializacao com o Windows (duplo clique / Win10+)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0remover-inicializacao-origem.ps1" %*
set ERR=%ERRORLEVEL%
echo.
pause
exit /b %ERR%
