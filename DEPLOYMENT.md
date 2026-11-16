# Deployment

## Requirements
- Python 3.10+
- Python-capable hosting for FastAPI
- Server-side `OPENAI_API_KEY`
- Optional `OPENAI_MODEL` (defaults to `gpt-4o-mini`)

## Local run
```bash
pip install -r backend/requirements.txt
cp .env.example .env
# Edit .env and add OPENAI_API_KEY
uvicorn backend.main:app --host 0.0.0.0 --port 8000
```

The FastAPI app serves the frontend from the same origin. GitHub Pages alone cannot run the Python API.

## Production checklist
- Configure secrets in the host secret manager, never in Git.
- Use HTTPS and a trusted domain.
- Configure `/api/health` health checks.
- Add rate limiting, monitoring, log redaction, and authentication before storing user data.
- Restrict CORS to trusted origins if frontend/backend are split.
- Verify provider quotas and spending alerts; test rollback.

Committing this document does not deploy the application.
