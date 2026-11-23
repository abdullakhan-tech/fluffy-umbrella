package com.example.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraActivationPolicyTest {
    @Test
    fun cameraStaysOffUntilUserEnablesIt() {
        assertFalse(CameraActivationPolicy.shouldBindCamera(cameraEnabled = false, hasCameraPermission = true))
    }

    @Test
    fun permissionDenialPreventsCameraBinding() {
        assertFalse(CameraActivationPolicy.shouldBindCamera(cameraEnabled = true, hasCameraPermission = false))
    }

    @Test
    fun cameraBindsOnlyWhenEnabledAndPermissionGranted() {
        assertTrue(CameraActivationPolicy.shouldBindCamera(cameraEnabled = true, hasCameraPermission = true))
    }

    @Test
    fun lensSwitchRequiresCameraEnabledPermissionAndAvailableTarget() {
        assertTrue(CameraActivationPolicy.canSwitchToCamera(true, true, true))
    }

    @Test
    fun lensSwitchIsBlockedWhenTargetCameraIsMissing() {
        assertFalse(CameraActivationPolicy.canSwitchToCamera(true, true, false))
    }

    @Test
    fun lensSwitchIsBlockedWhenCameraIsOffOrPermissionIsMissing() {
        assertFalse(CameraActivationPolicy.canSwitchToCamera(false, true, true))
        assertFalse(CameraActivationPolicy.canSwitchToCamera(true, false, true))
    }
}
