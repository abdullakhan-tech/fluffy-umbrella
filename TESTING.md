# Testing Guide

Automated test coverage is not yet established. Do not infer production readiness from the UI running locally.

## Manual smoke test
1. Install dependencies from `backend/requirements.txt`.
2. Configure `OPENAI_API_KEY` in an untracked `.env` for AI tests.
3. Start `uvicorn backend.main:app --reload`.
4. Check `/api/health` with and without a key.
5. Test chat success, missing/invalid key errors, language and each specialist mode.
6. Test voice APIs where supported and confirm text fallback.
7. Test notes/history/preferences after reload.
8. Test mobile layout, keyboard navigation, and reduced-motion mode.

## Future automated tests
- Pydantic validation and API error mapping
- Chat payload and specialist allowlist
- Frontend rendering and interaction tests
- Accessibility and responsive regression checks
- Secret scanning and dependency vulnerability checks
