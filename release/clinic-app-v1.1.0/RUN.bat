@echo off
set SCRIPT_DIR=%~dp0
for %%f in ("%SCRIPT_DIR%clinic-app-*.jar") do set JAR_PATH=%%f
if not exist "%JAR_PATH%" (
    echo Jar file not found. Did you run the release packaging script?
    exit /b 1
)
java -jar "%JAR_PATH%"
