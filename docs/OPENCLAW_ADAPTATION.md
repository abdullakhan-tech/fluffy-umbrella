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

The Kotlin registry is connected to the authenticated chat path and the Plugin Hub sandbox. Existing calculator, UTC clock, unit conversion, and JSON inspection commands run through one first-party, read-only adapter. The enabled plugin keys are supplied by the authenticated app session; model output cannot register adapters, grant permissions, or approve actions. The Plugin Hub displays the registered tool descriptor and uses the same registry as chat.

The runtime validates typed arguments and bounds, checks the signed-in user and session context, enforces timeouts/cancellation, and emits redacted audit metadata to Android logcat. It does not log prompts, raw arguments, or tool output. The approval gate is implemented for future non-read-only adapters, but no mutating/device-control adapter is currently registered, so there is no misleading claim of device control. Custom plugins remain declarative system instructions and never execute uploaded code.

This is not OpenClaw feature parity. Zaythar does not import OpenClaw's Node.js gateway, shell execution, messaging channels, dynamic plugin loading, or background daemon. Any future capability must be implemented as a narrow first-party adapter with its own permission, lifecycle, and approval design.

## Verification

Run the existing Android unit tests and build:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The tests cover read-only command execution through the registry, plugin enable/disable enforcement, no-match fall-through, authenticated user/session validation, exact-call approval binding, mutable-argument protection, permission denial, argument validation, duplicate registration, timeout handling, and safe failure output. CI runs the Firestore emulator rules tests, Android/Kotlin unit tests, and debug APK assembly.

No mutating or device-control adapter is enabled. If Zaythar later adds one, it must first receive a dedicated Android permission/lifecycle design, an action-preview UI that binds approval to the exact invocation, redacted audit persistence requirements, and device-level regression tests. Those are future product capabilities, not unfinished work in the current read-only runtime.
