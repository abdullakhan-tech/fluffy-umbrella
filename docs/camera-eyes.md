# Camera Eyes

Camera Eyes uses Android CameraX for an in-app front/rear camera preview.

## Privacy and operation
- Camera preview is **off by default**.
- The user must switch Camera Eyes on and grant Android's runtime camera permission.
- Turning the switch off unbinds the CameraX controller immediately.
- Leaving the Camera Eyes screen unbinds the controller.
- A still frame is captured only after the user taps **Capture Frame**.
- The frame is sent to the existing Firebase AI Logic Gemini vision engine only after the user taps **Analyze with Camera Eyes**.
- Captured frames are stored in the app cache; this feature does not start background recording or continuous frame uploads.

## Current capabilities
- Live local front/rear camera preview, lens switching, and explicit still-frame capture.
- Lens switching is available only while Camera Eyes is enabled, permission is granted, and CameraX initialization confirms the target lens exists. Devices without a front camera keep the switch disabled.
- Existing scene description, OCR, code/UI inspection, and object-identification prompts.
- Permission-gated **Speak Vision Question** uses Android speech recognition to fill the optional prompt; the user reviews it and taps Analyze. Microphone capture is not started automatically and is released when leaving the screen.
- Explicit **Send Visual Context to Chat** action forwards the returned analysis into the normal chat pipeline only after the user taps it.
- Existing optional save-to-RAG action for the returned text analysis.

Continuous object tracking, always-on vision, face identification, and background camera access are not implemented.
