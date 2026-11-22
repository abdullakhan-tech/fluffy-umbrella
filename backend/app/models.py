"""Strict request and response models enforcing input boundaries."""
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, ConfigDict, Field, field_validator


class StrictInputModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class ChatRole(str, Enum):
    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"


class ChatMessage(StrictInputModel):
    role: ChatRole = Field(..., description="The sender role")
    content: str = Field(..., min_length=1, max_length=4000, description="Message text")
    timestamp: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))


class ChatRequest(StrictInputModel):
    messages: List[ChatMessage] = Field(..., min_length=1, max_length=50)
    provider: Optional[str] = Field(default="auto", max_length=20)
    model: Optional[str] = Field(default=None, max_length=60, pattern=r"^[A-Za-z0-9._:-]+$")
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)
    max_tokens: int = Field(default=1024, ge=1, le=4096)
    session_id: Optional[str] = Field(default=None, max_length=100, pattern=r"^[A-Za-z0-9_-]+$")

    @field_validator("messages")
    @classmethod
    def reject_client_system_messages(cls, messages: List[ChatMessage]) -> List[ChatMessage]:
        if any(message.role == ChatRole.SYSTEM for message in messages):
            raise ValueError("System messages are reserved for server policy.")
        return messages


class ChatResponse(BaseModel):
    reply: str
    provider_used: str
    model_used: str
    status: str = "success"
    session_id: Optional[str] = None


class ToolType(str, Enum):
    CALCULATOR = "calculator"
    SUMMARIZE = "summarize"
    CODE_INSPECT = "code_inspect"
    GENERAL = "general"


class AgentTaskRequest(StrictInputModel):
    task: str = Field(..., min_length=1, max_length=2000)
    context: Optional[str] = Field(default="", max_length=4000)
    tool_hint: Optional[ToolType] = None


class AgentTaskResponse(BaseModel):
    task: str
    status: str
    summary: str
    tool_used: str = "none"
    tool_result: Optional[Dict[str, Any]] = None
    actions_taken: List[str] = Field(default_factory=list)


class SessionSummary(BaseModel):
    session_id: str
    message_count: int
    created_at: datetime
    last_active: datetime


class SessionChatRequest(StrictInputModel):
    message: str = Field(..., min_length=1, max_length=4000)
    provider: Optional[str] = Field(default="auto", max_length=20)
    model: Optional[str] = Field(default=None, max_length=60, pattern=r"^[A-Za-z0-9._:-]+$")
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)


class OrbVisualState(BaseModel):
    state: str
    pulse_rate_hz: float = Field(default=1.0, ge=0.1, le=10.0)
    glow_color_hex: str = Field(default="#00D2FF", pattern=r"^#[0-9A-Fa-f]{6}$")
    active_connections: int = Field(default=1, ge=0, le=1000)


class HealthResponse(BaseModel):
    status: str = "healthy"
    app: str
    version: str
    environment: str
