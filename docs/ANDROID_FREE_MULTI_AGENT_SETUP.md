# Free Android AI Coding and Multi-Agent Setup

This guide configures an Android-only workflow for contributing safely to Zaythar AI.

## Recommended app stack

1. **AndroidHarness — primary coding workspace.** It provides an on-device Linux workspace, Git, file editing, build/test commands, GitHub import/push, subagents, and a free built-in keyless model provider. Install from the [AndroidHarness GitHub project](https://github.com/mrmakrider/MakdroidHarness) (use its Releases or Google Play link from the project page). Free model availability, speed, context, and quality can vary; this is not unlimited hosted compute.
2. **Buddy — optional phone assistant.** Install from [F-Droid](https://f-droid.org/en/packages/app.buddy.assistant/). It can run OpenCode free models without an API key and provide a floating Android interface. Treat it as a companion, not a second writer on the same checkout.
3. **AndCode — optional alternative UI.** Install from [F-Droid](https://f-droid.org/en/packages/com.yugahashimoto.andcode/). It is a native Android front end for coding agents; it does not itself supply model inference. Configure one agent/provider at a time and avoid simultaneous edits to the same workspace.

## AndroidHarness first-time setup

1. Install AndroidHarness from its official project/release source.
2. Open Settings and choose the built-in keyless provider to start without a paid API key. Alternatively, download a small GGUF model under Settings → Local models if the device has enough free RAM and storage.
3. Grant the app's requested workspace/file access. Do not grant root access for normal repository work. Shizuku is optional; it is not required for the normal sandboxed workflow.
4. Open Settings → GitHub and use GitHub Device Flow. Complete the displayed device code on GitHub yourself. Grant access only to this repository where possible. Enable workflow permissions only if you intend to edit GitHub Actions.
5. Import https://github.com/abdullakhan-tech/fluffy-umbrella into a dedicated workspace.
6. Start in read-only/Plan mode. Ask the agent to inspect README.md, AGENTS.md, SECURITY.md, docs/PROJECT_STATUS.md, Gradle files, Firebase rules, and relevant tests before proposing changes.
7. Create a feature branch for each task. Review every diff, run tests/builds, then push the branch and open a pull request. Never force-push or push directly to main.

## Zaythar specialist team prompt

Paste this into an AndroidHarness project chat after importing the repository:

> You are the lead engineering coordinator for Zaythar AI in abdullakhan-tech/fluffy-umbrella. First inspect the current branch, git status, open pull requests/issues, README.md, AGENTS.md, SECURITY.md, docs/PROJECT_STATUS.md, Gradle configuration, Firebase rules, and relevant source/tests. Do not assume old task notes are current.
>
> Use subagents only for independent, non-overlapping investigations:
> - Android/Kotlin engineer: Compose, lifecycle, accessibility, performance, Gradle.
> - AI/multi-agent engineer: Firebase AI Logic, model routing, agent orchestration, bounded tools.
> - Security engineer: Auth/UID isolation, Firestore rules, App Check, SSRF, secret handling.
> - QA/CI engineer: unit tests, emulator/rules tests, GitHub Actions, build diagnostics.
> - Documentation reviewer: setup, architecture, release checklist, and truthful feature status.
>
> The lead must reconcile conflicting recommendations and own the final integrated change. Do not let agents edit the same files concurrently. Preserve the existing UI and architecture unless a task explicitly authorizes a change. Keep Firebase/provider secrets out of source, logs, prompts, and commits. Never invent credentials or modify production Firebase settings.
>
> Work on a new feature branch, not main. Make the smallest focused change, add/update tests and docs, run the relevant Gradle checks (./gradlew :app:testDebugUnitTest and ./gradlew :app:assembleDebug, plus Firebase emulator rules tests when relevant), inspect the complete diff, and report exact commands/results. If the device cannot run a check, leave it for GitHub Actions and say so. Push only the feature branch and open a draft PR. Never merge, force-push, delete data, or change repository settings without explicit approval.

## Suggested task cycle

1. Ask the coordinator for a read-only audit and a prioritized plan.
2. Approve one task at a time; require a dedicated branch and acceptance criteria.
3. Have specialists review in parallel, but serialize file edits.
4. Run tests and build checks; fix failures before opening a PR.
5. Review the PR diff and CI results in GitHub before merging.

## Free-tier and device limitations

- “Free” does not mean unlimited. Hosted model quotas, speed, context windows, and model availability can change. Local GGUF models avoid API charges but are limited by device RAM, storage, heat, and battery.
- Multi-agent orchestration multiplies model usage. Start with 2–4 focused specialists rather than dozens of agents.
- Android can edit and run some project checks, but a full Android emulator/device matrix is better handled by GitHub Actions.
- Keep only one app writing to a given checkout at a time. Do not let AndroidHarness, Buddy, and AndCode concurrently modify the same files.
- Never paste a GitHub personal access token or Firebase service-account key into an AI chat. Use the app's official GitHub sign-in flow and least-privilege permissions.
