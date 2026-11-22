# Zaythar AI — Python Companion Backend

This directory contains the Python AI companion service for **Zaythar AI** (`fluffy-umbrella`). It provides a server-side AI execution boundary, keeping provider API credentials off mobile devices and exposing authenticated REST endpoints for the Android client.

## Features

- **FastAPI Framework**: High-performance, async REST API with automatic OpenAPI / Swagger documentation (`/docs`).
- **Server-Side Security**: External AI API keys (OpenAI, Gemini, Ollama) remain server-side and are never committed or distributed to mobile devices (Rules 2 & 3).
- **Session Memory**: In-memory session manager for tracking conversational context across turns.
- **Deterministic Agent Tools**: Built-in safe tools for AST math evaluation, text summarization, and syntax code inspection (Rules 1 & 4).
- **Real-Time Streaming**: Server-Sent Events (SSE) `/api/v1/chat/stream` for real-time word-by-word streaming.
- **Orb UI State**: `/api/v1/orb/animation-state` provides visual state and pulse frequency hints for the Android Jetpack Compose Orb Canvas.
- **Strict Input Validation**: Pydantic models validate all incoming requests, character limits, and message histories before dispatch (Rule 5).
- **Local Fallback Engine**: If no external provider keys are configured, the service operates gracefully via a safe local engine without crashing (Rule 1).
- **Redacted Error Handling**: Provider errors and internal stack traces are redacted before reaching clients (Rule 9).
- **Container Ready**: Includes production-ready `Dockerfile` and automated `pytest` test suite.

## Architecture

```
fluffy-umbrella/
├── app/                  # Native Android Application (Kotlin + Jetpack Compose)
└── backend/              # Python Companion Backend (FastAPI)
    ├── app/
    │   ├── config.py     # Environment settings & CORS configuration
    │   ├── main.py       # FastAPI application and route definitions
    │   ├── models.py     # Pydantic request/response schemas
    │   └── services/
    │       ├── ai_service.py     # AI provider orchestrator & streaming
    │       ├── agent_tools.py    # Math evaluator, code inspector, text summarizer
    │       └── session_service.py # Bounded in-memory session manager
    ├── tests/
    │   └── test_api.py   # Automated pytest suite (11 unit tests)
    ├── Dockerfile
    ├── requirements.txt
    └── .env.example
```

## Quick Start

### 1. Prerequisites
- Python 3.10+
- Virtual environment (recommended)

### 2. Setup & Installation

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

### 3. Configuration

Copy the example environment file:

```bash
cp .env.example .env
```

Configure your server-side keys in `.env` (optional for local testing):
```env
PORT=8000
HOST=0.0.0.0
ENVIRONMENT=development
OPENAI_API_KEY=your_key_here
GEMINI_API_KEY=your_key_here
OLLAMA_BASE_URL=http://localhost:11434
```

### 4. Running the Server

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The interactive API documentation is available at:
- Swagger UI: `http://localhost:8000/docs`
- Health check: `http://localhost:8000/health`

### 5. Running Tests

```bash
PYTHONPATH=. pytest -v
```

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Service health status check |
| `GET` | `/api/v1/status` | API version and runtime status |
| `POST` | `/api/v1/chat` | Chat completion with provider routing & session tracking |
| `POST` | `/api/v1/chat/stream` | Server-Sent Events (SSE) streaming token output |
| `GET` | `/api/v1/sessions` | List active conversation sessions |
| `GET` | `/api/v1/sessions/{id}` | Retrieve history for a given session |
| `DELETE` | `/api/v1/sessions/{id}` | Clear conversation history for a session |
| `POST` | `/api/v1/sessions/{id}/chat` | Session-aware chat with automatic context history |
| `GET` | `/api/v1/orb/animation-state` | Visual animation hints for Android Compose Orb UI |
| `POST` | `/api/v1/agent/task` | Structured agent task execution with smart tool dispatch |

### Example Chat Request
```json
POST /api/v1/chat
Content-Type: application/json

{
  "messages": [
    {"role": "user", "content": "What are my upcoming tasks?"}
  ],
  "provider": "auto",
  "temperature": 0.7
}
```

### Example Agent Task with Safe Tool Execution
```json
POST /api/v1/agent/task
Content-Type: application/json

{
  "task": "calculate 25 * (4 + 6)",
  "tool_hint": "calculator"
}
```

Response:
```json
{
  "task": "calculate 25 * (4 + 6)",
  "status": "completed",
  "summary": "Calculated result: 250",
  "tool_used": "calculator",
  "tool_result": {
    "success": true,
    "expression": "calculate 25 * (4 + 6)",
    "result": 250,
    "formatted": "250"
  },
  "actions_taken": [
    "Parsed task instruction: 'calculate 25 * (4 + 6)...'",
    "Validated parameters against security boundaries",
    "Dispatched to deterministic SafeMathEvaluator",
    "Recorded tool outcome: calculator"
  ]
}
```

## Docker Support

Build and run with Docker:

```bash
docker build -t zaythar-backend .
docker run -p 8000:8000 --env-file .env zaythar-backend
```
