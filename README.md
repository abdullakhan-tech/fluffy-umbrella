# Zaythar AI

A JARVIS/FRIDAY-inspired multilingual personal assistant with a red cybernetic command-center interface. Responsive web UI, browser voice input/output, local browser memory, and a FastAPI backend.

## Working features
- Multilingual chat (English, Urdu, Arabic, Hindi, French, Spanish, German, Chinese and Japanese)
- Zaythar Neural Core orchestration layer with OpenAI and Gemini provider adapters
- Provider selection: OpenAI, Gemini, or hybrid (tries OpenAI first, then Gemini)
- Bounded recent conversation context is sent to the selected model
- SQLite-backed, browser-session-scoped long-term memory with list, add, delete and clear API endpoints
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
- Multi-agent orchestrator: runs 1–4 selected specialists concurrently, isolates individual failures, then synthesizes successful deliverables with a coordinator model call
- Dedicated `POST /api/agents/run` endpoint with task-size, provider and specialist-count validation; shares the chat endpoint's per-process rate limiter
- API key never stored in frontend code

## Interface modules and current scope
- **Overview / Neural Core:** animated visual design; non-API telemetry figures are illustrative UI values
- **AI Assistant:** working chat when the backend and provider key are configured
- **AI Settings / Persona Builder:** local preferences persist in this browser and persona values are sent with chat requests
- **Collaboration Nexus:** real backend orchestration of selected specialist model calls in parallel, followed by a coordinator synthesis call. Each specialist currently receives the same user task with a different role prompt; they do not yet share a durable task queue, independently call tools, or run unattended loops.
- **Image & Video Studio:** prepares a creative brief; image/video provider is not integrated
- **Code Copilot:** sample secure API request, not a full code execution environment
- **Prompt Nexus:** searchable prompt templates
- **Knowledge Base:** notes saved in this browser and synced to session-scoped SQLite memory when online; no verified account identity, cross-device sync, vector search or RAG yet
- **Profile:** local interface profile, not a verified account or subscription
- **API & Security:** setup guidance and backend configuration status; no API key entry field in the browser

## Run locally
1. Install Python 3.10+.
2. From the repository root, install dependencies: `pip install -r backend/requirements.txt`.
3. Copy `.env.example` to `.env` and set at least one server-side key: `OPENAI_API_KEY` and/or `GEMINI_API_KEY`. Set `ZAYTHAR_AI_PROVIDER=hybrid` for OpenAI-first fallback to Gemini.
4. Start the app: `uvicorn backend.main:app --reload`.
5. Open http://127.0.0.1:8000.

The backend loads the root `.env` file. Never commit a real `.env` file or put a provider key in frontend JavaScript.

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
