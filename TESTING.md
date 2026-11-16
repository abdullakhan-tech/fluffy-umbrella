# Testing Guide

Run from the repository root after installing `backend/requirements.txt`:

```bash
python -m compileall -q backend
python -m pytest -q
node --check app.js
```

## Automated coverage

- Account registration requires a configured invitation secret and rejects invalid invitations.
- Passwords are salted PBKDF2-HMAC-SHA256 hashes; plaintext passwords are not stored.
- Opaque bearer sessions are stored as SHA-256 digests, expire, and can be revoked.
- Protected chat, agent, memory and knowledge routes reject unauthenticated callers.
- Memory and knowledge CRUD are isolated between two synthetic accounts.
- A caller cannot override ownership by supplying a session ID or unknown request fields.
- Agent specialist names and local tool names are allow-listed; arbitrary Python execution syntax is rejected.
- Provider error responses are sanitized.
- Rate-limit behavior, public health checks and request validation are covered.

All account names, passwords, notes, facts and tokens in automated tests are synthetic. Tests use temporary SQLite databases.

## Manual Android / Windows smoke test

1. Copy `.env.example` to an untracked `.env` and configure at least one provider key.
2. Generate a 32+ character invitation token and set `ZAYTHAR_REGISTRATION_TOKEN`.
3. Run the setup script and start the backend on the local loopback interface.
4. Open the UI, create an account using the invitation token, then remove the token from `.env` and restart to disable new registrations.
5. Add a memory note and a knowledge fact; sign out and create/sign in as a second account. Confirm the second account cannot list or delete the first account's data.
6. Sign out, then confirm protected API routes return 401 without a bearer token.
7. Verify Android browser microphone permissions, voice fallback, mobile layout, keyboard navigation and reduced-motion behavior.

## Known test boundaries

GitHub Actions tests the supported Python environments; it does not replace a real Android Termux device check. The built-in rate limiter is per process. Multi-worker production deployments need shared rate limiting, HTTPS, encrypted storage and operational monitoring.
