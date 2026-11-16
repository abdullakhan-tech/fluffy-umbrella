# Contributing

## Workflow
1. Open or identify an issue describing the problem.
2. Inspect the current Android/Firebase implementation before proposing a new framework.
3. Keep changes focused and explain user-visible behaviour.
4. Update tests and documentation with code changes.
5. Never include secrets, personal data, generated credentials, or private Firebase configuration.

## Local setup
Follow [Android setup](docs/ANDROID_SETUP.md), [Gemini setup](docs/GEMINI_SETUP.md), and [Testing](TESTING.md). Use JDK 21 and the committed Gradle wrapper.

## Pull request checklist
- [ ] Current Android architecture and dependency versions respected.
- [ ] Firebase UID ownership and privacy boundaries preserved.
- [ ] Phone and expanded-screen layouts considered.
- [ ] Permissions, accessibility, and reduced-motion behaviour considered.
- [ ] Relevant Gradle tests/build run, or untested areas disclosed.
- [ ] Documentation updated and no credentials included.
- [ ] Planned or visual-only functionality is not described as shipped.