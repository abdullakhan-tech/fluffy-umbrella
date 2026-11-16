# Product Requirements Document

## Product

Zaythar AI is a multilingual, JARVIS-inspired native Android assistant built with Kotlin and Jetpack Compose.

## Current platform and architecture

- Android application using Jetpack Compose and Material 3.
- Firebase Authentication with Google sign-in.
- Cloud Firestore for authenticated, per-user conversations, memories, and knowledge facts.
- Firebase AI Logic for Gemini model access.
- Firebase App Check for abuse protection.
- Jetpack DataStore for local appearance and preference settings.
- Android SpeechRecognizer and TextToSpeech for voice input and output where supported.

## Product goals

- Provide a polished, accessible assistant interface on Android.
- Support multilingual conversational AI.
- Keep user data isolated by authenticated Firebase UID.
- Keep Gemini authorization out of the Android app package.
- Clearly distinguish implemented functionality from planned or visual-only features.

## Configuration prerequisites

- Register Android package com.aistudio.zaythar.zxtmkr in the intended Firebase project.
- Add Firebase-generated app/google-services.json.
- Enable Google sign-in, Cloud Firestore, Firebase AI Logic with Gemini Developer API, and Firebase App Check.
- Deploy and verify firestore.rules.

See docs/ANDROID_SETUP.md and docs/GEMINI_SETUP.md.

## Current limitations

- Firebase cloud setup and live Gemini calls are not verified by the presence of source code alone.
- A standalone Gemini API key is not required by the Firebase AI Logic Android path.
- Image & Video Studio prepares creative briefs; it is not a media rendering backend.
- Device control and real operating-system telemetry are not implemented claims.
- Cross-device data behaviour depends on the configured Firebase project and authenticated account.

## Acceptance criteria

- Android app builds with the documented JDK/SDK requirements.
- Google sign-in works with the registered application and signing fingerprints.
- Firestore rules prevent cross-user reads and writes.
- Gemini requests succeed through Firebase AI Logic with App Check enforced.
- Missing Firebase configuration produces an actionable setup failure.
- Unsupported voice APIs do not block text chat.
- Documentation reflects the shipped implementation and known limitations.
