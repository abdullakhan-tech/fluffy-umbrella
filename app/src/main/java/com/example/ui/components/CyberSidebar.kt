package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier
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
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            BrandMarkBox(sizeDp = 40)
            Column {
                Text(
                    text = "ZAYTHAR",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 2.5.sp
                )
                Text(
                    text = "NEURAL COMMAND NEXUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
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
                Text(
                    text = section,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                destinations.filter { it.section == section }.forEach { dest ->
                    val active = currentRoute == dest.route
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { onSelectRoute(dest.route) }
                            .testTag("drawer_nav_${dest.route}"),
                        shape = MaterialTheme.shapes.small,
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
                                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (firebaseReady) cyber.cyberGreen else cyber.accent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (firebaseReady) "CORE SYSTEM ACTIVE" else "CORE STANDBY",
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            text = "HYBRID INTELLIGENCE · v2.5",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 15.dp, top = 2.dp)
        )
    }
}
