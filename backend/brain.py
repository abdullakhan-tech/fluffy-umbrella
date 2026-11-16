"""Zaythar Neural Core: provider routing and bounded conversational context."""
import os
from google import genai
from openai import OpenAI


def _openai(system_prompt, history, message):
    key = os.getenv("OPENAI_API_KEY")
    if not key:
        raise RuntimeError("OPENAI_API_KEY is not configured")
    messages = [{"role": "system", "content": system_prompt}]
    for item in history[-16:]:
        if item.get("role") in {"user", "assistant"} and isinstance(item.get("content"), str):
            messages.append({"role": item["role"], "content": item["content"][:6000]})
    messages.append({"role": "user", "content": message})
    response = OpenAI(api_key=key, timeout=45.0, max_retries=1).chat.completions.create(
        model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"), messages=messages, temperature=0.7
    )
    return {"reply": response.choices[0].message.content or "", "model": response.model, "provider": "openai"}


def _gemini(system_prompt, history, message):
    key = os.getenv("GEMINI_API_KEY")
    if not key:
        raise RuntimeError("GEMINI_API_KEY is not configured")
    transcript = []
    for item in history[-16:]:
        if item.get("role") in {"user", "assistant"} and isinstance(item.get("content"), str):
            label = "User" if item["role"] == "user" else "Zaythar"
            transcript.append(label + ": " + item["content"][:6000])
    transcript.append("User: " + message)
    model = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
    response = genai.Client(api_key=key).models.generate_content(
        model=model, contents="\n\n".join(transcript),
        config={"system_instruction": system_prompt, "temperature": 0.7}
    )
    return {"reply": response.text or "", "model": model, "provider": "gemini"}


def run_brain(provider, system_prompt, history, message):
    """Select a provider, or try OpenAI then Gemini in hybrid mode."""
    selected = (provider or os.getenv("ZAYTHAR_AI_PROVIDER", "hybrid")).lower()
    if selected == "openai":
        return _openai(system_prompt, history, message)
    if selected == "gemini":
        return _gemini(system_prompt, history, message)
    if selected != "hybrid":
        raise ValueError("provider must be openai, gemini, or hybrid")
    errors = []
    for adapter in (_openai, _gemini):
        try:
            return adapter(system_prompt, history, message)
        except Exception as exc:
            errors.append(type(exc).__name__)
    raise RuntimeError("Both AI providers are unavailable or not configured.")
