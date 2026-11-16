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

For production, bind to the host-provided port and configure trusted proxy/network access. Do not expose the development server directly to the public internet.

The FastAPI app serves the frontend from the same origin. GitHub Pages alone cannot run the Python API.

## Production checklist
- Configure secrets in the host secret manager, never in Git.
- Use HTTPS and a trusted domain.
- Configure `/api/health` health checks.
- Add rate limiting, monitoring, log redaction, and authentication before storing user data.
- Restrict CORS to trusted origins if frontend/backend are split.
- Verify provider quotas and spending alerts; test rollback.

Committing this document does not deploy the application.
