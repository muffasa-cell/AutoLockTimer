# AutoLock Timer (Android)

A minimal native Android starter project. It runs a user-started foreground countdown and locks the device when the timer expires, using Android Device Administration (`force-lock`).

## Build an APK

1. Install Android Studio (current stable) and Android SDK Platform 35.
2. Open this `AutoLockTimer` folder in Android Studio.
3. Let Gradle sync complete.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. The debug APK is normally created at `app/build/outputs/apk/debug/app-debug.apk`.

## First-time setup on the phone

1. Install the APK.
2. Open **AutoLock Timer**.
3. Tap **ENABLE SCREEN-LOCK PERMISSION** and approve the Android Device Admin prompt.
4. Allow notifications when asked.
5. Set hours/minutes and tap **START TIMER**.
6. Leave the app and use any other app. The timer continues in a foreground service.
7. Use **CANCEL TIMER** from the app to cancel.

## Important Android limitations

- Device Admin permission is required for `lockNow()`. The app does not read your screen, messages, or other app content.
- Android/OEM battery managers can still restrict background work. For best reliability, allow AutoLock Timer to run in the background and disable battery optimization for it if your phone offers that option.
- The alarm is scheduled with `setAndAllowWhileIdle`; Android may defer idle alarms. The foreground service countdown is the primary path.
- After a reboot, Android starts the service again if a timer was active. A timer that expired while the phone was powered off locks when the boot receiver runs.
- Force-stopping the app from Android Settings, revoking Device Admin, or some aggressive OEM task killers can prevent automatic locking.
- This project is a starter implementation and should be tested on the target phone before relying on it for important time limits.

## Technical notes

- Package: `com.antorik.autolocktimer`
- Minimum Android: 8.0 (API 26)
- Target SDK: 35
- Foreground service type: `specialUse` with a declared subtype.
