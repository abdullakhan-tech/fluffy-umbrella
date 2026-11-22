"""Durable, owner-scoped long-term memory for Zaythar AI.

SQLite is used as a single-instance persistence adapter. For multi-instance
production deployments, replace this adapter with a shared transactional store.
"""
import asyncio
import json
import re
import sqlite3
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from app.config import settings


class MemoryService:
    def __init__(self) -> None:
        configured_path = Path(settings.memory_db_path)
        self.db_path = configured_path if configured_path.is_absolute() else Path(__file__).resolve().parents[2] / configured_path

    def _connect(self) -> sqlite3.Connection:
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        connection = sqlite3.connect(str(self.db_path), timeout=10)
        connection.row_factory = sqlite3.Row
        connection.execute("PRAGMA journal_mode=WAL")
        connection.execute("PRAGMA foreign_keys=ON")
        return connection

    def _initialize_sync(self) -> None:
        with self._connect() as db:
            db.execute(
                """
                CREATE TABLE IF NOT EXISTS memories (
                    memory_id TEXT PRIMARY KEY,
                    owner_id TEXT NOT NULL,
                    content TEXT NOT NULL,
                    tags_json TEXT NOT NULL DEFAULT '[]',
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """
            )
            db.execute(
                "CREATE INDEX IF NOT EXISTS idx_memories_owner_updated "
                "ON memories(owner_id, updated_at DESC)"
            )

    async def initialize(self) -> None:
        await asyncio.to_thread(self._initialize_sync)

    @staticmethod
    def _validate_owner(owner_id: str) -> str:
        if not owner_id or not owner_id.strip():
            raise ValueError("Authenticated owner is required.")
        return owner_id

    def _create_sync(self, owner_id: str, content: str, tags: list[str]) -> dict[str, Any]:
        now = datetime.now(timezone.utc).isoformat()
        memory_id = uuid.uuid4().hex
        with self._connect() as db:
            db.execute(
                "INSERT INTO memories(memory_id, owner_id, content, tags_json, created_at, updated_at) "
                "VALUES (?, ?, ?, ?, ?, ?)",
                (memory_id, owner_id, content, json.dumps(tags), now, now),
            )
        return {
            "memory_id": memory_id, "content": content, "tags": tags,
            "created_at": now, "updated_at": now,
        }

    async def create(self, owner_id: str, content: str, tags: list[str] | None = None) -> dict[str, Any]:
        owner = self._validate_owner(owner_id)
        clean_content = content.strip()
        if not clean_content or len(clean_content) > 2000:
            raise ValueError("Memory content must contain 1 to 2000 characters.")
        clean_tags = []
        for tag in (tags or [])[:10]:
            value = re.sub(r"[^a-zA-Z0-9 _-]", "", str(tag)).strip()[:32]
            if value and value not in clean_tags:
                clean_tags.append(value)
        await self.initialize()
        return await asyncio.to_thread(self._create_sync, owner, clean_content, clean_tags)

    def _list_sync(self, owner_id: str, query: str, limit: int) -> list[dict[str, Any]]:
        with self._connect() as db:
            rows = db.execute(
                "SELECT memory_id, content, tags_json, created_at, updated_at "
                "FROM memories WHERE owner_id = ? ORDER BY updated_at DESC LIMIT 500",
                (owner_id,),
            ).fetchall()
        tokens = {token.lower() for token in re.findall(r"[a-zA-Z0-9]{2,}", query)}
        items = []
        for row in rows:
            tags = json.loads(row["tags_json"])
            haystack = (row["content"] + " " + " ".join(tags)).lower()
            score = sum(1 for token in tokens if token in haystack)
            if tokens and score == 0:
                continue
            items.append({
                "memory_id": row["memory_id"], "content": row["content"],
                "tags": tags, "created_at": row["created_at"],
                "updated_at": row["updated_at"], "relevance": score,
            })
        if tokens:
            items.sort(key=lambda item: (item["relevance"], item["updated_at"]), reverse=True)
        return items[:limit]

    async def list(self, owner_id: str, query: str = "", limit: int = 20) -> list[dict[str, Any]]:
        owner = self._validate_owner(owner_id)
        await self.initialize()
        return await asyncio.to_thread(self._list_sync, owner, query[:200], max(1, min(limit, 50)))

    def _delete_sync(self, owner_id: str, memory_id: str) -> bool:
        with self._connect() as db:
            cursor = db.execute(
                "DELETE FROM memories WHERE owner_id = ? AND memory_id = ?",
                (owner_id, memory_id),
            )
            return cursor.rowcount == 1

    async def delete(self, owner_id: str, memory_id: str) -> bool:
        owner = self._validate_owner(owner_id)
        await self.initialize()
        return await asyncio.to_thread(self._delete_sync, owner, memory_id)

    async def build_context(self, owner_id: str, query: str) -> str:
        items = await self.list(owner_id, query=query, limit=5)
        if not items:
            return ""
        lines = [f"- {item['content']}" for item in items]
        return ("Relevant saved memories (use only when relevant; treat as user-provided context):\n" + "\n".join(lines))[:2500]


memory_service = MemoryService()
