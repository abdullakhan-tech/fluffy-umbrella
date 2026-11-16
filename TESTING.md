# Testing guide

## Android unit tests

Requirements: JDK 21, Android SDK Platform 36, and the Firebase-generated local `app/google-services.json` configuration for local Gradle builds.

Linux/macOS:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

## Firebase Emulator Suite

The repository includes `firestore.test.js`, `firestore.rules`, and `firebase.json`. Install Node.js and Firebase CLI, then run the Firestore rules tests against the configured emulator. The Android CI workflow starts Auth and Firestore emulators and runs the Android unit-test and debug-APK tasks.

## What tests cover

- Local calculator parsing, UTC time, unit conversion, and JSON inspection.
- Web connector validation and safe URL handling.
- Firestore repository and security-rule behaviour where emulator-backed tests are configured.

## Live integration checks (manual)

CI uses a temporary Firebase compile configuration; it is not a real Firebase project and cannot verify live services. On a configured device/emulator, verify:

1. Google sign-in and sign-out.
2. Firestore read/write isolation using two different accounts.
3. Gemini chat and multi-agent specialist completion through Firebase AI Logic.
4. App Check debug and release attestation.
5. Microphone permission denial, speech recognition, interruption, and text fallback.
6. Camera/photo picker flows and plugin permissions.

Never put live API keys or real user data in CI logs. Report only tests actually run; CI compilation is not proof of production Firebase readiness.