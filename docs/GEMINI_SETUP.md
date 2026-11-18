# Gemini configuration for Zaythar AI

## The important distinction: no standalone Gemini key in this Android app

The current Android implementation depends on Firebase AI Logic through the Firebase Android SDK:

- Gradle dependency: com.google.firebase:firebase-ai
- Firebase configuration: app/google-services.json
- AI provider setup: Firebase Console → AI Services → AI Logic
- Authentication and abuse protection: Firebase App Check

For Firebase AI Logic using the Gemini Developer API, current Firebase documentation says that Firebase provisions and uses a Google-managed service account for Gemini authorization. The app should not contain a standalone Gemini API key. A Firebase project's normal configuration API key is not a Gemini API key; do not add the Generative Language API to its allowlist.

This is safer than embedding a Gemini key in an Android APK: APK contents can be inspected and extracted.

## Model routing

As of 17 November 2026, the app's default Hybrid route uses the Firebase AI Logic-supported `gemini-4-argon` model first, then falls back in order to `gemini-3.8-flash` and `gemini-3.5-flash-lite`. The explicit Flash and Flash-Lite routes stay pinned to their corresponding single model. The same selected primary model is used by the Neural Brain and Camera Eyes paths; chat and specialist-agent runs use the bounded fallback list.

Model availability can vary by Firebase AI provider, project configuration, SDK release, and model lifecycle. Re-check the [official Firebase AI Logic supported-model list](https://firebase.google.com/docs/ai-logic/models) before release. The offline unit tests validate routing and fallback order; they do not claim that a user's Firebase project has enabled live inference. Fallback is bounded to the listed candidates for each request; there is no unbounded retry loop.

## Enable Gemini

1. Open the Firebase console and select the Firebase project for this app.
2. Go to **AI Services → AI Logic → Get started**.
3. Select **Gemini Developer API** and complete the guided setup.
4. Ensure Firebase App Check is enforced and configure a debug provider for local testing.
5. Register the Android app with package name **com.aistudio.zaythar.zxtmkr**, download google-services.json, and place it at **app/google-services.json**.
6. Enable Google sign-in in Firebase Authentication and enable Cloud Firestore.
7. Build and test the app using the Android setup guide.

## Do I need to add a GitHub secret named GEMINI_API_KEY?

**Not for the current Firebase AI Logic Android path.** Do not create a dummy secret or commit a key. The repository's GitHub connector does not provide a secure Firebase Console setup action, and a key supplied in a chat message should never be committed to source control.

If a separate server-side service is added in the future that calls the Gemini Developer API directly using the Google Gen AI SDK, that service should read GEMINI_API_KEY from its server environment or secret manager. That is a different architecture and must not reuse a mobile app's Firebase configuration as a Gemini key.

## Useful official references

- [Firebase AI Logic: get started](https://firebase.google.com/docs/ai-logic/get-started)
- [Firebase AI Logic: security FAQ](https://firebase.google.com/docs/ai-logic/faq-and-troubleshooting)
- [Firebase AI Logic overview](https://firebase.google.com/docs/ai-logic)
- [Gemini API key guidance](https://ai.google.dev/gemini-api/docs/api-key)
