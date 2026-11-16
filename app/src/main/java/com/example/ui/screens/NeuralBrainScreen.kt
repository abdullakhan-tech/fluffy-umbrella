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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
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
import com.example.domain.NeuralBrainAnalysis
import com.example.domain.NeuralBrainMode
import com.example.domain.SuggestedTriple
import com.example.domain.UiState
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
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
    onDeleteBrainRun: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    var selectedMode by remember { mutableStateOf(NeuralBrainMode.REASONER) }
    var prompt by remember { mutableStateOf("") }
    val pastRuns = (brainRunsState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "COGNITIVE SYNAPSE ENGINE",
                titlePrefix = "AI Brain &",
                titleAccent = "Neural Functions",
                trailing = {
                    Text(
                        text = "$memoriesCount MEM · $factsCount FACTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                CyberCardHeader(
                    title = "SELECT NEURAL PIPELINE",
                    badge = "STRUCTURED JSON CORE"
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
                            border = BorderStroke(
                                1.dp,
                                if (active) cyber.accent else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
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

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { if (it.length <= 4000) prompt = it },
                    label = { Text("COGNITIVE INPUT / CONTEXT") },
                    placeholder = {
                        Text("Paste a decision, raw notes, architecture goal, or conversation log to synthesize…")
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
                            text = "Running structured Gemini neural synthesis with saved memories & thread context…",
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
                    CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                        CyberCardHeader(
                            title = "${analysis.mode.title.uppercase()} OUTPUT",
                            badge = "${analysis.confidence}% CONFIDENCE · ${analysis.modelUsed}",
                            badgeColor = cyber.cyberGreen
                        )
                        Text(
                            text = analysis.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (analysis.insights.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "KEY NEURAL INSIGHTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = cyber.accentSoft
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                analysis.insights.forEachIndexed { idx, insight ->
                                    Text(
                                        text = "${idx + 1}. $insight",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (analysis.suggestedTriples.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "EXTRACTED KNOWLEDGE GRAPH TRIPLES (${analysis.suggestedTriples.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = cyber.cyberGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                analysis.suggestedTriples.forEach { t ->
                                    Text(
                                        text = "• ${t.subject}  —[${t.predicate}]→  ${t.objectValue}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            CyberSecondaryButton(
                                text = "COMMIT ALL TO KNOWLEDGE GRAPH",
                                onClick = { onCommitTriples(analysis.suggestedTriples) },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "commit_triples_button"
                            )
                        }
                    }
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
                                    imageVector = Icons.Default.AutoAwesome,
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
                            Text(
                                text = run.prompt,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
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
                                imageVector = Icons.Default.Delete,
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
