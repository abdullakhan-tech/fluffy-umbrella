"""SQLite-backed memory using server-derived account namespaces for API ownership."""
import os
import sqlite3
from pathlib import Path
from contextlib import contextmanager

DB_PATH = Path(os.getenv("ZAYTHAR_MEMORY_DB", Path(__file__).resolve().parent / "data" / "memory.db"))

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
        conn.execute("CREATE INDEX IF NOT EXISTS idx_memories_session ON memories(session_id, id DESC)")
        yield conn
        conn.commit()
    finally:
        conn.close()

def save_memory(session_id: str, content: str) -> dict:
    value = content.strip()
    if not value or len(value) > 2000:
        raise ValueError("Memory must contain 1 to 2000 characters")
    with _db() as db:
        cur = db.execute("INSERT INTO memories(session_id, content) VALUES (?, ?)", (session_id, value))
        return {"id": cur.lastrowid, "content": value}

def list_memories(session_id: str, limit: int = 50) -> list[dict]:
    with _db() as db:
        rows = db.execute("SELECT id, content, created_at FROM memories WHERE session_id=? ORDER BY id DESC LIMIT ?", (session_id, min(max(limit, 1), 100))).fetchall()
    return [{"id": r[0], "content": r[1], "created_at": r[2]} for r in rows]

def delete_memory(session_id: str, memory_id: int) -> bool:
    with _db() as db:
        cur = db.execute("DELETE FROM memories WHERE session_id=? AND id=?", (session_id, memory_id))
        return cur.rowcount > 0

def clear_memories(session_id: str) -> int:
    with _db() as db:
        cur = db.execute("DELETE FROM memories WHERE session_id=?", (session_id,))
        return cur.rowcount

def memory_context(session_id: str) -> str:
    memories = list_memories(session_id, 20)
    return "\n".join("- " + item["content"] for item in reversed(memories))
