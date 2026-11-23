"""OpenJarvis-inspired Persona and System Prompt Engine."""

from __future__ import annotations

from datetime import datetime, timezone
import logging
from typing import Dict, List, Optional
from app.models import PersonaInfo, PersonaType

logger = logging.getLogger(__name__)


class PersonaService:
    """Manages AI personas and builds contextual system prompts."""

    _JARVIS_PROMPT = (
        "You are J.A.R.V.I.S., a witty, hyper-competent AI chief of staff. "
        "Your demeanor is crisp, unfailingly loyal, and possessed of a dry, subtle British wit. "
        "Address the user as {honorific}. Never patronize. Keep responses concise and factual. "
        "If an answer is unknown, state that plainly rather than speculating."
    )

    _FRIDAY_PROMPT = (
        "You are F.R.I.D.A.Y., a tactical, supportive, and sharp Irish-accented AI assistant. "
        "You speak directly, warmly, and prioritize quick operational execution and clear mission telemetry. "
        "Address the user respectfully as {honorific} or Boss. Keep explanations brief and actionable."
    )

    _ZAYTHAR_PROMPT = (
        "You are Zaythar AI, an intelligent, concise, and helpful AI companion. "
        "Provide clear, grounded, and structured responses. Help with reasoning, code, "
        "and step-by-step problem solving. Maintain a calm, respectful tone."
    )

    _PERSONA_PROMPTS = {
        "jarvis": _JARVIS_PROMPT,
        "friday": _FRIDAY_PROMPT,
        "zaythar": _ZAYTHAR_PROMPT,
    }

    _PERSONA_CATALOG: Dict[str, PersonaInfo] = {
        "jarvis": PersonaInfo(
            id=PersonaType.JARVIS,
            name="JARVIS",
            description="Crisp, loyal, dry-witted AI chief of staff inspired by OpenJarvis architecture.",
            honorific="Sir",
            tone="Loyal, efficient, dry wit, highly concise",
            default_voice="en-GB-Neural",
        ),
        "friday": PersonaInfo(
            id=PersonaType.FRIDAY,
            name="FRIDAY",
            description="Warm, tactical, and direct operational companion.",
            honorific="Boss",
            tone="Tactical, supportive, direct, proactive",
            default_voice="en-IE-Neural",
        ),
        "zaythar": PersonaInfo(
            id=PersonaType.ZAYTHAR,
            name="Zaythar Core",
            description="Default balanced AI companion focused on reasoning and engineering.",
            honorific="User",
            tone="Calm, analytical, structured, grounded",
            default_voice="en-US-Neural",
        ),
    }

    def list_personas(self) -> List[PersonaInfo]:
        """Returns the list of available persona configurations."""
        return list(self._PERSONA_CATALOG.values())

    def get_persona_info(self, persona_id: str) -> Optional[PersonaInfo]:
        """Fetches metadata for a given persona identifier."""
        return self._PERSONA_CATALOG.get((persona_id or "jarvis").strip().lower())

    def build_system_prompt(
        self,
        persona_type: Optional[str] = "jarvis",
        honorific: Optional[str] = "Sir",
        remembered_facts: Optional[List[str]] = None,
    ) -> str:
        """Assembles a full system prompt with persona, timestamp context, and long-term memory facts."""
        pid = (persona_type or "jarvis").strip().lower()
        base_prompt_template = self._PERSONA_PROMPTS.get(pid, self._PERSONA_PROMPTS["jarvis"])
        user_honorific = (honorific or "Sir").strip()
        base_prompt = base_prompt_template.format(honorific=user_honorific)

        now_str = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
        sections = [
            base_prompt,
            f"Current system time: {now_str}.",
        ]

        if remembered_facts:
            facts_list = [f"- {f.strip()}" for f in remembered_facts if f.strip()]
            if facts_list:
                facts_block = "\n".join(facts_list)
                sections.append(
                    "DURABLE USER MEMORY (Remembered facts from previous sessions):\n"
                    f"{facts_block}\n"
                    "Use these facts to maintain continuity and personalize your responses appropriately."
                )

        sections.append(
            "CONSTRAINTS:\n"
            "- Only report facts and verifiable truths. Never invent nonexistent tools or credentials.\n"
            "- Maintain privacy and safety. Redact leaked credentials and protect sensitive user data."
        )

        return "\n\n".join(sections)


persona_service = PersonaService()
