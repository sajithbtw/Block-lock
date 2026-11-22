# ScrollLock

A native Android focus app built with Kotlin. It lets users select distracting apps, start a timed focus session, and uses Android Accessibility Service to detect selected app launches and display an interruption screen.

## Build

GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch. Open **Actions → Build ScrollLock APK**, run the workflow, then download the `ScrollLock-debug-apk` artifact.

## First run

1. Install the debug APK.
2. Open ScrollLock and enable its Accessibility Service in Android Settings.
3. Select apps, choose a duration, and start a session.
4. Open a selected app to test the interruption screen.

## Privacy and limitations

Accessibility is used for app/window transition events only. The prototype does not read screen content or send usage data to a server. Users remain in control and can disable the permission or uninstall the app. Android may restrict background behaviour depending on device and OS version.

## App details

- Application ID: `com.sajith.scrolllock`
- Minimum Android: 8.0 (API 26)
- Kotlin, Gradle, GitHub Actions
