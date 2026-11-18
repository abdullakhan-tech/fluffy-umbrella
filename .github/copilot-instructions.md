# GitHub Copilot Instructions

Follow `AGENTS.md`, `RULES.md`, `SECURITY.md`, and `ARCHITECTURE.md`.

- Use Kotlin, Jetpack Compose Material 3, StateFlow/ViewModels, and Firebase AI Logic (`Firebase.ai`).
- Never embed Gemini or Firebase secret keys in client source code, tests, or logs.
- Isolate all Cloud Firestore reads/writes by authenticated `FirebaseAuth` UID and mirror rules in `firestore.rules`.
- Keep interactive UI accessible (`>= 48.dp` touch targets, `contentDescription`, `testTag`) and reduced-motion aware.
- Validate and bound all user inputs, URLs (`WebConnector`), and bitmap/image payloads (`CameraEyesScreen`).
- Update tests (`app/src/test` and `firestore.test.js`) and documentation when behavior changes.
