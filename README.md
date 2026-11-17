# Zaythar AI — Android Command Nexus

A native Android (Kotlin + Jetpack Compose) command center powered by **Google Sign-In + Cloud Firestore** persistence, **Firebase AI Logic** (`gemini-3.8-flash` / `gemini-3.5-flash-lite`) streaming & structured JSON inference, **Live Internet Grounding**, **Camera Eyes Vision**, **AI Brain**, and an extensible **Plugin Plug Hub**.

## Implemented Features

- **Google Sign-In & Auth Gate**: Authenticated session management via Jetpack `CredentialManager` (`GetSignInWithGoogleOption` interactive sign-in and `GetGoogleIdOption` silent auto-sign-in) gating all Cloud Firestore collections per user UID.
- **Network Intelligence**: Live Android transport/internet validation, metered/roaming state, and optional 2G/3G/4G/5G reporting using the device telephony API. See [Network Intelligence](docs/NETWORK_INTELLIGENCE.md) for permissions and carrier limitations.
- **Overview & 3D Neural Core Visualizer**: Custom Compose `Canvas` 3D Fibonacci-sphere neural network with touch/drag parallax, animated orbital rings, activity-aware Ready/Listening/Thinking states, live system telemetry indicators (Firebase AI Logic, LLM Web Grounding, Active Plugins, and session uptime), and a Command Deck launcher.
- **Multi-Session Chat History & AI Assistant**:
  - Persistent multi-thread Cloud Firestore chat sessions (`/users/{userId}/chat_sessions/{sessionId}/messages/{messageId}`) with thread creation, switching, renaming, deletion, and automatic thread titling from the first prompt.
  - Real-time streaming multilingual chat (English, Urdu, Arabic, Hindi, French, Spanish, German, Chinese, Japanese) with Android `SpeechRecognizer` voice input (`RECORD_AUDIO`) and Android `TextToSpeech` spoken replies.
- **LLM Connect Internet (Live Web Grounding)**:
  - Persistent `WEB: ON / OFF` chip in Chat backed by Jetpack DataStore.
  - Combines Firebase AI Logic `Tool.googleSearch()` with an SSRF-hardened `https://` `WebConnector` (blocking private, loopback, link-local, and non-HTTPS targets) and DuckDuckGo / Wikipedia public lookups, surfacing clickable verified source links under assistant messages.
- **AI Brain & Neural Functions**:
  - Dedicated `NeuralBrainScreen` running 4 cognitive modes (**Reasoner**, **Memory Consolidator**, **Strategic Planner**, **Cognitive Diagnostics**) using strict Firebase AI Logic `Schema.obj(...)` structured JSON output.
  - Persists cognitive snapshots to Cloud Firestore (`brain_runs`) and supports one-tap committing extracted triples to the Shared Knowledge Graph.
- **Plugin Plug Hub**:
  - Cloud-synced extensible capability slots (`/users/{userId}/plugins/{pluginId}`) for toggling built-in local tools (`calculator`, `utc_clock`, `unit_converter`, `json_inspector`) and plugging custom declarative directive modules—complete with an on-device Plugin Sandbox evaluator (zero Dynamic Code Loading).
- **Camera Eyes (Multimodal Vision)**:
  - Live photo capture via `ActivityResultContracts.TakePicture` + `FileProvider` and zero-permission Android Photo Picker (`PickVisualMedia.ImageOnly`), off-UI-thread downscaling (`<= 1280px`), 4 vision inspection modes (**Scene & Object Scan**, **OCR & Document Extract**, **UI / Code Debug**, **Compare & Safety Check**), and automatic saving of results to the active chat thread.
- **Code Copilot & Media Studio**:
  - Interactive streaming **Code Copilot** workbench supporting Kotlin, Python, TypeScript, SQL, and Bash across Generate, Debug, Refactor, Unit Tests, and Security Audit modes.
  - Live Gemini-powered **Image & Video Studio** art direction & storyboard generator.
- **Collaboration Nexus & Knowledge Base**:
  - Concurrent Multi-Agent runner executing 1–4 specialists in parallel via Kotlin Coroutines (`async` / `awaitAll`), per-agent failure isolation, configured Gemini model fallback, and Lead Coordinator synthesis, plus Cloud Firestore Notes (`memories`) and Knowledge Graph (`knowledge_facts`).

## Architecture

- **UI**: Jetpack Compose + Material 3 (`Navigation Compose`, adaptive sidebar on tablets / drawer + bottom bar on phones, `Space Grotesk` & `Rajdhani` typography).
- **Authentication & Cloud Data**: Firebase Auth (Google Sign-In via `CredentialManager`), Cloud Firestore (`users/{userId}` profiles, `chat_sessions`, `messages`, `plugins`, `brain_runs`, `memories`, and `knowledge_facts` subcollections) backed by zero-trust `firestore.rules`.
- **AI & Security**: `com.google.firebase:firebase-ai` + `FirebaseAppCheck` (`DebugAppCheckProviderFactory` in debug builds, `RecaptchaAppCheckProviderFactory` in release builds).
- **Local Preferences**: Jetpack DataStore Preferences for client-only UI theme, accent, motion, cognitive tuning, and Internet grounding settings.

## Quick start

1. Install JDK 21 and Android SDK Platform 36.
2. Configure your Firebase project and place its generated `google-services.json` at `app/google-services.json` (never commit it).
3. Build and test:

Linux/macOS:
```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Windows PowerShell:
```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

See [Android setup](docs/ANDROID_SETUP.md) for Firebase, sign-in, App Check, and installation details. The Windows wrapper supports building the Android APK; a native Windows assistant client is not yet implemented.

## Project handbook

- [Version-controlled Wiki](docs/wiki/Home.md)
- [JARVIS execution and safety model](docs/JARVIS_EXECUTION_MODEL.md)
- [Android setup](docs/ANDROID_SETUP.md)
- [Gemini / Firebase setup](docs/GEMINI_SETUP.md)
- [Current project status](docs/PROJECT_STATUS.md)
- [Specialist agent profiles](.github/agents/)
