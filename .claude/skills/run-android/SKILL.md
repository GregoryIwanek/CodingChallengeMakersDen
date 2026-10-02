---
name: run-android
description: Install and launch the CodingChallenge app on a running emulator, then take a
  screenshot and look at it. Use to confirm a UI change works in the real app, not just in tests.
---

# Run CodingChallenge on Android

1. Run `adb devices`. If no device is listed, stop and ask the user to start the `Pixel_8a`
   emulator from Android Studio's Device Manager. Don't start one yourself.
2. Run `./gradlew installDebug`. If it fails, report the error and stop.
3. Run `adb shell am start -W -n pl.gi.codingchallenge/.MainActivity`.
4. Run `adb exec-out screencap -p > /tmp/run-android.png`, then Read `/tmp/run-android.png`.
5. Describe what's on screen and whether it matches the change being checked. If the change
   needs interaction (typing a query, scrolling), say what to do by hand; don't guess taps.
