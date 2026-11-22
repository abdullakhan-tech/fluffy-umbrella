from app.services.personality import AGENT_CATALOG, PERSONALITIES, THEME_PRESETS, system_prompt


def test_all_personalities_have_distinct_prompts():
    assert {"friday", "professional", "friendly", "developer", "researcher"} == set(PERSONALITIES)
    assert len(set(PERSONALITIES.values())) == len(PERSONALITIES)


def test_unknown_personality_falls_back_to_friday():
    assert system_prompt("unknown") == PERSONALITIES["friday"]


def test_theme_presets_have_required_palette_fields():
    required = {"id", "name", "mode", "background", "surface", "primary", "accent"}
    assert len(THEME_PRESETS) >= 5
    assert all(required.issubset(theme) for theme in THEME_PRESETS)


def test_agent_catalog_has_distinct_specialists():
    ids = [agent["id"] for agent in AGENT_CATALOG]
    assert len(ids) >= 5
    assert len(ids) == len(set(ids))
    assert all(agent["capabilities"] for agent in AGENT_CATALOG)
