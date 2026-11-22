"""FastAPI application entrypoint for Zaythar AI Companion Backend."""

from contextlib import asynccontextmanager
import logging
from typing import List

from fastapi import FastAPI, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse

from app.config import settings
from app.models import (
    AgentTaskRequest,
    AgentTaskResponse,
    ChatMessage,
    ChatRequest,
    ChatResponse,
    HealthResponse,
    OrbVisualState,
    SessionChatRequest,
    SessionSummary,
)
from app.services.ai_service import ai_service
from app.services.session_service import session_manager

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("zaythar_backend")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting Zaythar AI Companion Backend v%s (%s)", settings.version, settings.environment)
    yield
    logger.info("Shutting down Zaythar AI Companion Backend")
    await ai_service.close()


app = FastAPI(
    title=settings.app_name,
    version=settings.version,
    description="Python AI Companion Server for Zaythar AI (fluffy-umbrella)",
    lifespan=lifespan,
)

# CORS setup
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    """Enforces Rule 9: Redacts internal errors and returns safe sanitized response."""
    logger.error("Unhandled exception: %s", type(exc).__name__, exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"detail": "Internal server error occurred. Diagnostics have been logged securely."},
    )


@app.get("/", summary="Root index")
async def root():
    return {
        "app": settings.app_name,
        "version": settings.version,
        "status": "online",
        "docs_url": "/docs",
    }


@app.get("/health", response_model=HealthResponse, summary="Service health check")
async def health():
    return HealthResponse(
        status="healthy",
        app=settings.app_name,
        version=settings.version,
        environment=settings.environment,
    )


@app.get("/api/v1/status", response_model=HealthResponse, summary="API v1 Status")
async def api_status():
    return HealthResponse(
        status="healthy",
        app=settings.app_name,
        version=settings.version,
        environment=settings.environment,
    )


@app.post("/api/v1/chat", response_model=ChatResponse, summary="Chat completion endpoint")
async def chat_completion(request: ChatRequest):
    """Processes conversational messages and returns an AI-generated completion."""
    try:
        response = await ai_service.generate_chat_reply(
            messages=request.messages,
            preferred_provider=request.provider or "auto",
            model_override=request.model,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
            session_id=request.session_id,
        )
        return response
    except Exception as e:
        logger.error("Error in chat completion: %s", type(e).__name__)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Chat completion service encountered an error.",
        )


@app.post("/api/v1/chat/stream", summary="Real-time SSE token streaming")
async def chat_stream(request: ChatRequest):
    """Streams completion tokens over Server-Sent Events (SSE)."""
    try:
        return StreamingResponse(
            ai_service.generate_chat_stream(
                messages=request.messages,
                preferred_provider=request.provider or "auto",
                model_override=request.model,
                temperature=request.temperature,
                max_tokens=request.max_tokens,
            ),
            media_type="text/event-stream",
        )
    except Exception as e:
        logger.error("Error initiating stream: %s", type(e).__name__)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Streaming chat service encountered an error.",
        )


@app.get("/api/v1/sessions", response_model=List[SessionSummary], summary="List active chat sessions")
async def list_sessions():
    """Returns active conversation session summaries."""
    return await session_manager.list_sessions()


@app.get("/api/v1/sessions/{session_id}", response_model=List[ChatMessage], summary="Get session message history")
async def get_session_history(session_id: str):
    """Returns stored conversation history for a given session."""
    return await session_manager.get_history(session_id)


@app.delete("/api/v1/sessions/{session_id}", summary="Delete a chat session")
async def delete_session(session_id: str):
    """Clears and deletes an existing conversation session."""
    deleted = await session_manager.clear_session(session_id)
    return {"session_id": session_id, "deleted": deleted}


@app.post("/api/v1/sessions/{session_id}/chat", response_model=ChatResponse, summary="Session-aware chat")
async def session_chat(session_id: str, request: SessionChatRequest):
    """Sends a single message within a persistent session context."""
    from app.models import ChatRole

    # Fetch prior history
    history = await session_manager.get_history(session_id)
    new_msg = ChatMessage(role=ChatRole.USER, content=request.message)
    full_messages = history + [new_msg]

    response = await ai_service.generate_chat_reply(
        messages=full_messages,
        preferred_provider=request.provider or "auto",
        model_override=request.model,
        temperature=request.temperature,
        session_id=session_id,
    )
    return response


@app.get("/api/v1/orb/animation-state", response_model=OrbVisualState, summary="Client visual orb animation hint")
async def orb_visual_state():
    """Provides visual state hints for the Android Jetpack Compose Orb Canvas (Rule 1 compliant)."""
    sessions = await session_manager.list_sessions()
    active_count = len(sessions)
    return OrbVisualState(
        state="idle" if active_count == 0 else "processing",
        pulse_rate_hz=1.2 if active_count == 0 else 2.5,
        glow_color_hex="#00D2FF" if active_count == 0 else "#9D00FF",
        active_connections=max(1, active_count),
    )


@app.post("/api/v1/agent/task", response_model=AgentTaskResponse, summary="Agent task execution")
async def execute_agent_task(request: AgentTaskRequest):
    """Executes a defined agent task with tool dispatch and returns structured outcome."""
    try:
        response = await ai_service.execute_agent_task(
            task=request.task,
            context=request.context or "",
            tool_hint=request.tool_hint,
        )
        return response
    except Exception as e:
        logger.error("Error in agent task: %s", type(e).__name__)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Agent task could not be processed.",
        )
