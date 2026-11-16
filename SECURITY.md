# Security Policy

## Reporting
Please do not publish exploitable vulnerabilities, credentials, or personal data in public issues. Report security concerns privately to the repository maintainer through GitHub's private vulnerability reporting feature, if enabled.

## Secret handling
- Store `OPENAI_API_KEY` only in the deployment platform's secret/environment manager or an untracked local `.env`.
- Never commit `.env`, tokens, private keys, or real user data.
- Rotate any credential that was exposed.
- Keep `.env.example` limited to placeholder values.

## Current safeguards and gaps
- Provider key is read server-side.
- Request message length is bounded with Pydantic.
- The current app does not provide user authentication, per-user quotas, persistent server memory, or production-grade abuse controls.
- Add HTTPS, rate limiting, authentication, origin policy, request logging with redaction, and monitoring before public production use.

## Safe development
Use synthetic data in tests. Avoid logging prompts or provider responses by default. Return generic provider errors to clients; keep sensitive diagnostics server-side and redacted.
