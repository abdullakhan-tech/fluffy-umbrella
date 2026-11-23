package com.example.domain

/**
 * Centralizes the privacy gate for Camera Eyes.
 * A preview/capture session may bind only after an explicit user enable action
 * and a granted Android camera permission.
 */
internal object CameraActivationPolicy {
    fun shouldBindCamera(cameraEnabled: Boolean, hasCameraPermission: Boolean): Boolean =
        cameraEnabled && hasCameraPermission

    fun canSwitchToCamera(
        cameraEnabled: Boolean,
        hasCameraPermission: Boolean,
        targetCameraAvailable: Boolean
    ): Boolean = cameraEnabled && hasCameraPermission && targetCameraAvailable
}
