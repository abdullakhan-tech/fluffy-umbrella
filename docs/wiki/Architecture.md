# Architecture

## Current application stack

- Kotlin and Jetpack Compose for the native Android application.
- Firebase Authentication for Google sign-in.
- Cloud Firestore for account-scoped chat, memory, and knowledge data.
- Firebase AI Logic Android SDK for Gemini inference.
- Android speech recognition and TextToSpeech APIs.
- Jetpack DataStore for local preferences.

## Main boundaries

- **UI layer:** Compose screens and ViewModels expose state and user actions.
- **Domain layer:** assistant orchestration, provider routing, local deterministic tools, specialist-agent coordination, and the typed tool-runtime foundation.
- **Data layer:** preferences and Firestore repositories.
- **Platform layer:** Android speech, lifecycle, permissions, and app integration.

## Tool execution boundary

The original Kotlin tool registry is an allowlist for adapters registered by application code. It validates typed arguments, checks the authenticated execution context and required Android permissions, requires approval for non-read-only actions, enforces timeouts, propagates cancellation, and emits audit metadata without raw user content.

The registry is not yet wired to the chat model loop, Android permission launcher, Plugin Hub UI, or Firestore audit persistence. Do not treat model output as approval and do not execute arbitrary scripts, shell commands, or unrestricted intents. See [OpenClaw adaptation](../OPENCLAW_ADAPTATION.md).

## Identity and data flow

The authenticated Firebase UID is the ownership boundary for user data. Repository operations and Firestore rules must both enforce that boundary. Never trust a UID supplied by model output or user-controlled tool arguments as proof of access.

## AI request flow

1. Collect the current user message and a bounded recent history.
2. Build the assistant instruction and relevant account-scoped context.
3. Select the configured provider route.
4. Stream model output to the conversation UI.
5. Persist messages and memory only through the authenticated repository boundary.

For future tools, use the typed allowlist with argument validation, timeouts, cancellation, and confirmation gates. Never execute arbitrary model-generated code or shell commands.

See [JARVIS execution model](../JARVIS_EXECUTION_MODEL.md) for the target interaction and authorization flow.
