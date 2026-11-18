# Zaythar AI — Android Command Nexus

A native Android (Kotlin + Jetpack Compose) command center powered by **Google Sign-In + Cloud Firestore** persistence, **Firebase AI Logic** (`gemini-3.8-flash`, `gemini-3.5-flash-lite`, `gemini-4-argon`, and `gemini-nano-banana-2.1`) streaming, image generation, & structured JSON inference, **Smart Auto-Router**, **Live Internet Grounding**, **Camera Eyes Vision**, **AI Brain**, and an extensible **Plugin Plug Hub**.

## Implemented Features

- **AI Engine Nexus & Smart Auto-Router**: Dedicated `EngineHubScreen` featuring a deterministic prompt intent classifier (`SmartEngineRouter`) across 8 Zaythar engines, dynamic Gemini tier routing (`AUTO`, `HYBRID`, `FLASH`, `ARGON`, `LITE`), and an **All-Engine Verification Suite** running local deterministic checks + optional live `gemini-3.8-flash` connectivity probes.
- **Google Sign-In & Guest Mode**: Authenticated session management via Jetpack `CredentialManager` (`GetSignInWithGoogleOption` & `GetGoogleIdOption`) gating all Cloud Firestore collections per user UID, plus isolated local device **Guest Mode**.
- **Overview & 3D Neural Core Visualizer**: Custom Compose `Canvas` 3D Fibonacci-sphere neural network, custom hexagonal `ZaytharHexEmblem` & `CyberIconBadge` vector iconography, live system telemetry, and a Command Deck launcher.
- **Multi-Session Chat History & AI Assistant**:
  - Persistent multi-thread Cloud Firestore chat sessions (`/users/{userId}/chat_sessions/{sessionId}/messages/{messageId}`) with thread creation, switching, renaming, deletion, and automatic thread titling.
  - Real-time streaming multilingual chat with Android `SpeechRecognizer` voice input (`RECORD_AUDIO`) and Android `TextToSpeech` spoken replies.
- **LLM Connect Internet (Live Web Grounding)**:
  - Combines Firebase AI Logic `Tool.googleSearch()` with an SSRF-hardened `https://` `WebConnector` and DuckDuckGo / Wikipedia public lookups.
- **AI Brain & Neural Functions**:
  - Dedicated `NeuralBrainScreen` running 4 cognitive modes (**Reasoner**, **Memory Consolidator**, **Strategic Planner**, **Cognitive Diagnostics**) using strict Firebase AI Logic `Schema.obj(...)` structured JSON output and Knowledge Graph triple extraction.
- **Plugin Plug Hub & Deterministic Local Engines**:
  - Cloud-synced extensible capability slots (`/users/{userId}/plugins/{pluginId}`) for `calculator`, `utc_clock`, `unit_converter` (`km/mi`, `m/ft`, `cm/in`, `kg/lb`, `c/f`, `kb/mb/gb/tb`, `ms/s/min/hr`), `json_inspector`, `crypto_hash_uuid` (SHA-256, Base64, UUID v4), `color_text_metrics` (Hex/RGB, word/token stats), and `web_grounding`.
- **Camera Eyes (Multimodal Vision)**:
  - Live photo capture via `ActivityResultContracts.TakePicture` + `FileProvider` and zero-permission Android Photo Picker (`PickVisualMedia.ImageOnly`), 4 vision inspection modes, chat thread sync, and one-tap **Save to RAG Memory**.
- **Image Studio (`gemini-nano-banana-2.1`) & Code Copilot**:
  - Runtime AI text-to-image generation and Photo Picker image-to-image editing (`1:1`, `16:9`, `9:16`) with MediaStore gallery export, paired with Creative Storyboard Briefs.
  - Interactive streaming **Code Copilot** workbench supporting Kotlin, Python, TypeScript, SQL, and Bash.
- **Collaboration Nexus & Hybrid RAG Knowledge Base**:
  - Concurrent Multi-Agent runner executing 1–4 specialists in parallel via Kotlin Coroutines (`async` / `awaitAll`) with Lead Coordinator synthesis, plus Hybrid BM25 + Bigram + Graph RAG retrieval.

## Architecture

- **UI**: Jetpack Compose + Material 3 (`Navigation Compose`, adaptive sidebar on tablets / drawer + bottom bar on phones, `Space Grotesk` & `Rajdhani` typography).
- **Authentication & Cloud Data**: Firebase Auth (Google Sign-In via `CredentialManager`), Cloud Firestore (`users/{userId}` profiles, `chat_sessions`, `messages`, `plugins`, `brain_runs`, `memories`, and `knowledge_facts` subcollections) backed by zero-trust `firestore.rules`.
- **AI & Security**: `com.google.firebase:firebase-ai` + `FirebaseAppCheck` (`DebugAppCheckProviderFactory` in debug builds, `RecaptchaAppCheckProviderFactory` in release builds).
- **Local Preferences**: Jetpack DataStore Preferences for client-only UI theme, accent, motion, cognitive tuning, and Internet grounding settings.
