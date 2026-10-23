@echo off
setlocal EnableExtensions
set "APP_HOME=%~dp0"
set "JAVA_EXE="

rem Strip accidental surrounding/embedded quotes from JAVA_HOME.
if defined JAVA_HOME (
    set "JAVA_HOME_CLEAN=%JAVA_HOME:"=%"
    if exist "%JAVA_HOME_CLEAN%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME_CLEAN%\bin\java.exe"
)
if defined JAVA_EXE goto run_gradle

rem Fall back to Java on PATH if JAVA_HOME is missing or invalid.
where java.exe >nul 2>&1
if not errorlevel 1 (
    set "JAVA_EXE=java.exe"
    goto run_gradle
)

rem Android Studio installs a bundled JDK/JBR on most Windows machines.
if defined STUDIO_JDK if exist "%STUDIO_JDK%\bin\java.exe" set "JAVA_EXE=%STUDIO_JDK%\bin\java.exe"
if defined JAVA_EXE goto run_gradle
if defined ProgramFiles if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_EXE=%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe"
if defined JAVA_EXE goto run_gradle
if defined LOCALAPPDATA if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" set "JAVA_EXE=%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe"
if defined JAVA_EXE goto run_gradle

echo ERROR: Cannot find Java. Configure JAVA_HOME to a valid JDK or install Android Studio with its bundled JBR. 1>&2
exit /b 9009

:run_gradle
"%JAVA_EXE%" -Xmx128m -Dfile.encoding=UTF-8 -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
