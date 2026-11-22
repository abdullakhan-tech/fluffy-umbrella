# Stonic AI installer provenance

The Stonic artifact supplied for this integration is:

- Original filename: `Stonic-AI-Setup-3.1.24.exe`
- Format: Windows PE32 GUI executable; Nullsoft/NSIS self-extracting installer
- Size: 394,765,392 bytes (about 377 MiB)
- SHA-256: `9f6d5abc6ac489b6511fb216918039e7abe971c4ba210ceda05e37d8d764aa40`

## Repository handling

The original installer binary is intentionally **not committed** here. GitHub's ordinary Git file storage rejects files above 100 MB; this installer is almost 377 MiB. The connected GitHub integration also does not expose a Git LFS or Release-asset upload operation. Do not rename, split, or disguise the executable to bypass repository limits.

This directory contains provenance and integration notes only, not Stonic's proprietary engine or source code.

## Integration status

Zaythar's Python backend now has an optional adapter for a Stonic service that implements the OpenAI-compatible `POST /v1/chat/completions` contract. Configure these server-side variables only after obtaining a verified Stonic gateway/API:

```env
STONIC_BASE_URL=https://your-stonic-gateway.example
STONIC_API_KEY=replace-with-server-side-secret
STONIC_MODEL=your-model-id
```

The desktop installer itself does not establish that such an API exists. No installer execution, proprietary source extraction, or live Stonic API call has been performed.

## Required artifact for native integration

To integrate Stonic's actual engine into Android and Python, provide one of:

1. The extracted Stonic application/source folder as a ZIP (without secrets or personal data); or
2. Official API documentation, endpoint URL, authentication method, request/response schema, and licensing/redistribution permission.

Do not commit API keys, tokens, private certificates, or user data.
