import os
import time
from collections import defaultdict, deque
from pathlib import Path
from threading import Lock
from typing import Optional, Dict, Any

from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles
from backend.brain import run_brain
from pydantic import BaseModel, Field

BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(BASE_DIR / ".env")

app = FastAPI(title="Zaythar AI API", version="1.2.0")

class PersonaSettings(BaseModel):
    empathy: int = Field(default=70, ge=0, le=100)
    cognitive_capacity: int = Field(default=70, ge=0, le=100)
    adaptability: int = Field(default=70, ge=0, le=100)
    creativity: int = Field(default=70, ge=0, le=100)
    style: str = Field(default="Balanced & helpful", max_length=80)

class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=12000)
    language: Optional[str] = Field(default="en-US", max_length=16)
    persona: Optional[PersonaSettings] = None
    settings: Optional[Dict[str, Any]] = None
    agent: Optional[str] = Field(default="general", max_length=24)
    provider: str = Field(default="hybrid", pattern="^(openai|gemini|hybrid)$")
    history: list[Dict[str, str]] = Field(default_factory=list, max_length=16)

# Lightweight per-process protection. Use a gateway/shared limiter for multi-worker production.
_RATE_WINDOW_SECONDS = 60
_RATE_LIMIT = max(1, int(os.getenv("ZAYTHAR_CHAT_RATE_LIMIT", "20")))
_requests_by_ip = defaultdict(deque)
_rate_lock = Lock()

@app.middleware("http")
async def chat_rate_limit(request: Request, call_next):
    if request.url.path == "/api/chat" and request.method == "POST":
        client_ip = request.client.host if request.client else "unknown"
        now = time.monotonic()
        with _rate_lock:
            recent = _requests_by_ip[client_ip]
            while recent and now - recent[0] >= _RATE_WINDOW_SECONDS:
                recent.popleft()
            if len(recent) >= _RATE_LIMIT:
                return JSONResponse(
                    status_code=429,
                    content={"detail": "Too many chat requests. Please wait a minute and try again."},
                    headers={"Retry-After": str(_RATE_WINDOW_SECONDS)},
                )
            recent.append(now)
    return await call_next(request)

@app.get("/api/health")
def health():
    return {
        "status": "ok",
        "service": "Zaythar AI",
        "ai_configured": bool(os.getenv("OPENAI_API_KEY") or os.getenv("GEMINI_API_KEY")),
        "providers": {"openai": bool(os.getenv("OPENAI_API_KEY")), "gemini": bool(os.getenv("GEMINI_API_KEY"))},
        "default_provider": os.getenv("ZAYTHAR_AI_PROVIDER", "hybrid"),
        "version": app.version,
    }

@app.post("/api/chat")
def chat(body: ChatRequest):
    persona = body.persona or PersonaSettings()
    language_names = {"en-US": "English", "ur-PK": "Urdu", "ar-SA": "Arabic", "hi-IN": "Hindi", "fr-FR": "French", "es-ES": "Spanish", "de-DE": "German", "zh-CN": "Chinese", "ja-JP": "Japanese"}
    language = language_names.get(body.language or "", "the language used by the user")
    agent_instructions = {
        "general": "Act as a capable general-purpose assistant.",
        "research": "Act as a research specialist. Separate established facts from uncertainty.",
        "coding": "Act as a senior software engineer. Provide robust, secure code and consider edge cases.",
        "writing": "Act as an expert writing editor. Match audience and tone while preserving intent.",
        "analysis": "Act as an analytical strategist. Explain evidence, options, trade-offs, and recommendations.",
        "creative": "Act as a creative director. Develop original concepts with concrete details.",
    }
    agent_instruction = agent_instructions.get(body.agent or "general", agent_instructions["general"])
    system_prompt = (
        "You are Zaythar AI Neural Core, a safe, helpful, multilingual personal assistant. "
        + agent_instruction + " "
        + f"Reply in {language}, unless the user explicitly requests another language. "
        + f"Persona style: {persona.style}. Empathy: {persona.empathy}/100; analytical depth: {persona.cognitive_capacity}/100; adaptability: {persona.adaptability}/100; creativity: {persona.creativity}/100. "
        + "Treat these as tone preferences, not permission to ignore safety or accuracy. Be clear, practical, and honest."
    )
    try:
        return run_brain(body.provider, system_prompt, body.history, body.message)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc))
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc))
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"AI request failed: {type(exc).__name__}")

# Same-origin hosting: no permissive wildcard CORS is needed.
app.mount("/", StaticFiles(directory=str(BASE_DIR), html=True), name="web")
