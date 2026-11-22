"""Owner-scoped in-memory conversation sessions with bounded retention."""
import asyncio
import re
from datetime import datetime, timezone
from typing import Dict, List, Optional, Tuple
import uuid

from app.models import ChatMessage, SessionSummary


SessionKey = Tuple[str, str]
SESSION_ID_PATTERN = re.compile(r"^[A-Za-z0-9_-]{1,100}$")


class SessionManager:
    """Stores sessions under (verified Firebase UID, session ID), never session ID alone."""

    MAX_MESSAGES_PER_SESSION = 40
    MAX_SESSIONS = 500
    MAX_SESSIONS_PER_USER = 50

    def __init__(self):
        self._sessions: Dict[SessionKey, List[ChatMessage]] = {}
        self._created_at: Dict[SessionKey, datetime] = {}
        self._last_active: Dict[SessionKey, datetime] = {}
        self._lock = asyncio.Lock()

    @staticmethod
    def _key(owner_id: str, session_id: str) -> SessionKey:
        if not owner_id or not owner_id.strip():
            raise ValueError("Authenticated owner is required.")
        if not SESSION_ID_PATTERN.fullmatch(session_id):
            raise ValueError("Invalid session identifier.")
        return owner_id, session_id

    async def get_or_create_session(self, owner_id: str, session_id: Optional[str] = None) -> str:
        """Creates a random session ID or opens one inside the verified owner's namespace."""
        sid = session_id.strip() if session_id and session_id.strip() else uuid.uuid4().hex
        key = self._key(owner_id, sid)
        async with self._lock:
            now = datetime.now(timezone.utc)
            if key not in self._sessions:
                owned = [candidate for candidate in self._sessions if candidate[0] == owner_id]
                if len(owned) >= self.MAX_SESSIONS_PER_USER:
                    oldest_owned = min(owned, key=lambda candidate: self._last_active[candidate])
                    self._remove(oldest_owned)
                if len(self._sessions) >= self.MAX_SESSIONS:
                    oldest = min(self._last_active, key=self._last_active.get)
                    self._remove(oldest)
                self._sessions[key] = []
                self._created_at[key] = now
            self._last_active[key] = now
            return sid

    def _remove(self, key: SessionKey) -> None:
        self._sessions.pop(key, None)
        self._created_at.pop(key, None)
        self._last_active.pop(key, None)

    async def add_message(self, owner_id: str, session_id: str, message: ChatMessage) -> None:
        key = self._key(owner_id, session_id)
        async with self._lock:
            if key not in self._sessions:
                now = datetime.now(timezone.utc)
                self._sessions[key] = []
                self._created_at[key] = now
            self._sessions[key].append(message)
            self._last_active[key] = datetime.now(timezone.utc)
            if len(self._sessions[key]) > self.MAX_MESSAGES_PER_SESSION:
                self._sessions[key] = self._sessions[key][-self.MAX_MESSAGES_PER_SESSION :]

    async def session_exists(self, owner_id: str, session_id: str) -> bool:
        key = self._key(owner_id, session_id)
        async with self._lock:
            return key in self._sessions

    async def get_history(self, owner_id: str, session_id: str) -> List[ChatMessage]:
        key = self._key(owner_id, session_id)
        async with self._lock:
            return list(self._sessions.get(key, []))

    async def clear_session(self, owner_id: str, session_id: str) -> bool:
        key = self._key(owner_id, session_id)
        async with self._lock:
            if key not in self._sessions:
                return False
            self._remove(key)
            return True

    async def list_sessions(self, owner_id: str) -> List[SessionSummary]:
        if not owner_id or not owner_id.strip():
            raise ValueError("Authenticated owner is required.")
        async with self._lock:
            summaries = [
                SessionSummary(
                    session_id=session_id,
                    message_count=len(messages),
                    created_at=self._created_at[key],
                    last_active=self._last_active[key],
                )
                for key, messages in self._sessions.items()
                if key[0] == owner_id
                for session_id in [key[1]]
            ]
            return sorted(summaries, key=lambda item: item.last_active, reverse=True)


session_manager = SessionManager()
