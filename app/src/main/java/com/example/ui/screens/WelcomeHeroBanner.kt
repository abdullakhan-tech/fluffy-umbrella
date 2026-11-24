package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.components.CyberCard
import com.example.ui.components.ZaytharSpiralEmblem
import com.example.ui.theme.LocalCyberColors

private data class WelcomeQuickCardData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val tag: String
)

@Composable
fun ZaytharHeroWelcomeBanner(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cyber = LocalCyberColors.current
    val quickCards = listOf(
        WelcomeQuickCardData("Get ideas", "Smart prompt templates", Icons.Rounded.Bolt, "prompts", "hero_quick_prompts"),
        WelcomeQuickCardData("Create images", "Nano Banana 2.1 studio", Icons.Rounded.AutoAwesome, "studio", "hero_quick_studio"),
        WelcomeQuickCardData("Write code", "Audit, debug & test", Icons.Rounded.Code, "code", "hero_quick_code"),
        WelcomeQuickCardData("Search & chat", "Live Google & Maps AI", Icons.Rounded.Language, "assistant", "hero_quick_chat")
    )

    CyberCard(modifier = modifier.fillMaxWidth(), highlighted = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            cyber.accent.copy(alpha = if (cyber.isDark) 0.16f else 0.08f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZaytharSpiralEmblem(size = 64.dp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Hello, I'm ")
                        withStyle(SpanStyle(color = cyber.accentSoft, fontWeight = FontWeight.Bold)) {
                            append("Zaythar AI")
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ask Anything... Think Deeper...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))

                quickCards.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { card ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .clickable { onNavigate(card.route) }
                                    .testTag(card.tag),
                                shape = RoundedCornerShape(14.dp),
                                color = cyber.panelElevated,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = card.icon,
                                        contentDescription = card.title,
                                        tint = cyber.accentSoft,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = card.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = card.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
