# Voice assistant

## Current direction

The Android application integrates platform speech recognition and TextToSpeech. Availability and supported languages depend on the Android device and installed speech services.

## User guidance

- Grant microphone permission only when using speech input.
- Use text chat when speech recognition is unavailable or permission is denied.
- Stop speech output when the user interrupts or leaves the relevant interaction.
- Avoid recording or uploading audio without clear user awareness and a feature requirement.

## Wake-word roadmap

An always-listening wake word is not documented as a shipped feature. A future implementation must be opt-in, disclose microphone and battery use, use an Android foreground service with a persistent notification where required, and provide an immediate stop control. Push-to-talk should remain available as a privacy-preserving fallback.

## Troubleshooting

Check Android microphone permission, the device's default speech recognition service, network connectivity if the provider requires it, and the selected language. See [Troubleshooting](Troubleshooting.md).
