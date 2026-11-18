# Changelog

## Current Android Release
- Migrated Zaythar AI to native Android (Kotlin + Jetpack Compose + Material 3) targeting SDK 36 (`com.aistudio.zaythar.zxtmkr`).
- Added Firebase Authentication (Google Sign-In via Credential Manager), Cloud Firestore user-isolated persistence, and Firebase App Check.
- Integrated Firebase AI Logic (`Firebase.ai`) across AI Assistant, AI Brain (structured JSON), Camera Eyes (multimodal vision), Code Copilot, Image & Video Studio, and Multi-Agent Collaboration Nexus.
- Added multi-thread chat history drawer, Plugin Plug Hub (`LocalToolRouter` + custom cloud directives), and SSRF-hardened `WebConnector` internet grounding.
- Modularized repository and Compose UI files under 500 lines and expanded Robolectric UI, logic, and Firestore emulator test coverage.
- Added Hybrid BM25 + Knowledge Graph RAG (`RagRetrievalEngine`), 2-layer synaptic `NeuralCoreVisualizer`, and local Guest Mode (`GuestWorkspaceStore`).
- Hardened startup/runtime stability (App Check lookup, DataStore corruption recovery, `requireUserId()` inside `Result` guards, safe snapshot parsing, zero-size Compose Canvas guards, TTS binder safety, ProGuard keep rules) and added GitHub Actions Android CI (`.github/workflows/android-ci.yml`).
