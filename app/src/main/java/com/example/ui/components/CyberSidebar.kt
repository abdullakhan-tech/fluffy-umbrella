package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.FirestoreChatSession
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.SpaceGroteskFontFamily

data class ZaytharNavDestination(
    val route: String,
    val label: String,
    val section: String,
    val icon: ImageVector,
    val titlePrefix: String,
    val titleAccent: String
)

@Composable
fun CyberSidebar(
    destinations: List<ZaytharNavDestination>,
    currentRoute: String,
    firebaseReady: Boolean,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
    recentSessions: List<FirestoreChatSession> = emptyList(),
    activeSessionId: String = "",
    operatorName: String = "OPERATOR 01",
    isGuest: Boolean = false,
    onNewChatClick: (() -> Unit)? = null,
    onSelectSession: ((String) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    val cyber = LocalCyberColors.current
    Column(
        modifier = modifier
            .background(cyber.panelGlass)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            BrandMarkBox(sizeDp = 42)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Zaythar AI",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Your Advanced AI Assistant",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (onNewChatClick != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onNewChatClick)
                    .testTag("sidebar_new_chat_button"),
                shape = RoundedCornerShape(12.dp),
                color = cyber.accent.copy(alpha = 0.20f),
                border = BorderStroke(1.dp, cyber.accent.copy(alpha = 0.72f))
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(cyber.accent.copy(alpha = 0.28f), cyber.accent.copy(alpha = 0.08f))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Start a new chat",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "New Chat",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf("COMMAND", "WORKSPACE", "ACCOUNT").forEach { section ->
                val sectionItems = destinations.filter { it.section == section }
                if (sectionItems.isNotEmpty()) {
                    Text(
                        text = section,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                    sectionItems.forEach { dest ->
                        val active = currentRoute == dest.route
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { onSelectRoute(dest.route) }
                                .testTag("drawer_nav_${dest.route}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) cyber.accent.copy(alpha = 0.18f) else Color.Transparent,
                            border = if (active) BorderStroke(1.dp, cyber.accent.copy(alpha = 0.55f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = null,
                                    tint = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = dest.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (recentSessions.isNotEmpty() && onSelectSession != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "RECENT CHATS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                recentSessions.take(4).forEach { session ->
                    val isSelected = session.id == activeSessionId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { onSelectSession(session.id) }
                            .testTag("sidebar_recent_${session.id}"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) cyber.panelElevated else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                tint = if (isSelected) cyber.accentSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = session.title.ifBlank { "Untitled Chat" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = cyber.panelElevated,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(cyber.accent.copy(alpha = 0.25f))
                        .border(1.dp, cyber.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = operatorName.take(2).uppercase().ifBlank { "OP" },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = operatorName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (firebaseReady) cyber.cyberGreen else cyber.accent)
                        )
                        Text(
                            text = if (isGuest) "Guest Local" else if (firebaseReady) "AI Core Active" else "Standby",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onOpenSettings != null) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sidebar_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Open settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
