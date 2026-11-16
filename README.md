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
- Backend health endpoint reports whether an API key is configured
- Responsive desktop and mobile navigation
- CSS-based holographic 3D core, animated orbit rings, floating specialist nodes, avatar rings and studio hologram
- Optional 3D motion toggle with saved preference, reduced-motion support, and desktop pointer-depth effects
- Selectable Research, Coding, Writing, Analysis and Creative specialist modes routed through the AI backend
- API key never stored in frontend code

## Interface modules and current scope
- **Overview / Neural Core:** animated visual design; non-API telemetry figures are illustrative UI values
- **AI Assistant:** working chat when the backend and provider key are configured
- **AI Settings / Persona Builder:** local preferences persist in this browser and persona values are sent with chat requests
- **Collaboration Nexus:** selectable specialist modes (Research, Coding, Writing, Analysis and Creative) that send role-specific instructions to the same AI model; these are not independent autonomous agents
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
- Automated tests, rate limiting, abuse protection and production deployment


## Holographic UI expansion
- **AI Model Selection Matrix:** selects OpenAI, Gemini or hybrid provider mode for future chat requests. The selected mode is sent to the existing backend; server-side API keys and provider availability are still required.
- **Expanded AI Settings:** secondary glow, metallic specularity and contrast sliders, alongside accent color and existing processing/persona controls. These sliders change appearance only, not actual model speed or compute resources.
- **Operator Profile:** editable local display name. It is not a verified account identity.
- **Subscription cards:** informational UI only. Billing, payments and paid entitlements are not connected.
- **3D presentation:** model cards use depth, hover tilt and animated holographic rings, with responsive mobile layout and reduced-motion support.
