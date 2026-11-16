# Agent and Contributor Instructions

## Before changing code
1. Read `README.md`, `ARCHITECTURE.md`, `SECURITY.md`, and the relevant API/UI files.
2. Confirm whether a feature is implemented or only represented visually.
3. Keep changes small, reviewable, and compatible with mobile browsers.

## Rules
- Never put secrets in frontend code, commits, screenshots, tests, or logs.
- Keep provider calls on the server.
- Validate all request fields and bound input sizes.
- Do not add autonomous actions, shell execution, or device access without explicit user permission and safeguards.
- Preserve accessibility, keyboard operation, and reduced-motion support.
- Add or update tests and documentation with behaviour changes.
- Do not invent success states or fake live telemetry.

## Completion checklist
Run available tests, inspect the diff, update docs, and report known limitations. If tests cannot be run, say so.
