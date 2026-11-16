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
    monkeypatch.setattr(main, "run_multi_agent", lambda task, agents, provider, language, session_id="default": {
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


def test_knowledge_api_crud(monkeypatch):
    response = client.post("/api/knowledge", json={
        "session_id": "operator_test", "subject": "Zaythar",
        "predicate": "uses", "object": "shared graph"
    })
    assert response.status_code == 200
    fact_id = response.json()["id"]
    listed = client.get("/api/knowledge?session_id=operator_test")
    assert listed.status_code == 200
    assert listed.json()["facts"][0]["object"] == "shared graph"
    deleted = client.delete(f"/api/knowledge/{fact_id}?session_id=operator_test")
    assert deleted.status_code == 200
    assert client.get("/api/knowledge?session_id=operator_test").json()["facts"] == []


def test_knowledge_api_rejects_invalid_session():
    response = client.post("/api/knowledge", json={
        "session_id": "../unsafe", "subject": "A", "predicate": "B", "object": "C"
    })
    assert response.status_code == 422


def test_agent_request_accepts_session_context(monkeypatch):
    monkeypatch.setattr(main, "run_multi_agent", lambda task, agents, provider, language, session_id: {
        "reply": "Context-aware result", "agents": [], "completed_agents": 1,
        "total_agents": 1, "provider": "openai", "model": "test", "session_id": session_id
    })
    response = client.post("/api/agents/run", json={
        "task": "Continue project", "agents": ["analysis"], "session_id": "operator_1"
    })
    assert response.status_code == 200
    assert response.json()["session_id"] == "operator_1"


def test_chat_uses_adaptive_jarvis_style_protocol(monkeypatch):
    captured = {}

    def fake_brain(provider, system_prompt, history, message):
        captured["prompt"] = system_prompt
        captured["history"] = history
        captured["message"] = message
        return {"reply": "Assalam-o-alaikum. Main madad ke liye tayyar hoon.", "provider": "openai", "model": "test"}

    monkeypatch.setattr(main, "run_brain", fake_brain)
    response = client.post("/api/chat", json={
        "message": "Continue our previous plan",
        "language": "ur-PK",
        "persona": {"style": "Calm & concise", "empathy": 80, "cognitive_capacity": 90,
                    "adaptability": 85, "creativity": 60},
        "history": [{"role": "user", "content": "We are building Zaythar AI"}]
    })

    assert response.status_code == 200
    assert response.json()["reply"].startswith("Assalam")
    assert "high-end cinematic voice assistant" in captured["prompt"]
    assert "Reply in Urdu" in captured["prompt"]
    assert "Calm & concise" in captured["prompt"]
    assert "Never invent citations, test results" in captured["prompt"]
    assert captured["history"][0]["content"] == "We are building Zaythar AI"
    assert captured["message"] == "Continue our previous plan"
