#!/usr/bin/env bash
# Mechanical coding-standards checks. CI runs this before ./gradlew build.
# Judgement rules (not checkable by grep) live in CODING_STANDARDS.md.
set -euo pipefail
cd "$(dirname "$0")/.."

fail=0

# destructive DPM ops are callable only from the Fire module (fire/) and the
# DevicePolicyManager adapter (admin/)
hits=$(grep -rn "lockNow(\|wipeData(" app/src/main --include="*.kt" \
  | grep -v "/fire/" | grep -v "/admin/" || true)
if [ -n "$hits" ]; then
  echo "lockNow()/wipeData() outside fire/ and admin/:"
  echo "$hits"
  fail=1
fi

# only Preferences owns the device-protected plaintext mirror (ADR 0002)
hits=$(grep -rln "createDeviceProtectedStorageContext" app/src/main --include="*.kt" \
  | grep -v "Preferences.kt" || true)
if [ -n "$hits" ]; then
  echo "createDeviceProtectedStorageContext outside Preferences.kt (ADR 0002):"
  echo "$hits"
  fail=1
fi

exit "$fail"