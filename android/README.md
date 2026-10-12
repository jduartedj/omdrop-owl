# omdrop Android compatibility artifact

**Not a working AirDrop app.** Native iPhone send/receive is unavailable. No LAN/Wi-Fi Direct fallback is included. Read the [compatibility evidence](../docs/android/COMPATIBILITY.md), [implemented architecture](../docs/android/ARCHITECTURE.md), and [required acceptance](../docs/android/ACCEPTANCE.md).

This standalone arm64/API31+ Android application provides a real read-only NDK capability probe, nonunique model/SoC/OS diagnostics, an explicit fail-closed radio gate, local selected-document SHA256 receipts, and user-directed local JSON exports. Send/Receive stay disabled; the OWL adapter seam explicitly throws. No OWL or inherited helper code is packaged. See [license boundary](NOTICE.md).

## Build without touching phones

Toolchain used: Java17, Gradle8.14.3 (wrapper and distribution SHA256 pinned), Android Gradle Plugin8.10.1, SDK36, build-tools35.0.0 selected by AGP, NDK27.3.13750724/r27d, CMake3.28.3, Ninja. On this ARM64 Linux host, existing operator-owned qemu-x86_64 binfmt runs the official x86_64 SDK/NDK tools; no global configuration was modified. Other hosts need a compatible official SDK/NDK installation. The prototype has no third-party runtime Java dependencies.

Configure ignored `local.properties` with your own SDK path; use `cmake.dir` only when selecting an existing CMake3.28.3 installation. Install NDK27.3.13750724 through your normal SDK setup or point `-Pomdrop.ndkPath=/absolute/path/to/android-ndk-r27d` to a task-local unpacked official NDK. Do not commit machine-local paths. SDK licenses were already present on the build host; no new license acceptance or global SDK install was performed.

```sh
cd android
./gradlew clean :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. It is a development artifact, not a production/release-signed app. Run on actual phones only after explicit installation permission. Do not confuse APK build/signature checks with on-device UI/JNI/transfer acceptance.

## Host-only tests and read-only inventory

```sh
python3 -B -m unittest discover -s android/tests -v
python3 -B -m unittest discover -s tests -v  # inherited helper command-double tests; no real radio
python3 android/tools/inspect_devices.py
cc -std=c11 -Wall -Wextra -Werror -fsanitize=address,undefined \
  -I android/app/src/main/cpp android/tests/probe_core_test.c \
  android/app/src/main/cpp/probe_core.c -o /tmp/omdrop-probe-core-test
/tmp/omdrop-probe-core-test
```

The inventory reads only a property whitelist and a driver symlink, not accounts/serials/MACs/SSID/full getprop/logcat. No device means *unknown hardware*, not an unsupported-chipset assertion. Native host JNI smoke source is `tests/JNIHostSmoke.java`; compile the two probe C sources against host JNI headers and set `java.library.path` to that host library. This checks JNI plumbing on Linux only.

The lint configuration excludes only pinned-toolchain update suggestions (`AndroidGradlePluginVersion`) and ChromeOS x86 ABI support (`ChromeOsAbiSupport`), because this deliberately targets Samsung arm64. All other lint warnings are errors; no baseline hides diagnostics. Kotlin/AndroidX/emulator/device-install tasks are not needed.

## Source provenance / verification

* Official wrapper JAR SHA256: `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`, checked against `https://services.gradle.org/distributions/gradle-8.14.3-wrapper.jar.sha256`.
* Gradle distribution SHA256 is recorded in `gradle/wrapper/gradle-wrapper.properties`.
* Official NDK archive SHA1 (Google repository metadata): `22105e410cf29afcf163760cc95522b9fb981121`, size663956036 bytes. Downloaded archive was checked against metadata before the final build. NDK binaries/archive remain ignored task-local build tools, not Git payloads.
* Source-only checkpoint lives in the authorized [fork branch](https://github.com/jduartedj/omdrop-owl/tree/android/compatibility-gate). The private/local report bundle records APK SHA256, clean-build/test outputs and the exact human blockers.
