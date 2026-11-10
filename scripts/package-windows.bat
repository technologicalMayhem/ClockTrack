@echo off
rem Builds the Windows app folder target\dist\ClockTrack with ClockTrack.exe
rem
rem Usage: scripts\package-windows.bat
rem
rem Needs JDK 25 (jpackage on the PATH or JAVA_HOME set) and an internet connection for the first build.
setlocal
cd /d "%~dp0.."

set "JPACKAGE=jpackage"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"

set "INPUT_DIR=target\jpackage-input"
set "DIST_DIR=target\dist"
set "APP_DIR=%DIST_DIR%\ClockTrack"

if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
if exist "%APP_DIR%" rmdir /s /q "%APP_DIR%"

echo Building the project...
call mvnw.cmd -q clean package || goto :failed

echo Collecting runtime dependencies...
call mvnw.cmd -q org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies -DincludeScope=runtime -DoutputDirectory=%INPUT_DIR% || goto :failed
copy /y target\ClockTrack-1.0-SNAPSHOT.jar "%INPUT_DIR%" >nul || goto :failed

echo Creating the app folder...
"%JPACKAGE%" --type app-image ^
    --dest "%DIST_DIR%" ^
    --name ClockTrack ^
    --app-version 1.0 ^
    --icon assets\icon.ico ^
    --module-path "%INPUT_DIR%" ^
    --module net.techmayhem.clocktrack/net.techmayhem.clocktrack.ClockTrackApp ^
    --java-options "--enable-native-access=javafx.graphics" || goto :failed

echo.
echo Done: %APP_DIR%\ClockTrack.exe
exit /b 0

:failed
echo.
echo Build failed.
exit /b 1
