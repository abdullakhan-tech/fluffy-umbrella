package com.example.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Visual rendering profiles for Zaythar's Neural Core.
 * Each profile changes both the palette and the live motion geometry.
 */
enum class AnimationStyle(
    val id: String,
    val title: String,
    val description: String,
    val accent: Color,
    val secondary: Color,
    val rotationSpeed: Float,
    val pulseDurationMillis: Int,
    val packetSpeed: Float,
    val nodeScale: Float,
    val orbitRatios: List<Float>
) {
    HOLOGRAM("hologram", "Hologram", "Cyan projection & orbital glow", Color(0xFF52E8FF), Color(0xFF6BFFDE), 1.0f, 2000, 1.0f, 1.0f, listOf(0.84f, 1.03f, 1.19f)),
    CYBERPUNK_NEON("cyberpunk-neon", "Cyberpunk Neon", "Electric magenta & blue", Color(0xFFFF3BD4), Color(0xFF35D9FF), 1.65f, 1150, 1.8f, 1.15f, listOf(0.72f, 1.02f, 1.28f)),
    PAPER_CUTOUT("paper-cutout", "Paper Cutout", "Layered pastel craft", Color(0xFFFFA45B), Color(0xFF63B7A5), 0.32f, 3600, 0.45f, 1.25f, listOf(0.78f, 1.08f)),
    ISOMETRIC("isometric", "Isometric 3D", "Mint geometry & depth", Color(0xFF62D6A5), Color(0xFF79AFFF), 0.72f, 2600, 0.8f, 0.9f, listOf(0.68f, 0.92f, 1.16f)),
    BLUEPRINT("blueprint", "Blueprint", "Technical blue linework", Color(0xFF9BD7FF), Color(0xFF5AA9FF), 0.48f, 3000, 0.6f, 0.82f, listOf(0.9f, 1.12f)),
    THERMAL_CAM("thermal-cam", "Thermal Cam", "Heat-map spectrum", Color(0xFFFF5A36), Color(0xFFFFD34D), 1.25f, 1450, 1.35f, 1.3f, listOf(0.76f, 1.0f, 1.24f)),
    ANIME_CEL("anime-cel", "Anime Cel", "Bold cel-shaded energy", Color(0xFFFF668C), Color(0xFF8A7CFF), 1.4f, 900, 1.55f, 1.4f, listOf(0.82f, 1.08f)),
    ASCII_TERMINAL("ascii-terminal", "ASCII Terminal", "Monochrome green matrix", Color(0xFF66FF9A), Color(0xFF19C978), 0.85f, 1800, 2.0f, 0.75f, listOf(0.96f, 1.12f)),
    WATERCOLOR("watercolor", "Watercolor", "Soft pigment washes", Color(0xFF7BBCEB), Color(0xFFB9A1E8), 0.22f, 4400, 0.35f, 1.45f, listOf(0.88f, 1.18f)),
    CLAYMATION("claymation", "Claymation", "Warm tactile clay finish", Color(0xFFE6A36D), Color(0xFF8BCB8A), 0.58f, 3200, 0.55f, 1.55f, listOf(0.8f, 1.0f, 1.2f));

    companion object {
        fun fromId(id: String): AnimationStyle =
            entries.firstOrNull { it.id == id } ?: HOLOGRAM
    }
}
