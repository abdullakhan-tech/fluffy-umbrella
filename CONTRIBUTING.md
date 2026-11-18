# Contributing

## Workflow
1. Open an issue describing the problem or proposed change.
2. Keep pull requests focused, modular, and well-tested.
3. Update documentation (`ARCHITECTURE.md`, `TESTING.md`, `docs/`) and unit/UI/Firestore tests with code changes.
4. Never include secrets, private user data, or release keystore credentials.
5. Verify Material 3 accessibility (`48.dp` touch targets, `contentDescription`, light/dark theme contrast) for UI changes.

## Local setup
Follow `docs/ANDROID_SETUP.md`, `docs/GEMINI_SETUP.md`, and `TESTING.md`.

## Pull request checklist
- [ ] Scope and limitations are clear.
- [ ] Compact and tablet/foldable layouts considered.
- [ ] Accessibility semantics and reduced-motion toggle considered.
- [ ] `gradle :app:testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `firestore.test.js` run, or untested hardware/device areas disclosed.
- [ ] Documentation updated.
- [ ] No secrets or private user data included.
