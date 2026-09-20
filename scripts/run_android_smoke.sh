#!/usr/bin/env bash
set -euo pipefail

APP_APK="${1:-stable-smoke-apks/app.apk}"
TEST_APK="${2:-stable-smoke-apks/test.apk}"
OUT_DIR="stable-smoke-results"
LAUNCH_LOG="$OUT_DIR/launch-logcat.txt"
TEST_LOG="$OUT_DIR/emulator-logcat.txt"
PACKAGE="com.otaviobarreto.pokedex"
RUNNER="com.otaviobarreto.pokedex.test/androidx.test.runner.AndroidJUnitRunner"

mkdir -p "$OUT_DIR"

restart_adb() {
  adb kill-server >/dev/null 2>&1 || true
  sleep 2
  adb start-server >/dev/null
}

wait_for_android_ready() {
  local attempt
  restart_adb
  for attempt in $(seq 1 90); do
    if adb wait-for-device >/dev/null 2>&1 &&
       [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] &&
       adb shell cmd package list packages >/dev/null 2>&1; then
      echo "ANDROID_READY_AFTER_ATTEMPT=$attempt"
      return 0
    fi

    if [ $((attempt % 15)) -eq 0 ]; then
      echo "ADB/package manager not ready on attempt $attempt; restarting ADB."
      restart_adb
    fi
    sleep 2
  done

  echo "Android emulator never reached a usable ADB/package-manager state." >&2
  adb devices -l || true
  adb shell getprop sys.boot_completed || true
  return 1
}

install_apk_with_retry() {
  local apk="$1"
  local label="$2"
  local attempt

  for attempt in $(seq 1 5); do
    if adb install -r "$apk"; then
      echo "APK_INSTALL_${label}=OK_ATTEMPT_${attempt}"
      return 0
    fi

    echo "Install failed for $label on attempt $attempt; recovering ADB/package manager." >&2
    restart_adb
    wait_for_android_ready || true
    sleep 2
  done

  echo "Unable to install $label after retries." >&2
  return 1
}

wait_for_android_ready
install_apk_with_retry "$APP_APK" "APP"
install_apk_with_retry "$TEST_APK" "TEST"

adb shell input keyevent 82 >/dev/null 2>&1 || true
adb shell wm dismiss-keyguard >/dev/null 2>&1 || true

# Real APK launch smoke. Full online bootstrap is intentionally not awaited on
# the software-only CI emulator; the production process must launch and survive.
adb logcat -c || true
adb shell am force-stop "$PACKAGE" || true
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 8
adb logcat -d -v threadtime > "$LAUNCH_LOG" || true
if ! adb shell pidof "$PACKAGE" | grep -Eq '[0-9]'; then
  echo "Production app process did not survive launch." >&2
  cat "$LAUNCH_LOG"
  exit 1
fi
if grep -Eq "FATAL EXCEPTION.*$PACKAGE|ANR in $PACKAGE|Process $PACKAGE .* has died" "$LAUNCH_LOG"; then
  echo "Production app reported a launch crash/ANR." >&2
  cat "$LAUNCH_LOG"
  exit 1
fi
echo "PRODUCTION_LAUNCH_SMOKE=OK"
adb shell am force-stop "$PACKAGE" || true

run_instrumentation_group() {
  local name="$1"
  local selector="$2"
  local timeout_seconds="$3"
  local output="$OUT_DIR/${name}.txt"

  echo "Running stable smoke group: $name"
  set +e
  python3 - "$output" "$RUNNER" "$selector" "$timeout_seconds" <<'PY'
import subprocess
import sys

output_path, runner, selector, timeout_seconds = sys.argv[1:5]
command = [
    "adb", "shell", "am", "instrument", "-w", "-r",
    "-e", "class", selector,
    runner,
]
with open(output_path, "w", encoding="utf-8") as output:
    try:
        completed = subprocess.run(
            command,
            stdout=output,
            stderr=subprocess.STDOUT,
            timeout=int(timeout_seconds),
            check=False,
            text=True,
        )
        code = completed.returncode
    except subprocess.TimeoutExpired:
        output.write(f"\nSMOKE_TIMEOUT_AFTER_{timeout_seconds}_SECONDS\n")
        code = 124
sys.exit(code)
PY
  local status=$?
  set -e

  cat "$output"

  if [ "$status" -ne 0 ]; then
    echo "Instrumentation group $name exited with status $status" >&2
    return "$status"
  fi
  if ! grep -Eq '^OK \([1-9][0-9]* tests?\)$' "$output"; then
    echo "Instrumentation group $name did not report a successful non-zero test count." >&2
    return 1
  fi
  if grep -Eq 'FAILURES!!!|Process crashed|INSTRUMENTATION_FAILED|SMOKE_TIMEOUT' "$output"; then
    echo "Instrumentation group $name reported a failure." >&2
    return 1
  fi

  echo "SMOKE_GROUP_${name}=OK"
}

adb logcat -c || true

# One full traversal validates every primary destination and that bottom
# navigation remains available after route changes. The repetitive stress test
# and pixel snapshots remain compiled/manual checks, but are not stable gates.
run_instrumentation_group   "navigation"   "com.otaviobarreto.pokedex.PokedexNavigationInstrumentedTest#primaryRoutes_areReachableAndBottomNavigationSurvives"   300

# Regression gate: a Journey opened from Trainer Today must expose a visible
# return action and hand control back to the caller instead of trapping ROUTE.
run_instrumentation_group   "trainer-today-return"   "com.otaviobarreto.pokedex.TrainerTodayJourneyReturnInstrumentedTest#journeyOpenedFromTrainerToday_hasVisibleReturnAction"   180

run_instrumentation_group   "persistence"   "com.otaviobarreto.pokedex.data.CollectionPersistenceInstrumentedTest"   120

run_instrumentation_group   "chrome"   "com.otaviobarreto.pokedex.ui.PokedexChromeInstrumentedTest"   180

adb logcat -d -v threadtime > "$TEST_LOG" || true

echo "STABLE_DEVICE_SMOKE=OK"
