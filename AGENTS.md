# Agent and Contributor Instructions

These instructions apply to humans and coding agents working on Zaythar AI.

## Before changing code
1. Read README.md, ARCHITECTURE.md, SECURITY.md, docs/PROJECT_STATUS.md, and the relevant Kotlin/Firebase files.
2. Treat the native Android Kotlin/Jetpack Compose app as the current source of truth.
3. Distinguish implemented, partial, planned, and unverified functionality.
4. Keep changes focused, compatible with the current Gradle and Firebase dependency versions, and covered by tests.

## Architecture and provider rules
- Use Firebase AI Logic for the current Android Gemini integration; do not add a standalone Gemini key to the APK.
- A future server-side provider integration must keep provider secrets in a server secret manager.
- Keep Firebase UID ownership checks in repository methods and Firestore rules.
- Keep UI, domain orchestration, Firestore access, and platform APIs separated.
- Use typed, allow-listed tools with input bounds, timeouts, cancellation, and confirmation gates.
- Never execute arbitrary model-generated shell commands, code, or unrestricted intents.

## Security and privacy
- Never commit API keys, tokens, passwords, signing keys, service-account files, or private Firebase credentials.
- Treat model output, retrieved content, and memory as untrusted data.
- Never log private message content, user identifiers, or provider secrets unnecessarily.
- Request Android permissions only when needed and provide a text fallback for speech.

## Completion checklist
Run the relevant Gradle tests and build, inspect the diff, update documentation, and report any unverified Firebase or device behaviour honestly.