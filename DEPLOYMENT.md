# Deployment

## Requirements
- JDK 21 and Android SDK (API 36 `compileSdk` / `targetSdk`, `minSdk` 26)
- Firebase project (`gen-lang-client-0659731043`) with Google Sign-In, Cloud Firestore, Firebase AI Logic, and Firebase App Check enabled
- `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` configured for package `com.aistudio.zaythar.zxtmkr`

## Local Verification & Build

1. Start local Firebase emulators (`auth` on `9099`, `firestore` on `8085`) and run the security rules suite:
   ```bash
   FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
   ```
2. Run JVM + Robolectric Compose UI + Firestore emulator tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
3. Run Android Lint and build the debug APK:
   ```bash
   gradle :app:lintDebug :app:assembleDebug
   ```

## Production Checklist
- Register both debug and release SHA-1 / SHA-256 signing fingerprints in Firebase Console so Google Sign-In and App Check Play Integrity validate on physical devices.
- Deploy `firestore.rules` to the configured named Firestore database (`ai-studio-android-fluffyum-16d0226b-744f-4389-a061-b5bf3e77239a`).
- Enforce Firebase App Check for Cloud Firestore and Firebase AI Logic in Firebase Console.
- Never commit release keystores or `.env` secrets to Git.
