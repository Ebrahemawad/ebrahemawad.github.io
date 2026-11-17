# Masar Android (APK build)

Native Android WebView wrapper around the actual Masar shipping management system:
https://ebrahemawad.github.io/masar/

## Build
Use JDK 17 and Android SDK 35, with Gradle 8.10.2:
```sh
gradle :app:assembleDebug
```
The installable test APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Current limitations (important)
- The APK is an **Android WebView client**, not an independently functioning offline native app.
- Shipment, login, shifts and accounting remain governed by Supabase servers.
- Geolocation is available only when the app is foregrounded; **background/locked-screen location tracking is not supported in this first APK**.
- Excel exports created as `blob:` downloads need native bridge integration, and cannot be saved from this initial WebView directly. Excel imports use Android's file picker.
- Do not treat this debug-signed build as a production release or install from untrusted sources.
- The app requests location permission only when the web app requests location sharing.
- Shift enforcement (start required, no stop until eight hours) is performed server-side and remains in force if the app closes.

## Native courier delivery reminders (November 2026)
- Courier appointments are optional and stored in Supabase.
- The existing Android app now supports `window.MasarNative` to sync appointments, request Android notification permission, test notifications, and clear reminders on sign-out.
- Android AlarmManager schedules a reminder 10 minutes before each synchronized delivery appointment; reminders remain scheduled after closing the app and are restored on device reboot.
- Updated/removed appointments are reflected when the courier opens/syncs the app. Changes made elsewhere while the app is closed are NOT immediately synchronized; a separate push synchronization channel would be needed.
- On Android 12+, precise timing may require the system's Alarms & reminders permission; otherwise notification may be delayed. On Android 13+, notification permission is required.
- This change modifies the **existing** `masar-android` source. To obtain an APK, use the existing GitHub Actions workflow `Build Masar Android APK` and verify its result. A device-level delivery test and APK signature-compatible update have not yet been confirmed.
