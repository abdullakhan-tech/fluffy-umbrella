package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ZaytharPreferences
import com.example.domain.EngineCheckResult
import com.example.domain.EngineCheckStatus
import com.example.domain.EngineHealthReport
import com.example.domain.ProviderRoute
import com.example.domain.SmartEngineRouter
import com.example.domain.UiState
import com.example.domain.ZaytharEngineId
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.components.ZaytharIcons
import com.example.ui.theme.LocalCyberColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EngineHubScreen(
    prefs: ZaytharPreferences,
    diagnosticsState: UiState<EngineHealthReport>,
    activePluginCount: Int,
    onRunDiagnostics: (Boolean) -> Unit,
    onSelectRouteAndChat: (ProviderRoute, String) -> Unit,
    onNavigate: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    var routerInput by remember { mutableStateOf("") }
    var includeLiveProbe by remember { mutableStateOf(true) }
    val decision = remember(routerInput) { SmartEngineRouter.classifyPrompt(routerInput) }
    val isRunning = diagnosticsState is UiState.Loading

    val samplePrompts = remember {
        listOf(
            "Math Tool" to "calculate (24 * 15) + 42",
            "Image Art" to "Generate image of a holographic cyber command bridge at night",
            "Kotlin Code" to "Refactor Kotlin StateFlow repository with unit tests and security audit",
            "Brain Audit" to "Synthesize first-principles architecture roadmap and extract knowledge triples"
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "UNIFIED SMART INTELLIGENCE",
                titlePrefix = "AI Engine",
                titleAccent = "Nexus",
                trailing = {
                    Text(
                        text = "${ZaytharEngineId.entries.size} ENGINES · $activePluginCount PLUGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        // Smart Auto-Router Card
        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                CyberCardHeader(
                    title = "SMART AUTO-ROUTER ENGINE",
                    badge = decision.complexityLabel,
                    badgeColor = cyber.cyberGreen
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CyberIconBadge(
                        icon = ZaytharIcons.forEngine(decision.engine),
                        contentDescription = decision.engine.title,
                        sizeDp = 50.dp,
                        iconSizeDp = 26.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DETECTED: ${decision.engine.title.uppercase()} (${decision.confidencePercent}%)",
                            style = MaterialTheme.typography.titleSmall,
                            color = cyber.accentSoft
                        )
                        Text(
                            text = "Route: ${decision.recommendedRoute.title} · ${decision.candidateModels.joinToString(" -> ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = cyber.cyberGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    samplePrompts.forEach { (label, prompt) ->
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { routerInput = prompt },
                            shape = MaterialTheme.shapes.small,
                            color = cyber.panelElevated,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = cyber.accentSoft,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = routerInput,
                    onValueChange = { if (it.length <= 2000) routerInput = it },
                    label = { Text("TEST OR LAUNCH ANY PROMPT") },
                    placeholder = { Text("Type any prompt to auto-detect the optimal Zaythar engine & Gemini tier…") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_router_prompt_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = decision.rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                decision.localToolPreview?.let { hit ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = cyber.cyberGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, cyber.cyberGreen)
                    ) {
                        Text(
                            text = "INSTANT LOCAL RESULT [${hit.tool.uppercase()}]: ${hit.result}",
                            style = MaterialTheme.typography.bodySmall,
                            color = cyber.cyberGreen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CyberActionButton(
                        text = "SMART CHAT LAUNCH",
                        onClick = { onSelectRouteAndChat(ProviderRoute.AUTO, routerInput) },
                        modifier = Modifier.weight(1f),
                        testTag = "smart_chat_launch_button"
                    )
                    if (decision.engine != ZaytharEngineId.SMART_ROUTER) {
                        CyberSecondaryButton(
                            text = "OPEN TARGET",
                            onClick = { onNavigate(decision.engine.route) },
                            modifier = Modifier.weight(1f),
                            testTag = "open_target_engine_button"
                        )
                    }
                }
            }
        }

        // Engine Suite Diagnostics & Live Runner
        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(
                    title = "ALL-ENGINE VERIFICATION SUITE",
                    badge = "LOCAL + CLOUD PROBE",
                    badgeColor = cyber.cyberGreen
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Include Live Gemini Probe", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Pings gemini-3.8-flash via App Check & Firebase AI Logic",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = includeLiveProbe,
                        onCheckedChange = { includeLiveProbe = it },
                        modifier = Modifier.testTag("live_probe_toggle"),
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = cyber.accent)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                CyberActionButton(
                    text = if (isRunning) "RUNNING ALL ENGINES…" else "RUN ALL ZAYTHAR ENGINES",
                    onClick = { onRunDiagnostics(includeLiveProbe) },
                    enabled = !isRunning,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "run_all_engines_button"
                )

                when (diagnosticsState) {
                    is UiState.Loading -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = cyber.accent)
                    }
                    is UiState.Error -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = diagnosticsState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is UiState.Success -> {
                        val report = diagnosticsState.data
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = cyber.panelElevated,
                            border = BorderStroke(1.dp, cyber.cyberGreen.copy(alpha = 0.65f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "SUITE COMPLETE AT ${report.ranAtLabel} · ${report.passedCount}/${report.totalCount} ENGINES READY",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = cyber.cyberGreen,
                                    modifier = Modifier.testTag("engine_report_summary")
                                )
                                Text(
                                    text = "Live Cloud AI: ${report.liveGeminiStatus}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (report.liveGeminiLatencyMs != null) cyber.cyberGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        val diagResults = (diagnosticsState as? UiState.Success)?.data?.results.orEmpty()
        if (diagResults.isNotEmpty()) {
            items(diagResults, key = { it.engine.id }) { check ->
                EngineCheckCard(check = check, onOpen = { onNavigate(check.engine.route) })
            }
        }

        item {
            CyberSectionTitle(
                eyebrow = "ACTIVE ARCHITECTURE MATRIX",
                titlePrefix = "Installed",
                titleAccent = "AI Engines"
            )
        }

        item {
            val engines = ZaytharEngineId.entries.filter { it != ZaytharEngineId.SMART_ROUTER }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cols = if (maxWidth >= 720.dp) 2 else 1
                val rows = engines.chunked(cols)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { engine ->
                                EngineDirectoryCard(
                                    engine = engine,
                                    activeRoute = prefs.selectedProviderRoute,
                                    onClick = { onNavigate(engine.route) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size < cols) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineCheckCard(
    check: EngineCheckResult,
    onOpen: () -> Unit
) {
    val cyber = LocalCyberColors.current
    val statusColor = when (check.status) {
        EngineCheckStatus.PASS, EngineCheckStatus.READY -> cyber.cyberGreen
        EngineCheckStatus.WARN -> cyber.accentSoft
        EngineCheckStatus.FAIL -> MaterialTheme.colorScheme.error
    }
    val statusIcon = when (check.status) {
        EngineCheckStatus.PASS -> Icons.Rounded.Verified
        EngineCheckStatus.READY -> Icons.Rounded.CheckCircle
        EngineCheckStatus.WARN -> Icons.Rounded.WarningAmber
        EngineCheckStatus.FAIL -> Icons.Rounded.ErrorOutline
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = cyber.panelElevated,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onOpen)
            .testTag("engine_check_${check.engine.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CyberIconBadge(
                icon = ZaytharIcons.forEngine(check.engine),
                contentDescription = check.engine.title,
                sizeDp = 44.dp,
                iconSizeDp = 22.dp,
                tint = statusColor
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = check.engine.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = check.status.label,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (check.latencyMs > 0) "${check.status.label} · ${check.latencyMs}ms" else check.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
                Text(text = check.summary, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = check.details,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EngineDirectoryCard(
    engine: ZaytharEngineId,
    activeRoute: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyber = LocalCyberColors.current
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag("engine_card_${engine.id}"),
        shape = MaterialTheme.shapes.medium,
        color = cyber.panelElevated,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CyberIconBadge(
                icon = ZaytharIcons.forEngine(engine),
                contentDescription = engine.title,
                sizeDp = 46.dp,
                iconSizeDp = 24.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = engine.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (engine == ZaytharEngineId.NEURAL_CHAT) "${engine.modelTag} · ${activeRoute.uppercase()}" else engine.modelTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = cyber.cyberGreen
                )
                Text(
                    text = engine.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Open ${engine.title}",
                tint = cyber.accentSoft,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
