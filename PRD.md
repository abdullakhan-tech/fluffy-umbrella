# Product Requirements Document

## Product
Zaythar AI — a JARVIS/FRIDAY-inspired multilingual personal assistant for web, mobile browsers, and future desktop packaging.

## Current product
A responsive cybernetic command-centre UI with chat, voice input/output where supported, persona settings, local notes/history, specialist prompts, and a FastAPI/OpenAI backend.

## Goals
- Make multilingual AI assistance approachable on desktop and mobile.
- Provide a distinctive, accessible holographic interface.
- Keep secrets and provider calls on the backend.
- Clearly distinguish working features from visual concepts.

## Current non-goals
No verified user accounts, subscriptions/payments, independent autonomous agents, real-time OS telemetry, device control, image/video generation, or cross-device memory.

## Requirements
- Chat: submit bounded messages and display provider responses/errors.
- Language/persona: send preferences with each request.
- Voice: feature-detect browser speech APIs and retain text input fallback.
- Preferences: persist locally and provide reduced-motion support.
- Security: keep API keys server-side and never log secrets.

## Acceptance criteria
- Main UI is usable on narrow mobile screens and desktop.
- Missing API key produces a clear configuration error.
- Health endpoint does not reveal secrets.
- Unsupported voice features do not block text chat.
- Visual telemetry is labelled illustrative unless connected to a real source.
- Documentation matches actual shipped behaviour.
