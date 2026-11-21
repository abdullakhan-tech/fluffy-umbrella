# Security Policy

## Reporting

Do not publish exploitable vulnerabilities, credentials, access tokens, private keys, or personal data in public issues. Use GitHub's private vulnerability reporting feature if it is enabled for this repository.

## Credential and configuration handling

- Never commit standalone Gemini API keys, OAuth client secrets, service-account private keys, signing keys, or user credentials.
- The current Android Firebase AI Logic integration does not require a standalone Gemini API key in the app. Firebase manages Gemini Developer API authorization.
- Add the Firebase-generated google-services.json to the Android app module only after confirming the Firebase project, package registration, and App Check protections. Treat it as Firebase app configuration, not as a Gemini API key or service-account credential.
- Keep local .env files and signing materials out of source control.
- Rotate any credential that is accidentally exposed.

## Current safeguards

- Firebase Authentication provides Google sign-in.
- Firestore security rules use default-deny and UID-based ownership checks.
- Firebase App Check is integrated to help protect Firebase-backed services.
- AI provider credentials are not intended to be embedded in the Android client.

These controls still require correct Firebase Console configuration and deployment. Source files alone do not prove that production App Check attestation or Firestore rules are active.

## Release checklist

- Verify the Firebase project and Android package registration.
- Enable Google sign-in and register signing certificate fingerprints.
- Enable Firebase AI Logic and App Check; use a production attestation provider for releases.
- Review and deploy firestore.rules to the intended project.
- Do not ship debug App Check tokens, debug signing keys, service-account files, or server credentials.
- Use HTTPS for any future backend and redact credentials and personal data from logs.
- Test authorization boundaries with multiple accounts before release.

## Memory isolation regression coverage

The Firestore emulator suite checks that an authenticated account cannot read, list, create, update, or delete another account's memory notes or knowledge facts. Owner CRUD success paths remain covered separately. These emulator tests validate the checked-in rules only; they do not prove that the rules have been deployed to the production Firebase project.

## Safe development

Use synthetic data in tests. Avoid logging prompts, memory, or provider responses by default. Return sanitized errors to clients and keep sensitive diagnostics redacted.


## Client-side defense in depth and backend limits

- Firestore observer methods that accept a requested owner UID must compare it with the active Firebase Authentication UID before constructing a listener. This is an early client-side guard only; Firestore Security Rules remain the authoritative server-side boundary.
- Memory-note and knowledge-fact create, update, and delete operations derive the owner from the authenticated Firebase UID. Callers must not supply an owner UID. Client validation is duplicated by Firestore Security Rules.
- Diagnostic payloads must not include raw exception messages, Firestore paths, UIDs, email addresses, provider metadata, prompts, or memory content. UI errors should remain generic and actionable.
- This Android app currently accesses Firestore directly through the Firebase client SDK. No trusted Cloud Functions or Cloud Run memory API is configured in this repository.
- No server-enforced per-user or IP-aware rate limiter is currently active. Do not substitute client-side counters and describe them as a security control. Add a trusted backend that verifies Firebase ID tokens and App Check tokens, derives the owner from verified claims, validates request sizes, applies transactional quotas, and stores provider secrets in managed server-side secrets before exposing abuse-sensitive HTTP/callable endpoints.
- Application-level end-to-end encryption for stored memory is not implemented. Do not describe Firebase-managed encryption at rest as end-to-end encryption.

## Current release boundary

Source configuration and emulator tests do not prove that production Firestore rules or App Check enforcement are deployed. Verify the Firebase project, registered Android app, production attestation provider, deployed rules, and multi-account isolation in the Firebase Console before a public release.
