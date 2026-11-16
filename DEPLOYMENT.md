# Deployment

## Requirements
- Python 3.10+
- Python-capable hosting for FastAPI
- Server-side `OPENAI_API_KEY`
- Optional `OPENAI_MODEL` (defaults to `gpt-4o-mini`)

## Local run

Use the repository setup scripts:
- Windows: `powershell -ExecutionPolicy Bypass -File .\\scripts\\setup_windows.ps1`, add keys to `.env`, then run `scripts\\run_windows.bat`.
- Linux/macOS: `bash scripts/setup.sh`, add keys to `.env`, then run `bash scripts/run.sh`.
- Android: install Termux from F-Droid or official GitHub releases, clone the repository, run `bash scripts/setup_termux.sh`, configure `.env` as below, then run `bash scripts/run_termux.sh` and open `http://127.0.0.1:8000` on the same phone. This is the web app in Termux, not a native APK.

## Account bootstrap (required before using chat or personal data)

1. Copy `.env.example` to `.env` if the setup script has not already done so.
2. Add at least one provider API key on the server.
3. Generate an invitation secret locally with `python -c "import secrets; print(secrets.token_urlsafe(32))"` and set the result as `ZAYTHAR_REGISTRATION_TOKEN` in the server's `.env`.
4. Restart Zaythar, open the UI, choose **Create account**, and enter the invitation secret. Use a unique password of at least 12 characters.
5. Distribute the invitation secret only to intended account holders. Remove `ZAYTHAR_REGISTRATION_TOKEN` from the server environment and restart to disable further registrations.

The browser keeps the bearer session in tab-scoped `sessionStorage`. Sign out on shared devices. Account memory and knowledge are scoped by the authenticated server-side user ID, not by a browser-provided session ID. Existing pre-authentication session data is not automatically assigned to an account.

For production, bind to the host-provided port, use HTTPS, configure a trusted reverse proxy, and add a shared rate limiter. Do not expose the development server directly to the public internet.

The FastAPI app serves the frontend from the same origin. GitHub Pages alone cannot run the Python API.

## Authentication model

`POST /api/auth/register` requires the server-side invitation token; `POST /api/auth/login` returns an opaque bearer session. All chat, agent, memory and knowledge endpoints require that session. Only the health endpoint and account registration/login are public. See [API.md](API.md) for request details and [SECURITY.md](SECURITY.md) for limitations.

## Production checklist
- Configure secrets in the host secret manager, never in Git.
- Use HTTPS and a trusted domain.
- Configure `/api/health` health checks.
- Verify account registration and bearer-session settings before storing user data.
- Add a shared rate limiter, monitoring, log redaction and incident response.
- Restrict CORS to trusted origins if frontend/backend are split.
- Verify provider quotas and spending alerts; test rollback.

Committing this document does not deploy the application.

