# Zaythar AI — UI Artifacts & Feature Reference

This folder is the canonical index for the supplied Zaythar AI UI references and the original implementation already maintained in this repository. It is documentation plus source-code pointers, not a second or duplicate application.

## Reference material

The supplied reference set describes:
- A desktop workspace with a left navigation rail, central chat canvas, provider/model selector, composer, tool shortcuts, voice panel and a right-side Settings surface.
- A responsive mobile experience that collapses navigation and presents chat, composer, tools and settings in a narrow single-column layout.
- A multi-screen overview covering Chat, Appearance, Voice Chat, model selection, Tools, Projects, GPTs/personas, Search, Library, Canvas and Code Interpreter.

The first two uploaded desktop captures are visually identical. The third is a multi-panel overview. The images are design references; their labels are not proof that every named external model or service is integrated.

## Visual system

| Token | Reference direction |
|---|---|
| Base | Near-black / graphite backgrounds |
| Panels | Layered charcoal surfaces with subtle gradients |
| Primary accent | Crimson / Zaythar red |
| Supporting text | Silver and muted gray |
| Main text | High-contrast white |
| Surfaces | Rounded cards, thin metallic borders, restrained red glow |
| Interaction | Clear selected, hover, disabled and focus states |

Use the existing design tokens and theme implementation rather than hardcoding a second palette.

## Screen-by-screen overview

### 1. Main Chat
- Zaythar brand mark and greeting.
- New Chat and recent conversation navigation.
- Prompt composer with attachment, voice and send actions.
- Quick actions for ideas, image studio, code, research, projects and Canvas.
- Conversation message history and assistant response presentation.

### 2. Navigation
- Chat / New Chat
- Search
- Library
- Projects
- GPTs or personas
- Voice Chat
- Image Studio / Image Generator
- Canvas
- Code Interpreter
- More Tools
- Recent chats and profile/settings entry

Desktop uses a persistent sidebar. Mobile uses a collapsible navigation surface.

### 3. Model and provider selector
- Provider/model selection is driven by the configured backend catalog.
- The chosen provider is passed with chat requests.
- Only routes returned by the backend should be shown as available.
- GPT-6, Claude, Gemini, Grok or other names in concept art are illustrative unless a matching provider route is configured and verified. Never present a mock option as a connected model.

### 4. Settings
The reference settings surface groups:
- General
- Personalization
- Appearance
- Speech and Voice
- Notifications
- Data Controls
- Security
- Account
- About Zaythar AI

Appearance includes System, Light and Dark modes, color accents and compact/visual preferences. General chat preferences include auto-save, code display and compact layout. Every setting must be backed by persisted state and a real behavior; otherwise it must be identified as planned or unavailable.

### 5. Voice Chat
- Voice entry point and listening state.
- Microphone permission and clear start/stop controls.
- Visible transcript and send-to-chat path.
- Speech input/output depends on supported device/browser APIs and configured speech services. Do not claim always-on or realtime voice without a tested provider.

### 6. Tools
Reference tool categories:
- Web Search / Research
- Image Generation
- Python / Code
- File Upload
- Data Analysis
- Canvas

Each tool must report its integration state. Code review is not the same as arbitrary Python execution; a local Canvas draft is not cloud collaboration; a search prompt is not proof of a live web-search connector.

### 7. Projects and GPTs/personas
- Projects group related work and chats.
- Personas select a conversational style.
- Marketplace or cloud synchronization requires a real configured service.
- Local browser storage must be labelled as local-only.

### 8. Search and Library
- Search across available chats and saved material.
- Library stores notes or references supported by the current implementation.
- Do not imply cloud sync or cross-device availability unless it is implemented and tested.

### 9. Canvas and Code Interpreter
- Canvas is an editable drafting surface.
- Code workspace can support code drafting/review.
- Execution requires an isolated, secured execution backend; never execute untrusted code in the application server process.

## Original source map

| Surface | Existing source |
|---|---|
| Web workspace shell | `website/app.html` |
| Web styles and responsive breakpoints | `website/app.css` |
| Web interactions and views | `website/app.js` |
| Web landing page | `website/index.html`, `website/styles.css`, `website/script.js` |
| Android chat | `app/src/main/java/com/example/ui/screens/AssistantScreen.kt` |
| Android settings | `app/src/main/java/com/example/ui/screens/SettingsScreen.kt` |
| Android theme and accent palette | `app/src/main/java/com/example/ui/theme/Theme.kt` |
| Android message/markdown presentation | `app/src/main/java/com/example/ui/screens/AiMarkdownContent.kt` and related chat components |
| Existing design reference | `design/REFERENCE_UI_SPEC.md` |

## Current integration boundaries (do not overstate)

The current web reference specification records these boundaries:
- Chat requests use `/api/v1/chat`.
- Agent/code review uses `/api/v1/agent/task`; this is not an arbitrary Python sandbox.
- Browser voice uses Web Speech API where supported.
- Browser chats, notes, projects and Canvas drafts are localStorage data, not Firestore-synced.
- Image generation and dedicated web search are not exposed as web endpoints in the currently documented backend.
- The model catalog is limited to configured OpenAI, Gemini and Ollama routes. Actual model availability depends on backend configuration and credentials.

Re-check implementation and backend configuration before changing any status in this document.

## Responsive behavior

### Desktop / web
- Persistent left navigation.
- Central workspace with top bar and model/provider selector.
- Chat composer and contextual tool shortcuts.
- Settings shown as a dedicated page/panel.
- Wide layouts may expose secondary panels without shrinking the chat canvas excessively.

### Mobile / Android
- Navigation collapses into a drawer or compact navigation.
- Chat and composer occupy the available width.
- Tool shortcuts wrap or stack.
- Settings become vertically scrollable sections with touch-sized controls.
- Avoid horizontal overflow; preserve readable text and keyboard-safe composer behavior.

## Quality and acceptance checklist

- [ ] All reference screens are mapped to an existing source file or explicitly labelled as a future screen.
- [ ] No duplicate application or fake model/provider entries are introduced.
- [ ] Desktop and mobile breakpoints are tested at real viewport sizes.
- [ ] Every visible button has a real action or a clear disabled/unavailable state.
- [ ] Provider selection is reflected in the outgoing request.
- [ ] Settings persist and affect the UI.
- [ ] Voice permissions, start/stop and transcript behavior are tested on supported devices.
- [ ] Backend-dependent features are tested against configured services.
- [ ] Android unit/UI tests, lint and debug/release builds pass.
- [ ] Web smoke tests and backend integration tests pass.

## How to use these artifacts

1. Start with this index.
2. Read `../REFERENCE_UI_SPEC.md` for design intent and acceptance criteria.
3. Open the original source paths listed above for implementation.
4. Make changes in those original source files, not copied mock implementations.
5. Update this document only when implementation or verified integration status changes.
