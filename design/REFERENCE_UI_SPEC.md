# Zaythar AI reference UI specification

This document records the supplied Zaythar AI interface references and maps their visual elements to repository features. The two first uploads are duplicate captures; the third is a multi-screen panel overview.

## Visual direction
- Near-black graphite surfaces, restrained crimson highlights, metallic silver borders and readable white text.
- Dark, Light and System appearance modes; accent choices must remain distinguishable in both modes.
- Desktop: persistent left navigation, central chat workspace, compact provider selector and settings surface.
- Mobile: collapsible navigation, full-width chat composer, stacked tool cards and accessible settings controls.
- Use Zaythar AI branding; do not represent third-party model names as available unless the backend confirms their provider route is configured.

## Screens represented in the references
1. Main chat: New Chat, Search, Library, Projects, GPTs/personas, Voice Chat, Image Generator, Canvas, Code Interpreter, More Tools, recent chats and profile/settings.
2. Provider/model selector: show only supported provider routes and indicate whether the backend has credentials/configuration. Current backend routes are OpenAI, Gemini and Ollama; model names/defaults come from the backend catalog.
3. Settings: General, Personalization, Appearance, Speech/Voice, Notifications, Data Controls, Security, Account and About. Persisted controls must have a real effect and disclose local-only storage where applicable.
4. Voice panel: browser/device speech capture where supported; do not imply realtime synthesized conversation unless a speech provider is connected.
5. Tools: Web Search, Image Generation, Python, File Upload, Data Analysis and Canvas. Each card must disclose whether its service is connected.
6. Projects, GPT/personas, Search and Library: current browser workspace stores these locally unless a cloud sync service is explicitly configured.
7. Canvas and Code Interpreter: Canvas is a local editable draft; current web Code Interpreter supports code review via the agent endpoint, not arbitrary remote execution.

## Current verified integration boundaries
- Web chat posts to `/api/v1/chat`.
- Code review posts to `/api/v1/agent/task`; this is not a Python execution sandbox.
- Browser voice uses the Web Speech API when the browser supports it.
- Browser chat, notes, projects and Canvas drafts are localStorage data; they are not Firestore-synced.
- Image generation and dedicated web search are not exposed as web endpoints in the current backend.
- The provider catalog endpoint `GET /api/v1/models` exposes only configured backend-supported routes (OpenAI, Gemini, Ollama). It does not claim GPT-6, Claude, Grok or other unintegrated model services.

## Acceptance checklist for full parity
- [ ] Desktop layout visually reviewed at common laptop and widescreen breakpoints.
- [ ] Mobile layout visually reviewed on a real Android browser and native Android app.
- [ ] All visible controls have an implemented action, or are clearly marked unavailable.
- [ ] Provider selection changes the provider sent with chat requests.
- [ ] Settings persist and affect the active workspace.
- [ ] Authenticated cloud chat/history sync tested with Firebase.
- [ ] Web search, image generation and sandboxed Python execution have real backend integrations.
- [ ] Voice input and speech output tested on supported physical devices.
- [ ] Android debug/release builds and UI tests pass.
- [ ] End-to-end tests cover web, backend and Android flows.


## Artifact package

See [`ui-artifacts/README.md`](ui-artifacts/README.md) for the canonical source map and [`ui-artifacts/UI_ARTIFACTS_OVERVIEW.md`](ui-artifacts/UI_ARTIFACTS_OVERVIEW.md) for the detailed desktop/mobile screen inventory, feature descriptions, responsive behavior, integration boundaries and acceptance checklist. The package references the original web and Android implementation files; it intentionally does not introduce duplicate UI implementations.
