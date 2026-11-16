# RC Throwmeter: target Android 16 (API 36) for Google Play

Date: 2026-11-16 · Branch: `target-api-36` · Status: draft, awaiting approval

## Intent

Since Aug 31, 2026, Google Play only accepts new apps and updates that target API 36 (Android 16).
RC Throwmeter targets API 34, so no update can be published. Play also stops showing apps that target
below API 35 to new users on newer Android versions. The goal is a release build that Play accepts
and that behaves on Android 16 the way the app behaves today.

## Goals

- `targetSdk 36` and `compileSdk 36`, building from a clean checkout. Today the build fails on the
  missing JOML snapshot.
- On phones from Android 5.0 to Android 16, the app looks and works as it does today: the
  instructions page, the drawer, the exit reminder, picking sensors, the throw screen and settings.
- The throw and angle numbers stay exactly the same after the JOML swap.

## Non-goals

- Targeting API 37 (Android 17). Play will most likely require it around Aug 2027.
- Modernizing anything that API 36 doesn't force: AGP 9, newest AndroidX, `startActivityForResult`,
  `ViewModelProviders`, a UI redesign.
- Fixing older issues that API 36 doesn't cause. These are listed as follow-ups below.

## Anti-goals (a failure even if it builds)

- Any change in the measured angles, throw or travel values.
- Any change to how sensors are paired, picked or connected.
- Dropping users: `minSdk` stays 21.
- On Android 15 or 16: content hidden under the status or navigation bar, or status bar icons that
  can't be read.

## Constraints

- `org.joml:joml-android:1.9.3-SNAPSHOT` can no longer be downloaded from anywhere (both snapshot
  repositories return 404). The released `org.joml:joml:1.10.9` (MIT, Maven Central) is the same
  library. The app uses `Vector3d`, `Quaterniond` and `org.joml.Math` in `ThrowGauge` only.
- compileSdk 36 needs AGP 8.10 or newer. AGP 8.13.2 also needs Gradle 8.13+ and JDK 17
  (JDK 17 is installed).
- Android Studio Koala (2024.1) only supports AGP up to 8.6. Opening the project in Android Studio
  needs Narwhal 3 Feature Drop (2025.1.3) or newer. Command-line builds don't need it.
- The newest AndroidX releases need minSdk 23. The newest ones that still support 21 are
  appcompat 1.7.1, activity 1.11.0, core 1.17.0, material 1.13.0 and constraintlayout 2.2.1.
- Bluetooth to the WitMotion sensors can't be exercised on the emulator. The real-sensor check
  stays with the owner.

## Approaches considered

- **A. Compliance fix that keeps the app as it is (picked).** Fix the build, raise the SDK
  levels, and handle the three Android 16 changes in place. The Bluetooth and sensor code is not
  touched. This is the smallest change that passes Play's check without leaning on opt-outs that
  disappear next year (apart from the tablet one, which is a product choice to keep portrait).
- **B. Compliance plus modernizing.** AGP 9, the newest libraries (minSdk 23 drops Android 5),
  in-layout toolbars, replacing deprecated APIs, and a throw screen that survives rotation. It's
  more risk to the measuring flow and much more to retest. It fits better with the API 37 move.
- **C. Opt-outs wherever possible.** Fix edge-to-edge only, and opt out of the new back handling
  and the tablet change. It's the smallest diff, but both opt-outs end with API 37.

## Decisions

1. **Build:** AGP 8.13.2 and Gradle 8.14.x. `compileSdk 36`, `targetSdk 36`, `minSdk 21`.
   `dataBinding { enabled = true }` becomes `buildFeatures { dataBinding true }`. The dead
   `oss.sonatype.org` repository is removed. Version becomes `versionCode 10` / `versionName "1.7.0"`.
2. **JOML:** switch to `org.joml:joml:1.10.9`. Add a JVM unit test that pins `ThrowGauge`'s
   outputs to the values the old 1.9.3 library produces.
   *Found during planning:* 1.10.9 is not a drop-in replacement, and two one-line fixes in
   `ThrowGauge` restore exact parity. These were checked with a one-off harness, all 11 cases
   identical to 9 decimals.
   - `Quaterniond.angle()` no longer folds into 0–180°. A movement crossing roll ±180° (sensor
     upside down, −170° → +170°) reads 340° instead of 20°, with the travel sign flipped. Fix:
     fold the angle in `ThrowGauge` the way 1.9.3 did.
   - 1.10.9 adds `Math.toRadians(float)`, so `SetAngles(float…)` lost double precision. That
     is about 6e-7°, invisible on screen. Fix: convert to radians as `double`.
3. **Libraries:** raise AndroidX only as far as minSdk 21 allows: appcompat 1.7.1, material 1.13.0,
   constraintlayout 2.2.1, plus activity 1.11.0 for the edge-to-edge and back APIs.
4. **Edge-to-edge:** keep today's look on every screen and every Android version. The status bar
   stays teal with white icons, and nothing sits under the status or navigation bars. Each screen
   gets the system-bar insets applied.
5. **Back:** `MainActivity.onBackPressed()` becomes an `OnBackPressedCallback` with the same
   behavior. If the drawer is open, back closes it. Otherwise back shows the "remind sensor power"
   exit dialog.
6. **Tablets and foldables:** set the `android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY`
   manifest property so the portrait lock still holds on Android 16 large screens. Google says this
   opt-out is temporary and will end with API 37.
7. **16 KB pages:** the app has no native code. Confirm the built APK contains no `.so` files.

## Left to the builder

- How the insets are applied on the action-bar screens (device list, settings, throw screen). This
  gets decided after seeing them on an Android 16 emulator.
- The exact Gradle 8.14.x patch release, and any warnings the AGP upgrade raises.

## Verification

- `./gradlew clean test lint assembleDebug assembleRelease` passes.
- The unit test for the angle math passes, and the one-off comparison against old JOML matches.
- On an Android 16 (API 36) emulator: screenshots of the main screen, drawer, device list, settings
  and throw screen (reached through a fake saved sensor), the back button in each case, and the same
  screens on the existing API 30 emulator to catch regressions.
- Owner, before uploading to Play: a real-sensor session on a phone, ideally one running
  Android 15 or 16.

## Follow-ups (not in this change)

- On the throw screen, if Bluetooth is off, `BluetoothAdapter.enable()` silently does nothing
  (true since API 33). Use the "turn on Bluetooth" system dialog instead.
- Targeting API 37 by about Aug 2027: drop the tablet opt-out, handle rotation, use AGP 9.
