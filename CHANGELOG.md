# Changelog

## Unreleased
- Added resilient parallel specialist execution with per-agent model fallback and coordinator synthesis.
- Added optional-memory failure handling for the multi-agent runner.
- Added activity-aware neural-core states for Ready, Listening, and Thinking.
- Added the official Gradle 9.3.1 wrapper for reproducible Android and Windows builds.
- Added a main-branch Android CI workflow for unit tests and debug APK assembly.
- Added version-controlled project Wiki pages, JARVIS execution guidance, and specialist agent profiles.
- Redacted private Firestore paths, user identity, and raw exception details from diagnostics.
- Removed generated Gradle cache files from source control and ignored local Firebase/build artifacts.
- Updated legacy documentation to reflect the native Android product and current platform limitations.

## Historical note
Earlier revisions explored a browser/FastAPI assistant. The current default branch is a native Android Kotlin/Jetpack Compose application; old web-backend plans are not current deployment instructions.