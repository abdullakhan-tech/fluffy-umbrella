from backend.memory import save_memory, list_memories, delete_memory, clear_memories
from backend.tools import calculate

def test_memory_is_scoped_and_deletable(tmp_path, monkeypatch):
    import backend.memory as memory
    monkeypatch.setattr(memory, "DB_PATH", tmp_path / "test.db")
    item = save_memory("session-a", "Prefers Urdu")
    assert list_memories("session-a")[0]["content"] == "Prefers Urdu"
    assert list_memories("session-b") == []
    assert delete_memory("session-a", item["id"])
    assert list_memories("session-a") == []

def test_calculator_safe_arithmetic():
    assert calculate("2 + 3 * 4") == "14"


import pytest
from backend.tools import run_tool


@pytest.mark.parametrize("name", ["shell", "python", "filesystem", "__import__", "browser"])
def test_tool_allow_list_rejects_unregistered_tools(name):
    with pytest.raises(ValueError, match="not available"):
        run_tool(name, {})


def test_calculator_rejects_python_execution_and_attribute_access():
    with pytest.raises(ValueError):
        calculate("__import__('os').system('id')")
    with pytest.raises(ValueError):
        calculate("(1).__class__")
