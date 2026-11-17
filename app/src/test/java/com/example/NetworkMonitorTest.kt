package com.example

import com.example.data.remote.NetworkMonitor
import org.junit.Assert.assertEquals
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
}
