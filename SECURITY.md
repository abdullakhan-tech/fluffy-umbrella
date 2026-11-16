# Security Policy

## Reporting
Do not publish exploitable vulnerabilities, credentials, invitation tokens, session tokens or personal data in public issues. Report security concerns privately through GitHub's private vulnerability reporting feature when enabled, or contact the repository maintainer through a private channel.

## Secret handling
- Store provider API keys and `ZAYTHAR_REGISTRATION_TOKEN` only in an untracked local `.env` or deployment secret manager.
- Never commit `.env`, bearer tokens, passwords, private keys, or real user data.
- Keep `.env.example` limited to blank placeholders.
- Rotate any credential that may have been exposed.
- Browser bearer sessions are held in `sessionStorage`, not source code or persistent local storage. Treat an active browser session as sensitive and sign out on shared devices.

## Current safeguards
- Passwords are salted and hashed with PBKDF2-HMAC-SHA256; plaintext passwords are not stored.
- Opaque random bearer tokens are issued per login; only SHA-256 token digests are stored server-side. Sessions expire and logout revokes the current token.
- Registration is disabled unless a 32+ character `ZAYTHAR_REGISTRATION_TOKEN` is configured.
- Chat, agent, memory and knowledge APIs require an authenticated account.
- Memory and knowledge ownership is derived from the server-side user ID. Client-chosen session IDs are rejected and cannot select another user's namespace.
- Account, memory and knowledge inputs are bounded and unknown request fields are rejected.
- Provider exception details are not returned to API clients.
- API responses use `Cache-Control: no-store`; basic security headers and per-process rate limits are applied.
- Health checks remain public and expose only provider configured flags, never key values.

## Known limitations — not yet production-complete
- SQLite memory and account records are not encrypted at rest by this application. Use encrypted storage/volumes and restrictive filesystem permissions.
- There is no MFA, email verification, password reset flow, account recovery, administrator role system, or user-management UI.
- Registration uses one shared invitation secret. Distribute it privately and remove it from the environment when account creation should stop.
- Rate limiting is in-process and resets on restart; multi-worker/public deployments need a trusted shared gateway limiter and monitoring.
- Deploy only behind HTTPS. Do not expose the development server directly to the public internet.
- Browser XSS could expose an active session token. Keep frontend dependencies and content trusted, use a restrictive production Content Security Policy, and sign out on shared devices.
- Existing legacy rows keyed by browser-generated session IDs are not automatically assigned to accounts. They remain unclaimed in the old database namespace; do not migrate them without a verified owner mapping.

## Safe development
Use synthetic data in tests. Do not log passwords, invitation tokens, bearer tokens, prompts, provider responses, or database contents. Review dependency updates and run the full test suite before release.
