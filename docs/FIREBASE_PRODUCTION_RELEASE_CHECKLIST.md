# Firebase production release verification

This checklist is for a maintainer preparing a production release of Zaythar AI. It is a manual verification guide: repository files and Firebase Emulator tests do **not** prove that the live Firebase project is configured correctly.

## 1. Confirm the target project and Android application

- [ ] In Firebase Console, select the intended production project and verify its project ID matches the reviewed release configuration.
- [ ] Confirm the Android app is registered with application ID `com.aistudio.zaythar.zxtmkr`.
- [ ] Confirm the package name in the production `google-services.json` matches the application ID exactly.
- [ ] Review the configuration file. It may contain Firebase project identifiers and public client configuration, but must not contain service-account private keys, signing credentials, or provider secrets.
- [ ] Verify the production build variant uses the intended Firebase project; never silently substitute a developer or staging project.

## 2. Google Sign-In and signing certificates

- [ ] Enable Google as an Authentication sign-in provider in Firebase Authentication.
- [ ] Confirm the OAuth consent configuration and Android OAuth client correspond to the production package name.
- [ ] Register SHA-1 and SHA-256 fingerprints for every signing certificate used (debug only for debug testing; upload/app-signing certificate for Play releases).
- [ ] Test sign-in on a clean install using a supported physical Android device and a non-owner test account.
- [ ] Verify sign-out clears the active session and a second account cannot display the first account's data.

Do not publish signing keystores, passwords, OAuth client secrets, or access tokens in this repository or issue comments.

## 3. Firebase AI Logic

- [ ] Enable Firebase AI Logic for the selected project and confirm intended Gemini model IDs are currently available to that project and region.
- [ ] Verify the model allow-list and fallback order in source match the models enabled in Firebase. README model names are not proof of live availability.
- [ ] Test one bounded text request, one structured-output request, one vision request, and one multi-agent request on a real device.
- [ ] Confirm quota, permission, unavailable-model, timeout, and network failures become safe user-facing errors.
- [ ] Confirm cancellation stops work where supported and retry/fallback does not multiply requests without a bounded policy.
- [ ] Check billing plan, quotas, budgets, and alerts before enabling paid image-generation features.

The Android client must use Firebase AI Logic. Never add a raw Gemini API key or another provider secret to APK source, resources, assets, tests, or logs.

## 4. App Check rollout

- [ ] Confirm the release app uses the intended production attestation provider (for example, Play Integrity), not the debug provider.
- [ ] Register the production app and signing certificate as required by the provider.
- [ ] Verify valid release attestation on a Play-installed test build.
- [ ] Monitor App Check metrics before enabling enforcement.
- [ ] Enable enforcement service-by-service only after legitimate production traffic is verified.
- [ ] Confirm debug tokens are restricted to local development and have not been committed or reused in production.

Do not enable enforcement blindly: first verify the release build obtains valid tokens and all required clients are registered.

## 5. Firestore rules and account isolation

- [ ] Review `firestore.rules` against every collection and subcollection currently used by the app.
- [ ] Run `node --test firestore.test.js` against the Firebase Emulator Suite.
- [ ] Deploy the reviewed rules to the intended production project using an authorized maintainer account.
- [ ] Verify unauthenticated access is denied.
- [ ] With two independent test accounts, verify each account can read/write only its own `users/{uid}/...` data.
- [ ] Verify malformed, oversized, unexpected-field, and cross-session writes are rejected.
- [ ] Verify deletion and sign-out behavior does not leave another account's cached data visible.

Never use real user data for authorization tests. Keep emulator tests and production verification clearly separate.

## 6. CI, signing, and release artifacts

- [ ] Confirm the latest release-branch commit has successful Android unit/Robolectric tests, Firestore emulator rules tests, Android Lint, and debug/release APK assembly.
- [ ] Confirm CI uses emulator/demo configuration only and does not need production credentials.
- [ ] Inspect the release artifact's application ID, version, manifest permissions, and signing configuration.
- [ ] Sign distributable artifacts through the approved protected release process; do not commit release keystores or signing passwords.
- [ ] Record the exact commit SHA and artifact checksum in the release record.

## 7. Final manual smoke test

On a physical Android device, record device model, Android version, app version, commit SHA, and test date.

- [ ] Fresh install and first launch.
- [ ] Google Sign-In, sign-out, and second-account isolation.
- [ ] Text chat, streaming, cancellation, and bounded fallback.
- [ ] Voice permission granted and denied; speech recognition and TTS availability.
- [ ] Camera/photo picker permission and image handling.
- [ ] Network loss/recovery and HTTPS web grounding.
- [ ] App Check attestation and Firestore access.
- [ ] Reduced-motion setting and navigation lifecycle behavior.

## Release decision

A release is ready only when all applicable checks are completed and recorded. Mark any unavailable device, Firebase Console, billing, OAuth, or Play Integrity check as **not verified**—never infer success from a successful CI build.

### Current repository boundary

This repository can validate source-level configuration and emulator behavior. A maintainer with access to the production Firebase Console, signing setup, and a physical Android device must complete the live checks. Do not place credentials, private identifiers, screenshots containing personal data, or App Check debug tokens in the verification record.
