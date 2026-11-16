package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.SpaceGroteskFontFamily

@Composable
fun CyberGridBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val cyber = LocalCyberColors.current
    val bg = MaterialTheme.colorScheme.background
    val accent = cyber.accent
    val gridColor = cyber.gridLine

    Box(
        modifier = modifier
            .background(bg)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = if (cyber.isDark) 0.16f else 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.78f, 0f),
                        radius = size.maxDimension * 0.65f
                    )
                )
                val step = 42.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            }
    ) {
        content()
    }
}

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val cyber = LocalCyberColors.current
    val borderColor = if (highlighted) cyber.accent else MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = cyber.panelGlass,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Box {
            // Top-left glowing cyber accent bar
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(2.5.dp)
                    .background(cyber.accent)
            )
            Column(
                modifier = Modifier.padding(18.dp),
                content = content
            )
        }
    }
}

@Composable
fun CyberCardHeader(
    title: String,
    badge: String? = null,
    badgeColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val cyber = LocalCyberColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (badge != null) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor ?: cyber.cyberGreen
                    )
                }
                trailing?.invoke()
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun CyberSectionTitle(
    eyebrow: String,
    titlePrefix: String,
    titleAccent: String,
    trailing: (@Composable () -> Unit)? = null
) {
    val cyber = LocalCyberColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildAnnotatedString {
                    append(titlePrefix)
                    if (titlePrefix.isNotEmpty()) append(" ")
                    withStyle(SpanStyle(color = cyber.accentSoft)) {
                        append(titleAccent)
                    }
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun CyberActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = ""
) {
    val cyber = LocalCyberColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = cyber.accent.copy(alpha = 0.85f),
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, cyber.accentSoft)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun CyberSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = ""
) {
    val cyber = LocalCyberColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, cyber.accent.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun TelemetryBar(
    label: String,
    statusText: String,
    progress: Float,
    useGreen: Boolean = false
) {
    val cyber = LocalCyberColors.current
    val fillColor = if (useGreen) cyber.cyberGreen else cyber.accentSoft
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleSmall,
                color = if (useGreen) cyber.cyberGreen else cyber.accentSoft
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(fillColor.copy(alpha = 0.5f), fillColor)
                        )
                    )
            )
        }
    }
}

@Composable
fun CyberTopBar(
    titlePrefix: String,
    titleAccent: String,
    clockText: String,
    motionEnabled: Boolean,
    firebaseReady: Boolean,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    onToggleMotion: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val cyber = LocalCyberColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (showMenuButton) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("nav_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open navigation menu",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Column {
                    Text(
                        text = "ZAYTHAR AI / COMMAND CENTER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildAnnotatedString {
                            append(titlePrefix)
                            if (titlePrefix.isNotEmpty()) append(" ")
                            withStyle(SpanStyle(color = cyber.accentSoft)) {
                                append(titleAccent)
                            }
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status dot + clock on wider viewports
                Row(
                    modifier = Modifier
                        .border(1.dp, cyber.cyberGreen.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (firebaseReady) cyber.cyberGreen else cyber.accent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = clockText,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = cyber.cyberGreen
                    )
                }

                IconButton(
                    onClick = onToggleMotion,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .border(
                            1.dp,
                            if (motionEnabled) cyber.accent else MaterialTheme.colorScheme.outline,
                            MaterialTheme.shapes.small
                        )
                        .testTag("motion_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = if (motionEnabled) "Disable 3D effects" else "Enable 3D effects",
                        tint = if (motionEnabled) cyber.accentSoft else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                        .testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = "Toggle light or dark theme",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun BrandMarkBox(modifier: Modifier = Modifier, sizeDp: Int = 40) {
    val cyber = LocalCyberColors.current
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .border(1.dp, cyber.accent, MaterialTheme.shapes.small)
            .background(
                Brush.linearGradient(
                    colors = listOf(cyber.accent.copy(alpha = 0.35f), cyber.panelGlass)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Z",
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (sizeDp * 0.58f).sp,
            color = Color.White
        )
    }
}
