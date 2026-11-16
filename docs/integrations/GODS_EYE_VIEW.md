# Zaythar Spatial View — God's Eye View companion

Zaythar can use **God's Eye View as an optional, separately installed spatial-visualization companion**. It is not copied into the Android APK: the upstream project is a browser-based Vite/Node application (Node 24.14+ or 26), while this repository's primary app is native Android/Kotlin. Treat it as a companion service opened in a browser until a separately tested Android/WebView bridge is implemented.

## Pinned upstream source

- Project: https://github.com/bilawalsidhu/gods-eye-view
- Author/maintainers: Bilawal Sidhu and the God's Eye View contributors at Halfpixel
- Pinned commit: `ce671ce500a393be27e3cbb2a08799fbca9b6e28`
- Source license: MIT; preserve the upstream copyright and permission notice.
- Upstream license and data terms: [LICENSE](https://github.com/bilawalsidhu/gods-eye-view/blob/ce671ce500a393be27e3cbb2a08799fbca9b6e28/LICENSE), [DATA_SOURCES.md](https://github.com/bilawalsidhu/gods-eye-view/blob/ce671ce500a393be27e3cbb2a08799fbca9b6e28/DATA_SOURCES.md), [model attributions](https://github.com/bilawalsidhu/gods-eye-view/blob/ce671ce500a393be27e3cbb2a08799fbca9b6e28/public/models/README.md).

## Install locally

From the repository root:

```bash
bash scripts/fetch-gods-eye-view.sh
cd .local/gods-eye-view
npm ci
npm run doctor
npm run dev
```

The companion runs independently; it does not replace the Android app and is not automatically started by Gradle.

## Copyright and data guardrails

The MIT grant covers upstream **source code**, not every bundled data file, runtime feed, logo, name, screenshot, or 3D asset.

- Keep the upstream MIT notice with any copied/substantial source portions.
- Do not use the God's Eye View name/logo as Zaythar branding or imply endorsement. Describe it only as an independent companion based on the upstream project.
- The upstream license identifies TeleGeography submarine-cable data (CC BY-NC-SA 3.0) and Bhote Koshi imagery/derived coordinates (CC BY-NC 4.0) as **non-commercial**. Remove those datasets from any commercial distribution, or obtain separate permission.
- OpenSky, Google Maps, Cesium, AIS, OSM/ODbL, ADS-B and other live providers have their own terms, credentials, quotas and attribution requirements. Review the pinned `DATA_SOURCES.md` before enabling providers or deploying.
- Third-party models in `public/models/` are not covered by MIT; follow each model's license and attribution in its README.
- Do not copy upstream demo GIFs, screenshots, promotional media, or logo assets into Zaythar.
- Spatial feeds can be delayed, incomplete or inferred. Never present them as authoritative or use them for safety-critical decisions.

## Platform reality

The upstream app requires Node.js 24.14+ or 26 and a modern browser/GPU. It supports desktop operating systems and a phone-friendly browser layout; it is not a native Android module. Android integration must be implemented and tested separately, with an explicit origin allowlist, no privileged JavaScript bridge to untrusted pages, and provider credentials kept server-side where possible.
