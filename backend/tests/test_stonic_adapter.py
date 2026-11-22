"""Contract tests for the optional Stonic-compatible provider bridge."""
import httpx
import pytest

from app.models import ChatMessage, ChatRole
from app.services.stonic_adapter import StonicAdapter


@pytest.mark.asyncio
async def test_stonic_adapter_maps_chat_completion_contract():
    seen = {}

    def handler(request: httpx.Request) -> httpx.Response:
        seen["url"] = str(request.url)
        seen["authorization"] = request.headers.get("Authorization")
        seen["payload"] = request.read().decode()
        return httpx.Response(
            200,
            json={"choices": [{"message": {"content": "Stonic bridge online"}}]},
        )

    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        adapter = StonicAdapter(client, "https://stonic.example", "secret", "stonic-test")
        result = await adapter.generate(
            [ChatMessage(role=ChatRole.USER, content="Hello")],
            temperature=0.2,
            max_tokens=128,
        )

    assert result.reply == "Stonic bridge online"
    assert result.provider_used == "stonic"
    assert result.model_used == "stonic-test"
    assert seen["url"] == "https://stonic.example/v1/chat/completions"
    assert seen["authorization"] == "Bearer secret"
    assert '"content":"Hello"' in seen["payload"]


@pytest.mark.asyncio
async def test_stonic_adapter_rejects_bad_gateway_response():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"choices": []})

    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        adapter = StonicAdapter(client, "https://stonic.example", "secret", "stonic-test")
        with pytest.raises((KeyError, IndexError)):
            await adapter.generate([ChatMessage(role=ChatRole.USER, content="Hello")])
