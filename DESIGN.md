# Zaythar AI Design System

## Visual direction

A dark graphite command interface with restrained neon accents, glass-like panels, readable typography, and a cinematic neural-core orb. The active app is native Android using Jetpack Compose and Material 3; this document does not describe a shipped browser/CSS application.

## Neural Core

- The orb is an interactive Canvas-based visualizer, not a physical 3D simulation or proof of live hardware telemetry.
- Activity states should reflect actual app state: Ready, Listening, Thinking, and Speaking.
- Motion must respect the app motion preference and Android's system animation scale.
- Status must remain understandable without colour alone.

## Layout and accessibility

- Support compact phone layouts and expanded tablet/foldable layouts.
- Use semantic labels for interactive controls and meaningful touch targets.
- Keep text readable at larger font scales.
- Provide text interaction when speech recognition is unavailable or permission is denied.
- Avoid decorative animation that blocks navigation or reading.

## Honesty

Never label simulated telemetry as real device monitoring. Do not imply the app is always listening, conscious, or controlling hardware unless a separately permissioned feature actually exists.