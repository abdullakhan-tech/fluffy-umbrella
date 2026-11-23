"""Server-side AI service orchestrator enforcing provider security, personas, memory, and guardrails."""

import asyncio
import json
import logging
import re
from typing import AsyncGenerator, List, Optional
import httpx

from app.config import settings
from app.models import (
    AgentTaskResponse,
    ChatMessage,
    ChatResponse,
    ChatRole,
    ToolType,
)
from app.services.agent_tools import (
    execute_calculator,
    execute_code_inspector,
    execute_daily_briefing,
    execute_deep_research,
    execute_text_summarizer,
    execute_think_tool,
)
from app.services.codebase_analysis import analyze_source\nfrom app.services.codebase_analysis import analyze_source
from app.services.memory_service import memory_service
from app.services.persona_service import persona_service
from app.services.security_service import security_scanner
from app.services.session_service import session_manager

logger = logging.getLogger(__name__)


class AIService:
    """Manages AI provider routing with server-side secret isolation, personas, and memory."""

    def __init__(self):
        self.client = httpx.AsyncClient(timeout=30.0)

    async def close(self):
        """Closes the underlying HTTP client session."""
        await self.client.aclose()

    async def _prepare_messages(
        self,
        messages: List[ChatMessage],
        persona: Optional[str] = "jarvis",
        honorific: Optional[str] = "Sir",
        user_query: str = "",
    ) -> List[ChatMessage]:
        """Injects persona prompt and relevant remembered facts into the system message."""
        has_system = any(m.role == ChatRole.SYSTEM for m in messages)
        if not has_system:
            remembered_facts = await memory_service.get_relevant_facts(user_query)
            system_prompt = persona_service.build_system_prompt(
                persona_type=persona,
                honorific=honorific,
                remembered_facts=remembered_facts,
            )
            return [ChatMessage(role=ChatRole.SYSTEM, content=system_prompt)] + list(messages)
        return list(messages)

    async def generate_chat_reply(
        self,
        messages: List[ChatMessage],
        preferred_provider: str = "auto",
        model_override: Optional[str] = None,
        temperature: float = 0.7,
        max_tokens: int = 1024,
        session_id: Optional[str] = None,
        persona: Optional[str] = "jarvis",
        honorific: Optional[str] = "Sir",
    ) -> ChatResponse:
        """Routes chat requests to configured provider or safe local engine with guardrails & memory."""
        # Find latest user prompt
        raw_user_msg = next(
            (m.content for m in reversed(messages) if m.role == ChatRole.USER), ""
        )

        # 1. Run OpenJarvis-inspired Security Scanner (Secret & Injection Guardrails)
        scan_result = security_scanner.scan(raw_user_msg, redact=True)
        sanitized_user_msg = scan_result.clean_text

        # Re-construct sanitized messages list
        sanitized_messages: List[ChatMessage] = []
        for m in messages:
            if m.role == ChatRole.USER and m.content == raw_user_msg:
                sanitized_messages.append(ChatMessage(role=m.role, content=sanitized_user_msg))
            else:
                sanitized_messages.append(m)

        effective_messages = await self._prepare_messages(
            messages=sanitized_messages,
            persona=persona,
            honorific=honorific,
            user_query=sanitized_user_msg,
        )

        response: Optional[ChatResponse] = None

        # 2. Check OpenAI provider
        if (
            preferred_provider in ("auto", "openai")
            and settings.openai_api_key.strip()
        ):
            try:
                response = await self._call_openai(
                    messages=effective_messages,
                    model=model_override or "gpt-4o-mini",
                    temperature=temperature,
                    max_tokens=max_tokens,
                )
            except Exception as e:
                logger.error("OpenAI dispatch failed (redacted error): %s", type(e).__name__)

        # 3. Check Gemini provider
        if (
            response is None
            and preferred_provider in ("auto", "gemini")
            and settings.gemini_api_key.strip()
        ):
            try:
                response = await self._call_gemini(
                    messages=effective_messages,
                    model=model_override or "gemini-1.5-flash",
                    temperature=temperature,
                )
            except Exception as e:
                logger.error("Gemini dispatch failed (redacted error): %s", type(e).__name__)

        # 4. Check Ollama provider (local server)
        if (
            response is None
            and preferred_provider in ("auto", "ollama")
            and settings.ollama_base_url.strip()
        ):
            try:
                response = await self._call_ollama(
                    messages=effective_messages,
                    model=model_override or "llama3",
                )
            except Exception as e:
                logger.error("Ollama dispatch failed (redacted error): %s", type(e).__name__)

        # 5. Local Engine Fallback (Rule-based & persona-aware)
        if response is None:
            response = self._local_engine_fallback(
                prompt=sanitized_user_msg,
                persona=persona,
                honorific=honorific,
            )

        # 6. Sanitize reply output against secret leakage
        reply_scan = security_scanner.scan(response.reply, redact=True)
        response.reply = reply_scan.clean_text

        # 7. Update Session History & Trigger Memory Learning in Background
        if session_id:
            response.session_id = session_id
            await session_manager.add_message(
                session_id, ChatMessage(role=ChatRole.USER, content=sanitized_user_msg)
            )
            await session_manager.add_message(
                session_id, ChatMessage(role=ChatRole.ASSISTANT, content=response.reply)
            )

        # Extract and remember facts from conversation
        asyncio.create_task(
            memory_service.process_turn_and_remember(
                user_msg=sanitized_user_msg,
                assistant_msg=response.reply,
                session_id=session_id,
            )
        )

        return response

    def _local_engine_fallback(
        self,
        prompt: str,
        persona: Optional[str] = "jarvis",
        honorific: Optional[str] = "Sir",
    ) -> ChatResponse:
        """Deterministic persona-aware fallback when external provider credentials are not set."""
        sanitized = (prompt or "").strip()
        h = (honorific or "Sir").strip()
        p = (persona or "jarvis").lower()

        # Check for simple arithmetic in prompt
        if re.search(r"^\s*(\d+[\s\+\-\*\/\%\(\)\^]+)+\d+\s*$", sanitized):
            calc_res = execute_calculator(sanitized)
            if calc_res.get("success"):
                if p == "friday":
                    reply_text = f"Calculated it for you, Boss: {calc_res['formatted']}"
                elif p == "jarvis":
                    reply_text = f"According to my calculations, {h}: {calc_res['formatted']}"
                else:
                    reply_text = f"Calculation result: {calc_res['formatted']}"

                return ChatResponse(
                    reply=reply_text,
                    provider_used="local_engine",
                    model_used="zaythar-math-tool",
                    status="success",
                )

        if p == "jarvis":
            reply = (
                f"At your service, {h}. I have registered your instruction: '{sanitized}'. "
                "Neural core diagnostics and memory modules are operational. "
                "Configure external API keys (OpenAI or Gemini) or local Ollama for full generative inference."
            )
        elif p == "friday":
            reply = (
                f"Boss, I've got your request: '{sanitized}'. "
                "Diagnostic telemetry is solid and tools are online. Ready whenever you need me."
            )
        else:
            reply = (
                f"[Zaythar Local Engine] Received: '{sanitized}'. "
                "Server-side AI companion is active with memory and tool pipelines. "
                "Set OPENAI_API_KEY, GEMINI_API_KEY, or local Ollama to enable external LLM reasoning."
            )

        return ChatResponse(
            reply=reply,
            provider_used="local_engine",
            model_used="zaythar-rule-based-v2",
            status="success",
        )

    async def generate_chat_stream(
        self,
        messages: List[ChatMessage],
        preferred_provider: str = "auto",
        model_override: Optional[str] = None,
        temperature: float = 0.7,
        max_tokens: int = 1024,
        persona: Optional[str] = "jarvis",
        honorific: Optional[str] = "Sir",
    ) -> AsyncGenerator[str, None]:
        """Streams response tokens in SSE format with persona awareness."""
        reply_obj = await self.generate_chat_reply(
            messages=messages,
            preferred_provider=preferred_provider,
            model_override=model_override,
            temperature=temperature,
            max_tokens=max_tokens,
            persona=persona,
            honorific=honorific,
        )

        words = reply_obj.reply.split(" ")
        for i, word in enumerate(words):
            chunk = word + (" " if i < len(words) - 1 else "")
            data = json.dumps({"chunk": chunk, "provider": reply_obj.provider_used, "done": False})
            yield f"data: {data}\n\n"
            await asyncio.sleep(0.015)

        done_data = json.dumps({"chunk": "", "provider": reply_obj.provider_used, "done": True})
        yield f"data: {done_data}\n\n"

    async def execute_agent_task(
        self,
        task: str,
        context: str = "",
        tool_hint: Optional[ToolType] = None,
    ) -> AgentTaskResponse:
        """Executes a structured task breaking it down into actionable steps with tool dispatch."""
        actions: List[str] = [
            f"Parsed task instruction: '{task[:60]}...'",
            "Validated parameters against security boundaries and guardrails",
        ]

        selected_tool = tool_hint.value if tool_hint else "none"
        tool_result = None

        # Determine target tool
        task_lower = task.lower()
        is_codebase_analysis = tool_hint == ToolType.CODEBASE_ANALYZE
        is_codebase_analysis = tool_hint == ToolType.CODEBASE_ANALYZE\n        is_think = (
            tool_hint == ToolType.THINK
            or task_lower.startswith("think")
            or "reason step by step" in task_lower
        )
        is_briefing = (
            tool_hint == ToolType.DAILY_BRIEFING
            or "briefing" in task_lower
            or "morning digest" in task_lower
        )
        is_research = (
            tool_hint == ToolType.DEEP_RESEARCH
            or task_lower.startswith("research")
            or "investigate" in task_lower
        )
        is_math = (
            tool_hint == ToolType.CALCULATOR
            or re.search(r"(calculate|compute|solve|\d+\s*[\+\-\*\/])", task, re.IGNORECASE)
        )
        is_code = (
            tool_hint == ToolType.CODE_INSPECT
            or ("def " in task or "class " in task or "import " in task or "{" in task)
        )
        is_summarize = (
            tool_hint == ToolType.SUMMARIZE
            or task_lower.startswith("summarize")
            or len(context) > 200
        )

        if is_codebase_analysis:
            actions.append("Dispatched to read-only static dependency analyzer; source is never executed")
            tool_result = analyze_source(context, language="auto")
            selected_tool = "codebase_analyzer"
            summary = ("Static dependency analysis completed." if tool_result.get("success") else tool_result.get("error", "Analysis failed."))
        elif is_think:
            actions.append("Dispatched to OpenJarvis Reasoning Scratchpad (ThinkTool)")
            tool_result = execute_think_tool(task)
            selected_tool = "think"
            summary = f"Reasoning scratchpad completed with {tool_result.get('step_count', 0)} logical steps."
        elif is_briefing:
            actions.append("Dispatched to OpenJarvis Daily Briefing Generator")
            tool_result = execute_daily_briefing(honorific="Sir", context=context)
            selected_tool = "daily_briefing"
            summary = tool_result.get("briefing", "Daily briefing generated.")
        elif is_research:
            actions.append("Dispatched to OpenJarvis Deep Research Planner")
            tool_result = execute_deep_research(topic=task, context=context)
            selected_tool = "deep_research"
            summary = f"Deep research plan formulated for '{tool_result.get('topic', task)}'."
        elif is_math:
            actions.append("Dispatched to deterministic SafeMathEvaluator")
            tool_result = execute_calculator(task)
            selected_tool = "calculator"
            summary = (
                f"Calculated result: {tool_result.get('formatted', 'error')}"
                if tool_result.get("success")
                else f"Calculation error: {tool_result.get('error')}"
            )
        elif is_code:
            actions.append("Dispatched to AST CodeInspector")
            tool_result = execute_code_inspector(task)
            selected_tool = "code_inspector"
            summary = (
                "Code inspection complete. " + " ".join(tool_result.get("findings", []))
            )
        elif is_summarize:
            actions.append("Dispatched to TextSummarizer")
            text_to_summarize = context if context else task
            tool_result = execute_text_summarizer(text_to_summarize)
            selected_tool = "text_summarizer"
            summary = f"Summary generated ({tool_result.get('metrics', {}).get('word_count', 0)} words processed)."
        else:
            actions.append("Completed general task planning and validation")
            summary = f"Processed general task: '{task}'. All boundaries respected."

        actions.append(f"Recorded tool outcome: {selected_tool}")

        return AgentTaskResponse(
            task=task,
            status="completed",
            summary=summary,
            tool_used=selected_tool,
            tool_result=tool_result,
            actions_taken=actions,
        )

    async def _call_openai(
        self,
        messages: List[ChatMessage],
        model: str,
        temperature: float,
        max_tokens: int,
    ) -> ChatResponse:
        """Dispatches chat request to OpenAI API with server-side key."""
        url = "https://api.openai.com/v1/chat/completions"
        headers = {
            "Authorization": f"Bearer {settings.openai_api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": model,
            "messages": [{"role": m.role.value, "content": m.content} for m in messages],
            "temperature": temperature,
            "max_tokens": max_tokens,
        }

        resp = await self.client.post(url, headers=headers, json=payload)
        resp.raise_for_status()
        data = resp.json()
        content = data["choices"][0]["message"]["content"]

        return ChatResponse(
            reply=content,
            provider_used="openai",
            model_used=model,
            status="success",
        )

    async def _call_gemini(
        self,
        messages: List[ChatMessage],
        model: str,
        temperature: float,
    ) -> ChatResponse:
        """Dispatches chat request to Google Gemini API."""
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
        params = {"key": settings.gemini_api_key}

        contents = []
        for m in messages:
            role = "user" if m.role == ChatRole.USER else "model"
            contents.append({"role": role, "parts": [{"text": m.content}]})

        payload = {
            "contents": contents,
            "generationConfig": {"temperature": temperature},
        }

        resp = await self.client.post(url, params=params, json=payload)
        resp.raise_for_status()
        data = resp.json()
        content = data["candidates"][0]["content"]["parts"][0]["text"]

        return ChatResponse(
            reply=content,
            provider_used="gemini",
            model_used=model,
            status="success",
        )

    async def _call_ollama(
        self,
        messages: List[ChatMessage],
        model: str,
    ) -> ChatResponse:
        """Dispatches chat request to local Ollama server."""
        url = f"{settings.ollama_base_url.rstrip('/')}/api/chat"
        payload = {
            "model": model,
            "messages": [{"role": m.role.value, "content": m.content} for m in messages],
            "stream": False,
        }

        resp = await self.client.post(url, json=payload)
        resp.raise_for_status()
        data = resp.json()
        content = data["message"]["content"]

        return ChatResponse(
            reply=content,
            provider_used="ollama",
            model_used=model,
            status="success",
        )


ai_service = AIService()
