# Zaythar AI project status

This page distinguishes repository implementation from external service configuration and real-device verification.

## Current Android application

The default branch contains a native Android application using:

- Kotlin and Jetpack Compose.
- Firebase Authentication with Google sign-in.
- Cloud Firestore with per-user document paths and repository security rules.
- Firebase AI Logic for Gemini model access (Hybrid route: Gemini 4 Hydrogen primary with Gemini 3.8 Flash and Gemini 3.5 Flash-Lite fallbacks; model access still requires the user's Firebase project setup).
- Android speech recognition and TextToSpeech APIs.
- Jetpack DataStore for local preferences.
- Parallel specialist-agent execution with coordinator synthesis and bounded model fallback.
- Activity-aware Compose neural orb (Ready, Listening, Thinking, and TextToSpeech-callback-driven Speaking state).
- Native cyber-command UI across existing settings, persona, model routing, profile, diagnostics, collaboration, and coding screens; profile telemetry reads Android OS RAM/storage values.
- Overview dashboard includes a custom animated red-neon agent-topology visualization connected to the existing reduced-motion preference; the topology represents app modules and does not falsely report agent execution.
- Media Studio image generation through Firebase AI Logic Nano Banana 2.1 (16:9, 1K) with gallery export on Android 10+; video output is a Gemini-generated storyboard/shot list, not a rendered video file.
- Local calculator, UTC clock, unit converter, JSON inspector, camera/photo picker, and plugin hub.

Application ID: `com.aistudio.zaythar.zxtmkr`. Minimum Android API 26, compile SDK 36, Java 21.

## Build tooling

- Official Gradle 9.3.1 wrapper is committed (`gradlew`, `gradlew.bat`, wrapper JAR and properties).
- Main-branch GitHub Actions workflow runs Firestore Emulator security-rule tests, Android unit tests, and assembles a debug APK.
- The workflow uses a temporary compile-only Firebase configuration; it is not a usable Firebase project credential.
- CI uploads the debug APK only after tests and assembly succeed.

## Configuration required outside the repository

- Register the Android app in the intended Firebase project.
- Add the matching `app/google-services.json` locally.
- Enable Google sign-in and register signing fingerprints.
- Enable Cloud Firestore and deploy the reviewed `firestore.rules`.
- Enable Firebase AI Logic with the Gemini Developer API.
- Configure Firebase App Check for debug and production builds.

The Android Firebase AI Logic integration does not require a standalone Gemini API key.

## Platform status

- Android: native app source, Gradle wrapper, and CI build workflow are present.
- Windows: Gradle wrapper can build from Windows with JDK/Android SDK installed, but a native Windows desktop assistant is not implemented.
- Termux: wrapper scripts are POSIX-compatible; a full Android SDK build toolchain on-device is not guaranteed. Android Studio or CI is recommended for APK builds.

## Verification status

The main-branch Android workflow for the latest neural-orb and wrapper updates is pending/running. A previous workflow run on an older CI branch passed, but it does not verify the current main commit.

A successful CI compile does not prove Google sign-in, live Gemini requests, App Check attestation, deployed Firestore rules, or real-device behaviour. Verify those against the intended Firebase project and physical Android device before release.

## Known outstanding work

- Confirm latest main-branch build and unit-test run passes.
- Confirm Firestore Emulator Suite rule tests pass in CI and verify two-account isolation against the intended Firebase project.
- Verify live Gemini multi-agent execution and provider model availability with the real Firebase project.
- Verify Nano Banana 2.1 image generation quotas/model access and gallery export with the production Firebase App Check configuration.
- Complete user-facing memory review/export/deletion controls.
- Verify the neural orb's TextToSpeech Speaking state on physical devices and across TTS engines.
- Build and test a native Windows companion separately.

Older root documentation describing a browser/FastAPI/OpenAI design has been reconciled to the native Android architecture; do not deploy the old backend unless it is deliberately restored and independently tested.
