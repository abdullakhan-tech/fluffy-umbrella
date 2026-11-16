# Android setup guide

Zaythar AI is currently a native Android project built with Kotlin, Jetpack Compose, Firebase Authentication, Cloud Firestore, and Firebase AI Logic.

## Requirements

- Android Studio with Android SDK Platform 36 and compatible build tools.
- JDK 21.
- An Android device or emulator running Android 8.0 (API 26) or newer.
- Access to the Firebase project used by this repository.

The Android app module declares minSdk 26, compileSdk 36, Java source/target compatibility 21, and application ID **com.aistudio.zaythar.zxtmkr**.

## 1. Connect the Android app to Firebase

1. Open the Firebase console and select the Firebase project intended for Zaythar AI.
2. Register an Android app using the exact package name **com.aistudio.zaythar.zxtmkr**.
3. Download the generated **google-services.json** file.
4. Put the file at **app/google-services.json** (inside the app module, next to app/build.gradle.kts).
5. In Firebase Authentication, enable Google as a sign-in provider.
6. Add the debug signing certificate fingerprints required by Google Sign-In. Run **./gradlew signingReport** (Windows: **gradlew.bat signingReport**) and register the SHA-1/SHA-256 fingerprints shown for the debug variant.

Do not rename the JSON file or place it in the repository root. The Google Services Gradle plugin is already configured in the app module.

## 2. Enable Gemini through Firebase AI Logic

1. In the Firebase console, open **AI Services → AI Logic**.
2. Choose **Get started** and select the **Gemini Developer API** provider.
3. Complete the guided setup so Firebase enables the required APIs and provisions its managed authorization.
4. Keep Firebase App Check enabled. Configure the debug provider for local development and a production attestation provider before distributing a release build.
5. Confirm the app's Firebase AI Logic model configuration is compatible with the models enabled for the project.

**Do not paste a Gemini API key into Kotlin source, Gradle files, Android resources, GitHub commits, or the APK.** The app uses the Firebase AI Logic SDK (com.google.firebase:firebase-ai). With the current Firebase AI Logic security model, Firebase manages Gemini Developer API authorization using a Google-managed service account; the app does not need a standalone Gemini API key. See [Gemini setup](GEMINI_SETUP.md).

## 3. Configure Firestore

1. Enable Cloud Firestore in the same Firebase project.
2. Review firestore.rules before deployment.
3. Deploy the rules to the intended Firebase project:

~~~bash
firebase projects:list
firebase deploy --only firestore:rules --project YOUR_FIREBASE_PROJECT_ID
~~~

Replace the placeholder with the actual project ID shown by Firebase. Never deploy rules to a project until you have confirmed it is the intended project.

The rules are default-deny and scope user data to the authenticated Firebase UID. Do not weaken them to make a client-side permission error disappear.

## 4. Build and run

From the repository root:

Linux, macOS, or Termux with a compatible JDK/Android SDK setup:

~~~bash
./gradlew :app:assembleDebug
~~~

Windows PowerShell:

~~~powershell
.\gradlew.bat :app:assembleDebug
~~~

To install on a connected Android device, use **./gradlew :app:installDebug** (Windows: **gradlew.bat :app:installDebug**).

Open the project in Android Studio and allow Gradle sync to finish before running it. A successful compile does not prove that Firebase sign-in, App Check, Firestore, or live Gemini requests are configured; verify each on a real device or emulator.

## Troubleshooting

- **Google Services plugin reports a missing config:** confirm app/google-services.json exists and matches the application ID.
- **Google sign-in fails:** check that Google is enabled in Firebase Authentication and that the correct signing certificate fingerprints are registered.
- **AI request is rejected:** verify AI Logic is enabled for the correct Firebase project, App Check is configured, and the selected model is available.
- **Firestore permission denied:** verify the user is signed in, the document path uses that user's UID, and the deployed rules match firestore.rules.
- **Do not share credentials in issues or chat.** Share sanitized error messages only.
