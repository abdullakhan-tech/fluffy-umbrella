import pytest
from fastapi.testclient import TestClient

from backend import main

client = TestClient(main.app)

@pytest.fixture(autouse=True)
def reset_rate_limiter(monkeypatch):
    with main._rate_lock:
        main._requests_by_ip.clear()
    monkeypatch.setattr(main, "_RATE_LIMIT", 20)
    yield
    with main._rate_lock:
        main._requests_by_ip.clear()

def test_health_reports_missing_provider_key(monkeypatch):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    response = client.get("/api/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"
    assert response.json()["ai_configured"] is False

def test_chat_returns_clear_error_without_provider_key(monkeypatch):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    response = client.post("/api/chat", json={"message": "Salam", "language": "ur-PK"})
    assert response.status_code == 503
    assert "OPENAI_API_KEY" in response.json()["detail"]

def test_chat_rejects_empty_message():
    response = client.post("/api/chat", json={"message": ""})
    assert response.status_code == 422

def test_chat_rate_limit_returns_retry_after(monkeypatch):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    monkeypatch.setattr(main, "_RATE_LIMIT", 1)
    first = client.post("/api/chat", json={"message": "first"})
    second = client.post("/api/chat", json={"message": "second"})
    assert first.status_code == 503  # request reaches the endpoint; no key is configured
    assert second.status_code == 429
    assert second.headers["retry-after"] == "60"


def test_health_includes_real_process_uptime_and_provider_flags(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "test-key")
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    response = client.get("/api/health")
    payload = response.json()
    assert response.status_code == 200
    assert payload["providers"] == {"openai": True, "gemini": False}
    assert isinstance(payload["uptime_seconds"], int)
    assert payload["uptime_seconds"] >= 0


def test_multi_agent_endpoint_delegates_to_orchestrator(monkeypatch):
    monkeypatch.setattr(main, "run_multi_agent", lambda task, agents, provider, language: {
        "reply": "Coordinated output", "agents": [{"agent": "coding", "status": "completed"}],
        "completed_agents": 1, "total_agents": 1, "provider": "openai", "model": "test"
    })
    response = client.post("/api/agents/run", json={
        "task": "Review this function", "agents": ["coding"],
        "provider": "hybrid", "language": "English"
    })
    assert response.status_code == 200
    assert response.json()["reply"] == "Coordinated output"


def test_multi_agent_endpoint_rejects_more_than_four_specialists():
    response = client.post("/api/agents/run", json={
        "task": "Do a task",
        "agents": ["research", "coding", "analysis", "writing", "creative"]
    })
    assert response.status_code == 422
