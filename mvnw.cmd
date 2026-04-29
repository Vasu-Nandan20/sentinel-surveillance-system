@REM Maven Wrapper script for Windows
@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_PROPERTIES="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"

if exist %WRAPPER_JAR% (
    java -jar %WRAPPER_JAR% %*
) else (
    echo Maven wrapper jar not found. Installing Maven manually...
    
    REM Try to download Maven directly
    set MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
    set MAVEN_ZIP=%MAVEN_PROJECTBASEDIR%apache-maven.zip
    set MAVEN_HOME=%MAVEN_PROJECTBASEDIR%.maven

    if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
        echo Downloading Apache Maven 3.9.6...
        powershell -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%' -UseBasicParsing"
        echo Extracting...
        powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_HOME%' -Force"
        del "%MAVEN_ZIP%"
        
        REM Find the extracted directory
        for /d %%i in ("%MAVEN_HOME%\apache-maven-*") do set MAVEN_BIN=%%i\bin
    ) else (
        for /d %%i in ("%MAVEN_HOME%\apache-maven-*") do set MAVEN_BIN=%%i\bin
    )

    "%MAVEN_BIN%\mvn.cmd" %*
)

endlocal
