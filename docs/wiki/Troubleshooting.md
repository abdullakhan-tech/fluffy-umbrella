# Troubleshooting

## Gradle or Android build fails

- Confirm JDK 21 is selected.
- Confirm Android SDK and compile SDK 36 are installed.
- Run the wrapper from the repository root.
- Review the first meaningful Gradle error rather than only the final task failure.

## Google Sign-In fails

- Confirm the Android package name is exactly `com.aistudio.zaythar.zxtmkr`.
- Confirm the correct Firebase project file is at `app/google-services.json`.
- Enable Google as a sign-in provider and register the required signing certificate fingerprints.

## Gemini request fails

- Confirm Firebase AI Logic and the Gemini Developer API are enabled for the Firebase project.
- Confirm the app is initialized against the intended Firebase project.
- Check model availability, network connectivity, quotas, and App Check configuration.
- Do not attempt to fix client errors by hard-coding an API key in the APK.

## Firestore access denied

- Confirm the user is signed in.
- Check that the repository uses the current authenticated UID.
- Review deployed Firestore rules and ensure the document path belongs to that UID.
- Do not loosen rules to public read/write as a workaround.

## Speech input or output unavailable

- Check microphone permission and the device speech service.
- Verify that the chosen language is supported by the installed service.
- Fall back to text chat if recognition or TTS is unavailable.

For the authoritative setup checklist, see [Android setup](../ANDROID_SETUP.md) and [Project status](../PROJECT_STATUS.md).
