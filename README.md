# Zaythar AI

A JARVIS/FRIDAY-inspired multilingual personal assistant starter project. Responsive web interface, browser voice input, and a hybrid backend that uses an AI API when configured and a local fallback in the browser.

## Features
- Futuristic responsive assistant interface
- Multilingual chat UI (English, Urdu, Arabic, Hindi, French, Spanish, German, Chinese and Japanese)
- Browser speech recognition where supported (HTTPS/localhost and browser permission required)
- Hybrid mode: FastAPI backend + configurable OpenAI-compatible provider, with local fallback
- Server-side API key handling; no secrets in browser code
- Health endpoint at `/api/health`

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

## Current scope
This is a working starter foundation, not a fully autonomous operating-system agent. Browser speech recognition depends on browser support and permissions. Advanced AI requires a configured server-side API key.
