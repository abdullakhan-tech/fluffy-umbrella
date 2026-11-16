# Roadmap

This roadmap is directional. Items below are not claims of completed functionality.

## Existing foundations in the repository

- Native Android app with Compose UI.
- Firebase Authentication and account-scoped Firestore data model.
- Gemini through Firebase AI Logic.
- Speech recognition and TextToSpeech integration.
- Specialist-agent workflows and persona preferences.
- Local deterministic calculator and UTC time tools.

## Next engineering priorities

1. Validate account isolation with Firestore emulator/security-rule tests.
2. Improve voice lifecycle, interruption, language fallback, and permission-denial handling.
3. Introduce a typed tool registry with strict schemas and bounded execution.
4. Add action preview, confirmation, cancellation, and audit events.
5. Add user-facing memory review, correction, retention, and deletion.
6. Build a separately permissioned Windows companion; Android permissions do not grant Windows access.
7. Add automated debug/release build checks and device-level regression coverage.

## JARVIS-style principles

- Be honest about what the app can and cannot do.
- Verify tool outcomes before reporting success.
- Keep high-impact actions behind explicit user approval.
- Prefer push-to-talk until an opt-in, transparent wake-word design is implemented.
- Keep every account's private context isolated.
