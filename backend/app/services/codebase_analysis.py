"""Read-only, bounded source dependency analysis for developer-provided snippets.

This module never opens files, imports analyzed modules, or executes source code.
"""
import ast
import re
from typing import Any, Dict, List

MAX_SOURCE_CHARS = 20_000
MAX_FINDINGS = 100

def analyze_source(source: str, language: str = "auto") -> Dict[str, Any]:
    """Extract imports and lightweight dependency signals from supplied source text."""
    if not isinstance(source, str) or not source.strip():
        return {"success": False, "error": "Source text is required."}
    if len(source) > MAX_SOURCE_CHARS:
        return {"success": False, "error": f"Source exceeds {MAX_SOURCE_CHARS} characters."}

    text = source.strip()
    selected = language.lower().strip()
    if selected == "auto":
        selected = "python" if re.search(r"^\s*(from\s+\w+\s+import|import\s+\w+)", text, re.M) else "kotlin"
    if selected in {"py", "python"}:
        return _analyze_python(text)
    if selected in {"kt", "kts", "kotlin", "gradle"}:
        return _analyze_kotlin(text)
    return {"success": False, "error": "Supported languages: python, kotlin, gradle, auto."}

def _analyze_python(source: str) -> Dict[str, Any]:
    try:
        tree = ast.parse(source)
    except SyntaxError as exc:
        return {"success": True, "language": "python", "syntax_valid": False,
                "error": f"Line {exc.lineno}: {exc.msg}", "imports": [], "findings": []}

    imports = set()
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            imports.update(alias.name.split(".")[0] for alias in node.names)
        elif isinstance(node, ast.ImportFrom) and node.module:
            imports.add(node.module.split(".")[0])
    imported = sorted(imports)
    stdlib = sorted(name for name in imported if name in getattr(__import__("sys"), "stdlib_module_names", set()))
    third_party = sorted(set(imported) - set(stdlib))
    return {
        "success": True, "language": "python", "syntax_valid": True,
        "metrics": {"line_count": len(source.splitlines()), "import_count": len(imported)},
        "imports": imported, "stdlib_dependencies": stdlib,
        "external_or_project_dependencies": third_party,
        "findings": [f"Review dependency: {name}" for name in third_party][:MAX_FINDINGS],
        "limitations": ["Static snippet analysis only; no repository-wide import resolution or package lock inspection."],
    }

def _analyze_kotlin(source: str) -> Dict[str, Any]:
    imports = sorted(set(re.findall(r"^\s*import\s+([A-Za-z_][\w.]*)", source, re.M)))
    dependencies = sorted(set(re.findall(
        r"""(?:implementation|api|runtimeOnly|compileOnly|testImplementation)\s*\(?\s*["']([^"']+)["']""",
        source,
    )))
    package_names = sorted(set(re.findall(r"^\s*package\s+([\w.]+)", source, re.M)))
    return {
        "success": True, "language": "kotlin_or_gradle", "syntax_valid": None,
        "metrics": {"line_count": len(source.splitlines()), "import_count": len(imports),
                    "declared_dependency_count": len(dependencies)},
        "packages": package_names, "imports": imports, "declared_dependencies": dependencies,
        "findings": [f"Review declared dependency: {item}" for item in dependencies][:MAX_FINDINGS],
        "limitations": ["Regex-based inventory only; does not resolve Gradle variants, version catalogs, transitive dependencies, or Kotlin syntax."],
    }
