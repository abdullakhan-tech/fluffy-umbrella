package com.example

import com.example.data.remote.NetworkMonitor
import org.junit.Assert.assertEquals
import org.junit.Test
import android.telephony.TelephonyManager

class NetworkMonitorTest {
    @Test fun recognizesMobileGenerations() {
        assertEquals("5G", NetworkMonitor.networkGeneration(TelephonyManager.NETWORK_TYPE_NR))
        assertEquals("4G", NetworkMonitor.networkGeneration(TelephonyManager.NETWORK_TYPE_LTE))
        assertEquals("3G", NetworkMonitor.networkGeneration(TelephonyManager.NETWORK_TYPE_UMTS))
        assertEquals("2G", NetworkMonitor.networkGeneration(TelephonyManager.NETWORK_TYPE_EDGE))
    }
}
