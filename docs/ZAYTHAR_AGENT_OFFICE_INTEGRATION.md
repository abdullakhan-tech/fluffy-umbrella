# Zaythar AI — 7-agent Office integration

This document records the intended integration of the supplied `zaythar-office-backup.zip` with the existing Android + FastAPI Zaythar AI application.

## Agent roles

| Office agent | Responsibility | Existing Zaythar capability to reuse |
| --- | --- | --- |
| Jarvis | Orchestration, task planning and review | AI router + Collaboration Nexus |
| Friday | Code changes and implementation | Code Copilot + GitHub PR approval |
| Omnivex | Research and source gathering | Web grounding / Firecrawl |
| Rio | Social drafts and publishing queue | Human-approved social queue |
| Carbon | Memory and knowledge stewardship | Firestore memories + Knowledge Graph |
| Neon | UI/UX review and design | Jetpack Compose design system + themes |
| Argon | QA, tests and security review | Engine diagnostics + backend/Android CI |

## Engine routing

The office server should call the existing FastAPI `POST /api/v1/chat` endpoint through a private server-to-server URL (`ZAYTHAR_API_URL`). That endpoint already routes to configured Zaythar providers and accepts `messages`, `provider`, `model`, and `personality`. Keep provider credentials on the backend. Do not put API keys in `config.js`, HTML, Android resources, or Git.

The optional Node adapter is in `zaythar-office/server/lib/llm.js`. Anthropic is a compatibility fallback only; prefer the Zaythar endpoint.

## Firecrawl

The optional adapter in `zaythar-office/server/lib/firecrawl.js` supports Firecrawl Search and Scrape. Configure `FIRECRAWL_API_KEY` only in the office server's secret environment. Before production, wire the adapter into the existing `/api/search` and `/api/fetch` handlers and retain DNS/IP SSRF validation on every target and redirect.

## Firebase and memory — required security gate

The backup office stores agent memory and task history in local server JSON files. The Android app already owns user-scoped Firestore records and Firebase Auth. These stores must not be merged as a shared global memory.

A production sync must:
1. Send the signed-in Firebase user's ID token from the Android client to the office API over TLS.
2. Verify the token server-side with Firebase Admin SDK (or an equivalent trusted verifier).
3. Derive UID from the verified token; never trust a client-supplied UID.
4. Store agent memory beneath `users/{uid}/...` and enforce owner-only Firestore rules.
5. Keep guest-mode data local and isolated.
6. Add export/delete controls and tests proving one UID cannot read another UID's agent memory.

The current office JSON store is therefore a local/private prototype store, not yet a Firestore sync implementation.

## Native UI / 3D office

The Android Command Studio, Collaboration Nexus, existing themes and Firebase-backed workspace remain the native app's source of truth. The supplied Three.js office is an optional companion surface. A native entry point should be added only after a deployable office URL, TLS, CORS origin and authentication handoff are configured; never embed the office's shared `OFFICE_KEY` in the APK.

## Deployment checklist

- Configure private `ZAYTHAR_API_URL` and server-side provider credentials.
- Add a real server authentication verifier before exposing office APIs outside a private network.
- Configure `FIRECRAWL_API_KEY` only if Firecrawl is enabled.
- Configure `OFFICE_KEY` with a long random secret for local prototype use; do not treat a shared browser key as production user authentication.
- Configure Firebase Admin verification and UID-scoped persistence before enabling cloud memory.
- Run office unit tests, backend pytest, Android Gradle tests/build, Firestore emulator security tests, and end-to-end tests.
- Verify the deployed office, AI provider, Firecrawl, Firebase memory isolation and native navigation with real credentials before calling the integration complete.

## Current status

The adapter and architecture contract are committed as integration groundwork. This alone does not claim that the office server is deployed, Firebase memory sync is active, Firecrawl credentials are configured, the native app opens the 3D office, or production end-to-end tests have passed.
