# Getting started

## Requirements

- Android Studio with a compatible Android SDK (the project targets compile SDK 36).
- JDK 21.
- A Firebase project with the Android app registered as `com.aistudio.zaythar.zxtmkr`.
- A device or emulator running Android 8.0 (API 26) or newer.

## Configure Firebase

1. Follow [Android setup](../ANDROID_SETUP.md).
2. Download the Firebase-generated `google-services.json` for the exact application ID.
3. Place it at `app/google-services.json` locally. Do not commit credentials or private configuration files.
4. Enable Google Sign-In, Cloud Firestore, Firebase AI Logic with the Gemini Developer API, and Firebase App Check.
5. Review and deploy the repository's Firestore rules to the matching Firebase project.

## Build

Linux/macOS:
```bash
./gradlew :app:assembleDebug
```

Windows:
```powershell
.\gradlew.bat :app:assembleDebug
```

Install the resulting debug APK from the app module's build outputs. A successful local build does not prove that sign-in, Gemini, Firestore, or App Check are configured correctly.

## Gemini credentials

The Android client uses Firebase AI Logic. Do not hard-code a Gemini API key in Kotlin, resources, BuildConfig, or the APK. See [Gemini setup](../GEMINI_SETUP.md).
