package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.FirestoreKnowledgeFact
import com.example.domain.AgentRole
import com.example.domain.MultiAgentRunResult
import com.example.domain.UiState
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberSectionTitle
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollaborationScreen(
    agentRunState: UiState<MultiAgentRunResult>,
    knowledgeFactsState: UiState<List<FirestoreKnowledgeFact>>,
    onAgentNodeClick: (AgentRole) -> Unit,
    onRunTeam: (String, List<AgentRole>) -> Unit,
    onAddFact: (String, String, String) -> Unit,
    onDeleteFact: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    var taskText by remember { mutableStateOf("") }
    val selectedSpecialists = remember {
        mutableStateListOf(AgentRole.RESEARCH, AgentRole.CODING, AgentRole.ANALYSIS)
    }

    var factSubject by remember { mutableStateOf("") }
    var factPredicate by remember { mutableStateOf("") }
    var factObject by remember { mutableStateOf("") }

    val isRunning = agentRunState is UiState.Loading
    val knowledgeFacts = (knowledgeFactsState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "MULTI-AGENT WORKSPACE",
                titlePrefix = "Collaboration",
                titleAccent = "Nexus",
                trailing = {
                    Text(
                        text = "● ORCHESTRATOR READY",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        // Interactive Agent Network Orb Diagram
        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                ) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()
                    val nodeSize = 62.dp
                    val centerSize = 82.dp

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val accent = cyber.accent
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.14f), Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.minDimension * 0.55f
                            )
                        )
                        val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                        rotate(-25f) {
                            drawOval(
                                color = accent.copy(alpha = 0.35f),
                                topLeft = Offset(size.width * 0.1f, size.height * 0.16f),
                                size = Size(size.width * 0.8f, size.height * 0.68f),
                                style = Stroke(width = 1.5f, pathEffect = dash)
                            )
                        }
                        rotate(35f) {
                            drawOval(
                                color = accent.copy(alpha = 0.25f),
                                topLeft = Offset(size.width * 0.1f, size.height * 0.16f),
                                size = Size(size.width * 0.8f, size.height * 0.68f),
                                style = Stroke(width = 1.5f, pathEffect = dash)
                            )
                        }
                    }

                    // Center Core Agent
                    AgentOrbNode(
                        role = AgentRole.GENERAL,
                        sizeDp = centerSize,
                        isCenter = true,
                        onClick = { onAgentNodeClick(AgentRole.GENERAL) },
                        modifier = Modifier.align(Alignment.Center)
                    )

                    val positions = listOf(
                        AgentRole.RESEARCH to Pair(0.14f, 0.18f),
                        AgentRole.CODING to Pair(0.86f, 0.18f),
                        AgentRole.WRITING to Pair(0.10f, 0.78f),
                        AgentRole.ANALYSIS to Pair(0.90f, 0.78f),
                        AgentRole.CREATIVE to Pair(0.50f, 0.88f)
                    )
                    positions.forEach { (role, ratio) ->
                        val xOff = (widthPx * ratio.first).roundToInt()
                        val yOff = (heightPx * ratio.second).roundToInt()
                        AgentOrbNode(
                            role = role,
                            sizeDp = nodeSize,
                            isCenter = false,
                            onClick = { onAgentNodeClick(role) },
                            modifier = Modifier.offset {
                                val half = (nodeSize.toPx() / 2f).roundToInt()
                                IntOffset(xOff - half, yOff - half)
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "AGENT NETWORK (TAP TO OPEN)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "CONCURRENT ORCHESTRATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            }
        }

        // Multi-Agent Task Orchestrator
        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                CyberCardHeader(
                    title = "MULTI-AGENT TASK ORCHESTRATOR",
                    badge = "LIVE AI LOGIC"
                )

                Text(
                    text = "TASK FOR YOUR TEAM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { if (it.length <= 12_000) taskText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("agent_task_input"),
                    placeholder = {
                        Text("Describe the goal, constraints, expected output, and context…")
                    },
                    minLines = 3,
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AgentRole.specialists.forEach { role ->
                        val checked = role in selectedSpecialists
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable {
                                    if (checked) {
                                        selectedSpecialists.remove(role)
                                    } else if (selectedSpecialists.size < 4) {
                                        selectedSpecialists.add(role)
                                    }
                                }
                                .testTag("specialist_pick_${role.id}"),
                            shape = MaterialTheme.shapes.small,
                            color = cyber.panelElevated,
                            border = BorderStroke(
                                1.dp,
                                if (checked) cyber.accent else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 10.dp)
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { next ->
                                        if (!next) {
                                            selectedSpecialists.remove(role)
                                        } else if (selectedSpecialists.size < 4) {
                                            selectedSpecialists.add(role)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = cyber.accent)
                                )
                                Text(
                                    text = role.title.uppercase(),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choose 1–4 specialists. They run concurrently via Firebase AI Logic; the lead coordinator synthesizes their deliverables.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                CyberActionButton(
                    text = if (isRunning) "TEAM PROCESSING…" else "INITIALIZE AGENT TEAM",
                    onClick = { onRunTeam(taskText, selectedSpecialists.toList()) },
                    enabled = !isRunning,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "run_agent_team_button"
                )

                Spacer(modifier = Modifier.height(12.dp))
                when (agentRunState) {
                    is UiState.Idle -> {
                        Text(
                            text = "Awaiting task assignment.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cyber.cyberGreen
                        )
                    }
                    is UiState.Loading -> {
                        Text(
                            text = "Specialists are working concurrently. Waiting for coordinator synthesis…",
                            style = MaterialTheme.typography.bodySmall,
                            color = cyber.cyberGreen
                        )
                    }
                    is UiState.Error -> {
                        Text(
                            text = "Orchestration failed: ${agentRunState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is UiState.Success -> {
                        val res = agentRunState.data
                        Text(
                            text = "Completed ${res.completedCount} of ${res.totalCount} specialists · ${res.modelUsed}",
                            style = MaterialTheme.typography.labelLarge,
                            color = cyber.cyberGreen
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "COORDINATED RESULT",
                            style = MaterialTheme.typography.labelSmall,
                            color = cyber.accentSoft
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = cyber.panelElevated,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(48.dp)
                                        .background(cyber.accent)
                                )
                                Text(
                                    text = res.reply,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SPECIALIST DELIVERABLES",
                            style = MaterialTheme.typography.labelSmall,
                            color = cyber.accentSoft
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            res.deliverables.forEach { item ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = cyber.panelElevated,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "${item.agent.title.uppercase()} · ${item.status.uppercase()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (item.status == "completed") cyber.cyberGreen else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.output,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is UiState.Streaming -> {}
                }

                // Shared Knowledge Graph Editor
                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SHARED KNOWLEDGE GRAPH",
                    style = MaterialTheme.typography.titleSmall,
                    color = cyber.accentSoft
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add explicit facts as subject · relation · object. Synced to your Cloud Firestore account and shared with selected specialists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = factSubject,
                        onValueChange = { if (it.length <= 300) factSubject = it },
                        label = { Text("Subject (e.g. Zaythar AI)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fact_subject_input")
                    )
                    OutlinedTextField(
                        value = factPredicate,
                        onValueChange = { if (it.length <= 300) factPredicate = it },
                        label = { Text("Relation (e.g. uses)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fact_predicate_input")
                    )
                    OutlinedTextField(
                        value = factObject,
                        onValueChange = { if (it.length <= 300) factObject = it },
                        label = { Text("Object (e.g. Firebase AI Logic)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fact_object_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                CyberActionButton(
                    text = "SAVE KNOWLEDGE FACT",
                    onClick = {
                        onAddFact(factSubject, factPredicate, factObject)
                        if (factSubject.isNotBlank() && factPredicate.isNotBlank() && factObject.isNotBlank()) {
                            factSubject = ""
                            factPredicate = ""
                            factObject = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_fact_button"
                )

                if (knowledgeFactsState is UiState.Error) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sync warning: ${knowledgeFactsState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (knowledgeFacts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        knowledgeFacts.forEach { fact ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = cyber.panelElevated,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${fact.subject} · ${fact.predicate} · ${fact.objectValue}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onDeleteFact(fact.id) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete knowledge fact",
                                            tint = cyber.accentSoft
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Compact bottom stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStatCard(label = "SPECIALIST ROLES", value = "05", modifier = Modifier.weight(1f))
                MiniStatCard(label = "MAX PARALLEL", value = "04", modifier = Modifier.weight(1f))
                MiniStatCard(label = "COORDINATOR", value = "READY", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AgentOrbNode(
    role: AgentRole,
    sizeDp: androidx.compose.ui.unit.Dp,
    isCenter: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyber = LocalCyberColors.current
    Column(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(if (isCenter) cyber.accent.copy(alpha = 0.35f) else cyber.panelElevated)
            .border(2.dp, cyber.accent, CircleShape)
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag("agent_node_${role.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = role.shortCode,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = if (isCenter) 24.sp else 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = role.title.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = cyber.accentSoft
        )
    }
}

@Composable
private fun MiniStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val cyber = LocalCyberColors.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = cyber.panelGlass,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = cyber.accentSoft
            )
        }
    }
}
