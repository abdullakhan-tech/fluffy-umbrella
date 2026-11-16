# Project Rules

1. Truth over appearance: never present mock telemetry, subscription tiers, agent nodes, or visual effects as live infrastructure.
2. Protect secrets: no API keys, tokens, passwords, or private credentials in source control.
3. Backend boundary: external AI provider calls must remain server-side.
4. Least privilege: future tools require scoped permissions, validation, and user confirmation for consequential actions.
5. Input validation: enforce types, lengths, and allowed values on every API boundary.
6. Accessibility: semantic controls, visible focus, labels, keyboard support, and reduced-motion mode.
7. Mobile-first: test responsive layouts and touch targets.
8. Maintainability: avoid unnecessary dependencies and document new configuration.
9. Reliability: handle provider errors without leaking internal details.
10. Verify claims: update README and tests whenever functionality changes.
