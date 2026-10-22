@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "LOG=%~dp0shadow_engine_test_log.txt"
echo Shadow Shaper Duels - local engine regression and Android build check
echo No GitHub Actions or GitHub workflow minutes will be used.
echo.
echo Running :app:testDebugUnitTest and :app:assembleDebug...
echo Saving complete output to "%LOG%"
echo.

call "%~dp0gradlew.bat" :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain --stacktrace > "%LOG%" 2>&1
set "EXITCODE=%ERRORLEVEL%"
type "%LOG%"
echo.
if "%EXITCODE%"=="0" (
    echo SUCCESS: local unit tests and debug build completed.
) else (
    echo FAILED: exit code %EXITCODE%. Do not assume source changes compiled.
)
echo Full log: "%LOG%"
echo Send shadow_engine_test_log.txt if any error appears.
pause
exit /b %EXITCODE%
