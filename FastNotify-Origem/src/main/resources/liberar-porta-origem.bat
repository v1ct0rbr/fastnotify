@echo off
setlocal
set "PS1=%~dp0liberar-porta-origem.ps1"
if not exist "%PS1%" (
  echo Arquivo nao encontrado: %PS1%
  exit /b 1
)
echo Solicitando elevacao (Administrador) para liberar a porta da Origem...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath powershell -Verb RunAs -ArgumentList '-NoProfile','-ExecutionPolicy','Bypass','-File','%PS1%' -WorkingDirectory '%~dp0'"
exit /b %ERRORLEVEL%
