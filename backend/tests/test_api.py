import pytest
from fastapi.testclient import TestClient

from backend import auth, main, memory, knowledge

client = TestClient(main.app)
REGISTRATION_TOKEN = "test-registration-token-" + ("x" * 32)


@pytest.fixture(autouse=True)
def isolated_security_state(tmp_path, monkeypatch):
    monkeypatch.setattr(memory, "DB_PATH", tmp_path / "security-test.db")
    monkeypatch.setattr(knowledge, "DB_PATH", tmp_path / "security-test.db")
    monkeypatch.setenv("ZAYTHAR_REGISTRATION_TOKEN", REGISTRATION_TOKEN)
    monkeypatch.setattr(main, "_RATE_LIMIT", 20)
    monkeypatch.setattr(main, "_AUTH_RATE_LIMIT", 20)
    with main._rate_lock:
        main._requests_by_ip.clear()
    yield
    with main._rate_lock:
        main._requests_by_ip.clear()


@pytest.fixture
def register_user():
    def create(username):
        response = client.post("/api/auth/register", json={
            "username": username,
            "password": "correct-horse-battery-42",
            "registration_token": REGISTRATION_TOKEN,
        })
        assert response.status_code == 200, response.text
        return response.json()
    return create


@pytest.fixture
def auth_headers(register_user):
    account = register_user("test-operator")
    return {"Authorization": "Bearer " + account["token"]}


def test_health_is_public_and_does_not_expose_provider_secrets(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "synthetic-test-key")
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    response = client.get("/api/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"
    assert response.json()["providers"] == {"openai": True, "gemini": False}
    assert "synthetic-test-key" not in response.text
    assert response.headers["cache-control"] == "no-store"


def test_registration_requires_configured_invitation_token(monkeypatch):
    monkeypatch.delenv("ZAYTHAR_REGISTRATION_TOKEN", raising=False)
    response = client.post("/api/auth/register", json={
        "username": "new-user",
        "password": "correct-horse-battery-42",
        "registration_token": REGISTRATION_TOKEN,
    })
    assert response.status_code == 503


def test_registration_rejects_invalid_token():
    response = client.post("/api/auth/register", json={
        "username": "new-user",
        "password": "correct-horse-battery-42",
        "registration_token": "wrong-registration-token-" + ("y" * 32),
    })
    assert response.status_code == 403


def test_login_session_and_logout(register_user):
    account = register_user("login-user")
    login = client.post("/api/auth/login", json={
        "username": "LOGIN-USER",
        "password": "correct-horse-battery-42",
    })
    assert login.status_code == 200
    token = login.json()["token"]
    headers = {"Authorization": "Bearer " + token}
    assert client.get("/api/auth/me", headers=headers).json()["user"]["username"] == "login-user"
    assert client.post("/api/auth/logout", headers=headers).status_code == 200
    assert client.get("/api/auth/me", headers=headers).status_code == 401
    assert account["token"] != token


def test_login_uses_generic_failure_message():
    response = client.post("/api/auth/login", json={"username": "unknown-user", "password": "wrong-password"})
    assert response.status_code == 401
    assert response.json()["detail"] == "Invalid username or password."


def test_unauthenticated_requests_cannot_reach_personal_or_ai_apis():
    checks = [
        client.get("/api/memory"),
        client.post("/api/memory", json={"content": "private"}),
        client.get("/api/knowledge"),
        client.post("/api/knowledge", json={"subject": "A", "predicate": "B", "object": "C"}),
        client.post("/api/chat", json={"message": "hello"}),
        client.post("/api/agents/run", json={"task": "review", "agents": ["coding"]}),
    ]
    assert all(response.status_code == 401 for response in checks)


def test_memory_crud_is_bound_to_authenticated_user(auth_headers):
    created = client.post("/api/memory", headers=auth_headers, json={"content": "Synthetic preference"})
    assert created.status_code == 200
    memory_id = created.json()["id"]
    listed = client.get("/api/memory", headers=auth_headers)
    assert listed.status_code == 200
    assert listed.json()["memories"][0]["content"] == "Synthetic preference"
    updated = client.put(f"/api/memory/{memory_id}", headers=auth_headers, json={"content": "Updated synthetic preference"})
    assert updated.status_code == 200
    assert client.get("/api/memory", headers=auth_headers).json()["memories"][0]["content"] == "Updated synthetic preference"
    deleted = client.delete(f"/api/memory/{memory_id}", headers=auth_headers)
    assert deleted.status_code == 200
    assert client.get("/api/memory", headers=auth_headers).json()["memories"] == []
    client.post("/api/memory", headers=auth_headers, json={"content": "clear me"})
    assert client.delete("/api/memory", headers=auth_headers).json()["deleted_count"] == 1


def test_client_cannot_choose_memory_owner_or_session(auth_headers):
    response = client.post("/api/memory", headers=auth_headers, json={
        "content": "attempted owner override",
        "session_id": "another-user",
    })
    assert response.status_code == 422


def test_memory_and_knowledge_are_isolated_between_users(register_user):
    # A legacy browser session may have guessed a future username namespace.
    memory.save_memory("zaythar_user_1", "legacy session data must not become account data")
    knowledge.add_fact("zaythar_user_1", "Legacy", "must not leak", "into account")
    alice = register_user("alice-user")
    bob = register_user("bob-user")
    alice_headers = {"Authorization": "Bearer " + alice["token"]}
    bob_headers = {"Authorization": "Bearer " + bob["token"]}

    saved = client.post("/api/memory", headers=alice_headers, json={"content": "Alice private note"})
    assert saved.status_code == 200
    memory_id = saved.json()["id"]
    assert client.get("/api/memory", headers=bob_headers).json()["memories"] == []
    assert client.delete(f"/api/memory/{memory_id}", headers=bob_headers).status_code == 404
    assert client.put(f"/api/memory/{memory_id}", headers=bob_headers, json={"content": "hijack"}).status_code == 404
    assert client.get("/api/memory", headers=alice_headers).json()["memories"][0]["content"] == "Alice private note"

    fact = client.post("/api/knowledge", headers=alice_headers, json={
        "subject": "Alice", "predicate": "owns", "object": "private fact"
    })
    assert fact.status_code == 200
    fact_id = fact.json()["id"]
    assert client.get("/api/knowledge", headers=bob_headers).json()["facts"] == []
    assert client.delete(f"/api/knowledge/{fact_id}", headers=bob_headers).status_code == 404
    assert client.get("/api/knowledge", headers=alice_headers).json()["facts"][0]["object"] == "private fact"


def test_knowledge_api_crud(auth_headers):
    response = client.post("/api/knowledge", headers=auth_headers, json={
        "subject": "Zaythar", "predicate": "uses", "object": "private graph"
    })
    assert response.status_code == 200
    fact_id = response.json()["id"]
    listed = client.get("/api/knowledge", headers=auth_headers)
    assert listed.status_code == 200
    assert listed.json()["facts"][0]["object"] == "private graph"
    deleted = client.delete(f"/api/knowledge/{fact_id}", headers=auth_headers)
    assert deleted.status_code == 200
    assert client.get("/api/knowledge", headers=auth_headers).json()["facts"] == []


def test_chat_requires_auth_and_returns_sanitized_provider_error(monkeypatch, auth_headers):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    response = client.post("/api/chat", headers=auth_headers, json={"message": "Salam"})
    assert response.status_code == 503
    assert "provider" in response.json()["detail"].lower()
    assert "OPENAI_API_KEY" not in response.json()["detail"]


def test_chat_rejects_empty_message_with_authenticated_user(auth_headers):
    response = client.post("/api/chat", headers=auth_headers, json={"message": ""})
    assert response.status_code == 422


def test_chat_rate_limit_returns_retry_after(monkeypatch, auth_headers):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    monkeypatch.setattr(main, "_RATE_LIMIT", 1)
    first = client.post("/api/chat", headers=auth_headers, json={"message": "first"})
    second = client.post("/api/chat", headers=auth_headers, json={"message": "second"})
    assert first.status_code == 503
    assert second.status_code == 429
    assert second.headers["retry-after"] == "60"


def test_multi_agent_endpoint_uses_server_derived_user_scope(monkeypatch, auth_headers):
    captured = {}

    def fake_run(task, agents, provider, language, user_id):
        captured["user_id"] = user_id
        return {"reply": "Coordinated output", "agents": [], "completed_agents": 1,
                "total_agents": 1, "provider": "openai", "model": "test"}

    monkeypatch.setattr(main, "run_multi_agent", fake_run)
    response = client.post("/api/agents/run", headers=auth_headers, json={
        "task": "Review this function", "agents": ["coding"],
    })
    assert response.status_code == 200
    assert type(captured["user_id"]) is int and captured["user_id"] > 0


def test_multi_agent_rejects_more_than_four_specialists(auth_headers):
    response = client.post("/api/agents/run", headers=auth_headers, json={
        "task": "Do a task",
        "agents": ["research", "coding", "analysis", "writing", "creative"],
    })
    assert response.status_code == 422


def test_multi_agent_rejects_unknown_specialist(auth_headers):
    response = client.post("/api/agents/run", headers=auth_headers, json={
        "task": "Do a task", "agents": ["__import__"],
    })
    assert response.status_code == 422


def test_remote_clients_do_not_get_a_loopback_or_shared_token_bypass(monkeypatch):
    monkeypatch.setenv("ZAYTHAR_REGISTRATION_TOKEN", REGISTRATION_TOKEN)
    remote_client = TestClient(main.app, client=("203.0.113.10", 12345))
    assert remote_client.get("/api/memory").status_code == 401
    assert remote_client.get("/api/health").status_code == 200
