#!/usr/bin/env bash
set -euo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
PACKAGE="com.animeav.tv.debug"
ACTIVITY="com.animeav.tv.MainActivity"

if [[ ! -f "$APK" ]]; then
  echo "No existe $APK. Ejecuta scripts/build.sh primero." >&2
  exit 1
fi

if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB=(adb -s "$ANDROID_SERIAL")
else
  ADB=(adb)
fi

"${ADB[@]}" install -r "$APK"
"${ADB[@]}" shell am start -n "$PACKAGE/$ACTIVITY"
