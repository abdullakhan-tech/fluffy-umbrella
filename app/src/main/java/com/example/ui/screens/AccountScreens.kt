package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ZaytharPreferences
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.components.ZaytharIcons
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.SpaceGroteskFontFamily

@Composable
fun ProfileScreen(
    operatorName: String,
    userEmail: String?,
    userDisplayName: String?,
    onSaveOperatorName: (String) -> Unit,
    onSignOut: () -> Unit,
    isGuest: Boolean = false
) {
    val cyber = LocalCyberColors.current
    var nameField by remember(operatorName) { mutableStateOf(operatorName) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "OPERATOR ACCOUNT",
                titlePrefix = "User",
                titleAccent = "Profile"
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(cyber.accent.copy(alpha = 0.4f), cyber.panelGlass)
                                )
                            )
                            .border(2.5.dp, cyber.accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = operatorName.take(2).uppercase().ifBlank { "OP" },
                            fontFamily = SpaceGroteskFontFamily,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                    Text(
                        text = operatorName.uppercase(),
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (!userDisplayName.isNullOrBlank() || !userEmail.isNullOrBlank()) {
                        Text(
                            text = listOfNotNull(userDisplayName?.takeIf { it.isNotBlank() }, userEmail?.takeIf { it.isNotBlank() })
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    OutlinedTextField(
                        value = nameField,
                        onValueChange = { if (it.length <= 32) nameField = it },
                        label = { Text("OPERATOR DISPLAY NAME") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("operator_name_input")
                    )
                    CyberActionButton(
                        text = "SAVE PROFILE",
                        onClick = { onSaveOperatorName(nameField) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "save_profile_button"
                    )
                    CyberSecondaryButton(
                        text = if (isGuest) "SIGN IN WITH GOOGLE / EXIT GUEST" else "SIGN OUT OF GOOGLE ACCOUNT",
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "sign_out_button"
                    )
                    Text(
                        text = if (isGuest) "● GUEST MODE · LOCAL DEVICE SESSION" else "● GOOGLE AUTH · CLOUD FIRESTORE SYNCED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isGuest) cyber.accentSoft else cyber.cyberGreen
                    )
                    Text(
                        text = if (isGuest) {
                            "Guest conversations, RAG memory, and brain runs stay on this device for the active session. Sign in with Google anytime to enable Cloud Firestore sync."
                        } else {
                            "Operator profile, conversations, memory notes, and knowledge graph are isolated per authenticated Google account."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(title = "PLATFORM CAPABILITIES")

                PlanInfoCard(
                    tier = "CLOUD NEXUS SYNC",
                    badge = "FIRESTORE + GOOGLE AUTH",
                    description = "Authenticated multi-device storage for chat, memories, and knowledge facts",
                    highlighted = false
                )
                Spacer(modifier = Modifier.height(10.dp))
                PlanInfoCard(
                    tier = "NEURAL PRO",
                    badge = "AI LOGIC CONNECTED",
                    description = "Powered by Firebase AI Logic Gemini models & App Check",
                    highlighted = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No paid subscription or billing entitlement is activated by this interface. Plan cards are informational.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlanInfoCard(
    tier: String,
    badge: String,
    description: String,
    highlighted: Boolean
) {
    val cyber = LocalCyberColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = cyber.panelElevated,
        border = BorderStroke(1.dp, if (highlighted) cyber.accent else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = tier, style = MaterialTheme.typography.titleMedium)
            Text(text = badge, style = MaterialTheme.typography.labelLarge, color = cyber.accentSoft)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SecurityScreen(
    firebaseReady: Boolean,
    userEmail: String?,
    onToast: (String) -> Unit = {},
    isGuest: Boolean = false
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "CREDENTIAL MANAGEMENT",
                titlePrefix = "API &",
                titleAccent = "Security",
                trailing = {
                    Text(
                        text = "ZERO CLIENT KEYS",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberIconBadge(
                        icon = ZaytharIcons.SecurityShield,
                        contentDescription = "Zaythar Zero-Trust Security Shield",
                        sizeDp = 72.dp,
                        iconSizeDp = 36.dp
                    )
                    Text(
                        text = "Protect your intelligence core",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Zaythar AI uses Firebase AI Logic with App Check attestation, Google Sign-In authentication, and user-isolated Cloud Firestore security rules.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                SecurityStatusRow(
                    label = "Client secret exposure",
                    status = "BLOCKED BY DESIGN",
                    isSafe = true
                )
                SecurityStatusRow(
                    label = if (isGuest) "Session mode" else "Google Sign-In session",
                    status = if (isGuest) "GUEST (LOCAL SESSION)" else (userEmail ?: "AUTHENTICATED"),
                    isSafe = true
                )
                SecurityStatusRow(
                    label = "Cloud Firestore rules",
                    status = if (isGuest) "LOCAL ONLY (GUEST)" else "USER-ISOLATED",
                    isSafe = !isGuest
                )
                SecurityStatusRow(
                    label = "Firebase AI Logic & App Check",
                    status = if (firebaseReady) "INITIALIZED" else "NOT CONFIGURED",
                    isSafe = firebaseReady
                )

                Spacer(modifier = Modifier.height(14.dp))
                CyberActionButton(
                    text = "FIREBASE AI LOGIC DOCS",
                    onClick = {
                        runCatching {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://firebase.google.com/docs/ai-logic".toUri()
                            )
                            context.startActivity(intent)
                        }.onFailure {
                            onToast("No browser app available to open documentation.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "open_firebase_docs_button"
                )
            }
        }
    }
}

@Composable
private fun SecurityStatusRow(
    label: String,
    status: String,
    isSafe: Boolean
) {
    val cyber = LocalCyberColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSafe) cyber.cyberGreen else cyber.accentSoft
            )
        }
    }
}
