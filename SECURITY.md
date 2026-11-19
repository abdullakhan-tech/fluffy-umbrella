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
- Firebase App Check uses the debug provider only in debug builds and Play Integrity in release builds. Play Integrity requires the Android app and release signing certificate to be registered in the intended Firebase/Google Play configuration.
- Android automatic app-data backup is disabled to reduce accidental export of local preferences and debug-only App Check state.
- AI provider credentials are not intended to be embedded in the Android client.

These controls still require correct Firebase Console configuration and deployment. Source files alone do not prove that production App Check attestation or Firestore rules are active.

## Client-side defense in depth and backend limits

- Firestore observer APIs that accept a user ID now require it to match the active Firebase Authentication UID before constructing a listener. This is an early client-side guard only; Firestore Security Rules remain the authoritative access-control boundary.
- Firestore diagnostics now log only a fixed operation label and a bounded Firebase status code. Diagnostic payloads omit raw exception text, Firestore paths, UIDs, email addresses, provider metadata, and memory content; callers should still map failures to safe UI messages rather than display exception details.
- Firestore-backed chat/memory UI states and Google sign-in error surfaces now use generic messages; authentication logs record only exception class names, not provider messages or stack traces. This is a presentation/logging safeguard, not a replacement for server-side authorization.
- The current Android app talks directly to Firebase Firestore and Firebase AI Logic. There is no trusted Cloud Functions or Cloud Run API in this repository, so a client-side request counter would be bypassable and must not be described as a security rate limit.
- Before offering public, abuse-sensitive AI or memory operations, add a trusted backend that verifies Firebase ID tokens and App Check tokens, derives the owner UID from the verified token, validates request sizes, applies per-user and IP-aware quotas, and keeps privileged provider credentials in managed server secrets.
- Application-level end-to-end encryption for stored memory is not implemented. Do not describe Firebase-managed storage protections as end-to-end encryption; review the data classification and retention requirements before production use.

## Release checklist

- Verify the Firebase project and Android package registration.
- Enable Google sign-in and register the correct package name and signing certificate fingerprints.
- Enable Firebase AI Logic and App Check; register Play Integrity and enforce App Check for the intended Firebase services after monitoring valid traffic.
- Register debug App Check tokens only in non-production Firebase projects; never commit or share them.
- Review and deploy firestore.rules to the intended project.
- Do not ship debug App Check tokens, debug signing keys, service-account files, or server credentials.
- Use HTTPS for any future backend and redact credentials and personal data from logs.
- Test authorization boundaries with multiple accounts before release.

## Safe development

Use synthetic data in tests. Avoid logging prompts, memory, or provider responses by default. Return sanitized errors to clients and keep sensitive diagnostics redacted.
