"""SQLite knowledge graph with separate legacy-session and account ownership."""
import sqlite3
from contextlib import contextmanager
from backend.memory import DB_PATH

MAX_FIELD = 300
MAX_FACTS = 200
_ACCOUNT_SENTINEL = "__account_owned__"


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
        columns = {row[1] for row in conn.execute("PRAGMA table_info(knowledge_facts)").fetchall()}
        if "user_id" not in columns:
            conn.execute("ALTER TABLE knowledge_facts ADD COLUMN user_id INTEGER")
        conn.execute("CREATE INDEX IF NOT EXISTS idx_knowledge_session ON knowledge_facts(session_id, id DESC)")
        conn.execute("CREATE INDEX IF NOT EXISTS idx_knowledge_user ON knowledge_facts(user_id, id DESC)")
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


def _valid_user_id(user_id: int) -> int:
    if type(user_id) is not int or user_id < 1:
        raise ValueError("Invalid account owner")
    return user_id


def _fact_dict(row) -> dict:
    return {"id": row[0], "subject": row[1], "predicate": row[2], "object": row[3], "source": row[4], "created_at": row[5]}


# Legacy session functions are retained for local/unit compatibility and can
# touch only records without an authenticated user_id.
def add_fact(session_id: str, subject: str, predicate: str, object: str, source: str = "user") -> dict:
    sid = _clean(session_id, "session_id")
    subject = _clean(subject, "subject")
    predicate = _clean(predicate, "predicate")
    object = _clean(object, "object")
    source = _clean(source, "source")
    with _connect() as db:
        count = db.execute(
            "SELECT COUNT(*) FROM knowledge_facts WHERE session_id=? AND user_id IS NULL", (sid,)
        ).fetchone()[0]
        if count >= MAX_FACTS:
            raise ValueError(f"Knowledge graph limit reached ({MAX_FACTS} facts per session)")
        cur = db.execute(
            "INSERT INTO knowledge_facts(session_id,subject,predicate,object,source,user_id) VALUES(?,?,?,?,?,NULL)",
            (sid, subject, predicate, object, source),
        )
        return {"id": cur.lastrowid, "subject": subject, "predicate": predicate, "object": object, "source": source}


def list_facts(session_id: str, limit: int = 50) -> list[dict]:
    sid = _clean(session_id, "session_id")
    with _connect() as db:
        rows = db.execute(
            "SELECT id,subject,predicate,object,source,created_at FROM knowledge_facts WHERE session_id=? AND user_id IS NULL ORDER BY id DESC LIMIT ?",
            (sid, min(max(int(limit), 1), 100)),
        ).fetchall()
    return [_fact_dict(row) for row in rows]


def knowledge_context(session_id: str, limit: int = 30) -> str:
    facts = list_facts(session_id, limit)
    return "\n".join(
        f"- {fact['subject']} — {fact['predicate']} — {fact['object']} (source: {fact['source']})"
        for fact in reversed(facts)
    )


def delete_fact(session_id: str, fact_id: int) -> bool:
    sid = _clean(session_id, "session_id")
    with _connect() as db:
        cur = db.execute(
            "DELETE FROM knowledge_facts WHERE session_id=? AND id=? AND user_id IS NULL",
            (sid, fact_id),
        )
        return cur.rowcount > 0


def clear_facts(session_id: str) -> int:
    sid = _clean(session_id, "session_id")
    with _connect() as db:
        cur = db.execute(
            "DELETE FROM knowledge_facts WHERE session_id=? AND user_id IS NULL", (sid,)
        )
        return cur.rowcount


# Authenticated account functions are keyed by the server-side user primary key.
def add_user_fact(user_id: int, subject: str, predicate: str, object: str, source: str = "user") -> dict:
    owner = _valid_user_id(user_id)
    subject = _clean(subject, "subject")
    predicate = _clean(predicate, "predicate")
    object = _clean(object, "object")
    source = _clean(source, "source")
    with _connect() as db:
        count = db.execute("SELECT COUNT(*) FROM knowledge_facts WHERE user_id=?", (owner,)).fetchone()[0]
        if count >= MAX_FACTS:
            raise ValueError(f"Knowledge graph limit reached ({MAX_FACTS} facts per account)")
        cur = db.execute(
            "INSERT INTO knowledge_facts(session_id,subject,predicate,object,source,user_id) VALUES(?,?,?,?,?,?)",
            (_ACCOUNT_SENTINEL, subject, predicate, object, source, owner),
        )
        return {"id": cur.lastrowid, "subject": subject, "predicate": predicate, "object": object, "source": source}


def list_user_facts(user_id: int, limit: int = 50) -> list[dict]:
    owner = _valid_user_id(user_id)
    with _connect() as db:
        rows = db.execute(
            "SELECT id,subject,predicate,object,source,created_at FROM knowledge_facts WHERE user_id=? ORDER BY id DESC LIMIT ?",
            (owner, min(max(int(limit), 1), 100)),
        ).fetchall()
    return [_fact_dict(row) for row in rows]


def user_knowledge_context(user_id: int, limit: int = 30) -> str:
    facts = list_user_facts(user_id, limit)
    return "\n".join(
        f"- {fact['subject']} — {fact['predicate']} — {fact['object']} (source: {fact['source']})"
        for fact in reversed(facts)
    )


def delete_user_fact(user_id: int, fact_id: int) -> bool:
    owner = _valid_user_id(user_id)
    with _connect() as db:
        cur = db.execute("DELETE FROM knowledge_facts WHERE user_id=? AND id=?", (owner, fact_id))
        return cur.rowcount > 0


def clear_user_facts(user_id: int) -> int:
    owner = _valid_user_id(user_id)
    with _connect() as db:
        cur = db.execute("DELETE FROM knowledge_facts WHERE user_id=?", (owner,))
        return cur.rowcount
