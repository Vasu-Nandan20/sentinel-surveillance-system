@echo off
echo ============================================
echo  SENTINEL Real-Time Surveillance System
echo  Building and launching...
echo ============================================
echo.

REM Use local Maven installation
set MVN_CMD=.maven\apache-maven-3.9.6\bin\mvn.cmd

if not exist "%MVN_CMD%" (
    echo [INFO] Local Maven not found. Downloading...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile 'apache-maven.zip' -UseBasicParsing; Expand-Archive -Path 'apache-maven.zip' -DestinationPath '.maven' -Force; Remove-Item 'apache-maven.zip'"
)

REM Create required directories
if not exist "logs" mkdir logs
if not exist "reports" mkdir reports

REM Clean, compile and run
echo [1/2] Compiling project...
call %MVN_CMD% clean compile -q
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)

echo [2/2] Launching SENTINEL...
echo.
echo  Login: admin/admin or operator/operator
echo.
call %MVN_CMD% javafx:run
pause
