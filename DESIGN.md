# Design System

## UI Design Artifacts (Reference Files — Kept Without Duplication)
The following uploaded visual UI artifacts define Zaythar AI's obsidian-crimson glassmorphic suite and are preserved directly at their original repository paths without extra copies:
- `public/assets/aistudio/file_00000000f718821188e48dc2073c5ccf_57cc39c1.jpg` (Sidebar + Welcome/Chat + Model Dropdown + General/Appearance Settings + Voice Chat bottom pill)
- `public/assets/aistudio/file_000000001ce8821197b271341d7b38e6_dc6e9588.png` (Unified Multi-Screen Suite: Search Hub, Library, Visual Appearance Cards & Swatches, Voice Chat Orb, Select AI Model Sheet, 2x3 Tools Grid, Projects, Agents/GPTs, Canvas/Brain, Code Interpreter)

## Visual Direction
Obsidian-graphite command center (`ZaytharTheme`) with `#06070B` canvas, glassmorphic elevated panels (`#0D0F15` / `#141821`), `#FF2647` crimson primary glow + 7 circular accent swatches, subtle grid lines, Space Grotesk + Rajdhani typography, 7-blade crimson spiral emblem (`ZaytharSpiralEmblem`), visual Light/Dark/System preview cards, and glowing Voice Chat orb. Supports Dark, Light, and System Material 3 themes.

## Motion
- `NeuralCoreVisualizer` uses Compose animations for decorative interface polish.
- Respects the persistent user **3D FX / Motion** toggle in DataStore (`motion3dEnabled`) and falls back cleanly to a static render when disabled.

## Responsive Design
Uses Material 3 adaptive layouts (`NavigationBar` + `ModalNavigationDrawer` on compact phones, persistent `CyberSidebar` on expanded screens) and `BoxWithConstraints` for multi-column cards and quick-action grids.

## Accessibility
- All interactive controls meet minimum `48.dp` touch target requirements (`minimumInteractiveComponentSize()`).
- Icons and images provide descriptive `contentDescription` labels (or `null` when purely decorative).
- Primary interactive controls expose `Modifier.testTag(...)` identifiers for UI testing.

## Honesty
Status indicators report real Firebase initialization, Google Search/Maps grounding toggles, plugin count, active Firebase AI Logic routes (`ProviderRoute`), and session uptime. Only genuine Gemini/Zaythar backend models and features are displayed or selectable. Informational capability cards on the Profile screen explicitly state that no paid billing or subscription entitlement is activated.
