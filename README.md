# Zaythar AI — Android Command Nexus

A native Android (Kotlin + Jetpack Compose) rewrite of **Zaythar AI**, a JARVIS/FRIDAY-inspired multilingual personal assistant with a cybernetic command-center interface, interactive 3D neural visualizer, **Google Sign-In + Cloud Firestore** persistence, and **Firebase AI Logic** Gemini orchestration.

## Implemented Features

- **Google Sign-In & Auth Gate**: Authenticated session management via Jetpack `CredentialManager` (`GetSignInWithGoogleOption` interactive sign-in and `GetGoogleIdOption` silent auto-sign-in) gating all Cloud Firestore collections per user UID.
- **Overview & 3D Neural Core Visualizer**: Custom Compose `Canvas` 3D Fibonacci-sphere neural network with touch/drag parallax, animated orbital rings, system telemetry indicators (AI Logic & Cloud Firestore sync), session uptime counter, and a Command Deck launcher.
- **AI Assistant**: Real-time streaming multilingual chat (English, Urdu, Arabic, Hindi, French, Spanish, German, Chinese, Japanese) powered by **Firebase AI Logic** (`gemini-3.8-flash` / `gemini-3.5-flash-lite`) with persona-guided system instructions, safe local arithmetic & UTC time tool routing, Android `SpeechRecognizer` voice input (`RECORD_AUDIO`), Android `TextToSpeech` spoken replies, and real-time Cloud Firestore conversation sync.
- **Collaboration Nexus (Multi-Agent Orchestrator & Shared Knowledge Graph)**:
  - Interactive agent network diagram (AI Core + Research, Coding, Writing, Analysis, Creative specialists).
  - Concurrent multi-agent runner executing 1–4 selected specialists in parallel via Kotlin Coroutines (`async` / `awaitAll`) with individual failure isolation and Lead Coordinator synthesis.
  - Cloud Firestore Shared Knowledge Graph (`subject · predicate · object`) scoped per authenticated user with a 200-fact cap and injected into multi-agent runs.
- **Persona Builder & AI Cognitive Settings**: Persistent personality sliders, style selector, processing & visualizer controls, 6 cybernetic accent color swatches, Dark/Light theme toggle, and persistent 3D FX toggle (respecting system reduced-motion settings).
- **Model Selection Matrix**: Switch between **Hybrid Core** (`gemini-3.8-flash` with `gemini-3.5-flash-lite` fallback), **Gemini 3.8 Flash**, and **Gemini Flash-Lite**.
- **Image & Video Studio**: Creative prompt workspace that prepares structured creative briefs (no media rendering backend is connected).
- **Code Copilot & Prompt Nexus**: Copyable Kotlin Firebase AI snippet and searchable prompt templates that prefill the AI Assistant composer.
- **Knowledge Base**: User-scoped Cloud Firestore personal notes automatically injected into AI Assistant and Multi-Agent context.
- **User Profile & API Security**: Cloud-synced Operator display name, Google account details with Sign Out, and Firebase App Check + zero-trust Firestore rules telemetry.

## Architecture

- **UI**: Jetpack Compose + Material 3 (`Navigation Compose`, adaptive sidebar on tablets / drawer + bottom bar on phones, `Space Grotesk` & `Rajdhani` typography).
- **Authentication & Cloud Data**: Firebase Auth (Google Sign-In via `CredentialManager`), Cloud Firestore (`users/{userId}` profiles, `messages`, `memories`, and `knowledge_facts` subcollections) backed by zero-trust `firestore.rules`.
- **AI & Security**: `com.google.firebase:firebase-ai` + `FirebaseAppCheck` (`DebugAppCheckProviderFactory` in debug builds, `RecaptchaAppCheckProviderFactory` in release builds).
- **Local Preferences**: Jetpack DataStore Preferences for client-only UI theme, accent, motion, and cognitive tuning settings.
