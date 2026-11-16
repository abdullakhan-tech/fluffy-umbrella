import pytest

from backend import knowledge


@pytest.fixture
def isolated_db(tmp_path, monkeypatch):
    monkeypatch.setattr(knowledge, "DB_PATH", tmp_path / "knowledge-test.db")


def test_knowledge_fact_crud_and_session_isolation(isolated_db):
    fact = knowledge.add_fact("session_a", "Zaythar", "uses", "hybrid AI", "operator")
    assert fact["subject"] == "Zaythar"
    assert len(knowledge.list_facts("session_a")) == 1
    assert knowledge.list_facts("session_b") == []
    assert "Zaythar — uses — hybrid AI" in knowledge.knowledge_context("session_a")
    assert knowledge.delete_fact("session_a", fact["id"]) is True
    assert knowledge.delete_fact("session_a", fact["id"]) is False


def test_knowledge_clear_only_clears_selected_session(isolated_db):
    knowledge.add_fact("one", "A", "relates", "B")
    knowledge.add_fact("two", "C", "relates", "D")
    assert knowledge.clear_facts("one") == 1
    assert knowledge.list_facts("one") == []
    assert len(knowledge.list_facts("two")) == 1


@pytest.mark.parametrize("value", ["", "x" * 301, None])
def test_knowledge_rejects_invalid_fields(isolated_db, value):
    with pytest.raises(ValueError):
        knowledge.add_fact("session", value, "relation", "object")


def test_knowledge_caps_facts_per_session(isolated_db, monkeypatch):
    monkeypatch.setattr(knowledge, "MAX_FACTS", 1)
    knowledge.add_fact("session", "first", "relation", "object")
    with pytest.raises(ValueError, match="limit"):
        knowledge.add_fact("session", "second", "relation", "object")
