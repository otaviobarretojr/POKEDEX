#!/usr/bin/env bash
set -uo pipefail

APP_APK="${1:-stable-smoke-apks/app.apk}"
TEST_APK="${2:-stable-smoke-apks/test.apk}"
OUT_DIR="stable-smoke-results"
INSTRUMENTATION_OUT="$OUT_DIR/instrumentation.txt"
LOGCAT_OUT="$OUT_DIR/emulator-logcat.txt"

mkdir -p "$OUT_DIR"

adb install -r "$APP_APK"
adb install -r "$TEST_APK"
adb shell input keyevent 82 >/dev/null 2>&1 || true
adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
adb logcat -c || true

set +e
python3 - "$INSTRUMENTATION_OUT" <<'PY'
import subprocess
import sys

output_path = sys.argv[1]
command = [
    "adb", "shell", "am", "instrument", "-w", "-r",
    "com.otaviobarreto.pokedex.test/androidx.test.runner.AndroidJUnitRunner",
]
with open(output_path, "w", encoding="utf-8") as output:
    try:
        completed = subprocess.run(
            command,
            stdout=output,
            stderr=subprocess.STDOUT,
            timeout=240,
            check=False,
            text=True,
        )
        code = completed.returncode
    except subprocess.TimeoutExpired:
        output.write("\nSMOKE_TIMEOUT_AFTER_240_SECONDS\n")
        code = 124
sys.exit(code)
PY
status=$?
set -e

cat "$INSTRUMENTATION_OUT"
adb logcat -d -v threadtime > "$LOGCAT_OUT" || true

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

echo "STABLE_SMOKE=OK"
