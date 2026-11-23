# Camera Eyes

Camera Eyes uses Android CameraX for an in-app rear-camera preview.

## Privacy and operation
- Camera preview is **off by default**.
- The user must switch Camera Eyes on and grant Android's runtime camera permission.
- Turning the switch off unbinds the CameraX controller immediately.
- Leaving the Camera Eyes screen unbinds the controller.
- A still frame is captured only after the user taps **Capture Frame**.
- The frame is sent to the existing Firebase AI Logic Gemini vision engine only after the user taps **Analyze with Camera Eyes**.
- Captured frames are stored in the app cache; this feature does not start background recording or continuous frame uploads.

## Current capabilities
- Live local camera preview and explicit still-frame capture.
- Existing scene description, OCR, code/UI inspection, and object-identification prompts.
- Existing optional save-to-RAG action for the returned text analysis.

Continuous object tracking, always-on vision, face identification, and background camera access are not implemented.
