from app.services.codebase_analysis import analyze_source

def test_python_dependency_inventory_and_syntax():
    result = analyze_source("import os\nimport httpx\nfrom app.core import config", "python")
    assert result["success"] is True
    assert result["syntax_valid"] is True
    assert result["imports"] == ["app", "httpx", "os"]
    assert "os" in result["stdlib_dependencies"]
    assert "httpx" in result["external_or_project_dependencies"]

def test_python_syntax_error_is_reported_without_execution():
    result = analyze_source("def broken(:\n  pass", "python")
    assert result["success"] is True
    assert result["syntax_valid"] is False
    assert result["error"].startswith("Line 1:")

def test_kotlin_import_and_gradle_dependency_inventory():
    source = 'package com.example.app\nimport kotlinx.coroutines.flow.StateFlow\ndependencies { implementation("com.squareup.okhttp3:okhttp:4.12.0") }'
    result = analyze_source(source, "kotlin")
    assert result["imports"] == ["kotlinx.coroutines.flow.StateFlow"]
    assert result["declared_dependencies"] == ["com.squareup.okhttp3:okhttp:4.12.0"]

def test_source_size_is_bounded():
    result = analyze_source("x" * 20_001, "python")
    assert result["success"] is False
