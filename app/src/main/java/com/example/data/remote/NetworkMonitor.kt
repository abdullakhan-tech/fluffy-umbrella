package com.example.data.remote

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class NetworkSnapshot(
    val connected: Boolean,
    val internetValidated: Boolean,
    val transport: String,
    val generation: String,
    val metered: Boolean,
    val roaming: Boolean,
    val details: String
)

class NetworkMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
    private val telephony = appContext.getSystemService(TelephonyManager::class.java)
    @Volatile private var displayOverrideType: Int = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE

    fun observe(): Flow<NetworkSnapshot> = callbackFlow {
        fun publish() { trySend(readSnapshot()) }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = publish()
            override fun onLost(network: Network) = publish()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = publish()
        }
        publish()
        runCatching { connectivity.registerDefaultNetworkCallback(callback) }
        var displayInfoCallback: TelephonyCallback? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasPhoneStatePermission()) {
            val listener = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
                override fun onDisplayInfoChanged(info: TelephonyDisplayInfo) {
                    displayOverrideType = info.overrideNetworkType
                    publish()
                }
            }
            displayInfoCallback = listener
            runCatching {
                telephony.registerTelephonyCallback(ContextCompat.getMainExecutor(appContext), listener)
            }
        }
        val poller = launch {
            while (isActive) {
                delay(5_000L)
                publish()
            }
        }
        awaitClose {
            poller.cancel()
            displayInfoCallback?.let { listener ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching { telephony.unregisterTelephonyCallback(listener) }
                }
            }
            runCatching { connectivity.unregisterNetworkCallback(callback) }
        }
    }

    fun refresh(): NetworkSnapshot = readSnapshot()

    private fun readSnapshot(): NetworkSnapshot {
        val network = connectivity.activeNetwork
        val caps = network?.let(connectivity::getNetworkCapabilities)
        val connected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val transport = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile network"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Other"
        }
        val hasPhonePermission = hasPhoneStatePermission()
        val generation = if (transport == "Mobile network" && hasPhonePermission) {
            runCatching { networkGeneration(telephony.dataNetworkType, displayOverrideType) }.getOrDefault("Cellular")
        } else if (transport == "Mobile network") {
            "Cellular (grant phone-state permission for 3G/4G/5G detail)"
        } else if (connected) "Not applicable" else "Unavailable"
        val roaming = if (hasPhonePermission) runCatching { telephony.isNetworkRoaming }.getOrDefault(false) else false
        val metered = connectivity.isActiveNetworkMetered
        return NetworkSnapshot(
            connected = connected,
            internetValidated = validated,
            transport = transport,
            generation = generation,
            metered = metered,
            roaming = roaming,
            details = when {
                !connected -> "No active internet connection"
                !validated -> "Connected, but Android has not validated internet access"
                else -> "Internet connection is available"
            }
        )
    }

    private fun hasPhoneStatePermission(): Boolean = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    companion object {
        fun networkGeneration(
            type: Int,
            overrideType: Int = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
        ): String = when {
            overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ||
                overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5G"
            type == TelephonyManager.NETWORK_TYPE_NR -> "5G"
            type == TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD -> "3G"
            TelephonyManager.NETWORK_TYPE_GSM,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
            else -> "Cellular / unknown"
        }
    }
}
