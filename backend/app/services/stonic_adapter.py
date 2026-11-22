"""Optional Stonic bridge for servers exposing the OpenAI Chat Completions contract.

This is an interoperability adapter, not an extraction of the Stonic desktop
installer. It is disabled unless STONIC_BASE_URL and STONIC_API_KEY are set.
"""
from typing import List

import httpx

from app.models import ChatMessage, ChatResponse, ChatRole


class StonicAdapter:
    """Calls a configured OpenAI-compatible Stonic gateway."""

    def __init__(self, client: httpx.AsyncClient, base_url: str, api_key: str, model: str):
        self.client = client
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key
        self.model = model

    async def generate(
        self,
        messages: List[ChatMessage],
        temperature: float = 0.7,
        max_tokens: int = 1024,
        model_override: str | None = None,
    ) -> ChatResponse:
        payload = {
            "model": model_override or self.model,
            "messages": [
                {"role": message.role.value, "content": message.content}
                for message in messages
                if message.role != ChatRole.SYSTEM or message.content.strip()
            ],
            "temperature": temperature,
            "max_tokens": max_tokens,
        }
        response = await self.client.post(
            f"{self.base_url}/v1/chat/completions",
            json=payload,
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            },
        )
        response.raise_for_status()
        data = response.json()
        reply = data["choices"][0]["message"]["content"]
        if not isinstance(reply, str) or not reply.strip():
            raise ValueError("Stonic gateway returned an empty or invalid assistant message")
        return ChatResponse(
            reply=reply,
            provider_used="stonic",
            model_used=model_override or self.model,
            status="success",
        )
