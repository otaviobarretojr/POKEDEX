#!/usr/bin/env bash
set -euo pipefail

APP_APK="${1:-stable-smoke-apks/app.apk}"
TEST_APK="${2:-stable-smoke-apks/test.apk}"
OUT_DIR="stable-smoke-results"
INSTRUMENTATION_OUT="$OUT_DIR/instrumentation.txt"
LAUNCH_LOG="$OUT_DIR/launch-logcat.txt"
TEST_LOG="$OUT_DIR/emulator-logcat.txt"
PACKAGE="com.otaviobarreto.pokedex"
RUNNER="com.otaviobarreto.pokedex.test/androidx.test.runner.AndroidJUnitRunner"
CLASSES="com.otaviobarreto.pokedex.PokedexNavigationInstrumentedTest,com.otaviobarreto.pokedex.data.CollectionPersistenceInstrumentedTest,com.otaviobarreto.pokedex.ui.PokedexChromeInstrumentedTest"

mkdir -p "$OUT_DIR"

adb install -r "$APP_APK"
adb install -r "$TEST_APK"
adb shell input keyevent 82 >/dev/null 2>&1 || true
adb shell wm dismiss-keyguard >/dev/null 2>&1 || true

# Real APK launch smoke: start the production activity, verify that its process
# stays alive, and reject an immediate crash/ANR. We intentionally do not wait
# for the full online artwork/content bootstrap on a software-only CI emulator.
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

# Focused device tests run against the stable app surface and persistence layer.
# Full content/bootstrap logic is already covered by JVM contracts and repo audits.
adb logcat -c || true
set +e
python3 - "$INSTRUMENTATION_OUT" "$RUNNER" "$CLASSES" <<'PY'
import subprocess
import sys

output_path, runner, classes = sys.argv[1:4]
command = [
    "adb", "shell", "am", "instrument", "-w", "-r",
    "-e", "class", classes,
    runner,
]
with open(output_path, "w", encoding="utf-8") as output:
    try:
        completed = subprocess.run(
            command,
            stdout=output,
            stderr=subprocess.STDOUT,
            timeout=180,
            check=False,
            text=True,
        )
        code = completed.returncode
    except subprocess.TimeoutExpired:
        output.write("\nSMOKE_TIMEOUT_AFTER_180_SECONDS\n")
        code = 124
sys.exit(code)
PY
status=$?
set -e

cat "$INSTRUMENTATION_OUT"
adb logcat -d -v threadtime > "$TEST_LOG" || true

if [ "$status" -ne 0 ]; then
  echo "Instrumentation exited with status $status" >&2
  exit "$status"
fi
if ! grep -Eq '^OK \([1-9][0-9]* tests?\)$' "$INSTRUMENTATION_OUT"; then
  echo "Instrumentation did not report a successful non-zero test count." >&2
  exit 1
fi
if grep -Eq 'FAILURES!!!|Process crashed|INSTRUMENTATION_FAILED|SMOKE_TIMEOUT' "$INSTRUMENTATION_OUT"; then
  echo "Instrumentation reported a failure." >&2
  exit 1
fi

echo "STABLE_DEVICE_SMOKE=OK"
