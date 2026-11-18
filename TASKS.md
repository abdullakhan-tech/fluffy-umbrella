# Roadmap

## Foundation (Implemented)
- [x] Native Kotlin + Jetpack Compose Material 3 adaptive UI across 16 workspace screens
- [x] AI Engine Hub (`EngineHubScreen`) with `SmartEngineRouter` intent classification and All-Engine Verification Suite
- [x] Google Sign-In (`CredentialManager`) + Firebase Authentication identity boundary
- [x] Firebase AI Logic (`Firebase.ai`) integration (`3.8-flash`, `3.5-flash-lite`, `4-argon`, `nano-banana-2.1`) with App Check support
- [x] Multi-session persistent chat history, memories, and knowledge graph facts in Cloud Firestore
- [x] AI Brain structured-JSON synthesis (`Neural Reasoner`, `Memory Consolidator`, `Strategic Planner`, `Cognitive Diagnostics`)
- [x] Camera Eyes multimodal vision (Photo Picker + `FileProvider` camera capture + Save-to-RAG)
- [x] Plugin Plug Hub with built-in deterministic tools (`LocalToolRouter`: math, UTC, extended units, JSON, SHA-256/Base64/UUID, Hex/RGB, text stats)
- [x] SSRF-hardened `WebConnector` live HTTPS web grounding + Gemini Search grounding
- [x] Concurrent Multi-Agent Collaboration Nexus, Code Copilot, and Nano Banana 2.1 Image Generation & Editing Studio
- [x] Upgraded vector iconography (`ZaytharHexEmblem`, `CyberIconBadge`, `ZaytharIcons` Material Rounded)
- [x] Local Jetpack DataStore visual, theme, accent, and 3D motion preferences with corruption recovery
- [x] Local Guest Mode workspace (`GuestWorkspaceStore`) with strict Firebase isolation
- [x] Startup crash hardening across App Check, Credential Manager auto sign-in, Firestore snapshots, TTS, and zero-size Compose Canvas passes

## Automated Verification & CI
- [x] Local JVM & Robolectric Compose UI unit test suite (`gradle :app:testDebugUnitTest`)
- [x] Zero-trust Firebase Firestore & Auth emulator rules suites (`firestore.test.js` & JVM rule tests)
- [x] Android Lint, Debug APK, and Release APK assembly (`gradle :app:lintDebug :app:assembleDebug :app:assembleRelease`)
- [x] GitHub Actions Android CI workflow (`.github/workflows/android-ci.yml`)

## Release & Console Readiness
- [ ] Verify Google Sign-In, Play Integrity App Check, and live Gemini responses on a physical Google Play device
- [ ] Publish signed release APK/AAB after registering production SHA-1/SHA-256 certificates in Firebase Console
- [ ] Optional user data export workflow and vector/embedding semantic retrieval
