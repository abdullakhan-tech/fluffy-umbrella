"""Password authentication and opaque, revocable bearer sessions.

Session tokens are returned once and only their SHA-256 digests are stored.
User-owned data uses a server-derived owner namespace; client session IDs are
never accepted as an authorization boundary.
"""
import hashlib
import hmac
import os
import re
import secrets
import sqlite3
import time
from contextlib import contextmanager

from backend import memory

PBKDF2_ITERATIONS = 310_000
USERNAME_RE = re.compile(r"^[a-zA-Z0-9_.-]{3,32}$")
_DUMMY_SALT = b"zaythar-auth-dummy-salt"
_DUMMY_HASH = hashlib.pbkdf2_hmac("sha256", b"not-a-real-password", _DUMMY_SALT, PBKDF2_ITERATIONS)


class RegistrationDisabled(Exception):
    pass


class InvalidRegistrationToken(Exception):
    pass


class UsernameUnavailable(Exception):
    pass


def _connect():
    memory.DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(memory.DB_PATH, timeout=10)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    try:
        conn.execute(
            """CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_salt TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )"""
        )
        conn.execute(
            """CREATE TABLE IF NOT EXISTS auth_sessions (
                token_hash TEXT PRIMARY KEY,
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                expires_at INTEGER NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )"""
        )
        conn.execute("CREATE INDEX IF NOT EXISTS idx_auth_sessions_expiry ON auth_sessions(expires_at)")
        yield conn
        conn.commit()
    finally:
        conn.close()


def _validate_credentials(username: str, password: str) -> tuple[str, str]:
    if not isinstance(username, str):
        raise ValueError("Username must be text")
    normalized = username.strip().lower()
    if not USERNAME_RE.fullmatch(normalized):
        raise ValueError("Username must be 3–32 characters using letters, numbers, dot, underscore or hyphen")
    if not isinstance(password, str) or not 12 <= len(password) <= 128:
        raise ValueError("Password must contain 12–128 characters")
    return normalized, password


def _password_digest(password: str, salt: bytes) -> bytes:
    return hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, PBKDF2_ITERATIONS, dklen=32)


def register_user(username: str, password: str, registration_token: str) -> dict:
    expected = os.getenv("ZAYTHAR_REGISTRATION_TOKEN", "").strip()
    if len(expected) < 32:
        raise RegistrationDisabled("Account registration is disabled until a strong ZAYTHAR_REGISTRATION_TOKEN is configured.")
    if not isinstance(registration_token, str) or not hmac.compare_digest(registration_token, expected):
        raise InvalidRegistrationToken()

    username, password = _validate_credentials(username, password)
    salt = secrets.token_bytes(16)
    password_hash = _password_digest(password, salt).hex()
    try:
        with _connect() as db:
            cursor = db.execute(
                "INSERT INTO users(username,password_salt,password_hash) VALUES(?,?,?)",
                (username, salt.hex(), password_hash),
            )
            return {"id": cursor.lastrowid, "username": username}
    except sqlite3.IntegrityError as exc:
        raise UsernameUnavailable() from exc


def verify_password(username: str, password: str) -> dict | None:
    if not isinstance(username, str) or not isinstance(password, str) or len(password) > 128:
        return None
    normalized = username.strip().lower()
    if not USERNAME_RE.fullmatch(normalized):
        _password_digest(password[:128], _DUMMY_SALT)
        return None
    with _connect() as db:
        row = db.execute(
            "SELECT id,username,password_salt,password_hash FROM users WHERE username=?",
            (normalized,),
        ).fetchone()
    if row is None:
        _password_digest(password, _DUMMY_SALT)
        return None
    try:
        salt = bytes.fromhex(row["password_salt"])
        expected = bytes.fromhex(row["password_hash"])
    except (ValueError, TypeError):
        return None
    actual = _password_digest(password, salt)
    if not hmac.compare_digest(actual, expected):
        return None
    return {"id": row["id"], "username": row["username"]}


def issue_session(user_id: int) -> dict:
    token = secrets.token_urlsafe(32)
    token_hash = hashlib.sha256(token.encode("utf-8")).hexdigest()
    now = int(time.time())
    try:
        ttl = int(os.getenv("ZAYTHAR_SESSION_TTL_SECONDS", "43200"))
    except ValueError:
        ttl = 43200
    ttl = min(max(ttl, 300), 2_592_000)
    expires_at = now + ttl
    with _connect() as db:
        db.execute("DELETE FROM auth_sessions WHERE expires_at <= ?", (now,))
        db.execute(
            "INSERT INTO auth_sessions(token_hash,user_id,expires_at) VALUES(?,?,?)",
            (token_hash, user_id, expires_at),
        )
    return {"token": token, "expires_at": expires_at}


def user_for_token(token: str) -> dict | None:
    if not isinstance(token, str) or not 32 <= len(token) <= 256:
        return None
    token_hash = hashlib.sha256(token.encode("utf-8")).hexdigest()
    now = int(time.time())
    with _connect() as db:
        row = db.execute(
            """SELECT users.id, users.username
               FROM auth_sessions
               JOIN users ON users.id = auth_sessions.user_id
               WHERE auth_sessions.token_hash=? AND auth_sessions.expires_at>?""",
            (token_hash, now),
        ).fetchone()
        if row is None:
            db.execute("DELETE FROM auth_sessions WHERE token_hash=? OR expires_at<=?", (token_hash, now))
            return None
    return {"id": row["id"], "username": row["username"]}


def revoke_session(token: str) -> bool:
    if not isinstance(token, str) or not token:
        return False
    token_hash = hashlib.sha256(token.encode("utf-8")).hexdigest()
    with _connect() as db:
        cursor = db.execute("DELETE FROM auth_sessions WHERE token_hash=?", (token_hash,))
        return cursor.rowcount > 0
