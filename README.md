# Zaythar AI

A JARVIS/FRIDAY-inspired multilingual personal assistant with a red cybernetic command-center interface. Responsive web UI, browser voice input, and a hybrid FastAPI backend.

## Command-center modules
- **System Overview:** animated neural-core visualizer, system telemetry and module launch deck
- **AI Assistant:** multilingual chat interface with browser speech recognition where supported
- **AI Core Settings:** processing sliders, feature toggles and selectable accent colors
- **Persona Builder:** configurable assistant personality parameters
- **Collaboration Nexus:** multi-agent network visualization
- **Image & Video Studio:** creative prompt and storyboard brief workspace (provider integration required for actual generation)
- **Code Copilot:** secure API request sample
- **Prompt Nexus:** searchable prompt templates
- **Knowledge Base:** local-session notes workspace; document ingestion/RAG integration is not yet connected
- **User Profile and API & Security:** local profile overview and server-side credential guidance

## Core features
- Multilingual UI/chat language selection (English, Urdu, Arabic, Hindi, French, Spanish, German, Chinese and Japanese)
- Browser speech recognition where supported (HTTPS/localhost and browser permission required)
- FastAPI backend with `/api/health` and `/api/chat`
- Hybrid backend + local fallback
- Server-side API key handling; no secrets in browser code
- Responsive desktop and mobile layout

## Run locally
1. Install Python 3.10+.
2. Create and activate a virtual environment.
3. Install dependencies: `pip install -r backend/requirements.txt`
4. Copy `.env.example` to `.env`, set `OPENAI_API_KEY`, and export the variables into your shell (or use your preferred environment loader).
5. Start the app from the repository root: `uvicorn backend.main:app --reload`
6. Open http://127.0.0.1:8000

The API key must remain on the server. Never commit a real `.env` file.

## GitHub Pages
The static interface (`index.html`, `style.css`, `app.js`) can be hosted on GitHub Pages. GitHub Pages cannot run the Python backend; configure a separately hosted backend and update the frontend API URL for production deployment.

## Current scope and limitations
This is a feature-rich UI starter, not a fully autonomous operating-system agent. The Media Studio currently prepares prompts; it does not generate images/videos until a media provider is integrated. Collaboration and telemetry panels are visual interface modules, not live agent orchestration or hardware telemetry. Knowledge notes are currently held in the page session. Browser speech recognition depends on browser support and permissions. Advanced AI requires a configured server-side API key.
