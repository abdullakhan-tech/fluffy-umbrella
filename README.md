# Zaythar AI — Android Command Nexus

A native Android (Kotlin + Jetpack Compose) multilingual personal assistant with Google Sign-In, Cloud Firestore persistence, and Gemini model access through Firebase AI Logic.

## Current capabilities

- Google Sign-In authentication and UID-scoped Cloud Firestore data.
- Jetpack Compose command-center interface and animated neural visualizer.
- Multilingual AI chat through Firebase AI Logic.
- Speech recognition and Android TextToSpeech.
- Specialist workflows and shared user-scoped knowledge facts.
- Persona and appearance preferences stored with Jetpack DataStore.
- Firebase App Check integration and Firestore security rules.

The Image & Video Studio currently prepares creative briefs; it does not render media. Device control and real operating-system telemetry are not claimed capabilities.

## Quick start

1. Read [Android setup](docs/ANDROID_SETUP.md).
2. Register the Android app in Firebase using package name **com.aistudio.zaythar.zxtmkr**.
3. Add the Firebase-generated file at **app/google-services.json**.
4. Enable Google sign-in, Cloud Firestore, Firebase AI Logic (Gemini Developer API), and App Check.
5. Build with **./gradlew :app:assembleDebug** (Windows: **gradlew.bat :app:assembleDebug**).

**Gemini key note:** this app uses Firebase AI Logic. Do not embed a standalone Gemini API key in the Android app. Firebase manages Gemini Developer API authorization. See [Gemini configuration](docs/GEMINI_SETUP.md).

## Documentation

- [Android setup and run instructions](docs/ANDROID_SETUP.md)
- [Gemini / Firebase AI Logic setup](docs/GEMINI_SETUP.md)
- [Current project status and outstanding configuration](docs/PROJECT_STATUS.md)
- [AI behaviour and provider integration](AI.md)
- [Architecture](ARCHITECTURE.md)
- [API boundaries](API.md)
- [Security policy](SECURITY.md)
- [Project Wiki home](docs/wiki/Home.md)
- [Firestore security rules](firestore.rules)

## Architecture summary

- **UI:** Kotlin, Jetpack Compose, Material 3.
- **Identity:** Firebase Authentication with Google sign-in.
- **Cloud data:** Cloud Firestore with per-user paths and default-deny rules.
- **AI:** Firebase AI Logic Android SDK (com.google.firebase:firebase-ai).
- **Abuse protection:** Firebase App Check.
- **Local preferences:** Jetpack DataStore.

## Android project settings

- Application ID: **com.aistudio.zaythar.zxtmkr**
- Minimum Android version: API 26 (Android 8.0)
- Compile SDK: 36
- Java compatibility: 21

## Verification

Repository presence is not proof of a successful build or a configured cloud backend. See [project status](docs/PROJECT_STATUS.md) for the Firebase setup prerequisites and verification limitations.
