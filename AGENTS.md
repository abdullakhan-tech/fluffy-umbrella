# Agent and Contributor Instructions

## Before changing code
1. Read `README.md`, `ARCHITECTURE.md`, `SECURITY.md`, and the relevant Kotlin/Firebase files.
2. Confirm whether a feature is backed by Firebase/Android APIs or is purely informational UI.
3. Keep files modular (under 500 lines) and preserve responsive Material 3 layouts across compact and wide screens.

## Rules
- Never put API keys, signing secrets, or service credentials in source code, commits, screenshots, tests, or logs.
- Use Firebase AI Logic (`Firebase.ai`) and App Check rather than embedding raw Gemini API keys in the APK.
- Validate all user inputs, bound prompt/record lengths, and enforce `WebConnector` SSRF protections on all URLs.
- Keep all Cloud Firestore paths UID-scoped (`users/{userId}/...`) and covered by `firestore.rules`.
- Preserve accessibility (`>= 48.dp` touch targets, `contentDescription`, `testTag`) and the persistent 3D motion toggle.
- Add or update unit/UI/emulator tests and documentation with behaviour changes.
- Never invent live device/emulator test results or unverified telemetry.

## Completion checklist
Run `gradle :app:testDebugUnitTest`, `gradle :app:lintDebug`, `gradle :app:assembleDebug`, and `node --test firestore.test.js` (with Firebase emulators running), update docs, and report any environment boundaries honestly.


## GitHub Discussions and community documentation
- Keep `DISCUSSIONS.md`, the README community link, and `.github/DISCUSSION_TEMPLATE/*.yml` aligned when category names or community workflows change.
- Do not imply that committing discussion templates creates GitHub Discussion categories; category setup is a repository settings action.
- Route reproducible defects and implementation work to Issues/Pull Requests; use Discussions for questions, ideas, demos, and design conversations.
- Never request or publish API keys, Firebase credentials, access tokens, passwords, or private user data in public discussions.
- Send vulnerability reports through a private security channel, not a public discussion thread.
