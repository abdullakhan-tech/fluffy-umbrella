"""FastAPI application entrypoint for Zaythar AI Companion Backend."""

from contextlib import asynccontextmanager
import logging

from fastapi import FastAPI, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import settings
from app.models import (
    AgentTaskRequest,
    AgentTaskResponse,
    ChatRequest,
    ChatResponse,
    HealthResponse,
)
from app.services.ai_service import ai_service

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
        )
        return response
    except Exception as e:
        logger.error("Error in chat completion: %s", type(e).__name__)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Chat completion service encountered an error.",
        )


@app.post("/api/v1/agent/task", response_model=AgentTaskResponse, summary="Agent task execution")
async def execute_agent_task(request: AgentTaskRequest):
    """Executes a defined agent task and returns structured outcome."""
    try:
        response = await ai_service.execute_agent_task(
            task=request.task,
            context=request.context or "",
        )
        return response
    except Exception as e:
        logger.error("Error in agent task: %s", type(e).__name__)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Agent task could not be processed.",
        )
