"""Regression tests for conservative long-term memory behavior."""

import asyncio

from app.services.memory_service import FactExtractor, MemoryService


def test_fact_extractor_does_not_turn_ordinary_i_am_statements_into_identity():
    assert FactExtractor.extract_from_turn("I am tired after work.") == []
    assert FactExtractor.extract_from_turn("I am learning Python today.") == []


def test_fact_extractor_keeps_explicit_preferences_and_remember_requests():
    facts = FactExtractor.extract_from_turn(
        "I prefer concise answers. Please remember that my project is Zaythar AI."
    )
    assert "I prefer concise answers" in facts
    assert "Please remember that my project is Zaythar AI" in facts


def test_fact_extractor_supports_common_roman_urdu_preference_phrase():
    facts = FactExtractor.extract_from_turn("Mujhe Roman Urdu mein jawab pasand hai.")
    assert facts == ["Mujhe Roman Urdu mein jawab pasand hai"]


def test_relevant_memory_does_not_return_zero_overlap_facts(tmp_path):
    async def run():
        service = MemoryService()
        service.store = service.store.__class__(storage_path=str(tmp_path / "facts.json"))
        await service.add_fact("User prefers Roman Urdu responses.", category="preference")
        await service.add_fact("User is building Zaythar AI.", category="project")

        matches = await service.get_relevant_facts("How should you reply in Urdu?")
        assert matches == ["User prefers Roman Urdu responses."]

        unrelated = await service.get_relevant_facts("What is the weather?")
        assert unrelated == []

    asyncio.run(run())
