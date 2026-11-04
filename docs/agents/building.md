# Building, testing, running

Environment facts an agent needs before the first gradle invocation. Machine-specific paths refer to the current dev machine; verify before trusting them.

## JDK

- Current AGP is 8.7.3: needs JDK 17+ to run (Java 17 and 21 both verified). The machine's `JAVA_HOME` may be stale/nonexistent (was `java-27-openjdk`, fixed for new shells via `~/.bash.d/rea`); when a Gradle run fails on JAVA_HOME, pass it explicitly per invocation:
  `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew …`

## Gradle targets

- `assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- `assembleRelease` → `app/build/outputs/apk/release/app-release.apk`, auto-signed when `release.keystore` exists (repo root, gitignored); passwords overridable via `KEYSTORE_PASSWORD` / `KEY_PASSWORD` / `KEY_ALIAS` gradle properties (defaults in `app/build.gradle`).
- `testDebugUnitTest` — JVM unit tests (DwellMachine, DwellController, Firing.decide, reconciler derive). CI runs `./gradlew build` plus the mechanical standards check (`scripts/check-standards.sh`); judgement reviewer rules live in `CODING_STANDARDS.md`.
- `apksigner verify` lives in the real SDK, not the stale one: `/home/loki/.local/share/android-sdk/build-tools/35.0.0/apksigner`.

## Emulator

- Two SDK roots exist on this machine; the AVD resolves against the empty stale one unless overridden. Launch with:
  `ANDROID_SDK_ROOT=/home/loki/.local/share/android-sdk /home/loki/Android/Sdk/emulator/emulator -avd medium_phone > ~/.android/medium_phone/emulator.log 2>&1 &`
- `android emulator start <device>` exists but needs the same `ANDROID_SDK_ROOT`; check readiness by polling `adb shell getprop sys.boot_completed` == `1`. Installing before boot completes fails with `cmd: Can't find service: package`.
- Install + launch:
  `adb install --no-streaming -r app/build/outputs/apk/debug/app-debug.apk`
  `adb shell monkey -p me.lucky.wasted -c android.intent.category.LAUNCHER 1`
- The emulator has no network by default — network-dependent features fail offline.

## Geofence (police-station trigger) e2e

The dwell→fire path needs realistic fixes; spoof them from the emulator console:

1. `adb emu geo fix <lon> <lat>` inside a fence (fences and radius are in `StationStore`/`DwellMachine`; the bundled dataset is Australian facilities).
2. Dwell time comes from `Preferences.triggerGeofenceDwell` (minutes); keep fixes within the 25 m accuracy gate and inside the same fence for the whole dwell.
3. Device admin must be granted first (`adb shell dpm set-active-admin`? no — this app only accepts interactive `ACTION_ADD_DEVICE_ADMIN`; do it once in the emulator UI).
4. Watch state: `adb logcat -s Firing` (logged fire failures) and the FGS notification for "location unavailable".

