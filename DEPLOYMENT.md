# Build and deployment

## Current deliverable

The default branch contains a native Android application built with Kotlin and Jetpack Compose. There is no deployable FastAPI web backend or Windows desktop application in the current product path.

## Requirements

- JDK 21
- Android SDK Platform 36 and Build Tools 36
- Android Studio (recommended) or command-line Android SDK tools
- Firebase project configured for package `com.aistudio.zaythar.zxtmkr`

## Configure Firebase

Follow [Android setup](docs/ANDROID_SETUP.md) and [Gemini setup](docs/GEMINI_SETUP.md). Place the Firebase-generated configuration at `app/google-services.json` locally. Never commit Firebase service-account credentials, signing keys, or standalone Gemini API keys.

## Build

Linux/macOS/Termux with compatible Android SDK:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`. For a release, configure a private release signing key outside source control, review App Check release attestation, and build the release variant only after Firebase configuration is verified.

## Firebase deployment

Review `firestore.rules` and verify the target Firebase project before running:

```bash
firebase projects:list
firebase deploy --only firestore:rules --project YOUR_FIREBASE_PROJECT_ID
```

## Platform status

- Android: native application source and CI build workflow are present.
- Windows: a native desktop client is not implemented yet. The Gradle wrapper works on Windows, but it does not turn the Android app into a Windows application.
- Termux: the wrapper can be invoked from a compatible Termux/JDK/Android-SDK environment; a full Android SDK toolchain on-device may be impractical. Android Studio or CI is the recommended APK build environment.

A successful CI build does not prove Google sign-in, live Gemini requests, App Check, or real-device behaviour.