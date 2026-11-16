# Architecture

## Current Android system

~~~text
Android app (Kotlin + Jetpack Compose)
  ├── Firebase Authentication (Google Sign-In)
  ├── Firebase AI Logic SDK
  │     └── Firebase AI Logic proxy → Gemini Developer API
  ├── Cloud Firestore (UID-scoped user data)
  ├── Firebase App Check
  └── Jetpack DataStore (local UI preferences)
~~~

## Android app

- app/src/main/java/com/example/MainActivity.kt: app entry point, App Check initialization, authentication gate, and authenticated ViewModel lifecycle.
- app/build.gradle.kts: Android application, Firebase SDK, Google Services plugin, and build configuration.
- app/src/main/AndroidManifest.xml: network and microphone permissions plus launcher activity.
- gradle/libs.versions.toml: dependency and plugin version catalog.

## Firebase services

- **Authentication:** Google sign-in; the signed-in Firebase UID is the identity boundary.
- **Firestore:** user data is stored under users/{userId} and its subcollections.
- **Security rules:** firestore.rules denies access by default and checks authenticated ownership.
- **AI:** Firebase AI Logic provides the client SDK and proxy path to Gemini models.
- **App Check:** protects AI Logic and Firebase resources from unauthorized clients.

## Secrets and configuration

The Android module expects the Firebase-generated app/google-services.json file. It is not currently present in the inspected default branch. Register the app with package name com.aistudio.zaythar.zxtmkr and follow docs/ANDROID_SETUP.md.

The Android Firebase AI Logic integration does not require a standalone Gemini API key in the app. Firebase manages Gemini Developer API authorization. Never embed a Gemini key in the APK.

## Local preferences and platform APIs

Jetpack DataStore stores local appearance and cognitive preferences. Android SpeechRecognizer and TextToSpeech provide voice input/output subject to OS support and runtime permission.

## Boundaries and limitations

- Firebase project setup, Google sign-in, App Check attestation, Firestore deployment, and live Gemini requests require external Firebase Console configuration.
- An Android source tree does not prove a successful APK build or real-device test.
- Creative Studio prepares prompts/briefs; it is not a media generation backend.
- Do not claim device control or live hardware telemetry unless separately implemented and permissioned.
