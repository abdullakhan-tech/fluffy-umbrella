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
    monkeypatch.setenv("OPENAI_API_KEY", "test-placeholder")
    monkeypatch.setattr(main, "_RATE_LIMIT", 1)
    first = client.post("/api/chat", json={"message": "first"})
    second = client.post("/api/chat", json={"message": "second"})
    assert first.status_code == 503  # key is not validated until after the limiter
    assert second.status_code == 429
    assert second.headers["retry-after"] == "60"
