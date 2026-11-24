package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "System Default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    fun resolveIsDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    fun next(): ThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }

    companion object {
        fun fromId(id: String?): ThemeMode = entries.find { it.id == id } ?: SYSTEM
    }
}

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val RajdhaniFontFamily = FontFamily(
    Font(R.font.rajdhani, FontWeight.Normal),
    Font(R.font.rajdhani, FontWeight.Medium),
    Font(R.font.rajdhani, FontWeight.SemiBold),
    Font(R.font.rajdhani, FontWeight.Bold)
)

val AccentSwatches = listOf(
    0xFFFF2647L to "Crimson",
    0xFFFF623DL to "Ember",
    0xFFBB6CFFL to "Violet",
    0xFF39D9DBL to "Cyan",
    0xFF2ED573L to "Emerald",
    0xFFE6B84CL to "Amber",
    0xFF8DA4FFL to "Cobalt"
)

data class CyberExtraColors(
    val accent: Color = Color(0xFFFF2647),
    val accentSoft: Color = Color(0xFFFF647D),
    val cyberGreen: Color = Color(0xFF68E5B2),
    val panelGlass: Color = Color(0xFF0D0F15),
    val panelElevated: Color = Color(0xFF141821),
    val gridLine: Color = Color(0x0AFFFFFF),
    val glowStrength: Float = 0.72f,
    val metalSpecularity: Float = 0.65f,
    val contrastStrength: Float = 0.80f,
    val isDark: Boolean = true
)

val LocalCyberColors = staticCompositionLocalOf { CyberExtraColors() }

val ZaytharTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.6.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.5.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.4.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.4.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.3.sp
    )
)

val ZaytharShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun ZaytharTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = themeMode.resolveIsDark(isSystemInDarkTheme()),
    accentHex: Long = 0xFFFF2647L,
    glowStrength: Float = 0.72f,
    metalSpecularity: Float = 0.65f,
    contrastStrength: Float = 0.80f,
    content: @Composable () -> Unit
) {
    val normalizedHex = if (accentHex == 0xFFFF3155L) 0xFFFF2647L else accentHex
    val accent = Color(normalizedHex)
    val accentSoft = lerp(accent, Color.White, 0.25f)
    val green = Color(0xFF68E5B2)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = lerp(accent, Color(0xFF06070B), 0.72f),
            onPrimaryContainer = Color.White,
            secondary = accentSoft,
            onSecondary = Color(0xFF06070B),
            tertiary = green,
            onTertiary = Color(0xFF06070B),
            background = Color(0xFF06070B),
            onBackground = Color(0xFFF1F3F8),
            surface = Color(0xFF0D0F15),
            onSurface = Color(0xFFF1F3F8),
            surfaceVariant = Color(0xFF141821),
            onSurfaceVariant = Color(0xFF939AA8),
            outline = Color(0xFF282E3B),
            outlineVariant = Color(0xFF363E4F),
            error = Color(0xFFFF526B),
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = lerp(accent, Color.White, 0.82f),
            onPrimaryContainer = Color(0xFF1A2029),
            secondary = lerp(accent, Color(0xFF1A2029), 0.15f),
            onSecondary = Color.White,
            tertiary = Color(0xFF17694D),
            onTertiary = Color.White,
            background = Color(0xFFF0F2F6),
            onBackground = Color(0xFF141821),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF141821),
            surfaceVariant = Color(0xFFE9EDF3),
            onSurfaceVariant = Color(0xFF535D6B),
            outline = Color(0xFFC5CCD6),
            outlineVariant = Color(0xFFADB6C4),
            error = Color(0xFFC62840),
            onError = Color.White
        )
    }

    val extra = CyberExtraColors(
        accent = accent,
        accentSoft = accentSoft,
        cyberGreen = if (darkTheme) green else Color(0xFF17694D),
        panelGlass = if (darkTheme) Color(0xFF0D0F15) else Color(0xFFFFFFFF),
        panelElevated = if (darkTheme) Color(0xFF141821) else Color(0xFFF7F9FC),
        gridLine = if (darkTheme) Color(0x08FFFFFF) else Color(0x0B1A2029),
        glowStrength = glowStrength,
        metalSpecularity = metalSpecularity,
        contrastStrength = contrastStrength,
        isDark = darkTheme
    )

    CompositionLocalProvider(LocalCyberColors provides extra) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ZaytharTypography,
            shapes = ZaytharShapes,
            content = content
        )
    }
}
