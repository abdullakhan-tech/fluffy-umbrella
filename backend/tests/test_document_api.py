"""API tests for document extraction."""

from fastapi.testclient import TestClient

from app.main import app


def test_document_extract_endpoint_returns_text_and_in_memory_storage():
    with TestClient(app) as client:
        response = client.post(
            "/api/v1/files/extract",
            files={"file": ("brief.txt", b"Zaythar document test", "text/plain")},
        )
    assert response.status_code == 200
    payload = response.json()
    assert payload["filename"] == "brief.txt"
    assert payload["text"] == "Zaythar document test"
    assert payload["storage"] == "in_memory_only"
    assert payload["security_findings"] == []


def test_document_extract_endpoint_rejects_unsupported_files():
    with TestClient(app) as client:
        response = client.post(
            "/api/v1/files/extract",
            files={"file": ("script.py", b"print('not supported')", "text/x-python")},
        )
    assert response.status_code == 400
    assert "Unsupported file type" in response.json()["detail"]
