#!/usr/bin/env bash
set -euo pipefail
APK="${1:?release apk required}"
PACKAGE="com.otaviobarreto.pokedex"
adb wait-for-device
for _ in $(seq 1 90); do
  [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break
  sleep 2
done
adb install -r "$APK"
adb logcat -c || true
adb shell am force-stop "$PACKAGE" || true
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 8
adb logcat -d -v threadtime > compat-logcat.txt || true
adb shell pidof "$PACKAGE" | grep -Eq '[0-9]'
! grep -Eq "FATAL EXCEPTION.*$PACKAGE|ANR in $PACKAGE|Process $PACKAGE .* has died" compat-logcat.txt
echo "ANDROID_COMPAT_SMOKE=OK"
