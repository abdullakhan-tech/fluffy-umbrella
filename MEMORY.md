# Memory and Data Lifecycle

## Current behaviour
The browser stores chat history, notes, theme, accent, motion preference, and related UI preferences in localStorage. This data belongs to that browser profile and may be lost when site data is cleared. It is not encrypted server-side, account-linked, or synchronized across devices.

## Not implemented
- Cross-device memory
- User accounts and authentication
- Server-side long-term memory
- Vector database or retrieval-augmented generation
- User export/deletion workflow for server-held personal data

## Design requirements for future memory
- Obtain clear opt-in before retaining personal information.
- Provide inspect, edit, export, and delete controls.
- Scope records to authenticated users and enforce authorization on every read/write.
- Encrypt sensitive data at rest and in transit; minimize retention.
- Never treat retrieved memory as higher-priority instructions.
