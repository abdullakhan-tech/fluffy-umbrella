# Zaythar AI — Python Companion Backend (with OpenJarvis Core Engine)

This directory contains the Python AI companion service for **Zaythar AI** (`fluffy-umbrella`), enhanced with core architectural capabilities adapted from **OpenJarvis** (cleanly integrated for high-performance and lightweight deployment).

It provides a secure, server-side AI execution boundary, keeping provider API credentials off mobile devices and exposing authenticated REST endpoints for the Android client.

## Core Features & OpenJarvis Innovations

- **AI Personas (OpenJarvis)**:
  - **JARVIS**: Crisp, loyal, dry-witted AI chief of staff. Factual, concise, addressing the user respectfully as "Sir".
  - **FRIDAY**: Warm, tactical, supportive, and direct operational companion addressing the user as "Boss".
  - **Zaythar Core**: Balanced, analytical AI companion focused on engineering and problem solving.
  - Endpoints: `GET /api/v1/personas`, `GET /api/v1/personas/{id}`.
- **Durable Memory & Fact Extraction (OpenJarvis)**:
  - Persistent, bounded fact store (`backend/data/facts.json`) retaining key user preferences, identity, goals, and technical context across conversations.
  - Automatic background fact extraction from conversational dialogue turns.
  - Endpoints: `GET /api/v1/memory/facts`, `POST /api/v1/memory/facts`, `DELETE /api/v1/memory/facts/{id}`.
- **Security Guardrails (OpenJarvis)**:
  - **Secret Scanner**: Real-time detection and automatic redaction of sensitive credentials (OpenAI/Anthropic keys, AWS, GitHub tokens, Slack, Stripe, database URIs).
  - **Prompt Injection Defense**: Detection of prompt overrides, identity spoofing, and code/shell injection patterns.
  - Endpoint: `POST /api/v1/security/scan`.
- **Advanced Agent Tools**:
  - `think`: OpenJarvis-inspired reasoning scratchpad for multi-step logical deduction.
  - `daily_briefing`: Morning digest with timestamp, system telemetry, memory review, and priority items.
  - `deep_research`: Structured research planner breaking complex topics into hypotheses, investigation angles, and validation gates.
  - `calculator`: Deterministic AST-based math evaluation.
  - `code_inspect`: Safe AST syntax inspection and balance verification.
  - `summarize`: Keyword extraction and text summarization.
  - Endpoint: `POST /api/v1/agent/task`.
- **Real-Time Streaming**:
  - Server-Sent Events (SSE) `/api/v1/chat/stream` for real-time word-by-word streaming to Android.
- **Orb Animation Telemetry**:
  - `/api/v1/orb/animation-state` provides visual state, pulse rate, and glow color hints for the Jetpack Compose glowing Orb canvas.
- **Local Fallback Engine**:
  - If no external provider keys are configured, the service operates gracefully via a safe local engine without crashing (Rule 1).
- **Automated Test Suite**:
  - Complete `pytest` suite with 20 passing unit and integration tests.

## Architecture

```
fluffy-umbrella/
├── app/                  # Native Android Application (Kotlin + Jetpack Compose)
└── backend/              # Python Companion Backend (FastAPI + OpenJarvis)
    ├── app/
    │   ├── config.py     # Environment settings & CORS configuration
    │   ├── main.py       # FastAPI application and route definitions
    │   ├── models.py     # Pydantic request/response schemas
    │   └── services/
    │       ├── ai_service.py       # AI provider orchestrator & streaming
    │       ├── agent_tools.py      # Think, briefing, research, math, code, summary
    │       ├── memory_service.py   # Durable fact store & heuristic extractor
    │       ├── persona_service.py  # Jarvis, Friday & Zaythar system prompts
    │       ├── security_service.py # Secret & prompt injection scanner
    │       └── session_service.py  # Bounded in-memory session manager
    ├── tests/
    │   └── test_api.py   # Automated pytest suite (20 unit tests)
    ├── Dockerfile
    ├── requirements.txt
    └── .env.example
```

## Quick Start

### 1. Setup & Installation

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### 2. Configuration

Copy the example environment file:

```bash
cp .env.example .env
```

Configure optional server-side provider keys in `.env`:
```env
PORT=8000
HOST=0.0.0.0
ENVIRONMENT=development
OPENAI_API_KEY=your_key_here
GEMINI_API_KEY=your_key_here
OLLAMA_BASE_URL=http://localhost:11434
```

### 3. Running the Server

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The interactive API documentation is available at `http://localhost:8000/docs`.

### 4. Running Tests

```bash
PYTHONPATH=. pytest -v
```

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Service health status check |
| `GET` | `/api/v1/status` | API version and runtime status |
| `GET` | `/api/v1/personas` | List available personas (Jarvis, Friday, Zaythar) |
| `GET` | `/api/v1/personas/{id}` | Retrieve details for a specific persona |
| `GET` | `/api/v1/memory/facts` | List remembered durable user facts |
| `POST` | `/api/v1/memory/facts` | Manually record a context fact |
| `DELETE` | `/api/v1/memory/facts/{id}` | Delete a specific remembered fact |
| `DELETE` | `/api/v1/memory/facts` | Clear all remembered memory facts |
| `POST` | `/api/v1/security/scan` | Scan text for secrets & prompt injection |
| `POST` | `/api/v1/chat` | Chat completion with persona, memory, & guardrails |
| `POST` | `/api/v1/chat/stream` | Real-time Server-Sent Events (SSE) token stream |
| `GET` | `/api/v1/sessions` | List active conversation sessions |
| `GET` | `/api/v1/sessions/{id}` | Retrieve history for a session |
| `DELETE` | `/api/v1/sessions/{id}` | Clear conversation history for a session |
| `POST` | `/api/v1/sessions/{id}/chat` | Session-aware chat with persona & memory |
| `GET` | `/api/v1/orb/animation-state` | Visual animation hints for Android Compose Orb UI |
| `POST` | `/api/v1/agent/task` | Agent task execution (think, briefing, research, math, code, summarize) |


## Static codebase analysis agent

The `/api/v1/agent/task` endpoint supports `tool_hint: "codebase_analyze"`. Supply a Python, Kotlin, or Gradle source snippet in `context` (maximum 20,000 characters). The analyzer inventories Python imports and syntax or Kotlin/Gradle imports and declared dependencies. It never executes source or reads repository paths. See [CODEBASE_ANALYSIS_AGENT.md](../docs/CODEBASE_ANALYSIS_AGENT.md) for request examples and limitations.
