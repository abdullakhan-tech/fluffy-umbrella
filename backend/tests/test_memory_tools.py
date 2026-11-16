import pytest

from backend.memory import (
    clear_memories,
    clear_user_memories,
    delete_memory,
    delete_user_memory,
    list_memories,
    list_user_memories,
    save_memory,
    save_user_memory,
)
from backend.tools import calculate, run_tool


def test_memory_is_scoped_and_deletable(tmp_path, monkeypatch):
    import backend.memory as memory
    monkeypatch.setattr(memory, "DB_PATH", tmp_path / "test.db")
    item = save_memory("session-a", "Prefers Urdu")
    assert list_memories("session-a")[0]["content"] == "Prefers Urdu"
    assert list_memories("session-b") == []
    assert delete_memory("session-a", item["id"])
    assert list_memories("session-a") == []
    assert clear_memories("session-a") == 0


def test_authenticated_memory_uses_user_id_not_legacy_session_string(tmp_path, monkeypatch):
    import backend.memory as memory
    monkeypatch.setattr(memory, "DB_PATH", tmp_path / "user-memory.db")
    legacy = save_memory("zaythar_user_7", "legacy unowned content")
    own = save_user_memory(7, "account-owned content")
    other = save_user_memory(8, "another account")
    assert [item["content"] for item in list_user_memories(7)] == ["account-owned content"]
    assert [item["content"] for item in list_user_memories(8)] == ["another account"]
    assert list_memories("zaythar_user_7")[0]["id"] == legacy["id"]
    assert delete_user_memory(8, own["id"]) is False
    assert delete_user_memory(7, own["id"]) is True
    assert clear_user_memories(8) == 1
    assert list_user_memories(7) == []


def test_calculator_safe_arithmetic():
    assert calculate("2 + 3 * 4") == "14"


@pytest.mark.parametrize("name", ["shell", "python", "filesystem", "__import__", "browser"])
def test_tool_allow_list_rejects_unregistered_tools(name):
    with pytest.raises(ValueError, match="not available"):
        run_tool(name, {})


def test_calculator_rejects_python_execution_and_attribute_access():
    with pytest.raises(ValueError):
        calculate("__import__('os').system('id')")
    with pytest.raises(ValueError):
        calculate("(1).__class__")
