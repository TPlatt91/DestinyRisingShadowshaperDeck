#!/usr/bin/env sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -n "${JAVA_HOME:-}" ]; then JAVA_CMD="$JAVA_HOME/bin/java"; else JAVA_CMD="java"; fi
if ! command -v "$JAVA_CMD" >/dev/null 2>&1 && [ ! -x "$JAVA_CMD" ]; then
  echo "Java 17 or newer is required. Install Android Studio/JDK 17+ or set JAVA_HOME." >&2
  exit 1
fi
exec "$JAVA_CMD" -Xmx128m -Dfile.encoding=UTF-8 -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
