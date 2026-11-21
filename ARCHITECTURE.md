# Architecture

## Current Android system

```text
Android app (Kotlin + Jetpack Compose)
  ├── Firebase Authentication (Google Sign-In via Credential Manager)
  ├── Firebase AI Logic SDK (chat, AI Brain JSON, Camera Eyes vision, Code Copilot, Nano Banana Image Studio, Multi-Agent)
  │     └── Firebase AI Logic proxy → Gemini Developer API
  ├── SmartEngineRouter (intent auto-router & all-engine verification suite)
  ├── WebConnector (guarded HTTPS grounding + SSRF protection)
  ├── LocalToolRouter (calculator, UTC clock, extended units, JSON inspector, SHA-256/Base64/UUID, Hex/RGB, text stats)
  ├── Cloud Firestore (UID-scoped user sessions, messages, notes, triples, plugins, brain runs)
  ├── Firebase App Check (Play Integrity / Debug provider)
  └── Jetpack DataStore (local UI, theme, and neural preferences)
```

## Android app modules

- `app/src/main/java/com/example/MainActivity.kt`: app entry point, App Check initialization, Google Sign-In gate, and authenticated ViewModel lifecycle.
- `app/src/main/java/com/example/ui/ZaytharApp.kt` & `ZaytharNavigation.kt`: adaptive NavigationSuite / Rail / Drawer scaffold and Navigation Compose routing across 17 destinations (including `engine_hub` and `voice_audio`).
- `app/src/main/java/com/example/ui/components/ZaytharIcons.kt`: custom Compose `Canvas` hexagonal emblem (`ZaytharHexEmblem`), `CyberIconBadge`, and Material `Icons.Rounded.*` mappings.
- `app/src/main/java/com/example/ui/ZaytharViewModel.kt`, `NexusWorkspaceViewModel.kt` & `VoiceAudioController.kt`: state coordination for multi-thread chat, AI Engine Hub diagnostics, Live Voice (`gemini-3.8-live`), Audio Transcribe (`gemini-3.5-transcribe`), AI Brain, Camera Eyes, Plugin Hub, Code Copilot, Image Studio, and Collaboration Nexus.
- `app/src/main/java/com/example/domain/AiRepository.kt`, `AiWorkspaceCapabilities.kt`, `AiVisionAndMediaEngine.kt`, `AiVoiceAndAudioEngine.kt` & `SmartEngineRouter.kt`: streaming Gemini chat (`AUTO`, `HYBRID`, `FLASH`, `ARGON`, `LITE`, `HYDROGEN`, `CARBON`, `NEON`, `FLASH_LITE_31`), Google Search & Google Maps Grounding (`Tool.googleSearch()`, `Tool.googleMaps()`), Live bidirectional audio, recorded audio transcription, structured-JSON Neural Brain, Camera Eyes vision, Nano Banana 2.1 image generation & editing, engine diagnostics, and Multi-Agent team orchestration.
- `app/src/main/java/com/example/domain/WebConnector.kt` & `LocalToolRouter.kt`: SSRF-hardened HTTPS fetcher (`WebConnector`) and local deterministic tool plugins (`LocalToolRouter`).
- `app/src/main/java/com/example/data/remote/FirestoreRepository.kt` & `FirestoreSessionAndBrainStore.kt`: UID-scoped Cloud Firestore persistence (`users/{uid}/...`).
- `app/build.gradle.kts` & `gradle/libs.versions.toml`: Android Gradle build and dependency version catalog.
- `app/src/main/AndroidManifest.xml` & `res/xml/file_paths.xml`: network, microphone, and `FileProvider` configuration for Photo Picker / camera capture.

## Firebase services

- **Authentication:** Google sign-in via Credential Manager; the signed-in Firebase UID is the identity boundary.
- **Firestore:** user data is stored under `users/{userId}` and its subcollections (`chatSessions`, `messages`, `memoryNotes`, `knowledgeFacts`, `plugins`, `brainRuns`) in named database `ai-studio-android-fluffyum-16d0226b-744f-4389-a061-b5bf3e77239a`.
- **Security rules:** `firestore.rules` denies access by default and enforces authenticated UID ownership and bounded field validation.
- **AI:** Firebase AI Logic provides the client SDK and proxy path to Gemini models (`gemini-4-hydrogen`, `gemini-4-carbon`, `gemini-4-neon`, `gemini-4-argon`, `gemini-3.8-flash`, `gemini-3.8-live`, `gemini-3.5-transcribe`, `gemini-3.5-flash-lite`, `gemini-3.1-flash-lite`, and `gemini-nano-banana-2.1`).
- **App Check:** `AppCheckInit` configures reCAPTCHA Enterprise (release, when configured) or Debug App Check (debug builds) to protect AI Logic and Firebase resources.

## Secrets and configuration

- `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` are provisioned for Firebase project `gen-lang-client-0659731043`.
- The Android Firebase AI Logic integration does not require a standalone Gemini API key in the APK. Firebase manages Gemini Developer API authorization server-side. Never embed an API key in source code or commits.

## Local preferences and platform APIs

Jetpack DataStore stores local appearance and cognitive preferences. Android `SpeechRecognizer`, `TextToSpeech`, Photo Picker (`PickVisualMedia`), MediaStore image saving, and `TakePicture` + `FileProvider` provide voice, camera, and gallery capabilities subject to device support and runtime permission.

## Boundaries and limitations

- Live Gemini API calls, Play Integrity attestation, and interactive Google Sign-In require Firebase Console setup and a real device or Google Play–enabled emulator.
- The cloud build container runs unit, Robolectric Compose UI, Android Lint, and APK assembly, but has no live ADB/emulator hardware for camera/microphone or interactive sign-in tests.
- Image Studio generates and edits still 1K images via `gemini-nano-banana-2.1` (requiring Firebase Blaze plan) and generates creative video storyboards via Gemini text models; it is not an external video rendering backend.
