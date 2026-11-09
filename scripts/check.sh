#!/usr/bin/env bash
# Runs the same checks as CI (.github/workflows/ci.yml) locally, to save GitHub Actions minutes:
# lint, all unit tests (including the Robolectric UI tests) and the debug build.
#
#   scripts/check.sh            the CI checks
#   scripts/check.sh --release  also build the release APK (unsigned; R8 and resource shrinking)
#
# Needs JDK 21 and the Android SDK (Android Studio's, or ANDROID_HOME / local.properties).
set -euo pipefail
cd "$(dirname "$0")/.."

tasks=(lintDebug testDebugUnitTest assembleDebug)
if [[ "${1:-}" == "--release" ]]; then
  tasks+=(assembleRelease)
fi

./gradlew "${tasks[@]}"
echo
echo "All checks passed. Reports: app/build/reports/ (lint, tests); screenshots: app/build/screenshots/"
