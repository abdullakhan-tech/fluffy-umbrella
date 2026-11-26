# Static Codebase Analysis Agent

Zaythar's Python companion backend exposes a read-only static dependency inventory through the existing agent endpoint.

## Request

```json
{
  "task": "Analyze these source dependencies",
  "context": "import os\nimport httpx",
  "tool_hint": "codebase_analyze"
}
```

POST to `/api/v1/agent/task`. The source snippet is supplied as data in `context`; it is never imported, executed, or used to read a filesystem path. The input is bounded to 20,000 characters.

## Current coverage

- Python: AST syntax validation, import inventory, and standard-library versus external/project import grouping.
- Kotlin/Gradle: regex-based import and declared dependency inventory.
- Unsupported languages are rejected.
- Results explicitly state analysis limitations; Kotlin/Gradle syntax and transitive dependency resolution are not claimed.

This is a snippet-level analyzer, not a clone of a full repository index. For whole-repository symbol graphs, use the optional local Codebase Memory MCP developer tooling documented in `docs/CODEBASE_MEMORY_MCP.md`.
