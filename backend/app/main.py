"""FastAPI application entrypoint for the authenticated Zaythar AI Companion API."""
from contextlib import asynccontextmanager
import logging
from typing import List

from fastapi import Depends, FastAPI, HTTPException, Path, Request, status
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
from app.security import AuthenticatedUser, require_authenticated_user
from app.services.ai_service import ai_service
from app.services.session_service import session_manager

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
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
    description="Firebase-authenticated Python AI Companion API for Zaythar AI",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=False,
    allow_methods=["GET", "POST", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Firebase-AppCheck"],
)


@app.middleware("http")
async def security_headers_and_body_limit(request: Request, call_next):
    content_length = request.headers.get("content-length")
    if content_length:
        try:
            if int(content_length) > settings.max_request_body_bytes:
                return JSONResponse(status_code=413, content={"detail": "Request body is too large."})
        except ValueError:
            return JSONResponse(status_code=400, content={"detail": "Invalid Content-Length header."})

    response = await call_next(request)
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["Referrer-Policy"] = "no-referrer"
    response.headers["Cache-Control"] = "no-store"
    return response


@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    # Do not log exception messages or tracebacks: provider URLs may contain credentials.
    logger.error("Unhandled request failure (%s)", type(exc).__name__)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"detail": "Internal server error."},
    )


@app.get("/", summary="Root index")
async def root():
    return {"app": settings.app_name, "version": settings.version, "status": "online", "docs_url": "/docs"}


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
    return await health()


@app.post("/api/v1/chat", response_model=ChatResponse, summary="Authenticated chat completion")
async def chat_completion(
    request: ChatRequest,
    user: AuthenticatedUser = Depends(require_authenticated_user),
):
    try:
        return await ai_service.generate_chat_reply(
            messages=request.messages,
            preferred_provider=request.provider or "auto",
            model_override=request.model,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
            session_id=request.session_id,
            owner_id=user.uid,
        )
    except Exception as exc:
        logger.error("Chat completion failed (%s)", type(exc).__name__)
        raise HTTPException(status_code=503, detail="Chat completion is temporarily unavailable.") from None


@app.post("/api/v1/chat/stream", summary="Authenticated SSE token streaming")
async def chat_stream(
    request: ChatRequest,
    user: AuthenticatedUser = Depends(require_authenticated_user),
):
    return StreamingResponse(
        ai_service.generate_chat_stream(
            messages=request.messages,
            preferred_provider=request.provider or "auto",
            model_override=request.model,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
            session_id=request.session_id,
            owner_id=user.uid,
        ),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-store", "X-Accel-Buffering": "no"},
    )


@app.get("/api/v1/sessions", response_model=List[SessionSummary], summary="List current user's sessions")
async def list_sessions(user: AuthenticatedUser = Depends(require_authenticated_user)):
    return await session_manager.list_sessions(user.uid)


@app.get("/api/v1/sessions/{session_id}", response_model=List[ChatMessage], summary="Get owned session history")
async def get_session_history(
    session_id: str = Path(..., min_length=1, max_length=100, pattern=r"^[A-Za-z0-9_-]+$"),
    user: AuthenticatedUser = Depends(require_authenticated_user),
):
    if not await session_manager.session_exists(user.uid, session_id):
        raise HTTPException(status_code=404, detail="Session not found.")
    return await session_manager.get_history(user.uid, session_id)


@app.delete("/api/v1/sessions/{session_id}", summary="Delete an owned session")
async def delete_session(
    session_id: str = Path(..., min_length=1, max_length=100, pattern=r"^[A-Za-z0-9_-]+$"),
    user: AuthenticatedUser = Depends(require_authenticated_user),
):
    deleted = await session_manager.clear_session(user.uid, session_id)
    return {"session_id": session_id, "deleted": deleted}


@app.post("/api/v1/sessions/{session_id}/chat", response_model=ChatResponse, summary="Authenticated session-aware chat")
async def session_chat(
    request: SessionChatRequest,
    session_id: str = Path(..., min_length=1, max_length=100, pattern=r"^[A-Za-z0-9_-]+$"),
    user: AuthenticatedUser = Depends(require_authenticated_user),
):
    from app.models import ChatRole

    await session_manager.get_or_create_session(user.uid, session_id)
    history = await session_manager.get_history(user.uid, session_id)
    full_messages = history + [ChatMessage(role=ChatRole.USER, content=request.message)]
    return await ai_service.generate_chat_reply(
        messages=full_messages,
        preferred_provider=request.provider or "auto",
        model_override=request.model,
        temperature=request.temperature,
        session_id=session_id,
        owner_id=user.uid,
    )


@app.get("/api/v1/orb/animation-state", response_model=OrbVisualState, summary="Public static orb animation hint")
async def orb_visual_state():
    # Do not expose process-wide active session counts through a public UI hint.
    return OrbVisualState(
        state="idle",
        pulse_rate_hz=1.2,
        glow_color_hex="#00D2FF",
        active_connections=1,
    )


@app.post("/api/v1/agent/task", response_model=AgentTaskResponse, summary="Authenticated allow-listed agent task")
async def execute_agent_task(
    request: AgentTaskRequest,
    user: AuthenticatedUser = Depends(require_authenticated_user),
):
    try:
        return await ai_service.execute_agent_task(
            task=request.task,
            context=request.context or "",
            tool_hint=request.tool_hint,
        )
    except Exception as exc:
        logger.error("Agent task failed (%s)", type(exc).__name__)
        raise HTTPException(status_code=503, detail="Agent task is temporarily unavailable.") from None
