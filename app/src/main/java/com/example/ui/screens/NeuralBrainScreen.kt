package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.FirestoreBrainRun
import com.example.domain.AgentExecutionPlan
import com.example.domain.JarvisAgentOrchestrator
import com.example.domain.NeuralBrainAnalysis
import com.example.domain.NeuralBrainMode
import com.example.domain.SuggestedTriple
import com.example.domain.UiState
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.components.ZaytharIcons
import com.example.ui.theme.LocalCyberColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NeuralBrainScreen(
    brainAnalysisState: UiState<NeuralBrainAnalysis>,
    brainRunsState: UiState<List<FirestoreBrainRun>>,
    memoriesCount: Int,
    factsCount: Int,
    onRunBrain: (String, NeuralBrainMode) -> Unit,
    onCommitTriples: (List<SuggestedTriple>) -> Unit,
    onDeleteBrainRun: (String) -> Unit,
    onSaveSynthesisToMemory: (String) -> Unit = {}
) {
    val cyber = LocalCyberColors.current
    var selectedMode by remember { mutableStateOf(NeuralBrainMode.REASONER) }
    var prompt by remember { mutableStateOf("") }
    var agentTask by remember { mutableStateOf("") }
    var agentPlan by remember { mutableStateOf<AgentExecutionPlan?>(null) }
    val pastRuns = (brainRunsState as? UiState.Success)?.data ?: emptyList()

    val presets = remember {
        listOf(
            "System Audit" to "Audit my current architecture for concurrency risks, data integrity gaps, and performance bottlenecks.",
            "Extract Facts" to "Consolidate key architectural decisions and entities from recent context into structured knowledge triples.",
            "Execution Roadmap" to "Build a phased milestone execution plan with risks, mitigations, and measurable validation criteria."
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "RAG-GROUNDED COGNITIVE SYNAPSE ENGINE",
                titlePrefix = "AI Brain &",
                titleAccent = "Neural Functions",
                trailing = {
                    Text(
                        text = "$memoriesCount MEM · $factsCount GRAPH",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    com.example.ui.components.MolecularNeuralOrb(
                        modifier = Modifier,
                        size = 112.dp,
                        activity = 0.82f
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "MOLECULAR NEURAL CORE",
                            style = MaterialTheme.typography.labelSmall,
                            color = cyber.accentSoft
                        )
                        Text(
                            text = "JARVIS COGNITIVE SWARM",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "6 specialist personas · guarded autonomy · verification-first execution",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = com.example.domain.JarvisAgentOrchestrator.team.joinToString("  ·  ") { it.name.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = cyber.cyberGreen
                        )
                    }
                }
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                CyberCardHeader(
                    title = "JARVIS TASK DIRECTOR",
                    badge = "6 PERSONAS · GUARDED AUTONOMY"
                )
                OutlinedTextField(
                    value = agentTask,
                    onValueChange = { if (it.length <= 4000) agentTask = it },
                    label = { Text("TASK FOR YOUR AI TEAM") },
                    placeholder = { Text("e.g. Audit my project, plan fixes, verify tests and report blockers…") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("jarvis_agent_task_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                CyberActionButton(
                    text = "BUILD EXECUTION PLAN",
                    onClick = {
                        agentPlan = runCatching { JarvisAgentOrchestrator.createPlan(agentTask) }.getOrNull()
                    },
                    enabled = agentTask.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "jarvis_agent_plan_button"
                )
                agentPlan?.let { plan ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "TASK TYPE · ${plan.taskKind.name.replace('_', ' ')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.accentSoft
                    )
                    Text(
                        text = "TEAM · ${plan.assignedAgents.joinToString("  ·  ") { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cyber.cyberGreen
                    )
                    plan.steps.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. ${step.title} — ${step.ownerAgentId.uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (plan.approvalRequired) {
                        Text(
                            text = "APPROVAL REQUIRED · ${plan.approvalReason}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "READ-ONLY PLAN · No external action will run without a registered tool.",
                            style = MaterialTheme.typography.labelSmall,
                            color = cyber.cyberGreen
                        )
                    }
                }
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                CyberCardHeader(
                    title = "SELECT NEURAL PIPELINE",
                    badge = "HYBRID RAG + JSON CORE"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeuralBrainMode.entries.forEach { mode ->
                        val active = mode == selectedMode
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { selectedMode = mode }
                                .testTag("brain_mode_${mode.id}"),
                            shape = MaterialTheme.shapes.small,
                            color = if (active) cyber.accent.copy(alpha = 0.2f) else cyber.panelElevated,
                            border = BorderStroke(1.dp, if (active) cyber.accent else MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = ZaytharIcons.forBrainMode(mode),
                                    contentDescription = null,
                                    tint = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = mode.title.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedMode.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (label, text) ->
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { prompt = text },
                            shape = MaterialTheme.shapes.small,
                            color = cyber.panelElevated,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = cyber.cyberGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { if (it.length <= 4000) prompt = it },
                    label = { Text("COGNITIVE INPUT / CONTEXT") },
                    placeholder = {
                        Text("Paste a decision, architecture goal, code dilemma, or raw notes to synthesize with RAG memory…")
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("brain_prompt_input")
                )

                Spacer(modifier = Modifier.height(12.dp))
                CyberActionButton(
                    text = if (brainAnalysisState is UiState.Loading) "SYNTHESIZING…" else "EXECUTE NEURAL FUNCTION",
                    onClick = { onRunBrain(prompt, selectedMode) },
                    enabled = brainAnalysisState !is UiState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "execute_brain_button"
                )
            }
        }

        when (brainAnalysisState) {
            is UiState.Loading -> {
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Running Hybrid RAG retrieval + structured Gemini neural synthesis…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cyber.cyberGreen
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = cyber.accent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
            is UiState.Error -> {
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Neural function error: ${brainAnalysisState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            is UiState.Success -> {
                val analysis = brainAnalysisState.data
                item {
                    NeuralBrainResultCard(
                        analysis = analysis,
                        onCommitTriples = onCommitTriples,
                        onSaveSynthesisToMemory = onSaveSynthesisToMemory
                    )
                }
            }
            else -> {}
        }

        if (pastRuns.isNotEmpty()) {
            item {
                Text(
                    text = "RECENT BRAIN SNAPSHOTS (${pastRuns.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(pastRuns, key = { it.id }) { run ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = cyber.panelGlass,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = cyber.accentSoft,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${run.mode.uppercase()} · ${run.confidence}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cyber.cyberGreen
                                )
                            }
                            Text(text = run.prompt, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            Text(
                                text = run.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3
                            )
                        }
                        IconButton(
                            onClick = { onDeleteBrainRun(run.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete brain run",
                                tint = cyber.accentSoft
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NeuralBrainResultCard(
    analysis: NeuralBrainAnalysis,
    onCommitTriples: (List<SuggestedTriple>) -> Unit,
    onSaveSynthesisToMemory: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
        CyberCardHeader(
            title = "${analysis.mode.title.uppercase()} OUTPUT",
            badge = "${analysis.confidence}% CONFIDENCE · ${analysis.modelUsed}",
            badgeColor = cyber.cyberGreen
        )
        LinearProgressIndicator(
            progress = { (analysis.confidence / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = cyber.cyberGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        AiMarkdownContent(markdown = analysis.summary)

        if (analysis.ragCitations.isNotEmpty()) {
            BrainListSection(
                title = "RAG EVIDENCE GROUNDED (${analysis.ragCitations.size})",
                items = analysis.ragCitations,
                accentColor = cyber.cyberGreen
            )
        }
        if (analysis.reasoningSteps.isNotEmpty()) {
            BrainListSection(
                title = "COGNITIVE REASONING TRACE",
                items = analysis.reasoningSteps.mapIndexed { i, step -> "${i + 1}. $step" },
                accentColor = cyber.accentSoft
            )
        }
        if (analysis.insights.isNotEmpty()) {
            BrainListSection(
                title = "KEY NEURAL INSIGHTS",
                items = analysis.insights.mapIndexed { i, item -> "${i + 1}. $item" },
                accentColor = cyber.accentSoft
            )
        }
        if (analysis.risksAndGaps.isNotEmpty()) {
            BrainListSection(
                title = "RISKS & BLIND SPOTS",
                items = analysis.risksAndGaps.map { "• $it" },
                accentColor = MaterialTheme.colorScheme.error
            )
        }
        if (analysis.actionPlan.isNotEmpty()) {
            BrainListSection(
                title = "PRIORITIZED ACTION PLAN",
                items = analysis.actionPlan.mapIndexed { i, step -> "P${i + 1} · $step" },
                accentColor = cyber.cyberGreen
            )
        }
        if (analysis.suggestedTriples.isNotEmpty()) {
            BrainListSection(
                title = "EXTRACTED KNOWLEDGE GRAPH TRIPLES (${analysis.suggestedTriples.size})",
                items = analysis.suggestedTriples.map { "• ${it.subject}  —[${it.predicate}]→  ${it.objectValue}" },
                accentColor = cyber.cyberGreen
            )
            Spacer(modifier = Modifier.height(10.dp))
            CyberSecondaryButton(
                text = "COMMIT ALL TO KNOWLEDGE GRAPH",
                onClick = { onCommitTriples(analysis.suggestedTriples) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "commit_triples_button"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        CyberSecondaryButton(
            text = "SAVE SYNTHESIS TO RAG MEMORY",
            onClick = {
                onSaveSynthesisToMemory("[AI BRAIN · ${analysis.mode.title}]\n${analysis.summary}".take(1950))
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "save_brain_memory_button"
        )
    }
}

@Composable
private fun BrainListSection(
    title: String,
    items: List<String>,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = accentColor
    )
    Spacer(modifier = Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
