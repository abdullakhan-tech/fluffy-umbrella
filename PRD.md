# Product Requirements Document

## Product

Zaythar AI is a multilingual, JARVIS-inspired native Android assistant built with Kotlin and Jetpack Compose.

## Current platform and architecture

- Android application using Jetpack Compose and Material 3 (`minSdk 26`, `compileSdk 36`, `targetSdk 36`).
- Firebase Authentication with Google Sign-In (`CredentialManager`).
- Cloud Firestore for authenticated, per-user chat sessions, messages, memories, knowledge graph facts, plugins, and AI Brain snapshots.
- Firebase AI Logic (`Firebase.ai`) for streaming chat, structured-JSON Neural Brain synthesis, Camera Eyes multimodal vision, Code Copilot, Studio creative briefs, and concurrent Multi-Agent orchestration.
- Guarded `WebConnector` (SSRF-validated HTTPS grounding) and `LocalToolRouter` deterministic plugins (calculator, UTC clock, unit converter, JSON inspector).
- Firebase App Check for abuse protection.
- Jetpack DataStore for local appearance and cognitive preferences.
- Android `SpeechRecognizer`, `TextToSpeech`, Photo Picker, and camera capture (`FileProvider`) where supported by the device.

## Product goals

- Provide a polished, accessible, responsive assistant interface across phones, foldables, and tablets.
- Support multilingual conversational AI, visual inspection, and personal knowledge persistence.
- Keep user data strictly isolated by authenticated Firebase UID.
- Keep Gemini authorization out of the Android APK.
- Clearly distinguish implemented functionality from planned or console-dependent features.

## Configuration prerequisites

- Firebase project `gen-lang-client-0659731043` with Android package `com.aistudio.zaythar.zxtmkr` and signing SHA fingerprints registered.
- `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` aligned with the provisioned project and named Firestore database.
- Google Sign-In, Cloud Firestore (`firestore.rules`), Firebase AI Logic (Gemini Developer API), and Firebase App Check enabled in Firebase Console.

See `docs/ANDROID_SETUP.md` and `docs/GEMINI_SETUP.md`.

## Current limitations

- Live Gemini calls, Google Sign-In, and hardware camera/microphone capture cannot run in a headless container without a Google Play–enabled device or streaming emulator.
- A standalone Gemini API key is not required or embedded in the Firebase AI Logic Android build.
- Image & Video Studio generates art direction concepts and storyboards via Gemini; it is not an external image/video rendering pipeline.
- Hardware system telemetry cards reflect application/provider readiness and session uptime, not OS-level kernel telemetry.

## Acceptance criteria

- Android debug APK compiles cleanly (`gradle :app:assembleDebug`) and passes Android Lint error checks.
- Local JVM, Robolectric Compose UI, and Firebase Emulator Firestore rules tests pass.
- Google sign-in works with registered SHA-1/SHA-256 fingerprints on Play Services devices.
- Firestore rules deny unauthenticated and cross-user reads/writes.
- Unsupported voice or camera hardware degrades gracefully without blocking text chat or photo picking.
