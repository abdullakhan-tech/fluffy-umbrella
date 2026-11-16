import pytest

from backend import agents


def test_multi_agent_runs_specialists_and_coordinator(monkeypatch):
    calls = []

    def fake_run_brain(provider, system_prompt, history, message):
        calls.append((provider, system_prompt, message))
        if "lead coordinator" in system_prompt.lower():
            return {"reply": "Combined result", "provider": "openai", "model": "test-model"}
        return {"reply": "Specialist output", "provider": "gemini", "model": "test-specialist"}

    monkeypatch.setattr(agents, "run_brain", fake_run_brain)
    result = agents.run_multi_agent("Build a secure API", ["research", "coding"], "hybrid", "English")

    assert result["reply"] == "Combined result"
    assert result["completed_agents"] == 2
    assert result["total_agents"] == 2
    assert [item["agent"] for item in result["agents"]] == ["research", "coding"]
    assert all(item["status"] == "completed" for item in result["agents"])
    assert len(calls) == 3  # two specialists and one coordinator


def test_multi_agent_isolates_a_specialist_failure(monkeypatch):
    def fake_run_brain(provider, system_prompt, history, message):
        if "Research specialist" in system_prompt:
            raise RuntimeError("provider unavailable")
        if "lead coordinator" in system_prompt.lower():
            return {"reply": "Coding result synthesized", "provider": "openai", "model": "test"}
        return {"reply": "Code output", "provider": "openai", "model": "test"}

    monkeypatch.setattr(agents, "run_brain", fake_run_brain)
    result = agents.run_multi_agent("Fix a bug", ["research", "coding"])
    assert result["completed_agents"] == 1
    assert result["agents"][0]["status"] == "failed"
    assert result["agents"][1]["status"] == "completed"


@pytest.mark.parametrize("task,names", [
    ("", ["coding"]),
    ("Do work", []),
    ("Do work", ["unknown"]),
    ("Do work", ["research", "coding", "analysis", "writing", "creative"]),
])
def test_multi_agent_rejects_invalid_input(task, names):
    with pytest.raises(ValueError):
        agents.run_multi_agent(task, names)


def test_multi_agent_includes_shared_context_and_saves_summary(monkeypatch):
    captured = []
    monkeypatch.setattr(agents, "memory_context", lambda session: "Prior session preference")
    monkeypatch.setattr(agents, "knowledge_context", lambda session: "Zaythar — uses — hybrid")
    monkeypatch.setattr(agents, "save_memory", lambda session, content: captured.append((session, content)))

    def fake_run_brain(provider, system_prompt, history, message):
        if "lead coordinator" in system_prompt.lower():
            assert "Prior session preference" in message
            assert "Zaythar — uses — hybrid" in message
            return {"reply": "Remembered result", "provider": "openai", "model": "test"}
        assert "Prior session preference" in message
        assert "Zaythar — uses — hybrid" in message
        return {"reply": "Specialist result", "provider": "openai", "model": "test"}

    monkeypatch.setattr(agents, "run_brain", fake_run_brain)
    result = agents.run_multi_agent("Continue the work", ["analysis"], session_id="operator_1")
    assert result["reply"] == "Remembered result"
    assert captured and captured[0][0] == "operator_1"
    assert "Remembered result" in captured[0][1]


def test_multi_agent_uses_account_owned_context_and_persists_to_same_account(monkeypatch):
    captured = []
    saved = []
    monkeypatch.setattr(agents, "user_memory_context", lambda user_id: f"private memory for {user_id}")
    monkeypatch.setattr(agents, "user_knowledge_context", lambda user_id: f"private facts for {user_id}")
    monkeypatch.setattr(agents, "save_user_memory", lambda user_id, content: saved.append((user_id, content)))

    def fake_run_brain(provider, system_prompt, history, message):
        if "lead coordinator" in system_prompt.lower():
            assert "private memory for 41" in message
            assert "private facts for 41" in message
            return {"reply": "Account-scoped result", "provider": "openai", "model": "test"}
        assert "private memory for 41" in message
        return {"reply": "Specialist result", "provider": "openai", "model": "test"}

    monkeypatch.setattr(agents, "run_brain", fake_run_brain)
    result = agents.run_multi_agent("Continue securely", ["analysis"], user_id=41)
    assert result["reply"] == "Account-scoped result"
    assert saved and saved[0][0] == 41
