# Zaythar AI — UI Demo Reference

> Status: **DEMO / DESIGN REFERENCE ONLY**. This document records the two user-supplied UI concepts. It does not claim that the production UI has been implemented or that any feature shown here is working.
>
> **Image note:** The SVGs below are clearly labeled illustrative placeholders created for repository preview. They are NOT the original screenshots supplied in chat.

## Demo image previews

### Desktop settings concept

![Illustrative Zaythar AI desktop settings mockup](images/zaythar-ai-desktop-settings.svg)

### Responsive screens concept

![Illustrative Zaythar AI responsive screens mockup](images/zaythar-ai-responsive-screens.svg)

## Reference A — Desktop workspace with settings and model picker

The first supplied concept combines three visible regions:

- **Left navigation rail:** Zaythar AI branding, New Chat, Search, Library, Projects, GPTs, Explore GPTs, Voice Chat, Image Generator, Canvas, Code Interpreter and More Tools. Recent conversations appear below, with the account/profile control at the bottom.
- **Main chat workspace:** top bar with selected assistant/model and quick actions; centered Zaythar AI greeting; prompt starter cards for ideas, image creation, Python/code and web search; message composer with attachment/tool/voice/send controls; quick tool chips; and a bottom voice-chat panel.
- **Model picker:** a floating panel listing GPT-6 Advanced, Claude 3.5 Sonnet, Gemini 2.5 Pro, Llama 3.3 70B and Grok 3, plus a More Models action.
- **Settings panel:** account header and categories for General, Personalization, Appearance, Speech, Voice, Notifications, Data Controls, Security, Account and About. The visible Appearance content includes System/Light/Dark/Auto theme choices, accent-color swatches, and chat toggles for auto-save, show-code-by-default and compact mode.

## Reference B — Multi-screen responsive UI inventory

The second supplied concept is a contact sheet of related desktop panels and tool surfaces:

1. **Primary chat:** centered Zaythar AI mark and welcome copy, quick actions, composer and model selector.
2. **Settings / General:** language, theme, model, auto-save, show-code-by-default, compact mode, reset and save actions.
3. **Appearance:** Light/Dark/System previews, color theme, accent style and apply-to-all-devices control.
4. **Voice Chat:** orb-style listening state, waveform, microphone and end-call controls.
5. **Select AI Model:** model list and compare-models action.
6. **Tools:** Web Search, Image Generation, Python, File Upload, Data Analysis and Canvas, plus Browse More GPTs.
7. **Projects:** project list, recent activity and New Project action.
8. **GPTs / Explore GPTs:** category filters, featured assistants and discovery action.
9. **Search:** All, Chats, Files, Projects and GPTs filters with a search field and recent results.
10. **Library:** Chats, Saved Items, Custom Instructions, Memory, Archives and Deleted Chats.
11. **Canvas:** create/edit workspace with a launch action.
12. **Code Interpreter:** Python execution and data visualization entry point.

## Visual direction

- Near-black surfaces with high-contrast white text.
- Vivid red as the primary accent, including selected navigation, primary buttons, focus states and the Zaythar AI emblem.
- Rounded panels, restrained borders, soft shadows and subtle red ambient glow.
- Consistent iconography and compact spacing; preserve readable labels and visible interaction states.
- Desktop uses a persistent navigation rail and layered panels. Narrow screens should collapse navigation into a drawer, stack panels, and keep the composer and voice controls reachable without horizontal overflow.

## Implementation boundary

These are design references, not proof of implemented behavior. Model names are illustrative UI labels; provider availability, API credentials, model routing, speech recognition, speech synthesis, image generation, Python execution, search, persistence and account/security controls must be implemented and tested independently. Do not expose provider credentials in client code.

## Acceptance checklist

- [ ] Desktop workspace matches the reference hierarchy and red/black visual language.
- [ ] Navigation destinations open real screens and preserve navigation state.
- [ ] Model picker reflects only configured, available providers and reports failures.
- [ ] Settings controls persist and affect the corresponding application behavior.
- [ ] Voice UI reflects actual microphone permission and connection states.
- [ ] Search, Library, Projects, GPT discovery, Canvas and Python surfaces are connected to real functionality or clearly marked unavailable.
- [ ] Responsive layout works on mobile widths with keyboard and safe-area handling.
- [ ] Accessibility: keyboard navigation, focus visibility, labels, contrast and screen-reader semantics.
- [ ] UI tests and build/CI checks pass on supported targets.
