# Zaythar AI — Python Companion Backend

FastAPI companion service for the Zaythar AI Android app. The API verifies Firebase Authentication ID tokens and Firebase App Check tokens before allowing chat, agent, or session operations. AI provider credentials remain server-side.

## Security boundary

- `/health`, `/`, and `/api/v1/status` are public health endpoints.
- Chat, streaming, agent-task, and session endpoints require `Authorization: Bearer <Firebase ID token>`.
- Protected routes also require `X-Firebase-AppCheck: <App Check token>` by default.
- The backend verifies ID tokens with Firebase Admin SDK, checks revocation, and derives the owner UID from verified claims. A request body or session ID cannot choose the owner.
- Session keys are `(verified UID, session ID)`; the same caller-chosen session ID is isolated between accounts. Session IDs are identifiers, never credentials.
- Request models reject unknown fields, client-supplied system messages, malformed IDs, oversized strings, and unknown tool names.
- Authenticated API requests are limited per UID and per observed client IP. The current limiter is process-local and is only defense in depth; it is not a distributed quota for multi-worker or multi-instance deployments.
- Generic error responses and logs omit exception messages and tracebacks because upstream provider URLs may contain credentials.
- CORS defaults to no allowed browser origins. Configure exact origins only when a browser client is deployed.
- The Orb animation hint is static and does not disclose process-wide session counts.

## Prerequisites

- Python 3.10+
- A Firebase project with Authentication and App Check configured
- Firebase Admin Application Default Credentials with permission to verify Firebase tokens

## Install

```bash
cd backend
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\\Scripts\\activate
pip install -r requirements.txt
cp .env.example .env
```

Set `FIREBASE_PROJECT_ID` in `.env`. Supply Firebase Admin credentials outside the repository:

- On Google Cloud, prefer the runtime's attached workload identity / Application Default Credentials.
- For local development, use `gcloud auth application-default login`, or export `GOOGLE_APPLICATION_CREDENTIALS` to a service-account JSON file stored outside this repository.
- Never commit a service-account file or its contents.

Register the Android app with Firebase App Check. Use a debug token only in a local debug build and register that token in Firebase Console. Production builds must use the configured production attestation provider. Do not disable App Check on a public deployment.

For local-only tests against a loopback server, `APP_CHECK_REQUIRED=false` may be used only when the server is not exposed to a network. Authentication remains required. Restore `APP_CHECK_REQUIRED=true` before any shared or public deployment.

## Run locally

```bash
uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

The Android client obtains a Firebase ID token and App Check token from the installed Firebase SDK and sends both on protected companion API calls. A signed-in Firebase user is required.

## API

| Method | Endpoint | Access |
|---|---|---|
| GET | `/health` | Public health |
| GET | `/api/v1/status` | Public health |
| POST | `/api/v1/chat` | Firebase Auth + App Check |
| POST | `/api/v1/chat/stream` | Firebase Auth + App Check |
| GET | `/api/v1/sessions` | Firebase Auth + App Check |
| GET | `/api/v1/sessions/{id}` | Firebase Auth + App Check; owner scoped |
| DELETE | `/api/v1/sessions/{id}` | Firebase Auth + App Check; owner scoped |
| POST | `/api/v1/sessions/{id}/chat` | Firebase Auth + App Check; owner scoped |
| POST | `/api/v1/agent/task` | Firebase Auth + App Check; deterministic allow-list |
| GET | `/api/v1/orb/animation-state` | Public static UI hint |

Example protected request headers:

```http
Authorization: Bearer <Firebase ID token>
X-Firebase-AppCheck: <Firebase App Check token>
Content-Type: application/json
```

## Tests

```bash
PYTHONPATH=. pytest -v
```

Tests stub token verification with synthetic identities; they do not require real Firebase credentials. Firestore Rules emulator tests live at the repository root and separately verify memory/knowledge document ownership and CRUD.

## Important production limitations

- Session history is in process memory and is lost on restart; it is not a durable cloud memory store.
- The rate limiter is per process. Before scaling to multiple workers or instances, replace it with a shared atomic store such as Redis or a Firestore transaction-based quota.
- Production deployment, App Check enforcement, service identity, TLS termination, and deployed Firestore rules must be independently verified. A passing CI run does not prove production configuration.
- This backend does not yet expose a durable Firestore memory CRUD API. Do not treat its in-memory sessions as the user's permanent memory.

## Container

The Docker image listens on `0.0.0.0` inside the container. Publish it only behind a trusted HTTPS reverse proxy or private service network, and provide Application Default Credentials through workload identity rather than copying credentials into the image.
