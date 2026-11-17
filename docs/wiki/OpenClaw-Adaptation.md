# OpenClaw-inspired adaptation

Zaythar uses OpenClaw as an architectural reference, not as a source-code bundle.

- [Adaptation and licensing notes](../OPENCLAW_ADAPTATION.md)
- [Architecture](Architecture.md)
- [AI and agents](AI-and-Agents.md)
- [JARVIS execution model](../JARVIS_EXECUTION_MODEL.md)

The new Kotlin tool registry is a domain-layer foundation with a strict adapter allowlist, typed arguments, Android permission checks, explicit approval for non-read-only actions, bounded execution time, cancellation, and redacted audit events.

It is not yet wired into the chat UI or existing plugin screen. The app remains a native Android project; this page does not claim OpenClaw feature parity.
