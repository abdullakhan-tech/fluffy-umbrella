"""In-memory conversation session manager with safety bounds."""

import asyncio
from datetime import datetime, timezone
from typing import Dict, List, Optional
import uuid

from app.models import ChatMessage, ChatRole, SessionSummary


class SessionManager:
    """Manages active conversation sessions with bounded memory."""

    MAX_MESSAGES_PER_SESSION = 40
    MAX_SESSIONS = 500

    def __init__(self):
        self._sessions: Dict[str, List[ChatMessage]] = {}
        self._created_at: Dict[str, datetime] = {}
        self._last_active: Dict[str, datetime] = {}
        self._lock = asyncio.Lock()

    async def get_or_create_session(self, session_id: Optional[str] = None) -> str:
        """Returns existing session or creates a new UUID session."""
        async with self._lock:
            sid = session_id.strip() if session_id and session_id.strip() else str(uuid.uuid4())
            now = datetime.now(timezone.utc)
            if sid not in self._sessions:
                # Evict oldest if capacity exceeded
                if len(self._sessions) >= self.MAX_SESSIONS:
                    oldest = min(self._last_active.keys(), key=lambda k: self._last_active[k])
                    del self._sessions[oldest]
                    del self._created_at[oldest]
                    del self._last_active[oldest]

                self._sessions[sid] = []
                self._created_at[sid] = now
            self._last_active[sid] = now
            return sid

    async def add_message(self, session_id: str, message: ChatMessage) -> None:
        """Appends a validated message to the session."""
        async with self._lock:
            if session_id not in self._sessions:
                self._sessions[session_id] = []
                self._created_at[session_id] = datetime.now(timezone.utc)

            self._sessions[session_id].append(message)
            self._last_active[session_id] = datetime.now(timezone.utc)

            # Cap messages to prevent memory exhaustion
            if len(self._sessions[session_id]) > self.MAX_MESSAGES_PER_SESSION:
                self._sessions[session_id] = self._sessions[session_id][-self.MAX_MESSAGES_PER_SESSION :]

    async def get_history(self, session_id: str) -> List[ChatMessage]:
        """Returns messages for a session."""
        async with self._lock:
            return list(self._sessions.get(session_id, []))

    async def clear_session(self, session_id: str) -> bool:
        """Deletes a session."""
        async with self._lock:
            if session_id in self._sessions:
                del self._sessions[session_id]
                self._created_at.pop(session_id, None)
                self._last_active.pop(session_id, None)
                return True
            return False

    async def list_sessions(self) -> List[SessionSummary]:
        """Lists active sessions summary."""
        async with self._lock:
            summaries = []
            for sid, msgs in self._sessions.items():
                summaries.append(
                    SessionSummary(
                        session_id=sid,
                        message_count=len(msgs),
                        created_at=self._created_at.get(sid, datetime.now(timezone.utc)),
                        last_active=self._last_active.get(sid, datetime.now(timezone.utc)),
                    )
                )
            return sorted(summaries, key=lambda s: s.last_active, reverse=True)


session_manager = SessionManager()
