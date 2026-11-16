"""SQLite memory with separate legacy-session and authenticated-user ownership."""
import os
import sqlite3
from pathlib import Path
from contextlib import contextmanager

DB_PATH = Path(os.getenv("ZAYTHAR_MEMORY_DB", Path(__file__).resolve().parent / "data" / "memory.db"))
_ACCOUNT_SENTINEL = "__account_owned__"


@contextmanager
def _db():
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(DB_PATH, timeout=10)
    try:
        conn.execute("""CREATE TABLE IF NOT EXISTS memories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id TEXT NOT NULL,
            content TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        )""")
        columns = {row[1] for row in conn.execute("PRAGMA table_info(memories)").fetchall()}
        if "user_id" not in columns:
            conn.execute("ALTER TABLE memories ADD COLUMN user_id INTEGER")
        conn.execute("CREATE INDEX IF NOT EXISTS idx_memories_session ON memories(session_id, id DESC)")
        conn.execute("CREATE INDEX IF NOT EXISTS idx_memories_user ON memories(user_id, id DESC)")
        yield conn
        conn.commit()
    finally:
        conn.close()


def _clean_content(content: str) -> str:
    if not isinstance(content, str):
        raise ValueError("Memory must be text")
    value = content.strip()
    if not value or len(value) > 2000:
        raise ValueError("Memory must contain 1 to 2000 characters")
    return value


def _valid_user_id(user_id: int) -> int:
    if type(user_id) is not int or user_id < 1:
        raise ValueError("Invalid account owner")
    return user_id


# Legacy session functions remain for existing local/unit callers. They can
# read and mutate only rows without an authenticated user_id.
def save_memory(session_id: str, content: str) -> dict:
    value = _clean_content(content)
    with _db() as db:
        cur = db.execute(
            "INSERT INTO memories(session_id, content, user_id) VALUES (?, ?, NULL)",
            (session_id, value),
        )
        return {"id": cur.lastrowid, "content": value}


def list_memories(session_id: str, limit: int = 50) -> list[dict]:
    with _db() as db:
        rows = db.execute(
            "SELECT id, content, created_at FROM memories WHERE session_id=? AND user_id IS NULL ORDER BY id DESC LIMIT ?",
            (session_id, min(max(limit, 1), 100)),
        ).fetchall()
    return [{"id": r[0], "content": r[1], "created_at": r[2]} for r in rows]


def delete_memory(session_id: str, memory_id: int) -> bool:
    with _db() as db:
        cur = db.execute(
            "DELETE FROM memories WHERE session_id=? AND id=? AND user_id IS NULL",
            (session_id, memory_id),
        )
        return cur.rowcount > 0


def clear_memories(session_id: str) -> int:
    with _db() as db:
        cur = db.execute("DELETE FROM memories WHERE session_id=? AND user_id IS NULL", (session_id,))
        return cur.rowcount


def memory_context(session_id: str) -> str:
    memories = list_memories(session_id, 20)
    return "\n".join("- " + item["content"] for item in reversed(memories))


# Authenticated account functions never use or compare client-selected session IDs.
def save_user_memory(user_id: int, content: str) -> dict:
    owner = _valid_user_id(user_id)
    value = _clean_content(content)
    with _db() as db:
        cur = db.execute(
            "INSERT INTO memories(session_id, content, user_id) VALUES (?, ?, ?)",
            (_ACCOUNT_SENTINEL, value, owner),
        )
        return {"id": cur.lastrowid, "content": value}


def list_user_memories(user_id: int, limit: int = 50) -> list[dict]:
    owner = _valid_user_id(user_id)
    with _db() as db:
        rows = db.execute(
            "SELECT id, content, created_at FROM memories WHERE user_id=? ORDER BY id DESC LIMIT ?",
            (owner, min(max(limit, 1), 100)),
        ).fetchall()
    return [{"id": r[0], "content": r[1], "created_at": r[2]} for r in rows]


def delete_user_memory(user_id: int, memory_id: int) -> bool:
    owner = _valid_user_id(user_id)
    with _db() as db:
        cur = db.execute("DELETE FROM memories WHERE user_id=? AND id=?", (owner, memory_id))
        return cur.rowcount > 0


def clear_user_memories(user_id: int) -> int:
    owner = _valid_user_id(user_id)
    with _db() as db:
        cur = db.execute("DELETE FROM memories WHERE user_id=?", (owner,))
        return cur.rowcount


def user_memory_context(user_id: int) -> str:
    memories = list_user_memories(user_id, 20)
    return "\n".join("- " + item["content"] for item in reversed(memories))


def update_user_memory(user_id: int, memory_id: int, content: str) -> bool:
    owner = _valid_user_id(user_id)
    value = _clean_content(content)
    with _db() as db:
        cur = db.execute(
            "UPDATE memories SET content=? WHERE user_id=? AND id=?",
            (value, owner, memory_id),
        )
        return cur.rowcount > 0
