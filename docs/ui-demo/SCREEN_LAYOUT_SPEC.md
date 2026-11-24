# Zaythar AI UI Demo — Layout Specification

**Classification:** Non-production demo specification based on the two UI reference images supplied in the conversation.

## Desktop shell

- Three-part composition: left navigation, central conversation canvas, and optional right-side settings/model overlays.
- Navigation rail: fixed-width on desktop; collapsible to icon rail or drawer at smaller breakpoints.
- Central canvas: flexible width with a constrained conversation column; header remains at the top, composer remains at the bottom.
- Overlays: model selector and settings are independent dismissible panels. On mobile they become full-height sheets or dedicated routes rather than narrow floating columns.

## Chat home

- Header: product mark/name, model dropdown, search, tools/apps, notifications and profile.
- Empty state: large brand mark, greeting, short helper text and a small set of task starter cards.
- Composer: multiline message field with attachment, tools, image, voice and send actions.
- Tool shortcuts: Web Search, Image, Code/Python, Canvas, Analyze Data and More Tools.
- Voice panel: orb/visualizer, listening state, microphone control and close/end action.

## Settings information architecture

| Section | Demo controls / content |
|---|---|
| General | Language, theme, selected model, auto-save chats, show code by default, compact mode, reset and save |
| Personalization | User preferences and assistant behavior |
| Appearance | System/Light/Dark previews, accent colors, accent style and apply-to-all-devices |
| Speech | Speech output preferences |
| Voice | Voice selection and voice interaction preferences |
| Notifications | Notification preferences |
| Data Controls | Data and chat-history controls |
| Security | Security-related settings |
| Account | Account/profile information |
| About | Product information |

## Secondary surfaces

- **Voice Chat:** listening/idle/error states, microphone permission, live waveform and call termination.
- **Model selection:** provider/model label, availability state, selected indicator, model details and comparison entry.
- **Tools:** searchable or categorized tool launcher with explicit availability state.
- **Projects:** project creation, list, recent activity and per-project actions.
- **GPT discovery:** category filters, featured assistant cards and browse action.
- **Search:** scoped filters for chats, files, projects and GPTs; recent search results.
- **Library:** saved items, custom instructions, memory, archives and deleted-chat access.
- **Canvas:** editing workspace and open/create action.
- **Code Interpreter:** Python execution entry point, output area and visualization preview.

## Responsive and interaction requirements

- At mobile widths, replace the permanent sidebar with a drawer and keep the active conversation visible.
- Use full-width sheets for model selection and settings; avoid overlapping panels that obscure the composer.
- Keep the composer above the on-screen keyboard and device safe areas.
- Use accessible names for icon-only controls, visible keyboard focus and logical tab order.
- Every toggle must show its saved state and have a real effect; every navigation item must route to a working screen.
- Provide loading, empty, success, permission-denied, offline and provider-error states where applicable.

## Non-goals / honest status

This file describes a proposed demo UI only. It does not implement components, backend services, model integrations or persistence. The labels GPT-6 Advanced, Claude 3.5 Sonnet, Gemini 2.5 Pro, Llama 3.3 70B and Grok 3 in the reference are sample presentation content and must not be presented as connected providers unless configuration and routing are verified.
