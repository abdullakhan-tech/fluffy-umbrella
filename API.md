# API and integration boundaries

## Current Android client

The current default branch is a native Android application. Its primary AI integration is the Firebase AI Logic Android SDK (com.google.firebase:firebase-ai), not the earlier FastAPI POST /api/chat endpoint described by previous revisions of this document.

The Android app also uses:

- Firebase Authentication for Google sign-in.
- Cloud Firestore for UID-scoped conversations, memories, and knowledge facts.
- Firebase App Check to protect Firebase-backed resources.
- Jetpack DataStore for local UI preferences.

## Gemini requests

Gemini model requests are made through Firebase AI Logic. Enable the Gemini Developer API in Firebase Console → AI Services → AI Logic. Firebase handles authorization with its managed service identity; the Android app must not contain a standalone Gemini API key.

## Firestore data boundary

Firestore data is scoped below users/{userId}. The authenticated Firebase UID must match the userId in the document path. The authoritative client access policy is firestore.rules; review and deploy it to the intended Firebase project.

## Required configuration

- app/google-services.json generated for Android package com.aistudio.zaythar.zxtmkr.
- Google sign-in enabled in Firebase Authentication.
- Cloud Firestore enabled with the repository's reviewed security rules.
- Firebase AI Logic enabled with Gemini Developer API.
- Firebase App Check configured for development and production.

See docs/ANDROID_SETUP.md for setup and build steps.

## Legacy backend

Older revisions described a browser frontend and FastAPI/OpenAI API. That is not the current Android integration contract. Do not deploy or rely on the old endpoint unless a backend is restored and independently tested.
