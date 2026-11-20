@ECHO OFF
chcp 65001 >nul
title NGOC RONG OFFLINE - SERVER LOCAL (127.0.0.1:14445)
cd /d "%~dp0"

tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe" >NUL
if errorlevel 1 (
    echo [1/2] Dang khoi dong MySQL (XAMPP)...
    start "" /B "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini"
    timeout /t 3 /nobreak >nul
)

IF "%RAM_MAX%"=="" SET RAM_MAX=1024m
SET NRO_RESTART_LOOP=1
:START
echo [2/2] Dang khoi dong Server Ngoc Rong Offline (Cong 14445, Web Admin: http://localhost:8088)...
"C:\Users\TUF FX506\Downloads\nro\jre21\jdk-21.0.13+9-jre\bin\java.exe" -server -XX:-OmitStackTraceInFastThrow -Dfile.encoding=UTF-8 -Xms256m -Xmx%RAM_MAX% -XX:+UseG1GC -jar 20.jar
IF %ERRORLEVEL% EQU 3 (
    echo.
    echo Bao tri xong. Server tu khoi dong lai sau 5 giay...
    timeout /t 5
    GOTO START
)
PAUSE
