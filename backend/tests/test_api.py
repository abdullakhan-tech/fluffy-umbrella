"""Automated tests for Zaythar AI Python companion backend."""

from fastapi.testclient import TestClient

from app.main import app
from app.config import settings

client = TestClient(app)


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["app"] == settings.app_name
    assert data["version"] == settings.version


def test_root_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "online"
    assert "docs_url" in data


def test_api_status_endpoint():
    response = client.get("/api/v1/status")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"


def test_chat_endpoint_fallback():
    payload = {
        "messages": [
            {"role": "user", "content": "Hello Zaythar companion"}
        ],
        "provider": "auto",
    }
    response = client.post("/api/v1/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "Hello Zaythar companion" in data["reply"]
    assert data["provider_used"] in ("local_engine", "openai", "gemini", "ollama")


def test_chat_input_validation_empty_messages():
    payload = {"messages": []}
    response = client.post("/api/v1/chat", json=payload)
    assert response.status_code == 422  # Validation error (min_length=1)


def test_chat_input_validation_empty_content():
    payload = {
        "messages": [{"role": "user", "content": ""}]
    }
    response = client.post("/api/v1/chat", json=payload)
    assert response.status_code == 422


def test_agent_task_endpoint():
    payload = {
        "task": "Summarize user notes for morning briefing",
        "context": "User has 3 meetings scheduled",
    }
    response = client.post("/api/v1/agent/task", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "completed"
    assert "Summarize user notes" in data["task"]
    assert len(data["actions_taken"]) >= 1
