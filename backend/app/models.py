"""Data validation models enforcing input/output boundaries (Rule 5)."""

from datetime import datetime, timezone
from enum import Enum
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class ChatRole(str, Enum):
    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"


class ChatMessage(BaseModel):
    role: ChatRole = Field(..., description="Role of the sender")
    content: str = Field(..., min_length=1, max_length=4000, description="Message text")
    timestamp: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc),
        description="UTC timestamp of the message",
    )


class ChatRequest(BaseModel):
    messages: List[ChatMessage] = Field(..., min_length=1, max_length=50, description="Conversation history")
    provider: Optional[str] = Field(default="auto", description="Preferred AI provider")
    model: Optional[str] = Field(default=None, max_length=60, description="Specific model override")
    temperature: float = Field(default=0.7, ge=0.0, le=2.0, description="Sampling temperature")
    max_tokens: int = Field(default=1024, ge=1, le=4096, description="Maximum tokens")
    session_id: Optional[str] = Field(default=None, max_length=100, description="Optional session identifier for history")
    personality: str = Field(default="friday", pattern="^(friday|professional|friendly|developer|researcher)$", description="Assistant response personality")


class ChatResponse(BaseModel):
    reply: str = Field(..., description="Generated assistant response")
    provider_used: str = Field(..., description="The AI provider that fulfilled the request")
    model_used: str = Field(..., description="The model that produced the reply")
    status: str = Field(default="success", description="Status indicator")
    session_id: Optional[str] = Field(default=None, description="Active session ID if tracked")


class ToolType(str, Enum):
    CALCULATOR = "calculator"
    SUMMARIZE = "summarize"
    CODE_INSPECT = "code_inspect"
    GENERAL = "general"


class AgentTaskRequest(BaseModel):
    task: str = Field(..., min_length=1, max_length=2000, description="Task instruction for the AI agent")
    context: Optional[str] = Field(default="", max_length=4000, description="Optional supporting context")
    tool_hint: Optional[ToolType] = Field(default=None, description="Explicit tool hint if known")


class AgentTaskResponse(BaseModel):
    task: str = Field(..., description="Original task query")
    status: str = Field(..., description="Execution status")
    summary: str = Field(..., description="Executive summary of the agent outcome")
    tool_used: str = Field(default="none", description="Name of the deterministic tool executed")
    tool_result: Optional[Dict[str, Any]] = Field(default=None, description="Structured tool output data")
    actions_taken: List[str] = Field(default_factory=list, description="Step-by-step actions executed")


class SessionSummary(BaseModel):
    session_id: str = Field(..., description="Unique session ID")
    message_count: int = Field(..., description="Number of stored messages")
    created_at: datetime = Field(..., description="Session creation timestamp")
    last_active: datetime = Field(..., description="Last message timestamp")


class SessionChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=4000, description="User prompt message")
    provider: Optional[str] = Field(default="auto", description="Preferred AI provider")
    model: Optional[str] = Field(default=None, max_length=60, description="Specific model override")
    temperature: float = Field(default=0.7, ge=0.0, le=2.0, description="Sampling temperature")


class OrbVisualState(BaseModel):
    """Animation parameters for client-side Orb UI rendering (Rule 1)."""
    state: str = Field(..., description="Visual animation state: idle, listening, processing, speaking")
    pulse_rate_hz: float = Field(default=1.0, ge=0.1, le=10.0, description="Orb pulse frequency")
    glow_color_hex: str = Field(default="#00D2FF", description="Primary glow hex color for Compose Canvas")
    active_connections: int = Field(default=1, ge=0, description="Current active client connections")


class HealthResponse(BaseModel):
    status: str = Field(default="healthy")
    app: str
    version: str
    environment: str
