@echo off
setlocal EnableExtensions
cd /d "%~dp0"
set "LOG=%~dp0shadow_engine_test_log.txt"
echo Shadow Shaper Duels - local engine checks
echo No GitHub Actions will run.
echo.
> "%LOG%" echo ==== Shadow Shaper Duels diagnostics ====
>> "%LOG%" echo Started: %DATE% %TIME%
>> "%LOG%" echo JAVA_HOME=%JAVA_HOME%
>> "%LOG%" echo Java candidates on PATH:
where java.exe >> "%LOG%" 2>&1
>> "%LOG%" echo.
>> "%LOG%" echo ==== Gradle and gameplay checks ====
call "%~dp0gradlew.bat" :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain --stacktrace >> "%LOG%" 2>&1
set "EXITCODE=%ERRORLEVEL%"
>> "%LOG%" echo.
>> "%LOG%" echo Local checks exit code: %EXITCODE%
type "%LOG%"
echo.
if "%EXITCODE%"=="0" (
 echo SUCCESS: local checks completed.
) else (
 echo FAILED: exit code %EXITCODE%.
)
echo.
echo Log: "%LOG%"
echo The log is always overwritten. Push shadow_engine_test_log.txt to share the result.
pause
exit /b %EXITCODE%
