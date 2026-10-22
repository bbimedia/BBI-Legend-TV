@echo off
setlocal EnableExtensions
cd /d "%~dp0"
where py >nul 2>nul
if not errorlevel 1 (
  py -3 server.py
) else (
  where python >nul 2>nul
  if errorlevel 1 (
    echo Python 3.10 or newer is required.
    echo Download: https://www.python.org/downloads/
    echo Enable Add Python to PATH, then run this file again.
  ) else (
    python server.py
  )
)
pause
