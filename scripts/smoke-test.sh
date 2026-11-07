#!/usr/bin/env bash
set -euo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
PACKAGE="com.animeav.tv.debug"
ACTIVITY="com.animeav.tv.MainActivity"
OUT="${2:-/tmp/opencode/animeav_home.png}"

if [[ ! -f "$APK" ]]; then
  echo "No existe $APK. Ejecuta scripts/build.sh primero." >&2
  exit 1
fi

adb install -r "$APK"
adb shell am force-stop "$PACKAGE" || true
adb shell am start -W -n "$PACKAGE/$ACTIVITY"
sleep 12
adb exec-out screencap -p > "$OUT"
echo "Captura: $OUT"
echo "--- errores recientes ---"
adb logcat -d -s AndroidRuntime:E | tail -40
