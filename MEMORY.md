# Memory and Data Lifecycle

## Current behaviour
- **Cloud Firestore (`users/{userId}/...`)**: Stores authenticated user profiles, chat sessions (`chatSessions`), session messages (`messages`), saved memory notes (`memoryNotes`), knowledge graph triples (`knowledgeFacts`), plugin configurations (`plugins`), and AI Brain snapshots (`brainRuns`) in named database `ai-studio-android-fluffyum-16d0226b-744f-4389-a061-b5bf3e77239a`.
- **Jetpack DataStore (`zaythar_prefs`)**: Stores device-local UI preferences (theme mode, accent color, 3D FX motion toggle, persona sliders, language code, TTS toggle, web grounding toggle, and model route).

## Security & Isolation Controls
- Every Firestore read and write is gated behind Google Sign-In (`FirebaseAuth`) and enforced server-side by `firestore.rules` (`request.auth.uid == userId`).
- Retrieved memory notes and knowledge graph facts are clearly framed in prompts as unverified user context, never as higher-priority system instructions.
- Users can create, inspect, rename, and delete chat sessions, clear thread history, and delete saved memory notes, knowledge facts, custom plugins, and AI Brain snapshots directly in the app.
