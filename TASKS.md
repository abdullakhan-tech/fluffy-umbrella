# Zaythar AI Roadmap

Items are marked complete only when implemented and verified. Source presence alone does not prove cloud configuration or real-device behaviour.

## Implemented foundations

- [x] Native Android Kotlin + Jetpack Compose application.
- [x] Google sign-in integration and authenticated app gate.
- [x] Firestore repositories and UID-scoped document model.
- [x] Firebase AI Logic Gemini integration.
- [x] Specialist-agent parallel execution and coordinator synthesis.
- [x] Bounded local calculator, UTC clock, unit converter, and JSON inspector.
- [x] Android speech recognition and TextToSpeech integration.
- [x] Neural Core visualizer with activity-aware states.
- [x] Gradle 9.3.1 wrapper for reproducible builds.
- [x] GitHub Actions Android unit-test and debug-APK workflow.
- [x] Version-controlled project Wiki and specialist agent guidance.

## Verification and hardening

- [ ] Obtain a successful build and unit-test run on the latest main commit.
- [ ] Run Firestore emulator security-rule tests and verify two-account isolation.
- [ ] Verify Google sign-in, App Check, and live Gemini requests in the real Firebase project.
- [ ] Add UI/instrumentation tests for voice, camera, and lifecycle.
- [ ] Add user-facing memory review, correction, export, and deletion.
- [ ] Add dependency and secret scanning to CI.

## Platform expansion

- [ ] Build and test a native Windows companion with a separately permissioned tool layer.
- [ ] Add cross-device memory workflows only after explicit account-scoped privacy review.
- [ ] Evaluate typed tool registry and action confirmation/audit UI.
- [ ] Add real-device performance and accessibility regression tests.

## Not shipped

- Native Windows desktop client.
- Always-on wake-word listener.
- Autonomous shell execution or unrestricted device control.
- Actual image/video rendering backend (the current Studio prepares creative briefs).
- Verified production Firebase deployment or real-device certification.