# Design System

## Visual direction
Dark graphite command centre (`ZaytharTheme`) with glassmorphic elevated panels, configurable neon accent swatches, subtle grid lines, Space Grotesk + Rajdhani typography, and an animated Compose `Canvas` Neural Core visualizer. Supports both dark and light Material 3 themes.

## Motion
- `NeuralCoreVisualizer` uses Compose animations for decorative interface polish.
- Respects the persistent user **3D FX / Motion** toggle in DataStore (`motion3dEnabled`) and falls back cleanly to a static render when disabled.

## Responsive design
Uses Material 3 adaptive layouts (`NavigationBar` on compact phones, `NavigationRail` / persistent `NavigationDrawer` on medium/expanded screens) and `BoxWithConstraints` for multi-column cards.

## Accessibility
- All interactive controls meet minimum `48.dp` touch target requirements (`minimumInteractiveComponentSize()`).
- Icons and images provide descriptive `contentDescription` labels (or `null` when purely decorative).
- Primary interactive controls expose `Modifier.testTag(...)` identifiers for UI testing.

## Honesty
Status indicators report real Firebase initialization, internet grounding toggle, plugin count, and session uptime. Informational capability cards on the Profile screen explicitly state that no paid billing or subscription entitlement is activated.
