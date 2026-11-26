"""Regression tests for bounded transient AI-provider retries."""

import asyncio

import httpx
import pytest

from app.config import settings
from app.models import ChatMessage, ChatRole
from app.services.ai_service import AIService


class FakeResponse:
    def __init__(self, status_code=200, headers=None, payload=None):
        self.status_code = status_code
        self.headers = headers or {}
        self.payload = payload or {}
        self.closed = False

    def json(self):
        return self.payload

    def raise_for_status(self):
        if self.status_code >= 400:
            request = httpx.Request("POST", "https://provider.invalid/test")
            response = httpx.Response(self.status_code, request=request)
            raise httpx.HTTPStatusError("synthetic provider error", request=request, response=response)

    async def aclose(self):
        self.closed = True


class FakeClient:
    def __init__(self, outcomes):
        self.outcomes = list(outcomes)
        self.calls = 0

    async def post(self, url, **kwargs):
        self.calls += 1
        outcome = self.outcomes.pop(0)
        if isinstance(outcome, BaseException):
            raise outcome
        return outcome


def make_service(client):
    service = object.__new__(AIService)
    service.client = client
    return service


def configure_retries(monkeypatch, attempts=2, base=0.25, maximum=2.0):
    monkeypatch.setattr(settings, "provider_max_attempts", attempts)
    monkeypatch.setattr(settings, "provider_retry_base_seconds", base)
    monkeypatch.setattr(settings, "provider_retry_max_seconds", maximum)
    delays = []

    async def fake_sleep(seconds):
        delays.append(seconds)

    monkeypatch.setattr(asyncio, "sleep", fake_sleep)
    return delays


def test_retries_transient_503_then_succeeds(monkeypatch):
    delays = configure_retries(monkeypatch)
    first = FakeResponse(503)
    client = FakeClient([first, FakeResponse(200, payload={"ok": True})])
    service = make_service(client)

    response = asyncio.run(service._post_with_retry("https://provider.invalid/chat", json={}))

    assert response.status_code == 200
    assert client.calls == 2
    assert first.closed is True
    assert delays == [0.25]


def test_does_not_retry_authentication_failure(monkeypatch):
    delays = configure_retries(monkeypatch)
    client = FakeClient([FakeResponse(401), FakeResponse(200)])
    service = make_service(client)

    response = asyncio.run(service._post_with_retry("https://provider.invalid/chat", json={}))

    assert response.status_code == 401
    assert client.calls == 1
    assert delays == []


def test_retries_transient_transport_error(monkeypatch):
    delays = configure_retries(monkeypatch)
    client = FakeClient(
        [httpx.ConnectError("synthetic network failure"), FakeResponse(200)]
    )
    service = make_service(client)

    response = asyncio.run(service._post_with_retry("https://provider.invalid/chat", json={}))

    assert response.status_code == 200
    assert client.calls == 2
    assert delays == [0.25]


def test_does_not_retry_read_timeout_after_request_may_have_been_sent(monkeypatch):
    delays = configure_retries(monkeypatch)
    client = FakeClient(
        [httpx.ReadTimeout("synthetic response timeout"), FakeResponse(200)]
    )
    service = make_service(client)

    with pytest.raises(httpx.ReadTimeout):
        asyncio.run(service._post_with_retry("https://provider.invalid/chat", json={}))

    assert client.calls == 1
    assert delays == []


def test_backoff_is_exponential_and_attempt_count_is_bounded(monkeypatch):
    delays = configure_retries(monkeypatch, attempts=3)
    first, second, last = FakeResponse(503), FakeResponse(503), FakeResponse(503)
    client = FakeClient([first, second, last])
    service = make_service(client)

    response = asyncio.run(service._post_with_retry("https://provider.invalid/chat", json={}))

    assert response is last
    assert client.calls == 3
    assert first.closed is True
    assert second.closed is True
    assert delays == [0.25, 0.5]


def test_retry_after_is_clamped_to_configured_maximum(monkeypatch):
    delays = configure_retries(monkeypatch, maximum=1.0)
    client = FakeClient(
        [FakeResponse(429, headers={"Retry-After": "99"}), FakeResponse(200)]
    )
    service = make_service(client)

    response = asyncio.run(service._post_with_retry("https://provider.invalid/chat", json={}))

    assert response.status_code == 200
    assert delays == [1.0]


@pytest.mark.parametrize(
    ("method_name", "payload", "expected_provider", "extra_kwargs"),
    [
        (
            "_call_openai",
            {"choices": [{"message": {"content": "openai reply"}}]},
            "openai",
            {"temperature": 0.2, "max_tokens": 32},
        ),
        (
            "_call_gemini",
            {"candidates": [{"content": {"parts": [{"text": "gemini reply"}]}}]},
            "gemini",
            {"temperature": 0.2},
        ),
        (
            "_call_ollama",
            {"message": {"content": "ollama reply"}},
            "ollama",
            {},
        ),
    ],
)
def test_all_provider_adapters_use_retry_transport(
    monkeypatch, method_name, payload, expected_provider, extra_kwargs
):
    monkeypatch.setattr(settings, "openai_api_key", "synthetic-openai-key")
    monkeypatch.setattr(settings, "gemini_api_key", "synthetic-gemini-key")
    service = make_service(FakeClient([]))
    requested_urls = []

    async def fake_post_with_retry(url, **kwargs):
        requested_urls.append(url)
        return FakeResponse(200, payload=payload)

    service._post_with_retry = fake_post_with_retry
    messages = [ChatMessage(role=ChatRole.USER, content="synthetic request")]
    method = getattr(service, method_name)

    response = asyncio.run(
        method(messages=messages, model="synthetic-model", **extra_kwargs)
    )

    assert response.provider_used == expected_provider
    assert response.reply.endswith("reply")
    assert len(requested_urls) == 1
