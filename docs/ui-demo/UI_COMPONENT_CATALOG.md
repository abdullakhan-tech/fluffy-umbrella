# Zaythar AI — Complete UI Demo Catalog

> **Demo documentation only.** This catalog describes the visual system, screens, controls, navigation, responsive behavior and implementation boundaries. A control shown in a mockup is not evidence that its backend action is connected. See each status note before presenting a feature as working.

## 1. Product shell and visual language

### Desktop composition
- **Left navigation rail:** brand mark and product name, New Chat, primary destinations, recent-chat list, user/profile area and Settings entry.
- **Top bar:** mobile-menu trigger when applicable, product identity, provider/model selector, current section label, connection status and Settings shortcut.
- **Main workspace:** route-specific content with a flexible central column.
- **Footer/status line:** assistant accuracy reminder and backend-configuration hint.
- **Overlays and sheets:** settings, model selection, tool selection and confirmation surfaces; these should not permanently obscure the composer.

### Visual tokens
- Base surfaces: near-black and graphite.
- Elevated surfaces: charcoal panels, subtle gradients and thin borders.
- Primary accent: crimson / Zaythar red.
- Text: high-contrast white, silver secondary text and muted gray metadata.
- Shape language: rounded cards, compact spacing, restrained glow and clear selected/focus states.
- Interaction states: default, hover, pressed, selected, disabled, loading, success and error.
- Accessibility: semantic labels, visible focus, logical keyboard order, adequate contrast and touch-sized controls.

## 2. Navigation map

| Destination | Purpose | Expected primary content |
|---|---|---|
| Chat / New Chat | Start or continue a conversation | Greeting, history, model selector and composer |
| Search | Find workspace content | Query field, scope filters and result list |
| Library | Browse saved workspace material | Saved items, notes and reference collections |
| Projects | Group related work | Project list, recent activity and create action |
| GPTs & Personas | Choose assistant configurations | Persona cards, categories and details |
| Voice Chat | Speak with the assistant | Microphone state, waveform, transcript and call controls |
| Image Studio | Prepare image-generation requests | Prompt, options, preview and generation status |
| Canvas | Draft and edit content | Editable document surface and save/export actions |
| Code Interpreter | Work with code and analysis | Code editor/review surface, output and visualization area |
| More Tools | Discover available utilities | Categorized tool launcher with availability labels |
| Settings | Configure the application | Settings categories and persisted preferences |

Recent chats and profile/settings controls remain available from the desktop navigation shell. On narrow screens, the rail becomes a drawer or compact navigation surface.

## 3. Chat workspace

### Empty state
- Product mark and welcome heading.
- Short helper text describing what the assistant can help with.
- Starter actions for ideas, image work, code/Python, research, projects and Canvas.
- Optional recent conversation shortcuts.

### Conversation state
- User and assistant message groups with readable spacing.
- Message metadata and clear loading/error feedback.
- Long-form response rendering, including code and markdown where supported.
- Conversation history and a way to start a new chat.

### Composer
- Multiline text input.
- Attachment/file action where supported.
- Tool/action selector.
- Image and voice entry points where available.
- Send action with disabled/loading states.
- Keyboard-safe placement on mobile.

### Model/provider selector
- Displays only providers returned by the configured backend catalog.
- Shows selected provider/model and availability.
- Selection must affect the provider/model sent with the chat request.
- Unconfigured providers must be marked unavailable or omitted; concept-art labels are not connected integrations.

## 4. Settings information architecture

| Category | Demo content / controls | Behavior required before claiming complete |
|---|---|---|
| General | Language, theme, selected model, autosave, code display, compact layout, reset/save | Persist values and apply them to the workspace |
| Personalization | User preferences and assistant behavior | Apply preferences to subsequent conversations |
| Appearance | System/Light/Dark, accent swatches, accent style, glow and contrast | Apply theme immediately and persist selection |
| Speech | Speech output preferences | Reflect actual supported speech settings |
| Voice | Voice choice and interaction preferences | Respect device/browser support and permissions |
| Notifications | Notification preferences | Connect to real notification delivery |
| Data Controls | Chat/history and data options | Explain and execute the actual storage behavior |
| Security | Security-related controls | Connect to real authentication/session controls |
| Account | Profile and account details | Reflect authenticated account state |
| About | Product information and version | Show current app/build metadata |

### Native Android settings currently represented in source
The Android Settings screen includes:
- **AI Cognitive Settings / Processing Parameters:** Processing velocity, Synaptic depth, Response precision and Creativity bias sliders.
- **Neural interface toggles:** Neural overdrive, Holographic interface and Auto cognition.
- **Theme & Visualizer:** System, Light and Dark segmented selection.
- **Visual tuning:** Secondary glow, Metallic specularity and Contrast sliders.
- **Accent color:** selectable accent swatches and selected-color label.
- **Telemetry bars:** Interface glow and Metallic contrast indicators.
- **Save action:** displays a local-device preferences confirmation.

Source: `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`. Preference persistence and each setting's downstream effect must be checked against the data layer before being described as fully integrated.

## 5. Secondary screen specifications

### Voice Chat
- Idle, requesting-permission, listening, processing, speaking, disconnected and error states.
- Microphone permission explanation and start/stop controls.
- Orb or waveform visualization with clear activity state.
- Transcript preview and route back to the conversation.
- Browser speech behavior depends on Web Speech API support; do not imply always-on or realtime voice without a tested service.

### Image Studio
- Prompt input, generation settings, submit action, progress and result/empty/error states.
- Show unavailable state if no image-generation backend is configured.
- Do not use a decorative preview as proof of generated output.

### Tools
- Web Search / Research, Image Generation, Python / Code, File Upload, Data Analysis and Canvas categories.
- Each tool card has a name, concise purpose, availability/integration status and launch action.
- Distinguish code drafting/review from executing arbitrary code.
- A search entry point is not proof of a live search provider.

### Projects
- Project list, create project, project details and recent activity.
- Empty, loading and error states.
- Clearly label local-only storage versus authenticated cloud sync.

### GPTs and Personas
- Persona/category filters, featured assistant cards, detail view and select action.
- Distinguish built-in personas from a remote marketplace.
- Do not imply marketplace connectivity unless a service is configured.

### Search
- Query field and scope filters for All, Chats, Files, Projects and GPTs.
- Recent queries, results, empty state and no-match state.
- Results must reflect the actual searchable data source.

### Library
- Saved items, notes/references, custom instructions, memory, archives and deleted-chat areas where implemented.
- Explain whether data is local, account-bound or cloud-synced.
- Provide empty, loading and error states.

### Canvas
- Editable drafting surface, document title, editing controls and save/export actions.
- Show persistence status.
- Current browser Canvas drafts are local unless a sync service is implemented.

### Code Interpreter
- Code drafting/review entry point, input/output presentation and visualization preview.
- Arbitrary Python execution requires a separately secured sandbox.
- The documented agent task endpoint performs code review/task handling; it is not a Python execution sandbox.

## 6. Responsive behavior

### Wide desktop
- Persistent navigation rail.
- Flexible central workspace and readable conversation width.
- Settings/model overlays may appear beside the workspace without compressing it excessively.

### Tablet / compact desktop
- Collapsible or icon-only navigation.
- Panels stack or become sheets.
- Tool shortcuts wrap without clipping.

### Mobile
- Navigation becomes a drawer or dedicated route.
- Single-column content and full-width settings/model sheets.
- Composer stays above the on-screen keyboard and device safe areas.
- Touch targets remain comfortably sized.
- No horizontal overflow; long labels and code blocks wrap or scroll within their own region.
- Voice controls remain reachable while the keyboard is open or closed.

## 7. Loading, empty and failure states

Every network-backed surface should define:
- Initial/loading state.
- Empty state with a useful next action.
- Success state with visible confirmation.
- Permission-denied state where device permissions are involved.
- Offline/network failure state with retry guidance.
- Provider-not-configured or unavailable state.
- Validation error state for invalid user input.

Never leave a control visually active if its action is unavailable. Prefer a clear disabled state and a short explanation.

## 8. Source-code map

| UI area | Repository source |
|---|---|
| Web workspace shell | `website/app.html` |
| Web styles and responsive breakpoints | `website/app.css` |
| Web navigation, views and interactions | `website/app.js` |
| Web landing page | `website/index.html`, `website/styles.css`, `website/script.js` |
| Android chat | `app/src/main/java/com/example/ui/screens/AssistantScreen.kt` |
| Android settings | `app/src/main/java/com/example/ui/screens/SettingsScreen.kt` |
| Android theme/accent palette | `app/src/main/java/com/example/ui/theme/Theme.kt` |
| Android markdown/message presentation | `app/src/main/java/com/example/ui/screens/AiMarkdownContent.kt` and related chat components |
| Design reference | `design/REFERENCE_UI_SPEC.md` |
| Feature/source index | `design/ui-artifacts/UI_ARTIFACTS_OVERVIEW.md` |

## 9. Known integration boundaries to preserve

Based on the repository's documented integration notes; re-check source and runtime configuration before changing these statuses:
- Web chat uses `/api/v1/chat`.
- Agent/code review uses `/api/v1/agent/task`; it is not an arbitrary Python execution service.
- Browser voice uses Web Speech API when supported.
- Browser chats, notes, projects and Canvas drafts are localStorage data unless cloud sync is added and verified.
- Image generation and dedicated web search are not exposed as web endpoints in the documented backend.
- The model catalog exposes configured OpenAI, Gemini and Ollama routes; actual availability depends on backend configuration and credentials.
- Do not place provider credentials in browser or Android client code.

## 10. Demo assets and status

The demo folder includes illustrative SVG previews:
- `images/zaythar-ai-desktop-settings.svg`
- `images/zaythar-ai-responsive-screens.svg`

These are explicitly illustrative demo assets, not original user screenshots and not screenshots captured from a running application. Keep that distinction visible in the README.

## 11. Acceptance checklist

- [ ] Every screen in the reference set appears in this catalog and is mapped to a source file or labelled as a demo/future surface.
- [ ] Desktop and mobile layouts are visually reviewed at real viewport sizes.
- [ ] Navigation items open the correct destinations.
- [ ] Every visible control performs its stated action or explains why it is unavailable.
- [ ] Provider selection changes the outgoing request and only configured providers appear.
- [ ] Settings persist and affect the relevant UI/behavior.
- [ ] Voice permission, listening, transcript and failure states are tested on supported devices.
- [ ] Search, Library and Projects accurately describe local/cloud storage.
- [ ] Image generation, web search and Python execution are not claimed without working backend integrations.
- [ ] Accessibility, keyboard navigation, focus, contrast and touch targets are checked.
- [ ] Web tests, Android UI tests, build and CI checks pass on supported targets.
