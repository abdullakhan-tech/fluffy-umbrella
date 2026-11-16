# Project Rules

1. Truth over appearance: never present mock telemetry, decorative agent nodes, or visual effects as live infrastructure.
2. Protect secrets: no API keys, tokens, passwords, signing keys, or private credentials in source control.
3. Provider boundary: use Firebase AI Logic for the current Android Gemini path; any future direct server provider calls keep credentials server-side.
4. Least privilege: future tools require scoped permissions, validation, and user confirmation for consequential actions.
5. Input validation: enforce types, lengths, allowed values, timeouts, and cancellation at every tool boundary.
6. Account isolation: enforce Firebase UID ownership in both Firestore repository methods and security rules.
7. Accessibility: semantic controls, visible state, readable text, touch targets, and reduced-motion support.
8. Mobile-first: test compact Android layouts and expanded tablet layouts.
9. Reliability: isolate specialist failures, bound retries, and return sanitized errors.
10. Verify claims: update README, tests, and documentation whenever functionality changes.