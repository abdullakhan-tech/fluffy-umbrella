# Architecture

## Current system

```text
Browser (HTML/CSS/JavaScript)
  | same-origin HTTP
  v
FastAPI backend
  |-- provider adapter (OpenAI / Gemini / hybrid)
  |-- specialist orchestration (research, coding, analysis, writing, creative)
  |-- bounded session memory and knowledge context
  |-- allow-listed local tools
  v
Hosted AI provider APIs
```

## Frontend
- `index.html`: application shell and module markup.
- `style.css`: responsive styling, themes, and CSS-based holographic effects.
- `app.js`: UI events, localStorage preferences/history, browser speech APIs, and chat requests.

## Backend
- `backend/main.py`: FastAPI endpoints, API access guard, and frontend serving.
- `backend/brain.py`: server-side OpenAI and Gemini adapters with hybrid fallback.
- `backend/agents.py`: bounded concurrent specialist calls and coordinator synthesis.
- `backend/memory.py` and `backend/knowledge.py`: session-keyed local persistence and knowledge context.
- `backend/tools.py`: allow-listed utility tools; no arbitrary shell execution.
- `backend/tests/`: API, agent, memory, and knowledge regression tests.

## Agent model
The backend specialist roles are prompt-driven workers that call configured hosted models; they are not separate trained models. GitHub Copilot custom-agent profiles live in `.github/agents/` and guide repository development; they are distinct from runtime agents in `backend/agents.py`.

## State and security boundaries
- Browser preferences/history are local to the browser and are not authenticated cross-device identity.
- Session-keyed memory is not equivalent to verified user ownership.
- A shared remote API bearer token is a deployment gate, not individual user authentication.
- Provider keys remain server-side.
- Image/video generation, document ingestion/RAG, verified user accounts, and connected-device control remain separate future integrations unless implemented and tested.

## Platform support
- Android currently uses the browser UI with a local FastAPI process under Termux; this is not a native APK.
- Windows uses the local Python/FastAPI setup; a packaged desktop installer is not yet provided.

## Future direction
Prioritize individual authentication and per-user data ownership, provider health/rate/cost controls, reliable voice workflows, document retrieval with access controls, cross-platform smoke tests, and approval-gated task orchestration.
