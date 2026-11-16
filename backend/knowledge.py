"""Shared, session-scoped knowledge graph for specialist agents.

Facts are explicit user/agent-provided triples. This module does not infer
or silently promote model output into verified knowledge.
"""
import sqlite3
from contextlib import contextmanager
from backend.memory import DB_PATH

MAX_FIELD = 300
MAX_FACTS = 200


@contextmanager
def _connect():
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(DB_PATH, timeout=10)
    try:
        conn.execute("""CREATE TABLE IF NOT EXISTS knowledge_facts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        session_id TEXT NOT NULL,
        subject TEXT NOT NULL,
        predicate TEXT NOT NULL,
        object TEXT NOT NULL,
        source TEXT NOT NULL DEFAULT 'user',
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        )""")
        conn.execute("CREATE INDEX IF NOT EXISTS idx_knowledge_session ON knowledge_facts(session_id, id DESC)")
        yield conn
        conn.commit()
    finally:
        conn.close()


def _clean(value: str, field: str) -> str:
    if not isinstance(value, str):
        raise ValueError(f"{field} must be text")
    value = value.strip()
    if not value or len(value) > MAX_FIELD:
        raise ValueError(f"{field} must contain 1 to {MAX_FIELD} characters")
    return value


def add_fact(session_id: str, subject: str, predicate: str, object: str, source: str = "user") -> dict:
    sid = _clean(session_id, "session_id")
    subject = _clean(subject, "subject")
    predicate = _clean(predicate, "predicate")
    object = _clean(object, "object")
    source = _clean(source, "source")
    with _connect() as db:
        count = db.execute("SELECT COUNT(*) FROM knowledge_facts WHERE session_id=?", (sid,)).fetchone()[0]
        if count >= MAX_FACTS:
            raise ValueError(f"Knowledge graph limit reached ({MAX_FACTS} facts per session)")
        cur = db.execute(
            "INSERT INTO knowledge_facts(session_id,subject,predicate,object,source) VALUES(?,?,?,?,?)",
            (sid, subject, predicate, object, source),
        )
        return {"id": cur.lastrowid, "subject": subject, "predicate": predicate, "object": object, "source": source}


def list_facts(session_id: str, limit: int = 50) -> list[dict]:
    sid = _clean(session_id, "session_id")
    with _connect() as db:
        rows = db.execute(
            "SELECT id,subject,predicate,object,source,created_at FROM knowledge_facts WHERE session_id=? ORDER BY id DESC LIMIT ?",
            (sid, min(max(int(limit), 1), 100)),
        ).fetchall()
    return [{"id":r[0],"subject":r[1],"predicate":r[2],"object":r[3],"source":r[4],"created_at":r[5]} for r in rows]


def knowledge_context(session_id: str, limit: int = 30) -> str:
    facts = list_facts(session_id, limit)
    return "\\n".join(f"- {f['subject']} — {f['predicate']} — {f['object']} (source: {f['source']})" for f in reversed(facts))


def delete_fact(session_id: str, fact_id: int) -> bool:
    sid = _clean(session_id, "session_id")
    with _connect() as db:
        cur = db.execute("DELETE FROM knowledge_facts WHERE session_id=? AND id=?", (sid, fact_id))
        return cur.rowcount > 0


def clear_facts(session_id: str) -> int:
    sid = _clean(session_id, "session_id")
    with _connect() as db:
        cur = db.execute("DELETE FROM knowledge_facts WHERE session_id=?", (sid,))
        return cur.rowcount
