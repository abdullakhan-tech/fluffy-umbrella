# Zaythar AI

A JARVIS/FRIDAY-inspired multilingual personal assistant with a red cybernetic command-center interface. Responsive web UI, browser voice input/output, local browser memory, and a FastAPI backend.

## Working features
- Multilingual chat (English, Urdu, Arabic, Hindi, French, Spanish, German, Chinese and Japanese)
- OpenAI-powered chat through a server-side API key
- Persona settings and language preference included with chat requests
- Browser speech recognition where supported, plus optional speech-synthesis replies
- Chat history, notes, theme, accent and UI preferences saved in the current browser's local storage
- Individual note deletion and conversation clearing
- Backend health endpoint reports whether an API key is configured
- Responsive desktop and mobile navigation
- API key never stored in frontend code

## Interface modules and current scope
- **Overview / Neural Core:** animated visual design; non-API telemetry figures are illustrative UI values
- **AI Assistant:** working chat when the backend and provider key are configured
- **AI Settings / Persona Builder:** local preferences persist in this browser and persona values are sent with chat requests
- **Collaboration Nexus:** visual preview only; no real multi-agent orchestration yet
- **Image & Video Studio:** prepares a creative brief; image/video provider is not integrated
- **Code Copilot:** sample secure API request, not a full code execution environment
- **Prompt Nexus:** searchable prompt templates
- **Knowledge Base:** notes saved in this browser only; no document upload, cross-device sync, vector search or RAG yet
- **Profile:** local interface profile, not a verified account or subscription
- **API & Security:** setup guidance and backend configuration status; no API key entry field in the browser

## Run locally
1. Install Python 3.10+.
2. From the repository root, install dependencies: `pip install -r backend/requirements.txt`.
3. Copy `.env.example` to `.env` and set your server-side `OPENAI_API_KEY`.
4. Start the app: `uvicorn backend.main:app --reload`.
5. Open http://127.0.0.1:8000.

The backend loads the root `.env` file. Never commit a real `.env` file or put a provider key in frontend JavaScript.

## Deployment
Host the FastAPI app on a Python-capable service for AI chat. Static-only hosting such as GitHub Pages cannot run the Python backend. For a separate frontend/backend origin, configure a narrowly scoped CORS allowlist before enabling cross-origin requests; the current backend intentionally does not enable wildcard CORS.

## Remaining major upgrades
- Real agent orchestration and task tools with explicit user permissions
- Media generation provider integration
- Document upload, parsing, embeddings and retrieval-augmented generation
- Authenticated user accounts and encrypted server-side synced memory
- Automated tests, rate limiting, abuse protection and production deployment
