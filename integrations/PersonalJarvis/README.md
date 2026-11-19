# PersonalJarvis companion integration for Zaythar AI

This folder tracks the upstream **PersonalJarvis** source bundle and documents how it can be used alongside Zaythar AI.

- Upstream: https://github.com/tbckdev/PersonalJarvis
- Source ZIP: https://github.com/tbckdev/PersonalJarvis/archive/refs/heads/main.zip
- License: follow the upstream `LICENSE` and `NOTICE` files. Current upstream main is Apache-2.0; releases through 1.6.0 remain MIT.
- Package workflow: `.github/workflows/package-personaljarvis.yml`

## Important compatibility note

Zaythar AI is a native Android application (Kotlin + Jetpack Compose). PersonalJarvis is a separate Python desktop/server application for Windows, macOS and Linux; it is **not** an Android Gradle library and cannot safely be dropped into the Android app as an APK module.

The intended integration is an optional companion service:
1. Run PersonalJarvis on a supported desktop or Linux server.
2. Configure its model/provider credentials locally in PersonalJarvis. Never commit API keys, `.env` files, credential stores, or service-account JSON.
3. Connect Zaythar AI to a secured companion API only after implementing and reviewing an authenticated HTTPS client and server-side authorization. Do not expose the local Jarvis API publicly without authentication and TLS.

## Get the ZIP bundle

Run the **Package PersonalJarvis source ZIP** workflow from the repository's GitHub Actions tab. It downloads the current upstream source archive, calculates SHA-256, and publishes both as the `PersonalJarvis-source-bundle` workflow artifact for 30 days.

You can also download the upstream archive directly from the Source ZIP link above. The workflow artifact is a source archive, not an Android installable APK.

## Run PersonalJarvis separately

Follow the upstream installation guide for your host OS:
https://github.com/tbckdev/PersonalJarvis#install

Upstream documents Windows, macOS and Linux desktop installs, plus a Python server mode. Android is not listed as a supported desktop host.

## Integration status

- [x] Add upstream source and licensing references.
- [x] Add a reproducible ZIP + SHA-256 packaging workflow.
- [ ] Build and test an authenticated Zaythar-to-PersonalJarvis HTTPS bridge.
- [ ] Add Android UI controls only after the bridge contract and security model are implemented.

No upstream application code has been copied into or represented as native Android code in this repository.
