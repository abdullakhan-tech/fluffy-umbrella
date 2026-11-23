from fastapi.testclient import TestClient

from app.main import app

def test_codebase_analyzer_agent_endpoint_reports_python_dependencies():
    with TestClient(app) as client:
        response = client.post("/api/v1/agent/task", json={
            "task": "Analyze these Python source dependencies",
            "context": "import os\nimport httpx\nfrom app.core import config",
            "tool_hint": "codebase_analyze",
        })
    assert response.status_code == 200
    payload = response.json()
    assert payload["status"] == "completed"
    assert payload["tool_used"] == "codebase_analyzer"
    assert payload["tool_result"]["syntax_valid"] is True
    assert "httpx" in payload["tool_result"]["external_or_project_dependencies"]

def test_codebase_analyzer_rejects_oversized_source_context():
    with TestClient(app) as client:
        response = client.post("/api/v1/agent/task", json={
            "task": "Analyze source",
            "context": "x" * 20_001,
            "tool_hint": "codebase_analyze",
        })
    assert response.status_code == 422
