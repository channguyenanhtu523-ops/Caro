@echo off
call compile.bat
if errorlevel 1 exit /b %errorlevel%
start "" javaw -cp out com.dmx.caro.client.GameClientLauncher
