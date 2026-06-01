@echo off
call compile.bat
if errorlevel 1 exit /b %errorlevel%
java -cp out com.dmx.caro.server.GameServer
