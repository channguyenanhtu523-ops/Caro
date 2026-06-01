@echo off
setlocal
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
set EXIT_CODE=%ERRORLEVEL%
del sources.txt >nul 2>nul
exit /b %EXIT_CODE%
