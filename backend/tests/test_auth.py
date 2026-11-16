import sqlite3

import pytest

from backend import auth, memory


@pytest.fixture
def isolated_auth_db(tmp_path, monkeypatch):
    monkeypatch.setattr(memory, "DB_PATH", tmp_path / "auth-test.db")
    monkeypatch.setenv("ZAYTHAR_REGISTRATION_TOKEN", "invitation-secret-" + ("q" * 32))


def test_passwords_are_salted_and_never_stored_in_plaintext(isolated_auth_db):
    user = auth.register_user("secure-user", "correct-horse-battery-42", "invitation-secret-" + ("q" * 32))
    with sqlite3.connect(memory.DB_PATH) as db:
        row = db.execute("SELECT password_salt,password_hash FROM users WHERE id=?", (user["id"],)).fetchone()
    assert row is not None
    assert row[0] != row[1]
    assert "correct-horse-battery-42" not in row[1]
    assert auth.verify_password("secure-user", "correct-horse-battery-42")["id"] == user["id"]
    assert auth.verify_password("secure-user", "incorrect-password") is None


def test_session_token_is_hashed_and_revocable(isolated_auth_db):
    user = auth.register_user("session-user", "correct-horse-battery-42", "invitation-secret-" + ("q" * 32))
    session = auth.issue_session(user["id"])
    assert auth.user_for_token(session["token"])["id"] == user["id"]
    with sqlite3.connect(memory.DB_PATH) as db:
        stored = db.execute("SELECT token_hash FROM auth_sessions").fetchone()[0]
    assert stored != session["token"]
    assert auth.revoke_session(session["token"]) is True
    assert auth.user_for_token(session["token"]) is None


def test_expired_session_is_rejected(isolated_auth_db, monkeypatch):
    user = auth.register_user("expired-user", "correct-horse-battery-42", "invitation-secret-" + ("q" * 32))
    clock = {"now": 1_800_000_000}
    monkeypatch.setattr(auth.time, "time", lambda: clock["now"])
    session = auth.issue_session(user["id"])
    clock["now"] = session["expires_at"] + 1
    assert auth.user_for_token(session["token"]) is None


@pytest.mark.parametrize("username,password", [
    ("ab", "correct-horse-battery-42"),
    ("valid-user", "short"),
    ("bad username", "correct-horse-battery-42"),
])
def test_registration_rejects_weak_or_invalid_credentials(isolated_auth_db, username, password):
    with pytest.raises(ValueError):
        auth.register_user(username, password, "invitation-secret-" + ("q" * 32))
