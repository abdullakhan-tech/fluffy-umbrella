# Security and privacy

## Authentication and authorization

- Firebase Authentication identifies the signed-in account.
- Repository writes derive the owner UID from the current Firebase Auth user; a caller-supplied session or document ID is never an identity credential.
- Firestore documents must be scoped to the authenticated UID.
- Enforce ownership in both repository code and Firestore security rules.
- Default-deny access when ownership cannot be established.

## App integrity

- Debug builds use the Firebase App Check debug provider. Register debug tokens only in a non-production Firebase project.
- Release builds use the Play Integrity App Check provider. Register the package and release signing certificate in Firebase/Google Play, then enable App Check enforcement after checking production metrics.
- Client initialization does not prove that Firebase Console enforcement is enabled.

## Secrets

- Never commit `google-services.json` if it contains project configuration that your team treats as private, service-account JSON, private keys, access tokens, or API secrets.
- Never embed standalone Gemini API keys or server credentials in the Android app.
- Keep server-side provider secrets on a trusted backend if a backend is introduced.
- Redact secrets and personal content from logs and crash reports.
- Android app-data backup is disabled; do not re-enable it without explicit backup exclusions for private state.

## AI and tool safety

Treat user content, retrieved documents, memory, and model output as untrusted data. Only expose explicitly registered tools. Validate typed arguments and enforce size limits and timeouts. Require user confirmation for consequential actions such as sending communications, deleting data, purchases, or controlling connected devices.

## Memory

Store only useful information with an appropriate user-facing explanation and controls. Support review, correction, retention limits, and deletion. Never allow one account's memory to enter another account's prompt.

## Reporting issues

Do not publish credentials, personal data, or private logs in public issues. Share a minimal reproduction and redact sensitive values.
