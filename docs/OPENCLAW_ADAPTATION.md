# OpenClaw-inspired architecture adaptation

This document records a compatibility-first adaptation plan for Zaythar AI. The reference project is [OpenClaw](https://github.com/openclaw/openclaw).

## Licensing and source provenance

OpenClaw's repository declares the MIT License. MIT permits reuse and modification when its copyright and permission notice are retained. OpenClaw's `THIRD_PARTY_NOTICES.md` separately identifies incorporated or adapted third-party components with their own notices.

This Zaythar change is an independent Kotlin implementation of a small tool-runtime boundary. It does not copy OpenClaw source files, bundled assets, prompts, documentation, or third-party implementation code. The OpenClaw project is cited as an architectural reference only. No upstream copyright notice is removed or represented as belonging to Zaythar.

If source code is ever reused in a later change, review the exact file and its applicable license/third-party notices first, and include every required notice. A repository-level MIT license does not automatically relicense dependencies or third-party assets.

## Compatibility decision

OpenClaw is a cross-platform Node.js/TypeScript assistant with a gateway, channels, tools, skills, and companion-device integrations. Zaythar's current product is a native Kotlin/Jetpack Compose Android app using Firebase Auth, Firestore, Firebase AI Logic, and Android platform APIs.

Copying OpenClaw's workspace or server folders into this Android repository would introduce a second runtime and unrelated build dependencies. Instead, Zaythar keeps its existing application architecture and adopts only compatible design ideas.

| OpenClaw concept | Zaythar adaptation |
| --- | --- |
| Gateway as a control plane | Keep orchestration inside the Android domain layer; add a remote gateway only as a separately designed service |
| Tool allowlist and policy boundary | Original Kotlin `ToolRegistry`, typed argument schema, permission checks, approval gate, timeout, and cancellation |
| Skills and extensions | Continue using Zaythar's existing declarative Plugin Plug Hub; do not enable dynamic code loading |
| Sessions and memory | Preserve authenticated Firebase UID ownership and existing session-scoped Firestore paths |
| Device nodes and channels | Add narrow Android adapters only when there is a concrete permission and lifecycle design |

## What this change adds

- A stable tool descriptor with typed text, number, and boolean parameters.
- Strict allowlisting: only adapters registered by application code can execute.
- Argument validation, size bounds, permission checks, and bounded execution time.
- Explicit approval for reversible and high-impact actions.
- Cancellation propagation so leaving a screen or cancelling a task stops the tool.
- Audit events that omit prompts, raw arguments, and tool output.

## Current implementation boundary

The registry is a tested domain-layer foundation. It is not yet connected to the chat model loop, Plugin Hub UI, Android permission launcher, or Firestore audit persistence. Existing calculator, clock, and other local tools are not silently replaced by this change. Do not advertise arbitrary device control, shell access, background gateway service, messaging-channel support, or OpenClaw feature parity.

## Verification

Run the existing Android unit tests and build:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The new tests cover read-only execution, approval gating, Android permission gating, argument validation, unauthenticated contexts, duplicate registration, and timeout handling. A green CI run verifies compilation and unit tests; real Android permission and lifecycle behaviour still requires device testing.

## Next integration steps

1. Adapt existing deterministic local tools to `ZaytharTool` interfaces.
2. Build an action-preview screen that creates an approval for one exact request ID.
3. Supply the Firebase UID, active session ID, and current Android permission state from trusted app code.
4. Persist only redacted audit metadata under the signed-in user's Firestore scope if the user-facing audit feature is approved.
5. Add emulator and device tests before enabling consequential actions.
