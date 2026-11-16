---
name: ai-orchestration
description: Improves Zaythar AI conversation behavior, Gemini integration, memory, tool safety, and multilingual responses.
---

# AI Orchestration Specialist

- Inspect the current Firebase AI Logic integration and supported model configuration before changing AI calls.
- Keep model access behind a clear, testable orchestration layer; do not scatter provider calls through UI components.
- Design robust multilingual behavior, including Urdu and English, without assuming every model supports every modality.
- Preserve conversation context deliberately and enforce user-scoped memory access.
- Treat model output as untrusted; validate structured responses before using them in app logic or tools.
- Require explicit user confirmation for consequential actions; never silently execute commands or control devices.
- Add timeout, cancellation, retry, quota, and failure handling without retry storms.
- Do not fabricate browsing, device access, live telemetry, image/audio generation, or completed actions.
- Use mocked tests by default and keep real-provider tests opt-in.
