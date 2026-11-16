# Zaythar AI project status

This page records what is visible in the repository. It does not claim that cloud services or device builds have been tested.

## Current Android app direction

The current default branch contains a native Android project using:

- Kotlin and Jetpack Compose.
- Firebase Authentication with Google sign-in.
- Cloud Firestore with per-user document paths and repository security rules.
- Firebase AI Logic for Gemini model access.
- Android speech recognition and text-to-speech APIs.
- Jetpack DataStore for local preferences.

The app module uses application ID **com.aistudio.zaythar.zxtmkr**, minimum Android API 26, compile SDK 36, and Java 21 compatibility.

## Configuration still required outside the repository

- Register the Android application in the intended Firebase project.
- Add the matching app/google-services.json file.
- Enable Google sign-in in Firebase Authentication and register signing fingerprints.
- Enable Cloud Firestore and deploy the reviewed firestore.rules.
- Enable Firebase AI Logic with the Gemini Developer API.
- Configure Firebase App Check for debug and production builds.

Firebase AI Logic is the chosen Gemini integration. A raw GEMINI_API_KEY is not required in the Android app.

## Verification status

The latest repository commit inspected while preparing this document initializes the Android project structure. No workflow run or combined status was returned for that commit by the connected GitHub checks. A successful Android build, live Gemini request, Google sign-in, App Check attestation, and Firestore access have therefore **not** been independently verified in this update.

## Known documentation note

Some older root-level documentation describes an earlier browser/FastAPI/OpenAI design. Treat the Android setup and Gemini integration guides in docs/ as the current Android onboarding path. Reconcile any legacy backend documentation before reintroducing or deploying that backend.
