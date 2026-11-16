# Zaythar AI

A JARVIS/FRIDAY-inspired multilingual personal assistant with a red cybernetic command-center interface. Responsive web UI, browser voice input/output, local browser memory, and a FastAPI backend.

## Working features
- Multilingual chat (English, Urdu, Arabic, Hindi, French, Spanish, German, Chinese and Japanese)
- Zaythar Neural Core orchestration layer with OpenAI and Gemini provider adapters
- Provider selection: OpenAI, Gemini, or hybrid (tries OpenAI first, then Gemini)
- Bounded recent conversation context is sent to the selected model
- SQLite-backed, browser-session-scoped long-term memory with list, add, delete and clear API endpoints
- Shared session-scoped knowledge graph storing explicit subject–predicate–object facts, with add/list/delete/clear API operations and per-session fact limits
- Multi-agent runs receive relevant session memories and graph facts as untrusted context; bounded coordinator summaries are saved back to session memory
- Knowledge Base notes sync to server-side memory when the backend is online
- Safe local tool router for arithmetic and UTC time; arbitrary code execution is not permitted
- Lightweight task-decomposition hints guide planning without exposing private chain-of-thought
- Provider API keys stay server-side in environment variables
- Persona settings and language preference included with chat requests
- Browser speech recognition where supported, plus optional speech-synthesis replies
- Chat history, notes, theme, accent and UI preferences saved in the current browser's local storage
- Individual note deletion and conversation clearing
- Backend health endpoint reports configured providers, app version and process uptime
- Responsive desktop and mobile navigation
- Interactive 3D neural-field canvas with depth-projected synapses, animated orbital paths, pointer parallax, responsive high-DPI rendering and reduced-motion support
- Live backend health telemetry: configured provider availability and real server-process uptime (not fabricated compute/bandwidth metrics)
- CSS-based holographic core, floating specialist nodes, avatar rings and studio hologram
- Optional 3D motion toggle with saved preference, reduced-motion support, and desktop pointer-depth effects
- Five allow-listed specialist roles: Research, Coding, Writing, Analysis and Creative
- Multi-agent orchestrator: runs 1–4 selected specialists concurrently, isolates individual failures, shares session memory and explicit graph facts, then synthesizes successful deliverables with a coordinator model call
- Dedicated `POST /api/agents/run` endpoint with task-size, provider and specialist-count validation; shares the chat endpoint's per-process rate limiter
- API key never stored in frontend code

## Interface modules and current scope
- **Overview / Neural Core:** animated visual design; non-API telemetry figures are illustrative UI values
- **AI Assistant:** working chat when the backend and provider key are configured
- **AI Settings / Persona Builder:** local preferences persist in this browser and persona values are sent with chat requests
- **Collaboration Nexus:** real backend orchestration of selected specialist model calls in parallel, followed by coordinator synthesis. Agents receive session-scoped memory and explicit graph facts, while coordinator summaries are persisted for later runs. The graph does not automatically convert model-generated statements into verified facts. Agents do not yet share a durable task queue, independently call tools, or run unattended loops.
- **Image & Video Studio:** prepares a creative brief; image/video provider is not integrated
- **Code Copilot:** sample secure API request, not a full code execution environment
- **Prompt Nexus:** searchable prompt templates
- **Knowledge Base:** notes saved in this browser and synced to session-scoped SQLite memory when online; Collaboration Nexus also exposes an explicit triple-based knowledge graph. No verified account identity, cross-device sync, vector search or RAG yet
- **Profile:** local interface profile, not a verified account or subscription
- **API & Security:** setup guidance and backend configuration status; no API key entry field in the browser

## Run locally

### Windows
1. Install Python 3.10+ and Git.
2. Open the repository folder in PowerShell.
3. Run `powershell -ExecutionPolicy Bypass -File .\\scripts\\setup_windows.ps1`. This creates `.venv`, installs dependencies, creates `.env` if missing, compiles the backend, and runs tests.
4. Add at least one real provider key to `.env` (OpenAI or Gemini). Keep unused provider keys blank.
5. Double-click `scripts\\run_windows.bat` or run it from Command Prompt.
6. Open http://127.0.0.1:8000.

### Linux / macOS
1. Install Python 3.10+ and Git.
2. Run `bash scripts/setup.sh` from the repository root. It creates `.venv`, installs dependencies, creates `.env` if missing, compiles the backend, and runs tests.
3. Add at least one real provider key to `.env`.
4. Start with `bash scripts/run.sh`.
5. Open http://127.0.0.1:8000.

### Manual setup
```bash
python -m venv .venv
# Activate .venv, then:
python -m pip install -r backend/requirements.txt
python -m compileall -q backend
python -m pytest -q
uvicorn backend.main:app --reload --host 127.0.0.1 --port 8000
```

The backend loads the root `.env` file. The example file contains blank provider keys intentionally; health status will not claim a provider is configured until a real key is added. Never commit a real `.env` file or put a provider key in frontend JavaScript.

## Deployment
Host the FastAPI app on a Python-capable service for AI chat. Static-only hosting such as GitHub Pages cannot run the Python backend. For a separate frontend/backend origin, configure a narrowly scoped CORS allowlist before enabling cross-origin requests; the current backend intentionally does not enable wildcard CORS.

## Remaining major upgrades
- Training a proprietary foundation model from scratch (the current Neural Core orchestrates hosted models; it is not a newly trained LLM)
- Real agent orchestration and a broader permissioned tool ecosystem
- Authentication and authorization for multi-user memory; session IDs are not an account security boundary
- Media generation provider integration
- Document upload, parsing, embeddings and retrieval-augmented generation
- Authenticated user accounts and encrypted server-side synced memory
- Authentication and authorization for multi-user memory; session IDs are not an account security boundary
- Production-grade shared rate limiting, abuse protection, monitoring and deployment validation


## Holographic UI expansion
- **AI Model Selection Matrix:** selects OpenAI, Gemini or hybrid provider mode for future chat requests. The selected mode is sent to the existing backend; server-side API keys and provider availability are still required.
- **Expanded AI Settings:** secondary glow, metallic specularity and contrast sliders, alongside accent color and existing processing/persona controls. These sliders change appearance only, not actual model speed or compute resources.
- **Operator Profile:** editable local display name. It is not a verified account identity.
- **Subscription cards:** informational UI only. Billing, payments and paid entitlements are not connected.
- **3D presentation:** model cards use depth, hover tilt and animated holographic rings, with responsive mobile layout and reduced-motion support.


## Neural visualizer and telemetry
The overview and assistant core include a canvas-rendered 3D neural field. Nodes are projected from a 3D sphere, nearby nodes are linked as synapses, orbital paths animate around the field, and pointer movement adds parallax on supported devices. Rendering scales for high-DPI displays, pauses when the tab is hidden, and respects reduced-motion preferences. The overview's provider indicators and uptime are read from `/api/health`; no fake GPU, bandwidth, or memory-utilization claims are made.


## Multi-agent orchestration
The Collaboration Nexus calls `POST /api/agents/run`. Choose between one and four specialists from the allow-list (`research`, `coding`, `analysis`, `writing`, `creative`). The backend dispatches specialist model requests concurrently using the selected OpenAI, Gemini, or hybrid provider route, isolates failed specialist calls, and sends successful deliverables to a lead coordinator for synthesis. The response includes the coordinated answer and each specialist's status/output.

Example request:

```json
{
  "task": "Review this API design for reliability and security",
  "agents": ["research", "coding", "analysis"],
  "provider": "hybrid",
  "language": "English"
}
```

This is bounded, request-scoped orchestration—not unrestricted autonomous agents. A run makes one orchestration call per successful specialist plus one coordinator call (up to five orchestration calls for four specialists). In hybrid mode, each orchestration call can attempt OpenAI and then Gemini if fallback is needed, so actual provider requests may be higher. Provider usage and cost scale with the selected team size. Failed specialist calls are isolated; if all specialists fail, the endpoint returns a service error. Specialist tools, persistent queues, cancellation, and account-level permissions are future work. Do not expose the endpoint publicly without authentication and production-grade rate limiting.


## Shared agent memory and knowledge graph
The `backend/knowledge.py` module stores explicit subject–predicate–object facts in SQLite, scoped to the same browser-generated session ID used by the existing memory feature. The Collaboration Nexus lets the operator add and delete facts. Each multi-agent run receives recent session memories and graph facts as context, and stores a bounded coordinator-result note for continuity. Model-generated text is not automatically promoted into graph facts or marked verified. The graph is capped at 200 facts per session, fields are limited to 300 characters, and list responses are bounded.

API:
- `GET /api/knowledge?session_id=...`
- `POST /api/knowledge` with `session_id`, `subject`, `predicate`, `object`, and optional `source`
- `DELETE /api/knowledge/{fact_id}?session_id=...`
- `DELETE /api/knowledge` with `session_id`

**Security boundary:** this is session scoping, not user authentication. A client can choose its session ID. Do not treat it as private multi-user storage or expose it publicly with sensitive data until authenticated user ownership and authorization are implemented.
