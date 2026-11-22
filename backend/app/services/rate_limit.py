"""Process-local sliding-window limits for authenticated API work.

This protects a single worker. Production multi-worker deployments must use a
shared transactional store (for example Redis or Firestore) before scaling out.
"""
import asyncio
import time
from collections import deque
from typing import Deque, Dict, Tuple

from fastapi import HTTPException

from app.config import settings


class SlidingWindowRateLimiter:
    def __init__(self, user_limit: int = 60, ip_limit: int = 120, window_seconds: int = 60):
        self.user_limit = max(1, user_limit)
        self.ip_limit = max(1, ip_limit)
        self.window_seconds = max(1, window_seconds)
        self._events: Dict[Tuple[str, str], Deque[float]] = {}
        self._lock = asyncio.Lock()

    def reset(self) -> None:
        """Clear counters for deterministic tests and controlled process shutdown."""
        self._events.clear()

    def _prune(self, now: float) -> None:
        for key in list(self._events):
            events = self._events[key]
            while events and now - events[0] >= self.window_seconds:
                events.popleft()
            if not events:
                del self._events[key]

        while len(self._events) > 10000:
            self._events.pop(next(iter(self._events)))

    async def enforce(self, user_id: str, client_ip: str) -> None:
        now = time.monotonic()
        buckets = (
            (("user", user_id), self.user_limit),
            (("ip", client_ip), self.ip_limit),
        )
        async with self._lock:
            self._prune(now)
            for key, limit in buckets:
                events = self._events.get(key, deque())
                if len(events) >= limit:
                    retry_after = max(1, int(self.window_seconds - (now - events[0])))
                    raise HTTPException(
                        status_code=429,
                        detail="Request rate limit exceeded. Retry later.",
                        headers={"Retry-After": str(retry_after)},
                    )
            for key, _ in buckets:
                self._events.setdefault(key, deque()).append(now)


rate_limiter = SlidingWindowRateLimiter(
    user_limit=settings.rate_limit_user_per_minute,
    ip_limit=settings.rate_limit_ip_per_minute,
)
