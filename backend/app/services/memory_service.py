"""OpenJarvis-inspired Long-Term Memory and Durable Fact Extractor service."""

from __future__ import annotations

import asyncio
from datetime import datetime, timezone
import json
import logging
from pathlib import Path
import re
from typing import Dict, List, Optional
import uuid

from app.models import FactRecord

logger = logging.getLogger(__name__)


class FactStore:
    """Thread-safe persistent store for durable user facts."""

    MAX_FACTS = 500

    def __init__(self, storage_path: Optional[str] = None):
        if storage_path:
            self.storage_path = Path(storage_path)
        else:
            base_dir = Path(__file__).resolve().parent.parent.parent
            self.storage_path = base_dir / "data" / "facts.json"

        self._facts: Dict[str, FactRecord] = {}
        self._lock = asyncio.Lock()
        self._load_from_disk()

    def _load_from_disk(self) -> None:
        try:
            if self.storage_path.exists():
                with open(self.storage_path, "r", encoding="utf-8") as f:
                    raw_data = json.load(f)
                    for item in raw_data:
                        record = FactRecord(**item)
                        self._facts[record.id] = record
                logger.info("Loaded %d durable memory facts from disk.", len(self._facts))
        except Exception as e:
            logger.warning("Could not load facts from disk (%s). Starting with fresh store.", e)

    def _save_to_disk(self) -> None:
        try:
            self.storage_path.parent.mkdir(parents=True, exist_ok=True)
            with open(self.storage_path, "w", encoding="utf-8") as f:
                data = [f.model_dump(mode="json") for f in self._facts.values()]
                json.dump(data, f, indent=2, default=str)
        except Exception as e:
            logger.warning("Failed to save memory facts to disk: %s", e)

    async def add_fact(
        self,
        fact_text: str,
        category: str = "general",
        session_id: Optional[str] = None,
    ) -> Optional[FactRecord]:
        clean = fact_text.strip()
        if not clean or len(clean) > 400:
            return None

        async with self._lock:
            key = clean.lower()
            for existing in self._facts.values():
                if existing.fact.lower() == key:
                    return existing

            if len(self._facts) >= self.MAX_FACTS:
                oldest_id = min(self._facts.keys(), key=lambda k: self._facts[k].created_at)
                del self._facts[oldest_id]

            record = FactRecord(
                id=str(uuid.uuid4())[:8],
                fact=clean,
                category=category,
                created_at=datetime.now(timezone.utc),
                source_session_id=session_id,
            )
            self._facts[record.id] = record
            self._save_to_disk()
            return record

    async def list_facts(self, limit: int = 50) -> List[FactRecord]:
        async with self._lock:
            facts = list(self._facts.values())
            facts.sort(key=lambda x: x.created_at, reverse=True)
            return facts[:limit]

    async def delete_fact(self, fact_id: str) -> bool:
        async with self._lock:
            if fact_id in self._facts:
                del self._facts[fact_id]
                self._save_to_disk()
                return True
            return False

    async def clear_all(self) -> None:
        async with self._lock:
            self._facts.clear()
            self._save_to_disk()


class FactExtractor:
    """Heuristic and pattern-based durable fact extractor."""

    @classmethod
    def extract_from_turn(cls, user_text: str, assistant_text: str = "") -> List[str]:
        extracted: List[str] = []
        user_clean = (user_text or "").strip()
        if not user_clean:
            return []

        patterns = [
            (r"(?i)\b(?:i prefer|i like|i love|my preference is)\s+([^.!?\n]{3,120})", "preference"),
            (r"(?i)\b(?:my name is|i am|call me)\s+([A-Za-z0-9_\- ]{2,60})", "identity"),
            (r"(?i)\b(?:i am working on|my project is|we are building|building)\s+([^.!?\n]{3,120})", "project"),
            (r"(?i)\b(?:remember that|note that|keep in mind that)\s+([^.!?\n]{3,150})", "general"),
        ]

        for p, category in patterns:
            for match in re.finditer(p, user_clean):
                captured = match.group(0).strip()
                fact = captured.strip(" ,.-:;")
                if len(fact) >= 8 and fact not in extracted:
                    extracted.append(fact)

        return extracted[:5]


class MemoryService:
    """Orchestrates memory persistence and background extraction."""

    def __init__(self):
        self.store = FactStore()
        self.extractor = FactExtractor()

    async def get_recent_facts(self, limit: int = 10) -> List[str]:
        records = await self.store.list_facts(limit=limit)
        return [r.fact for r in records]

    async def get_relevant_facts(self, query: str = "", limit: int = 10) -> List[str]:
        """Returns relevant or recent facts matching user query."""
        all_records = await self.store.list_facts(limit=50)
        if not all_records:
            return []
        if not query:
            return [r.fact for r in all_records[:limit]]

        query_tokens = set(re.findall(r"\w+", query.lower()))
        scored = []
        for r in all_records:
            fact_tokens = set(re.findall(r"\w+", r.fact.lower()))
            overlap = len(query_tokens & fact_tokens)
            scored.append((overlap, r))

        scored.sort(key=lambda x: x[0], reverse=True)
        return [r.fact for _, r in scored[:limit]]

    async def list_facts(self, limit: int = 50) -> List[FactRecord]:
        return await self.store.list_facts(limit=limit)

    async def add_fact(
        self,
        content: str,
        category: str = "general",
        source: str = "manual",
        session_id: Optional[str] = None,
    ) -> Optional[FactRecord]:
        return await self.store.add_fact(fact_text=content, category=category, session_id=session_id)

    async def delete_fact(self, fact_id: str) -> bool:
        return await self.store.delete_fact(fact_id=fact_id)

    async def clear_all(self) -> int:
        count = len(self.store._facts)
        await self.store.clear_all()
        return count

    async def process_turn_and_remember(
        self,
        user_text: str = "",
        assistant_text: str = "",
        user_msg: Optional[str] = None,
        assistant_msg: Optional[str] = None,
        session_id: Optional[str] = None,
    ) -> List[FactRecord]:
        effective_user = (user_msg if user_msg is not None else user_text) or ""
        effective_assistant = (assistant_msg if assistant_msg is not None else assistant_text) or ""
        facts = self.extractor.extract_from_turn(effective_user, effective_assistant)
        saved = []
        for f in facts:
            record = await self.store.add_fact(f, category="auto_extracted", session_id=session_id)
            if record:
                saved.append(record)
        return saved


memory_service = MemoryService()
