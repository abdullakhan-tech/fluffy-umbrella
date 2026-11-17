package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.material3.Icon
import com.example.data.remote.NetworkMonitor
import com.example.data.remote.NetworkSnapshot

@Composable
fun NetworkStatusScreen() {
    val context = LocalContext.current
    val monitor = remember(context) { NetworkMonitor(context) }
    var snapshot by remember { mutableStateOf(monitor.refresh()) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        snapshot = monitor.refresh()
    }

    LaunchedEffect(monitor) {
        monitor.observe().collect { snapshot = it }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("NETWORK INTELLIGENCE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Live connectivity", style = MaterialTheme.typography.headlineMedium)
        Text("Zaythar monitors Android connectivity and reports the active transport. Mobile generation is read from the device when permission is granted.", style = MaterialTheme.typography.bodyMedium)
        NetworkMetricCard(snapshot)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mobile network detail", style = MaterialTheme.typography.titleMedium)
                Text(if (hasPermission) "Phone-state permission is enabled." else "Allow phone-state access to identify 2G / 3G / 4G / 5G when using mobile data.", style = MaterialTheme.typography.bodyMedium)
                if (!hasPermission) {
                    OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }) {
                        Icon(Icons.Default.SignalCellularAlt, contentDescription = null)
                        Text("  Enable network generation detection")
                    }
                }
                Text("This permission is used only to read the current network type and roaming state. It does not read calls, messages, or contacts.", style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(onClick = { snapshot = monitor.refresh() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text("  Refresh network status")
        }
    }
}

@Composable
private fun NetworkMetricCard(snapshot: NetworkSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if (snapshot.transport == "Wi-Fi") Icons.Default.Wifi else Icons.Default.SignalCellularAlt, contentDescription = null, tint = if (snapshot.internetValidated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Column {
                    Text(if (snapshot.internetValidated) "INTERNET READY" else if (snapshot.connected) "LIMITED CONNECTION" else "OFFLINE", style = MaterialTheme.typography.labelLarge)
                    Text(snapshot.details, style = MaterialTheme.typography.bodySmall)
                }
            }
            MetricLine("Transport", snapshot.transport)
            MetricLine("Network generation", snapshot.generation)
            MetricLine("Metered connection", if (snapshot.metered) "Yes" else "No")
            MetricLine("Roaming", if (snapshot.roaming) "Yes" else "No / unavailable")
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
