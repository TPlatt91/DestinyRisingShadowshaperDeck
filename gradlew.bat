@echo off
setlocal EnableExtensions
set "APP_HOME=%~dp0"
set "JAVA_EXE="
set "JDK_DIR="

rem Prefer Android Studio's bundled runtime when JAVA_HOME is unavailable.
if defined JAVA_HOME (
  set "JDK_DIR=%JAVA_HOME:"=%"
  if not exist "%JDK_DIR%\bin\java.exe" set "JDK_DIR="
)
if not defined JDK_DIR if defined STUDIO_JDK (
  if exist "%STUDIO_JDK%\bin\java.exe" set "JDK_DIR=%STUDIO_JDK%"
)
if not defined JDK_DIR if defined ProgramFiles (
  if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JDK_DIR=%ProgramFiles%\Android\Android Studio\jbr"
)
if not defined JDK_DIR if defined LOCALAPPDATA (
  if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" set "JDK_DIR=%LOCALAPPDATA%\Programs\Android Studio\jbr"
)
if defined JDK_DIR (
  set "JAVA_HOME=%JDK_DIR%"
  set "PATH=%JDK_DIR%\bin;%PATH%"
  set "JAVA_EXE=%JDK_DIR%\bin\java.exe"
)
if not defined JAVA_EXE (
  for /f "delims=" %%J in ('where java.exe 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%J"
)
if not defined JAVA_EXE (
  echo ERROR: Java not found. Set JAVA_HOME to a valid JDK or install Android Studio with bundled JBR. 1>&2
  exit /b 9009
)
rem Ensure subprocesses launched by the Gradle wrapper also have a valid JAVA_HOME.
if not defined JAVA_HOME (
  for %%J in ("%JAVA_EXE%\..\..") do set "JAVA_HOME=%%~fJ"
)
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo ERROR: JAVA_HOME is not a valid JDK: "%JAVA_HOME%" 1>&2
  exit /b 9009
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
"%JAVA_EXE%" -Xmx128m -Dfile.encoding=UTF-8 -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
