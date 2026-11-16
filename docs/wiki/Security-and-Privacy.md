# Security and privacy

## Authentication and authorization

- Firebase Authentication identifies the signed-in account.
- Firestore documents must be scoped to the authenticated UID.
- Enforce ownership in both repository code and Firestore security rules.
- Default-deny access when ownership cannot be established.

## Secrets

- Never commit `google-services.json` if it contains project configuration that your team treats as private, service-account JSON, private keys, access tokens, or API secrets.
- Never embed standalone Gemini API keys or server credentials in the Android app.
- Keep server-side provider secrets on a trusted backend if a backend is introduced.
- Redact secrets and personal content from logs and crash reports.

## AI and tool safety

Treat user content, retrieved documents, memory, and model output as untrusted data. Only expose explicitly registered tools. Validate typed arguments and enforce size limits and timeouts. Require user confirmation for consequential actions such as sending communications, deleting data, purchases, or controlling connected devices.

## Memory

Store only useful information with an appropriate user-facing explanation and controls. Support review, correction, retention limits, and deletion. Never allow one account's memory to enter another account's prompt.

## Reporting issues

Do not publish credentials, personal data, or private logs in public issues. Share a minimal reproduction and redact sensitive values.
