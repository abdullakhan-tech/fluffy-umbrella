# Zaythar AI — Python Companion API

Python 3.10+ FastAPI service for the Zaythar AI Android app. It provides authenticated chat, SSE streaming, allow-listed agent tools, owner-scoped sessions, and durable long-term memory. Provider credentials remain server-side.

## Install and run

```bash
cd backend
python -m venv .venv
# Windows: .venv\\Scripts\\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

Configure Firebase Admin Application Default Credentials and `FIREBASE_PROJECT_ID`. Use workload identity on hosted deployments. Do not commit service-account files, Firebase configuration, provider keys, or real user data.

## API

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/health` | Public liveness check |
| POST | `/api/v1/chat` | Authenticated chat completion |
| POST | `/api/v1/chat/stream` | Authenticated Server-Sent Events chat |
| POST | `/api/v1/agent/task` | Allow-listed deterministic agent tools |
| GET | `/api/v1/sessions` | List caller's sessions |
| GET/DELETE | `/api/v1/sessions/{id}` | Read/delete caller's session |
| POST | `/api/v1/sessions/{id}/chat` | Session-aware chat |
| GET | `/api/v1/memories?q=...` | Search caller's saved memories |
| POST | `/api/v1/memories` | Save a memory |
| DELETE | `/api/v1/memories/{memory_id}` | Delete only caller's memory |

Create memory example:

```json
{"content":"User prefers concise Jarvis-style voice replies.","tags":["voice","preferences"]}
```

All protected endpoints require `Authorization: Bearer <Firebase-ID-token>` and, when enabled, `X-Firebase-AppCheck: <App-Check-token>`. Owner identity comes only from verified Firebase claims; never send a client-supplied owner ID. Unknown request fields and client system prompts are rejected.

## Durable AI memory

Memory records are stored in SQLite at `MEMORY_DB_PATH` (default `data/zaythar_memory.sqlite3`, relative to the backend working directory). Chat and streaming requests retrieve a small relevant memory context using the verified Firebase UID. Records are isolated by owner, parameterized SQL is used, and retrieved context is capped.

SQLite is a single-instance persistence adapter. For multi-worker or multi-instance production, replace it with a shared transactional database and managed backups. Local database files are ignored by Git and must never be committed.

## Python usage

The backend is a native Python API. It can be called from Python with `httpx`:

```python
import asyncio
import httpx

async def save_memory(base_url: str, firebase_id_token: str, app_check_token: str):
    async with httpx.AsyncClient(base_url=base_url, timeout=20) as client:
        response = await client.post(
            "/api/v1/memories",
            headers={
                "Authorization": f"Bearer {firebase_id_token}",
                "X-Firebase-AppCheck": app_check_token,
            },
            json={"content": "Prefers concise voice responses", "tags": ["voice"]},
        )
        response.raise_for_status()
        return response.json()

# asyncio.run(save_memory("http://127.0.0.1:8000", id_token, app_check_token))
```

## Tests

```bash
cd backend
PYTHONPATH=. pytest -q
```

Tests use synthetic Firebase token verifiers. They do not prove live Firebase Console configuration, production App Check enforcement, external provider connectivity, multi-instance durability, or Android device behavior.
