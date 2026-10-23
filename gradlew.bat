@echo off
setlocal EnableExtensions
set "APP_HOME=%~dp0"
set "JDK_DIR="
rem Gradle 8.11.1/Kotlin DSL used by this project cannot run on Java 25.
rem Prefer Android Studio JBR or installed Java 21/17 over Java 25.
if defined ProgramFiles (
  if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" call :checkjdk "%ProgramFiles%\Android\Android Studio\jbr"
)
if not defined JDK_DIR if defined STUDIO_JDK call :checkjdk "%STUDIO_JDK%"
if not defined JDK_DIR if defined JAVA_HOME call :checkjdk "%JAVA_HOME%"
if not defined JDK_DIR if defined ProgramFiles (
  for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*" "%ProgramFiles%\Eclipse Adoptium\jdk-17*" "%ProgramFiles%\Java\jdk-21*" "%ProgramFiles%\Java\jdk-17*" "%ProgramFiles%\Microsoft\jdk-21*" "%ProgramFiles%\Microsoft\jdk-17*") do (
    if not defined JDK_DIR call :checkjdk "%%~fD"
  )
)
if not defined JDK_DIR if defined LOCALAPPDATA (
  if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" call :checkjdk "%LOCALAPPDATA%\Programs\Android Studio\jbr"
)
if not defined JDK_DIR (
  echo ERROR: This project requires JDK 17 or 21 to run Gradle 8.11.1. 1>&2
  echo Current Java may be 25, which fails with IllegalArgumentException: 25.0.2. 1>&2
  echo Install JDK 21 and set JAVA_HOME to its root, or configure Android Studio's Gradle JDK to 21. 1>&2
  exit /b 9009
)
set "JAVA_HOME=%JDK_DIR%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
"%JAVA_HOME%\bin\java.exe" -Xmx128m -Dfile.encoding=UTF-8 -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%

:checkjdk
if defined JDK_DIR exit /b 0
set "CANDIDATE=%~1"
set "CANDIDATE=%CANDIDATE:"=%"
if not exist "%CANDIDATE%\bin\java.exe" exit /b 0
set "CHECK_VERSION="
set "JAVA_VERSION_LOG=%TEMP%\shadow_java_version_%RANDOM%_%RANDOM%.txt"
"%CANDIDATE%\bin\java.exe" -version 2>"%JAVA_VERSION_LOG%"
for /f "tokens=3" %%V in ('findstr /i "version" "%JAVA_VERSION_LOG%"') do if not defined CHECK_VERSION set "CHECK_VERSION=%%~V"
del /q "%JAVA_VERSION_LOG%" >nul 2>&1
if "%CHECK_VERSION:~0,2%"=="21" set "JDK_DIR=%CANDIDATE%"
if "%CHECK_VERSION:~0,2%"=="17" set "JDK_DIR=%CANDIDATE%"
exit /b 0
