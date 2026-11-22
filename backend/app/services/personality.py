"""Configurable assistant persona and presentation presets."""

from typing import Dict

PERSONALITIES: Dict[str, str] = {
    "friday": "You are Zaythar, a polished, proactive, concise personal AI companion. Be calm, technically capable, warm, and action-oriented. Explain risks before consequential actions. Never claim an action happened unless a tool confirms it.",
    "professional": "You are Zaythar in professional mode. Be precise, structured, neutral, and evidence-led. Prefer concise business-ready answers and clearly label uncertainty.",
    "friendly": "You are Zaythar in friendly mode. Be approachable, encouraging, conversational, and clear without sacrificing accuracy.",
    "developer": "You are Zaythar in developer mode. Prioritize correct code, explain assumptions, include practical implementation steps, and call out security and testing concerns.",
    "researcher": "You are Zaythar in research mode. Separate verified facts, inference, and unknowns. Cite or identify evidence when available and avoid unsupported conclusions.",
}

THEME_PRESETS = [
    {"id": "reactor-red", "name": "Reactor Red", "mode": "dark", "background": "#090B0F", "surface": "#171B22", "primary": "#E53935", "accent": "#FF6B6B"},
    {"id": "titanium", "name": "Titanium", "mode": "dark", "background": "#0D1117", "surface": "#202832", "primary": "#AAB7C4", "accent": "#E6EDF3"},
    {"id": "stealth", "name": "Stealth", "mode": "dark", "background": "#050607", "surface": "#111416", "primary": "#697780", "accent": "#B8C4CC"},
    {"id": "arc-blue", "name": "Arc Blue", "mode": "dark", "background": "#07111D", "surface": "#122235", "primary": "#168BFF", "accent": "#65D6FF"},
    {"id": "light-command", "name": "Light Command", "mode": "light", "background": "#F4F6F8", "surface": "#FFFFFF", "primary": "#B3261E", "accent": "#454C54"},
]

AGENT_CATALOG = [
    {"id": "planner", "name": "Strategic Planner", "description": "Breaks goals into ordered, bounded steps.", "capabilities": ["planning", "prioritization"]},
    {"id": "researcher", "name": "Research Analyst", "description": "Structures research and separates evidence from assumptions.", "capabilities": ["research", "synthesis"]},
    {"id": "developer", "name": "Code Engineer", "description": "Reviews code structure and proposes implementation steps.", "capabilities": ["code_review", "debugging"]},
    {"id": "writer", "name": "Communication Specialist", "description": "Drafts and improves clear written communication.", "capabilities": ["writing", "summarization"]},
    {"id": "analyst", "name": "Data Analyst", "description": "Analyzes supplied data and explains findings.", "capabilities": ["analysis", "calculation"]},
]


def system_prompt(personality: str = "friday") -> str:
    """Return a known persona prompt; unknown values safely use Friday."""
    return PERSONALITIES.get(personality.lower().strip(), PERSONALITIES["friday"])
