# RC Throwmeter: target Android 16 (API 36) for Google Play

Date: 2026-11-16 · Branch: `target-api-36` · Status: implemented (see "What was built")

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
   `dataBinding { enabled = true }` becomes `buildFeatures { dataBinding = true }`. The dead
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

## What was built

- **Build:** AGP 8.13.2, Gradle 8.14.7 and JDK 17, with versionCode 10 and versionName 1.7.0.
  The release APK and AAB build signed and contain no `.so` files, so 16 KB pages need nothing.
- **JOML:** as decided. `ThrowGaugeTest` pins 11 cases to the values the old library gives.
- **Back:** `MainActivity` handles back with an `OnBackPressedCallback`. No other screen overrode
  `onBackPressed()`.
- **Edge-to-edge (the builder's choice):** the main screen's `DrawerLayout` and app bar already
  handle the insets. The device list, settings and throw screens call `SystemBars.applyTo(this)`
  after `setContentView`. Under edge-to-edge, appcompat 1.7.1 lays a screen's content out across
  the whole window and adds the action bar's height to the top inset it hands over. On Android 15
  and newer, `SystemBars` pads the content by those insets and paints a `colorPrimaryDark` strip
  behind the status bar through the window background. It also keeps the status bar icons white.
  On older versions it does nothing.
  - The navigation bar is the one visible change. On Android 15 and newer it is no longer black:
    it shows the screen's light background. Its buttons are made dark there on all four screens
    (the main screen calls `SystemBars.showDarkNavigationButtons(this)`), so they stay readable
    with 3-button and gesture navigation, in light and dark mode, and over the open drawer. Below
    Android 15 the bar stays black with white buttons.
- **Tablets:** the manifest property was checked on an emulator display of 1600x2560 at 240 dpi,
  held in landscape. Without the property, the main screen stretched to landscape. With it, the
  app kept a portrait window, letterboxed. System UI shows a one-time tip about letterboxed apps.
- **Lint:** `app/lint-baseline.xml` records the 97 issues that predate this work (28 errors and
  69 warnings), so lint fails only on new ones. Release builds print a note that the baseline's
  entries were "not found". That note comes from the release build's fatal-only lint pass. It
  doesn't mean the issues were fixed.
- **Tests:** the JVM test is `ThrowGaugeTest` (11 tests). The instrumented tests are
  `MainActivityBackTest` (5), `SystemBarsTest` (4) and `ManifestPropertiesTest` (1, Android 12
  and newer).
  - On the Android 16 emulator they all pass with 3-button navigation. Screenshots taken after
    the navigation-button fix show the same layout and dark buttons in dark mode and with
    gesture navigation.
  - On the existing API 30 emulator they pass too. The manifest test is skipped there.
- **Emulator setup:** on this Intel Mac, the Android 16 emulator needs software rendering
  (`swiftshader_indirect`), 3 GB of RAM and `-no-window`. The host GPU offers only GLES 3.0, and
  Android 16 needs 3.1. Without these settings, System UI stops responding and its dialog takes
  focus away from the tests.

## Follow-ups (not in this change)

- On the throw screen, if Bluetooth is off, `BluetoothAdapter.enable()` silently does nothing
  (true since API 33). Use the "turn on Bluetooth" system dialog instead.
- Targeting API 37 by about Aug 2027: drop the tablet opt-out, handle rotation, use AGP 9.
  - Lint already flags the three portrait locks (`SourceLockedOrientationActivity`).
  - In landscape, the main screen leaves a light strip beside its toolbar over the camera cutout.
    This was seen with the tablet opt-out removed.
- Gradle 10: in `app/build.gradle`, `namespace` and `signingConfig` still use the space form
  (`namespace '…'`), which Gradle 10 removes. Switch them to `=`.
- These lint errors predate this work and are kept in the baseline:
  - 23 Bluetooth calls have no permission check.
  - `onActivityResult` skips `super` in `MainActivity`, `ThrowActivity` and
    `BluetoothBaseActivity`.
  - `MainActivity.onRequestPermissionsResult` also skips `super`. So the permission launcher's
    callback, which asks again after a denial, never runs. There is no ask-again loop. On
    Android 11 and older, though, the main screen asks for the Android 12 Bluetooth permissions
    at every start, and the system silently denies them.
  - One missing translation (`txt_diffButton`).
- Lint reports constraintlayout 2.2.2. This work didn't check whether it still supports minSdk 21.
