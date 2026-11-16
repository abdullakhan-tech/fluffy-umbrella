"""Intent routing and compact task decomposition (not exposed chain-of-thought)."""
import re
from backend.tools import run_tool

_CALC = re.compile(r"^(?:calculate|compute|what is)\s+([0-9().+*/%\- ]{1,120})\??$", re.I)

def route_tool(message: str):
    match = _CALC.match(message.strip())
    if match:
        return run_tool("calculator", {"expression": match.group(1).strip()})
    if re.search(r"\b(utc time|current utc time|time in utc)\b", message, re.I):
        return run_tool("utc_time", {})
    return None

def reasoning_hint(message: str) -> str:
    """A short, user-safe planning hint; never asks the model to reveal private reasoning."""
    text = message.strip()
    if len(text) > 500:
        return "Break the request into key goals, constraints, and actionable steps."
    if re.search(r"\b(compare| versus | vs |pros and cons)\b", text, re.I):
        return "Compare options using consistent criteria, evidence, trade-offs, and a recommendation."
    if re.search(r"\b(plan|build|create|implement|project)\b", text, re.I):
        return "Identify requirements, sequence the work, check dependencies, and validate the result."
    return "Answer directly; verify assumptions and state uncertainty where relevant."
