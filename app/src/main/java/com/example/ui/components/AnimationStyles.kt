package com.example.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Visual rendering profiles for Zaythar's Neural Core.
 * Profiles change the live Compose renderer; they are not static screenshots.
 */
enum class AnimationStyle(
    val id: String,
    val title: String,
    val description: String,
    val accent: Color,
    val secondary: Color
) {
    HOLOGRAM("hologram", "Hologram", "Cyan projection & orbital glow", Color(0xFF52E8FF), Color(0xFF6BFFDE)),
    CYBERPUNK_NEON("cyberpunk-neon", "Cyberpunk Neon", "Electric magenta & blue", Color(0xFFFF3BD4), Color(0xFF35D9FF)),
    PAPER_CUTOUT("paper-cutout", "Paper Cutout", "Layered pastel craft", Color(0xFFFFA45B), Color(0xFF63B7A5)),
    ISOMETRIC("isometric", "Isometric 3D", "Mint geometry & depth", Color(0xFF62D6A5), Color(0xFF79AFFF)),
    BLUEPRINT("blueprint", "Blueprint", "Technical blue linework", Color(0xFF9BD7FF), Color(0xFF5AA9FF)),
    THERMAL_CAM("thermal-cam", "Thermal Cam", "Heat-map spectrum", Color(0xFFFF5A36), Color(0xFFFFD34D)),
    ANIME_CEL("anime-cel", "Anime Cel", "Bold cel-shaded energy", Color(0xFFFF668C), Color(0xFF8A7CFF)),
    ASCII_TERMINAL("ascii-terminal", "ASCII Terminal", "Monochrome green matrix", Color(0xFF66FF9A), Color(0xFF19C978)),
    WATERCOLOR("watercolor", "Watercolor", "Soft pigment washes", Color(0xFF7BBCEB), Color(0xFFB9A1E8)),
    CLAYMATION("claymation", "Claymation", "Warm tactile clay finish", Color(0xFFE6A36D), Color(0xFF8BCB8A));

    companion object {
        fun fromId(id: String): AnimationStyle =
            entries.firstOrNull { it.id == id } ?: HOLOGRAM
    }
}
