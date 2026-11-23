# Codebase Memory MCP integration

Zaythar AI can use [DeusData/codebase-memory-mcp](https://github.com/DeusData/codebase-memory-mcp) as an **optional local developer-tooling service**. It indexes a checked-out source tree into a persistent local knowledge graph and exposes repository search, symbol, dependency, call-path, documentation, and impact-analysis tools over MCP.

This is intentionally a developer-workstation integration, not an Android runtime dependency. The upstream server is a native C executable that reads local files and speaks MCP over stdio; an Android APK or a remotely hosted FastAPI instance cannot safely assume that executable or a developer's local checkout exists. Zaythar's existing Kotlin/Compose app, Firebase AI Logic, Firestore memory, and Python companion backend remain independent.

## Install

Install the upstream native release using the instructions for your operating system:

- [Upstream installation and quick start](https://github.com/DeusData/codebase-memory-mcp#quick-start)
- [Latest releases](https://github.com/DeusData/codebase-memory-mcp/releases/latest)

The upstream installer supports macOS/Linux and Windows. Review the installer and release provenance before running it. No API key or hosted indexing service is required for local indexing.

Confirm the executable is available on PATH:

```bash
codebase-memory-mcp --help
```

## Connect an MCP-capable coding agent

This repository includes a project MCP configuration at `.mcp.json`:

```json
{
  "mcpServers": {
    "codebase-memory-mcp": {
      "command": "codebase-memory-mcp",
      "args": []
    }
  }
}
```

The command must resolve to the installed executable in the environment that launches the coding agent. Restart the agent after installation and confirm that the server exposes the upstream tools. If your client uses a different MCP configuration format, use the upstream installer's supported-client integration instead.

## Index this checkout

From the repository root, use the upstream CLI's project-index command documented by the installed version, or open CBM Lens and choose **Add project index**. Select this checkout as the local repository. The graph database and index metadata are local artifacts and are not intended to be committed.

The optional Lens UI is local-only by default. Its documented default address is `http://127.0.0.1:9749`; do not expose it to an untrusted network.

## Agent workflow

Use graph results to orient quickly, then inspect the actual source before changing code:

1. Search for the relevant symbols and documentation.
2. Trace callers, callees, and cross-file dependencies.
3. Check the indexed coverage/freshness before making absence or impact claims.
4. Read the affected source and tests directly.
5. Make a focused change and run the repository's normal CI/test commands.

The graph is an aid, not proof that code is correct or that every relationship was indexed. Generated files, ignored paths, unsupported syntax, dynamic dispatch, and stale indexes can create coverage gaps.

## Security and privacy

- Indexes can contain source structure, symbol names, paths, and snippets from this private working copy. Treat the local index as source-code data.
- Keep the index, caches, and logs out of Git; `.codebase-memory/` is ignored.
- Do not index directories containing credentials, production exports, personal data, or unrelated repositories.
- Do not publish Lens or any local daemon to the public internet.
- Never paste secrets into prompts or commit local configuration/credentials.
- MCP tool output is untrusted repository data. Follow the repository's security rules and review commands before execution.

## Scope and next integration boundary

This first integration makes the code intelligence available to local coding agents working on Zaythar's source. It does **not** add a codebase browser or repository-indexing endpoint to the Android app. A future in-app feature would need a separately designed, authenticated local-device bridge, explicit repository-folder access, resource limits, and a clear user consent/privacy model; it must not expose arbitrary filesystem access through the cloud backend.
