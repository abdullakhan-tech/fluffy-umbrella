"""Data validation models enforcing input/output boundaries (Rule 5)."""

from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, Field


class ChatRole(str, Enum):
    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"


class ChatMessage(BaseModel):
    role: ChatRole = Field(..., description="Role of the sender")
    content: str = Field(..., min_length=1, max_length=4000, description="Message text")


class ChatRequest(BaseModel):
    messages: List[ChatMessage] = Field(..., min_length=1, max_length=50, description="Conversation history")
    provider: Optional[str] = Field(default="auto", description="Preferred AI provider")
    model: Optional[str] = Field(default=None, max_length=60, description="Specific model override")
    temperature: float = Field(default=0.7, ge=0.0, le=2.0, description="Sampling temperature")
    max_tokens: int = Field(default=1024, ge=1, le=4096, description="Maximum tokens")


class ChatResponse(BaseModel):
    reply: str = Field(..., description="Generated assistant response")
    provider_used: str = Field(..., description="The AI provider that fulfilled the request")
    model_used: str = Field(..., description="The model that produced the reply")
    status: str = Field(default="success", description="Status indicator")


class AgentTaskRequest(BaseModel):
    task: str = Field(..., min_length=1, max_length=2000, description="Task instruction for the AI agent")
    context: Optional[str] = Field(default="", max_length=4000, description="Optional supporting context")


class AgentTaskResponse(BaseModel):
    task: str = Field(..., description="Original task query")
    status: str = Field(..., description="Execution status")
    summary: str = Field(..., description="Executive summary of the agent outcome")
    actions_taken: List[str] = Field(default_factory=list, description="Step-by-step actions executed")


class HealthResponse(BaseModel):
    status: str = Field(default="healthy")
    app: str
    version: str
    environment: str
