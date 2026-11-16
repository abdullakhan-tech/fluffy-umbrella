# Architecture

## Current system
```text
Browser (HTML/CSS/JavaScript)
  | same-origin HTTP
  v
FastAPI backend
  | server-side OPENAI_API_KEY
  v
OpenAI Chat Completions API
```

## Frontend
- `index.html`: application shell and module markup.
- `style.css`: responsive styling, themes, and CSS-based holographic effects.
- `app.js`: UI events, localStorage preferences/history, browser speech APIs, and chat requests.

## Backend
- `backend/main.py`: FastAPI health and chat endpoints; serves the frontend.
- `backend/requirements.txt`: Python dependencies.
- `.env.example`: documented environment variable names only.

## State and boundaries
- Chat history, notes, and preferences are browser-local; they are not synced across devices or authenticated to a user.
- AI chat requires a configured server-side provider key.
- Specialist modes change system instructions; there is no multi-agent runtime.
- Image/video generation, document ingestion/RAG, user accounts, and hardware/device control are not implemented.

## Future direction
Add authentication, database-backed encrypted memory, file ingestion with access controls, task orchestration with approvals, observability, and deployment automation as separate reviewed components.
