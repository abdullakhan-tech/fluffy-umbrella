# GitHub Copilot Instructions

Follow `AGENTS.md`, `RULES.md`, `SECURITY.md`, and `ARCHITECTURE.md`.

- Preserve frontend/backend separation; provider keys belong only on the server.
- Do not invent working integrations for UI-only modules.
- Keep the interface responsive, accessible, and reduced-motion aware.
- Validate API inputs and avoid leaking secrets or prompts in errors/logs.
- Update tests and docs for behaviour changes.
- Specialist modes are prompt profiles, not autonomous agents.
