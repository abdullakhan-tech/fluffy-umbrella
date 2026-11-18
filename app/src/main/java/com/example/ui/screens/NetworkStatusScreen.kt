package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.remote.NetworkMonitor
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.components.TelemetryBar
import com.example.ui.theme.LocalCyberColors

@Composable
fun NetworkStatusScreen() {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current
    val monitor = remember(context) { NetworkMonitor(context) }
    var snapshot by remember(monitor) { mutableStateOf(monitor.refresh()) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        snapshot = monitor.refresh()
    }

    LaunchedEffect(monitor, hasPermission) {
        monitor.observe().collect { snapshot = it }
    }

    val statusText = when {
        snapshot.internetValidated -> "INTERNET READY"
        snapshot.connected -> "LIMITED CONNECTION"
        else -> "OFFLINE"
    }
    val statusProgress = when {
        snapshot.internetValidated -> 1f
        snapshot.connected -> 0.45f
        else -> 0f
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "LIVE SYSTEM DIAGNOSTICS",
                titlePrefix = "Network",
                titleAccent = "Intelligence",
                trailing = {
                    Text(
                        text = if (snapshot.internetValidated) "● ONLINE" else "● CHECK LINK",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (snapshot.internetValidated) cyber.cyberGreen else cyber.accentSoft
                    )
                }
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = snapshot.internetValidated) {
                CyberCardHeader(
                    title = "LIVE CONNECTIVITY MATRIX",
                    badge = statusText,
                    badgeColor = if (snapshot.internetValidated) cyber.cyberGreen else cyber.accentSoft
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (snapshot.transport == "Wi-Fi") {
                            Icons.Default.Wifi
                        } else {
                            Icons.Default.SignalCellularAlt
                        },
                        contentDescription = null,
                        tint = if (snapshot.internetValidated) cyber.cyberGreen else cyber.accentSoft
                    )
                    Column {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = snapshot.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TelemetryBar(
                    label = "Internet validation",
                    statusText = statusText,
                    progress = statusProgress,
                    useGreen = snapshot.internetValidated
                )
                Text(
                    text = "Bandwidth values below are Android network capability estimates, not a live speed-test result.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(title = "NETWORK TELEMETRY", badge = "OS REPORTED")
                NetworkMetricRow("Transport", snapshot.transport)
                NetworkMetricRow("Network generation", snapshot.generation)
                NetworkMetricRow("Estimated downlink", formatBandwidth(snapshot.downstreamKbps))
                NetworkMetricRow("Estimated uplink", formatBandwidth(snapshot.upstreamKbps))
                NetworkMetricRow("Metered connection", if (!snapshot.connected) "Unavailable" else if (snapshot.metered) "Yes" else "No")
                NetworkMetricRow(
                    "Roaming",
                    when {
                        snapshot.transport != "Mobile network" -> "Not applicable"
                        !hasPermission -> "Permission needed"
                        snapshot.roaming -> "Yes"
                        else -> "No"
                    }
                )
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(
                    title = "MOBILE NETWORK DIAGNOSTICS",
                    badge = if (hasPermission) "PERMISSION ACTIVE" else "PERMISSION NEEDED",
                    badgeColor = if (hasPermission) cyber.cyberGreen else cyber.accentSoft
                )
                Text(
                    text = if (hasPermission) {
                        "Phone-state access is enabled. Zaythar can read the current mobile network generation when mobile data is active."
                    } else {
                        "Allow phone-state access to identify 2G / 3G / 4G / 5G when using mobile data."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!hasPermission) {
                    CyberSecondaryButton(
                        text = "ENABLE NETWORK GENERATION DETECTION",
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "network_permission_button"
                    )
                }
                Text(
                    text = "This permission is used only to read network type and roaming state. It does not read calls, messages, or contacts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CyberActionButton(
                    text = "REFRESH NETWORK STATUS",
                    onClick = { snapshot = monitor.refresh() },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "refresh_network_button"
                )
                CyberSecondaryButton(
                    text = "OPEN ANDROID NETWORK SETTINGS",
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "open_network_settings_button"
                )
            }
        }
    }
}

@Composable
private fun NetworkMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatBandwidth(kbps: Int): String = when {
    kbps <= 0 -> "Unavailable"
    kbps >= 1_000 -> String.format("%.1f Mbps", kbps / 1_000f)
    else -> "$kbps Kbps"
}
