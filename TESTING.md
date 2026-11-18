# Testing Guide

This document describes the automated test suites and build verification commands for the native Android + Firebase repository.

## Prerequisites

- JDK 21 and Android SDK (configured via `ANDROID_SDK_ROOT`)
- Node.js 20+ (for `firestore.test.js`)
- Firebase CLI with Firestore & Auth emulators (for rule suites)

## 1. Local JVM & Robolectric UI / Domain Tests

Run all JVM unit tests, Robolectric Compose UI tests, and Firestore emulator integration tests:

```bash
gradle :app:testDebugUnitTest
```

Covered suites under `app/src/test/java/com/example/`:
- `LocalToolRouterTest`: deterministic calculator, UTC clock, unit converter, JSON inspector, and plugin enable/disable routing.
- `WebConnectorTest`: SSRF URL validation (blocks localhost, RFC 1918 private ranges, IPv6 ULA/loopback, `.internal`) and public HTTPS extraction bounds.
- `StartupResilienceTest` & `domain/GuestWorkspaceStoreTest`: unauthenticated Firestore `Result.failure` safety, `ZaytharViewModel` startup resilience, `attemptAutoSignIn` lifecycle guards, and local guest workspace isolation.
- `ui/ZaytharUiAndLogicTest`, `ui/GeminiChatUiTest`, `ui/ChatGroundingAndMarkdownUiTest`: Robolectric Compose UI checks across `AuthScreen` (Google + Guest), `OverviewScreen`, `AssistantScreen`, `NeuralBrainScreen`, `PluginHubScreen`, `CameraEyesScreen`, `CodeCopilotScreen`, `StudioScreen`, `PromptNexusScreen`, `KnowledgeBaseScreen`, and `SettingsScreen`.
- `data/repository/FirestoreRepositoryRuleTest` & `data/repository/ChatSessionPersistenceTest`: UID isolation, schema boundaries, and multi-thread session persistence against the local Firestore & Auth emulators.

## 2. Firebase Security Rules Tests (Node.js)

Start the local Auth (port `9099`) and Firestore (port `8085`) emulators, then run:

```bash
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```

## 3. Android Lint & Debug / Release APK Assembly

```bash
gradle :app:lintDebug :app:assembleDebug :app:assembleRelease
```

These checks also run automatically in GitHub Actions via `.github/workflows/android-ci.yml`.

## Container / Headless Limitations

The cloud build container does not provide ADB or a live Android emulator/device. Real-device Google Sign-In (`CredentialManager`), physical camera/microphone permissions, and live Firebase AI Logic network responses must be verified on a Google Play–enabled device or streaming emulator.
