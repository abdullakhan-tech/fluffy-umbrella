package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.FirestoreKnowledgeFact
import com.example.domain.AgentRole
import com.example.domain.UiState
import com.example.ui.components.CyberActionButton
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.SpaceGroteskFontFamily

@Composable
internal fun SharedKnowledgeGraphSection(
    knowledgeFactsState: UiState<List<FirestoreKnowledgeFact>>,
    knowledgeFacts: List<FirestoreKnowledgeFact>,
    onAddFact: (String, String, String) -> Unit,
    onDeleteFact: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    var factSubject by remember { mutableStateOf("") }
    var factPredicate by remember { mutableStateOf("") }
    var factObject by remember { mutableStateOf("") }

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

@Composable
internal fun AgentOrbNode(
    role: AgentRole,
    sizeDp: Dp,
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
internal fun MiniStatCard(
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
