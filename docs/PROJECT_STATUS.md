# Zaythar AI project status

This page records the verified state of the Android + Firebase workspace and distinguishes local build/emulator checks from live Play Services device checks.

## Current Android app implementation

The repository contains a native Android application built with:

- Kotlin and Jetpack Compose with Material Design 3 adaptive navigation (`Overview`, `AI Assistant`, `AI Brain`, `Camera Eyes`, `Plugin Plug Hub`, `Persona Builder`, `AI Settings`, `Collaboration Nexus`, `Model Matrix`, `Image & Video Studio`, `Code Copilot`, `Prompt Nexus`, `Knowledge Base`, `User Profile`, `API & Security`).
- Firebase Authentication with Google Sign-In (`CredentialManager`).
- Cloud Firestore (`users/{uid}/...`) with strict zero-trust security rules (`firestore.rules`) across chat sessions, messages, memories, knowledge graph facts, plugins, and neural brain snapshots.
- Firebase AI Logic (`Firebase.ai`) for Gemini streaming chat, structured-JSON Neural Brain synthesis, Camera Eyes vision, Code Copilot, Studio creative briefs, and concurrent Multi-Agent teams.
- Deterministic `LocalToolRouter` plugins, SSRF-hardened `WebConnector` grounding, Android speech/TTS, and Jetpack DataStore preferences.

The `app` module uses application ID **com.aistudio.zaythar.zxtmkr**, `minSdk 26`, `compileSdk 36`, `targetSdk 36`, and Java 21 compatibility.

## Provisioned & external Firebase configuration

- `app/google-services.json`, `app/src/main/res/values/firebase_applet_config.xml`, and `firestore.rules` are configured for Firebase project `gen-lang-client-0659731043` (`asia-east1`) and named database `ai-studio-android-fluffyum-16d0226b-744f-4389-a061-b5bf3e77239a`.
- Production rollout still requires ensuring Google Sign-In OAuth client IDs, debug/release SHA-1/SHA-256 fingerprints, Play Integrity App Check, and Firebase AI Logic API enablement are active in the Firebase Console.
- Firebase AI Logic requires **no** client-side `GEMINI_API_KEY` in the APK.

## Verification status

Verified in the local build environment:
- Debug and Release APK compilation (`gradle :app:assembleDebug :app:assembleRelease`).
- Android Lint analysis (`gradle :app:lintDebug`).
- Local JVM + Robolectric Compose UI test suite (`gradle :app:testDebugUnitTest`).
- Firebase Emulator Auth + Firestore security rules suites (`firestore.test.js` and `FirestoreRepositoryRuleTest` / `ChatSessionPersistenceTest`).
- GitHub Actions Android CI workflow configured in `.github/workflows/android-ci.yml`.

Not directly executable in a headless build container (no ADB/physical device):
- Live end-to-end Google Sign-In interactive credential sheet, live Gemini network requests, hardware camera capture, and Play Integrity App Check attestation.
