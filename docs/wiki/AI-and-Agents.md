# AI and agents

## Gemini integration

Zaythar Android uses Firebase AI Logic to access Gemini. Firebase configuration and provider enablement are required. A standalone Gemini API key should not be embedded in the Android client.

See [Gemini setup](../GEMINI_SETUP.md) for configuration details.

## Provider routing

The app exposes provider-route preferences and uses configured Gemini model choices. Model availability can change; verify supported model identifiers against the Firebase AI Logic SDK and provider documentation before changing them.

## Specialist workflows

The repository includes multi-agent specialist orchestration. Specialist roles produce bounded deliverables for a shared task. Treat specialist output as untrusted model content: validate it, do not interpret it as executable instructions, and do not claim that an agent performed external actions unless a connected tool actually confirmed them.

## Memory context

Memory and knowledge context must remain scoped to the signed-in Firebase UID. Keep context bounded, relevant, and protected by Firestore rules. Provide user-facing controls to review and remove stored facts as the memory feature evolves.

## Future improvements

- Typed tool registry and explicit tool schemas.
- Better intent classification and model fallback observability.
- Per-tool permission and confirmation policy.
- Evaluation tests for multilingual quality, prompt injection resistance, and agent reliability.
