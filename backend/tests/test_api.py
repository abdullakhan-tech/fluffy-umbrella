"""Test suite for Zaythar AI Companion Backend API."""

from fastapi.testclient import TestClient
import pytest

from app.main import app


@pytest.fixture
def client():
    with TestClient(app) as test_client:
        yield test_client


def test_root_endpoint(client):
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert data["app"] == "Zaythar AI Companion Backend"
    assert data["status"] == "online"


def test_health_endpoint(client):
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "version" in data


def test_status_endpoint(client):
    response = client.get("/api/v1/status")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"


def test_chat_completion_local_fallback(client):
    payload = {
        "messages": [
            {"role": "user", "content": "Hello Zaythar AI!"}
        ],
        "provider": "auto"
    }
    response = client.post("/api/v1/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "reply" in data
    assert data["status"] == "success"
    assert data["provider_used"] == "local_engine"
    assert "Zaythar Local Engine" in data["reply"]


def test_chat_completion_validation_error(client):
    # Empty messages list should trigger 422 Unprocessable Entity
    payload = {"messages": []}
    response = client.post("/api/v1/chat", json=payload)
    assert response.status_code == 422


def test_chat_stream_endpoint(client):
    payload = {
        "messages": [
            {"role": "user", "content": "Testing streaming response"}
        ],
        "provider": "auto"
    }
    response = client.post("/api/v1/chat/stream", json=payload)
    assert response.status_code == 200
    assert "text/event-stream" in response.headers.get("content-type", "")
    content = response.text
    assert "data:" in content
    assert '"done": true' in content


def test_session_lifecycle(client):
    session_id = "test-session-123"

    # 1. Send first message in session
    payload = {"message": "My name is Abdullah."}
    res1 = client.post(f"/api/v1/sessions/{session_id}/chat", json=payload)
    assert res1.status_code == 200
    assert res1.json()["session_id"] == session_id

    # 2. Check history
    res2 = client.get(f"/api/v1/sessions/{session_id}")
    assert res2.status_code == 200
    history = res2.json()
    assert len(history) >= 2  # user message and assistant reply

    # 3. Check sessions list
    res3 = client.get("/api/v1/sessions")
    assert res3.status_code == 200
    sessions = res3.json()
    assert any(s["session_id"] == session_id for s in sessions)

    # 4. Delete session
    res4 = client.delete(f"/api/v1/sessions/{session_id}")
    assert res4.status_code == 200
    assert res4.json()["deleted"] is True


def test_agent_task_math_calculator(client):
    payload = {
        "task": "calculate 25 * 4 + 50",
        "tool_hint": "calculator"
    }
    response = client.post("/api/v1/agent/task", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "completed"
    assert data["tool_used"] == "calculator"
    assert data["tool_result"]["success"] is True
    assert data["tool_result"]["result"] == 150


def test_agent_task_code_inspection(client):
    payload = {
        "task": "def add(a, b):\n    return a + b\n\nclass MathService:\n    pass",
        "tool_hint": "code_inspect"
    }
    response = client.post("/api/v1/agent/task", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "completed"
    assert data["tool_used"] == "code_inspector"
    assert data["tool_result"]["syntax_valid"] is True
    assert "add" in data["tool_result"]["functions"]
    assert "MathService" in data["tool_result"]["classes"]


def test_agent_task_summarization(client):
    long_text = (
        "Artificial Intelligence companion systems require robust security boundaries. "
        "Server-side validation ensures that keys remain protected. "
        "Deterministic local engines ensure zero downtime even without external networks. "
        "Comprehensive testing guarantees system reliability across Android and web clients."
    )
    payload = {
        "task": "Summarize key architecture tenets",
        "context": long_text,
        "tool_hint": "summarize"
    }
    response = client.post("/api/v1/agent/task", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "completed"
    assert data["tool_used"] == "text_summarizer"
    assert data["tool_result"]["metrics"]["word_count"] > 10


def test_orb_animation_state(client):
    response = client.get("/api/v1/orb/animation-state")
    assert response.status_code == 200
    data = response.json()
    assert "state" in data
    assert "pulse_rate_hz" in data
    assert "glow_color_hex" in data
    assert data["glow_color_hex"].startswith("#")
