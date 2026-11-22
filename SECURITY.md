# Security Policy

## Reporting

Do not publish exploitable vulnerabilities, credentials, access tokens, private keys, or personal data in public issues. Use GitHub's private vulnerability reporting feature if enabled.

## Credential handling

- Never commit provider API keys, OAuth client secrets, Firebase service-account private keys, signing keys, or user credentials.
- Keep local `.env` files and service-account files outside source control.
- Prefer workload identity / Application Default Credentials for Firebase Admin in production.
- Rotate any credential that is accidentally exposed.
- Provider credentials must remain on the server and must never be embedded in Android source or APKs.

## Firebase client data

- Firebase Authentication provides Google sign-in.
- Firestore rules use default-deny and UID-based ownership checks.
- Android Firestore repository methods perform owner checks as defense in depth; Firestore Security Rules are authoritative.
- Firebase App Check is integrated in the Android client. Production enforcement still requires Firebase Console configuration and verification.
- Firestore emulator tests cover unauthenticated access, cross-user memory/knowledge isolation, schema validation, and owner CRUD. They validate checked-in rules only, not deployed production rules.

## Python companion API

- All chat, stream, agent-task, and session routes require a Firebase ID token verified by Firebase Admin SDK with revocation checking.
- Protected routes also require a Firebase App Check token by default.
- Owner identity is derived only from verified token claims. Client-supplied owner IDs are rejected by strict request models.
- Session storage is namespaced by verified UID and caller-provided session ID; session IDs are not credentials and cannot cross account boundaries.
- Request bodies are bounded by content length and strict Pydantic field limits. Client-supplied system messages and unknown tool names are rejected.
- Authenticated requests use per-UID and per-observed-IP sliding-window limits. Current counters are process-local and do not protect a horizontally scaled service from coordinated abuse; deploy a shared atomic quota store before scaling.
- Generic errors and logs omit exception text and tracebacks. Avoid logging prompts, tokens, provider responses, memory content, or user identifiers.
- Android release builds disable cleartext traffic and the companion client rejects HTTP except for debug loopback hosts. Android automatic backup is disabled to reduce exposure of local app data.
- CORS defaults to no allowed origins. Add exact browser origins only.
- The Orb endpoint returns a static hint and does not expose global session counts.

## Known limitations

- Python companion session history is in-memory only; it is not durable memory.
- The Python backend does not yet expose durable Firestore memory CRUD.
- Application-level end-to-end encryption for stored Firestore memory is not implemented. Firebase-managed encryption at rest is not end-to-end encryption.
- Production App Check enforcement, deployed Firestore rules, runtime identity, rate-limit storage, and HTTPS deployment must be verified in the Firebase / hosting environment. Source code and emulator tests do not prove deployment.

## Release checklist

- Verify the Firebase project ID and Android package registration.
- Enable Google sign-in and register signing certificate fingerprints.
- Register the Android App Check app and configure a production attestation provider.
- Deploy Firestore rules to the intended Firebase project and test with at least two accounts.
- Configure Firebase Admin workload identity / ADC for the backend.
- Keep App Check required on shared/public deployments.
- Use HTTPS and a trusted reverse proxy; do not trust arbitrary forwarded-IP headers.
- Replace process-local rate limits with shared atomic quotas before running multiple workers or instances.
- Never ship debug App Check tokens, service-account files, debug signing keys, or server credentials.

## Safe development

Use synthetic data in tests. Never commit API keys, credentials, or real personal data.
