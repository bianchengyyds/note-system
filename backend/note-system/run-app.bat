@echo off
echo Starting Note System Application...

REM Build the JAR file if not already built
echo Building JAR file...
mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo Starting application...
java -jar target/note-system-0.0.1-SNAPSHOT.jar

pause