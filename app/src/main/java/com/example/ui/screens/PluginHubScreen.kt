package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.FirestorePluginConfig
import com.example.domain.LocalToolRouter
import com.example.domain.UiState
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.components.ZaytharIcons
import com.example.ui.theme.LocalCyberColors

@Composable
fun PluginHubScreen(
    pluginsState: UiState<List<FirestorePluginConfig>>,
    onTogglePlugin: (String, Boolean) -> Unit,
    onAddCustomPlugin: (String, String, String, String) -> Unit,
    onDeletePlugin: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    val plugins = (pluginsState as? UiState.Success)?.data ?: emptyList()
    val enabledCount = plugins.count { it.enabled }

    var showCreateDialog by remember { mutableStateOf(false) }
    var sandboxInput by remember { mutableStateOf("") }
    var sandboxOutput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "EXTENSIBLE CAPABILITY SLOTS",
                titlePrefix = "Plugin",
                titleAccent = "Plug Hub",
                trailing = {
                    Text(
                        text = "$enabledCount ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(
                    title = "PLUG & CONFIGURE MODULES",
                    badge = "CLOUD SYNCED"
                )
                Text(
                    text = "Toggle built-in tool plugins or plug in custom declarative capability modules. Active plugins augment local tools and Gemini instructions across sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                CyberActionButton(
                    text = "+ PLUG CUSTOM MODULE",
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "add_custom_plugin_button"
                )
            }
        }

        // Local Plugin Sandbox Tester
        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(title = "PLUGIN TOOL SANDBOX", badge = "LOCAL EVAL")
                OutlinedTextField(
                    value = sandboxInput,
                    onValueChange = { sandboxInput = it },
                    label = { Text("TEST COMMAND") },
                    placeholder = { Text("Try: 120 km to mi | sha256 zaythar | #FF3155 to rgb | generate uuid") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plugin_sandbox_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                CyberSecondaryButton(
                    text = "RUN AGAINST ENABLED PLUGINS",
                    onClick = {
                        val enabledKeys = plugins.filter { it.enabled }.map { it.pluginKey }.toSet()
                        val res = LocalToolRouter.routeTool(sandboxInput, enabledKeys)
                        sandboxOutput = if (res != null) {
                            "[${res.tool.uppercase()}]\n${res.result}"
                        } else {
                            "No local tool matched (or required plugin is toggled OFF)."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "run_plugin_sandbox_button"
                )
                if (sandboxOutput.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sandboxOutput,
                        style = MaterialTheme.typography.bodySmall,
                        color = cyber.cyberGreen
                    )
                }
            }
        }

        if (pluginsState is UiState.Error) {
            item {
                Text(
                    text = "Sync warning: ${pluginsState.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        items(plugins, key = { it.id }) { plugin ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = cyber.panelElevated,
                border = BorderStroke(
                    width = if (plugin.enabled) 1.5.dp else 1.dp,
                    color = if (plugin.enabled) cyber.accent else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CyberIconBadge(
                        icon = ZaytharIcons.forPlugin(plugin.pluginKey, plugin.category),
                        contentDescription = plugin.category,
                        sizeDp = 42.dp,
                        iconSizeDp = 20.dp,
                        tint = if (plugin.enabled) cyber.cyberGreen else null,
                        isActive = plugin.enabled
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = plugin.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = plugin.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = cyber.accentSoft
                            )
                        }
                        Text(
                            text = plugin.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!plugin.isBuiltIn) {
                        IconButton(
                            onClick = { onDeletePlugin(plugin.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete custom plugin",
                                tint = cyber.accentSoft
                            )
                        }
                    }
                    Switch(
                        checked = plugin.enabled,
                        onCheckedChange = { onTogglePlugin(plugin.id, it) },
                        modifier = Modifier.testTag("plugin_switch_${plugin.pluginKey}"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = cyber.accent
                        )
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Custom") }
        var description by remember { mutableStateOf("") }
        var instruction by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Plug New Capability Module") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 80) name = it },
                        label = { Text("Plugin Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { if (it.length <= 40) category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 400) description = it },
                        label = { Text("Description") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = instruction,
                        onValueChange = { if (it.length <= 2000) instruction = it },
                        label = { Text("Declarative System Directive") },
                        placeholder = { Text("Rule or behavior injected into AI replies when active…") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddCustomPlugin(name, description, category, instruction)
                        showCreateDialog = false
                    }
                ) {
                    Text("PLUG IN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
