"""Permissioned multi-agent orchestration.

Each specialist receives the same bounded task and its own role prompt.
Specialists run concurrently; a coordinator synthesizes their deliverables.
No arbitrary tools, shell access, or private chain-of-thought are exposed.
"""
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Any

from backend.brain import run_brain

AGENT_ROLES = {
    "research": "Research specialist. Produce verifiable findings, distinguish facts from assumptions, and flag missing sources.",
    "coding": "Senior software engineer. Produce implementation guidance/code, tests, security considerations, and edge cases.",
    "analysis": "Analyst. Evaluate constraints, alternatives, risks, trade-offs, and a reasoned recommendation.",
    "writing": "Writing specialist. Produce clear, audience-appropriate wording and preserve the task's intent.",
    "creative": "Creative specialist. Generate original concepts with concrete execution details and constraints.",
}

MAX_TASK_CHARS = 12000
MAX_AGENT_OUTPUT_CHARS = 7000


def _run_specialist(name: str, task: str, provider: str, language: str) -> dict[str, Any]:
    role = AGENT_ROLES[name]
    prompt = (
        "You are one specialist in Zaythar AI's multi-agent team. "
        + role + " Work only on the user's task. Do not claim actions you did not perform. "
        + f"Write your deliverable in {language}. "
        + "Return a concise, structured deliverable with findings, recommendations, and uncertainties."
    )
    result = run_brain(provider, prompt, [], task)
    return {
        "agent": name,
        "status": "completed",
        "provider": result.get("provider", "unknown"),
        "model": result.get("model", "unknown"),
        "output": (result.get("reply") or "")[:MAX_AGENT_OUTPUT_CHARS],
    }


def run_multi_agent(task: str, agents: list[str], provider: str = "hybrid", language: str = "English") -> dict[str, Any]:
    """Run up to four allow-listed specialists concurrently, then synthesize."""
    task = task.strip()
    if not task or len(task) > MAX_TASK_CHARS:
        raise ValueError(f"Task must contain 1 to {MAX_TASK_CHARS} characters")
    selected = list(dict.fromkeys(agents))
    if not selected:
        raise ValueError("Select at least one specialist")
    if len(selected) > 4:
        raise ValueError("Select no more than four specialists")
    invalid = [name for name in selected if name not in AGENT_ROLES]
    if invalid:
        raise ValueError("Unsupported specialist: " + ", ".join(invalid))

    results: list[dict[str, Any]] = []
    with ThreadPoolExecutor(max_workers=min(4, len(selected))) as pool:
        futures = {pool.submit(_run_specialist, name, task, provider, language): name for name in selected}
        for future in as_completed(futures):
            name = futures[future]
            try:
                results.append(future.result())
            except Exception as exc:
                results.append({"agent": name, "status": "failed", "error": type(exc).__name__})

    results.sort(key=lambda item: selected.index(item["agent"]))
    completed = [item for item in results if item["status"] == "completed"]
    if not completed:
        raise RuntimeError("All selected specialists failed. Check provider configuration and service availability.")

    evidence = "\\n\\n".join(
        f"[{item['agent'].upper()} SPECIALIST]\n{item['output']}" for item in completed
    )
    coordinator_prompt = (
        "You are Zaythar AI's lead coordinator. Synthesize specialist deliverables into one useful answer. "
        "Resolve contradictions explicitly, preserve useful detail, identify uncertainty, and give prioritized next steps. "
        "Do not invent verification or claim that code was executed. Do not reveal private chain-of-thought. "
        f"Write in {language}."
    )
    synthesis = run_brain(
        provider,
        coordinator_prompt,
        [],
        "USER TASK:\n" + task + "\n\nSPECIALIST DELIVERABLES:\n" + evidence,
    )
    return {
        "reply": synthesis.get("reply") or "",
        "provider": synthesis.get("provider", "unknown"),
        "model": synthesis.get("model", "unknown"),
        "agents": results,
        "completed_agents": len(completed),
        "total_agents": len(selected),
    }
