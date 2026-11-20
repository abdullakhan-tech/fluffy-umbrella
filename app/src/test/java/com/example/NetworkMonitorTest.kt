package com.example

import com.example.data.remote.NetworkMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import android.telephony.TelephonyManager
import android.telephony.TelephonyDisplayInfo

class NetworkMonitorTest {
    @Test fun recognizesMobileGenerations() {
        assertEquals("5G", NetworkMonitor.networkGeneration(TelephonyManager.NETWORK_TYPE_NR))
        assertEquals("4G", NetworkMonitor.networkGeneration(TelephonyManager.NETWORK_TYPE_LTE))
        assertEquals("3G", NetworkMonitor.networkGeneration(TelephonyManager.NETWORK_TYPE_UMTS))
        assertEquals("2G", NetworkMonitor.networkGeneration(TelephonyManager.NETWORK_TYPE_EDGE))
    }

    @Test fun recognizes5gNonStandaloneAndAdvancedDisplayOverrides() {
        assertEquals("5G", NetworkMonitor.networkGeneration(TelephonyManager.NETWORK_TYPE_LTE, TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA))
        assertEquals("5G", NetworkMonitor.networkGeneration(TelephonyManager.NETWORK_TYPE_LTE, TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED))
    }

    @Test fun mapsGsmAsSecondGeneration() {
        assertEquals("2G", NetworkMonitor.networkGeneration(TelephonyManager.NETWORK_TYPE_GSM))
    }

    @Test fun unknownTypeDoesNotClaimAHighSpeedGeneration() {
        assertEquals("Cellular / unknown", NetworkMonitor.networkGeneration(TelephonyManager.NETWORK_TYPE_UNKNOWN))
    }

    @Test fun doesNotReadCellularTypeWithoutPhoneStatePermission() {
        var accessed = false
        val result = NetworkMonitor.guardedNetworkGeneration(
            permissionGranted = false,
            readNetworkType = {
                accessed = true
                TelephonyManager.NETWORK_TYPE_LTE
            }
        )

        assertFalse(accessed)
        assertEquals("Cellular (grant phone-state permission for 3G/4G/5G detail)", result)
    }

    @Test fun fallsBackWhenPhoneStatePermissionIsRevokedBeforePlatformRead() {
        val result = NetworkMonitor.guardedNetworkGeneration(
            permissionGranted = true,
            readNetworkType = { throw SecurityException("permission revoked") }
        )

        assertEquals("Cellular", result)
    }
}
