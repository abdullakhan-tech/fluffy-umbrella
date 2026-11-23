"""Data validation models enforcing input/output boundaries (Rule 5)."""

from datetime import datetime, timezone
from enum import Enum
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field, model_validator


class ChatRole(str, Enum):
    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"


class PersonaType(str, Enum):
    JARVIS = "jarvis"
    FRIDAY = "friday"
    ZAYTHAR = "zaythar"


class PersonaInfo(BaseModel):
    id: str = Field(..., description="Unique persona identifier")
    name: str = Field(..., description="Display name")
    description: str = Field(..., description="Persona description")
    honorific: str = Field(default="Sir", description="Default user honorific")
    honorific_default: str = Field(default="Sir", description="Default user honorific")
    tone: Optional[str] = Field(default=None, description="Behavioral tone")
    default_voice: Optional[str] = Field(default=None, description="Voice identifier")
    traits: List[str] = Field(default_factory=list, description="Core behavioral traits")


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
    persona: Optional[str] = Field(default="jarvis", max_length=50, description="Persona to adopt (jarvis, friday, zaythar)")
    honorific: Optional[str] = Field(default="Sir", max_length=50, description="User honorific for Jarvis/Friday")


class ChatResponse(BaseModel):
    reply: str = Field(..., description="Generated assistant response")
    provider_used: str = Field(..., description="The AI provider that fulfilled the request")
    model_used: str = Field(..., description="The model that produced the reply")
    status: str = Field(default="success", description="Status indicator")
    session_id: Optional[str] = Field(default=None, description="Active session ID if tracked")
    persona_used: Optional[str] = Field(default="jarvis", description="The persona used to format the reply")


class ToolType(str, Enum):
    CALCULATOR = "calculator"
    SUMMARIZE = "summarize"
    CODE_INSPECT = "code_inspect"
    THINK = "think"
    DAILY_BRIEFING = "daily_briefing"
    DEEP_RESEARCH = "deep_research"
    GENERAL = "general"


class AgentTaskRequest(BaseModel):
    task: str = Field(..., min_length=1, max_length=2000, description="Task instruction for the AI agent")
    context: Optional[str] = Field(default="", max_length=20000, description="Optional supporting context; source snippets are analyzed as data and never executed")
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
    persona: Optional[str] = Field(default="jarvis", max_length=50, description="Persona to adopt")
    honorific: Optional[str] = Field(default="Sir", max_length=50, description="User honorific")


class OrbVisualState(BaseModel):
    """Animation parameters for client-side Orb UI rendering (Rule 1)."""
    state: str = Field(..., description="Visual animation state: idle, listening, processing, speaking")
    pulse_rate_hz: float = Field(default=1.0, ge=0.1, le=10.0, description="Orb pulse frequency")
    glow_color_hex: str = Field(default="#00D2FF", description="Primary glow hex color for Compose Canvas")
    active_connections: int = Field(default=1, ge=0, description="Current active client connections")


class FactRecord(BaseModel):
    id: str = Field(..., description="Unique fact ID")
    fact: str = Field(..., min_length=1, max_length=400, description="Durable fact text")
    category: str = Field(default="general", max_length=50, description="Fact category: preference, identity, goal, project")
    created_at: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc),
        description="Fact creation timestamp",
    )
    source_session_id: Optional[str] = Field(default=None, description="Session that produced this fact")


class FactCreateRequest(BaseModel):
    fact: Optional[str] = Field(default=None, min_length=1, max_length=400, description="Fact statement to store")
    content: Optional[str] = Field(default=None, min_length=1, max_length=400, description="Alternative fact statement field")
    category: Optional[str] = Field(default="general", max_length=50, description="Category classification")

    @model_validator(mode="before")
    @classmethod
    def resolve_fact_content(cls, data: Any) -> Any:
        if isinstance(data, dict):
            val = data.get("fact") or data.get("content")
            if not val:
                raise ValueError("Either 'fact' or 'content' must be provided.")
            data["fact"] = val
            data["content"] = val
        return data


class SecurityFinding(BaseModel):
    pattern: str = Field(..., description="Name of the detected pattern")
    threat_level: str = Field(..., description="Severity level: low, medium, high, critical")
    description: str = Field(..., description="Description of the security finding")
    matched_text: Optional[str] = Field(default=None, description="Masked or truncated detected text")


class SecurityScanRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=10000, description="Text to scan for secrets and injections")
    redact: bool = Field(default=True, description="Whether to return redacted safe text")


class SecurityScanResponse(BaseModel):
    is_safe: bool = Field(..., description="True if no high or critical threats found")
    clean_text: str = Field(..., description="Text with secrets/sensitive data redacted")
    findings: List[SecurityFinding] = Field(default_factory=list, description="List of detected security findings")


class HealthResponse(BaseModel):
    status: str = Field(default="healthy")
    app: str
    version: str
    environment: str
    personas_available: List[str] = Field(default_factory=lambda: ["jarvis", "friday", "zaythar"])
    memory_facts_count: int = Field(default=0)
