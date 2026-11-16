# JARVIS capability and execution model

This document defines the next safe evolution of Zaythar AI into a JARVIS-style assistant. It is an implementation plan, not a claim that every capability is already shipped.

## Current foundations
- Native Android application built with Kotlin and Jetpack Compose.
- Firebase Authentication and UID-scoped Firestore repositories.
- Gemini access through Firebase AI Logic (no client-side Gemini API key).
- Streaming chat, specialist-agent workflows, persona preferences, and memory context.
- Android speech recognition and TextToSpeech integration.
- A bounded local calculator and UTC-time tool.

A source-code capability is not proof of successful Firebase configuration, APK build, or real-device behaviour. See [Project Status](PROJECT_STATUS.md).

## JARVIS interaction loop
1. **Listen:** accept explicit tap-to-talk input first; show microphone state and request runtime permission only when needed.
2. **Understand:** normalize language, detect intent, identify missing details, and classify risk.
3. **Plan:** select a model route or deterministic local tool; keep tool arguments typed and bounded.
4. **Authorize:** classify each action as read-only, reversible, or consequential. Ask for confirmation before sending messages, changing settings, deleting data, purchasing, or controlling connected devices.
5. **Execute:** invoke only registered tools through narrow interfaces. Never execute model-generated shell commands, arbitrary URLs, scripts, or unrestricted intents.
6. **Verify:** inspect the tool result and report success only when the platform confirms it.
7. **Remember:** persist only useful user-approved facts under the authenticated Firebase UID; provide inspect, edit, and delete controls.

## Capability tiers
| Tier | Examples | Default policy |
|---|---|---|
| Conversational | multilingual chat, summaries, explanations | available after sign-in and provider setup |
| Local deterministic tools | arithmetic, date/time, app status | bounded input; no model-generated code execution |
| Read-only device context | battery, network, app permissions | disclose data source; request Android permissions only when required |
| User-confirmed actions | open an app/settings page, create a reminder | preview action and require user approval |
| High-impact actions | send communications, delete files, payments, unlock/control devices | explicit per-action confirmation; never autonomous |

## Architecture target
- **Assistant UI:** conversation, voice controls, action previews, permission explanations.
- **ConversationOrchestrator:** intent classification, model routing, context assembly, bounded retry.
- **ToolRegistry:** typed tool definitions, schema validation, timeouts, cancellation, audit events.
- **PermissionBroker:** maps each tool to Android permissions and confirmation requirements.
- **DeviceTools:** small allowlisted Android adapters; no generic shell or unrestricted intent execution.
- **MemoryRepository:** account-scoped persistence, retention limits, and user-controlled CRUD.
- **ProviderGateway:** Firebase AI Logic now; any future server provider must keep credentials server-side.

## Voice and privacy requirements
- Ship push-to-talk before considering an always-listening wake word.
- Do not keep the microphone active in the background by default.
- If a future foreground listening service is added, show a persistent notification, provide a visible stop control, and explain battery/privacy impact.
- Handle missing speech services, denied permissions, silence, network loss, and language mismatch without blocking text chat.
- Do not upload raw audio unless the user has been clearly informed and the feature requires it.

## Reliability and security requirements
- Every tool has a stable name, typed arguments, maximum input size, timeout, and explicit result type.
- Treat model output, chat history, memory, documents, and retrieved web content as untrusted data, never as executable instructions.
- Enforce Firebase UID ownership in Firestore rules and repository methods; never accept a user ID from model-generated arguments as an authorization decision.
- Apply request limits, cancellation, bounded retries with backoff, and safe error messages.
- Redact tokens, API keys, personal content, and raw prompts from logs.
- Add unit tests for routing and policy decisions, emulator tests for Firestore ownership, and Android instrumentation tests for permissions and lifecycle.
- Use CI to build debug and release variants; do not claim device validation from CI alone.

## Delivery sequence
1. **Foundation:** finish and merge the existing authentication, per-user memory, agent-profile, and Android build PRs only after review and green required checks.
2. **Voice reliability:** test speech recognition/TTS lifecycle, permission denial, language selection, interruption, and fallback to text.
3. **Typed tool registry:** migrate local calculator/time utilities behind a tested registry and add safe read-only device tools.
4. **Action approval:** add preview/confirm/cancel UI and an audit trail before introducing reversible actions.
5. **Memory controls:** add user-facing review, correction, retention, and deletion flows with Firestore rule tests.
6. **Cross-platform expansion:** design a separately permissioned Windows companion; do not imply Android code grants Windows access.

## Definition of done
A capability is complete only when implementation, tests, permission UX, security review, documentation, and CI are present. Label roadmap items as planned until then. Never represent decorative visualizer metrics as live telemetry or a prompt as an executed action.