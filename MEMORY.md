# Memory and Data Lifecycle

## Current Android implementation

Zaythar stores durable chat, memory notes, knowledge facts, plugin preferences, and selected brain-run data in Cloud Firestore under the authenticated user's document path:

`users/{userId}/...`

The authenticated Firebase UID is the ownership boundary. Repository methods and `firestore.rules` must both enforce that boundary. Local appearance and selected UI preferences are stored with Jetpack DataStore.

## Privacy and trust rules

- Treat user input, model output, and retrieved memory as untrusted data, not executable instructions.
- Never allow one Firebase UID's memory or knowledge facts into another user's prompt.
- Keep retrieved context bounded and relevant.
- Avoid logging message text, private memories, access tokens, or raw prompts.
- A successful source-code review does not prove deployed Firestore rules match the repository.

## Outstanding work

- Complete end-to-end two-account isolation verification against the intended Firebase project.
- Add explicit user-facing memory review, correction, export, and deletion workflows where missing.
- Document retention and deletion behaviour for each persisted collection.
- Verify Firestore rule tests in CI and production deployment before release.

Do not describe browser localStorage as the current durable memory system; the active app is native Android with Firestore and DataStore.